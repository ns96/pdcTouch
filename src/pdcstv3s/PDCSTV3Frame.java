/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pdcstv3s;

import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import mim.MiMTalk;

/**
 *
 * @author nathan
 */
public class PDCSTV3Frame extends javax.swing.JFrame {

    private MiMTalk miMTalk;
    private boolean connected = false;

    // variables used to navigate screen
    private int row = 1, srow1 = 1, srow2 = 1;

    // is the deviced paused
    private boolean paused = true;

    // keep tract of of to keep update the speed ext
    private boolean updateUI = true;
    private boolean showPause = false;

    // set the speed
    private int measuredRPM = 0;
    private int setRPM = 0; // set RPM 
    private int psetRPM = -1; // previous set rpm
    private int runTime = 0;

    // variables used to calcuated the average rpm
    private long totalRPM = 0;
    private int loopCount = 1;

    // digital control
    private int[] inc = {50, 100, 500};
    private int incIdx = 1;

    // ramp mode parameters
    private String minText = "MIN";
    private String maxText = "MAX";
    private int rampIdx = 2;
    private int rampStep = 0;
    private int stepTime = 0;
    private int setTime = 0;
    private int[] ramp;
    private int[] ramp1 = {500, 15, 3100, 30};
    private int[] ramp2 = {600, 25, 3500, 30};
    private int[] ramp3 = {700, 20, 2600, 40};

    private int[] speed = {6000, 8000};
    private int speedIdx = 0;

    private String[] start = {"NONE", "ANALOG", "DIGITAL", "RAMP", "DCOAT"};
    private int startIdx = 1;

    private String[] bl = {"ON", "OFF"};
    private int blIdx = 0;

    private String[] model = {"SCK300", "SCK300P"};
    int modelIdx = 0;
    int calibratePWM = 0;
    int calibrateRPM = 0;

    // define some menus
    private enum Menu {
        MAIN,
        ANALOG,
        DIGITAL,
        RAMP,
        DIPCOAT,
        SETUP,
        SETUP_BLDC,
        SETUP_STEPPER
    }
    private Menu currentMenu = Menu.MAIN;
    private Menu backMenu = null;

    /**
     * Creates new form PDCSTV3Frame
     */
    public PDCSTV3Frame() {
        initComponents();

        // display the main
        displayMenu(0);
    }

    private void displayMenu(int change) {
        if (currentMenu == Menu.MAIN) {
            backMenu = null;

            row -= change;
            if (row > 5) {
                row = 5;
            }
            if (row < 1) {
                row = 1;
            }

            displayMainMenu(change);
        } else if (currentMenu == Menu.ANALOG) {
            backMenu = Menu.MAIN;
            displayAnalogMenu(change);
        } else if (currentMenu == Menu.DIGITAL) {
            backMenu = Menu.MAIN;
            displayDigitalMenu(change);
        } else if (currentMenu == Menu.RAMP) {
            backMenu = Menu.MAIN;
            displayRampMenu(change);
        } else if (currentMenu == Menu.DIPCOAT) {
            backMenu = Menu.MAIN;
            displayDipCoaterMenu(change);
        } else if (currentMenu == Menu.SETUP) {
            backMenu = Menu.MAIN;
            displaySetupMenu(change);
        } else if (currentMenu == Menu.SETUP_BLDC) {
            backMenu = Menu.SETUP;
            displayBLDCMenu(change);
        } else if (currentMenu == Menu.SETUP_STEPPER) {
            backMenu = Menu.SETUP;
            displayStepperMenu(change);
        }
    }

    private void displayMainMenu(int change) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT MODE\n");

        sb.append(((row == 1) ? "  *ANALOG\n" : "   ANALOG\n"));
        sb.append(((row == 2) ? "  *DIGITAL\n" : "   DIGITAL\n"));
        sb.append(((row == 3) ? "  *RAMP\n" : "   RAMP\n"));
        sb.append(((row == 4) ? "  *DIP COATER\n" : "   DIP COATER\n"));
        sb.append(((row == 5) ? "  *SETUP" : "   SETUP"));

        screenTextArea.setText(sb.toString());
        highlightText("DIP COATER", 0);
    }

    private void displayAnalogMenu(int change) {
        // read the speed value
        double value = (float) jSlider1.getValue();
        setRPM = (int) ((value / 100.0) * speed[speedIdx]);

        StringBuilder sb = new StringBuilder();
        sb.append("ANALOG CONTROL\n\n");
        sb.append("MRPM " + getMeasuredRPM() + "\n");
        sb.append("SRPM " + setRPM + "\n");
        sb.append("TIME " + getPaddedNumber(runTime / 2, 4) + " s");

        screenTextArea.setText(sb.toString());
    }

    private void displayDigitalMenu(int change) {
        setRPM += change * inc[incIdx];
        if (setRPM < 0) {
            setRPM = 0;
        }
        if (setRPM > speed[speedIdx]) {
            setRPM = speed[speedIdx];
        }

        StringBuilder sb = new StringBuilder();
        sb.append("DIGITAL CONTROL\n");
        sb.append("MRPM " + getMeasuredRPM() + "\n");
        sb.append("SRPM " + setRPM + "\n");
        sb.append("TIME " + getPaddedNumber(runTime / 2, 4) + " s\n");
        sb.append("INC: 50 100 500\n");

        screenTextArea.setText(sb.toString());
        highlightText("" + inc[incIdx], 46);
    }

    private void displayRampMenu(int change) {
        if (paused) {
            //check to see we need to stop motor
            if(stepTime > 0) {
                stopMotor();
            }
            
            rampIdx += change;
            if (rampIdx < 1) {
                rampIdx = 1;
            }
            if (rampIdx > 3) {
                rampIdx = 3;
            }

            if (rampIdx == 1) {
                ramp = ramp1;
            } else if (rampIdx == 2) {
                ramp = ramp2;
            } else {
                ramp = ramp3;
            }
        } else {
            int time = runTime/2;
            
            if (time == 0) {
                if(rampStep >= 0 && rampStep <= 3) {
                    setRPM = ramp[rampStep];
                    setTime = ramp[++rampStep];
                } else {
                    setRPM = 0;
                    paused = true;
                    stopMotor();
                }
                
                minText = "MIN";
                maxText = "MAX";
            } else {
                stepTime = setTime - time;
                
                if(rampStep == 1) {
                    if(stepTime%2 == 0) {
                        minText = "MIN";
                    } else {
                        minText =  getPaddedNumber(stepTime, 3);
                    }
                } else {
                    if(time%2 == 0) {
                        maxText = "MAX";
                    } else {
                        maxText = getPaddedNumber(stepTime, 3);
                    }
                }
                
                // reach end timme got this step so just move to next step
                if(stepTime == 0) {
                    runTime = 0;
                    rampStep++;
                }
            }
        }

        // display menu now
        StringBuilder sb = new StringBuilder();

        sb.append("RAMP:  1  2  3 \n\n");
        sb.append(minText + " " + getPaddedNumber(ramp[0], 4) + " " + ramp[1] + " s\n");
        sb.append(maxText + " " + getPaddedNumber(ramp[2], 4) + " " + ramp[3] + " s\n");
        sb.append("RPM " + getMeasuredRPM());

        screenTextArea.setText(sb.toString());
        highlightText(" " + rampIdx + " ", 0);

    }

    private void displayDipCoaterMenu(int change) {
        StringBuilder sb = new StringBuilder();
        sb.append("DIP COATER\n");

        screenTextArea.setText(sb.toString());
    }

    private void displaySetupMenu(int change) {
        if (paused) {
            srow1 -= change;
            if (srow1 > 5) {
                srow1 = 5;
            }
            if (srow1 < 1) {
                srow1 = 1;
            }
        }

        StringBuilder sb = new StringBuilder();

        sb.append("SETUP\n");
        sb.append(((srow1 == 1) ? "  *" : "   ")).append("START " + start[startIdx] + "\n");
        sb.append(((srow1 == 2) ? "  *" : "   ")).append("BL " + bl[blIdx] + "\n");
        sb.append(((srow1 == 3) ? "  *" : "   ")).append("RAMP\n");
        sb.append(((srow1 == 4) ? "  *" : "   ")).append("BLDC\n");
        sb.append(((srow1 == 5) ? "  *" : "   ")).append("STEPPER");

        screenTextArea.setText(sb.toString());
    }

    /**
     * Menu for configuring the BLDC motor
     *
     * @param change
     */
    private void displayBLDCMenu(int change) {
        if (paused) {
            srow2 -= change;
            if (srow2 > 2) {
                srow2 = 2;
            }
            if (srow2 < 1) {
                srow2 = 1;
            }
        }

        if (srow2 == 1) {
            if (change == 1) {
                modelIdx = 1;
            } else {
                modelIdx = 0;
            }
        }

        StringBuilder sb = new StringBuilder();

        sb.append("BLDC SETUP\n");
        sb.append(((srow2 == 1) ? "  *" : "   ")).append("MODEL " + model[modelIdx] + "\n");
        sb.append("   SPEED " + speed[modelIdx] + "\n\n");
        sb.append(((srow2 == 2) ? "  *" : "   ")).append("CALIBRATE\n");
        sb.append("   PWM: " + calibratePWM + " / RPM: " + calibrateRPM);

        screenTextArea.setText(sb.toString());

        if (!paused) {
            if (srow2 == 1) {
                highlightText(model[modelIdx], 0);
            } else if (srow2 == 2) {
                highlightText("CALIBRATE", 0);
            }
        }
    }

    private void displayStepperMenu(int change) {
        StringBuilder sb = new StringBuilder();
        sb.append("STEPPER SETUP\n");

        screenTextArea.setText(sb.toString());
    }

    /**
     * Method to highlight a particular ext on the simulated screen
     *
     * @param text
     */
    private void highlightText(String word, int fromIndex) {
        try {
            Highlighter highlighter = screenTextArea.getHighlighter();
            int p0 = screenTextArea.getText().indexOf(word, fromIndex);
            int p1 = p0 + word.length();
            highlighter.addHighlight(p0, p1,
                    new DefaultHighlighter.DefaultHighlightPainter(Color.LIGHT_GRAY));
        } catch (BadLocationException ex) {
            Logger.getLogger(PDCSTV3Frame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        powerButton = new javax.swing.JButton();
        jSlider1 = new javax.swing.JSlider();
        connectToggleButton = new javax.swing.JToggleButton();
        commTextField = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        screenTextArea = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        consoleTextArea = new javax.swing.JTextArea();
        clearButton = new javax.swing.JButton();
        entButton = new javax.swing.JButton();
        upButton = new javax.swing.JButton();
        downButton = new javax.swing.JButton();
        extButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("PDC STV3S Simulator v1.0");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        powerButton.setText("Power");
        powerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                powerButtonActionPerformed(evt);
            }
        });

        jSlider1.setMajorTickSpacing(10);
        jSlider1.setOrientation(javax.swing.JSlider.VERTICAL);
        jSlider1.setPaintLabels(true);
        jSlider1.setPaintTicks(true);
        jSlider1.setValue(0);

        connectToggleButton.setText("Connect");
        connectToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectToggleButtonActionPerformed(evt);
            }
        });

        commTextField.setColumns(5);
        commTextField.setText("COM8");

        screenTextArea.setEditable(false);
        screenTextArea.setBackground(new java.awt.Color(204, 255, 153));
        screenTextArea.setColumns(20);
        screenTextArea.setFont(new java.awt.Font("Monospaced", 1, 24)); // NOI18N
        screenTextArea.setRows(5);
        jScrollPane1.setViewportView(screenTextArea);

        consoleTextArea.setEditable(false);
        consoleTextArea.setColumns(20);
        consoleTextArea.setRows(5);
        consoleTextArea.setText("Console");
        jScrollPane2.setViewportView(consoleTextArea);

        clearButton.setText("Clear");

        entButton.setText("ENT");
        entButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                entButtonActionPerformed(evt);
            }
        });

        upButton.setText("UP");
        upButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upButtonActionPerformed(evt);
            }
        });

        downButton.setText("DOWN");
        downButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downButtonActionPerformed(evt);
            }
        });

        extButton.setText("EXT");
        extButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                extButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(entButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(upButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(downButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(extButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 19, Short.MAX_VALUE)
                        .addComponent(connectToggleButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(commTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearButton))
                    .addComponent(jScrollPane1)
                    .addComponent(jScrollPane2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(powerButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSlider1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSlider1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 213, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 128, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(powerButton)
                    .addComponent(connectToggleButton)
                    .addComponent(commTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(clearButton)
                    .addComponent(entButton)
                    .addComponent(upButton)
                    .addComponent(downButton)
                    .addComponent(extButton)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Connect to the MiM board
     *
     * @param evt
     */
    private void connectToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectToggleButtonActionPerformed
        if (connectToggleButton.isSelected()) {
            miMTalk = new MiMTalk();
            miMTalk.connect(commTextField.getText());
            String response = miMTalk.getVersion();

            if (response.contains("MIM")) {
                connected = true;
                consoleTextArea.setText("Connected to MIM\n");
                miMTalk.sendCommand("BLDCon");
            }
        } else {
            connected = false;
            miMTalk.sendCommand("BLDCoff");
            miMTalk.close();
        }
    }//GEN-LAST:event_connectToggleButtonActionPerformed

    private void upButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upButtonActionPerformed
        displayMenu(1);
    }//GEN-LAST:event_upButtonActionPerformed

    private void downButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downButtonActionPerformed
        displayMenu(-1);
    }//GEN-LAST:event_downButtonActionPerformed

    private void entButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_entButtonActionPerformed
        if (currentMenu == Menu.MAIN) {
            switch (row) {
                case 1:
                    currentMenu = Menu.ANALOG;
                    break;
                case 2:
                    currentMenu = Menu.DIGITAL;
                    break;
                case 3:
                    currentMenu = Menu.RAMP;
                    break;
                case 4:
                    currentMenu = Menu.DIPCOAT;
                    break;
                case 5:
                    currentMenu = Menu.SETUP;
                    break;
                default:
                    currentMenu = Menu.MAIN;
                    break;
            }

            setRPM = 0;
            displayMenu(0);
            startMenuUpdate();
        } else if (currentMenu == Menu.ANALOG) {
            paused = false;
        } else if (currentMenu == Menu.DIGITAL) {
            if (paused) {
                paused = false;
            } else {
                // increment the inx index
                incIdx++;
                if (incIdx == inc.length) {
                    incIdx = 0;
                }
            }
        } else if (currentMenu == Menu.RAMP) {
            rampStep = 0;
            paused = false;
        } else if (currentMenu == Menu.SETUP) {
            switch (srow1) {
                case 4:
                    currentMenu = Menu.SETUP_BLDC;
                    break;
                case 5:
                    currentMenu = Menu.SETUP_STEPPER;
                    break;
                default:
                    break;
            }

            displayMenu(0);
        } else if (currentMenu == Menu.SETUP_BLDC) {
            paused = false;
            displayBLDCMenu(0);
        }
    }//GEN-LAST:event_entButtonActionPerformed

    private void extButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_extButtonActionPerformed
        if (backMenu != null && paused) {
            currentMenu = backMenu;
            updateUI = false;
            displayMenu(0);
        } else {
            paused = true;
            stopMotor();
            displayMenu(0);
        }
    }//GEN-LAST:event_extButtonActionPerformed

    private void powerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_powerButtonActionPerformed
        if (connected) {
            miMTalk.sendCommand("BLDCoff");
            miMTalk.close();
        }

        System.exit(0);
    }//GEN-LAST:event_powerButtonActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        powerButtonActionPerformed(null);
    }//GEN-LAST:event_formWindowClosed

    private void startMenuUpdate() {
        updateUI = true;

        Thread thread = new Thread() {
            public void run() {
                while (updateUI) {
                    displayMenu(0);

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(PDCSTV3Frame.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    if (!paused) {
                        runTime++;
                        moveMotor();
                    }

                    // flip this to cycle between pause and not paused
                    showPause = !showPause;
                }
            }
        };

        thread.start();
    }

    private void moveMotor() {
        if (setRPM != psetRPM) {
            psetRPM = setRPM;
            loopCount = 1;
            totalRPM = 0;
            if (connected) {
                miMTalk.sendCommand("SetRPM," + setRPM);
                System.out.println("Running motor @ " + setRPM);
            }
        } else {
            loopCount++;
        }
    }

    private void stopMotor() {
        psetRPM = -1;
        runTime = 0;
        if (connected) {
            miMTalk.sendCommand("SetRPM," + 0);
        }
        System.out.println("Stoping motor");
    }

    private String getMeasuredRPM() {
        if (paused) {
            if (showPause) {
                return "PAUSE";
            } else {
                return "0";
            }
        } else {
            if (connected) {
                try {
                    String rpm = miMTalk.sendCommand("GetRPM");
                    measuredRPM = Integer.parseInt(miMTalk.getResponseValue(rpm));

                    // get the average rpm after 2 seconds
                    if (loopCount > 4) {
                        totalRPM += measuredRPM;
                        int averageRPM = (int) (totalRPM / (loopCount - 4));
                        consoleTextArea.setText("Average RPM: " + averageRPM);
                    }
                } catch (StringIndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }

            return "" + measuredRPM;
        }
    }

    private String getPaddedNumber(int number, int pad) {
        return String.format("%0" + pad + "d", number);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(PDCSTV3Frame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(PDCSTV3Frame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(PDCSTV3Frame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(PDCSTV3Frame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PDCSTV3Frame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton clearButton;
    private javax.swing.JTextField commTextField;
    private javax.swing.JToggleButton connectToggleButton;
    private javax.swing.JTextArea consoleTextArea;
    private javax.swing.JButton downButton;
    private javax.swing.JButton entButton;
    private javax.swing.JButton extButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSlider jSlider1;
    private javax.swing.JButton powerButton;
    private javax.swing.JTextArea screenTextArea;
    private javax.swing.JButton upButton;
    // End of variables declaration//GEN-END:variables
}
