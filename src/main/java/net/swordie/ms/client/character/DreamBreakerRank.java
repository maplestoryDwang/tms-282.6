package net.swordie.ms.client.character;

import java.util.LinkedHashMap;
import java.util.Map;

public class DreamBreakerRank {
    public static Map<String, Integer> Rank = new LinkedHashMap<>();

    public static int getRank(String name) {
        int index = 1;
        if (!Rank.containsKey(name)) {
            return 0;
        }
        for (Map.Entry<String, Integer> info : Rank.entrySet()) {
            if (info.getKey().equals(name)) {
                break;
            }
            index++;
        }
        return index;
    }
}
