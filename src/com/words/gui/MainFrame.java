package com.words.gui;

import com.words.gui.guiutils.WordTypeColors;
import com.words.gui.tooltip.WordTooltip;
import com.words.controller.Controller;
import com.words.controller.preferences.ApplicationLocationException;
import com.words.controller.preferences.SoundPreferences;
import com.words.controller.preferences.AutoMode;
import com.words.controller.preferences.TooltipPreferences;
import com.words.controller.sound.PlayWav;
import com.words.controller.words.Word;
import com.words.controller.words.wordkinds.WordComplexity;
import com.words.controller.words.wordkinds.WordType;
import com.words.controller.words.wordkinds.display.strategy.WordDisplayFactory;
import com.words.gui.guiutils.AutoButtonIcons;
import com.words.gui.guiutils.ComplexityGuiUtils;
import com.words.gui.verification.CharsDialog;
import com.words.gui.dialogs.AddWordDialog;
import com.words.gui.dialogs.EditWordDialog;
import com.words.gui.dialogs.HintDialog;
import com.words.main.EnglishWords;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class MainFrame extends JFrame {
    
    private static final int FRAME_WIDTH = 730;
    private static final int FRAME_HEIGHT = 32;
    
    private static final int TOOLTIP_RATIO = 5;
    
    private static final Font FONT = new Font(Font.DIALOG, Font.PLAIN, 26);
    private static final Font SMALL_FONT = FONT.deriveFont(Font.BOLD, 18f);
    
    private static final Border DEFAULT_BORDER =
        BorderFactory.createLineBorder(Color.BLACK, 1);
    
    private final Controller controller;
    
    private final JPanel rootPanel;
    
    private final JButton mainButton;
    private final JToggleButton prefsButton;
    private final JButton soundButton;
    
    private final JToggleButton autoButton;
    private final FlickeringEffect flickeringEffect;
    
    private final JPopupMenu menu = new JPopupMenu();
    private JMenuItem repeatWordMenuItem;
    private JMenuItem backupMenuItem;
    
    // Window position
    private int posX, posY;
    
    // Application icon, icon to show in JOptionPanes
    private ImageIcon icon;
    
    private AuxilliaryDialog auxDialog;
    private WordTooltip tooltip;
    
    // button group to synchronize autoMode radio buttons
    private final ButtonGroup autoModeGroup = new ButtonGroup();
    
    // hotkeys
    private InputMap inputMap;
    private ActionMap actionMap;
    
    // edit words dialog
    private EditWordDialog ewd;
    private AddWordDialog awd;
    
    // main button possible icons
    private EnumMap<WordType, Icon> mainButtonIcons =
        new EnumMap<>(WordType.class);
    
    private final ComplexityGui complexityGui;
    
    public MainFrame(Controller cntr) {
        this.controller = cntr;
        
        setTitle(EnglishWords.TITLE);
        setAlwaysOnTop(true);
        setMinimumSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        setResizable(false);
        setUndecorated(true);
        
        // set icon
        icon = new ImageIcon(
            getClass().getResource("/resources/icons/icon64.png"));
        setIconImage(icon.getImage());
        
        // tooltip
        tooltip = new WordTooltip(this, controller);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                tooltip.updateTooltipLocation();
            }
        });
        
        // init buttons
        mainButton = new JButton() {
            
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                
                if (controller.getTooltipPreferences() ==
                    TooltipPreferences.EDGES) {
                    // paint serifs for tooltip
                    ((Graphics2D) g).setStroke(new BasicStroke(3));
                    g.setColor(Color.LIGHT_GRAY);
                    
                    int width = getWidth();
                    int len = 5;
                    
                    g.drawLine(width / TOOLTIP_RATIO, 0,
                        width / TOOLTIP_RATIO, len);
                    g.drawLine(width - width / TOOLTIP_RATIO, 0,
                        width - width / TOOLTIP_RATIO, len);
                }
            }
        };
        
        mainButton.setFocusPainted(false);
        mainButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        mainButton.setFont(FONT);
        mainButton.setPreferredSize(new Dimension(
            FRAME_WIDTH - 4 * FRAME_HEIGHT, FRAME_HEIGHT));
        mainButton.setIcon(null);
        mainButton.setIconTextGap(6);
        
        // possible button icons depending on current word type
        mainButtonIcons = new EnumMap<>(WordType.class);
        int width = 12;
        for (WordType wordType : WordType.values()) {
            BufferedImage img = new BufferedImage(width, width,
                BufferedImage.TYPE_INT_ARGB);
            
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2.setColor(WordTypeColors.getColor(wordType));
            g2.fillOval(0, 0, width, width);
            
            mainButtonIcons.put(wordType, new ImageIcon(img));
        }
        
        // standard word without icon
        mainButtonIcons.put(WordType.STANDARD, null);
        
        prefsButton = new JToggleButton();
        prefsButton.setToolTipText("<html><b>Preferences");
        makeSquareButton(prefsButton, "prefs.png");
        
        autoButton = new JToggleButton();
        autoButton.setToolTipText("<html><b>Change auto mode (F1)");
        makeSquareButton(autoButton, null);
        autoButton.setIcon(AutoButtonIcons.getIcon(controller.getAutoMode()));
        flickeringEffect = new FlickeringEffect(autoButton);
        
        soundButton = new JButton();
        soundButton.setToolTipText("<html><b>Pronounce word");
        makeSquareButton(soundButton, "sound.png");
        
        complexityGui = new ComplexityGui(this, cntr);
        
        // set root layout
        rootPanel = new JPanel(new GridBagLayout());
        rootPanel.setBorder(DEFAULT_BORDER);
        add(rootPanel);
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        
        c.weightx = 0;
        rootPanel.add(prefsButton, c);
        
        c.weightx = 0;
        rootPanel.add(complexityGui.getButton(), c);
        
        c.weightx = 1000;
        rootPanel.add(mainButton, c);
        
        c.weightx = 0;
        rootPanel.add(soundButton, c);
        
        c.weightx = 0;
        rootPanel.add(autoButton, c);
        
        mainButton.addActionListener(e -> {
            flickeringEffect.stop();
            autoButton.setIcon(AutoButtonIcons.getIcon(controller.getAutoMode()));
            controller.nextWord();
        });
        
        soundButton.addActionListener(pronounceAction);
        
        prefsButton.addActionListener(e ->
            menu.show(MainFrame.this, 0, MainFrame.this.getHeight()));
        
        // window moving on right click
        mainButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                tooltip.hideTooltip();
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                posX = e.getX() + mainButton.getBounds().x;
                posY = e.getY() + mainButton.getBounds().y;
            }
        });
        
        mainButton.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    setLocation(e.getXOnScreen() - posX,
                        e.getYOnScreen() - posY);
                    tooltip.hideTooltip();
                }
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!menu.isVisible() && !complexityGui.getPopup().isVisible()) {
                    if (controller.getTooltipPreferences() !=
                        TooltipPreferences.EDGES) {
                        tooltip.showTooltip();
                        return;
                    }
                    
                    int width = mainButton.getWidth();
                    if (e.getX() < width / TOOLTIP_RATIO ||
                        e.getX() > width - width / TOOLTIP_RATIO)
                        tooltip.showTooltip();
                    else tooltip.hideTooltip();
                }
            }
        });
        
        controller.setAutoAction(() -> {
            SwingUtilities.invokeLater(() -> {
                controller.nextWord();
                
                flickeringEffect.start(
                    AutoButtonIcons.getIcon(controller.getAutoMode()),
                    AutoButtonIcons.getIcon(AutoMode.OFF));
            });
        });
        
        autoButton.addActionListener(nextAutoModeAction);
        
        auxDialog = new AuxilliaryDialog(controller, icon);
        auxDialog.setEditAction((word, parent) -> editWord(word, parent));
        
        initCallbacks();
        
        initMenu();
        initHotkeys();
        
        pack();
        
        // set location
        try {
            int[] point = controller.getApplicationLocation();
            setLocation(point[0], point[1]);
        } catch(ApplicationLocationException ale) {
            setLocationRelativeTo(null);
        }
        
        updateMainButton();
        
        setCompactMode(controller.getCompactGuiMode());
    }
    
    // init all callbacks
    private void initCallbacks() {
        controller.setStateChangedCallback(b -> stateChanged(b));
        controller.setModelChangedCallback(() -> modelChanged());
        
        controller.setShowBundleCallback((bundle, word) -> {
            SwingUtilities.invokeLater(() -> {
                tooltip.hideTooltip();
                auxDialog.showBundle(bundle, word);
            });
        });
        
        // update and notification of repeat status update
        controller.setRepeatWordCallback((word, parent) -> {
            SwingUtilities.invokeLater(() -> {
                modelChanged();
                
                if ((parent == null) || (parent instanceof Component)) {
                    PlayWav.notification();
                    if (word.getWordType() != WordType.REPEAT) {
                        JOptionPane.showMessageDialog((Component) parent,
                            "<html><h2>Word «" + word.getWord() +
                                "» is not marked for repeating anymore!",
                            "Information", JOptionPane.OK_OPTION, icon);
                    } else {
                        JOptionPane.showMessageDialog((Component) parent,
                            "<html><h2>Word «" + word.getWord() +
                                "» has been added<br>to repeat list!",
                            "Information", JOptionPane.OK_OPTION, icon);
                    }
                }
            });
        });
        
        controller.setShowDefinitionCallback((word, definition) -> {
            SwingUtilities.invokeLater(() -> {
                showConsole.actionPerformed(null);
                this.setVisible(true);
            });
        });
        
        controller.setHintCallback(word ->
            SwingUtilities.invokeLater(() -> showHintDialog(word)));
        
        controller.setVerifyCharsCallback(word -> {
            SwingUtilities.invokeLater(() -> {
                showCharsDialog(word);
            });
        });
    }
    
    // hint dialog with complete information
    private void showHintDialog(Word word) {
        tooltip.hideTooltip();
        
        // boundry case
        if (word == null || word.getWord() == null) return;
        
        HintDialog.showDialog(word, icon, controller.getDefinition(word));
//        StringBuilder sb = new StringBuilder();
//        sb.append("<html><b>")
//            .append("<font size=5>")
//            .append(word.getWord())
//            .append("<br />")
//            .append("<font color=#AA0000>").append(word.getTranslation())
//            .append("</font></font>");
//        
//        if (!word.getSynonyms().isEmpty()) {
//            sb.append("<br /><font size=5 color=#04B4AE>")
//                .append(word.getSynonyms()).append("</font>");
//        }
//        
//        String definition = controller.getDefinition(word);
//        if (definition != null) {
//            definition = definition.replaceAll("\n", "<br />");
//            sb.append("<br /><font size=5 color=#0000FF>")
//                .append(definition).append("</font>");
//        }
//        
//        // add square to show word type
//        sb.append("<div style=\"width:100%;height:12px;background-color:")
//            .append(WordTypeColors.getHtmlColor(word.getWordType()))
//            .append(";margin-top: 5px\" />");
//        
//        // always on top dialog
//        JOptionPane pane = new JOptionPane(sb.toString(),
//            JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_OPTION,
//            icon, new Object[] {"OK"});
//        JDialog dialog = pane.createDialog(String.format("%s (%s, %d, %s, %s)",
//            DateTimeUtils.localDateToString(word.getBundle()),
//            word.timePassedString(), word.getTimesPicked(),
//            word.getComplexity(), word.getLastPickedString()));
//        dialog.setAlwaysOnTop(true);
//        dialog.setModal(true);
//        
//        dialog.setVisible(true);
    }
    
    private void stateChanged(boolean currentWordReplaced) {
        updateMainButton();
        
        // manage tooltip
        tooltip.updateTooltip();
        
        if (controller.getTooltipPreferences() == TooltipPreferences.ALWAYS) {
            tooltip.showTooltip();
        } else {
            tooltip.hideTooltip();
            tooltip.setTimestamp(currentWordReplaced);
        }
        
        repeatWordMenuItem.setEnabled(true);
        repeatWordMenuItem.setText(controller.getCurrentWord().getWordType() ==
            WordType.REPEAT ? "Delete repeat word" : "Add repeat word");
        
        complexityGui.stateChanged();
    }
    
    private void modelChanged() {
        stateChanged(false);
        auxDialog.fireUpdate();
    }
    
    // Choose text font according to text width.
    private void updateMainButton() {
        Icon mainButtonIcon = 
            mainButtonIcons.get(controller.getCurrentWord().getWordType());
        mainButton.setIcon(mainButtonIcon);
        
        String text = controller.getDisplayText();
        
        Graphics g = mainButton.getGraphics();
        g.setFont(FONT);
        FontMetrics metrics = g.getFontMetrics();
        
        int iconWidth = mainButtonIcon == null ? 0 : 20;
        if (metrics.stringWidth(text) > mainButton.getWidth() - 34 - iconWidth) {
            mainButton.setFont(SMALL_FONT);
        } else {
            mainButton.setFont(FONT);
        }
        
        mainButton.setText(text);
    }
    
    @Override
    public void setVisible(boolean isVisible) {
        super.setVisible(isVisible);
        mainButton.requestFocusInWindow();
    }
    
    // hotkeys
    private void initHotkeys() {
        inputMap = getRootPane().getInputMap(
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        actionMap = getRootPane().getActionMap();
        
        addGlobalAction(closeAction, 0, KeyEvent.VK_ESCAPE);
        addGlobalAction(pronounceAction, 0, KeyEvent.VK_S,
            KeyEvent.getExtendedKeyCodeForChar('ы'));
        addGlobalAction(hintAction, 0, KeyEvent.VK_H,
            KeyEvent.getExtendedKeyCodeForChar('р'));
        addGlobalAction(nextAutoModeAction, 0, KeyEvent.VK_F1);
        addGlobalAction(restartAction, KeyEvent.CTRL_DOWN_MASK,
            KeyEvent.VK_R, KeyEvent.getExtendedKeyCodeForChar('к'));
        addGlobalAction(openBundleAction, KeyEvent.CTRL_DOWN_MASK,
            KeyEvent.VK_O, KeyEvent.getExtendedKeyCodeForChar('щ'));
        addGlobalAction(showFutureWordsAction, KeyEvent.CTRL_DOWN_MASK,
            KeyEvent.VK_F, KeyEvent.getExtendedKeyCodeForChar('а'));
        addGlobalAction(showAllWordsAction, KeyEvent.CTRL_DOWN_MASK,
            KeyEvent.VK_A, KeyEvent.getExtendedKeyCodeForChar('ф'));
        addGlobalAction(showConsole, KeyEvent.CTRL_DOWN_MASK,
            KeyEvent.VK_C, KeyEvent.getExtendedKeyCodeForChar('с'));
        addGlobalAction(openDirectoryAction, KeyEvent.CTRL_DOWN_MASK,
            KeyEvent.VK_D, KeyEvent.getExtendedKeyCodeForChar('в'));
        addGlobalAction(toggleRepeatWordAction, KeyEvent.CTRL_DOWN_MASK,
            KeyEvent.VK_Y, KeyEvent.getExtendedKeyCodeForChar('н'));
        addGlobalAction(addNewWordAction, KeyEvent.CTRL_DOWN_MASK,
            KeyEvent.VK_N, KeyEvent.getExtendedKeyCodeForChar('т'));
        addGlobalAction(editCurrentWordAction, KeyEvent.CTRL_DOWN_MASK,
            KeyEvent.VK_E, KeyEvent.getExtendedKeyCodeForChar('у'));
        addGlobalAction(getDefinitionAction, 0,
            KeyEvent.VK_D, KeyEvent.getExtendedKeyCodeForChar('в'));
        addGlobalAction(dumpWordPool, KeyEvent.CTRL_DOWN_MASK,
            KeyEvent.VK_W, KeyEvent.getExtendedKeyCodeForChar('ц'));
        addGlobalAction(showStatisticsAction, KeyEvent.CTRL_DOWN_MASK,
            KeyEvent.VK_S, KeyEvent.getExtendedKeyCodeForChar('ы'));
        addGlobalAction(backupAction, KeyEvent.CTRL_DOWN_MASK,
            KeyEvent.VK_B, KeyEvent.getExtendedKeyCodeForChar('и'));
        
        // add 1, 2, 3, 4, 5 hotkeys to change word complexity
        for (int i = 0; i < WordComplexity.values().length; i++) {
            String key = "" + i;
            WordComplexity complexity = WordComplexity.values()[i];
            
            inputMap.put(KeyStroke.getKeyStroke(key), key);
            actionMap.put(key, new AbstractAction() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (controller.hasCurrentWord()) {
                        controller.setComplexity(controller.getCurrentWord(),
                            complexity);
                    }
                }
            });
        }
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0), "compact");
        actionMap.put("compact", compactModeAction);
        
        inputMap.put(
            KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), "menu");
        actionMap.put("menu", new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                tooltip.hideTooltip();
                
                if (!menu.isVisible())
                    menu.show(MainFrame.this, 0, MainFrame.this.getHeight());
                else
                    menu.setVisible(false);
            }
        });
        
        // fast hotkeys to increase and decrease complexity
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "minus");
        actionMap.put("minus", complexityGui.getDecreaseComplexityAction());
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), "plus");
        actionMap.put("plus", complexityGui.getIncreaseComplexityAction());
    }
    
    // Adds global hotkeys. Uses varargs.
    // Action should use descriptive name.
    private void addGlobalAction(Action action, int modifiers, int key,
        int... otherKeys) {
        actionMap.put(action.getValue(Action.NAME), action);
        inputMap.put(KeyStroke.getKeyStroke(key, modifiers),
            action.getValue(Action.NAME));
        for (int secondaryKey : otherKeys) {
            inputMap.put(KeyStroke.getKeyStroke(secondaryKey, modifiers),
                action.getValue(Action.NAME));
        }
    }
    
    private void makeSquareButton(AbstractButton b, String imgName) {
        b.setFocusPainted(false);
        b.setFocusable(false);
        
        int width = getHeight();
        b.setPreferredSize(new Dimension(width, width));
        
        b.addActionListener(e -> mainButton.requestFocusInWindow());
        
        if (imgName != null) {
            try {
                b.setIcon(new ImageIcon(ImageIO.read(getClass()
                    .getResource("/resources/icons/" + imgName))));
            } catch (IOException ex) { }
        }
    }
    
    // popup menu
    private void initMenu() {
        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                prefsButton.setSelected(true);
                mainButton.setSelected(true);
            }
            
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                prefsButton.setSelected(false);
            }
            
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        
        menu.add(newMenuItem(pronounceAction, 's',
            KeyStroke.getKeyStroke(KeyEvent.VK_S, 0)));
        menu.add(newMenuItem(hintAction, 'h',
            KeyStroke.getKeyStroke(KeyEvent.VK_H, 0)));
        menu.add(newMenuItem(getDefinitionAction, 'd',
            KeyStroke.getKeyStroke(KeyEvent.VK_D, 0)));
        menu.add(new JSeparator());
        
        // auto mode
        JMenu autoModeMenu = new JMenu("Auto mode");
        menu.add(autoModeMenu);
        
        ActionListener autoModeAction = e -> {
            AutoMode autoMode = AutoMode.valueOf(
                e.getActionCommand());
            controller.changeAutoMode(autoMode);
            autoButton.setIcon(AutoButtonIcons.getIcon(autoMode));
            autoButton.setSelected(autoMode != AutoMode.OFF);
        };
        
        for (AutoMode autoMode : AutoMode.values()) {
            JRadioButtonMenuItem autoModeItem = new JRadioButtonMenuItem(
                autoMode.getDescription());
            autoModeItem.setActionCommand(autoMode.name());
            autoModeItem.addActionListener(autoModeAction);
            autoModeItem.setIcon(AutoButtonIcons.getIcon(autoMode));
            autoModeGroup.add(autoModeItem);
            autoModeMenu.add(autoModeItem);
            autoModeItem.setSelected(autoMode == controller.getAutoMode());
        }
        
        // sound preferences menu
        JMenu soundMenu = new JMenu("Sound");
        menu.add(soundMenu);
        ButtonGroup soundGroup = new ButtonGroup();
        
        ActionListener soundAction = (ActionEvent e) -> {
            controller.setSoundPreferences(
                SoundPreferences.valueOf(e.getActionCommand()));
        };
        
        for (SoundPreferences sp : SoundPreferences.values()) {
            JMenuItem soundItem = new JRadioButtonMenuItem(sp.getDescription());
            soundItem.setActionCommand(sp.name());
            soundItem.addActionListener(soundAction);
            soundGroup.add(soundItem);
            soundMenu.add(soundItem);
            soundItem.setSelected(sp == controller.getSoundPreferences());
        }
        
        //**********************************************************
        JMenu displayMenu = new JMenu("Display strategy");
        menu.add(displayMenu);
        
        ActionListener displayAction = e -> {
            String actionCommand = e.getActionCommand();
            controller.setDisplayStrategy(actionCommand);
        };
        
        ButtonGroup displayGroup = new ButtonGroup();
        WordDisplayFactory.DESCRIPTIONS.stream().forEach(desc -> {
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(desc);
            menuItem.setActionCommand(desc);
            menuItem.addActionListener(displayAction);
            
            displayGroup.add(menuItem);
            displayMenu.add(menuItem);
            
            if (desc.equals(WordDisplayFactory.DESCRIPTIONS.get(0)))
                menuItem.setSelected(true);
        });
        
        //**********************************************************
        JMenu tooltipMenu = new JMenu("Show tooltip");
        menu.add(tooltipMenu);
        ButtonGroup tooltipGroup = new ButtonGroup();
        
        ActionListener tooltipAction = (ActionEvent e) -> {
            TooltipPreferences tp = TooltipPreferences.valueOf(
                e.getActionCommand());
            controller.setTooltipPreferences(tp);
            
            switch (tp) {
            case ALWAYS:
                tooltip.showTooltip();
                break;
            default:
                tooltip.hideTooltip();
            }
        };
        
        EnumMap<TooltipPreferences, JRadioButtonMenuItem> tooltipItemMap
            = new EnumMap<>(TooltipPreferences.class);
        
        for (TooltipPreferences tp : TooltipPreferences.values()) {
            JRadioButtonMenuItem tooltipItem = new JRadioButtonMenuItem(
                tp.getDescription());
            tooltipItem.setActionCommand(tp.name());
            tooltipItem.addActionListener(tooltipAction);
            tooltipGroup.add(tooltipItem);
            tooltipMenu.add(tooltipItem);
            tooltipItem.setSelected(tp == controller.getTooltipPreferences());
            tooltipItemMap.put(tp, tooltipItem);
        }
        
        tooltip.setMenuItemMap(tooltipItemMap);
        
        //=============================================
        // update word pool
        JMenu updateWordPoolMenu = new JMenu("Change word pool");
        menu.add(updateWordPoolMenu);
        
        JMenuItem allWordsItem = new JMenuItem("All words");
        updateWordPoolMenu.add(allWordsItem);
        allWordsItem.addActionListener((ActionEvent e) -> {
            controller.addAllWordsToPool();
            PlayWav.notification();
            JOptionPane.showMessageDialog(null,
                "<html><h2>Added to word pool all words.",
                "Information", JOptionPane.OK_OPTION, icon);
        });
        
        JMenuItem lastBundleItem = new JMenuItem("Last words");
        updateWordPoolMenu.add(lastBundleItem);
        lastBundleItem.addActionListener(e -> {
            controller.resetPoolToLastBundle();
            PlayWav.notification();
            JOptionPane.showMessageDialog(null,
                "<html><h2>Updated current word pool to the last bundle.",
                "Information", JOptionPane.OK_OPTION, icon);
        });
        
        updateWordPoolMenu.add(new JSeparator());
        
        JMenu byComplexityMenu = new JMenu("By complexity");
        updateWordPoolMenu.add(byComplexityMenu);
        
        for (WordComplexity wc : WordComplexity.values()) {
            JMenuItem complItem = new JMenuItem(wc.toString(),
                ComplexityGuiUtils.getIcon(wc));
            byComplexityMenu.add(complItem);
            complItem.addActionListener(e -> {
                if (controller.updatePoolByComplexity(wc)) {
                    PlayWav.notification();
                    JOptionPane.showMessageDialog(null,
                        "<html><h2>Updated word pool to " +
                            wc.toString().toLowerCase() + " words.",
                        "Information", JOptionPane.OK_OPTION, icon);
                } else
                    PlayWav.exclamation();
            });
        }
        
        //=============================================
        JMenu showMenu = new JMenu("Show");
        menu.add(showMenu);
        
        showMenu.add(newMenuItem(showAllWordsAction, 'a',
            KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK)));
        showMenu.add(newMenuItem(showFutureWordsAction, 'f',
            KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK)));
        showMenu.add(newMenuItem(openBundleAction, 'o',
            KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK)));
        showMenu.add(newMenuItem(showConsole, 'c',
            KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK)));
        showMenu.add(newMenuItem(dumpWordPool, 'w',
            KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK)));
        showMenu.add(newMenuItem(showStatisticsAction, 's',
            KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK)));
        showMenu.add(new JSeparator());
        showMenu.add(newMenuItem(openDirectoryAction, 'd',
            KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK)));
        
        menu.add(new JSeparator());
        
        menu.add(newMenuItem(editCurrentWordAction, 'e',
            KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK)));
        menu.add(newMenuItem(addNewWordAction, 'n',
            KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK)));
        
        repeatWordMenuItem = newMenuItem(toggleRepeatWordAction, 'y',
            KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK));
        repeatWordMenuItem.setText("Add repeat word");
        repeatWordMenuItem.setEnabled(false);
        menu.add(repeatWordMenuItem);
        
        menu.add(new JSeparator());
        
        menu.add(newMenuItem(compactModeAction, 'm',
            KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
        
        // backup
        backupMenuItem = newMenuItem(backupAction, 'b',
            KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK));
        menu.add(backupMenuItem);
        
        menu.add(newMenuItem(restartAction, 'r',
            KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK)));
        menu.add(newMenuItem(closeAction, 'c',
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)));
        
        if (UIManager.getLookAndFeel().getClass().getName().contains("gtk")) {
            Border border =
                BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK);
            menu.setBorder(border);
        }
    }
    
    private JMenuItem newMenuItem(Action action, int mnemonic,
        KeyStroke accelerator) {
        JMenuItem menuItem = new JMenuItem(action);
        menuItem.setAccelerator(accelerator);
        if (mnemonic != 0) menuItem.setMnemonic(mnemonic);
        return menuItem;
    }
    
    private final Action pronounceAction = new AbstractAction("Pronounce word") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            controller.pronounceCurrentWord();
        }
    };
    
    private final Action closeAction = new AbstractAction("Close") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            tooltip.hideTooltip();
            
            complexityGui.getPopup().setVisible(false);
            
            if (JOptionPane.showConfirmDialog(null, "<html><h2>Are you sure?",
                "Confirm exit", JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE, icon) == JOptionPane.OK_OPTION) {
                Point p = MainFrame.this.getLocationOnScreen();
                controller.saveApplicationSettings(p.x, p.y);
                
                System.exit(0);
            }
        }
    };
    
    private final Action hintAction = new AbstractAction("Show hint") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            showHintDialog(controller.getCurrentWord());
        }
    };
    
    private final Action nextAutoModeAction = new AbstractAction("Next auto mode") {
        @Override
        public void actionPerformed(ActionEvent e) {
            flickeringEffect.stop();
            
            controller.nextAutoMode();
            autoButton.setIcon(AutoButtonIcons.getIcon(controller.getAutoMode()));
            autoButton.setSelected(
                controller.getAutoMode() != AutoMode.OFF);
            
            Collections.list(autoModeGroup.getElements()).stream()
                .filter(b -> b.getActionCommand()
                    .equals(controller.getAutoMode().name()))
                .findAny().ifPresent(button -> button.setSelected(true));
        }
    };
    
    // restart application
    private final Action restartAction = new AbstractAction("Restart application") {
        @Override
        public void actionPerformed(ActionEvent e) {
            tooltip.hideTooltip();
            
            if (JOptionPane.showConfirmDialog(null,
                "<html><h2>Do you really want to restart<br>the application?",
                "Restart application", JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE, icon) ==
                JOptionPane.OK_OPTION) {
                Point p = MainFrame.this.getLocationOnScreen();
                controller.saveApplicationSettings(p.x, p.y);
                EnglishWords.restartApplicaiton();
            }
        }
    };
    
    private final Action openBundleAction = new AbstractAction("Bundles") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            tooltip.hideTooltip();
            
            Word currentWord = controller.getCurrentWord();
            String word = currentWord.getWord();
            LocalDate bundle = currentWord.getBundle();
            if (bundle == null) bundle = controller.getLastBundleName();
            
            auxDialog.showBundle(bundle, word);
        }
    };
    
    private final Action showFutureWordsAction = new AbstractAction("Future words") {
        @Override
        public void actionPerformed(ActionEvent e) {
            tooltip.hideTooltip();
            auxDialog.showFutureWords();
        }
    };
    
    private final Action showAllWordsAction = new AbstractAction("Words") {
        @Override
        public void actionPerformed(ActionEvent e) {
            tooltip.hideTooltip();
            auxDialog.showAllWords();
        }
    };
    
    private final Action showConsole = new AbstractAction("Console") {
        @Override
        public void actionPerformed(ActionEvent e) {
            tooltip.hideTooltip();
            auxDialog.showConsole();
        }
    };
    
    private final Action openDirectoryAction = new AbstractAction("Application directory") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            tooltip.hideTooltip();
            
            if (Desktop.isDesktopSupported()) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        Desktop.getDesktop().open(
                            controller.getProjectDirectory().toFile());
                    } catch (IOException ex) {
                        System.err.println("Unable to open directory!");
                    }
                });
            } else {
                PlayWav.exclamation();
                JOptionPane.showMessageDialog(MainFrame.this,
                    "Unable to open application directory!", "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    };
    
    private final Action toggleRepeatWordAction = new AbstractAction("Add/delete repeat word") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            tooltip.hideTooltip();
            
            if (!controller.hasCurrentWord()) return; // boundary case
            
            Word currentWord = controller.getCurrentWord();
            String question = "<html><h2>Do you really want to %s<br>" +
                "«%s» as repeating word?";
            String actionString = "mark";
            if (currentWord.getWordType() == WordType.REPEAT)
                actionString = "un" + actionString;
            
            int choice = JOptionPane.showConfirmDialog(null,
                String.format(question, actionString, currentWord.getWord()),
                "Confirm action", JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE, icon);
            if (choice == JOptionPane.OK_OPTION)
                controller.toggleRepeatWord(currentWord, null);
        }
    };
    
    private final Action addNewWordAction = new AbstractAction("Add new word") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            tooltip.hideTooltip();
            
            if (awd == null) awd = new AddWordDialog(icon, controller);
            
            if (awd.showDialog()) {
                Word newWord = awd.getNewWord();
                controller.addNewWord(newWord);
                JOptionPane.showMessageDialog(null,
                    "<html><h2>Word «" + newWord.getWord() +
                        "» has been saved.",
                    "New word", JOptionPane.INFORMATION_MESSAGE, icon);
            }
        }
    };
    
    /**
     * This method allows to edit specified word from different gui windows.
     * Method searches for a word in a model and then shows edit dialog.
     * Reuses 1 instance of edit dialog and uses lazy instantiation.
     * @param englishWord word to edit. If word is null nothings happens.
     * @param parent parent on top of which to show edit dialog
     */
    private void editWord(String englishWord, JComponent parent) {
        if (englishWord == null) return;
        
        Word editedWord = controller.getWordInstance(englishWord);
        if (editedWord == null) return;
        
        if (ewd == null) ewd = new EditWordDialog(icon, controller);
        
        if (ewd.showDialog(editedWord)) {
            Word originalWord = ewd.getOriginalWord();
            
            Map<Word, Word> map = new IdentityHashMap<>();
            map.put(editedWord, originalWord);
            
            controller.editWords(map);
            
            stateChanged(false);
            
            JOptionPane.showMessageDialog(parent,
                "<html><h2>Word «" + editedWord.getWord() +
                    "» has been saved.",
                "Edit word", JOptionPane.INFORMATION_MESSAGE, icon);
        }
    }
    
    private final Action editCurrentWordAction = new AbstractAction("Edit current word") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            tooltip.hideTooltip();
            editWord(controller.getCurrentWord().getWord(), null);
        }
    };
    
    private final Action getDefinitionAction = new AbstractAction("Show definition") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            controller.showDefinition(controller.getCurrentWord().getWord());
        }
    };
    
    private final Action dumpWordPool = new AbstractAction("Word pool dump") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            tooltip.hideTooltip();
            controller.dumpWordPool();
            auxDialog.showConsole();
        }
    };
    
    private final Action showStatisticsAction = new AbstractAction("Show statistics") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            tooltip.hideTooltip();
            auxDialog.showStatistics();
        }
    };
    
    private final Action compactModeAction = new AbstractAction("Compact mode") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean currentMode = !controller.getCompactGuiMode();
            setCompactMode(currentMode);
            controller.setCompactGuiMode(currentMode);
        }
    };
    
    private final Action backupAction = new AbstractAction("Backup") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (JOptionPane.showConfirmDialog(null,
                "<html><h2>Do you really want to backup everything?", "Backup",
                JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
                icon) != JOptionPane.OK_OPTION)
                return;
            
            backupMenuItem.setEnabled(false);
            
            controller.backup(null, () -> {
                PlayWav.notification();
                SwingUtilities.invokeLater(() -> {
                    backupMenuItem.setEnabled(true);
                    JOptionPane.showMessageDialog(null, "<html><h2>Backuping completed!",
                        "Backup", JOptionPane.INFORMATION_MESSAGE, icon);
                });
            });
        }
    };
    
    private void setCompactMode(boolean compactMode) {
        prefsButton.setVisible(compactMode);
        soundButton.setVisible(compactMode);
        autoButton.setVisible(compactMode);
        
        // correct frame size and location
        int frameWidth = compactMode ?
            FRAME_WIDTH : FRAME_WIDTH - 3 * FRAME_HEIGHT;
        setMinimumSize(new Dimension(frameWidth, FRAME_HEIGHT));
        setSize(new Dimension(frameWidth, FRAME_HEIGHT));
        
        pack();
        
        tooltip.hideTooltip();
        tooltip.updateTooltip();
    }
    
    private void showCharsDialog(Word word) {
        CharsDialog cd = new CharsDialog(word, icon);
        cd.setVisible(true);
    }
}
