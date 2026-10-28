package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.constants.QuestConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.Etc.VCore.VCore;
import net.swordie.ms.enums.MatrixStateType;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;

/** Character integration; new authority is separate from the retained legacy core inventory. */
public final class Tms282VMatrixRuntime {
    // ponytail: one economy gate for this local server avoids cross-character trade lock inversion.
    // Replace only if measured contention warrants a shared inventory transaction dispatcher.
    public static final Object ECONOMY_MONITOR = new Object();
    // Enable only after the reviewed CREATE TABLE deployment. Unrelated profile tests never touch SQL.
    private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("tms282.vmatrix.enabled",
            ServerRuntimeConfig.stringValue("tms282.vmatrix.enabled")));
    private static final class Holder {
        static final Tms282VMatrixData DATA = loadData();
        static final Tms282VMatrixService SERVICE = new Tms282VMatrixService(DATA);
        static final Tms282VMatrixStore STORE = new Tms282VMatrixStore();
        static final Tms282VMatrixJournal JOURNAL = new Tms282VMatrixJournal();
        static final Tms282VMatrixProcStore PROCS = new Tms282VMatrixProcStore();
    }
    public static final class Context {
        private Tms282VMatrixState state;
        private final Tms282VMatrixSpecialEngine special;
        private ScheduledFuture<?> expiry;
        private boolean death;
        private Context(Tms282VMatrixState state) {
            this.state=state;
            special=new Tms282VMatrixSpecialEngine(Holder.DATA,()->ThreadLocalRandom.current().nextDouble());
        }
    }
    private Tms282VMatrixRuntime() { }
    private static Tms282VMatrixData loadData() {
        try { return new Tms282VMatrixData(Path.of("data/protocol/tms282.3/vmatrix-data.json")); }
        catch(java.io.IOException e) { throw new IllegalStateException("新版五轉資料無法載入",e); }
    }
    public static boolean enabled() { return ENABLED && Protocol.active().version()==ProtocolVersion.TMS_282_3; }
    public static boolean economyEnabled() {
        return enabled() || Tms282SkillRingRuntime.enabled() || Tms282CubeRuntime.enabled()
                || Tms282MyHomeRuntime.enabled()
                || net.swordie.ms.client.social.Guild.Tms282GuildRuntime.enabled()
                || Protocol.active().version() == ProtocolVersion.TMS_282_3
                && net.swordie.ms.world.tms282.items.SymbolCouponExchange.hasUncertainInstances();
    }
    public static boolean economicBlocked(int id) {
        return Tms282VMatrixJournal.isBlocked(id) || Tms282SkillRingJournal.isBlocked(id)
                || Tms282CubeJournal.isBlocked(id)
                || Tms282MyHomeJournal.isBlocked(id)
                || net.swordie.ms.client.social.Guild.Tms282GuildJournal.isBlocked(id);
    }
    public static boolean managed(Char chr) { return enabled() && chr!=null && chr.getTms282VMatrix()!=null; }
    public static void requireEconomicAccess(Char chr) {
        net.swordie.ms.world.tms282.items.SymbolCouponExchange.requireCertain(chr);
        if(Tms282MyHomeJournal.isBlocked(chr.getId()))
            throw new IllegalStateException("小屋家具資料等待復原，暫時無法變更道具或保存角色。");
        if(Tms282CubeRuntime.enabled() && Tms282CubeJournal.isBlocked(chr.getId()))
            throw new IllegalStateException("方塊資料等待復原，暫時無法變更道具或楓幣。");
        if(net.swordie.ms.client.social.Guild.Tms282GuildRuntime.enabled()
                && net.swordie.ms.client.social.Guild.Tms282GuildJournal.isBlocked(chr.getId()))
            throw new IllegalStateException("公會資料等待復原，暫時無法變更道具或楓幣。");
        if(Tms282SkillRingRuntime.enabled() && Tms282SkillRingJournal.isBlocked(chr.getId()))
            throw new IllegalStateException("戒指研磨資料等待復原，暫時無法變更道具或楓幣。");
        if(enabled() && Tms282VMatrixJournal.isBlocked(chr.getId()))
            throw new IllegalStateException("五轉資料等待復原，暫時無法變更道具或楓幣。");
    }
    public static void alive(Char chr) {
        if(managed(chr)) synchronized(chr) {chr.getTms282VMatrix().death=false;}
    }
    public static boolean eligible(Char chr) {
        return chr!=null && chr.getLevel()>=200 && chr.hasQuestCompleted(QuestConstants.FIFTH_JOB_QUEST)
                && Holder.DATA.groups().containsKey((int)chr.getJob());
    }
    /** Before legacy inventory/money loading; never recover against already-published stale memory. */
    public static void beforeLoad(Char chr) throws SQLException {
        if (!enabled()) return;
        synchronized(chr) {
            Holder.JOURNAL.recoverForLoad(chr);
            Context prior=chr.getTms282VMatrix();
            if(prior!=null && prior.expiry!=null)prior.expiry.cancel(false);
            chr.setTms282VMatrix(null);
        }
    }
    private static Context context(Char chr) throws SQLException {
        Context context=chr.getTms282VMatrix();
        if(context!=null)return context;
        if(!Holder.DATA.groups().containsKey((int)chr.getJob()))throw new IllegalArgumentException("目前職業沒有對應的新版V矩陣資料。");
        if(!eligible(chr))throw new IllegalArgumentException("請先達到200級並完成五轉。");
        var existing=Holder.STORE.load(chr.getId());
        Tms282VMatrixState state;
        if(existing.isPresent())state=existing.get();
        else {
            long points=0; String qr=chr.getQRValueByKey(1477,"vPoint");
            if(qr!=null && !qr.isBlank()) {
                try {points=Long.parseLong(qr);} catch(NumberFormatException e){throw new IllegalArgumentException("原有V點數資料不正確，未轉換。");}
                if(points<0 || points>Tms282VMatrixState.MAX_POINTS)throw new IllegalArgumentException("原有V點數超出上限，未轉換。");
            }
            Set<Integer> ids=new TreeSet<>(),boosts=new HashSet<>();
            for(var core:chr.getMatrixCore())if(core.getState()!=MatrixStateType.DISASSEMBLED) {
                ids.add(core.getCoreID());
                var definition=VCore.getCore(core.getCoreID());
                if(core.isActive() && definition!=null && definition.getType()==VCore.BOOST) {
                    for(int skill:new int[]{core.getSkillID1(),core.getSkillID2(),core.getSkillID3()})if(skill>0)boosts.add(skill);
                }
            }
            Map<Integer,Integer> levels=new HashMap<>();
            chr.getSkills().forEach(s->levels.put(s.getSkillId(),s.getCurrentLevel()));
            var seed=Tms282VMatrixMigration.fromEffectiveSkills(Holder.DATA,chr.getJob(),points,ids,levels,boosts);
            state=Holder.STORE.initialize(chr.getId(),seed.state());
            if(!seed.pendingCores().isEmpty())System.out.printf("[TMS282] VMatrix retained legacy cores character=%d count=%d%n",chr.getId(),seed.pendingCores().size());
        }
        Holder.SERVICE.validate(state);
        context=new Context(state);
        Holder.PROCS.load(chr.getId()).forEach(context.special::restoreCooldown);
        chr.setTms282VMatrix(context);return context;
    }
    public static void onFieldEntry(Char chr) {
        if(!enabled())return;
        synchronized(chr) {
            if(!eligible(chr)) {
                Context old=chr.getTms282VMatrix();
                if(old!=null) {
                    Map<Integer,Integer> revoked=new HashMap<>();
                    Holder.SERVICE.effectiveSkills(old.state,System.currentTimeMillis()).keySet().forEach(id->revoked.put(id,0));
                    Holder.SERVICE.specialBuffSkills().forEach(id->{revoked.put(id,0);chr.getTemporaryStatManager().removeStatsBySkill(id);});
                    applySkills(chr,revoked);
                    if(old.expiry!=null)old.expiry.cancel(false);
                    chr.setTms282VMatrix(null);
                }
                return;
            }
            try {Context c=context(chr); publish(chr,c,c.state,Set.of(),true);}
            catch(SQLException|RuntimeException e){failed(chr,e);}
        }
    }
    public static boolean isGemstone(int id) { return enabled() && Holder.DATA.gemstones().containsKey(id); }
    public static boolean consumeGemstone(Char chr,int id,int quantity) {
        return handle(chr,new Tms282VMatrixRequest(1,id,quantity));
    }
    public static boolean handle(Char chr,Tms282VMatrixRequest request) {
        if(!enabled()) {chr.chatMessage("新版V矩陣尚未啟用。");return false;}
        synchronized(ECONOMY_MONITOR) { synchronized(chr) {
            try {
                Context c=context(chr);
                var plan=Holder.SERVICE.plan(c.state,request,player(chr),System.currentTimeMillis());
                commit(chr,c,plan);return true;
            }catch(SQLException|RuntimeException e){failed(chr,e);return false;}
            finally{chr.write(Tms282StatPackets.unlock());}
        } }
    }
    public static int maxForJob(Char chr) {
        synchronized(ECONOMY_MONITOR) { synchronized(chr) {
            try {
                Context c=context(chr); if(isMaxed(chr))return 0;
                var plan=Holder.SERVICE.maxForJob(c.state,player(chr),System.currentTimeMillis());
                int changed=(int)plan.state().cores().values().stream().filter(v->!v.equals(c.state.cores().get(v.id()))).count();
                commit(chr,c,plan);return changed;
            }catch(SQLException e){throw new IllegalStateException("五轉資料保存失敗，未確認成功。",e);}
        } }
    }
    public static boolean isMaxed(Char chr) {
        synchronized(chr) {
            try {
                var state=context(chr).state;boolean any=false;
                for(var core:Holder.DATA.cores().values())if(core.type()<2 && Holder.DATA.allowed(chr.getJob(),core.id())) {
                    any=true;var saved=state.cores().get(core.id());
                    if(saved==null || saved.level()!=core.maxLevel())return false;
                }
                return any;
            }catch(SQLException e){throw new IllegalStateException("五轉資料讀取失敗。",e);}
        }
    }
    private static Tms282VMatrixService.Player player(Char chr) {
        Map<Integer,Long> items=new HashMap<>();
        for(Item item:chr.getConsumeInventory().getItems())if(item.getQuantity()>0
                && (item.getDateExpire()==null || !item.getDateExpire().isExpired()))items.merge(item.getItemId(),(long)item.getQuantity(),Long::sum);
        Set<Integer> quests=new HashSet<>(),cooldowns=new HashSet<>(),buffs=new HashSet<>();
        for(var gem:Holder.DATA.gemstones().values())if(gem.requiredQuest()>0 && chr.hasQuestCompleted(gem.requiredQuest()))quests.add(gem.requiredQuest());
        for(var core:Holder.DATA.cores().values()) {
            for(int skill:core.cooldownSkills())if(chr.hasSkillOnCooldown(skill))cooldowns.add(skill);
            for(int skill:core.equipRemovalSkills())if(chr.getTemporaryStatManager().hasStatBySkillId(skill))buffs.add(skill);
        }
        return new Tms282VMatrixService.Player(chr.getJob(),eligible(chr),chr.getHP()>0,chr.getMoney(),items,quests,cooldowns,buffs);
    }
    private record Bag(Item item,int quantity,int slot) { }
    private static void commit(Char chr,Context context,Tms282VMatrixService.Plan plan)throws SQLException {
        // Skill definitions must exist before any payment/state commit.
        for(int id:plan.effectiveSkills().keySet())if(SkillData.getSkillInfoById(id)==null)throw new IllegalArgumentException("核心技能資料不完整，未套用變更。");
        if(Tms282VMatrixJournal.isBlocked(chr.getId()) || Holder.JOURNAL.hasPending(chr.getId()))throw new SQLException("Pending matrix recovery");
        if(plan.items().isEmpty() && plan.mesoDelta()==0)Holder.STORE.commit(chr.getId(),context.state.revision(),plan.state(),c->{});
        else {
            chr.saveToSQL(); // Under the same character lock; journal verifies this persisted baseline exactly.
            Map<Long,Bag> before=new HashMap<>();
            chr.getConsumeInventory().getItems().forEach(i->before.put(i.getId(),new Bag(i,i.getQuantity(),i.getBagIndex())));
            Holder.JOURNAL.execute(chr,context.state,plan);
            Map<Long,Item> after=new HashMap<>();chr.getConsumeInventory().getItems().forEach(i->after.put(i.getId(),i));
            List<Tms282InventoryPackets.Operation> operations=new ArrayList<>();
            for(var entry:before.entrySet()) {
                var current=after.get(entry.getKey());Bag old=entry.getValue();
                if(current==null)operations.add(new Tms282InventoryPackets.Remove(InvType.CONSUME,(short)old.slot()));
                else if(current.getQuantity()!=old.quantity())operations.add(new Tms282InventoryPackets.UpdateQuantity(InvType.CONSUME,(short)current.getBagIndex(),current.getQuantity()));
            }
            for(var entry:after.entrySet())if(!before.containsKey(entry.getKey()))operations.add(new Tms282InventoryPackets.Add(InvType.CONSUME,(short)entry.getValue().getBagIndex(),entry.getValue()));
            if(!operations.isEmpty())chr.write(Tms282InventoryPackets.inventoryOperation(true,operations,
                    net.swordie.ms.handlers.login.tms282.Tms282LoginSettings.load().nameCharset()));
            if(plan.mesoDelta()!=0)chr.sendStatsPacket(Map.of(net.swordie.ms.enums.Stat.money,chr.getMoney()));
        }
        publish(chr,context,plan.state(),plan.removeBuffs(),true);
    }
    private static void publish(Char chr,Context context,Tms282VMatrixState state,Set<Integer> removeBuffs,boolean sync) {
        context.state=state;
        removeBuffs.forEach(id->chr.getTemporaryStatManager().removeStatsBySkill(id));
        applySkills(chr,effectiveForJob(state,chr.getJob(),System.currentTimeMillis()));
        int selectedSource=Holder.SERVICE.selectedSpecial(state,System.currentTimeMillis())
                .map(core->Holder.DATA.core(core.id()).specialOption().path("effect").path("skill_id").asInt()).orElse(0);
        Holder.SERVICE.specialBuffSkills().stream().filter(id->id!=selectedSource)
                .forEach(id->chr.getTemporaryStatManager().removeStatsBySkill(id));
        context.special.reset(Holder.SERVICE.selectedSpecial(state,System.currentTimeMillis()).orElse(null),System.currentTimeMillis());
        if(context.expiry!=null)context.expiry.cancel(false);
        long now=System.currentTimeMillis();
        long next=state.cores().values().stream().filter(c->c.selected() && c.expiresAt()>Tms282VMatrixState.fileTime(now))
                .mapToLong(c->c.expiresAt()/10000-11644473600000L).min().orElse(0);
        if(next>0 && chr.getTimer()!=null)context.expiry=chr.getTimer().addEvent(()->{
            synchronized(chr){if(chr.getTms282VMatrix()!=context)return;
                if(Holder.SERVICE.selectedSpecial(context.state,System.currentTimeMillis()).isPresent())return;
                Holder.SERVICE.specialBuffSkills().forEach(id->chr.getTemporaryStatManager().removeStatsBySkill(id));
                publish(chr,context,context.state,Set.of(),true);}
        },Math.max(1,next-now+1));
        if(sync){chr.setQRValueByKey(1477,"vPoint",Long.toString(state.points()));chr.write(Tms282VMatrixPackets.update(state,false));}
    }
    private static Map<Integer,Integer> effectiveForJob(Tms282VMatrixState state,int job,long now) {
        Map<Integer,Integer> all=new HashMap<>();
        Holder.SERVICE.effectiveSkills(state,now).keySet().forEach(id->all.put(id,0));
        Map<Integer,Tms282VMatrixState.Core> allowed=new HashMap<>();
        state.cores().forEach((id,core)->{if(Holder.DATA.allowed(job,id))allowed.put(id,core);});
        all.putAll(Holder.SERVICE.effectiveSkills(new Tms282VMatrixState(state.points(),state.revision(),state.migrationPending(),allowed,state.retainedSet()),now));
        return all;
    }
    /** Derived in-memory records; the separate matrix authority is committed first, legacy saves may follow later. */
    public static void applySkills(Char chr,Map<Integer,Integer> levels) {
        List<Skill> changed=new ArrayList<>();
        for(var entry:levels.entrySet()) {
            Skill skill=chr.getSkills().stream().filter(s->s.getSkillId()==entry.getKey()).findFirst().orElse(null);
            int level=entry.getValue();
            if(skill!=null && skill.getCurrentLevel()==level && skill.getMasterLevel()>=level)continue;
            if(skill==null){if(level==0)continue;skill=SkillData.getSkillDeepCopyById(entry.getKey());if(skill==null)throw new IllegalArgumentException("Missing matrix skill");skill.setCharId(chr.getId());}
            else if(skill.getCurrentLevel()>0 && SkillConstants.isPassiveSkill(skill.getSkillId()))chr.removeFromBaseStatCache(skill);
            skill.setCurrentLevel(level);skill.setMasterLevel(Math.max(level,skill.getMasterLevel()));chr.putSkill(skill);
            if(level>0 && SkillConstants.isPassiveSkill(skill.getSkillId()))chr.addToBaseStatCache(skill);
            if(!SkillConstants.isSpecialInvisibleSkill(skill.getSkillId()))changed.add(skill);
        }
        if(!changed.isEmpty())chr.write(WvsContext.changeSkillRecordResult(changed,true,false,false));
    }
    public static void attack(Char chr,AttackInfo info) {
        if(!managed(chr) || !eligible(chr) || info==null)return;
        synchronized(chr) {
            var selected=Holder.SERVICE.selectedSpecial(chr.getTms282VMatrix().state,System.currentTimeMillis());
            if(selected.isEmpty())return;
            boolean perMob=Holder.DATA.core(selected.get().id()).specialOption().path("cond").path("type").asText().equals("attackCountMob");
            int total=0,first=0;
            for(var mob:info.mobAttackInfo)if(mob.mobId>0 && mob.damages!=null && chr.getField()!=null && chr.getField().getMobByObjectId(mob.mobId)!=null) {
                int lines=(int)Arrays.stream(mob.damages).filter(d->d>0).count();
                if(lines==0)continue;
                if(perMob)event(chr,Tms282VMatrixSpecialEngine.Type.ATTACK,lines,mob.mobId);
                else{total+=lines;if(first==0)first=mob.mobId;}
            }
            if(!perMob && total>0)event(chr,Tms282VMatrixSpecialEngine.Type.ATTACK,total,first);
        }
    }
    public static void event(Char chr,Tms282VMatrixSpecialEngine.Type type,int amount,int oid) {
        if(!managed(chr))return;
        synchronized(chr) {
            Context c=chr.getTms282VMatrix();long now=System.currentTimeMillis();
            if(!eligible(chr)) {
                c.special.reset(null,now);
                Holder.SERVICE.specialBuffSkills().forEach(id->chr.getTemporaryStatManager().removeStatsBySkill(id));
                return;
            }
            if(chr.getHP()>0)c.death=false;
            var selected=Holder.SERVICE.selectedSpecial(c.state,now);c.special.reset(selected.orElse(null),now);
            if(selected.isEmpty() || !Holder.DATA.allowed(chr.getJob(),selected.get().id()))return;
            c.special.onEvent(new Tms282VMatrixSpecialEngine.Event(type,amount,oid),now).ifPresent(effect->{
                long cooldown=Holder.DATA.core(effect.coreId()).specialOption().path("cond").path("cooltime").asLong();
                try {
                    if(Holder.PROCS.reserve(chr.getId(),effect.coreId(),now,cooldown))Tms282VMatrixEffects.apply(chr,effect);
                    else Holder.PROCS.load(chr.getId()).forEach(c.special::restoreCooldown);
                } catch(SQLException e) {
                    System.out.printf("[TMS282] Special core cooldown could not be reserved character=%d%n",chr.getId());
                }
            });
        }
    }
    public static void deathOnce(Char chr) {
        if(!managed(chr))return;
        synchronized(chr){Context c=chr.getTms282VMatrix();if(c.death)return;c.death=true;event(chr,Tms282VMatrixSpecialEngine.Type.DEATH,1,0);}
    }
    private static void failed(Char chr,Exception error) {
        System.out.printf("[TMS282] VMatrix character=%d failed: %s%n",chr.getId(),error.getClass().getSimpleName());
        if(Tms282VMatrixJournal.isBlocked(chr.getId())){chr.chatMessage("五轉操作等待資料復原，請重新登入。");chr.getClient().close();}
        else chr.chatMessage(error instanceof IllegalArgumentException ? error.getMessage() : "五轉資料暫時無法保存，未確認操作成功。");
    }
}
