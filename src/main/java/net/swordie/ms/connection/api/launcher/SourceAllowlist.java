package net.swordie.ms.connection.api.launcher;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Local-only HTTP API source policy. Config and JVM overrides cannot broaden the accepted source.
 * The literal parser remains available for validation and diagnostics without resolving host names.
 */
public final class SourceAllowlist {
    public static final String KEY = "api.http.allow";
    public static final String DEFAULT = "127.0.0.1";
    private static final Pattern IPV4 = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})");
    private static final Pattern IPV6 = Pattern.compile("[0-9A-Fa-f:][0-9A-Fa-f:.]{1,44}");
    private static final SourceAllowlist LOCAL_ONLY = parse(DEFAULT);

    private record Rule(byte[] network, int prefix, String text) {
        boolean matches(byte[] address) {
            if (address.length != network.length) {
                return false;
            }
            int full = prefix / 8;
            for (int i = 0; i < full; i++) {
                if (address[i] != network[i]) {
                    return false;
                }
            }
            int rest = prefix % 8;
            if (rest == 0) {
                return true;
            }
            int mask = (0xFF << (8 - rest)) & 0xFF;
            return (address[full] & mask) == (network[full] & mask);
        }
    }

    private final String raw;
    private final List<Rule> rules;
    private final List<String> rejected;

    private SourceAllowlist(String raw, List<Rule> rules, List<String> rejected) {
        this.raw = raw;
        this.rules = rules;
        this.rejected = rejected;
    }

    /** The packaged policy is fixed; configuration cannot open remote access. */
    public static SourceAllowlist current() {
        return LOCAL_ONLY;
    }

    public static SourceAllowlist parse(String raw) {
        List<Rule> rules = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        for (String part : raw.split(",")) {
            String entry = part.trim();
            if (entry.isEmpty()) {
                continue;
            }
            Rule rule = rule(entry);
            if (rule == null) {
                rejected.add(entry);
            } else {
                rules.add(rule);
            }
        }
        return new SourceAllowlist(raw, List.copyOf(rules), List.copyOf(rejected));
    }

    public boolean allows(InetAddress address) {
        if (address == null) {
            return false;
        }
        byte[] bytes = address.getAddress();
        for (Rule rule : rules) {
            if (rule.matches(bytes)) {
                return true;
            }
        }
        return false;
    }

    /** For an address literal; anything that is not a literal is refused. */
    public boolean allows(String ip) {
        byte[] bytes = literal(ip);
        if (bytes == null) {
            return false;
        }
        for (Rule rule : rules) {
            if (rule.matches(bytes)) {
                return true;
            }
        }
        return false;
    }

    /** Accepted entries, normalised as {@code address/prefix}. */
    public List<String> rules() {
        return rules.stream().map(Rule::text).toList();
    }

    public List<String> rejected() {
        return rejected;
    }

    @Override
    public String toString() {
        return String.join(",", rules());
    }

    private static Rule rule(String entry) {
        int slash = entry.indexOf('/');
        String address = slash < 0 ? entry : entry.substring(0, slash);
        byte[] bytes = literal(address);
        if (bytes == null) {
            return null;
        }
        int max = bytes.length * 8;
        int prefix = max;
        if (slash >= 0) {
            try {
                prefix = Integer.parseInt(entry.substring(slash + 1).trim());
            } catch (NumberFormatException e) {
                return null;
            }
            if (prefix < 0 || prefix > max) {
                return null;
            }
        }
        return new Rule(bytes, prefix, address + "/" + prefix);
    }

    /** IPv4 / IPv6 literal bytes without any DNS lookup, or null. IPv4-mapped IPv6 comes back as 4 bytes. */
    static byte[] literal(String text) {
        if (text == null) {
            return null;
        }
        String s = text.trim();
        var m = IPV4.matcher(s);
        if (m.matches()) {
            byte[] out = new byte[4];
            for (int i = 0; i < 4; i++) {
                int v = Integer.parseInt(m.group(i + 1));
                if (v > 255) {
                    return null;
                }
                out[i] = (byte) v;
            }
            return out;
        }
        if (s.indexOf(':') >= 0 && IPV6.matcher(s).matches()) {
            try {
                // Starts with a hex digit or ':' and contains ':': InetAddress parses it as a literal, never DNS.
                return InetAddress.getByName(s).getAddress();
            } catch (UnknownHostException | IllegalArgumentException | SecurityException e) {
                return null;
            }
        }
        return null;
    }
}
