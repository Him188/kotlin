/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers

import org.jetbrains.kotlin.fir.PrivateForInline
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class SymbolProviderCache<K, V : Any> @PrivateForInline constructor(@PrivateForInline val cache: MutableMap<K, Any>) {
    @PrivateForInline
    object NullValue

    @OptIn(PrivateForInline::class)
    inline fun lookupCacheOrCalculate(key: K, crossinline l: (K) -> V?): V? {
        val value = cache.computeIfAbsent(key) {
            l(key) ?: NullValue
        }

        @Suppress("UNCHECKED_CAST")
        return when (value) {
            NullValue -> null
            else -> value as V
        }
    }

    @OptIn(PrivateForInline::class)
    operator fun contains(key: K): Boolean = key in cache

    @Suppress("UNCHECKED_CAST")
    @OptIn(PrivateForInline::class)
    operator fun get(key: K): V? = cache[key].takeIf { it !== NullValue } as V?

    @OptIn(PrivateForInline::class)
    operator fun set(key: K, value: V) {
        cache[key] = value
    }

    @OptIn(PrivateForInline::class)
    fun remove(key: K) {
        cache.remove(key)
    }

    companion object {
        @OptIn(PrivateForInline::class)
        fun <K, V : Any> createThreadSafeCache(): SymbolProviderCache<K, V> =
            SymbolProviderCache(ConcurrentHashMap())

        @OptIn(PrivateForInline::class)
        fun <K, V : Any> createNonThreadSafeCache(): SymbolProviderCache<K, V> =
            SymbolProviderCache(hashMapOf())
    }
}
