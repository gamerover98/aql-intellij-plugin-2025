package com.arangodb.intellij.aql.lang

import com.arangodb.intellij.aql.grammar.custom.psi.AqlMixinType
import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElement
import com.arangodb.intellij.aql.lang.psi.*
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val EMPTY_PSI_REF_ARRAY = arrayOfNulls<PsiReference>(0)

// Priority for the reference provider to ensure it is checked before others.
private const val REFERENCE_PROVIDER_PRIORITY = PsiReferenceRegistrar.HIGHER_PRIORITY

/**
 * Contributes custom PSI reference providers for the AQL language in the IntelliJ Platform.
 *
 * This implementation registers reference providers that enable IDE features such as
 * `Go to Definition` and `Find Usages` for AQL elements. It determines the type of each
 * AQL PSI element and creates the appropriate reference, improving code navigation and
 * analysis for users working with AQL in the IDE.
 *
 * @property log Logger instance for diagnostic output.
 */
class AqlPsiReferenceContributor(
    private val log: Logger = LoggerFactory.getLogger(AqlPsiReferenceContributor::class.java)
) : PsiReferenceContributor() {

    /**
     * Registers reference providers for AQL PSI elements.
     *
     * This method is called by the IntelliJ platform to allow this contributor to register
     * custom reference providers for specific PSI elements. Here, it registers a provider
     * for all PSI elements that belong to the AQL language.
     *
     * The registered provider checks if the given element is an [AqlNamedElement]. If so,
     * it creates a reference for it based on its type (using [createReference]). The reference
     * is used by the IDE for features like `Go to Definition` and `Find Usages`.
     *
     * @param registrar The registrar used to register reference providers.
     */
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Register a reference provider for all PSI elements with AQL language.
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement().withLanguage(AqlLanguage),
            object : PsiReferenceProvider() {

                /**
                 * Returns references for the given PSI element.
                 *
                 * If the element is an [AqlNamedElement], creates and returns
                 * the appropriate reference(s) for it; otherwise, returns an empty array.
                 */
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference?> {
                    // Check if the element is an AQL named element
                    if (element is AqlNamedElement) {
                        // The reference will cover the entire text of the element
                        val rangeInElement = TextRange(0, element.text.length)
                        // Create and return the reference(s) for this element
                        return createReference(
                            element,
                            element as PsiNamedElement,
                            rangeInElement
                        )
                    }
                    // Return empty array if not applicable
                    return EMPTY_PSI_REF_ARRAY
                }
            },
            REFERENCE_PROVIDER_PRIORITY
        )
    }

    private fun createReference(
        identifier: AqlNamedElement,
        element: PsiNamedElement?,
        rangeInElement: TextRange?
    ): Array<PsiReference?> {

        // Find the type of the identifier and create the appropriate reference.
        val reference = when (identifier.aqlType) {
            AqlMixinType.FUNCTION -> AqlFunctionReference(identifier, rangeInElement)
            AqlMixinType.VAR_PARAMETER -> AqlPropertyParameterReference(identifier, rangeInElement)
            AqlMixinType.VAR_PLACEHOLDER -> AqlPropertyPlaceholderReference(identifier, rangeInElement)
            AqlMixinType.SYSTEM_PROPERTY -> AqlSystemPropertyReference(identifier, rangeInElement)
            AqlMixinType.PROPERTY_LOOKUP -> AqlPropertyLookupReference(identifier, rangeInElement)
            AqlMixinType.ID -> AqlPropertyIdReference(identifier, rangeInElement)
            AqlMixinType.KEYWORD -> AqlKeywordReference(identifier, rangeInElement)
            else -> null
        }

        if (reference != null) {
            return arrayOf(reference)
        }

        log.info("Identifier AQL Type: {}", identifier.aqlType)
        log.info("Element text: {}", element?.text)
        return EMPTY_PSI_REF_ARRAY
    }
}