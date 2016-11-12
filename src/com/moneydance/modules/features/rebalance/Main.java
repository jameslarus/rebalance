// Main.java
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

import com.moneydance.apps.md.controller.FeatureModule;
import com.moneydance.apps.md.controller.FeatureModuleContext;

import java.io.*;
import java.awt.*;

// Plugable module used to rebalance an Account

public class Main extends FeatureModule {
    private ReBalanceWindow rebalanceWindow;

    @Override
    public void init() {
        // The first thing we will do is register this module to be invoked
        // via the application toolbar
        FeatureModuleContext context = this.getContext();
        try {
            context.registerFeature(this, "rebalance", this.getIcon("accountlist"), this.getName());
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    @Override
    public void cleanup() {
        this.closeRebalanceWindow();
    }

    private Image getIcon(String action) {
        try {
            ClassLoader cl = this.getClass().getClassLoader();
            InputStream in =
                    cl.getResourceAsStream("/com/moneydance/modules/features/rebalance/icon.gif");
            if (in != null) {
                ByteArrayOutputStream bout = new ByteArrayOutputStream(1000);
                byte buf[] = new byte[256];
                int n;
                while ((n = in.read(buf, 0, buf.length)) >= 0) {
                    bout.write(buf, 0, n);
                }
                return Toolkit.getDefaultToolkit().createImage(bout.toByteArray());
            }
        } catch (Throwable e) {
        }
        return null;
    }

     // Process an invocation of this module with the given URI
    @Override
    public void invoke(String uri) {
        String command = uri;
        //String parameters = "";
        int theIdx = uri.indexOf('?');
        if (theIdx >= 0) {
            command = uri.substring(0, theIdx);
            //parameters = uri.substring(theIdx + 1);
        } else {
            theIdx = uri.indexOf(':');
            if (theIdx >= 0) {
                command = uri.substring(0, theIdx);
            }
        }

        if (command.equals("rebalance")) {
            this.rebalance();
        }
    }

    @Override
    public String getName() {
        return "ReBalance";
    }

    private synchronized void rebalance() {
        if (this.rebalanceWindow == null) {
            this.rebalanceWindow = new ReBalanceWindow(this);
            this.rebalanceWindow.setVisible(true);
        } else {
            this.rebalanceWindow.setVisible(true);
            this.rebalanceWindow.toFront();
            this.rebalanceWindow.requestFocus();
        }
    }

    FeatureModuleContext getUnprotectedContext() {
        return this.getContext();
    }

    synchronized void closeRebalanceWindow() {
        if (this.rebalanceWindow != null) {
            this.rebalanceWindow.goAway();
            this.rebalanceWindow = null;
        }
    }
}


