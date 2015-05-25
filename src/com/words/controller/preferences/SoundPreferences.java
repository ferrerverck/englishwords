package com.words.controller.preferences;

public enum SoundPreferences {
    
    STANDARD("Standard"),
    ONLY_ENGLISH("Only english"),
    MUTE("Mute"),
    NEXT_WORD("Next word");
    
    private final String description;
    
    private SoundPreferences(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
