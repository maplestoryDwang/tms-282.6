package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.PortableChair;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.constants.QuestConstants;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A remote user's portable chair in the 282 user-enter packet (LP03D0 #49 item id, #56 has-chair + body; research
 * packet-audit/chair-miniroom-282.md). The client picks the chair class from the item (35AEC90, table
 * {@code chair-types.tsv} from its Install data) and reads that class's body right after #56, so a body of the wrong
 * shape shifts every later field. Only the normal chair (no body; emulator CLEAN) is sent; any other chair keeps the
 * earlier behaviour (no chair: the user looks standing to newcomers). The other bodies below are the research
 * proposal, kept for when they are proven.
 * <ul>
 *     <li>normal: no body;</li>
 *     <li>tower: i32 n, then exactly 18 x i32 chair ids (QR tower chair setting);</li>
 *     <li>text: str message, chat info (1345CF0, = 265 Char.encodeChatInfo);</li>
 *     <li>meso: u8 0 (nothing more).</li>
 * </ul>
 */
public record Tms282ChairView(int itemId, String kind, List<Integer> towerIds, String message, ChatInfo chat) {
    public static final Path TABLE = Path.of("data", "protocol", "tms282.3", "chair-types.tsv");
    public static final int TOWER_SLOTS = 18;
    private static volatile Map<Integer, String> kinds;

    /** 265 Char.encodeChatInfo fields. */
    public record ChatInfo(String name, int accountId, int characterId, int worldId) {
        public void encode(OutPacket out, String message, Charset charset) {
            Tms282LoginPacketSupport.str(out, name, charset);
            Tms282LoginPacketSupport.str(out, message == null ? "" : message, charset);
            out.encodeInt(accountId);
            out.encodeInt(characterId);
            out.encodeByte(worldId);
            out.encodeInt(characterId);
            out.encodeInt(0);
            Tms282LoginPacketSupport.str(out, "", charset);
            out.encodeInt(0);
            Tms282LoginPacketSupport.str(out, "", charset);
            out.encodeInt(0);
        }

        public static ChatInfo of(Char chr) {
            return new ChatInfo(chr.getName(), chr.getAccount() == null ? 0 : chr.getAccount().getId(), chr.getId(),
                    chr.getAccount() == null ? 0 : chr.getAccount().getWorldId());
        }
    }

    public Tms282ChairView {
        towerIds = List.copyOf(towerIds);
    }

    static Map<Integer, String> kinds() {
        Map<Integer, String> map = kinds;
        if (map == null) {
            map = new HashMap<>();
            try {
                for (String line : Files.readAllLines(TABLE)) {
                    if (line.isBlank() || line.startsWith("#")) {
                        continue;
                    }
                    String[] f = line.split("\t");
                    map.put(Integer.parseInt(f[0].strip()), f.length > 1 ? f[1].strip() : "");
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            kinds = map;
        }
        return map;
    }

    /** The 282 kind of a chair item: the client table, else its item range. */
    public static String kindOf(int itemId) {
        String kind = kinds().get(itemId);
        if (kind != null) {
            return kind;
        }
        return switch (itemId / 1000) {
            case 3017 -> "tower";
            case 3014 -> "text";
            default -> "unknown";
        };
    }

    /** The chair the character sits on, when 282 can be sent it; null otherwise. */
    public static Tms282ChairView of(Char chr) {
        PortableChair chair = chr.getChair();
        if (chair == null || chair.getItemID() <= 0) {
            return null;
        }
        String kind = kindOf(chair.getItemID());
        List<Integer> tower = new ArrayList<>();
        if (kind.equals("tower")) {
            Quest q = chr.getQuestById(QuestConstants.TOWER_CHAIR_SETTING_QUEST);
            if (q != null) {
                q.convertQRValueToProperties();
                for (String v : q.getProperties().values()) {
                    try {
                        tower.add(Integer.parseInt(v.trim()));
                    } catch (NumberFormatException ignored) {
                        // not a chair id
                    }
                }
            }
        }
        // only the normal chair is sent: its LP03D0 reads CLEAN in the field-trace emulator; text / tower / meso bodies
        // are not proven (text needs the WZ chair class, meso read past the proposed body, tower did not finish)
        return switch (kind) {
            case "normal" -> new Tms282ChairView(chair.getItemID(), kind, tower, chair.getMsg(), ChatInfo.of(chr));
            default -> null;
        };
    }

    /** #56 = 1 and the body (the caller wrote #49 = itemId). */
    public void encodeBody(OutPacket out, Charset charset) {
        switch (kind) {
            case "tower" -> {
                int n = Math.min(towerIds.size(), TOWER_SLOTS);
                out.encodeInt(n);
                for (int i = 0; i < TOWER_SLOTS; i++) {
                    out.encodeInt(i < n ? towerIds.get(i) : 0);
                }
            }
            case "text" -> {
                Tms282LoginPacketSupport.str(out, message == null ? "" : message, charset);
                chat.encode(out, message, charset);
            }
            case "mesoChair" -> out.encodeByte(0);
            default -> {
                // normal: no body
            }
        }
    }
}
