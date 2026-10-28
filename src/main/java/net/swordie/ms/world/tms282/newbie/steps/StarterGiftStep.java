package net.swordie.ms.world.tms282.newbie.steps;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.world.tms282.newbie.NewbieGame;
import net.swordie.ms.world.tms282.newbie.NewbieJson;
import net.swordie.ms.world.tms282.newbie.NewbieRun;
import net.swordie.ms.world.tms282.newbie.NewbieStep;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code StarterGift}: the starter gift, once per character. Settings: {@code meso}, {@code items} (everyone),
 * {@code equipment} (set key -> lines; the key is the first-job branch, {@code "*"} for every other character),
 * texts, and {@code maxClaimsPerUser} (0 = no limit per login user).
 * A line is {@code {"itemId", "quantity", "periodDays"?, "gender"?}} ({@code gender} 0 / 1 = only for male / female
 * characters). {@code chooseOne} (optional, with {@code chooseText}): lines of which the player picks exactly one in a
 * menu before the gift is claimed (the permanent mount of the newbie gift); closing the menu claims nothing.
 * <p>
 * Once: {@link NewbieGame#grantGift} checks space and grantability under the locks, then the claim transaction
 * ({@code newbie_flow.gift_claimed_at} NULL -> now + one {@code newbie_gift_log} row, UNIQUE per character) and only
 * then gives meso and items through the game's own grant path. A full bag claims nothing (the step stays); a second
 * dialog, a relogin, another channel or a concurrent claim sees ALREADY.
 */
public final class StarterGiftStep implements NewbieStep {
    public static final String TYPE = "StarterGift";
    public static final String ANY_SET = "*";

    /** One configured line. */
    public record Line(int itemId, int quantity, int periodDays, int gender) {
        public boolean fits(int characterGender) {
            return gender < 0 || gender == characterGender;
        }
    }

    private final String id;
    private final long meso;
    private final int nxPrepaid;
    private final List<Line> items;
    private final Map<String, List<Line>> equipment;
    private final List<Line> chooseOne;
    private final String chooseText;
    private final int maxClaimsPerUser;
    private final String introText, confirmText, doneText, declinedText, fullText, limitText, blockedText;

    StarterGiftStep(String id, long meso, int nxPrepaid, List<Line> items, Map<String, List<Line>> equipment,
                    List<Line> chooseOne, String chooseText, int maxClaimsPerUser,
                    String introText, String confirmText, String doneText, String declinedText, String fullText,
                    String limitText, String blockedText) {
        this.id = id;
        this.meso = meso;
        this.nxPrepaid = nxPrepaid;
        this.items = List.copyOf(items);
        Map<String, List<Line>> eq = new LinkedHashMap<>();
        equipment.forEach((k, v) -> eq.put(k, List.copyOf(v)));
        this.equipment = Map.copyOf(eq);
        this.chooseOne = List.copyOf(chooseOne);
        this.chooseText = chooseText;
        this.maxClaimsPerUser = maxClaimsPerUser;
        this.introText = introText;
        this.confirmText = confirmText;
        this.doneText = doneText;
        this.declinedText = declinedText;
        this.fullText = fullText;
        this.limitText = limitText;
        this.blockedText = blockedText;
    }

    public static StarterGiftStep read(String id, NewbieJson s) {
        long meso = s.number("meso", 0, 100_000_000, 0);
        List<Line> items = lines(s.array("items", 0, 20));
        Map<String, List<Line>> eq = new LinkedHashMap<>();
        for (Map.Entry<String, NewbieJson> e : s.entries("equipment").entrySet()) {
            if (!e.getKey().matches("[A-Za-z0-9_*.\\-]{1,32}")) throw NewbieJson.invalid(e.getValue().path, "set key must be 1..32 of A-Z a-z 0-9 _ - . *");
            eq.put(e.getKey(), lines(e.getValue().asArray(0, 12)));
        }
        int max = s.integer("maxClaimsPerUser", 0, 1000, 0);
        List<Line> choose = s.has("chooseOne") ? lines(s.array("chooseOne", 0, 12)) : List.of();
        return new StarterGiftStep(id, meso, s.integer("nxPrepaid", 0, 1_000_000, 0), items, eq, choose,
                s.text("chooseText", "請選擇一個要一起領取的道具："), max, s.text("introText"), s.text("confirmText"), s.text("doneText"),
                s.text("declinedText"), s.text("fullText"), s.text("limitText"), s.text("blockedText"));
    }

    private static List<Line> lines(List<NewbieJson> nodes) {
        List<Line> out = new ArrayList<>();
        for (NewbieJson n : nodes) {
            int item = n.integer("itemId", 1_000_000, 9_999_999);
            int qty = n.integer("quantity", 1, 30_000);
            int days = n.integer("periodDays", 0, 3650, 0);
            int gender = n.integer("gender", 0, 1, -1);
            if (item / 1_000_000 == 1 && qty > 5) throw NewbieJson.invalid(n.path + ".quantity", "at most 5 of one equip");
            out.add(new Line(item, qty, days, gender));
        }
        return out;
    }

    public long meso() {
        return meso;
    }

    public int nxPrepaid() { return nxPrepaid; }

    private Outcome points(NewbieRun run) {
        if (nxPrepaid == 0) return Outcome.NEXT;
        NewbieGame.Points p = run.game().grantWelcomePoints(run.character(), nxPrepaid);
        if (p == NewbieGame.Points.FAILED) {
            run.dialog().ok("新手樂豆點暫時無法入帳，重新登入即可再試；已領取的道具會保留。");
            return Outcome.STAY;
        }
        if (p == NewbieGame.Points.GRANTED) run.dialog().ok("新手樂豆點 " + String.format("%,d", nxPrepaid) + " 已入帳。同一登入帳號限領一次。");
        return Outcome.NEXT;
    }

    public List<Line> items() {
        return items;
    }

    public Map<String, List<Line>> equipment() {
        return equipment;
    }

    public int maxClaimsPerUser() {
        return maxClaimsPerUser;
    }

    /** Every item id the configuration can give (for the data validation). */
    public List<Integer> allItemIds() {
        List<Integer> ids = new ArrayList<>();
        items.forEach(l -> ids.add(l.itemId()));
        equipment.values().forEach(v -> v.forEach(l -> ids.add(l.itemId())));
        chooseOne.forEach(l -> ids.add(l.itemId()));
        return ids;
    }

    /** What {@code branch} / {@code gender} receive: the items, then the branch's set (or {@code "*"}). */
    public List<Line> linesFor(String branch, int gender) {
        List<Line> out = new ArrayList<>();
        for (Line l : items) {
            if (l.fits(gender)) out.add(l);
        }
        List<Line> set = branch != null && !branch.isEmpty() && equipment.containsKey(branch) ? equipment.get(branch)
                : equipment.getOrDefault(ANY_SET, List.of());
        for (Line l : set) {
            if (l.fits(gender)) out.add(l);
        }
        return out;
    }

    /** The log column: {@code meso=500000;2000000x100;...}. */
    public static String summary(long meso, List<Line> lines) {
        StringBuilder b = new StringBuilder("meso=").append(meso);
        for (Line l : lines) {
            b.append(';').append(l.itemId()).append('x').append(l.quantity());
            if (l.periodDays() > 0) b.append('d').append(l.periodDays());
        }
        return b.toString();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Outcome run(NewbieRun run) {
        if (run.progress().giftClaimed()) return points(run);
        Char chr = run.character();
        String branch = run.progress().branch();
        List<Line> lines = new ArrayList<>(linesFor(branch, chr.getAvatarData().getCharacterStat().getGender()));
        if (!chooseOne.isEmpty()) {
            // picked before anything is claimed: a closed menu leaves the step as it was
            List<String> labels = chooseOne.stream().map(l -> "#i" + l.itemId() + "# #t" + l.itemId() + "#").toList();
            int pick = -1;
            for (int attempt = 0; attempt < 5 && pick < 0; attempt++) {
                int i = run.dialog().menu(chooseText, labels);
                if (i >= 0 && i < chooseOne.size()) pick = i;
            }
            if (pick < 0) return Outcome.STAY;
            lines.add(chooseOne.get(pick));
        }
        StringBuilder text = new StringBuilder(introText);
        if (meso > 0) text.append("\r\n#b楓幣 ").append(String.format("%,d", meso)).append("#k");
        if (nxPrepaid > 0) text.append("\r\n#b樂豆點 ").append(String.format("%,d", nxPrepaid)).append("#k（同一登入帳號一次）");
        for (Line l : lines) {
            text.append("\r\n#i").append(l.itemId()).append("# #t").append(l.itemId()).append("# x").append(l.quantity());
            if (l.periodDays() > 0) text.append("（").append(l.periodDays()).append(" 天）");
        }
        text.append("\r\n\r\n").append(confirmText);
        if (!run.dialog().yesNo(text.toString())) {
            run.dialog().ok(declinedText);
            return Outcome.STAY;
        }
        List<NewbieGame.Grant> grants = lines.stream().map(l -> new NewbieGame.Grant(l.itemId(), l.quantity(), l.periodDays())).toList();
        String summary = summary(meso, lines);
        String version = run.config().version();
        NewbieGame.Gift r = run.game().grantGift(chr, meso, grants,
                () -> run.store().claimGift(chr.getId(), version, branch, summary, maxClaimsPerUser));
        switch (r) {
            case GRANTED -> {
                System.out.println("[Newbie] GIFT character=" + chr.getId() + " branch=" + (branch.isEmpty() ? ANY_SET : branch)
                        + " " + summary);
                run.game().saveSoon(chr);
                run.dialog().ok(doneText);
                return points(run);
            }
            case ALREADY -> {
                return points(run);
            }
            case LIMIT -> {
                run.dialog().ok(limitText);
                return Outcome.NEXT;
            }
            case FULL -> {
                run.dialog().ok(fullText);
                return Outcome.STAY;
            }
            case BLOCKED -> {
                run.dialog().ok(blockedText);
                return Outcome.STAY;
            }
            default -> {
                return Outcome.STAY;
            }
        }
    }
}
