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
        book = extension.getUnprotectedContext().getCurrentAccountBook();
        up = UserPreferences.getInstance();

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

        accountList = new JComboBox<>(getInvestmentAccounts());
        accountList.addItemListener(this);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        pane.add(accountList, c);

        JPanel ptPanel = new JPanel();
        percentThresholdCheckbox = new JCheckBox("", false);
        ptPanel.add(percentThresholdCheckbox);
        SpinnerModel percentModel = new SpinnerNumberModel(percentThresholdDefault, 0.0, 100.0, 0.1);
        percentThreshold = new JSpinner(percentModel);
        ptPanel.add(percentThreshold);
        c.gridx = 1;
        c.gridy = 1;
        pane.add(ptPanel, c);

        JPanel amtPanel = new JPanel();
        valueThresholdCheckbox = new JCheckBox("", false);
        amtPanel.add(valueThresholdCheckbox);
        SpinnerModel valueModel = new SpinnerNumberModel(valueThresholdDefault, 0, 100000, 100);
        valueThreshold = new JSpinner(valueModel);
        amtPanel.add(valueThreshold);
        c.gridx = 2;
        c.gridy = 1;
        pane.add(amtPanel, c);

        // Set preferences before adding listeners to avoid running them before table is created
        restoreThresholdPreferences();
        percentThresholdCheckbox.addChangeListener(this);
        percentThreshold.addChangeListener(this);
        valueThresholdCheckbox.addChangeListener(this);
        valueThreshold.addChangeListener(this);

        // Row 3
        PairedTableModel tableModel = createRebalanceTableModel((String) accountList.getSelectedItem());
        rebalanceTable = new PairedTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (column == TARGET_COL) {
                    c.setBackground(LightGoldenRodYellow);
                }
                if (column == BUY_COL) {
                    JComponent jc = (JComponent) c;
                    jc.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, Color.BLACK));
                }
                return c;
            }
        };
        c.gridx = 0;
        c.gridwidth = 3;
        c.gridy = 2;
        pane.add(new PairedTablePanel(rebalanceTable), c);
        tableModel.addTableModelListener(this);

        // Row 4
        JButton plusButton = new JButton("+");
        plusButton.addActionListener(e -> createNewSecurity());
        JButton minusButton = new JButton("-");
        minusButton.addActionListener(e -> deleteSecurity());
        JPanel panel = new JPanel();
        JButton copyTargetButton = new JButton("Copy Targets");
        copyTargetButton.addActionListener(e -> copyTargets());

        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(plusButton);
        panel.add(minusButton);
        c.gridx = 0;
        c.gridwidth = 1;
        c.gridy = 3;
        pane.add(panel, c);
        c.gridx = 2;
        c.gridy = 3;
        pane.add(copyTargetButton, c);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        enableEvents(WindowEvent.WINDOW_CLOSING);

        pack();
        AwtUtil.centerWindow(this);
    }

    private Vector<String> getInvestmentAccounts() {
        Vector<String> accounts = new Vector<>();
        if (book != null) {
            for (Account a : book.getRootAccount().getSubAccounts()) {
                if (a.getAccountType() == AccountType.INVESTMENT && !a.getAccountIsInactive()) {
                    accounts.add(a.getAccountName());
                }
            }
        }
        Collections.sort(accounts);
        return accounts;
    }


    private final String[] names
            = {"Name", "Symbol", "Target", "Actual", "Shares", "Price", "Value", "Buy", "Sell", "Result"};
    private final Vector<String> columnNames = new Vector<>(Arrays.asList(names));
    private final int NAME_COL = columnNames.indexOf("Name");
    private final int SYMBOL_COL = columnNames.indexOf("Symbol");
    private final int TARGET_COL = columnNames.indexOf("Target");
    private final int ACTUAL_COL = columnNames.indexOf("Actual");
    private final int SHARE_COL = columnNames.indexOf("Shares");
    private final int PRICE_COL = columnNames.indexOf("Price");
    private final int VALUE_COL = columnNames.indexOf("Value");
    private final int BUY_COL = columnNames.indexOf("Buy");
    private final int SELL_COL = columnNames.indexOf("Sell");
    private final int RESULT_COL = columnNames.indexOf("Result");

    private final String[] types
            = {"Text", "Text", "Percent", "Percent", "Integer", "Currency2", "Currency2", "Integer", "Integer", "Percent"};
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

        // Securities
        for (Account a : account.getSubAccounts()) {
            if (a.getAccountType() == AccountType.SECURITY
                    && !a.getCurrencyType().getHideInUI()
                    && a.getCurrentBalance() > 0) {
                if (a.getCurrencyType().getCurrencyType() == CurrencyType.Type.SECURITY) {
                    createEntry(data, a, a.getAccountName(), a.getCurrencyType().getTickerSymbol(), totalValue);
                }
            }
        }
        for (Object s : getSecurityPreferences(accountName)) {
            createEntryFromPreferences(data, (String) s);
        }

        // Cash
        if (account.getCurrencyType().getCurrencyType() == CurrencyType.Type.CURRENCY) {
            createEntry(footer, account, "Cash", null, totalValue);
        }

        // Total value
        Vector<Object> entry = new Vector<>(names.length);
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

        rebalance(data, footer, totalValue);
    }

    private void createEntry(Vector<Vector<Object>> entries, Account security, String name, String symbol,
                             Double totalValue) {
        Double shares = security.getBalance() / Math.pow(10.0, security.getCurrencyType().getDecimalPlaces());
        Double price = 1.0 / security.getCurrencyType().getUserRate();
        createEntry(entries, name, symbol,
                getTargetPreferenences(security.getParentAccount().getAccountName(), name),
                shares * price / totalValue, shares, price, shares * price);
    }

    private Vector<Object> createEntry(Vector<Vector<Object>> entries, String name, String symbol, Double target,
                                       Double actual, Double shares, Double price, Double value) {
        Vector<Object> entry = new Vector<>(names.length);
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
        return entry;
    }

    // User can add securities to accounts for calculation purposes. These securities appear only in
    // Preferences, not in MD accounts.
    private Vector<Object> createEntryFromPreferences(Vector<Vector<Object>> data, String securityName) {
        for (int i = 0; i < data.size(); i++) {
            if (securityName.equals(data.get(i).get(NAME_COL))) {
                return data.get(i); // Actual security in MD account
            }
        }
        CurrencyType ct = book.getCurrencies().getCurrencyByName(securityName);
        return createEntry(data, securityName, ct.getTickerSymbol(), 0.0, 0.0, 0.0, 1.0 / ct.getUserRate(), 0.0);
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
        Double cash = (Double) cashEntry.get(VALUE_COL);
        Double availableFunds = cash;
        Double totalTarget = 0.0;

        // Sell excess shares
        for (Vector<Object> entry : data) {
            entry.set(SELL_COL, null);
            entry.set(BUY_COL, null);
            availableFunds += extractExcessValue(entry, totalValue);
            totalTarget += entry.get(TARGET_COL) == null ? 0.0 : (Double) entry.get(TARGET_COL);
        }

        // Spend funds on new shares
        for (Vector<Object> entry : data) {
            availableFunds -= useExcessValue(entry, totalValue, availableFunds);
        }

        // Adjust cash
        cashEntry.set(SELL_COL, null);
        cashEntry.set(BUY_COL, null);
        totalTarget += cashEntry.get(TARGET_COL) == null ? 0.0 : (Double) cashEntry.get(TARGET_COL);
        if (!cash.equals(availableFunds)) {
            if (cash > availableFunds) {
                cashEntry.set(SELL_COL, cash - availableFunds);
            } else {
                cashEntry.set(BUY_COL, availableFunds - cash);
            }
        }
        cashEntry.set(TARGET_COL, null); // No cash target
        cashEntry.set(RESULT_COL, availableFunds / totalValue);

        footer.get(1).set(TARGET_COL, totalTarget);
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
                entry.set(RESULT_COL, oldShares * price / totalValue);
                return sharesToSell * price;
            } else {
                entry.set(RESULT_COL, entry.get(ACTUAL_COL));
            }
        }
        return 0.0;
    }

    private Double useExcessValue(Vector<Object> entry, Double totalValue, Double availableFunds) {
        Double targetError = (Double) entry.get(TARGET_COL) - (Double) entry.get(ACTUAL_COL);
        if (availableFunds > 0.0 && targetError > 0.0) {
            Double valueError = targetError * totalValue;
            Double price = (Double) entry.get(PRICE_COL);
            Double sharesToBuy = Math.floor(valueError / price);
            if (sharesToBuy > 0.0 && exceedsALimit(targetError, valueError)) {
                entry.set(BUY_COL, sharesToBuy);
                Double newShares = (Double) entry.get(SHARE_COL) + sharesToBuy;
                entry.set(RESULT_COL, newShares * price / totalValue);
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
        return targetError > percentLimit || valueError > valueLimit;
    }


    // Create / delete securities
    //
    private void createNewSecurity() {
        Object[] securities = getSecurities().toArray();
        String securityName = (String) JOptionPane.showInputDialog(this, "Choose one", "Add security",
                JOptionPane.INFORMATION_MESSAGE, null, securities, securities[0]);
        CurrencyType ct = book.getCurrencies().getCurrencyByName(securityName);
        createEntry(rebalanceTable.getDataVector(), securityName, ct.getTickerSymbol(),
                0.0, 0.0, 0.0, 1.0 / ct.getUserRate(), 0.0);
        rebalanceTable.dataChanged();
        saveTargetPreferences();
    }

    private Vector<String> getSecurities() {
        Vector<String> securities = new Vector<>();
        if (book != null) {
            for (CurrencyType ct : book.getCurrencies()) {
                if (ct.getCurrencyType() == CurrencyType.Type.SECURITY && !ct.getHideInUI()) {
                    securities.add(ct.getName());
                }
            }
        }
        Collections.sort(securities);
        return securities;
    }

    private void deleteSecurity() {
        int row = rebalanceTable.getSelectedRow();
        if (row != -1) {
            int rowModelIndex = rebalanceTable.convertRowIndexToModel(row);
            rebalanceTable.getDataVector().remove(rowModelIndex);
            rebalanceTable.dataChanged();
            saveTargetPreferences();
        }
    }


    // Copy targets from another account
    //
    private void copyTargets() {
        Vector<Vector<Object>> data = rebalanceTable.getDataVector();
        String accountName = (String) accountList.getSelectedItem();
        Account account = book.getRootAccount().getAccountByName(accountName);
        // Clear existing targets and added securities
        for (int i = data.size() - 1; i > 0; i--) { // because we are removing rows
            Vector<Object> entry = data.get(i);
            entry.set(TARGET_COL, null);
            if (entry.get(SHARE_COL) == null || (Double)entry.get(SHARE_COL) == 0.0) {
                data.remove(i);
            }
        }
        // Copy targets from another account and add securities as needed
        Object[] accounts = getInvestmentAccounts().toArray();
        String copyFromAccountName = (String) JOptionPane.showInputDialog(this, "Choose one", "Copy from Account",
                JOptionPane.INFORMATION_MESSAGE, null, accounts, accounts[0]);
        StreamVector securities = getSecurityPreferences(copyFromAccountName);
        for (Object sn : securities) {
            String securityName = (String) sn;
            findOrAddSecurityAndSetTarget(data, copyFromAccountName, book.getCurrencies().getCurrencyByName(securityName));
        }
        rebalanceTable.dataChanged();
        saveTargetPreferences();
        pack();
    }

    private void findOrAddSecurityAndSetTarget(Vector<Vector<Object>> data, String accountName, CurrencyType security) {
        String securityName = security.getName();
        for (int i = 0; i < data.size(); i++) {
            Vector<Object> entry = data.get(i);
            if (entry.get(NAME_COL).equals(securityName)) {
                entry.set(TARGET_COL, getTargetPreferenences(accountName, securityName));
                return;
            }
        }
        createEntryFromPreferences(data, securityName).set(TARGET_COL, getTargetPreferenences(accountName, securityName));
    }


    // Listeners:
    //
    // Checkboxes and spinners
    @Override
    public void stateChanged(ChangeEvent e) {
        rebalanceTable.dataChanged();
        saveThresholdPreferences();
    }

    // Accounts dropdown
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            rebalanceTable.setModel(createRebalanceTableModel((String) accountList.getSelectedItem()));
            rebalanceTable.getDataModel().addTableModelListener(this);
            pack();
        }
    }

    // The table
    @Override
    public void tableChanged(TableModelEvent e) {
        rebalanceTable.getDataModel().removeTableModelListener(this); // Avoid recursion
        rebalanceTable.dataChanged(); // Force change event on footer table as well (in case entered directly)
        rebalance(rebalanceTable.getDataVector(), rebalanceTable.getFooterDataVector());
        saveTargetPreferences();
        rebalanceTable.getDataModel().addTableModelListener(this);
    }

    @Override
    public void processEvent(AWTEvent evt) {
        if (evt.getID() == WindowEvent.WINDOW_CLOSING) {
            saveThresholdPreferences();
            saveTargetPreferences();
            extension.closeRebalanceWindow();
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
        up.setSetting(RB_PREF + SEP + PT_CHECKBOX, percentThresholdCheckbox.isSelected());
        up.setSetting(RB_PREF + SEP + PT_THRESHOLD, percentThreshold.getValue().toString());
        up.setSetting(RB_PREF + SEP + VT_CHECKBOX, valueThresholdCheckbox.isSelected());
        up.setSetting(RB_PREF + SEP + VT_THRESHOLD, valueThreshold.getValue().toString());
    }

    private void restoreThresholdPreferences() {
        percentThresholdCheckbox.setSelected(up.getBoolSetting(RB_PREF + SEP + PT_CHECKBOX, false));
        String defaultThreshold = Double.toString(percentThresholdDefault);
        percentThreshold.setValue(Double.parseDouble(up.getSetting(RB_PREF + SEP + PT_THRESHOLD, defaultThreshold)));

        valueThresholdCheckbox.setSelected(up.getBoolSetting(RB_PREF + SEP + VT_CHECKBOX, false));
        defaultThreshold = Integer.toString(valueThresholdDefault);
        valueThreshold.setValue(Integer.parseInt(up.getSetting(RB_PREF + SEP + VT_THRESHOLD, defaultThreshold)));
    }

    private void saveTargetPreferences() {
        String accountName = (String) accountList.getSelectedItem();
        StreamVector securities = new StreamVector();
        StreamVector targets = new StreamVector();
        for (int i = 0; i < rebalanceTable.getRowCount(); i++) {
            Vector<Object> entry = rebalanceTable.getDataVector().get(i);
            securities.add(entry.get(NAME_COL));
            targets.add(entry.get(TARGET_COL).toString());
        }
        up.setSetting(RB_PREF + SEP + ACCOUNT + SEP + accountName + SEP + SECURITIES, securities);
        up.setSetting(RB_PREF + SEP + ACCOUNT + SEP + accountName + SEP + TARGETS, targets);
    }

    private Double getTargetPreferenences(String accountName, String securityName) {
        StreamVector securities = new StreamVector();
        StreamVector targets = new StreamVector();
        securities = up.getVectorSetting(RB_PREF + SEP + ACCOUNT + SEP + accountName + SEP + SECURITIES, securities);
        targets = up.getVectorSetting(RB_PREF + SEP + ACCOUNT + SEP + accountName + SEP + TARGETS, targets);

        int index = securities.indexOf(securityName);
        return index == -1 || targets.get(index) == null ? 0.0 : Double.parseDouble((String) targets.get(index));
    }

    private StreamVector getSecurityPreferences(String accountName) {
        StreamVector securities = new StreamVector();
        return up.getVectorSetting(RB_PREF + SEP + ACCOUNT + SEP + accountName + SEP + SECURITIES, securities);

    }

    void goAway() {
        setVisible(false);
        dispose();
    }
}
