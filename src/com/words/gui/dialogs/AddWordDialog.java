package com.words.gui.dialogs;

import com.words.controller.Controller;
import com.words.controller.sound.PlayWav;
import com.words.controller.utils.DateTimeUtils;
import com.words.controller.words.Word;
import com.words.controller.utils.Utils;
import com.words.controller.words.WordFactory;
import java.awt.event.ActionEvent;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 * @author vlad
 */
public class AddWordDialog extends WordDialog {
    
    private Word newWord;
    
    public AddWordDialog(ImageIcon icon, Controller contr) {
        super(icon, contr);
        
        resetButton.addActionListener((ActionEvent e) -> {
            wordField.setText("");
            translationField.setText("");
            synonymsField.setText("");
            bundleField.setText(DateTimeUtils.localDateToString(
                controller.getLastBundleName()));
        });
        
        correctLabelText(wordLabel, "required");
        correctLabelText(translationLabel, "required");
        correctLabelText(synonymsLabel, "optional");
        correctLabelText(bundleLabel, "required");
        
        setTitle("New word");
    }
    
    private void correctLabelText(JLabel label, String append) {
        label.setText(label.getText().replaceAll(":$", " (" + append + "):"));
    }
    
    public boolean showDialog() {
        userAction = UserAction.CANCEL;
        newWord = null;
        resetButton.doClick();
        setLocationRelativeTo(null);
        setVisible(true);
        return userAction == UserAction.OK;
    }
    
    @Override
    protected void confirmAction() {
        userAction = UserAction.CANCEL;
        if (!emptyCheck()) return;
        
        newWord = WordFactory.newWord();
        newWord.setWord(wordField.getText());
        newWord.setTranslation(translationField.getText());
        newWord.setSynonyms(
            Utils.trimTrailingPunctuation(synonymsField.getText()));
        newWord.setBundle(DateTimeUtils.parseDate(bundleField.getText()));
        
        if (controller.wordExists(newWord.getWord())) {
            PlayWav.exclamation();
            JOptionPane.showMessageDialog(AddWordDialog.this,
                "<html><h2>The word «" + newWord.getWord() + "» already exists.",
                "Warning", JOptionPane.WARNING_MESSAGE, appIcon);
            wordField.requestFocusInWindow();
            wordField.selectAll();
            return;
        }
        
        userAction = UserAction.OK;
        setVisible(false);
    }
    
    public Word getNewWord() {
        return newWord;
    }
}
