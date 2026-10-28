package net.swordie.ms.util;

import java.io.*;
import java.util.*;

public class MobStatGetter {
    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.println("Paste hex (or q to quit):");
            String line = br.readLine();
            if (line == null) return;

            line = line.trim();
            if (line.equalsIgnoreCase("q") || line.equalsIgnoreCase("quit")) return;
            if (line.isEmpty()) continue;

            byte[] b = parseHex(line);

            int rem = b.length & 3;
            if (rem != 0) {
                byte[] nb = new byte[b.length + (4 - rem)];
                System.arraycopy(b, 0, nb, 0, b.length);
                b = nb;
            }

            int ints = b.length / 4;
            for (int pos = 0; pos < ints; pos++) {
                int v = readIntLE(b, pos * 4);
                System.out.printf("mask[%d] = %d (0x%08X)%n", pos, v, v);

                if (v != 0) {
                    for (int bit = 0; bit < 32; bit++) {
                        if ((v & (1 << bit)) != 0) {
                            int bitPos = pos * 32 + (31 - bit); // Maple MSB-first
                            System.out.println("  bitPos = " + bitPos);
                        }
                    }
                }
            }
        }
    }

    static int readIntLE(byte[] b, int i) {
        return (b[i] & 0xFF)
                | ((b[i + 1] & 0xFF) << 8)
                | ((b[i + 2] & 0xFF) << 16)
                | ((b[i + 3] & 0xFF) << 24);
    }

    static byte[] parseHex(String s) {
        s = s.replaceAll("[^0-9A-Fa-f]", "");
        if ((s.length() & 1) != 0) s += "0";
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
