package com.words.controller.sound.downloadmp3;

public class AlreadyDownloadingException extends Exception { 
    
    public AlreadyDownloadingException(String word) {
        super(word);
    }
}
