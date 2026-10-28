package net.swordie.ms.loaders;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.items.*;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.loaders.containerclasses.ItemRewardInfo;
import net.swordie.ms.loaders.containerclasses.PetInfo;
import net.swordie.ms.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static net.swordie.ms.client.character.items.Item.Type.ITEM;
import static net.swordie.ms.enums.ScrollStat.*;

public class ItemData {
    private static final boolean LOG_UNKS = false;

    public static Int2ObjectMap<Equip> equips = new Int2ObjectOpenHashMap<>();
    public static Int2ObjectMap<ItemInfo> items = new Int2ObjectOpenHashMap<>();
    public static Int2ObjectMap<PetInfo> pets = new Int2ObjectOpenHashMap<>();
    public static Int2ObjectMap<ItemOption> itemOptions = new Int2ObjectOpenHashMap<>();

    public static Int2IntMap petFoods = new Int2IntOpenHashMap();
    public static Int2IntMap petCommands = new Int2IntOpenHashMap();
    public static Int2IntMap skillIdByItemId = new Int2IntOpenHashMap();

    public static List<ItemOption> filteredItemOptions = new ArrayList<>();
    private static final Set<Integer> startingItems = new ObjectOpenHashSet<>();

    private static final List<Integer> validFaces = new ArrayList<>();
    private static final List<Integer> validHairs = new ArrayList<>();

    public ItemData() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class ItemDataJson {
        public List<ItemInfo> items = new ArrayList<>();
        public List<Equip> equips = new ArrayList<>();
    }

    public static Equip getEquipDeepCopyFromID(int itemId, boolean randomizeStats) {
        Equip equipInfo = getEquipById(itemId);
        Equip equip = equipInfo == null ? null : equipInfo.deepCopy();
        if (equip != null) {
            equip.setQuantity(1);
            equip.setCuttable((short) -1);
            equip.addHyperUpgrade((short) EquipState.AmazingHyperUpgradeChecked.getVal());
            equip.setType(Item.Type.EQUIP);
            if (equipInfo.isCash()) {
                equip.setInvType(InvType.DECORATION);
            } else {
                equip.setInvType(InvType.EQUIP);
                equip.random();
                if (equip.getTuc() > 0) {
                    equip.setTuc((short) (equip.getTuc() + 1)); // tại sao vậy nhỉ?
                }
                if (randomizeStats) {
                    if (ItemConstants.canEquipHavePotential(equip)) {
                        ItemGrade grade = ItemGrade.None;
                        if (Util.succeedProp(GameConstants.RANDOM_EQUIP_UNIQUE_CHANCE)) {
                            grade = ItemGrade.Unique;
                        } else if (Util.succeedProp(GameConstants.RANDOM_EQUIP_EPIC_CHANCE)) {
                            grade = ItemGrade.Epic;
                        } else if (Util.succeedProp(GameConstants.RANDOM_EQUIP_RARE_CHANCE)) {
                            grade = ItemGrade.Rare;
                        }
                        if (grade != ItemGrade.None) {
                            equip.setOptionBase(grade.getVal(), ItemConstants.THIRD_LINE_CHANCE);
                        }
                    }
                }
            }
            equip.setExpireOnLogout(equipInfo.isExpireOnLogout());
        }
        return equip;
    }

    /** 282 profile only: data/dat282 equips/items read before the (startup-filled) 265 caches. */
    private static final net.swordie.ms.loaders.overlay.OverlayRecordCache<Equip> OVERLAY_EQUIPS =
            new net.swordie.ms.loaders.overlay.OverlayRecordCache<>("equips");
    private static final net.swordie.ms.loaders.overlay.OverlayRecordCache<ItemInfo> OVERLAY_ITEMS =
            new net.swordie.ms.loaders.overlay.OverlayRecordCache<>("items");
    private static final net.swordie.ms.loaders.overlay.OverlayRecordCache<PetInfo> OVERLAY_PETS =
            new net.swordie.ms.loaders.overlay.OverlayRecordCache<>("pets");

    public static Equip getEquipById(int itemId) {
        Equip overlay = OVERLAY_EQUIPS.get(itemId, ItemData::readEquipFromFile);
        if (overlay != null) {
            return overlay;
        }
        Equip equip = equips.get(itemId);
        if (equip == null) {
            equip = getEquipFromFile(itemId);
        }
        return equip;
    }

    public static Equip getEquipFromFile(int itemId) {
        // 282 profile: data/dat282 overlay first; 265 profile: exactly DAT_DIR/equips as before.
        File file = net.swordie.ms.loaders.overlay.DataOverlays.current().datFile("equips/" + itemId + ".dat");
        if (!file.exists()) {
            return null;
        } else {
            return readEquipFromFile(file);
        }
    }

    private static Equip readEquipFromFile(File file) {
        Equip equip = null;
        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file))) {
            equip = new Equip();
            equip.setItemId(dataInputStream.readInt());
            equip.setiSlot(dataInputStream.readUTF());
            equip.setvSlot(dataInputStream.readUTF());
            equip.setrJob(dataInputStream.readShort());
            equip.setrLevel(dataInputStream.readShort());
            equip.setrStr(dataInputStream.readShort());
            equip.setrDex(dataInputStream.readShort());
            equip.setrInt(dataInputStream.readShort());
            equip.setrLuk(dataInputStream.readShort());
            equip.setrPop(dataInputStream.readShort());
            equip.setiStr(dataInputStream.readShort());
            equip.setiDex(dataInputStream.readShort());
            equip.setiInt(dataInputStream.readShort());
            equip.setiLuk(dataInputStream.readShort());
            equip.setiPDD(dataInputStream.readShort());
            equip.setiMDD(dataInputStream.readShort());
            equip.setiMaxHp(dataInputStream.readShort());
            equip.setiMaxHpr(dataInputStream.readShort());
            equip.setiMaxMp(dataInputStream.readShort());
            equip.setiMaxMpr(dataInputStream.readShort());
            equip.setiPad(dataInputStream.readShort());
            equip.setiMad(dataInputStream.readShort());
            equip.setiEva(dataInputStream.readShort());
            equip.setiAcc(dataInputStream.readShort());
            equip.setiCraft(dataInputStream.readShort());
            equip.setiSpeed(dataInputStream.readShort());
            equip.setiJump(dataInputStream.readShort());
            equip.getSymbol().setInc(dataInputStream.readShort());
            equip.setDamR(dataInputStream.readShort());
            equip.setStatR(dataInputStream.readShort());
            equip.setBdr(dataInputStream.readShort());
            equip.setImdr(dataInputStream.readShort());
            equip.setTuc(dataInputStream.readShort());
            equip.setCharmEXP(dataInputStream.readInt());
            equip.setSetItemID(dataInputStream.readInt());
            equip.setPrice(dataInputStream.readLong());
            equip.setAttackSpeed(dataInputStream.readInt());
            equip.setCash(dataInputStream.readBoolean());
            equip.setExpireOnLogout(dataInputStream.readBoolean());
            equip.setExItem(dataInputStream.readBoolean());
            equip.setNotSale(dataInputStream.readBoolean());
            equip.setOnly(dataInputStream.readBoolean());
            equip.setTradeBlock(dataInputStream.readBoolean());
            equip.setEquipTradeBlock(dataInputStream.readBoolean());
            equip.setFixedPotential(dataInputStream.readBoolean());
            equip.setNoPotential(dataInputStream.readBoolean());
            equip.setBossReward(dataInputStream.readBoolean() || List.of(ItemConstants.NON_KMS_BOSS_SETS).contains(equip.getSetItemID()) || List.of(ItemConstants.NON_KMS_BOSS_ITEMS).contains(equip.getItemId()));
            equip.setSuperiorEqp(dataInputStream.readBoolean());
            equip.setiReduceReq(dataInputStream.readShort());
            equip.setHasIUCMax(dataInputStream.readBoolean());
            if (equip.isHasIUCMax()) {
                equip.setIUCMax(dataInputStream.readShort());
            }
            short optionLength = dataInputStream.readShort();
            List<Integer> options = new ArrayList<>(optionLength);
            for (int i = 0; i < optionLength; i++) {
                options.add(dataInputStream.readInt());
            }
            for (int i = 0; i < 7 - optionLength; i++) {
                options.add(0);
            }
            equip.setOptions(options);
            short skillsLength = dataInputStream.readShort();
            for (int i = 0; i < skillsLength; i++) {
                equip.addItemSkill(new ItemSkill(dataInputStream.readInt(), dataInputStream.readByte()));
            }
            equip.setFixedGrade(dataInputStream.readInt());
            equip.setSpecialGrade(dataInputStream.readInt());
            equip.setAndroid(dataInputStream.readInt());
            equip.setAndroidGrade(dataInputStream.readInt());
            equip.setTradeAvailable(dataInputStream.readInt());
            equip.setAccountSharable(dataInputStream.readBoolean());
            equip.setSharableOnce(dataInputStream.readBoolean());
            equips.put(equip.getItemId(), equip);
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        return equip;
    }

    @Saver(varName = "equips")
    public static void saveEquips(String dir) {
        Util.makeDirIfAbsent(dir);
        for (Equip equip : equips.values()) {
            try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(dir + "/" + equip.getItemId() + ".dat"))) {
                dataOutputStream.writeInt(equip.getItemId());
                dataOutputStream.writeUTF(equip.getiSlot());
                dataOutputStream.writeUTF(equip.getvSlot());
                dataOutputStream.writeShort(equip.getrJob());
                dataOutputStream.writeShort(equip.getrLevel());
                dataOutputStream.writeShort(equip.getrStr());
                dataOutputStream.writeShort(equip.getrDex());
                dataOutputStream.writeShort(equip.getrInt());
                dataOutputStream.writeShort(equip.getrLuk());
                dataOutputStream.writeShort(equip.getrPop());
                dataOutputStream.writeShort(equip.getiStr());
                dataOutputStream.writeShort(equip.getiDex());
                dataOutputStream.writeShort(equip.getiInt());
                dataOutputStream.writeShort(equip.getiLuk());
                dataOutputStream.writeShort(equip.getiPDD());
                dataOutputStream.writeShort(equip.getiMDD());
                dataOutputStream.writeShort(equip.getiMaxHp());
                dataOutputStream.writeShort(equip.getiMaxHpr());
                dataOutputStream.writeShort(equip.getiMaxMp());
                dataOutputStream.writeShort(equip.getiMaxMpr());
                dataOutputStream.writeShort(equip.getiPad());
                dataOutputStream.writeShort(equip.getiMad());
                dataOutputStream.writeShort(equip.getiEva());
                dataOutputStream.writeShort(equip.getiAcc());
                dataOutputStream.writeShort(equip.getiCraft());
                dataOutputStream.writeShort(equip.getiSpeed());
                dataOutputStream.writeShort(equip.getiJump());
                dataOutputStream.writeShort(equip.getSymbol().getInc());
                dataOutputStream.writeShort(equip.getDamR());
                dataOutputStream.writeShort(equip.getStatR());
                dataOutputStream.writeShort(equip.getBdr());
                dataOutputStream.writeShort(equip.getImdr());
                dataOutputStream.writeShort(equip.getTuc());
                dataOutputStream.writeInt(equip.getCharmEXP());
                dataOutputStream.writeInt(equip.getSetItemID());
                dataOutputStream.writeLong(equip.getPrice());
                dataOutputStream.writeInt(equip.getAttackSpeed());
                dataOutputStream.writeBoolean(equip.isCash());
                dataOutputStream.writeBoolean(equip.isExpireOnLogout());
                dataOutputStream.writeBoolean(equip.isExItem());
                dataOutputStream.writeBoolean(equip.isNotSale());
                dataOutputStream.writeBoolean(equip.isOnly());
                dataOutputStream.writeBoolean(equip.isTradeBlock());
                dataOutputStream.writeBoolean(equip.isEquipTradeBlock());
                dataOutputStream.writeBoolean(equip.isFixedPotential());
                dataOutputStream.writeBoolean(equip.isNoPotential());
                dataOutputStream.writeBoolean(equip.isBossReward()
                        || Util.arrayContains(ItemConstants.NON_KMS_BOSS_SETS, equip.getSetItemID())
                        || Util.arrayContains(ItemConstants.NON_KMS_BOSS_ITEMS, equip.getItemId()));
                dataOutputStream.writeBoolean(equip.isSuperiorEqp());
                dataOutputStream.writeShort(equip.getiReduceReq());
                dataOutputStream.writeBoolean(equip.isHasIUCMax());
                if (equip.isHasIUCMax()) {
                    dataOutputStream.writeShort(equip.getIUCMax());
                }
                dataOutputStream.writeShort(equip.getOptions().size());
                for (int i : equip.getOptions()) {
                    dataOutputStream.writeInt(i);
                }
                dataOutputStream.writeShort(equip.getItemSkills().size());
                for (ItemSkill skill : equip.getItemSkills()) {
                    dataOutputStream.writeInt(skill.getSkill());
                    dataOutputStream.writeByte(skill.getSlv());
                }
                dataOutputStream.writeInt(equip.getFixedGrade());
                dataOutputStream.writeInt(equip.getSpecialGrade());
                dataOutputStream.writeInt(equip.getAndroid());
                dataOutputStream.writeInt(equip.getAndroidGrade());
                dataOutputStream.writeInt(equip.getTradeAvailable());
                dataOutputStream.writeBoolean(equip.getAccountSharable());
                dataOutputStream.writeBoolean(equip.getSharableOnce());
            } catch (IOException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
    }

    public static void loadEquipsFromWz() {
        String wzDir = ServerConstants.WZ_DIR + "/Character.wz";
        String[] subMaps = new String[]{"Accessory", "Android", "Cap", "Cape", "Coat", "Dragon", "Hair", "Face", "Glove",
                    "Longcoat", "Mechanic", "Pants", "PetEquip", "Ring", "Shield", "Shoes", "Totem", "Weapon", "MonsterBook",
                    "ArcaneForce", "AuthenticForce"};
        for (String subMap : subMaps) {
            File subDir = new File(String.format("%s/%s", wzDir, subMap));
            File[] files = subDir.listFiles();
            for (File file : files) {
                if (file.getName().contains("LinkCashWeaponData")) { //TODO [HEX]: Figure out what this meme is
                    continue;
                }
                if (file.isDirectory()) continue;
                Node node = XMLApi.getRoot(file);
                List<Node> nodes = XMLApi.getAllChildren(node);
                for (Node mainNode : nodes) {
                    Map<String, String> attributes = XMLApi.getAttributes(mainNode);
                    String mainName = attributes.get("name");
                    if (mainName != null) {
                        int itemId = Integer.parseInt(attributes.get("name").replace(".img", ""));
                        Equip equip = new Equip();
                        equip.setItemId(itemId);
                        equip.setType(Item.Type.EQUIP);
                        equip.setDateExpire(FileTime.MAX_TIME());
                        List<Integer> options = new ArrayList<>(7);
                        for (Node n : XMLApi.getAllChildren(XMLApi.getFirstChildByNameBF(mainNode, "info"))) {
                            String name = XMLApi.getNamedAttribute(n, "name");
                            String value = XMLApi.getNamedAttribute(n, "value");
                            switch (name) {
                                case "islot":
                                    equip.setiSlot(value);
                                    break;
                                case "vslot":
                                    equip.setvSlot(value);
                                    break;
                                case "reqJob":
                                    equip.setrJob(Short.parseShort(value));
                                    break;
                                case "reqLevel":
                                    equip.setrLevel(Short.parseShort(value));
                                    break;
                                case "reqSTR":
                                    equip.setrStr(Short.parseShort(value));
                                    break;
                                case "reqDEX":
                                    equip.setrDex(Short.parseShort(value));
                                    break;
                                case "reqINT":
                                    equip.setrInt(Short.parseShort(value));
                                    break;
                                case "reqLUK":
                                    equip.setrLuk(Short.parseShort(value));
                                    break;
                                case "reqPOP":
                                    equip.setrPop(Short.parseShort(value));
                                    break;
                                case "incSTR":
                                    equip.setiStr(Short.parseShort(value));
                                    break;
                                case "incDEX":
                                    equip.setiDex(Short.parseShort(value));
                                    break;
                                case "incINT":
                                    equip.setiInt(Short.parseShort(value));
                                    break;
                                case "incLUK":
                                    equip.setiLuk(Short.parseShort(value));
                                    break;
                                case "incPDD":
                                    equip.setiPDD(Short.parseShort(value));
                                    break;
                                case "incMDD":
                                    equip.setiMDD(Short.parseShort(value));
                                    break;
                                case "incMHP":
                                    equip.setiMaxHp(Short.parseShort(value));
                                    break;
                                case "incMMP":
                                    equip.setiMaxMp(Short.parseShort(value));
                                    break;
                                case "incMHPr":
                                    equip.setiMaxHpr(Short.parseShort(value));
                                    break;
                                case "incMMPr":
                                    equip.setiMaxMpr(Short.parseShort(value));
                                    break;
                                case "incPAD":
                                    equip.setiPad(Short.parseShort(value));
                                    break;
                                case "incMAD":
                                    equip.setiMad(Short.parseShort(value));
                                    break;
                                case "incEVA":
                                    equip.setiEva(Short.parseShort(value));
                                    break;
                                case "incACC":
                                    equip.setiAcc(Short.parseShort(value));
                                    break;
                                case "incSpeed":
                                    equip.setiSpeed(Short.parseShort(value));
                                    break;
                                case "incJump":
                                    equip.setiJump(Short.parseShort(value));
                                    break;
                                case "incARC":
                                    equip.getSymbol().setInc(Short.parseShort(value));
                                    break;
                                case "damR":
                                    equip.setDamR(Short.parseShort(value));
                                    break;
                                case "statR":
                                    equip.setStatR(Short.parseShort(value));
                                    break;
                                case "imdR":
                                    equip.setImdr(Short.parseShort(value));
                                    break;
                                case "bdR":
                                    equip.setBdr(Short.parseShort(value));
                                    break;
                                case "tuc":
                                    equip.setTuc(Short.parseShort(value));
                                    break;
                                case "IUCMax":
                                    equip.setHasIUCMax(true);
                                    equip.setIUCMax(Short.parseShort(value));
                                    break;
                                case "setItemID":
                                    equip.setSetItemID(Integer.parseInt(value));
                                    break;
                                case "price":
                                    equip.setPrice(Long.parseLong(value));
                                    break;
                                case "attackSpeed":
                                    equip.setAttackSpeed(Integer.parseInt(value));
                                    break;
                                case "cash":
                                    equip.setCash(Integer.parseInt(value) != 0);
                                    break;
                                case "expireOnLogout":
                                    equip.setExpireOnLogout(Integer.parseInt(value) != 0);
                                    break;
                                case "exItem":
                                    equip.setExItem(Integer.parseInt(value) != 0);
                                    break;
                                case "notSale":
                                    equip.setNotSale(Integer.parseInt(value) != 0);
                                    break;
                                case "only":
                                    equip.setOnly(Integer.parseInt(value) != 0);
                                    break;
                                case "tradeBlock":
                                    equip.setTradeBlock(Integer.parseInt(value) != 0);
                                    break;
                                case "equipTradeBlock":
                                    if (Util.isInteger(value)) {
                                        equip.setEquipTradeBlock(Integer.parseInt(value) != 0);
                                    }
                                    break;
                                case "fixedPotential":
                                    equip.setFixedPotential(Integer.parseInt(value) != 0);
                                    break;
                                case "noPotential":
                                    equip.setNoPotential(Integer.parseInt(value) != 0);
                                    break;
                                case "bossReward":
                                    equip.setBossReward(Integer.parseInt(value) != 0);
                                    break;
                                case "superiorEqp":
                                    equip.setSuperiorEqp(Integer.parseInt(value) != 0);
                                    break;
                                case "reduceReq":
                                    equip.setiReduceReq(Short.parseShort(value));
                                    break;
                                case "fixedGrade":
                                    equip.setFixedGrade(Integer.parseInt(value));
                                    break;
                                case "specialGrade":
                                    equip.setSpecialGrade(Integer.parseInt(value));
                                    break;
                                case "charmEXP":
                                    equip.setCharmEXP(Integer.parseInt(value));
                                    break;
                                case "level":
                                    // TODO: proper parsing, actual stats and skills for each level the equip gets
                                    Node levelCase = XMLApi.getFirstChildByNameBF(n, "case");
                                    if (levelCase != null) {
                                        Node case0 = XMLApi.getFirstChildByNameBF(levelCase, "0");
                                        if (case0 != null) {
                                            Node case1 = XMLApi.getFirstChildByNameBF(case0, "1");
                                            if (case1 != null) {
                                                Node equipmentSkill = XMLApi.getFirstChildByNameBF(case1, "EquipmentSkill");
                                                if (equipmentSkill != null) {
                                                    for (Node equipSkill : XMLApi.getAllChildren(equipmentSkill)) {
                                                        Map<String, String> idAttr = XMLApi.getAttributes(XMLApi.getFirstChildByNameBF(equipSkill, "id"));
                                                        Map<String, String> levelAttr = XMLApi.getAttributes(XMLApi.getFirstChildByNameBF(equipSkill, "level"));
                                                        equip.addItemSkill(new ItemSkill(Integer.parseInt(idAttr.get("value")), Byte.parseByte(levelAttr.get("value"))));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    break;
                                case "option":
                                    for (Node whichOptionNode : XMLApi.getAllChildren(n)) {
                                        attributes = XMLApi.getAttributes(whichOptionNode);
                                        int index = Integer.parseInt(attributes.get("name"));
                                        Node optionNode = XMLApi.getFirstChildByNameBF(whichOptionNode, "option");
                                        Map<String, String> optionAttr = XMLApi.getAttributes(optionNode);
                                        options.set(index, Integer.parseInt(optionAttr.get("value")));
                                    }
                                    break;
                                case "android":
                                    equip.setAndroid(Integer.parseInt(value));
                                    break;
                                case "grade":
                                    equip.setAndroidGrade(Integer.parseInt(value));
                                    break;
                                case "tradeAvailable":
                                    equip.setTradeAvailable(Integer.parseInt(value));
                                    break;
                                case "accountSharable":
                                    equip.setAccountSharable(Integer.parseInt(value) == 1);
                                    break;
                                case "sharableOnce":
                                    equip.setSharableOnce(Integer.parseInt(value) == 1);
                                    break;
                                case "icon":
                                case "iconRaw":
                                case "walk":
                                case "stand":
                                case "attack":
                                case "afterImage":
                                case "sfx":
                                case "accountShareTag":
                                case "addition":
                                case "invisibleFace":
                                case "onlyEquip":
                                case "bonusExp":
                                case "slotMax":
                                case "notExtend":
                                    break;
                                default:
                                    if (LOG_UNKS) {
                                        System.out.printf("Unhandled equip node, id = %s, name = %s, value = %s.", itemId, name, value);
                                    }
                                    break;
                            }
                            for (int i = 0; i < 7 - options.size(); i++) {
                                options.add(0);
                            }
                            equip.setOptions(options);
                        }
                        if (equip.isCash()) {
                            equip.setInvType(InvType.DECORATION);
                        } else {
                            equip.setInvType(InvType.EQUIP);
                        }
                        equips.put(equip.getItemId(), equip);
                    }
                }
            }
        }
    }

    public static ItemInfo loadItemByFile(File file) {
        ItemInfo itemInfo = null;
        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file))) {
            itemInfo = new ItemInfo();
            itemInfo.setItemId(dataInputStream.readInt());
            itemInfo.setInvType(InvType.getInvTypeByString(dataInputStream.readUTF()));
            itemInfo.setCash(dataInputStream.readBoolean());
            itemInfo.setPrice(dataInputStream.readInt());
            itemInfo.setUnitPrice(dataInputStream.readInt());
            itemInfo.setSlotMax(dataInputStream.readInt());
            itemInfo.setTradeBlock(dataInputStream.readBoolean());
            itemInfo.setNotSale(dataInputStream.readBoolean());
            itemInfo.setPath(dataInputStream.readUTF());
            itemInfo.setNoCursed(dataInputStream.readBoolean());
            itemInfo.setBagType(dataInputStream.readInt());
            itemInfo.setCharismaEXP(dataInputStream.readInt());
            itemInfo.setCharmEXP(dataInputStream.readInt());
            itemInfo.setSenseEXP(dataInputStream.readInt());
            itemInfo.setCraftEXP(dataInputStream.readInt());
            itemInfo.setWillEXP(dataInputStream.readInt());
            itemInfo.setNickSkill(dataInputStream.readInt());
            itemInfo.setQuest(dataInputStream.readBoolean());
            itemInfo.setReqQuestOnProgress(dataInputStream.readInt());
            itemInfo.setNotConsume(dataInputStream.readBoolean());
            itemInfo.setMonsterBook(dataInputStream.readBoolean());
            itemInfo.setMobID(dataInputStream.readInt());
            itemInfo.setCreateID(dataInputStream.readInt());
            itemInfo.setMobHP(dataInputStream.readInt());
            itemInfo.setNpcID(dataInputStream.readInt());
            itemInfo.setLinkedID(dataInputStream.readInt());
            itemInfo.setScript(dataInputStream.readUTF());
            itemInfo.setScriptNPC(dataInputStream.readInt());
            short size = dataInputStream.readShort();
            for (int i = 0; i < size; i++) {
                ScrollStat ss = ScrollStat.getScrollStatByString(dataInputStream.readUTF());
                int val = dataInputStream.readInt();
                itemInfo.putScrollStat(ss, val);
            }
            size = dataInputStream.readShort();
            for (int i = 0; i < size; i++) {
                SpecStat ss = SpecStat.getSpecStatByName(dataInputStream.readUTF());
                int val = dataInputStream.readInt();
                itemInfo.putSpecStat(ss, val);
            }
            size = dataInputStream.readShort();
            for (int i = 0; i < size; i++) {
                itemInfo.addQuest(dataInputStream.readInt());
            }
            size = dataInputStream.readShort();
            for (int i = 0; i < size; i++) {
                itemInfo.getReqItemIds().add(dataInputStream.readInt());
            }
            size = dataInputStream.readShort();
            for (int i = 0; i < size; i++) {
                itemInfo.addSkill(dataInputStream.readInt());
            }
            size = dataInputStream.readShort();
            for (int i = 0; i < size; i++) {
                ItemRewardInfo iri = new ItemRewardInfo();
                iri.setCount(dataInputStream.readInt());
                iri.setItemID(dataInputStream.readInt());
                iri.setProb(dataInputStream.readDouble());
                iri.setPeriod(dataInputStream.readInt());
                iri.setEffect(dataInputStream.readUTF());
                iri.setMeso(dataInputStream.readInt());
                itemInfo.addItemReward(iri);
            }
            itemInfo.setReqSkillLv(dataInputStream.readInt());
            itemInfo.setMasterLv(dataInputStream.readInt());
            itemInfo.setStateChangeItem(dataInputStream.readInt());
            itemInfo.setMeso(dataInputStream.readInt());
            itemInfo.setMaplepoint(dataInputStream.readInt());
            itemInfo.setPointCost(dataInputStream.readInt());
            itemInfo.setExNew(dataInputStream.readBoolean());
            itemInfo.setMoveTo(dataInputStream.readInt());
            itemInfo.setSkillId(dataInputStream.readInt());
            itemInfo.setRecoveryHP(dataInputStream.readInt());
            itemInfo.setRecoveryMP(dataInputStream.readInt());
            itemInfo.setMinusLevel(dataInputStream.readInt());
            itemInfo.setExpireOnLogout(dataInputStream.readBoolean());
            itemInfo.setAccountSharable(dataInputStream.readBoolean());
            itemInfo.setSharableOnce(dataInputStream.readBoolean());
            items.put(itemInfo.getItemId(), itemInfo);
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        return itemInfo;

    }

    public static void saveItems(String dir) {
        Util.makeDirIfAbsent(dir);
        for (ItemInfo ii : items.values()) {
            try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(new File(dir + "/" + ii.getItemId() + ".dat")))) {
                dataOutputStream.writeInt(ii.getItemId());
                dataOutputStream.writeUTF(ii.getInvType().toString());
                dataOutputStream.writeBoolean(ii.isCash());
                dataOutputStream.writeInt(ii.getPrice());
                dataOutputStream.writeInt(ii.getUnitPrice());
                dataOutputStream.writeInt(ii.getSlotMax());
                dataOutputStream.writeBoolean(ii.isTradeBlock());
                dataOutputStream.writeBoolean(ii.isNotSale());
                dataOutputStream.writeUTF(ii.getPath());
                dataOutputStream.writeBoolean(ii.isNoCursed());
                dataOutputStream.writeInt(ii.getBagType());
                dataOutputStream.writeInt(ii.getCharismaEXP());
                dataOutputStream.writeInt(ii.getCharmEXP());
                dataOutputStream.writeInt(ii.getSenseEXP());
                dataOutputStream.writeInt(ii.getCraftEXP());
                dataOutputStream.writeInt(ii.getWillEXP());
                dataOutputStream.writeInt(ii.getNickSkill());
                dataOutputStream.writeBoolean(ii.isQuest());
                dataOutputStream.writeInt(ii.getReqQuestOnProgress());
                dataOutputStream.writeBoolean(ii.isNotConsume());
                dataOutputStream.writeBoolean(ii.isMonsterBook());
                dataOutputStream.writeInt(ii.getMobID());
                dataOutputStream.writeInt(ii.getCreateID());
                dataOutputStream.writeInt(ii.getMobHP());
                dataOutputStream.writeInt(ii.getNpcID());
                dataOutputStream.writeInt(ii.getLinkedID());
                dataOutputStream.writeUTF(ii.getScript());
                dataOutputStream.writeInt(ii.getScriptNPC());
                dataOutputStream.writeShort(ii.getScrollStats().size());
                for (Map.Entry<ScrollStat, Integer> entry : ii.getScrollStats().entrySet()) {
                    dataOutputStream.writeUTF(entry.getKey().toString());
                    dataOutputStream.writeInt(entry.getValue());
                }
                dataOutputStream.writeShort(ii.getSpecStats().size());
                for (Map.Entry<SpecStat, Integer> entry : ii.getSpecStats().entrySet()) {
                    dataOutputStream.writeUTF(entry.getKey().toString());
                    dataOutputStream.writeInt(entry.getValue());
                }
                dataOutputStream.writeShort(ii.getQuestIDs().size());
                for (int i : ii.getQuestIDs()) {
                    dataOutputStream.writeInt(i);
                }
                dataOutputStream.writeShort(ii.getReqItemIds().size());
                for (int i : ii.getReqItemIds()) {
                    dataOutputStream.writeInt(i);
                }
                dataOutputStream.writeShort(ii.getSkills().size());
                for (int i : ii.getSkills()) {
                    dataOutputStream.writeInt(i);
                }
                dataOutputStream.writeShort(ii.getItemRewardInfos().size());
                for (ItemRewardInfo iri : ii.getItemRewardInfos()) {
                    dataOutputStream.writeInt(iri.getCount());
                    dataOutputStream.writeInt(iri.getItemID());
                    dataOutputStream.writeDouble(iri.getProb());
                    dataOutputStream.writeInt(iri.getPeriod());
                    dataOutputStream.writeUTF(iri.getEffect());
                    dataOutputStream.writeInt(iri.getMeso());
                }
                dataOutputStream.writeInt(ii.getReqSkillLv());
                dataOutputStream.writeInt(ii.getMasterLv());
                dataOutputStream.writeInt(ii.getStateChangeItem());
                dataOutputStream.writeInt(ii.getMeso());
                dataOutputStream.writeInt(ii.getMaplepoint());
                dataOutputStream.writeInt(ii.getPointCost());
                dataOutputStream.writeBoolean(ii.isExNew());
                dataOutputStream.writeInt(ii.getMoveTo());
                dataOutputStream.writeInt(ii.getSkillId());
                dataOutputStream.writeInt(ii.getRecoveryHP());
                dataOutputStream.writeInt(ii.getRecoveryMP());
                dataOutputStream.writeInt(ii.getMinusLevel());
                dataOutputStream.writeBoolean(ii.isExpireOnLogout());
                dataOutputStream.writeBoolean(ii.isAccountSharable());
                dataOutputStream.writeBoolean(ii.isSharableOnce());
            } catch (IOException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
    }

    public static void savePets(String dir) {
        Util.makeDirIfAbsent(dir);
        for (PetInfo pi : pets.values()) {
            File file = new File(String.format("%s/%d.dat", dir, pi.getItemID()));
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
                dos.writeInt(pi.getItemID());
                dos.writeByte(pi.getInvType().getVal());
                dos.writeShort(pi.getWonderGrade());
                dos.writeInt(pi.getLife());
                dos.writeInt(pi.getSetItemID());
                dos.writeInt(pi.getLimitedLife());
                dos.writeInt(pi.getEvolutionID());
                dos.writeInt(pi.getType());
                dos.writeInt(pi.getEvolReqItemID());
                dos.writeInt(pi.getEvolNo());
                dos.writeInt(pi.getEvol1());
                dos.writeInt(pi.getEvol2());
                dos.writeInt(pi.getEvol3());
                dos.writeInt(pi.getEvol4());
                dos.writeInt(pi.getEvol5());
                dos.writeInt(pi.getProbEvol1());
                dos.writeInt(pi.getProbEvol2());
                dos.writeInt(pi.getProbEvol3());
                dos.writeInt(pi.getProbEvol4());
                dos.writeInt(pi.getProbEvol5());
                dos.writeInt(pi.getEvolReqPetLvl());
                dos.writeBoolean(pi.isAllowOverlappedSet());
                dos.writeBoolean(pi.isNoRevive());
                dos.writeBoolean(pi.isNoScroll());
                dos.writeBoolean(pi.isCash());
                dos.writeBoolean(pi.isGiantPet());
                dos.writeBoolean(pi.isPermanent());
                dos.writeBoolean(pi.isPickupItem());
                dos.writeBoolean(pi.isInteractByUserAction());
                dos.writeBoolean(pi.isLongRange());
                dos.writeBoolean(pi.isMultiPet());
                dos.writeBoolean(pi.isAutoBuff());
                dos.writeBoolean(pi.isStarPlanetPet());
                dos.writeBoolean(pi.isEvol());
                dos.writeBoolean(pi.isAutoReact());
                dos.writeBoolean(pi.isPickupAll());
                dos.writeBoolean(pi.isSweepForDrop());
                dos.writeBoolean(pi.isConsumeMP());
                dos.writeUTF(pi.getRunScript() == null ? "" : pi.getRunScript());
            } catch (IOException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
    }

    public static PetInfo getPetInfoByID(int id) {
        PetInfo overlay = OVERLAY_PETS.get(id, ItemData::loadPetFromFile);
        if (overlay != null) {
            return overlay;
        }
        return pets.getOrDefault(id, loadPetByID(id));
    }

    public static PetInfo loadPetByID(int id) {
        File file = new File(String.format("%s/pets/%d.dat", ServerConstants.DAT_DIR, id));
        if (file.exists()) {
            return loadPetFromFile(file);
        } else {
            return null;
        }
    }

    private static PetInfo loadPetFromFile(File file) {
        PetInfo pi = null;
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            pi = new PetInfo();
            pi.setItemID(dis.readInt());
            pi.setInvType(InvType.getInvTypeByVal(dis.readByte()));
            pi.setWonderGrade(dis.readShort());
            pi.setLife(dis.readInt());
            pi.setSetItemID(dis.readInt());
            pi.setLimitedLife(dis.readInt());
            pi.setEvolutionID(dis.readInt());
            pi.setType(dis.readInt());
            pi.setEvolReqItemID(dis.readInt());
            pi.setEvolNo(dis.readInt());
            pi.setEvol1(dis.readInt());
            pi.setEvol2(dis.readInt());
            pi.setEvol3(dis.readInt());
            pi.setEvol4(dis.readInt());
            pi.setEvol5(dis.readInt());
            pi.setProbEvol1(dis.readInt());
            pi.setProbEvol2(dis.readInt());
            pi.setProbEvol3(dis.readInt());
            pi.setProbEvol4(dis.readInt());
            pi.setProbEvol5(dis.readInt());
            pi.setEvolReqPetLvl(dis.readInt());
            pi.setAllowOverlappedSet(dis.readBoolean());
            pi.setNoRevive(dis.readBoolean());
            pi.setNoScroll(dis.readBoolean());
            pi.setCash(dis.readBoolean());
            pi.setGiantPet(dis.readBoolean());
            pi.setPermanent(dis.readBoolean());
            pi.setPickupItem(dis.readBoolean());
            pi.setInteractByUserAction(dis.readBoolean());
            pi.setLongRange(dis.readBoolean());
            pi.setMultiPet(dis.readBoolean());
            pi.setAutoBuff(dis.readBoolean());
            pi.setStarPlanetPet(dis.readBoolean());
            pi.setEvol(dis.readBoolean());
            pi.setAutoReact(dis.readBoolean());
            pi.setPickupAll(dis.readBoolean());
            pi.setSweepForDrop(dis.readBoolean());
            pi.setConsumeMP(dis.readBoolean());
            pi.setRunScript(dis.readUTF());
            pets.put(pi.getItemID(), pi);
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        return pi;
    }

    public static void loadPetsFromWZ() {
        String wzDir = ServerConstants.WZ_DIR + "/Item.wz";
        File petDir = new File(String.format("%s/%s", wzDir, "Pet"));
        for (File file : petDir.listFiles()) {
            if (file.isDirectory()) continue;
            Document doc = XMLApi.getRoot(file);
            int id = Integer.parseInt(file.getName().replace(".img.xml", ""));
            PetInfo pi = new PetInfo();
            pi.setItemID(id);
            pi.setInvType(InvType.CONSUME);
            Node infoNode = XMLApi.getFirstChildByNameBF(doc, "info");
            for (Node node : XMLApi.getAllChildren(infoNode)) {
                String name = XMLApi.getNamedAttribute(node, "name");
                String value = XMLApi.getNamedAttribute(node, "value");
                switch (name) {
                    case "icon":
                    case "iconD":
                    case "iconRaw":
                    case "iconRawD":
                    case "hungry":
                    case "nameTag":
                    case "chatBalloon":
                    case "noHungry":
                        break;
                    case "wonderGrade":
                        pi.setWonderGrade(Short.parseShort(value));
                        break;
                    case "life":
                        pi.setLife(Integer.parseInt(value));
                        break;
                    case "setItemID":
                        pi.setSetItemID(Integer.parseInt(value));
                        break;
                    case "evolutionID":
                        pi.setEvolutionID(Integer.parseInt(value));
                        break;
                    case "type":
                        pi.setType(Integer.parseInt(value));
                        break;
                    case "limitedLife":
                        pi.setLimitedLife(Integer.parseInt(value));
                        break;
                    case "evol1":
                        pi.setEvol1(Integer.parseInt(value));
                        break;
                    case "evol2":
                        pi.setEvol2(Integer.parseInt(value));
                        break;
                    case "evol3":
                        pi.setEvol3(Integer.parseInt(value));
                        break;
                    case "evol4":
                        pi.setEvol4(Integer.parseInt(value));
                        break;
                    case "evol5":
                        pi.setEvol5(Integer.parseInt(value));
                        break;
                    case "evolProb1":
                        pi.setProbEvol1(Integer.parseInt(value));
                        break;
                    case "evolProb2":
                        pi.setProbEvol2(Integer.parseInt(value));
                        break;
                    case "evolProb3":
                        pi.setProbEvol3(Integer.parseInt(value));
                        break;
                    case "evolProb4":
                        pi.setProbEvol4(Integer.parseInt(value));
                        break;
                    case "evolProb5":
                        pi.setProbEvol5(Integer.parseInt(value));
                        break;
                    case "evolReqItemID":
                        pi.setEvolReqItemID(Integer.parseInt(value));
                        break;
                    case "evolReqPetLvl":
                        pi.setEvolReqPetLvl(Integer.parseInt(value));
                        break;
                    case "evolNo":
                        pi.setEvolNo(Integer.parseInt(value));
                        break;
                    case "permanent":
                        pi.setPermanent(Integer.parseInt(value) != 0);
                        break;
                    case "pickupItem":
                        pi.setPickupItem(Integer.parseInt(value) != 0);
                        break;
                    case "interactByUserAction":
                        pi.setInteractByUserAction(Integer.parseInt(value) != 0);
                        break;
                    case "longRange":
                        pi.setLongRange(Integer.parseInt(value) != 0);
                        break;
                    case "giantPet":
                        pi.setGiantPet(Integer.parseInt(value) != 0);
                        break;
                    case "noMoveToLocker":
                        pi.setAllowOverlappedSet(Integer.parseInt(value) != 0);
                        break;
                    case "allowOverlappedSet":
                        pi.setAllowOverlappedSet(Integer.parseInt(value) != 0);
                        break;
                    case "noRevive":
                        pi.setNoRevive(Integer.parseInt(value) != 0);
                        break;
                    case "noScroll":
                        pi.setNoScroll(Integer.parseInt(value) != 0);
                        break;
                    case "autoBuff":
                        pi.setAutoBuff(Integer.parseInt(value) != 0);
                        break;
                    case "multiPet":
                        pi.setMultiPet(Integer.parseInt(value) != 0);
                        break;
                    case "autoReact":
                        pi.setAutoReact(Integer.parseInt(value) != 0);
                        break;
                    case "pickupAll":
                        pi.setPickupAll(Integer.parseInt(value) != 0);
                        break;
                    case "sweepForDrop":
                        pi.setSweepForDrop(Integer.parseInt(value) != 0);
                        break;
                    case "consumeMP":
                        pi.setConsumeMP(Integer.parseInt(value) != 0);
                        break;
                    case "evol":
                        pi.setEvol(Integer.parseInt(value) != 0);
                        break;
                    case "starPlanetPet":
                        pi.setStarPlanetPet(Integer.parseInt(value) != 0);
                        break;
                    case "cash":
                        pi.setCash(Integer.parseInt(value) != 0);
                        pi.setInvType(InvType.CASH);
                        pi.setCash(true);
                        break;
                    case "runScript":
                        pi.setRunScript(value);
                        break;
                    case "interact":
                        Node interactNode = XMLApi.getFirstChildByNameBF(doc, name);
                        for (Node iNode : XMLApi.getAllChildren(interactNode)) {
                            String iName = XMLApi.getNamedAttribute(iNode, "name");
                            String iValue = XMLApi.getNamedAttribute(iNode, "value");
                            Node iNameNode = XMLApi.getFirstChildByNameBF(doc, iName);
                            for (Node siNode : XMLApi.getAllChildren(iNameNode)) {
                                String siName = XMLApi.getNamedAttribute(siNode, "name");
                                String siValue = XMLApi.getNamedAttribute(siNode, "value");
                                switch (siName) {
                                    case "prob":
                                        petCommands.put(Integer.parseInt(iName), Integer.parseInt(siValue));
                                        break;
                                }
                            }
                        }
                        break;
                    default:
                        if (LOG_UNKS) {
                            System.out.printf("Unhandled pet node, name = %s, value = %s.", name, value);
                        }
                        break;
                }
            }
            pets.put(pi.getItemID(), pi);
            ItemInfo ii = new ItemInfo();
            ii.setItemId(pi.getItemID());
            ii.setInvType(pi.getInvType());
            items.put(ii.getItemId(), ii);
        }
    }

    public static void loadItemsFromWZ() {
        String wzDir = ServerConstants.WZ_DIR + "/Item.wz";
        String[] subMaps = new String[]{"Cash", "Consume", "Etc", "Install", "Special"}; // not pet
        for (String subMap : subMaps) {
            File subDir = new File(String.format("%s/%s", wzDir, subMap));
            File[] files = subDir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) continue;
                Document doc = XMLApi.getRoot(file);
                Node node = doc;
                List<Node> nodes = XMLApi.getAllChildren(node);
                for (Node mainNode : XMLApi.getAllChildren(nodes.get(0))) {
                    String nodeName = XMLApi.getNamedAttribute(mainNode, "name");
                    if (!Util.isNumber(nodeName)) {
                        if (LOG_UNKS) {
                            System.out.printf("%s is not a number.", nodeName);
                        }
                        continue;
                    }
                    int id = Integer.parseInt(nodeName);
                    ItemInfo item = new ItemInfo();
                    item.setItemId(id);
                    item.setInvType(InvType.getInvTypeByString(subMap));
                    Node infoNode = XMLApi.getFirstChildByNameBF(mainNode, "info");
                    if (infoNode != null) {
                        for (Node info : XMLApi.getAllChildren(infoNode)) {
                            String name = XMLApi.getNamedAttribute(info, "name");
                            String value = XMLApi.getNamedAttribute(info, "value");
                            int intValue = 0;
                            if (Util.isInteger(value)) {
                                intValue = Integer.parseInt(value);
                            }
                            switch (name) {
                                case "cash":
                                    item.setCash(intValue != 0);
                                    break;
                                case "price":
                                    item.setPrice(intValue);
                                    break;
                                case "slotMax":
                                    item.setSlotMax(intValue);
                                    break;
                                case "accountSharable":
                                    item.setAccountSharable(intValue == 1);
                                    break;
                                case "sharableOnce":
                                    item.setSharableOnce(intValue == 1);
                                    break;
                                // info not currently interesting. May be interesting in the future.
                                case "icon":
                                case "iconRaw":
                                case "iconD":
                                case "iconReward":
                                case "iconShop":
                                    break;
                                case "recoveryHP":
                                    item.setRecoveryHP(intValue);
                                    break;
                                case "recoveryMP":
                                    item.setRecoveryMP(intValue);
                                    break;
                                case "sitAction":
                                case "bodyRelMove":
                                case "only":
                                case "noDrop":
                                case "timeLimited":
                                case "nickTag":
                                case "endLotteryDate":
                                case "noFlip":
                                case "noMoveToLocker":
                                case "soldInform":
                                case "purchaseShop":
                                case "flatRate":
                                case "limitMin":
                                case "protectTime":
                                case "maxDays":
                                case "replace":
                                    break;
                                case "expireOnLogout":
                                    item.setExpireOnLogout(intValue != 0);
                                    break;
                                case "max":
                                case "lvOptimum":
                                case "lvRange":
                                case "limitedLv":
                                case "tradeReward":
                                case "type":
                                case "floatType":
                                case "message":
                                case "pquest":
                                case "bonusEXPRate":
                                case "notExtend":
                                    break;
                                case "skill":
                                    for (Node masteryBookSkillIdNode : XMLApi.getAllChildren(info)) {
                                        item.addSkill(Integer.parseInt((XMLApi.getNamedAttribute(masteryBookSkillIdNode, "value"))));
                                    }
                                    break;
                                case "reqSkillLevel":
                                    item.setReqSkillLv(intValue);
                                    break;
                                case "masterLevel":
                                    item.setMasterLv(intValue);
                                    break;
                                case "stateChangeItem":
                                    item.setStateChangeItem(intValue);
                                    break;
                                case "meso":
                                    item.setMeso(intValue);
                                    break;
                                case "maplepoint":
                                    item.setMaplepoint(intValue);
                                    break;
                                case "direction":
                                case "exGrade":
                                case "exGradeWeight":
                                case "effect":
                                case "bigSize":
                                case "nickSkillTimeLimited":
                                case "StarPlanet":
                                case "useTradeBlock":
                                case "commerce":
                                case "invisibleWeapon":
                                case "sitEmotion":
                                case "sitLeft":
                                case "tamingMob":
                                case "textInfo":
                                case "lv":
                                case "tradeAvailable":
                                case "pickUpBlock":
                                case "rewardItemID":
                                case "autoPrice":
                                case "selectedSlot":
                                case "addTime":
                                case "reqLevel":
                                case "waittime":
                                case "buffchair":
                                case "cooltime":
                                case "consumeitem":
                                case "distanceX":
                                case "distanceY":
                                case "maxDiff":
                                case "maxDX":
                                case "levelDX":
                                case "maxLevel":
                                case "exp":
                                case "dropBlock":
                                case "dropExpireTime":
                                case "animation_create":
                                case "animation_dropped":
                                case "noCancelMouse":
                                case "soulItemType":
                                case "Rate":
                                case "delayMsg":
                                case "bridlePropZeroMsg":
                                    break;
                                case "minusLevel":
                                    item.setMinusLevel(intValue);
                                    break;
                                case "create":
                                    item.setCreateID(intValue);
                                    break;
                                case "nomobMsg":
                                case "bridleProp":
                                case "bridlePropChg":
                                case "bridleMsgType":
                                    break;
                                case "mobHP":
                                    item.setMobHP(intValue);
                                    break;
                                case "pointCost":
                                    item.setPointCost(intValue);
                                    break;
                                case "exNew":
                                    item.setExNew(intValue != 0);
                                    break;
                                case "left":
                                case "right":
                                case "top":
                                case "bottom":
                                case "useDelay":
                                case "name":
                                case "uiData":
                                case "UI":
                                case "recoveryRate":
                                case "itemMsg":
                                case "noRotateIcon":
                                case "endUseDate":
                                case "noSound":
                                case "slotMat":
                                case "isBgmOrEffect":
                                case "bgmPath":
                                case "repeat":
                                case "NoCancel":
                                case "rotateSpeed":
                                case "gender":
                                case "life":
                                case "pickupItem":
                                case "add":
                                case "consumeHP":
                                case "longRange":
                                case "dropSweep":
                                case "pickupAll":
                                case "ignorePickup":
                                case "consumeMP":
                                case "autoBuff":
                                case "smartPet":
                                case "giantPet":
                                case "shop":
                                case "recall":
                                case "autoSpeaking":
                                case "consumeCure":
                                case "rate":
                                case "overlap":
                                case "lt":
                                case "rb":
                                case "path4Top":
                                case "jumplevel":
                                case "slotIndex":
                                case "addDay":
                                case "incLEV":
                                case "cashTradeBlock":
                                case "dressUpgrade":
                                case "skillEffectID":
                                case "emotion":
                                case "tradBlock":
                                case "tragetBlock":
                                case "scanTradeBlock":
                                case "mobPotion":
                                case "ignoreTendencyStatLimit":
                                case "effectByItemID":
                                case "pachinko":
                                case "iconEnter":
                                case "iconLeave":
                                case "noMoveIcon":
                                case "noShadow":
                                case "preventslip":
                                case "warmsupport":
                                case "reqCUC":
                                case "incCraft":
                                case "reqEquipLevelMin":
                                case "incPVPDamage":
                                case "successRates":
                                case "enchantCategory":
                                case "additionalSuccess":
                                case "level":
                                case "specialItem":
                                case "cuttable": // Equipment items that do not have a #cScissor count# will have <cuttable> Scissor uses added to them.
                                case "resetRUC": // Scroll for Carlton's Mustache for Upgrade Count Reset
                                case "incMax":
                                case "noSuperior":
                                case "noRecovery":
                                case "reqMap":
                                case "random":
                                case "limit":
                                case "cantAccountSharable":
                                case "LvUpWarning":
                                case "canAccountSharable":
                                case "canUseJob":
                                case "createPeriod":
                                case "iconLarge":
                                case "morphItem":
                                case "consumableFrom":
                                case "noExpend":
                                case "sample":
                                case "notPickUpByPet":
                                case "bonusStageItem":
                                case "sampleOffsetY":
                                case "runOnPickup":
                                case "noSale":
                                case "skillCast":
                                case "activateCardSetID":
                                case "cursor":
                                case "karma":
                                case "itemPoint":
                                case "sharedStatCostGrade":
                                case "levelVariation":
                                case "accountShareable":
                                case "extendLimit":
                                case "showMessage":
                                case "mcType":
                                case "consumeItem":
                                case "hybrid":
                                case "mobId":
                                case "lvMin":
                                case "lvMax":
                                case "picture":
                                case "ratef":
                                case "time":
                                case "reqGuildLevel":
                                case "guild":
                                case "randEffect":
                                case "accountShareTag":
                                case "removeEffect":
                                case "forcingItem":
                                case "fixFrameIdx":
                                case "buffItemID":
                                case "removeCharacterInfo":
                                case "nameInfo":
                                case "bgmInfo":
                                case "flip":
                                case "pos":
                                case "randomChair":
                                case "maxLength":
                                case "continuity":
                                case "specificDX":
                                case "groupTWInfo":
                                case "face":
                                case "removeBody":
                                case "mesoChair":
                                case "towerBottom":
                                case "towerTop":
                                case "topOffset":
                                case "summonSoulMobID":
                                    break;
                                case "unitPrice":
                                    item.setUnitPrice(intValue);
                                    break;
                                case "tradeBlock":
                                    item.setTradeBlock(intValue != 0);
                                    break;
                                case "notSale":
                                    item.setNotSale(intValue != 0);
                                    break;
                                case "path":
                                    item.setPath(value);
                                    break;
                                case "noCursed":
                                    item.setNoCursed(intValue != 0);
                                    break;
                                case "noNegative":
                                    item.putScrollStat(noNegative, intValue);
                                    break;
                                case "incRandVol":
                                    item.putScrollStat(incRandVol, intValue);
                                    break;
                                case "success":
                                    item.putScrollStat(success, intValue);
                                    break;
                                case "incSTR":
                                    item.putScrollStat(incSTR, intValue);
                                    break;
                                case "incDEX":
                                    item.putScrollStat(incDEX, intValue);
                                    break;
                                case "incINT":
                                    item.putScrollStat(incINT, intValue);
                                    break;
                                case "incLUK":
                                    item.putScrollStat(incLUK, intValue);
                                    break;
                                case "incPAD":
                                    item.putScrollStat(incPAD, intValue);
                                    break;
                                case "incMAD":
                                    item.putScrollStat(incMAD, intValue);
                                    break;
                                case "incPDD":
                                    item.putScrollStat(incPDD, intValue);
                                    break;
                                case "incMDD":
                                    item.putScrollStat(incMDD, intValue);
                                    break;
                                case "incEVA":
                                    item.putScrollStat(incEVA, intValue);
                                    break;
                                case "incACC":
                                    item.putScrollStat(incACC, intValue);
                                    break;
                                case "incPERIOD":
                                    item.putScrollStat(incPERIOD, intValue);
                                    break;
                                case "incMHP":
                                case "incMaxHP":
                                    item.putScrollStat(incMHP, intValue);
                                    break;
                                case "incMMP":
                                case "incMaxMP":
                                    item.putScrollStat(incMMP, intValue);
                                    break;
                                case "incSpeed":
                                    item.putScrollStat(incSpeed, intValue);
                                    break;
                                case "incJump":
                                    item.putScrollStat(incJump, intValue);
                                    break;
                                case "incReqLevel":
                                    item.putScrollStat(incReqLevel, intValue);
                                    break;
                                case "randOption":
                                    item.putScrollStat(randOption, intValue);
                                    break;
                                case "randstat":
                                case "randStat":
                                    item.putScrollStat(randStat, intValue);
                                    break;
                                case "tuc":
                                    item.putScrollStat(tuc, intValue);
                                    break;
                                case "incIUC":
                                    item.putScrollStat(incIUC, intValue);
                                    break;
                                case "speed":
                                    item.putScrollStat(speed, intValue);
                                    break;
                                case "forceUpgrade":
                                    item.putScrollStat(forceUpgrade, intValue);
                                    break;
                                case "cursed":
                                    item.putScrollStat(cursed, intValue);
                                    break;
                                case "maxSuperiorEqp":
                                    item.putScrollStat(maxSuperiorEqp, intValue);
                                    break;
                                case "reqRUC":
                                    item.putScrollStat(reqRUC, intValue);
                                    break;
                                case "bagType":
                                    item.setBagType(intValue);
                                    break;
                                case "charismaEXP":
                                    item.setCharismaEXP(intValue);
                                    break;
                                case "charmEXP":
                                    item.setCharmEXP(intValue);
                                    break;
                                case "senseEXP":
                                    item.setSenseEXP(intValue);
                                    break;
                                case "craftEXP":
                                    item.setCraftEXP(intValue);
                                    break;
                                case "willEXP":
                                    item.setWillEXP(intValue);
                                    break;
                                case "nickSkill":
                                    item.setNickSkill(intValue);
                                    break;
                                case "quest":
                                    item.setQuest(intValue != 0);
                                    break;
                                case "reqQuestOnProgress":
                                    item.setReqQuestOnProgress(intValue);
                                    break;
                                case "qid":
                                case "questId":
                                    if (value.contains(".") && value.split("[.]").length > 0) {
                                        item.addQuest(Integer.parseInt(value.split("[.]")[0]));
                                    } else {
                                        item.addQuest(intValue);
                                    }
                                    break;
                                case "notConsume":
                                    item.setNotConsume(intValue != 0);
                                    break;
                                case "monsterBook":
                                    item.setMonsterBook(intValue != 0);
                                    break;
                                case "mob":
                                    item.setMobID(intValue);
                                    break;
                                case "npc":
                                    item.setNpcID(intValue);
                                    break;
                                case "linkedID":
                                    item.setLinkedID(intValue);
                                    break;
                                case "reqEquipLevelMax":
                                    item.putScrollStat(reqEquipLevelMax, intValue);
                                    break;
                                case "createType":
                                    item.putScrollStat(createType, intValue);
                                    break;
                                case "optionType":
                                    item.putScrollStat(optionType, intValue);
                                    break;
                                case "grade":
                                    item.setGrade(intValue);
                                    break;
                                case "android":
                                    item.setAndroid(intValue);
                                    break;
                                case "recover":
                                    item.putScrollStat(recover, intValue);
                                    break;
                                case "reset":
                                    item.putScrollStat(reset, intValue);
                                    break;
                                case "perfectReset":
                                    item.putScrollStat(perfectReset, intValue);
                                    break;
                                case "setItemCategory":
                                    item.putScrollStat(setItemCategory, intValue);
                                    break;
                                default:
                                    if (LOG_UNKS) {
                                        System.out.printf("Unknown node: %s, value = %s, itemID = %s", name, value, item.getItemId());
                                    }
                            }
                        }
                    }
                    Node reqNode = XMLApi.getFirstChildByNameBF(mainNode, "req");
                    if (reqNode != null) {
                        for (Node req : XMLApi.getAllChildren(reqNode)) {
                            String name = XMLApi.getNamedAttribute(req, "name");
                            String value = XMLApi.getNamedAttribute(req, "value");
                            item.getReqItemIds().add(Integer.parseInt(value));
                        }
                    }
                    Node socket = XMLApi.getFirstChildByNameBF(mainNode, "socket");
                    if (socket != null) {
                        for (Node socketNode : XMLApi.getAllChildren(socket)) {
                            String name = XMLApi.getNamedAttribute(socketNode, "name");
                            String value = XMLApi.getNamedAttribute(socketNode, "value");
                            switch (name) {
                                case "optionType":
                                    item.putScrollStat(optionType, Integer.parseInt(value));
                                    break;
                            }
                        }
                        Node option = XMLApi.getFirstChildByNameBF(socket, "option");
                        if (option != null) {
                            Node o = XMLApi.getFirstChildByNameBF(option, "0");
                            if (o != null) {
                                Node optionString = XMLApi.getFirstChildByNameDF(o, "optionString");
                                String ssName = "";
                                if (XMLApi.getNamedAttribute(optionString, "value") != null) {
                                    ssName = XMLApi.getNamedAttribute(optionString, "value");
                                }
                                Node level = XMLApi.getFirstChildByNameDF(o, "level");
                                int ssVal = 0;
                                if (XMLApi.getNamedAttribute(level, "value") != null) {
                                    ssVal = Integer.parseInt(XMLApi.getNamedAttribute(level, "value"));
                                }
                                if (ScrollStat.getScrollStatByString(ssName) != null) {
                                    item.putScrollStat(ScrollStat.valueOf(ssName), ssVal);
                                } else {
                                    System.out.println("non existent scroll stat" + ssName);
                                }
                            }
                        }
                    }
                    Node spec = XMLApi.getFirstChildByNameBF(mainNode, "spec");
                    if (spec != null) {
                        for (Node specNode : XMLApi.getAllChildren(spec)) {
                            String name = XMLApi.getNamedAttribute(specNode, "name");
                            String value = XMLApi.getNamedAttribute(specNode, "value");
                            switch (name) {
                                case "script":
                                    item.setScript(value);
                                    break;
                                case "npc":
                                    item.setScriptNPC(Integer.parseInt(value));
                                    break;
                                case "moveTo":
                                    item.setMoveTo(Integer.parseInt(value));
                                    break;
                                default:
                                    if (ItemConstants.isPet(id)) {
                                        getPetFoods().put(id, Integer.parseInt(value));
                                    }
                                    SpecStat ss = SpecStat.getSpecStatByName(name);
                                    if (ss != null && value != null) {
                                        item.putSpecStat(ss, Integer.parseInt(value));
                                    } else if (LOG_UNKS) {
                                        System.out.printf("Unhandled spec for id %d, name %s, value %s", id, name, value);
                                    }
                            }
                        }
                    }
                    Node reward = XMLApi.getFirstChildByNameBF(mainNode, "reward");
                    if (reward != null) {
                        for (Node rewardNode : XMLApi.getAllChildren(reward)) {
                            ItemRewardInfo iri = new ItemRewardInfo();
                            for (Node rewardInfoNode : XMLApi.getAllChildren(rewardNode)) {
                                String name = XMLApi.getNamedAttribute(rewardInfoNode, "name");
                                String value = XMLApi.getNamedAttribute(rewardInfoNode, "value");
                                if (value == null) {
                                    continue;
                                }
                                value = value.replace("\n", "").replace("\r", "")
                                        .replace("\\n", "").replace("\\r", "")
                                        .replace("[R8]", "");
                                switch (name) {
                                    case "count":
                                        value = value.replaceAll(" ", "");
                                        iri.setCount(Integer.parseInt(value));
                                        break;
                                    case "item":
                                        value = value.replaceAll(" ", "");
                                        iri.setItemID(Integer.parseInt(value));
                                        break;
                                    case "prob":
                                        value = value.replaceAll(" ", "");
                                        iri.setProb(Double.parseDouble(value));
                                        break;
                                    case "period":
                                        value = value.replaceAll(" ", "");
                                        iri.setPeriod(Integer.parseInt(value));
                                        break;
                                    case "effect":
                                    case "Effect":
                                        iri.setEffect(value);
                                        break;
                                    case "meso":
                                        value = value.replaceAll(" ", "");
                                        iri.setMeso(Integer.parseInt(value));
                                        break;
                                }
                            }
                            item.addItemReward(iri);
                        }
                    }
                    item.setSkillId(getSkillIdByItemId(id));
                    items.put(item.getItemId(), item);
                }
            }
        }
    }

    public static void loadMountItemsFromFile() {
        File file = new File(String.format("%s/mountsFromItem.txt", ServerConstants.RESOURCES_DIR));
        try (Scanner scanner = new Scanner(new FileInputStream(file))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] lineSplit = line.split(" ");
                int itemId = Integer.parseInt(lineSplit[0]);
                int skillId = Integer.parseInt(lineSplit[1]);
                skillIdByItemId.put(itemId, skillId);
            }
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public static int getSkillIdByItemId(int itemId) {
        return skillIdByItemId.getOrDefault(itemId, 0);
    }

    public static Item getDeepCopyByItemInfo(ItemInfo itemInfo) {
        if (itemInfo == null) {
            return null;
        }
        Item res = new Item();
        res.setItemId(itemInfo.getItemId());
        res.setQuantity(1);
        res.setType(ITEM);
        res.setInvType(itemInfo.getInvType());
        res.setCash(itemInfo.isCash());
        res.setExpireOnLogout(itemInfo.isExpireOnLogout());
        return res;
    }

    public static Item getItemDeepCopy(int id) {
        return getItemDeepCopy(id, false);
    }

    public static Item getItemDeepCopy(int id, boolean randomize) {
        if (ItemConstants.isEquip(id)) {
            return getEquipDeepCopyFromID(id, randomize);
        } else if (ItemConstants.isPet(id)) {
            return getPetDeepCopyFromID(id);
        }
        return getDeepCopyByItemInfo(getItemInfoByID(id));
    }

    private static PetItem getPetDeepCopyFromID(int id) {
        return getPetInfoByID(id) == null ? null : getPetInfoByID(id).createPetItem();
    }

    public static ItemInfo getItemInfoByID(int itemID) {
        ItemInfo overlay = OVERLAY_ITEMS.get(itemID, ItemData::loadItemByFile);
        if (overlay != null) {
            return overlay;
        }
        ItemInfo itemInfo = items.get(itemID);
        if (itemInfo != null) {
            return itemInfo;
        }
        File file = new File(String.format("%s/items/%d.dat", ServerConstants.DAT_DIR, itemID));
        if (!file.exists()) {
            return null;
        }
        return loadItemByFile(file);
    }

    public static void loadItemOptionsFromWZ() {
        String wzDir = ServerConstants.WZ_DIR + "/Item.wz";
        String itemOptionDir = String.format("%s/ItemOption.img.xml", wzDir);
        File file = new File(itemOptionDir);
        Document doc = XMLApi.getRoot(file);
        Node node = doc;
        List<Node> nodes = XMLApi.getAllChildren(node);
        for (Node mainNode : XMLApi.getAllChildren(nodes.get(0))) {
            boolean block = false;
            ItemOption io = new ItemOption();
            String nodeName = XMLApi.getNamedAttribute(mainNode, "name");
            io.setId(Integer.parseInt(nodeName));
            Node infoNode = XMLApi.getFirstChildByNameBF(mainNode, "info");
            if (infoNode != null) {
                for (Node infoChild : XMLApi.getAllChildren(infoNode)) {
                    String name = XMLApi.getNamedAttribute(infoChild, "name");
                    String value = XMLApi.getNamedAttribute(infoChild, "value");
                    switch (name) {
                        case "optionType":
                            io.setOptionType(Integer.parseInt(value));
                            break;
                        case "weight":
                            io.setWeight(Integer.parseInt(value));
                            break;
                        case "reqLevel":
                            io.setReqLevel(Integer.parseInt(value));
                            break;
                        case "string":
                            io.setString(value);
                            break;
                        case "optionBlock":
                            if (Integer.parseInt(value) != 0) {
                                // unavailable option, just skip it
                                block = true;
                            }
                    }
                }
            }
            Node levelNode = XMLApi.getFirstChildByNameBF(mainNode, "level");
            if (levelNode != null) {
                for (Node levelChild : XMLApi.getAllChildren(levelNode)) {
                    int level = Integer.parseInt(XMLApi.getNamedAttribute(levelChild, "name"));
                    for (Node levelInfoNode : XMLApi.getAllChildren(levelChild)) {
                        String name = XMLApi.getNamedAttribute(levelInfoNode, "name");
                        String stringValue = XMLApi.getNamedAttribute(levelInfoNode, "value");
                        int value = 0;
                        if (Util.isNumber(stringValue)) {
                            value = Integer.parseInt(stringValue);
                        }
                        switch (name) {
                            case "incSTR":
                                io.addStatValue(level, BaseStat.str, value);
                                break;
                            case "incDEX":
                                io.addStatValue(level, BaseStat.dex, value);
                                break;
                            case "incINT":
                                io.addStatValue(level, BaseStat.inte, value);
                                break;
                            case "incLUK":
                                io.addStatValue(level, BaseStat.luk, value);
                                break;
                            case "incMHP":
                                io.addStatValue(level, BaseStat.mhp, value);
                                break;
                            case "incMMP":
                                io.addStatValue(level, BaseStat.mmp, value);
                                break;
                            case "incACC":
                                io.addStatValue(level, BaseStat.acc, value);
                                break;
                            case "incEVA":
                                io.addStatValue(level, BaseStat.eva, value);
                                break;
                            case "incSpeed":
                                io.addStatValue(level, BaseStat.speed, value);
                                break;
                            case "incJump":
                                io.addStatValue(level, BaseStat.jump, value);
                                break;
                            case "incPAD":
                                io.addStatValue(level, BaseStat.pad, value);
                                break;
                            case "incMAD":
                                io.addStatValue(level, BaseStat.mad, value);
                                break;
                            case "incPDD":
                                io.addStatValue(level, BaseStat.pdd, value);
                                break;
                            case "incMDD":
                                io.addStatValue(level, BaseStat.mdd, value);
                                break;
                            case "incCr":
                                io.addStatValue(level, BaseStat.cr, value);
                                break;
                            case "incPADr":
                                io.addStatValue(level, BaseStat.padR, value);
                                break;
                            case "incMADr":
                                io.addStatValue(level, BaseStat.madR, value);
                                break;
                            case "incSTRr":
                                io.addStatValue(level, BaseStat.strR, value);
                                break;
                            case "incDEXr":
                                io.addStatValue(level, BaseStat.dexR, value);
                                break;
                            case "incINTr":
                                io.addStatValue(level, BaseStat.intR, value);
                                break;
                            case "incLUKr":
                                io.addStatValue(level, BaseStat.lukR, value);
                                break;
                            case "ignoreTargetDEF":
                                io.addStatValue(level, BaseStat.ied, value);
                                break;
                            case "incDAMr":
                                io.addStatValue(level, BaseStat.fd, value);
                                break;
                            case "boss":
                                Node incDamgNode = XMLApi.getFirstChildByNameDF(levelChild, "incDAMr");
                                if (incDamgNode != null) {
                                    value = Integer.parseInt(XMLApi.getNamedAttribute(incDamgNode, "value"));
                                }
                                io.addStatValue(level, BaseStat.bd, value);
                                break;
                            case "incAllskill":
                                io.addStatValue(level, BaseStat.incAllSkill, value);
                                break;
                            case "incMHPr":
                                io.addStatValue(level, BaseStat.mhpR, value);
                                break;
                            case "incMMPr":
                                io.addStatValue(level, BaseStat.mmpR, value);
                                break;
                            case "incACCr":
                                io.addStatValue(level, BaseStat.accR, value);
                                break;
                            case "incEVAr":
                                io.addStatValue(level, BaseStat.evaR, value);
                                break;
                            case "incPDDr":
                                io.addStatValue(level, BaseStat.pddR, value);
                                break;
                            case "incMDDr":
                                io.addStatValue(level, BaseStat.mddR, value);
                                break;
                            case "RecoveryHP":
                                io.addStatValue(level, BaseStat.hpRecovery, value);
                                break;
                            case "RecoveryMP":
                                io.addStatValue(level, BaseStat.mpRecovery, value);
                                break;
                            case "incMaxDamage":
                                io.addStatValue(level, BaseStat.damageOver, value);
                                break;
                            case "incSTRlv":
                                io.addStatValue(level, BaseStat.strLv, value);
                                break;
                            case "incDEXlv":
                                io.addStatValue(level, BaseStat.dexLv, value);
                                break;
                            case "incINTlv":
                                io.addStatValue(level, BaseStat.intLv, value);
                                break;
                            case "incLUKlv":
                                io.addStatValue(level, BaseStat.lukLv, value);
                                break;
                            case "RecoveryUP":
                                io.addStatValue(level, BaseStat.recoveryUp, value);
                                break;
                            case "incTerR":
                                io.addStatValue(level, BaseStat.ter, value);
                                break;
                            case "incAsrR":
                                io.addStatValue(level, BaseStat.asr, value);
                                break;
                            case "incEXPr":
                                io.addStatValue(level, BaseStat.expR, value);
                                break;
                            case "mpconReduce":
                                io.addStatValue(level, BaseStat.mpconReduce, value);
                                break;
                            case "reduceCooltime":
                                io.addStatValue(level, BaseStat.reduceCooltime, value);
                                break;
                            case "incMesoProp":
                                io.addStatValue(level, BaseStat.mesoR, value);
                                break;
                            case "incRewardProp":
                                io.addStatValue(level, BaseStat.dropR, value);
                                break;
                            case "incCriticaldamage":
                            case "incCriticaldamageMin":
                                io.addStatValue(level, BaseStat.crDmg, value);
                                break;
                            case "incCriticaldamageMax":
                                io.addStatValue(level, BaseStat.crDmg, value);
                                break;
                            case "incPADlv":
                                io.addStatValue(level, BaseStat.padLv, value);
                                break;
                            case "incMADlv":
                                io.addStatValue(level, BaseStat.madLv, value);
                                break;
                            case "incMHPlv":
                                io.addStatValue(level, BaseStat.mhpLv, value);
                                break;
                            case "incMMPlv":
                                io.addStatValue(level, BaseStat.mmpLv, value);
                                break;
                            case "prop":
                                io.addMiscValue(level, ItemOption.ItemOptionType.prop, value);
                                break;
                            case "face":
                                io.addMiscValue(level, ItemOption.ItemOptionType.face, value);
                                break;
                            case "time":
                                io.addMiscValue(level, ItemOption.ItemOptionType.time, value);
                                break;
                            case "HP":
                                io.addMiscValue(level, ItemOption.ItemOptionType.hpRecoveryOnHit, value);
                                break;
                            case "MP":
                                io.addMiscValue(level, ItemOption.ItemOptionType.mpRecoveryOnHit, value);
                                break;
                            case "attackType":
                                io.addMiscValue(level, ItemOption.ItemOptionType.attackType, value);
                                break;
                            case "level":
                                io.addMiscValue(level, ItemOption.ItemOptionType.level, value);
                                break;
                            case "ignoreDAM":
                                io.addMiscValue(level, ItemOption.ItemOptionType.ignoreDam, value);
                                break;
                        }
                    }

                }
            }
            if (io.getWeight() == 0) {
                io.setWeight(1);
            }
            if (!block) {
                itemOptions.put(io.getId(), io);
            }
        }
    }

    public static List<ItemOption> getFilteredItemOptions() {
        return filteredItemOptions;
    }

    public static ItemOption getItemOptionById(int id) {
        return itemOptions.get(id);
    }

    public static void saveItemOptions(String dir) {
        File file = new File(String.format("%s/itemOptions.dat", dir));
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            dos.writeInt(itemOptions.size());
            for (ItemOption io : itemOptions.values()) {
                dos.writeInt(io.getId());
                dos.writeInt(io.getOptionType());
                dos.writeInt(io.getWeight());
                dos.writeInt(io.getReqLevel());
                dos.writeUTF(io.getString());
                dos.writeShort(io.getStatValuesPerLevel().size());
                for (Map.Entry<Integer, Map<BaseStat, Double>> entry1 : io.getStatValuesPerLevel().entrySet()) {
                    dos.writeInt(entry1.getKey());
                    dos.writeShort(entry1.getValue().size());
                    for (Map.Entry<BaseStat, Double> entry2 : entry1.getValue().entrySet()) {
                        dos.writeInt(entry2.getKey().ordinal());
                        dos.writeDouble(entry2.getValue());
                    }
                }
                dos.writeShort(io.getMiscValuesPerLevel().size());
                for (Map.Entry<Integer, Map<ItemOption.ItemOptionType, Integer>> entry1 : io.getMiscValuesPerLevel().entrySet()) {
                    dos.writeInt(entry1.getKey());
                    dos.writeShort(entry1.getValue().size());
                    for (Map.Entry<ItemOption.ItemOptionType, Integer> entry2 : entry1.getValue().entrySet()) {
                        dos.writeInt(entry2.getKey().ordinal());
                        dos.writeInt(entry2.getValue());
                    }
                }
            }
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    @Loader(varName = "itemOptions")
    public static void loadItemOptions(File file, boolean exists) {
        // 282 profile: data/dat282/itemOptions.dat (the whole 282 ItemOption.img) replaces the 265 table.
        File overlay = net.swordie.ms.loaders.overlay.DataOverlays.current().overlayDat("itemOptions.dat").orElse(null);
        if (overlay != null) {
            file = overlay;
            exists = true;
        }
        if (!exists) {
            loadItemOptionsFromWZ();
            saveItemOptions(ServerConstants.DAT_DIR);
        } else {
            try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
                int size = dis.readInt();
                for (int i = 0; i < size; i++) {
                    ItemOption io = new ItemOption();
                    io.setId(dis.readInt());
                    io.setOptionType(dis.readInt());
                    io.setWeight(dis.readInt());
                    io.setReqLevel(dis.readInt());
                    io.setString(dis.readUTF());
                    short size2 = dis.readShort();
                    for (int j = 0; j < size2; j++) {
                        int level = dis.readInt();
                        short size3 = dis.readShort();
                        for (int k = 0; k < size3; k++) {
                            io.addStatValue(level, BaseStat.values()[dis.readInt()], dis.readDouble());
                        }
                    }
                    size2 = dis.readShort();
                    for (int j = 0; j < size2; j++) {
                        int level = dis.readInt();
                        short size3 = dis.readShort();
                        for (int k = 0; k < size3; k++) {
                            io.addMiscValue(level, ItemOption.ItemOptionType.values()[dis.readInt()], dis.readInt());
                        }
                    }
                    itemOptions.put(io.getId(), io);
                }
            } catch (IOException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
            createFilteredOptions();
        }
    }

    private static void createFilteredOptions() {
        Collection<ItemOption> data = itemOptions.values();
        filteredItemOptions = data.stream().filter(io ->
                io.getId() % 1000 != 14 //Old Magic Def, now regular Def ; removed to not have duplicates
                        && io.getId() != 14 //Old Magic Def, now regular Def ; removed to not have duplicates
                        && io.getId() % 1000 != 54 //Old Magic Def%, now regular Def% ; removed to not have duplicates
                        && (io.getId() % 1000 != 7 || io.getId() == 41007) //Old Accuracy, now Max HP ; removed to not have duplicates (41007 = Decent Speed Infusion For Gloves)
                        && io.getId() != 7 //Old Accuracy, now Max HP ; removed to not have duplicates
                        && (io.getId() % 1000 != 47 || io.getId() == 12047) //Old Accuracy%, now Max HP% ; removed to not have duplicates (12047 = Bonus Weapons STR% Rare)
                        && io.getId() % 1000 != 8 //Old Avoid, now Max MP ; removed to not have duplicates
                        && io.getId() != 8 //Old Accuracy, now Max HP ; removed to not have duplicates
                        && (io.getId() % 1000 != 48 || io.getId() == 12048) //Old Avoid%, now Max MP% ; removed to not have duplicates (12048 = Bonus Weapons DEX% Rare)
                        && io.getId() != 40081 //Flat AllStat
                        && io.getId() % 1000 != 202 //Additional %HP Recovery ; removed to not have duplicates
                        && io.getId() % 1000 != 207 //Additional %MP Recovery ; removed to not have duplicates
                        && io.getId() != 10222 //Secondary Rare-Prime 20% Poison Chance - WeaponsEmblemSecondary
                        && io.getId() != 10227 //Secondary Rare-Prime 10% Stun Chance - WeaponsEmblemSecondary
                        && io.getId() != 10232 //Secondary Rare-Prime 20% Slow Chance - WeaponsEmblemSecondary
                        && io.getId() != 10237 //Secondary Rare-Prime 20% Blind Chance - WeaponsEmblemSecondary
                        && io.getId() != 10242 //Secondary Rare-Prime 10% Freeze Chance - WeaponsEmblemSecondary
                        && io.getId() != 10247 //Secondary Rare-Prime 10% Seal Chance - WeaponsEmblemSecondary
                        && io.getId() % 1000 != 801 //Old Damage Cap Increase, now AllStat/Ignore Enemy Defense/AllStat%/Abnormal Status Res
                        && io.getId() % 1000 != 802 //Old Damage Cap Increase, now AllStat%/ElementalResist
                        && io.getId() % 10000 != 2056 //Old CritRate/Magic Def%, now AllStat%/ElementalResist ; removed to not have duplicates
                        && io.getId() != 32661 //EXP Obtained
                        && io.getId() != 42661 //EXP Obtained
                        && io.getId() != 20396 //Duplicate "invincible for additional seconds"
                        && io.getId() != 40057 //Glove's Crit Damage Duplicate
                        && io.getId() != 42061 //Bonus - Glove's Crit Damage Duplicate
                        && io.getId() != 42062 //Bonus - Armor's 1% Crit Damage Duplicate
                        && io.getId() != 22056 //Bonus - Non-Weapon Crit Rate%
                        && io.getId() != 32052 //Bonus - Non-Weapon Attack%
                        && io.getId() != 32054 //Bonus - Non-Weapon MagicAttack%
                        && io.getId() != 32058 //Bonus - Non-Weapon Crit Rate%
                        && io.getId() != 32071 //Bonus - Non-Weapon Damage%
                        && io.getId() != 42052 //Bonus - Non-Weapon Attack%
                        && io.getId() != 42054 //Bonus - Non-Weapon MagicAttack%
                        && io.getId() != 42058 //Bonus - Non-Weapon Crit Rate%
                        && io.getId() != 42071 //Bonus - Non-Weapon Damage%
                        && !(io.getId() > 14 && io.getId() < 900) //Rare Junk Filter
                        && !(io.getId() > 20000 && io.getId() < 20014) //No Flat Stats Above Rare
                        && !(io.getId() > 30000 && io.getId() < 30014) //No Flat Stats Above Rare
                        && !(io.getId() > 40000 && io.getId() < 40014) //No Flat Stats Above Rare
        ).collect(Collectors.toList());

    }

    private static void saveStartingItems(String dir) {
        File file = new File(String.format("%s/startingItems.dat", dir));
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            dos.writeInt(startingItems.size());
            for (int i : startingItems) {
                dos.writeInt(i);
            }
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    private static void loadStartingItemsFromWZ() {
        String wzDir = ServerConstants.WZ_DIR + "/Etc.wz";
        String itemOptionDir = String.format("%s/MakeCharInfo.img.xml", wzDir);
        File file = new File(itemOptionDir);
        startingItems.addAll(searchForStartingItems(XMLApi.getRoot(file)));
    }

    private static Set<Integer> searchForStartingItems(Node n) {
        List<Node> subNodes = XMLApi.getAllChildren(n);
        for (Node node : subNodes) {
            String name = XMLApi.getNamedAttribute(node, "name");
            String value = XMLApi.getNamedAttribute(node, "value");
            if (Util.isNumber(name) && value != null && Util.isNumber(value)) {
                startingItems.add(Integer.parseInt(value));
            }
            startingItems.addAll(searchForStartingItems(node));
        }
        return startingItems;
    }


    @Loader(varName = "startingItems")
    public static void loadStartingItems(File file, boolean exists) {
        if (!exists) {
            loadStartingItemsFromWZ();
            saveStartingItems(ServerConstants.DAT_DIR);
        } else {
            try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
                int size = dis.readInt();
                for (int i = 0; i < size; i++) {
                    startingItems.add(dis.readInt());
                }
            } catch (IOException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
    }

    @SuppressWarnings("unused") // Reflection
    public static void generateDatFiles() {
        System.out.println("Started generating item data.");
        long start = System.currentTimeMillis();
        loadEquipsFromWz();
        loadMountItemsFromFile();
        loadItemsFromWZ();
        loadPetsFromWZ();
        loadItemOptionsFromWZ();
        QuestData.linkItemData();
        saveEquips(ServerConstants.DAT_DIR + "/equips");
        saveItems(ServerConstants.DAT_DIR + "/items");
        savePets(ServerConstants.DAT_DIR + "/pets");
        saveItemOptions(ServerConstants.DAT_DIR);
        System.out.printf("Completed generating item data in %dms%n", System.currentTimeMillis() - start);
    }

    public static void main(String[] args) {
        //DatabaseManager.init();
        generateDatFiles();
    }

    public static void addItemInfo(ItemInfo ii) {
        items.put(ii.getItemId(), ii);
    }

    public static Int2IntMap getPetFoods() {
        return petFoods;
    }

    public static Int2IntMap getPetCommands() {
        return petCommands;
    }

    public static void clear() {
        equips.clear();
        items.clear();
        pets.clear();
        petFoods.clear();
        petCommands.clear();
        itemOptions.clear();
        filteredItemOptions.clear();
        skillIdByItemId.clear();
        startingItems.clear();
        OVERLAY_EQUIPS.clear();
        OVERLAY_ITEMS.clear();
        OVERLAY_PETS.clear();
    }

    public static void load() {
        loadEquipsFromWz();
        loadMountItemsFromFile();
        loadItemsFromWZ();
        loadPetsFromWZ();
        loadItemOptionsFromWZ();
    }

    public static boolean isStartingItems(int[] items) {
        for (int item : items) {
            if (!isStartingItem(item)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isStartingItems(int[] items, short jobId) {
        for (int item : items) {
            // TMS282 MakeCharInfo/12200/{male,female}/4/0 specifies this weapon.
            // It is absent from the retained 265 cache and belongs only to Saitama's family.
            if (item == 1482110) {
                if (jobId != 12006 && jobId != 12200) {
                    return false;
                }
            } else if (!isStartingItem(item)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isStartingItem(int item) {
        return startingItems.contains(item);
    }

    public static List<Integer> getValidFaces() {
        return validFaces;
    }

    public static List<Integer> getValidHairs() {
        return validHairs;
    }

    public static boolean isValidFaceHair(int lookId, boolean isFace) {
        if (isFace) {
            return getValidFaces().contains(lookId);
        } else {
            return getValidHairs().contains(lookId);
        }
    }

    public static void loadFacesAndHairs() {
        System.out.println("[WZ Data] Started loading valid hairs and faces from wz.");
        long start = System.currentTimeMillis();
        String wzDir = ServerConstants.WZ_DIR + "/Character.wz";
        String[] subMaps = new String[]{"Face", "Hair"};
        for (String subMap : subMaps) {
            File subDir = new File(String.format("%s/%s", wzDir, subMap));
            File[] files = subDir.listFiles();
            assert files != null;
            for (File file : files) {
                if (file.isDirectory()) continue;
                Node node = XMLApi.getRoot(file);
                List<Node> nodes = XMLApi.getAllChildren(node);
                for (Node mainNode : nodes) {
                    Map<String, String> attributes = XMLApi.getAttributes(mainNode);
                    String mainName = attributes.get("name");
                    if (mainName != null) {
                        int itemId = Integer.parseInt(attributes.get("name").replace(".img", ""));
                        for (Node n : XMLApi.getAllChildren(Objects.requireNonNull(XMLApi.getFirstChildByNameBF(mainNode, "default")))) {
                            for (Node n2 : XMLApi.getAllChildren(n)) {
                                String name = XMLApi.getNamedAttribute(n2, "name");
                                if (!name.equalsIgnoreCase("_outlink")) {
                                    if (ItemConstants.MIN_FACE <= itemId && itemId < ItemConstants.MAX_FACE
                                    || ItemConstants.MIN_FACE_2 <= itemId && itemId < ItemConstants.MAX_FACE_2) {
                                        getValidFaces().add(itemId);
                                    } else {
                                        getValidHairs().add(itemId);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.printf("[WZ Data] Loaded valid hairs and faces in %dms%n", System.currentTimeMillis() - start);
    }
}
