/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pdctouch;

/**
 *
 * @author nathan
 */
public class PDCTouch {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                PDCTouchFrame pdcFrame = new PDCTouchFrame();
                pdcFrame.showMainPanel();
                pdcFrame.setVisible(true);
            }
        });
    }
    
}
