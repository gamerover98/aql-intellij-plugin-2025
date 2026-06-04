package com.arangodb.intellij.aql.grammar.custom.psi;

import com.arangodb.intellij.aql.grammar.custom.psi.impl.AqlIdMixin;
import com.arangodb.intellij.aql.grammar.custom.psi.impl.AqlNamedFunctionMixin;
import com.arangodb.intellij.aql.grammar.custom.psi.impl.AqlSystemPropertyMixin;
import com.arangodb.intellij.aql.grammar.generated.psi.AqlTypes;
import com.arangodb.intellij.aql.util.Icons;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.TokenType;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class AqlPsiUtil {
    private AqlPsiUtil() {
    }

    public static ItemPresentation getPresentation(final AqlNamedElement element) {
        return new ItemPresentation() {
            @NotNull
            @Override
            public String getPresentableText() {
                return element.getAqlType().name();
            }

            @NotNull
            @Override
            public String getLocationString() {
                return element.getContainingFile().getName();
            }

            @NotNull
            @Override
            public Icon getIcon(boolean unused) {
                if (element instanceof AqlNamedFunctionMixin) {
                    return Icons.ICON_FUNCTION;
                } else if (element instanceof AqlIdMixin) {
                    return Icons.ICON_ID;
                } else if (element instanceof AqlSystemPropertyMixin) {
                    return Icons.ICON_PROPERTY;
                }
                return Icons.ICON_ARANGO_SMALL;
            }
        };
    }

    //############################################
    // BNF used methods
    //############################################
    public static String getName(final AqlNamedElement element) {
        return element.getText();
    }

    public static String getFunctionName(final AqlNamedElement element) {
        return getFunctionName((PsiElement) element);
    }

    /** Extracts the bare function name from a PSI element whose text may include the argument list. */
    public static String getFunctionName(final PsiElement element) {
        final String text = element.getText();
        final int idx = text.indexOf('(');
        return idx > 0 ? text.substring(0, idx) : text;
    }

    /**
     * Renames the identifier of the given named element by replacing its leaf token in the AST.
     * Looks for a child {@code ID} token first; falls back to the first non-whitespace leaf.
     * Throws {@link IncorrectOperationException} if no renameable leaf can be found.
     */
    public static AqlNamedElement setName(final AqlNamedElement element, final String newName) {
        final ASTNode node = element.getNode();
        final ASTNode idNode = node.findChildByType(AqlTypes.ID);
        if (idNode instanceof LeafElement) {
            ((LeafElement) idNode).replaceWithText(newName);
            return element;
        }
        ASTNode child = node.getFirstChildNode();
        while (child != null) {
            if (child instanceof LeafElement && child.getElementType() != TokenType.WHITE_SPACE) {
                ((LeafElement) child).replaceWithText(newName);
                return element;
            }
            child = child.getTreeNext();
        }
        throw new IncorrectOperationException("Rename not supported for element type: " + element.getAqlType());
    }

}
