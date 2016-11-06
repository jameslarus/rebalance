package com.moneydance.modules.features.rebalance;// com.moneydance.modules.features.rebalance.PairedTableModel.java
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

import com.infinitekind.moneydance.model.CurrencyType;

import javax.swing.table.DefaultTableModel;
import java.util.Vector;


class PairedTableModel extends DefaultTableModel {
    private final Vector<String> columnTypes;
    private final CurrencyType currency;
    private final Vector<Vector<Object>> footer;

    PairedTableModel(Vector<Vector<Object>> data, Vector<Vector<Object>> footer, Vector<String> columnNames,
                     Vector<String> columnTypes, CurrencyType currency) {
        super(data, columnNames);
        this.columnTypes = columnTypes;
        this.currency = currency;
        this.footer = footer;
    }

    Vector<String> getColumnNames() {
        return columnIdentifiers;
    }

    Vector<String> getColumnTypes() {
        return columnTypes;
    }

    CurrencyType getCurrency() {
        return currency;
    }

    Vector<Vector<Object>> getFooterVector() {
        return footer;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return true;
    }

    public void setValueAt(Object value, int row, int col) {
        ((Vector<Object>)dataVector.get(row)).set(col, value); // Only edit data model
        fireTableCellUpdated(row, col);
    }
}

