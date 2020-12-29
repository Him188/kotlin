/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl.utils

/**
 * Lazily calculated value which runs postCompute in the same thread,
 * assuming that postCompute may try to read that value inside current thread,
 * So in the period then value is calculated but post compute was not finished,
 * only thread that initiated the calculating may see the value,
 * other threads will have to wait wait until that value is calculated
 */
internal class LazyValueWithPostCompute<V : Any>(calculate: () -> V, postCompute: (V) -> Unit) {
    private var _calculate: (() -> V)? = calculate
    private var _postCompute: ((V) -> Unit)? = postCompute

    /**
     * can be in one of the following three states:
     * null -- value is not initialized and thread are now executing [_postCompute]
     * [ValueIsPostComputingNow] -- thread with threadId has computed the value and only it can access it during post compute
     * some value of type [V] -- value is computed and post compute was executed, values is visible for all threads
     *
     * Value may be set only under [LazyValueWithPostCompute] intrinsic lock hold
     * And may be read from any thread
     */
    @Volatile
    private var value: Any? = null

    /**
     * We need at least one final field to be written in constructor to guarantee safe initialization of our [LazyValueWithPostCompute]
     */
    @Suppress("PrivatePropertyName", "unused")
    private val BARRIER = Unit

    @Suppress("UNCHECKED_CAST")
    fun getValue(): V {
        when (val stateSnapshot = value) {
            is ValueIsPostComputingNow -> {
                if (stateSnapshot.threadId == Thread.currentThread().id) {
                    return stateSnapshot.value as V
                } else {
                    synchronized(this) { // wait until other thread which holds the lock now computes the value
                        return value as V
                    }
                }
            }
            null -> synchronized(this) {
                // if we entered synchronized section that's mean that the value is not yet calculated and was not started to be calculated
                // or the some other thread calculated the value while we were waiting to acquire the lock

                if (value != null) { // some other thread calculated our value
                    return value as V
                }
                val calculatedValue = _calculate!!()
                value = ValueIsPostComputingNow(calculatedValue, Thread.currentThread().id) // only current thread may see the value
                _postCompute!!(calculatedValue)
                _calculate = null
                _postCompute = null
                value = calculatedValue
                return calculatedValue
            }
            else -> {
                return value as V
            }
        }
    }

    private class ValueIsPostComputingNow(val value: Any, val threadId: Long)
}