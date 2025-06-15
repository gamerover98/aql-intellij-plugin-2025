package com.arangodb.intellij.aql.lang.psi

import com.intellij.lang.ASTFactory

/**
 * Custom AST (Abstract Syntax Tree) factory for the AQL language plugin.
 *
 * [ASTFactory] is a base class from the IntelliJ Platform used
 * to create AST nodes and leaves during the parsing process. By extending
 * this class, you can customize how the PSI (Program Structure Interface)
 * tree is built for your language, enabling advanced features such as custom
 * syntax highlighting, code completion, and refactoring support.
 *
 * > This class can be extended to override methods for creating specific
 *   AST nodes or leaves, allowing fine-grained control over the structure
 *   and behavior of the language's PSI tree.
 */
class AqlASTFactory : ASTFactory() {
    //TODO: Improve leaf creation.
}