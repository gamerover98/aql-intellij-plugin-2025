package com.arangodb.intellij.aql.grammar.custom.psi.impl

import com.arangodb.intellij.aql.grammar.custom.psi.AqlMixinType
import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElementImpl
import com.intellij.lang.ASTNode

abstract class AqlIdMixin(node: ASTNode) : AqlNamedElementImpl(node) {
    override val aqlType: AqlMixinType = AqlMixinType.ID
}
