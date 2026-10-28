package net.swordie.ms.connection.api.launcher.release;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The {@code download} object of release.json / launcherVersion: the five values of release-sign.mjs's
 * release-entry.json, passed on unchanged (the server neither signs nor checks the signature; it has no key).
 */
public record ReleaseDownload(String version, String file, long size, String sha256, String signature) {
    public ReleaseDownload {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(sha256, "sha256");
        Objects.requireNonNull(signature, "signature");
    }

    /** Wire order: version, file, size, sha256, signature (nothing else - keyId / test stay in the file). */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("version", version);
        m.put("file", file);
        m.put("size", size);
        m.put("sha256", sha256);
        m.put("signature", signature);
        return m;
    }
}
