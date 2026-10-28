package net.swordie.ms.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Updates {@code key=value} lines of a .properties file in place: other lines, comments, byte encoding and line
 * endings are kept; missing keys are appended. The write goes through a temp file and an atomic move. */
public final class PropertiesFileLines {
    private PropertiesFileLines() { }

    public static synchronized void put(Path file, Map<String, String> updates) throws IOException {
        // ISO-8859-1 round-trips every byte, so UTF-8 comments survive untouched.
        String text = Files.isRegularFile(file) ? Files.readString(file, StandardCharsets.ISO_8859_1) : "";
        String eol = text.contains("\r\n") ? "\r\n" : "\n";
        List<String> lines = new ArrayList<>(text.isEmpty() ? List.of() : List.of(text.split("\r?\n", -1)));
        if (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1); // trailing newline
        }
        for (Map.Entry<String, String> u : updates.entrySet()) {
            String key = u.getKey(), line = key + "=" + escape(u.getValue() == null ? "" : u.getValue());
            boolean replaced = false;
            for (int i = 0; i < lines.size(); i++) {
                String t = lines.get(i).stripLeading();
                if (!t.startsWith("#") && !t.startsWith("!")
                        && (t.startsWith(key + "=") || t.startsWith(key + " ") || t.startsWith(key + ":") || t.startsWith(key + "\t"))) {
                    lines.set(i, line);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                lines.add(line);
            }
        }
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, String.join(eol, lines) + eol, StandardCharsets.ISO_8859_1);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /** Minimal .properties escaping so the value reads back as written (non-Latin-1 as \\uXXXX). */
    static String escape(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (i == 0 && ch == ' ') sb.append("\\ ");
                    else if (ch > 0xFF) sb.append(String.format("\\u%04X", (int) ch));
                    else sb.append(ch);
                }
            }
        }
        return sb.toString();
    }
}
