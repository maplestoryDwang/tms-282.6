package net.swordie.ms.connection.packet;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.util.FileTime;

import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.DEFAULT_CHARSET;
import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.checkByte;
import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.checkWorldId;
import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.str;

/**
 * TMS 282.3 character selection: LP0013 character list, LP0014 select result, LP0017 name check,
 * LP0019 creation, LP001A-001D deletion and slot state, LP003D/003E slot purchase, LP0046 rename,
 * LP0048/004A select state, LP0062 world transfer, LP006D initialize result, LP01E9 burning result
 * (CWvsContext handler that works on the CharSelect UI) and LP0B7F quick login.
 * The WORLD_TYPE_* values are the LP0013/LP0014 world-type field, so they live here.
 * See {@link Tms282LoginPacketSupport} for the opcodes deliberately left out.
 */
public final class Tms282CharacterPackets {
    private Tms282CharacterPackets() {
    }

    /** Value the probe has always sent; not a special world type. */
    public static final int WORLD_TYPE_NORMAL = 1;
    /** 1217E90: class select keeps 13100 (Pink Bean) and 13500 (Yeti) only for this type. */
    public static final int WORLD_TYPE_PINK_BEAN_YETI = 5;
    /** 1217ED0: class select keeps 12100 (Tanjiro) and 12200 (Saitama) only for this type. */
    public static final int WORLD_TYPE_COLLABORATION = 6;

    /** Only item the LP003E handler accepts (0x52DAF0); any other value is ignored by the client. */
    public static final int CHARACTER_SLOT_ITEM_ID = 5430000;

    /** LP001A result codes (3CA8FAC table) that show a notice without reading further fields. */
    private static final Set<Integer> DELETE_RESULTS_WITHOUT_EXTRA = Set.of(
            0x06, 0x09, 0x0A, 0x10, 0x14, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x23, 0x24,
            0x33, 0x38, 0x39, 0x3A, 0x46, 0x8E, 0x94, 0x98);

    /**
     * CLogin+230 value for this account (spw-state-2823.md): 1 = a secondary password is registered, so the
     * enter-game path shows the SPW dialog; 0 = none registered. LP0031 byte0 overwrites it before each action.
     */
    public static int secondaryPasswordState(User user) {
        Objects.requireNonNull(user, "user");
        return user.getPic() != null && !user.getPic().isBlank() ? 1 : 0;
    }

    /** Only the 156-byte empty-account layout at reader RVA 0x3CA5B00 is verified. */
    public static OutPacket emptyCharacterList(User user, Account account, int wireChannelId) {
        return emptyCharacterList(user,account,wireChannelId,WORLD_TYPE_NORMAL);
    }

    /** Empty list for a world of the given type; no avatar is encoded, so {@link Tms282LoginPacketSupport#DEFAULT_CHARSET} is used. */
    public static OutPacket emptyCharacterList(User user, Account account, int wireChannelId, int worldType) {
        Objects.requireNonNull(account, "account");
        if(account.getCharacters()!=null && !account.getCharacters().isEmpty())
            throw new UnsupportedOperationException("Use the TMS 282 populated character-list builder");
        return characterList(user,account,wireChannelId,DEFAULT_CHARSET,0,worldType);
    }

    /**
     * LP0013 rejection (reader 3CA6870), the probe's reply for an unavailable world or channel: u8 result 1
     * and str message, the same leading fields the success layout sends as {u8 0, str ""}. Nothing else
     * follows; no further field of the failure path has been documented.
     */
    public static OutPacket characterListFailure(String message) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x0013);
        out.encodeByte(1);
        str(out, Objects.requireNonNull(message, "message"), DEFAULT_CHARSET);
        return out;
    }

    /** Native layout; auxiliaryFlag is explicit until its meaning has been verified on a real avatar. */
    public static OutPacket characterList(User user, Account account, int wireChannelId, Charset charset, int auxiliaryFlag) {
        return characterList(user,account,wireChannelId,charset,auxiliaryFlag,WORLD_TYPE_NORMAL);
    }

    /**
     * worldType: LP0013 reader 3CA6870 (3CBA650) passes it to 1217860, which stores the global that
     * ClassSelect init (3D381BC..3D381F0) uses to skip event jobs. One value per world.
     */
    public static OutPacket characterList(User user, Account account, int wireChannelId, Charset charset,
                                          int auxiliaryFlag, int worldType) {
        Objects.requireNonNull(user, "user");
        return characterList(user, account, wireChannelId, charset, auxiliaryFlag, worldType,
                Tms282DisplayedCharacters.showAll(user.getCharacterSlots()));
    }

    /**
     * As above with explicit displayed-character fields after the AvatarEntry list (u8 limitFlag, i32
     * displayCount, i32 n, n x i32; burning-display-chars-2823.md 3.3). The other overloads pass
     * {@link Tms282DisplayedCharacters#showAll}(character slots), the bytes every earlier LP0013 carried.
     * Every displayed ID must be one of the listed characters.
     */
    public static OutPacket characterList(User user, Account account, int wireChannelId, Charset charset,
                                          int auxiliaryFlag, int worldType, Tms282DisplayedCharacters displayed) {
        return characterList(user, account, wireChannelId, charset, auxiliaryFlag, worldType, displayed, null, null);
    }

    public static OutPacket characterList(User user, Account account, int wireChannelId, Charset charset,
                                          int auxiliaryFlag, int worldType, Tms282DisplayedCharacters displayed,
                                          Tms282QuickSelection favorite, Tms282QuickSelection previous) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(displayed, "displayed");
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(charset, "charset");
        checkWorldId(account.getWorldId());
        checkByte(wireChannelId, "Wire channel ID");
        if (account.getCharacters() == null) {
            throw new IllegalStateException("Load the account's characters before building its character list");
        }
        checkByte(auxiliaryFlag, "Auxiliary avatar flag");
        List<Char> characters=account.getCharacters().stream()
                .sorted(Comparator.comparingInt(Char::getOrderId).thenComparingInt(Char::getId)).toList();
        if(characters.size()>255 || characters.size()>user.getCharacterSlots())throw new IllegalArgumentException("Too many character entries");
        for(int id:displayed.characterIds())
            if(characters.stream().noneMatch(c->c.getId()==id))
                throw new IllegalArgumentException("Displayed character "+id+" is not in this list");
        Objects.requireNonNull(account.getTrunk(), "account trunk");
        OutPacket out = Tms282LoginPacketSupport.packet(0x0013);
        out.encodeByte(0);
        str(out, "", charset);
        out.encodeFT(FileTime.currentTime());
        out.encodeInt(account.getWorldId());
        out.encodeInt(wireChannelId);
        out.encodeByte(0); // No optional hardware-check request.
        out.encodeInt(worldType);
        out.encodeInt(1);
        out.encodeInt(account.getTrunk().getSlotCount());
        out.encodeByte(0);
        out.encodeByte(0);
        // Reserved deletions: always 0. Readers 1C0BA00/1C0C9B0 (int n, n x (int id, FILETIME)) write the same
        // reservation map as LP001C, but their link to this LP0013 field is unproven (login-cp-layouts-2823.md 1.9).
        out.encodeInt(0);
        out.encodeInt(characters.size()); // Character order.
        for(Char character:characters)out.encodeInt(character.getId());
        out.encodeByte(characters.size());
        for(Char character:characters)Tms282AvatarEncoder.encode(out,character.getAvatarData(),charset);
        if(!characters.isEmpty()) {
            byte[] auxiliary=new byte[characters.size()];
            java.util.Arrays.fill(auxiliary,(byte)auxiliaryFlag);
            out.setAuxiliaryData(auxiliary);
        }
        displayed.encode(out); // u8 limitFlag, i32 displayCount, i32 n, n x i32 (CLogin+768/+750).
        out.encodeByte(secondaryPasswordState(user)); // CLogin+230 (spw-state-2823.md 4)
        out.encodeByte(0); // ctx+436C: odd together with a non-zero ctx+4370 would skip the SPW dialog
        out.encodeByte(0); // CLogin+234: non-zero makes character creation ask for the SPW (CP00C3)
        out.encodeInt(user.getCharacterSlots());
        out.encodeInt(0);
        out.encodeInt(-1);
        out.encodeByte(0); // No timed world-slot information.
        out.encodeByte(0);
        out.encodeByte(0);
        for (int i = 0; i < 7; i++) {
            out.encodeInt(0); // Reader at RVA 0x8D9290.
        }
        out.encodeInt(0);
        out.encodeInt(0);
        out.encodeByte(0);
        out.encodeByte(0);
        out.encodeInt(user.getVipGrade());
        Tms282QuickSelection quick = favorite != null ? favorite : previous;
        out.encodeInt(quick == null ? account.getWorldId() : quick.worldId());
        out.encodeInt(quick == null ? wireChannelId : quick.channelId());
        out.encodeInt(quick == null ? 0 : quick.characterId());
        out.encodeByte(favorite != null);
        out.encodeInt(previous == null ? account.getWorldId() : previous.worldId());
        out.encodeInt(previous == null ? wireChannelId : previous.channelId());
        out.encodeInt(previous == null ? 0 : previous.characterId());
        str(out, "", charset);
        out.encodeInt(0);
        out.encodeByte(0);
        str(out, "", charset);
        out.encodeFT(user.getCreationDate());
        return out;
    }

    /** LP0019 normal result: status, request context, avatar MAIN, then return-to-selection flag. */
    public static OutPacket createCharacterSuccess(Char character, int requestContext, Charset charset, int auxiliaryFlag) {
        Objects.requireNonNull(character,"character");
        checkByte(auxiliaryFlag, "Auxiliary avatar flag");
        OutPacket out=Tms282LoginPacketSupport.packet(0x0019);
        out.encodeByte(0);out.encodeInt(requestContext);
        Tms282AvatarEncoder.encode(out,character.getAvatarData(),charset);
        out.encodeByte(1); // Return to selection; do not auto-enter the character via CPC6.
        out.setAuxiliaryData(new byte[]{(byte)auxiliaryFlag});
        return out;
    }

    /** No available owned quick-login character (including a deleted or stale previous id). */
    public static OutPacket noQuickLoginCharacter() {
        OutPacket out = Tms282LoginPacketSupport.packet(0x0B7F);
        out.encodeByte(false);
        return out;
    }

    /** LP0B7F native3C9A920: name, level, job, combat power, AvatarEntry(934170). */
    public static OutPacket quickLoginCharacter(Char character, Charset charset, int auxiliaryFlag) {
        Objects.requireNonNull(character, "character");
        checkByte(auxiliaryFlag, "Auxiliary avatar flag");
        OutPacket out = Tms282LoginPacketSupport.packet(0x0B7F);
        out.encodeByte(true);
        str(out, character.getName(), charset);
        out.encodeInt(character.getLevel());
        out.encodeShort(character.getJob());
        out.encodeLong(character.getCombatPower());
        Tms282AvatarEncoder.encode(out, character.getAvatarData(), charset);
        out.setAuxiliaryData(new byte[]{(byte) auxiliaryFlag});
        return out;
    }

    /** LP0017 reader 3CA7F10: original length-prefixed name bytes, then one result byte (0=available). */
    public static OutPacket nameCheckResult(Tms282CharacterNameRequest request, int result) {
        Objects.requireNonNull(request, "request");
        checkByte(result, "Character-name result");
        byte[] name = request.nameBytes();
        OutPacket out = Tms282LoginPacketSupport.packet(0x0017);
        // Byte count, not Java String.length(); echoing also avoids guessing UTF-8 versus CP950.
        out.encodeShort(name.length);
        out.encodeArr(name);
        out.encodeByte(result);
        return out;
    }

    /**
     * LP0014 failure (reader 3CAA330). Results other than 0/0x0C go to 3C96070, which shows the
     * native error dialog; 0x0C, 0x53 and 0x27.. special paths are excluded here.
     */
    public static OutPacket selectCharacterFailure(int result) {
        if (result <= 0 || result > 0xFF || result == 0x0C || result == 0x27 || result == 0x53) {
            throw new IllegalArgumentException("Use a plain native error result");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(0x0014);
        out.encodeByte(result);
        str(out, "", DEFAULT_CHARSET);
        out.encodeByte(0);
        return out;
    }

    /**
     * LP0014 success, reader 3CAA330. The client connects to channelIpv4:channelPort
     * (5DC7A00 -> CClientSocket 346ACD0). characterId must be in the loaded list, otherwise the
     * client drops the packet silently. worldType must equal this world's LP0013 value (same global).
     * The 4-byte token is stored by the client (global 9EC7730); its later use is not yet known.
     */
    public static OutPacket selectCharacterSuccess(byte[] channelIpv4, int channelPort, int characterId,
                                                   int worldType, int token, int blobKey) {
        Objects.requireNonNull(channelIpv4, "channelIpv4");
        if (channelIpv4.length != 4 || channelPort <= 0 || channelPort > 0xFFFF) {
            throw new IllegalArgumentException("Invalid channel endpoint");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(0x0014);
        out.encodeByte(0);
        str(out, "", DEFAULT_CHARSET);
        out.encodeByte(0);
        out.encodeArr(channelIpv4);   // sin_addr, network order
        out.encodeShort(channelPort); // little-endian, client applies htons
        out.encodeInt(characterId);
        out.encodeInt(worldType);     // 1217860, same field as LP0013
        out.encodeInt(1);             // 12189B0, same value LP0013 sends
        out.encodeInt(0);             // ctx+56D0
        out.encodeByte(0);            // flags ctx+240C
        out.encodeInt(0);             // ctx+2414
        out.encodeByte(0);            // ctx+2418
        out.encodeByte(0);            // ctx+56D4
        out.encodeInt(0);             // ctx+56D8
        byte[] plain = java.nio.ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(token).array();
        out.encodeInt(blobKey);
        out.encodeInt(plain.length);
        out.encodeArr(encryptSelectBlob(plain, blobKey));
        return out;
    }

    /** Inverse of the LP0014 block decrypt at 3CAA9AB..3CAAB54. */
    public static byte[] encryptSelectBlob(byte[] plain, int key) {
        byte[] data = plain.clone();
        var buf = java.nio.ByteBuffer.wrap(data).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        int i = 0;
        for (; i + 4 <= data.length; i += 4) {
            int w = buf.getInt(i);
            w += i * key;
            w ^= 0xAAAABBBB;
            w -= key >>> 7;
            w += 0xC960EBB3;
            w ^= key;
            buf.putInt(i, w);
        }
        for (; i < data.length; i++) {
            int b = data[i] & 0xFF;
            b = (b + i * key) & 0xFF;
            b ^= 0xAB;
            b = (b - ((key >>> 7) & 0xFF)) & 0xFF;
            b = (b + 0xC9) & 0xFF;
            b ^= (key >>> 1) & 0xFF;
            data[i] = (byte) b;
        }
        return data;
    }

    /** The client's decrypt, kept for offline round-trip checks of encryptSelectBlob. */
    public static byte[] decryptSelectBlob(byte[] cipher, int key) {
        byte[] data = cipher.clone();
        var buf = java.nio.ByteBuffer.wrap(data).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        int i = 0;
        for (; i + 4 <= data.length; i += 4) {
            int w = buf.getInt(i);
            w ^= key;
            w -= 0xC960EBB3;
            w += key >>> 7;
            w ^= 0xAAAABBBB;
            w -= i * key;
            buf.putInt(i, w);
        }
        for (; i < data.length; i++) {
            int b = data[i] & 0xFF;
            b ^= (key >>> 1) & 0xFF;
            b = (b - 0xC9) & 0xFF;
            b = (b + ((key >>> 7) & 0xFF)) & 0xFF;
            b ^= 0xAB;
            b = (b - ((i * key) & 0xFF)) & 0xFF;
            data[i] = (byte) b;
        }
        return data;
    }

    /** LP001C success: the client keeps the second FILETIME (deletable-at, 217A400). */
    public static OutPacket reserveDeleteSuccess(int characterId, FileTime requestedAt, FileTime deletableAt) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x001C);
        out.encodeInt(characterId);
        out.encodeByte(0);
        out.encodeFT(requestedAt);
        out.encodeFT(deletableAt);
        return out;
    }

    /** LP001C failure: 0x14 shows the generic failure notice (3CA9574 table). */
    public static OutPacket reserveDeleteFailure(int characterId) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x001C);
        out.encodeInt(characterId);
        out.encodeByte(0x14);
        return out;
    }

    /** LP001D: any result clears the client's reservation for this character. */
    public static OutPacket cancelDeleteReservation(int characterId) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x001D);
        out.encodeInt(characterId);
        out.encodeByte(0);
        return out;
    }

    /**
     * LP001A (3CA8840): int characterId, u8 result. 0 = deleted (client removes it from the list).
     * 0x12/0x45/0x47/0x8B read extra fields and are rejected; 1..5 and other unlisted codes take the
     * success path (the character still disappears), so only 0 or a listed notice code is accepted.
     */
    public static OutPacket deleteCharacterResult(int characterId, int result) {
        if (result != 0 && !DELETE_RESULTS_WITHOUT_EXTRA.contains(result)) {
            throw new IllegalArgumentException("Delete result must be 0 or a notice code without extra fields");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(0x001A);
        out.encodeInt(characterId);
        out.encodeByte(result);
        return out;
    }

    /**
     * LP001B (3CA9050), reader 8D9290: 7 ints into CLogin+608..+620 (same structure as the LP0013
     * block); the 7th is not stored when negative. Character-slot/button state, not a delete reply.
     */
    public static OutPacket characterSlotState(int[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length != 7) {
            throw new IllegalArgumentException("Character-slot state has exactly 7 ints");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(0x001B);
        for (int value : values) {
            out.encodeInt(value);
        }
        return out;
    }

    /** LP003D (inline 3C933E0): i32 a, i32 b; updates the slot-purchase dialog at CLogin+0x708 if open. */
    public static OutPacket slotPurchaseDialogUpdate(int a, int b) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x003D);
        out.encodeInt(a);
        out.encodeInt(b);
        return out;
    }

    /**
     * LP003E (inline 3C93414): i32 itemId (5430000), i32 a, i32 b, u8 c, u8 unused, u8 result.
     * result 0 = bought, 1 = slots maxed, 2 = purchase failed.
     */
    public static OutPacket characterSlotPurchaseResult(int a, int b, int c, int result) {
        checkByte(c, "Slot purchase byte");
        if (result < 0 || result > 2) {
            throw new IllegalArgumentException("Slot purchase result must be 0, 1 or 2");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(0x003E);
        out.encodeInt(CHARACTER_SLOT_ITEM_ID);
        out.encodeInt(a);
        out.encodeInt(b);
        out.encodeByte(c);
        out.encodeByte(0); // Read and discarded at 3C93443.
        out.encodeByte(result);
        return out;
    }

    /**
     * LP0046 (3CB8F20), reply to CP00E8 (rename application): u8 ok; 0 = notice 6. ok != 0: i32 characterId,
     * str, str, shows "名稱變更申請成功" and adds characterId to the CLogin+0x728 list; the client does not
     * read the names back into the list (unknown-busy-cp-2823.md). The id and names are not sent (and may
     * be null) when ok is false.
     */
    public static OutPacket characterRenameResult(boolean ok, int characterId, String first, String second,
                                                  Charset charset) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x0046);
        out.encodeByte(ok);
        if (ok) {
            out.encodeInt(characterId);
            str(out, first, charset);
            str(out, second, charset);
        }
        return out;
    }

    /** LP0048 (inline), low confidence: u8 -> CLogin+0x428 (character-select state; 0/2 make the timer send CP00D0). */
    public static OutPacket characterSelectState(int value) {
        checkByte(value, "Character-select state");
        OutPacket out = Tms282LoginPacketSupport.packet(0x0048);
        out.encodeByte(value);
        return out;
    }

    /** LP004A (inline), low confidence: no payload; unlocks, CLogin+0x428 = 3, then 3C9D980 (VM). */
    public static OutPacket characterSelectStateThree() {
        return Tms282LoginPacketSupport.packet(0x004A);
    }

    /**
     * LP0062 (3C9A760): read only on character select; i32 must equal CLogin+0x62C (same field as LP0019),
     * then AvatarEntry. Shows the world-transfer completed message. AUX handling as createCharacterSuccess.
     */
    public static OutPacket worldTransferCharacterAdded(Char character, int selectedWorldContext, Charset charset,
                                                        int auxiliaryFlag) {
        Objects.requireNonNull(character, "character");
        checkByte(auxiliaryFlag, "Auxiliary avatar flag");
        OutPacket out = Tms282LoginPacketSupport.packet(0x0062);
        out.encodeInt(selectedWorldContext);
        Tms282AvatarEncoder.encode(out, character.getAvatarData(), charset);
        out.setAuxiliaryData(new byte[]{(byte) auxiliaryFlag});
        return out;
    }

    /** LP01E9 r=0: only clears the CharSelect UI's CP00C5 pending flag (+47A8); no message, no update. */
    public static OutPacket burningResultNone() {
        return burningResult(0);
    }

    /** LP01E9 r=2: clears pending and shows login notice 0x0C (3CEE920(0x0C, 0)). */
    public static OutPacket burningResultFailure() {
        return burningResult(2);
    }

    /**
     * LP01E9 r=1 (burning-display-chars-2823.md 1.3, case 5DF8BE2 -> 3CCD220): i32 characterId, then the
     * 8D93D0 burning block; the client updates that character's block (2179EA0) and refreshes the burning
     * buttons. Its link to CP00C5 is [推定]; it does not change the LP001B/LP0013 assigned counts.
     */
    public static OutPacket burningResultSuccess(int characterId, Tms282BurningBlock block, Charset charset) {
        Objects.requireNonNull(block, "block");
        OutPacket out = burningResult(1);
        out.encodeInt(characterId);
        block.encode(out, charset);
        return out;
    }

    private static OutPacket burningResult(int result) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x01E9);
        out.encodeByte(result);
        return out;
    }

    /** LP006D (inline 3C9367D): i32 result; 0 = success (client sends CP0086), else "not again until tomorrow". */
    public static OutPacket initializeResult(int result) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x006D);
        out.encodeInt(result);
        return out;
    }
}
