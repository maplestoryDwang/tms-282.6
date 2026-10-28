package net.swordie.ms.console;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URL;

public final class ConsoleIconLoader {
    private static final String DEFAULT_ICON = "MapleStory.png";

    private ConsoleIconLoader() {
    }

    public static void applyFrameIcon(Window window) {
        Image image = loadImage(DEFAULT_ICON);
        if (image != null && window != null) {
            window.setIconImage(image);
        }
    }

    private static Image loadImage(String name) {
        URL url = ConsoleIconLoader.class.getResource("/icon/" + name);
        if (url != null) {
            return new ImageIcon(url).getImage();
        }
        File file = new File("src/main/java/icon/" + name);
        if (file.isFile()) {
            return new ImageIcon(file.getAbsolutePath()).getImage();
        }
        return null;
    }
}