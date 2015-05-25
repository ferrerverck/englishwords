package com.words.gui.dialogs;

import com.words.controller.utils.DateTimeUtils;
import com.words.controller.utils.Utils;
import com.words.controller.words.Word;
import com.words.controller.words.WordFactory;
import com.words.gui.guiutils.ComplexityGuiUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

/**
 * This class represents hint dialog for gui application.
 * It is used as a replacement for JOptionPane with html editing.
 * Contains a bug because of native implementation of look&feels.
 * Can't set background color for definition TextArea.
 * @author vlad
 */
public class HintDialog {
    
    private static class CustomDialog extends JDialog {
        
        private static final int FONT_SIZE = 17;
        public static final Font FONT =
            new Font(Font.DIALOG, Font.BOLD, FONT_SIZE);
        public static final Font SECONDARY_FONT =
            FONT.deriveFont(Font.PLAIN, FONT_SIZE - 2);
        
        public static final int BORDER_WIDTH = 6;
        public static final int MAX_ROWS_FOR_DEFINITION = 30;
        
        private final AbstractAction closeDialogAction = new AbstractAction("close") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        };
        
        public CustomDialog(Word word, ImageIcon icon, String definition) {
            setModal(true);
            setAlwaysOnTop(true);
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            setTitle("Hint dialog");
            setResizable(false);
            
            JPanel rootPanel = new JPanel();
            rootPanel.setLayout(new BorderLayout(BORDER_WIDTH, BORDER_WIDTH));
            rootPanel.setBorder(new EmptyBorder(BORDER_WIDTH, BORDER_WIDTH,
                BORDER_WIDTH, BORDER_WIDTH));
            add(rootPanel);
            
            JLabel iconLabel = new JLabel();
            iconLabel.setIcon(icon);
            iconLabel.setVerticalAlignment(JLabel.TOP);
            rootPanel.add(iconLabel, BorderLayout.WEST);
            
            JPanel centerPanel = new JPanel();
            centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
            rootPanel.add(centerPanel, BorderLayout.CENTER);
            
            JPanel wordPanel = new JPanel(new BorderLayout(0, 0));
            wordPanel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
            centerPanel.add(wordPanel);
            
            JLabel wordLabel = new JLabel(word.getWord());
            wordLabel.setIcon(ComplexityGuiUtils.getIcon(word.getComplexity()));
            wordLabel.setIconTextGap(2);
            wordLabel.setFont(FONT);
            wordLabel.setForeground(Color.BLACK);
            wordPanel.add(wordLabel, BorderLayout.WEST);
            
            JLabel bundleLabel = new JLabel(
                DateTimeUtils.localDateToString(word.getBundle()));
            bundleLabel.setBorder(new EmptyBorder(0, 10, 0, 0));
            bundleLabel.setFont(SECONDARY_FONT);
            wordPanel.add(bundleLabel, BorderLayout.EAST);
            
            JLabel translationLabel = new JLabel(word.getTranslation());
            translationLabel.setFont(FONT);
            translationLabel.setForeground(Color.decode("#AA0000"));
            translationLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
            centerPanel.add(translationLabel);
            
            if (!word.getSynonyms().isEmpty()) {
                JLabel synsLabel = new JLabel(word.getSynonyms());
                synsLabel.setFont(FONT);
                synsLabel.setForeground(Color.decode("#04B4AE"));
                synsLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
                centerPanel.add(synsLabel);
            }
            
            if (definition != null) {
                int lines = definition.length() -
                    definition.replace("\n", "").length() + 1;
                
                JTextArea defArea = new JTextArea(formatDefinition(definition));
                defArea.setFont(FONT);
                defArea.setForeground(Color.decode("#0000FF"));
                defArea.setBackground(this.getBackground());
                defArea.setEditable(false);
                defArea.setBorder(new EmptyBorder(5, 5, 5, 5));
                defArea.setCursor(Cursor.getPredefinedCursor(
                    Cursor.TEXT_CURSOR));
                
                JScrollPane scroll = new JScrollPane(defArea,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                scroll.setAlignmentX(JLabel.LEFT_ALIGNMENT);
                
                if (lines > MAX_ROWS_FOR_DEFINITION) {
                    defArea.setRows(MAX_ROWS_FOR_DEFINITION);
                }
                
                centerPanel.add(scroll);
            }
            
            centerPanel.add(Box.createVerticalStrut(5));
            
            JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
            bottomPanel.setBorder(null);
            bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, Color.GRAY),
                new EmptyBorder(BORDER_WIDTH / 2, 0, 0, 0)));
            bottomPanel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
            centerPanel.add(bottomPanel);
            
            JButton okButton = new JButton("OK");
            okButton.setFocusPainted(false);
            okButton.addActionListener(closeDialogAction);
            bottomPanel.add(okButton, BorderLayout.EAST);
            
            String delimiter = ", ";
            String timesStr = word.getTimesPicked() == 1 ? "1 time" : 
                word.getTimesPicked() + " times";
            JLabel statsLabel = new JLabel("Picked " +
                word.getLastPickedString() + delimiter + timesStr);
            statsLabel.setFont(SECONDARY_FONT);
            statsLabel.setBorder(null);
            bottomPanel.add(statsLabel, BorderLayout.CENTER);
            
            addHotkeys();
            
            pack();
            setLocationRelativeTo(null);
            
            okButton.requestFocusInWindow();
        }
        
        private String formatDefinition(String definition) {
            // replace &nbsp
            definition = definition.replaceAll("\u00A0", " ");
            
            StringBuilder toReplace = new StringBuilder("\n");
            for (int i = 0; i < definition.length(); i++) {
                char ch = definition.charAt(i);
                if (Character.isSpaceChar(ch)) toReplace.append(ch);
                else break;
            }
            
            return definition.replaceAll(toReplace.toString(), "\n").trim();
        }
        
        private void addHotkeys() {
            InputMap inputMap = getRootPane().getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap actionMap = getRootPane().getActionMap();
            
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ESC");
            actionMap.put("ESC", closeDialogAction);
            
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "Space");
            actionMap.put("Space", closeDialogAction);
        }
    }
    
    public static void main(String[] args) {
        Word word = WordFactory.newWord();
        word.setWord("to a lesser extent");
        word.setTranslation("в меньшей степени");
        word.setSynonyms("less then");
        word.setBundle(LocalDate.now());
        
        String definition = "    v 1: take in solid food; \"She was eating a banana\"; \"What did" +
            "\n" + "         you eat for dinner last night?\"" +
            "\n" +  "    2: eat a meal; take a meal; \"We did not eat until 10 P.M." +
            "\n" +  "       because there were so many phone calls\"; \"I didn't eat yet," +
            "\n" +  "       so I gladly accept your invitation\"" +
            "\n" +  "    3: take in food; used of animals only; \"This dog doesn't eat" +
            "\n" +  "       certain kinds of meat\"; \"What do whales eat?\" [syn: {feed}," +
            "\n" +  "       {eat}]" +
            "\n" +  "    4: worry or cause anxiety in a persistent way; \"What's eating" +
            "\n" +  "       you?\" [syn: {eat}, {eat on}]" +
            "\n" +  "    5: use up (resources or materials); \"this car consumes a lot of" +
            "\n" +  "       gas\"; \"We exhausted our savings\"; \"They run through 20" +
            "\n" +  "       bottles of wine a week\" [syn: {consume}, {eat up}, {use up}," +
            "\n" +  "       {eat}, {deplete}, {exhaust}, {run through}, {wipe out}]" +
            "\n" +  "    6: cause to deteriorate due to the action of water, air, or an" +
            "\n" +  "       acid; \"The acid corroded the metal\"; \"The steady dripping of" +
            "\n" +  "       water rusted the metal stopper in the sink\" [syn: {corrode}," +
            "\n" +  "       {eat}, {rust}]             ";
        
        ImageIcon icon = new ImageIcon(WordDialog.class
            .getResource("/resources/icons/icon64.png"));
        
        showDialog(word, icon, definition);
    }
    
    public static void showDialog(Word word, ImageIcon icon, String definition) {
        JDialog dialog = new CustomDialog(word, icon, definition);
        dialog.setVisible(true);
    }
}
