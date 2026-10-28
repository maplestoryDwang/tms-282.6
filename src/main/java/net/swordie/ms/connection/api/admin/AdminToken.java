package net.swordie.ms.connection.api.admin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The admin token of this server run: {@value #TOKEN_BYTES} {@link SecureRandom} bytes as unpadded base64url
 * (43 characters), new at every start, written to {@code admin.token} in the server's working directory
 * ({@code D:\TMS\server\265\admin.token}; {@value #FILE_KEY} overrides it). The file is readable by the current
 * user only (Windows: an ACL with one entry for the user running the server and no inherited entries; elsewhere
 * {@code rw-------}). Only its SHA-256 stays in memory; {@link #matches} compares digests in constant time.
 */
public final class AdminToken {
    public static final String FILE_KEY = "admin.tokenFile";
    public static final String DEFAULT_FILE = "admin.token";
    public static final int TOKEN_BYTES = 32;
    private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final byte[] digest;
    private final Path file;

    private AdminToken(byte[] digest, Path file) {
        this.digest = digest;
        this.file = file;
    }

    /** A new token written to {@code file} (replacing the previous run's); IOException when it cannot be written. */
    public static AdminToken createAndWrite(Path file) throws IOException {
        byte[] raw = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        writeRestricted(file, token);
        return new AdminToken(sha256(token), file);
    }

    /** For checks: a token held in memory only. */
    static AdminToken ofValue(String token) {
        return new AdminToken(sha256(token), null);
    }

    public Path file() {
        return file;
    }

    /** Constant-time comparison of the presented token's digest with the stored one. */
    public boolean matches(String presented) {
        if (presented == null || !FORMAT.matcher(presented).matches()) {
            // same work as a well-formed token, so the timing says nothing about the format
            MessageDigest.isEqual(sha256(""), digest);
            return false;
        }
        return MessageDigest.isEqual(sha256(presented), digest);
    }

    static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Writes {@code token} to a temporary file in the same folder that is created already restricted to the current
     * user ({@link #createRestricted}), then moves it over {@code file} (the move keeps the restricted ACL).
     */
    static void writeRestricted(Path file, String token) throws IOException {
        Path abs = file.toAbsolutePath();
        Path dir = abs.getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }
        Path tmp = abs.resolveSibling(abs.getFileName() + ".tmp-" + Long.toHexString(RANDOM.nextLong() & Long.MAX_VALUE));
        createRestricted(tmp);
        try {
            // again after creation: confirms the ACL really is the single entry (some file systems ignore the attribute)
            restrictToCurrentUser(tmp);
            Files.writeString(tmp, token, StandardCharsets.US_ASCII);
            IOException last = null;
            for (int i = 0; i < 20; i++) {
                try {
                    try {
                        Files.move(tmp, abs, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException e) {
                        Files.move(tmp, abs, StandardCopyOption.REPLACE_EXISTING);
                    }
                    return;
                } catch (FileSystemException e) {
                    // the console may be reading the old file right now (Windows sharing violation): retry briefly
                    last = e;
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            throw last != null ? last : new IOException("cannot replace " + abs);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /**
     * Creates the empty file with its final permissions in the same call (Windows: the ACL is a creation attribute,
     * so no other account can open the file in between with the inherited folder ACL; POSIX: rw-------).
     */
    static void createRestricted(Path p) throws IOException {
        AclFileAttributeView parentAcl = p.getParent() == null ? null
                : Files.getFileAttributeView(p.getParent(), AclFileAttributeView.class);
        if (parentAcl != null) {
            AclEntry entry = AclEntry.newBuilder()
                    .setType(AclEntryType.ALLOW)
                    .setPrincipal(currentUser(p.getParent(), parentAcl))
                    .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                    .build();
            List<AclEntry> acl = List.of(entry);
            FileAttribute<List<AclEntry>> attr = new FileAttribute<>() {
                @Override
                public String name() {
                    return "acl:acl";
                }

                @Override
                public List<AclEntry> value() {
                    return acl;
                }
            };
            Files.createFile(p, attr);
            return;
        }
        if (Files.getFileStore(p.getParent() == null ? p.toAbsolutePath().getParent() : p.getParent())
                .supportsFileAttributeView(PosixFileAttributeView.class)) {
            Files.createFile(p, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            return;
        }
        Files.createFile(p);
    }

    /** One ACL entry (full control for the user running the server), nothing inherited; POSIX: rw-------. */
    static void restrictToCurrentUser(Path p) throws IOException {
        AclFileAttributeView acl = Files.getFileAttributeView(p, AclFileAttributeView.class);
        if (acl != null) {
            UserPrincipal me = currentUser(p, acl);
            AclEntry entry = AclEntry.newBuilder()
                    .setType(AclEntryType.ALLOW)
                    .setPrincipal(me)
                    .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                    .build();
            acl.setAcl(List.of(entry));
            return;
        }
        PosixFileAttributeView posix = Files.getFileAttributeView(p, PosixFileAttributeView.class);
        if (posix != null) {
            posix.setPermissions(PosixFilePermissions.fromString("rw-------"));
            return;
        }
        throw new IOException("cannot restrict " + p + " to the current user");
    }

    private static UserPrincipal currentUser(Path p, AclFileAttributeView acl) throws IOException {
        String name = System.getProperty("user.name");
        if (name != null && !name.isBlank()) {
            try {
                UserPrincipal u = p.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(name);
                if (!(u instanceof GroupPrincipal)) {
                    return u;
                }
            } catch (IOException | UnsupportedOperationException ignored) {
                // fall back to the owner (the user that created the file)
            }
        }
        return acl.getOwner();
    }
}
