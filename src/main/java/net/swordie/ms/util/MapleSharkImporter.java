package net.swordie.ms.util;

import net.swordie.ms.ServerConstants;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapleSharkImporter {
    // Matches: NAME(123)  with optional spaces, commas, etc.
    private static final Pattern LINE = Pattern.compile("^\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*\\(\\s*(\\d+)\\s*\\)\\s*,?\\s*$");

    private static final int BUILD = ServerConstants.version;
    private static final int LOCALE = 8;
    private static final boolean OUTBOUND = false;
    private static final boolean IGNORE = false;

    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line;

        while ((line = br.readLine()) != null) {
            if (line.trim().equalsIgnoreCase("END")) break;

            Matcher m = LINE.matcher(line);
            if (!m.matches()) continue; // AUTH_SERVER(UnkValue) sẽ bị skip

            String name = m.group(1);
            String opcode = m.group(2);

            System.out.print("  <Definition>\n" +
                    "    <Build>" + BUILD + "</Build>\n" +
                    "    <Locale>" + LOCALE + "</Locale>\n" +
                    "    <Outbound>false</Outbound>\n" +
                    "    <Opcode>" + opcode + "</Opcode>\n" +
                    "    <Name>" + name + "</Name>\n" +
                    "    <Ignore>false</Ignore>\n" +
                    "  </Definition>\n");
        }
    }
}