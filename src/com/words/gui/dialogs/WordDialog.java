package com.words.gui.dialogs;

import com.words.controller.Controller;
import com.words.controller.sound.PlayWav;
import com.words.controller.words.Word;
import com.words.gui.guiutils.GuiUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

public abstract class WordDialog extends JDialog {
    
    public enum UserAction {
        OK, CANCEL, NO_CHANGES;
    }
    
    public static final Font FONT = new Font(Font.DIALOG, Font.BOLD, 14);
    public static final int BORDER_WIDTH = 8;
    
    protected final Controller controller;
    
    protected final ImageIcon appIcon;
    protected final JLabel imageLabel;
    protected final JTextField wordField, translationField, synonymsField,
        bundleField;
    protected final JLabel wordLabel, translationLabel, synonymsLabel,
        bundleLabel;
    protected final JButton resetButton;
    
    protected UserAction userAction = UserAction.CANCEL;
    
    public static void main(String[] args) {
        ImageIcon icon = new ImageIcon(WordDialog.class
            .getResource("/resources/icons/icon64.png"));
        WordDialog sd = new EditWordDialog(icon, null);
        sd.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        sd.setVisible(true);
    }
    
    // okButton action with all checks
    protected abstract void confirmAction();
    
    public WordDialog(ImageIcon icon, Controller contr) {
        this.controller = contr;
        appIcon = icon;
        
        setModal(true);
        setMinimumSize(new Dimension(500, 340));
        setPreferredSize(new Dimension(500, 340));
//        setResizable(false);
        setAlwaysOnTop(true);
        
        JPanel rootPanel = new JPanel();
        rootPanel.setLayout(new BorderLayout(5, 5));
        rootPanel.setBorder(new EmptyBorder(BORDER_WIDTH, BORDER_WIDTH,
            BORDER_WIDTH, BORDER_WIDTH));
        add(rootPanel);
        
        imageLabel = new JLabel();
        imageLabel.setIcon(appIcon);
        imageLabel.setVerticalAlignment(JLabel.TOP);
        rootPanel.add(imageLabel, BorderLayout.WEST);
        
        JPanel centerPanel = new JPanel();
        rootPanel.add(centerPanel, BorderLayout.CENTER);
        centerPanel.setBorder(new EmptyBorder(0, 5, 0, 0));
        centerPanel.setLayout(new GridBagLayout());
        
        Insets afterLabel = new Insets(0, 0, 0, 0);
        Insets afterField = new Insets(0, 0, 6, 0);
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 0;
        c.insets = afterLabel;
        wordLabel = createLabel("Word");
        centerPanel.add(wordLabel, c);
        
        c.gridx = 0;
        c.gridy = 1;
        c.insets = afterField;
        wordField = createTextField("word");
        centerPanel.add(wordField, c);
        
        c.gridx = 0;
        c.gridy = 2;
        c.weighty = 0;
        c.insets = afterLabel;
        translationLabel = createLabel("Translation");
        centerPanel.add(translationLabel, c);
        
        c.gridx = 0;
        c.gridy = 3;
        c.insets = afterField;
        centerPanel.add(translationField = createTextField("translation"), c);
        
        c.gridx = 0;
        c.gridy = 4;
        c.weighty = 0;
        c.insets = afterLabel;
        synonymsLabel = createLabel(Word.THIRD_FIELD_DESCRIPTION);
        centerPanel.add(synonymsLabel, c);
        
        c.gridx = 0;
        c.gridy = 5;
        c.insets = afterField;
        centerPanel.add(synonymsField = createTextField(
            Word.THIRD_FIELD_DESCRIPTION.toLowerCase()), c);
        
        c.gridx = 0;
        c.gridy = 6;
        c.weighty = 0;
        c.insets = afterLabel;
        bundleLabel = createLabel("Date");
        centerPanel.add(bundleLabel, c);
        
        c.gridx = 0;
        c.gridy = 7;
        c.weightx = 0;
        c.weighty = 0;
        c.insets = afterField;
        bundleField = createTextField("bundle");
        centerPanel.add(bundleField, c);
        
        c.gridx = 0;
        c.gridy = 8;
        c.weighty = 1;
        c.insets = afterLabel;
        c.anchor = GridBagConstraints.LAST_LINE_END;
        c.fill = GridBagConstraints.NONE;
        resetButton = new JButton("Reset");
        resetButton.setToolTipText("<html><b>Reset fields");
        
        centerPanel.add(resetButton, c);
        
        JPanel bottomPanel = new JPanel(new FlowLayout(
            FlowLayout.TRAILING, 0, 0));
        rootPanel.add(bottomPanel, BorderLayout.SOUTH);
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
            new EmptyBorder(5, 0, 0, 0)));
        
        JButton cancelButton = new JButton(cancelAction);
        JButton okButton = new JButton(okAction);
        
        GuiUtils.setSameButtonSize(cancelButton, okButton, resetButton);
        
        bottomPanel.add(okButton);
        bottomPanel.add(Box.createHorizontalStrut(5));
        bottomPanel.add(cancelButton);
        
        InputMap inputMap = getRootPane().getInputMap(
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(
            KeyEvent.VK_ESCAPE, 0), "hideDialog");
        actionMap.put("hideDialog", cancelAction);
        
        pack();
        setLocationRelativeTo(null);
    }
    
    /*
    * Create similar styled labels with unique text
    * @param text text for the newly created label
    */
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text + ':');
        label.setFont(FONT);
        return label;
    }
    
    // create similar styled text fields
    private JTextField createTextField(String fieldName) {
        JTextField textField = new JTextField(15);
        textField.setDisabledTextColor(Color.RED);
        textField.setName(fieldName);
        textField.setFont(FONT.deriveFont(Font.PLAIN));
        textField.setBorder(BorderFactory.createCompoundBorder(
            textField.getBorder(), new EmptyBorder(2, 4, 2, 4)));
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE,
            textField.getPreferredSize().height));
        textField.addActionListener(okAction);
        textField.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        return textField;
    }
    
    private final Action cancelAction = new AbstractAction("Cancel") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            userAction = UserAction.CANCEL;
            setVisible(false);
        }
    };
    
    private final Action okAction = new AbstractAction("OK") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            confirmAction();
        }
    };
    
    // Checks if contents of required text fields are empty.
    // Returns true if test passes.
    protected boolean emptyCheck() {
        JTextField[] fields = {wordField, translationField, bundleField};
        for (JTextField textField : fields) {
            if (textField.getText().isEmpty()) {
                showEmptyWarning(textField);
                return false;
            }
        }
        return true;
    }
    
    // show warning message if required field is empty
    private void showEmptyWarning(JTextField textField) {
        PlayWav.exclamation();
        JOptionPane.showMessageDialog(WordDialog.this,
            "<html><h2>Field with " + textField.getName() + " can't be empty.",
            "Warning", JOptionPane.WARNING_MESSAGE, appIcon);
        textField.requestFocusInWindow();
        textField.selectAll();
    }
}
