package net.swordie.ms.connection.api.launcher.release;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * One signed client file list (release-sign.mjs client-sign's {@code client-files.json}):
 * <pre>
 * {"format":"cat-launcher-client-files","version":"1.0.0","createdAt":"...","keyId":"...","test":true?,
 *  "files":[{"path":"Data/UI/UI_000.wz","size":75061454,"sha256":"..."}],"signature":"..."}
 * </pre>
 * Checked with {@link ClientFileRules}; files kept sorted by path; sha256 lower-case (as written). The signature is
 * passed on unchanged: the server holds no key.
 */
public record ClientFileSet(String version, List<Entry> files, String signature, long totalSize, String listSha256, boolean test, String keyId) {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    public record Entry(String path, long size, String sha256) {
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("path", path);
            m.put("size", size);
            m.put("sha256", sha256);
            return m;
        }
    }

    public ClientFileSet {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(signature, "signature");
        files = List.copyOf(files);
    }

    /** The list could not be accepted; the message names the first offending field (never the content). */
    public static final class Invalid extends Exception {
        private static final long serialVersionUID = 1L;

        public Invalid(String where) {
            super(where);
        }
    }

    public static ClientFileSet parse(byte[] bytes) throws Invalid {
        if (bytes == null || bytes.length > ClientFileRules.LIST_MAX_BYTES) {
            throw new Invalid("size");
        }
        JsonNode root;
        try {
            root = JSON.readTree(bytes);
        } catch (IOException e) {
            throw new Invalid("json");
        }
        return parse(root);
    }

    public static ClientFileSet parse(JsonNode root) throws Invalid {
        if (root == null || !root.isObject()) {
            throw new Invalid("root");
        }
        if (!ClientFileRules.FORMAT.equals(root.path("format").textValue())) {
            throw new Invalid("format");
        }
        JsonNode v = root.get("version");
        if (v == null || !v.isTextual() || !ReleaseRules.isVersion(v.textValue())) {
            throw new Invalid("version");
        }
        JsonNode arr = root.get("files");
        if (arr == null || !arr.isArray() || arr.isEmpty() || arr.size() > ClientFileRules.FILES_MAX) {
            throw new Invalid("files");
        }
        List<Entry> files = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        long total = 0;
        for (int i = 0; i < arr.size(); i++) {
            JsonNode f = arr.get(i);
            String at = "files[" + i + "]";
            if (f == null || !f.isObject()) {
                throw new Invalid(at);
            }
            JsonNode p = f.get("path");
            if (p == null || !p.isTextual() || !ClientFileRules.isSafePath(p.textValue())) {
                throw new Invalid(at + ".path");
            }
            JsonNode s = f.get("size");
            if (s == null || !s.isIntegralNumber() || !s.canConvertToLong() || s.longValue() < 0 || s.longValue() > ClientFileRules.FILE_SIZE_MAX) {
                throw new Invalid(at + ".size");
            }
            JsonNode h = f.get("sha256");
            if (h == null || !h.isTextual() || !ClientFileRules.isSha256(h.textValue())) {
                throw new Invalid(at + ".sha256");
            }
            if (!seen.add(p.textValue().toLowerCase(Locale.ROOT))) {
                throw new Invalid(at + ".duplicate");
            }
            total += s.longValue();
            if (total > ClientFileRules.TOTAL_SIZE_MAX) {
                throw new Invalid("files.totalSize");
            }
            files.add(new Entry(p.textValue(), s.longValue(), h.textValue()));
        }
        files.sort(Comparator.comparing(Entry::path));
        JsonNode sig = root.get("signature");
        if (sig == null || !sig.isTextual() || !ReleaseRules.isSignature(sig.textValue())) {
            throw new Invalid("signature");
        }
        JsonNode t = root.get("test");
        boolean test = t != null && !(t.isBoolean() && !t.booleanValue());
        JsonNode k = root.get("keyId");
        String keyId = k != null && k.isTextual() && k.textValue().matches("[0-9a-f]{16}") ? k.textValue() : null;
        String version = v.textValue();
        return new ClientFileSet(version, files, sig.textValue(), total, ClientFileRules.listSha256(version, files), test, keyId);
    }

    /** Some path needs path rules 2 (a segment starting with {@code _}): launchers up to 1.2.5 would refuse the list. */
    public boolean pathRules2() {
        return ClientFileRules.needsPathRules2(files);
    }

    public Entry find(String path) {
        for (Entry e : files) {
            if (e.path().equals(path)) {
                return e;
            }
        }
        return null;
    }

    /** launcherVersion's {@code clientFiles}: version, count, totalSize, listSha256 (small: old launchers cap the answer at 8 KB). */
    public Map<String, Object> summary() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("version", version);
        m.put("count", files.size());
        m.put("totalSize", totalSize);
        m.put("listSha256", listSha256);
        return m;
    }

    /** The files as the clientFiles action sends them. */
    public List<Map<String, Object>> fileMaps() {
        List<Map<String, Object>> out = new ArrayList<>();
        files.forEach(f -> out.add(f.toMap()));
        return out;
    }
}
