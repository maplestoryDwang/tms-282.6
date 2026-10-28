package net.swordie.ms.connection.hotfix;

/**
 * One hotfix override: a full WZ path (backslash separated, e.g. {@code String\Consume.img\2430070\name}) and a
 * typed value. The client applies each entry over its loaded WZ data (format of the 265 Data2.wz sample: a flat
 * "Property" image whose child names are full paths).
 */
public record HotfixEntry(String path, Value value) {

    /** Typed WZ property values the hotfix image can carry. */
    public sealed interface Value permits Null, Short, Int, Long, Float, Double, Str, Bool, Vector, Uol {
    }

    public record Null() implements Value {
    }

    public record Short(short v) implements Value {
    }

    public record Int(int v) implements Value {
    }

    public record Long(long v) implements Value {
    }

    public record Float(float v) implements Value {
    }

    public record Double(double v) implements Value {
    }

    public record Str(String v) implements Value {
    }

    public record Bool(boolean v) implements Value {
    }

    public record Vector(int x, int y) implements Value {
    }

    public record Uol(String link) implements Value {
    }

    public static HotfixEntry string(String path, String value) {
        return new HotfixEntry(path, new Str(value == null ? "" : value));
    }
}
