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

import com.infinitekind.moneydance.model.Account.AccountType;
import com.infinitekind.util.StreamVector;
import com.moneydance.awt.*;
import com.infinitekind.moneydance.model.*;
import com.moneydance.apps.md.controller.UserPreferences;

import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;


// Window used for the ReBalance interface

class ReBalanceWindow extends JFrame implements ChangeListener, ItemListener, TableModelListener {
    private final Main extension;
    private final AccountBook book;
    private UserPreferences up;

    private final JComboBox<String> accountList;
    private final JCheckBox percentThresholdCheckbox;
    private final JSpinner percentThreshold;
    private final double percentThresholdDefault = 1.0; // 1%
    private final JCheckBox valueThresholdCheckbox;
    private final JSpinner valueThreshold;
    private final int valueThresholdDefault = 1000;
    private final PairedTable rebalanceTable;
    private final Color LightGoldenRodYellow = new Color(0XFAFAD2);

    ReBalanceWindow(Main extension) {
        super("ReBalance Investment Account");
        this.extension = extension;
        this.book = extension.getUnprotectedContext().getCurrentAccountBook();
        this.up = UserPreferences.getInstance();

        JPanel pane = new JPanel(new GridBagLayout());
        setContentPane(pane);
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

        this.accountList = new JComboBox<>(this.getInvestmentAccounts());
        this.accountList.addItemListener(this);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        pane.add(this.accountList, c);

        JPanel ptPanel = new JPanel();
        this.percentThresholdCheckbox = new JCheckBox("", false);
        ptPanel.add(this.percentThresholdCheckbox);
        SpinnerModel percentModel = new SpinnerNumberModel(this.percentThresholdDefault, 0.0, 100.0, 0.1);
        this.percentThreshold = new JSpinner(percentModel);
        ptPanel.add(this.percentThreshold);
        c.gridx = 1;
        c.gridy = 1;
        pane.add(ptPanel, c);

        JPanel amtPanel = new JPanel();
        this.valueThresholdCheckbox = new JCheckBox("", false);
        amtPanel.add(this.valueThresholdCheckbox);
        SpinnerModel valueModel = new SpinnerNumberModel(this.valueThresholdDefault, 0, 100000, 100);
        this.valueThreshold = new JSpinner(valueModel);
        amtPanel.add(this.valueThreshold);
        c.gridx = 2;
        c.gridy = 1;
        pane.add(amtPanel, c);

        // Set preferences before adding listeners to avoid running them before table is created
        this.restoreThresholdPreferences();
        this.percentThresholdCheckbox.addChangeListener(this);
        this.percentThreshold.addChangeListener(this);
        this.valueThresholdCheckbox.addChangeListener(this);
        this.valueThreshold.addChangeListener(this);

        // Row 3
        PairedTableModel tableModel = this.createRebalanceTableModel((String) this.accountList.getSelectedItem());
        this.rebalanceTable = new PairedTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (column == ReBalanceWindow.this.TARGET_COL) {
                    c.setBackground(ReBalanceWindow.this.LightGoldenRodYellow);
                }
                if (column == ReBalanceWindow.this.BUY_COL) {
                    JComponent jc = (JComponent) c;
                    jc.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, Color.BLACK));
                }
                return c;
            }
        };
        c.gridx = 0;
        c.gridwidth = 3;
        c.gridy = 2;
        pane.add(new PairedTablePanel(this.rebalanceTable), c);
        tableModel.addTableModelListener(this);

        // Row 4
        JButton plusButton = new JButton("+");
        plusButton.addActionListener(e -> this.createNewSecurity());
        JButton minusButton = new JButton("-");
        minusButton.addActionListener(e -> this.deleteSecurity());
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(plusButton);
        panel.add(minusButton);
        c.gridx = 0;
        c.gridwidth = 1;
        c.gridy = 3;
        pane.add(panel, c);

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.enableEvents(WindowEvent.WINDOW_CLOSING);

        this.pack();
        AwtUtil.centerWindow(this);
    }

    private Vector<String> getInvestmentAccounts() {
        Vector<String> accounts = new Vector<>();
        if (this.book != null) {
            for (Account a : this.book.getRootAccount().getSubAccounts()) {
                if (a.getAccountType() == AccountType.INVESTMENT && !a.getAccountIsInactive()) {
                    accounts.add(a.getAccountName());
                }
            }
        }
        Collections.sort(accounts);
        return accounts;
    }


    private final String[] names = {"Name", "Symbol", "Target", "Actual", "Shares", "Price", "Value", "Buy", "Sell", "Result"};
    private final Vector<String> columnNames = new Vector<>(Arrays.asList(this.names));
    private final int NAME_COL = this.columnNames.indexOf("Name");
    private final int SYMBOL_COL = this.columnNames.indexOf("Symbol");
    private final int TARGET_COL = this.columnNames.indexOf("Target");
    private final int ACTUAL_COL = this.columnNames.indexOf("Actual");
    private final int SHARE_COL = this.columnNames.indexOf("Shares");
    private final int PRICE_COL = this.columnNames.indexOf("Price");
    private final int VALUE_COL = this.columnNames.indexOf("Value");
    private final int BUY_COL = this.columnNames.indexOf("Buy");
    private final int SELL_COL = this.columnNames.indexOf("Sell");
    private final int RESULT_COL = this.columnNames.indexOf("Result");

    private final String[] types = {"Text", "Text", "Percent", "Percent", "Integer", "Currency2", "Currency2", "Integer", "Integer", "Percent"};
    private final Vector<String> columnTypes = new Vector<>(Arrays.asList(this.types));

    private PairedTableModel createRebalanceTableModel(String accountName) {
        Account account = this.book.getRootAccount().getAccountByName(accountName);
        Vector<Vector<Object>> data = new Vector<>();
        Vector<Vector<Object>> footer = new Vector<>();

        this.fillRebalanceTable(accountName, data, footer);
        return new PairedTableModel(data, footer, ReBalanceWindow.this.columnNames, ReBalanceWindow.this.columnTypes, account.getCurrencyType()) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == ReBalanceWindow.this.TARGET_COL;
            }
        };
    }

    private void fillRebalanceTable(String accountName, Vector<Vector<Object>> data, Vector<Vector<Object>> footer) {
        Account account = this.book.getRootAccount().getAccountByName(accountName);
        Double scale = Math.pow(10.0, account.getCurrencyType().getDecimalPlaces());
        Double totalValue = account.getRecursiveBalance() / scale;

        // Securities
        for (Account a : account.getSubAccounts()) {
            if (a.getAccountType() == AccountType.SECURITY
                    && !a.getCurrencyType().getHideInUI()
                    && a.getCurrentBalance() > 0) {
                if (a.getCurrencyType().getCurrencyType() == CurrencyType.Type.SECURITY) {
                    this.createEntry(data, a, a.getAccountName(), a.getCurrencyType().getTickerSymbol(), totalValue);
                }
            }
        }
        for (Object s : this.getSecurityPreferences(accountName)) {
            this.createEntryFromPreferences(data, (String) s);
        }

        // Cash
        if (account.getCurrencyType().getCurrencyType() == CurrencyType.Type.CURRENCY) {
            this.createEntry(footer, account, "Cash", null, totalValue);
        }

        // Total value
        Vector<Object> entry = new Vector<>(this.names.length);
        entry.add("Total");
        entry.add(null);
        entry.add(null);
        entry.add(null);
        entry.add(null);
        entry.add(null);
        entry.add(totalValue);
        entry.add(null);
        entry.add(null);
        entry.add(null);
        footer.add(entry);

        this.rebalance(data, footer, totalValue);
    }

    private void createEntry(Vector<Vector<Object>> entries, Account security, String name, String symbol,
                             Double totalValue) {
        Double shares = security.getBalance() / Math.pow(10.0, security.getCurrencyType().getDecimalPlaces());
        Double price = 1.0 / security.getCurrencyType().getUserRate();
        this.createEntry(entries, name, symbol,
                this.restoreTargetPreference(security.getParentAccount().getAccountName(), name),
                shares * price / totalValue, shares, price, shares * price);
    }

    private void createEntry(Vector<Vector<Object>> entries, String name, String symbol, Double target, Double actual,
                             Double shares, Double price, Double value) {
        Vector<Object> entry = new Vector<>(this.names.length);
        entry.add(name);
        entry.add(symbol);
        entry.add(target);
        entry.add(actual);
        entry.add(shares);
        entry.add(price);
        entry.add(value);
        entry.add(0.0);   // calc at end
        entry.add(0.0);   // calc at end
        entry.add(0.0);   // calc at end
        entries.add(entry);
    }

    // User can add securities to accounts for calculation purposes. These securities appear only in
    // Preferences, not in MD accounts.
    private void createEntryFromPreferences(Vector<Vector<Object>> data, String securityName) {
        for (int i = 0; i < data.size(); i++) {
            if (securityName.equals(data.get(i).get(this.NAME_COL))) {
                return; // Actual security in MD account
            }
        }
        CurrencyType ct = this.book.getCurrencies().getCurrencyByName(securityName);
        this.createEntry(data, securityName, ct.getTickerSymbol(), 0.0, 0.0, 0.0, 1.0 / ct.getUserRate(), 0.0);
    }

    // This could be an integer-linear programming problem, but something simpler should work.
    // First, find all securities to sell and cash that can be used.
    // Then, compute integer number of shares to buy.
    // Goal: get close to targets, but do not incur unnecessary trading costs (i.e., buy 1 or 2 shares).
    private void rebalance(Vector<Vector<Object>> data, Vector<Vector<Object>> footer) {
        Account account = this.book.getRootAccount().getAccountByName((String) this.accountList.getSelectedItem());
        Double totalValue = account.getRecursiveBalance() / Math.pow(10.0, account.getCurrencyType().getDecimalPlaces());
        this.rebalance(data, footer, totalValue);
    }

    private void rebalance(Vector<Vector<Object>> data, Vector<Vector<Object>> footer, Double totalValue) {
        Vector<Object> cashEntry = footer.get(0);
        Double cash = (Double) cashEntry.get(this.VALUE_COL);
        Double availableFunds = cash;
        Double totalTarget = 0.0;

        // Sell excess shares
        for (Vector<Object> entry : data) {
            entry.set(this.SELL_COL, null);
            entry.set(this.BUY_COL, null);
            availableFunds += this.extractExcessValue(entry, totalValue);
            totalTarget += entry.get(this.TARGET_COL) == null ? 0.0 : (Double) entry.get(this.TARGET_COL);
        }

        // Spend funds on new shares
        for (Vector<Object> entry : data) {
            availableFunds -= this.useExcessValue(entry, totalValue, availableFunds);
        }

        // Adjust cash
        cashEntry.set(this.SELL_COL, null);
        cashEntry.set(this.BUY_COL, null);
        totalTarget += cashEntry.get(this.TARGET_COL) == null ? 0.0 : (Double) cashEntry.get(this.TARGET_COL);
        if (!cash.equals(availableFunds)) {
            if (cash > availableFunds) {
                cashEntry.set(this.SELL_COL, cash - availableFunds);
            } else {
                cashEntry.set(this.BUY_COL, availableFunds - cash);
            }
        }
        cashEntry.set(this.TARGET_COL, null); // No cash target
        cashEntry.set(this.RESULT_COL, availableFunds / totalValue);

        footer.get(1).set(this.TARGET_COL, totalTarget);
    }

    // Find excess value of securities that exceed target by appropriate amount (either % or magnitude).
    // Return amount of sale or purchase
    private Double extractExcessValue(Vector<Object> entry, Double totalValue) {
        Double targetError = (Double) entry.get(this.ACTUAL_COL) - (Double) entry.get(this.TARGET_COL);
        if (targetError > 0.0) {
            Double price = (Double) entry.get(this.PRICE_COL);
            Double valueError = targetError * totalValue;
            Double sharesToSell = Math.floor(valueError / price);
            if (sharesToSell > 0.0 && this.exceedsALimit(targetError, valueError)) {
                entry.set(this.SELL_COL, sharesToSell);
                Double oldShares = (Double) entry.get(this.SHARE_COL) - sharesToSell;
                entry.set(this.RESULT_COL, oldShares * price / totalValue);
                return sharesToSell * price;
            } else {
                entry.set(this.RESULT_COL, entry.get(this.ACTUAL_COL));
            }
        }
        return 0.0;
    }

    private Double useExcessValue(Vector<Object> entry, Double totalValue, Double availableFunds) {
        Double targetError = (Double) entry.get(this.TARGET_COL) - (Double) entry.get(this.ACTUAL_COL);
        if (availableFunds > 0.0 && targetError > 0.0) {
            Double valueError = targetError * totalValue;
            Double price = (Double) entry.get(this.PRICE_COL);
            Double sharesToBuy = Math.floor(valueError / price);
            if (sharesToBuy > 0.0 && this.exceedsALimit(targetError, valueError)) {
                entry.set(this.BUY_COL, sharesToBuy);
                Double newShares = (Double) entry.get(this.SHARE_COL) + sharesToBuy;
                entry.set(this.RESULT_COL, newShares * price / totalValue);
                return sharesToBuy * price;
            } else {
                entry.set(this.RESULT_COL, entry.get(this.ACTUAL_COL));
            }
        }
        return 0.0;
    }

    private boolean exceedsALimit(Double targetError, Double valueError) {
        Double percentLimit = this.percentThresholdCheckbox.isSelected() ? (Double) this.percentThreshold.getValue() / 100.0
                : Double.MAX_VALUE;
        Double valueLimit = this.valueThresholdCheckbox.isSelected() ? (Double) this.valueThreshold.getValue()
                : Double.MAX_VALUE;
        return targetError > percentLimit || valueError > valueLimit;
    }


    // Create / delete securities:
    //
    private void createNewSecurity() {
        Object[] securities = this.getSecurities().toArray();
        String securityName = (String) JOptionPane.showInputDialog(this, "Choose one", "Add security",
                JOptionPane.INFORMATION_MESSAGE, null, securities, securities[0]);
        CurrencyType ct = this.book.getCurrencies().getCurrencyByName(securityName);
        this.createEntry(this.rebalanceTable.getDataVector(), securityName, ct.getTickerSymbol(),
                0.0, 0.0, 0.0, 1.0 / ct.getUserRate(), 0.0);
        this.rebalanceTable.dataChanged();
        this.saveTargetPreferences();
    }

    private Vector<String> getSecurities() {
        Vector<String> securities = new Vector<>();
        if (this.book != null) {
            for (CurrencyType ct : this.book.getCurrencies()) {
                if (ct.getCurrencyType() == CurrencyType.Type.SECURITY && !ct.getHideInUI()) {
                    securities.add(ct.getName());
                }
            }
        }
        Collections.sort(securities);
        return securities;
    }

    private void deleteSecurity() {
        int row = this.rebalanceTable.getSelectedRow();
        if (row != -1) {
            int rowModelIndex = this.rebalanceTable.convertRowIndexToModel(row);
            this.rebalanceTable.getDataVector().remove(rowModelIndex);
            this.rebalanceTable.dataChanged();
            this.saveTargetPreferences();
        }
    }


    // Listeners:
    //
    // Checkboxes and spinners
    @Override
    public void stateChanged(ChangeEvent e) {
        this.rebalanceTable.dataChanged();
        this.saveThresholdPreferences();
    }

    // Accounts dropdown
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            this.rebalanceTable.setModel(this.createRebalanceTableModel((String) this.accountList.getSelectedItem()));
            this.rebalanceTable.getDataModel().addTableModelListener(this);
            this.pack();
        }
    }

    // The table
    @Override
    public void tableChanged(TableModelEvent e) {
        this.rebalanceTable.getDataModel().removeTableModelListener(this); // Avoid recursion
        this.rebalanceTable.dataChanged(); // Force change event on footer table as well (in case entered directly)
        this.rebalance(this.rebalanceTable.getDataVector(), this.rebalanceTable.getFooterDataVector());
        this.saveTargetPreferences();
        this.rebalanceTable.getDataModel().addTableModelListener(this);
    }

    @Override
    public void processEvent(AWTEvent evt) {
        if (evt.getID() == WindowEvent.WINDOW_CLOSING) {
            this.saveThresholdPreferences();
            this.saveTargetPreferences();
            this.extension.closeRebalanceWindow();
            return;
        }
        super.processEvent(evt);
    }


    // Save / restore preferences
    //
    private static final String RB_PREF = "ReBalance";
    private static final String SEP = "%%";
    private static final String PT_CHECKBOX = "PTCheckbox";
    private static final String PT_THRESHOLD = "PTThreshold";
    private static final String VT_CHECKBOX = "VTCheckbox";
    private static final String VT_THRESHOLD = "VTThreshold";
    private static final String ACCOUNT = "Account";
    private static final String SECURITIES = "Securities";
    private static final String TARGETS = "Targets";

    private void saveThresholdPreferences() {
        this.up.setSetting(ReBalanceWindow.RB_PREF + ReBalanceWindow.SEP + ReBalanceWindow.PT_CHECKBOX, this.percentThresholdCheckbox.isSelected());
        this.up.setSetting(ReBalanceWindow.RB_PREF + ReBalanceWindow.SEP + ReBalanceWindow.PT_THRESHOLD, this.percentThreshold.getValue().toString());
        this.up.setSetting(ReBalanceWindow.RB_PREF + ReBalanceWindow.SEP + ReBalanceWindow.VT_CHECKBOX, this.valueThresholdCheckbox.isSelected());
        this.up.setSetting(ReBalanceWindow.RB_PREF + ReBalanceWindow.SEP + ReBalanceWindow.VT_THRESHOLD, this.valueThreshold.getValue().toString());
    }

    private void restoreThresholdPreferences() {
        this.percentThresholdCheckbox.setSelected(this.up.getBoolSetting(ReBalanceWindow.RB_PREF + ReBalanceWindow.SEP + ReBalanceWindow.PT_CHECKBOX, false));
        String defaultThreshold = Double.toString(this.percentThresholdDefault);
        this.percentThreshold.setValue(Double.parseDouble(this.up.getSetting(ReBalanceWindow.RB_PREF + ReBalanceWindow.SEP + ReBalanceWindow.PT_THRESHOLD, defaultThreshold)));

        this.valueThresholdCheckbox.setSelected(this.up.getBoolSetting(ReBalanceWindow.RB_PREF + ReBalanceWindow.SEP + ReBalanceWindow.VT_CHECKBOX, false));
        defaultThreshold = Integer.toString(this.valueThresholdDefault);
        this.valueThreshold.setValue(Integer.parseInt(this.up.getSetting(ReBalanceWindow.RB_PREF + ReBalanceWindow.SEP + ReBalanceWindow.VT_THRESHOLD, defaultThreshold)));
    }

    private void saveTargetPreferences() {
        String account = (String) this.accountList.getSelectedItem();
        StreamVector securities = new StreamVector();
        StreamVector targets = new StreamVector();
        for (int i = 0; i < this.rebalanceTable.getRowCount(); i++) {
            Vector<Object> entry = this.rebalanceTable.getDataVector().get(i);
            securities.add(entry.get(this.NAME_COL));
            targets.add(entry.get(this.TARGET_COL).toString());
        }
        this.up.setSetting(ReBalanceWindow.RB_PREF + ReBalanceWindow.SEP + ReBalanceWindow.ACCOUNT + ReBalanceWindow.SEP + account + ReBalanceWindow.SEP + ReBalanceWindow.SECURITIES, securities);
        this.up.setSetting(ReBalanceWindow.RB_PREF + ReBalanceWindow.SEP + ReBalanceWindow.ACCOUNT + ReBalanceWindow.SEP + account + ReBalanceWindow.SEP + ReBalanceWindow.TARGETS, targets);
    }

    private Double restoreTargetPreference(String accountName, String securityName) {
        StreamVector securities = new StreamVector();
        StreamVector targets = new StreamVector();
        securities = this.up.getVectorSetting(ReBalanceWindow.RB_PREF + ReBalanceWindow.SEP + ReBalanceWindow.ACCOUNT + ReBalanceWindow.SEP + accountName + ReBalanceWindow.SEP + ReBalanceWindow.SECURITIES, securities);
        targets = this.up.getVectorSetting(ReBalanceWindow.RB_PREF + ReBalanceWindow.SEP + ReBalanceWindow.ACCOUNT + ReBalanceWindow.SEP + accountName + ReBalanceWindow.SEP + ReBalanceWindow.TARGETS, targets);

        int index = securities.indexOf(securityName);
        return index == -1 || targets.get(index) == null ? 0.0 : Double.parseDouble((String) targets.get(index));
    }

    private StreamVector getSecurityPreferences(String accountName) {
        StreamVector securities = new StreamVector();
        return this.up.getVectorSetting(ReBalanceWindow.RB_PREF + ReBalanceWindow.SEP + ReBalanceWindow.ACCOUNT + ReBalanceWindow.SEP + accountName + ReBalanceWindow.SEP + ReBalanceWindow.SECURITIES, securities);

    }


    void goAway() {
        this.setVisible(false);
        this.dispose();
    }
}
