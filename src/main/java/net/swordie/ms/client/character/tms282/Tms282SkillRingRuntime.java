package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.connection.protocol.*;
import net.swordie.ms.loaders.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

/** Native282 ring grinding. Enable only after pending-table migration and candidate validation. */
public final class Tms282SkillRingRuntime {
    private static final boolean ENABLED=Boolean.parseBoolean(System.getProperty("tms282.skillring.enabled",
            ServerRuntimeConfig.stringValue("tms282.skillring.enabled")));
    private static final class Holder {
        static final Tms282SkillRingRules RULES=load();
        static final Tms282SkillRingJournal JOURNAL=new Tms282SkillRingJournal();
        private static Tms282SkillRingRules load() {
            try { return Tms282SkillRingRules.load(Path.of("data/protocol/tms282.3/skillring.json")); }
            catch(java.io.IOException e) { throw new IllegalStateException("戒指研磨資料無法載入。",e); }
        }
    }
    private Tms282SkillRingRuntime() { }
    public static boolean enabled() { return ENABLED && Protocol.active().version()==ProtocolVersion.TMS_282_3; }
    public static void beforeLoad(Char chr) throws SQLException {
        if(enabled()) synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) { Holder.JOURNAL.recoverForLoad(chr); }
    }
    public static void onFieldEntry(Char chr) {
        if(enabled()) chr.write(Tms282SkillRingPackets.grindstoneTable(Holder.RULES.rows()));
    }
    public static boolean upgrade(Char chr,Tms282SkillRingRequest request) throws SQLException {
        if(!enabled()) throw new IllegalArgumentException("戒指研磨尚未開放。");
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized(chr) {
            Tms282VMatrixRuntime.requireEconomicAccess(chr);
            if(chr.getTradeRoom()!=null || chr.getMiniRoom()!=null)
                throw new IllegalArgumentException("請先結束交易或個人商店。");
            var item=chr.getEquipInventory().getItemBySlot(request.slot());
            if(!(item instanceof Equip ring) || ring.isCash() || ring.getDateExpire()==null || ring.getDateExpire().isExpired())
                throw new IllegalArgumentException("找不到可研磨的戒指。");
            var template=ItemData.getEquipById(ring.getItemId());
            int cap=0;
            if(template!=null && !template.getItemSkills().isEmpty()) {
                cap=Integer.MAX_VALUE;
                for(var skill:template.getItemSkills()) {
                    var info=SkillData.getSkillInfoById(skill.getSkill());
                    cap=Math.min(cap,info==null?0:info.getMaxLevel());
                }
            }
            long count=chr.getConsumeInventory().getItems().stream()
                    .filter(i->request.rule().acceptsStone(i.getItemId()) && i.getQuantity()>0 && i.getDateExpire()!=null && !i.getDateExpire().isExpired())
                    .mapToLong(i->i.getQuantity()).sum();
            var rule=Holder.RULES.validate(request,ring.getItemId(),ring.getItemLevel(),chr.getMoney(),count,cap);
            // Persist current balances first. Legacy saves swallow errors, so journal.prepare separately
            // requires the exact saved identities/balances before it can charge anything.
            chr.saveToSQL();
            boolean success=ThreadLocalRandom.current().nextInt(100)<rule.prop();
            return Holder.JOURNAL.execute(chr,ring,rule,success);
        } }
    }
}
