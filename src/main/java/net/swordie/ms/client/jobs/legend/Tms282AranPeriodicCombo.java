package net.swordie.ms.client.jobs.legend;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.connection.packet.Tms282SkillStackTranslators;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.WeaponType;
import net.swordie.ms.handlers.login.tms282.Tms282LoginState;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.world.field.Field;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.AdrenalinBoost;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.AdrenalinMaximum;

/** 282 Combo Ability's tooltip-proven periodic gain. The native500ms request polls a server-owned5s deadline. */
public final class Tms282AranPeriodicCombo {
    private final Char owner;
    private final Aran job;
    private Client client;
    private User user;
    private int userId;
    private Field field;
    private long next;
    private long last;

    public Tms282AranPeriodicCombo(Char owner,Aran job) {
        this.owner=owner;
        this.job=job;
    }

    public static boolean activeProfile() {
        return Protocol.active().version()==ProtocolVersion.TMS_282_3;
    }

    private boolean currentOwner() {
        Client current=owner.getClient();
        var weapon=owner.getEquippedInventory().getItemBySlot(-11);
        return activeProfile() && owner.getJobHandler()==job && JobConstants.isAran(owner.getJob())
                && owner.getId()>0 && owner.getHP()>0 && current!=null && current.getChr()==owner
                && current.getCh()!=null && current.getCh().isActive() && current.getCurrentState()==Client.IN_FIELD
                && current.getNextState()==Client.NONE
                && current.getUser()!=null && current.getUser()==owner.getUser() && current.getUser().getId()>0
                && Tms282LoginState.of(current).isChannel() && Tms282LoginState.of(current).fieldCharacter()==owner
                && owner.getField()!=null
                && owner.getField().getCharByID(owner.getId())==owner && weapon!=null
                && ItemConstants.getWeaponType(weapon.getItemId())==WeaponType.Polearm;
    }

    /** Central CP0407/0408 dispatcher supplies its server clock; the packet's tick never grants points. */
    public synchronized boolean tick(int skillId,long now) {
        if (skillId!=Aran.COMBO_ABILITY) return false;
        if (!currentOwner() || now<0) { clear();return false; }
        int level=owner.getSkillLevel(Aran.COMBO_ABILITY);
        var info=SkillData.getSkillInfoById(Aran.COMBO_ABILITY);
        int combo=job.getCombo();
        var tsm=owner.getTemporaryStatManager();
        if (level<=0 || info==null) { clear();return false; }
        long interval=info.getValue(SkillStat.u,level)*1000L;
        int amount=info.getValue(SkillStat.v,level);
        if (interval!=5000 || amount!=30) { clear();return false; } // Exact installed282 WZ and tooltip contract.
        if (combo<1 || combo>999 || tsm.hasStat(AdrenalinBoost) || tsm.hasStat(AdrenalinMaximum)) {
            clear();rearm(false);return false;
        }
        if (client!=owner.getClient() || user!=owner.getUser() || userId!=owner.getUser().getId()
                || field!=owner.getField() || next==0 || now<last) {
            client=owner.getClient();user=owner.getUser();field=owner.getField();
            userId=user.getId();last=now;next=now+interval;rearm(true);return false;
        }
        last=now;
        if (now<next) { rearm(true);return false; }
        next=now+interval; // One pulse, no accumulated credit for a delayed client or missing field membership.
        job.setCombo(Math.min(1000,combo+amount)); // Existing mapped ComboAbilityBuff and native LP04EC codec.
        rearm(job.getCombo()<1000);
        return true;
    }

    private void rearm(boolean running) {
        owner.write(Tms282SkillStackTranslators.stackResult(Aran.COMBO_ABILITY,running?1:0));
    }

    /** Root Char.setField receipt / job cancellation starts a fresh deadline without changing the counter. */
    public synchronized void clear() {
        client=null;user=null;userId=0;field=null;next=0;last=0;
    }
}
