@file:Suppress("EmptyCatchBlock", "TooGenericExceptionThrown", "VariableNaming", "ImplicitDefaultLocale", "WildcardImport", "MagicNumber", "MaxLineLength", "ComplexCondition", "CyclomaticComplexMethod", "LongMethod", "ReturnCount", "LongParameterList", "SwallowedException", "TooGenericExceptionCaught", "NestedBlockDepth", "FunctionNaming", "LoopWithTooManyJumpStatements", "TooManyFunctions")
package com.aladin.android.model

import java.util.UUID

/** Marcador manual colocado por el usuario sobre el cielo. */
data class Marker(
    val id: String = UUID.randomUUID().toString(),
    val position: SkyPosition,
    val label: String,
    val color: Long = 0xFFEF4444,
)
