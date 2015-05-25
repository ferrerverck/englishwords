package com.words.gui;

import com.words.controller.Controller;
import com.words.controller.futurewords.FutureWord;
import com.words.controller.futurewords.FwAlreadyUsedException;
import com.words.controller.futurewords.FwEmptyWordException;
import com.words.controller.futurewords.FwTimeoutException;
import com.words.controller.sound.PlayWav;
import com.words.controller.utils.Utils;
import com.words.controller.words.Word;
import com.words.gui.guiutils.GuiUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Future words panel. Allows user to add, delete words.
 * Check all future words, change priority.
 * Filters list contents according to JTextField.
 */
class FutureWordsPanel extends JPanel {
    
    private static final int CELL_HEIGHT = 30;
    
    private final Controller controller;
    
    private static Font font;
    private static ImageIcon icon;
    
    private final JTextField textField;
    private final JLabel amountLabel;
    
    private final JList<String> list;
    private final FutureListModel listModel;
    
    private JPopupMenu popup;
    
    // delays statistic loading
    private boolean firstShow = true;
    
    public FutureWordsPanel(Controller ctrl, ImageIcon ic, Font f) {
        this.controller = ctrl;
        FutureWordsPanel.font = f;
        FutureWordsPanel.icon = ic;
        
        setLayout(new BorderLayout(3, 3));
        setBorder(new EmptyBorder(3, 3, 3, 3));
        
        JPanel upperPanel = new JPanel(new BorderLayout(0, 0));
        
        JPanel textFieldPanel = new JPanel(new BorderLayout(3, 3));
        textFieldPanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Color.BLACK, 1),
            new EmptyBorder(4, 6, 4, 5)));
        textFieldPanel.setBackground(Color.WHITE);
        textFieldPanel.setOpaque(true);
        textFieldPanel.setCursor(
            Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        upperPanel.add(textFieldPanel, BorderLayout.CENTER);
        
        textField = new JTextField();
        textField.setFont(font);
        textField.setBorder(null);
        textField.setBackground(Color.WHITE);
        textField.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        textFieldPanel.add(textField, BorderLayout.CENTER);
        
        // text field on value change
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                valueChanged();
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                valueChanged();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                valueChanged();
            }
            
            private void valueChanged() {
                update();
            }
        });
        
        amountLabel = new JLabel();
        amountLabel.setToolTipText("<html><b>Amount of words shown in list");
        amountLabel.setFont(font);
        amountLabel.setVerticalAlignment(JLabel.CENTER);
        amountLabel.setForeground(Color.GRAY);
        
        JPanel textFieldControlPanel = new JPanel();
        BoxLayout bl = new BoxLayout(textFieldControlPanel, BoxLayout.X_AXIS);
        textFieldControlPanel.setLayout(bl);
        textFieldControlPanel.setAlignmentY(JPanel.CENTER_ALIGNMENT);
//        textFieldControlPanel.setPreferredSize(new Dimension(100, 20));
        textFieldControlPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        textFieldControlPanel.setBackground(Color.WHITE);
        textFieldPanel.add(textFieldControlPanel, BorderLayout.EAST);
        textFieldControlPanel.add(amountLabel);
        textFieldControlPanel.add(Box.createRigidArea(new Dimension(5,0)));
        
        JButton clearButton = createStyledButton("clear.png");
        clearButton.setToolTipText("<html><b>Clear text field");
        clearButton.setPreferredSize(new Dimension(30, 20));
        clearButton.setBorderPainted(false);
        clearButton.setContentAreaFilled(false);
        clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        textFieldControlPanel.add(clearButton);
        
        clearButton.addActionListener((ActionEvent e) -> {
            if (!textField.getText().isEmpty()) {
                textField.setText("");
                textField.requestFocusInWindow();
            }
        });
        
        JPanel buttonPanel = new JPanel(
            new FlowLayout(FlowLayout.LEADING, 3, 0));
        buttonPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        upperPanel.add(buttonPanel, BorderLayout.EAST);
        
        JButton addButton = createStyledButton("add.png");
        addButton.setPreferredSize(new Dimension(33, 33));
        addButton.setToolTipText("<html><b>Add new word");
        buttonPanel.add(addButton);
        
        JButton newBundleButton = createStyledButton("new.png");
        newBundleButton.setPreferredSize(new Dimension(33, 33));
        newBundleButton.setToolTipText("<html><b>Generate new bundle");
        buttonPanel.add(newBundleButton);
        
        createPopupMenu();
        
        listModel = new FutureListModel();
        list = new JList<>();
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setVisibleRowCount(-1);
        list.setCellRenderer(new CellRenderer());
        list.setModel(listModel);
        list.setFixedCellHeight(CELL_HEIGHT);
        
        add(upperPanel, BorderLayout.NORTH);
        add(new JScrollPane(list,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        
        JPanel lowerPanel = new JPanel(new BorderLayout());
        lowerPanel.setBorder(null);
        add(lowerPanel, BorderLayout.SOUTH);
        
        // on list double click
        list.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (listModel.sourceList.isEmpty()) return;
                
                if (e.getButton() == MouseEvent.BUTTON1
                    && e.getClickCount() == 2) {
                    String word = listModel.getElementAt(
                        list.locationToIndex(e.getPoint()));
                    increasePriority(word);
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    list.setSelectedIndex(list.locationToIndex(e.getPoint()));
                    popup.show(list, e.getX(), e.getY());
                }
                
                textField.requestFocusInWindow();
            }
        });
        
        // add action
        ActionListener addAction = (e) -> {
            String word = Utils.formatString(textField.getText());
            
            if (!word.isEmpty()) {
                int choice = JOptionPane.showConfirmDialog(
                    FutureWordsPanel.this,
                    "<html><h2>Do you really want to add<br>word «" +
                        onlyWord(word) + "» to future list?",
                    "FutureWords", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, icon);
                
                if (choice == JOptionPane.YES_OPTION) {
                    listModel.addNewFutureWord(word);
                    
                    addFutureWord(word);
                }
            }
            
            textField.requestFocusInWindow();
        };
        
        addButton.addActionListener(addAction);
        textField.addActionListener(addAction);
        
        // generate button
        newBundleButton.addActionListener(e -> generateNewBundle());
    }
    
    public void showPanel() {
        if (firstShow) {
            firstShow = false;
            listModel.setFutureWords(controller.getFutureWords());
            update();
            revalidate();
        }
    }
    
    private void update() {
        listModel.setFilter(textField.getText().trim().toLowerCase());
        setAmountLabelText();
    }
    
    // increase word priority on menu click or list click
    private void increasePriority(String word) {
        int choice = JOptionPane.showConfirmDialog(
            FutureWordsPanel.this,
            "<html><h2>Do you really want to increase<br>word «" +
                onlyWord(word) + "» priority?",
            "FutureWords", JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE, icon);
        
        if (choice == JOptionPane.YES_OPTION) addFutureWord(word);
    }
    
    // create auxiliary buttons
    private JButton createStyledButton(String imgName) {
        JButton b = new JButton();
        b.setFocusPainted(false);
        
        b.addActionListener(e -> textField.requestFocusInWindow());
        
        if (imgName != null) {
            try {
                b.setIcon(new ImageIcon(ImageIO.read(getClass()
                    .getResource("/resources/icons/" + imgName))));
            } catch (IOException ex) { }
        }
        
        return b;
    }
    
    /**
     * Request textField focus.
     */
    public void focusTextField() {
        textField.requestFocusInWindow();
        textField.selectAll();
    }
    
    // add word to future list
    private void addFutureWord(String word) {
        // clear fields
        textField.setText("");
        list.clearSelection();
        
        try {
            controller.updateFutureWords(word);
            
            PlayWav.notification();
            
            JOptionPane.showMessageDialog(FutureWordsPanel.this,
                "<html><h2>Future words have been successfully updated.",
                "Information", JOptionPane.INFORMATION_MESSAGE, icon);
        } catch (FwEmptyWordException ex) {
            textField.requestFocusInWindow();
            
            PlayWav.exclamation();
            
            JOptionPane.showMessageDialog(FutureWordsPanel.this,
                "<html><h2>Please enter any word.",
                "Warning", JOptionPane.WARNING_MESSAGE, icon);
        } catch (FwTimeoutException ex) {
            PlayWav.exclamation();
            
            JOptionPane.showMessageDialog(FutureWordsPanel.this,
                "<html><h2>You can't increase priority of the word<br>«" +
                    onlyWord(word) + "» right now.<br>Not enough days have passed.",
                "Warning", JOptionPane.WARNING_MESSAGE, icon);
        } catch (FwAlreadyUsedException ex) {
            listModel.deleteFutureWord(word);
            
            PlayWav.exclamation();
            
            JOptionPane.showMessageDialog(FutureWordsPanel.this,
                "<html><h2>The word «" + onlyWord(word) + "»<br>is already in use.",
                "Warning", JOptionPane.WARNING_MESSAGE, icon);
        }
        
        setAmountLabelText();
    }
    
    // set amount label text
    private void setAmountLabelText() {
        amountLabel.setText("" + listModel.getSize());
    }
    
    // cell renderer for suggestion list
    private static class CellRenderer implements ListCellRenderer<String> {
        
        private final JLabel label;
        
        public CellRenderer() {
            label = new JLabel();
            label.setFont(font);
            label.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        }
        
        @Override
        public Component getListCellRendererComponent(
            JList<? extends String> list, String value, int index,
            boolean isSelected, boolean cellHasFocus) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            
            label.setText(value);
            
            if (isSelected) {
                panel.setBackground(GuiUtils.SELECTION_COLOR);
                label.setForeground(Color.WHITE);
            } else {
                label.setForeground(list.getForeground());
                panel.setBackground(index % 2 == 0
                    ? GuiUtils.EVEN_COLOR : GuiUtils.ODD_COLOR);
            }
            
            panel.add(label);
            return panel;
        }
    }
    
    private static class FutureListModel extends AbstractListModel<String> {
        
        private final List<String> sourceList;
        private final List<Integer> indices;
        
        private String filterString = "";
        
        public FutureListModel() {
            sourceList = new ArrayList<>();
            indices = new ArrayList<>();
        }
        
        public void setFutureWords(Collection<String> futureWords) {
            sourceList.clear();
            indices.clear();
            sourceList.addAll(futureWords);
        }
        
        @Override
        public int getSize() {
            return filterString.isEmpty() ? sourceList.size() : indices.size();
        }
        
        @Override
        public String getElementAt(int index) {
            return filterString.isEmpty() ? sourceList.get(index) :
                sourceList.get(indices.get(index));
        }
        
        /**
         * Set filter and update list contents.
         * @param str new filter string
         */
        public void setFilter(String str) {
            filterString = str;
            indices.clear();
            
            for (int i = 0; i < sourceList.size(); i++) {
                if (sourceList.get(i).contains(filterString)) indices.add(i);
            }
            
            fireContentsChanged(this, 0, getSize() - 1);
        }
        
        /**
         * Add new word and sort list if words doesn't exist.
         * @param newWord word to add
         */
        public void addNewFutureWord(String newWord) {
            int index = Collections.binarySearch(sourceList, newWord);
            if (index < 0) {
                sourceList.add(-index - 1, newWord);
                fireContentsChanged(this, 0, getSize() - 1);
            }
        }
        
        /**
         * Delete new future word from source list.
         * @param word word to delete
         */
        public void deleteFutureWord(String word) {
            int index = Collections.binarySearch(sourceList, word);
            sourceList.remove(index);
            fireContentsChanged(this, 0, getSize() - 1);
        }
    }
    
    private void createPopupMenu() {
        popup = new JPopupMenu();
        
        JMenuItem deleteItem = new JMenuItem(new AbstractAction("Delete word") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                String word = list.getSelectedValue();
                System.err.println(word);
                
                int choice = JOptionPane.showConfirmDialog(FutureWordsPanel.this,
                    "<html><h2>Do you really want to delete word<br>«" +
                        onlyWord(word) + "»?", "FutureWords",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, icon);
                
                if (choice != JOptionPane.YES_OPTION) return;
                
                textField.setText("");
                list.clearSelection();
                listModel.deleteFutureWord(word);
                controller.deleteFutureWords(Arrays.asList(word));
                setAmountLabelText();
                
                PlayWav.notification();
                
                JOptionPane.showMessageDialog(FutureWordsPanel.this,
                    "<html><h2>Word «" + onlyWord(word) +
                        "» has been<br>successfully deleted!",
                    "FutureWords", JOptionPane.INFORMATION_MESSAGE, icon);
            }
        });
        popup.add(deleteItem);
        
        popup.add(new JMenuItem(new AbstractAction("Increase priority") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                increasePriority(list.getSelectedValue());
            }
        }));
    }
    
    private void generateNewBundle() {
        int max = Controller.MAX_GENERATED_NEW_WORDS;
        int n = (max - 10) / 5 + 1;
        
        Integer[] values = new Integer[n];
        for (int i = 0; i < n; i++) {
            values[i] = 10 + i * 5;
        }
        
        Object choice = JOptionPane.showInputDialog(this,
            "<html><h2>Amount of words to generate:",
            "Generate new bundle", JOptionPane.INFORMATION_MESSAGE, icon,
            values, values[values.length / 2]);
        
        if (choice != null) {
            int amount = Integer.parseInt(choice.toString());
            
            List<Word> newBundleList = controller.generateNewBundle(amount);
            if (!newBundleList.isEmpty()) {
                newBundleList.stream().forEach(
                    word -> listModel.deleteFutureWord(word.getWord()));
                
                setAmountLabelText();
                
                PlayWav.notification();
                
                JOptionPane.showMessageDialog(FutureWordsPanel.this,
                    "<html><h2>New bundle has been created.<br>"
                        + "Please restart the application.",
                    "FutureWords", JOptionPane.INFORMATION_MESSAGE, icon);
            } else {
                PlayWav.exclamation();
                
                JOptionPane.showMessageDialog(FutureWordsPanel.this,
                    "<html><h2>Unable to create a new bundle.",
                    "FutureWords", JOptionPane.WARNING_MESSAGE, icon);
            }
        }
        
        textField.requestFocusInWindow();
        textField.setText("");
    }
    
    private String onlyWord(String word) {
        System.err.println(word.split(" - ")[0]);
        return word.split(" - ")[0];
    }
}
