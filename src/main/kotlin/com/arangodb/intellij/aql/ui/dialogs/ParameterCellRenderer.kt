package com.arangodb.intellij.aql.ui.dialogs

import java.awt.Component
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer

class ParameterCellRenderer : TableCellRenderer {

    private val cellRenderer = DefaultTableCellRenderer()

    override fun getTableCellRendererComponent(
        table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
    ): Component = cellRenderer
}
