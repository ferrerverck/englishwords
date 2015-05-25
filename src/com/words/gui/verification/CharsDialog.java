package com.words.gui.verification;

import com.sun.glass.events.KeyEvent;
import com.words.controller.sound.PlayWav;
import com.words.controller.words.Word;
import com.words.controller.words.WordFactory;
import com.words.gui.guiutils.GuiUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class CharsDialog extends JDialog {
    
    private static final String EMPTY = "";
    
    private static final int SIZE = 50;
    private static final Dimension CHAR_SIZE = new Dimension(SIZE, SIZE);
    
    private static final Font FONT = new Font(Font.MONOSPACED, Font.PLAIN, 22);
    private static final Border DEFAULT_BORDER = new LineBorder(Color.LIGHT_GRAY, 2);
    
    public static final DataFlavor SUPPORTED_DATE_FLAVOR =
//            new DataFlavor(CharComponent.class, "CharComponent");
        new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + "; class=\"" +
            CharComponent.class.getName() + "\"", "CharComponent");
    
    private static final int BORDER = 10;
    
    private final ImageIcon icon;
    private final JPanel rootPanel;
    
    private final String englishWord;
    private final List<String> chars;
    
    private final CharComponent[] answer;
    private final CharComponent[] suggestion;
    
    
    public static void main(String[] args) {
        try {
            UIManager.LookAndFeelInfo[] lafi
                = UIManager.getInstalledLookAndFeels();
            UIManager.setLookAndFeel(lafi[new Random().nextInt(lafi.length)]
                .getClassName());
//            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
//            UIManager.setLookAndFeel("javax.swing.plaf.metal");
        } catch (Exception ex) { }
        
        SwingUtilities.invokeLater(() -> {
            Word word = WordFactory.newWord();
            word.setWord("defer to");
            word.setTranslation("положиться на кого-л.; откладывать");
            
            JDialog dialog = new CharsDialog(word,
                new ImageIcon(VerificationDialog.class.getResource("/resources/icons/icon64.png")));
            dialog.setVisible(true);
        });
    }
    
    public CharsDialog(Word word, ImageIcon icon) {
        this.icon = icon;
        
        this.englishWord = word.getWord().replaceAll("^to ", "");
        chars = Arrays.asList(englishWord.split(""));
        Collections.shuffle(chars);
        
        answer = new CharComponent[chars.size()];
        suggestion = new CharComponent[chars.size()];
        
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        setTitle("Enter your answer");
        setModal(true);
        setAlwaysOnTop(true);
        
        rootPanel = new JPanel();
        rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.Y_AXIS));
        rootPanel.setBorder(new EmptyBorder(BORDER, BORDER, 0, BORDER));
        add(rootPanel);
        
        JLabel translationLabel = new JLabel("Translation:", JLabel.CENTER);
        translationLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        
        JLabel wordLabel = new JLabel(word.getTranslation(),
            JLabel.CENTER);
        wordLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        
        JPanel targetPanel = new JPanel(new FlowLayout());
        
        JLabel suggestionLabel = new JLabel(
            "Press or drag buttons with characters into appropriate places",
            JLabel.CENTER);
        suggestionLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        
        JPanel suggestionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        
        // init rows
        for (int i = 0; i < englishWord.length(); i++) {
            CharComponent cc1 = new CharComponent();
            cc1.getButton().addActionListener(e -> {
                String ch = cc1.getChar();
                cc1.clear();
                addCharToSuggestion(ch);
            });
            answer[i] = cc1;
            targetPanel.add(cc1);
            
            CharComponent cc2 = new CharComponent(chars.get(i));
            cc2.getButton().addActionListener(e -> {
                String ch = cc2.getChar();
                cc2.clear();
                addCharToAnswer(ch);
            });
            suggestion[i] = cc2;
            suggestionPanel.add(cc2);
        }
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton okButton = createButton("OK", null);
        JButton cancelButton = createButton("Cancel", null);
        JButton resetButton = createButton("Reset", e -> reset());
        
        GuiUtils.setSameButtonSize(okButton, cancelButton, resetButton);
        
//        buttonPanel.add(okButton);
//        buttonPanel.add(cancelButton);
        buttonPanel.add(resetButton);
        
        addKeyHandlers();
        
        rootPanel.add(suggestionLabel);
        rootPanel.add(suggestionPanel);
        rootPanel.add(translationLabel);
        rootPanel.add(wordLabel);
        rootPanel.add(targetPanel);
        rootPanel.add(buttonPanel);
        
        pack();
        setMinimumSize(getSize());
        
        setLocationRelativeTo(null);
    }
    
    private JButton createButton(String text, ActionListener a) {
        JButton button = new JButton(text);
        button.addActionListener(a);
        button.setFocusPainted(false);
        button.setFocusable(false);
        return button;
    }
    
    private void addCharToAnswer(String ch) {
        int i = 0;
        while (!answer[i].isEmpty()) i++;
        answer[i].setChar(ch);
        
        checkAnswer();
    }
    
    private void addCharToSuggestion(String ch) {
        int i = 0;
        while (!suggestion[i].isEmpty()) i++;
        suggestion[i].setChar(ch);
    }
    
    private void reset() {
        for (CharComponent cc : answer) cc.clear();
        
        for (int i = 0; i < chars.size(); i++) {
            suggestion[i].setChar(chars.get(i));
        }
    }
    
    private void checkAnswer() {
        StringBuilder ans = new StringBuilder(englishWord.length());
        for (int i = 0; i < englishWord.length(); i++) {
            if (answer[i].isEmpty()) return;
            ans.append(answer[i].getChar());
        }
        
        if (englishWord.equals(ans.toString())) {
            PlayWav.notification();
            this.setVisible(false);
            
            JOptionPane.showMessageDialog(null,
                "<html><h2>Correct answer!",
                "Correct", JOptionPane.OK_OPTION, icon);
        } else {
            PlayWav.exclamation();
        }
    }
    
    // listens to all key presses and allows for faster solving
    private void addKeyHandlers() {
        class KeyHandle extends AbstractAction {
            
            private final String pressedChar;
            
            public KeyHandle(char c) { pressedChar = "" + c; }
            
            @Override
            public void actionPerformed(ActionEvent e) {
                for (CharComponent cc : suggestion) {
                    if (cc.getChar().equals(pressedChar)) {
                        cc.getButton().doClick();
                        break;
                    }
                }
            }
        }
        
        InputMap inputMap = getRootPane().getInputMap(
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();
        
        for (char c = 'a'; c <= 'z'; c++) {
            KeyHandle action = new KeyHandle(c);
            inputMap.put(KeyStroke.getKeyStroke(c), c);
            inputMap.put(KeyStroke.getKeyStroke(Character.toUpperCase(c)), c);
            actionMap.put(c, action);
        }
        
        // whitespace
        char c = ' ';
        KeyHandle action = new KeyHandle(c);
        inputMap.put(KeyStroke.getKeyStroke(c), c);
        actionMap.put(c, action);
        
        // backspace
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACKSPACE, 0),
            "backspace");
        actionMap.put("backspace", new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = answer.length - 1; i >= 0; i--) {
                    if (!answer[i].isEmpty()) {
                        answer[i].getButton().doClick();
                        break;
                    }
                }
            }
        });
    }
    
    private class CharTransferHandler extends TransferHandler {
        
        private final CharComponent cc;
        
        public CharTransferHandler(CharComponent cc) {
            this.cc = cc;
        }
        
        @Override
        public int getSourceActions(JComponent c) {
            return DnDConstants.ACTION_COPY_OR_MOVE;
        }
        
        @Override
        protected Transferable createTransferable(JComponent c) {
            Transferable t = new Transferable() {
                
                @Override
                public DataFlavor[] getTransferDataFlavors() {
                    return new DataFlavor[] {SUPPORTED_DATE_FLAVOR};
                }
                
                @Override
                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    return getTransferDataFlavors()[0] == flavor;
                }
                
                @Override
                public Object getTransferData(DataFlavor flavor) throws
                    UnsupportedFlavorException, IOException {
                    if (isDataFlavorSupported(flavor)) return cc;
                    else throw new UnsupportedFlavorException(flavor);
                }
            };
            
            return t;
        }
        
        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            super.exportDone(source, data, action);
            checkAnswer();
        }
        
        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            return support.isDataFlavorSupported(SUPPORTED_DATE_FLAVOR);
        }
        
        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            boolean accept = false;
            
            if (canImport(support)) {
                try {
                    Transferable t = support.getTransferable();
                    Object value = t.getTransferData(SUPPORTED_DATE_FLAVOR);
                    
                    if (value instanceof CharComponent) {
                        CharComponent source = (CharComponent) value;
                        String distChar = cc.getChar();
                        
                        cc.setChar(source.getChar());
                        source.setChar(distChar);
                        
                        accept = true;
                    }
                } catch (UnsupportedFlavorException | IOException exp) { }
            }
            
            return accept;
        }
    }
    
    public class CharComponent extends JPanel implements Serializable {
        
        private String ch;
        
        private final JButton button;
        
        public CharComponent() {
            setPreferredSize(CHAR_SIZE);
            setOpaque(true);
            setBackground(Color.LIGHT_GRAY);
            
            setLayout(new BorderLayout());
            
            button = new JButton();
            button.setPreferredSize(CHAR_SIZE);
            button.setFocusPainted(false);
            button.setFont(FONT);
            button.addActionListener(e -> clear());
            button.setFocusable(false);
            add(button, BorderLayout.CENTER);
            
            CharTransferHandler cth = new CharTransferHandler(this);
            setTransferHandler(cth);
            button.setTransferHandler(cth);
            
            button.addActionListener(e -> button.setVisible(false));
            
            button.addMouseMotionListener(new MouseAdapter() {
                
                @Override
                public void mouseDragged(MouseEvent e) {
                    button.getTransferHandler().exportAsDrag(
                        button, e, TransferHandler.COPY);
                    
                    // done to prevent button staying pressed in metal l&f
                    button.setEnabled(false);
                    button.setEnabled(true);
                }
            });
            
            clear();
        }
        
        public CharComponent(String ch) {
            this();
            setChar(ch);
        }
        
        public final void clear() {
            this.ch = EMPTY;
            
//            setOpaque(true);
            setBorder(DEFAULT_BORDER);
            
            button.setVisible(false);
            button.setText("");
        }
        
        public final void setChar(String ch) {
            this.ch = ch;
            
            if (EMPTY.equals(ch)) {
                clear();
                return;
            }
            
//            setOpaque(false);
            setBorder(null);
            
            button.setVisible(true);
            button.setText(ch);
        }
        
        public boolean isEmpty() {
            return EMPTY.equals(ch);
        }
        
        public String getChar() {
            return ch;
        }
        
        public JButton getButton() {
            return button;
        }
    }
}
