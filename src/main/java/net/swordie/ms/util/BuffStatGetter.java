package net.swordie.ms.util;

import java.nio.file.*;
import java.util.regex.*;
import java.util.*;
import java.io.*;

import static net.swordie.ms.ServerConstants.DIR;

public class BuffStatGetter {
    static class Call {
        final String name;
        final int id;
        Call(String n, int i){ name=n; id=i; }
    }
    private static final String FILE_PATH = STR."\{DIR}/secondaryStat263.cpp";

    public static void main(String[] args) throws Exception {
        String text = new String(Files.readAllBytes(Paths.get(FILE_PATH)));

        // Hàm mục tiêu: mặc định là CSecondaryStat_BuffStat, có thể đổi qua argv[1]
        String targetFunc = args.length > 1 ? args[1] : "CSecondaryStat_BuffStat";

        List<Call> calls = parseCalls(text, targetFunc);

        // buffs: 按 ID 升序排列
        System.out.println("// Buff 列表：");
        calls.stream()
                .sorted(Comparator.comparingInt(c -> c.id))
                .forEach(c -> System.out.println( c.name + "(" + c.id + "),"));

        // orders: giữ nguyên thứ tự xuất hiện
        System.out.println();
        System.out.println("// 順序列表：");
        for (int i = 0; i < calls.size(); i++) {
            System.out.print(calls.get(i).name);
            System.out.print(i + 1 < calls.size() ? ", " : ",");
        }
        System.out.println();
    }

    static List<Call> parseCalls(String s, String funcName){
        List<Call> out = new ArrayList<>();

        // Bắt đúng hàm cần quét
        Pattern callPat = Pattern.compile(Pattern.quote(funcName) + "\\s*\\((.*?)\\);", Pattern.DOTALL);
        Matcher m = callPat.matcher(s);

        while (m.find()) {
            String args = m.group(1);

            // TH1: có chuỗi tên "Name", rồi tới số ID
            Matcher named = Pattern.compile(",\\s*\"([^\"]+)\"\\s*,\\s*(\\d+)\\s*,").matcher(args);
            if (named.find()) {
                String name = named.group(1).trim();
                int id = Integer.parseInt(named.group(2));
                out.add(new Call(name, id));
                continue;
            }

            // TH2: không có chuỗi tên, lấy ID gần cuối và đặt tên Unk<ID>
            Matcher unk = Pattern.compile(",\\s*&[A-Za-z0-9_]+[^,]*,\\s*(\\d+)\\s*,").matcher(args);
            int id = -1;
            if (unk.find()) {
                id = Integer.parseInt(unk.group(1));
            } else {
                // dự phòng: tìm số trong 3 tham số cuối
                String[] parts = args.split(",");
                String tail = String.join(",", Arrays.copyOfRange(parts, Math.max(0, parts.length - 3), parts.length));
                Matcher any = Pattern.compile("(\\d+)").matcher(tail);
                if (any.find()) id = Integer.parseInt(any.group(1));
            }
            String name = id >= 0 ? ("Unk" + id) : "Unk";
            out.add(new Call(name, id));
        }
        return out;
    }

}
