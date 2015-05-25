package com.words.controller;

import com.words.controller.callbacks.ConsoleCallback;
import com.words.controller.utils.DateTimeUtils;
import java.util.ArrayList;
import java.util.List;

final class Console implements ConsoleCallback {
    
    private static final String RESET = (char) 27 + "[0m";
    
    private static final String CONSOLE_MESSAGE_FORMAT = "[%s] %s";
    
    // system console
    private static class SystemConsole implements ConsoleCallback {
        
        @Override
        public void addWordMessage(String message) {
            showMessage(message);
        }
        
        @Override
        public void addInfoMessage(String message) {
            showMessage((char) 27 + "[32m" + message + RESET);
        }
        
        @Override
        public void addErrorMessage(String message) {
//            System.err.println(message);
            showMessage((char) 27 + "[31m" + message + RESET);
        }
        
        @Override
        public void addEmptyLine() {
            System.out.println();
        }
        
        @Override
        public void addMessage(String message) {
            System.out.println(message);
        }
        
        private void showMessage(String message) {
            System.out.println(message);
        }
    }
    
    private final List<ConsoleCallback> callbacks = new ArrayList<>();
    
    private final StringBuilder wordLog = new StringBuilder();
    
    Console() {
        callbacks.add(new SystemConsole());
    }
    
    void setWordLog(String currentWordLog) {
        if (!currentWordLog.isEmpty()) {
            wordLog.append(currentWordLog).append("\n");
            addMessage(currentWordLog);
        }
    }
    
    String getWordLog() { return wordLog.toString().trim(); }
    
    /**
     * Add one more console callback.
     * @param cc new instance
     */
    void addConsoleCallback(ConsoleCallback cc) {
        callbacks.add(cc);
    }
    
    @Override
    public synchronized void addWordMessage(String message) {
        String formattedMessage = formatConsoleMessage(message);
        wordLog.append(formattedMessage).append("\n");
        callbacks.stream().forEach(cc -> cc.addWordMessage(formattedMessage));
    }
    
    @Override
    public synchronized void addInfoMessage(String message) {
        callbacks.stream().forEach((cc) ->
            cc.addInfoMessage(formatConsoleMessage(message)));
    }
    
    @Override
    public synchronized void addErrorMessage(String message) {
        callbacks.stream().forEach((cc) ->
            cc.addErrorMessage(formatConsoleMessage(message)));
    }
    
    @Override
    public synchronized void addEmptyLine() {
        callbacks.stream().forEach(cc -> cc.addEmptyLine());
    }
    
    private String formatConsoleMessage(String message) {
        return String.format(
            CONSOLE_MESSAGE_FORMAT,
            DateTimeUtils.nowAsString(),
            message);
    }
    
    @Override
    public synchronized void addMessage(String message) {
        callbacks.stream().forEach(cc -> cc.addMessage(message));
    }
}
