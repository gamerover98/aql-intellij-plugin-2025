package com.arangodb.intellij.aql.grammar.custom.psi.impl

import com.arangodb.intellij.aql.grammar.custom.psi.AqlMixinType
import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElementImpl
import com.intellij.lang.ASTNode

abstract class AqlPropertyLookupMixin(node: ASTNode) : AqlNamedElementImpl(node) {
    override val aqlType: AqlMixinType = AqlMixinType.PROPERTY_LOOKUP
}
