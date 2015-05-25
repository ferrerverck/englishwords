package com.words.gui;

import com.words.controller.Controller;
import com.words.controller.callbacks.ConsoleCallback;
import com.words.gui.guiutils.GuiUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.util.Collections;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

/**
 * Console to show application messages.
 * @author vlad
 */
class ConsolePanel extends JPanel {
    
    private static final Font CONSOLE_FONT =
        new Font(Font.MONOSPACED, Font.PLAIN, 18);
    
    private final JTextPane textPane;
    private final DefaultStyledDocument doc;
    private final Style mainStyle, infoStyle, errorStyle;
    
    public ConsolePanel(Controller controller) {
        setLayout(new BorderLayout());
        
        // Create the StyleContext, the document and the pane
        StyleContext sc = new StyleContext();
        doc = new DefaultStyledDocument(sc);
        textPane = new JTextPane(doc);
        
        Style defaultStyle = sc.getStyle(StyleContext.DEFAULT_STYLE);
        mainStyle = sc.addStyle("MainStyle", defaultStyle);
        // StyleConstants.setBold(mainStyle, true);
        
        infoStyle = sc.addStyle("infoStyle", mainStyle);
        StyleConstants.setForeground(infoStyle, Color.decode("#31B404"));
        
        errorStyle = sc.addStyle("errorStyle", infoStyle);
        StyleConstants.setForeground(errorStyle, Color.decode("#AA0000"));
        
        textPane.setEditable(false);
        textPane.setFocusTraversalPolicyProvider(false);
        textPane.setFont(CONSOLE_FONT);
        textPane.setBorder(new EmptyBorder(5, 10, 5, 10));
        textPane.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        
        JScrollPane scrollPane = new JScrollPane(
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().add(textPane);
        
        add(scrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        add(buttonPanel, BorderLayout.SOUTH);
        
        JButton dumpPoolButton = GuiUtils.newNotFocusableButton("Show word pool");
        JButton nextWordButton = GuiUtils.newNotFocusableButton("Next word");
        
        // callback to update textPanel
        if (controller != null) {
            nextWordButton.addActionListener(e -> controller.nextWord());
            buttonPanel.add(nextWordButton);
            
            dumpPoolButton.addActionListener(e -> controller.dumpWordPool());
            buttonPanel.add(dumpPoolButton);
            
            controller.addConsoleCallback(new ConsoleCallback() {
                @Override
                public void addWordMessage(String message) {
                    addStyledMessage(message, mainStyle);
                }
                
                @Override
                public void addInfoMessage(String message) {
                    addStyledMessage(message, infoStyle);
                }
                
                @Override
                public void addErrorMessage(String message) {
                    addStyledMessage(message, errorStyle);
                }
                
                @Override
                public void addMessage(String message) {
                    addStyledMessage(message, mainStyle);
                }
                
                @Override
                public void addEmptyLine() {
                    addStyledMessage("", mainStyle);
                }
                
                private void addStyledMessage(final String message,
                    final Style style) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            doc.insertString(doc.getLength(),
                                message + '\n', style);
                            textPane.select(0, 0);
                            textPane.setCaretPosition(doc.getLength());
                        } catch (BadLocationException ex) { }
                    });
                }
            });
        }
        
        JButton clearButton = GuiUtils.newNotFocusableButton("Clear console");
        clearButton.addActionListener(e -> textPane.setText(""));
        buttonPanel.add(clearButton);
        
        GuiUtils.setSameButtonSize(clearButton, dumpPoolButton, nextWordButton);
        
    }
}
