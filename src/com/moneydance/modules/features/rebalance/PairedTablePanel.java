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
import javax.swing.table.*;
import java.awt.*;

class PairedTablePanel extends JPanel {
    PairedTablePanel(PairedTable table) {
        super();

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(table.getTableHeader());
        add(table);
        add(table.getFooterTable());
        setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY));
    }

    private void adjustColumnPreferredWidths(PairedTable table) {
        for (int col = 0; col < table.getColumnCount(); col++) {
            TableColumnModel columnModel = table.getColumnModel();
            TableColumn column = columnModel.getColumn(col);
            int maxWidth = column.getPreferredWidth();
            maxWidth = Math.max(maxWidth, findColumnPreferredWidth(table, col));
            maxWidth = Math.max(maxWidth, findColumnPreferredWidth(table.getFooterTable(), col));
            maxWidth = Math.max(maxWidth, findHeaderPreferredWidth(table, col));

            column.setPreferredWidth(maxWidth);
        }
    }

    private int findColumnPreferredWidth(JTable table, int col) {
        int maxWidth = 0;
        int fontSizeIncrease = 2;
        for (int row = 0; row < table.getRowCount(); row++) {
            TableCellRenderer rend = table.getCellRenderer(row, col);
            Object value = table.getValueAt(row, col);
            Component comp = rend.getTableCellRendererComponent(table, value, false, false, row, col);
            int increasedWidth = 0;
            int preferredWidth = comp.getPreferredSize().width;

            // workaround--getPreferredSize insufficient for (at least some) numbers, so set width based on larger font size
            if (value instanceof Number) {
                JLabel comp1 = (JLabel) comp;
                Font f1 = comp1.getFont().deriveFont(comp1.getFont().getSize() + fontSizeIncrease);
                comp1.setFont(f1);
                increasedWidth = comp1.getPreferredSize().width;
            }
            maxWidth = Math.max(maxWidth, Math.max(preferredWidth, increasedWidth));
        }
        return maxWidth;
    }

    private int findHeaderPreferredWidth(JTable table, int col) {
        TableColumnModel columnModel = table.getColumnModel();
        TableColumn column = columnModel.getColumn(col);
        TableCellRenderer headerRenderer = column.getHeaderRenderer();
        if (headerRenderer == null) {
            headerRenderer = table.getTableHeader().getDefaultRenderer();
        }
        Object headerValue = column.getHeaderValue();
        Component headerComp = headerRenderer.getTableCellRendererComponent(table, headerValue, false, false, -1, col);
        return headerComp.getPreferredSize().width;
    }
}