// ReBalanceWindow.java
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

import com.moneydance.awt.*;
import com.infinitekind.moneydance.model.*;

import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;


// Window used for the ReBalance interface

class ReBalanceWindow extends JFrame implements ChangeListener, ItemListener, TableModelListener {
    private Main extension;
    private AccountBook book;

    private JComboBox<String> accountList;
    private JCheckBox percentThresholdCheckbox;
    private JSpinner percentThreshold;
    private JCheckBox valueThresholdCheckbox;
    private JSpinner valueThreshold;
    private PairedTable rebalanceTable;

    ReBalanceWindow(Main extension) {
        super("ReBalance Investment Account");
        this.extension = extension;
        book = extension.getUnprotectedContext().getCurrentAccountBook();

        JPanel pane = new JPanel(new GridBagLayout());
        this.setContentPane(pane);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.weightx = 0.1;

        // Row 1
        c.anchor = GridBagConstraints.PAGE_START;

        JLabel label1 = new JLabel("Account");
        c.gridx = 0;
        c.gridy = 0;
        pane.add(label1, c);

        JLabel label2 = new JLabel("% Threshold");
        c.gridx = 1;
        c.gridy = 0;
        pane.add(label2, c);

        JLabel label3 = new JLabel("Value Threshold");
        c.gridx = 2;
        c.gridy = 0;
        pane.add(label3, c);

        // Row 2
        c.anchor = GridBagConstraints.LINE_START;

        accountList = new JComboBox<>(getInvestmentAccounts());
        accountList.addItemListener(this);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        pane.add(accountList, c);

        JPanel ptPanel = new JPanel();
        percentThresholdCheckbox = new JCheckBox("", false);
        percentThresholdCheckbox.addChangeListener(this);
        ptPanel.add(percentThresholdCheckbox);
        SpinnerModel percentModel = new SpinnerNumberModel(10.0, 0.0, 100.0, 0.1);
        percentThreshold = new JSpinner(percentModel);
        percentThreshold.addChangeListener(this);
        ptPanel.add(percentThreshold);
        c.gridx = 1;
        c.gridy = 1;
        pane.add(ptPanel, c);

        JPanel amtPanel = new JPanel();
        valueThresholdCheckbox = new JCheckBox("", false);
        valueThresholdCheckbox.addChangeListener(this);
        amtPanel.add(valueThresholdCheckbox);
        SpinnerModel valueModel = new SpinnerNumberModel(1000, 0, 100000, 1);
        valueThreshold = new JSpinner(valueModel);
        valueThreshold.addChangeListener(this);
        amtPanel.add(valueThreshold);
        c.gridx = 2;
        c.gridy = 1;
        pane.add(amtPanel, c);

        // Row 3
        PairedTableModel tableModel = createRebalanceTableModel((String) accountList.getSelectedItem());
        rebalanceTable = new PairedTable(tableModel);
        c.gridx = 0;
        c.gridwidth = 3;
        c.gridy = 2;
        pane.add(new PairedTablePanel(rebalanceTable), c);
        tableModel.addTableModelListener(this);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        enableEvents(WindowEvent.WINDOW_CLOSING);

        pack();
        AwtUtil.centerWindow(this);
    }

    private Vector<String> getInvestmentAccounts() {
        Vector<String> accounts = new Vector<>();
        if (book != null) {
            for (Account a : book.getRootAccount().getSubAccounts()) {
                if (a.getAccountType() == Account.AccountType.INVESTMENT && !a.getAccountIsInactive()) {
                    accounts.add(a.getAccountName());
                }
            }
        }
        Collections.sort(accounts);
        return accounts;
    }

    private final String[] names = {"Name", "Target", "Actual", "Shares", "Price", "Value", "Buy", "Sell", "Result"};
    private final Vector<String> columnNames = new Vector<>(Arrays.asList(names));
    private final int NAME_COL = columnNames.indexOf("Name");
    private final int TARGET_COL = columnNames.indexOf("Target");
    private final int ACTUAL_COL = columnNames.indexOf("Actual");
    private final int SHARE_COL = columnNames.indexOf("Shares");
    private final int PRICE_COL = columnNames.indexOf("Price");
    private final int VALUE_COL = columnNames.indexOf("Value");
    private final int BUY_COL = columnNames.indexOf("Buy");
    private final int SELL_COL = columnNames.indexOf("Sell");
    private final int RESULT_COL = columnNames.indexOf("Result");

    private final String[] types = {"Text", "Percent", "Percent", "Integer", "Currency2", "Currency2", "Integer", "Integer", "Percent"};
    private final Vector<String> columnTypes = new Vector<>(Arrays.asList(types));

    private PairedTableModel createRebalanceTableModel(String accountName) {
        Account account = book.getRootAccount().getAccountByName(accountName);
        Vector<Vector<Object>> data = new Vector<>();
        Vector<Vector<Object>> footer = new Vector<>();

        fillRebalanceTable(accountName, data, footer);
        return new PairedTableModel(data, footer, columnNames, columnTypes, account.getCurrencyType()) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == TARGET_COL;
            }
        };
    }

    private void fillRebalanceTable(String accountName, Vector<Vector<Object>> data, Vector<Vector<Object>> footer) {
        Account account = book.getRootAccount().getAccountByName(accountName);
        Double scale = Math.pow(10.0, account.getCurrencyType().getDecimalPlaces());
        Double totalValue = account.getRecursiveBalance() / scale;
        Double cashValue = account.getBalance() / scale;

        // Securities
        for (Account a : account.getSubAccounts()) {
            if (a.getAccountType() == Account.AccountType.SECURITY && !a.getCurrencyType().getHideInUI()) {
                if (a.getCurrencyType().getCurrencyType() == CurrencyType.Type.SECURITY) {
                    createEntry(data, a, a.getAccountName(), totalValue);
                }
            }
        }

        // Cash
        if (account.getCurrencyType().getCurrencyType() == CurrencyType.Type.CURRENCY) {
            createEntry(footer, account, "Cash", cashValue);
        }

        // Total value
        Vector<Object> entry = new Vector<>(names.length);
        entry.add("Total");
        entry.add(null);
        entry.add(null);
        entry.add(null);
        entry.add(null);
        entry.add(totalValue);
        entry.add(null);
        entry.add(null);
        entry.add(null);
        footer.add(entry);

        rebalance(data, footer, totalValue);
    }

    private void createEntry(Vector<Vector<Object>> entries, Account account, String name, Double totalValue) {
        Double balance = account.getBalance() / Math.pow(10.0, account.getCurrencyType().getDecimalPlaces());
        Double price = 1.0 / account.getCurrencyType().getUserRate();

        Vector<Object> entry = new Vector<>(names.length);
        entry.add(name);
        entry.add(0.1);    // FIXME: 0 or saved state
        entry.add(balance * price / totalValue);
        entry.add(balance);
        entry.add(price);
        entry.add(balance * price);
        entry.add(0.0);   // calc at end
        entry.add(0.0);   // calc at end
        entry.add(0.0);   // calc at end
        entries.add(entry);
    }

    // This could be an integer-linear programming problem, but something simpler should work.
    // First, find all securities to sell and cash that can be used.
    // Then, compute integer number of shares to buy.
    // Goal: get close to targets, but do not incur unnecessary trading costs (i.e., buy 1 or 2 shares).
    private void rebalance(Vector<Vector<Object>> data, Vector<Vector<Object>> footer) {
        Account account = book.getRootAccount().getAccountByName((String) accountList.getSelectedItem());
        Double totalValue = account.getRecursiveBalance() / Math.pow(10.0, account.getCurrencyType().getDecimalPlaces());
        rebalance(data, footer, totalValue);
    }

    private void rebalance(Vector<Vector<Object>> data, Vector<Vector<Object>> footer, Double totalValue) {
        Vector<Object> cashEntry = footer.get(0);
        Double cash = (Double)cashEntry.get(VALUE_COL);
        Double availableFunds = cash;

        // Sell excess shares
        for (Vector<Object> entry : data) {
            entry.set(SELL_COL, null);
            entry.set(BUY_COL, null);
            availableFunds += extractExcessValue(entry, totalValue);
        }

        // Spend funds on new shares
        for (Vector<Object> entry : data) {
            availableFunds -= useExcessValue(entry, totalValue, availableFunds);
        }

        // Adjust cash
        cashEntry.set(SELL_COL, null);
        cashEntry.set(BUY_COL, null);
        if (!cash.equals(availableFunds)) {
            if (cash > availableFunds) {
                cashEntry.set(SELL_COL, cash - availableFunds);
            } else {
                cashEntry.set(BUY_COL, availableFunds - cash);
            }
        }
        cashEntry.set(RESULT_COL, availableFunds / totalValue);
        //ToDo: use remaining funds?
    }

    // Find excess value of securities that exceed target by appropriate amount (either % or magnitude).
    // Return amount of sale or purchase
    private Double extractExcessValue(Vector<Object> entry, Double totalValue) {
        Double targetError = (Double) entry.get(ACTUAL_COL) - (Double) entry.get(TARGET_COL);
        if (targetError > 0.0) {
            Double price = (Double) entry.get(PRICE_COL);
            Double valueError = targetError * totalValue;
            Double sharesToSell = Math.floor(valueError / price);
            if (sharesToSell > 0.0 && exceedsALimit(targetError, valueError)) {
                entry.set(SELL_COL, sharesToSell);
                Double oldShares = (Double) entry.get(SHARE_COL) - sharesToSell;
                entry.set(RESULT_COL, (oldShares * price) / totalValue);
                return sharesToSell * price;
            } else {
                entry.set(RESULT_COL, entry.get(ACTUAL_COL));
            }
        }
        return 0.0;
    }

    private Double useExcessValue(Vector<Object> entry, Double totalValue, Double availableFunds) {
        Double targetError = (Double) entry.get(TARGET_COL) - (Double) entry.get(ACTUAL_COL);
        if (targetError > 0.0) {
            Double valueError = targetError * totalValue;
            Double price = (Double) entry.get(PRICE_COL);
            Double sharesToBuy = Math.floor(valueError / price);
            if (sharesToBuy > 0.0 && exceedsALimit(targetError, valueError)) {
                entry.set(BUY_COL, sharesToBuy);
                Double newShares = (Double) entry.get(SHARE_COL) + sharesToBuy;
                entry.set(RESULT_COL, (newShares * price) / totalValue);
                return sharesToBuy * price;
            } else {
                entry.set(RESULT_COL, entry.get(ACTUAL_COL));
            }
        }
        return 0.0;
    }

    private boolean exceedsALimit(Double targetError, Double valueError) {
        Double percentLimit = percentThresholdCheckbox.isSelected() ? (Double) percentThreshold.getValue() / 100.0
                : Double.MAX_VALUE;
        Double valueLimit = valueThresholdCheckbox.isSelected() ? (Double) valueThreshold.getValue()
                : Double.MAX_VALUE;
        return (targetError > percentLimit) || (valueError > valueLimit);
    }


    // Checkboxes and spinners
    public void stateChanged(ChangeEvent e) {
        rebalanceTable.dataChanged();
    }

    // Accounts dropdown
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            rebalanceTable.setModel(createRebalanceTableModel((String) accountList.getSelectedItem()));
            pack();
        }
    }

    // The table
    public void tableChanged(TableModelEvent e) {
        rebalanceTable.getDataModel().removeTableModelListener(this); // Avoid recursion
        rebalanceTable.dataChanged(); // Force change event on footer table as well (in case enterred directly)
        rebalance(rebalanceTable.getDataVector(), rebalanceTable.getFooterDataVector());
        rebalanceTable.getDataModel().addTableModelListener(this);
    }

    public final void processEvent(AWTEvent evt) {
        if (evt.getID() == WindowEvent.WINDOW_CLOSING) {
            extension.closeRebalanceWindow();
            return;
        }
        super.processEvent(evt);
    }

    void goAway() {
        setVisible(false);
        dispose();
    }
}
