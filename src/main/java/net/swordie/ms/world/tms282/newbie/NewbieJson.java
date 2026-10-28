package net.swordie.ms.world.tms282.newbie;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Strict reading of {@code newbie-flow.json}: every accessor names the offending field (a JSON path such as
 * {@code steps[3].items[2].itemId}) in the {@link NewbieFlowConfig.InvalidConfig} it throws. Dialog texts must be
 * encodable in MS950 (the TW client's charset), at most {@value #MAX_TEXT} characters, without control characters
 * other than CR / LF / TAB.
 */
public final class NewbieJson {
    static final int MAX_TEXT = 1500;
    static final Charset DIALOG_CHARSET = Charset.forName("MS950");

    public final JsonNode node;
    public final String path;

    public NewbieJson(JsonNode node, String path) {
        this.node = node;
        this.path = path;
    }

    public static NewbieFlowConfig.InvalidConfig invalid(String where, String what) {
        return new NewbieFlowConfig.InvalidConfig(where + ": " + what);
    }

    private String at(String field) {
        return path.isEmpty() ? field : path + "." + field;
    }

    public boolean has(String field) {
        return node.has(field) && !node.get(field).isNull();
    }

    public NewbieJson object(String field) {
        JsonNode n = node.get(field);
        if (n == null || !n.isObject()) throw invalid(at(field), "must be an object");
        return new NewbieJson(n, at(field));
    }

    public List<NewbieJson> array(String field, int min, int max) {
        JsonNode n = node.get(field);
        if (n == null || !n.isArray()) throw invalid(at(field), "must be an array");
        if (n.size() < min || n.size() > max) throw invalid(at(field), "needs " + min + ".." + max + " entries, has " + n.size());
        List<NewbieJson> out = new ArrayList<>();
        for (int i = 0; i < n.size(); i++) {
            out.add(new NewbieJson(n.get(i), at(field) + "[" + i + "]"));
        }
        return out;
    }

    /** An optional object of named entries (empty when absent). */
    public Map<String, NewbieJson> entries(String field) {
        Map<String, NewbieJson> out = new LinkedHashMap<>();
        if (!has(field)) return out;
        NewbieJson o = object(field);
        Iterator<Map.Entry<String, JsonNode>> it = o.node.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            out.put(e.getKey(), new NewbieJson(e.getValue(), o.path + "." + e.getKey()));
        }
        return out;
    }

    public int integer(String field, long min, long max) {
        JsonNode n = node.get(field);
        if (n == null || !n.isIntegralNumber()) throw invalid(at(field), "must be an integer");
        long v = n.asLong();
        if (v < min || v > max) throw invalid(at(field), "must be " + min + ".." + max);
        return (int) v;
    }

    public int integer(String field, long min, long max, int fallback) {
        return has(field) ? integer(field, min, max) : fallback;
    }

    public long number(String field, long min, long max, long fallback) {
        if (!has(field)) return fallback;
        JsonNode n = node.get(field);
        if (!n.isIntegralNumber()) throw invalid(at(field), "must be an integer");
        long v = n.asLong();
        if (v < min || v > max) throw invalid(at(field), "must be " + min + ".." + max);
        return v;
    }

    public boolean bool(String field, boolean fallback) {
        if (!has(field)) return fallback;
        JsonNode n = node.get(field);
        if (!n.isBoolean()) throw invalid(at(field), "must be true or false");
        return n.asBoolean();
    }

    public String word(String field, int maxLength) {
        JsonNode n = node.get(field);
        if (n == null || !n.isTextual()) throw invalid(at(field), "must be a string");
        String s = n.asText();
        if (!s.matches("[A-Za-z0-9_*.\\-]{1," + maxLength + "}")) throw invalid(at(field), "must be 1.." + maxLength + " of A-Z a-z 0-9 _ - . *");
        return s;
    }

    public String text(String field) {
        JsonNode n = node.get(field);
        if (n == null || !n.isTextual()) throw invalid(at(field), "must be a string");
        return checkText(at(field), n.asText(), false);
    }

    public String text(String field, String fallback) {
        return has(field) ? text(field) : fallback;
    }

    /** This node as a dialog text. */
    public String asText() {
        if (!node.isTextual()) throw invalid(path, "must be a string");
        return checkText(path, node.asText(), false);
    }

    public List<String> texts(String field, int min, int max) {
        List<String> out = new ArrayList<>();
        for (NewbieJson j : array(field, min, max)) {
            out.add(j.asText());
        }
        return out;
    }

    /** This node as an array of {@code min..max} entries. */
    public List<NewbieJson> asArray(int min, int max) {
        if (!node.isArray()) throw invalid(path, "must be an array");
        if (node.size() < min || node.size() > max) throw invalid(path, "needs " + min + ".." + max + " entries, has " + node.size());
        List<NewbieJson> out = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            out.add(new NewbieJson(node.get(i), path + "[" + i + "]"));
        }
        return out;
    }

    /** This node as an array of dialog texts. */
    public List<String> asTexts(int min, int max) {
        List<String> out = new ArrayList<>();
        for (NewbieJson j : asArray(min, max)) {
            out.add(j.asText());
        }
        return out;
    }

    public List<Integer> integers(String field, long min, long max, int maxCount) {
        List<Integer> out = new ArrayList<>();
        if (!has(field)) return out;
        for (NewbieJson j : array(field, 0, maxCount)) {
            if (!j.node.isIntegralNumber() || j.node.asLong() < min || j.node.asLong() > max) {
                throw invalid(j.path, "must be an integer " + min + ".." + max);
            }
            out.add(j.node.asInt());
        }
        return out;
    }

    public static String checkText(String where, String s, boolean allowEmpty) {
        if (s.isEmpty() && !allowEmpty) throw invalid(where, "must not be empty");
        if (s.length() > MAX_TEXT) throw invalid(where, "longer than " + MAX_TEXT + " characters");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 0x20 && c != '\r' && c != '\n' && c != '\t') throw invalid(where, "control character U+" + String.format("%04X", (int) c));
        }
        CharsetEncoder enc = DIALOG_CHARSET.newEncoder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!enc.canEncode(c)) {
                throw invalid(where, "character '" + c + "' (U+" + String.format("%04X", (int) c) + ") is not in MS950 (the TW client cannot show it)");
            }
        }
        return s;
    }
}
