package com.words.gui;

import com.words.gui.guiutils.WordTypeColors;
import com.words.controller.Controller;
import com.words.controller.words.Word;
import com.words.controller.words.wordkinds.WordType;
import com.words.controller.utils.DateTimeUtils;
import com.words.controller.utils.Utils;
import com.words.controller.words.WordFactory;
import com.words.controller.words.wordkinds.WordComplexity;
import com.words.gui.guiutils.GuiUtils;
import com.words.gui.guiutils.ComplexityGuiUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.NavigableSet;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * Shows all words for specified bundle.
 */
class BundlePanel extends JPanel {
    
    private static final String[] TABLE_HEADERS = {"", "Word", "Translation",
        Word.THIRD_FIELD_DESCRIPTION, ""};
    
    private static final int ROW_HEIGHT = 26;
    private static final float FONT_SIZE = 16f;
    private static final int MIN_COL_WIDTH = 150;
    private static final int ICON_COL_WIDTH = 28;
    private static final int PICKED_COL_WIDTH = 60;
    private static final int UPPER_COMPONENT_GAP = 3;
    
    private static Font font, headerFont;
    private Controller controller;
    private ImageIcon icon;
    
    private LocalDate bundle;
    private final JLabel infoLabel;
    private final JButton saveButton;
    private final JButton addThisBundleButton;
    
    private final JTable table;
    private BundleTableModel tableModel;
    private final TableRowSorter<TableModel> sorter;
    
    private JComboBox<LocalDate> bundleComboBox;
    private DefaultComboBoxModel<LocalDate> bundleComboModel;
    
    private JPopupMenu menu;
    
    // map to store changed words
    private final IdentityHashMap<Word, Word> editedWords =
        new IdentityHashMap<>();
    private JLabel saveLabel = new JLabel("Don't forget to save!", JLabel.RIGHT);
    
    public BundlePanel(Controller contr, ImageIcon ic, Font f) {
        this.controller = contr;
        this.icon = ic;
        font = f.deriveFont(FONT_SIZE);
        headerFont = font.deriveFont(Font.BOLD);
        
        setLayout(new BorderLayout(3, 3));
        setBorder(new EmptyBorder(3, 3, 3, 3));
        
        table = new JTable();
        table.getTableHeader().setFont(headerFont);
        table.setRowHeight(ROW_HEIGHT);
        table.setDefaultRenderer(Object.class, new BundleTableCellRenderer());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(false);
        table.setFocusable(true);
        
        // init model
        tableModel = new BundleTableModel();
        table.setModel(tableModel);
        
        // set column widths
        int firstColWidth = 1000 / 6;
        
        table.getColumnModel().getColumn(0).setMinWidth(ICON_COL_WIDTH);
        table.getColumnModel().getColumn(0).setMaxWidth(ICON_COL_WIDTH);
        table.getColumnModel().getColumn(0).setPreferredWidth(ICON_COL_WIDTH);
        table.getColumnModel().getColumn(0).setResizable(false);
        
        table.getColumnModel().getColumn(1).setMinWidth(MIN_COL_WIDTH);
        table.getColumnModel().getColumn(1).setPreferredWidth(firstColWidth);
        
        table.getColumnModel().getColumn(2).setMinWidth(MIN_COL_WIDTH);
        table.getColumnModel().getColumn(2).setPreferredWidth(
            3 * firstColWidth);
        
        table.getColumnModel().getColumn(3).setMinWidth(MIN_COL_WIDTH);
        table.getColumnModel().getColumn(3).setPreferredWidth(
            2 * firstColWidth);
        
        table.getColumnModel().getColumn(4).setMinWidth(PICKED_COL_WIDTH);
        table.getColumnModel().getColumn(4).setMaxWidth(PICKED_COL_WIDTH);
        table.getColumnModel().getColumn(0).setResizable(false);
        
        // create row sorter
        table.setAutoCreateRowSorter(true);
        sorter = new TableRowSorter<>(tableModel);
        
        Comparator<String> wordColComp = (s1, s2) ->
            s1.replaceAll("^to ", "").compareTo(s2.replaceAll("^to ", ""));
        
        sorter.setComparator(1, wordColComp);
        
        sorter.setComparator(0, (Word w1, Word w2) -> {
            int complComp = Integer.compare(w1.getComplexity().getWeight(),
                w2.getComplexity().getWeight());
            if (complComp != 0) return complComp;
            
            int wordComp = wordColComp.compare(w1.getWord(), w2.getWord());
            if (sorter.getSortKeys().get(0).getSortOrder() != SortOrder.ASCENDING)
                wordComp = -wordComp;
            
            return wordComp;
        });
        
        sorter.setComparator(4, Comparator.comparingInt(Word::getTimesPicked));
        // sorting priority for columns
        List <RowSorter.SortKey> sortKeys = new ArrayList<>();
//        sortKeys.add(new RowSorter.SortKey(0, SortOrder.DESCENDING));
        sortKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        
        table.setRowSorter(sorter);
        
        // set default cell editor
        JTextField textField = new JTextField();
        textField.setFont(font);
        textField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Color.RED, 2),
            new EmptyBorder(0, 7, 0, 7)));
        DefaultCellEditor dce = new DefaultCellEditor(textField);
        dce.setClickCountToStart(2);
        table.setDefaultEditor(Object.class, dce);
        
        table.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row == -1) return;
                
                table.getSelectionModel().setSelectionInterval(row, row);
                if (e.getButton() == MouseEvent.BUTTON3) {
                    menu.show(table, e.getX(), e.getY());
                }
            }
        });
        
        add(new JScrollPane(table), BorderLayout.CENTER);
        
        saveButton = GuiUtils.newNotFocusableButton("Save");
        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> saveChanges());
        
        addThisBundleButton = GuiUtils.newNotFocusableButton("Add");
        addThisBundleButton.setToolTipText(
            "<html><b>Add this bundle to current word pool");
        addThisBundleButton.addActionListener(e -> {
            controller.addBundleToPool(bundle);
            JOptionPane.showMessageDialog(BundlePanel.this,
                "<html><h2>Bundle «" + DateTimeUtils.localDateToString(bundle) +
                    "» has been added to the pool.",
                "Information", JOptionPane.INFORMATION_MESSAGE, icon);
        });
        
        //*********************************************************
        // Init upper panel
        JPanel upperPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        
        List<String> bundles =
            new ArrayList(controller.allBundlesSorted().descendingSet());
        
        bundleComboModel = new DefaultComboBoxModel<>(
            bundles.toArray(new LocalDate[bundles.size()]));
        
        bundleComboBox = new JComboBox<>(bundleComboModel);
        bundleComboBox.setFont(font);
        bundleComboBox.setFocusable(false);
        
        bundleComboBox.setRenderer(new DefaultListCellRenderer() {
            
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
                setText(DateTimeUtils.localDateToString((LocalDate) value));
                setBorder(new EmptyBorder(2, 4, 2, 2));
                return this;
            }
        });
        
        bundleComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                setBundle(bundleComboBox.getItemAt(bundleComboBox.getSelectedIndex()));
            }
        });
        
        bundleComboBox.addMouseWheelListener(e -> {
            int count = bundleComboBox.getItemCount();
            if (count < 2) return;
            
            int rotation = e.getWheelRotation();
            
            bundleComboBox.setSelectedIndex(
                (count + bundleComboBox.getSelectedIndex() + rotation) %
                    count);
        });
        
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.insets = new Insets(0, 0, 0, UPPER_COMPONENT_GAP);
        upperPanel.add(bundleComboBox, c);
        
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0;
        c.insets = new Insets(0, 0, 0, 0);
        upperPanel.add(addThisBundleButton, c);
        
        infoLabel = new JLabel("");
        infoLabel.setFont(font);
        infoLabel.setHorizontalAlignment(JLabel.LEFT);
        infoLabel.setIconTextGap(2);
        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 0;
        c.insets = new Insets(0, UPPER_COMPONENT_GAP, 0,
            UPPER_COMPONENT_GAP);
        upperPanel.add(infoLabel, c);
        
        c.gridx = 3;
        c.gridy = 0;
        c.weightx = 1;
        upperPanel.add(new JLabel(""), c);
        
        saveLabel.setFont(saveLabel.getFont().deriveFont(Font.BOLD));
        saveLabel.setForeground(GuiUtils.EDITED_COLOR);
        c.gridx = 4;
        c.gridy = 0;
        c.weightx = 0;
        c.insets = new Insets(0, UPPER_COMPONENT_GAP, 0, 5);
        upperPanel.add(saveLabel, c);
        
        c.gridx = 5;
        c.gridy = 0;
        c.weightx = 0;
        c.insets = new Insets(0, UPPER_COMPONENT_GAP, 0, 0);
        upperPanel.add(saveButton, c);
        
        add(upperPanel, BorderLayout.SOUTH);
        
        GuiUtils.setSameButtonSize(saveButton, addThisBundleButton);
        
        initMenu();
        
        registerHotkeys();
        
        buttonAndLabelSync();
    }
    
    private void registerHotkeys() {
        InputMap inputMap = table.getInputMap(
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = table.getActionMap();
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
        actionMap.put("Enter", new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (table.getSelectedRow() != -1)

                    controller.pronounceWord(getSelectedWord());
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "Up");
        actionMap.put("Up", new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (tableModel.getRowCount() > 0) {
                    int selectedIndex = table.getSelectedRow() - 1;
                    if (selectedIndex <= 0)
                        selectedIndex = tableModel.getRowCount() - 1;
                    table.getSelectionModel().setSelectionInterval(
                        selectedIndex, selectedIndex);
                    table.scrollRectToVisible(new Rectangle(
                        table.getCellRect(selectedIndex, 0, true)));
                }
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "Down");
        actionMap.put("Down", new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (tableModel.getRowCount() > 0) {
                    int selectedIndex = table.getSelectedRow() + 1;
                    if (selectedIndex >= tableModel.getRowCount()) selectedIndex = 0;
                    table.getSelectionModel().setSelectionInterval(
                        selectedIndex, selectedIndex);
                    table.scrollRectToVisible(new Rectangle(
                        table.getCellRect(selectedIndex, 0, true)));
                }
            }
        });
    }
    
    private void initMenu() {
        // popup menu
        menu = new JPopupMenu();
        
        JMenuItem showDefinitionItem = new JMenuItem(new AbstractAction("Show hint") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.showHint(getSelectedWord());
            }
        });
        menu.add(showDefinitionItem);
        
        JMenuItem soundItem = new JMenuItem("Pronounce word");
        menu.add(soundItem);
        soundItem.addActionListener(
            e -> controller.pronounceWord(getSelectedWord()));
        
        JMenuItem repeatWordItem = new JMenuItem("Add/delete repeat word");
        menu.add(repeatWordItem);
        repeatWordItem.addActionListener((e) -> {
            controller.toggleRepeatWord(getSelectedWord(), BundlePanel.this);
            table.repaint();
        });
        
        JMenuItem deleteWordItem = new JMenuItem("Delete word");
        menu.add(deleteWordItem);
        deleteWordItem.addActionListener(e -> deleteSelectedWord());
        
        menu.addPopupMenuListener(new PopupMenuListener() {
            
            @Override
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) { }
            
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) { }
            
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                repeatWordItem.setText(getSelectedWord().getWordType() ==
                    WordType.REPEAT ? "Delete repeat word" : "Add repeat word");
            }
        });
        
        GuiUtils.addComplexityMenuItem(menu,
            complexity -> e -> {
                controller.setComplexity(getSelectedWord(), complexity);
//                tableModel.fireTableDataChanged();
            },
            () -> getSelectedWord());
    }
    
    private void deleteSelectedWord() {
        Word word = getSelectedWord();
        
        int choice = JOptionPane.showConfirmDialog(
            BundlePanel.this,
            "<html><h2>Do you really want to delete «" + word.getWord() + "»?",
            "Confirm action", JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE, icon
        );
        
        if (choice == JOptionPane.OK_OPTION) {
            editedWords.remove(word);
            controller.deleteWord(word.getWord());
            
            buttonAndLabelSync();
        }
    }
    
    // enables / disables button and label 
    private void buttonAndLabelSync() {
        saveButton.setEnabled(!editedWords.isEmpty());
        saveLabel.setVisible(!editedWords.isEmpty());
    }
    
    // this method caches all modfied words so they can be saved later
    private void manageEditedWord(Word editedWord, Word originalWord) {
        if (editedWords.containsKey(editedWord)) {
            if (editedWord.equals(editedWords.get(editedWord))) editedWords.remove(editedWord);
        } else {
            editedWords.put(editedWord, originalWord);
        }
        
        buttonAndLabelSync();
        
        System.out.println("Edited words cache: " + editedWords);
    }
    
    // required to work properly with row sorter
    private Word getSelectedWord() {
        return tableModel.getWord(table.convertRowIndexToModel(
            table.getSelectedRow()));
    }
    
    /**
     * Set bundle and update table.
     * @param bundle bundle name
     */
    public final void setBundle(LocalDate newBundle) {
        NavigableSet<LocalDate> bundles = controller.allBundlesSorted();
        
        bundle = bundles.contains(newBundle) ? newBundle : bundles.last();
        if (bundle == null) return;
        
        Collection<Word> words = controller.getWordsBundle(bundle);
        int totalPicked = words.stream().mapToInt(Word::getTimesPicked).sum();
        int averageComplexityWeight = (int) words.stream()
            .mapToInt(w -> w.getComplexity().getWeight()).average().orElse(100d);
        
        bundleComboBox.setSelectedItem(bundle);
        
        StringBuilder info = new StringBuilder(30);
        final String delimiter = ", ";
        info.append(averageComplexityWeight).append(delimiter)
            .append(DateTimeUtils.getFormattedPeriod(bundle)).append(delimiter)
            .append(Utils.getNumeralWithWord(words.size(), "word"))
            .append(" (").append(totalPicked).append(")");
        infoLabel.setText(info.toString());
        infoLabel.setIcon(ComplexityGuiUtils.getIcon(
            WordComplexity.closest(averageComplexityWeight)));
        
        tableModel.setNewBundle(words);
        table.clearSelection();
    }
    
    /**
     * Select specified word.
     * @param word word to select. Can be null which means no selection.
     * @throws NullPointerException word can't be null
     */
    public void setSelectedWord(String word) {
        table.requestFocusInWindow();
        
        if (word == null) {
            return;
        }
        
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (word.equals(tableModel.getWord(i).getWord())) {
                int k = table.convertRowIndexToView(i);
                table.getSelectionModel().setSelectionInterval(k, k);
                table.scrollRectToVisible(
                    new Rectangle(table.getCellRect(k, 0, true)));
                break;
            }
        }
    }
    
    /**
     * Force gui to update.
     */
    public void fireUpdate() {
        // update bundles
        List<String> bundles =
            new ArrayList(controller.allBundlesSorted().descendingSet());
        bundleComboModel = new DefaultComboBoxModel<>(
            bundles.toArray(new LocalDate[bundles.size()]));
        bundleComboBox.setModel(bundleComboModel);
        
        setBundle(bundle);
        
        tableModel.fireTableDataChanged();
    }
    
    private void saveChanges() {
        controller.editWords(new IdentityHashMap<>(editedWords));
        editedWords.clear();
        
        buttonAndLabelSync();
    }
    
    private class BundleTableModel extends AbstractTableModel {
        
        private final List<Word> bundleList = new ArrayList<>();
        
        public BundleTableModel() { }
        
        @Override
        public void setValueAt(Object value, int row, int column) {
            String newValue = value.toString().trim().toLowerCase()
                .replaceAll("\\s+", " ");
            Word word = bundleList.get(row);
            Word originalWord = WordFactory.copyWord(word);
            
            switch (column) {
            case 1:
                if (!newValue.equals(word.getWord())) {
                    word.setWord(value.toString());
                    word.setMp3File(null);
                    manageEditedWord(word, originalWord);
                }
                break;
            case 2:
                if (!newValue.equals(word.getTranslation())) {
                    word.setTranslation(value.toString());
                    manageEditedWord(word, originalWord);
                }
                break;
            case 3:
                if (!newValue.equals(word.getSynonyms())) {
                    word.setSynonyms(value.toString());
                    manageEditedWord(word, originalWord);
                }
                break;
            default:
                break;
            }
        }
        
        public void setNewBundle(Collection<Word> list) {
            bundleList.clear();
            bundleList.addAll(list);
            
            fireTableDataChanged();
        }
        
        /**
         * Get selected word.
         * @param index selected row index
         * @return selected word
         */
        public Word getWord(int index) {
            return bundleList.get(index);
        }
        
        /**
         * Get table data.
         * @return underlying list
         */
        public List<Word> getBundleList() {
            return bundleList;
        }
        
        @Override
        public String getColumnName(int column) {
            return TABLE_HEADERS[column];
        }
        
        @Override
        public boolean isCellEditable(int row, int column) {
            switch (column) {
            case 1: case 2: case 3:
                return true;
            default:
                return false;
            }
        }
        
        @Override
        public int getRowCount() {
            return bundleList.size();
        }
        
        @Override
        public int getColumnCount() {
            return TABLE_HEADERS.length;
        }
        
        @Override
        public Class getColumnClass(int col) {
            switch (col) {
            case 1: case 2: case 3:
                return String.class;
            default:
                return Word.class;
            }
        }
        
        @Override
        public Object getValueAt(int row, int column) {
            switch (column) {
            case 0: return bundleList.get(row);
            case 1: return bundleList.get(row).getWord();
            case 2: return bundleList.get(row).getTranslation();
            case 3: return bundleList.get(row).getSynonyms();
            case 4: return bundleList.get(row);
            default: return null;
            }
        }
        
        /*
        * Delete word by index.
        * @param index index of word to delete
        * Thrwos IllegalArgumentException if index isn't in a range
        */
        public void deleteWord(Word w) {
            bundleList.remove(w);
        }
    }
    
    private class BundleTableCellRenderer extends DefaultTableCellRenderer {
        
        @Override
        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column) {
            
            Word word = tableModel.getWord(table.convertRowIndexToModel(row));
            
            JLabel label = (JLabel) super.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);
            label.setFont(font);
            label.setBorder(new EmptyBorder(1, 8, 1, 8));
            label.setIcon(null);
            
            if (editedWords.containsKey(word)) {
                label.setFont(font.deriveFont(Font.BOLD));
            }
            
            switch (column) {
            case 0:
                setHorizontalAlignment(CENTER);
                label.setIcon(ComplexityGuiUtils.getIcon(word.getComplexity()));
                label.setBorder(null);
                label.setText("");
                break;
            case 4:
                setHorizontalAlignment(TRAILING);
                label.setText("" + ((Word) value).getTimesPicked());
                break;
            default:
                setHorizontalAlignment(LEADING);
                break;
            }
            
            if (isSelected) {
                label.setBackground(GuiUtils.SELECTION_COLOR);
                label.setForeground(Color.WHITE);
                
                if (word.getWordType() == WordType.REPEAT) {
                    WordType type = word.getWordType();
                    label.setBackground(WordTypeColors.getAverageColor(type,
                        GuiUtils.SELECTION_COLOR));
                    label.setForeground(WordTypeColors.getForegroundColor());
                }
            } else {
                label.setBackground(row % 2 == 0 ?
                    GuiUtils.EVEN_COLOR : GuiUtils.ODD_COLOR);
                label.setForeground(Color.BLACK);
                
                if (editedWords.containsKey(word)) {
                    label.setBackground(GuiUtils.EDITED_COLOR);
                    label.setForeground(Color.WHITE);
                }
                
                if (word.getWordType() == WordType.REPEAT) {
                    WordType type = word.getWordType();
                    label.setBackground(WordTypeColors.getColor(type));
                    label.setForeground(WordTypeColors.getForegroundColor());
                }
            }

            return label;
        }
    }
}
