package com.words.gui;

import com.words.gui.stats.StatsPanel;
import com.words.controller.Controller;
import com.words.main.EnglishWords;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class AuxilliaryDialog extends JDialog {
    
    private static final int DIALOG_WIDTH = 1000;
    private static final int DIALOG_HEIGHT = 600;
    
    private static final Font FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 18);
    
    private final ImageIcon icon;
    private final Controller controller;
    private final JTabbedPane tabbedPane;
    
    private final BundlePanel bundlePanel;
    private final FutureWordsPanel futureWordsPanel;
    private final AllWordsPanel allWordsPanel;
    private final ConsolePanel consolePanel;
    private final StatsPanel statsPanel;
    
    AuxilliaryDialog(Controller ctrl, ImageIcon ic) {
        this.controller = ctrl;
        this.icon = ic;
        
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        setPreferredSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
        setMinimumSize(new Dimension(800, DIALOG_HEIGHT / 2));
        setTitle(EnglishWords.TITLE);
        setModal(false);
        setAlwaysOnTop(true);
        
        if (icon != null) setIconImage(icon.getImage());
        
        JPanel rootPanel = new JPanel(new BorderLayout());
        add(rootPanel);
        
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(FONT);
        add(tabbedPane, BorderLayout.CENTER);
        
        bundlePanel = new BundlePanel(controller, icon, FONT);
        bundlePanel.setBundle(controller.getLastBundleName());
        
        futureWordsPanel = new FutureWordsPanel(controller, icon, FONT);
        
        allWordsPanel = new AllWordsPanel(controller, FONT, icon);
        
        consolePanel = new ConsolePanel(controller);
        
        statsPanel = new StatsPanel(controller, FONT.deriveFont(18f));
        
        registerHotkeys();
        
        // add tabs
        tabbedPane.add("Words", allWordsPanel);
        tabbedPane.add("Future Words", futureWordsPanel);
        tabbedPane.add("Bundles", bundlePanel);
        tabbedPane.add("Console", consolePanel);
        tabbedPane.add("Statistics", statsPanel);
        
        tabbedPane.setSelectedComponent(bundlePanel);
        
        tabbedPane.addChangeListener((ChangeEvent changeEvent) -> {
            int index = tabbedPane.getSelectedIndex();
            String tabTitle = tabbedPane.getTitleAt(index);
            
            switch (tabTitle) {
            case "Statistics":
                statsPanel.showPanel();
                break;
            case "Words":
                allWordsPanel.showPanel();
                break;
            case "Future Words":
                futureWordsPanel.showPanel();
                break;
            default:
                break;
            }
        });
        
        pack();
        setLocationRelativeTo(null);
    }
    
    // register hotkeys
    private void registerHotkeys() {
        InputMap inputMap = getRootPane().getInputMap(
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        actionMap.put("close", new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                AuxilliaryDialog.this.setVisible(false);
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(
            KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK), "openBundle");
        inputMap.put(KeyStroke.getKeyStroke(
            KeyEvent.getExtendedKeyCodeForChar('Щ'), KeyEvent.CTRL_DOWN_MASK),
            "openBundle");
        actionMap.put("openBundle", new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                tabbedPane.setSelectedComponent(bundlePanel);
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(
            KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "showFutureWords");
        inputMap.put(KeyStroke.getKeyStroke(
            KeyEvent.getExtendedKeyCodeForChar('А'), KeyEvent.CTRL_DOWN_MASK),
            "showFutureWords");
        actionMap.put("showFutureWords", new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                tabbedPane.setSelectedComponent(futureWordsPanel);
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(
            KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK), "showAllWords");
        inputMap.put(KeyStroke.getKeyStroke(
            KeyEvent.getExtendedKeyCodeForChar('Ф'), KeyEvent.CTRL_DOWN_MASK),
            "showAllWords");
        actionMap.put("showAllWords", new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                tabbedPane.setSelectedComponent(allWordsPanel);
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(
            KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK), "showConsole");
        inputMap.put(KeyStroke.getKeyStroke(
            KeyEvent.getExtendedKeyCodeForChar('с'), KeyEvent.CTRL_DOWN_MASK),
            "showConsole");
        actionMap.put("showConsole", new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                tabbedPane.setSelectedComponent(consolePanel);
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(
            KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "showStats");
        inputMap.put(KeyStroke.getKeyStroke(
            KeyEvent.getExtendedKeyCodeForChar('ы'), KeyEvent.CTRL_DOWN_MASK),
            "showStats");
        actionMap.put("showStats", new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                tabbedPane.setSelectedComponent(statsPanel);
            }
        });
        
        // ctrl-tab, delete tab event
        tabbedPane.setFocusTraversalKeys(
            KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
            Collections.singleton(KeyStroke.getKeyStroke("TAB")));
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,
            KeyEvent.CTRL_DOWN_MASK), "ctrl-tab");
        actionMap.put("ctrl-tab", new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                tabbedPane.setSelectedIndex(
                    (tabbedPane.getSelectedIndex() + 1) % tabbedPane.getComponentCount());
            }
        });
    }
    
    /**
     * Display word bundle and then select the word.
     * @param bundle bundle to show
     * @param word word to select. Can be null which means no selection.
     * @throws NullPointerException if bundle is null
     */
    public void showBundle(LocalDate bundle, String word) {
        Objects.requireNonNull(bundle);
        
        bundlePanel.setBundle(bundle);
        bundlePanel.setSelectedWord(word);
        tabbedPane.setSelectedComponent(bundlePanel);
        
        setVisible(true);
    }
    
    /**
     * Display FutureWords panel.
     */
    public void showFutureWords() {
        tabbedPane.setSelectedComponent(futureWordsPanel);
        futureWordsPanel.focusTextField();
        setVisible(true);
    }
    
    /**
     * Display AllWords panel.
     */
    public void showAllWords() {
        tabbedPane.setSelectedComponent(allWordsPanel);
        allWordsPanel.focusTextField();
        setVisible(true);
    }
    
    /**
     * Update gui.
     */
    public void fireUpdate() {
        allWordsPanel.fireUpdate();
        bundlePanel.fireUpdate();
    }
    
    public void showConsole() {
        tabbedPane.setSelectedComponent(consolePanel);
        setVisible(true);
    }
    
    public void showStatistics() {
        tabbedPane.setSelectedComponent(statsPanel);
        setVisible(true);
    }
    
    /**
     * Bridge method to all words panel.
     * @param editAction action
     */
    public void setEditAction(BiConsumer<String, JComponent> editAction) {
        allWordsPanel.setEditAction(editAction);
    }
}
