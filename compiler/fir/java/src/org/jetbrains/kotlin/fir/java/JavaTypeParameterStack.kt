/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.java.declarations.FirJavaConstructor
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameterListOwner

internal class JavaTypeParameterStack(
    private val typeParameterMap: PersistentMap<JavaTypeParameter, FirTypeParameterSymbol> = persistentHashMapOf()
) {
    fun pushParameters(parameters: Map<JavaTypeParameter, FirTypeParameterSymbol>): JavaTypeParameterStack =
        JavaTypeParameterStack(typeParameterMap.putAll(parameters))

    inline fun <R> withTypeParametersOf(
        declaration: JavaTypeParameterListOwner?,
        session: FirSession,
        action: (JavaTypeParameterStack) -> R
    ): R {
        val newStack = when (declaration) {
            null -> this
            else -> JavaTypeParameterConverter.convertTypeParameters(
                session,
                this,
                declaration.typeParameters
            )
        }
        return action(newStack)
    }

    operator fun get(javaTypeParameter: JavaTypeParameter): FirTypeParameterSymbol {
        return typeParameterMap[javaTypeParameter]
            ?: throw IllegalArgumentException("Cannot find Java type parameter ${javaTypeParameter.name} in stack")
    }

    companion object {
        val EMPTY: JavaTypeParameterStack = JavaTypeParameterStack()
    }
}

internal inline fun FirCallableDeclaration<*>.getOwnJavaTypeParameterStackOrDefault(
    createDefault: () -> JavaTypeParameterStack
): JavaTypeParameterStack = when (this) {
    is FirJavaMethod -> javaTypeParameterStack
    is FirJavaConstructor -> javaTypeParametersStack
    else -> createDefault()
}

internal fun FirCallableDeclaration<*>.getOwnJavaTypeParameterStackOrDefault(default: JavaTypeParameterStack): JavaTypeParameterStack =
    getOwnJavaTypeParameterStackOrDefault { default }