package net.swordie.ms.connection.packet;

import net.swordie.ms.enums.InvType;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * TMS 282.3 equipment-enhancement requests (scrolls, star force, potential, cubes, flames, symbols, hammers): each
 * {@link Layout} decodes one 282 client packet strictly and rewrites it as the body the 265 handler reads. Encode
 * sequences come from the static senders (research/tms2823/packet-audit/ingame-cp-map-2823.tsv, re-read for the
 * adapted ones with the sender disassembly, equip-enhance-2823.md). Tags: [證實] = the 282 sequence equals the 265
 * decode; [推定] = the 282 fields are known by width/order but which one the 265 handler wants is chosen from the
 * character's slots (a {@link SlotLookup}) or presumed.
 * <p>
 * No layout sends anything; the channel handler (Tms282EquipHandlers) runs the 265 handler with {@link Parsed#body}
 * and answers with the inventory it changed.
 */
public final class Tms282EnhanceRequests {

    private Tms282EnhanceRequests() {
    }

    /** What the character holds: the item id at {@code pos} of {@code type} (EQUIPPED for {@code pos < 0}), or 0. */
    @FunctionalInterface
    public interface SlotLookup {
        int itemAt(InvType type, int pos);

        /** The equip at a 265 equip position (negative = worn), or 0. */
        default int equipAt(int pos) {
            return pos == 0 ? 0 : itemAt(pos < 0 ? InvType.EQUIPPED : InvType.EQUIP, pos);
        }
    }

    /** One decoded request: its 265 body (no opcode) and a log line with every 282 field. */
    public record Parsed(Layout layout, byte[] body, String describe) {
        public Parsed {
            Objects.requireNonNull(layout, "layout");
            body = body.clone();
        }

        @Override
        public byte[] body() {
            return body.clone();
        }
    }

    /** One request kind: its 282 opcode and how its body becomes the 265 body. */
    public enum Layout {
        /** CP0162 5E09560 [證實 exact]: tick, i16 uPos, i16 1, i16 ePos, i16 1, u8 enchantSkill. */
        UPGRADE_ITEM(0x0162, "scroll") {
            @Override
            Parsed parse(Reader r, SlotLookup slots) {
                return exact(this, r, "4 2 2 2 2 1");
            }
        },
        /** CP0164 5E099E0 [證實 prefix]: tick, i16 uPos, i16 ePos, u8 enchantSkill, u8 (282 only, 265 ignores it). */
        HYPER_UPGRADE_ITEM(0x0164, "star force scroll") {
            @Override
            Parsed parse(Reader r, SlotLookup slots) {
                return exact(this, r, "4 2 2 1 1");
            }
        },
        /**
         * CP0165 3695A90 [推定]: tick, B(4), a loop of B(2) use positions, i32 flameId, i16 ePos, u8 x4, str. 265
         * reads {@code tick, i32 1, i16 usePos, i32 flameId, i16 ePos, u8, u8}; the loop length is taken as the
         * count that makes the packet end exactly (one position for a single flame).
         */
        MEMORIAL_EX_UPGRADE_ITEM(0x0165, "flame") {
            @Override
            Parsed parse(Reader r, SlotLookup slots) {
                int tick = r.i32();
                int one = r.i32();
                int start = r.pos();
                for (int n = 1; n <= 8; n++) {
                    Reader t = r.at(start);
                    List<Short> uses = new ArrayList<>();
                    try {
                        for (int i = 0; i < n; i++) {
                            uses.add(t.i16());
                        }
                        int flameId = t.i32();
                        short ePos = t.i16();
                        int b1 = t.u8(), b2 = t.u8(), b3 = t.u8(), b4 = t.u8();
                        String s = t.strHex();
                        if (t.remaining() != 0) {
                            continue;
                        }
                        byte[] body = new Writer().i32(tick).i32(one).i16(uses.get(0)).i32(flameId).i16(ePos).u8(b1).u8(b2).bytes();
                        return new Parsed(this, body, String.format("flame tick=%d b4=%d usePos=%s flame=%d ePos=%d flags=%d,%d,%d,%d str=%s",
                                tick, one, uses, flameId, ePos, b1, b2, b3, b4, s));
                    } catch (IllegalArgumentException ignored) {
                        // try a longer position list
                    }
                }
                throw new IllegalArgumentException("CP0165: no position count makes the packet end");
            }
        },
        /** CP0169 5E0A280 [證實 exact]: tick, i16 uPos, i16 ePos, u8 enchantSkill (potential scroll). */
        ITEM_OPTION_UPGRADE_ITEM(0x0169, "potential scroll") {
            @Override
            Parsed parse(Reader r, SlotLookup slots) {
                return exact(this, r, "4 2 2 1");
            }
        },
        /** CP016A 5E0A920 [證實 exact]: tick, i16 uPos, i16 ePos, u8 enchantSkill (bonus potential scroll). */
        ADDITIONAL_OPT_UPGRADE_ITEM(0x016A, "bonus potential scroll") {
            @Override
            Parsed parse(Reader r, SlotLookup slots) {
                return exact(this, r, "4 2 2 1");
            }
        },
        /**
         * CP0175 369BED0 [native 369BF08/369C189/369C2FB/369CB85]: a=r14 is the cube slot,
         * b=esi is the equipment slot. c is an output of 5E4BBA0, NOT a fallback equipment slot.
         * The flag meanings and native result window still require their dedicated codec.
         */
        CONSUME_CUBE_ITEM(0x0175, "cube") {
            @Override
            Parsed parse(Reader r, SlotLookup slots) {
                int tick = r.i32();
                short a = r.i16();
                short b = r.i16();
                int c = r.i32();
                int f1 = r.u8(), f2 = r.u8();
                String s = r.strHex();
                r.end();
                Pair p = cubeAndEquip(a, b);
                byte[] body = new Writer().i32(tick).i16(p.use()).i32(p.equip()).u8(f1).bytes();
                return new Parsed(this, body, String.format("cube tick=%d a=%d b=%d c=%d flags=%d,%d str=%s -> cube %d equip %d",
                        tick, a, b, c, f1, f2, s, p.use(), p.equip()));
            }
        },
        /** CP0176 369CBE5..369CC63: tick, cube slot, cube ID, equipment slot, extra, two flags, string. */
        BONUS_MYSTICAL_CUBE_ITEM(0x0176, "bonus mystical cube") {
            @Override
            Parsed parse(Reader r, SlotLookup slots) {
                int tick=r.i32();short use=r.i16();int id=r.i32(),equip=r.i32(),extra=r.i32();
                int f1=r.u8(),f2=r.u8();String text=r.strHex();r.end();
                if(equip!=(short)equip || id<=0)throw new IllegalArgumentException("Invalid bonus cube item/slot");
                Pair p=cubeAndEquip(use,(short)equip);
                byte[] body=new Writer().i32(tick).i16(p.use()).i32(id).i32(p.equip()).u8(f1).bytes();
                return new Parsed(this,body,String.format("bonus mystical tick=%d cube=%d use=%d equip=%d extra=%d flags=%d,%d str=%s",
                        tick,id,use,equip,extra,f1,f2,text));
            }
        },
        /**
         * CP0177 369BED0 (0x369CAD5) [推定]: tick, i32 cubeId, i16 a, i16 b, i32 c, u8, u8, u8, str. 265 reads
         * {@code tick, i32 cubeId, i16 uPos, i16 ePos}, skips to the last byte (cube index).
         */
        BONUS_BRIGHT_CUBE_ITEM(0x0177, "bonus cube") {
            @Override
            Parsed parse(Reader r, SlotLookup slots) {
                int tick = r.i32();
                int cubeId = r.i32();
                short a = r.i16();
                short b = r.i16();
                int c = r.i32();
                int f1 = r.u8(), f2 = r.u8(), f3 = r.u8();
                String s = r.strHex();
                r.end();
                Pair p = cubeAndEquip(a, b);
                byte[] body = new Writer().i32(tick).i32(cubeId).i16(p.use()).i16(p.equip()).u8(f1).bytes();
                return new Parsed(this, body, String.format("bonus cube tick=%d cube=%d a=%d b=%d c=%d flags=%d,%d,%d str=%s -> cube %d equip %d",
                        tick, cubeId, a, b, c, f1, f2, f3, s, p.use(), p.equip()));
            }
        },
        /**
         * CP0179 USER_EQUIPMENT_ENCHANT_WITH_SINGLE_UI_REQUEST (senders 2330672, 2340798, 2337D3C, 2336B69,
         * 232FC39): u8 type, then per type -
         * <ul>
         *     <li>0 spell trace [證實 = 265]: tick, i16 pos, i32 scroll index;</li>
         *     <li>1 star force [推定]: tick, i32 ePos, i32, i32, u8 safeGuard(+0x2760), u8, u8, u8, str, [i32, i32].
         *     265 reads {@code tick, i32 ePos, u8 miniGame, [i32], i32, i32, u8, u8 safeGuard, u8}: sent with
         *     miniGame 0, the two i32 as they are and safeGuard from +0x2760;</li>
         *     <li>0x32 spell trace display [證實]: i32 ePos;</li>
         *     <li>0x33 [證實 = 265 ScrollTimerEffective]: nothing;</li>
         *     <li>0x34 star force display [推定]: i32 ePos, u8 safeGuard. 265 reads one more u8: 0.</li>
         * </ul>
         */
        EQUIPMENT_ENCHANT(0x0179, "star force / spell trace") {
            @Override
            Parsed parse(Reader r, SlotLookup slots) {
                int type = r.u8();
                Writer w = new Writer().u8(type);
                String d;
                switch (type) {
                    case 0 -> {
                        int tick = r.i32();
                        short pos = r.i16();
                        int index = r.i32();
                        w.i32(tick).i16(pos).i32(index);
                        d = String.format("spell trace pos=%d index=%d", pos, index);
                    }
                    case 1 -> {
                        int tick = r.i32();
                        int ePos = r.i32();
                        int v1 = r.i32(), v2 = r.i32();
                        int safeGuard = r.u8(), b2 = r.u8(), b3 = r.u8(), b4 = r.u8();
                        String s = r.strHex();
                        int[] extra = new int[r.remaining() / 4];
                        for (int i = 0; i < extra.length; i++) {
                            extra[i] = r.i32();
                        }
                        w.i32(tick).i32(ePos).u8(0).i32(v1).i32(v2).u8(0).u8(safeGuard).u8(1);
                        d = String.format("star force ePos=%d v=%d,%d safeGuard=%d flags=%d,%d,%d str=%s extra=%s",
                                ePos, v1, v2, safeGuard, b2, b3, b4, s, java.util.Arrays.toString(extra));
                    }
                    case 0x32 -> {
                        int ePos = r.i32();
                        w.i32(ePos);
                        d = "spell trace display ePos=" + ePos;
                    }
                    case 0x33 -> d = "scroll timer";
                    case 0x34 -> {
                        int ePos = r.i32();
                        int safeGuard = r.u8();
                        w.i32(ePos).u8(safeGuard).u8(0);
                        d = String.format("star force display ePos=%d safeGuard=%d", ePos, safeGuard);
                    }
                    default -> throw new IllegalArgumentException(String.format("CP0179 type 0x%X is not ported", type));
                }
                r.end();
                return new Parsed(this, w.bytes(), "type 0x" + Integer.toHexString(type) + " " + d);
            }
        },
        /** CP017C 368BBA0/4A08200 [證實 exact, 3 variants]: i32 type, then i32 ePos / i32 slot / i32 itemId... */
        ARCANE_SYMBOL_ENCHANCE(0x017C, "arcane symbol") {
            @Override
            Parsed parse(Reader r, SlotLookup slots) {
                return words(this, r);
            }
        },
        /** CP017D 307D3D0/36AEA80 [證實 exact, 3 variants]: i32 type, i32 ... (authentic symbol). */
        SACRED_SYMBOL_ENCHANCE(0x017D, "authentic symbol") {
            @Override
            Parsed parse(Reader r, SlotLookup slots) {
                return words(this, r);
            }
        },
        /**
         * CP0181 5E08F80 [推定]: tick, i32 a, i16 b, u8, [str]. 265 reads {@code tick, i16 uPos, i16 ePos}; the
         * position whose CONSUME slot holds a magnifying glass (246xxxx) is uPos, the other the equip.
         */
        ITEM_RELEASE(0x0181, "magnifying glass") {
            @Override
            Parsed parse(Reader r, SlotLookup slots) {
                int tick = r.i32();
                int a = r.i32();
                short b = r.i16();
                int flag = r.u8();
                String s = r.remaining() > 0 ? r.strHex() : "-";
                r.end();
                boolean aIsGlass = isGlass(slots.itemAt(InvType.CONSUME, a));
                int use = aIsGlass ? a : b;
                int equip = aIsGlass ? b : a;
                byte[] body = new Writer().i32(tick).i16(use).i16(equip).bytes();
                return new Parsed(this, body, String.format("release tick=%d a=%d b=%d flag=%d str=%s -> glass %d equip %d",
                        tick, a, b, flag, s, use, equip));
            }
        },
        /** CP0187 5144740.. [證實 exact]: i16, i16, i16 (Toad's hammer). */
        TOADS_HAMMER(0x0187, "toad's hammer") {
            @Override
            Parsed parse(Reader r, SlotLookup slots) {
                return exact(this, r, "2 2 2");
            }
        };

        private final int opcode;
        private final String label;

        Layout(int opcode, String label) {
            this.opcode = opcode;
            this.label = label;
        }

        public int opcode() {
            return opcode;
        }

        public String label() {
            return label;
        }

        /** Decodes the whole plaintext packet (opcode included). */
        public Parsed parse(byte[] packet, SlotLookup slots) {
            Objects.requireNonNull(slots, "slots");
            Reader r = Reader.open(packet, opcode);
            return parse(r, slots);
        }

        abstract Parsed parse(Reader r, SlotLookup slots);

        public static Layout byOpcode(int opcode) {
            for (Layout l : values()) {
                if (l.opcode == opcode) {
                    return l;
                }
            }
            return null;
        }
    }

    /** The use position and the equip position (265 equip positions: negative = worn). */
    record Pair(short use, short equip) {
    }

    /** Never redirect a request to another populated slot based on current inventory contents. */
    static Pair cubeAndEquip(short a, short b) {
        if (a <= 0 || b == 0 || b == Short.MIN_VALUE)
            throw new IllegalArgumentException("Invalid cube/equipment slot");
        return new Pair(a, b);
    }

    static boolean isGlass(int itemId) {
        return itemId / 10000 == 246;
    }

    /** A request whose 282 body equals the 265 body: the widths are checked, the bytes kept. */
    static Parsed exact(Layout layout, Reader r, String widths) {
        int start = r.pos();
        StringBuilder d = new StringBuilder(layout.label());
        for (String w : widths.split(" ")) {
            long v = switch (w) {
                case "1" -> r.u8();
                case "2" -> r.i16();
                case "4" -> r.i32();
                default -> throw new IllegalStateException("width " + w);
            };
            d.append(' ').append(v);
        }
        r.end();
        return new Parsed(layout, r.slice(start), d.toString());
    }

    /** A request of whole i32 words (the symbol variants), kept as it is. */
    static Parsed words(Layout layout, Reader r) {
        int start = r.pos();
        if (r.remaining() == 0 || r.remaining() % 4 != 0) {
            throw new IllegalArgumentException(String.format("CP%04X: %d bytes are not whole i32 words", layout.opcode(), r.remaining()));
        }
        StringBuilder d = new StringBuilder(layout.label());
        while (r.remaining() > 0) {
            d.append(' ').append(r.i32());
        }
        return new Parsed(layout, r.slice(start), d.toString());
    }

    /** Strict little-endian reader of one packet body. */
    static final class Reader {
        private final byte[] packet;
        private final ByteBuffer in;

        private Reader(byte[] packet, int pos) {
            this.packet = packet;
            this.in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
            this.in.position(pos);
        }

        static Reader open(byte[] packet, int opcode) {
            Objects.requireNonNull(packet, "packet");
            if (packet.length < 2 || ((packet[0] & 0xFF) | (packet[1] & 0xFF) << 8) != opcode) {
                throw new IllegalArgumentException(String.format("Wrong opcode (expected %04X)", opcode));
            }
            return new Reader(packet, 2);
        }

        Reader at(int pos) {
            return new Reader(packet, pos);
        }

        int pos() {
            return in.position();
        }

        int remaining() {
            return in.remaining();
        }

        int u8() {
            need(1);
            return in.get() & 0xFF;
        }

        short i16() {
            need(2);
            return in.getShort();
        }

        int i32() {
            need(4);
            return in.getInt();
        }

        /** EncodeStr: u16 length and bytes, shown as hex (charset not needed to rebuild the 265 body). */
        String strHex() {
            int n = i16() & 0xFFFF;
            need(n);
            byte[] b = new byte[n];
            in.get(b);
            return n == 0 ? "\"\"" : HexFormat.of().formatHex(b);
        }

        byte[] slice(int start) {
            return java.util.Arrays.copyOfRange(packet, start, in.position());
        }

        void end() {
            if (in.hasRemaining()) {
                throw new IllegalArgumentException(in.remaining() + " trailing bytes");
            }
        }

        private void need(int n) {
            if (in.remaining() < n) {
                throw new IllegalArgumentException("truncated: need " + n + ", have " + in.remaining());
            }
        }
    }

    /** Little-endian writer of a 265 body. */
    static final class Writer {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        Writer u8(int v) {
            out.write(v);
            return this;
        }

        Writer i16(int v) {
            out.write(v);
            out.write(v >> 8);
            return this;
        }

        Writer i32(int v) {
            for (int i = 0; i < 4; i++) {
                out.write(v >> (8 * i));
            }
            return this;
        }

        byte[] bytes() {
            return out.toByteArray();
        }
    }
}
