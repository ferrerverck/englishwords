package com.words.gui.dialogs;

import com.words.controller.Controller;
import com.words.controller.sound.PlayWav;
import com.words.controller.utils.DateTimeUtils;
import com.words.controller.words.Word;
import com.words.controller.utils.Utils;
import com.words.controller.words.WordFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

/**
 * Dialog to edit a word.
 * Does not allow to change bundle (date).
 * @author vlad
 */
public final class EditWordDialog extends WordDialog {
    
    private Word originalWord; // copy of original word to save initial state
    private Word changedWord;
    
    public EditWordDialog(ImageIcon icon, Controller contr) {
        super(icon, contr);
        setTitle("Word edit dialog");
        
        bundleField.setEnabled(false);
        // bundleField.setFont(bundleField.getFont().deriveFont(Font.BOLD));
        
        resetButton.addActionListener(e -> {
            wordField.setText(originalWord.getWord());
            translationField.setText(originalWord.getTranslation());
            synonymsField.setText(originalWord.getSynonyms());
            bundleField.setText(DateTimeUtils.localDateToString(
                originalWord.getBundle()));
            synonymsField.requestFocusInWindow();
        });
    }
    
    @Override
    protected void confirmAction() {
        userAction = UserAction.CANCEL;
        if (!emptyCheck()) return;
        
        changedWord.setWord(Utils.formatString(wordField.getText()));
        changedWord.setTranslation(Utils.formatString(translationField.getText()));
        changedWord.setSynonyms(Utils.formatString(synonymsField.getText()));
        changedWord.setBundle(DateTimeUtils.parseDate(bundleField.getText()));
        
        if ((changedWord.getWord().equals(originalWord.getWord())) &&
            (changedWord.getTranslation().equals(originalWord.getTranslation())) &&
            (changedWord.getSynonyms().equals(originalWord.getSynonyms()))) {
            userAction = UserAction.NO_CHANGES;
            
            PlayWav.exclamation();
            JOptionPane.showMessageDialog(EditWordDialog.this,
                "<html><h2>Please change something in the word.",
                "Warning", JOptionPane.WARNING_MESSAGE, appIcon);
            
            wordField.requestFocusInWindow();
            wordField.selectAll();
            
            return;
        }
        
        // checks if such word is already exists and has different date
//        Word otherWord = controller.getWordInstance(changedWord.getWord());
//        if (otherWord != null && otherWord != wordToEdit) {
//            PlayWav.exclamation();
//            JOptionPane.showMessageDialog(EditWordDialog.this,
//                "<html><h2>The word «" + changedWord.getWord() +
//                    "» already exists.",
//                "Warning", JOptionPane.WARNING_MESSAGE, appIcon);
//            
//            wordField.requestFocusInWindow();
//            wordField.selectAll();
//            
//            return;
//        }
        
        userAction = UserAction.OK;
        setVisible(false);
    }
    
    public boolean showDialog(Word word) {
        return showDialog(word, null);
    }
    
    public boolean showDialog(Word word, JComponent parent) {
        setLocationRelativeTo(parent);
        setTitle("Word edit dialog for «" + word.getWord() + "»");
        
        originalWord = WordFactory.copyWord(word);
        changedWord = word;
        resetButton.doClick();
        
        setVisible(true);
        
        return userAction == UserAction.OK;
    }
    
    public Word getChangedWord() {
        return changedWord;
    }
    
    public Word getOriginalWord() { return originalWord; }
}
