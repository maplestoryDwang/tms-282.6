package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.tms282.Tms282StatDiff;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.achievement.AchievementHandler;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.tms282.Tms282InventoryDiff;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282CombatPackets;
import net.swordie.ms.connection.packet.Tms282DropPickUpRequest;
import net.swordie.ms.connection.packet.Tms282InventoryPackets;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.connection.packet.Tms282UserHitRequest;
import net.swordie.ms.connection.protocol.codec.AttackRequest;
import net.swordie.ms.connection.protocol.codec.AttackRequestDecoder;
import net.swordie.ms.connection.protocol.codec.Tms282AttackDecoder;
import net.swordie.ms.connection.protocol.codec.Tms282AttackKind;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.user.AttackHandler;
import net.swordie.ms.handlers.user.UserHandler;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.drop.Drop;
import net.swordie.ms.world.field.Field;

import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * TMS 282.3 combat on the channel socket after LP02EF: the 282 requests are decoded here and handed to the 265
 * game logic (damage, mob death, drops and EXP stay 265 code; their outbound packets reach the client through the
 * 282 codecs of the profile: MobPacketEncoder, DropPacketEncoder, ExpGainStrategy).
 * <ul>
 *     <li>CP010D..CP0111 attacks: {@link Tms282AttackDecoder} with {@link Tms282CombatSettings#attackLayout}
 *     ({@link AttackRequestDecoder}) -> {@link Tms282CombatGuard} -> 265
 *     {@code AttackHandler.handleAttack(Char, AttackInfo)}. An unreadable attack is logged and skipped.</li>
 *     <li>CP0115 USER_HIT: {@link Tms282UserHitRequest} -> its 265 body -> 265 {@code UserHandler.handleUserHit};
 *     then LP009D hp/mp to the character and LP047F REMOTE_HIT to the rest of the field.</li>
 *     <li>DROP_PICK_UP_REQUEST, only when {@code tms282.dropPickUpOp} is set (the opcode is not captured):
 *     {@link Tms282DropPickUpRequest} (265 layout) -> 265 {@code Char.addDrop}; then LP008F (items, from a
 *     {@link Tms282InventoryDiff}) or LP009D money, the LP00AE pick-up message, and LP074E to the field through the
 *     265 {@code Field.removeDrop}. A refused pick-up gets LP009D unlock.</li>
 * </ul>
 * Every failure is logged and the packet dropped; nothing here ends the session (the route adapter would).
 */
final class Tms282CombatHandlers {
    private final Tms282CombatSettings settings;
    private final Tms282CombatGuard guard;
    private final Tms282AttackDecoder ownDecoder;
    private final Charset charset;
    private final Tms282FieldServices services;

    Tms282CombatHandlers(Tms282LoginContext context) {
        this(context, Tms282CombatSettings.load());
    }

    Tms282CombatHandlers(Tms282LoginContext context, Tms282CombatSettings settings) {
        Objects.requireNonNull(context, "context");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.guard = new Tms282CombatGuard(settings);
        this.ownDecoder = new Tms282AttackDecoder(settings.attackLayout());
        this.charset = context.settings().nameCharset();
        this.services = context.fieldServices();
    }

    void register(Tms282Routes routes) {
        for (Tms282AttackKind kind : Tms282AttackKind.values()) {
            routes.channel(kind.op(), kind.request()).when(Tms282Guards.IN_FIELD)
                    .handle(kind + " attack -> 265 damage/death/drops/EXP", (s, op, p) -> onAttack(s, kind, p));
        }
        routes.channel(Tms282UserHitRequest.OPCODE, InHeader.USER_HIT).when(Tms282Guards.IN_FIELD)
                .handle("hit -> 265 handleUserHit, LP009D, LP047F to the field", this::onUserHit);
        settings.dropPickUpOp().ifPresent(op -> routes.channel(op, InHeader.DROP_PICK_UP_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("drop pick-up (configured opcode) -> LP008F/LP009D, LP00AE, LP074E", this::onPickUp));
    }

    // ---- CP010D..CP0111 --------------------------------------------------------------------------------------

    /**
     * The decoder of {@code tms282.attackMobLocator} (the profile registers the standard one for other callers;
     * the handler keeps its own so the setting applies).
     */
    private AttackRequestDecoder decoder() {
        return ownDecoder;
    }

    private void onAttack(Tms282LoginState session, Tms282AttackKind kind, byte[] packet) {
        Char chr = session.fieldCharacter();
        String op = String.format("CP%04X", kind.op());
        Optional<AttackRequest> decoded;
        try {
            InPacket in = new InPacket(packet);
            in.decodeShort();
            decoded = decoder().decode(chr, in, kind.request());
        } catch (RuntimeException e) {
            System.out.println("[TMS282] REJECTED " + op + " len=" + packet.length + ": " + e);
            return;
        }
        if (decoded.isEmpty()) {
            return; // the decoder logged why
        }
        AttackRequest request = decoded.get();
        AttackInfo ai = request.info();
        if (net.swordie.ms.client.character.items.Tms282SpecialWeaponSkills.rejectUnimplementedAttack(request.requestedSkillId())
                || net.swordie.ms.client.character.items.Tms282SpecialWeaponSkills.rejectUnimplementedAttack(ai.skillId)
                || net.swordie.ms.client.character.items.Tms282SpecialWeaponEffects.requiresBurstGrant(ai.skillId)
                   && !net.swordie.ms.client.character.items.Tms282SpecialWeaponEffects.hasBurstGrant(chr, ai.skillId)) {
            System.out.println("[TMS282] REJECTED " + op + ": special weapon attack lacks a verified server authorization");
            session.send(Tms282StatPackets.unlock());
            return;
        }
        Tms282CombatGuard.Verdict verdict = guard.inspect(chr, ai);
        String summary = op + " " + kind + " character=" + chr.getId() + " skill=" + request.requestedSkillId()
                + " slv=" + ai.slv + " hits=" + ai.hits + " mobs=" + ai.mobAttackInfo.size() + " total=" + ai.totalDamageDealt;
        if (!verdict.clean()) {
            System.out.println("[TMS282] COMBAT CHECK " + summary + " " + verdict.findings()
                    + (verdict.apply() ? " (logged only)" : " (attack not applied: " + settings.antiCheat() + ")"));
        }
        if (!verdict.apply()) {
            return;
        }
        if (chr.getField() == null) {
            System.out.println("[TMS282] DROPPED " + summary + ": character has no field");
            return;
        }
        if (!net.swordie.ms.client.jobs.sengoku.Tms282KannaOniController.beforeAttack(chr, ai)) {
            System.out.println("[TMS282] REJECTED " + summary + ": inactive or unowned Kanna Oni");
            return;
        }
        int mpBefore = chr.getMP();
        try {
            Tms282SkillProcessLifecycleHandlers.beforeAttack(chr,ai);
            AttackHandler.handleAttack(chr, ai, session.buffs().book());
        } catch (RuntimeException | LinkageError e) {
            System.out.println("[TMS282] FAILED " + summary + ": " + e);
            return;
        } finally {
            if (net.swordie.ms.client.jobs.adventurer.thief.Tms282BladeTempest.handles(ai.skillId) && chr.getMP()!=mpBefore)
                session.hpMpSaveTimer(services).markDirty();
        }
        System.out.println("[TMS282] " + summary + " -> 265 attack queued; " + String.join("; ", request.notes()));
    }

    // ---- CP0115 ----------------------------------------------------------------------------------------------

    private void onUserHit(Tms282LoginState session, int op, byte[] packet) {
        Tms282UserHitRequest request;
        try {
            request = Tms282UserHitRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP0115 len=%d: %s%n", packet.length, e.getMessage());
            return;
        }
        Char chr = session.fieldCharacter();
        int hpBefore = chr.getHP();
        net.swordie.ms.client.jobs.adventurer.thief.Tms282BladeTempest.validate(chr,System.currentTimeMillis());
        if(!request.hasBlock282() && net.swordie.ms.client.jobs.flora.Tms282AetherGuard.active(chr,System.currentTimeMillis())) {
            chr.dispose(); return; // An incomplete native hit cannot trigger the perfect-guard reward.
        }
        Tms282StatDiff statDiff = Tms282StatDiff.of(chr);
        try {
            long now=System.currentTimeMillis();
            int damage=net.swordie.ms.client.jobs.flora.Tms282AetherGuard.damage(chr,request.hpDamage(),request.elemAttr()==1,now);
            if(damage==request.hpDamage()) damage=net.swordie.ms.client.character.tms282.Tms282UserHitDamage.damage(chr,request,now);
            UserHandler.handleUserHit(session.client(), new InPacket(request.toGms265Body(damage)));
        } catch (RuntimeException | LinkageError e) {
            System.out.println("[TMS282] FAILED CP0115 " + request + " character=" + chr.getId() + ": " + e);
            return;
        }
        int hp = chr.getHP();
        net.swordie.ms.world.boss.SerenFight.onUserHit(chr, hpBefore - hp); // bossmech3-0930: Seren sun gauge (no-op elsewhere)
        session.send(hitStats(chr, statDiff.changes()));
        int others = 0;
        Field field = chr.getField();
        if (field != null) {
            OutPacket remote = Tms282CombatPackets.remoteHit(chr.getId(), Math.max(0, hpBefore - hp), request.critical() != 0);
            field.broadcast(remote, chr);
            others = Math.max(0, field.getChars().size() - 1);
        }
        System.out.println("[TMS282] " + request + " character=" + chr.getId() + " hp " + hpBefore + "->" + hp
                + "; SENT LP009D" + (others > 0 ? " + LP047F to " + others + " other(s)" : ""));
    }

    /**
     * The LP009D of a hit: HP and MP as before; when the 265 hit also changed other stats - the EXP a death costs
     * (Char death penalty, 10% less the guild skill) - those go in the same packet, so the EXP bar follows at once.
     */
    static OutPacket hitStats(Char chr, Map<Tms282StatPackets.Stat, Long> changes) {
        boolean others = changes.keySet().stream()
                .anyMatch(k -> k != Tms282StatPackets.Stat.HP && k != Tms282StatPackets.Stat.MP);
        if (!others) {
            return Tms282StatPackets.hpMpChanged(false, chr.getHP(), chr.getMP());
        }
        Map<Tms282StatPackets.Stat, Long> stats = new EnumMap<>(Tms282StatPackets.Stat.class);
        stats.putAll(changes);
        stats.put(Tms282StatPackets.Stat.HP, (long) chr.getHP());
        stats.put(Tms282StatPackets.Stat.MP, (long) chr.getMP());
        return Tms282StatPackets.statChanged(false, stats);
    }

    // ---- DROP_PICK_UP_REQUEST (configured opcode) --------------------------------------------------------------

    private void onPickUp(Tms282LoginState session, int op, byte[] packet) {
        Tms282DropPickUpRequest request;
        try {
            request = Tms282DropPickUpRequest.parse(packet, op);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP%04X len=%d: %s; SENT LP009D unlock%n", op, packet.length, e.getMessage());
            session.send(Tms282StatPackets.unlock());
            return;
        }
        Char chr = session.fieldCharacter();
        Field field = chr.getField();
        Life life = field == null ? null : field.getLifeByObjectID(request.dropId());
        if (!(life instanceof Drop drop)) {
            session.send(Tms282StatPackets.unlock());
            System.out.println("[TMS282] " + request + " character=" + chr.getId() + ": no such drop; SENT LP009D unlock");
            return;
        }
        PickUp result;
        try {
            result = pickUp(chr, drop);
        } catch (RuntimeException | LinkageError e) {
            session.send(Tms282StatPackets.unlock());
            System.out.println("[TMS282] FAILED " + request + " character=" + chr.getId() + ": " + e + "; SENT LP009D unlock");
            return;
        }
        if (!result.picked()) {
            session.send(Tms282StatPackets.unlock());
            System.out.println("[TMS282] " + request + " character=" + chr.getId() + " refused (owner/inventory); SENT LP009D unlock");
            return;
        }
        for (OutPacket p : result.packets()) {
            session.send(p);
        }
        field.removeDrop(drop.getObjectId(), chr.getId(), false, -1); // LP074E to the field (DropPacketEncoder)
        try {
            chr.handleDropForPQs(drop);
            AchievementHandler.handleLoot(chr, drop);
        } catch (RuntimeException e) {
            System.out.println("[TMS282] pick-up extras failed: " + e);
        }
        System.out.println("[TMS282] " + request + " character=" + chr.getId() + " " + result.log() + " + LP074E");
    }

    /** The replies of an accepted pick-up, in send order. */
    record PickUp(boolean picked, List<OutPacket> packets, String log) {
    }

    /** 265 {@code Char.addDrop} and the 282 replies it would have needed. */
    PickUp pickUp(Char chr, Drop drop) {
        if (drop.getOwnerID() != chr.getId() && !drop.canBePickedUpBy(chr)) {
            return new PickUp(false, List.of(), "not the owner");
        }
        if (drop.isMoney()) {
            long before = chr.getMoney();
            if (!chr.addDrop(drop, false)) {
                return new PickUp(false, List.of(), "265 refused");
            }
            Map<Tms282StatPackets.Stat, Long> money = new EnumMap<>(Tms282StatPackets.Stat.class);
            money.put(Tms282StatPackets.Stat.MONEY, chr.getMoney());
            return new PickUp(true, List.of(Tms282StatPackets.statChanged(true, money),
                    Tms282CombatPackets.mesoPickUpMessage(drop.getMoney(), (int) Math.max(0, chr.getMoney() - before - drop.getMoney()))),
                    "meso " + drop.getMoney() + " -> " + chr.getMoney() + "; SENT LP009D money + LP00AE");
        }
        InvType type = drop.getItem().getInvType();
        Tms282InventoryDiff diff = Tms282InventoryDiff.of(type, chr.getInventoryByType(type));
        int itemId = drop.getItem().getItemId();
        int quantity = drop.getItem().getQuantity();
        if (!chr.addDrop(drop, false)) {
            return new PickUp(false, List.of(), "265 refused");
        }
        List<Tms282InventoryPackets.Operation> ops = diff.changes();
        return new PickUp(true, List.of(Tms282InventoryPackets.inventoryOperation(true, ops, charset),
                Tms282CombatPackets.itemPickUpMessage(itemId, quantity)),
                "item " + itemId + " x" + quantity + " " + ops + "; SENT LP008F + LP00AE");
    }
}
