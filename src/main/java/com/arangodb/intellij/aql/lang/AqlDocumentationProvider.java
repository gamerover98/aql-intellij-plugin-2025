

package com.arangodb.intellij.aql.lang;

import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElement;
import com.arangodb.intellij.aql.grammar.generated.psi.AqlKeywordStatements;
import com.arangodb.intellij.aql.grammar.generated.psi.AqlNamedFunctions;
import com.arangodb.intellij.aql.grammar.generated.psi.AqlPropertyName;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.CharStreams;
import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class AqlDocumentationProvider extends AbstractDocumentationProvider {

    private static final Logger log = Logger.getInstance('#' + AqlDocumentationProvider.class.getName());

    private final LoadingCache<String, String> documentationCache =
            CacheBuilder
                    .newBuilder()
                    .maximumSize(100)
                    .expireAfterWrite(20, TimeUnit.MINUTES)
                    .build(new DocCacheLoader());


    /**
     * Provides quick navigation information for the given PSI element.
     *
     * <p>
     * This method is invoked for example when holding down the CTRL key and
     * hovering with the mouse over an AQL language keyword (e.g., FOR, RETURN, etc.).
     * </p>
     *
     * @param element         The PSI element used to retrieve quick navigation info.
     * @param originalElement The original PSI element (used for further context).
     * @return A string with quick navigation info if available, or null otherwise.
     */
    @Nullable
    @Override
    public String getQuickNavigateInfo(PsiElement element,
                                       PsiElement originalElement) {
        if (element instanceof AqlKeywordStatements aqlStatements) {
            // The characters of the node as a String, representing the statement's name.
            return String.valueOf(aqlStatements.getNode().getChars());
        }
        return null;
    }

    @Override
    public String generateDoc(final PsiElement element, @Nullable final PsiElement originalElement) {

        // The name of a node or a collection.
        if (element instanceof AqlPropertyName aqlPropertyName) {
            return "No documentation for '" + aqlPropertyName.getText() + "' found.";
        }

        if (element instanceof AqlNamedElement aqlNamedElement) {
            String name = aqlNamedElement.getName();

            if (aqlNamedElement instanceof AqlKeywordStatements aqlKeywordStatements) {
                // For AqlKeywordStatements, we use the text of the node as the name.
                name = aqlKeywordStatements.getNode().getChars().toString();
            } else if (aqlNamedElement instanceof AqlNamedFunctions aqlNamedFunctions) {
                // For AqlNamedFunctions, we use the text of the function as the name.
                name = aqlNamedFunctions.getText();
            }

            if (name != null) {
                return loadDocumentForName(name.toUpperCase());
            }
        }
        return "<no documentation>";
    }

    /**
     * Loads the documentation for the specified key from the cache.
     *
     * @param key The key representing the documentation topic to load.
     * @return The documentation string if found, or a default message if not.
     */
    private String loadDocumentForName(String key) {
        try {
            return documentationCache.get(key);
        } catch (ExecutionException e) {
            log.error("Error loading documentation for: " + key, e);
        }
        return "No documentation found";
    }

    private static class DocCacheLoader extends CacheLoader<String, String> {

        /**
         * Loads the documentation for the given key.
         *
         * @param key The key for which to load the documentation.
         * @return The documentation as a string, or a not found
         * message if no documentation is available.
         */
        @NotNull
        @Override
        public String load(@NotNull String key) {
            var notFound = "No documentation found for: '" + key + "'";

            // TODO: Replace with a more robust way to handle documentation files.
            try (var stream = AqlDocumentationProvider.class.getResourceAsStream("/docs/" + key + ".html")) {
                if (stream == null) {
                    return notFound;
                }
                return CharStreams.toString(new InputStreamReader(stream, StandardCharsets.UTF_8));
            } catch (IOException ignore) {
                // ignore
            }

            return notFound;
        }
    }
}
