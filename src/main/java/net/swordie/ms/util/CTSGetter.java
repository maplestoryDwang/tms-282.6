package net.swordie.ms.util;

import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.InPacket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class CTSGetter {

    static void main(String[] args) throws Exception {
        System.out.println("Paste hex. Blank line = decode. Type 'exit' to quit.");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        StringBuilder buf = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            String t = line.trim();
            if (t.equalsIgnoreCase("exit")) break;

            if (t.isEmpty()) {           // dòng trống => xử lý block hiện tại
                processHexBlock(buf);
                buf.setLength(0);
            } else {
                buf.append(line).append('\n');
            }
        }
        // xử lý phần còn lại nếu người dùng không xuống dòng trống cuối
        processHexBlock(buf);
    }

    private static void processHexBlock(CharSequence raw) {
        if (raw == null || raw.toString().trim().isEmpty()) return;

        String cleaned = normalizeToSpacedHex(raw.toString());
        if (cleaned.isEmpty()) {
            System.out.println("No hex found.");
            return;
        }
        // GIỮ cấu trúc cũ: dùng lại getBuffCTS(String hex)
        getBuffCTS(cleaned);
        System.out.println("----");
    }

    /**
     * Nhận mọi định dạng dán vào (có/không khoảng trắng, xuống dòng, có '0x', trộn hoa/thường).
     * Trả về chuỗi hex dạng "AA BB CC ..." phù hợp với Util.getByteArrayByString(...)
     */
    private static String normalizeToSpacedHex(String s) {
        // bỏ '0x', giữ lại ký tự hex, còn lại đổi thành space
        String cleaned = s.replaceAll("0x", "")
                .replaceAll("[^0-9A-Fa-f]", " ")
                .trim()
                .replaceAll("\\s+", " ");

        // gom lại, nếu lẻ nibble thì cắt bỏ nibble cuối
        String compact = cleaned.replace(" ", "");
        if ((compact.length() & 1) == 1) {
            compact = compact.substring(0, compact.length() - 1);
        }
        // chèn space mỗi 2 ký tự
        StringBuilder out = new StringBuilder(compact.length() + compact.length() / 2);
        for (int i = 0; i < compact.length(); i += 2) {
            if (i > 0) out.append(' ');
            out.append(compact.charAt(i)).append(compact.charAt(i + 1));
        }
        return out.toString();
    }

    private static void getBuffCTS(String hex) {
        byte[] arr = Util.getByteArrayByString(hex);
        InPacket inPacket = new InPacket(arr);
        List<CharacterTemporaryStat> list = new LinkedList<>();
        for (int i = 0; i < CharacterTemporaryStat.length; i++) {
            int mask = inPacket.decodeInt();
            //System.out.println("mask " + i + " : " + mask);
            for (CharacterTemporaryStat cts : CharacterTemporaryStat.values()) {
                if (cts.getPos() == i && (cts.getVal() & mask) != 0) {
                    list.add(cts);
                }
            }
        }
        for (CharacterTemporaryStat cts : list) {
            System.out.printf("Contained stat %s (%d)%n", cts, cts.getBitPos());
        }
    }
}
