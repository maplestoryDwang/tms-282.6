package net.swordie.ms.connection.api.launcher.release;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One valid {@code data/launcher/release.json}:
 * <pre>
 * {"latest":"1.2.0","minSupported":"1.1.0","notes":["..."],"publishedAt":"2026-10-04T12:00:00+08:00",
 *  "download":{"version":"1.2.0","file":"CatLauncher-1.2.0-portable.exe","size":104857600,"sha256":"...","signature":"..."}}
 * </pre>
 * Rules (docs/launcher-api-update.md sections 2 and 9; any violation makes the whole file invalid and the server keeps
 * the previous valid version):
 * <ul>
 *     <li>a UTF-8 JSON object (BOM allowed) of at most {@value ReleaseRules#FILE_MAX_BYTES} bytes, no duplicate keys;
 *     other keys are ignored and never served;</li>
 *     <li>{@code latest} / {@code minSupported} x.y.z and minSupported &lt;= latest;</li>
 *     <li>{@code notes} absent / null or an array of strings; each line cleaned ({@link ReleaseRules#cleanNote}),
 *     empty lines dropped, at most 10 lines kept, at most 100 characters each;</li>
 *     <li>{@code publishedAt} served only when {@link OffsetDateTime} or {@link Instant} parses it (else left out, not
 *     an error);</li>
 *     <li>{@code download} absent / null, or an object whose five values follow section 7 and whose version is not
 *     newer than latest (extra keys such as keyId / test are dropped).</li>
 * </ul>
 */
public record ReleaseInfo(String latest, String minSupported, List<String> notes, String publishedAt, ReleaseDownload download) {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    public ReleaseInfo {
        Objects.requireNonNull(latest, "latest");
        Objects.requireNonNull(minSupported, "minSupported");
        notes = List.copyOf(notes);
    }

    /** The file could not be accepted; the message names the first offending field (never the content). */
    public static final class InvalidRelease extends Exception {
        private static final long serialVersionUID = 1L;

        public InvalidRelease(String where) {
            super(where);
        }
    }

    public static ReleaseInfo parse(byte[] bytes) throws InvalidRelease {
        if (bytes == null || bytes.length > ReleaseRules.FILE_MAX_BYTES) {
            throw new InvalidRelease("size");
        }
        JsonNode root;
        try {
            root = JSON.readTree(bytes);
        } catch (IOException e) {
            throw new InvalidRelease("json");
        }
        return parse(root);
    }

    public static ReleaseInfo parse(JsonNode root) throws InvalidRelease {
        if (root == null || !root.isObject()) {
            throw new InvalidRelease("root");
        }
        String latest = version(root.get("latest"), "latest");
        String min = version(root.get("minSupported"), "minSupported");
        if (ReleaseRules.compare(min, latest) > 0) {
            throw new InvalidRelease("minSupported.newerThanLatest");
        }
        List<String> notes = notes(root.get("notes"));
        String publishedAt = publishedAt(root.get("publishedAt"));
        JsonNode d = root.get("download");
        ReleaseDownload download = d == null || d.isNull() ? null : download(d);
        if (download != null && ReleaseRules.compare(download.version(), latest) > 0) {
            throw new InvalidRelease("download.version.newerThanLatest");
        }
        return new ReleaseInfo(latest, min, notes, publishedAt, download);
    }

    private static String version(JsonNode v, String at) throws InvalidRelease {
        if (v == null || !v.isTextual() || !ReleaseRules.isVersion(v.textValue())) {
            throw new InvalidRelease(at);
        }
        return v.textValue();
    }

    static List<String> notes(JsonNode n) throws InvalidRelease {
        if (n == null || n.isNull()) {
            return List.of();
        }
        if (!n.isArray()) {
            throw new InvalidRelease("notes");
        }
        if (n.size() > 200) {
            throw new InvalidRelease("notes.size");
        }
        List<String> out = new ArrayList<>();
        for (int i = 0; i < n.size(); i++) {
            JsonNode line = n.get(i);
            if (!line.isTextual()) {
                throw new InvalidRelease("notes[" + i + "]");
            }
            String c = ReleaseRules.cleanNote(line.textValue());
            if (c != null && out.size() < ReleaseRules.NOTES_MAX) {
                out.add(c);
            }
        }
        return out;
    }

    /** The original text when it parses as an offset date-time or an instant (and is not too long), else null. */
    static String publishedAt(JsonNode n) {
        if (n == null || !n.isTextual()) {
            return null;
        }
        String s = n.textValue();
        if (s.isEmpty() || s.length() > ReleaseRules.PUBLISHED_AT_MAX) {
            return null;
        }
        try {
            OffsetDateTime.parse(s);
            return s;
        } catch (DateTimeParseException e) {
            try {
                Instant.parse(s);
                return s;
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    /** A download object (release.json, or release-entry.json for an import). */
    public static ReleaseDownload download(JsonNode d) throws InvalidRelease {
        if (d == null || !d.isObject()) {
            throw new InvalidRelease("download");
        }
        JsonNode version = d.get("version");
        if (version == null || !version.isTextual() || !ReleaseRules.isVersion(version.textValue())) {
            throw new InvalidRelease("download.version");
        }
        JsonNode file = d.get("file");
        if (file == null || !file.isTextual() || !ReleaseRules.isSafeFileName(file.textValue())) {
            throw new InvalidRelease("download.file");
        }
        JsonNode size = d.get("size");
        if (size == null || !size.isIntegralNumber() || !size.canConvertToLong() || !ReleaseRules.isSize(size.longValue())) {
            throw new InvalidRelease("download.size");
        }
        JsonNode sha = d.get("sha256");
        if (sha == null || !sha.isTextual() || !ReleaseRules.isSha256(sha.textValue())) {
            throw new InvalidRelease("download.sha256");
        }
        JsonNode sig = d.get("signature");
        if (sig == null || !sig.isTextual() || !ReleaseRules.isSignature(sig.textValue())) {
            throw new InvalidRelease("download.signature");
        }
        return new ReleaseDownload(version.textValue(), file.textValue(), size.longValue(), sha.textValue(), sig.textValue());
    }
}
