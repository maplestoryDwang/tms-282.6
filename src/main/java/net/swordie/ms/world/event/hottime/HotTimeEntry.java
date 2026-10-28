package net.swordie.ms.world.event.hottime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * hottime.json 的一筆排程。type = "hottime"（倍率時段）或 "notice"（排程公告）。
 * weekdays 為 ISO 星期 (1=一 ... 7=日)，空陣列代表每天。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HotTimeEntry {

    public static final String TYPE_HOTTIME = "hottime";
    public static final String TYPE_NOTICE = "notice";

    public String id;
    public String type = TYPE_HOTTIME;
    public boolean enabled = true;
    public int[] weekdays = new int[0];

    // ---- hottime ----
    public String start;          // "HH:mm"
    public String end;            // "HH:mm"（<= start 表示跨午夜）
    public double expRate = 1.0;
    public double dropRate = 1.0;
    public double mesoRate = 1.0;
    public String slide;          // 開始時的滑動公告
    public String message;        // 開始時的全服公告
    public String endMessage;     // 結束時的全服公告
    public String endSlide;       // 結束時的滑動公告（空字串 = 清除）
    public int rewardItem;
    public int rewardCount = 1;
    public int rewardMinLevel;
    public int rewardGraceMinutes = 5; // 只有在開始後 N 分鐘內偵測到開始才發獎（避免重啟重複發放）

    // ---- notice ----
    public List<String> times = new ArrayList<>();  // "HH:mm" 固定時間點
    public int intervalMinutes;                     // >0 時以間隔公告
    public boolean random = true;                   // 間隔為 1~intervalMinutes 隨機
    public String style = "notice";                 // chat | notice | slide | weather | popup
    public int chatType = 7;
    public String prefix = "";
    public List<String> messages = new ArrayList<>();
    public int weatherType = 371;
    public int weatherDuration = 6000;

    public HotTimeEntry() {
    }

    public boolean isHotTime() {
        return TYPE_HOTTIME.equalsIgnoreCase(type);
    }

    public boolean isNotice() {
        return TYPE_NOTICE.equalsIgnoreCase(type);
    }

    public String key() {
        return id != null ? id : String.valueOf(System.identityHashCode(this));
    }

    public boolean matchesDay(DayOfWeek day) {
        if (weekdays == null || weekdays.length == 0) {
            return true;
        }
        int v = day.getValue();
        for (int w : weekdays) {
            if (w == v || (w == 0 && v == 7)) { // 容許 0 代表星期日 (JS getDay 慣例)
                return true;
            }
        }
        return false;
    }

    static LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String[] p = s.trim().split(":");
        int h = Integer.parseInt(p[0]);
        int m = p.length > 1 ? Integer.parseInt(p[1]) : 0;
        return h >= 24 ? LocalTime.MAX : LocalTime.of(h, m);
    }
}
