package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.client.character.tms282.Tms282FamiliarFees;
import net.swordie.ms.client.character.tms282.Tms282FamiliarState;

import java.util.ArrayList;

/** Verified AFamiliarController envelopes. Runtime must own the network actor ID in the current world.
 * These packets initialize the window or report refusal; they do not register or summon a familiar.
 */
public final class Tms282FamiliarPackets {
    // AFamiliarController dereferences its CUser owner; a field-scoped0392 actor is unsafe.
    public static final int OPCODE=0x0425;
    public static final int CLASS_SIGNATURE=0x9947A4C2;
    private Tms282FamiliarPackets() { }
    private static void actor(long id){if(id<=0 || id>0xFFFFFFFFL)throw new IllegalArgumentException("Invalid familiar actor ID");}
    private static OutPacket packet(int characterId) {
        if(characterId<=0)throw new IllegalArgumentException("Invalid familiar owner character");
        OutPacket out=Tms282LoginPacketSupport.packet(OPCODE);out.encodeInt(characterId);return out;
    }
    public static OutPacket bootstrap(int characterId,long actorId,int userId,boolean canSummon) {
        actor(actorId);
        if(userId<=0)throw new IllegalArgumentException("Invalid familiar owner user");
        OutPacket out=packet(characterId);
        out.encodeByte(0);out.encodeInt((int)actorId);out.encodeInt(CLASS_SIGNATURE);
        out.encodeByte(3);out.encodeByte(0);out.encodeByte(0);
        out.encodeShort(16);out.encodeInt(userId);out.encodeInt(characterId); // NetOwnerID: user, not world Account.id
        out.encodeShort(17);out.encodeByte(3); // AActor.RemoteRole, one-byte ENetRole
        fees(out);
        out.encodeShort(60);out.encodeByte(canSummon);
        out.encodeShort(0);out.encodeShort(0);out.encodeByte(0);out.encodeInt(0);
        return out;
    }
    /**
     * Fee leaves in the order of the emulator-verified fixture familiar-20260927/lp0425-bootstrap-fees-candidate.bin
     * (fee_initialization_check.py): 42/43 appearance change/restore, 58 link activate max hours, 59 essence 0 (the
     * snapshot sends the real one), then the vectors 44/46 upgrade essence normal/special, 48/50 disassemble return
     * normal/special, 52 link unlock (2), 54 link apply (3), 56 link activate (3). Empty or short vectors make the
     * native link/upgrade paths read out of bounds (FEE-SAFETY.md); unavailable special grades are -1.
     */
    static void fees(OutPacket out) {
        out.encodeShort(42);out.encodeInt(Tms282FamiliarFees.APPEARANCE_CHANGE);
        out.encodeShort(43);out.encodeInt(Tms282FamiliarFees.APPEARANCE_RESTORE);
        out.encodeShort(58);out.encodeInt(Tms282FamiliarFees.LINK_ACTIVATE_MAX_HOURS);
        out.encodeShort(59);out.encodeInt(0);
        vector(out,44,Tms282FamiliarFees.UPGRADE_NORMAL);
        vector(out,46,Tms282FamiliarFees.UPGRADE_SPECIAL);
        vector(out,48,Tms282FamiliarFees.DISASSEMBLE_NORMAL);
        vector(out,50,Tms282FamiliarFees.DISASSEMBLE_SPECIAL);
        vector(out,52,Tms282FamiliarFees.LINK_UNLOCK);
        vector(out,54,Tms282FamiliarFees.LINK_APPLY);
        vector(out,56,Tms282FamiliarFees.LINK_ACTIVATE);
    }
    private static void vector(OutPacket out,int leaf,int[] values) {
        out.encodeShort(leaf);length(out,values.length);
        for(int i=0;i<values.length;i++){out.encodeShort(i+1);out.encodeInt(values[i]);}
        out.encodeShort(0);
    }

    /** Controller leaves 37..40 LinkInfo Slot1..3 + VipSlot: u64 registered serial, 0 = empty (LINK-RESEARCH.md). */
    public static OutPacket linkSlots(int characterId,long controllerActorId,long[] slots) {
        actor(controllerActorId);
        OutPacket out=packet(characterId);
        out.encodeByte(0);out.encodeInt((int)controllerActorId);out.encodeInt(CLASS_SIGNATURE);
        out.encodeByte(3);out.encodeByte(0);out.encodeByte(0);
        for(int i=0;i<4;i++){out.encodeShort(37+i);out.encodeLong(i<slots.length?slots[i]:0);}
        out.encodeShort(0);out.encodeShort(0);out.encodeByte(0);out.encodeInt(0);
        return out;
    }

    /** A controller callback (S->C: u8 9, u16 length, i32 actor, i32 class, u16 wire, args; no flag byte). */
    public static OutPacket callback(int characterId,long actorId,int wire,byte[] args) {
        actor(actorId);
        OutPacket out=packet(characterId);
        out.encodeByte(9);out.encodeShort(10+args.length);
        out.encodeInt((int)actorId);out.encodeInt(CLASS_SIGNATURE);out.encodeShort(wire);out.encodeArr(args);
        return out;
    }

    /**
     * An AFamiliar (field familiar) callback, PAWN-RPC-RESEARCH.md: 85 OnFlushMovePath (move path bytes), 87
     * OnAttackMob, 89 ChangeNameTag, 90 OnCreateMagicAtom. {@code ownerCharacterId} is the LP0425 prefix: observers get
     * the owner's id, which 5AA7B10 resolves to that remote user's UserCommon world.
     */
    public static OutPacket pawnCallback(int ownerCharacterId,long pawnActorId,int wire,byte[] args) {
        actor(pawnActorId);
        OutPacket out=packet(ownerCharacterId);
        out.encodeByte(9);out.encodeShort(10+args.length);
        out.encodeInt((int)pawnActorId);out.encodeInt(PAWN_CLASS_SIGNATURE);out.encodeShort(wire);out.encodeArr(args);
        return out;
    }
    /** Wire 87 OnAttackMob: i32 attack index, varint count of {i32 mob, varint count of i32 damage}, i32 extra. */
    public static OutPacket pawnAttack(int ownerCharacterId,long pawnActorId,int attackIndex,java.util.Map<Integer,long[]> hits,int extra) {
        return pawnCallback(ownerCharacterId,pawnActorId,87,body(o->{o.encodeInt(attackIndex);length(o,hits.size());
            for(var e:hits.entrySet()){o.encodeInt(e.getKey());length(o,e.getValue().length);
                for(long d:e.getValue())o.encodeInt((int)Math.min(Integer.MAX_VALUE,d));}
            o.encodeInt(extra);}));
    }

    /** RegisteredInfo as the callbacks read it (7735440): the 14 leaf21 fields, no selectors. */
    public static void record(OutPacket out,Tms282FamiliarState.RegisteredInfo r) {
        out.encodeLong(r.serial());out.encodeByte(r.state());out.encodeInt((int)r.familiarId());
        byte[] name=r.nameBytes();length(out,name.length);out.encodeArr(name);
        out.encodeShort(r.level());out.encodeShort(r.skillId());out.encodeInt(r.exp());out.encodeShort(r.optionLevel());
        out.encodeShort(r.option1());out.encodeShort(r.option2());out.encodeShort(r.option3());
        out.encodeByte(r.attributes());out.encodeByte(r.grade());out.encodeInt(r.lookId());
    }
    private static byte[] body(java.util.function.Consumer<OutPacket> writer) {
        OutPacket tmp=new OutPacket((short)0);writer.accept(tmp);
        byte[] d=tmp.getData();return java.util.Arrays.copyOfRange(d,2,d.length);
    }
    /** Wire 94 OnUseFamiliarCubeResult: RegisteredInfo; clears the cube pending flag. */
    public static OutPacket cubeResult(int characterId,long actorId,Tms282FamiliarState.RegisteredInfo r) {
        return callback(characterId,actorId,94,body(o->record(o,r)));
    }
    /** Wire 95 OnFamiliarOptionSelect: i64 serial, varint count of {u16,u16,u16} option sets, i32, u8 showUi. */
    public static OutPacket optionSelect(int characterId,long actorId,long serial,java.util.List<int[]> sets,int value,boolean showUi) {
        return callback(characterId,actorId,95,body(o->{o.encodeLong(serial);length(o,sets.size());
            for(int[] set:sets){o.encodeShort(set[0]);o.encodeShort(set[1]);o.encodeShort(set[2]);}
            o.encodeInt(value);o.encodeByte(showUi);}));
    }
    /** Wire 96 OnFamiliarOptionSelectResult: RegisteredInfo, i32. */
    public static OutPacket optionSelectResult(int characterId,long actorId,Tms282FamiliarState.RegisteredInfo r,int value) {
        return callback(characterId,actorId,96,body(o->{record(o,r);o.encodeInt(value);}));
    }
    /** Wire 97 OnFamiliarOptionStatDirty: i64 serial. */
    public static OutPacket optionStatDirty(int characterId,long actorId,long serial) {
        return callback(characterId,actorId,97,body(o->o.encodeLong(serial)));
    }
    /** Wire 93 OnUpgradeFamiliarResult: u8 result (1 = grade up), RegisteredInfo; its effect clears pending. */
    public static OutPacket upgradeResult(int characterId,long actorId,boolean gradeUp,Tms282FamiliarState.RegisteredInfo r) {
        return callback(characterId,actorId,93,body(o->{o.encodeByte(gradeUp);record(o,r);}));
    }
    /** Wire 124 OnLinkFamiliarError: i32 (ignored by the client; leaves link mode). */
    public static OutPacket linkError(int characterId,long actorId,int code) {
        return callback(characterId,actorId,124,body(o->o.encodeInt(code)));
    }
    /** Wire 125 OnFamiliarServerUpdated: i32 event (642CD00: 1 unlock, 3 link, 4 activate, 5 extend, 7 disassemble), i64 vector, u16 n + UTF-16. */
    public static OutPacket serverUpdated(int characterId,long actorId,int event,long[] values) {
        return callback(characterId,actorId,125,body(o->{o.encodeInt(event);length(o,values.length);
            for(long v:values)o.encodeLong(v);o.encodeShort(0);}));
    }
    /** Wire 132 OnValidateStat: the client recomputes the familiar stats (640F760). */
    public static OutPacket validateStat(int characterId,long actorId) {
        return callback(characterId,actorId,132,new byte[0]);
    }

    /** Only destroy actors allocated by this server feature in the same native world. */
    public static OutPacket destroy(int characterId,long actorId) {
        actor(actorId);
        OutPacket out=packet(characterId);
        out.encodeByte(0);out.encodeInt((int)actorId);out.encodeInt(CLASS_SIGNATURE);
        out.encodeByte(4);out.encodeByte(0);out.encodeInt(0);return out;
    }
    /** Full loaded state, not first-registration defaults. Requires the server's user-scoped actor.
     * Leaf21 resizes and patches every registered record;36 is SummonedSN;59 is POD int32 essence.
     */
    public static OutPacket snapshot(int characterId,long actorId,Tms282FamiliarState state) {
        actor(actorId);
        if(state==null || state.characterId()!=characterId)throw new IllegalArgumentException("Familiar snapshot owner mismatch");
        if(state.registered().size()>500)throw new IllegalArgumentException("Familiar snapshot exceeds500 records");
        var records=new ArrayList<>(state.registered().values());
        records.sort((left,right)->Long.compareUnsigned(left.serial(),right.serial()));
        OutPacket out=packet(characterId);
        out.encodeByte(0);out.encodeInt((int)actorId);out.encodeInt(CLASS_SIGNATURE);
        out.encodeByte(3);out.encodeByte(0);out.encodeByte(0);
        out.encodeShort(21);length(out,records.size());
        for(int index=0;index<records.size();index++) {
            var record=records.get(index);int field=index*14;
            out.encodeShort(++field);out.encodeLong(record.serial());
            out.encodeShort(++field);out.encodeByte(record.state());
            out.encodeShort(++field);out.encodeInt((int)record.familiarId());
            out.encodeShort(++field);byte[] name=record.nameBytes();length(out,name.length);out.encodeArr(name);
            out.encodeShort(++field);out.encodeShort(record.level());
            out.encodeShort(++field);out.encodeShort(record.skillId());
            out.encodeShort(++field);out.encodeInt(record.exp());
            out.encodeShort(++field);out.encodeShort(record.optionLevel());
            out.encodeShort(++field);out.encodeShort(record.option1());
            out.encodeShort(++field);out.encodeShort(record.option2());
            out.encodeShort(++field);out.encodeShort(record.option3());
            out.encodeShort(++field);out.encodeByte(record.attributes());
            out.encodeShort(++field);out.encodeByte(record.grade());
            out.encodeShort(++field);out.encodeInt(record.lookId());
        }
        out.encodeShort(0); // end array field patches
        out.encodeShort(36);out.encodeLong(state.summonedSerial());
        out.encodeShort(59);out.encodeInt(state.essence());
        out.encodeShort(0);out.encodeShort(0);out.encodeByte(0);out.encodeInt(0);
        return out;
    }
    /** AFamiliar (rva 8A3E8C8, size 0x750): the summoned familiar in the owner's UserCommon world. */
    public static final int PAWN_CLASS_SIGNATURE=0x9B1C9E5B;

    /**
     * Creates the summoned familiar (familiar-20260927/SUMMON-RESEARCH.md): owner leaves 16/17 as the controller,
     * leaves 20..33 m_Info = the 14 registered-record fields (leaf21 widths), leaf 34 m_ptPos (i32 x, i32 y). On
     * m_Info the client (7738BA0 -> 64230A0) moves it to the owner and loads the look (lookId, else familiarId); the
     * owner's client runs its movement and attack AI.
     */
    public static OutPacket pawnCreate(int characterId,long pawnActorId,int userId,Tms282FamiliarState.RegisteredInfo record,int x,int y) {
        actor(pawnActorId);
        if(userId<=0)throw new IllegalArgumentException("Invalid familiar owner user");
        OutPacket out=packet(characterId);
        out.encodeByte(0);out.encodeInt((int)pawnActorId);out.encodeInt(PAWN_CLASS_SIGNATURE);
        out.encodeByte(3);out.encodeByte(0);out.encodeByte(0);
        out.encodeShort(16);out.encodeInt(userId);out.encodeInt(characterId);
        out.encodeShort(17);out.encodeByte(3);
        int leaf=20;
        out.encodeShort(leaf++);out.encodeLong(record.serial());
        out.encodeShort(leaf++);out.encodeByte(record.state());
        out.encodeShort(leaf++);out.encodeInt((int)record.familiarId());
        out.encodeShort(leaf++);byte[] name=record.nameBytes();length(out,name.length);out.encodeArr(name);
        out.encodeShort(leaf++);out.encodeShort(record.level());
        out.encodeShort(leaf++);out.encodeShort(record.skillId());
        out.encodeShort(leaf++);out.encodeInt(record.exp());
        out.encodeShort(leaf++);out.encodeShort(record.optionLevel());
        out.encodeShort(leaf++);out.encodeShort(record.option1());
        out.encodeShort(leaf++);out.encodeShort(record.option2());
        out.encodeShort(leaf++);out.encodeShort(record.option3());
        out.encodeShort(leaf++);out.encodeByte(record.attributes());
        out.encodeShort(leaf++);out.encodeByte(record.grade());
        out.encodeShort(leaf++);out.encodeInt(record.lookId());
        out.encodeShort(34);out.encodeInt(x);out.encodeInt(y);
        out.encodeShort(0);out.encodeShort(0);out.encodeByte(0);out.encodeInt(0);
        return out;
    }

    /** Controller leaf20 m_pSummonedFamiliar (u64: high 0 = network actor id, 0 = none; 6FEAD30) and leaf36 SummonedSN. */
    public static OutPacket summoned(int characterId,long controllerActorId,long pawnActorId,long serial) {
        actor(controllerActorId);
        if(pawnActorId!=0)actor(pawnActorId);
        OutPacket out=packet(characterId);
        out.encodeByte(0);out.encodeInt((int)controllerActorId);out.encodeInt(CLASS_SIGNATURE);
        out.encodeByte(3);out.encodeByte(0);out.encodeByte(0);
        out.encodeShort(20);out.encodeLong(pawnActorId);
        out.encodeShort(36);out.encodeLong(serial);
        out.encodeShort(0);out.encodeShort(0);out.encodeByte(0);out.encodeInt(0);
        return out;
    }

    /** Destroys the summoned familiar (action 4, AFamiliar class). */
    public static OutPacket pawnDestroy(int characterId,long pawnActorId) {
        actor(pawnActorId);
        OutPacket out=packet(characterId);
        out.encodeByte(0);out.encodeInt((int)pawnActorId);out.encodeInt(PAWN_CLASS_SIGNATURE);
        out.encodeByte(4);out.encodeByte(0);out.encodeInt(0);return out;
    }

    private static void length(OutPacket out,int value) {
        int encoded=value<<1; // native90F080 nonnegative signed-varint encoding
        while(encoded>=128){out.encodeByte((encoded&127)|128);encoded>>>=7;}
        out.encodeByte(encoded);
    }
    /** Native OnRequestFailed clears UIFamiliar's pending flag. Verified reasons: Unknown/TryAgain/TryNextTime. */
    public static OutPacket failure(int characterId,long actorId,int reason) {
        actor(actorId);
        // 64265D0 table: 1 NotRegistered, 2 AlreadyMaxGrade, 3 OnlySameGrade, 5 NotBeSummoned, 9 Dead, 12 map not allowed,
        // 13 Unknown, 14 TryAgain, 16/18 locked, 20 essence, 21 linked, 22-25 upgrade amounts, 27 TryNextTime
        if(reason<1 || reason>27)throw new IllegalArgumentException("Unverified familiar failure reason");
        OutPacket out=packet(characterId);
        out.encodeByte(9);out.encodeShort(14); // Unlike C->S, no mode byte after kind9.
        out.encodeInt((int)actorId);out.encodeInt(CLASS_SIGNATURE);out.encodeShort(87);out.encodeInt(reason);
        return out;
    }
}
