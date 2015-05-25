package com.words.gui.guiutils;

import com.words.controller.preferences.AutoMode;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class AutoButtonIcons {
    private static final Map<AutoMode, ImageIcon> ICONS =
        new EnumMap<>(AutoMode.class);
    static {
        ICONS.put(AutoMode.VERY_SLOW, loadIcon("teal.png"));
        ICONS.put(AutoMode.OFF, loadIcon("gray.png"));
        ICONS.put(AutoMode.FAST, loadIcon("green.png"));
        ICONS.put(AutoMode.AVERAGE, loadIcon("orange.png"));
        ICONS.put(AutoMode.SLOW, loadIcon("red.png"));
    }
    
    private AutoButtonIcons() { throw new AssertionError(); }
    
    private static ImageIcon loadIcon(String iconName) {
        try {
            return new ImageIcon(ImageIO.read(AutoButtonIcons.class
                .getResource("/resources/icons/" + iconName)));
        } catch (IOException ex) {
            return null;
        }
    }
    
    public static ImageIcon getIcon(AutoMode autoMode) {
        return ICONS.get(autoMode);
    }
}
