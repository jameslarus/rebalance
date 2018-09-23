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


// Base functionality for paired tables.

class FooterTableBase extends JTable {
    FooterTableBase(TableModel tableModel) {
        super(tableModel);
        setColumnSelectionAllowed(false);
        setCellSelectionEnabled(true);
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    }

    FooterTableModel getDataModel() {
        return (FooterTableModel) dataModel;
    }

    // Draw a line above the footer to visually separate it.
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if (row == 0 && getDataModel().getFooterVector().isEmpty()) {
            JComponent jc = (JComponent) c;
            jc.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        }
        return c;
    }

    @Override
    public void columnMarginChanged(ChangeEvent event) {
        TableColumnModel eventModel = (DefaultTableColumnModel) event.getSource();
        TableColumnModel thisModel = getColumnModel();
        int columnCount = eventModel.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            thisModel.getColumn(i).setWidth(eventModel.getColumn(i).getWidth());
        }
        repaint();
    }

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

    // Base class for rendering a table cell containing a number of some type. NaN or null produces a blank cell.
    // Negative values are red.
    private class NumberCellRenderer extends DefaultTableCellRenderer {
        Double doubleValue;
        final String defaultValue = "?";

        boolean isCloseToZero(Double value) {
            return Math.abs(value) < 0.01;
        }

        @Override
        public void setValue(Object value) {
            try {
                setText(defaultValue);
                if (value == null) {
                    setText("");
                    return;
                }
                if (value instanceof String) {
                    value = Double.valueOf((String) value);
                }
                doubleValue = (Double) value;
                if (Double.isNaN(doubleValue)) {
                    setText("");
                } else {
                    if (isCloseToZero(doubleValue)) {
                        doubleValue = 0.0;
                    }
                    if (doubleValue < 0.0) {
                        setForeground(Color.RED);
                    } else {
                        setForeground(Color.BLACK);
                    }
                }
            } catch (Exception e) {
                setText("exp");
            }
        }
    }

    // Render a currency with given number of fractional digits.
    private class CurrencyRenderer extends NumberCellRenderer {
        private final boolean noDecimals;
        private final CurrencyType relativeTo;
        private final char decimalSeparator = '.'; // ToDo: Set from preferences (how?)
        private final NumberFormat noDecimalFormatter;

        CurrencyRenderer(CurrencyType currency, boolean noDecimals) {
            this.noDecimals = noDecimals;
            CurrencyTable ct = currency.getTable();
            String relativeToName = currency.getParameter(CurrencyType.TAG_RELATIVE_TO_CURR);
            relativeTo = relativeToName == null ? ct.getBaseType() : ct.getCurrencyByIDString(relativeToName);
            noDecimalFormatter = NumberFormat.getNumberInstance();
            noDecimalFormatter.setMinimumFractionDigits(0);
            noDecimalFormatter.setMaximumFractionDigits(0);
        }

        @Override
        boolean isCloseToZero(Double value) {
            return Math.abs(value) < 0.01;
        }

        @Override
        public void setValue(Object value) {
            try {
                super.setValue(value);
                if (getText().equals(defaultValue)) {
                    if (noDecimals) {
                        // MD format functions can't print comma-separated values without a decimal point so
                        // we have to do it ourselves
                        double scaledValue = doubleValue * relativeTo.getUserRate();
                        setText(relativeTo.getPrefix() + " " + noDecimalFormatter.format(scaledValue)
                                + relativeTo.getSuffix());
                    } else {
                        long scaledValue = relativeTo.convertValue(relativeTo.getLongValue(doubleValue));
                        setText(relativeTo.formatFancy(scaledValue, decimalSeparator));
                    }
                }
            } catch (Exception e) {
                setText("exp");
            }
        }
    }

    // Render a percentage with 2 digits after the decimal point.
    private class PercentRenderer extends NumberCellRenderer {
        private final char decimalSeparator = '.'; // ToDo: Set from preferences (how?)

        PercentRenderer() {
        }

        @Override
        boolean isCloseToZero(Double value) {
            return Math.abs(value) < 0.0001;
        }

        @Override
        public void setValue(Object value) {
            try {
                super.setValue(value);
                if (getText().equals(defaultValue)) {
                    setText(StringUtils.formatPercentage(doubleValue, decimalSeparator) + "%");
                }
            } catch (Exception e) {
                setText("exp");
            }
        }
    }

    // Render an integer.
    private class IntegerRenderer extends NumberCellRenderer {
        IntegerRenderer() {
        }

        @Override
        public void setValue(Object value) {
            try {
                super.setValue(value);
                if (getText().equals(defaultValue)) {
                    setText(Long.toString(Math.round(doubleValue)));
                }
            } catch (Exception e) {
                setText("exp");
            }
        }
    }

    // Directly edit target percentages (e.g. 5%). They are represented in the table as fractions (e.g. 0.05).
    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        String columnName = getDataModel().getColumnNames().get(column);
        switch (columnName) {
            case "Target":
                return new DefaultCellEditor(new JTextField() {
                    @Override
                    public void setText(String r) {
                        Double perValue = (Double) getValueAt(row, column) * 100.0;
                        super.setText(perValue.toString());
                    }
                }) {
                    @Override
                    public Object getCellEditorValue() {
                        Double perValue = Double.parseDouble((String) super.getCellEditorValue()) / 100.0;
                        return perValue.toString();
                    }
                };

            default:
                return null;
        }
    }
}