package com.arangodb.intellij.aql.lang

import com.arangodb.intellij.aql.fileTypes.AqlFile
import com.intellij.ide.structureView.*
import com.intellij.ide.structureView.StructureViewModel.ElementInfoProvider
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

/**
 * Factory that implements [PsiStructureViewFactory] to provide a
 * custom structure view for AQL files in the IntelliJ Platform.
 *
 * A PSI (Program Structure Interface) Structure View in IntelliJ shows the
 * hierarchical structure of a file (like classes, functions, or queries)
 * by analyzing its PSI tree.
 *
 * For example:
 *  * in a Java file, it displays all classes and methods;
 *  * in an AQL file, it can show collections, queries, and variables;
 *
 * helping developers navigate and understand the file structure quickly.
 */
class AqlPsiStructureViewFactory : PsiStructureViewFactory {

    /** Creates a structure view builder for the given PSI file. */
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        return object : TreeBasedStructureViewBuilder() {
            /** Creates a structure view model for the given PSI file. */
            override fun createStructureViewModel(editor: Editor?) = AqlStructureViewModel(psiFile)
        }
    }
}

/**
 * Structure view model for AQL files. Provides information about
 * the structure of an AQL file for the IntelliJ structure view.
 *
 * A `StructureViewModel` in IntelliJ defines how the structure of a file
 * (such as classes, methods, or custom elements) is displayed in the Structure tool window.
 *  > For example, in a Java file, it shows classes and methods; in a custom language,
 *    you can implement a `StructureViewModel` to display relevant elements.
 *
 * @param psiFile The PSI file to build the structure view for.
 */
private class AqlStructureViewModel(
    psiFile: PsiFile
) : StructureViewModelBase(
    psiFile,
    AqlStructureViewElement(psiFile) // <-- The AQL StructureViewElement for the view.
), ElementInfoProvider {

    /**
     * Indicates whether the given element should always
     * show a plus sign (+) in the structure view.
     *
     * @param element The structure view tree element.
     * @return False, as no element always shows a plus.
     */
    override fun isAlwaysShowsPlus(element: StructureViewTreeElement?) = false

    /**
     * Indicates whether the given element is always
     * a leaf in the structure view.
     *
     * @param element The structure view tree element.
     * @return True if the element is an [AqlFile], false otherwise.
     */
    override fun isAlwaysLeaf(element: StructureViewTreeElement?) = element is AqlFile
}