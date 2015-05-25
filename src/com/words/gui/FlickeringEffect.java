package com.words.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.Timer;

/**
 * Flickering effect on auto button.
 * Changes icon during small time period.
 * @author vlad
 */
class FlickeringEffect implements ActionListener {
    
    private static final int REPEATS = 12;
    private static final int DELAY = 500;
    
    private final Timer timer;
    private final AbstractButton button;

    private ImageIcon onIcon, offIcon;
    private int counter = 0;
    
    public FlickeringEffect(AbstractButton button) {
        this.timer = new Timer(DELAY, this);
        this.button = button;
        timer.setRepeats(true);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        counter++;
        if (counter % 2 == 0) button.setIcon(onIcon);
        else button.setIcon(offIcon);
        if (counter == REPEATS) timer.stop();
    }
    
    public void start(ImageIcon onIcon, ImageIcon offIcon) {
        stop();
        
        this.onIcon = onIcon;
        this.offIcon = offIcon;
        
        counter = 0;
        
        timer.start();
    }
    
    public void stop() {
        if (timer != null && timer.isRunning()) timer.stop();
    }
}
