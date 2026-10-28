package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.*;
import net.swordie.ms.connection.protocol.*;
import net.swordie.ms.connection.packet.Tms282CubeRequest;
import net.swordie.ms.connection.packet.Tms282CubeChoiceRequest;
import net.swordie.ms.connection.packet.Tms282CashCubeChoiceRequest;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.handlers.item.CubeHandler;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.enhance.CubeRateTable;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Payment/storage service only; native request/result codecs must validate their own exact variants.
 * Cubes with rows in data/resources/tms282/enhance (MetaMS 273 Grade/OptionLevel/option weights, TMS 279
 * cube-rate.toml overrides; see CubeRateTable) use those; every other cube keeps this fork's own probabilities,
 * which are not a claim of official TMS rates.
 * Pity/select-line/6-line cubes intentionally have no family here.
 */
public final class Tms282CubeRuntime {
    private static final boolean ENABLED=Boolean.parseBoolean(System.getProperty("tms282.cube.enabled",
            ServerRuntimeConfig.stringValue("tms282.cube.enabled")));
    private static final Tms282CubeJournal JOURNAL=new Tms282CubeJournal();
    public record Family(boolean bonus, boolean choice, ItemGrade minimum, ItemGrade maximum,
                         boolean karma, int probabilityPolicy) { }
    public record Result(Equip equip, Equip rolled, int cubeId, int cubeSlot, int remaining,
                         int index, boolean bonus, boolean choice) { }
    private Tms282CubeRuntime() { }
    public static boolean enabled() {return ENABLED && Protocol.active().version()==ProtocolVersion.TMS_282_3;}
    /** Stable proposals reserve only their target, not the whole character's economy or save path. */
    public static boolean isReserved(Char chr,Item item) {
        if(!enabled() || chr==null || item==null || item.getDateExpire()!=null && item.getDateExpire().isExpired())return false;
        var pending=chr.getMemorialCubeInfo();var target=pending==null?null:pending.getOriginalEquip();
        return target!=null && (target==item || target.getId()>0 && target.getId()==item.getId());
    }
    public static void requireMovable(Char chr,Item item) {
        if(isReserved(chr,item))throw new IllegalArgumentException("請先選擇此裝備的方塊結果，再移動或交易裝備。");
    }
    public static boolean refuseMove(Char chr,Item item) {
        if(enabled() && chr!=null && item!=null && item.getDateExpire()!=null && item.getDateExpire().isExpired()) {
            chr.chatMessage("此道具已到期，無法移動或交易，請等待系統清理。");chr.dispose();return true;
        }
        if(!isReserved(chr,item))return false;
        chr.chatMessage("請先選擇此裝備的方塊結果，再移動或交易裝備。");chr.dispose();return true;
    }
    public static void beforeLoad(Char chr) throws SQLException {
        if(enabled()) synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) {JOURNAL.recoverForLoad(chr);}
    }
    public static Family family(int id) {
        // IDs and restrictions checked against original282 CubeManager and String.wz descriptions.
        // Ring-only, downgrade, mirror, pity, six-line, eternal-binding and one-line cubes are separate mechanics.
        return switch(id) {
            case 2711000,2711001,2711009,2711011,2711027 -> rule(false,false,ItemGrade.Epic,false,ItemConstants.MYSTICAL_CUBE);
            case 2711003,2711005,2711012 -> rule(false,false,ItemGrade.Unique,false,ItemConstants.HARD_CUBE);
            case 2711023,2711026 -> rule(false,false,ItemGrade.Unique,true,ItemConstants.HARD_CUBE);
            case 2711004,2711006,2711013 -> rule(false,false,ItemGrade.Legendary,false,ItemConstants.SOLID_CUBE);
            case 2711017,2711021,2711028 -> rule(false,false,ItemGrade.Legendary,true,ItemConstants.SOLID_CUBE);
            case 2711024,2711025,5062032 -> rule(false,true,ItemGrade.Legendary,true,ItemConstants.BRIGHT_CUBE);
            case 5062030 -> rule(false,true,ItemGrade.Legendary,false,ItemConstants.BRIGHT_CUBE);
            case 2730000,2730002,2730004,2730013 -> rule(true,false,ItemGrade.Epic,false,ItemConstants.BONUS_MYSTICAL_CUBE);
            case 2730006,2730020 -> rule(true,false,ItemGrade.Epic,true,ItemConstants.BONUS_MYSTICAL_CUBE);
            case 2730011,2730012,5062521 -> rule(true,true,ItemGrade.Legendary,true,ItemConstants.BONUS_BRIGHT_CUBE_1);
            case 5062507,5062508 -> new Family(true,false,ItemGrade.Epic,ItemGrade.Legendary,id==5062508,ItemConstants.BONUS_POTENTIAL_CUBE);
            case 5062031 -> new Family(true,true,ItemGrade.Epic,ItemGrade.Legendary,false,ItemConstants.BONUS_BRIGHT_CUBE_1);
            default -> null;
        };
    }
    private static Family rule(boolean bonus,boolean choice,ItemGrade max,boolean karma,int policy) {
        return new Family(bonus,choice,ItemGrade.Rare,max,karma,policy);
    }
    public static Result roll(Char chr,Tms282CubeRequest request) throws SQLException {
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) {synchronized(chr) {
            Item cube=chr.getInventoryByType(request.kind().inventory()).getItemBySlot(request.cubeSlot());
            Family family=cube==null?null:family(cube.getItemId());
            if(family==null || family.choice() || family.bonus()!=request.kind().bonus() || !request.kind().acceptsItem(cube.getItemId()))
                throw new IllegalArgumentException("方塊種類與原生請求不符，未扣除道具。");
            if(request.extra()!=0)throw new IllegalArgumentException("免鑑定費道具的規則尚未支援，未扣除方塊或楓幣。");
            var inv=chr.getInventoryByType(request.equipPosition()<0?InvType.EQUIPPED:InvType.EQUIP);
            Item target=inv.getItemBySlot(Math.abs(request.equipPosition()));
            return roll(chr,cube,target instanceof Equip e?e:null,0);
        }}
    }
    public static Result roll(Char chr,Tms282CubeChoiceRequest request) throws SQLException {
        if(request.selection() || request.mode()!=0)throw new IllegalArgumentException("不支援的方塊使用模式，未扣除道具。");
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) {synchronized(chr) {
            Item cube=chr.getConsumeInventory().getItemBySlot(request.cubeSlot());
            if(cube==null || !request.kind().supportedItem(cube.getItemId()))
                throw new IllegalArgumentException("方塊種類與原生比較介面不符，未扣除道具。");
            var inventory=chr.getInventoryByType(request.equipPosition()<0?InvType.EQUIPPED:InvType.EQUIP);
            Item target=inventory.getItemBySlot(Math.abs(request.equipPosition()));
            // This native choice request does not carry the optional appraisal-coupon slot.
            // Without a verified coupon rule/second-item journal, do not charge through a potentially free UI quote.
            if(chr.getConsumeInventory().getItems().stream().anyMatch(i->i.getItemId()/10000==246 && i.getQuantity()>0
                    && i.getDateExpire()!=null && !i.getDateExpire().isExpired()))
                throw new IllegalArgumentException("持有鑑定道具時的比較方塊費用規則尚未支援，未扣除道具或楓幣。");
            return roll(chr,cube,target instanceof Equip e?e:null,0);
        }}
    }
    public static Result choose(Char chr,Tms282CubeChoiceRequest request) throws SQLException {
        if(!enabled())throw new IllegalArgumentException("方塊服務尚未開放。");
        if(!request.selection() || request.selectedIndex()!=-1 && request.selectedIndex()!=0)
            throw new IllegalArgumentException("無效的方塊候選選擇。");
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) {synchronized(chr) {
            Tms282VMatrixRuntime.requireEconomicAccess(chr);
            var intent=JOURNAL.choice(chr.getId());
            if(intent==null || !request.kind().supportedItem(Integer.parseInt(intent.cubeBefore().get("itemid")))
                    || intent.bonus()!=request.kind().bonus())throw new IllegalArgumentException("沒有相符的方塊候選。");
            return choose(chr,intent.equipId(),request.selectedIndex()==0);
        }}
    }
    public static Result roll(Char chr,Tms282CashCubeChoiceRequest request) throws SQLException {
        if(request.selection() || request.extra()!=0 || request.mode()!=0 || request.flag1()!=0 || request.flag2()!=0)
            throw new IllegalArgumentException("此現金比較方塊的選購或鑑定模式尚未支援，未扣除道具或楓幣。");
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) {synchronized(chr) {
            Item cube=chr.getCashInventory().getItemBySlot(request.cubeSlot());
            if(cube==null || !request.kind().supportedItem(cube.getItemId()))
                throw new IllegalArgumentException("方塊種類與原生現金比較介面不符，未扣除道具。");
            var inventory=chr.getInventoryByType(request.equipPosition()<0?InvType.EQUIPPED:InvType.EQUIP);
            Item target=inventory.getItemBySlot(Math.abs(request.equipPosition()));
            return roll(chr,cube,target instanceof Equip e?e:null,0);
        }}
    }
    public static Result choose(Char chr,Tms282CashCubeChoiceRequest request) throws SQLException {
        if(!enabled())throw new IllegalArgumentException("方塊服務尚未開放。");
        if(!request.selection() || request.selectedIndex()!=-1 && request.selectedIndex()!=0)
            throw new IllegalArgumentException("無效的現金方塊候選選擇。");
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) {synchronized(chr) {
            Tms282VMatrixRuntime.requireEconomicAccess(chr);
            var intent=JOURNAL.choice(chr.getId());
            if(intent==null || !request.kind().supportedItem(Integer.parseInt(intent.cubeBefore().get("itemid")))
                    || intent.bonus()!=request.kind().bonus()
                    || Integer.parseInt(intent.cubeBefore().get("invtype"))!=InvType.CASH.getVal())
                throw new IllegalArgumentException("沒有相符的現金方塊候選。");
            return choose(chr,intent.equipId(),request.selectedIndex()==0);
        }}
    }
    public static Result roll(Char chr, Item cube, Equip equip, int index) throws SQLException {
        if(!enabled())throw new IllegalArgumentException("方塊服務尚未開放。");
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) {synchronized(chr) {
            Tms282VMatrixRuntime.requireEconomicAccess(chr);
            if(chr.getTradeRoom()!=null || chr.getMiniRoom()!=null)throw new IllegalArgumentException("請先結束交易或個人商店。");
            if(cube==null || equip==null || cube.getQuantity()<1 || cube.getBagIndex()<=0
                    || (cube.getInvType()!=InvType.CASH && cube.getInvType()!=InvType.CONSUME)
                    || !owns(chr,cube) || !owns(chr,equip) || cube.getDateExpire()==null || cube.getDateExpire().isExpired()
                    || equip.getDateExpire()==null || equip.getDateExpire().isExpired() || index<0 || index>255
                    || ItemData.getItemInfoByID(cube.getItemId())==null || ItemData.getEquipById(equip.getItemId())==null)
                throw new IllegalArgumentException("找不到有效的方塊或裝備。");
            Family family=family(cube.getItemId());
            if(family==null)throw new IllegalArgumentException("此方塊的特殊規則尚未支援，未扣除道具。");
            var activeChoice=JOURNAL.choice(chr.getId());
            if(activeChoice!=null) {
                Equip activeTarget=findEquip(chr,activeChoice.equipId());
                if(activeTarget==null || activeTarget.getDateExpire()==null || activeTarget.getDateExpire().isExpired()) {
                    pending(chr);activeChoice=JOURNAL.choice(chr.getId());
                }
            }
            if(chr.getMemorialCubeInfo()!=null || activeChoice!=null)
                throw new IllegalArgumentException("請先選擇上一次方塊的結果。");
            int grade=family.bonus()?equip.getBonusGrade():equip.getBaseGrade();
            if(grade<family.minimum().getVal() || grade>family.maximum().getVal() || equip.isCash()
                    || equip.isFixedPotential() || !ItemConstants.canEquipHavePotential(equip) || ItemConstants.isLongOrBigSword(equip.getItemId()))
                throw new IllegalArgumentException("此裝備或潛能等級不支援這種方塊。");
            if(equip.hasAttribute(EquipAttribute.Locked) || family.karma()
                    && !equip.isTradeBlock() && !equip.hasAttribute(EquipAttribute.UnTradable))
                throw new IllegalArgumentException("裝備已上鎖，或不符合卡勒馬方塊的不可交易限制。");
            long cost=mesoCost(equip.getrLevel()+equip.getiIncReq(),cube.getItemId(),
                    chr.getTraitLevelByExp(chr.getStat(Stat.insightEXP)));
            if(cost<0 || chr.getMoney()<cost)throw new IllegalArgumentException("楓幣不足。");
            final int gradeNow=grade;
            int tableRate=CubeRateTable.tierUp(cube.getItemId(),CubeRateTable.gradeIndex(ItemGrade.getGradeByVal(gradeNow)));
            // Table rate is per 10000 for this exact cube; otherwise the family policy's fork rate (per 1000). The
            // context keeps the policy id from picking up another cube's table row.
            boolean up=tableRate>=0
                    ? ThreadLocalRandom.current().nextInt(10_000)<net.swordie.ms.GameSettings.scaleTierUpChance(tableRate*ItemConstants.GLOBAL_TIER_UP_MULTIPLIER,10_000)
                    : ThreadLocalRandom.current().nextInt(1000)<CubeRateTable.withCube(cube.getItemId(),
                            ()->ItemConstants.getTierUpChance(family.probabilityPolicy(),ItemGrade.getGradeByVal(gradeNow)));
            if(grade<family.maximum().getVal() && up)grade++;
            // Do not invent the old handler's unrelated 0.1% second promotion for newly enabled TMS paths.
            final short rolledGrade=(short)grade;
            Equip rolled=CubeRateTable.withCube(cube.getItemId(),
                    ()->CubeHandler.rollPotential(equip,rolledGrade,family.bonus(),family.probabilityPolicy()));
            Equip before=equip.deepCopy();
            chr.saveToSQL(); // The journal independently verifies every persisted before image.
            var intent=JOURNAL.execute(chr,cube,equip,rolled,cost,family.choice(),family.bonus(),index);
            if(family.choice())restoreChoice(chr,intent);
            else refreshSkills(chr,before,equip);
            return new Result(equip,rolled,cube.getItemId(),cube.getBagIndex(),remaining(chr,cube.getItemId()),index,family.bonus(),family.choice());
        }}
    }
    public static Result pending(Char chr) throws SQLException {
        if(!enabled())return null;
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) {synchronized(chr) {
            var intent=JOURNAL.choice(chr.getId());
            if(intent==null)return null;
            Equip target=findEquip(chr,intent.equipId());
            if(target==null || target.getDateExpire()==null || target.getDateExpire().isExpired()) {
                boolean cancelled=JOURNAL.cancelUnavailableTarget(chr);
                chr.setMemorialCubeInfo(null);
                System.out.printf("[TMS282] Cube choice character=%d target=%d %s; no item/meso write or refund%n",
                        chr.getId(),intent.equipId(),cancelled?"cancelled: target unavailable":"quarantined: owned target not loaded");
                return null;
            }
            return restoreChoice(chr,intent);
        }}
    }
    private static Result restoreChoice(Char chr,Tms282CubeJournal.Intent intent) {
        Equip equip=findEquip(chr,intent.equipId());
        if(equip==null)throw new IllegalStateException("Pending cube target missing; do not discard choice");
        Equip rolled=equip.deepCopy();rolled.setOptions(Tms282CubeJournal.options(intent.optionsAfter()));
        int cubeId=Integer.parseInt(intent.cubeBefore().get("itemid"));
        var memorial=new MemorialCubeInfo(rolled,Tms282CubeJournal.options(intent.optionsBefore()),cubeId);
        memorial.setOriginalEquip(equip);chr.setMemorialCubeInfo(memorial);
        return new Result(equip,rolled,cubeId,Integer.parseInt(intent.cubeBefore().get("bagindex")),remaining(chr,cubeId),intent.index(),intent.bonus(),true);
    }
    public static Result choose(Char chr,long equipSn,boolean keepNew) throws SQLException {
        if(!enabled())throw new IllegalArgumentException("方塊服務尚未開放。");
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) {synchronized(chr) {
            Tms282VMatrixRuntime.requireEconomicAccess(chr);
            var old=JOURNAL.choice(chr.getId());
            if(old==null || old.equipId()!=equipSn)throw new IllegalArgumentException("沒有可選擇的方塊結果。");
            Equip equip=findEquip(chr,equipSn);
            if(equip==null || equip.getDateExpire()==null || equip.getDateExpire().isExpired()) {
                pending(chr);throw new IllegalArgumentException("此裝備已到期或無法使用，未套用候選。");
            }
            if(!owns(chr,equip) || keepNew && equip.getItemId()!=Integer.parseInt(old.equipBefore().get("itemid")))
                throw new IllegalArgumentException("裝備資料已變更，無法套用此候選。");
            if(chr.getTradeRoom()!=null || chr.getMiniRoom()!=null)throw new IllegalArgumentException("裝備目前無法變更。");
            Equip before=equip.deepCopy();
            chr.saveToSQL();JOURNAL.choose(chr,equip,keepNew);chr.setMemorialCubeInfo(null);
            if(keepNew)refreshSkills(chr,before,equip);
            int cubeId=Integer.parseInt(old.cubeBefore().get("itemid"));
            return new Result(equip,equip,cubeId,Integer.parseInt(old.cubeBefore().get("bagindex")),remaining(chr,cubeId),old.index(),old.bonus(),false);
        }}
    }
    private static int remaining(Char chr,int id) {
        return java.util.stream.Stream.concat(chr.getConsumeInventory().getItems().stream(),chr.getCashInventory().getItems().stream())
                .filter(i->i.getItemId()==id && i.getQuantity()>0 && i.getDateExpire()!=null && !i.getDateExpire().isExpired())
                .mapToInt(Item::getQuantity).sum();
    }
    private static void refreshSkills(Char chr,Equip before,Equip after) {
        try {chr.refreshEquipmentSkillsAfterChange(before,after);}
        catch(RuntimeException e){throw new IllegalStateException("Cube payment committed; equipment skill refresh requires reload",e);}
    }
    /** A94F10/5E4BCF6 baseline with no native benefit discount (this profile broadcasts none).
     * Non-bonus CASH skips appraisal entirely (609AF80 ->369C436). Trait/coupon exemptions are separate.
     */
    public static long baseMesoCost(int equipLevel,int cubeId) {
        Family family=family(cubeId);
        if(family==null || equipLevel<0 || equipLevel>1000)throw new IllegalArgumentException("Unsupported cube cost inputs");
        if(equipLevel<=30 || cubeId/1_000_000==5 && !family.bonus())return 0;
        long square=(long)equipLevel*equipLevel;
        long raw=equipLevel<=70?square/2:equipLevel<=120?square*5/2:square*20;
        return raw/100*100;
    }
    /** BasicStat+98 is Insight (5EEB862..87D sets stat bit21); A9B060 picks the free appraisal range. */
    public static long mesoCost(int equipLevel,int cubeId,int insightLevel) {
        if(insightLevel<0 || insightLevel>100)throw new IllegalArgumentException("Invalid Insight level");
        long baseline=baseMesoCost(equipLevel,cubeId);
        int freeThrough=insightLevel>=90?120:insightLevel>=60?70:insightLevel>=30?30:0;
        return equipLevel<=freeThrough?0:baseline;
    }
    private static boolean owns(Char chr,Item item) {
        return chr.getInventoryByType(item.getInvType())!=null
                && chr.getInventoryByType(item.getInvType()).getItemBySlot(item.getBagIndex())==item;
    }
    private static Equip findEquip(Char chr,long id) {
        return java.util.stream.Stream.concat(chr.getEquipInventory().getItems().stream(),chr.getEquippedInventory().getItems().stream())
                .filter(i->i.getId()==id && i instanceof Equip).map(i->(Equip)i).findFirst().orElse(null);
    }
}
