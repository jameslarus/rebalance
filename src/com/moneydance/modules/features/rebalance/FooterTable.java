//
// Copyright (c) 2016, James Larus
//  All rights reserved.
//
//  Redistribution and use in source and binary forms, with or without
//  modification, are permitted provided that the following conditions are
//  met:
//
//  1. Redistributions of source code must retain the above copyright
//  notice, this list of conditions and the following disclaimer.
//
//  2. Redistributions in binary form must reproduce the above copyright
//  notice, this list of conditions and the following disclaimer in the
//  documentation and/or other materials provided with the distribution.
//
//  3. Neither the name of the copyright holder nor the names of its
//  contributors may be used to endorse or promote products derived from
//  this software without specific prior written permission.
//
//  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
//  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
//  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
//  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
//  HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
//  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
//  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
//  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
//  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
//  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
//  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.moneydance.modules.features.rebalance;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import java.awt.*;
import java.util.Vector;


class FooterTable extends FooterTableBase {
    private final FooterTableBase footerTable;
    private final Color lightLightGray = new Color(0xDCDCDC);
    private final int cellPadding = 20; // Extra space so cells aren't so tight

    FooterTable(FooterTableModel tableModel) {
        super(tableModel);

        fixColumnRenderer();
        setAutoCreateRowSorter(true);
        getRowSorter().toggleSortOrder(0); // Default: sort by symbol

        // Create footer table (not editable by user since values are function of account positions).
        // Could make cash target settable. Would require changes to algorithm since the rebalancing
        // could be overconstrained.
        footerTable = new FooterTableBase(new FooterTableModel(tableModel.getFooterVector(),
            new Vector<>(),
            tableModel.getColumnNames(),
            tableModel.getColumnTypes(),
            tableModel.getCurrency()) {
                @Override
                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return false;
                }
            });

        // Link body and footer columns
        // http://stackoverflow.com/questions/2666758/issue-with-resizing-columns-in-a-double-jtable
        footerTable.setColumnModel(getColumnModel());
        getColumnModel().addColumnModelListener(footerTable);
        footerTable.getColumnModel().addColumnModelListener(this);
        footerTable.getModel().addTableModelListener(this);
        footerTable.setCellSelectionEnabled(false);

        adjustColumnPreferredWidths();
    }

    // Changing the table's data model changes its headers, which erases their renderers. Replace them.
    private void fixColumnRenderer() {
        TableColumnModel cm = getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            TableColumn col = cm.getColumn(i);
            col.setHeaderRenderer(new HeaderRenderer());
        }
    }

    // Set the preferred widths for a column large enough to contain cells from the header, data, and footer.
    private void adjustColumnPreferredWidths() {
        for (int col = 0; col < getColumnCount(); col++) {
            TableColumnModel columnModel = getColumnModel();
            TableColumn column = columnModel.getColumn(col);
            int maxWidth = column.getPreferredWidth();
            maxWidth = Math.max(maxWidth, findColumnPreferredWidth(this, col));
            maxWidth = Math.max(maxWidth, findColumnPreferredWidth(footerTable, col));
            maxWidth = Math.max(maxWidth, findHeaderPreferredWidth(col));
            column.setPreferredWidth(maxWidth);
        }
    }

    private int findColumnPreferredWidth(JTable table, int col) {
        int maxWidth = 0;
        for (int row = 0; row < table.getRowCount(); row++) {
            TableCellRenderer rend = table.getCellRenderer(row, col);
            Object value = table.getValueAt(row, col);
            Component comp = rend.getTableCellRendererComponent(table, value, false, false, row, col);
            maxWidth = Math.max(maxWidth, comp.getPreferredSize().width + cellPadding);
        }
        return maxWidth;
    }

    private int findHeaderPreferredWidth(int col) {
        TableColumnModel columnModel = getColumnModel();
        TableColumn column = columnModel.getColumn(col);
        TableCellRenderer headerRenderer = column.getHeaderRenderer();
        if (headerRenderer == null) {
            headerRenderer = getTableHeader().getDefaultRenderer();
        }
        Object headerValue = column.getHeaderValue();
        Component headerComp = headerRenderer.getTableCellRendererComponent(this, headerValue, false, false, -1, col);
        return headerComp.getPreferredSize().width + cellPadding;
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if (!isRowSelected(row)) {
            c.setBackground(row % 2 == 0 ? getBackground() : lightLightGray);   // Banded rows
        }
        return c;
    }

    Vector<Vector<Object>> getDataVector() {
        return getDataModel().getDataVector();
    }

    Vector<Vector<Object>> getFooterDataVector() {
        return footerTable.getDataModel().getDataVector();
    }

    JTable getFooterTable() {
        return footerTable;
    }

    void dataChanged() {
        getDataModel().newDataAvailable(new TableModelEvent(getDataModel()));
        footerTable.getDataModel().newDataAvailable(new TableModelEvent(footerTable.getDataModel()));
    }

    public void setModel(FooterTableModel model) {
        super.setModel(model);
        footerTable.setModel(new FooterTableModel(model.getFooterVector(), new Vector<>(),
                model.getColumnNames(), model.getColumnTypes(), model.getCurrency()));
        adjustColumnPreferredWidths();
    }

    private class HeaderRenderer extends DefaultTableCellRenderer {
        HeaderRenderer() {
            setForeground(Color.BLACK);
            setFont(getFont().deriveFont(Font.BOLD));
            setBackground(Color.lightGray);
            setHorizontalAlignment(JLabel.CENTER);
        }
    }
}
