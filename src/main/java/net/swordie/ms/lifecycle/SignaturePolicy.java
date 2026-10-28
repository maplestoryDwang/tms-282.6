package net.swordie.ms.lifecycle;

import net.swordie.ms.Server;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.util.HexFormat;
import java.util.jar.JarFile;

/** The local server verifies its original signer before opening any service. */
public final class SignaturePolicy {
    private static final byte[] CERTIFICATE_HASH = HexFormat.of().parseHex(
            "0645C960D311AA169C67E8C95ED388ADF1CBD9A3015ECE23C70DDF999971AC24");

    private SignaturePolicy() { }

    public static void verifyRunningArtifact() {
        try {
            var source = Server.class.getProtectionDomain().getCodeSource();
            if (source == null || !trusted(source.getCertificates())) {
                throw new SecurityException("Server JAR is unsigned or has an unexpected signer");
            }
            Path artifact = Path.of(source.getLocation().toURI());
            int classes = 0;
            try (JarFile jar = new JarFile(artifact.toFile(), true)) {
                var entries = jar.entries();
                byte[] buffer = new byte[65536];
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    if (!entry.getName().startsWith("net/swordie/ms/") || !entry.getName().endsWith(".class")) continue;
                    try (var input = jar.getInputStream(entry)) {
                        while (input.read(buffer) != -1) { }
                    }
                    if (!trusted(entry.getCertificates())) {
                        throw new SecurityException("Server JAR contains an unsigned application class");
                    }
                    classes++;
                }
            }
            if (classes == 0) throw new SecurityException("Server JAR has no signed application classes");
            System.out.println("[Signature] Original local server signature verified");
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException("Cannot verify local server JAR signature", e);
        }
    }

    private static boolean trusted(Certificate[] certificates) throws Exception {
        if (certificates == null) return false;
        var digest = MessageDigest.getInstance("SHA-256");
        for (var certificate : certificates) {
            if (MessageDigest.isEqual(CERTIFICATE_HASH, digest.digest(certificate.getEncoded()))) return true;
        }
        return false;
    }
}
