package com.words.gui;

import com.words.gui.guiutils.WordTypeColors;
import com.words.controller.Controller;
import com.words.controller.utils.DateTimeUtils;
import com.words.controller.utils.Utils;
import com.words.controller.words.Word;
import com.words.controller.words.wordkinds.WordComplexity;
import com.words.controller.words.wordkinds.WordType;
import com.words.gui.guiutils.GuiUtils;
import com.words.gui.guiutils.ComplexityGuiUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

class AllWordsPanel extends JPanel {
    
    // required to delay list update
    private static final int TIMEOUT_MILLIS = 400;
    private final ScheduledExecutorService exec =
        Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> listFuture = null;
    
    private static final int SMALL_FONT_SIZE = 14;
    
    private final JTextField textField;
    private final JLabel amountLabel;
    
    private final JCheckBox repeatCheckBox;
    private final JCheckBox filterSynsCheckBox;
    
    private final JList<Word> list;
    private final AllWordsListModel listModel;
    
    private final Controller controller;
    private final ImageIcon icon;
    
    private final JPopupMenu menu = new JPopupMenu();
    
    private BiConsumer<String, JComponent> editAction = null;
    
    private final JCheckBox complexityCheckBox;
    private final JComboBox<WordComplexity> complexityBox;
    
    // delays statistic loading
    private boolean firstShow = true;
    
    public AllWordsPanel(Controller ctrl, Font font, ImageIcon ic) {
        this.controller = ctrl;
        this.icon = ic;
        
        setLayout(new BorderLayout(3, 3));
        setBorder(new EmptyBorder(3, 3, 3, 3));
        
        JPanel upperPanel = new JPanel(new GridBagLayout());
        add(upperPanel, BorderLayout.NORTH);
        upperPanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Color.BLACK, 1),
            new EmptyBorder(4, 6, 4, 5)));
        upperPanel.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        upperPanel.setBackground(Color.WHITE);
        
        textField = new JTextField(30);
        textField.setFont(font);
        textField.setBorder(null);
        textField.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        
        final Runnable listRunnable = () ->
            SwingUtilities.invokeLater(() ->fireUpdate());
        
        // text field on value change
        textField.getDocument().addDocumentListener(new DocumentListener() {
            
            @Override
            public void insertUpdate(DocumentEvent e) { updateDocument(); }
            
            @Override
            public void removeUpdate(DocumentEvent e) { updateDocument(); }
            
            @Override
            public void changedUpdate(DocumentEvent e) { updateDocument(); }
            
            private void updateDocument() {
                if (listFuture != null && !listFuture.isDone())
                    listFuture.cancel(false);
                listFuture = exec.schedule(listRunnable, TIMEOUT_MILLIS,
                    TimeUnit.MILLISECONDS);
            }
        });
        
        amountLabel = new JLabel("0");
        amountLabel.setFont(font);
        amountLabel.setForeground(Color.GRAY);
        amountLabel.setBorder(new EmptyBorder(0, 5, 0, 5));
        amountLabel.setFocusable(false);
        amountLabel.setToolTipText("<html><b>Total words shown");
        
        listModel = new AllWordsListModel();
        
        list = new JList<>(listModel);
        list.setCellRenderer(new AllWordsCellRenderer(font));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // clear button
        JButton clearButton = new JButton("");
        clearButton.setToolTipText("<html><b>Clear text field");
        clearButton.setPreferredSize(new Dimension(30, 22));
        clearButton.setBorderPainted(false);
        clearButton.setContentAreaFilled(false);
        clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearButton.setFocusPainted(false);
        
        try {
            clearButton.setIcon(new ImageIcon(ImageIO.read(getClass()
                .getResource("/resources/icons/clear.png"))));
        } catch (IOException ex) { }
        
        clearButton.addActionListener(e -> {
            if (!textField.getText().isEmpty()) {
                textField.setText("");
                textField.requestFocusInWindow();
            }
        });
        
        GridBagConstraints c = new GridBagConstraints();
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.PAGE_START;
        upperPanel.add(textField, c);
        
        c.fill = GridBagConstraints.NONE;
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0;
        c.anchor = GridBagConstraints.PAGE_END;
        upperPanel.add(amountLabel, c);
        
        c.fill = GridBagConstraints.NONE;
        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 0;
        c.anchor = GridBagConstraints.PAGE_END;
        upperPanel.add(clearButton, c);
        
        add(new JScrollPane(list,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        
        initMenu();
        
        list.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (listModel.indices.isEmpty()) return;
                
                if (e.getButton() == MouseEvent.BUTTON1
                    && e.getClickCount() == 2) {
                    controller.pronounceWord(list.getSelectedValue());
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    list.setSelectedIndex(list.locationToIndex(e.getPoint()));
                    menu.show(list, e.getX(), e.getY());
                }
            }
        });
        
        // show only repeat words?
        repeatCheckBox = new JCheckBox(new AbstractAction("Show only repeat words") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (repeatCheckBox.isSelected()) {
                    textField.setText("");
                    complexityCheckBox.setSelected(false);
                }
                fireUpdate();
            }
        });
        repeatCheckBox.setSelected(false);
        repeatCheckBox.setFocusPainted(false);
        
        filterSynsCheckBox = new JCheckBox(new AbstractAction("Filter using synonyms") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                fireUpdate();
            }
        });
        filterSynsCheckBox.setSelected(false);
        filterSynsCheckBox.setFocusPainted(false);
        
        // complexity filter
        complexityCheckBox = new JCheckBox(new AbstractAction("Filter by complexity:") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (complexityCheckBox.isSelected()) textField.setText("");
                fireUpdate();
            }
        });
        
        complexityCheckBox.setSelected(false);
        complexityCheckBox.setFocusPainted(false);
        
        complexityBox = new JComboBox<>(WordComplexity.values());
        complexityBox.setSelectedIndex(2);
        
        complexityBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                complexityCheckBox.setSelected(true);
                fireUpdate();
            }
        });
        
        complexityBox.setRenderer((list1, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value.toString(),
                ComplexityGuiUtils.getIcon(value), JLabel.LEFT);
            label.setOpaque(true);
            label.setBorder(new EmptyBorder(1, 3, 1, 1));
            if (isSelected) label.setBackground(Color.LIGHT_GRAY);
            return label;
        });
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        add(bottomPanel, BorderLayout.SOUTH);
        
        bottomPanel.add(filterSynsCheckBox);
        bottomPanel.add(Box.createHorizontalStrut(10));
        bottomPanel.add(repeatCheckBox);
        bottomPanel.add(Box.createHorizontalStrut(10));
        bottomPanel.add(complexityCheckBox);
        bottomPanel.add(Box.createHorizontalStrut(2));
        bottomPanel.add(complexityBox);
        
        registerHotkeys();
    }
    
    public void showPanel() {
        if (firstShow) {
            firstShow = false;
            fireUpdate();
        }
    }
    
    public void setEditAction(BiConsumer<String, JComponent> editAction) {
        this.editAction = editAction;
    }
    
    private void updateAmountLabel() {
        amountLabel.setText("" + listModel.getSize());
    }
    
    /**
     * Request textField focus.
     */
    public void focusTextField() {
        textField.requestFocusInWindow();
        textField.selectAll();
    }
    
    private void initMenu() {
        JMenuItem editWordItem = new JMenuItem(new AbstractAction("Edit word") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (editAction != null)
                    editAction.accept(
                        list.getSelectedValue().getWord(), AllWordsPanel.this);
            }
        });
        menu.add(editWordItem);
        
        JMenuItem showDefinitionItem = new JMenuItem(new AbstractAction("Show hint") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.showHint(list.getSelectedValue());
            }
        });
        menu.add(showDefinitionItem);
        
        JMenuItem pronounceItem = new JMenuItem(new AbstractAction("Pronounce word") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.pronounceWord(list.getSelectedValue());
            }
        });
        menu.add(pronounceItem);
        
        JMenuItem repeatItem = new JMenuItem(new AbstractAction("Add/delete repeat word") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                Word word = list.getSelectedValue();
                controller.toggleRepeatWord(word, AllWordsPanel.this);
                fireUpdate();
            }
        });
        menu.add(repeatItem);
        
        JMenuItem showBundleItem = new JMenuItem(new AbstractAction("Show bundle") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                Word word = list.getSelectedValue();
                controller.showWordsBundle(word.getBundle(), word.getWord());
            }
        });
        menu.add(showBundleItem);
        
        JMenuItem addWordItem = new JMenuItem(new AbstractAction("Add word into pool") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                Word word = list.getSelectedValue();
                controller.addWord(word);
                JOptionPane.showMessageDialog(AllWordsPanel.this,
                    "<html><h2>Word «" + word.getWord() +
                        "» has been added to current pool.",
                    word.getWord(),
                    JOptionPane.INFORMATION_MESSAGE, icon);
            }
        });
        
        menu.addPopupMenuListener(new PopupMenuListener() {
            
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                repeatItem.setText(list.getSelectedValue().getWordType() ==
                    WordType.REPEAT ? "Delete repeat word" : "Add repeat word");
            }
            
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { }
            
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) { }
        });
        
        GuiUtils.addComplexityMenuItem(menu, complexity -> e -> {
            controller.setComplexity(list.getSelectedValue(), complexity);
            fireUpdate();
        }, () -> list.getSelectedValue());
    }
    
    private static class AllWordsCellRenderer extends JPanel implements
        ListCellRenderer<Word> {
        
        private final JLabel wordLabel;
        private final JLabel translationLabel;
        private final JLabel dateLabel;
        private final JLabel synonymsLabel;
        private final JLabel pickedLabel;
        
        public AllWordsCellRenderer(Font font) {
            setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Color.LIGHT_GRAY, 1),
                new EmptyBorder(4, 8, 4, 8)));
            
            wordLabel = new JLabel("Word");
            wordLabel.setFont(font);
            
            translationLabel = new JLabel("Translation");
            translationLabel.setFont(font);
            
            dateLabel = new JLabel("11.11.2011", JLabel.RIGHT);
            dateLabel.setFont(font.deriveFont(Font.PLAIN, SMALL_FONT_SIZE));
            
            synonymsLabel = new JLabel("Synonyms");
            synonymsLabel.setFont(font);
            
            pickedLabel = new JLabel("", JLabel.RIGHT);
            pickedLabel.setFont(font.deriveFont(Font.PLAIN, SMALL_FONT_SIZE));
            
            setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1;
            c.anchor = GridBagConstraints.PAGE_START;
            add(wordLabel, c);
            
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 0;
            c.anchor = GridBagConstraints.PAGE_END;
            add(pickedLabel, c);
            
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 1;
            c.weightx = 1;
            c.anchor = GridBagConstraints.PAGE_START;
            add(translationLabel, c);
            
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.gridy = 1;
            c.weightx = 0;
            c.anchor = GridBagConstraints.PAGE_END;
            add(dateLabel, c);
            
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 2;
            c.gridwidth = 1;
            c.weightx = 1;
            c.anchor = GridBagConstraints.PAGE_START;
            add(synonymsLabel, c);
        }
        
        @Override
        public Component getListCellRendererComponent(
            JList<? extends Word> list, Word value, int index,
            boolean isSelected, boolean cellHasFocus) {
            
            if (isSelected) {
                setBackground(GuiUtils.SELECTION_COLOR);
                wordLabel.setForeground(Color.WHITE);
                translationLabel.setForeground(Color.WHITE);
                dateLabel.setForeground(Color.WHITE);
                synonymsLabel.setForeground(Color.WHITE);
                pickedLabel.setForeground(Color.WHITE);
                
                if (value.getWordType() == WordType.REPEAT) {
                    WordType type = value.getWordType();
                    setBackground(WordTypeColors.getAverageColor(type,
                        GuiUtils.SELECTION_COLOR));
                    wordLabel.setForeground(WordTypeColors.getForegroundColor());
                    translationLabel.setForeground(WordTypeColors.getForegroundColor());
                    dateLabel.setForeground(WordTypeColors.getForegroundColor());
                    synonymsLabel.setForeground(WordTypeColors.getForegroundColor());
                    pickedLabel.setForeground(WordTypeColors.getForegroundColor());
                }
            } else {
                setBackground(index % 2 == 0 ?
                    GuiUtils.ODD_COLOR : GuiUtils.EVEN_COLOR);
                
                wordLabel.setForeground(Color.BLACK);
                dateLabel.setForeground(Color.BLACK);
                
                translationLabel.setForeground(GuiUtils.SELECTION_COLOR);
                pickedLabel.setForeground(Color.BLACK);
                
                synonymsLabel.setForeground(GuiUtils.SYNONYMS_COLOR);
                
                if (value.getWordType() == WordType.REPEAT) {
                    WordType type = value.getWordType();
                    setBackground(WordTypeColors.getColor(type));
                    wordLabel.setForeground(WordTypeColors.getForegroundColor());
                    translationLabel.setForeground(WordTypeColors.getForegroundColor());
                    dateLabel.setForeground(WordTypeColors.getForegroundColor());
                    synonymsLabel.setForeground(WordTypeColors.getForegroundColor());
                    pickedLabel.setForeground(WordTypeColors.getForegroundColor());
                }
            }
            
            wordLabel.setText(value.getWord());
            wordLabel.setIcon(ComplexityGuiUtils.getIcon(value.getComplexity()));
            
            translationLabel.setText(value.getTranslation());
            dateLabel.setText(DateTimeUtils.localDateToString(value.getBundle()));
            synonymsLabel.setText(value.getSynonyms());
            
            pickedLabel.setText(value.getLastPickedString() + ", " +
                Utils.getNumeralWithWord(value.getTimesPicked(), "time"));
            
            setVisibleBottomRow(!value.getSynonyms().isEmpty());
            
            return this;
        }
        
        private void setVisibleBottomRow(boolean isVisible) {
            synonymsLabel.setVisible(isVisible);
        }
    }
    
    // model
    private static class AllWordsListModel extends AbstractListModel<Word> {
        
        private final List<Word> sourceList;
        private final List<Integer> indices;
        
        public AllWordsListModel() {
            sourceList = new ArrayList<>(2000);
            indices = new ArrayList<>();
        }
        
        public void setWords(Collection<Word> words) {
            sourceList.clear();
            sourceList.addAll(words);
            indices.clear();
        }
        
        @Override
        public int getSize() {
            return indices.size();
        }
        
        @Override
        public Word getElementAt(int index) {
            return sourceList.get(indices.get(index));
        }
        
        /**
         * Set filter and update list contents.
         * @param str new filter string
         * @param onlyRepeatWords filter only repeat words
         * @param filterUsingSyns filter also synonyms
         * @param complexity filter with complexity, null skips this option
         */
        private void setFilter(String str, boolean onlyRepeatWords,
            boolean filterUsingSyns, WordComplexity complexity) {
            String filterString = str;
            indices.clear();
            
            List<Integer> secondaryIndices = new LinkedList<>();
            
            for (int i = 0; i < sourceList.size(); i++) {
                Word word = sourceList.get(i);
                
                if (complexity != null && !word.getComplexity().equals(complexity))
                    continue;
                
                if (onlyRepeatWords && word.getWordType() != WordType.REPEAT)
                    continue;
                
                if (filterString.isEmpty()) {
                    indices.add(i);
                    continue;
                }
                
                if (word.getWord().contains(filterString)) {
                    String wordStr = word.getWord();
                    if (wordStr.startsWith("to ") && wordStr.substring(3).startsWith(str)) {
                        indices.add(i);
                    } else {
                        if (word.getWord().startsWith(str)) indices.add(i);
                        else secondaryIndices.add(i);
                    }
                    continue;
                }
                
                if (word.getTranslation().contains(filterString) ||
                    DateTimeUtils.localDateToString(word.getBundle()).contains(filterString)) {
                    secondaryIndices.add(i);
                    continue;
                }
                
                if (filterUsingSyns && word.getSynonyms().contains(filterString))
                    secondaryIndices.add(i);
            }
            
            indices.addAll(secondaryIndices);
            
            fireContentsChanged(this, 0, getSize() - 1);
        }
    }
    
    /**
     * Updates list
     */
    public final void fireUpdate() {
        WordComplexity complexity = null;
        
        listModel.setWords(controller.getAllWordsAsList());
        
        if (complexityCheckBox.isSelected())
            complexity = (WordComplexity) complexityBox.getSelectedItem();
        
        listModel.setFilter(textField.getText().trim().toLowerCase(),
            repeatCheckBox.isSelected(), filterSynsCheckBox.isSelected(),
            complexity);
        updateAmountLabel();
        
        int selectedIndex = 0;
        list.setSelectedIndex(selectedIndex);
        list.ensureIndexIsVisible(selectedIndex);
    }
    
    private void registerHotkeys() {
        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
        actionMap.put("Enter", new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (listModel.getSize() > 0) {
                    list.ensureIndexIsVisible(list.getSelectedIndex());
                    controller.pronounceWord(list.getSelectedValue());
                }
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "Up");
        actionMap.put("Up", new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (listModel.getSize() > 0) {
                    int selectedIndex = list.getSelectedIndex() - 1;
                    if (selectedIndex < 0) selectedIndex = 0;
                    list.setSelectedIndex(selectedIndex);
                    list.ensureIndexIsVisible(selectedIndex);
                }
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "Down");
        actionMap.put("Down", new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (listModel.getSize() > 0) {
                    int selectedIndex = list.getSelectedIndex() + 1;
                    if (selectedIndex >= listModel.getSize())
                        selectedIndex = listModel.getSize() - 1;
                    list.setSelectedIndex(selectedIndex);
                    list.ensureIndexIsVisible(selectedIndex);
                }
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "F1");
        actionMap.put("F1", new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                filterSynsCheckBox.doClick();
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "F2");
        actionMap.put("F2", new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                repeatCheckBox.doClick();
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "F3");
        actionMap.put("F3", new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                complexityCheckBox.doClick();
            }
        });
    }
}