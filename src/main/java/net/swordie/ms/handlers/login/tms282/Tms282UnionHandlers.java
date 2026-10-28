package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.union.Union;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.connection.packet.UnionPacket;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.ui.UnionHandler;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * 282 戰地聯盟 board (research/tms2823/union-282/REPORT.md). CP03E0 (4774670): i32 mode, i32 preset, board {u8 flag;
 * flag != 0: 8 x i32 synergy, i32 count, count x member}. 265's ASSIGN and PRESET_CHANGE are one request in 282
 * (mode 0 saves the board, mode 2 applies a preset); without the mode the body is the 265 PRESET_CHANGE body, which
 * saves the grid and the members of the preset. A board without the flag only switches the active preset.
 * Replies: LP021A (UNION_ASSIGN_RESULT) and the LP009D unlock.
 * <p>
 * The window's open request is not known yet (static search found none, union-282/REPORT.md); {@link #open} sends
 * the 265 union result (LP0AAC with open = 1) for the admin panel.
 */
public final class Tms282UnionHandlers {
    static final int BOARD = 0x03E0;

    void register(Tms282Routes routes) {
        routes.channel(BOARD, InHeader.UNION_PRESET_CHANGE).when(Tms282Guards.IN_FIELD)
                .handle("union board -> 265 preset change, LP021A + LP009D", this::onBoard);
    }

    void onBoard(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        if (packet.length < 2 + 9 || chr.getUnion() == null) {
            session.send(Tms282StatPackets.unlock());
            System.out.printf("[TMS282] REFUSED CP03E0 len=%d character=%d: layout / no union%n", packet.length, chr.getId());
            return;
        }
        ByteBuffer in = ByteBuffer.wrap(packet, 2, packet.length - 2).slice().order(ByteOrder.LITTLE_ENDIAN);
        int mode = in.getInt();
        int preset = in.getInt();
        int flag = in.get();
        if (flag == 0) {
            Union union = chr.getUnion();
            int p = union.hasPresetUnlocked(preset - 1) ? preset - 1 : preset;
            if (union.hasPresetUnlocked(p) && union.getBoardByPreset(p) != null) {
                chr.setActiveUnionPreset(p);
                chr.write(UnionPacket.unionAssignResult(union.getUnionRank(), chr.getAccount().getEligibleUnionChars(),
                        union.getBoardByPreset(p), null, null, null, null));
            }
            chr.dispose();
            System.out.printf("[TMS282] CP03E0 mode %d preset %d (no board) character=%d%n", mode, preset, chr.getId());
            return;
        }
        try {
            UnionHandler.handleUnionPresetChange(chr, new InPacket(Arrays.copyOfRange(packet, 6, packet.length)));
        } catch (RuntimeException e) {
            session.send(Tms282StatPackets.unlock());
            System.out.printf("[TMS282] FAILED CP03E0 mode %d preset %d character=%d: %s%n", mode, preset, chr.getId(), e);
            return;
        }
        System.out.printf("[TMS282] CP03E0 mode %d preset %d board saved character=%d%n", mode, preset, chr.getId());
    }

    /** The union window for a 282 client: 265 union request (LP0AAC open, LP0ABB coin), called by the admin panel. */
    public static void open(Char chr) {
        UnionHandler.handleUnionRequest(chr, null);
    }
}
