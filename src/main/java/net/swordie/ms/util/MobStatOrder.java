package net.swordie.ms.util;

import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.life.mob.MobStat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.swordie.ms.ServerConstants.DIR;

public class MobStatOrder {

    private static final String FILE_PATH = STR."\{DIR}/mobStat.cpp";

    private static int parseCppIntLiteral(String s) {
        s = s.trim();
        // bỏ suffix kiểu C/C++: u, U, l, L (có thể lặp)
        s = s.replaceAll("(?i)[uUlL]+$", "");

        if (s.startsWith("0x") || s.startsWith("0X")) {
            return Integer.parseUnsignedInt(s.substring(2), 16);
        }
        return Integer.parseInt(s);
    }

    static void main(String[] args) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(FILE_PATH)));
            String regex =
                    "CMobFlag_Getter\\s*\\(\\s*mask\\s*,\\s*(0x[0-9A-Fa-f]+|\\d+)(?:[uUlL]+)?\\s*\\)";
            List<String> extractedValues = extractValues(content, regex);
            if (extractedValues.isEmpty()) {
                System.out.println("文件中未找到任何 buff stat 值。");
            } else {
                List<String> out = new ArrayList<>(extractedValues.size());
                for (String s : extractedValues) {
                    int code = parseCppIntLiteral(s);
                    MobStat cts = MobStat.getByBitPos(code);
                    out.add(cts != null ? cts.name() : code + "");
                }
                System.out.println("找到 " + extractedValues.size() + " 個 buff stat 值：");
                System.out.println(String.join(", ", out));
            }

        } catch (IOException e) {
            System.err.println("讀取文件時出錯：" + e.getMessage());
            System.err.println("請確認該文件與 Java 程序位於同一目錄。");
        }
    }

    /**
     * Hàm thực hiện trích xuất các giá trị số sử dụng Regex.
     * @param text Nội dung text đầu vào.
     * @param regex Biểu thức chính quy.
     * @return Danh sách các chuỗi giá trị số được trích xuất.
     */
    private static List<String> extractValues(String text, String regex) {
        List<String> matches = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        // Lặp qua tất cả các lần khớp
        while (matcher.find()) {
            // Lấy nội dung của nhóm bắt giữ đầu tiên (Group 1), chính là số cần tìm
            matches.add(matcher.group(1));
        }
        return matches;
    }
}