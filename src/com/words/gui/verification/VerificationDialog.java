package com.words.gui.verification;

import com.words.controller.sound.PlayWav;
import com.words.gui.guiutils.GuiUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

public class VerificationDialog extends JDialog {
    
    private static final int BORDER = 10;
    
    private final String answer;
    
    private final ButtonGroup buttonGroup;
    private final ImageIcon icon;
    
    public static void main(String[] args) {
        try {
            UIManager.LookAndFeelInfo[] lafi
                = UIManager.getInstalledLookAndFeels();
            UIManager.setLookAndFeel(lafi[new Random().nextInt(lafi.length)]
                .getClassName());
//            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (ClassNotFoundException | IllegalAccessException |
            InstantiationException | UnsupportedLookAndFeelException ex) {
        }
        
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new VerificationDialog(
                new ImageIcon(VerificationDialog.class.getResource("/resources/icons/icon64.png")),
                new Font(Font.DIALOG, Font.BOLD, 18),
                Arrays.asList("abc", "1", "2", "3", "4", "5"));
            dialog.setVisible(true);
        });
    }
    
    public VerificationDialog(ImageIcon icon, Font font, List<String> answers) {
        this.icon = icon;
        
        this.answer = answers.get(0);
        List<String> options = new ArrayList<>(answers);
        Collections.shuffle(options);
        
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);
        setTitle("Title here");
        setModal(true);
        setAlwaysOnTop(true);
        
        JPanel rootPanel = new JPanel(new BorderLayout(BORDER, 0));
        rootPanel.setBorder(new EmptyBorder(
            new Insets(BORDER, BORDER, BORDER, BORDER)));
        add(rootPanel);
        
        JLabel imageLabel = new JLabel("", icon, JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.TOP);
        rootPanel.add(imageLabel, BorderLayout.WEST);
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
//        contentPanel.setOpaque(true);
//        contentPanel.setBackground(Color.BLUE);
        rootPanel.add(contentPanel, BorderLayout.CENTER);
        
        JLabel wordLabel = new JLabel("Word goes here", JLabel.LEFT);
        wordLabel.setFont(font);
        contentPanel.add(wordLabel);
        contentPanel.add(Box.createVerticalStrut(BORDER));
        
        Font optionsFont = font.deriveFont(font.getSize2D() - 3);
        
        buttonGroup = new ButtonGroup();
        options.forEach(str -> {
            JRadioButton radioButton = new JRadioButton(str);
            radioButton.setBorder(new EmptyBorder(
                new Insets(2, BORDER / 2, 2, BORDER / 2)));
            radioButton.setFocusPainted(false);
            radioButton.setFont(optionsFont);
            radioButton.setForeground(GuiUtils.SELECTION_COLOR);
            radioButton.setActionCommand(str);
            
            buttonGroup.add(radioButton);
            contentPanel.add(radioButton);
        });
        
        contentPanel.add(Box.createVerticalStrut(BORDER / 2));
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(new Insets(2, 0, 0, 0), Color.LIGHT_GRAY), 
            new EmptyBorder(new Insets(BORDER / 2, 0, 0, 0))));
        
        JButton button = new JButton("OK");
        button.setFont(optionsFont);
//        button.setFocusPainted(false);
        buttonPanel.add(button);
        rootPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        button.addActionListener(e -> buttonClicked());
        
        pack();
        setLocationRelativeTo(null);
        button.requestFocusInWindow();
    }
    
    private void buttonClicked() {
        Optional<AbstractButton> optional = Collections.list(buttonGroup.getElements()).
            stream().filter(b -> b.isSelected()).findFirst();
        
        if (optional.isPresent()) {
            String chosenAnswer = optional.get().getActionCommand();
            setVisible(false);
            
            if (chosenAnswer.equals(answer)) {
                PlayWav.notification();
                
                JOptionPane.showMessageDialog(this,
                    "<html><h2>Your answer is correct.",
                    "Information", JOptionPane.OK_OPTION, icon);
            } else {
                PlayWav.exclamation();
                
                // here should be action to modify word after failed attempt
                
                JOptionPane.showMessageDialog(this,
                    "<html><h2>Your answer is not correct.<br>Correct answer is «"
                        + answer + "»!",
                    "Information", JOptionPane.OK_OPTION, icon);
            }
        } else {
            PlayWav.exclamation();
            
            JOptionPane.showMessageDialog(this,
                "<html><h2>Please select any option!",
                "Information", JOptionPane.OK_OPTION, icon);
        }
    }
}
