package com.arangodb.intellij.aql.ui.dialogs

import javax.swing.AbstractCellEditor
import javax.swing.JTable
import javax.swing.table.TableCellEditor
import java.awt.Component

class ParameterCellEditor : AbstractCellEditor(), TableCellEditor {

    override fun getCellEditorValue(): Any = "xxxs"

    override fun getTableCellEditorComponent(
        table: JTable, value: Any?, isSelected: Boolean, row: Int, column: Int
    ): Component? = null
}
