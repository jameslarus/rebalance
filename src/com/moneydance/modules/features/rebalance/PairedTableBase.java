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

import com.infinitekind.moneydance.model.CurrencyTable;
import com.infinitekind.moneydance.model.CurrencyType;
import com.infinitekind.util.StringUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.*;
import java.awt.*;
import java.text.NumberFormat;


// Basic functional for paired tables.

class PairedTableBase extends JTable {
    PairedTableBase(TableModel tableModel) {
        super(tableModel);
    }

    PairedTableModel getDataModel() {
        return (PairedTableModel) dataModel;
    }

    // Draw a line above the footer to visually separate it.
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if (row == 0 && getDataModel().getFooterVector().size() == 0) {
            JComponent jc = (JComponent) c;
            jc.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        }
        return c;
    }

    // Rendering depends on row (i.e. security's currency) as well as column
    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        DefaultTableCellRenderer renderer;
        String columnType = getDataModel().getColumnTypes().get(column);
        switch (columnType) {
            case "Text":
                renderer = new DefaultTableCellRenderer();
                renderer.setHorizontalAlignment(JLabel.LEFT);
                break;

            case "Currency0":
            case "Currency2":
                renderer = new CurrencyRenderer(getDataModel().getCurrency(), columnType.equals("Currency0"));
                renderer.setHorizontalAlignment(JLabel.RIGHT);
                break;

            case "Percent":
                renderer = new PercentRenderer();
                renderer.setHorizontalAlignment(JLabel.RIGHT);
                break;

            case "Integer":
                renderer = new IntegerRenderer();
                renderer.setHorizontalAlignment(JLabel.RIGHT);
                break;

            default:
                renderer = new DefaultTableCellRenderer();
        }
        return renderer;
    }

    @Override
    public void columnMarginChanged(ChangeEvent event) {
        final TableColumnModel eventModel = (DefaultTableColumnModel) event.getSource();
        final TableColumnModel thisModel = this.getColumnModel();
        final int columnCount = eventModel.getColumnCount();

        for (int i = 0; i < columnCount; i++) {
            thisModel.getColumn(i).setWidth(eventModel.getColumn(i).getWidth());
        }
        repaint();
    }

    // Render a currency with given number of fractional digits. NaN or null is an empty cell.
    // Negative values are red.
    private class CurrencyRenderer extends DefaultTableCellRenderer {
        private final boolean noDecimals;
        private final CurrencyType relativeTo;
        private final char decimalSeparator = '.'; // ToDo: Set from preferences (how?)
        private final NumberFormat noDecimalFormatter;


        CurrencyRenderer(CurrencyType currency, boolean noDecimals) {
            super();
            this.noDecimals = noDecimals;
            CurrencyTable ct = currency.getTable();
            String relativeToName = currency.getParameter(CurrencyType.TAG_RELATIVE_TO_CURR);
            if (relativeToName != null) {
                relativeTo = ct.getCurrencyByIDString(relativeToName);
            } else {
                relativeTo = ct.getBaseType();
            }
            noDecimalFormatter = NumberFormat.getNumberInstance();
            noDecimalFormatter.setMinimumFractionDigits(0);
            noDecimalFormatter.setMaximumFractionDigits(0);
        }

        boolean isZero(Double value) {
            return Math.abs(value) < 0.01;
        }

        @Override
        public void setValue(Object value) {
            if (value == null) {
                setText("");
            } else if (Double.isNaN((Double) value)) {
                setText("");
            } else {
                if (isZero((Double) value)) {
                    value = 0.0;
                }
                if (noDecimals) {
                    // MD format functions can't print comma-separated values without a decimal point so
                    // we have to do it ourselves
                    final double scaledValue = (Double) value * relativeTo.getUserRate();
                    setText(relativeTo.getPrefix() + " " + noDecimalFormatter.format(scaledValue) + relativeTo.getSuffix());
                } else {
                    final long scaledValue = relativeTo.convertValue(relativeTo.getLongValue((Double) value));
                    setText(relativeTo.formatFancy(scaledValue, decimalSeparator));
                }
                if ((Double) value < 0.0) {
                    setForeground(Color.RED);
                } else {
                    setForeground(Color.BLACK);
                }
            }
        }
    }

    // Render a percentage with 2 digits after the decimal point. Conventions as CurrencyRenderer
    private class PercentRenderer extends DefaultTableCellRenderer {
        private final char decimalSeparator = '.'; // ToDo: Set from preferences (how?)

        PercentRenderer() {
            super();
        }

        private boolean isZero(Double value) {
            return Math.abs(value) < 0.0001;
        }

        @Override
        public void setValue(Object value) {
            if (value == null) {
                setText("");
            } else if (Double.isNaN((Double) value)) {
                setText("");
            } else {
                if (isZero((Double) value)) {
                    value = 0.0;
                }
                setText(StringUtils.formatPercentage((Double) value, decimalSeparator) + "%");
                if ((Double) value < 0.0) {
                    setForeground(Color.RED);
                } else {
                    setForeground(Color.BLACK);
                }
            }
        }
    }

    // Render an integer. Conventions as CurrencyRenderer
    private class IntegerRenderer extends DefaultTableCellRenderer {
        IntegerRenderer() {
            super();
        }

        @Override
        public void setValue(Object value) {
            if (value == null) {
                setText("");
            } else if (Double.isNaN((Double) value)) {
                setText("");
            } else {
                Double num = (Double)value;
                long intNum = Math.round(num);
                setText(Long.toString(intNum));
                if (intNum < 0) {
                    setForeground(Color.RED);
                } else {
                    setForeground(Color.BLACK);
                }
            }
        }
    }
}