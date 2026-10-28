package net.swordie.ms.constants;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.ReactorPool;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.life.Reactor;
import net.swordie.ms.loaders.ReactorData;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.event.EventListData;
import net.swordie.ms.world.event.EventListLoader;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Foothold;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventConstants {
    public static volatile List<EventListData> events;

    /** Snapshot at class initialisation (kept for old callers); the live list is EventListRepository.standard(). */
    static {
        events = net.swordie.ms.world.event.EventListRepository.standard().current();
    }

    /** The entry with {@code unkVal == index} of the current list (re-read when events.tw.json changes). */
    public static EventListData getEventByIndex(int index) {
        return net.swordie.ms.world.event.EventListRepository.standard().byKey(index);
    }

    /* 0 = Basic ; 1 = Halloween ; 2 = Bảy màu ; 3 = Bốn màu ; 4 = Mật ong ; 5 = Cây dừa; 100 = Bảy màu bong bóng; 1006 = Neon */
    public static final int COMBO_MULTI_KILL_TYPE = 100;

    // https://maplestory.nexon.net/micro-site/59404
    public static final boolean RANDOM_PORTAL_EVENT = false;
    public static final int RANDOM_PORTAL_SPAWN_CHANCE = 100; // out of a 10000
    public static final int RANDOM_PORTAL_COOLTIME = 15 * 60 * 1000; // 15 minutes

    // https://maplestory.nexon.net/news/3436/gachapon-stamp-event-7-28-8-11
    public static final boolean GACHAPON_STAMP_EVENT = false;

    public static final boolean HYPER_BURNING_MAX = true;
    public static final LocalDateTime HYPER_BURNING_MAX_START_DATE = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
    public static final LocalDateTime HYPER_BURNING_MAX_END_DATE = LocalDateTime.of(2026, 12, 31, 23, 59, 59);
    public static final int TERA_BLINK_FIELD = 993236800;
    public static final int HYPER_BURNING_MAX_MIN_LEVEL = 10;
    public static final int HYPER_BURNING_MAX_MAX_LEVEL = 260;
    public static final int HYPER_BURNING_MAX_TYPE = 4;
    public static final int HYPER_BURNING_MAX_BURNING_TYPE = 5;

    public static final boolean BEYOND_BURNING = true;
    public static final LocalDateTime BEYOND_BURNING_START_DATE = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
    public static final LocalDateTime BEYOND_BURNING_END_DATE = LocalDateTime.of(2026, 12, 31, 23, 59, 59);
    public static final int BEYOND_BURNING_MIN_LEVEL = 260;
    public static final int BEYOND_BURNING_MAX_LEVEL = 270;
    public static final int BEYOND_BURNING_TYPE = 1;
    public static final int BEYOND_BURNING_BURNING_TYPE = 10;

    // https://maplestory.nexon.net/news/28325/spell-trace-fever-time-february-24-25
    public static boolean STAR_FORCE_FEVER_TIME_EVENT = false;

    public static boolean MIRACLE_TIME_EVENT = false;

    public static boolean EXP_RATE_EVENT = true;

    public static boolean DROP_RATE_EVENT = true;

    public static boolean VOTE_RATE_EVENT = false;

    public static final boolean DAILY_GIFT_EVENT = false;

    public static final int DAILY_GIFT_REQ_LEVEL = 33;

    public static boolean RED_LEAF_HIGH_EVENT = false;
    public static final int RED_LEAF_HIGH_RECORD = 5551996; // Custom QR Key
    public static final int RED_LEAF_HIGH_MOB_KILLS_RECORD = 6661996; // Custom QR Key
    public static final int RED_LEAF_HIGH_EXTRA_SLOT = 6661997; // Custom QR Key

    public static boolean FARM_EVENT = false;

    public static boolean FREE_XU_VANG = false;

    public static final boolean DONATION_POINT_EVENT = false;

    public static final int SUB_ZERO_HUNT = 224224;

    public static final boolean ARK_EVENT = false;

    public static int[] EventMaps = {100000001, 100000002, 100000003};

    public static void initReactorDropEvent(Field field) {
        if (!field.isTown() && !field.getMobs().isEmpty()) {
            if (FARM_EVENT) {
                spawnReactor(field, 100014);
                spawnReactor(field, 200014);
            }
            if (FREE_XU_VANG) {
                if (Util.succeedProp(1, 300)) {
                    spawnReactor(field, 100014);
                }
            }
        }
    }

    private static void spawnReactor(Field field, int reactorID) {
        Reactor reactor = ReactorData.getReactorByID(reactorID);
        Foothold fh = field.getNonWallFootholds().get(Util.getRandom(0, field.getNonWallFootholds().size() - 1));
        Position position = new Position(fh.getX1(), fh.getY1());
        reactor.setPosition(position);
        reactor.setHomePosition(position);
        field.addLife(reactor);
        field.broadcast(ReactorPool.reactorEnterField(reactor));
    }

    public static void dropItemFromReactor(Char chr, Reactor reactor) {
        if (FARM_EVENT) {
            List<Integer> totalItems = new ArrayList<>(List.of(
                    4440300, //Mighty Jewel C
                    4441300, //Lucky Jewel C
                    4442300, //Keen Jewel C
                    4443300  //Nimble Jewel C
            ));
            if (Util.succeedProp(10)) {
                totalItems.add(4033442); //Red Essence Stone
                totalItems.add(4033445); //Green Essence Stone
                totalItems.add(4033444); //Yellow Essence Stone
                totalItems.add(4033446); //Blue Essence Stone
            }
            int[] items = new int[3];
            for (int i = 0; i < 3; i++) {
                int item = Util.getRandomFromCollection(totalItems);
                totalItems.removeIf(x -> x.equals(item));
                items[i] = item;
            }
            chr.getField().dropItemsAlongLine(items, new int[]{1, 1, 1}, true, 50, reactor.getX(), reactor.getY(), 100);
        } else if (FREE_XU_VANG) {
            int x = Util.succeedProp(1, 10000) ? 2 : 1;
            if (Util.succeedProp(1, 100000)) {

                x = Util.succeedProp(5, 100000) ? 10 : 9;

            } else if (Util.succeedProp(2, 100000)) {

                x = Util.succeedProp(15, 100000) ? 8 : 7;

            } else if (Util.succeedProp(3, 100000)) {

                x = Util.succeedProp(30, 100000) ? 6 : 5;

            } else if (Util.succeedProp(4, 10000)) {

                x = Util.succeedProp(50, 10000) ? 4 : 3;

            }
            int xuvang = x * 1000;
            chr.getUser().addDonationPoint(xuvang);
            chr.chatMessage(ChatType.SystemNotice, "你的帳號獲得了 " + Util.getNumberFormat(xuvang) + " 贊助點數！");
            DataPrinter.send(DataPrinter.FREEXUVANG, "賬號 " + chr.getUser().getName() + " 挖到了 " + Util.getNumberFormat(xuvang) + " 金幣。");
        }
    }

}
