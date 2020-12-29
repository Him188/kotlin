/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.addDefaultBoundIfNecessary
import org.jetbrains.kotlin.fir.declarations.builder.FirTypeParameterBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameter
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.types.Variance

internal class JavaTypeParameterConverter private constructor(
    private val session: FirSession,
) {

    private fun JavaTypeParameter.bindTypeParameterToSymbol(
        firSymbol: FirTypeParameterSymbol,
        newTypeParameterStack: JavaTypeParameterStack
    ) {
        buildTypeParameter {
            session = this@JavaTypeParameterConverter.session
            origin = FirDeclarationOrigin.Java
            this.name = this@bindTypeParameterToSymbol.name
            symbol = firSymbol
            variance = Variance.INVARIANT
            isReified = false
            addBounds(this@bindTypeParameterToSymbol, newTypeParameterStack)
        }
    }

    private fun FirTypeParameterBuilder.addBounds(
        javaTypeParameter: JavaTypeParameter,
        stack: JavaTypeParameterStack,
    ) {
        for (upperBound in javaTypeParameter.upperBounds) {
            bounds += upperBound.toFirResolvedTypeRef(
                this@JavaTypeParameterConverter.session,
                stack,
                isForSupertypes = false,
                forTypeParameterBounds = true
            )
        }
        addDefaultBoundIfNecessary(isFlexible = true)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun convertTypeParameters(
        typeParameters: List<JavaTypeParameter>,
        typeParameterStack: JavaTypeParameterStack
    ): JavaTypeParameterStack {
        val typeParameterSymbols = typeParameters.associateWith { FirTypeParameterSymbol() }
        val newStack = typeParameterStack.pushParameters(typeParameterSymbols)
        typeParameters.forEach { typeParameter ->
            val symbol = typeParameterSymbols.getValue(typeParameter)
            typeParameter.bindTypeParameterToSymbol(symbol, newStack)
        }
        return newStack
    }

    companion object {
        fun convertTypeParameters(
            session: FirSession,
            typeParameterStack: JavaTypeParameterStack,
            typeParameters: List<JavaTypeParameter>
        ): JavaTypeParameterStack =
            JavaTypeParameterConverter(session).convertTypeParameters(typeParameters, typeParameterStack)
    }
}