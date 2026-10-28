package net.swordie.ms.world.tms282.boxes;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Inventory;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.items.PetItem;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282ItemEncoder;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.containerclasses.PetInfo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Luna Crystal pet fusion (5069100 月光水晶 = table type 1, 5069101 特殊月光水晶 = table type 2), reward data from
 * {@code luna/luna-crystal.tsv} (TMS279 luna-crystal.csv).
 * <p>
 * 282.3 client flow (static RE + Unicorn, research/tms2823/luna-crystal/README.md):
 * <ul>
 *   <li>GetCashItemType AEFA50 returns 0x5F for both crystals; the generic cash-use sender 5E11F10 sends nothing
 *   for 0x5F, so a double-click never produces a plain CP0150 for a crystal.</li>
 *   <li>The crystal is dragged onto a pet (4AF9E50 -> check 5F3E2E0), which opens the Luna UI (ctor 4C21820,
 *   IsKindOf 9AF99A0). The second (material) pet is put in the UI (4C26500) and the confirm (4C25840, StringPool
 *   0x29B4) sends CP0150 from 4C262C0: {@link FusionRequest} (44 bytes with opcode).</li>
 *   <li>The answer is LP0193 (handler 5F3E6B0): u8 ok; ok -> GW_ItemSlotBase (932790, {@link Tms282ItemEncoder});
 *   else i32 code, see {@link Failure}. It also clears the exclusive-request flag.</li>
 * </ul>
 * {@link #fuse} is the real flow. It is not routed yet: CP0150 goes through {@code Tms282CashUseRequest.parse}, which
 * currently refuses every id outside 553xxxx / pet-skill items. The route owner calls {@link #isFusionPacket} first
 * and hands the raw packet to {@link #handleFusion}.
 * <p>
 * {@link #use} (a plain cash-item use) is never produced by the real 282 client; it refuses and does not grant a pet.
 */
public final class LunaCrystalHandler implements BoxUseHandler {
    /** LP0193: Luna Crystal / Masterpiece result (5F3E6B0). */
    public static final int RESULT_OPCODE = 0x0193;
    public static final int REQUEST_OPCODE = 0x0150;
    /** Fusion CP0150 length including the opcode (4C262C0). */
    public static final int FUSION_LENGTH = 44;
    /** Client a030b0 grades: the base pet may be 1 or 4 (5F3E5F4), the material pet only 1 (4C266FF). */
    static final int GRADE_WONDER = 1;
    static final int GRADE_BASE_ALT = 4;

    public record Reward(int petId, int wonderGrade, boolean notice, LocalDate termStart, LocalDate termEnd) {
        boolean activeOn(LocalDate day) {
            return (termStart == null || !day.isBefore(termStart)) && (termEnd == null || !day.isAfter(termEnd));
        }
    }

    /**
     * CP0150 fusion body written by 4C262C0 (Unicorn: research/tms2823/luna-crystal/luna_sender_check.py):
     * u16 op, i32 0, i32 tick, u16 crystalSlot, i32 crystalId, i32 baseId, u16 baseSlot, u64 baseSn,
     * i32 materialId, u16 materialSlot, u64 materialSn. All slots are CASH-tab slots; SN = GW_ItemSlotBase +0x38
     * (the cash SN {@link Tms282ItemEncoder} writes, i.e. {@link Item#getId()}).
     */
    public record FusionRequest(int crystalSlot, int crystalId, int baseId, int baseSlot, long baseSn,
                                int materialId, int materialSlot, long materialSn) {
        public static FusionRequest parse(byte[] packet) {
            if (packet == null || packet.length != FUSION_LENGTH) {
                throw new IllegalArgumentException("Luna fusion length");
            }
            ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
            if (Short.toUnsignedInt(in.getShort()) != REQUEST_OPCODE || in.getInt() != 0) {
                throw new IllegalArgumentException("Luna fusion prefix");
            }
            in.getInt(); // client tick
            int crystalSlot = Short.toUnsignedInt(in.getShort());
            int crystalId = in.getInt();
            int baseId = in.getInt();
            int baseSlot = Short.toUnsignedInt(in.getShort());
            long baseSn = in.getLong();
            int materialId = in.getInt();
            int materialSlot = Short.toUnsignedInt(in.getShort());
            long materialSn = in.getLong();
            return new FusionRequest(crystalSlot, crystalId, baseId, baseSlot, baseSn, materialId, materialSlot, materialSn);
        }
    }

    /** LP0193 failure codes as the client maps them (jump table 5F3EA0C, Luna branch). */
    public enum Failure {
        /** 0x29B9 請確認裝飾欄與現金欄是否各有空出１格以上的空間。 */
        NO_SPACE(0x1A),
        /** 0x29AA 相同道具不可合成。 */
        SAME_ITEM(0x36),
        /** 0x29A9 道具不符。請確認基本，材料道具。 */
        MISMATCH(0x39),
        /** 0x29AC 超過了現金保管箱可保管的現金道具數量。 */
        LOCKER_FULL(0x3F),
        /** 0x29AB 該道具無法合成。 */
        NOT_FUSIBLE(0x4E),
        /** 0x29BA 目前無法使用月光水晶。請稍後再嘗試。 */
        UNAVAILABLE(0x55),
        /** any other code: 0x2998 使用失敗，請稍後重新再試 */
        FAILED(0x01);

        public final int code;

        Failure(int code) {
            this.code = code;
        }
    }

    /** Outcome of {@link #fuse}: exactly one of {@code pet} / {@code failure} is set. */
    public record FusionResult(Item pet, Failure failure) {
        public boolean ok() {
            return pet != null;
        }
    }

    private static volatile LunaCrystalHandler instance;
    private final Map<Integer, WeightedPool<Reward>> pools;

    private LunaCrystalHandler(Map<Integer, WeightedPool<Reward>> pools) {
        this.pools = Map.copyOf(pools);
    }

    public static LunaCrystalHandler load() {
        Map<Integer, WeightedPool.Builder<Reward>> builders = new HashMap<>();
        List<String[]> rows = BoxSupport.readTsv(BoxSupport.dataDir().resolve("luna").resolve("luna-crystal.tsv"), 8);
        for (String[] r : rows) {
            int crystal = Integer.parseInt(r[1]);
            Reward reward = new Reward(Integer.parseInt(r[2]), BoxSupport.intOr(r[3], 0), "1".equals(r[5]),
                    r[6].isEmpty() ? null : LocalDate.parse(r[6]), r[7].isEmpty() ? null : LocalDate.parse(r[7]));
            builders.computeIfAbsent(crystal, k -> WeightedPool.builder()).add(reward, Integer.parseInt(r[4]));
        }
        Map<Integer, WeightedPool<Reward>> pools = new HashMap<>();
        builders.forEach((k, b) -> pools.put(k, b.build()));
        LunaCrystalHandler h = new LunaCrystalHandler(pools);
        instance = h;
        return h;
    }

    /** The loaded handler (loads the table on first use). */
    public static LunaCrystalHandler get() {
        LunaCrystalHandler h = instance;
        return h != null ? h : load();
    }

    @Override
    public String name() {
        return "luna(" + pools.size() + " crystals)";
    }

    @Override
    public boolean handles(int itemId) {
        return pools.containsKey(itemId);
    }

    /** A plain use never comes from the 282 client (type 0x5F is not sent by 5E11F10): explain and keep the item. */
    @Override
    public void use(Char chr, Item box) {
        chr.chatMessage(ChatType.SystemNotice, "請將月光水晶拖曳到奇幻黑色寵物上，並放入第二隻奇幻寵物後使用。");
    }

    /** True for a 44-byte CP0150 whose crystal id (offset 12) is a Luna Crystal with table data. */
    public static boolean isFusionPacket(byte[] packet) {
        if (packet == null || packet.length != FUSION_LENGTH) {
            return false;
        }
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        return Short.toUnsignedInt(in.getShort(0)) == REQUEST_OPCODE && in.getInt(2) == 0
                && get().handles(in.getInt(12));
    }

    /**
     * Parses, fuses and returns the LP0193 answer (never null). Inventory changes go out through the normal
     * inventory-operation path of {@link Char#consumeItem} / {@link Char#addItemToInventory}; send them before
     * the returned packet (the client closes/animates the UI when LP0193 arrives).
     */
    public static OutPacket handleFusion(Char chr, byte[] packet, Charset charset) {
        FusionResult result;
        try {
            result = get().fuse(chr, FusionRequest.parse(packet));
        } catch (RuntimeException e) {
            System.out.println("[TMS282][Luna] fusion refused: " + e);
            result = new FusionResult(null, Failure.FAILED);
        }
        return resultPacket(result, charset);
    }

    /** LP0193: u8 1 + GW_ItemSlotBase, or u8 0 + i32 code (5F3E739 Decode1, 932790 / 5F3E860 Decode4). */
    public static OutPacket resultPacket(FusionResult result, Charset charset) {
        Objects.requireNonNull(result, "result");
        OutPacket out = OutPacket.forProfile(ProtocolVersion.TMS_282_3, RESULT_OPCODE);
        if (result.ok()) {
            out.encodeByte(1);
            Tms282ItemEncoder.encode(out, result.pet(), Objects.requireNonNull(charset, "charset"));
        } else {
            out.encodeByte(0);
            out.encodeInt(result.failure().code);
        }
        return out;
    }

    /**
     * Validates the two pets and the crystal, consumes all three and adds the result pet. Nothing changes when a
     * check fails.
     */
    public synchronized FusionResult fuse(Char chr, FusionRequest r) {
        WeightedPool<Reward> pool = pools.get(r.crystalId());
        Inventory cash = chr.getInventoryByType(InvType.CASH);
        if (pool == null || cash == null) {
            return fail(chr, Failure.NOT_FUSIBLE, "not a luna crystal " + r.crystalId());
        }
        Item crystal = cash.getItemBySlot((short) r.crystalSlot());
        if (crystal == null || crystal.getItemId() != r.crystalId() || crystal.getQuantity() < 1
                || (crystal.getDateExpire() != null && crystal.getDateExpire().isExpired())) {
            return fail(chr, Failure.MISMATCH, "crystal slot " + r.crystalSlot());
        }
        if (r.baseSn() == r.materialSn() || r.baseSlot() == r.materialSlot()) {
            return fail(chr, Failure.SAME_ITEM, "same pet twice");
        }
        PetItem base = pet(chr, cash, r.baseSlot(), r.baseId(), r.baseSn());
        PetItem material = pet(chr, cash, r.materialSlot(), r.materialId(), r.materialSn());
        if (base == null || material == null) {
            return fail(chr, Failure.MISMATCH, "pet slot/id/SN or summoned");
        }
        int baseGrade = grade(base.getItemId());
        int materialGrade = grade(material.getItemId());
        if ((baseGrade != GRADE_WONDER && baseGrade != GRADE_BASE_ALT) || materialGrade != GRADE_WONDER) {
            return fail(chr, Failure.NOT_FUSIBLE, "wonder grades " + baseGrade + "/" + materialGrade);
        }
        LocalDate today = LocalDate.now();
        Reward pick = pool.pick(x -> x.activeOn(today) && BoxSupport.available(x.petId()));
        Item result = pick == null ? null : BoxSupport.createItem(pick.petId(), 1, 0);
        if (result == null) {
            return fail(chr, Failure.UNAVAILABLE, "no reward pet for " + r.crystalId());
        }
        PetInfo info = ItemData.getPetInfoByID(pick.petId());
        if (info != null && pick.wonderGrade() != 0 && info.getWonderGrade() != pick.wonderGrade()) {
            System.out.printf("[TMS282][Luna] pet %d wonderGrade data=%d table=%d%n",
                    pick.petId(), info.getWonderGrade(), pick.wonderGrade());
        }
        chr.consumeItem(base, 1);
        chr.consumeItem(material, 1);
        chr.consumeItem(crystal, 1);
        // two pets and (usually) the crystal leave the CASH tab first, so the result always has a slot
        if (!chr.canHold(List.of(result))) {
            System.out.println("[TMS282][Luna] no cash slot after consuming inputs for " + chr.getName());
        }
        chr.addItemToInventory(result);
        chr.chatMessage(ChatType.GameDesc, String.format("月光之力凝聚成了 %s。", BoxSupport.itemName(pick.petId())));
        if (pick.notice()) {
            BoxSupport.announce(chr, r.crystalId(), result);
        }
        return new FusionResult(result, null);
    }

    private static PetItem pet(Char chr, Inventory cash, int slot, int itemId, long sn) {
        Item it = cash.getItemBySlot((short) slot);
        if (!(it instanceof PetItem p) || p.getItemId() != itemId || p.getId() != sn || p.getActiveState() != 0) {
            return null;
        }
        boolean summoned = chr.getPets().stream().anyMatch(x -> x.getItem() == p || x.getItem().getId() == sn);
        return summoned ? null : p;
    }

    private static int grade(int petId) {
        PetInfo info = ItemData.getPetInfoByID(petId);
        return info == null ? 0 : info.getWonderGrade();
    }

    private static FusionResult fail(Char chr, Failure f, String why) {
        System.out.printf("[TMS282][Luna] %s refused (%s): %s%n", chr.getName(), f, why);
        return new FusionResult(null, f);
    }
}
