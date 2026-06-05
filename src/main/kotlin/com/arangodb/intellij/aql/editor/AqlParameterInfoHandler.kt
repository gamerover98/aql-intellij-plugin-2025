package com.arangodb.intellij.aql.editor

import com.arangodb.intellij.aql.grammar.custom.psi.AqlPsiUtil
import com.arangodb.intellij.aql.grammar.generated.psi.AqlExpressionType
import com.arangodb.intellij.aql.grammar.generated.psi.AqlFunctionExpression
import com.arangodb.intellij.aql.grammar.generated.psi.AqlNamedFunctions
import com.arangodb.intellij.aql.grammar.generated.psi.AqlParameterVariable
import com.arangodb.intellij.aql.grammar.generated.psi.AqlTypes
import com.arangodb.intellij.aql.util.JSON
import com.intellij.lang.ASTNode
import com.intellij.lang.parameterInfo.CreateParameterInfoContext
import com.intellij.lang.parameterInfo.ParameterInfoHandlerWithTabActionSupport
import com.intellij.lang.parameterInfo.ParameterInfoUIContext
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.text.CharArrayUtil
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class AqlParameterInfoHandler :
    ParameterInfoHandlerWithTabActionSupport<AqlNamedFunctions, Any, AqlParameterVariable> {

    companion object {
        private val log = LoggerFactory.getLogger(AqlParameterInfoHandler::class.java)
        private val AST_NODES = emptyArray<ASTNode>()
        private val EMPTY = emptyArray<AqlParameterVariable>()
        @Volatile private var cachedParams: AqlParams? = null
        private val ALLOWED_PARENT_CLASSES: Set<Class<*>> =
            ContainerUtil.newHashSet(AqlNamedFunctions::class.java, AqlExpressionType::class.java)
    }

    override fun findElementForParameterInfo(context: CreateParameterInfoContext): AqlNamedFunctions? {
        val namedFunctions = getAqlNamedFunctions(context)
        if (namedFunctions != null) {
            context.itemsToShow = arrayOf(namedFunctions)
        }
        return namedFunctions
    }

    private fun getAqlNamedFunctions(context: CreateParameterInfoContext): AqlNamedFunctions? {
        val file = context.file
        var offset = context.offset
        val chars = file.viewProvider.contents
        if (offset >= chars.length) offset = chars.length - 1
        val offset1 = CharArrayUtil.shiftBackward(chars, offset, " \t\n\r")
        if (offset1 < 0) return null
        if (offset1 != offset) offset = offset1
        val element = file.findElementAt(offset) ?: return null
        val expr = PsiTreeUtil.getParentOfType(element, AqlFunctionExpression::class.java)
        return expr?.namedFunctions
    }

    override fun showParameterInfo(element: AqlNamedFunctions, context: CreateParameterInfoContext) {
        val offset = element.textRange.startOffset + 1
        context.showHint(element, offset, this)
    }

    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): AqlNamedFunctions? {
        val owner = context.parameterOwner
        return if (owner is AqlNamedFunctions) owner else null
    }

    override fun updateParameterInfo(aqlNamedFunctions: AqlNamedFunctions, context: UpdateParameterInfoContext) {
        if (context.parameterOwner == null || aqlNamedFunctions == context.parameterOwner) {
            context.parameterOwner = aqlNamedFunctions
        } else {
            context.removeHint()
        }
    }

    override fun updateUI(o: Any?, context: ParameterInfoUIContext) {
        if (o !is AqlNamedFunctions) return
        val functionName = AqlPsiUtil.getFunctionName(o)
        val params = getParameters(functionName)
        val builder = StringBuilder(params.size * 20)
        builder.append("<table>")
        params.forEachIndexed { index, param ->
            if (index == 0) {
                builder.append("<tr><td><b>$param</b></td></tr>")
            } else {
                builder.append("<tr><td>$param</td></tr>")
            }
        }
        builder.append("</table>")
        context.setupRawUIComponentPresentation(builder.toString())
    }

    override fun getActualParameters(o: AqlNamedFunctions): Array<AqlParameterVariable> = EMPTY

    override fun getActualParameterDelimiterType(): IElementType = AqlTypes.T_COMMA

    override fun getActualParametersRBraceType(): IElementType = AqlTypes.T_CLOSE

    override fun getArgumentListAllowedParentClasses(): Set<Class<*>> = ALLOWED_PARENT_CLASSES

    override fun getArgListStopSearchClasses(): Set<Class<*>> = ALLOWED_PARENT_CLASSES

    override fun getArgumentListClass(): Class<AqlNamedFunctions> = AqlNamedFunctions::class.java

    private fun getParameters(functionName: String?): List<String> {
        if (functionName == null) return emptyList()
        if (cachedParams == null) {
            try {
                javaClass.getResourceAsStream("/AqlFunctionParameters.json")?.use { stream ->
                    val text = InputStreamReader(stream, StandardCharsets.UTF_8).readText()
                    cachedParams = JSON.fromJson(text, AqlParams::class.java)
                } ?: return emptyList()
            } catch (e: IOException) {
                log.error("Error reading arguments list", e)
            }
        }
        return cachedParams?.forName(functionName) ?: emptyList()
    }

    private class AqlParams {
        var params: Map<String, List<String>> = HashMap()

        fun forName(functionName: String): List<String> =
            params[functionName] ?: emptyList()
    }
}
