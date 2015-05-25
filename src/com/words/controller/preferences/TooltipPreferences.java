package com.words.controller.preferences;

public enum TooltipPreferences {
    
    STANDARD("Standard"),
    EDGES("Edges"),
    ALWAYS("Always"),
    NEVER("Never");
    
    private final String description;
    
    private TooltipPreferences(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
