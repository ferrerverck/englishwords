package com.words.gui.tooltip;

import com.words.controller.Controller;
import com.words.controller.preferences.TooltipPreferences;
import com.words.gui.MainFrame;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import javax.swing.JDialog;
import javax.swing.JRadioButtonMenuItem;

/**
 * Transparent tooltip appearing on mouse enter.
 */
public class WordTooltip extends JDialog {
    
    private static final int TOOLTIP_HEIGHT = 96;
    private static final int DY = 8; // dy between main frame and tooltip
    
    private static final long HIDDEN_DELAY = 5000L;
    
    private final TooltipPanel rootPanel;
    
    private int posX, posY;
    
    private final MainFrame parent;
    private final Controller controller;
    
    // timestamp of last hide, used to prevent showing during first
    // HIDDEN_DELAY millis
    private long lastHidden = 0L;
    
    // Menu radio buttons to check from main menu
    private EnumMap<TooltipPreferences, JRadioButtonMenuItem> buttonMap;
    
    public WordTooltip(MainFrame parent, Controller ctrl) {
        this.controller = ctrl;
        this.parent = parent;
        
        setPreferredSize(new Dimension(parent.getWidth(), TOOLTIP_HEIGHT));
        setAlwaysOnTop(true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setFocusableWindowState(false);
        setFocusable(false);
        
        rootPanel = new RoundCornersPanel(TOOLTIP_HEIGHT);
        add(rootPanel);
        
        // hide on double click
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1
                    && e.getClickCount() % 2 == 0) {
                    controller.setTooltipPreferences(TooltipPreferences.STANDARD);
                    WordTooltip.this.setVisible(false);
                    
                    // check required button in main gui
                    if (buttonMap != null)
                        buttonMap.get(TooltipPreferences.STANDARD)
                            .setSelected(true);
                }
            }
        });
        
        pack();
    }
    
    public void updateTooltip() {
        //updateTooltipLocation();
        int width = parent.getSize().width;
        setSize(width, TOOLTIP_HEIGHT);
        rootPanel.setWord(width, controller.getCurrentWord());
    }

    public void updateTooltipLocation() {
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        posX = parent.getLocationOnScreen().x;
        
        posY = parent.getLocationOnScreen().y + parent.getHeight() + DY;
        if (posY + TOOLTIP_HEIGHT > screenHeight)
            posY = parent.getLocationOnScreen().y - DY - TOOLTIP_HEIGHT;
        
        setLocation(posX, posY);
    }
    
    /**
     * Show tooltip depending on preferences.
     */
    public void showTooltip() {
        if (!controller.hasCurrentWord()) return;
        
        if ((System.currentTimeMillis() - lastHidden) < HIDDEN_DELAY) return;
        
        if (controller.getTooltipPreferences() != TooltipPreferences.NEVER &&
            !isVisible()) {
            setLocation(posX, posY);
            setVisible(true);
        }
        
        repaint();
    }
    
    /**
     * Hide tooltip depending on preferences.
     * @param autoHide specifies if action was automatic so prevent 
     *                 tooltip showing for some time
     */
    public void hideTooltip() {
        if (controller.getTooltipPreferences() != TooltipPreferences.ALWAYS /*&& isVisible()*/) {
//            if (autoHide) lastHidden = System.currentTimeMillis();
            setVisible(false);
        }
    }
    
    public void setTimestamp(boolean currentWordReplaced) {
        if (currentWordReplaced) lastHidden = System.currentTimeMillis();
    }
    
    /**
     * Register radio buttons to get checked when tooltip will become hidden.
     * @param tooltipItemMap radio buttons
     */
    public void setMenuItemMap(
        EnumMap<TooltipPreferences, JRadioButtonMenuItem> tooltipItemMap) {
        this.buttonMap = tooltipItemMap;
    }
}
