package net.swordie.ms.connection.api.launcher.gift;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/**
 * The HMAC-SHA256 key of the gift codes (P7). Never in the source, git or the database: the server makes it the first
 * time it is needed, as 32 {@link SecureRandom} bytes (base64, one line) in {@code data/secrets/giftcode-hmac.key}
 * under the working directory, created together with an ACL that grants only the server's account, SYSTEM and
 * Administrators (the directory too). It is never logged or returned; only {@link #keyId()} (16 hex characters of a
 * SHA-256 over a label and the key) is stored with each code, so a code made with another key is recognised and
 * refused instead of silently never matching.
 * <p>
 * Losing the file makes every existing code unusable (they can be disabled and made again); it belongs in the
 * host's offline backup together with the database.
 */
public final class GiftCodeKey {
    public static final Path DEFAULT_FILE = Path.of("data", "secrets", "giftcode-hmac.key");
    private static final int KEY_BYTES = 32;
    private static volatile GiftCodeKey standard;

    private final byte[] key;
    private final String keyId;

    private GiftCodeKey(byte[] key) {
        this.key = key.clone();
        this.keyId = id(key);
    }

    /** The working directory's key, made on first use. */
    public static GiftCodeKey standard() throws IOException {
        GiftCodeKey k = standard;
        if (k == null) {
            synchronized (GiftCodeKey.class) {
                k = standard;
                if (k == null) {
                    k = load(DEFAULT_FILE, true);
                    standard = k;
                }
            }
        }
        return k;
    }

    /** Tests: a fixed key. */
    static GiftCodeKey of(byte[] key) {
        if (key.length != KEY_BYTES) throw new IllegalArgumentException("key length");
        return new GiftCodeKey(key);
    }

    /** Reads {@code file}; when it does not exist and {@code create}, makes it (owner-only ACL). */
    public static GiftCodeKey load(Path file, boolean create) throws IOException {
        if (!Files.exists(file)) {
            if (!create) throw new IOException("gift code key missing: " + file);
            byte[] fresh = new byte[KEY_BYTES];
            new SecureRandom().nextBytes(fresh);
            try {
                writeNew(file, (Base64.getEncoder().encodeToString(fresh) + "\n").getBytes(StandardCharsets.US_ASCII));
                System.out.println("[GiftCode] created a new HMAC key file " + file.toAbsolutePath() + " (owner-only; back it up offline)");
            } catch (FileAlreadyExistsException raced) {
                // another thread / process made it first: read that one
            } finally {
                Arrays.fill(fresh, (byte) 0);
            }
        }
        byte[] raw = Files.readAllBytes(file);
        byte[] key;
        try {
            key = Base64.getDecoder().decode(new String(raw, StandardCharsets.US_ASCII).strip());
        } catch (IllegalArgumentException e) {
            throw new IOException("gift code key file is not base64: " + file);
        } finally {
            Arrays.fill(raw, (byte) 0);
        }
        if (key.length != KEY_BYTES) throw new IOException("gift code key file has " + key.length + " bytes, expected " + KEY_BYTES);
        GiftCodeKey k = new GiftCodeKey(key);
        Arrays.fill(key, (byte) 0);
        return k;
    }

    /** HMAC-SHA256 of a normalized code. */
    public byte[] hash(String normalizedCode) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(normalizedCode.getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public String keyId() {
        return keyId;
    }

    private static String id(byte[] key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update("tms-giftcode-key-id\n".getBytes(StandardCharsets.US_ASCII));
            md.update(key);
            return HexFormat.of().formatHex(md.digest(), 0, 8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        return "GiftCodeKey[id=" + keyId + "]";
    }

    // ------------------------------------------------------------------ owner-only file

    static void writeNew(Path file, byte[] content) throws IOException {
        Path dir = file.toAbsolutePath().getParent();
        if (!Files.isDirectory(dir)) {
            try {
                Files.createDirectory(dir, attributes(dir, true));
            } catch (FileAlreadyExistsException ignored) {
                // made meanwhile
            }
        }
        try (SeekableByteChannel ch = Files.newByteChannel(file, Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE),
                attributes(dir, false))) {
            ch.write(ByteBuffer.wrap(content));
        }
    }

    /** NTFS: an explicit (not inherited) ACL - the server's account, SYSTEM, Administrators. Elsewhere: 0600 / 0700. */
    static FileAttribute<?>[] attributes(Path near, boolean directory) throws IOException {
        Path probe = near;
        while (probe != null && !Files.exists(probe)) probe = probe.getParent();
        if (probe != null && Files.getFileAttributeView(probe, AclFileAttributeView.class) != null) {
            UserPrincipalLookupService lookup = FileSystems.getDefault().getUserPrincipalLookupService();
            List<AclEntry> acl = new ArrayList<>();
            List<UserPrincipal> who = new ArrayList<>();
            who.add(lookup.lookupPrincipalByName(System.getProperty("user.name")));
            for (String n : List.of("NT AUTHORITY\\SYSTEM", "BUILTIN\\Administrators")) {
                try {
                    who.add(lookup.lookupPrincipalByName(n));
                } catch (IOException ignored) {
                    // a localised / missing group: the owner entry is enough
                }
            }
            Set<AclEntryFlag> flags = directory ? EnumSet.of(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT) : EnumSet.noneOf(AclEntryFlag.class);
            for (UserPrincipal p : who) {
                acl.add(AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(p)
                        .setPermissions(EnumSet.allOf(AclEntryPermission.class)).setFlags(flags).build());
            }
            List<AclEntry> value = List.copyOf(acl);
            return new FileAttribute<?>[]{new FileAttribute<List<AclEntry>>() {
                @Override
                public String name() {
                    return "acl:acl";
                }

                @Override
                public List<AclEntry> value() {
                    return value;
                }
            }};
        }
        return new FileAttribute<?>[]{PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(directory ? "rwx------" : "rw-------"))};
    }
}
