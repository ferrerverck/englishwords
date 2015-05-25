package com.words.controller.callbacks;

/**
 * Console callback to post messages to gui console.
 * @author vlad
 */
public interface ConsoleCallback {
    
    void addWordMessage(String message);
    
    void addInfoMessage(String message);
    
    void addErrorMessage(String message);
    
    void addEmptyLine();
    
    /**
     * Add message without timestamp.
     * @param message string to add
     */
    void addMessage(String message);
}
