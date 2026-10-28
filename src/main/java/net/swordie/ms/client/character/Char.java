package net.swordie.ms.client.character;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.swordie.ms.Server;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.*;
import net.swordie.ms.client.character.achievement.AchievementData;
import net.swordie.ms.client.character.achievement.AchievementHandler;
import net.swordie.ms.client.character.achievement.AchievementRank;
import net.swordie.ms.client.character.avatar.AvatarData;
import net.swordie.ms.client.character.avatar.AvatarLook;
import net.swordie.ms.client.character.cards.MonsterBookInfo;
import net.swordie.ms.client.character.damage.DamageCalc;
import net.swordie.ms.client.character.damage.DamageSkinSaveData;
import net.swordie.ms.client.character.hexa.HexaSkill;
import net.swordie.ms.client.character.hexa.HexaStat;
import net.swordie.ms.client.character.info.*;
import net.swordie.ms.client.character.items.*;
import net.swordie.ms.client.character.keys.FuncKeyMap;
import net.swordie.ms.client.character.monsterbattle.MonsterBattleLadder;
import net.swordie.ms.client.character.monsterbattle.MonsterBattleMobInfo;
import net.swordie.ms.client.character.monsterbattle.MonsterBattleRankInfo;
import net.swordie.ms.client.character.potential.CharacterPotential;
import net.swordie.ms.client.character.potential.CharacterPotentialMan;
import net.swordie.ms.client.character.potential.CharacterPotentialValueHolder;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.client.character.quest.progress.*;
import net.swordie.ms.client.character.quest.requirement.QuestStartCompletionRequirement;
import net.swordie.ms.client.character.quest.requirement.QuestStartJobRequirement;
import net.swordie.ms.client.character.quest.requirement.QuestStartMinStatRequirement;
import net.swordie.ms.client.character.quest.requirement.QuestStartRequirement;
import net.swordie.ms.client.character.quest.reward.QuestBuffItemReward;
import net.swordie.ms.client.character.quest.reward.QuestExpReward;
import net.swordie.ms.client.character.quest.reward.QuestItemReward;
import net.swordie.ms.client.character.quest.reward.QuestReward;
import net.swordie.ms.client.character.reward.RewardInfo;
import net.swordie.ms.client.character.reward.RewardResult;
import net.swordie.ms.client.character.reward.RewardSystem;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.character.skills.info.ForceAtomInfo;
import net.swordie.ms.client.character.skills.info.SkillAlarmInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.jupiterthunder.JupiterThunder;
import net.swordie.ms.client.character.skills.matrix.MatrixCore;
import net.swordie.ms.client.character.skills.matrix.MatrixSlot;
import net.swordie.ms.client.character.skills.matrix.SpecialNodeSkill;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.character.union.Union;
import net.swordie.ms.client.character.union.UnionArtifact;
import net.swordie.ms.client.character.union.UnionBoard;
import net.swordie.ms.client.character.union.UnionMember;
import net.swordie.ms.client.daily.DailyCoin;
import net.swordie.ms.client.daily.DailyGift;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.client.jobs.JobManager;
import net.swordie.ms.client.jobs.Zero;
import net.swordie.ms.client.jobs.adventurer.Kinesis;
import net.swordie.ms.client.jobs.adventurer.archer.BowMaster;
import net.swordie.ms.client.jobs.adventurer.archer.Marksman;
import net.swordie.ms.client.jobs.adventurer.magician.Bishop;
import net.swordie.ms.client.jobs.adventurer.magician.FirePoison;
import net.swordie.ms.client.jobs.adventurer.magician.IceLightning;
import net.swordie.ms.client.jobs.adventurer.pirate.Cannoneer;
import net.swordie.ms.client.jobs.adventurer.thief.DualBlade;
import net.swordie.ms.client.jobs.adventurer.thief.NightLord;
import net.swordie.ms.client.jobs.adventurer.warrior.Hero;
import net.swordie.ms.client.jobs.adventurer.warrior.Paladin;
import net.swordie.ms.client.jobs.cygnus.NightWalker;
import net.swordie.ms.client.jobs.cygnus.WindArcher;
import net.swordie.ms.client.jobs.legend.Evan;
import net.swordie.ms.client.jobs.legend.Phantom;
import net.swordie.ms.client.jobs.legend.Shade;
import net.swordie.ms.client.jobs.resistance.Blaster;
import net.swordie.ms.client.jobs.resistance.WildHunter;
import net.swordie.ms.client.jobs.resistance.WildHunterInfo;
import net.swordie.ms.client.jobs.resistance.demon.DemonAvenger;
import net.swordie.ms.client.jobs.resistance.demon.DemonSlayer;
import net.swordie.ms.client.jobs.sengoku.Kanna;
import net.swordie.ms.client.social.Alliance.Alliance;
import net.swordie.ms.client.social.Alliance.AllianceResult;
import net.swordie.ms.client.social.Friend.Friend;
import net.swordie.ms.client.social.Friend.FriendRecord;
import net.swordie.ms.client.social.Friend.FriendResult;
import net.swordie.ms.client.social.Friend.FriendshipRingRecord;
import net.swordie.ms.client.social.Guild.*;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Party.PartyBoss;
import net.swordie.ms.client.social.Party.PartyMember;
import net.swordie.ms.client.social.Party.PartyResult;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.*;
import net.swordie.ms.enums.*;
import net.swordie.ms.enums.reward.RewardItemType;
import net.swordie.ms.enums.reward.RewardSystemType;
import net.swordie.ms.enums.social.Friend.FriendFlag;
import net.swordie.ms.enums.social.Party.PartyQuestType;
import net.swordie.ms.handlers.ClientSocket;
import net.swordie.ms.handlers.GlobalTimerManager;
import net.swordie.ms.handlers.Timer;
import net.swordie.ms.handlers.PsychicLock;
import net.swordie.ms.handlers.ui.MatrixHandler;
import net.swordie.ms.life.*;
import net.swordie.ms.life.Merchant.EmployeeTrunk;
import net.swordie.ms.life.Merchant.Merchant;
import net.swordie.ms.life.Merchant.MerchantItem;
import net.swordie.ms.life.drop.Drop;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.skill.MobSkillID;
import net.swordie.ms.life.pet.Pet;
import net.swordie.ms.loaders.Etc.Achievement.AchievementInfo;
import net.swordie.ms.loaders.Etc.Achievement.AchievementInfoData;
import net.swordie.ms.loaders.Etc.Artifact.ArtifactData;
import net.swordie.ms.loaders.Etc.Artifact.ArtifactInfo;
import net.swordie.ms.loaders.Etc.EtcData;
import net.swordie.ms.loaders.*;
import net.swordie.ms.loaders.Etc.HexaCore.HexaCore;
import net.swordie.ms.loaders.containerclasses.AndroidInfo;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.loaders.containerclasses.QuestInfo;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.world.boss.BossHelper;
import net.swordie.ms.util.*;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.Channel;
import net.swordie.ms.world.World;
import net.swordie.ms.world.event.*;
import net.swordie.ms.world.field.*;
import net.swordie.ms.world.field.fieldeffect.FieldEffect;
import net.swordie.ms.world.field.fieldeffect.GreyFieldType;
import net.swordie.ms.world.gach.GachaponManager;
import net.swordie.ms.world.partyquest.*;
import net.swordie.ms.world.shop.NpcShopDlg;
import net.swordie.ms.world.shop.NpcShopItem;

import java.awt.*;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.client.jobs.adventurer.pirate.Corsair.*;
import static net.swordie.ms.enums.ChatType.*;
import static net.swordie.ms.enums.InvType.*;
import static net.swordie.ms.enums.InventoryOperation.*;
import static net.swordie.ms.enums.QuestStatus.*;
import static net.swordie.ms.world.field.FieldInstanceType.CHANNEL;

public class Char {
    public static long creationTime = 134116992000000000L; // CREATION TIME 01-01-2026 00:00:00 (UTC)
    private static final int PERMANENT_POWER_ELIXIR_ITEM_ID = 2001505;

    private Client client;
    private int rewardPoints;
    private int id;
    private int accId;
    private int orderId;
    private Map<Integer, Quest> quests = new ConcurrentHashMap<>(); //QuestID, Quest
    private Inventory equippedInventory;
    private Inventory equipInventory;
    private Inventory consumeInventory;
    private Inventory etcInventory;
    private Inventory installInventory;
    private Inventory cashInventory;
    private Inventory decorationInventory;

    private AvatarData avatarData;

    private Map<Integer, FuncKeyMap> funcKeyMaps = new LinkedHashMap<>();

    private Int2ObjectMap<Skill> skills;
    private List<LinkedSkill> linkedSkill = new ArrayList<>();
    private Int2LongMap skillCoolTimes;

    private Set<MatrixCore> matrixCore = new HashSet<>();
    private volatile net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.Context tms282VMatrix;
    public net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.Context getTms282VMatrix() { return tms282VMatrix; }
    public void setTms282VMatrix(net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.Context value) { tms282VMatrix=value; }
    private Set<MatrixSlot> matrixSlot = new HashSet<>();

    private Map<Integer, HexaStat> hexaStats = new HashMap<>();
    private Set<HexaSkill> hexaSkills = new HashSet<>();

    private Set<CharacterPotential> potentials = new HashSet<>();

    private Set<Friend> friends;
    private Set<Familiar> familiars;
    private List<Macro> macros = new ArrayList<>();
    private int guildID = 0;
    private Guild guild;
    private int partyID = 0;
    private Party party;
    private MonsterBookInfo monsterBookInfo;
    private Set<StolenSkill> stolenSkills;
    private Set<ChosenSkill> chosenSkills;
    private int[] hyperrockfields = new int[13];
    private byte monsterParkCount;
    private int previousFieldID;
    private int previousPortalID; // not super important so we wont save to db
    private Map<Long, Integer> itemBoughtAmounts;
    private Set<MedalAchievementInfo> medalAchievementInfo;
    private int equippedMedalItem;
    private Set<DamageSkinSaveData> damageSkins;
    private Set<PartyBoss> partyboss;
    private Set<Core> cores;
    private CommerceRecord commerceRecord;
    private CharacterPotentialMan potentialMan;
    private Ranking ranking;
    private int combatOrders;
    private List<ItemPot> itemPots;
    private List<Pet> pets;
    private boolean petLoot;
    private List<FriendRecord> friendRecords;
    private List<ExpConsumeItem> expConsumeItems;
    private List<MonsterBattleMobInfo> monsterBattleMobInfos;
    private MonsterBattleLadder monsterBattleLadder;
    private MonsterBattleRankInfo monsterBattleRankInfo;
    private Position position;
    private Position oldPosition;
    private Field field;
    private transient net.swordie.ms.client.character.tms282.Tms282RuneRuntime.State runeChallengeState;
    private byte moveAction;

    private ScheduledFuture<?> updateTimer;
    private TemporaryStatManager temporaryStatManager;
    private Map<ForcedStat, Object> forcedStats;

    private GachaponManager gachaponManager;
    private Job jobHandler;
    private MarriageRecord marriageRecord;
    private WildHunterInfo wildHunterInfo;
    private ZeroInfo zeroInfo;
    private DamageSkinSaveData activeDamageSkin;
    private DamageSkinSaveData premiumDamageSkin;
    private DamageSkinSaveData newDamageSkin;
    // multiplayer-0930: 282 sends CP01F4 PARTY_INVITABLE_SET only when its settings object exists (u8 1 captured);
    // the flag is not stored in the DB, so a default of false refused every invitation.
    private boolean partyInvitable = true;
    private ScriptManagerImpl scriptManagerImpl = new ScriptManagerImpl(this);
    private int driverID;
    private int passengerID;
    private int chocoCount;
    private int activeEffectItemID;
    private int monkeyEffectItemID;
    private int completedSetItemID;
    private short fieldSeatID;
    private PortableChair chair;
    private short foothold;
    private int tamingMobLevel = 1;
    private int tamingMobExp;
    private int tamingMobFatigue;
    private MiniRoom miniRoom;
    private String ADBoardRemoteMsg;
    private boolean inCouple;
    private CoupleRecord couple;
    private FriendshipRingRecord friendshipRingRecord;
    private int evanDragonGlide;
    private int kaiserMorphRotateHueExtern;
    private int kaiserMorphPrimiumBlack;
    private int kaiserMorphRotateHueInnner;
    private int makingMeisterSkillEff;
    private FarmUserInfo farmUserInfo;
    private int customizeEffect;
    private String customizeEffectMsg;
    private byte soulEffect;
    private FreezeHotEventInfo freezeHotEventInfo;
    private int eventBestFriendAID;
    private int mesoChairCount;
    private boolean beastFormWingOn;
    private int activeNickItemID;
    private int activeNickSkillID;
    private boolean online;
    private FieldInstanceType fieldInstanceType;
    private int bulletIDForAttack;
    private NpcShopDlg shop;
    private List<NpcShopItem> repurchaseItems = new ArrayList<>(10);
    private User user;
    private Account account;
    private Client chatClient;
    private DamageCalc damageCalc;
    private int comboCounter;
    private int deathCount = -1;
    private boolean[] vhDeathCount = new boolean[5];
    private long runeStoneCooldown;
    private MemorialCubeInfo memorialCubeInfo;
    private Familiar activeFamiliar;
    private final Map<BaseStat, Long> baseStats = new HashMap<>();
    private TownPortal townPortal;
    private TradeRoom tradeRoom;
    private boolean battleRecordOn;
    private Map<Integer, Integer> currentDirectionNode;
    private String lieDetectorAnswer = "";
    private long lastLieDetector = 0;
    private boolean tutor = false;
    private boolean isPracticeMode = false;
    private int transferField = 0;
    private int transferFieldReq = 0;
    private String blessingOfFairy = null;
    private String blessingOfEmpress = null;
    private Map<Integer, Integer> hyperPsdSkillsCooltimeR = new HashMap<>();
    private boolean isInvincible = false;
    private List<Integer> quickslotKeys;
    private Android android;
    private FoxMan foxman;
    private SkillPet skillpet;
    private Map<Integer, PsychicArea> psychicAreas;
    private Map<Integer, PsychicLock> psychicLocks;
    private Map<Integer, PsychicLockBall> psychicLockBalls;
    private Instance instance;
    private Merchant merchant;
    private Merchant visitingmerchant;
    private EventNameTag eventNameTag;
    private List<SkillAlarmInfo> skillAlarms = new ArrayList<>();
    private List<SequenceSkill> sequenceSkills = new ArrayList<>();
    private List<SequenceBuff> sequenceBuffs = new ArrayList<>();
    private Int2ObjectMap<Int2IntMap> buffFavorites = new Int2ObjectOpenHashMap<>();
    private SpecialNodeSkill specialNodeSkill = new SpecialNodeSkill(this);
    private int ascentSkillStack = 0;
    private MobZoneDebuff mobZoneDebuff = null;
    private Tuple<Long, Byte> lastNormalAttack;
    private long dojoStartTime = 0;
    private long dojoCoolTime = 0;
    private boolean hide = false;
    private long lastReviveTime = 0L;
    private List<CharacterPotentialValueHolder> characterPotentialValueHolders = new LinkedList<>();
    private List<String> lastCharReportName = new ArrayList<>();
    private int reportCount;
    private long lastReportTime;
    private int oxQuizCount;
    private RewardSystem rewardSystem;
    private List<RewardInfo> rewardInfos = new ArrayList<>();
    private Tuple<Integer, Integer> recipe = new Tuple<>(0, 0);
    private DefenseEvent defenseEvent = null;
    private DefenseEventMember defenseEventMember = null;
    private DefenseTowerWave defenseTowerWave = null;
    private BountyHunting bountyHunting = null;
    private FrittoEagle frittoEagle = null;
    private FrittoEgg frittoEgg = null;
    private FrittoDancing frittoDancing = null;
    private DreamBreaker dreamBreaker = null;
    private SpiritSavior spiritSavior = null;
    private EvolutionSystem evolutionSystem = null;
    private Map<BaseStat, Integer> diceBaseStats = new HashMap<>();
    private long lastCheckAndRemoveExpiredItems = 0;
    private Map<Integer, String> visitorStageResult = new HashMap<>();
    private boolean isTurnOffBackground = false;
    private Equip violetEquip = null;
    private Equip violetZeroEquip = null;

    private List<BuffData> buffDataList = new ArrayList<>();
    private BeautySalon beautySalon;
    private List<PetVac> petVacs;
    private boolean isSellingPetVac = false;
    private List<Integer> filterItems = new ArrayList<>();
    private MonsterCarnivalRanking monsterCarnivalRanking;

    private DressUpInfo dressUpInfo;

    private IdleHunting idleHunting = null;
    private boolean isStartedHunting = false;

    private long onlineTime;
    private long onlineTimeMilli;
    private long lastOnlineTickMs;
    private int onlineDay;
    private long luxeSaunaMilli;

    private int forceAtomKeyCounter = 1;
    private Int2ObjectMap<ForceAtom> forceAtoms;
    private int secondAtomKeyCounter = 0;
    private Int2ObjectMap<SecondAtom> secondAtoms;
    private int jupiterThunderCounter = 0;
    private Int2ObjectMap<JupiterThunder> jupiterThunders;

    private ScheduledFuture<?> keyDownTimer;

    private int lastBossTemplateID = 0;

    private int lucidMode;

    private int moonGauge = 0;
    private int clearSpiderWeb = 0;
    private boolean isBot = false;

    public long blockHyperUpgradeDisplayUntil = 0;

    private List<HyperStat> hyperStats = new ArrayList<>();
    private Map<Integer, Integer> soulCollection = new HashMap<>();
    private ConcurrentHashMap<Integer, Tuple<ExpIncreaseInfo, Long>> expPerMob = new ConcurrentHashMap<>();

    private PartyQuestManager partyQuestManager = new PartyQuestManager();
    private int burningFieldLevel;
    private ScheduledFuture<?> burningFieldTimer;

    private DailyCoin dailyCoin;
    private boolean isSubZeroHunt;

    private ScheduledFuture<?> comboKillResetTimer;
    private ScheduledFuture<?> timeLimitTimer;
    private ScheduledFuture<?> dropFatigueTimer;
    private ScheduledFuture<?> runeRecoveryTimer;
    private ScheduledFuture<?> willGaugeTimer;
    private ScheduledFuture<?> starDustTimer;
    private ScheduledFuture<?> bonusTimer;
    private ScheduledFuture<?> scoreTimer;
    private ScheduledFuture<?> startEventTimer;
    private ScheduledFuture<?> endEventTimer;
    private ScheduledFuture<?> unionRaidDmg;
    private ScheduledFuture<?> deathPenaltyTimer;
    /** 282: the last death was covered by a safety charm, so revive() adds no death debuff (quest 100716). */
    private boolean deathProtectedByCharm;

    private final Int2LongOpenHashMap lastAttackEffectAt = new Int2LongOpenHashMap();
    private long deletionStartTime;

    public ReturnEffectInfo returnEffectInfo = new ReturnEffectInfo(this, null);
    public ExtraTMSSystem extraTMSSystem = new ExtraTMSSystem(this, Util.getRandom(1, 4095));

    public static Char getCharDataByName(String name) {
        Char chr = null;
        int characterId = -1;

        String statsQuery = "SELECT characterid FROM characterstats WHERE name = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(statsQuery)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    characterId = rs.getInt("characterid");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting character ID by name: " + e);
            return null;
        }

        if (characterId == -1) {
            return null;
        }
        String query = "SELECT * FROM characters WHERE id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    chr = new Char();
                    chr.setId(characterId);
                    chr.setAccId(rs.getInt("accid"));
                    chr.setOrderId(rs.getInt("orderid"));
                    chr.setGuildID(rs.getInt("guild"));
                    chr.setDeletionStartTime(rs.getLong("deletionStartTime"));
                    int avatarDataID = rs.getInt("avatardata");
                    if (avatarDataID != 0) {
                        chr.setAvatarData(AvatarData.getAvatarDataFromSQLByID(avatarDataID));
                    }
                    chr.setFriends(Friend.getFriendsFromSQLByOwnerID(characterId));
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return chr;
    }

    public static Char getCharDeleteByID(int charID) {
        Char chr = null;
        String query = "SELECT * FROM characters WHERE id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int monsterBookID = rs.getInt("monsterbook");
                    chr = new Char();
                    chr.setId(charID);
                    chr.setAccId(rs.getInt("accid"));
                    chr.setPartyID(rs.getInt("party"));
                    chr.setOrderId(rs.getInt("orderid"));
                    chr.setGuildID(rs.getInt("guild"));
                    chr.setDeletionStartTime(rs.getLong("deletionStartTime"));
                    int avatarDataID = rs.getInt("avatardata");
                    if (avatarDataID != 0) {
                        chr.setAvatarData(AvatarData.getAvatarDataFromSQLByID(avatarDataID));
                    }
                    if (monsterBookID != 0) {
                        chr.setMonsterBookInfo(MonsterBookInfo.getMonsterBookInfoFromSQLByMonsterBookID(monsterBookID));
                    }
                    chr.setMacros(Macro.getMarcosFromSQLByCharID(charID));
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return chr;
    }

    public static Char getCharDataByID(int charID) {
        Char chr = null;
        String query = "SELECT * FROM characters WHERE id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    chr = new Char();
                    chr.setId(charID);
                    int accID = rs.getInt("accid");
                    chr.setAccId(accID);
                    chr.setPartyID(rs.getInt("party"));
                    Account account = Account.getAccountFromSQLByAccountID(accID);

                    chr.setOrderId(rs.getInt("orderid"));
                    chr.setGuildID(rs.getInt("guild"));
                    chr.setDeletionStartTime(rs.getLong("deletionStartTime"));
                    int avatarDataID = rs.getInt("avatardata");
                    if (avatarDataID != 0) {
                        chr.setAvatarData(AvatarData.getAvatarDataFromSQLByID(avatarDataID));
                    }
                    chr.setFriends(Friend.getFriendsFromSQLByOwnerID(charID));
                    chr.setMatrixSlot(MatrixSlot.getMatrixSlotsFromSQLByCharID(charID));
                    chr.setMatrixCore(MatrixCore.getMatrixCoresFromSQLByCharID(charID));
                    chr.setSkills(Skill.getSkillsFromSQLByCharID(charID));
                    account.setCurrentChr(chr);
                    chr.setAccount(account);
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return chr;
    }

    public static Char getCharAvatarDataByID(int charID) {
        Char chr = null;
        String query = "SELECT * FROM characters WHERE id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    chr = new Char();
                    chr.setId(charID);
                    chr.setAccId(rs.getInt("accid"));
                    int avatarDataID = rs.getInt("avatardata");
                    if (avatarDataID != 0) {
                        AvatarData avatarData = AvatarData.getAvatarDataFromSQLByID(avatarDataID);
                        chr.setAvatarData(avatarData);
                        chr.setFieldID((int) avatarData.getCharacterStat().getPosMap());
                    }
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return chr;
    }

    public static Char getCharSkillsByID(int charID) {
        Char chr = null;
        String query = "SELECT * FROM characters WHERE id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    chr = new Char();
                    chr.setId(charID);
                    chr.setSkills(Skill.getSkillsFromSQLByCharID(charID));
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return chr;
    }

    public static Char getCharByGuildID(int guildID) {
        Char chr = null;
        String query = "SELECT * FROM characters WHERE guild = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, guildID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    chr = new Char();
                    chr.setId(rs.getInt("id"));
                    chr.setGuildID(guildID);
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return chr;
    }

    public static Set<Char> loadAvatarData(int accountID) {
        Set<Char> charSet = new HashSet<>();
        String query = "SELECT * FROM characters WHERE accid = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, accountID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int avatarDataID = rs.getInt("avatardata");
                    Char chr = new Char();
                    chr.setId(id);
                    chr.setAccId(rs.getInt("accid"));
                    chr.setOrderId(rs.getInt("orderid"));
                    chr.setGuildID(rs.getInt("guild"));
                    chr.setDeletionStartTime(rs.getLong("deletionStartTime"));
                    if (avatarDataID != 0) {
                        AvatarData avatarData = AvatarData.getAvatarDataFromSQLByID(avatarDataID);
                        chr.setAvatarData(avatarData);
                        chr.setFieldID((int) avatarData.getCharacterStat().getPosMap());
                    }
                    chr.setFriends(Friend.getFriendsFromSQLByOwnerID(id));
                    chr.setSkills(Skill.getSkillsFromSQLByCharID(id));
                    charSet.add(chr);
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load the avatar list for account " + accountID, exception);
        }
        return charSet;
    }

    public static boolean isAvailableCharacter(String name, int worldID) {
        boolean isAvailable = false;
        String query = "SELECT name FROM characterstats WHERE name = ? AND worldidforlog = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {

            ps.setString(1, name);
            ps.setInt(2, worldID);

            try (ResultSet rs = ps.executeQuery()) {
                isAvailable = !rs.next();
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return isAvailable;
    }

    public void loadCharacterData() {
        String query = "SELECT * FROM characters WHERE id = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int equippedInventoryID = rs.getInt("equippedinventory");
                    int equipInventoryID = rs.getInt("equipinventory");
                    int consumeInventoryID = rs.getInt("consumeinventory");
                    int etcInventoryID = rs.getInt("etcinventory");
                    int installInventoryID = rs.getInt("installinventory");
                    int cashInventoryID = rs.getInt("cashinventory");
                    int decorationInventoryID = rs.getInt("decorationinventory");
                    int funckeymapID = rs.getInt("funckeymap_id");
                    int guildID = rs.getInt("guild");
                    int partyID = rs.getInt("party");
                    int rewardPoints = rs.getInt("rewardPoints"); //0 All no need set in current Time
                    int monsterBookID = rs.getInt("monsterbook");
                    int medalID = rs.getInt("medalid");
                    byte monsterParkCount = rs.getByte("monsterparkcount");
                    long previousFieldID = rs.getLong("previousFieldID");
                    String quickSlotKeys = rs.getString("quickslotKeys");
                    int onlineDay = rs.getInt("onlineDay");
                    long onlineTime = rs.getLong("onlineTime");

                    Map<Integer, Inventory> allInventories = Inventory.getAllInventoriesFromSQLByCharID(getId());
                    if (equippedInventoryID != 0) {
                        setEquippedInventory(allInventories.get(equippedInventoryID));
                    }
                    if (equipInventoryID != 0) {
                        setEquipInventory(allInventories.get(equipInventoryID));
                    }
                    if (consumeInventoryID != 0) {
                        setConsumeInventory(allInventories.get(consumeInventoryID));
                    }
                    if (etcInventoryID != 0) {
                        setEtcInventory(allInventories.get(etcInventoryID));
                    }
                    if (installInventoryID != 0) {
                        setInstallInventory(allInventories.get(installInventoryID));
                    }
                    if (cashInventoryID != 0) {
                        setCashInventory(allInventories.get(cashInventoryID));
                    }
                    if (decorationInventoryID != 0) {
                        setDecorationInventory(allInventories.get(decorationInventoryID));
                    }
                    if (partyID != 0) {
                        setPartyID(partyID);
                    }
                    if (guildID != 0) {
                        setGuildID(guildID);
                    }
                    if (monsterBookID != 0) {
                        setMonsterBookInfo(MonsterBookInfo.getMonsterBookInfoFromSQLByMonsterBookID(monsterBookID));
                    }
                    if (onlineDay != 0) {
                        setOnlineDay(onlineDay);
                    }
                    if (onlineTime != 0) {
                        setOnlineTime(onlineTime);
                    }
                    if (quickSlotKeys != null) {
                        setQuickslotKeys(Arrays.stream(quickSlotKeys.split(",")).map(String::trim).map(Integer::parseInt).collect(Collectors.toList()));
                    }
                    setMonsterParkCount(monsterParkCount);
                    setPreviousFieldID((int) previousFieldID);
                    setEquippedMedalItem(medalID);
                    setFuncKeyMaps(FuncKeyMap.getFuncKeyMapsFromSQLByCharID(getId()));
                    setSkills(Skill.getSkillsFromSQLByCharID(getId()));
                    setMatrixCore(MatrixCore.getMatrixCoresFromSQLByCharID(getId()));
                    setMatrixSlot(MatrixSlot.getMatrixSlotsFromSQLByCharID(getId()));
                    setHexaStats(HexaStat.getHexaStatsFromSQLByCharID(getId()));
                    setHexaSkills(HexaSkill.getHexaSkillsFromSQLByCharID(getId()));
                    setSkillAlarms(SkillAlarmInfo.getSkillAlarmsByCharID(getId()));
                    setSequenceSkills(SequenceSkill.getSkillSequenceSkillsByCharID(getId()));
                    setSequenceBuffs(SequenceBuff.getSkillSequenceBuffsByCharID(getId()));
                    setBuffFavorites(BuffFavorite.getBuffFavoritesByCharID(getId()));
                    setBeautySalon(BeautySalon.getBeautySalonFromSQLByCharID(getId()));
                    setFamiliars(Familiar.getFamiliarsFromSQLByCharID(getId()));
                    setMacros(Macro.getMarcosFromSQLByCharID(getId()));
                    setStolenSkills(StolenSkill.getStolenSkillsFromSQLByCharID(getId()));
                    setChosenSkills(ChosenSkill.getChosenSkillsFromSQLByCharID(getId()));
                    setHyperRockFields(getHyperRockFieldsFromSQLByCharID(getId()));
                    setSkillCoolTimes(getSkillCoolTimesFromSQLByCharID(getId()));
                    setItemBoughtAmounts(getItemBoughtAmountsFromSQLByCharID(getId()));
                    setMedalAchievementInfo(MedalAchievementInfo.getMedalAchievementInfosFromSQLByCharID(getId()));
                    setDamageSkins(DamageSkinSaveData.getDamageSkinsFromSQLByCharID(getId()));
                    setPartyboss(PartyBoss.getPartyBossesFromSQLByCharID(getId()));
                    setCores(Core.getCoresFromSQLByCharID(getId()));
                    setBuffDataList(BuffData.getBuffDataListFromSQLByCharID(getId()));
                    setPetVacs(PetVac.getPetVacFromSQLByCharID(getId()));
                    setDailyCoin(DailyCoin.getDailyCoinFromSQLByCharID(getId()));
                    if (getDailyCoin() == null) {
                        DailyCoin dailyCoin = new DailyCoin();
                        dailyCoin.setCharID(getId());
                        setDailyCoin(dailyCoin);
                    }
                    setQuests(getAllQuestsFromSQLByCharID(getId()));
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
    }

    public static Map<Integer, Quest> getAllQuestsFromSQLByCharID(int charId) {
        Map<Integer, Quest> allQuests = new HashMap<>();
        String query = "SELECT " +
                "    q.id AS q_id, " +
                "    q.charid, " +
                "    q.qrkey, " +
                "    q.qrvalue, " +
                "    q.status, " +
                "    q.completedtime, " +
                "    q.expireterm, " +
                "    qpr.id AS qpr_id, " +
                "    qpr.orderNum, " +
                "    qpr.progresstype, " +
                "    qpr.unitid, " +
                "    qpr.requiredcount, " +
                "    qpr.currentcount " +
                "FROM quests q " +
                "LEFT JOIN questprogressrequirements qpr ON q.id = qpr.questid " +
                "WHERE q.charid = ?"; // Thêm điều kiện WHERE để lọc theo charid
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, charId); // Gán giá trị charid vào dấu hỏi
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int questID = rs.getInt("qrkey");
                    if (!allQuests.containsKey(questID)) {
                        Quest quest = new Quest();
                        quest.setId(rs.getLong("q_id"));
                        quest.setCharId(rs.getInt("charid"));
                        quest.setQRKey(questID);
                        quest.setQrValue(rs.getString("qrvalue"));
                        quest.setStatus(getValByNum(rs.getInt("status")));
                        FileTime completedTime = DatabaseManager.getFileTimeFromString(rs.getString("completedtime"));
                        if (completedTime != null) {
                            quest.setCompletedTime(completedTime);
                        }
                        FileTime expireTerm = DatabaseManager.getFileTimeFromString(rs.getString("expireterm"));
                        if (expireTerm != null) {
                            quest.setExpireTerm(expireTerm);
                        }
                        allQuests.put(questID, quest);
                    }

                    long qprID = rs.getLong("qpr_id");
                    if (qprID != 0) {
                        Quest currentQuest = allQuests.get(questID);
                        if (currentQuest != null) {
                            String progressType = rs.getString("progresstype");
                            int unitID = rs.getInt("unitid");
                            int requiredCount = rs.getInt("requiredcount");
                            int currentCount = rs.getInt("currentcount");
                            int orderNum = rs.getInt("orderNum");
                            QuestProgressRequirement qpr = null;
                            switch (progressType) {
                                case "mob": {
                                    QuestProgressMobRequirement questProgressMobRequirement = new QuestProgressMobRequirement();
                                    questProgressMobRequirement.setId(qprID);
                                    questProgressMobRequirement.setOrder(orderNum);
                                    questProgressMobRequirement.setMobID(unitID);
                                    questProgressMobRequirement.setRequiredCount(requiredCount);
                                    questProgressMobRequirement.setCurrentCount(currentCount);
                                    qpr = questProgressMobRequirement;
                                    break;
                                }
                                case "item": {
                                    QuestProgressItemRequirement questProgressItemRequirement = new QuestProgressItemRequirement();
                                    questProgressItemRequirement.setId(qprID);
                                    questProgressItemRequirement.setOrder(orderNum);
                                    questProgressItemRequirement.setItemID(unitID);
                                    questProgressItemRequirement.setRequiredCount(requiredCount);
                                    questProgressItemRequirement.setCurrentCount(currentCount);
                                    qpr = questProgressItemRequirement;
                                    break;
                                }
                                case "money": {
                                    QuestProgressMoneyRequirement questProgressMoneyRequirement = new QuestProgressMoneyRequirement();
                                    questProgressMoneyRequirement.setId(qprID);
                                    questProgressMoneyRequirement.setOrder(orderNum);
                                    questProgressMoneyRequirement.setMoney(QuestShortcut.enabled() ? requiredCount : unitID);
                                    qpr = questProgressMoneyRequirement;
                                    break;
                                }
                                case "level": {
                                    QuestProgressLevelRequirement questProgressLevelRequirement = new QuestProgressLevelRequirement();
                                    questProgressLevelRequirement.setId(qprID);
                                    questProgressLevelRequirement.setOrder(orderNum);
                                    questProgressLevelRequirement.setLevel(QuestShortcut.enabled() ? requiredCount : unitID);
                                    qpr = questProgressLevelRequirement;
                                    break;
                                }
                            }
                            if (qpr != null) {
                                currentQuest.getProgressRequirements().add(qpr);
                            }
                        }
                    }
                }
            }
        } catch (Exception exception) {
            System.err.println("[HikariCP Database] Exception: " + exception);
            if (QuestShortcut.enabled()) throw new IllegalStateException("Character quest load failed", exception);
        }
        return allQuests;
    }

    public void loadCharacterPartyData() {
        if (getPartyID() == 0) {
            return;
        }
        if (this.party == null) {
            if (getWorld() != null) {
                Party newParty = getWorld().getPartyByPartyID(getPartyID());
                if (newParty.getPartyMemberByID(getId()) != null) {
                    this.party = newParty;
                } else {
                    this.partyID = 0;
                }
            } else {
                Party newParty = Server.get().getWorld().getPartyByPartyID(getPartyID());
                if (newParty == null) {
                    return;
                } else {
                    if (newParty.getPartyMemberByID(getId()) != null) {
                        this.party = newParty;
                    } else {
                        this.partyID = 0;
                    }
                }
            }
        }
        if (this.party != null) {
            for (PartyMember partyMember : this.party.getMembers()) {
                if (partyMember.getCharID() == getId()) {
                    partyMember.updateInfoByChar(this);
                }
            }
            PartyMember leader = this.party.getPartyLeader();
            if (leader == null) {
                this.partyID = 0;
                this.party = null;
            }
        }
    }

    public void loadCharacterGuildData() {
        if (getGuildID() == 0) {
            return;
        }
        if (getWorld() != null) {
            if (this.guild == null) {
                this.guild = getWorld().getGuildByID(getGuildID());
            }
        } else {
            this.guild = Server.get().getWorld().getGuildByID(getGuildID());
        }
    }

    public void updateCharFieldToSQL(String columnName, Object value) {
        String query = String.format("UPDATE characters SET %s = ? WHERE id = ?", columnName);
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            if (value instanceof String) {
                ps.setString(1, (String) value);
            } else if (value instanceof Integer) {
                ps.setInt(1, (Integer) value);
            } else if (value instanceof Long) {
                ps.setLong(1, (Long) value);
            } else {
                // Handle other data types if needed
                ps.setObject(1, value);
            }

            ps.setInt(2, getId());
            ps.executeUpdate();

        } catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
    }

    public void saveToSQL() {
        if(net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.economyEnabled()) {
            // step 6: save gate -> this, without the global ECONOMY_MONITOR, when the caller holds neither (EconomyLocks)
            if(net.swordie.ms.client.character.tms282.EconomyLocks.saveWithoutEconomy(this, () -> {
                if(!net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.economicBlocked(getId()))saveCharacterState();
                return null;
            }))return;
            synchronized(net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized(this) {
                if(net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.economicBlocked(getId()))return;
                saveCharacterState();
            } }
        } else saveCharacterState();
    }

    private void saveCharacterState() {
        net.swordie.ms.world.tms282.items.SymbolCouponExchange.requireCertain(this);
        DataPrinter.send(DataPrinter.HIKARICP, "[HikariCP Database] Saving Character: " + getName(), true);
        if (getId() == 0) {
            insertToSQL();
        } else {
            updateToSQL();
        }
    }

    private void insertToSQL() {
        if (getAvatarData() != null) {
            getAvatarData().saveToSQL();
        }
        if (getInventories() != null) {
            for (Inventory inventory : getInventories()) {
                inventory.insertToSQL();
            }
        }
        if (getFuncKeyMaps() != null) {
            for (FuncKeyMap funcKeyMap : getFuncKeyMaps().values()) {
                funcKeyMap.saveToSQL();
            }
        }
        if (getMonsterBookInfo() != null) {
            getMonsterBookInfo().saveToSQL();
        }
        String query = "INSERT INTO `characters` (" +
                "`accid`, `orderid`, `avatardata`, `equippedinventory`, `equipinventory`, " +
                "`consumeinventory`, `etcinventory`, `installinventory`, `cashinventory`, `decorationinventory`, " +
                "`funckeymap_id`, `fieldid`, `guild`, `rewardPoints`, " +
                "`monsterbook`, `party`, `medalid`, `monsterparkcount`, `previousFieldID`, " +
                "`onlineTime`, `onlineDay`, `quickslotKeys`, `deletionStartTime` " +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            int i = 1;
            ps.setInt(i++, getAccId());
            ps.setInt(i++, getOrderId());
            ps.setObject(i++, getAvatarData() != null ? getAvatarData().getId() : null, Types.INTEGER);
            ps.setObject(i++, getEquippedInventory() != null ? getEquippedInventory().getId() : null, Types.INTEGER);
            ps.setObject(i++, getEquipInventory() != null ? getEquipInventory().getId() : null, Types.INTEGER);
            ps.setObject(i++, getConsumeInventory() != null ? getConsumeInventory().getId() : null, Types.INTEGER);
            ps.setObject(i++, getEtcInventory() != null ? getEtcInventory().getId() : null, Types.INTEGER);
            ps.setObject(i++, getInstallInventory() != null ? getInstallInventory().getId() : null, Types.INTEGER);
            ps.setObject(i++, getCashInventory() != null ? getCashInventory().getId() : null, Types.INTEGER);
            ps.setObject(i++, getDecorationInventory() != null ? getDecorationInventory().getId() : null, Types.INTEGER);
            ps.setObject(i++, getFuncKeyMap() != null ? getFuncKeyMap().getId() : null, Types.INTEGER);
            ps.setInt(i++, getFieldID());
            ps.setNull(i++, Types.INTEGER); // Guild
            ps.setInt(i++, getRewardPoints());
            ps.setObject(i++, getMonsterBookInfo() != null ? getMonsterBookInfo().getId() : null, Types.INTEGER);
            ps.setNull(i++, Types.INTEGER); // Party
            ps.setInt(i++, getEquippedMedalItem()); // Medal ID
            ps.setInt(i++, getMonsterParkCount());
            ps.setInt(i++, getPreviousFieldID());
            ps.setLong(i++, getOnlineTime());
            ps.setInt(i++, getOnlineDay());
            ps.setString(i++, getQuickSlotKeysForUpdate());
            ps.setLong(i++, getDeletionStartTime());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int newId = rs.getInt(1);
                    setId(newId);

                    // Cập nhật các đối tượng con với ID mới
                    if (getQuests() != null && !getQuests().isEmpty()) {
                        Quest.saveToSQL(getQuests(), newId);
                    }
                    if (getAvatarData() != null) {
                        getAvatarData().getCharacterStat().setCharacterId(newId);
                        getAvatarData().getCharacterStat().setCharacterIdForLog(newId);
                        getAvatarData().saveToSQL();
                    }
                    if (getEventNameTag() != null) {
                        getEventNameTag().setCharId(newId);
                        getEventNameTag().saveToSQL();
                    }
                    if (getFuncKeyMaps() != null) {
                        for (FuncKeyMap funcKeyMap : getFuncKeyMaps().values()) {
                            funcKeyMap.setCharID(newId);
                            funcKeyMap.saveToSQL();
                        }
                    }
                    if (getInventories() != null) {
                        for (Inventory inventory : getInventories()) {
                            if (inventory.getItems() != null) {
                                for (Item item : inventory.getItems()) {
                                    item.setInventoryID(inventory.getId());
                                    item.setCharID(newId);
                                    item.updatePositionInSQL();
                                }
                            }
                        }
                    }
                    if (getSkills() != null) {
                        for (Skill skill : getSkills()) {
                            skill.setCharId(newId);
                            skill.saveToSQL();
                        }
                    }
                    if (getMatrixCore() != null) {
                        for (MatrixCore core : getMatrixCore()) {
                            core.setCharId(newId);
                            core.saveToSQL();
                        }
                    }
                    if (getMatrixSlot() != null) {
                        for (MatrixSlot slot : getMatrixSlot()) {
                            slot.setCharId(newId);
                            slot.saveToSQL();
                        }
                    }
                    if (getHexaSkills() != null) {
                        for (HexaSkill hexa : getHexaSkills()) {
                            hexa.setCharId(newId);
                            hexa.saveToSQL();
                        }
                    }
                    if (getDailyCoin() != null) {
                        getDailyCoin().setCharID(newId);
                        getDailyCoin().saveToSQL();
                    }
                    if (getMonsterBookInfo() != null) {
                        getMonsterBookInfo().saveToSQL();
                    }
                    if (getMedalAchievementInfo() != null) {
                        for (MedalAchievementInfo info : getMedalAchievementInfo()) {
                            info.setCharId(newId);
                            info.saveToSQL();
                        }
                    }
                    if (getBeautySalon() != null) {
                        getBeautySalon().setCharID(newId);
                        getBeautySalon().saveToSQL();
                    }
                    if (getMacros() != null) {
                        for (Macro macro : getMacros()) {
                            macro.setCharID(newId);
                            macro.saveToSQL(getId());
                        }
                    }
                    if (getStolenSkills() != null) {
                        for (StolenSkill skill : getStolenSkills()) {
                            skill.setCharID(newId);
                            skill.saveToSQL();
                        }
                    }
                    if (getChosenSkills() != null) {
                        for (ChosenSkill skill : getChosenSkills()) {
                            skill.setCharID(newId);
                            skill.saveToSQL();
                        }
                    }
                    if (getFriends() != null) {
                        for (Friend friend : getFriends()) {
                            friend.setOwnerID(newId);
                            friend.saveToSQL();
                        }
                    }
                    if (getPotentials() != null) {
                        for (CharacterPotential potential : getPotentials()) {
                            potential.setCharID(newId);
                            potential.saveToSQL();
                        }
                    }
                    if (getHyperStats() != null) {
                        for (HyperStat hyperStat : getHyperStats()) {
                            hyperStat.setCharId(newId);
                            hyperStat.saveToSQL();
                        }
                    }
                    if (getLinkedSkills() != null) {
                        for (LinkedSkill linkedSkill : getLinkedSkills()) {
                            linkedSkill.setCharId(newId);
                            linkedSkill.saveToSQL();
                        }
                    }
                    if (getFamiliars() != null) {
                        for (Familiar familiar : getFamiliars()) {
                            familiar.setCharID(newId);
                            familiar.saveToSQL();
                        }
                    }
                    if (getDamageSkins() != null) {
                        for (DamageSkinSaveData data : getDamageSkins()) {
                            data.setCharId(newId);
                            data.saveToSQL();
                        }
                    }
                    if (getPartyboss() != null) {
                        for (PartyBoss boss : getPartyboss()) {
                            boss.setCharId(newId);
                            boss.saveToSQL();
                        }
                    }
                    if (getCores() != null) {
                        for (Core core : getCores()) {
                            core.setCharId(newId);
                            core.saveToSQL();
                        }
                    }
                    if (getBuffDataList() != null) {
                        for (BuffData data : getBuffDataList()) {
                            data.setCharID(newId);
                            data.saveToSQL();
                        }
                    }
                    if (getPetVacs() != null) {
                        for (PetVac petvac : getPetVacs()) {
                            petvac.setCharID(newId);
                            petvac.saveToSQL();
                        }
                    }
                    updateHyperRockFieldsToSQL(true);
                }
            }
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            if (getId() != 0) {
                // The characters row exists but a sub-table write failed: report it so the caller can
                // delete the partial character instead of treating it as saved.
                throw new IllegalStateException("Character " + getName() + " (id=" + getId()
                        + ") was only partially saved", e);
            }
        }
    }

    // Hàm mới để cập nhật người dùng đã tồn tại
    private void updateToSQL() {
        String query = "UPDATE characters SET " +
                "monsterparkcount = ?, previousFieldID = ?, onlineTime = ?, onlineDay = ?, " +
                "quickslotKeys = ?, deletionStartTime = ?, medalID = ? " +
                "WHERE id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            int i = 1;
            ps.setInt(i++, getMonsterParkCount());
            ps.setInt(i++, getPreviousFieldID());
            ps.setLong(i++, getOnlineTime());
            ps.setInt(i++, getOnlineDay());
            ps.setString(i++, getQuickSlotKeysForUpdate());
            ps.setLong(i++, getDeletionStartTime());
            ps.setInt(i++, getEquippedMedalItem());
            ps.setInt(i++, getId()); // Mệnh đề WHERE
            ps.executeUpdate();
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
        if (getQuests() != null && !getQuests().isEmpty()) {
            Quest.saveToSQL(getQuests(), getId());
        }
        if (getAvatarData() != null) {
            getAvatarData().saveToSQL();
        }
        if (getEventNameTag() != null) {
            getEventNameTag().saveToSQL();
        }
        if (getFuncKeyMaps() != null) {
            for (FuncKeyMap funcKeyMap : getFuncKeyMaps().values()) {
                funcKeyMap.saveToSQL();
            }
        }
        if (getInventories() != null) {
            for (Inventory inventory : getInventories()) {
                inventory.updateToSQL();
            }
        }
        if (getSkills() != null) {
            for (Skill skill : getSkills()) {
                skill.saveToSQL();
            }
        }
        if (getMatrixCore() != null) {
            for (MatrixCore core : getMatrixCore()) {
                core.saveToSQL();
            }
        }
        if (getMatrixSlot() != null) {
            for (MatrixSlot slot : getMatrixSlot()) {
                slot.saveToSQL();
            }
        }
        if (getHexaSkills() != null) {
            for (HexaSkill skill : getHexaSkills()) {
                skill.saveToSQL();
            }
        }
        if (getDailyCoin() != null) {
            getDailyCoin().saveToSQL();
        }
        if (getMonsterBookInfo() != null) {
            getMonsterBookInfo().saveToSQL();
        }
        if (getMedalAchievementInfo() != null) {
            for (MedalAchievementInfo info : getMedalAchievementInfo()) {
                info.saveToSQL();
            }
        }
        if (getBeautySalon() != null) {
            getBeautySalon().saveToSQL();
        }
        if (getMacros() != null) {
            for (Macro macro : getMacros()) {
                macro.saveToSQL(getId());
            }
        }
        if (getStolenSkills() != null) {
            for (StolenSkill skill : getStolenSkills()) {
                skill.saveToSQL();
            }
        }
        if (getChosenSkills() != null) {
            for (ChosenSkill skill : getChosenSkills()) {
                skill.saveToSQL();
            }
        }
        if (getFriends() != null) {
            for (Friend friend : getFriends()) {
                friend.saveToSQL();
            }
        }
        if (getPotentials() != null) {
            for (CharacterPotential potential : getPotentials()) {
                potential.saveToSQL();
            }
        }
        if (getHyperStats() != null) {
            for (HyperStat hyperStat : getHyperStats()) {
                hyperStat.saveToSQL();
            }
        }
        if (getLinkedSkills() != null) {
            for (LinkedSkill linkedSkill : getLinkedSkills()) {
                linkedSkill.saveToSQL();
            }
        }
        if (getFamiliars() != null) {
            for (Familiar familiar : getFamiliars()) {
                familiar.saveToSQL();
            }
        }
        if (getDamageSkins() != null) {
            for (DamageSkinSaveData data : getDamageSkins()) {
                data.saveToSQL();
            }
        }
        if (getPartyboss() != null) {
            for (PartyBoss boss : getPartyboss()) {
                boss.saveToSQL();
            }
        }
        if (getCores() != null) {
            for (Core core : getCores()) {
                core.saveToSQL();
            }
        }
        if (getBuffDataList() != null) {
            for (BuffData data : getBuffDataList()) {
                data.saveToSQL();
            }
        }
        if (getPetVacs() != null) {
            for (PetVac petvac : getPetVacs()) {
                petvac.saveToSQL();
            }
        }
        updateHyperRockFieldsToSQL(false);
    }

    public void deleteFromSQL() {
        Connection con = null;
        try {
            con = DatabaseManager.getConnection();
            con.setAutoCommit(false); // Start the transaction
            if (getAvatarData() != null) {
                getAvatarData().deleteFromSQL(con);
            }
            if (getMonsterBookInfo() != null) {
                getMonsterBookInfo().deleteFromSQL(con);
            }
            for (Macro macro : getMacros()) {
                macro.deleteFromSQL(con);
            }
            deleteDataFromTable(con, "items", "charid", getId());
            deleteDataFromTable(con, "equips", "charid", getId());
            deleteDataFromTable(con, "petitems", "charid", getId());
            deleteDataFromTable(con, "quests", "charid", getId());
            deleteDataFromTable(con, "questprogressrequirements", "charid", getId());
            deleteDataFromTable(con, "keymaps", "charid", getId());
            deleteDataFromTable(con, "funckeymap", "charid", getId());
            deleteDataFromTable(con, "hyperrockfields", "charid", getId());
            deleteDataFromTable(con, "skills", "charid", getId());
            deleteDataFromTable(con, "skillcooltimes", "charid", getId());
            deleteDataFromTable(con, "matrixskill", "charid", getId());
            deleteDataFromTable(con, "matrixslot", "charid", getId());
            deleteDataFromTable(con, "skillalarms", "charid", getId());
            deleteDataFromTable(con, "skillsequences_skills", "charid", getId());
            deleteDataFromTable(con, "skillsequences_buffs", "charid", getId());
            deleteDataFromTable(con, "bufffavorites", "charid", getId());
            deleteDataFromTable(con, "linkedskills", "charid", getId());
            deleteDataFromTable(con, "hyperstats", "charid", getId());
            deleteDataFromTable(con, "hexaskills", "charid", getId());
            deleteDataFromTable(con, "hexastats", "charid", getId());
            deleteDataFromTable(con, "medals", "charid", getId());
            deleteDataFromTable(con, "chosenskills", "charid", getId());
            deleteDataFromTable(con, "stolenskills", "charid", getId());
            deleteDataFromTable(con, "friends", "ownerid", getId());
            deleteDataFromTable(con, "characterpotentials", "charid", getId());
            deleteDataFromTable(con, "familiars", "charid", getId());
            deleteDataFromTable(con, "damageskins", "charid", getId());
            deleteDataFromTable(con, "partyboss", "charid", getId());
            deleteDataFromTable(con, "cores", "charid", getId());
            deleteDataFromTable(con, "beautydata", "charid", getId());
            deleteDataFromTable(con, "buffdata", "charid", getId());
            deleteDataFromTable(con, "petvac", "charid", getId());
            deleteDataFromTable(con, "dailycoin", "charid", getId());
            deleteDataFromTable(con, "offenses", "charid", getId());
            deleteDataFromTable(con, "itemsbuylimit", "charid", getId());
            deleteDataFromTable(con, "rewardinfo", "charid", getId());
            // other players' friend lists naming this character
            deleteDataFromTable(con, "friends", "friendid", getId());
            // the TMS 282 additions keep per-character state in tables of their own
            for (String[] table : TMS282_CHARACTER_TABLES) {
                deleteDataFromTableIfPresent(con, table[0], table[1], getId());
            }
            deleteRowsIfPresent(con, "DELETE l FROM outfit_like l JOIN outfit_share s ON s.id = l.outfit_id WHERE s.char_id = ?", getId());
            deleteDataFromTableIfPresent(con, "outfit_share", "char_id", getId());
            deleteDataFromTable(con, "characters", "id", getId());
            con.commit();
            setId(0);
        } catch (Exception e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (Exception rollBack) {
                    DataPrinter.send(DataPrinter.HIKARICP_ERROR, rollBack);
                }
            }
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
                }
            }
        }
    }

    /**
     * Per-character tables of the TMS 282 features, emptied with the character. Not here on purpose: logs and audit
     * trails (log_money, zlog_*, gift_code_redemptions, newbie_*_log, launcher_reward_queue), auctions, the meso market
     * and warehouses (other players' goods or money), guild rows (deletion is refused while in a guild).
     */
    static final String[][] TMS282_CHARACTER_TABLES = {
            {"character_dress_up", "character_id"}, {"character_familiar282", "character_id"},
            {"character_familiar282_user", "character_id"}, {"character_func_key", "character_id"},
            {"character_hexa_skill", "character_id"}, {"character_questex", "charid"},
            {"character_skill_record", "character_id"}, {"character_timed_skills", "charid"},
            {"character_vcore", "character_id"}, {"character_vmatrix282", "character_id"},
            {"character_vmatrix282_core", "character_id"}, {"character_vmatrix282_special_cd", "char_id"},
            {"character_vmatrix_slot", "character_id"}, {"char_cube282_choice", "character_id"},
            {"char_cube282_pending", "character_id"}, {"char_skillring282_pending", "character_id"},
            {"char_vmatrix282_pending", "character_id"}, {"cs_wishlist", "character_id"}, {"eventnametag", "charid"},
            {"macros", "charid"}, {"charactercards", "characterid"}, {"newbie_flow", "character_id"},
            {"revival_progress", "charid"}, {"jili_task_character", "characterId"}, {"marin_tarot", "cid"},
            {"tms282_character_emoticon", "character_id"}, {"tms282_character_emoticon_command", "character_id"},
            {"tms282_dimension_tower", "character_id"}, {"tms282_dream_bag", "character_id"},
            {"tms282_dream_items", "character_id"}, {"tms282_jewel", "character_id"}, {"tms282_pop_record", "character_id"},
            {"tms282_couple_record", "character_id"}, {"tms282_friend_record", "character_id"},
            {"tms282_blacklist_guild", "character_id"}, {"tms282_bossrun_clear", "char_id"},
            {"tms282_bossrun_pending", "char_id"}, {"tms282_bossrun_pqlog", "char_id"}, {"urus_reward_pending", "char_id"},
            {"urus_shop_buy", "char_id"}, {"zsf_collect", "charid"}, {"zsf_skill_loading", "character_id"},
            {"zzmj_yewaiboss", "character_id"}, {"zzmj_yewaiboss_rank", "character_id"}, {"mcranking", "charID"},
            {"dojo_weekly_record", "char_id"}, {"unionmember", "charid"},
    };

    /** As deleteDataFromTable, but a table that does not exist (a feature this database never used) is skipped. */
    private void deleteDataFromTableIfPresent(Connection conn, String tableName, String whereColumn, long id) throws SQLException {
        deleteRowsIfPresent(conn, "DELETE FROM `" + tableName + "` WHERE `" + whereColumn + "` = ?", id);
    }

    private void deleteRowsIfPresent(Connection conn, String query, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() != 1146 && e.getErrorCode() != 1054) { // ER_NO_SUCH_TABLE, ER_BAD_FIELD_ERROR
                throw e;
            }
        }
    }

    private void deleteDataFromTable(Connection conn, String tableName, String whereColumn, long id) throws SQLException {
        String query = "DELETE FROM `" + tableName + "` WHERE `" + whereColumn + "` = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setLong(1, id);
        ps.executeUpdate();
    }

    public void updateCharacterPartyIDToSQL() {
        String query = "UPDATE characters SET " +
                (getPartyID() != 0 ? String.format("party = %d ", getPartyID()) : "party = 0 ") +
                String.format("WHERE id = %d;", getId());
        DatabaseManager.executeStatement(query);
        System.out.println(query);
    }

    public void updateCharacterGuildToSQL() {
        String query = "UPDATE characters SET " +
                (getGuild() != null ? String.format("guild = %d ", getGuild().getId()) : "guild = NULL ") +
                String.format("WHERE id = %d;", getId());
        DatabaseManager.executeStatement(query);
        System.out.println(query);
    }

    public void removeCharacterGuildToSQL() {
        String query = "UPDATE characters SET guild = NULL " + String.format("WHERE id = %d;", getId());
        DatabaseManager.executeStatement(query);
        System.out.println(query);
    }

    public void updateCharacterOrderIDToSQL() {
        String query = "UPDATE characters SET " +
                String.format("orderid = %d ", getOrderId()) +
                String.format("WHERE id = %d;", getId());
        DatabaseManager.executeStatement(query);
    }

    public static int[] getHyperRockFieldsFromSQLByCharID(int charID) {
        int[] hyperRockFields = new int[13];
        String query = "SELECT ord, fieldid FROM hyperrockfields WHERE charid = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int ord = rs.getInt("ord");
                    int fieldID = rs.getInt("fieldid");
                    if (ord >= 0 && ord < hyperRockFields.length) {
                        hyperRockFields[ord] = fieldID;
                    }
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return hyperRockFields;
    }

    public static Int2LongMap getSkillCoolTimesFromSQLByCharID(int charID) {
        Int2LongMap skillCoolTimes = new Int2LongOpenHashMap();
        String query = "SELECT skillid, nextusabletime FROM skillcooltimes WHERE charid = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int skillID = rs.getInt("skillid");
                    long nextUsableTime = rs.getLong("nextusabletime");
                    skillCoolTimes.put(skillID, nextUsableTime);
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return skillCoolTimes;
    }

    public void updateSkillsCoolTimesToSQL(boolean insert) {
        if (insert) {
            getSkillCoolTimes().forEach((key, value) -> {
                String query = "INSERT INTO `skillcooltimes` (" +
                        "`charid`, " +
                        "`skillid`, " +
                        "`nextusabletime` " +
                        ") VALUES (" +
                        String.format("%d, ", getId()) +
                        String.format("%d, ", key) +
                        String.format("%d ", value) +
                        ");";
                DatabaseManager.executeStatement(query);
            });
        } else {
            getSkillCoolTimes().forEach((key, value) -> {
                String query = "UPDATE skillcooltimes SET " +
                        String.format("nextusabletime = %d ", value) +
                        String.format("WHERE charid = %d AND skillid = %d;", getId(), key);
                DatabaseManager.executeStatement(query);
            });
        }
    }

    public void deleteSkillsCoolTimesFromSQL() {
        String query = "DELETE FROM `skillcooltimes` WHERE " +
                String.format("`charid` = %d", getId());
        DatabaseManager.executeStatement(query);
    }

    public static Map<Long, Integer> getItemBoughtAmountsFromSQLByCharID(int charID) {
        Map<Long, Integer> itemBoughtAmounts = new HashMap<>();
        String query = "SELECT shopitemid, amountbought FROM itemsbuylimit WHERE charid = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long shopItemID = rs.getLong("shopitemid");
                    int amountBought = rs.getInt("amountbought");
                    itemBoughtAmounts.put(shopItemID, amountBought);
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return itemBoughtAmounts;
    }

    public void updateItemBoughtAmountsToSQL(long shopItemID, int quantity) {
        if (getItemBoughtAmounts() == null) {
            return;
        }

        String query = "INSERT INTO `itemsbuylimit` (`shopitemid`, `charid`, `amountbought`) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE `amountbought` = VALUES(`amountbought`);";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setLong(1, shopItemID);
            ps.setInt(2, getId());
            ps.setInt(3, quantity);
            ps.executeUpdate();
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
    }

    public void updateHyperRockFieldsToSQL(boolean insert) {
        if (insert) {
            for (int i = 0; i < getHyperRockFields().length; i++) {
                String query = "INSERT INTO `hyperrockfields` (" +
                        "`charid`, " +
                        "`ord`, " +
                        "`fieldid` " +
                        ") VALUES (" +
                        String.format("%d, ", getId()) +
                        String.format("%d, ", i) +
                        String.format("%d ", 999999999) +
                        ");";
                DatabaseManager.executeStatement(query);
            }
        } else {
            for (int i = 0; i < getHyperRockFields().length; i++) {
                String query = "UPDATE hyperrockfields SET " +
                        String.format("fieldid = %d ", getHyperRockFields()[i]) +
                        String.format("WHERE charid = %d AND ord = %d;", getId(), i);
                DatabaseManager.executeStatement(query);
            }
        }
    }

    public Char() {
        this(0, "", 0, 0, 0, (short) 0, (byte) -1, (byte) -1, 0, 0, new int[]{});
    }

    public Char(int accId, String name, int keySettingType, int eventNewCharSaleJob, int job, short curSelectedSubJob, byte gender, byte skin, int face, int hair, int[] items) {
        this.accId = accId;
        this.avatarData = new AvatarData();
        this.avatarData.setAvatarLook(new AvatarLook());
        AvatarLook avatarLook = getAvatarData().getAvatarLook();
        avatarLook.setName(name);
        avatarLook.setGender(gender);
        avatarLook.setSkin(skin);
        avatarLook.setFace(face);
        avatarLook.setHair(hair);
        List<Integer> hairEquips = new ArrayList<>();
        for (int itemId : items) {
            Equip equip = ItemData.getEquipDeepCopyFromID(itemId, false);
            if (equip != null && ItemConstants.isEquip(itemId)) {
                hairEquips.add(itemId);
                if ("Wp".equals(equip.getiSlot())) {
                    if (!equip.isCash()) {
                        avatarLook.setWeaponId(itemId);
                    } else {
                        avatarLook.setWeaponStickerId(itemId);
                    }
                }
            }
        }
        avatarLook.setHairEquips(hairEquips);
        avatarLook.setJob(job);
        CharacterStat characterStat = new CharacterStat(name, job);
        getAvatarData().setCharacterStat(characterStat);
        characterStat.setGender(gender);
        characterStat.setSkin(skin);
        characterStat.setFace(face);
        characterStat.setHair(hair);
        characterStat.setSubJob(curSelectedSubJob);
        setFieldInstanceType(CHANNEL);
        this.ranking = new Ranking();
        this.pets = new ArrayList<>();
        this.quests = new ConcurrentHashMap<>();
        this.itemPots = new ArrayList<>();
        this.friendRecords = new ArrayList<>();
        this.expConsumeItems = new ArrayList<>();
        this.linkedSkill = new ArrayList<>();
        this.skills = new Int2ObjectOpenHashMap<>();
        this.skillCoolTimes = new Int2LongOpenHashMap();
        this.matrixCore = new HashSet<>();
        this.matrixSlot = new HashSet<>();
        this.hexaStats = new HashMap<>();
        this.hexaSkills = new HashSet<>();
        this.temporaryStatManager = new TemporaryStatManager(this);
        this.gachaponManager = new GachaponManager();
        this.friends = new HashSet<>(50);
        this.monsterBookInfo = new MonsterBookInfo();
        this.potentialMan = new CharacterPotentialMan(this);
        this.familiars = new HashSet<>();
        this.hyperrockfields = new int[]{
                999999999, 999999999, 999999999, 999999999, 999999999,
                999999999, 999999999, 999999999, 999999999, 999999999,
                999999999, 999999999, 999999999,
        };
        this.monsterParkCount = 0;
        this.equippedMedalItem = 0;
        this.currentDirectionNode = new HashMap<>();
        this.potentials = new HashSet<>();
        //monsterBattleMobInfos = new ArrayList<>();
        //monsterBattleLadder = new MonsterBattleLadder();
        //monsterBattleRankInfo = new MonsterBattleRankInfo();
        this.psychicAreas = new HashMap<>();
        this.psychicLocks = new HashMap<>();
        this.psychicLockBalls = new HashMap<>();
        this.funcKeyMaps = new LinkedHashMap<>();
        this.medalAchievementInfo = new HashSet<>();
        this.damageSkins = new HashSet<>();
        this.partyboss = new HashSet<>();
        this.cores = new HashSet<>();
    }

    public AvatarData getAvatarData() {
        return avatarData;
    }

    public void setAvatarData(AvatarData avatarData) {
        this.avatarData = avatarData;
    }

    public Ranking getRanking() {
        return ranking;
    }

    public void setRanking(Ranking ranking) {
        this.ranking = ranking;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getAccId() {
        return accId;
    }

    public void setAccId(int accId) {
        this.accId = accId;
    }

    public Inventory getEquippedInventory() {
        if (equippedInventory == null) {
            this.equippedInventory = new Inventory(EQUIPPED, GameConstants.MAX_INVENTORY_SLOTS);
        }
        return equippedInventory;
    }

    public void setEquippedInventory(Inventory equippedInventory) {
        this.equippedInventory = equippedInventory;
    }

    public void addItemToInventoryToNewCharacter(InvType type, Item item, boolean hasCorrectBagIndex) {
        if (item == null) {
            return;
        }
        Inventory inventory = getInventoryByType(type);
        int itemID = item.getItemId();
        ItemInfo itemInfo = ItemData.getItemInfoByID(item.getItemId());
        int quantity = item.getQuantity();
        if (inventory != null) {
            Item existingItem = inventory.getItemByItemIDAndStackable(item);
            boolean rec = false;
            if (existingItem != null && existingItem.getInvType().isStackable() && existingItem.getQuantity() < itemInfo.getSlotMax()) {
                if (quantity + existingItem.getQuantity() > itemInfo.getSlotMax()) {
                    quantity = itemInfo.getSlotMax() - existingItem.getQuantity();
                    item.setQuantity(item.getQuantity() - quantity);
                    rec = true;
                }
                existingItem.addQuantity(quantity);
                Item copy = item.deepCopy();
                copy.setQuantity(quantity);
                if (rec) {
                    addItemToInventoryToNewCharacter(item);
                }
            } else {
                if (!hasCorrectBagIndex) {
                    item.setBagIndex(inventory.getFirstOpenSlot());
                }
                Item itemCopy = null;
                if (item.getInvType().isStackable() && itemInfo != null && item.getQuantity() > itemInfo.getSlotMax()) {
                    itemCopy = item.deepCopy();
                    quantity = quantity - itemInfo.getSlotMax();
                    itemCopy.setQuantity(quantity);
                    item.setQuantity(itemInfo.getSlotMax());
                    rec = true;
                }
                inventory.addItem(item, getId());
                if (rec) {
                    addItemToInventoryToNewCharacter(itemCopy);
                }
            }
            setBulletIDForAttack(calculateBulletIDForAttack(1));
        }
    }

    public void addItemToInventoryToNewCharacter(Item item) {
        addItemToInventoryToNewCharacter(item.getInvType(), item, false);
    }

    public void addItemToInventory(InvType type, Item item, boolean hasCorrectBagIndex, boolean isPetLoot) {
        if(net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.economyEnabled()) {
            synchronized(net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized(this) {
                net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.requireEconomicAccess(this);
                addInventoryItem(type,item,hasCorrectBagIndex,isPetLoot);
            } }
        } else addInventoryItem(type,item,hasCorrectBagIndex,isPetLoot);
    }

    private void addInventoryItem(InvType type, Item item, boolean hasCorrectBagIndex, boolean isPetLoot) {
        if (item == null || item.getItemId() == GameConstants.MOB_SOUL) {
            return;
        }
        checkAndRemoveExpiredItems(false);
        int itemID = item.getItemId();
        Inventory inventory = getInventoryByType(type);
        ItemInfo itemInfo = ItemData.getItemInfoByID(itemID);
        int quantity = item.getQuantity();

        if (inventory != null) {
            Item existingItem = inventory.getItemByItemIDAndStackable(item);
            boolean rec = false;
            //Nếu đã có item rồi và nó có thể stack và số lượng phải < Stack Max
            if (existingItem != null && existingItem.getInvType().isStackable() && existingItem.getQuantity() < itemInfo.getSlotMax()
                    && !ItemConstants.isThrowingItem(itemID)
                    && !ItemConstants.isBullet(itemID)
                    && !(EventConstants.RED_LEAF_HIGH_EVENT && existingItem.getItemId() == 4033766)) {
                if (quantity + existingItem.getQuantity() > itemInfo.getSlotMax()) {
                    quantity = itemInfo.getSlotMax() - existingItem.getQuantity();
                    item.setQuantity(item.getQuantity() - quantity);
                    rec = true;
                }
                existingItem.addQuantity(quantity);
                net.swordie.ms.world.tms282.audit.AuditLog.itemGain(this, existingItem, quantity); // audit: zlog_inventory
                write(WvsContext.inventoryOperation(!isPetLoot, false, UpdateQuantity, (short) existingItem.getBagIndex(), (byte) -1, 0, existingItem));
                Item copy = item.deepCopy();
                copy.setQuantity(quantity);
                if (rec) {
                    addItemToInventory(item);
                }
                //Không có item trong inventory.
            } else {
                if (!hasCorrectBagIndex) {
                    item.setBagIndex(inventory.getFirstOpenSlot());
                }
                Item itemCopy = null;
                if (item.getInvType().isStackable() && itemInfo != null && item.getQuantity() > itemInfo.getSlotMax()) {
                    itemCopy = item.deepCopy();
                    quantity = quantity - itemInfo.getSlotMax();
                    itemCopy.setQuantity(quantity);
                    item.setQuantity(itemInfo.getSlotMax());
                    rec = true;
                }
                if (ItemConstants.isPet(item.getItemId())) {
                    PetItem petItem = (PetItem) item;
                    if (petItem.getLevel() < 1) {
                        petItem.setLevel((byte) 1);
                        petItem.setTameness((short) 0);
                        petItem.setRepleteness((byte) 100);
                        petItem.setRemainLife(0);
                    }
                    petItem.setDateDead(item.getDateExpire());
                }
                inventory.addItem(item, getId());
                net.swordie.ms.world.tms282.audit.AuditLog.itemGain(this, item, item.getQuantity()); // audit: zlog_inventory
                item.saveToSQL();
                write(WvsContext.inventoryOperation(!isPetLoot, false, Add, (short) item.getBagIndex(), (byte) -1, 0, item));
                if (GameConstants.isIntensePowerCrystal(itemID)) {
                    write(WvsContext.setBossReward(this));
                }
                if (rec) {
                    addItemToInventory(itemCopy);
                }
            }
            if (ItemConstants.isCoreItem(itemID)) {
                Core existingCore = getCores().stream().filter(c -> c.getCoreID() == itemID).findAny().orElse(null);
                if (existingCore != null) {
                    existingCore.setLeftCount(existingCore.getLeftCount() + quantity);
                    write(EvolvingPacket.coreInventoryOperation(getCores(), EvolvingSystemType.Update_Quantity));
                } else {
                    getCores().add(new Core(getId(), getFirstOpenSlot(), 1, itemID, quantity));
                    write(EvolvingPacket.coreInventoryOperation(getCores(), EvolvingSystemType.Add));
                }
            }
            setBulletIDForAttack(calculateBulletIDForAttack(1));
            String itemName = StringData.getItemStringById(itemID);
            if (itemName != null) {
                DataPrinter.send(DataPrinter.ITEM, String.format("Player %s added Item %s | ID: %d | Item ID: %d | Quantity: %d | InvType: %d | Slot: %d.",
                        getName(), itemName, item.getId(), itemID, item.getQuantity(), item.getInvType().getVal(), item.getBagIndex()), true);
            }
        }
    }

    public void addItemToInventory(Item item) {
        addItemToInventory(item.getInvType(), item, false, false);
    }

    public void addItemToInventory(Item item, boolean isPetLoot) {
        addItemToInventory(item.getInvType(), item, false, isPetLoot);
    }

    public void addStackableItemToInventory(InvType type, Item item, boolean hasCorrectBagIndex, boolean isPetLoot) {
        if (item == null || item.getItemId() == GameConstants.MOB_SOUL) {
            return;
        }
        checkAndRemoveExpiredItems(false);
        Inventory inventory = getInventoryByType(type);
        ItemInfo itemInfo = ItemData.getItemInfoByID(item.getItemId());
        int quantity = item.getQuantity();
        if (inventory != null) {
            Item existingItem = inventory.getItemByItemIDAndStackable(item);
            boolean rec = false;
            if (existingItem != null && existingItem.getQuantity() < itemInfo.getSlotMax() && !ItemConstants.isThrowingItem(item.getItemId())) {
                if (quantity + existingItem.getQuantity() > itemInfo.getSlotMax()) {
                    quantity = itemInfo.getSlotMax() - existingItem.getQuantity();
                    item.setQuantity(item.getQuantity() - quantity);
                    rec = true;
                }
                existingItem.addQuantity(quantity);
                net.swordie.ms.world.tms282.audit.AuditLog.itemGain(this, existingItem, quantity); // audit: zlog_inventory
                write(WvsContext.inventoryOperation(!isPetLoot, false, UpdateQuantity, (short) existingItem.getBagIndex(), (byte) -1, 0, existingItem));
                Item copy = item.deepCopy();
                copy.setQuantity(quantity);
                if (rec) {
                    addStackableItemToInventory(item, isPetLoot);
                }
            } else {
                if (!hasCorrectBagIndex) {
                    item.setBagIndex(inventory.getFirstOpenSlot());
                }
                Item itemCopy = null;
                if (itemInfo != null && item.getQuantity() > itemInfo.getSlotMax()) {
                    itemCopy = item.deepCopy();
                    quantity = quantity - itemInfo.getSlotMax();
                    itemCopy.setQuantity(quantity);
                    item.setQuantity(itemInfo.getSlotMax());
                    rec = true;
                }
                inventory.addItem(item, getId());
                net.swordie.ms.world.tms282.audit.AuditLog.itemGain(this, item, item.getQuantity()); // audit: zlog_inventory
                DataPrinter.send(DataPrinter.ITEM, String.format("Player %s added Stackable Cash Item %s | ID: %d | ItemID: %d | Quantity: %d | InvType: %d | BagSlotIndex: %d.",
                        getName(), StringData.getItemStringById(item.getItemId()), item.getId(), item.getItemId(), item.getQuantity(), item.getInvType().getVal(), item.getBagIndex()), true);
                write(WvsContext.inventoryOperation(!isPetLoot, false, Add, (short) item.getBagIndex(), (byte) -1, 0, item));
                if (rec) {
                    addStackableItemToInventory(itemCopy, isPetLoot);
                }
            }
            setBulletIDForAttack(calculateBulletIDForAttack(1));
        }
    }

    public void addStackableWithSlotMaxItemToInventory(Item item, int slotMax, boolean isPetLoot) {
        addStackableWithSlotMaxItemToInventory(item.getInvType(), item, slotMax, false, isPetLoot);
    }

    public void addStackableWithSlotMaxItemToInventory(InvType type, Item item, int slotMax, boolean hasCorrectBagIndex, boolean isPetLoot) {
        if (item == null || item.getItemId() == GameConstants.MOB_SOUL) {
            return;
        }
        checkAndRemoveExpiredItems(false);
        Inventory inventory = getInventoryByType(type);
        ItemInfo itemInfo = ItemData.getItemInfoByID(item.getItemId());
        int quantity = item.getQuantity();
        if (inventory != null) {
            Item existingItem = inventory.getItemByItemIDAndStackable(item, slotMax);
            boolean rec = false;
            if (existingItem != null && existingItem.getQuantity() < slotMax) {
                if (quantity + existingItem.getQuantity() > slotMax) {
                    quantity = slotMax - existingItem.getQuantity();
                    item.setQuantity(item.getQuantity() - quantity);
                    rec = true;
                }
                existingItem.addQuantity(quantity);
                net.swordie.ms.world.tms282.audit.AuditLog.itemGain(this, existingItem, quantity); // audit: zlog_inventory
                write(WvsContext.inventoryOperation(!isPetLoot, false, UpdateQuantity, (short) existingItem.getBagIndex(), (byte) -1, 0, existingItem));
                Item copy = item.deepCopy();
                copy.setQuantity(quantity);
                if (rec) {
                    addStackableWithSlotMaxItemToInventory(item, slotMax, isPetLoot);
                }
            } else {
                if (!hasCorrectBagIndex) {
                    item.setBagIndex(inventory.getFirstOpenSlot());
                }
                Item itemCopy = null;
                if (itemInfo != null && item.getQuantity() > slotMax) {
                    itemCopy = item.deepCopy();
                    quantity = quantity - slotMax;
                    itemCopy.setQuantity(quantity);
                    item.setQuantity(slotMax);
                    rec = true;
                }
                inventory.addItem(item, getId());
                net.swordie.ms.world.tms282.audit.AuditLog.itemGain(this, item, item.getQuantity()); // audit: zlog_inventory
                DataPrinter.send(DataPrinter.ITEM, String.format("Player %s added Stackable Cash Item %s | ID: %d | ItemID: %d | Quantity: %d | InvType: %d | BagSlotIndex: %d.",
                        getName(), StringData.getItemStringById(item.getItemId()), item.getId(), item.getItemId(), item.getQuantity(), item.getInvType().getVal(), item.getBagIndex()), true);
                write(WvsContext.inventoryOperation(!isPetLoot, false, Add, (short) item.getBagIndex(), (byte) -1, 0, item));
                if (rec) {
                    addStackableWithSlotMaxItemToInventory(itemCopy, slotMax, isPetLoot);
                }
            }
            setBulletIDForAttack(calculateBulletIDForAttack(1));
        }
    }

    public void addStackableItemToInventory(Item item, boolean isPetLoot) {
        addStackableItemToInventory(item.getInvType(), item, false, isPetLoot);
    }

    public Inventory getEquipInventory() {
        if (equipInventory == null) {
            this.equipInventory = new Inventory(EQUIP, 48);
        }
        return equipInventory;
    }

    public void setEquipInventory(Inventory equipInventory) {
        this.equipInventory = equipInventory;
    }

    public Inventory getConsumeInventory() {
        if (consumeInventory == null) {
            this.consumeInventory = new Inventory(CONSUME, 48);
        }
        return consumeInventory;
    }

    public void setConsumeInventory(Inventory consumeInventory) {
        this.consumeInventory = consumeInventory;
    }

    public Inventory getEtcInventory() {
        if (etcInventory == null) {
            this.etcInventory = new Inventory(ETC, 48);
        }
        return etcInventory;
    }

    public void setEtcInventory(Inventory etcInventory) {
        this.etcInventory = etcInventory;
    }

    public Inventory getInstallInventory() {
        if (installInventory == null) {
            this.installInventory = new Inventory(INSTALL, 48);
        }
        return installInventory;
    }

    public void setInstallInventory(Inventory installInventory) {
        this.installInventory = installInventory;
    }

    public Inventory getCashInventory() {
        if (cashInventory == null) {
            this.cashInventory = new Inventory(CASH, 128);
        }
        return cashInventory;
    }

    public void setCashInventory(Inventory cashInventory) {
        this.cashInventory = cashInventory;
    }

    public Inventory getDecorationInventory() {
        if (decorationInventory == null) {
            this.decorationInventory = new Inventory(DECORATION, 256);
        }
        return decorationInventory;
    }

    public void setDecorationInventory(Inventory decorationInventory) {
        this.decorationInventory = decorationInventory;
    }

    public void encode(OutPacket outPacket, DBChar mask) {
        for (int i = 0; i < 100; i++) {
            outPacket.encodeByte(1);
        }
        outPacket.encodeByte(getCombatOrders());
        for (int i = 0; i < GameConstants.MAX_PET_AMOUNT; i++) {
            if (i < getPets().size()) {
                outPacket.encodeInt(getPets().get(i).getActiveSkillCoolTime());
            } else {
                outPacket.encodeInt(-2);
            }
        }
        outPacket.encodeByte(0); // unk, not in kmst
        outPacket.encodeByte(0);
        outPacket.encodeInt(0);
        outPacket.encodeByte(0);
        if (mask.isInMask(DBChar.Character)) {
            getAvatarData().getCharacterStat().encode(outPacket);
            outPacket.encodeByte(getFriendRecords().size());
            boolean hasBlessingOfFairy = getBlessingOfFairy() != null;
            outPacket.encodeByte(hasBlessingOfFairy);
            if (hasBlessingOfFairy) {
                outPacket.encodeString(getBlessingOfFairy());
            }
            boolean hasBlessingOfEmpress = getBlessingOfEmpress() != null;
            outPacket.encodeByte(hasBlessingOfEmpress);
            if (hasBlessingOfEmpress) {
                outPacket.encodeString(getBlessingOfEmpress());
            }
            outPacket.encodeByte(false); // ultimate explorer, deprecated
        }
        // sub_140687380
        outPacket.encodeShort(0);
        outPacket.encodeFT(FileTime.MIN_TIME());

        outPacket.encodeInt(GameConstants.DAMAGE_SKIN_MAX_SIZE);
        encodeDamageSkins(outPacket);

        if (mask.isInMask(DBChar.Money)) {
            outPacket.encodeLong(getMoney());
        }
        if (mask.isInMask(DBChar.ItemSlotConsume) || mask.isInMask(DBChar.ExpConsumeItem)) {
            outPacket.encodeInt(getExpConsumeItems().size());
            for (ExpConsumeItem eci : getExpConsumeItems()) {
                eci.encode(outPacket);
            }
        }
        if (mask.isInMask(DBChar.ItemSlotConsume) || mask.isInMask(DBChar.ShopBuyLimit)) {
            int size = 0;
            outPacket.encodeInt(size);
            for (int i = 0; i < size; i++) {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeLong(0);
                outPacket.encodeLong(0);
            }
        }
        if (mask.isInMask(DBChar.InventorySize)) {
            outPacket.encodeInt(getEquipInventory().getSlots());
            outPacket.encodeInt(getConsumeInventory().getSlots());
            outPacket.encodeInt(getInstallInventory().getSlots());
            outPacket.encodeInt(getEtcInventory().getSlots());
            outPacket.encodeInt(getCashInventory().getSlots());
            outPacket.encodeInt(getDecorationInventory().getSlots());
        }

        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);

        if (mask.isInMask(DBChar.ItemSlotEquip)) {
            encodeEquips(outPacket);
        }
        if (mask.isInMask(DBChar.ItemSlotConsume)) {
            final List<Item> consumeItems = getConsumeInventory().getItems();
            for (Item item : consumeItems) {
                outPacket.encodeShort(item.getBagIndex());
                item.encode(outPacket);
            }
            outPacket.encodeShort(0);
        }
        if (mask.isInMask(DBChar.ItemSlotInstall)) {
            final List<Item> installItems = getInstallInventory().getItems();
            for (Item item : installItems) {
                outPacket.encodeShort(item.getBagIndex());
                item.encode(outPacket);
            }
            outPacket.encodeShort(0);
        }
        if (mask.isInMask(DBChar.ItemSlotEtc)) {
            final List<Item> etcItems = getEtcInventory().getItems();
            for (Item item : etcItems) {
                outPacket.encodeShort(item.getBagIndex());
                item.encode(outPacket);
            }
            outPacket.encodeShort(0);
        }
        if (mask.isInMask(DBChar.ItemSlotCash)) {
            final List<Item> cashItems = getCashInventory().getItems();
            for (Item item : cashItems) {
                outPacket.encodeShort(item.getBagIndex());
                item.encode(outPacket);
            }
            outPacket.encodeShort(0);
        }
        // BagDatas
        if (mask.isInMask(DBChar.ItemSlotConsume)) {
            outPacket.encodeInt(0);
        }
        if (mask.isInMask(DBChar.ItemSlotInstall)) {
            outPacket.encodeInt(0);
        }
        if (mask.isInMask(DBChar.ItemSlotEtc)) {
            outPacket.encodeInt(0);
        }
        // sub_140651D90 || more bag datas
        outPacket.encodeInt(0); // size => item.encode (Special)
        outPacket.encodeInt(0); // size => item.encode (Special)
        outPacket.encodeInt(0); // size => item.encode (Special)
        outPacket.encodeInt(0); // size => item.encode (Special)
        outPacket.encodeInt(0); // size => item.encode (Special)
        // End bagdatas

        if (mask.isInMask(DBChar.ItemSlotEtc)) {
            encodeBossReward(outPacket);
        }

        if (mask.isInMask(DBChar.CoreAura)) {
            int val = 0;
            outPacket.encodeInt(val);
            for (int i = 0; i < val; i++) {
                outPacket.encodeInt(0);
                outPacket.encodeFT(FileTime.MAX_TIME());
            }
        }
        if (mask.isInMask(DBChar.Unk40000000)) {
            int size = 0;
            outPacket.encodeInt(size);
            for (int i = 0; i < size; i++) {
                outPacket.encodeLong(0); // 1st half is id, 2nd half level?
                outPacket.encodeFT(FileTime.MAX_TIME()); // time
            }
        }
        if (mask.isInMask(DBChar.SkillRecord)) {
            outPacket.encodeByte(true);
            List<Skill> skills = getSkills().stream().filter(s -> !SkillConstants.isUnionSkill(s.getSkillId())).sorted(Comparator.comparingInt(Skill::getSkillId)).collect(Collectors.toList());
            outPacket.encodeShort(skills.size());
            for (Skill skill : skills) {
                outPacket.encodeInt(skill.getSkillId());
                outPacket.encodeInt(skill.getCurrentLevel());
                outPacket.encodeFT(FileTime.MAX_TIME());
                if (SkillConstants.isSkillNeedMasterLevel(skill.getSkillId())) {
                    outPacket.encodeInt(skill.getMasterLevel());
                }
            }
            String str = getQRValueByKey(QuestConstants.HYPER_STATS_PRESET, "hyperstats");
            outPacket.encodeByte(Integer.parseInt(str != null ? str : "0"));
            for (int i = 0; i <= 2; i++) {
                outPacket.encodeInt(getHyperStats(i).size()); // WvsContext::490
                for (HyperStat hyperStat : getHyperStats(i)) { // sub_9AC0E0
                    hyperStat.encode(outPacket);
                }
            }
            // Link Skills:
            short job = getJob();
            byte linkSkillLevel = JobConstants.isZero(job) ? (byte) getSkillLevel(Zero.RHINNES_BLESSING_BOOST) : (byte) SkillConstants.getLinkSkillLevelByCharLevel(getLevel(), job);
            int linkSkillID = SkillConstants.getLinkSkillByJob(job);
            int originalOfLinkSkillID = SkillConstants.getOriginalOfLinkedSkill(linkSkillID);
            outPacket.encodeInt(getId());
            outPacket.encodeInt(originalOfLinkSkillID); // owner link skill id
            outPacket.encodeInt(linkSkillLevel); // -1

            outPacket.encodeByte(1);
            outPacket.encodeShort(getAccountLinkSkills().size());
            for (LinkSkill linkSkill : getAccountLinkSkills()) {
                outPacket.encodeInt(linkSkill.getOwnerID());
                outPacket.encodeInt(SkillConstants.getOriginalOfLinkedSkill(linkSkill.getLinkSkillID()));
                outPacket.encodeInt(linkSkill.getLinkCount()); // link count
            }

            Int2IntMap linkskills = new Int2IntOpenHashMap();
            for (LinkSkill linkSkill : getAccountLinkSkills()) {
                int ordinarySkill = SkillConstants.getStackingLinkSkill(linkSkill.getLinkSkillID());
                linkskills.put(ordinarySkill, linkskills.get(ordinarySkill) + linkSkill.getLevel());
            }
            outPacket.encodeShort(linkskills.size());
            for (var entry : linkskills.int2IntEntrySet()) {
                outPacket.encodeInt(entry.getIntKey());
                outPacket.encodeShort(entry.getIntValue());
            }

            outPacket.encodeByte(1);
            String presetStr = getQRValueByKey(QuestConstants.LINK_SKILL_PRESET, "preset");
            int preset = Integer.parseInt(presetStr != null ? presetStr : "0");
            outPacket.encodeInt(getLinkedSkills(preset).size());
            for (LinkedSkill linkedSkill : getLinkedSkills(preset)) {
                outPacket.encodeInt(linkedSkill.getSkillID());
            }
            outPacket.encodeInt(0); // 0
            outPacket.encodeInt(0); // 0
        }
        if (mask.isInMask(DBChar.SkillCooltime)) {
            final long curTime = System.currentTimeMillis();
            Int2LongMap cooltimes = new Int2LongOpenHashMap();
            getSkillCoolTimes().forEach((key, value) -> {
                if (value - curTime > 0) {
                    cooltimes.put(key, value);
                }
            });
            outPacket.encodeShort(cooltimes.size());
            for (var cooltime : cooltimes.int2LongEntrySet()) {
                outPacket.encodeInt(cooltime.getIntKey()); // nSkillId
                outPacket.encodeInt((int) ((cooltime.getLongValue() - curTime) / 1000)); // nSkillCooltime
            }
        }
        if (mask.isInMask(DBChar.SkillAlarmInfo)) {
            for (int i = 0; i < SkillAlarmInfo.MAX_INDEX; i++) {
                outPacket.encodeInt(getSkillAlarms().get(i).getSkillId());
            }
            for (int i = 0; i < SkillAlarmInfo.MAX_INDEX; i++) {
                outPacket.encodeByte(getSkillAlarms().get(i).isEnable() ? 1 : 0);
            }
            for (int i = 0; i < SkillAlarmInfo.MAX_INDEX; i++) {
                outPacket.encodeInt(getSkillAlarms().get(i).getKey());
            }
        }
        if (mask.isInMask(DBChar.QuestRecord)) {
            boolean removeAllOldEntries = true;
            outPacket.encodeByte(removeAllOldEntries);
            List<Quest> inProgressSet = getQuestsInProgress();
            List<Quest> subList;
            if (inProgressSet.size() <= QuestConstants.MAX_QUESTS_ENCODE) {
                subList = new ArrayList<>(inProgressSet);
                subList.sort(Comparator.comparingInt(Quest::getQRKey));
            } else {
                subList = getSortedQuestSublist(inProgressSet, 0, QuestConstants.MAX_QUESTS_ENCODE);
            }
            outPacket.encodeShort(subList.size());
            for (Quest quest : subList) {
                outPacket.encodeInt(quest.getQRKey());
                outPacket.encodeString(quest.getQRValue());
            }

            outPacket.encodeShort(0); // size

            if (!removeAllOldEntries) {
                // blacklisted quests
                short size2 = 0;
                outPacket.encodeShort(size2);
                for (int i = 0; i < size2; i++) {
                    outPacket.encodeInt(0); // nQRKey
                }
            }
            int size = 0;
            outPacket.encodeShort(size);
            // Not sure what this is for
            for (int i = 0; i < size; i++) {
                outPacket.encodeString("");
                outPacket.encodeString("");
            }
        }

        if (mask.isInMask(DBChar.QuestComplete)) {
            boolean removeAllOldEntries = true;
            outPacket.encodeByte(removeAllOldEntries);
            List<Quest> completedSet = getCompletedQuests();
            // TMS 282: the 5th job chain (1460..1465, V matrix gate) is stored as an account quest here, but the
            // client checks it in the character's completed list, so account completions are listed too.
            List<net.swordie.ms.client.AccountQuest> accountFifthJob = new ArrayList<>();
            if (net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3 && getAccount() != null) {
                Set<Integer> own = new HashSet<>();
                for (Quest quest : completedSet) own.add(quest.getQRKey());
                for (net.swordie.ms.client.AccountQuest quest : getAccount().getCompletedQuests()) {
                    int key = quest.getQRKey();
                    if (key >= net.swordie.ms.constants.QuestConstants.V_JOB_QUEST_ADV_START && key < net.swordie.ms.constants.QuestConstants.V_JOB_QUEST_ADV_END && !own.contains(key)) {
                        accountFifthJob.add(quest);
                    }
                }
            }
            outPacket.encodeShort(completedSet.size() + accountFifthJob.size());
            for (Quest quest : completedSet) {
                outPacket.encodeInt(quest.getQRKey());
                outPacket.encodeFT(quest.getCompletedTime()); // Timestamp of completion
            }
            for (net.swordie.ms.client.AccountQuest quest : accountFifthJob) {
                outPacket.encodeInt(quest.getQRKey());
                outPacket.encodeFT(quest.getCompletedTime() != null ? quest.getCompletedTime() : net.swordie.ms.util.FileTime.currentTime());
            }
            if (!removeAllOldEntries) {
                short size = 0;
                outPacket.encodeShort(size);
                for (int i = 0; i < size; i++) {
                    outPacket.encodeInt(0); // nQRKey?
                }
            }
        }
        if (mask.isInMask(DBChar.MinigameRecord)) {
            int size = 0;
            outPacket.encodeShort(size);
            for (int i = 0; i < size; i++) {
                new MiniGameRecord().encode(outPacket);
            }
        }
        if (mask.isInMask(DBChar.CoupleRecord)) {
            net.swordie.ms.client.character.social.tms282.RingRecordService.get().encodeCharacterData265(this, outPacket); // social: ring records (3 x u16 0 when none)
        }
        if (mask.isInMask(DBChar.MapTransfer)) {
            for (int i = 0; i < 5; i++) {
                outPacket.encodeInt(999999999); // Teleport Rock
            }
            for (int i = 0; i < 10; i++) {
                outPacket.encodeInt(999999999); // Vip Teleport Rock
            }
            for (int i = 0; i < 13; i++) {
                outPacket.encodeInt(999999999); // Premium Vip Teleport Rock
            }
            for (int i = 0; i < 13; i++) {
                outPacket.encodeInt(999999999); // Hyper Teleport Rock
            }
        }
        if (mask.isInMask(DBChar.FamiliarCodex)) {
            outPacket.encodeArr(new byte[44]);
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 3; j++) {
                    outPacket.encodeInt(0);
                }
            }
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < GameConstants.FAMILIAR_BADGE_SLOTS; j++) {
                    outPacket.encodeByte(-1);
                }
            }
        }
        if (mask.isInMask(DBChar.Familiar)) {
            boolean bool = false;
            int size = 0;
            outPacket.encodeByte(bool);
            outPacket.encodeInt(size); // getFamiliars().size());
            if (bool) {
                for (Familiar familiar : getFamiliars()) {
                    familiar.encode(outPacket, this);
                }
            } else {
                for (int i = 0; i < size; i++) {
                    outPacket.encodeInt(0);
                }
            }
        }
        if (mask.isInMask(DBChar.FamiliarCodex)) {
            boolean bool = false;
            int size = 0;
            outPacket.encodeByte(bool);
            outPacket.encodeInt(size);
            if (bool) {
                for (int i = 0; i < size; i++) {
                    outPacket.encodeArr(new byte[22]);
                }
            } else {
                for (int i = 0; i < size; i++) {
                    outPacket.encodeInt(0);
                    outPacket.encodeArr(new byte[22]);
                }
            }
        }
        if (mask.isInMask(DBChar.QuestRecordEx)) {
            List<Quest> quests = getQuestsEx();
            outPacket.encodeShort(quests.size());
            for (Quest quest : quests) {
                outPacket.encodeInt(quest.getQRKey());
                outPacket.encodeString(quest.getQRValue());
            }
        }
        if (mask.isInMask(DBChar.NewYearCard)) {
            int size = 0;
            outPacket.encodeShort(size);
            for (int i = 0; i < size; i++) {
                outPacket.encodeInt(0);
                outPacket.encodeShort(0);
            }
        }

        boolean bool = true; // bNxRecordAccessAuth
        outPacket.encodeByte(bool); // new 196

        if (bool && mask.isInMask(DBChar.Unk10000000000)) {
            int size = 0;
            outPacket.encodeInt(size);
            for (int i = 0; i < size; i++) {
                outPacket.encodeInt(0);
                outPacket.encodeString("");
            }
        }
        if (mask.isInMask(DBChar.Unk100000000000)) {
            int size = 0;
            outPacket.encodeInt(size);
            for (int i = 0; i < size; i++) {
                outPacket.encodeInt(getUser().getId());
                outPacket.encodeInt(-1);
            }
        }
        if (mask.isInMask(DBChar.WildHunterInfo)) {
            if (JobConstants.isWildHunter(getAvatarData().getCharacterStat().getJob())) {
                // could make WildHunterInfo an entity for this
                WildHunterInfo whi = getWildHunterInfo();
                Quest chosenQuest = getQuestById(QuestConstants.WILD_HUNTER_JAGUAR_CHOSEN_ID);
                int toID = -1;
                if (chosenQuest == null) {
                    chosenQuest = new Quest(getId(), QuestConstants.WILD_HUNTER_JAGUAR_CHOSEN_ID, Started);
                    addQuest(chosenQuest);
                } else if (Util.isNumber(chosenQuest.getQRValue())) {
                    toID = Integer.parseInt(chosenQuest.getQRValue());
                }
                whi.setIdx((byte) toID);
                whi.setRidingType((byte) toID);
                chosenQuest.setQrValue("" + toID);
                getWildHunterInfo().encode(outPacket); // GW_WildHunterInfo::Decode
            }
        }
        if (mask.isInMask(DBChar.ZeroInfo)) {
            if (JobConstants.isZero(getAvatarData().getCharacterStat().getJob())) {
                if (getZeroInfo() == null) {
                    initZeroInfo();
                }
                getZeroInfo().encode(outPacket); // ZeroInfo::Decode
            }
        }
        if (mask.isInMask(DBChar.ShopBuyLimit)) {
            outPacket.encodeShort(0);
        }
        if (mask.isInMask(DBChar.ShopBuyLimit)) {
            outPacket.encodeShort(0);
        }
        if (mask.isInMask(DBChar.ShopBuyLimit)) {
            outPacket.encodeShort(0);
        }

        // sub_14069C7E0
        outPacket.encodeInt(0);
        // for { int, int, int, int }

        // new 263
        outPacket.encodeInt(0);

        if (mask.isInMask(DBChar.StolenSkills)) {
            if (JobConstants.isPhantom(getAvatarData().getCharacterStat().getJob())) {
                for (int i = 0; i < 16; i++) {
                    StolenSkill stolenSkill = getStolenSkillByPosition(i);
                    outPacket.encodeInt(stolenSkill == null ? 0 : stolenSkill.getSkillid());
                }
            } else {
                for (int i = 0; i < 16; i++) {
                    outPacket.encodeInt(0);
                }
            }
        }
        if (mask.isInMask(DBChar.ChosenSkills)) {
            if (JobConstants.isPhantom(getAvatarData().getCharacterStat().getJob())) {
                for (int i = 1; i <= 5; i++) { //Shifted by +1 to accomodate the Skill Management Tabs
                    ChosenSkill chosenSkill = getChosenSkillByPosition(i);
                    outPacket.encodeInt(chosenSkill == null
                            ? 0
                            : isChosenSkillInStolenSkillList(chosenSkill.getSkillId())
                            ? chosenSkill.getSkillId()
                            : 0
                    );
                }
            } else {
                for (int i = 0; i < 5; i++) {
                    outPacket.encodeInt(0);
                }
            }
        }
        if (mask.isInMask(DBChar.CharacterPotential)) {
            for (int i = 0; i < 3; i++) {
                final Set<CharacterPotential> characterPotentialSet = getPotentialsByPreset(i);
                outPacket.encodeShort(characterPotentialSet.size());
                for (CharacterPotential cp : characterPotentialSet) {
                    cp.encode(outPacket);
                }
            }
        }
        if (mask.isInMask(DBChar.SoulCollection)) {
            outPacket.encodeShort(getSoulCollection().size());
            for (Map.Entry<Integer, Integer> entry : getSoulCollection().entrySet()) {
                outPacket.encodeInt(entry.getKey());
                outPacket.encodeInt(entry.getValue());
            }
        }
        if (mask.isInMask(DBChar.Character)) {
            outPacket.encodeInt(1); // honor level, deprecated
            outPacket.encodeInt(getHonorExp()); // honor exp
        }
        if (mask.isInMask(DBChar.Unk200000000)) {
            boolean shouldIEncodeThis = true;
            outPacket.encodeByte(shouldIEncodeThis);
            if (shouldIEncodeThis) {
                short size = 0;
                outPacket.encodeShort(size);
                for (int i = 0; i < size; i++) {
                    short category = 0;
                    outPacket.encodeShort(category);
                    short size2 = 0;
                    outPacket.encodeShort(size2);
                    for (int i2 = 0; i2 < size2; i2++) {
                        outPacket.encodeInt(0); // nItemId
                        outPacket.encodeInt(0); // nCount
                    }
                }
            } else {
                short size2 = 0;
                outPacket.encodeShort(size2);
                for (int i2 = 0; i2 < size2; i2++) {
                    outPacket.encodeShort(1); // nCategory
                    outPacket.encodeInt(1302000); // nItemId
                    outPacket.encodeInt(3); // nCount
                }
            }
        }
        if (mask.isInMask(DBChar.ReturnEffectInfo)) {
            returnEffectInfo.encode(outPacket);
        }
        if (mask.isInMask(DBChar.DressUpInfo)) {
            if (JobConstants.isAngelicBuster(getJob())) {
                DressUpInfo dressUpInfo = new DressUpInfo();
                AvatarLook avatarLook = getAvatarData().getAvatarLook();
                CharacterStat cs = getAvatarData().getCharacterStat();
                if (cs.getFace() != 21074 && cs.getFace() != 21075
                        && cs.getFace() != 21174 && cs.getFace() != 21175
                        && cs.getFace() != 21274 && cs.getFace() != 21275
                        && cs.getFace() != 21374 && cs.getFace() != 21375
                        && cs.getFace() != 21474 && cs.getFace() != 21475
                        && cs.getFace() != 21574 && cs.getFace() != 21575
                        && cs.getFace() != 21674 && cs.getFace() != 21675
                        && cs.getFace() != 21774 && cs.getFace() != 21775
                        && cs.getFace() != 21874 && cs.getFace() != 21875) { // default
                    dressUpInfo.setFace(avatarLook.getFace());
                }
                if (cs.getHair() < 37240 || cs.getHair() > 37257) { // default
                    dressUpInfo.setHair(avatarLook.getHair());
                }
                if (cs.getMixBaseHairColor() != -1) { // default
                    dressUpInfo.setMixBaseHairColor(cs.getMixBaseHairColor());
                    dressUpInfo.setMixAddHairColor(cs.getMixAddHairColor());
                    dressUpInfo.setMixHairBaseProb(cs.getMixHairBaseProb());
                }
                for (Item item : getEquippedInventory().getItems()) {
                    if (item.getItemId() / 10000 == 105 && item.isCash()) {
                        dressUpInfo.setClothe(item.getItemId());
                        break;
                    }
                }
                setDressUpInfo(dressUpInfo);
            } else {
                setDressUpInfo(new DressUpInfo());
            }
            getDressUpInfo().encode(outPacket); // GW_DressUpInfo::Decode
        }
        if (mask.isInMask(DBChar.ActiveDamageSkin)) {
            outPacket.encodeInt(getActiveDamageSkin().getDamageSkinID());
            outPacket.encodeInt(getPremiumDamageSkin().getDamageSkinID());
            outPacket.encodeFT(getActiveDamageSkin().getActivateTime());
            outPacket.encodeString(StringData.getItemStringById(getActiveDamageSkin().getItemID()));
            outPacket.encodeInt(getActiveDamageSkin().getDamageSkinID());
        }

        if (mask.isInMask(DBChar.MemorialCubeInfo)) {
            outPacket.encodeByte(false); // MemorialCube::Decode
        }

        if (mask.isInMask(DBChar.MemorialFlameInfo)) {
            outPacket.encodeByte(false); // MemorialFlame::Decode
        }

        if (mask.isInMask(DBChar.LikePoint)) {
            new LikePoint().encode(outPacket);
        }
        if (mask.isInMask(DBChar.RunnerGameRecord)) {
            new RunnerGameRecord().encode(outPacket); // RunnerGameRecord::Decode
        }
        if (mask.isInMask(DBChar.Unk8000000000000)) {
            int size = 0;
            outPacket.encodeInt(0);
            for (int i = 0; i < size; i++) {
                outPacket.encodeInt(-1);
                outPacket.encodeByte(-1);
                outPacket.encodeByte(-1);
                outPacket.encodeByte(-1);
            }
            outPacket.encodeInt(0);
            outPacket.encodeLong(0);
            outPacket.encodeByte(0);
            outPacket.encodeByte(0);
        }
        List<AccountQuest> quests = getWorldShareQuests();
        outPacket.encodeShort(quests.size());
        for (AccountQuest quest : quests) {
            outPacket.encodeInt(quest.getQRKey());
            outPacket.encodeString(quest.getQRValue());
        }
        if (mask.isInMask(DBChar.MonsterCollection)) {
            Set<MonsterCollectionExploration> mces = getAccount().getMonsterCollection().getMonsterCollectionExplorations();
            outPacket.encodeShort(mces.size());
            for (MonsterCollectionExploration mce : mces) {
                outPacket.encodeInt(mce.getPosition());
                outPacket.encodeString(mce.getValue(true));
            }
        }
        outPacket.encodeByte(1);
        outPacket.encodeShort(0);
        int sizeInt = 0;
        // CharacterData::DecodeTextEquipInfo
        outPacket.encodeInt(sizeInt);
        for (int i = 0; i < sizeInt; i++) {
            outPacket.encodeInt(0);
            outPacket.encodeString("");
        }
        if (mask.isInMask(DBChar.Unk10000000000000)) { // new 196
            int size = 0;
            outPacket.encodeShort(size);
            for (int i = 0; i < size; i++) {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
            }
        }
        if (mask.isInMask(DBChar.VMatrix)) {
            encodeMatrixSkills(outPacket);
        }
        // Hexa skills:
        encodeHexaSkills(outPacket);
        // Hexa stats:
        encodeHexaStats(outPacket);
        // Hexa skills 2:
        outPacket.encodeInt(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(0);
        // Legion Artifact:
        encodeUnionArtifacts(outPacket);
        // Unk
        int size = 0;
        outPacket.encodeInt(size);
        for (int i = 0; i < size; i++) {
            outPacket.encodeInt(0);
        }

        if (mask.isInMask(DBChar.Achievement)) {
            outPacket.encodeInt(getAccId());
            outPacket.encodeInt(getId());
            outPacket.encodeInt(0);
            outPacket.encodeInt(-1);
            outPacket.encodeInt(0); // encode Later
            outPacket.encodeInt(getAccount().getAchievementRanks().size());
            for (AchievementRank ar : getAccount().getAchievementRanks()) {
                ar.encode(outPacket);
            }
        }
        if (mask.isInMask(DBChar.ItemSlotEtc)) {
            encodeBossReward(outPacket);
        }
        for (int i = 0; i < 3; i++) {
            outPacket.encodeByte(0);
            outPacket.encodeByte(0);
            for (int j = 0; j < 42; j++) {
                bool = false;
                outPacket.encodeByte(bool);
                if (bool) {
                    outPacket.encodeByte(0);
                    outPacket.encodeByte(0);

                    outPacket.encodeInt(0);

                    outPacket.encodeByte(0);
                    outPacket.encodeByte(0);
                    outPacket.encodeByte(0);

                }
            }
        }
        if (mask.isInMask(DBChar.Familiar)) {
            // Emoticons / EmoticonTabs: i32 0, i32 0, u16 0 unless emoticon.encode.enabled
            net.swordie.ms.client.character.emoticon.EmoticonService.get().encodeCharacterData(this, outPacket);
        }

        outPacket.encodeInt(0);

        if (mask.isInMask(DBChar.Unk0x4000000000000000)) {
            encodeCommerceRecord(outPacket);
        }

        if (mask.isInMask(DBChar.NewYearCard)) {
            outPacket.encodeByte(0);
        }
        if (mask.isInMask(DBChar.NewYearCard)) {
            size = 0;
            outPacket.encodeInt(size);
            for (int i = 0; i < size; i++) {
                // sub
                outPacket.encodeShort(0);
                outPacket.encodeShort(0);
            }
            size = 0;
            outPacket.encodeInt(size);
            for (int i = 0; i < size; i++) {
                // sub
                outPacket.encodeShort(0);
                outPacket.encodeInt(0);
            }
        }
        if (mask.isInMask(DBChar.Unk20000000000)) {
            size = 0;
            outPacket.encodeShort(size);
            for (int i = 0; i < size; i++) {
                outPacket.encodeShort(233);
                outPacket.encodeShort(543);
            }
        }
        if (mask.isInMask(DBChar.RedLeafInfo)) {
            // red leaf information
            outPacket.encodeInt(getAccId());
            outPacket.encodeInt(getId());
            outPacket.encodeInt(4);
            outPacket.encodeInt(0);
            outPacket.encodeArr(new byte[32]); // real
        }
    }

    public void encodeMatrixSkills(OutPacket outPacket) {
        var matrixRecords = getSortedMatrixCores();
        outPacket.encodeInt(matrixRecords.size());
        for (var skill : matrixRecords) {
            skill.encode(outPacket);
        }
        outPacket.encodeInt(MatrixConstants.MAX_NODE_SLOTS);
        for (int i = 0; i < MatrixConstants.MAX_NODE_SLOTS; i++) {
            var mr = getMatrixCoreByPosition(i);
            var ms = getMatrixSlotByPosition(i);
            outPacket.encodeInt(mr != null ? matrixRecords.indexOf(mr) : -1); // nodeID
            outPacket.encodeInt(i); // matrix Slot position
            outPacket.encodeInt(ms != null ? ms.getLevel() : 0); // slot enhancement level
            outPacket.encodeByte(ms != null && ms.isUnLock()); // bShow Unlocked Symbol
        }
    }

    public void encodeBossReward(OutPacket outPacket) {
        List<Item> items = new ArrayList<>();
        for (Item item : getEtcInventory().getItems()) {
            if (GameConstants.isIntensePowerCrystal(item.getItemId()) && !item.getDateExpire().isExpired()) {
                items.add(item);
            }
        }
        outPacket.encodeInt(items.size());
        for (Item item : items) {
            outPacket.encodeLong(item.getId());
            outPacket.encodeInt(item.getBossRewardID()); // 9210000
            outPacket.encodeInt(item.getPartySize());
            outPacket.encodeLong(item.getPrice());
            outPacket.encodeLong(0);
            outPacket.encodeFT(FileTime.currentTime()); // time
        }
    }

    private void encodeCommerceRecord(OutPacket outPacket) {
        boolean bool = false;
        outPacket.encodeByte(bool);
        if (bool) {
            // sub_905BD0
            outPacket.encodeByte(0);
            outPacket.encodeInt(1);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeFT(FileTime.MIN_TIME());
        }
        int size = 0;
        outPacket.encodeShort(size);
        for (int i = 0; i < size; i++) {
            // sub_9059E0
            outPacket.encodeByte(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
        }
        size = 0;
        outPacket.encodeShort(size);
        for (int i = 0; i < size; i++) {
            // sub_905D50
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeFT(FileTime.MIN_TIME());
        }
    }

    private void encodeEquips(OutPacket outPacket) {
        boolean onlyEquipped = false;
        List<Equip> equippedItems = getEquippedInventory().getItems().stream().map(e -> (Equip) e).sorted(Comparator.comparingInt(Item::getBagIndex)).toList();
        List<Equip> equipItems = getEquipInventory().getItems().stream().map(e -> (Equip) e).filter(Equip::hasPotential).sorted(Comparator.comparingInt(Item::getBagIndex)).toList();

        outPacket.encodeByte(onlyEquipped); // v263

        // Normal equipped items
        for (Equip item : equippedItems) {
            if (item.getBagIndex() > BodyPart.BPBase.getVal() && item.getBagIndex() < BodyPart.BPEnd.getVal() && !item.isCash()) {
                outPacket.encodeShort(item.getBagIndex());
                item.encode(outPacket);
            }
        }
        outPacket.encodeShort(0); // v263
        if (!onlyEquipped) {
            // Equip inventory
            for (Equip item : equipItems) {
                if (!item.isCash() && item.hasPotential()) {
                    outPacket.encodeShort(item.getBagIndex());
                    item.encode(outPacket);
                }
            }
            outPacket.encodeShort(0);
        }

        for (Equip item : equippedItems) { // v263 | 1
            if (item.getBagIndex() >= BodyPart.EvanBase.getVal() && item.getBagIndex() < BodyPart.EvanEnd.getVal()) {
                outPacket.encodeShort(item.getBagIndex());
                item.encode(outPacket);
            }
        }
        outPacket.encodeShort(0); // 1

        for (Equip item : equippedItems) { // v263 | 2
            if (item.getBagIndex() >= BodyPart.MechBase.getVal() && item.getBagIndex() < BodyPart.MechEnd.getVal()) {
                outPacket.encodeShort(item.getBagIndex());
                item.encode(outPacket);
            }
        }
        outPacket.encodeShort(0); // 2

        for (Equip item : equippedItems) { // v263 | 3
            if (item.getBagIndex() >= BodyPart.BitsBase.getVal() && item.getBagIndex() < BodyPart.BitsEnd.getVal()) {
                outPacket.encodeShort(item.getBagIndex());
                item.encode(outPacket);
            }
        }
        outPacket.encodeShort(0); // 3

        for (Equip item : equippedItems) { // v263 | 4
            if (item.getBagIndex() >= BodyPart.MBPBase.getVal() && item.getBagIndex() < BodyPart.MBPEnd.getVal()) {
                outPacket.encodeShort(item.getBagIndex());
                item.encode(outPacket);
            }
        }
        outPacket.encodeShort(0); // 4

        for (Equip item : equippedItems) { // v263 | 5
            if (item.getBagIndex() >= BodyPart.ArcBase.getVal() && item.getBagIndex() < BodyPart.ArcEnd.getVal()) {
                outPacket.encodeShort(item.getBagIndex());
                item.encode(outPacket);
            }
        }
        outPacket.encodeShort(0); // 5

        for (Equip item : equippedItems) { // v263 | 6
            if (item.getBagIndex() >= BodyPart.AUSBase.getVal() && item.getBagIndex() < BodyPart.AUSEnd.getVal()) {
                outPacket.encodeShort(item.getBagIndex());
                item.encode(outPacket);
            }
        }
        outPacket.encodeShort(0); // 6

        for (Equip item : equippedItems) { // v263 | 7
            if (item.getBagIndex() >= BodyPart.HakuStart.getVal() && item.getBagIndex() < BodyPart.HakuEnd.getVal()) {
                outPacket.encodeShort(item.getBagIndex());
                item.encode(outPacket);
            }
        }
        outPacket.encodeShort(0); // 7

        // v263 | 8
        outPacket.encodeShort(0);

        // v263 | 9
        outPacket.encodeShort(0);

        // v263 | 10
        outPacket.encodeShort(0);

        for (Equip item : equippedItems) { // v263 | 11
            if (item.getBagIndex() >= BodyPart.TotemBase.getVal() && item.getBagIndex() < BodyPart.TotemEnd.getVal()) {
                outPacket.encodeShort(item.getBagIndex());
                item.encode(outPacket);
            }
        }
        outPacket.encodeShort(0); // 1

        // v263 | 12
        outPacket.encodeShort(0);

        // sub_14066B780
        outPacket.encodeShort(0); // 20001~20048
        outPacket.encodeShort(0); // 20049~20051

        outPacket.encodeByte(onlyEquipped); // ?

        // Cash equipped items
        for (Equip item : equippedItems) {
            if (item.getBagIndex() >= BodyPart.CBPBase.getVal() && item.getBagIndex() <= BodyPart.CBPEnd.getVal()) {
                outPacket.encodeShort(item.getBagIndex() - 100);
                item.encode(outPacket);
            }
        }
        outPacket.encodeShort(0);
        if (!onlyEquipped) {
            for (Item item : getDecorationInventory().getItems()) {
                Equip equip = (Equip) item;
                outPacket.encodeShort(equip.getBagIndex());
                equip.encode(outPacket);
            }
            outPacket.encodeShort(0);
        }

        for (Equip item : equippedItems) { // v263 | 13
            if (item.getBagIndex() >= BodyPart.APBase.getVal() && item.getBagIndex() < BodyPart.APEnd.getVal()) {
                outPacket.encodeShort(item.getBagIndex());
                item.encode(outPacket);
            }
        }
        outPacket.encodeShort(0);

        for (Equip item : equippedItems) { // v263 | 14
            if (item.getBagIndex() >= BodyPart.DUBase.getVal() && item.getBagIndex() < BodyPart.DUEnd.getVal()) {
                outPacket.encodeShort(item.getBagIndex());
                item.encode(outPacket);
            }
        }
        outPacket.encodeShort(0);

        for (Equip item : equippedItems) { // v263 | 15
            if (item.getBagIndex() >= BodyPart.ZeroBase.getVal() && item.getBagIndex() < BodyPart.ZeroEnd.getVal()) {
                outPacket.encodeShort(item.getBagIndex());
                item.encode(outPacket);
            }
        }
        outPacket.encodeShort(0);

        // v263 | 16
        outPacket.encodeShort(0); // cashPreset?

        // v263 | 17
        outPacket.encodeShort(0);

        // v263 | 18
        outPacket.encodeShort(0);
    }

    public void initialize() {
        if (client == null) return;
        long start = System.currentTimeMillis();

        ScriptManagerImpl sm = getScriptManager();

        LocalDateTime now = LocalDateTime.now();
        write(WvsContext.hourChange((short) now.getDayOfWeek().getValue(), (short) now.getHour()));

        int chuc = 0;
        int arc = 0;
        int aut = 0;
        for (Item item : getEquippedInventory().getItems()) {
            int itemId = item.getItemId();
            if (ItemConstants.isSymbol(itemId)) {
                var equip = (Equip) item;
                var symbol = equip.getSymbol();
                if (symbol.getLevel() == 0) {
                    symbol.setLevel(1);
                    symbol.setInc((short) (ItemConstants.isArcaneSymbol(itemId) ? 30 : 10));
                    if (JobConstants.isDemonAvenger(getJob())) {
                        if (ItemConstants.isArcaneSymbol(itemId)) {
                            equip.setiMaxHp((short) (itemId == 1712000 ? 4200 : 5250));
                        } else {
                            equip.setiMaxHp((short) (10500));
                        }
                    } else if (JobConstants.isXenon(getJob())) {
                        if (ItemConstants.isArcaneSymbol(itemId)) {
                            equip.setiStr((short) 117);
                            equip.setiDex((short) 117);
                            equip.setiLuk((short) 117);
                        } else {
                            equip.setiStr((short) 240);
                            equip.setiDex((short) 240);
                            equip.setiLuk((short) 240);
                        }
                    } else {
                        if (ItemConstants.isArcaneSymbol(itemId)) {
                            equip.setBaseStat(calculateMainStatForChar(), 300);
                        } else {
                            equip.setBaseStat(calculateMainStatForChar(), 500);
                        }
                    }
                }
                else if (symbol.getLevel() != 0 && symbol.getInc() != 0) {
                    if (JobConstants.isDemonAvenger(getJob())) {
                        if (equip.getiMaxHp() == 0) {
                            if (ItemConstants.isArcaneSymbol(itemId)) {
                                equip.setiMaxHp((short) (itemId == 1712000 ? 4200 : 5250));
                            } else {
                                equip.setiMaxHp((short) (10500));
                            }
                        }
                    } else if (JobConstants.isXenon(getJob())) {
                        if (equip.getiStr() == 0) {
                            if (ItemConstants.isArcaneSymbol(itemId)) {
                                equip.setiStr((short) 117);
                            } else {
                                equip.setiStr((short) 240);
                            }
                        }
                        if (equip.getiDex() == 0) {
                            if (ItemConstants.isArcaneSymbol(itemId)) {
                                equip.setiDex((short) 117);
                            } else {
                                equip.setiDex((short) 240);
                            }
                        }
                        if (equip.getiLuk() == 0) {
                            if (ItemConstants.isArcaneSymbol(itemId)) {
                                equip.setiLuk((short) 117);
                            } else {
                                equip.setiLuk((short) 240);
                            }
                        }
                    } else {
                        if (equip.getBaseStat(calculateMainStatForChar()) == 0) {
                            if (ItemConstants.isArcaneSymbol(itemId)) {
                                equip.setBaseStat(calculateMainStatForChar(), 300);
                            } else {
                                equip.setBaseStat(calculateMainStatForChar(), 500);
                            }
                        }
                    }
                }
                write(WvsContext.addItemToInventory(item));
                if (ItemConstants.isArcaneSymbol(itemId)) {
                    arc += ((Equip) item).getSymbol().getInc();
                } else if (ItemConstants.isSacredSymbol(itemId)) {
                    aut += ((Equip) item).getSymbol().getInc();
                }
            }
            chuc += ((Equip) item).getChuc();
        }
        var characterStat = getAvatarData().getCharacterStat();
        if (chuc != 0) characterStat.setChuc(chuc);
        if (arc != 0) characterStat.setArc(arc);
        if (aut != 0) characterStat.setAut(aut);

        for (Item item : getEquipInventory().getItems()) {
            if (item instanceof Equip equip && !equip.hasPotential()) {
                if (equip.isCash()) {
                    continue;
                }
                write(WvsContext.addItemToInventory(item));
            }
        }

        if (getLevel() >= 200) {
            if (getMatrixSlot().size() < MatrixConstants.MAX_NODE_SLOTS) {
                for (int i = getMatrixSlot().size(); i < MatrixConstants.MAX_NODE_SLOTS; i++) {
                    getMatrixSlot().add(new MatrixSlot(getId(), i));
                }
            }
            for (int i = 0; i < MatrixConstants.MAX_NODE_SLOTS; i++) {
                MatrixCore mc = getMatrixCoreByPosition(i);
                if (mc != null && mc.getState().getVal() != MatrixStateType.DISASSEMBLED.getVal()) {
                    if (mc.getState().getVal() == MatrixStateType.ACTIVE.getVal()) {
                        MatrixHandler.setNodeSkill(this, mc, MatrixUpdateType.Activate);
                    }
                }
            }
            //write(WvsContext.updateVMatrix(this, true, MatrixUpdateType.Update.getVal(), 0));
            var itemID = hasQuest(1473) ? Integer.parseInt(getQRValueByKey(1473, "itemID")) : 0;
            if (ScriptManagerImpl.GAME_SCRIPTS_ENABLED && itemID >= 2435734 && itemID <= 2435736) {
                var questID = 1470 + itemID - 2435734;
                if (!hasQuestCompleted(questID + 4)) {
                    sm.openUIWithOption(1128, itemID);
                }
            }
        }
        if (getLevel() >= 260) {
            if (hasQuestCompleted(1488)) {
                completeQuest(1488);
            }
            if (ScriptManagerImpl.GAME_SCRIPTS_ENABLED && hasQuest(101563)) {
                int val = Integer.parseInt(getQRValueByKey(101563, "soErUI"));
                if (val == 1) {
                    sm.openUI(UIType.UI_SOL_ERDA);
                }
            }
        }

        // Xử lý từ quest thứ MAX_QUESTS_ENCODE trở đi:
        int maxSize = QuestConstants.MAX_QUESTS_ENCODE;
        List<Quest> inProgressSet = getQuestsInProgress();
        int remainingInProgressSize = inProgressSet.size() - maxSize;
        if (remainingInProgressSize > 0) {
            List<Quest> subInprogressList = getSortedQuestSublist(inProgressSet, maxSize, remainingInProgressSize);
            for (Quest quest : subInprogressList) {
                write(WvsContext.questRecordMessage(quest));
            }
        }
        for (AccountQuest quest : getAccount().getQuests().values()) {
            write(WvsContext.questRecordMessage(quest));
            write(WvsContext.questRecordExMessage(quest));
            if (quest.getQRKey() < 1000) {
                write(WvsContext.questWorldShareMessage(quest));
            }
        }
        //write(WvsContext.MVPInfo());
        write(WvsContext.updateAchievement(getAccount().getAchievementDatas()));

        write(WvsContext.setBuyEquipExt());

        write(WvsContext.setPotionDiscountRate((byte) 0));

        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                && getJobHandler() instanceof net.swordie.ms.client.jobs.legend.Aran aran)
            write(WvsContext.modComboResponse(aran.getCombo()));
        else write(WvsContext.modComboResponse(0));

        write(UserLocal.setMonsterDebuffMark(null));

        calculateBulletIDForAttack(1);

        String presetStr = getQRValueByKey(QuestConstants.CHARACTER_POTENTIAL_PRESET, "potential");
        int preset = Integer.parseInt(presetStr != null ? presetStr : "0");
        write(WvsContext.characterPotentialChangePreset(true, preset));

        presetStr = getQRValueByKey(QuestConstants.LINK_SKILL_PRESET, "preset");
        preset = Integer.parseInt(presetStr != null ? presetStr : "0");
        write(WvsContext.linkedSkillInfo((byte) 0, getLinkedSkills(0)));
        write(WvsContext.linkedSkillInfo((byte) 1, getLinkedSkills(1)));
        write(WvsContext.linkedSkillInfo((byte) 2, getLinkedSkills(2)));
        write(WvsContext.linkedSkillInfo(1, (byte) preset, null));

        write(UserLocal.setInGameDirectionMode(false, false, false, true));

        write(WvsContext.skillSquenceBuffsInformation());

        write(WvsContext.sendMigrateSecurityResult(getWorld().getWorldId(), getId()));

        write(UserLocal.sendClientResolution());

        write(WvsContext.getRegDate(FileTime.fromEpochMillis(creationTime)));

        write(WvsContext.updateTime());

        write(FieldPacket.setQuestClear());

        write(FieldPacket.setTamingMobInfo(getId(), getTamingMobLevel(), getTamingMobExp(), getTamingMobFatigue(), false));

        write(UserLocal.isUniverse(false));

        write(WvsContext.setMaplePoint(getUser().getMaplePoints()));

        write(ShopDlg.shopCrystal(true));
        String time = FileTime.currentTime().toYYYYMMDD();
        if (!hasQuest(QuestConstants.CRYSTAL_ALL)) {
            int maxAll = QuestConstants.CRYSTAL_MAX_ALL;
            createQuestWithQRValue(QuestConstants.CRYSTAL_ALL, "count="+maxAll+";time="+time+";max="+maxAll+";type=2");
        }
        if (!hasQuest(QuestConstants.CRYSTAL_WEEKLY)) {
            int maxWeekly = QuestConstants.CRYSTAL_MAX_WEEKLY;
            createQuestWithQRValue(QuestConstants.CRYSTAL_WEEKLY, "time="+time+";max="+maxWeekly);
        }

        write(WvsContext.unionArtifactInformation());

        write(WvsContext.hexaMatrixInformation());

        write(WvsContext.unk372());

        write(WvsContext.userCharacterListResult(getAccount()));

        if (hasQuest(100716)) {
            int remain = Integer.parseInt(getQRValueByKey(100716, "remain"));
            if (remain > 0 && this.deathPenaltyTimer == null) {
                this.deathPenaltyTimer = getTimer().addFixedRateEvent(this::handleExpDropPenalty, 0, 1000L, remain);
            }
        }
        if (!hasQuest(QuestConstants.SOUL_EFFECT_SHOW)) {
            createQuestWithQRValue(QuestConstants.SOUL_EFFECT_SHOW, "effect=1");
            setSoulEffect((byte) 1);
            getField().broadcast(UserPacket.SetSoulEffect(getId(), true));
        } else {
            byte soulEff = Byte.parseByte(getQRValueByKey(QuestConstants.SOUL_EFFECT_SHOW, "effect"));
            setSoulEffect(soulEff);
            getField().broadcast(UserPacket.SetSoulEffect(getId(), soulEff != 0));
        }

        calculateCombatPower();

        if (ScriptManagerImpl.GAME_SCRIPTS_ENABLED) DailyGift.init(this);

        write(WvsContext.sendExtraSystemResult(-1861917715, (int) (System.currentTimeMillis() - 668_611_176L)));

        write(WvsContext.claimSVRStatusChanged(true));

        write(WvsContext.allStackSkillsResult());

        write(WvsContext.sessionValue("kill_count", "0"));

        write(WvsContext.achievementUpdate((byte) 0));

        write(WvsContext.passiveDebuffSkillRequest(SkillConstants.passiveDebuffs));

        initBlessingSkills();

        if (ScriptManagerImpl.GAME_SCRIPTS_ENABLED) initEventNameTag();

        initKeyBoards();

        initHyperRockFields();

        //c.getMonsterCollection().init(chr);

        //chr.checkAndDeletedTotemQuests();

        if (ScriptManagerImpl.GAME_SCRIPTS_ENABLED) {
            initWeeklyReset();
            initDailyReset();
        }

        if (ScriptManagerImpl.GAME_SCRIPTS_ENABLED && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) initMVPDailyGift();

        net.swordie.ms.client.character.tms282.Tms282JobAdvance.initializeAfterMigrate(this);

        handledBuffDataList();

        sm.initCharacterPotential(12394);
        sm.initCharacterPotential(12395);
        sm.initCharacterPotential(12396);

        deleteSkillsCoolTimesFromSQL();

        if (ScriptManagerImpl.GAME_SCRIPTS_ENABLED && JobConstants.isArk(getJob())) {
            for (int qrKey = 34940; qrKey < 34960; qrKey++) {
                if (!sm.hasQuestCompleted(qrKey)) {
                    sm.completeQuestNoRewards(qrKey);
                }
            }
        } else if (ScriptManagerImpl.GAME_SCRIPTS_ENABLED && JobConstants.isCadena(getJob())) {
            for (int qrKey = 34600; qrKey < 34650; qrKey++) {
                if (!sm.hasQuestCompleted(qrKey)) {
                    sm.completeQuestNoRewards(qrKey);
                }
            }
        } else if (ScriptManagerImpl.GAME_SCRIPTS_ENABLED && JobConstants.isIllium(getJob())) {
            for (int qrKey = 34900; qrKey < 34905; qrKey++) {
                if (!sm.hasQuestCompleted(qrKey)) {
                    sm.completeQuestNoRewards(qrKey);
                }
            }
        } else if (ScriptManagerImpl.GAME_SCRIPTS_ENABLED && JobConstants.isMoXuan(getJob())) {
            for (int qrKey = 65920; qrKey < 65971; qrKey++) {
                if (!sm.hasQuestCompleted(qrKey)) {
                    sm.completeQuestNoRewards(qrKey);
                }
            }
        }

        // Event below
        if (ScriptManagerImpl.GAME_SCRIPTS_ENABLED) {
            if (EventConstants.HYPER_BURNING_MAX) {
                if (!hasQuest(102425)) {
                    createQuestWithQRValue(102425, "start=1;dialog=1;npc=0");
                }
                if (!hasSkill(MYSTICAL_POWER_OF_THE_HAT)) {
                    addSkill(MYSTICAL_POWER_OF_THE_HAT, 1, 1);
                }
                sendPopupSay(9010000, "你已使用 #r究極燃燒 MAX#k 角色登入。\r\n" +
                        "請體驗不受限制的成長過程\r\n" +
                        "通過 #b等級提升 1+4#k 效果\r\n" +
                        "一路成長至 #rLv. 260#k!");
            } else {
                if (hasSkill(MYSTICAL_POWER_OF_THE_HAT)) {
                    removeSkill(MYSTICAL_POWER_OF_THE_HAT);
                }
            }
        }

        dispose();

        System.out.printf("Completed char data in %dms%n", System.currentTimeMillis() - start);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Char && ((Char) other).getId() == getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    private String getBlessingOfEmpress() {
        return blessingOfEmpress;
    }

    public void setBlessingOfEmpress(String blessingOfEmpress) {
        this.blessingOfEmpress = blessingOfEmpress;
    }

    private String getBlessingOfFairy() {
        return blessingOfFairy;
    }

    public void setBlessingOfFairy(String blessingOfFairy) {
        this.blessingOfFairy = blessingOfFairy;
    }

    public int getCombatOrders() {
        return combatOrders;
    }

    public void setCombatOrders(int combatOrders) {
        this.combatOrders = combatOrders;
    }

    public List<ItemPot> getItemPots() {
        return null;
    }

    public void setItemPots(List<ItemPot> itemPots) {
        this.itemPots = itemPots;
    }

    public List<Pet> getPets() {
        return pets;
    }

    public void setPets(List<Pet> pets) {
        this.pets = pets;
    }

    public void setPetLoot(final boolean status) {
        this.petLoot = status;
    }

    public boolean getPetLoot() {
        return this.petLoot;
    }

    public List<FriendRecord> getFriendRecords() {
        return friendRecords;
    }

    public void setFriendRecords(List<FriendRecord> friendRecords) {
        this.friendRecords = friendRecords;
    }

    public long getMoney() {
        return getAvatarData().getCharacterStat().getMoney();
    }

    public List<ExpConsumeItem> getExpConsumeItems() {
        return expConsumeItems;
    }

    public void setExpConsumeItems(List<ExpConsumeItem> expConsumeItems) {
        this.expConsumeItems = expConsumeItems;
    }

    public List<MonsterBattleMobInfo> getMonsterBattleMobInfos() {
        return monsterBattleMobInfos;
    }

    public void setMonsterBattleMobInfos(List<MonsterBattleMobInfo> monsterBattleMobInfos) {
        this.monsterBattleMobInfos = monsterBattleMobInfos;
    }

    public MonsterBattleLadder getMonsterBattleLadder() {
        return monsterBattleLadder;
    }

    public void setMonsterBattleLadder(MonsterBattleLadder monsterBattleLadder) {
        this.monsterBattleLadder = monsterBattleLadder;
    }

    public MonsterBattleRankInfo getMonsterBattleRankInfo() {
        return monsterBattleRankInfo;
    }

    public void setMonsterBattleRankInfo(MonsterBattleRankInfo monsterBattleRankInfo) {
        this.monsterBattleRankInfo = monsterBattleRankInfo;
    }

    public List<Inventory> getInventories() {
        return new ArrayList<>(Arrays.asList(getEquippedInventory(), getEquipInventory(),
                getConsumeInventory(), getEtcInventory(), getInstallInventory(), getCashInventory(), getDecorationInventory()));
    }

    public Inventory getInventoryByType(InvType invType) {
        return switch (invType) {
            case EQUIPPED -> getEquippedInventory();
            case EQUIP -> getEquipInventory();
            case CONSUME -> getConsumeInventory();
            case ETC -> getEtcInventory();
            case INSTALL -> getInstallInventory();
            case CASH -> getCashInventory();
            case DECORATION -> getDecorationInventory();
        };
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public int getFieldID() {
        return (int) getAvatarData().getCharacterStat().getPosMap();
    }

    public void setFieldID(int fieldID) {
        getAvatarData().getCharacterStat().setPosMap(fieldID);
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Field getField() {
        return field;
    }

    public net.swordie.ms.client.character.tms282.Tms282RuneRuntime.State getRuneChallengeState() { return runeChallengeState; }
    public void setRuneChallengeState(net.swordie.ms.client.character.tms282.Tms282RuneRuntime.State state) { runeChallengeState = state; }

    public void setField(Field field) {
        net.swordie.ms.client.jobs.flora.Tms282AetherGuard.reset(this, System.currentTimeMillis());
        net.swordie.ms.client.jobs.flora.Tms282AdeleShardbreaker.clear(this);
        net.swordie.ms.client.jobs.adventurer.thief.Tms282BladeTempest.reset(this);
        net.swordie.ms.client.jobs.cygnus.BlazeWizard.stopInferno(this);
        net.swordie.ms.client.character.tms282.Tms282RuneRuntime.onFieldChange(this);
        net.swordie.ms.client.character.tms282.Tms282DragonMaster.onFieldChange(this);
        net.swordie.ms.client.jobs.resistance.Tms282OmegaBlaster.reset(this);
        net.swordie.ms.client.jobs.Jianghu.Tms282HowlingStorm.reset(this);
        net.swordie.ms.client.character.tms282.Tms282LoadedDice.clear(this);
        net.swordie.ms.client.character.tms282.Tms282ProjectileState.clear(this);
        net.swordie.ms.handlers.login.tms282.Tms282SkillProcessLifecycleHandlers.onFieldChange(this);
        if (getJobHandler() instanceof net.swordie.ms.client.jobs.flora.Adele adele) adele.onTms282FieldChange();
        if (getJobHandler() instanceof net.swordie.ms.client.jobs.resistance.Xenon xenon) xenon.onTms282FieldChange();
        if (getJobHandler() instanceof net.swordie.ms.client.jobs.legend.Aran aran
                && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) aran.getPeriodicCombo().clear();
        if (getJobHandler() instanceof net.swordie.ms.client.jobs.flora.Illium illium) {
            illium.clearTms282Javelins();
            illium.clearTms282BatteryTimer();
        }
        if (getJobHandler() instanceof net.swordie.ms.client.jobs.sengoku.Kanna kanna
                && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) kanna.getOniController().leaveField();
        if (getJobHandler() instanceof net.swordie.ms.client.jobs.resistance.WildHunter hunter
                && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) hunter.predatorsLeap().clear();
        net.swordie.ms.client.character.skills.temp.SecondAtomLockOnStat.clear(this, 0);
        net.swordie.ms.client.character.skills.GuidedBullet.clear(this, 0);
        this.field = field;
        setFieldID(field.getId());
    }

    public short getJob() {
        return getAvatarData().getCharacterStat().getJob();
    }

    /**
     * Sets the job of this Char with a given id. Does nothing if the id is invalid.
     * If it is valid, will set this Char's job, add all Skills that the job should have by default,
     * and sends the info to the client.
     */
    public void setJob(int id) {
        JobConstants.JobEnum job = JobConstants.JobEnum.getJobById((short) id);
        if (job == null) {
            chatMessage("無法設定未知的職業 ID " + id);
            return;
        }
        getAvatarData().getCharacterStat().setJob(id);
        getAvatarData().getAvatarLook().setJob(id);
        setJobHandler(JobManager.getJobById((short) id, this));
        if (getField() != null) getField().broadcast(UserRemote.effect(getId(), Effect.changeJobEffect()), this);

        int subId = (id == 2210) ? 2200 : (id == 2212) ? 2211 : (id == 2218) ? 2217 : 0;
        List<Skill> newSkills = JobConstants.isSpecialEventJob((short) id)
                ? net.swordie.ms.client.jobs.special.SpecialJobProgression.getLearnableSkills((short) id, getLevel())
                : SkillData.getSkillsByJob((short) (subId > 0 ? subId : id));
        newSkills.forEach(skill -> {
            net.swordie.ms.client.character.tms282.Tms282SkillLearning.prepareInitialLevel(this, skill);
            addSkill(skill, true);
        });
        write(WvsContext.changeSkillRecordResult(newSkills, true, false, false));

        notifyChanges();
    }

    /**
     * Sets the SP to the current job level.
     *
     * @param num The new SP amount.
     */
    public void setSpToCurrentJob(int num) {
        CharacterStat cs = getAvatarData().getCharacterStat();
        if (JobConstants.isExtendSpJob(getJob())) {
            byte jobLevel = (byte) JobConstants.getJobLevel(getJob());
            cs.getExtendSP().setSpToJobLevel(jobLevel, num);
        } else {
            cs.setSp(num);
        }
    }

    /**
     * Sets the SP to the current job level.
     *
     * @param num The new SP amount.
     */
    public void addSpToSpecificJob(short job, int num) {
        CharacterStat cs = getAvatarData().getCharacterStat();
        if (JobConstants.isExtendSpJob(job)) {
            byte jobLevel = (byte) JobConstants.getJobLevel(job);
            num += cs.getExtendSP().getSpByJobLevel(jobLevel);
            cs.getExtendSP().setSpToJobLevel(jobLevel, num);
        } else {
            cs.setSp(num);
        }
    }

    /**
     * Sets the SP to the job level according to the current level.
     *
     * @param num The amount of SP to add
     */
    public void addSpToJobByCurrentLevel(int num) {
        CharacterStat cs = getAvatarData().getCharacterStat();
        if (JobConstants.isExtendSpJob(getJob())) {
            byte jobLevel = (byte) JobConstants.getJobLevelByCharLevel(getJob(), getLevel(), getSubJob());
            num += cs.getExtendSP().getSpByJobLevel(jobLevel);
            getAvatarData().getCharacterStat().getExtendSP().setSpToJobLevel(jobLevel, num);
            //chatMessage("Extend SP Job: " + jobLevel + " " + num + " Job: "  + getJob() + " Level: " + getLevel());
        } else {
            num += cs.getSp();
            getAvatarData().getCharacterStat().setSp(num);
            //chatMessage("Character Stat SP: " + num);
        }
    }

    public Collection<Skill> getSkills() {
        return skills.values();
    }

    public void setSkills(Int2ObjectMap<Skill> skills) {
        this.skills = skills;
    }

    public List<Integer> getLinked_LinkSkillIDs() {
        List<Integer> linked_LinkSkill = new ArrayList<>();
        for (var entry : skills.int2ObjectEntrySet()) {
            if (SkillConstants.getOriginalOfLinkedSkill(entry.getValue().getSkillId()) != 0) {
                linked_LinkSkill.add(entry.getIntKey());
            }
        }
        return linked_LinkSkill;
    }

    public Set<LinkSkill> getAccountLinkSkills() {
        return getAccount().getLinkSkills().stream()
                .filter(ls -> ls.getOwnerID() != getId())
                .collect(Collectors.toSet());
    }

    public void putSkill(Skill skill) {
        skills.put(skill.getSkillId(), skill);
    }

    /**
     * Adds a skill to this Char. If the Char already has this skill, just changes the levels.
     *
     * @param skillID      the skill's id to add
     * @param currentLevel the current level of the skill
     * @param masterLevel  the master level of the skill
     */
    public void addSkill(int skillID, int currentLevel, int masterLevel) {
        Skill skill = SkillData.getSkillDeepCopyById(skillID);
        if (skill == null && !SkillConstants.isMakingSkill(skillID)) {
            System.out.printf("No such skill %d found.%d", skillID);
            return;
        }
        skill.setCurrentLevel(currentLevel);
        skill.setMasterLevel(masterLevel);
        addSkill(skill);
        write(WvsContext.changeSkillRecordResult(skill));
    }

    /**
     * Adds a {@link Skill} to this Char. Changes the old Skill if the Char already has a Skill
     * with the same id. Removes the skill if the given skill's id is 0.
     *
     * @param skill The Skill this Char should get.
     */
    public void addSkill(Skill skill) {
        addSkill(skill, false);
    }

    /**
     * Adds a {@link Skill} to this Char. Changes the old Skill if the Char already has a Skill
     * with the same id. Removes the skill if the given skill's id is 0.
     *
     * @param skill                The Skill this Char should get.
     * @param addRegardlessOfLevel if this is true, the skill will not be removed from the char, even if the cur level
     *                             of the given skill is 0.
     */
    public void addSkill(Skill skill, boolean addRegardlessOfLevel) {
        if (skill == null) {
            System.out.println("Unable to addSkill since Skill is NULL.");
            return;
        }
        int skillId = skill.getSkillId();
        if (!addRegardlessOfLevel && skill.getCurrentLevel() == 0) {
            removeSkill(skillId);
            return;
        }
        skill.setCharId(getId());
        boolean isPassive = SkillConstants.isPassiveSkill(skillId);
        boolean isChanged;

        Skill oldSkill = skills.get(skillId);
        if (oldSkill == null) {
            skills.put(skillId, skill);
            if (getId() > 0) skill.saveToSQL();
            isChanged = true;
        } else {
            isChanged = oldSkill.getCurrentLevel() != skill.getCurrentLevel();
            if (isPassive && isChanged) {
                removeFromBaseStatCache(oldSkill);
            }
            oldSkill.setCurrentLevel(skill.getCurrentLevel());
            oldSkill.setMasterLevel(skill.getMasterLevel());
            if (getId() > 0) oldSkill.saveToSQL();
        }
        if (isPassive && isChanged) {
            addToBaseStatCache(skill);
        }
    }

    public void addListSkill(List<Skill> skills) {
        List<Skill> visibleSkills = new ArrayList<>();
        for (Skill skill : skills) {
            if (skill == null) {
                System.out.println("Unable to addListSkill since Skill is NULL.");
                continue;
            }
            if (!SkillConstants.isSpecialInvisibleSkill(skill.getSkillId())) {
                visibleSkills.add(skill);
            }
            if (!hasSkill(skill.getSkillId())) {
                addSkill(skill);
            }
        }
        if (!visibleSkills.isEmpty()) {
            write(WvsContext.changeSkillRecordResult(visibleSkills, true, false, false));
        }
    }

    /**
     * Set a new Mastery of a skill.
     *
     * @param skillID the id of the skill that should be edited
     */
    public void setMasterySkillLevel(int skillID, int MasterLevel) {
        Skill skill = getSkill(skillID);
        if (skill != null) {
            skill.setCurrentLevel(skill.getCurrentLevel());
            skill.setMasterLevel(MasterLevel);
            write(WvsContext.changeSkillRecordResult(skill));
        }
    }

    /**
     * Removes a Skill from this Char.
     *
     * @param skillID the id of the skill that should be removed
     */
    public void removeSkill(int skillID) {
        Skill skill = Util.findWithPred(getSkills(), s -> s.getSkillId() == skillID);
        if (skill != null) {
            if (SkillConstants.isPassiveSkill(skillID)) {
                removeFromBaseStatCache(skill);
            }
            skill.deleteSkillFromSQL();
            skills.remove(skillID);
        }
    }

    /**
     * Removes a Skill from this Char.
     * Sends change skill record to remove the skill from the client.
     *
     * @param skillID the id of the skill that should be removed
     */
    public void removeSkillAndSendPacket(int skillID) {
        Skill skill = getSkill(skillID);
        if (skill != null) {
            boolean tms = net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
            if (tms) removeSkill(skillID); // subtract the stored level before making the client removal record
            skill.setCurrentLevel(-1);
            skill.setMasterLevel(-1);
            write(WvsContext.changeSkillRecordResult(Collections.singletonList(skill), true, false, false));
            if (!tms) removeSkill(skillID);
        }
    }

    /**
     * Initializes the BaseStat cache, by going through all the needed passive stat changers.
     */
    public void initBaseStats() {
        baseStats.clear();
        baseStats.put(BaseStat.cr, 5L);
        baseStats.put(BaseStat.crDmg, 20L);
        baseStats.put(BaseStat.pdd, 9L);
        baseStats.put(BaseStat.mdd, 9L);
        baseStats.put(BaseStat.acc, 11L);
        baseStats.put(BaseStat.eva, 8L);
        baseStats.put(BaseStat.buffTimeR, 100L);
        getSkills().stream().filter(skill -> SkillConstants.isPassiveSkill_NoPsdSkillsCheck(skill.getSkillId())).forEach(this::addToBaseStatCache);
        if (!diceBaseStats.isEmpty()) {
            for (Map.Entry<BaseStat, Integer> stat : diceBaseStats.entrySet()) {
                addBaseStat(stat.getKey(), stat.getValue());
            }
        }
        // TODO: Handle Legion stats here
    }

    /**
     * Adds a Skill's info to the current base stat cache.
     *
     * @param skill The skill to add
     */
    public void addToBaseStatCache(Skill skill) {
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        if (SkillConstants.isPassiveSkill(skill.getSkillId())) {
            Map<BaseStat, Integer> stats = si.getBaseStatValues(this, skill.getCurrentLevel());
            stats.forEach(this::addBaseStat);
        }
        if (si.isPsd() && si.getSkillStatInfo().containsKey(coolTimeR)) {
            for (int psdSkill : si.getPsdSkills()) {
                getHyperPsdSkillsCooltimeR().put(psdSkill, si.getValue(coolTimeR, 1));
            }
        }
    }

    /**
     * Removes a Skill's info from the current base stat cache.
     *
     * @param skill The skill to remove
     */
    public void removeFromBaseStatCache(Skill skill) {
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        Map<BaseStat, Integer> stats = si.getBaseStatValues(this, skill.getCurrentLevel());
        stats.forEach(this::removeBaseStat);
    }

    /**
     * Returns whether or not this Char has a {@link Skill} with a given id.
     *
     * @param id The id of the Skill.
     * @return Whether or not this Char has a Skill with the given id.
     */
    public boolean hasSkill(int id) {
        Skill s = skills.get(id);
        return s != null && s.getCurrentLevel() > 0;
    }

    /**
     * Gets a {@link Skill} of this Char with a given id.
     *
     * @param id The id of the requested Skill.
     * @return The Skill corresponding to the given id of this Char, or null if there is none.
     */
    public Skill getSkill(int id) {
        return getSkill(id, false);
    }

    /**
     * Gets a {@link Skill} with a given ID. If <code>createIfNull</code> is true, creates the Skill
     * if it doesn't exist yet.
     * If it is false, will return null if this Char does not have the given Skill.
     *
     * @param id           The id of the requested Skill.
     * @param createIfNull Whether or not this method should create the Skill if it doesn't exist.
     * @return The Skill that the Char has, or <code>null</code> if there is no such skill and
     * <code>createIfNull</code> is false.
     */
    public Skill getSkill(int id, boolean createIfNull) {
        Skill s = skills.get(id);
        if (s != null) {
            return s;
        }
        return createIfNull ? createAndReturnSkill(id) : null;
    }

    public Skill getSkill(int id, int slv) {
        Skill s = skills.get(id);
        if (s != null) {
            return s;
        }
        return createAndReturnSkill(id, slv);
    }

    public int getSkillLevel(int skillID) {
        Skill s = skills.get(skillID);
        return s != null ? s.getCurrentLevel() : 0;
    }

    public int getMasterySkillLevel(int skillID) {
        Skill s = skills.get(skillID);
        return s != null ? s.getMasterLevel() : 0;
    }

    /**
     * Gets the given SkillStat's Value.
     *
     * @param skillStat SkillStat to get the value from.
     * @param skillId   Specified skillId to grab the SkillInfo from.
     * @return value of the given SkillStat of the given Skill Id
     */
    public int getSkillStatValue(SkillStat skillStat, int skillId) {
        if (hasSkill(skillId)) {
            SkillInfo si = SkillData.getSkillInfoById(skillId);
            return si.getValue(skillStat, getSkillLevel(skillId));
        }
        return 0;
    }

    public int getRemainRecipeUseCount(int recipeID) {
        if (SkillConstants.isMakingSkill(recipeID)) {
            return getSkillLevel(recipeID);
        }
        return 0;
    }

    /**
     * Creates a new {@link Skill} for this Char.
     *
     * @param id The skillID of the Skill to be created.
     * @return The new Skill.
     */
    private Skill createAndReturnSkill(int id) {
        Skill skill = SkillData.getSkillDeepCopyById(id);
        addSkill(skill);
        return skill;
    }

    private Skill createAndReturnSkill(int id, int slv) {
        Skill skill = SkillData.getSkillDeepCopyById(id);
        skill.setCurrentLevel(1);
        addSkill(skill);
        return skill;
    }

    public boolean assignStat(Stat charStat, int amount) {
        CharacterStat cs = getAvatarData().getCharacterStat();
        if (amount > cs.getAp()) {
            return false;
        }
        int oldStat = 0;
        if (charStat == Stat.str) {
            oldStat = cs.getStr();
        } else if (charStat == Stat.dex) {
            oldStat = cs.getDex();
        } else if (charStat == Stat.inte) {
            oldStat = cs.getInt();
        } else if (charStat == Stat.luk) {
            oldStat = cs.getLuk();
        }
        int newStat = amount + oldStat;
        if (newStat < 4 || newStat > 32767) {
            return false;
        }
        int newAP = cs.getAp() - amount;
        setStat(charStat, newStat);
        setStat(Stat.ap, newAP);
        Map<Stat, Object> stats = new HashMap<>();
        stats.put(charStat, (short) newStat);
        stats.put(Stat.ap, (short) newAP);
        sendStatsPacket(stats);
        return true;
    }

    public void setStat(Stat charStat, int amount) {
        CharacterStat cs = getAvatarData().getCharacterStat();
        switch (charStat) {
            case str -> cs.setStr(amount);
            case dex -> cs.setDex(amount);
            case inte -> cs.setInt(amount);
            case luk -> cs.setLuk(amount);
            case hp -> { cs.setHp(amount); if(amount>0)net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.alive(this);
                else {
                    net.swordie.ms.client.jobs.resistance.Tms282OmegaBlaster.reset(this);
                    net.swordie.ms.client.jobs.flora.Tms282AetherGuard.reset(this, System.currentTimeMillis());
                    net.swordie.ms.client.jobs.Jianghu.Tms282HowlingStorm.reset(this);
                    net.swordie.ms.client.jobs.adventurer.thief.Tms282BladeTempest.reset(this);
                    net.swordie.ms.client.jobs.cygnus.BlazeWizard.stopInferno(this);
                    net.swordie.ms.client.jobs.flora.Tms282AdeleShardbreaker.clear(this);
                } }
            case mhp -> {
                cs.setMaxHp(amount);
                if (JobConstants.isDemonAvenger(getJob()) && getJobHandler() instanceof DemonAvenger demonAvenger) {
                    demonAvenger.sendHpUpdate();
                }
            }
            case mp -> cs.setMp(amount);
            case mmp -> cs.setMaxMp(amount);
            case ap -> cs.setAp(amount);
            case level -> {
                cs.setLevel(amount);
                notifyChanges();
            }
            case skin -> cs.setSkin(amount);
            case face -> cs.setFace(amount);
            case hair -> cs.setHair(amount);
            case pop -> cs.setPop(amount);
            case charismaEXP -> cs.setCharismaExp(amount);
            case charmEXP -> cs.setCharmExp(amount);
            case craftEXP -> cs.setCraftExp(amount);
            case insightEXP -> cs.setInsightExp(amount);
            case senseEXP -> cs.setSenseExp(amount);
            case willEXP -> cs.setWillExp(amount);
            case fatigue -> cs.setFatigue(amount);
        }
    }

    /**
     * Notifies all groups (such as party, guild) about all your changes, such as level and job.
     */
    private void notifyChanges() {
        Party party = getParty();
        if (party != null) {
            party.updatePartyMemberInfoByChr(this);
        }
        if (this.guild != null) {
            GuildMember gm = this.guild.getMemberByCharID(getId());
            if (gm != null) {
                gm.updateInfoFromChar(this);
                this.guild.broadcast(WvsContext.guildResult(GuildResult.response_ChangeLevelOrJob_Success(this.guild, gm)));
            }
            Alliance ally = this.guild.getAlliance();
            if (ally != null) {
                ally.broadcast(WvsContext.allianceResult(AllianceResult.changeLevelOrJob(ally, this.guild, gm)));
            }
        }
        Account account = getAccount();
        if (account != null) {
            Union union = account.getUnion();
            if (union != null) {
                for (UnionBoard unionBoard : union.getUnionBoards()) {
                    if (unionBoard == null) {
                        continue;
                    }
                    for (UnionMember unionMember : unionBoard.getActiveMembers()) {
                        if (unionMember == null) {
                            continue;
                        }
                        if (unionMember.getCharId() == getId()) {
                            unionMember.setCharLevel(getLevel());
                            unionMember.setCharJob(getJob());
                            unionMember.setCharSubJob(getSubJob());
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets a raw Stat from this Char, unaffected by things such as equips and skills.
     *
     * @param charStat The requested Stat
     * @return the requested stat's value
     */
    public int getStat(Stat charStat) {
        CharacterStat cs = getAvatarData().getCharacterStat();
        return switch (charStat) {
            case str -> cs.getStr();
            case dex -> cs.getDex();
            case inte -> cs.getInt();
            case luk -> cs.getLuk();
            case hp -> cs.getHp();
            case mhp -> cs.getMaxHp();
            case mp -> cs.getMp();
            case mmp -> cs.getMaxMp();
            case ap -> cs.getAp();
            case level -> cs.getLevel();
            case skin -> cs.getSkin();
            case face -> cs.getFace();
            case hair -> cs.getHair();
            case pop -> cs.getPop();
            case charismaEXP -> cs.getCharismaExp();
            case charmEXP -> cs.getCharmExp();
            case craftEXP -> cs.getCraftExp();
            case insightEXP -> cs.getInsightExp();
            case senseEXP -> cs.getSenseExp();
            case willEXP -> cs.getWillExp();
            case fatigue -> cs.getFatigue();
            case job -> cs.getJob();
            default -> -1;
        };
    }

    /**
     * Adds a Stat to this Char.
     *
     * @param charStat which Stat to add
     * @param amount   the amount of Stat to add
     */
    public void addStat(Stat charStat, int amount) {
        setStat(charStat, getStat(charStat) + amount);
    }

    /**
     * Adds a Stat to this Char, and immediately sends the packet to the client notifying the change.
     *
     * @param charStat which Stat to change
     * @param amount   the amount of Stat to add
     */
    public void addStatAndSendPacket(Stat charStat, int amount) {
        setStatAndSendPacket(charStat, getStat(charStat) + amount);
    }

    /**
     * Adds a Stat to this Char, and immediately sends the packet to the client notifying the change.
     *
     * @param charStat which Stat to change
     * @param value    the value of Stat to set
     */
    public void setStatAndSendPacket(Stat charStat, int value) {
        value = checkValidStat(charStat, value);
        setStat(charStat, value);
        Map<Stat, Object> stats = new HashMap<>();
        switch (charStat) {
            case skin:
            case fatigue:
                stats.put(charStat, (byte) getStat(charStat));
                break;
            case str:
            case dex:
            case inte:
            case luk:
            case ap:
            case job:
                stats.put(charStat, (short) getStat(charStat));
                break;
            case hp:
            case mhp:
            case mp:
            case mmp:
            case face:
            case hair:
            case pop:
            case charismaEXP:
            case insightEXP:
            case willEXP:
            case craftEXP:
            case senseEXP:
            case charmEXP:
            case eventPoints:
            case level:
                stats.put(charStat, getStat(charStat));
                break;
        }
        sendStatsPacket(stats);
    }

    public void sendStatsPacket(Map<Stat, Object> stats) {
        CharacterStat cs = getAvatarData().getCharacterStat();
        byte mixBaseHairColor = (byte) cs.getMixBaseHairColor();
        byte mixAddHairColor = (byte) cs.getMixAddHairColor();
        byte mixHairBaseProb = (byte) cs.getMixHairBaseProb();
        write(WvsContext.statChanged(getClientStatPacketValues(stats), getSubJob(), mixBaseHairColor, mixAddHairColor, mixHairBaseProb));
    }

    private Map<Stat, Object> getClientStatPacketValues(Map<Stat, Object> stats) {
        if (!stats.containsKey(Stat.mhp) && !stats.containsKey(Stat.mmp)) {
            return stats;
        }
        Map<Stat, Object> packetStats = new HashMap<>(stats);
        if (packetStats.containsKey(Stat.mhp)) {
            packetStats.put(Stat.mhp, getClientMaxHPPacketValue());
        }
        if (packetStats.containsKey(Stat.mmp)) {
            packetStats.put(Stat.mmp, getClientMaxMPPacketValue());
        }
        return packetStats;
    }

    private void syncMaxHpMpWithClient() {
        syncMaxHpMpWithClient(false, false);
    }

    private void syncMaxHpMpWithClient(boolean fillHP, boolean fillMP) {
        Map<Stat, Object> stats = new HashMap<>();
        stats.put(Stat.mhp, getStat(Stat.mhp));
        stats.put(Stat.mmp, getStat(Stat.mmp));

        int maxHP = getMaxHP();
        if (fillHP || getHP() > maxHP) {
            setStat(Stat.hp, maxHP);
            stats.put(Stat.hp, maxHP);
        }

        int maxMP = getMaxMP();
        if (fillMP || getMP() > maxMP) {
            setStat(Stat.mp, maxMP);
            stats.put(Stat.mp, maxMP);
        }

        sendStatsPacket(stats);
    }

    private int checkValidStat(Stat charStat, int value) {
        switch (charStat) {
            case fatigue: {
                if (value > 200) {
                    value = 200;
                } else if (value < 0) {
                    value = 0;
                }
                break;
            }
            case hp:
            case mp:
            case mhp:
            case mmp:
                if (value > GameConstants.MAX_HP_MP) {
                    value = GameConstants.MAX_HP_MP;
                } else if (value < 0) {
                    value = 0;
                }
                break;
            case str:
            case dex:
            case inte:
            case luk:
                if (value > 32767) {
                    value = 32767;
                } else if (value < 0) {
                    value = 0;
                }
                break;
        }
        return value;
    }

    /**
     * Adds a certain amount of money to the current character. Also sends the
     * packet to update the client's state.
     *
     * @param amount The amount of money to add. May be negative.
     */
    public void addMoney(long amount) {
        if(net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.economyEnabled()) {
            synchronized(net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized(this) {
                net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.requireEconomicAccess(this);
                addMoneyInternal(amount);
            } }
        } else addMoneyInternal(amount);
    }

    private void addMoneyInternal(long amount) {
        CharacterStat cs = getAvatarData().getCharacterStat();
        long money = cs.getMoney();
        long newMoney = money + amount;
        if (newMoney >= 0) {
            newMoney = Math.min(GameConstants.MAX_MONEY, newMoney);
            Map<Stat, Object> stats = new HashMap<>();
            cs.setMoney(newMoney);
            stats.put(Stat.money, newMoney);
            sendStatsPacket(stats);
        } else {
            Map<Stat, Object> stats = new HashMap<>();
            cs.setMoney(0);
            stats.put(Stat.money, 0);
            sendStatsPacket(stats);
        }
        net.swordie.ms.world.tms282.audit.AuditLog.money(this, money, cs.getMoney()); // audit: log_money
    }

    /**
     * The same as addMoney, but negates the amount.
     *
     * @param amount The money to deduct. May be negative.
     */
    public void deductMoney(long amount) {
        addMoney(-amount);
    }

    public Position getOldPosition() {
        return oldPosition;
    }

    public void setOldPosition(Position oldPosition) {
        this.oldPosition = oldPosition;
    }

    public byte getMoveAction() {
        return moveAction;
    }

    public void setMoveAction(byte moveAction) {
        this.moveAction = moveAction;
    }

    /**
     * Sends a message to this Char through the ScriptProgress packet.
     *
     * @param msg The message to display.
     */
    public void chatScriptMessage(String msg) {
        write(WvsContext.scriptProgressMessage(msg));
    }

    /**
     * Sends a message to this Char with a default colour {@link ChatType#SystemNotice}.
     *
     * @param msg The message to display.
     */
    public void chatMessage(String msg) {
        chatMessage(SystemNotice, msg);
    }

    /**
     * Sends a formatted message to this Char with a default color {@link ChatType#SystemNotice}.
     *
     * @param msg  The message to display
     * @param args The format arguments
     */
    public void chatMessage(String msg, Object... args) {
        chatMessage(SystemNotice, msg, args);
    }

    /**
     * Sends a formatted message to this Char with a given {@link ChatType colour}.
     *
     * @param clr  The Colour this message should be in.
     * @param msg  The message to display.
     * @param args The format arguments
     */
    public void chatMessage(ChatType clr, String msg, Object... args) {
        write(UserLocal.chatMsg(clr, String.format(msg, args)));
    }

    /**
     * Sends a message to this Char with a given {@link ChatType colour}.
     *
     * @param clr The Colour this message should be in.
     * @param msg The message to display.
     */
    public void chatMessage(ChatType clr, String msg) {
        write(UserLocal.chatMsg(clr, msg));
    }

    /**
     * Sends a message to the character if the debug config flag is turned on.
     *
     * @param message message to send
     */
    public void dbgChatMsg(String message) {
        if (ServerConfig.DEBUG_MODE && isGM())
            chatMessage(message);
    }

    public void chatPopup(String message) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            // BROADCAST_MSG is not mapped in 282; use the supported system-chat path.
            chatMessage(message);
            return;
        }
        write(WvsContext.broadcastMsg(BroadcastMsg.popUpMessage(message)));
    }

    /** Reconcile equipment-derived skills after a committed282 potential change on a worn item. */
    public void refreshEquipmentSkillsAfterChange(Equip before, Equip after) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                || before == null || after == null
                || getEquippedInventory().getItems().stream().noneMatch(item -> item == after)) return;
        refreshEquipmentSkills(before);
        refreshEquipmentSkills(after);
    }

    private void refreshEquipmentSkills(Equip changed) {
        int beginnerJob = JobConstants.JobEnum.getJobById(getJob()).getBeginnerJobId();
        var affected = net.swordie.ms.client.character.items.EquipmentSkills.levels(List.of(changed), beginnerJob);
        var remaining = net.swordie.ms.client.character.items.EquipmentSkills.levels(getEquippedInventory().getItems(), beginnerJob);
        java.util.Set<Integer> affectedIds = affected.keySet();
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            affectedIds = new java.util.HashSet<>(affectedIds);
            affectedIds.addAll(net.swordie.ms.client.character.items.Tms282EquipmentSkillData.skillIds(changed.getItemId()));
            affectedIds.addAll(net.swordie.ms.client.character.items.Tms282SpecialWeaponSkills.passiveIds(changed.getItemId()));
            // info/addition item skills (9000xxxx, with con/lv; equips README WI-6)
            affectedIds.addAll(net.swordie.ms.client.character.items.Tms282EquipAdditions.skillIds(changed.getItemId()));
            java.util.Map<Integer, Integer> withAdditions = new java.util.HashMap<>(remaining);
            net.swordie.ms.client.character.items.Tms282EquipAdditions.skillLevels(getEquippedInventory().getItems(),
                    getLevel(), getJob()).forEach((skill, level) -> withAdditions.merge(skill, level, Math::max));
            remaining = withAdditions;
        }
        refreshEquipmentSkillIds(affectedIds, remaining);
    }

    /** Reconcile stale passive records even when no matching special weapon is currently worn. */
    public void refreshSpecialWeaponSkills() {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) return;
        int beginnerJob = JobConstants.JobEnum.getJobById(getJob()).getBeginnerJobId();
        refreshEquipmentSkillIds(net.swordie.ms.client.character.items.Tms282SpecialWeaponSkills.allPassiveIds(),
                net.swordie.ms.client.character.items.EquipmentSkills.levels(getEquippedInventory().getItems(), beginnerJob));
    }

    private void refreshEquipmentSkillIds(java.util.Set<Integer> affectedIds, java.util.Map<Integer, Integer> remaining) {
        List<Skill> updates = new ArrayList<>();
        for (int skillId : affectedIds) {
            int level = remaining.getOrDefault(skillId, 0);
            Skill skill = getSkill(skillId);
            if (level > 0) {
                if (net.swordie.ms.connection.protocol.Protocol.active().version()
                        == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                    Skill fresh = SkillData.getSkillDeepCopyById(skillId);
                    if (fresh == null) continue;
                    if (skill != null && skill.getCurrentLevel() == Math.min(level, fresh.getMaxLevel())
                            && skill.getMaxLevel() == fresh.getMaxLevel()
                            && skill.getMasterLevel() == fresh.getMasterLevel()) continue;
                    if (skill != null) skill.setMaxLevel(fresh.getMaxLevel());
                    skill = fresh;
                } else if (skill == null) skill = SkillData.getSkillDeepCopyById(skillId);
                if (skill == null) continue;
                skill.setCurrentLevel(Math.min(level, skill.getMaxLevel()));
                addSkill(skill);
            } else {
                if (skill == null) continue;
                removeSkill(skillId);
                getTemporaryStatManager().removeStatsBySkill(skillId);
                skill.setCurrentLevel(-1);
            }
            updates.add(skill);
        }
        if (!updates.isEmpty()) write(WvsContext.changeSkillRecordResult(updates, true, false, false));
        net.swordie.ms.client.character.items.Tms282SpecialWeaponEffects.reconcile(this);
    }

    /**
     * Unequips an {@link Item}. Ensures that the hairEquips and both inventories get updated.
     *
     * @param item The Item to equip.
     */
    public void unequip(Item item, int toSlot) {
        net.swordie.ms.client.character.tms282.Tms282CubeRuntime.requireMovable(this, item);
        net.swordie.ms.client.character.tms282.Tms282CubeRuntime.requireMovable(this,
                getInventoryByType(item.isCash() ? DECORATION : EQUIP).getItemBySlot(toSlot));
        AvatarLook al = getAvatarData().getAvatarLook();
        int itemID = item.getItemId();
        try {
            getInventoryByType(EQUIPPED).removeItem(item);
        } finally {
            Inventory inv = getInventoryByType(item.isCash() ? DECORATION : EQUIP);
            item.setBagIndex(toSlot);
            inv.addSwapItem(item);
        }

        al.removeItem(itemID);

        if (JobConstants.isZero(getJob())) {
            if (getAvatarData().getZeroAvatarLook().getHairEquips().contains(itemID)) {
                getAvatarData().getZeroAvatarLook().removeItem(itemID);
            }
        }

        Equip equip = (Equip) item;
        if (equip.getSetItemID() != 0) {
            initCompletedSetItemID();
        }

        if (JobConstants.isAngelicBuster(getJob())) {
            if (item.getItemId() / 10000 == 105 && item.isCash()) {
                getDressUpInfo().setClothe(1051291);
            }
            getField().broadcast(WvsContext.dressUpInfoModified(getDressUpInfo()));
        }
        if (ItemConstants.isWeapon(itemID)) {
            getJobHandler().weaponType = 0;
        }
        if (net.swordie.ms.client.character.social.tms282.RingKind.isPairRing(itemID)) net.swordie.ms.client.character.social.tms282.RingRecordService.get().onEquipChanged(this); // social: ring records
        getField().broadcast(UserRemote.avatarModified(this, AvatarModifiedMask.AvatarLook.getVal(), (byte) 0), this);
        removeSoulMP(equip);
        if (ItemConstants.isRing(itemID) && itemID == 1112932) {
            // Goddess' Guard
            for (Summon s : field.getSummons()) {
                if (s.getOwnerId() == getId()
                        && (s.getSkillID() == 223 + JobConstants.JobEnum.getJobById(getJob()).getBeginnerJobId() * 10000
                        || s.getSkillID() == 1224 + JobConstants.JobEnum.getJobById(getJob()).getBeginnerJobId() * 10000)) {
                    field.removeSummon(s.getSkillID(), getId());
                    getTemporaryStatManager().removeStatsBySkill(s.getSkillID());
                }
            }
        }
        refreshEquipmentSkills(equip);
        int equippedSummonSkill = ItemConstants.getEquippedSummonSkillItem(item.getItemId(), getJob());
        if (equippedSummonSkill != 0) {
            getField().removeSummon(equippedSummonSkill, getId());
            TemporaryStatManager tsm = getTemporaryStatManager();
            tsm.removeStatsBySkill(equippedSummonSkill);
            tsm.removeStatsBySkill(tsm.getOption(RepeatEffect).rOption);
            removeSkillAndSendPacket(equippedSummonSkill);
        }
        if (JobConstants.isDemonAvenger(getJob()) && getJobHandler() instanceof DemonAvenger demonAvenger) {
            demonAvenger.sendHpUpdate();
        }
        if (ItemConstants.isAndroid(itemID) || ItemConstants.isMechanicalHeart(itemID)) {
            if (getAndroid() != null) {
                getField().removeLife(getAndroid());
            }
            setAndroid(null);
        }
        syncMaxHpMpWithClient();
        //FilePrinter.print(FilePrinter.HIKARICP_ERROR, String.format("[%s] Un-equip Item | ID: %d | ItemID: %d | ItemName: %s | InvType: %s | Bag Index: %d.", getName(), equip.getId(), equip.getItemId(), StringData.getItemStringById(equip.getItemId()), equip.getInvType(), equip.getBagIndex()));
        //item.saveToSQL();
    }

    /**
     * Equips an {@link Item}. Ensures that the hairEquips and both inventories get updated.
     *
     * @param item The Item to equip.
     */
    public boolean equip(Item item, int newPos) {
        net.swordie.ms.client.character.tms282.Tms282CubeRuntime.requireMovable(this, item);
        net.swordie.ms.client.character.tms282.Tms282CubeRuntime.requireMovable(this,
                getEquippedInventory().getItemBySlot(Math.abs(newPos)));
        if (!net.swordie.ms.loaders.Tms282PetEquipment.canWear(this, item, newPos)) {
            chatPopup("此裝備與該欄位的寵物不相容。");
            return false;
        }
        Equip equip = (Equip) item;
        if (equip.hasSpecialAttribute(EquipSpecialAttribute.Vestige)) {
            chatPopup("無法穿戴該裝備。");
            return false;
        }
        if (equip.isEquipTradeBlock()) {
            equip.setTradeBlock(true);
            equip.setEquipTradeBlock(false);
            equip.setEquippedDate(FileTime.currentTime());
            equip.addAttribute(EquipAttribute.UnTradable);
        }
        if (equip.hasAttribute(EquipAttribute.UnTradableAfterTransaction)) {
            equip.setTradeBlock(true);
            equip.removeAttribute(EquipAttribute.UnTradableAfterTransaction);
            equip.addAttribute(EquipAttribute.UnTradable);
        }
        if (equip.hasAttribute(EquipAttribute.TradedOnceWithinAccount)) {
            equip.setTradeBlock(true);
            equip.removeAttribute(EquipAttribute.TradedOnceWithinAccount);
            equip.addAttribute(EquipAttribute.UnTradable);
        }
        if (equip.getCharmEXP() > 0) {
            addStatAndSendPacket(Stat.charmEXP, equip.getCharmEXP());
            equip.setCharmEXP(0);
            equip.setiCraft((short) 0);
            equip.addAttribute(EquipAttribute.NoNonCombatStatGain);
        }

        AvatarLook al = getAvatarData().getAvatarLook();
        AvatarLook zeroAvatarLook = null;
        if (JobConstants.isZero(getJob())) {
            zeroAvatarLook = getAvatarData().getZeroAvatarLook();
        }
        int itemID = item.getItemId();
        int oldMaxHP = getMaxHP();
        int oldMaxMP = getMaxMP();
        boolean wasFullHP = getHP() >= oldMaxHP;
        boolean wasFullMP = getMP() >= oldMaxMP;

        try {
            getInventoryByType(item.isCash() ? DECORATION : EQUIP).removeItem(item);
        } finally {
            item.setBagIndex(newPos);
            getInventoryByType(EQUIPPED).addSwapItem(item);
        }

        List<Integer> hairEquips = getAvatarData().getAvatarLook().getHairEquips();
        if (newPos < BodyPart.APBase.getVal() || newPos > BodyPart.APEnd.getVal()) {
            // only add if not part of your own body
            if (ItemConstants.isCashWeapon(itemID)) {
                al.setWeaponStickerId(itemID);
                if (zeroAvatarLook != null) {
                    zeroAvatarLook.setWeaponStickerId(itemID);
                }
            }
            if (ItemConstants.isWeapon(itemID)) {
                al.setWeaponId(itemID);
                if (zeroAvatarLook != null) {
                    zeroAvatarLook.setWeaponId(itemID);
                }
            }
            if (ItemConstants.isSubWeapon(itemID)) {
                al.setSubWeaponId(itemID);
                if (zeroAvatarLook != null) {
                    zeroAvatarLook.setWeaponId(itemID);
                }
            }
            if (ItemConstants.isRing(itemID)) {
                if (itemID == 1112932) {
                    // Goddess' Guard
                    Summon summon = Summon.getSummonByAndSetStat(this, 223 + JobConstants.JobEnum.getJobById(getJob()).getBeginnerJobId() * 10000, (byte) 1);
                    if (summon != null) {
                        summon.setAssistType(AssistType.Bodyguard);
                        field.spawnSummon(summon);
                    } else {
                        summon = Summon.getSummonByAndSetStat(this, 1224 + JobConstants.JobEnum.getJobById(getJob()).getBeginnerJobId() * 10000, (byte) 1);
                        summon.setAssistType(AssistType.Bodyguard);
                        field.spawnSummon(summon);
                    }
                }
            }
            if (ItemConstants.isTotem(itemID)) {
                al.addTotemID(itemID);
                if (zeroAvatarLook != null) {
                    zeroAvatarLook.addTotemID(itemID);
                }
            }
            if (!hairEquips.contains(itemID)) {
                al.addHairEquip(itemID);
                if (zeroAvatarLook != null) {
                    zeroAvatarLook.addHairEquip(itemID);
                }
            }
            // Khúc này có vẻ cần tối ưu
            if (zeroAvatarLook != null) {
                if (!zeroAvatarLook.getHairEquips().contains(itemID)) {
                    if (newPos >= BodyPart.ZeroBase.getVal() && newPos < BodyPart.ZeroEnd.getVal()) {
                        al.removeItem(itemID);
                        zeroAvatarLook.addHairEquip(itemID);
                    }
                }
            }
            if (ItemConstants.isMedal(itemID)) {
                for (MedalAchievementInfo medals : getMedalAchievementInfo()) {
                    if (medals.getItemID() == itemID) {
                        setEquippedMedalItem(itemID);
                    }
                }
            }
        }
        refreshEquipmentSkills(equip);
        int equippedSummonSkill = ItemConstants.getEquippedSummonSkillItem(equip.getItemId(), getJob());
        if (equippedSummonSkill != 0) {
            SkillInfo si = SkillData.getSkillInfoById(equippedSummonSkill);
            if (si != null) {
                getJobHandler().handleSkill(getClient(), null, new SkillUseInfo(si, 1));
            }
        }
        if (equip.getSetItemID() != 0) {
            initCompletedSetItemID();
        }
        if (JobConstants.isAngelicBuster(getJob())) {
            initDressUpInfo(item);
            getField().broadcast(WvsContext.dressUpInfoModified(getDressUpInfo()));
        }
        if (ItemConstants.isWeapon(itemID)) {
            getJobHandler().weaponType = ItemConstants.getWeaponTypeVal(itemID);
        }
        if (net.swordie.ms.client.character.social.tms282.RingKind.isPairRing(itemID)) net.swordie.ms.client.character.social.tms282.RingRecordService.get().onEquipChanged(this); // social: ring records
        getField().broadcast(UserRemote.avatarModified(this, AvatarModifiedMask.AvatarLook.getVal(), (byte) 0), this);
        addSoulMP(equip);
        if (JobConstants.isDemonAvenger(getJob()) && getJobHandler() instanceof DemonAvenger demonAvenger) {
            demonAvenger.sendHpUpdate();
        }
        // check android status
        if (ItemConstants.isAndroid(itemID) || ItemConstants.isMechanicalHeart(itemID)) {
            initAndroid(true);
            if (getAndroid() != null) {
                getField().spawnLife(getAndroid(), null);
            }
        }
        syncMaxHpMpWithClient(wasFullHP, wasFullMP);
        //DataPrinter.send(DataPrinter.HIKARICP_ERROR, String.format("[%s] Equip Item | ID: %d | ItemID: %d | ItemName: %s | InvType: %s | Bag Index: %d.", getName(), equip.getId(), equip.getItemId(), StringData.getItemStringById(equip.getItemId()), equip.getInvType(), equip.getBagIndex()));
        //item.saveToSQL();
        return true;
    }

    public TemporaryStatManager getTemporaryStatManager() {
        return temporaryStatManager;
    }

    public Map<ForcedStat, Object> getForcedStats() {
        if (forcedStats == null) {
            forcedStats = new HashMap<>();
        }
        return forcedStats;
    }

    public void update(long now) {
        try {
            if (this.temporaryStatManager != null) {
                this.temporaryStatManager.update(now);
            }
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        try {
            if (this.jobHandler != null) {
                this.jobHandler.update(now);
            }
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        try {
            updateOnlineTimer(now);
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        try {
            updateLuxeSauna(now);
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        try {
            removeSecondAtoms(now);
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    /** True while the player sits in the cash shop / auction house — field-only packets
     *  (effects, drops...) sent in that state crash the client. */
    private boolean isOutOfField() {
        return getUser() != null && getUser().getClientState() != Client.IN_FIELD;
    }

    public void updateLuxeSauna(long now) {
        if (isOutOfField()) {
            return;
        }
        if (getField() != null && getField().getId() == 993263300 && getLevel() < 300 && (luxeSaunaMilli == 0 || now - luxeSaunaMilli >= 2000)) {
            var sm = getScriptManager();
            int curCnt = Integer.parseInt(getQRValueByKey(518, "totalCnt"));
            int totalIncCount = Integer.parseInt(getQRValueByKey(518, "totalIncCount"));
            if (curCnt >= totalIncCount) {
                sm.warp(FieldConstants.HENESYS_ID);
                return;
            }
            curCnt += 1;
            setQRValueByKey(518, "totalCnt", curCnt);
            var effect =  Effect.effectFromWZ("Effect/CharacterEff.img/eventEff/vipSauna", false, 3000, 5, 0);
            write(UserPacket.effect(effect));
            getField().broadcast(UserRemote.effect(getId(), effect), this);
            long expGain = (long) (GameConstants.charExp[getLevel()] * 191.5 / 3600000);
            sm.fieldItemConsumed(expGain);
            addExpNoMsg(expGain);
            luxeSaunaMilli = now;
        }
    }

    public void updateOnlineTimer(long now) {
        final LocalDateTime nowDate = LocalDateTime.now();
        if (!hasQuest(QuestConstants.ONLINE_REWARDS_RECORD)) {
            createQuestWithQRValue(QuestConstants.ONLINE_REWARDS_RECORD, "Name=" + getName());
        }
        if (onlineDay != nowDate.getDayOfYear()) {
            onlineDay = nowDate.getDayOfYear();
            onlineTime = 0;
            lastOnlineTickMs = now;
        }
        if (lastOnlineTickMs <= 0L) {
            lastOnlineTickMs = now;
            return;
        }
        long elapsedMs = now - lastOnlineTickMs;
        // 2026-09-29: the server ticks every 500 ms; "elapsedMs / 1000" was always 0 and the tick base was reset,
        // so onlineTime never grew. Count whole seconds and keep the remainder for the next tick.
        if (elapsedMs < 1000L) {
            return;
        }
        long seconds = elapsedMs / 1000L;
        lastOnlineTickMs += seconds * 1000L;
        onlineTime += seconds;
        if (!isOutOfField() && getField() != null && getField().getId() == FieldConstants.HOME_MAP && getLevel() < 300 && (onlineTimeMilli == 0 || now - onlineTimeMilli >= 5000)) {
            long expGain = (long) GameConstants.charExp[getLevel()] / 100000;
            write(UserPacket.effect(Effect.fieldItemConsumed((int) (expGain > Integer.MAX_VALUE ? Integer.MAX_VALUE : expGain))));
            addExpNoMsg(expGain);
            onlineTimeMilli = now;
        }
    }

    public void removeSecondAtoms(long now) {
        if (secondAtoms == null) {
            return;
        }
        IntArrayList keysToRemove = new IntArrayList();
        ObjectArrayList<SecondAtom> atomsToRemove = new ObjectArrayList<>();
        for (var it = getSecondAtoms().int2ObjectEntrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            SecondAtom sa = e.getValue();
            long endTime = sa.getStart() + sa.getExpire();
            if (endTime <= now) {
                keysToRemove.add(e.getIntKey());
                atomsToRemove.add(sa);
            }
        }
        if (keysToRemove.isEmpty()) {
            return;
        }
        for (int i = 0; i < atomsToRemove.size(); i++) {
            removeSecondAtomInternal(atomsToRemove.get(i));
        }
        for (int i = 0; i < keysToRemove.size(); i++) {
            getSecondAtoms().remove(keysToRemove.getInt(i));
        }
    }

    public GachaponManager getGachaponManager() {
        return gachaponManager;
    }

    public Job getJobHandler() {
        return jobHandler;
    }

    public void setJobHandler(Job jobHandler) {
        if(this.jobHandler!=jobHandler) {
            net.swordie.ms.client.jobs.resistance.Tms282OmegaBlaster.reset(this);
            net.swordie.ms.client.jobs.flora.Tms282AetherGuard.reset(this, System.currentTimeMillis());
            net.swordie.ms.client.jobs.Jianghu.Tms282HowlingStorm.reset(this);
            net.swordie.ms.client.jobs.adventurer.thief.Tms282BladeTempest.reset(this);
            net.swordie.ms.client.jobs.cygnus.BlazeWizard.stopInferno(this);
            net.swordie.ms.client.jobs.flora.Tms282AdeleShardbreaker.clear(this);
        }
        this.jobHandler = jobHandler;
        var fixed = net.swordie.ms.client.character.tms282.Tms282SkillLearning.reconcileFixed(this,
                skill -> addSkill(skill, true));
        if (!fixed.isEmpty()) {
            initBaseStats(); // login loads skill rows before the normal field-entry cache initialization
            write(net.swordie.ms.connection.packet.Tms282SkillPackets.changeSkillRecordResult(fixed, true, false, false));
        }
    }

    public FuncKeyMap getFuncKeyMap() {
        return funcKeyMaps.getOrDefault(0, null);
    }

    public FuncKeyMap getFuncKeyMapByPreset(int preset) {
        return funcKeyMaps.getOrDefault(preset, null);
    }

    public Map<Integer, FuncKeyMap> getFuncKeyMaps() {
        return funcKeyMaps;
    }

    public void setFuncKeyMaps(Map<Integer, FuncKeyMap> funcKeyMaps) {
        this.funcKeyMaps = funcKeyMaps;
    }

    public void initFuncKeyMaps(int keySettingType, boolean beastTamer) {
        for (int preset = 0; preset < 3; preset++) {
            int amount = beastTamer ? 5 : 1;
            for (int count = 0; count < amount; count++) {
                FuncKeyMap funcKeyMap = FuncKeyMap.getDefaultMapping(getId(), preset, keySettingType);
                funcKeyMaps.put(preset, funcKeyMap);
            }
        }
    }

    /**
     * Creates a {@link Rect} with regard to this character. Adds all values to this Char's
     * position.
     *
     * @param rect The rectangle to use.
     * @return The new rectangle.
     */
    public Rect getRectAround(Rect rect) {
        int x = getPosition().getX();
        int y = getPosition().getY();
        return new Rect(x + rect.getLeft(), y + rect.getTop(), x + rect.getRight(), y + rect.getBottom());
    }

    /**
     * Returns the Equip equipped at a certain {@link BodyPart}.
     *
     * @param bodyPart The requested bodyPart.
     * @return The Equip corresponding to <code>bodyPart</code>. Null if there is none.
     */
    public Item getEquippedItemByBodyPart(BodyPart bodyPart) {
        List<Item> items = getEquippedInventory().getItemsByBodyPart(bodyPart);
        return items.isEmpty() ? null : items.getFirst();
    }

    public boolean isLeft() {
        return moveAction > 0 && (moveAction % 2) == 1;
    }

    public MarriageRecord getMarriageRecord() {
        return marriageRecord;
    }

    public void setMarriageRecord(MarriageRecord marriageRecord) {
        this.marriageRecord = marriageRecord;
    }

    /**
     * Returns a {@link Field} based on the current {@link FieldInstanceType} of this Char (channel,
     * expedition,
     * party or solo).
     *
     * @return The Field corresponding to the current FieldInstanceType.
     */
    public Field getOrCreateFieldByCurrentInstanceType(int fieldID) {
        Field res;
        if (getInstance() == null) {
            res = getClient().getChannelInstance().getField(fieldID);
            if (res != null) {
                initCustomJaguarField(res);
            }
        } else {
            res = getInstance().getField(fieldID);
            if (res != null) {
                res.setRuneStone(null);
                if (res.getId() == 926100401 || res.getId() == 926110401) {
                    res.initRomeoJulietPQ();
                }
            }
        }
        return res;
    }

    /**
     * Warps this Char to a given field at the starting portal.
     *
     * @param fieldId the ID of the field to warp to
     */
    public void warp(int fieldId) {
        warp(getOrCreateFieldByCurrentInstanceType(fieldId));
    }

    /**
     * Warps this Char to a given field at the given portal. If the portal doesn't exist, takes the starting portal.
     *
     * @param fieldId  the ID of the field to warp to
     * @param portalId the ID of the portal where the Char should spawn
     */
    public void warp(int fieldId, int portalId) {
        Field field = getOrCreateFieldByCurrentInstanceType(fieldId);
        if (field == null) {
            field = getOrCreateFieldByCurrentInstanceType(FieldConstants.HENESYS_ID);
        }
        Portal portal = field.getPortalByID(portalId);
        if (portal == null) {
            portal = field.getDefaultPortal();
        }
        warp(field, portal);
    }

    /**
     * Warps this character to a given field, at the starting position.
     * See {@link #warp(Field, Portal) warp}.
     *
     * @param toField The field to warp to.
     */
    public void warp(Field toField) {
        if (toField == null) {
            toField = getOrCreateFieldByCurrentInstanceType(FieldConstants.HENESYS_ID);
        }
        warp(toField, toField.getPortalByName("sp"), false, true);
    }

    /**
     * Warps this Char to a given {@link Field}, with the Field's "sp" portal as spawn position.
     *
     * @param toField       The Field to warp to.
     * @param characterData Whether or not the character data should be encoded.
     */
    public void warp(Field toField, boolean characterData) {
        if (toField == null) {
            toField = getOrCreateFieldByCurrentInstanceType(FieldConstants.HENESYS_ID);
        }
        warp(toField, toField.getPortalByName("sp"), characterData, true);
    }

    public void warp(int fieldId, int portalId, boolean saveReturnMap) {
        Field field = getOrCreateFieldByCurrentInstanceType(fieldId);
        if (field == null) {
            field = getOrCreateFieldByCurrentInstanceType(FieldConstants.HENESYS_ID);
        }
        Portal portal = field.getPortalByID(portalId);
        if (portal == null) {
            portal = field.getDefaultPortal();
        }
        warp(field, portal, false, saveReturnMap);
    }

    /**
     * Warps this Char to a given {@link Field} and {@link Portal}. Will not include character data.
     *
     * @param toField  The Field to warp to.
     * @param toPortal The Portal to spawn at.
     */
    public void warp(Field toField, Portal toPortal) {
        warp(toField, toPortal, false, true);
    }

    /**
     * Sets the return portal to the nearest current portal.
     */
    public void setNearestReturnPortal() {
        Rect rect = new Rect(
                new Position(
                        getPosition().getX() - 30,
                        getPosition().getY() - 30),
                new Position(
                        getPosition().getX() + 50, // wide girth
                        getPosition().getY() + 50)
        );

        List<Portal> portals = getField().getClosestPortal(rect);

        if (portals.size() > 0) {
            setPreviousPortalID(portals.get(0).getId());
        } else {
            setPreviousPortalID(0);
        }
    }

    public void warp(Field toField, Portal portal, boolean characterData, boolean saveReturnMap) {
        warp(toField, portal, characterData, saveReturnMap, false);
    }

    /**
     * Warps this character to a given field, at a given portal.
     * Ensures that the previous map does not contain this Char anymore, and that the new field
     * does.
     * Ensures that all Lifes are immediately spawned for the new player.
     *
     * @param toField The {@link Field} to warp to.
     * @param portal  The {@link Portal} where to spawn at.
     */
    public void warp(Field toField, Portal portal, boolean characterData, boolean saveReturnMap, boolean isGM) {
        if (toField == null) return;
        var currentField = getField();
        if (!net.swordie.ms.connection.protocol.codec.FieldAvailability.active().isAvailable(toField.getId())) {
            // The client of the active profile has no such map and would crash while loading it (TMS 282.3).
            System.out.printf("[FieldAvailability] warp of %s to %d refused: map not in the client%n", getName(), toField.getId());
            if (currentField != null) {
                chatMessage("此地圖在目前的客戶端版本已不存在，無法傳送。(" + toField.getId() + ")");
                dispose();
                return;
            }
            toField = getOrCreateFieldByCurrentInstanceType(FieldConstants.HENESYS_ID);
            portal = toField.getPortalByName("sp");
        }
        if (currentField != null && !isGM && !net.swordie.ms.GameSettings.canEnterField(this, toField)) { dispose(); return; } // field.check.level
        toField = handleInstanceNull(toField);
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3)
            net.swordie.ms.client.character.tms282.Tms282MyHomeRuntime.beforeWarp(this, toField);
        var tsm = getTemporaryStatManager();
        List<AffectedArea> aas = new ArrayList<>(tsm.getAffectedAreas());
        var sm = getScriptManager();
        var job = getJobHandler();
        sm.cancelBossIntroIfLeaving(toField);
        for (AffectedArea aa : aas) {
            if (aa == null) continue;
            tsm.removeStatsBySkill(aa.getSkillID());
        }
        if (currentField != null) {
            if (saveReturnMap) {
                setPreviousFieldID(currentField.getId()); // this may be a bad idea in some cases? idk
                setNearestReturnPortal();
            }
            if (ScriptManagerImpl.GAME_SCRIPTS_ENABLED && net.swordie.ms.connection.protocol.Protocol.active().version()
                    == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                net.swordie.ms.world.tms282.collab.frieren.FrierenCollab.get().onFieldLeave(this, currentField);
            }
            TradeRoom leavingTrade = getTradeRoom();
            if (leavingTrade != null && currentField != toField) {
                // multiplayer-0930: a field change closes the trade window (the escrow goes back to both owners)
                Char partner = leavingTrade.getOtherChar(getId());
                leavingTrade.cancelTrade();
                write(net.swordie.ms.connection.packet.MiniroomPacket.cancelTrade());
                if (partner != null) {
                    partner.write(net.swordie.ms.connection.packet.MiniroomPacket.cancelTrade());
                }
            }
            currentField.removeChar(getId());
        }
        // multiplayer-0930: position, foothold and pets at the spawn portal BEFORE addChar builds the LP03D0 that
        // the others in toField receive (they saw the new user and its pets at the old map's coordinates, or (0,0)
        // on login, until its first move).
        getAvatarData().getCharacterStat().setPortal(portal != null ? portal.getId() : 0);
        Position spawnAt = new Position(portal != null ? portal.getX() : 0, portal != null ? portal.getY() : 0);
        setPosition(spawnAt);
        var spawnFh = toField.findFootHoldBelow(spawnAt);
        if (spawnFh != null) {
            setFoothold((short) spawnFh.getId());
        }
        for (Pet spawnPet : new ArrayList<>(getPets())) {
            if (spawnPet != null) {
                spawnPet.setPosition(spawnAt.deepCopy());
                spawnPet.setFh(spawnFh != null ? spawnFh.getId() : getFoothold());
            }
        }
        boolean tms282FieldChange = net.swordie.ms.connection.packet.Tms282RemoteBuffs.active();
        // Clear field-scoped state while still in the old field, before the destination's LP03D0 snapshot.
        if (tms282FieldChange) setField(toField);
        if (!isGM) toField.addChar(this);
        if (!tms282FieldChange) setField(toField);
        final OutPacket packet = Stage.setField(this, toField, getClient().getChannel(), false, 0,
                characterData, false, (portal != null ? (byte) portal.getId() : (byte) 0),  null,
                100, null, -1);
        write(packet);
        showProperUI(currentField != null ? currentField.getId() : -1, toField.getId());
        if (getParty() != null) {
            if (getParty().getOnlineChars().size() > 1) {
                for (Char pmChr : getParty().getOnlineChars()) {
                    write(UserRemote.receiveHP(pmChr));
                    pmChr.write(UserRemote.receiveHP(this));
                }
            }
        }
        write(WvsContext.partyResult(PartyResult.load(getParty())));
        if (getInstance() != null) {
            if (getInstance().getAchieveRatio() > 0) {
                write(FieldPacket.setAchieveRate(getInstance().getAchieveRatio()));
            }
            write(FieldPacket.practiceMode(isPracticeMode()));
        }
        if (getGuild() != null) {
            write(WvsContext.guildResult(GuildResult.response_GuildLoad_Success(getGuild())));
            if (getGuild().getAlliance() != null) {
                write(WvsContext.allianceResult(AllianceResult.loadDone(getGuild().getAlliance())));
                write(WvsContext.allianceResult(AllianceResult.loadGuildDone(getGuild().getAlliance())));
            }
        }
        toField.spawnLifesForChar(this);
        handleFieldChangeSummons(tsm, toField);
        if (ScriptManagerImpl.GAME_SCRIPTS_ENABLED && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            net.swordie.ms.world.tms282.collab.frieren.FrierenCollab.get().onFieldEnter(this);
        }
        notifyChanges();
        boolean newbieDialogue = net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                && net.swordie.ms.world.tms282.newbie.NewbieFlowService.get().replacesEntryScript(this);
        if (!newbieDialogue && !isGM && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            toField.execFirstUserEnterScript(this);
        }
        if (!newbieDialogue) toField.execUserEnterScript(this);
        if (ScriptManagerImpl.GAME_SCRIPTS_ENABLED)
            net.swordie.ms.scripts.event.EventManager.get().onPlayerChangedMap(this, toField);
        if (ScriptManagerImpl.GAME_SCRIPTS_ENABLED && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            net.swordie.ms.world.tms282.newbie.NewbieFlowService.get().onFieldEnter(this, toField.getId()); // step 7
            net.swordie.ms.world.tms282.revival.hub.RevivalHubs.onFieldEnter(this); // revival-1005 hub progress preload
        }
        if (toField.getTimeLimit() > 0) {
            Field warpTo = getOrCreateFieldByCurrentInstanceType(toField.getReturnMap());
            if (warpTo != null && toField.getReturnMap() != toField.getId()) {
                if (timeLimitTimer != null) {
                    timeLimitTimer.cancel(true);
                }
                new Clock(ClockType.SecondsClock, getField(), toField.getTimeLimit());
                timeLimitTimer = getTimer().addEvent(() -> warp(warpTo), toField.getTimeLimit(), TimeUnit.SECONDS);
            }
        }
        showDeathCount(getDeathCount());
        for (Mob mob : toField.getMobs()) {
            mob.addObserver(sm);
        }
        if (toField.isTown()) {
            write(FieldPacket.setQuickMoveInfo(getInstance() == null, GameConstants.getQuickMoveInfos()
                    .stream().filter(qmi -> !qmi.isNoInstances() || getField().isChannelField())
                    .collect(Collectors.toList())));
        }
        if (getBurningFieldLevel() > 0 && !toField.getMobGens().isEmpty()) {
            showBurningLevel();
            startBurningFieldTimer(toField);
        }
        if (!isGM) {
            if (JobConstants.isEvan(getJob()) || getJob() == 2001) {
                if (getJob() == 2001 || getJob() == 2200) setJob(2210);
                if (job instanceof Evan evan) evan.spawnDragon();
            }
            if (toField.getEliteState() == EliteState.EliteBoss) write(FieldPacket.eliteState(EliteState.EliteBoss, true, GameConstants.ELITE_BOSS_BGM, null, null));
            if (getFoxMan() != null && getSkillPet() != null && job instanceof Kanna kanna) kanna.spawnHaku();
            if (getActiveFamiliar() != null && !Arrays.stream(FieldConstants.BLOCKED_RUNE_MAPS).anyMatch(m -> m == getFieldID())) //toField.broadcastPacket(CFamiliar.familiarEnterField(getId(), true, getActiveFamiliar(), true, false));
            if (getAndroid() != null) toField.spawnLife(getAndroid(), null);
        }
        sendPacketRewards();
        job.handleInitAfterField();
        if (getBeautySalon() != null) write(UserLocal.beautyDataResult(getBeautySalon()));
        if (isTurnOffBackground()) {
            //sm.setFieldColour(GreyFieldType.Background, (short) 0, (short) 0, (short) 0, 1000);
        } else {
            //sm.setFieldColour(GreyFieldType.Background, (short) 250, (short) 250, (short) 250, 1000);
        }
        if (toField.isDefaultClock()) {
            var t = java.time.LocalTime.now();
            write(FieldPacket.clock(ClockPacket.hmsClock((byte) t.getHour(),(byte) t.getMinute(),(byte) t.getSecond())));
        }
        getTimer().addEvent(() -> {
            initPets();
            openUIOnDead();
            sm.sendAutoEventClock();
            write(ExtraTMSSystem.initField(false, this, extraTMSSystem.magicNumber));
        }, 2500, TimeUnit.MILLISECONDS);

        write(WvsContext.sendExtraSystemStack(0, -1639974713, (byte) 246));
        write(WvsContext.sendExtraSystemStack(1, -1639974713, (byte) 247));
        write(WvsContext.sendExtraSystemStack(2, -1639974713, (byte) 248));
        write(WvsContext.sendExtraSystemInit());
        //write(WvsContext.infernoSphereRequest());

        AchievementHandler.handleFieldEnter(this, toField.getId()); // ?? err38
    }

    public void handleFieldChangeSummons(TemporaryStatManager tsm, Field toField) {
        if (!tsm.hasStat(IndieEmpty)) {
            return;
        }
        try {
            List<Integer> removedSkillIds = new ArrayList<>();
            List<Option> indieOptions = tsm.getOptions(IndieEmpty);
            for (Option option : indieOptions) {
                Summon summon = option.summon;
                if (summon == null) {
                    continue; // Skip if there's no summon associated.
                }
                if (summon.getMoveAbility().changeFieldWithOwner()) {
                    Position pos = getPosition().deepCopy();
                    summon.setPosition(pos);
                    summon.setCurFoothold(toField.findFootHoldBelow(pos) != null ? (short) toField.findFootHoldBelow(pos).getId() : 0);
                    summon.setObjectId(toField.getNewObjectID());
                    toField.spawnLife(summon, null);
                } else {
                    removedSkillIds.add(option.nReason);
                }
            }
            for (Integer skillId : removedSkillIds) {
                tsm.removeStatsBySkill(skillId);
            }
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public Field handleInstanceNull(Field toField) {
        InGameEvent openEvent = InGameEventManager.getInstance().getOpenEvent();
        InGameEvent activeEvent = InGameEventManager.getInstance().getActiveEvent();
        if (toField.getId() == 109050000) {
            if (openEvent == null) {
                toField = getOrCreateFieldByCurrentInstanceType(FieldConstants.HENESYS_ID);
            }
            if (activeEvent == null) {
                toField = getOrCreateFieldByCurrentInstanceType(FieldConstants.HENESYS_ID);
            }
        }
        if (toField.getId() == 993010000) {
            toField = getOrCreateFieldByCurrentInstanceType(FieldConstants.PLANET_GLACIUS);
        }
        if (toField.getId() >= GameConstants.EVOLVING_LINK_MAP_1 && toField.getId() <= GameConstants.EVOLVING_CENTRAL_CONTROL_MAP) {
            if (getInstance() == null) {
                toField = getOrCreateFieldByCurrentInstanceType(GameConstants.EVOLVING_ENTRANCE_MAP);
            }
        }
        if (toField.getId() == GameConstants.FIRST_TIME_TOGETHER_STAGE_1 || toField.getId() == GameConstants.FIRST_TIME_TOGETHER_STAGE_4) {
            if (getInstance() == null) {
                toField = getOrCreateFieldByCurrentInstanceType(GameConstants.FIRST_TIME_TOGETHER_ENTRANCE_MAP);
            }
        }
        if (toField.getId() == 933001000 || toField.getId() == 910010000 || toField.getId() == 910010001) {
            if (getInstance() == null) {
                toField = getOrCreateFieldByCurrentInstanceType(GameConstants.MOON_BUNNY_ENTRANCE_MAP);
            }
        }
        if (toField.getId() == 272020200 || toField.getId() == 272020210) {
            if (getInstance() == null) {
                toField = getOrCreateFieldByCurrentInstanceType(BossConstants.ARKARIUM_ENTRACE_MAP);
            }
        }
        if (toField.getId() == 271041100 || toField.getId() == 271040100) {
            if (getInstance() == null) {
                toField = getOrCreateFieldByCurrentInstanceType(BossConstants.CYGNUS_ENTRACE_MAP);
            }
        }
        if (toField.getId() == 350160200
                || toField.getId() == 350160240
                || toField.getId() == 350160100
                || toField.getId() == 350160140) {
            if (getInstance() == null) {
                toField = getOrCreateFieldByCurrentInstanceType(BossConstants.DEMIAN_ENTRACE_MAP);
            }
        }
        if (toField.getId() == 262030100
                || toField.getId() == 262030200
                || toField.getId() == 262030300
                || toField.getId() == 262031100
                || toField.getId() == 262031200
                || toField.getId() == 262031300) {
            if (getInstance() == null) {
                toField = getOrCreateFieldByCurrentInstanceType(BossConstants.HILLA_ENTRANCE_MAP);
            }
        }
        if (toField.getId() == 240060000
                || toField.getId() == 240060100
                || toField.getId() == 240060300
                || toField.getId() == 240060002
                || toField.getId() == 240060102
                || toField.getId() == 240060200
                || toField.getId() == 240060001
                || toField.getId() == 240060101
                || toField.getId() == 240060201) {
            if (getInstance() == null) {
                toField = getOrCreateFieldByCurrentInstanceType(BossConstants.HORNTAIL_ENTRANCE_MAP);
            }
        }
        if (toField.getId() == 350060400
                || toField.getId() == 350060500
                || toField.getId() == 350060600
                || toField.getId() == 350060700
                || toField.getId() == 350060800
                || toField.getId() == 350060900) {
            if (getInstance() == null) {
                toField = getOrCreateFieldByCurrentInstanceType(BossConstants.LOTUS_ENTRANCE_MAP);
            }
        }
        if (toField.getId() == 450004150
                || toField.getId() == 450004250
                || toField.getId() == 450004300) {
            if (getInstance() == null) {
                toField = getOrCreateFieldByCurrentInstanceType(BossConstants.LUCID_ENTRACE_MAP);
            }
        }
        if (toField.getId() == 401060100
                || toField.getId() == 401060200) {
            if (getInstance() == null) {
                toField = getOrCreateFieldByCurrentInstanceType(BossConstants.MAGNUS_HARD_ENTRANCE_MAP);
            }
        }
        if (toField.getId() == 401060300) {
            if (getInstance() == null) {
                toField = getOrCreateFieldByCurrentInstanceType(BossConstants.MAGNUS_EASY_ENTRANCE_MAP);
            }
        }
        if (toField.getId() == 270050100
                || toField.getId() == 270051100) {
            if (getInstance() == null) {
                toField = getOrCreateFieldByCurrentInstanceType(BossConstants.PINK_BEAN_ENTRACE_MAP);
            }
        }
        if (toField.getId() == 807300110
                || toField.getId() == 807300210) {
            if (getInstance() == null) {
                toField = getOrCreateFieldByCurrentInstanceType(BossConstants.RANMARU_ENTRACE_MAP);
            }
        }
        if (toField.getId() == 211070100
                || toField.getId() == 211070102
                || toField.getId() == 211070104) {
            if (getInstance() == null) {
                toField = getOrCreateFieldByCurrentInstanceType(BossConstants.VON_LEON_ENTRACE_MAP);
            }
        }
        if (toField.getId() == 280030000
                || toField.getId() == 280030100
                || toField.getId() == 280030200) {
            if (getInstance() == null) {
                toField = getOrCreateFieldByCurrentInstanceType(BossConstants.ZAKUM_CHAOS_ENTRANCE);
            }
        }
        if (toField.getId() == 863010100
                || toField.getId() == 863010200
                || toField.getId() == 863010210
                || toField.getId() == 863010220
                || toField.getId() == 863010230
                || toField.getId() == 863010240
                || toField.getId() == 863010300
                || toField.getId() == 863010310
                || toField.getId() == 863010320
                || toField.getId() == 863010400
                || toField.getId() == 863010410
                || toField.getId() == 863010420
                || toField.getId() == 863010600
                || toField.getId() == 863010700) {
            if (getInstance() == null) {
                toField = getOrCreateFieldByCurrentInstanceType(BossConstants.GOLLUX_ENTRACE_MAP);
            }
        }
        if (toField.getId() == 105200100
                || toField.getId() == 105200110
                || toField.getId() == 105200200
                || toField.getId() == 105200210
                || toField.getId() == 105200300
                || toField.getId() == 105200310
                || toField.getId() == 105200400
                || toField.getId() == 105200410
                || toField.getId() == 105200500
                || toField.getId() == 105200510
                || toField.getId() == 105200600
                || toField.getId() == 105200610
                || toField.getId() == 105200700
                || toField.getId() == 105200710
                || toField.getId() == 105200800
                || toField.getId() == 105200810) {
            if (getInstance() == null) {
                toField = getOrCreateFieldByCurrentInstanceType(BossConstants.ROOT_ABYSS_ENTRACE_MAP);
            }
        }
        if (toField.getId() == 450008100
                || toField.getId() == 450008150
                || toField.getId() == 450008200
                || toField.getId() == 450008250
                || toField.getId() == 450008300
                || toField.getId() == 450008350
                || toField.getId() == 450008400
                || toField.getId() == 450008700
                || toField.getId() == 450008750
                || toField.getId() == 450008800
                || toField.getId() == 450008850
                || toField.getId() == 450008900
                || toField.getId() == 450008950) {
            if (getInstance() == null) {
                toField = getOrCreateFieldByCurrentInstanceType(BossConstants.WILL_ENTRACE_MAP);
            }
        }
        if (getInstance() == null) {
            int entrance = BossConstants.getBossEntranceMapForFightField(toField.getId());
            if (entrance > 0) {
                toField = getOrCreateFieldByCurrentInstanceType(entrance);
            }
        }
        return toField;
    }

    public void initCustomJaguarField(Field res) {
        if (res.getId() == 931000500) {
            if (Randomizer.isSuccess(20)) {
                chatMessage("這裡出現了可以捕捉的特殊美洲豹");
                List<Integer> jaguar = List.of(9304005, 9304006, 9304007, 9304008);
                boolean isExits = false;
                for (Mob mob : res.getMobs()) {
                    if (jaguar.contains(mob.getTemplateId())) {
                        isExits = true;
                        break;
                    }
                }
                if (!isExits) {
                    Mob mob = MobData.getMobDeepCopyById(jaguar.get(Randomizer.rand(0, 3)));
                    res.spawnMob(mob.getTemplateId(), mob.getHomePosition().getX(), mob.getHomePosition().getY(), false, mob.getMaxHp());
                    res.setDropsDisabled(true);
                }
            }
        }
    }

    /**
     * Adds a given amount of exp to this Char. Immediately checks for level-up possibility, and
     * sends the updated
     * stats to the client. Allows multi-leveling.
     *
     * @param amount The amount of exp to add.
     */
    public void addExp(long amount, boolean isAffectedByExpRate) {
        ExpIncreaseInfo eii = new ExpIncreaseInfo();
        eii.setLastHit(true);
        eii.setIncEXP(Util.maxInt(amount));
        addExp(amount, eii, isAffectedByExpRate);
    }

    /**
     * Adds exp to this Char. Will calculate the extra exp gained from buffs and the exp rate of the server.
     * Also takes an argument to show this info to the client. Will not send anything if this argument (eii) is null.
     *
     * @param amount The amount of exp to add
     * @param eii    The info to send to the client
     */
    public void addExp(long amount, ExpIncreaseInfo eii, boolean isAffectedByExpRate) {
        var profileExp = net.swordie.ms.connection.protocol.Protocol.active().codecs()
                .find(net.swordie.ms.connection.protocol.codec.ExpGainStrategy.class);
        if (profileExp.isPresent()) { // a profile whose EXP/level packets differ from 265 (TMS 282.3)
            profileExp.get().addExp(this, amount, eii);
            return;
        }
        boolean isLevelUp = false;
        if (amount <= 0 || getHP() <= 0) {
            return;
        }
        CharacterStat cs = getAvatarData().getCharacterStat();
        int level = getLevel();
        long curExp = cs.getExp();
        if (curExp < 0) {
            curExp = Math.abs(curExp);
        }
        if (level >= GameConstants.MAX_LEVEL) {
            if (curExp > 0) {
                cs.setExp(0);
            }
            return;
        }
        if (hasQuest(100716)) {
            int decExpR = Integer.parseInt(getQRValueByKey(100716, "decExpR"));
            long decExp = (long) amount * decExpR / 100;
            amount -= decExp;
            chatMessage(GameDesc, "由於死亡懲罰效果，你被扣除了 " + Util.getNumberFormat(decExp) + " EXP。");
        }
        long newExp = curExp + amount;
        final int hyperMax = 260;
        final int beyondMax = 270;
        final boolean hyperBurningMax = EventConstants.HYPER_BURNING_MAX;
        final boolean beyondBurning = EventConstants.BEYOND_BURNING;
        final boolean isZero = JobConstants.isZero(getJob());
        Map<Stat, Object> stats = new HashMap<>();
        while (level < GameConstants.MAX_LEVEL && newExp >= GameConstants.charExp[level]) {
            newExp -= GameConstants.charExp[level];
            int step = 1;
            if (hyperBurningMax && level < hyperMax) {
                int start = isZero ? 100 : 10;
                if (level >= start) {
                    step = Math.min(5, hyperMax - level);
                    if (step < 1) step = 1;
                }
            }
            else if (beyondBurning && level >= hyperMax && level < beyondMax) {
                step = Math.min(2, beyondMax - level);
                if (step < 1) step = 1;
            }
            for (int i = 0; i < step; i++) {
                if (level >= GameConstants.MAX_LEVEL) break;
                level++;
                addStat(Stat.level, 1);
                stats.put(Stat.level, level);
                getJobHandler().handleLevelUp((short) level);
                getField().broadcast(UserRemote.effect(getId(), Effect.levelUpEffect()), this);
                isLevelUp = true;
                if (level > 199) {
                    DataPrinter.send(DataPrinter.ALL_IN_ONE, "角色 " + getName() + " 已升到 " + level + " 級");
                }
            }
        }
        if (level >= GameConstants.MAX_LEVEL) {
            level = GameConstants.MAX_LEVEL;
            newExp = 0;
        }
        if (newExp < 0) {
            newExp = Math.abs(newExp);
        }
        cs.setExp(newExp);
        stats.put(Stat.exp, newExp);
        if (eii != null) {
            write(WvsContext.incExpMessage(eii));
        }
        sendStatsPacket(stats);
        if (isLevelUp && level > 99) {
            if (getGuild() != null) {
                getGuild().broadcast(FieldPacket.groupMessage(GroupMessageType.Guild, this, "已升到 " + level + " 級！"));
            }
            getAvatarData().getCharacterStat().updateCharacterStatToSQL();
            if (getGuild() != null) {
                GuildMember gm = getGuild().getMemberByCharID(getId());
                if (gm != null) {
                    gm.setLevel(getLevel());
                    gm.updateGuildMemberToSQL();
                }
            }
        }
        if (getIdleHunting() != null && isStartedHunting()) {
            getIdleHunting().incEXP(amount);
            getIdleHunting().incMob(1);
        }
    }

    /**
     * Adds a given amount of exp to this Char, however it does not display the Exp Message.
     * Immediately checks for level-up possibility, and sends the updated
     * stats to the client. Allows multi-leveling.
     *
     * @param amount The amount of exp to add.
     */
    public void addExpNoMsg(long amount) {
        addExp(amount, null, true);
    }

    public void addTraitExp(Stat traitStat, int amount) {
        int traitExp = getStat(traitStat);
        if (traitExp >= GameConstants.MAX_TRAIT_EXP) {
            chatPopup("你的 " + traitStat.name() + " 等級已達到上限！");
            return;
        }
        if (traitExp + amount > GameConstants.MAX_TRAIT_EXP) {
            amount = GameConstants.MAX_TRAIT_EXP - traitExp;
        } else if (traitExp + amount < 0) {
            amount = 0;
        }
        Map<Stat, Object> stats = new HashMap<>();
        addStat(traitStat, amount);
        stats.put(traitStat, traitExp + amount);
        stats.put(Stat.dayLimit, getAvatarData().getCharacterStat().getNonCombatStatDayLimit());
        sendStatsPacket(stats);
        write(WvsContext.incNonCombatStatEXPMessage(traitStat, amount));
        if (getTraitLevelByExp(traitExp) < getTraitLevelByExp(getStat(traitStat))) {
            //initBaseStats();
            //initStatPercent();
            initTraits();
        }
    }

    /**
     * Adds exp to the equipment based on killing mobs.
     *
     * @param expGain The amount of exp to add
     */
    public void addEquipExp(long expGain) {
        if (expGain < 0) {
            expGain = 0;
        }
        for (Item item : getEquippedInventory().getItems()) {
            Equip equip = (Equip) item;
            if (equip == null) {
                return;
            }
            equip.gainItemExp(this, expGain);
        }
    }

    /**
     * Writes a packet to this Char's client.
     *
     * @param outPacket The OutPacket to write.
     */
    public void write(OutPacket outPacket) {
        if (!isBot() && client != null) {
            client.write(outPacket);
        }
    }

    public ExpIncreaseInfo getExpIncreaseInfo() {
        return new ExpIncreaseInfo();
    }

    public WildHunterInfo getWildHunterInfo() {
        return wildHunterInfo;
    }

    public void setWildHunterInfo(WildHunterInfo wildHunterInfo) {
        this.wildHunterInfo = wildHunterInfo;
    }

    public ZeroInfo getZeroInfo() {
        return zeroInfo;
    }

    public void setZeroInfo(ZeroInfo zeroInfo) {
        this.zeroInfo = zeroInfo;
    }

    public Set<MedalAchievementInfo> getMedalAchievementInfo() {
        return medalAchievementInfo;
    }

    public void setMedalAchievementInfo(Set<MedalAchievementInfo> medalAchievementInfo) {
        this.medalAchievementInfo = medalAchievementInfo;
    }

    public void addMedalAchievementInfo(MedalAchievementInfo medal) {
        getMedalAchievementInfo().add(medal);
    }

    public MedalAchievementInfo findMedalByID(int itemID, int questID) {
        for (MedalAchievementInfo medal : medalAchievementInfo) {
            if (medal.getItemID() == itemID && medal.getQuestID() == questID) {
                return medal;
            }
        }
        return null;
    }

    public int getEquippedMedalItem() {
        return equippedMedalItem;
    }

    public void setEquippedMedalItem(int equippedMedalItem) {
        this.equippedMedalItem = equippedMedalItem;
    }

    public DamageSkinSaveData getActiveDamageSkin() {
        if (this.activeDamageSkin == null) {
            DamageSkinSaveData skin = getDamageSkins().iterator().next();
            if (skin == null) {
                return new DamageSkinSaveData(getId(),0, 0, false, "預設傷害字型。", FileTime.currentTime());
            } else {
                return skin;
            }
        }
        return this.activeDamageSkin;
    }

    public void setActiveDamageSkin(DamageSkinSaveData activeDamageSkin) {
        this.activeDamageSkin = activeDamageSkin;
    }

    public DamageSkinSaveData getPremiumDamageSkin() {
        if (premiumDamageSkin == null) {
            return new DamageSkinSaveData(-1, 0, true, "");
        }
        return premiumDamageSkin;
    }

    public void setPremiumDamageSkin(DamageSkinSaveData premiumDamageSkin) {
        this.premiumDamageSkin = premiumDamageSkin;
    }

    public DamageSkinSaveData getNewDamageSkin() {
        if (newDamageSkin == null) {
            return new DamageSkinSaveData(-1, 0, true, "");
        }
        return newDamageSkin;
    }

    public void setNewDamageSkin(DamageSkinSaveData newDamageSkin) {
        this.newDamageSkin = newDamageSkin;
    }

    public void addDamageSkin(DamageSkinSaveData dssd) {
        if (getDamageSkinByItemID(dssd.getItemID()) == null) {
            getDamageSkins().add(dssd);
        }
    }

    public void removeDamageSkin(DamageSkinSaveData dssd) {
        if (dssd != null) {
            getDamageSkins().removeIf(x -> x.getDamageSkinID() == dssd.getDamageSkinID() && x.getItemID() == dssd.getItemID());
        }
    }

    public void removeDamageSkin(int itemID) {
        removeDamageSkin(getDamageSkinByItemID(itemID));
    }

    public void addDamageSkinByItemID(int itemID) {
        addDamageSkin(new DamageSkinSaveData(ItemConstants.getDamageSkinIDByItemID(itemID), itemID, false,
                StringData.getItemStringById(itemID)));
    }

    public DamageSkinSaveData getDamageSkinByItemID(int itemID) {
        return getDamageSkins().stream().filter(d -> d.getItemID() == itemID).findAny().orElse(null);
    }

    public DamageSkinSaveData getDamageSkinBySkinID(int skinID) {
        return getDamageSkins().stream().filter(d -> d.getDamageSkinID() == skinID).findAny().orElse(null);
    }

    public Set<DamageSkinSaveData> getDamageSkins() {
        if (damageSkins.isEmpty()) {
            damageSkins.add(new DamageSkinSaveData(getId(),0, 0, false, "預設傷害字型。", FileTime.currentTime()));
        }
        return damageSkins;
    }

    public void setDamageSkins(Set<DamageSkinSaveData> damageSkins) {
        this.damageSkins = damageSkins;
    }

    /**
     * Returns if this Char can be invited to a party.
     *
     * @return Whether or not this Char can be invited to a party.
     */
    public boolean isPartyInvitable() {
        return partyInvitable;
    }

    public void setPartyInvitable(boolean partyInvitable) {
        this.partyInvitable = partyInvitable;
    }

    /**
     * Returns if this character is currently in its beta state.
     *
     * @return true if this Char is in a beta state.
     */
    public boolean isZeroBeta() {
        return getZeroInfo().isZeroBetaState();
    }

    /**
     * Zero only.
     * Goes into Beta form if Alpha, and into Alpha if Beta.
     */
    public void swapZeroState(int oldTF, int newTF) {
        if (!(JobConstants.isZero(getJob())) || getZeroInfo() == null) {
            return;
        }
        CharacterStat characterStat = getAvatarData().getCharacterStat();
        ZeroInfo currentInfo = getZeroInfo();
        if (characterStat == null || currentInfo == null) {
            return;
        }
        int nCurrentHP = characterStat.getHp();
        int nCurrentMaxHP = characterStat.getMaxHp();
        int nCurrentTP = newTF > characterStat.getMaxMp() ? characterStat.getMp() : newTF; // Nếu new Time Force lớn hơn Max Time Force thì = Current Time Force False thì = new Time Force
        int nCurrentMaxTP = characterStat.getMaxMp();

        characterStat.setHp(currentInfo.getSubHP());
        characterStat.setMaxHp(currentInfo.getSubMHP());
        characterStat.setMp(oldTF > currentInfo.getSubMMP() ? currentInfo.getSubMP() : oldTF);
        characterStat.setMaxMp(currentInfo.getSubMMP());

        currentInfo.setZeroBetaState(!currentInfo.isZeroBetaState());
        currentInfo.setSubHP(nCurrentHP);
        currentInfo.setSubMHP(nCurrentMaxHP);
        currentInfo.setSubMP(nCurrentTP);
        currentInfo.setSubMMP(nCurrentMaxTP);

        Map<Stat, Object> updatedStats = new HashMap<>();
        updatedStats.put(Stat.hp, characterStat.getHp());
        updatedStats.put(Stat.mhp, characterStat.getMaxHp());
        updatedStats.put(Stat.mp, characterStat.getMp());
        updatedStats.put(Stat.mmp, characterStat.getMaxMp());
        sendStatsPacket(updatedStats);
    }

    /**
     * Initializes zero info with HP values.
     */
    public void initZeroInfo() {
        ZeroInfo zeroInfo = new ZeroInfo();
        CharacterStat cs = getAvatarData().getCharacterStat();
        AvatarLook al = getAvatarData().getAvatarLook(true);
        zeroInfo.setSubHP(cs.getHp());
        zeroInfo.setSubMHP(cs.getMaxHp());
        zeroInfo.setSubMP(cs.getMp());
        zeroInfo.setSubMMP(cs.getMaxMp());
        zeroInfo.setSubSkin(al.getSkin());
        zeroInfo.setSubHair(al.getHair());
        zeroInfo.setSubFace(al.getFace());
        setZeroInfo(zeroInfo);
    }

    public ScriptManagerImpl getScriptManager() {
        return scriptManagerImpl;
    }

    /**
     * Adds a {@link Drop} to this Char.
     *
     * @param drop The Drop that has been picked up.
     */
    public boolean addDrop(Drop drop, boolean isPetLoot) {
        net.swordie.ms.world.tms282.audit.AuditContext.mark(this, net.swordie.ms.world.tms282.audit.AuditLog.PICKUP, drop.getOwnerID()); // audit label
        if (drop.isMoney()) {
            int bonusMesos = 0;
            if (getGuild() != null) {
                GuildSkill gs = getGuild().getSkillById(GuildConstants.SPOTTING_SMALL_CHANGE);
                SkillInfo si = SkillData.getSkillInfoById(GuildConstants.SPOTTING_SMALL_CHANGE);
                if (gs != null && si != null) {
                    bonusMesos = si.getValue(mesoG, gs.getLevel());
                }
            }
            long mesoGained = drop.getMoney() + bonusMesos;
            addMoney(mesoGained);
            handleMoneyGain(drop.getMoney());
            write(WvsContext.dropPickupMessage(drop.getMoney(), (short) 0, (short) bonusMesos));
            write(WvsContext.mobDropMesoPickUp(drop.getMoney()));
            if (getIdleHunting() != null && isStartedHunting()) {
                getIdleHunting().incMeso(mesoGained);
            }
            if (!isPetLoot) {
                dispose();
            }
            return true;
        } else {
            Item item = drop.getItem();
            int itemID = item.getItemId();
            int quantity = item.getQuantity();
            boolean isConsume = false;
            boolean isRunOnPickUp = false;
            if (!ItemConstants.isEquip(itemID)) {
                ItemInfo ii = ItemData.getItemInfoByID(itemID);
                if (ii == null) {
                    return false;
                }
                isConsume = ii.getSpecStats().getOrDefault(SpecStat.consumeOnPickup, 0) != 0;
                isRunOnPickUp = ii.getSpecStats().getOrDefault(SpecStat.runOnPickup, 0) != 0 || itemID == 2433808 || itemID == 2434021 || itemID == 2434288 || itemID == 2434290; // Special Medal of Honor
            }

            if (itemID == GameConstants.BLUE_EXP_ORB_ID || itemID == GameConstants.PURPLE_EXP_ORB_ID || itemID == GameConstants.RED_EXP_ORB_ID || itemID == GameConstants.YELLOW_EXP_ORB_ID) {
                int linkSkillID = hasSkill(20000297) ? 20000297 : hasSkill(80000370) ? 80000370 : 0;
                int expBonusByLinkSkill = 0;
                if (linkSkillID != 0) {
                    SkillInfo si = SkillData.getSkillInfoById(linkSkillID);
                    expBonusByLinkSkill = (int) (si.getValue(x, getSkillLevel(linkSkillID)) / 100.0D);
                }
                long expGain = (long) ((drop.getMobExp() * GameConstants.getExpOrbExpModifierById(itemID)) * (1 + expBonusByLinkSkill));
                write(UserPacket.effect(Effect.fieldItemConsumed((int) (expGain > Integer.MAX_VALUE ? Integer.MAX_VALUE : expGain))));
                addExpNoMsg(expGain);
                // Exp Orb Buff On Pickup
                TemporaryStatManager tsm = getTemporaryStatManager();
                ItemBuffs.giveItemBuffsFromItemID(this, tsm, itemID);
            } else if (itemID == 4001847) {
                spiritBondMax();
                dispose();
                return true;
            }
            if (isConsume) {
                consumeItemOnPickup(item);
                dispose();
                return true;
            } else if (isRunOnPickUp) {
                String script = String.valueOf(itemID);
                ItemInfo ii = ItemData.getItemInfoByID(itemID);
                if (ii == null) {
                    return false;
                }
                if (handleSpecialRunOnPickUp(itemID, quantity)) {
                    return true;
                }
                if (ii.getScript() != null && !"".equals(ii.getScript())) {
                    script = ii.getScript();
                }
                return getScriptManager().startScript(this, itemID, script, ScriptType.Item);
            } else if (getInventoryByType(item.getInvType()).canPickUp(item)) {
                if (item instanceof Equip equip) {
                    if (equip.hasAttribute(EquipAttribute.UnTradableAfterTransaction)) {
                        equip.removeAttribute(EquipAttribute.UnTradableAfterTransaction);
                        equip.addAttribute(EquipAttribute.UnTradable);
                    }
                    if (ItemConstants.isArcaneSymbol(equip.getItemId())) {
                        equip.getSymbol().setLevel(1);
                        equip.getSymbol().setExp(1);
                        equip.getSymbol().setInc((short) 30);
                        if (JobConstants.isDemonAvenger(getJob())) {
                            equip.setiMaxHp((short) (equip.getItemId() == 1712000 ? 4200 : 5250));
                        } else if (JobConstants.isXenon(getJob())) {
                            equip.setiStr((short) 117);
                            equip.setiDex((short) 117);
                            equip.setiLuk((short) 117);
                        } else {
                            equip.setBaseStat(calculateMainStatForChar(), 300);
                        }
                    } else if (ItemConstants.isSacredSymbol(equip.getItemId())) {
                        equip.getSymbol().setLevel(1);
                        equip.getSymbol().setExp(1);
                        equip.getSymbol().setInc((short) 10);
                        if (JobConstants.isDemonAvenger(getJob())) {
                            equip.setiMaxHp((short) 10500);
                        } else if (JobConstants.isXenon(getJob())) {
                            equip.setiStr((short) 240);
                            equip.setiDex((short) 240);
                            equip.setiLuk((short) 240);
                        } else {
                            equip.setBaseStat(calculateMainStatForChar(), 500);
                        }
                    }
                }
                if (item.isCash()) {
                    if (item instanceof PetItem petItem) {
                        petItem.setDateDead(FileTime.fromDate(LocalDateTime.now().plusDays(ItemConstants.CASH_ITEM_AVAILABLE_DAYS)));
                    } else {
                        item.setDateExpire(FileTime.fromDate(LocalDateTime.now().plusDays(ItemConstants.CASH_ITEM_AVAILABLE_DAYS)));
                    }
                }
                if (GameConstants.isIntensePowerCrystal(item.getItemId())) {
                    if (getLastBossTemplateID() != 0) {
                        int partySize = (getParty() != null) ? getParty().getMembers().size() : 1;
                        long price = GameConstants.getBossRewardPrice(getLastBossTemplateID());
                        long sellingPrice = price / partySize;
                        item.setBossRewardID(GameConstants.getBossRewardID(getLastBossTemplateID()));
                        item.setPartySize(partySize);
                        item.setPrice(sellingPrice);
                        item.setDateExpire(FileTime.fromDate(LocalDateTime.now().plusDays(7)));
                        setLastBossTemplateID(0);
                    } else {
                        chatMessage("無法撿取這個強烈的力量結晶。");
                        dispose();
                        return false;
                    }
                }
                addItemToInventory(item, isPetLoot);
                write(WvsContext.dropPickupMessage(item, (short) item.getQuantity()));
                return true;
            } else {
                write(WvsContext.dropPickupMessage(0, (byte) -1, (short) 0, (short) 0, (short) 0));
                dispose();
                return false;
            }
        }
    }

    public boolean handleSpecialRunOnPickUp(int itemID, int quantity) {
        if (HexaMatrixConstants.isSolErdaEnergy(itemID)) {
            if (HexaMatrixConstants.isFaintSolErdaEnergy(itemID)) {
                addSolErdaStrength(10 * quantity);
                return true;
            } else if (HexaMatrixConstants.isCommonSolErdaEnergy(itemID)) {
                addSolErdaStrength(200 * quantity);
                return true;
            } else if (HexaMatrixConstants.isDenseSolErdaEnergy(itemID)) {
                addSolErdaStrength(500 * quantity);
                return true;
            }
        }
        return false;
    }

    public boolean addItemFromPetVac(PetVac petVac) {
        Item item = ItemData.getItemDeepCopy(petVac.getItemID());
        item.setQuantity(petVac.getQuantity());
        if (getInventoryByType(item.getInvType()).canPickUp(item)) {
            if (item instanceof Equip equip) {
                if (equip.hasAttribute(EquipAttribute.UnTradableAfterTransaction)) {
                    equip.removeAttribute(EquipAttribute.UnTradableAfterTransaction);
                    equip.addAttribute(EquipAttribute.UnTradable);
                }
                if (ItemConstants.isArcaneSymbol(equip.getItemId())) {
                    equip.getSymbol().setLevel(1);
                    equip.getSymbol().setExp(1);
                    equip.getSymbol().setInc((short) 30);
                    if (JobConstants.isDemonAvenger(getJob())) {
                        equip.setiMaxHp((short) (petVac.getItemID() == 1712000 ? 4200 : 5250));
                    } else if (JobConstants.isXenon(getJob())) {
                        equip.setiStr((short) 117);
                        equip.setiDex((short) 117);
                        equip.setiLuk((short) 117);
                    } else {
                        equip.setBaseStat(calculateMainStatForChar(), 300);
                    }
                } else if (ItemConstants.isSacredSymbol(equip.getItemId())) {
                    equip.getSymbol().setLevel(1);
                    equip.getSymbol().setExp(1);
                    equip.getSymbol().setInc((short) 10);
                    if (JobConstants.isDemonAvenger(getJob())) {
                        equip.setiMaxHp((short) 10500);
                    } else if (JobConstants.isXenon(getJob())) {
                        equip.setiStr((short) 240);
                        equip.setiDex((short) 240);
                        equip.setiLuk((short) 240);
                    } else {
                        equip.setBaseStat(calculateMainStatForChar(), 500);
                    }
                }
            }
            if (item.isCash()) {
                if (item instanceof PetItem petItem) {
                    petItem.setDateDead(FileTime.fromDate(LocalDateTime.now().plusDays(ItemConstants.CASH_ITEM_AVAILABLE_DAYS)));
                } else {
                    item.setDateExpire(FileTime.fromDate(LocalDateTime.now().plusDays(ItemConstants.CASH_ITEM_AVAILABLE_DAYS)));
                }
            }
            if (ItemConstants.isSymbol(item.getItemId())) {
                for (int i = 0; i <= petVac.getQuantity(); i++) {
                    addItemToInventory(item, true);
                }
            } else {
                addItemToInventory(item, true);
            }
            DataPrinter.send("log-pet-vac", "玩家 " + getName() + " 從 Pet Vac 獲得物品 " + petVac.getItemID() + "，數量: " + petVac.getQuantity() + "。");
            petVac.deletePetVacToSQL();
            return true;
        }
        return false;
    }

    public void addItemsFromPetVac() {
        List<PetVac> removeList = new ArrayList<>();
        for (PetVac petVac : getPetVacs()) {
            if (addItemFromPetVac(petVac)) {
                removeList.add(petVac);
            }
        }
        getPetVacs().removeAll(removeList);
    }

    public boolean addDrop(Drop drop) {
        Item item = drop.getItem();
        int itemID = item.getItemId();
        int quantity = item.getQuantity();
        boolean isConsume = false;
        boolean isRunOnPickUp = false;
        if (!ItemConstants.isEquip(itemID)) {
            ItemInfo ii = ItemData.getItemInfoByID(itemID);
            if (ii == null) {
                return false;
            }
            isConsume = ii.getSpecStats().getOrDefault(SpecStat.consumeOnPickup, 0) != 0;
            isRunOnPickUp = ii.getSpecStats().getOrDefault(SpecStat.runOnPickup, 0) != 0 || itemID == 2433808 || itemID == 2434021 || itemID == 2434288 || itemID == 2434290; // Special Medal of Honor
        }
        if (isConsume) {
            consumeItemOnPickup(item);
            return true;
        } else if (isRunOnPickUp) {
            String script = String.valueOf(itemID);
            ItemInfo ii = ItemData.getItemInfoByID(itemID);
            if (ii == null) {
                return false;
            }
            if (handleSpecialRunOnPickUp(itemID, quantity)) {
                return true;
            }
            if (ii.getScript() != null && !"".equals(ii.getScript())) {
                script = ii.getScript();
            }
            return getScriptManager().startScript(this, itemID, script, ScriptType.Item);
        } else {
            for (PetVac petVac : getPetVacs()) {
                if (petVac.getItemID() == itemID && !ItemConstants.isEquip(itemID)) {
                    petVac.setQuantity(petVac.getQuantity() + quantity);
                    return true;
                }
            }
            PetVac petVac = new PetVac();
            petVac.setCharID(getId());
            petVac.setItemID(itemID);
            petVac.setQuantity(quantity);
            getPetVacs().add(petVac);
            return true;
        }
    }

    public void handleDropForPQs(Drop drop) {
        if (!drop.isMoney()) {
            ScriptManagerImpl sm = getScriptManager();
            if (drop.getItem().getItemId() == GameConstants.RICE_CAKE && getFieldID() == GameConstants.MOON_BUNNY_STAGE) {
                if (getParty().isLeader(this)) {
                    int quantity = getScriptManager().getQuantityOfItem(GameConstants.RICE_CAKE);
                    if (quantity == 80) {
                        getField().broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.MoonBunny, "需要收集 80 個年糕！太好了，請再幫我一次。", 7000));
                        getField().broadcast(FieldPacket.setAchieveRate(100));
                    } else {
                        chatScriptMessage(String.format("你已經為興兒收集了 %d 個年糕，還需要 %d 個！", quantity, 80 - quantity));
                    }
                } else {
                    chatMessage("請把年糕交給隊伍。");
                }
            }
            if (drop.getItem().getItemId() >= 4034329 && drop.getItem().getItemId() <= 4034338 && getFieldID() == 933014000) {
                consumeItem(drop.getItem().getItemId(), 1);
                if (drop.getItem().getItemId() == 4034329) {
                    return;
                }
                Instance instance = getParty().getInstance();
                if (instance.hasProperty("kpq4clear")) {
                    chatMessage("請儘快前往下一個門！");
                    return;
                }
                if (instance.hasProperty("kpq4calculate")) {
                    if (!instance.hasProperty("kpq4result")) {
                        instance.addProperty("kpq4result", 0);
                    }
                    String type = (String) instance.getProperty("kpq4calculate");
                    int result = (int) instance.getProperty("kpq4result");
                    switch (type) {
                        case "+":
                            result += (drop.getItem().getItemId() - 4034329);
                            break;
                        case "-":
                            result -= (drop.getItem().getItemId() - 4034329);
                            if (result < 0) {
                                result = 0;
                            }
                            break;
                        case "x":
                            result *= (drop.getItem().getItemId() - 4034329);
                            break;
                        case "/":
                            result /= (drop.getItem().getItemId() - 4034329);
                            break;
                    }
                    result = (int) Math.floor(result);
                    if (result == (int) instance.getProperty("kpq4answer")) {
                        instance.addProperty("kpq4clear", getField());
                        sm.showEffectToField(WzConstants.EFFECT_PQ_CLEAR);
                        sm.playSound(WzConstants.EFFECT_PQ_SOUND_CLEAR, true);
                        sm.setObjectState("gate", 0);
                        sm.playPortalSoundToField();
                        sm.setAchieveRatio(4 * 20);
                    } else {
                        instance.setProperty("kpq4result", result);
                        chatScriptMessage("當前數字：" + result);
                        //write(UserLocal.addPopupSay(9076110, 1000, String.format("#eGet Number: #r %s #k\r\nCurrent Number: #b %s #k #n" , StringData.getItemStringById(drop.getItem().getItemId()), result), "FarmSE.img/boxResult"));
                    }
                }
            }
        }
    }

    private void consumeItemOnPickup(Item item) {
        int itemID = item.getItemId();
        if (ItemConstants.isMobCard(itemID)) {
            MonsterBookInfo mbi = getMonsterBookInfo();
            int id = 0;
            if (!mbi.hasCard(itemID)) {
                mbi.addCard(itemID);
                id = itemID;
            }
            write(WvsContext.monsterBookSetCard(id));
        }
        if (ItemConstants.isChangeStatItem(itemID)) {
            useStatChangeItem(item, false);
        }
    }

    /**
     * Returns the Char's name.
     *
     * @return The Char's name.
     */
    public String getName() {
        return getAvatarData().getCharacterStat().getName();
    }

    public void setName(String name) {
        getAvatarData().getCharacterStat().setName(name);
    }

    /**
     * Disposes this Char, allowing it to send packets to the server again.
     */
    public void dispose() {
        write(WvsContext.exclRequest());
    }

    /**
     * Returns the current HP of this Char.
     *
     * @return the current HP of this Char.
     */
    public int getHP() {
        return getStat(Stat.hp);
    }

    /**
     * Returns the current MP of this Char.
     *
     * @return the current MP of this Char.
     */
    public int getMP() {
        if (JobConstants.isKinesis(getJob()) && JobConstants.isNoManaJob(getJob())) { // 265: Kinesis MP is its PP; 282 has real MP
            return ((Kinesis) getJobHandler()).getPP();
        }
        return getStat(Stat.mp);
    }

    /**
     * Gets the total max hp of this Char.
     *
     * @return The total max hp of this Char
     */
    public int getMaxHP() {
        return getClientDisplayedMaxStatValue(BaseStat.mhp, BaseStat.mhpR, getServerMaxHP());
    }

    /**
     * Gets the total max mp of this Char.
     *
     * @return The total max mp of this Char
     */
    public int getMaxMP() {
        return getClientDisplayedMaxStatValue(BaseStat.mmp, BaseStat.mmpR, getServerMaxMP());
    }

    private int getServerMaxHP() {
        return getTotalMaxStatWithDisplayedCharacterPotentialRate(BaseStat.mhp, BaseStat.mhpR);
    }

    private int getServerMaxMP() {
        return getTotalMaxStatWithDisplayedCharacterPotentialRate(BaseStat.mmp, BaseStat.mmpR);
    }

    private int getClientMaxHPPacketValue() {
        return getClientMaxStatPacketValue(BaseStat.mhp, BaseStat.mhpR, getMaxHP());
    }

    private int getClientMaxMPPacketValue() {
        return getClientMaxStatPacketValue(BaseStat.mmp, BaseStat.mmpR, getMaxMP());
    }

    private int getClientMaxStatPacketValue(BaseStat flatStat, BaseStat rateStat, int finalMax) {
        int clientFlat = getClientVisibleMaxStatFlat(flatStat);
        int clientRate = getClientVisibleMaxStatRate(rateStat);
        double rateMultiplier = 1 + clientRate / 100D;
        int packetValue = clientRate <= -100
                ? finalMax - clientFlat
                : (int) Math.ceil(finalMax / rateMultiplier) - clientFlat;
        return Math.max(0, Math.min(packetValue, GameConstants.MAX_HP_MP));
    }

    private int getClientDisplayedMaxStatValue(BaseStat flatStat, BaseStat rateStat, int serverMax) {
        int clientFlat = getClientVisibleMaxStatFlat(flatStat);
        int clientRate = getClientVisibleMaxStatRate(rateStat);
        int packetValue = getClientMaxStatPacketValue(flatStat, rateStat, serverMax);
        long clientBase = (long) packetValue + clientFlat;
        long displayedMax = clientRate <= -100
                ? clientBase
                : clientBase * (100L + clientRate) / 100L;
        return Math.max(serverMax, Math.min((int) displayedMax, GameConstants.MAX_HP_MP));
    }

    private int getGuildBaseStatContribution(BaseStat stat) {
        return net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                ? SkillInfo.getGuildBaseStatValues(this).getOrDefault(stat, 0) : 0;
    }

    private int getClientVisibleMaxStatFlat(BaseStat flatStat) {
        return Math.toIntExact(baseStats.getOrDefault(flatStat, 0L)
                + getGuildBaseStatContribution(flatStat)
                + getTemporaryStatManager().getBaseStats().getOrDefault(flatStat, 0)
                + getLevelStatContribution(flatStat)
                + getEquippedStatContribution(flatStat)
                + getStatAmountSetEffect(flatStat)
                + getCharacterPotentialStatContribution(flatStat, true));
    }

    private int getClientVisibleMaxStatRate(BaseStat rateStat) {
        return Math.toIntExact(baseStats.getOrDefault(rateStat, 0L)
                + getGuildBaseStatContribution(rateStat)
                + getTemporaryStatManager().getBaseStats().getOrDefault(rateStat, 0)
                + getLevelStatContribution(rateStat)
                + getStatAmountSetEffect(rateStat)
                + getCharacterPotentialStatContribution(rateStat, true)
                + getClientVisiblePassiveSkillRateGap(rateStat));
    }

    private int getClientVisiblePassiveSkillRateGap(BaseStat rateStat) {
        int passiveSkillRate = getClientVisiblePassiveSkillStatContribution(rateStat);
        int cachedPassiveRate = Math.toIntExact(baseStats.getOrDefault(rateStat, 0L));
        return Math.max(0, passiveSkillRate - cachedPassiveRate);
    }

    private int getClientVisiblePassiveSkillStatContribution(BaseStat stat) {
        int value = 0;
        for (Skill skill : getSkills()) {
            if (skill == null || skill.getCurrentLevel() <= 0) {
                continue;
            }
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            if (si == null || !si.isPsd()) {
                continue;
            }
            value += si.getBaseStatValues(this, skill.getCurrentLevel()).getOrDefault(stat, 0);
        }
        return value;
    }

    private int getTotalMaxStatWithDisplayedCharacterPotentialRate(BaseStat baseStat, BaseStat rateStat) {
        double total = getTotalStatAsDouble(baseStat);
        int serverRate = getTotalStat(rateStat);
        int displayedCharacterPotentialRate = getDisplayedCharacterPotentialRate(rateStat);
        int characterPotentialMissingRate = Math.max(0, displayedCharacterPotentialRate - serverRate);
        int passiveSkillMissingRate = getClientVisiblePassiveSkillRateGap(rateStat);
        int missingRate = characterPotentialMissingRate + passiveSkillMissingRate;
        if (missingRate > 0) {
            double baseBeforeRate = serverRate <= -100 ? total : total / (1 + serverRate / 100D);
            total += baseBeforeRate * missingRate / 100D;
        }
        return Math.min((int) total, GameConstants.MAX_HP_MP);
    }

    private int getDisplayedCharacterPotentialRate(BaseStat rateStat) {
        String presetStr = getQRValueByKey(QuestConstants.CHARACTER_POTENTIAL_PRESET, "potential");
        if (presetStr == null) {
            presetStr = "0";
        }
        int preset = Integer.parseInt(presetStr);
        int rate = 0;
        for (CharacterPotential cp : getPotentialsByPreset(preset)) {
            Skill skill = cp.getSkill();
            if (skill == null) {
                continue;
            }
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            if (si == null) {
                continue;
            }
            int serverValue = si.getBaseStatValues(this, cp.getSlv()).getOrDefault(rateStat, 0);
            int displayedValue = si.getBaseStatValues(this, cp.getSlv() + 15).getOrDefault(rateStat, 0);
            rate += Math.max(serverValue, displayedValue);
        }
        return rate;
    }

    public String getHpMpAnalysis() {
        StringBuilder sb = new StringBuilder();
        appendHpMpAnalysisHeader(sb);
        appendHpMpStatAnalysis(sb, "HP", BaseStat.mhp, BaseStat.mhpR, Stat.mhp, getHP(), getMaxHP());
        appendHpMpStatAnalysis(sb, "MP", BaseStat.mmp, BaseStat.mmpR, Stat.mmp, getMP(), getMaxMP());
        return sb.toString();
    }

    private void appendHpMpAnalysisHeader(StringBuilder sb) {
        sb.append("#e#b[HPMP分析]#k#n\r\n");
        sb.append("角色：#b").append(getName()).append("#k  ID：").append(getId()).append("\r\n");
        sb.append("等級：").append(getLevel()).append("  職業：").append(getJob()).append("\r\n\r\n");
        sb.append("說明：該輸出只讀取運行時數據，不會修改 HP/MP。\r\n");
        sb.append("若當前值高於上限，優先看臨時BUFF、角色潛能顯示等級和公式拆解。\r\n\r\n");
    }

    private void appendHpMpStatAnalysis(StringBuilder sb, String label, BaseStat flatStat, BaseStat rateStat, Stat rawMaxStat, int current, int finalMax) {
        int rawMax = getStat(rawMaxStat);
        long passiveFlat = baseStats.getOrDefault(flatStat, 0L);
        long passiveRate = baseStats.getOrDefault(rateStat, 0L);
        int buffFlat = getTemporaryStatManager().getBaseStats().getOrDefault(flatStat, 0);
        int buffRate = getTemporaryStatManager().getBaseStats().getOrDefault(rateStat, 0);
        long levelFlat = getLevelStatContribution(flatStat);
        long levelRate = getLevelStatContribution(rateStat);
        long equipFlat = getEquippedStatContribution(flatStat);
        long equipRate = getEquippedStatContribution(rateStat);
        int setFlat = getStatAmountSetEffect(flatStat);
        int setRate = getStatAmountSetEffect(rateStat);
        int characterPotentialFlat = getCharacterPotentialStatContribution(flatStat, false);
        int characterPotentialRateServer = getCharacterPotentialStatContribution(rateStat, false);
        int characterPotentialRateDisplayed = getCharacterPotentialStatContribution(rateStat, true);
        int serverRate = getTotalStat(rateStat);
        int displayedMissingRate = Math.max(0, characterPotentialRateDisplayed - serverRate);
        int clientPassiveSkillRate = getClientVisiblePassiveSkillStatContribution(rateStat);
        int clientPassiveSkillRateGap = getClientVisiblePassiveSkillRateGap(rateStat);
        int totalMissingRate = displayedMissingRate + clientPassiveSkillRateGap;
        double serverTotalBeforeDisplayedFix = getTotalStatAsDouble(flatStat);
        double baseBeforeRate = serverRate <= -100 ? serverTotalBeforeDisplayedFix : serverTotalBeforeDisplayedFix / (1 + serverRate / 100D);
        int predictedFinal = Math.min((int) (serverTotalBeforeDisplayedFix + baseBeforeRate * totalMissingRate / 100D), GameConstants.MAX_HP_MP);
        int clientPacketValue = getClientMaxStatPacketValue(flatStat, rateStat, finalMax);
        int clientVisibleFlat = getClientVisibleMaxStatFlat(flatStat);
        int clientVisibleRate = getClientVisibleMaxStatRate(rateStat);

        sb.append("#e#d[").append(label).append("]#k#n\r\n");
        sb.append("當前值：#r").append(current).append("#k  最終上限：#r").append(finalMax).append("#k  差值：#r").append(current - finalMax).append("#k\r\n");
        sb.append("DB基礎上限：").append(rawMax).append("\r\n");
        sb.append("被動緩存：flat=").append(passiveFlat).append("  rate=").append(passiveRate).append("%\r\n");
        sb.append("臨時BUFF：flat=").append(buffFlat).append("  rate=").append(buffRate).append("%\r\n");
        sb.append("等級變量：flat=").append(levelFlat).append("  rate=").append(levelRate).append("%\r\n");
        sb.append("裝備合計：flat=").append(equipFlat).append("  rate=").append(equipRate).append("%\r\n");
        sb.append("套裝效果：flat=").append(setFlat).append("  rate=").append(setRate).append("%\r\n");
        sb.append("角色潛能：flat=").append(characterPotentialFlat)
                .append("  serverRate=").append(characterPotentialRateServer)
                .append("%  displayedRate=").append(characterPotentialRateDisplayed).append("%\r\n");
        sb.append("服務端rate合計：").append(serverRate).append("%  顯示等級補償rate：").append(displayedMissingRate).append("%\r\n");
        sb.append("客戶端被動技能rate：").append(clientPassiveSkillRate)
                .append("%  客戶端被動補差rate：").append(clientPassiveSkillRateGap).append("%\r\n");
        sb.append("服務端原始上限：").append((int) serverTotalBeforeDisplayedFix)
                .append("  補償後預測上限：").append(predictedFinal).append("\r\n");
        sb.append("客戶端包基準：").append(clientPacketValue)
                .append("  客戶端本地flat：").append(clientVisibleFlat)
                .append("  客戶端本地rate：").append(clientVisibleRate).append("%\r\n\r\n");
    }

    private long getLevelStatContribution(BaseStat baseStat) {
        BaseStat levelStat = baseStat.getLevelVar();
        if (levelStat == null) {
            return 0;
        }
        return (long) getTotalStatAsDouble(levelStat) * getLevel();
    }

    private long getEquippedStatContribution(BaseStat baseStat) {
        long value = 0;
        for (Item item : getEquippedInventory().getItems()) {
            if (!(item instanceof Equip equip)) {
                continue;
            }
            value += equip.getBaseStat(baseStat);
            if (baseStat == BaseStat.mhpR && equip.getiMaxHpr() > 0) {
                value += equip.getiMaxHpr();
            } else if (baseStat == BaseStat.mmpR && equip.getiMaxMpr() > 0) {
                value += equip.getiMaxMpr();
            }
        }
        return value;
    }

    private int getCharacterPotentialStatContribution(BaseStat baseStat, boolean useDisplayedLevel) {
        String presetStr = getQRValueByKey(QuestConstants.CHARACTER_POTENTIAL_PRESET, "potential");
        if (presetStr == null) {
            presetStr = "0";
        }
        int preset = Integer.parseInt(presetStr);
        int value = 0;
        for (CharacterPotential cp : getPotentialsByPreset(preset)) {
            Skill skill = cp.getSkill();
            if (skill == null) {
                continue;
            }
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            if (si == null) {
                continue;
            }
            int level = useDisplayedLevel ? cp.getSlv() + 15 : cp.getSlv();
            value += si.getBaseStatValues(this, level).getOrDefault(baseStat, 0);
        }
        return value;
    }

    /**
     * Gets the current percentage of HP of this Char.
     *
     * @return
     */
    public double getCurrentHPPerc() {
        return 100 * (((double) getHP()) / getMaxHP());
    }

    /**
     * Gets the current percentage of MP of this Char.
     *
     * @return
     */
    public double getCurrentMPPerc() {
        return 100 * (((double) getMP()) / getMaxMP());
    }

    /**
     * Gets the amount that is 1% of this Char's Max HP
     *
     * @return
     */

    public int getHPPerc() {
        return getHPPerc(1);
    }

    /**
     * Gets the amount that is 'amount'% of this Char's Max HP
     *
     * @param amount
     * @return
     */

    public int getHPPerc(int amount) {
        return (int) (amount * (getMaxHP() / 100D));
    }

    public int getMPPerc(int amount) {
        return (int) (amount * (getMaxMP() / 100D));
    }

    /**
     * Heals character's MP and HP completely.
     */
    public void healHPMP() {
        heal(getMaxHP(), true);
        healMP(getMaxMP());
    }

    /**
     * Heals this Char's HP for a certain amount. Caps off at maximum HP.
     *
     * @param amount The amount to heal.
     */
    public void heal(int amount, boolean whilstDeath) {
        if (amount > 0) {
            if (JobConstants.isDemon(getJob()) && getTemporaryStatManager().hasStatBySkillId(DemonAvenger.DEMONIC_FRENZY) && amount > (int) (getMaxHP() * 2.0D / 100.0D)) {
                amount = (int) (getMaxHP() * 2.0D / 100.0D);
            }
        }
        if (is1HitKOSkill(-amount)) {
            return;
        }
        int curHP = getHP();
        int maxHP = getMaxHP();
        int newHP = Math.min(curHP + amount, maxHP);
        if (newHP > GameConstants.MAX_HP_MP) {
            newHP = GameConstants.MAX_HP_MP;
        }
        Map<Stat, Object> stats = new HashMap<>();
        if (newHP <= 0) {
            if (newHP < 0) {
                newHP = 0;
            }
            if (getHP() > 0) {
                if (net.swordie.ms.world.tms282.items.Tms282SafetyCharms.protectOnDeath(this)) {
                    deathProtectedByCharm = true; // 282: charm by data, no EXP loss and no death debuff
                } else if (hasItem(5130000)) {
                    getScriptManager().startScript(this, 0, "cash_5130000", ScriptType.Item);
                } else {
                    int antiExpLostR = 0;
                    if (getGuild() != null) {
                        GuildSkill guildSkill = getGuild().getSkillById(GuildConstants.FEARLESS);
                        SkillInfo skillInfo = SkillData.getSkillInfoById(GuildConstants.FEARLESS);
                        if (guildSkill != null && skillInfo != null) {
                            antiExpLostR = skillInfo.getValue(expLossReduceR, guildSkill.getLevel());
                        }
                    }
                    long exp = getAvatarData().getCharacterStat().getExp();
                    long expLost = exp * (10 - antiExpLostR) / 100; //10 is 10% exp lost when death.
                    long newExp = exp - expLost;
                    if (newExp < 0) {
                        newExp = 0;
                    }
                    getAvatarData().getCharacterStat().setExp(newExp);
                    stats.put(Stat.exp, newExp);
                    sendStatsPacket(stats);
                }
            }
            openUIOnDead();
        }
        if (whilstDeath || getHP() > 0) {
            setStat(Stat.hp, newHP);
            stats.put(Stat.hp, newHP);
            sendStatsPacket(stats);
        }
        if (getParty() != null) {
            getParty().broadcast(UserRemote.receiveHP(this));
            for (Char chr : getParty().getOnlineChars()) {
                write(UserRemote.receiveHP(chr));
            }
        }
        if (amount > 10 || amount < 0) {
            write(UserPacket.effect(Effect.IncDecHPRegenEffect(Math.min(amount, GameConstants.MAX_HP_MP))));
        }
    }

    public void heal(int amount) {
        if (getMobZoneDebuff() != null) {
            amount = (int) (amount * getMobZoneDebuff().getHealRate() / 100.0D);
        }
        heal(amount, false);
    }

    /**
     * "Heals" this Char's MP for a certain amount. Caps off at maximum MP.
     *
     * @param amount The amount to heal.
     */
    public void healMP(int amount) {
        if (getMobZoneDebuff() != null) {
            amount = (int) (amount * getMobZoneDebuff().getHealRate() / 100.0D);
        }
        if (getTemporaryStatManager().hasStat(BishopPray)) {
            int totalInt = getTotalStat(BaseStat.inte);
            if (totalInt >= 2000) {
                int bonus = Math.min((totalInt / 1500), 15);
                amount += ((amount * bonus) / 100.0D);
            }
        }
        int curMP = getMP();
        int maxMP = getMaxMP();
        int newMP = 0;
        if (curMP + amount > 0) {
            newMP = Math.min(curMP + amount, maxMP);
        }
        if (newMP > GameConstants.MAX_HP_MP) {
            newMP = GameConstants.MAX_HP_MP;
        } else if (newMP < 0) {
            newMP = 0;
        }
        Map<Stat, Object> stats = new HashMap<>();
        setStat(Stat.mp, newMP);
        stats.put(Stat.mp, newMP);
        sendStatsPacket(stats);
        //System.out.println("Character: " + getName() + " newMP: " + newMP + " Amount: " + amount);
    }

    public void consumeItem(Item item, int qty) {
        if(net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.economyEnabled()) {
            synchronized(net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized(this) {
                net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.requireEconomicAccess(this);
                consumeInventoryItem(item,qty);
            } }
        } else consumeInventoryItem(item,qty);
    }

    private void consumeInventoryItem(Item item, int qty) {
        if (item == null || qty <= 0) return;
        net.swordie.ms.client.character.tms282.Tms282CubeRuntime.requireMovable(this, item);
        Inventory inv = getInventoryByType(item.getInvType());
        int have = item.getQuantity();
        int take = Math.min(qty, have);
        int left = have - take;
        net.swordie.ms.world.tms282.audit.AuditLog.itemLose(this, item, take); // audit: zlog_inventory
        if (left <= 0 && !ItemConstants.isThrowingStar(item.getItemId()) && !ItemConstants.isBullet(item.getItemId())) {
            short bagIndex = (short) item.getBagIndex();
            if (item.getInvType() == EQUIPPED) {
                getAvatarData().getAvatarLook().removeItem(item.getItemId());
                bagIndex = (short) -bagIndex;
            }
            DataPrinter.send(DataPrinter.ITEM,
                    String.format("Player %s consumed item %s | ID: %d | Item ID: %d | Qty: %d -> 0 | InvType: %d | Slot: %d.",
                            getName(), StringData.getItemStringById(item.getItemId()),
                            item.getId(), item.getItemId(), have, item.getInvType().getVal(), item.getBagIndex()),
                    true);
            item.setQuantity(0);
            item.setCharID(0);
            inv.removeItem(item);
            item.deleteFromSQL();
            write(WvsContext.inventoryOperation(true, false, Remove, bagIndex, (byte) 0, 0, item));
        } else {
            DataPrinter.send(DataPrinter.ITEM,
                    String.format("Player %s consumed item %s | ID: %d | Item ID: %d | Qty: %d -> %d | InvType: %d | Slot: %d.",
                            getName(), StringData.getItemStringById(item.getItemId()),
                            item.getId(), item.getItemId(), have, left, item.getInvType().getVal(), item.getBagIndex()),
                    true);
            item.setQuantity(left);
            item.updateQuantityInSQL();
            write(WvsContext.inventoryOperation(true, false, UpdateQuantity, (short) item.getBagIndex(), (byte) -1, 0, item));
        }

        setBulletIDForAttack(calculateBulletIDForAttack(1));
    }

    public void consumeItem(Item item) {
        consumeItem(item, 1);
    }

    public void consumeItem(int id, int quantity) {
        Item proto = ItemData.getItemDeepCopy(id);
        if (proto == null) return;
        Inventory inv = getInventoryByType(proto.getInvType());
        Item item = inv.getItemByItemID(id);
        if (item != null) {
            consumeItem(item, quantity);
        }
    }

    public void consumeAllThrowingItem(Item item) {
        Inventory inventory = getInventoryByType(item.getInvType());
        item.setQuantity(0);
        item.setCharID(0);
        inventory.removeItem(item);
        //Item đã hết
        item.deleteFromSQL();

        DataPrinter.send(DataPrinter.ITEM, String.format("玩家 %s 丟棄物品 %s | ID: %d | 物品ID: %d | 數量: %d | 背包類型: %d | 位置: %d。",
                getName(), StringData.getItemStringById(item.getItemId()), item.getId(), item.getItemId(), item.getQuantity(), item.getInvType().getVal(), item.getBagIndex()), true);
        short bagIndex = (short) item.getBagIndex();
        if (item.getInvType() == EQUIPPED) {
            getAvatarData().getAvatarLook().removeItem(item.getItemId());
            bagIndex = (short) -bagIndex;
        }
        write(WvsContext.inventoryOperation(true, false, Remove, bagIndex, (byte) 0, 0, item));
        setBulletIDForAttack(calculateBulletIDForAttack(1));
    }

    public boolean hasItem(int itemID) {
        return getInventories().stream().anyMatch(inv -> inv.containsItem(itemID));
    }

    public boolean hasItemCount(int itemID, int count) {
        Item item = ItemData.getItemDeepCopy(itemID);
        if (item == null) {
            //chatMessage("Something wrong please report this to dev: " + itemID + ":" + count);
            return false;
        }
        Inventory inv = getInventoryByType(item.getInvType());
        return inv.getItems().stream()
                .filter(i -> i.getItemId() == itemID)
                .mapToInt(Item::getQuantity)
                .sum() >= count;
    }

    public short getLevel() {
        return getAvatarData().getCharacterStat().getLevel();
    }

    public boolean isMarried() {
        return marriageRecord != null; // set by RingRecordService.apply
    }

    public int getGuildID() {
        return guildID;
    }

    public void setGuildID(int guildID) {
        this.guildID = guildID;
    }

    public Guild getGuild() {
        return guild;
    }

    public void setGuild(Guild guild) {
        if (guild != null) {
            // to ensure that the same instance of a guild is retrieved for all characters
            this.guild = getClient().getWorld().getGuildByID(guild.getId());
        } else {
            this.guild = null;
        }
    }

    public void initGuild(boolean online) {
        if(net.swordie.ms.client.social.Guild.Tms282GuildRuntime.enabled()) {
            net.swordie.ms.client.social.Guild.Tms282GuildRuntime.onPresence(this,online);
            return;
        }
        if (this.guild != null) {
            GuildMember gm = this.guild.getMemberByCharID(getId());
            if (gm != null) {
                gm.updateInfoFromChar(this);
                gm.setOnline(online);
                if (this.guild.getLeaderID() == getId()) {
                    this.guild.setLeader(gm);
                }
                this.guild.broadcast(WvsContext.guildResult(GuildResult.response_GuildNotify_LoginOrLogout(this.guild, gm, online, online)), this);
                write(WvsContext.guildResult(GuildResult.response_GuildLoad_Success(this.guild)));
                Alliance ally = this.guild.getAlliance();
                if (ally != null) {
                    ally.broadcast(WvsContext.allianceResult(AllianceResult.notifyLoginOrLogout(ally, this.guild, gm, !this.online && online)), this);
                }
            }
        }
        GuildRequestor guildRequestor = GuildRequestor.getGuildRequestorFromSQLByCharID(getId());
        if (guildRequestor != null) {
            GuildRequestor.setOnlineStateByChar(this, guildRequestor, online);
        }
    }

    public int getTotalArc() {
        int totalArc = 0;
        for (Item i : getInventoryByType(EQUIPPED).getItems()) {
            if (ItemConstants.isArcaneSymbol(i.getItemId())) {
                totalArc += ((Equip) i).getSymbol().getInc();
            }
        }
        int bonusArc = 0;
        if (getGuild() != null) {
            GuildSkill gs = getGuild().getSkillById(GuildConstants.ARCANE_FORCE);
            SkillInfo si = SkillData.getSkillInfoById(GuildConstants.ARCANE_FORCE);
            if (gs != null && si != null) {
                bonusArc = si.getValue(arcX, gs.getLevel());
            }
        }
        return totalArc + bonusArc;
    }

    public int getTotalAut() {
        int totalAut = 0;
        for (Item i : getInventoryByType(EQUIPPED).getItems()) {
            if (ItemConstants.isSacredSymbol(i.getItemId())) {
                totalAut += ((Equip) i).getSymbol().getInc();
            }
        }
        return totalAut;
    }

    public int getTotalChuc(boolean isUnion) {
        int totalChuc = 0;
        for (Item i : getInventoryByType(EQUIPPED).getItems()) {
            totalChuc += ((Equip) i).getChuc();
        }
        int bonusChuc = 0;
        if (!isUnion) {
            if (getGuild() != null) {
                GuildSkill gs = getGuild().getSkillById(GuildConstants.AMIST_THE_STARS);
                SkillInfo si = SkillData.getSkillInfoById(GuildConstants.AMIST_THE_STARS);
                if (gs != null && si != null) {
                    bonusChuc = si.getValue(starX, gs.getLevel());
                }
            }
        }
        return totalChuc + bonusChuc;
    }

    public int getDriverID() {
        return driverID;
    }

    public void setDriverID(int driverID) {
        this.driverID = driverID;
    }

    public int getPassengerID() {
        return passengerID;
    }

    public void setPassengerID(int passengerID) {
        this.passengerID = passengerID;
    }

    public int getChocoCount() {
        return chocoCount;
    }

    public void setChocoCount(int chocoCount) {
        this.chocoCount = chocoCount;
    }

    public int getActiveEffectItemID() {
        return activeEffectItemID;
    }

    public void setActiveEffectItemID(int activeEffectItemID) {
        this.activeEffectItemID = activeEffectItemID;
    }

    public int getMonkeyEffectItemID() {
        return monkeyEffectItemID;
    }

    public void setMonkeyEffectItemID(int monkeyEffectItemID) {
        this.monkeyEffectItemID = monkeyEffectItemID;
    }

    public int getCompletedSetItemID() {
        return completedSetItemID;
    }

    public void setCompletedSetItemID(int completedSetItemID) {
        this.completedSetItemID = completedSetItemID;
    }

    public void initCompletedSetItemID() {
        HashMap<Integer, Integer> setIdToLevel = new HashMap<>();
        for (Item item : getEquippedInventory().getItems()) {
            Equip equip = (Equip) item;
            int setItemId = equip.getSetItemID();
            if (setItemId > 0) {
                int level = setIdToLevel.getOrDefault(setItemId, 0);
                level++;
                setIdToLevel.put(setItemId, level);
            }
        }
        for (Map.Entry<Integer, Integer> entry : setIdToLevel.entrySet()) {
            int setId = entry.getKey();
            int setLevel = entry.getValue();
            if (EtcData.loadSetItemCompleteCount(setId) == setLevel) {
                setCompletedSetItemID(setId);
                return;
            }
        }
        setCompletedSetItemID(0);
    }

    public void initDressUpInfo(Item item) {
        if (JobConstants.isAngelicBuster(getJob())) {
            DressUpInfo dressUpInfo = new DressUpInfo();
            dressUpInfo.setFace(getAvatarData().getAvatarLook().getFace());
            dressUpInfo.setHair(getAvatarData().getAvatarLook().getHair());
            if (item.getItemId() / 10000 == 105 && item.isCash()) {
                dressUpInfo.setClothe(item.getItemId());
            }
            setDressUpInfo(dressUpInfo);
        }
    }

    public short getFieldSeatID() {
        return -1;
    }

    public void setFieldSeatID(short fieldSeatID) {
        this.fieldSeatID = fieldSeatID;
    }

    public PortableChair getChair() {
        return chair;
    }

    public void setChair(PortableChair chair) {
        this.chair = chair;
    }

    public short getFoothold() {
        return foothold;
    }

    public void setFoothold(short foothold) {
        this.foothold = foothold;
    }

    public int getTamingMobLevel() {
        return tamingMobLevel;
    }

    public void setTamingMobLevel(int tamingMobLevel) {
        this.tamingMobLevel = tamingMobLevel;
    }

    public int getTamingMobExp() {
        return tamingMobExp;
    }

    public void setTamingMobExp(int tamingMobExp) {
        this.tamingMobExp = tamingMobExp;
    }

    public int getTamingMobFatigue() {
        return tamingMobFatigue;
    }

    public void setTamingMobFatigue(int tamingMobFatigue) {
        this.tamingMobFatigue = tamingMobFatigue;
    }

    public MiniRoom getMiniRoom() {
        return miniRoom;
    }

    public void setMiniRoom(MiniRoom miniRoom) {
        this.miniRoom = miniRoom;
    }

    public String getADBoardRemoteMsg() {
        return ADBoardRemoteMsg;
    }

    public void setADBoardRemoteMsg(String ADBoardRemoteMsg) {
        this.ADBoardRemoteMsg = ADBoardRemoteMsg;
    }

    public boolean isInCouple() {
        return inCouple;
    }

    public void setInCouple(boolean inCouple) {
        this.inCouple = inCouple;
    }

    public CoupleRecord getCouple() {
        return couple;
    }

    public void setCouple(CoupleRecord couple) {
        this.couple = couple;
    }

    public boolean hasFriendshipItem() {
        return friendshipRingRecord != null; // set by RingRecordService.apply
    }

    public FriendshipRingRecord getFriendshipRingRecord() {
        return friendshipRingRecord;
    }

    public void setFriendshipRingRecord(FriendshipRingRecord friendshipRingRecord) {
        this.friendshipRingRecord = friendshipRingRecord;
    }

    public int getComboCounter() {
        return comboCounter;
    }

    public void setComboCounter(int comboCounter) {
        this.comboCounter = comboCounter;
    }

    public int getEvanDragonGlide() {
        return evanDragonGlide;
    }

    public void setEvanDragonGlide(int evanDragonGlide) {
        this.evanDragonGlide = evanDragonGlide;
    }

    public int getKaiserMorphRotateHueExtern() {
        return kaiserMorphRotateHueExtern;
    }

    public void setKaiserMorphRotateHueExtern(int kaiserMorphRotateHueExtern) {
        this.kaiserMorphRotateHueExtern = kaiserMorphRotateHueExtern;
    }

    public int getKaiserMorphPrimiumBlack() {
        return kaiserMorphPrimiumBlack;
    }

    public void setKaiserMorphPrimiumBlack(int kaiserMorphPrimiumBlack) {
        this.kaiserMorphPrimiumBlack = kaiserMorphPrimiumBlack;
    }

    public int getKaiserMorphRotateHueInnner() {
        return kaiserMorphRotateHueInnner;
    }

    public void setKaiserMorphRotateHueInnner(int kaiserMorphRotateHueInnner) {
        this.kaiserMorphRotateHueInnner = kaiserMorphRotateHueInnner;
    }

    public int getMakingMeisterSkillEff() {
        return makingMeisterSkillEff;
    }

    public void setMakingMeisterSkillEff(int makingMeisterSkillEff) {
        this.makingMeisterSkillEff = makingMeisterSkillEff;
    }

    public FarmUserInfo getFarmUserInfo() {
        if (farmUserInfo == null) {
            return new FarmUserInfo();
        }
        return farmUserInfo;
    }

    public void setFarmUserInfo(FarmUserInfo farmUserInfo) {
        this.farmUserInfo = farmUserInfo;
    }

    public int getCustomizeEffect() {
        return customizeEffect;
    }

    public void setCustomizeEffect(int customizeEffect) {
        this.customizeEffect = customizeEffect;
    }

    public String getCustomizeEffectMsg() {
        return customizeEffectMsg;
    }

    public void setCustomizeEffectMsg(String customizeEffectMsg) {
        this.customizeEffectMsg = customizeEffectMsg;
    }

    public byte getSoulEffect() {
        return soulEffect;
    }

    public void setSoulEffect(byte soulEffect) {
        this.soulEffect = soulEffect;
    }

    public FreezeHotEventInfo getFreezeHotEventInfo() {
        if (freezeHotEventInfo == null) {
            return new FreezeHotEventInfo();
        }
        return freezeHotEventInfo;
    }

    public void setFreezeHotEventInfo(FreezeHotEventInfo freezeHotEventInfo) {
        this.freezeHotEventInfo = freezeHotEventInfo;
    }

    public int getEventBestFriendAID() {
        return eventBestFriendAID;
    }

    public void setEventBestFriendAID(int eventBestFriendAID) {
        this.eventBestFriendAID = eventBestFriendAID;
    }

    public int getMesoChairCount() {
        return mesoChairCount;
    }

    public void setMesoChairCount(int mesoChairCount) {
        this.mesoChairCount = mesoChairCount;
    }

    public boolean isBeastFormWingOn() {
        return beastFormWingOn;
    }

    public void setBeastFormWingOn(boolean beastFormWingOn) {
        this.beastFormWingOn = beastFormWingOn;
    }

    public int getActiveNickItemID() {
        return activeNickItemID;
    }

    public void setActiveNickItemID(int activeNickItemID) {
        this.activeNickItemID = activeNickItemID;
    }

    public int getActiveNickSkillID() {
        return activeNickSkillID;
    }

    public void setActiveNickSkillID(int activeNickSkillID) {
        this.activeNickSkillID = activeNickSkillID;
    }

    public void encodeCustomNickName(OutPacket outPacket) {
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeString("");
        outPacket.encodeFT(FileTime.currentTime());
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        if(!online) {
            net.swordie.ms.client.jobs.flora.Tms282AdeleShardbreaker.clear(this);
            net.swordie.ms.client.jobs.resistance.Tms282OmegaBlaster.reset(this);
            net.swordie.ms.client.jobs.flora.Tms282AetherGuard.reset(this, System.currentTimeMillis());
            net.swordie.ms.client.jobs.Jianghu.Tms282HowlingStorm.reset(this);
            net.swordie.ms.client.jobs.adventurer.thief.Tms282BladeTempest.reset(this);
            net.swordie.ms.client.jobs.cygnus.BlazeWizard.stopInferno(this);
        }
        this.online = online;
        //initFriends(online);
        initParty(online);
        //initGuild(online);
    }

    public int getPartyID() {
        return partyID;
    }

    public void setPartyID(int partyID) {
        this.partyID = partyID;
    }

    public Party getParty() {
        return party;
    }

    public void setParty(Party party) {
        this.party = party;
    }

    public void changeChannel(byte channelId) {
        changeChannelAndWarp(channelId, getFieldID());
    }

    public void changeChannelAndWarp(byte channelId, int fieldId) {
        write(UserLocal.setInGameDirectionMode(true, false, true, false));
        user.setClientState(Client.CHANGING_CHANNEL);
        client.setCurrentState(Client.CHANGING_CHANNEL);
        client.setNextState(Client.IN_FIELD);
        user.saveToSQL(false);
        if (getFieldID() != fieldId) {
            setField(getOrCreateFieldByCurrentInstanceType(fieldId));
        }
        Channel channel = Server.get().getWorld().getChannelById(channelId);
        channel.addClientInTransfer(channelId, getId(), getClient());
        short port = (short) channel.getPort();
        getTimer().addEvent(() -> write(ClientSocket.migrateCommand(getClient(), true, port)), 1000);
        DataPrinter.send(DataPrinter.LOGIN, String.format("[IP: %s] [賬號: %s] [角色: %s] 已切換到頻道 %d。", client.getIP(), user.getName(), getName(), channelId));
    }

    public int getSubJob() {
        return getAvatarData().getCharacterStat().getSubJob();
    }

    public FieldInstanceType getFieldInstanceType() {
        return fieldInstanceType;
    }

    public void setFieldInstanceType(FieldInstanceType fieldInstanceType) {
        this.fieldInstanceType = fieldInstanceType;
    }

    public Instance getInstance() {
        // multiplayer-0930: the party's instance only for members that are eligible for it (online at its setup and
        // not left since); a member in town, one who joined later or one who left keeps its own (usually none), so
        // their death / relogin / exit no longer clears the instance of the others.
        Party p = this.party;
        Instance shared = p != null ? p.getInstance() : null;
        if (shared != null && (shared == instance || shared.getChars().containsKey(getId()))) {
            return shared;
        }
        return instance;
    }

    public void setInstance(Instance instance) {
        if (getInstance() != null && instance == null) {
            getInstance().removeChar(this);
            setDeathCount(-1);
            setMobZoneDebuff(null);
            getScriptManager().stopEvents();
            getPartyQuestManager().setPartyQuest(null);
            setPracticeMode(false);
            removeQuest(102401);
        }
        this.instance = instance;
    }

    private void showProperUI(int fromField, int toField) {
        if (GameConstants.getMaplerunnerField(toField) > 0 && GameConstants.getMaplerunnerField(fromField) <= 0) {
            write(FieldPacket.openUI(UIType.UI_PLATFORM_STAGE_LEAVE));
        } else if (GameConstants.getMaplerunnerField(fromField) > 0 && GameConstants.getMaplerunnerField(toField) <= 0) {
            write(FieldPacket.closeUI(UIType.UI_PLATFORM_STAGE_LEAVE));
        }
        if (!GameConstants.IS_OPEN_UI_MAPLE_WORLD_REWARD_POINT) {
            write(FieldPacket.closeUI(UIType.UI_MAPLE_WORLD_REWARD_POINT));
        }
    }

    public int calculateBulletIDForAttack(int requiredAmount) {
        Item weapon = getEquippedInventory().getFirstItemByBodyPart(BodyPart.Weapon);
        if (weapon == null) {
            return 0;
        }
        Predicate<Item> kindOfBulletPred;
        int id = weapon.getItemId();

        if (ItemConstants.isClaw(id)) {
            kindOfBulletPred = i -> ItemConstants.isThrowingStar(i.getItemId());
        } else if (ItemConstants.isBow(id)) {
            kindOfBulletPred = i -> ItemConstants.isBowArrow(i.getItemId());
        } else if (ItemConstants.isXBow(id)) {
            kindOfBulletPred = i -> ItemConstants.isXBowArrow(i.getItemId());
        } else if (ItemConstants.isGun(id)) {
            kindOfBulletPred = i -> ItemConstants.isBullet(i.getItemId());
        } else {
            return 0;
        }
        Item i = getConsumeInventory().getItems().stream().sorted(Comparator.comparing(Item::getBagIndex)).filter(kindOfBulletPred).filter(item -> item.getQuantity() >= requiredAmount).findFirst().orElse(null);
        return i != null ? i.getItemId() : 0;
    }

    public int getBulletIDForAttack() {
        return bulletIDForAttack;
    }

    public void setBulletIDForAttack(int bulletIDForAttack) {
        this.bulletIDForAttack = bulletIDForAttack;
    }

    public NpcShopDlg getShop() {
        return shop;
    }

    public void setShop(NpcShopDlg shop) {
        this.shop = shop;
    }

    public List<NpcShopItem> getRepurchaseItems() {
        return repurchaseItems;
    }

    public void setRepurchaseItems(List<NpcShopItem> repurchaseItems) {
        this.repurchaseItems = repurchaseItems;
    }

    /**
     * Recursive function that checks if this Char can hold a list of items in their inventory.
     *
     * @param items the list of items this char should be able to hold
     * @return whether or not this Char can hold the list of items
     */
    public boolean canHold(List<Item> items) {
        if (items == null || items.isEmpty()) return true;
        if (net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3)
            return canHoldWithExpiry(items);
        final Map<Integer, ItemInfo> itemInfoCache = new HashMap<>();
        final Map<Integer, Equip> equipCache = new HashMap<>();
        int needEquip = 0;
        int needDeco  = 0;
        final EnumMap<InvType, Int2LongOpenHashMap> req = new EnumMap<>(InvType.class);
        for (Item it : items) {
            if (it == null) continue;
            int itemId = it.getItemId();
            int qty = it.getQuantity();
            if (qty <= 0) continue;
            if (ItemConstants.isEquip(itemId)) {
                Equip eq = equipCache.computeIfAbsent(itemId, ItemData::getEquipById);
                if (eq == null) return false;
                if (eq.isCash()) needDeco++;
                else needEquip++;
                continue;
            }
            ItemInfo info = itemInfoCache.computeIfAbsent(itemId, ItemData::getItemInfoByID);
            if (info == null) return false;
            InvType invType = info.getInvType();
            if (invType == null) return false;
            req.computeIfAbsent(invType, k -> {
                var m = new Int2LongOpenHashMap();
                m.defaultReturnValue(0L);
                return m;
            }).addTo(itemId, qty);
        }
        if (needEquip > 0) {
            Inventory equipInv = getInventoryByType(InvType.EQUIP);
            if (equipInv == null || equipInv.getEmptySlots() < needEquip) return false;
        }
        if (needDeco > 0) {
            Inventory decoInv = getInventoryByType(InvType.DECORATION);
            if (decoInv == null || decoInv.getEmptySlots() < needDeco) return false;
        }
        for (var e : req.entrySet()) {
            InvType invType = e.getKey();
            var needById = e.getValue();
            Inventory inv = getInventoryByType(invType);
            if (inv == null) return false;
            int freeSlots = inv.getEmptySlots();
            if (freeSlots < 0) freeSlots = 0;
            final var existingSpace = new Int2LongOpenHashMap(needById.size());
            existingSpace.defaultReturnValue(0L);
            for (Item invItem : inv.getItems()) {
                if (invItem == null) continue;
                int curId = invItem.getItemId();
                if (!needById.containsKey(curId)) continue;
                ItemInfo info = itemInfoCache.computeIfAbsent(curId, ItemData::getItemInfoByID);
                if (info == null) continue;
                int slotMax = info.getSlotMax();
                if (slotMax <= 1) continue;
                int curQty = invItem.getQuantity();
                if (curQty < slotMax) {
                    existingSpace.addTo(curId, slotMax - curQty);
                }
            }
            for (var it = needById.int2LongEntrySet().fastIterator(); it.hasNext(); ) {
                var ne = it.next();
                int itemId = ne.getIntKey();
                long needQty = ne.getLongValue();
                ItemInfo info = itemInfoCache.get(itemId);
                if (info == null) return false;
                int slotMax = info.getSlotMax();
                if (slotMax <= 1) {
                    if (needQty > freeSlots) return false;
                    freeSlots -= (int) needQty;
                    continue;
                }
                long space = existingSpace.get(itemId);
                if (space > 0) {
                    long use = Math.min(needQty, space);
                    needQty -= use;
                }
                if (needQty > 0) {
                    long stacksNeeded = (needQty + slotMax - 1L) / slotMax;
                    if (stacksNeeded > freeSlots) return false;
                    freeSlots -= (int) stacksNeeded;
                }
            }
        }
        return true;
    }

    private record StackRequest(int itemId, long expiry) {}

    /** Capacity uses the same expiry groups as the 282 grant path, including duplicate lines in one gift. */
    private boolean canHoldWithExpiry(List<Item> items) {
        EnumMap<InvType, Long> newSlots = new EnumMap<>(InvType.class);
        EnumMap<InvType, Map<StackRequest, Long>> quantities = new EnumMap<>(InvType.class);
        for (Item item : items) {
            if (item == null || item.getQuantity() <= 0) continue;
            int id = item.getItemId();
            if (ItemConstants.isEquip(id)) {
                Equip equip = ItemData.getEquipById(id);
                if (equip == null) return false;
                newSlots.merge(equip.isCash() ? InvType.DECORATION : InvType.EQUIP, 1L, Long::sum);
                continue;
            }
            ItemInfo info = ItemData.getItemInfoByID(id);
            if (info == null || info.getInvType() == null) return false;
            int max = Math.max(1, info.getSlotMax());
            if (!info.getInvType().isStackable() || ItemConstants.isThrowingItem(id) || ItemConstants.isBullet(id)) {
                newSlots.merge(info.getInvType(), (item.getQuantity() + max - 1L) / max, Long::sum);
            } else {
                StackRequest key = new StackRequest(id, Inventory.expiry(item));
                quantities.computeIfAbsent(info.getInvType(), k -> new HashMap<>()).merge(key, (long)item.getQuantity(), Long::sum);
            }
        }
        for (var type : quantities.entrySet()) {
            Inventory inv = getInventoryByType(type.getKey());
            if (inv == null) return false;
            for (var line : type.getValue().entrySet()) {
                int max = Math.max(1, ItemData.getItemInfoByID(line.getKey().itemId()).getSlotMax());
                long required = line.getValue();
                for (Item existing : inv.getItems()) {
                    if (existing.getItemId() == line.getKey().itemId() && Inventory.expiry(existing) == line.getKey().expiry()
                            && (existing.getDateExpire() == null || !existing.getDateExpire().isExpired())) {
                        required -= Math.max(0, max - existing.getQuantity());
                    }
                }
                if (required > 0) newSlots.merge(type.getKey(), (required + max - 1) / max, Long::sum);
            }
        }
        for (var needed : newSlots.entrySet()) {
            Inventory inv = getInventoryByType(needed.getKey());
            if (inv == null || needed.getValue() > inv.getEmptySlots()) return false;
        }
        return true;
    }

    /**
     * Checks if this Char can hold an Item in their inventory, assuming that its quantity is 1.
     *
     * @param id the item's itemID.
     * @return whether or not this Char can hold an item in their inventory.
     */
    public boolean canHold(int id) {
        return canHold(id, 1);
    }

    /**
     * Checks if this Char can hold an Item in their inventory.
     *
     * @param id       the item's itemID.
     * @param quantity the item's quantity.
     * @return whether or not this Char can hold item(s) in their inventory.
     */
    public boolean canHold(int id, int quantity) {
        if (quantity <= 0) {
            return true;
        }
        if (ItemConstants.isEquip(id)) {
            Equip equip = ItemData.getEquipById(id);
            if (equip == null) {
                return false;
            }
            if (equip.isCash()) {
                Inventory decorationInv = getInventoryByType(DECORATION);
                return decorationInv != null && decorationInv.getEmptySlots() >= quantity;
            } else {
                Inventory equipInv = getInventoryByType(EQUIP);
                return equipInv != null && equipInv.getEmptySlots() >= quantity;
            }
        } else {
            ItemInfo itemInfo = ItemData.getItemInfoByID(id);
            // 282 pets added after 265 have pet data but no ItemInfo: the deep copy below decides (gift queue item
            // 5002828 stayed undelivered for ever because of this early return)
            boolean petWithoutInfo = itemInfo == null && id / 10000 == 500
                    && net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
            if (itemInfo == null && !petWithoutInfo) {
                return false;
            }
            if (net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                Item reward = ItemData.getItemDeepCopy(id);
                if (reward == null) return false;
                reward.setQuantity(quantity);
                return canHold(List.of(reward));
            }
            InvType invType = itemInfo.getInvType();
            Inventory inv = getInventoryByType(invType);
            if (inv == null) {
                return false;
            }
            int slotMax = itemInfo.getSlotMax();
            if (slotMax <= 0) {
                slotMax = 1;
            }
            long spaceInStacks = 0;
            for (Item invItem : inv.getItems()) {
                if (invItem.getItemId() != id) {
                    continue;
                }
                int curQty = invItem.getQuantity();
                if (curQty < slotMax) {
                    spaceInStacks += (slotMax - curQty);
                }
            }
            int freeSlots = inv.getEmptySlots();
            long totalCapacity = spaceInStacks + (long) freeSlots * slotMax;
            return totalCapacity >= quantity;
        }
    }

    public void initParty(boolean isOnline) {
        if (getParty() == null) {
            return;
        }
        PartyMember partyMember = getParty().getPartyMemberByID(getId());
        if (partyMember == null) {
            return;
        }
        partyMember.updateInfoByChar(this);
        for (PartyMember pm : getParty().getMembers()) {
            if (pm.getCharID() == partyMember.getCharID()) {
                continue;
            }
            if (pm.getChannel() != partyMember.getChannel()) {
                partyMember.setFieldID(0);
            }
            if (isOnline && client.getCurrentState() == Client.IN_FIELD) {
                if (pm.getChr() != null) {
                    pm.getChr().chatMessage(String.format("[隊伍] %s 正在頻道 %d 的 %s 地圖在線。", getName(), getClient().getChannel(), StringData.getMapStringById(getFieldID())));
                }
            } else {
                if (pm.getChr() != null) {
                    pm.getChr().chatMessage(String.format("[隊伍] %s 已離線。", getName()));
                }
            }
        }
        if (client.getCurrentState() >= Client.IN_CASH_SHOP) {
            partyMember.setChannel(0);
            partyMember.setFieldID(0);
        }
        //Set Remote HP
        for (Char partyChar : getParty().getOnlineChars()) {
            write(UserRemote.receiveHP(partyChar));
        }
        getParty().broadcast(UserRemote.receiveHP(this), this);
        getParty().updateFull();
    }

    public void initFriends(boolean isOnline) {
        if (isOnline) {
            write(WvsContext.friendResult(FriendResult.response_Load_Success(getAllFriends())));
        }
        for (Friend friend : getAllFriends()) {
            Char requesterChar = getClient().getWorld().getCharById(friend.getFriendID());
            if (requesterChar == null) {
                continue;
            }
            Friend requesterAccountFriend = requesterChar.getAccountFriendByCharID(getId());
            Friend requesterCharacterFriend = requesterChar.getFriendByCharID(getId());
            if (requesterAccountFriend == null && requesterCharacterFriend == null) {
                continue;
            }
            FriendFlag flag = FriendFlag.getTypeByVal(friend.getFlag());
            if (flag == null) {
                continue;
            }
            if (requesterAccountFriend != null && requesterCharacterFriend == null) {
                if ((flag == FriendFlag.AccountFriendOnline || flag == FriendFlag.AccountFriendOffline) && requesterAccountFriend.getFlag() == FriendFlag.AccountFriendRequest.getVal()) {
                    continue;
                }
                if (flag == FriendFlag.AccountFriendRequest) {
                    write(WvsContext.friendResult(FriendResult.response_Invite(friend, friend.isAccount(), requesterChar.getLevel(), requesterChar.getJob(), requesterChar.getSubJob())));
                } else if (flag != FriendFlag.AccountFriendRequest) {
                    if (requesterChar.isOnline()) {
                        friend.setFlag(FriendFlag.AccountFriendOnline);
                    }
                    if (requesterChar.getClient().getChannel() == getClient().getChannel()) {
                        requesterAccountFriend.setChannelID(0);
                    } else if (requesterChar.getClient().getChannel() != getClient().getChannel()) {
                        requesterAccountFriend.setChannelID(requesterChar.getClient().getChannel());
                    }
                    requesterAccountFriend.setFlag(isOnline ? FriendFlag.AccountFriendOnline : FriendFlag.AccountFriendOffline);
                    if (isOnline) {
                        //requesterChar.write(WvsContext.friendResult(FriendResult.response_NotifyChange_FriendInfo(requesterAccountFriend)));
                    }
                    requesterChar.write(WvsContext.friendResult(FriendResult.response_Notify(requesterAccountFriend, getClient().getChannel(), isOnline)));
                }
            } else if (requesterAccountFriend == null && requesterCharacterFriend != null) {
                if ((flag == FriendFlag.FriendOnline || flag == FriendFlag.FriendOffline) && requesterCharacterFriend.getFlag() == FriendFlag.FriendRequest.getVal()) {
                    continue;
                }
                if (flag == FriendFlag.FriendRequest) {
                    write(WvsContext.friendResult(FriendResult.response_Invite(friend, friend.isAccount(), requesterChar.getLevel(), requesterChar.getJob(), requesterChar.getSubJob())));
                } else if (flag != FriendFlag.FriendRequest) {
                    if (requesterChar.isOnline()) {
                        friend.setFlag(FriendFlag.AccountFriendOnline);
                    }
                    if (requesterChar.getClient().getChannel() == getClient().getChannel()) {
                        requesterCharacterFriend.setChannelID(0);
                    } else if (requesterChar.getClient().getChannel() != getClient().getChannel()) {
                        requesterCharacterFriend.setChannelID(requesterChar.getClient().getChannel());
                    }
                    requesterCharacterFriend.setFlag(isOnline ? FriendFlag.FriendOnline : FriendFlag.FriendOffline);
                    if (isOnline) {
                        //requesterChar.write(WvsContext.friendResult(FriendResult.response_NotifyChange_FriendInfo(requesterCharacterFriend)));
                    }
                    requesterChar.write(WvsContext.friendResult(FriendResult.response_Notify(requesterCharacterFriend, getClient().getChannel(), isOnline)));
                }
            }
            requesterChar.write(WvsContext.friendResult(FriendResult.response_Load_Success(requesterChar.getAllFriends())));
            write(WvsContext.friendResult(FriendResult.response_Load_Success(getAllFriends())));
        }
    }

    /**
     * Returns the set of personal (i.e., non-account) friends of this Char.
     *
     * @return The set of personal friends
     */
    public Set<Friend> getFriends() {
        return friends;
    }

    public void setFriends(Set<Friend> friends) {
        this.friends = friends;
    }

    /**
     * Returns the total list of friends of this Char + the owning Account's friends.
     *
     * @return The total list of friends
     */
    public Set<Friend> getAllFriends() {
        Set<Friend> res = new HashSet<>(getFriends());
        if (getAccount() != null) {
            res.addAll(getAccount().getFriends());
        }
        return res;
    }

    public Friend getFriendByCharID(int charID) {
        return getFriends().stream().filter(f -> f.getFriendID() == charID).findAny().orElse(null);
    }

    public Friend getAccountFriendByCharID(int charID) {
        return getAccount().getFriends().stream().filter(f -> f.getFriendID() == charID).findAny().orElse(null);
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public void removeFriend(Friend f) {
        if (f != null) {
            getFriends().removeIf(x -> x.getId() == f.getId()
                    && x.getFriendAccountID() == f.getFriendAccountID()
                    && x.getOwnerID() == f.getOwnerID()
                    && x.getFriendID() == f.getFriendID()
            );
        }
    }

    public void removeFriendByID(int charID) {
        removeFriend(getFriendByCharID(charID));
    }

    public void addFriend(Friend friend) {
        if (getFriendByCharID(friend.getFriendID()) == null) {
            getFriends().add(friend);
        }
    }

    public Client getChatClient() {
        return chatClient;
    }

    public void setChatClient(Client chatClient) {
        this.chatClient = chatClient;
    }

    public List<Macro> getMacros() {
        return macros;
    }

    public void setMacros(List<Macro> macros) {
        this.macros = macros;
    }

    public void encodeDamageSkins(OutPacket outPacket) {
        final List<DamageSkinSaveData> skins = getDamageSkins().stream().filter(d -> d.getDamageSkinID() != 0).toList();
        outPacket.encodeInt(skins.size());
        for (DamageSkinSaveData skin : skins) {
            outPacket.encodeInt(skin.getDamageSkinID());
            outPacket.encodeInt(skin.getItemID());
            outPacket.encodeFT(skin.getActivateTime());
        }
        outPacket.encodeInt(getActiveDamageSkin().getDamageSkinID());
        outPacket.encodeInt(getActiveDamageSkin().getItemID());
        outPacket.encodeFT(getActiveDamageSkin().getActivateTime());

        outPacket.encodeInt(getPremiumDamageSkin().getDamageSkinID());
        outPacket.encodeInt(getPremiumDamageSkin().getItemID());
        outPacket.encodeFT(getPremiumDamageSkin().getActivateTime());

        outPacket.encodeInt(getNewDamageSkin().getDamageSkinID());
        outPacket.encodeInt(getNewDamageSkin().getItemID());
        outPacket.encodeFT(getNewDamageSkin().getActivateTime());
    }

    public boolean canAddMoney(long reqMoney) {
        return getMoney() + reqMoney > 0 && getMoney() + reqMoney < GameConstants.MAX_MONEY;
    }

    public void addPet(Pet pet) {
        getPets().add(pet);
    }

    public void removePet(Pet pet) {
        Item petItem = pet.getItem();
        if (petItem != null) {
            getPets().removeIf(x -> x.getItem() != null && x.getItem().equals(petItem));
        }
    }

    public void initPets() {
        getPets().clear();
        for (PetItem pi : java.util.stream.Stream.concat(getCashInventory().getItems().stream(), getConsumeInventory().getItems().stream())
                .filter(i -> i instanceof PetItem && ((PetItem) i).getActiveState() != 0).map(i -> (PetItem) i)
                .sorted(java.util.Comparator.comparingInt(PetItem::getActiveState)).toList()) {
            if (!pi.canSummon() || getPets().size() >= GameConstants.MAX_PET_AMOUNT) {
                pi.setActiveState((byte) 0);
                pi.updateToChar(this);
                continue;
            }
            Pet p = getPets().stream().filter(pet -> pet.getItem().equals(pi)).findAny().orElse(null);
            if (p == null) {
                p = pi.createPet(this);
                addPet(p);
            }
            getField().broadcast(PetPacket.activated(p));
            if (pi.getExceptionList() != null && !pi.getExceptionList().isEmpty()) {
                //write(PetPacket.loadExceptionList(getId(), p));
            }
        }
        if (!getPets().isEmpty()) {
            write(WvsContext.cashPetPickUpOnOffResult(getPetLoot() ? 1 : 0));
            if (hasQuest(69991)) {
                write(FieldPacket.petConsumeItemInit(Integer.parseInt(getQRValueByKey(69991, "id"))));
            }
            if (hasQuest(69992)) {
                write(FieldPacket.petConsumeMPItem(Integer.parseInt(getQRValueByKey(69992, "id"))));
            }
            if (hasQuest(69993)) {
                write(FieldPacket.petConsumeCureItem(Integer.parseInt(getQRValueByKey(69993, "id"))));
            }
        }
    }

    public void initPets(Char chr) {
        for (PetItem pi : java.util.stream.Stream.concat(getCashInventory().getItems().stream(), getConsumeInventory().getItems().stream())
                .filter(i -> i instanceof PetItem && ((PetItem) i).getActiveState() != 0).map(i -> (PetItem) i)
                .sorted(java.util.Comparator.comparingInt(PetItem::getActiveState)).toList()) {
            Pet p = getPets().stream().filter(pet -> pet.getItem().equals(pi)).findAny().orElse(null);
            if (p != null) {
                chr.write(PetPacket.activated(p));
            }
        }
    }

    public Pet getPetByIdx(int idx) {
        return getPets().stream()
                .filter(p -> p.getIdx() == idx)
                .findAny()
                .orElse(null);
    }

    public int getPetAccByIdx(int idx) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            // 282 pet equip slots 114 / 124 / 126 (equips README WI-3)
            int slot = net.swordie.ms.loaders.Tms282PetEquipment.petWearSlot(idx);
            Item acc = slot < 0 ? null : getEquippedInventory().getItemBySlot(slot);
            return acc == null ? 0 : acc.getItemId();
        }
        if (idx == 0) {
            if (getEquippedItemByBodyPart(BodyPart.PetAcc1) != null) {
                return getEquippedItemByBodyPart(BodyPart.PetAcc1).getItemId();
            }
        } else if (idx == 1) {
            if (getEquippedItemByBodyPart(BodyPart.PetAcc2) != null) {
                return getEquippedItemByBodyPart(BodyPart.PetAcc2).getItemId();
            }
        } else if (idx == 2) {
            if (getEquippedItemByBodyPart(BodyPart.PetAcc3) != null) {
                return getEquippedItemByBodyPart(BodyPart.PetAcc3).getItemId();
            }
        }
        return 0;
    }

    public int getFirstPetIdx() {
        int chosenIdx = -1;
        for (int i = 0; i < GameConstants.MAX_PET_AMOUNT; i++) {
            Pet p = getPetByIdx(i);
            if (p == null) {
                chosenIdx = i;
                break;
            }
        }
        return chosenIdx;
    }

    /**
     * Initializes the equips' enchantment stats.
     */
    public void initEquips() {
        final List<Equip> equippedItems = getEquippedInventory().getItems().stream().map(e -> (Equip) e).toList();
        final List<Equip> equipItems = getEquipInventory().getItems().stream().map(e -> (Equip) e).toList();
        // TMS 282 wears medal / shoulder / pocket in 21 / 28 / 31 (the 265 "_OLD" numbers): the legacy migration below
        // would hide them after a relog (equips README WI-1). 282 repairs items an earlier login moved instead.
        boolean tms282 = net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
        if (tms282) {
            net.swordie.ms.client.character.tms282.Tms282WornSlotRepair.repair(this);
        }
        for (Equip e : tms282 ? List.<Equip>of() : equippedItems) {
            if (e.getBagIndex() == BodyPart.Medal_OLD.getVal()) {
                e.setBagIndex(BodyPart.Medal.getVal());
            } else if (e.getBagIndex() == BodyPart.Shoulder_OLD.getVal()) {
                e.setBagIndex(BodyPart.Shoulder.getVal());
            } else if (e.getBagIndex() == BodyPart.PocketItem_OLD.getVal()) {
                e.setBagIndex(BodyPart.PocketItem.getVal());
            } else if (e.getBagIndex() == BodyPart.Android_OLD.getVal()) {
                e.setBagIndex(BodyPart.Android.getVal());
            } else if (e.getBagIndex() == BodyPart.MechanicalHeart_OLD.getVal()) {
                e.setBagIndex(BodyPart.MechanicalHeart.getVal());
            } else if (e.getBagIndex() == BodyPart.Badge_OLD.getVal()) {
                e.setBagIndex(BodyPart.Badge.getVal());
            } else if (e.getBagIndex() == BodyPart.Emblem_OLD.getVal()) {
                e.setBagIndex(BodyPart.Emblem.getVal());
            } else if (e.getBagIndex() == BodyPart.ExtendedPendant_OLD.getVal()) {
                e.setBagIndex(BodyPart.ExtendedPendant.getVal());
            }
        }
        for (Equip e : equippedItems) {
            e.recalcEnchantmentStats();
            refreshEquipmentSkills(e);
        }
        for (Equip e : equipItems) {
            e.recalcEnchantmentStats();
        }
        refreshSpecialWeaponSkills();
    }

    public void initSoulMP() {
        Equip weapon = (Equip) getEquippedItemByBodyPart(BodyPart.Weapon);
        TemporaryStatManager tsm = getTemporaryStatManager();
        if (weapon != null && weapon.getSoulSocketId() != 0 && !tsm.hasStat(SoulMP)) {
            Option o = new Option();
            o.rOption = ItemConstants.getSoulSkillFromSoulID(weapon.getSoulOptionId());
            o.xOption = ItemConstants.MAX_SOUL_CAPACITY;
            tsm.sendStat(SoulMP, o);
            write(FieldPacket.openUI(UIType.UI_SOUL_MP_COUNT));
        }
    }

    public void addSoulMP(Equip equip) {
        TemporaryStatManager tsm = getTemporaryStatManager();
        if (!tsm.hasStat(SoulMP) && ItemConstants.isWeapon(equip.getItemId()) && equip.getSoulSocketId() != 0) {
            Option o = new Option();
            o.rOption = ItemConstants.getSoulSkillFromSoulID(equip.getSoulOptionId());
            o.xOption = ItemConstants.MAX_SOUL_CAPACITY;
            tsm.sendStat(SoulMP, o);
            write(FieldPacket.openUI(UIType.UI_SOUL_MP_COUNT));
        }
    }

    public void removeSoulMP(Equip equip) {
        TemporaryStatManager tsm = getTemporaryStatManager();
        if (tsm.hasStat(SoulMP) && ItemConstants.isWeapon(equip.getItemId())) {
            tsm.removeStatsBySkill(ItemConstants.getSoulSkillFromSoulID(equip.getSoulOptionId()));
        }
    }

    public MonsterBookInfo getMonsterBookInfo() {
        return monsterBookInfo;
    }

    public void setMonsterBookInfo(MonsterBookInfo monsterBookInfo) {
        this.monsterBookInfo = monsterBookInfo;
    }

    public DamageCalc getDamageCalc() {
        return damageCalc;
    }

    public void setDamageCalc(DamageCalc damageCalc) {
        this.damageCalc = damageCalc;
    }

    /**
     * Gets the current amount of a given stat the character has. Includes things such as skills, items, etc...
     *
     * @param baseStat the requested stat
     * @return the amount of stat
     */
    private double getTotalStatAsDouble(BaseStat baseStat) {
        double stat = 0;
        if (JobConstants.isNoManaJob(getJob()) && baseStat == BaseStat.mmp) {
            if (JobConstants.isKinesis(getJob())) {
                return ((Kinesis) getJobHandler()).getMaxPP();
            }
            if (JobConstants.isZero(getJob())) {
                stat += baseStat.toStat() == null ? 0 : getStat(baseStat.toStat());
                stat += baseStats.getOrDefault(baseStat, 0L);
                // Stat gained by the stat's corresponding rate value
                if (baseStat.getRateVar() != null) {
                    if (!baseStat.isRateVar()) {
                        stat += stat * (getTotalStat(baseStat.getRateVar()) / 100D);
                    }
                }
                return stat;
            }
            if (JobConstants.isAngelicBuster(getJob()) || JobConstants.isDemonAvenger(getJob())) {
                return 0;
            }
            stat += baseStat.toStat() == null ? 0 : getStat(baseStat.toStat());
            if (hasSkill(80000406)) { // Increases Max DF
                stat += getSkill(80000406, false).getCurrentLevel() * 10;
            }
            // Demon Slayer's DF gained by secondary weapons
            if (JobConstants.isDemonSlayer(getJob())) {
                Equip equip = (Equip) getEquippedInventory().getItemBySlot(10);
                if (equip != null) {
                    stat += equip.getBaseStat(baseStat);
                }
            }
        } else {
            BaseStat kannaHPBaseStat = null;
            if (JobConstants.isKanna(getJob()) && baseStat == BaseStat.mhp && JobConstants.isNoManaJob(getJob())) {
                kannaHPBaseStat = BaseStat.mmp; // 265: Kanna's Max MP bonuses go to Max HP (282 keeps them on MP)
            }
            // Stat allocated by sp
            stat += baseStat.toStat() == null ? 0 : getStat(baseStat.toStat());
            // Stat gained by passives
            stat += baseStats.getOrDefault(baseStat, 0L);
            stat += getGuildBaseStatContribution(baseStat);
            // Stat gained by buffs
            stat += getTemporaryStatManager().getBaseStats().getOrDefault(baseStat, 0);
            // Stat gained by the stat's corresponding "per level" value
            if (baseStat.getLevelVar() != null) {
                stat += getTotalStatAsDouble(baseStat.getLevelVar()) * getLevel();
            }
            // Stat gained by equips
            for (Item item : getEquippedInventory().getItems()) {
                Equip equip = (Equip) item;
                stat += equip.getBaseStat(baseStat);
                if (kannaHPBaseStat != null) {
                    stat += equip.getBaseStat(kannaHPBaseStat);
                }
                if (baseStat == BaseStat.mhpR && equip.getiMaxHpr() > 0) {
                    stat += equip.getiMaxHpr();
                } else if (baseStat == BaseStat.mmpR && equip.getiMaxMpr() > 0) {
                    stat += equip.getiMaxMpr();
                }
            }
            // Stat gained by set effects
            stat += getStatAmountSetEffect(baseStat);
            if (kannaHPBaseStat != null) {
                stat += getStatAmountSetEffect(kannaHPBaseStat);
            }
            // Stat gained by character potentials
            String presetStr = getQRValueByKey(QuestConstants.CHARACTER_POTENTIAL_PRESET, "potential");
            if (presetStr == null) {
                presetStr = "0";
            }
            int preset = Integer.parseInt(presetStr);
            for (CharacterPotential cp : getPotentialsByPreset(preset)) {
                Skill skill = cp.getSkill();
                if (skill == null) {
                    continue;
                }
                SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
                Map<BaseStat, Integer> stats = si.getBaseStatValues(this, skill.getCurrentLevel());
                stat += stats.getOrDefault(baseStat, 0);
                if (kannaHPBaseStat != null) {
                    stat += stats.getOrDefault(kannaHPBaseStat, 0);
                }
            }
            // Stat gained by the stat's corresponding rate value
            if (baseStat.getRateVar() != null) {
                if (!baseStat.isRateVar()) {
                    if (kannaHPBaseStat != null) {
                        stat += stat * ((getTotalStat(baseStat.getRateVar()) + getTotalStat(kannaHPBaseStat.getRateVar())) / 100D);
                    } else {
                        stat += stat * (getTotalStat(baseStat.getRateVar()) / 100D);
                    }
                }
            }
        }
        return stat;
    }

    private double getTotalStatCP(BaseStat baseStat) {
        double stat = 0;
        if (JobConstants.isNoManaJob(getJob()) && baseStat == BaseStat.mmp) {
            if (JobConstants.isKinesis(getJob())) {
                return ((Kinesis) getJobHandler()).getMaxPP();
            }
            if (JobConstants.isZero(getJob())) {
                stat += baseStat.toStat() == null ? 0 : getStat(baseStat.toStat());
                stat += baseStats.getOrDefault(baseStat, 0L);
                // Stat gained by the stat's corresponding rate value
                if (baseStat.getRateVar() != null) {
                    if (!baseStat.isRateVar()) {
                        stat += stat * (getTotalStat(baseStat.getRateVar()) / 100D);
                    }
                }
                return stat;
            }
            if (JobConstants.isAngelicBuster(getJob()) || JobConstants.isDemonAvenger(getJob())) {
                return 0;
            }
            stat += baseStat.toStat() == null ? 0 : getStat(baseStat.toStat());
            if (hasSkill(80000406)) { // Increases Max DF
                stat += getSkill(80000406, false).getCurrentLevel() * 10;
            }
            // Demon Slayer's DF gained by secondary weapons
            if (JobConstants.isDemonSlayer(getJob())) {
                Equip equip = (Equip) getEquippedInventory().getItemBySlot(10);
                if (equip != null) {
                    stat += equip.getBaseStat(baseStat);
                }
            }
        } else {
            BaseStat kannaHPBaseStat = null;
            if (JobConstants.isKanna(getJob()) && baseStat == BaseStat.mhp && JobConstants.isNoManaJob(getJob())) {
                kannaHPBaseStat = BaseStat.mmp; // 265: Kanna's Max MP bonuses go to Max HP (282 keeps them on MP)
            }
            // Stat allocated by sp
            stat += baseStat.toStat() == null ? 0 : getStat(baseStat.toStat());
            // Stat gained by passives
            stat += baseStats.getOrDefault(baseStat, 0L);
            stat += getGuildBaseStatContribution(baseStat);
            // Stat gained by equips
            for (Item item : getEquippedInventory().getItems()) {
                Equip equip = (Equip) item;
                stat += equip.getBaseStat(baseStat);
                if (kannaHPBaseStat != null) {
                    stat += equip.getBaseStat(kannaHPBaseStat);
                }
                if (baseStat == BaseStat.mhpR && equip.getiMaxHpr() > 0) {
                    stat += equip.getiMaxHpr();
                } else if (baseStat == BaseStat.mmpR && equip.getiMaxMpr() > 0) {
                    stat += equip.getiMaxMpr();
                }
            }
            // Stat gained by set effects
            stat += getStatAmountSetEffect(baseStat);
            if (kannaHPBaseStat != null) {
                stat += getStatAmountSetEffect(kannaHPBaseStat);
            }
            // Stat gained by character potentials
            String presetStr = getQRValueByKey(QuestConstants.CHARACTER_POTENTIAL_PRESET, "potential");
            if (presetStr == null) {
                presetStr = "0";
            }
            int preset = Integer.parseInt(presetStr);
            for (CharacterPotential cp : getPotentialsByPreset(preset)) {
                Skill skill = cp.getSkill();
                if (skill == null) {
                    continue;
                }
                SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
                Map<BaseStat, Integer> stats = si.getBaseStatValues(this, skill.getCurrentLevel());
                stat += stats.getOrDefault(baseStat, 0);
                if (kannaHPBaseStat != null) {
                    stat += stats.getOrDefault(kannaHPBaseStat, 0);
                }
            }
            // Stat gained by the stat's corresponding rate value
            if (baseStat.getRateVar() != null) {
                if (!baseStat.isRateVar()) {
                    if (kannaHPBaseStat != null) {
                        stat += stat * ((getTotalStat(baseStat.getRateVar()) + getTotalStat(kannaHPBaseStat.getRateVar())) / 100D);
                    } else {
                        stat += stat * (getTotalStat(baseStat.getRateVar()) / 100D);
                    }
                }
            }
        }
        return stat;
    }

    public int getTotalStat(BaseStat stat) {
        return (int) getTotalStatAsDouble(stat);
    }

    /**
     * Gets a total list of basic stats that a character has, including from skills, items, etc...
     *
     * @return the total list of basic stats
     */
    public Map<BaseStat, Integer> getTotalBasicStats() {
        Map<BaseStat, Integer> stats = new HashMap<>();
        for (BaseStat bs : BaseStat.values()) {
            stats.put(bs, getTotalStat(bs));
        }
        return stats;
    }

    /**
     * Returns the item the user has for protecting buffs.
     *
     * @return the Item the user has for protecting buffs, or null if there is none.
     */
    public boolean hasBuffProtector() {
        return hasItem(5133000) || hasItem(5133001);
    }

    public boolean hasDeadProtectExpMaplePoint() {
        return false;
    }

    public boolean hasDeadProtectBuffMaplePoint() {
        return false;
    }

    public int getReviveType() {
        boolean isPremiumType = false;
        boolean isMaplePointType = false;
        boolean isQuestPointType = false;
        boolean isUpgradeTombType = false;
        int reviveType;
        if (isPremiumType) {
            // Unhandled PREMIUM(218)
            reviveType = ReviveType.PREMIUM.getVal();
        } else if (isMaplePointType) {
            // Unhandled USER_PROTECT_BUFF_DIE_MAPLE_POINT_REQUEST(364)
            // Unhandled USER_PROTECT_EXP_DIE_MAPLE_POINT_REQUEST(365)
            reviveType = ReviveType.MAPLEPOINT.getVal();
        } else if (isQuestPointType) {
            // Find out what is this?
            reviveType = ReviveType.QUESTPOINT.getVal();
        } else if (isUpgradeTombType) {
            // Unhandled USER_UPGRADE_TOMB_EFFECT(216)
            reviveType = ReviveType.UPGRADETOMB.getVal();
        } else if (getTemporaryStatManager().hasStat(SoulStone)) {
            // Nexon removed soul stone skill so this is useless.
            reviveType = ReviveType.SOULSTONE.getVal();
        } else if (InGameEventManager.getInstance().charInEventMap(getId())) {
            // For player that died in InGameEvent by monster hit.
            reviveType = ReviveType.EVENT.getVal();
        } else {
            reviveType = ReviveType.NORMAL.getVal();
        }
        return reviveType;
    }

    public void openUIOnDead() {
        if (getHP() <= 0) {
            net.swordie.ms.client.jobs.flora.Tms282AetherGuard.reset(this, System.currentTimeMillis());
            net.swordie.ms.client.jobs.adventurer.thief.Tms282BladeTempest.reset(this);
            net.swordie.ms.client.jobs.cygnus.BlazeWizard.stopInferno(this);
            net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.deathOnce(this);
            int flag = 1;
            int reviveType = 0;
            if (hasItem(5510000)) {
                reviveType = 6;
            }
            if (hasItem(5133000) || hasItem(5133001)) {
                flag |= 2;
            }
            if (getDeathCount() >= 0 && getInstance() != null) {
                reviveType = 3;
            }
            write(UserLocal.openUIOnDead(this, flag, reviveType));
        }
    }

    public void openUIOnDeadOld() {
        if (getHP() <= 0) {
            if (getLastReviveTime() != 0) {
                if (System.currentTimeMillis() - getLastReviveTime() >= 8000L) {
                    revive(getField().getId());
                }
            } else {
                setLastReviveTime(System.currentTimeMillis());
                getTimer().addEvent(() -> revive(getField().getId()), 5000L);
            }
        }
    }

    public void handleExpDropPenalty() {
        if (getInstance() != null) {
            removeQuest(100716);
            write(WvsContext.updateExpDropPenalty(false, 0, 0, 0, 0));
            return;
        }
        if (hasQuest(100716)) {
            int remain = Integer.parseInt(getQRValueByKey(100716, "remain"));
            if (remain > 0) {
                remain -= 1;
                setQRValueByKey(100716, "remain", remain + "");
                setQRValueByKey(100716, "time", FileTime.currentTime().toYYYYMMDDHHMMSS());
                write(WvsContext.updateExpDropPenalty(false, 300, remain, 80, 80));
            } else {
                removeQuest(100716);
                write(WvsContext.updateExpDropPenalty(false, 0, 0, 0, 0));
            }
        }
    }

    public void revive(int targetField) {
        var oniForRevive = net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                && getJobHandler() instanceof net.swordie.ms.client.jobs.sengoku.Kanna kannaForRevive
                ? kannaForRevive.getOniController() : null;
        if (oniForRevive != null) oniForRevive.beforeRevive();
        try {
            dispose();
            Field field = getField();
            Mob will = field.getMobByTemplateId(8880300);
            if (will == null) {
                will = field.getMobByTemplateId(8880340);
                if (will == null) {
                    will = field.getMobByTemplateId(8880301);
                    if (will == null) {
                        will = field.getMobByTemplateId(8880341);
                    }
                }
            }
            if (will != null) {
                if (will.isSpecialPattern()) {
                    will.setSpecialPattern(false);
                }
                if (will.isUseSpecialSkill()) {
                    will.setUseSpecialSkill(false);
                }
            }
            if (InGameEventManager.getInstance().charInEventMap(getId())) {
                InGameEventManager.getInstance().getActiveEvent().onMigrateDeath(this);
                heal(getMaxHP(), true);
                healMP(getMaxMP());
                return;
            }
            // Character is dead, respawn request
            int returnMap = field.getReturnMap() == 999999999 ? getPreviousFieldID() : field.getReturnMap();
            TemporaryStatManager tsm = getTemporaryStatManager();
            ScriptManagerImpl sm = getScriptManager();
            Option spreadThrow = tsm.getOption(NightLord_SpreadThrow);
            if (getInstance() == null) {
                if (!hasBuffProtector()) {
                    tsm.removeAllStats();
                    if (JobConstants.isWindArcher(getJob()) && getJobHandler() instanceof WindArcher windArcher) {
                        windArcher.updateWindEnergy(0);
                    } else if (JobConstants.isCannoneer(getJob()) && getJobHandler() instanceof Cannoneer) {
                        getJobHandler().updateVSkillStackBuff(this, 0);
                    }
                } else {
                    chatMessage(Tip, "你已復活，並啟動了凍結加持器的效果！");
                }
            }
            //Khi có deadCount
            int deathCount = getDeathCount();
            if (getInstance() == null) {
                if (deathCount > 0) {
                    deathCount -= 1;
                    setDeathCount(deathCount);
                    showDeathCount(deathCount);
                } else {
                    warp(returnMap);
                    if (deathProtectedByCharm) {
                        deathProtectedByCharm = false; // 282 safety charm: no death debuff
                    } else {
                        createQuestWithQRValue(100716, "time="+FileTime.currentTime().toYYYYMMDDHHMMSS()+";remain=300;total=300;decExpR=80;decDropR=80");
                        write(WvsContext.updateExpDropPenalty(true, 300, 300, 80, 80));
                        this.deathPenaltyTimer = getTimer().addFixedRateEvent(this::handleExpDropPenalty, 0, 1000L, 302);
                    }
                }
            } else if (getInstance() != null) {
                if (deathCount > 0) {
                    deathCount -= 1;
                    setDeathCount(deathCount);
                    showDeathCount(deathCount);
                    if (deathCount <= 0) {
                        sm.showEffectToField("Map/Effect.img/killing/fail");
                        // multiplayer-0930: only this character leaves (was: warpInstanceOut = whole party out, and an
                        // NPE when the instance was gone after the 2 s)
                        final Instance deadIn = getInstance();
                        getTimer().addEvent(() -> {
                            if (deadIn != null && getInstance() == deadIn) {
                                net.swordie.ms.world.field.InstanceMembership.leave(this, -1, 0);
                            }
                        }, 2000);
                    }
                } else { // No DeathCount ( = -1): only this character goes to the forced return map (multiplayer-0930)
                    net.swordie.ms.world.field.InstanceMembership.leave(this, -1, 0);
                }
            } else if (getTransferField() == targetField && getTransferFieldReq() == field.getId()) {
                Field toField = getOrCreateFieldByCurrentInstanceType(getTransferField());
                if (toField != null && getTransferField() > 0) {
                    warp(toField);
                }
                setTransferField(0);
            } else {
                warp(getOrCreateFieldByCurrentInstanceType(field.getForcedReturn()));
            }
            healHPMP();
            Portal portal = field.getPortalByName("sp");
            if (portal != null) {
                Position position = new Position(portal.getX(), portal.getY());
                write(FieldPacket.teleport(position, this));
            } else {
                chatMessage("未找到合適的復活位置，因此你將在死亡位置立即復活。");
            }
            write(FieldPacket.fieldEffect(FieldEffect.takeSnapShotOfClient(1000)));
            if (spreadThrow != null && hasBuffProtector()) {
                SkillInfo si = SkillData.getSkillInfoById(NightLord.THROWING_STAR_BARRAGE);
                if (si != null) {
                    int time = si.getValue(SkillStat.time, getSkillLevel(NightLord.THROWING_STAR_BARRAGE)) - (((Util.getCurrentTime() - spreadThrow.tStart)) / 1000);
                    if (time > 2) {
                        Option o = new Option();
                        o.nOption = 1;
                        o.rOption = spreadThrow.rOption;
                        o.tOption = time;
                        o.tStart = spreadThrow.tStart;
                        tsm.sendStat(NightLord_SpreadThrow, o);
                    }
                }
            }
            int linkSkillID = SkillConstants.getOriginalOfLinkedSkill(SkillConstants.getLinkSkillByJob(getJob()));
            if (hasSkill(80000329)) {
                Option o = new Option();
                o.nOption = 1;
                o.rOption = 80000329;
                o.tOption = getSkillLevel(80000329);
                tsm.sendStat(NotDamaged, o);
            } else if (linkSkillID >= 30000074 && linkSkillID <= 30000077) {
                Option o = new Option();
                o.nOption = 1;
                o.rOption = linkSkillID;
                o.tOption = hasSkill(80000329) ? getSkillLevel(80000329) : getSkillLevel(linkSkillID);
                tsm.sendStat(NotDamaged, o);
            }
            setLastReviveTime(0L);
        } finally {
            if (oniForRevive != null) oniForRevive.afterRevive();
        }
    }

    /**
     * Resets the combo kill's timer. Interrupts the previous timer if there was one.
     */
    public void comboKillResetTimer() {
        if (comboKillResetTimer != null && !comboKillResetTimer.isDone()) {
            comboKillResetTimer.cancel(true);
        }
        comboKillResetTimer = getTimer().addEvent(() -> setComboCounter(0), GameConstants.COMBO_KILL_RESET_TIMER, TimeUnit.SECONDS);
    }

    public Int2LongMap getSkillCoolTimes() {
        return skillCoolTimes;
    }

    public void setSkillCoolTimes(Int2LongMap skillCoolTimes) {
        this.skillCoolTimes = skillCoolTimes;
    }

    public void addSkillCoolTime(int skillId, long nextusabletime) {
        getSkillCoolTimes().put(skillId, nextusabletime);
    }

    public void removeSkillCoolTime(int skillId) {
        skillCoolTimes.remove(skillId);
    }

    public void resetSkillCoolTime(int skillId) {
        SkillInfo si = SkillData.getSkillInfoById(skillId);
        if (hasSkillOnCooldown(skillId) && !si.isNotCooltimeReset()) {
            addSkillCoolTime(skillId, 0);
            write(UserLocal.skillCooltimeSetM(skillId, 0));
        }
    }

    public void reduceSkillCoolTime(int skillId, long amountInMS) {
        SkillInfo si = SkillData.getSkillInfoById(skillId);
        if (hasSkillOnCooldown(skillId) && !si.isNotCooltimeReset()) {
            long nextUsableTime = getSkillCoolTimes().get(skillId);
            addSkillCoolTime(skillId, nextUsableTime - amountInMS);
            write(UserLocal.skillCooltimeSetM(skillId, (int) ((nextUsableTime - amountInMS) - System.currentTimeMillis() < 0 ? 0 : (nextUsableTime - amountInMS) - System.currentTimeMillis())));
        }
    }

    public long getRemainingCoolTime(int skillId) {
        if (hasSkillOnCooldown(skillId)) {
            return getSkillCoolTimes().getOrDefault(skillId, System.currentTimeMillis()) - System.currentTimeMillis();
        }
        return 0L;
    }

    /**
     * Checks whether or not a skill is currently on cooldown.
     *
     * @param skillID the skill's id to check
     * @return whether or not a skill is currently on cooldown
     */
    public boolean hasSkillOnCooldown(int skillID) {
        long now = System.currentTimeMillis();
        long nextUsableTime = skillCoolTimes.get(skillID); // 0 nếu không có
        return now < nextUsableTime;
    }

    public boolean checkAndSetSkillCooltime(int skillID, boolean fromAttack) {
        if (skillID == 0) {
            return true;
        }
        var tsm = getTemporaryStatManager();
        if (fromAttack) {
            switch (skillID) {
                case DemonSlayer.DARK_METAMORPHOSIS:
                case DualBlade.BLADE_CLONE:
                case DualBlade.HEXA_BLADE_CLONE:
                    if (tsm.hasStatBySkillId(skillID)) {
                        return true;
                    }
                    break;
                case Paladin.HAMMERS_OF_THE_RIGHTEOUS:
                case Kanna.EXORCIST_CHARM:
                case Job.STELLAR_STAFF:
                case Job.UFO_RAID:
                    return true;
                default:
                    var si = SkillData.getSkillInfoById(skillID);
                    if (SkillConstants.isNoCoolDownAttack(skillID) || SixthJobSkillConstants.isRecognizedOriginSkill(skillID, si) || SixthJobSkillConstants.isRecognizedAscentSkill(skillID, si)) {
                        return true;
                    }
                    break;
            }
        }
        else {
            if (skillID == Phantom.SHROUD_WALK || skillID == Zero.CHRONO_BREAK) {
                return true;
            }
            if (skillID == Paladin.HAMMERS_OF_THE_RIGHTEOUS_2) {
                skillID = Paladin.HAMMERS_OF_THE_RIGHTEOUS;
            }
        }
        var o1 = tsm.getOptByCTSAndSkill(FifthGoddessBless, Job.GRANDIS_GODDESS_BLESSING_KAISER);
        if (o1 != null) {
            SkillInfo si = SkillData.getSkillInfoById(Job.GRANDIS_GODDESS_BLESSING);
            int slv = getSkillLevel(Job.GRANDIS_GODDESS_BLESSING);
            int currentZOption = o1.zOption;
            if (Util.succeedProp(si.getValue(x, slv)) && currentZOption >= 1) {
                if (currentZOption == 1) {
                    tsm.removeStatsBySkill(Job.GRANDIS_GODDESS_BLESSING_KAISER);
                    write(UserPacket.effect(Effect.skillUse(Job.GRANDIS_GODDESS_BLESSING_KAISER, getLevel(), slv)));
                    getField().broadcast(UserRemote.effect(getId(), Effect.skillUse(Job.GRANDIS_GODDESS_BLESSING_KAISER, getLevel(), slv)), this);
                    return true;
                } else {
                    currentZOption -= 1;
                    o1.nOption = 1;
                    o1.rOption = Job.GRANDIS_GODDESS_BLESSING_KAISER;
                    o1.tOption = (int) tsm.getRemainingTime(FifthGoddessBless, Job.GRANDIS_GODDESS_BLESSING_KAISER) / 1000;
                    o1.zOption = currentZOption;
                    tsm.sendStat(FifthGoddessBless, o1);
                    write(UserPacket.effect(Effect.skillUse(Job.GRANDIS_GODDESS_BLESSING_KAISER, getLevel(), slv)));
                    getField().broadcast(UserRemote.effect(getId(), Effect.skillUse(Job.GRANDIS_GODDESS_BLESSING_KAISER, getLevel(), slv)), this);
                    return true;
                }
            }
        }
        var scd = getPotentialBySkillID(0, 70000045);
        SkillInfo skillInfoForCooldown = SkillData.getSkillInfoById(skillID);
        if (scd != null && skillID != 0 && skillInfoForCooldown != null && skillInfoForCooldown.hasCooltime()) {
            var si = SkillData.getSkillInfoById(70000045);
            if (si != null && SkillConstants.isAppliedSkipCooldown(getJob(), skillID) && Util.succeedProp(si.getValue(nocoolProp, scd.getSlv()))) {
                chatMessage("你通過潛能效果成功跳過了該技能的冷卻時間。");
                return true;
            }
        }
        if (hasSkillOnCooldown(skillID)) {
            if (skillID == net.swordie.ms.client.jobs.resistance.BattleMage.ABYSSAL_LIGHTNING) {
                return tsm.hasStatBySkillId(skillID); // recast during the buff = portal teleport (cooldown already running)
            } else if (skillID == Hero.BURNING_SOUL_BLADE || skillID == Hero.BURNING_SOUL_BLADE_STATIONARY) {
                return tsm.hasStatBySkillId(Hero.BURNING_SOUL_BLADE) || tsm.hasStatBySkillId(Hero.BURNING_SOUL_BLADE_STATIONARY);
            } else {
                return false;
            }
        } else {
            int count = getJobHandler().handleSetCoolDownSkill(skillID);
            if (count > 0) {
                return true;
            }
            switch (skillID) {
                case 2321055 -> { // Heaven's Door
                    setSkillCooldown(2321052, getSkillLevel(2321052));
                    return true;
                }
                case Hero.BURNING_SOUL_BLADE, Hero.BURNING_SOUL_BLADE_STATIONARY -> {
                    setSkillCooldown(skillID, getSkillLevel(skillID));
                    return true;
                }
            }
            var si = SkillData.getSkillInfoById(skillID);
            if (SixthJobSkillConstants.isRecognizedAscentSkill(skillID, si)) {
                //return true; TODO
            }
            var skill = getSkill(skillID);
            if (skill != null && si != null && si.hasCooltime() && !SkillConstants.isKeydownCDSkill(skillID)) {
                int actualSkillID = SkillConstants.getActualSkillIDfromSkillID(skillID);
                setSkillCooldown(actualSkillID, getSkillLevel(actualSkillID));
            }
            return true;
        }
    }

    public void addSkillCooldown(int skillId, int time) {
        addSkillCoolTime(skillId, System.currentTimeMillis() + time);
        write(UserLocal.skillCooltimeSetM(skillId, time));
    }

    /**
     * Sets a skill's cooltime according to their property in the WZ files, and stores the moment where the skill
     * comes off of cooldown.
     *
     * @param skillID the skill's id to set
     * @param slv     the current skill level
     */
    public void setSkillCooldown(int skillID, int slv) {
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        TemporaryStatManager tsm = getTemporaryStatManager();
        if (si != null) {
            int cdInSec = si.getValue(cooltime, slv);
            if (skillID == DemonAvenger.DEMONIC_FRENZY) {
                cdInSec = si.getValue(z, slv);
            } else if (skillID == Hero.BURNING_SOUL_BLADE
                    || skillID == Hero.BURNING_SOUL_BLADE_STATIONARY) {
                cdInSec = si.getValue(x, slv);
            } else if (skillID == Zero.CHRONO_BREAK) {
                cdInSec = si.getValue(q, slv);
            } else if (skillID == NightWalker.SHADOW_STITCH) { // Skill-log
                cdInSec = 200 - slv; // c.d 210s -> 180s
            } else if (skillID == Hero.CRY_VALHALLA) { // Skill-log
                cdInSec = 80; // c.d 150s -> 80s
            } else if (skillID == Paladin.HEAVENS_HAMMER) { // Skill-log
                cdInSec = 14 - (slv / 3); // c.d 15s -> 4s
            }

            int cdInMillis = cdInSec > 0 ? cdInSec * 1000 : si.getValue(cooltimeMS, slv);
            int alteredcd = getJobHandler().alterCooldownSkill(skillID);
            if (alteredcd >= 0) {
                cdInMillis = alteredcd;
            }

            // RuneStone of Skill
            if (tsm.hasStat(FixCoolTime) && !si.isNotCooltimeReset()) {
                cdInMillis = (int) (tsm.getTotalNOptionOfStat(FixCoolTime) * 1000);
            }

            // Stats reduced Skill Cool Time
            if (getTotalStat(BaseStat.reduceCooltime) != 0) {
                cdInMillis -= getTotalStat(BaseStat.reduceCooltime) * 1000;
            }

            if (tsm.hasStat(IndieCooltimeReduce)) {
                int cooltimeReduce = (int) (tsm.getTotalNOptionOfStat(IndieCooltimeReduce) / 1000);
                if (cooltimeReduce > 0) {
                    cdInMillis /= cooltimeReduce;
                } else {
                    cdInMillis *= (1 - cooltimeReduce);
                }
            }

            chatMessage(AdminChat, "[技能冷卻] 技能 ID : " + skillID + " : "  + cdInMillis);
            if (cdInMillis > 0) {
                addSkillCoolTime(skillID, System.currentTimeMillis() + cdInMillis);
                write(UserLocal.skillCooltimeSetM(skillID, cdInMillis));
            }
        }
    }

    public CharacterPotentialMan getPotentialMan() {
        return potentialMan;
    }

    public Set<CharacterPotential> getPotentials() {
        return potentials;
    }

    public Set<CharacterPotential> getPotentialsByPreset(int preset) {
        Set<CharacterPotential> characterPotentials = new HashSet<>();
        for (CharacterPotential potential : getPotentials()) {
            if (potential.getPreset() == preset) {
                characterPotentials.add(potential);
            }
        }
        return characterPotentials;
    }

    public void setPotentials(Set<CharacterPotential> potentials) {
        this.potentials = potentials;
    }

    public List<Integer> getPotentialSkills(int preset) {
        List<Integer> result = new ArrayList<>();
        getPotentialsByPreset(preset).forEach(cp -> result.add(cp.getSkillID()));
        return result;
    }

    public CharacterPotential getPotentialBySkillID(int preset, int skillID) {
        for (CharacterPotential characterPotential : getPotentialsByPreset(preset)) {
            if (characterPotential.getSkillID() == skillID) {
                return characterPotential;
            }
        }
        return null;
    }

    public int getHonorExp() {
        return getAvatarData().getCharacterStat().getHonorExp();
    }

    public void setHonorExp(int honorExp) {
        getAvatarData().getCharacterStat().setHonorExp(honorExp);
    }

    /**
     * Adds honor exp to this Char, and sends a packet to the client with the new honor exp.
     * Honor exp added may be negative, but the total honor exp will never go below 0.
     *
     * @param exp the exp to add (may be negative)
     */
    public void addHonorExp(int exp) {
        setHonorExp(Math.max(0, getHonorExp() + exp));
        write(WvsContext.characterHonorExp(getHonorExp()));
        if (exp > 0) {
            AchievementHandler.handleEarnHonorEXP(this, exp);
        } else {
            AchievementHandler.handleConsumeHonorEXP(this, exp);
        }
    }

    public int getDeathCount() {
        return deathCount;
    }

    public void setDeathCount(int deathCount) {
        this.deathCount = deathCount;
    }

    public boolean[] getVHDeathCount() {
        return vhDeathCount;
    }

    public void setVHDeathCount(boolean[] vhDeathCount) {
        this.vhDeathCount = vhDeathCount;
    }

    public void showDeathCount(int deathCount) {
        if (deathCount > 0) {
            if (getFieldID() != 262030100 && getFieldID() != 262031100
                    && getFieldID() != 262030200 && getFieldID() != 262030310
                    && getFieldID() != 262031200 && getFieldID() != 262031300
                    && getFieldID() != 262031310 && getFieldID() != 450010500) {
                //write(UserLocal.deathCountInfo(deathCount));
                write(UserLocal.deathCountInfo(getId(), deathCount));
            } else if (getFieldID() == 262031300) {
                write(WvsContext.fieldValue("TotalDeathCount", "15"));
                write(WvsContext.fieldValue("DeathCount", (15 - deathCount) + ""));
            } else if (getFieldID() == 450010500) {
                write(VerusHillaPacket.encode(3, this, getField()));
            }
        }
    }

    public long getRuneCooldown() {
        return runeStoneCooldown;
    }

    public void setRuneCooldown(long runeCooldown) {
        this.runeStoneCooldown = runeCooldown;
    }

    public MemorialCubeInfo getMemorialCubeInfo() {
        return memorialCubeInfo;
    }

    public void setMemorialCubeInfo(MemorialCubeInfo memorialCubeInfo) {
        this.memorialCubeInfo = memorialCubeInfo;
    }

    public Set<Familiar> getFamiliars() {
        return familiars;
    }

    public void setFamiliars(Set<Familiar> familiars) {
        this.familiars = familiars;
    }

    public boolean hasFamiliar(int familiarID) {
        return getFamiliars().stream().anyMatch(f -> f.getFamiliarID() == familiarID);
    }

    public Familiar getFamiliarByID(int familiarID) {
        return getFamiliars().stream().filter(f -> f.getFamiliarID() == familiarID).findAny().orElse(null);
    }

    public void addFamiliar(Familiar familiar) {
        getFamiliars().add(familiar);
    }

    public void removeFamiliarByID(int familiarID) {
        removeFamiliar(getFamiliarByID(familiarID));
    }

    public void removeFamiliar(Familiar familiar) {
        if (familiar != null) {
            getFamiliars().remove(familiar);
        }
    }

    public Familiar getActiveFamiliar() {
        return activeFamiliar;
    }

    public void setActiveFamiliar(Familiar activeFamiliar) {
        this.activeFamiliar = activeFamiliar;
    }

    public Set<StolenSkill> getStolenSkills() {
        return stolenSkills;
    }

    public void setStolenSkills(Set<StolenSkill> stolenSkills) {
        this.stolenSkills = stolenSkills;
    }

    public void addStolenSkill(StolenSkill stolenSkill) {
        getStolenSkills().add(stolenSkill);
    }

    public void removeStolenSkill(StolenSkill stolenSkill) {
        if (stolenSkill != null) {
            getStolenSkills().removeIf(x -> x.getSkillid() == stolenSkill.getSkillid() && x.getCharID() == stolenSkill.getCharID() && x.getCurrentlv() == stolenSkill.getCurrentlv());
            stolenSkill.deleteStolenSkillFromSQL();
        }
    }

    public StolenSkill getStolenSkillByPosition(int position) {
        return getStolenSkills().stream().filter(ss -> ss.getPosition() == position).findAny().orElse(null);
    }

    public StolenSkill getStolenSkillBySkillId(int skillId) {
        return getStolenSkills().stream().filter(ss -> ss.getSkillid() == skillId).findAny().orElse(null);
    }


    public Set<ChosenSkill> getChosenSkills() {
        return chosenSkills;
    }

    public void setChosenSkills(Set<ChosenSkill> chosenSkills) {
        this.chosenSkills = chosenSkills;
    }

    public void addChosenSkill(ChosenSkill chosenSkill) {
        getChosenSkills().add(chosenSkill);
    }

    public void removeChosenSkill(ChosenSkill chosenSkill) {
        if (chosenSkill != null) {
            getChosenSkills().removeIf(x -> x.getSkillId() == chosenSkill.getSkillId() && x.getCharID() == chosenSkill.getCharID() && x.getPosition() == chosenSkill.getPosition());
        }
    }

    public ChosenSkill getChosenSkillByPosition(int position) {
        if (getChosenSkills() == null) return null;
        return getChosenSkills().stream().filter(ss -> ss.getPosition() == position).findAny().orElse(null);
    }

    public boolean isChosenSkillInStolenSkillList(int skillId) {
        return getStolenSkills().stream().filter(ss -> ss.getSkillid() == skillId).findAny().orElse(null) != null;
    }

    /**
     * Adds a BaseStat's amount to this Char's BaseStat cache.
     *
     * @param bs     The BaseStat
     * @param amount the amount of BaseStat to add
     */
    public void addBaseStat(BaseStat bs, int amount) {
        baseStats.put(bs, baseStats.getOrDefault(bs, 0L) + amount);
    }

    /**
     * Removes a BaseStat's amount from this Char's BaseStat cache.
     *
     * @param bs     The BaseStat
     * @param amount the amount of BaseStat to remove
     */
    public void removeBaseStat(BaseStat bs, int amount) {
        addBaseStat(bs, -amount);
    }

    public void addItemToInventory(int id, int quantity) {
        if (!net.swordie.ms.world.tms282.items.Tms282ItemAvailability.allowGrant(this, id, "Char.addItemToInventory")) return;
        if (ItemConstants.isEquip(id)) {
            Equip equip = ItemData.getEquipDeepCopyFromID(id, false);
            addItemToInventory(equip.getInvType(), equip, false, false);
            write(WvsContext.inventoryOperation(true, false, Add, (short) equip.getBagIndex(), (byte) -1, 0, equip));
        } else {
            Item item = ItemData.getItemDeepCopy(id);
            item.setQuantity(quantity);
            addItemToInventory(item);
            write(WvsContext.inventoryOperation(true, false, Add, (short) item.getBagIndex(), (byte) -1, 0, item));
        }
        write(WvsContext.dropPickupMessage(id, (short) quantity));
    }

    public EquipBaseStat calculateMainStatForChar() {
        BaseStat mainStat = GameConstants.getMainBaseStatForJob(getJob());
        if (mainStat == null) {
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, "Player " + getName() + " try to calculateMainStatForChar null.");
            return EquipBaseStat.iStr;
        }
        if (mainStat.equals(BaseStat.str)) {
            return EquipBaseStat.iStr;
        } else if (mainStat.equals(BaseStat.dex)) {
            return EquipBaseStat.iDex;
        } else if (mainStat.equals(BaseStat.inte)) {
            return EquipBaseStat.iInt;
        } else if (mainStat.equals(BaseStat.luk)) {
            return EquipBaseStat.iLuk;
        }
        return null;
    }

    public void addItemToInventory(int id, int quantity, String type, long expires) {
        addItemToInventory(id, quantity, type, expires, 0);
    }

    public void addItemToInventory(int id, int quantity, String type, long expires, int chuc) {
        if (!net.swordie.ms.world.tms282.items.Tms282ItemAvailability.allowGrant(this, id, "Char.addItemToInventory")) return;
        LocalDateTime localDateTime = switch (type) {
            case "hour" -> LocalDateTime.now().plusHours(expires);
            case "minute" -> LocalDateTime.now().plusMinutes(expires);
            case "second" -> LocalDateTime.now().plusSeconds(expires);
            case "day" -> LocalDateTime.now().plusDays(expires);
            case "week" -> LocalDateTime.now().plusWeeks(expires);
            case "month" -> LocalDateTime.now().plusMonths(expires);
            default -> LocalDateTime.now().plusYears(expires);
        };
        if (ItemConstants.isEquip(id)) {  //Equip
            Equip equip = ItemData.getEquipDeepCopyFromID(id, false);
            equip.setDateExpire(FileTime.fromDate(localDateTime));
            if (id == 1702769) {
                equip.setiPad((short) 3);
                equip.setiMad((short) 3);
            } else if (id == 1004974 || id == 1053187 || id == 1103022) {
                equip.setStatR((short) 1);
                equip.setiMaxHp((short) 50);
            }
            if (chuc > 0) {
                equip.setChuc((short) chuc, true);
            }
            addItemToInventory(equip.getInvType(), equip, false, false);
            write(WvsContext.inventoryOperation(true, false,
                    Add, (short) equip.getBagIndex(), (byte) -1, 0, equip));

        } else {    //Item
            Item item = ItemData.getItemDeepCopy(id);
            item.setQuantity(quantity);
            item.setDateExpire(FileTime.fromDate(localDateTime));
            addItemToInventory(item);
            write(WvsContext.inventoryOperation(true, false,
                    Add, (short) item.getBagIndex(), (byte) -1, 0, item));
        }
    }

    public void addStackableWithSlotMaxItemToInventory(int id, int quantity, int slotMax, String type, long expires) {
        LocalDateTime localDateTime = switch (type) {
            case "hour" -> LocalDateTime.now().plusHours(expires);
            case "minute" -> LocalDateTime.now().plusMinutes(expires);
            case "second" -> LocalDateTime.now().plusSeconds(expires);
            case "day" -> LocalDateTime.now().plusDays(expires);
            case "week" -> LocalDateTime.now().plusWeeks(expires);
            case "month" -> LocalDateTime.now().plusMonths(expires);
            default -> LocalDateTime.now().plusYears(expires);
        };
        if (ItemConstants.isEquip(id)) {  //Equip
            Equip equip = ItemData.getEquipDeepCopyFromID(id, false);
            equip.setDateExpire(FileTime.fromDate(localDateTime));
            addStackableWithSlotMaxItemToInventory(equip.getInvType(), equip, slotMax, false, false);
            write(WvsContext.inventoryOperation(true, false,
                    Add, (short) equip.getBagIndex(), (byte) -1, 0, equip));

        } else {    //Item
            Item item = ItemData.getItemDeepCopy(id);
            item.setQuantity(quantity);
            item.setDateExpire(FileTime.fromDate(localDateTime));
            addStackableWithSlotMaxItemToInventory(item, slotMax, false);
            write(WvsContext.inventoryOperation(true, false,
                    Add, (short) item.getBagIndex(), (byte) -1, 0, item));

        }
    }

    public int getSpentHyperSp(int index) {
        int sp = 0;
        for (int skillID = 80000400; skillID <= 80000422; skillID++) {
            if (!SkillConstants.isHyperStat(skillID)) {
                continue;
            }
            HyperStat hyperStat = getHyperStats(index, skillID);
            if (hyperStat != null) {
                sp += SkillConstants.getTotalNeededSpForHyperStatSkill(hyperStat.getSkillLevel());
            }
        }
        return sp;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

    public void setDeletionStartTime(long deletionStartTime) {
        this.deletionStartTime = deletionStartTime;
    }

    public long getDeletionStartTime() {
        return deletionStartTime;
    }

    public int[] getHyperRockFields() {
        return hyperrockfields;
    }

    public void setHyperRockFields(int[] hyperrockfields) {
        this.hyperrockfields = hyperrockfields;
    }

    public byte getMonsterParkCount() {
        return monsterParkCount;
    }

    public void setMonsterParkCount(byte monsterParkCount) {
        this.monsterParkCount = monsterParkCount;
    }

    public TownPortal getTownPortal() {
        return townPortal;
    }

    public void setTownPortal(TownPortal townPortal) {
        this.townPortal = townPortal;
    }

    public TradeRoom getTradeRoom() {
        return tradeRoom;
    }

    public void setTradeRoom(TradeRoom tradeRoom) {
        this.tradeRoom = tradeRoom;
    }

    public void damage(int damage) {
        if (net.swordie.ms.client.character.items.Tms282SpecialWeaponEffects.blocksDamage(this, System.currentTimeMillis())) return;
        HitInfo hi = new HitInfo();
        hi.hpDamage = damage;
        getField().broadcast(UserPacket.userHitByCounter(getId(), -damage));
        getJobHandler().handleHit(getClient(), hi);
    }

    public void die() {
        setStatAndSendPacket(Stat.hp, 0);
        openUIOnDead();
    }

    @Override
    public String toString() {
        return "Char{" +
                "(" + super.toString() +
                ")id=" + getId() +
                ", accId=" + getAccId() +
                ", name=" + getName() +
                '}';
    }

    public boolean isBattleRecordOn() {
        return battleRecordOn;
    }

    public void setBattleRecordOn(boolean battleRecordOn) {
        this.battleRecordOn = battleRecordOn;
    }

    public void checkAndRemoveExpiredItems(boolean isExpireOnLogout) {
        if (lastCheckAndRemoveExpiredItems == 0 || System.currentTimeMillis() - lastCheckAndRemoveExpiredItems >= 600000L) {
            Inventory[] inventories = new Inventory[]{getEquippedInventory(), getEquipInventory(), getConsumeInventory(), getEtcInventory(), getInstallInventory(), getCashInventory(), getDecorationInventory()};
            Set<Item> expiredItems = new HashSet<>();
            for (Inventory inv : inventories) {
                expiredItems.addAll(inv.getItems().stream().filter(item -> item.getDateExpire() != null && item.getDateExpire().isExpired()).collect(Collectors.toSet()));
                if (isExpireOnLogout) {
                    expiredItems.addAll(inv.getItems().stream().filter(Item::isExpireOnLogout).collect(Collectors.toSet()));
                }
            }
            List<Integer> expiredItemIDs = expiredItems.stream().map(Item::getItemId).collect(Collectors.toList());
            if (!expiredItemIDs.isEmpty()) {
                write(WvsContext.message(MessageType.GENERAL_ITEM_EXPIRE_MESSAGE, expiredItemIDs));
                for (Item item : expiredItems) {
                    consumeItem(item);
                }
            }
            TimedSkills.removeExpired(this);
            lastCheckAndRemoveExpiredItems = System.currentTimeMillis();
        }
    }

    public void checkAndDeletedTotemQuests() {
        for (int questId = 62128; questId <= 62130; questId++) {
            if (hasQuestCompleted(questId)) {
                Quest q = getQuestById(questId);
                if (q != null && q.getCompletedTime() != null) {
                    if (q.getCompletedTime().toLocalDateTime().plusDays(30).isBefore(LocalDateTime.now())) {
                        removeQuest(questId);
                    }
                }
            }
        }
    }

    public boolean isGuildMaster() {
        return getGuild() != null && getGuild().getLeaderID() == getId();
    }

    /**
     * Checks if this Char has any of the given quests in progress. Also true if the size of the given set is 0.
     *
     * @param quests the set of quest ids to check
     * @return whether or not this Char has any of the given quests
     */
    public boolean hasAnyQuestsInProgress(Set<Integer> quests) {
        return quests.size() == 0 || quests.stream().anyMatch(this::hasQuestInProgress);
    }

    public int getPreviousFieldID() {
        return previousFieldID == 0 || previousFieldID == 999999999 ? 100000000 : previousFieldID;
    }

    public void setPreviousFieldID(int previousFieldID) {
        this.previousFieldID = previousFieldID;
    }

    public int getPreviousPortalID() {
        return previousPortalID;
    }

    public void setPreviousPortalID(int portalId) {
        previousPortalID = portalId;
    }

    public long getNextRandomPortalTime() {
        if (hasQuest(QuestConstants.RANDOM_PORTAL_RECORD)) {
            return Long.parseLong(getQRValue(QuestConstants.RANDOM_PORTAL_RECORD));
        } else {
            createQuestWithQRValue(QuestConstants.RANDOM_PORTAL_RECORD, "" + System.currentTimeMillis());
            return System.currentTimeMillis();
        }
    }

    public void setNextRandomPortalTime(long nextRandomPortalTime) {
        setQRValue(QuestConstants.RANDOM_PORTAL_RECORD, "" + nextRandomPortalTime);
    }

    public void clearCurrentDirectionNode() {
        this.currentDirectionNode.clear();
    }

    public int getCurrentDirectionNode(int node) {
        Integer direction = currentDirectionNode.getOrDefault(node, null);
        if (direction == null) {
            currentDirectionNode.put(node, 0);
        }
        return currentDirectionNode.get(node);
    }

    public void increaseCurrentDirectionNode(int node) {
        Integer direction = currentDirectionNode.getOrDefault(node, null);
        if (direction == null) {
            currentDirectionNode.put(node, 1);
        } else {
            currentDirectionNode.put(node, direction + 1);
        }
    }

    public void punishLieDetectorEvasion() {
        if (getLieDetectorAnswer().length() > 0) {
            net.swordie.ms.world.tms282.audit.AuditLog.antiMacro(this, 0, 0, getLieDetectorAnswer(), null, false, "EVASION"); // audit: zlog_antimacro
            failedLieDetector();
        }
    }

    public String getLieDetectorAnswer() {
        return lieDetectorAnswer;
    }

    public void setLieDetectorAnswer(String answer) {
        lieDetectorAnswer = answer;
    }

    public void failedLieDetector() {
        setLieDetectorAnswer("");
        chatMessage(SpeakerChannel, "你未通過測謊儀測試。");

        write(WvsContext.antiMacroResult(null, AntiMacro.AntiMacroResultType.AntiMacroRes_Fail.getVal(), AntiMacro.AntiMacroType.AntiMacroFieldRequest.getVal()));

        // TODO: handle fail
    }

    public void passedLieDetector() {
        setLieDetectorAnswer("");
        chatMessage(SpeakerChannel, "你已通過測謊儀測試！");

        write(WvsContext.antiMacroResult(null, AntiMacro.AntiMacroResultType.AntiMacroRes_Success.getVal(), AntiMacro.AntiMacroType.AntiMacroFieldRequest.getVal()));

        // TODO: handle pass
    }

    public boolean sendLieDetector() {
        return sendLieDetector(false);
    }

    public boolean sendLieDetector(boolean force) {
        // LD ran too recently (15 min)
        if (!force && lastLieDetector != 0 && System.currentTimeMillis() - lastLieDetector < 900_000L) {
            return false;
        }

        // TODO: don't allow more than 3 refreshes

        lieDetectorAnswer = "";
        String font = AntiMacro.FONTS[Util.getRandom(AntiMacro.FONTS.length - 1)];

        String options = "abcdefghijklmnopqrstuvwxyz0123456789";

        for (int i = 1; i <= 6; i++) {
            if (Util.getRandom(1) == 0) {
                options = options.toUpperCase();
            } else {
                options = options.toLowerCase();
            }

            lieDetectorAnswer += options.charAt(Util.getRandom(options.length() - 1));
        }

        try {
            AntiMacro am = new AntiMacro(font, lieDetectorAnswer);
            lastLieDetector = System.currentTimeMillis();

            byte[] image = am.generateImage(196, 44, Color.BLACK, AntiMacro.getRandomColor());
            write(WvsContext.antiMacroResult(image, AntiMacro.AntiMacroResultType.AntiMacroRes.getVal(), AntiMacro.AntiMacroType.AntiMacroFieldRequest.getVal()));
        } catch (IOException | FontFormatException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);

            return false;
        }

        return true;
    }

    public List<String> getLastCharReportName() {
        return lastCharReportName;
    }

    public int getReportCount() {
        return reportCount;
    }

    public void setReportCount(int reportCount) {
        this.reportCount = reportCount;
    }

    public long getLastReportTime() {
        return lastReportTime;
    }

    public int getOxQuizCount() {
        return oxQuizCount;
    }

    public void setOxQuizCount(int oxQuizCount) {
        this.oxQuizCount = oxQuizCount;
    }

    public void incOxQuizCount() {
        this.oxQuizCount += 1;
    }

    /**
     * Applies the mp consumption of a skill.
     *
     * @param si
     * @param slv     the current skill level
     * @return whether the consumption was successful (unsuccessful = not enough mp)
     */
    public boolean applyMpCon(SkillInfo si, int slv, boolean isUserSkillUseRequest) {
        if (si == null) {
            return true;
        }
        int skillID = si.getSkillId();
        TemporaryStatManager tsm = getTemporaryStatManager();
        short jobID = getJob();
        if (JobConstants.isAdventurerMage(jobID) && (hasSkill(FirePoison.INFINITY) || hasSkill(IceLightning.INFINITY) || hasSkill(Bishop.INFINITY))) {
            if (tsm.hasStat(Infinity)) {
                return true;
            }
        }
        if (JobConstants.isDemonSlayer(jobID) && hasSkill(DemonSlayer.BOUNDLESS_RAGE)) {
            if (tsm.hasStat(InfinityForce)) {
                return true;
            }
        }
        if (JobConstants.isBlaster(jobID) && (skillID == Blaster.BOBBING || skillID == Blaster.WEAVING)) {
            return true;
        }
        if (!isUserSkillUseRequest && JobConstants.isKinesis(getJob())) {
            return true;
        }
        if (isUserSkillUseRequest && skillID == Zero.CHRONO_BREAK) {
            return true;
        }
        if (!isUserSkillUseRequest && (SkillConstants.isShootObj(si)
                                    || SkillConstants.isNoMPConsumeSkill(skillID)
                                    || SixthJobSkillConstants.isRecognizedOriginSkill(skillID, si)
                                    || SixthJobSkillConstants.isRecognizedAscentSkill(skillID, si))) {
            return true;
        }
        int curMp = getStat(Stat.mp);
        int mpCon = JobConstants.isKanna(getJob()) ? si.getValue(epCon, slv) : si.getValue(SkillStat.mpCon, slv);
        int mpRCon = si.getValue(SkillStat.mpRCon, slv);
        if (mpRCon > 0) {
            mpCon = (int) (getMaxMP() * mpRCon / 100.0D);
        }
        if (hasSkill(Job.MANA_OVERLOAD) && tsm.hasStat(Wizard_OverloadMana)) {
            if (JobConstants.isDemonSlayer(getJob()) || JobConstants.isZero(getJob())) {
                mpCon = (int) (curMp * 1.0D / 100.0D);
            } else {
                mpCon = (int) (curMp * 2.0D / 100.0D);
            }
        } else {
            if (JobConstants.isDemonSlayer(getJob()) || JobConstants.isZero(getJob())) {
                mpCon = si.getValue(forceCon, slv);
                if (hasSkill(DemonSlayer.BLUE_BLOOD)) {
                    SkillInfo dsi = SkillData.getSkillInfoById(DemonSlayer.BLUE_BLOOD);
                    if (dsi != null) {
                        mpCon = mpCon * (100 - dsi.getValue(reduceForceR, getSkillLevel(DemonSlayer.BLUE_BLOOD))) / 100; // demon-0930: was *= (int) 0.8 = 0 (free DF)
                    }
                }
                if (skillID == DemonSlayer.DEMON_IMPACT && hasSkill(31120051)) {
                    // Demon Impact - Reduce Fury
                    SkillInfo dsi = SkillData.getSkillInfoById(31120051);
                    if (dsi != null) {
                        mpCon = mpCon * (100 - dsi.getValue(reduceForceR, getSkillLevel(31120051))) / 100; // demon-0930
                    }
                }
                if (skillID == DemonSlayer.DARK_METAMORPHOSIS && hasSkill(31120048)) {
                    // Dark Metamorphosis - Reduced Fury
                    SkillInfo dsi = SkillData.getSkillInfoById(31120048);
                    if (dsi != null) {
                        mpCon = mpCon * (100 - dsi.getValue(reduceForceR, getSkillLevel(31120048))) / 100; // demon-0930
                    }
                }
                if (skillID == DemonSlayer.DARK_METAMORPHOSIS && !isUserSkillUseRequest) {
                    return true;
                }
            } else {
                mpCon -= (int) (mpCon * (getTotalStat(BaseStat.mpconReduce) / 100D));
            }
        }
        if (mpCon == 0 && si.getValue(soulmpCon, slv) > 0) {
            mpCon = si.getValue(soulmpCon, slv);
            if (tsm.hasStat(SoulMP) && tsm.getOption(SoulMP).nOption >= ItemConstants.MAX_SOUL_CAPACITY && tsm.hasStat(FullSoulMP)) {
                int currentSoulCon = tsm.getOption(SoulMP).nOption;
                if (currentSoulCon >= mpCon) {
                    tsm.removeStat(FullSoulMP);
                    currentSoulCon -= mpCon;
                    Option o = tsm.getOption(SoulMP);
                    int soulID = ((Equip) getEquippedItemByBodyPart(BodyPart.Weapon)).getSoulOptionId();
                    o.nOption = Math.min(ItemConstants.MAX_SOUL_CAPACITY, currentSoulCon);
                    o.rOption = ItemConstants.getSoulSkillFromSoulID(soulID);
                    o.xOption = ItemConstants.MAX_SOUL_CAPACITY;
                    tsm.sendStat(SoulMP, o);
                    return true;
                }
                return false;
            }
            return false;
        }

        boolean hasEnough = curMp >= mpCon;
        if (hasEnough) {
            if (JobConstants.isKanna(getJob()) && getJobHandler() instanceof Kanna kanna) {
                kanna.chargeMPShikigami(mpCon);
            }
            addStatAndSendPacket(Stat.mp, -mpCon);
        }
        return hasEnough;
    }

    public boolean applyBulletCon(SkillInfo si, int slv) {
        if (si == null) {
            return true;
        }
        short jobID = getJob();

        if (!(JobConstants.isBowMaster(jobID)
                || JobConstants.isMarksman(jobID)
                || JobConstants.isNightLord(jobID)
                || JobConstants.isCorsair(jobID)
                || JobConstants.isWindArcher(jobID)
                || JobConstants.isNightWalker(jobID)
                || JobConstants.isWildHunter(jobID))) {
            return true;
        }

        if ((JobConstants.isBowMaster(jobID)   && hasSkill(BowMaster.SOUL_ARROW))
                || (JobConstants.isMarksman(jobID)   && hasSkill(Marksman.SOUL_ARROW))
                || (JobConstants.isNightLord(jobID)  && hasSkill(NightLord.SPIRIT_OF_THE_STAR))
                || (JobConstants.isCorsair(jobID)    && hasSkill(INFINITY_BLAST))
                || (JobConstants.isWindArcher(jobID) && hasSkill(WindArcher.SYLVAN_AID))
                || (JobConstants.isNightWalker(jobID)&& hasSkill(NightWalker.SPIRIT_PROJECTION))
                || (JobConstants.isWildHunter(jobID) && hasSkill(WildHunter.SOUL_ARROW_CROSSBOW))) {
            return true;
        }
        int bulletCon = 0;
        bulletCon = si.getValue(bulletCount, slv) + si.getValue(bulletConsume, slv);
        if (bulletCon <= 0) {
            return true;
        }
        int bulletItemId = getBulletIDForAttack();
        if (bulletItemId == 0) {
            return false;
        }
        if (!hasItemCount(getBulletIDForAttack(), bulletCon)) {
            setBulletIDForAttack(calculateBulletIDForAttack(bulletCon));
        }
        boolean hasEnough = hasItemCount(bulletItemId, bulletCon);
        if (hasEnough) {
            consumeItem(bulletItemId, bulletCon);
        }
        return hasEnough;
    }

    public boolean hasTutor() {
        return tutor;
    }

    public void hireTutor(boolean set) {
        tutor = set;
        write(UserLocal.hireTutor(set));
    }

    public boolean isPracticeMode() {
        return isPracticeMode;
    }

    public void setPracticeMode(boolean bool) {
        this.isPracticeMode = bool;
    }

    /**
     * Shows tutor automated message (the client is taking the message information from wz).
     *
     * @param id       the id of the message.
     * @param duration message duration
     */
    public void tutorAutomatedMsg(int id, int duration) {
        if (!tutor) {
            hireTutor(true);
        }
        write(UserLocal.tutorMsg(id, duration));
    }

    /**
     * Shows tutor custom message (you decide which message the tutor will say).
     *
     * @param message  your custom message
     * @param width    size of the message box
     * @param duration message duration
     */
    public void tutorCustomMsg(String message, int width, int duration) {
        if (!tutor) {
            hireTutor(true);
        }
        write(UserLocal.tutorMsg(message, width, duration));
    }

    public int getTransferField() {
        return transferField;
    }

    public void setTransferField(int fieldID) {
        this.transferField = fieldID;
        this.transferFieldReq = fieldID == 0 ? 0 : getField().getId();
    }

    public int getTransferFieldReq() {
        return transferFieldReq;
    }

    public void setMakingSkillLevel(int skillID, int level) {
        Skill skill = getSkill(skillID);
        if (skill != null) {
            skill.setCurrentLevel((level << 24) + getMakingSkillProficiency(skillID));
            addSkill(skill);
            write(WvsContext.changeSkillRecordResult(skill));
        }
    }

    public int getMakingSkillLevel(int skillID) {
        return Math.max((getSkillLevel(skillID) >> 24), 0);
    }

    public void setMakingSkillProficiency(int skillID, int proficiency) {
        Skill skill = getSkill(skillID);
        if (skill != null) {
            skill.setCurrentLevel((getMakingSkillLevel(skillID) << 24) + proficiency);
            addSkill(skill);
            write(WvsContext.changeSkillRecordResult(skill));
        }
    }

    public int getMakingSkillProficiency(int skillID) {
        return Math.max((getSkillLevel(skillID) & 0xFFFFFF), 0);
    }

    public void addMakingSkillProficiency(int skillID, int amount) {
        int makingSkillID = SkillConstants.recipeCodeToMakingSkillCode(skillID);
        int level = getMakingSkillLevel(makingSkillID);

        int neededExp = SkillConstants.getNeededProficiency(level);
        if (neededExp <= 0) {
            return;
        }
        int exp = getMakingSkillProficiency(makingSkillID);
        if (exp >= neededExp) {
            write(UserLocal.chatMsg(GameDesc, "熟練度已達到上限，請先提升專業技能等級。"));
            write(UserLocal.chatMsg(GameDesc, "請前往匠人街尋找對應 NPC 進行升級。"));
            setMakingSkillProficiency(makingSkillID, neededExp);
            return;
        }
        int newExp = exp + amount;
        write(UserLocal.chatMsg(GameDesc, SkillConstants.getMakingSkillName(makingSkillID) + " 熟練度增加。（+" + amount + "）"));
        if (newExp >= neededExp) {
            chatPopup("你已積累足夠的 " + SkillConstants.getMakingSkillName(makingSkillID) + " 熟練度。請前往城鎮尋找 NPC 升級。");
            setMakingSkillProficiency(makingSkillID, neededExp);
        } else {
            setMakingSkillProficiency(makingSkillID, newExp);
        }
    }

    public void makingSkillLevelUp(int skillID) {
        int level = getMakingSkillLevel(skillID);
        int neededExp = SkillConstants.getNeededProficiency(level);
        if (neededExp <= 0) {
            return;
        }
        int exp = getMakingSkillProficiency(skillID);
        if (exp >= neededExp) {
            setMakingSkillProficiency(skillID, 0);
            setMakingSkillLevel(skillID, level + 1);
            Stat trait = switch (skillID) {
                case 92000000 -> Stat.senseEXP;
                case 92010000 -> Stat.willEXP;
                default -> Stat.craftEXP;
            };
            addTraitExp(trait, (int) Math.pow(2, (level + 1) + 2));
            write(FieldPacket.fieldEffect(FieldEffect.playSound("profession/levelup", 100)));
        }
    }

    public void addNx(int nx) {
        getUser().addMaplePoints(nx);
        String msg = "你獲得了 " + String.format("%,d", nx) + " 點抵用券！";
        chatScriptMessage(msg);
        write(WvsContext.setMaplePoint(getUser().getMaplePoints()));
    }

    public void initBlessingSkillNames() {
        Account account = getAccount();
        Char fairyChar = null;
        for (Char chr : account.getCharacters()) {
            if (chr.getId() != getId()
                    && chr.getLevel() >= 10
                    && (fairyChar == null || chr.getLevel() > fairyChar.getLevel())) {
                fairyChar = chr;
            }
        }
        if (fairyChar != null) {
            setBlessingOfFairy(fairyChar.getName());
        }
        Char empressChar = null;
        for (Char chr : account.getCharacters()) {
            if (chr.getId() != getId()
                    && (JobConstants.isCygnusKnight(chr.getJob()) || JobConstants.isMihile(chr.getJob()))
                    && chr.getLevel() >= 5
                    && (empressChar == null || chr.getLevel() > empressChar.getLevel())) {
                empressChar = chr;
            }
        }
        if (empressChar != null) {
            setBlessingOfEmpress(empressChar.getName());
        }
    }

    public void initBlessingSkills() {
        Char fairyChar = getCharDataByName(getBlessingOfFairy());
        if (fairyChar != null) {
            if (hasSkill(SkillConstants.getFairyBlessingByJob(getJob()))) {
                removeSkill(SkillConstants.getFairyBlessingByJob(getJob()));
            }
            addSkill(SkillConstants.getFairyBlessingByJob(getJob()), Math.min(20, fairyChar.getLevel() / 10), 20);
        }
        Char empressChar = getCharDataByName(getBlessingOfEmpress());
        if (empressChar != null) {
            if (hasSkill(SkillConstants.getEmpressBlessingByJob(getJob()))) {
                removeSkill(SkillConstants.getEmpressBlessingByJob(getJob()));
            }
            addSkill(SkillConstants.getEmpressBlessingByJob(getJob()), Math.min(30, empressChar.getLevel() / 5), 30);
        }
    }

    public Map<Integer, Integer> getHyperPsdSkillsCooltimeR() {
        return hyperPsdSkillsCooltimeR;
    }

    public void setHyperPsdSkillsCooltimeR(Map<Integer, Integer> hyperPsdSkillsCooltimeR) {
        this.hyperPsdSkillsCooltimeR = hyperPsdSkillsCooltimeR;
    }

    public boolean isInvincible() {
        return isInvincible;
    }

    public void setInvincible(boolean invincible) {
        isInvincible = invincible;
    }

    public List<Integer> getQuickslotKeys() {
        return quickslotKeys;
    }

    public void setQuickslotKeys(List<Integer> quickslotKeys) {
        this.quickslotKeys = quickslotKeys;
    }

    public String getQuickSlotKeysForUpdate() {
        StringBuilder value = new StringBuilder();
        if (getQuickslotKeys() == null || getQuickslotKeys().isEmpty()) {
            return "NULL";
        }
        for (int i = 0; i < getQuickslotKeys().size(); i++) {
            if (i == getQuickslotKeys().size() - 1) {
                value.append(getQuickslotKeys().get(i));
            } else {
                value.append(getQuickslotKeys().get(i)).append(",");
            }
        }
        return value.toString();
    }

    public Dragon getDragon() {
        Dragon dragon = null;
        if (getJobHandler() instanceof Evan evan) {
            dragon = evan.getDragon();
        }
        return dragon;
    }

    /**
     * Checks if this Char has a skill with at least a given level.
     *
     * @param skillID the skill to get
     * @param slv     the minimum skill level
     * @return whether or not this Char has the skill with the given skill level
     */
    public boolean hasSkillWithSlv(int skillID, short slv) {
        Skill skill = getSkill(skillID);
        return skill != null && skill.getCurrentLevel() >= slv;
    }

    public World getWorld() {
        return getClient().getWorld();
    }

    public Android getAndroid() {
        return android;
    }

    public void setAndroid(Android android) {
        this.android = android;
    }

    public FoxMan getFoxMan() {
        return foxman;
    }

    public void setFoxMan(FoxMan foxman) {
        this.foxman = foxman;
    }

    public SkillPet getSkillPet() {
        return skillpet;
    }

    public void setSkillPet(SkillPet skillpet) {
        this.skillpet = skillpet;
    }

    /**
     * Initializes this Char's Android according to their heart + android equips. Will not do anything if an Android
     * already exists.
     *
     * @param override Whether or not to override the old Android if one exists.
     */
    public void initAndroid(boolean override) {
        repairTms282AndroidSlots();
        if (getAndroid() == null || override) {
            Item heart = getEquippedItemByBodyPart(BodyPart.MechanicalHeart);
            Item android = getEquippedItemByBodyPart(BodyPart.Android);
            if (heart == null && android != null) {
                write(WvsContext.androidMachineHeartAlsetMessage());
                return;
            }
            if (android != null && ((Equip) android).getAndroidGrade() + 3 >= ((Equip) heart).getAndroidGrade()) {
                int androidId = ((Equip) android).getAndroid();
                AndroidInfo androidInfo = EtcData.getAndroidInfoById(androidId);
                if (androidInfo == null) {
                    // an android type no data layer has: keep the equip change, show no android (an exception here left the
                    // 282 client's inventory out of step - live 2026-09-28, item 1662271 type 318 before its 282 export)
                    System.out.printf("[Android] no data for android type %d of item %d, character %d: android not shown%n",
                            androidId, android.getItemId(), getId());
                    return;
                }
                if (getAndroid() != null) {
                    getField().removeLife(getAndroid());
                }
                Android newAndroid = new Android(this, androidInfo);
                if (getPosition() != null) {
                    newAndroid.setPosition(getPosition().deepCopy());
                }
                ((Equip) android).setAndroidLife(newAndroid);
                setAndroid(newAndroid);
            }
        }
    }


    /** TMS 282: before the slot mapping was corrected, a worn android / heart was stored under the raw 282 slot
     * (32 / 33) instead of 265 BodyPart.Android / MechanicalHeart (53 / 54). Move such items back so the android is
     * found; only 166xxxx / 167xxxx items move, and only into an empty target slot. */
    void repairTms282AndroidSlots() {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3 || getEquippedInventory() == null) {
            return;
        }
        int[][] moves = {{net.swordie.ms.connection.packet.Tms282BodySlots.ANDROID_282, BodyPart.Android.getVal(), 166},
                {net.swordie.ms.connection.packet.Tms282BodySlots.HEART_282, BodyPart.MechanicalHeart.getVal(), 167}};
        for (int[] m : moves) {
            Item item = getEquippedInventory().getItemBySlot(m[0]);
            if (item != null && item.getItemId() / 10000 == m[2] && getEquippedInventory().getItemBySlot(m[1]) == null) {
                item.setBagIndex(m[1]);
                System.out.printf("[TMS282] android slot repair character=%d item=%d %d -> %d%n", getId(), item.getItemId(), m[0], m[1]);
            }
        }
    }

    public void initKanna() {
        if (JobConstants.isKanna(getJob())) {
            if (getFoxMan() == null) setFoxMan(new FoxMan(this, false));
            if (getSkillPet() == null) setSkillPet(new SkillPet(this));
        }
    }

    public void useStatChangeItem(Item item, boolean consume) {
        TemporaryStatManager tsm = getTemporaryStatManager();
        int itemID = item.getItemId();
        ItemInfo itemInfo = ItemData.getItemInfoByID(itemID);
        if (itemInfo == null) {
            System.out.printf("Item Info %d%n is null.", itemID);
            chatMessage("無法使用該物品。");
            return;
        }
        Map<SpecStat, Integer> specStats = itemInfo.getSpecStats();
        if (specStats.size() > 0) {
            ItemBuffs.giveItemBuffsFromItemID(this, tsm, itemID);
        } else {
            switch (itemID) {
                case 2050000: // Cures the state of being poisoned.
                    tsm.removeStat(Poison);
                    break;
                case 2050001: // Cures the state of darkness
                    tsm.removeStat(Darkness);
                    break;
                case 2050002: // Cures the state of weakness.
                    tsm.removeStat(Weakness);
                    break;
                case 2050003: // Allows you to recover from the state of curse or being sealed up.
                    if (tsm.hasStat(Curse)) {
                        tsm.removeStat(Curse);
                    }
                    if (tsm.hasStat(Seal)) {
                        tsm.removeStat(Seal);
                    }
                    break;
                case 2001556:
                case 2050004:
                case 2050008: // Allows you to recover from any abnormal state.
                    tsm.removeAllDebuffs();
                    break;
                case 2350011:
                    if (getParty() != null) {
                        for (Char chr : getParty().getPartyMembersInSameField(this)) {
                            if (chr != null) {
                                chr.getTemporaryStatManager().removeAllDebuffs();
                            }
                        }
                    } else {
                        tsm.removeAllDebuffs();
                    }
                    break;
                case 2050007: // Recovers all Abnormal Status effects. Only usable when fighting Crumbling Zakum (Easy Mode).
                    if (getField().getId() == 280030200) {
                        tsm.removeAllDebuffs();
                    }
                    break;
                default:
                    System.out.printf("Unhandled stat change item %d%n", itemID);
                    break;
            }
            write(UserPacket.effect(Effect.buffItemEffect(itemID)));
            getField().broadcast(UserRemote.effect(getId(), Effect.buffItemEffect(itemID)), this);
            write(WvsContext.giveBuffMessage(itemID));
        }
        if (consume) {
            if (itemID != PERMANENT_POWER_ELIXIR_ITEM_ID) {
                consumeItem(itemID, 1);
            }
            AchievementHandler.handlePowerElixir(this, itemID);
        }
        dispose();
    }

    public int getSpentActiveHyperSkillSp() {
        int sp = 0;
        for (Skill skill : getSkills()) {
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            if (si == null) {
                continue;
            }
            if (si.getHyper() == 2) {
                sp += skill.getCurrentLevel();
            }
        }
        return sp;
    }

    public int getSpentPassiveHyperSkillSp() {
        int sp = 0;
        for (Skill skill : getSkills()) {
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            if (si == null) {
                continue;
            }
            if (si.getHyper() == 1) {
                sp += skill.getCurrentLevel();
            }
        }
        return sp;
    }

    public void addPsychicArea(PsychicArea pa) {
        psychicAreas.put(pa.localPsychicAreaKey, pa);
    }

    public void removePsychicArea(int psychicAreaKey) {
        this.psychicAreas.entrySet().removeIf(x -> x.getKey() == psychicAreaKey);
    }

    public PsychicArea getPsychicArea(int psychicAreaKey) {
        return psychicAreas.getOrDefault(psychicAreaKey, null);
    }

    public void addPsychicLock(PsychicLock pl) {
        psychicLocks.put(pl.key, pl);
    }

    public void removePsychicLock(int key) {
        this.psychicLocks.entrySet().removeIf(x -> x.getKey() == key);
    }

    public Map<Long, Integer> getItemBoughtAmounts() {
        return itemBoughtAmounts;
    }

    public void setItemBoughtAmounts(Map<Long, Integer> itemBoughtAmounts) {
        this.itemBoughtAmounts = itemBoughtAmounts;
    }

    public void addItemBoughtAmount(long itemId, int amount) {
        getItemBoughtAmounts().put(itemId, amount);
    }

    public void increaseGolluxStack() {
        int maxStack = 5;
        TemporaryStatManager tsm = getTemporaryStatManager();
        int stack = tsm.getOption(Stigma).nOption;
        stack++;
        Option o = new Option();
        if (stack >= maxStack) {
            this.damage(getHP());
            stack = maxStack;
        }
        o.nOption = stack;
        o.rOption = 800;
        o.bOption = maxStack;
        tsm.sendStat(Stigma, o);
        // no tOption  as it would probably be permanent (till death)
    }

    public Merchant getVisitingmerchant() {
        return visitingmerchant;
    }

    public void setVisitingmerchant(Merchant visitingmerchant) {
        this.visitingmerchant = visitingmerchant;
    }

    public Merchant getMerchant() {
        if (merchant == null) {
            findMerchant();
        }
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public void findMerchant() {
        ArrayList<Merchant> allmerchants = this.getWorld().getMerchants();
        for (Merchant m : allmerchants) {
            if (m.getOwnerID() == this.getId()) {
                this.setMerchant(m);
                break;
            }
        }
    }

    public void getItemsFromEmployeeTrunk() {
        EmployeeTrunk employeeTrunk = getAccount().getEmployeeTrunk();
        long earnings = employeeTrunk.getMoney();
        if (getMerchant() != null) {
            chatMessage("你仍有一個開啓中的商店，房間：" + getMerchant().getField().getId() % 10 + "，頻道：" + getMerchant().getField().getChannel());
            return;
        }
        if (!canAddMoney(earnings)) {
            chatMessage("你無法繼續領取楓幣。");
            return;
        }
        List<MerchantItem> itemsMoved = new ArrayList<>();
        for (MerchantItem mi : employeeTrunk.getItems()) {
            Item i = mi.item.deepCopy();
            i.setQuantity(mi.bundles * i.getQuantity());
            if (getInventoryByType(i.getInvType()).canPickUp(i)) {
                if (mi.bundles > 0) {    //remove MerchantItem from merchant and database but if bundles <= 0 but don't add to char inv
                    addItemToInventory(i);
                }
                itemsMoved.add(mi);
            }
        }
        employeeTrunk.getItems().removeAll(itemsMoved);
        if (getMerchant() != null) { //merchant can be null after server restart
            merchant.getItems().removeAll(itemsMoved);
        }
        addMoney(earnings);
        employeeTrunk.setMoney(0);
    }

    public Map<ScrollStat, Integer> getStatsBySetEffects() {
        boolean isJokerSetItem = false; //v179 only hat type
        int jokerSetLevel = 0;
        int jokerSetItem = 0;
        Map<ScrollStat, Integer> stats = new EnumMap<>(ScrollStat.class);
        Map<Integer, Integer> setIdToLevel = new HashMap<>();
        for (Item item : getEquippedInventory().getItems()) {
            if (ItemConstants.isJokerSetItem(item.getItemId())) {
                isJokerSetItem = true;
            }
            Equip equip = (Equip) item;
            int setItemId = equip.getSetItemID();
            if (setItemId > 0) {
                int level = setIdToLevel.getOrDefault(setItemId, 0);
                level++;
                setIdToLevel.put(setItemId, level);
                if (jokerSetLevel < equip.getrLevel()) {
                    jokerSetLevel = equip.getrLevel();
                    jokerSetItem = setItemId;
                }
            }
        }
        for (Map.Entry<Integer, Integer> entry : setIdToLevel.entrySet()) {
            int setId = entry.getKey();
            int setLevel = entry.getValue();
            if (isJokerSetItem && setId == jokerSetItem) {
                List<Integer> setItems = EtcData.getItemsInSetItem(setId);
                for (int item : setItems) {
                    if (ItemConstants.isHat(item)) {
                        setLevel++;
                        break;
                    }
                }
            }
            SetEffect setEffect = EtcData.getSetEffectInfoById(setId);
            for (int i = 1; i <= setLevel; i++) {
                if (setEffect.getStatsByLevel(i) == null) {
                    continue;
                }
                for (Object effect : setEffect.getStatsByLevel(i)) {
                    if (effect instanceof Tuple) {
                        ScrollStat ss = (ScrollStat) (((Tuple) effect).getLeft());
                        int amount = (int) (((Tuple) effect).getRight());
                        if (stats.containsKey(ss)) {
                            stats.replace(ss, stats.get(ss) + amount);
                        } else {
                            stats.put(ss, amount);
                        }
                    }
                }
            }
        }
        return stats;
    }

    public List<ItemOption> getItemOptionsBySetEffects() {
        List<ItemOption> options = new ArrayList<>();
        Map<Integer, Integer> setIdToLevel = new HashMap<>();
        for (Item item : getEquippedInventory().getItems()) {
            Equip equip = (Equip) item;
            int setItemId = equip.getSetItemID();
            if (setItemId > 0) {
                int level = setIdToLevel.getOrDefault(setItemId, 0);
                level++;
                setIdToLevel.put(setItemId, level);
            }
        }
        for (Map.Entry<Integer, Integer> entry : setIdToLevel.entrySet()) {
            int setId = entry.getKey();
            int setLevel = entry.getValue();
            SetEffect setEffect = EtcData.getSetEffectInfoById(setId);
            if (setEffect == null) {
                continue;
            }
            for (int i = 1; i <= setLevel; i++) {
                if (setEffect.getStatsByLevel(i) == null) {
                    continue;
                }
                for (Object effect : setEffect.getStatsByLevel(i)) {
                    if (effect instanceof ItemOption) {
                        options.add((ItemOption) effect);
                    }
                }
            }
        }
        return options;
    }

    public int getStatAmountSetEffect(BaseStat baseStat) {
        int amount = 0;
        Map<ScrollStat, Integer> stats = getStatsBySetEffects();
        for (Map.Entry<ScrollStat, Integer> entry : stats.entrySet()) {
            if (entry.getKey().getBaseStat() == baseStat) {
                amount += entry.getValue();
            }
        }
        List<ItemOption> options = getItemOptionsBySetEffects();
        for (ItemOption option : options) {
            int id = option.getId();
            int level = option.getReqLevel();
            ItemOption io = ItemData.getItemOptionById(id);
            if (io != null) {
                Map<BaseStat, Double> valMap = io.getStatValuesByLevel(level);
                amount += valMap.getOrDefault(baseStat, 0D);
            }
        }

        return amount;
    }

    public void addMaplePoint(int maplePoint) {
        getUser().addMaplePoints(maplePoint);
        write(WvsContext.setMaplePoint(getUser().getMaplePoints()));
    }

    public Set<PartyBoss> getPartyboss() {
        return partyboss;
    }

    public void setPartyboss(Set<PartyBoss> partyboss) {
        this.partyboss = partyboss;
    }

    public boolean hasBossPartyAttempt(BossPartyType bpt) {
        if (bpt == null) return true;
        BossPartyEnterCountType enterCount = bpt.getEnterCount();
        VIPGrade vipGrade = VIPGrade.getValByNum(getUser().getVipGrade());
        int count;
        if (enterCount == null) return true;
        if (getPartyboss().stream().filter(i -> i.getOrderId() == bpt.getOrderId() && i.getDifficulty() == bpt.getDifficulty().getVal()).findAny().orElse(null) == null) {
            return false;
        } else {
            final LocalDateTime ct = LocalDateTime.now();
            for (PartyBoss pb : getPartyboss()) {
                if (pb.getCharId() == getId()) {
                    LocalDateTime lastTime = pb.getLastAttemptTime().toLocalDateTime();
                    LocalDateTime lt = LocalDateTime.of(lastTime.getYear(), lastTime.getMonth().getValue(), lastTime.getDayOfMonth(), 0, 0, 0);
                    switch (enterCount) {
                        case OnceADay:
                            // MVP Specials:
                            count = 1;
                            if (vipGrade.getVal() == VIPGrade.Gold.getVal()) {
                                count += 1;
                            } else if (vipGrade.getVal() == VIPGrade.Diamond.getVal()) {
                                count += 2;
                            }
                            if (pb.getOrderId() == bpt.getOrderId()
                                    && pb.getDifficulty() == bpt.getDifficulty().getVal()
                                    && pb.getAttempt() == count
                                    && lt.plusDays(1).isAfter(ct)) {
                                return true;
                            }
                            break;
                        case OnceInSevendays:
                            // MVP Specials:
                            count = 1;
                            if (vipGrade.getVal() == VIPGrade.Gold.getVal()) {
                                count += 1;
                            } else if (vipGrade.getVal() == VIPGrade.Diamond.getVal()) {
                                count += 2;
                            }
                            if (pb.getOrderId() == bpt.getOrderId()
                                    && pb.getDifficulty() == bpt.getDifficulty().getVal()
                                    && pb.getAttempt() == count
                                    && lt.plusDays(7).isAfter(ct)) {
                                return true;
                            }
                            break;
                        case TwiceAday:
                            // MVP Specials:
                            count = 2;
                            if (vipGrade.getVal() == VIPGrade.Gold.getVal()) {
                                count += 1;
                            } else if (vipGrade.getVal() == VIPGrade.Diamond.getVal()) {
                                count += 2;
                            }
                            if (pb.getOrderId() == bpt.getOrderId()
                                    && pb.getDifficulty() == bpt.getDifficulty().getVal()
                                    && pb.getAttempt() == count
                                    && lt.plusDays(1).isAfter(ct)) {
                                return true;
                            }
                            break;
                        case TwiceInSevendays:
                            // MVP Specials:
                            count = 2;
                            if (vipGrade.getVal() == VIPGrade.Gold.getVal()) {
                                count += 1;
                            } else if (vipGrade.getVal() == VIPGrade.Diamond.getVal()) {
                                count += 2;
                            }
                            if (pb.getOrderId() == bpt.getOrderId()
                                    && pb.getDifficulty() == bpt.getDifficulty().getVal()
                                    && pb.getAttempt() == count
                                    && lt.plusDays(7).isAfter(ct)) {
                                return true;
                            }
                            break;
                        case ThreeTimesAday:
                            // MVP Specials:
                            count = 3;
                            if (vipGrade.getVal() == VIPGrade.Gold.getVal()) {
                                count += 1;
                            } else if (vipGrade.getVal() == VIPGrade.Diamond.getVal()) {
                                count += 2;
                            }
                            if (pb.getOrderId() == bpt.getOrderId()
                                    && pb.getDifficulty() == bpt.getDifficulty().getVal()
                                    && pb.getAttempt() == count
                                    && lt.plusDays(1).isAfter(ct)) {
                                return true;
                            }
                            break;
                        case SevenTimesADay:
                            // MVP Specials:
                            count = 7;
                            if (vipGrade.getVal() == VIPGrade.Gold.getVal()) {
                                count += 1;
                            } else if (vipGrade.getVal() == VIPGrade.Diamond.getVal()) {
                                count += 2;
                            }
                            if (pb.getOrderId() == bpt.getOrderId()
                                    && pb.getDifficulty() == bpt.getDifficulty().getVal()
                                    && pb.getAttempt() == count
                                    && lt.plusDays(1).isAfter(ct)) {
                                return true;
                            }
                            break;
                        case TenTimesADay:
                            // MVP Specials:
                            count = 10;
                            if (vipGrade.getVal() == VIPGrade.Gold.getVal()) {
                                count += 1;
                            } else if (vipGrade.getVal() == VIPGrade.Diamond.getVal()) {
                                count += 2;
                            }
                            if (pb.getOrderId() == bpt.getOrderId()
                                    && pb.getDifficulty() == bpt.getDifficulty().getVal()
                                    && pb.getAttempt() == count
                                    && lt.plusDays(1).isAfter(ct)) {
                                return true;
                            }
                            break;
                        case OnceInTwodays:
                            // MVP Specials:
                            count = 1;
                            if (vipGrade.getVal() == VIPGrade.Gold.getVal()) {
                                count += 1;
                            } else if (vipGrade.getVal() == VIPGrade.Diamond.getVal()) {
                                count += 2;
                            }
                            if (pb.getOrderId() == bpt.getOrderId()
                                    && pb.getDifficulty() == bpt.getDifficulty().getVal()
                                    && pb.getAttempt() == count
                                    && lt.plusDays(2).isAfter(ct)) {
                                return true;
                            }
                            break;
                        case OnceInThreedays:
                            // MVP Specials:
                            count = 1;
                            if (vipGrade.getVal() == VIPGrade.Gold.getVal()) {
                                count += 1;
                            } else if (vipGrade.getVal() == VIPGrade.Diamond.getVal()) {
                                count += 2;
                            }
                            if (pb.getOrderId() == bpt.getOrderId()
                                    && pb.getDifficulty() == bpt.getDifficulty().getVal()
                                    && pb.getAttempt() == count
                                    && lt.plusDays(3).isAfter(ct)) {
                                return true;
                            }
                            break;
                        case OnceInAMonth:
                            if (pb.getOrderId() == bpt.getOrderId()
                                    && pb.getDifficulty() == bpt.getDifficulty().getVal()
                                    && pb.getAttempt() == 1
                                    && lt.plusMonths(1).isAfter(ct)) {
                                return true;
                            }
                            break;
                    }
                }
            }
        }
        return false;
    }

    public void addPartyboss(BossPartyType bpt) {
        if (bpt == null) return;
        LocalDateTime now = LocalDateTime.now();
        BossPartyEnterCountType enterCount = bpt.getEnterCount();
        if (enterCount == null) return;
        PartyBoss partyBoss = getPartyboss().stream().filter(i ->
                i.getCharId() == getId()
                        && i.getOrderId() == bpt.getOrderId()
                        && i.getDifficulty() == bpt.getDifficulty().getVal()).findFirst().orElse(null);
        if (partyBoss == null) {
            PartyBoss partyBossNew = new PartyBoss(getId(), bpt.getOrderId(), bpt.getBossName(), bpt.getDifficulty().getVal(), 1, FileTime.currentTime());
            partyBossNew.saveToSQL();
            getPartyboss().add(partyBossNew);
        } else {
            switch (enterCount) {
                case OnceADay, OnceInTwodays, OnceInThreedays, OnceInSevendays -> {
                    partyBoss.setAttempt(1);
                    partyBoss.setLastAttemptTime(FileTime.currentTime());
                    partyBoss.saveToSQL();
                }
                case TwiceAday -> {
                    if (partyBoss.getLastAttemptTime().toLocalDateTime().getDayOfYear() != now.getDayOfYear()) {
                        partyBoss.setAttempt(1);
                        partyBoss.setLastAttemptTime(FileTime.currentTime());
                        partyBoss.saveToSQL();
                    } else {
                        if (partyBoss.getAttempt() == 2) {
                            partyBoss.setAttempt(1);
                            partyBoss.setLastAttemptTime(FileTime.currentTime());
                            partyBoss.saveToSQL();
                        } else if (partyBoss.getAttempt() == 1) {
                            partyBoss.setAttempt(2);
                            partyBoss.setLastAttemptTime(FileTime.currentTime());
                            partyBoss.saveToSQL();
                        }
                    }
                }
                case ThreeTimesAday -> {
                    if (partyBoss.getLastAttemptTime().toLocalDateTime().getDayOfYear() != now.getDayOfYear()) {
                        partyBoss.setAttempt(1);
                        partyBoss.setLastAttemptTime(FileTime.currentTime());
                        partyBoss.saveToSQL();
                    } else {
                        if (partyBoss.getAttempt() < 3) {
                            partyBoss.addAttempt(1);
                            partyBoss.setLastAttemptTime(FileTime.currentTime());
                            partyBoss.saveToSQL();
                        } else if (partyBoss.getAttempt() == 3) {
                            partyBoss.setAttempt(1);
                            partyBoss.setLastAttemptTime(FileTime.currentTime());
                            partyBoss.saveToSQL();
                        }
                    }
                }
                case SevenTimesADay -> {
                    if (partyBoss.getLastAttemptTime().toLocalDateTime().getDayOfYear() != now.getDayOfYear()) {
                        partyBoss.setAttempt(1);
                        partyBoss.setLastAttemptTime(FileTime.currentTime());
                        partyBoss.saveToSQL();
                    } else {
                        if (partyBoss.getAttempt() < 7) {
                            partyBoss.addAttempt(1);
                            partyBoss.setLastAttemptTime(FileTime.currentTime());
                            partyBoss.saveToSQL();
                        } else if (partyBoss.getAttempt() == 7) {
                            partyBoss.setAttempt(1);
                            partyBoss.setLastAttemptTime(FileTime.currentTime());
                            partyBoss.saveToSQL();
                        }
                    }
                }
                case TenTimesADay -> {
                    if (partyBoss.getLastAttemptTime().toLocalDateTime().getDayOfYear() != now.getDayOfYear()) {
                        partyBoss.setAttempt(1);
                        partyBoss.setLastAttemptTime(FileTime.currentTime());
                        partyBoss.saveToSQL();
                    } else {
                        if (partyBoss.getAttempt() < 10) {
                            partyBoss.addAttempt(1);
                            partyBoss.setLastAttemptTime(FileTime.currentTime());
                            partyBoss.saveToSQL();
                        } else if (partyBoss.getAttempt() == 10) {
                            partyBoss.setAttempt(1);
                            partyBoss.setLastAttemptTime(FileTime.currentTime());
                            partyBoss.saveToSQL();
                        }
                    }
                }
                default ->
                        System.out.println("Unable to find enter count type: " + enterCount + " on character name: " + getName());
            }
        }
    }

    public Set<MatrixCore> getMatrixCore() {
        return matrixCore;
    }

    public void setMatrixCore(Set<MatrixCore> matrixCore) {
        this.matrixCore = matrixCore;
    }

    public void addMatrixCore(MatrixCore matrixSkill) {
        this.matrixCore.add(matrixSkill);
    }

    public Set<MatrixCore> getInactiveMatrixCore() {
        return matrixCore.stream().filter(c -> c.getState() == MatrixStateType.INACTIVE && c.getSlot() == -1).collect(Collectors.toSet());
    }

    public List<MatrixCore> getSortedMatrixCores() {
        List<MatrixCore> cores = new ArrayList<>(500);
        List<MatrixCore> currents = getMatrixCore().stream().sorted(Comparator.comparingLong(MatrixCore::getId)).toList();
        for (var core : currents) {
            if (cores.size() >= 500) {
                break;
            }
            cores.add(core);
        }
        return cores;
    }

    public MatrixCore getMatrixCoreByPosition(int slot) {
        return getSortedMatrixCores().stream().filter(n -> n.getSlot() == slot).findFirst().orElse(null);
    }

    public Set<MatrixSlot> getMatrixSlot() {
        return matrixSlot;
    }

    public int getFirstOpenMatrixSlot() {
        for (int i = 0; i < getMaxMatrixSlots(); i++) {
            MatrixCore mc = getMatrixCoreByPosition(i);
            if (mc == null) {
                return i;
            }
        }
        return -1337;
    }

    public int getTotalMatrixSlotsEnhancements() {
        return getMatrixSlot().stream().mapToInt(MatrixSlot::getLevel).sum();
    }

    public int getMatrixPoints() {
        return getLevel() - 200 + 2;
    }

    public int getAvailableMatrixPoints() {
        return getMatrixPoints() - getTotalMatrixSlotsEnhancements();
    }

    public int getMaxMatrixSlots() {
        int max = 0;
        int lv = getLevel();

        for (int pos = 0; pos < MatrixConstants.MAX_NODE_SLOTS; pos++) {
            MatrixSlot ms = getMatrixSlotByPosition(pos);
            if ((ms != null && ms.isUnLock()) || lv >= MatrixConstants.REQ_LV_BY_MATRIX_SLOT_POS[pos]) {
                max++;
            }
        }
        return max;
    }

    public MatrixSlot getMatrixSlotByPosition(int pos) {
        return getMatrixSlot().stream().filter(ms -> ms.getPosition() == pos).findFirst().orElse(null);
    }

    public void setMatrixSlot(Set<MatrixSlot> matrixSlot) {
        this.matrixSlot = matrixSlot;
    }

    public void addMatrixSlot(MatrixSlot matrixSlot) {
        this.matrixSlot.add(matrixSlot);
    }

    public Map<Integer, HexaStat> getHexaStats() {
        return hexaStats;
    }

    public void setHexaStats(Map<Integer, HexaStat> hexaStats) {
        this.hexaStats = hexaStats;
    }

    public HexaStat getHexaStatByCoreID(int coreID) {
        final Map<Integer, HexaStat> hexaStats = getHexaStats();
        for (Map.Entry<Integer, HexaStat> entry : hexaStats.entrySet()) {
            if (entry.getValue().getCoreId() == coreID) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void encodeHexaStats(OutPacket outPacket) {
        final Map<Integer, HexaStat> hexaStats = getHexaStats();
        int size = hexaStats.size();
        outPacket.encodeInt(size);
        for (Map.Entry<Integer, HexaStat> entry : hexaStats.entrySet()) {
            HexaStat hexaStat = entry.getValue();
            if (hexaStat == null) {
                continue;
            }
            Tuple<HexaCore.HexaStatType, Integer> info0 = hexaStat.getStats().get(0);
            int stat0 = info0 == null ? -1 : info0.getLeft().ordinal();
            int level0 = info0 == null ? 0 : info0.getRight();
            Tuple<HexaCore.HexaStatType, Integer> info1 = hexaStat.getStats().get(1);
            int stat1 = info1 == null ? -1 : info1.getLeft().ordinal();
            int level1 = info1 == null ? 0 : info1.getRight();
            Tuple<HexaCore.HexaStatType, Integer> info2 = hexaStat.getStats().get(2);
            int stat2 = info2 == null ? -1 : info2.getLeft().ordinal();
            int level2 = info2 == null ? 0 : info2.getRight();

            outPacket.encodeInt(hexaStat.getCoreId());
            // sub_140666270
            outPacket.encodeInt(hexaStat.getCoreId());
            outPacket.encodeInt(0);
            outPacket.encodeInt(1); // enable
            outPacket.encodeInt(stat0);
            outPacket.encodeInt(level0);
            outPacket.encodeInt(stat1);
            outPacket.encodeInt(level1);
            outPacket.encodeInt(stat2);
            outPacket.encodeInt(level2);
            // SAVE DATA
            outPacket.encodeInt(hexaStat.getCoreId());
            outPacket.encodeInt(1);
            outPacket.encodeInt(0);
            outPacket.encodeInt(-1);
            outPacket.encodeInt(0);
            outPacket.encodeInt(-1);
            outPacket.encodeInt(0);
            outPacket.encodeInt(-1);
            outPacket.encodeInt(0);
        }
        outPacket.encodeInt(size);
        for (Map.Entry<Integer, HexaStat> entry : hexaStats.entrySet()) {
            HexaStat hexaStat = entry.getValue();
            if (hexaStat == null) {
                continue;
            }
            outPacket.encodeInt(hexaStat.getCoreId());
            // sub_140666350
            outPacket.encodeInt(hexaStat.getCoreId());
            outPacket.encodeInt(0);
        }
        outPacket.encodeInt(0); // size
        outPacket.encodeInt(0); // size
    }

    public Set<HexaSkill> getHexaSkills() {
        return hexaSkills;
    }

    public void setHexaSkills(Set<HexaSkill> hexaSkills) {
        this.hexaSkills = hexaSkills;
    }

    public int getSolErda() {
        if (!hasQuest(HexaMatrixConstants.solErda)) {
            createQuestWithQRValue(HexaMatrixConstants.solErda, "0=0;0exp=0");
            return 0;
        }
        return Integer.parseInt(getQRValueByKey(HexaMatrixConstants.solErda, "0"));
    }

    public int getSolErdaStrength() {
        if (!hasQuest(HexaMatrixConstants.solErda)) {
            createQuestWithQRValue(HexaMatrixConstants.solErda, "0=0;0exp=0");
            return 0;
        }
        return Integer.parseInt(getQRValueByKey(HexaMatrixConstants.solErda, "0exp"));
    }

    public void addSolErda(int inc) {
        int curSolErda = getSolErda();
        int newSolErda = Math.max(0, curSolErda + inc);
        setQRValueByKey(HexaMatrixConstants.solErda, 0, newSolErda);
    }

    public void addSolErdaStrength(int inc) {
        if (inc <= 0) return;

        final int max = HexaMatrixConstants.solErdaStrengthMax;
        int cur = getSolErdaStrength();
        if (cur < 0 || cur >= max) {
            cur = Math.floorMod(cur, max);
        }
        long total = (long) cur + (long) inc;
        int carry = (int) (total / max);
        int rem   = (int) (total % max);
        setQRValueByKey(HexaMatrixConstants.solErda, "0exp", Integer.toString(rem));
        if (carry > 0) addSolErda(carry);
        if (carry == 0) {
            getScriptManager().progressMessageFont(3, 24, 9, 0,
                    String.format("你獲得了 %d 點靈魂艾爾達的氣息", inc));
        } else {
            getScriptManager().progressMessageFont(3, 24, 9, 0,
                    String.format("你獲得了 %d 點靈魂艾爾達的氣息，並轉化為 %d 個靈魂艾爾達", inc, carry));
        }
    }

    public void setHexaSkill(int coreID, int level) {
        boolean hasCore = false;
        HexaCore.HexaSkillCoreData coreData = HexaCore.getSkillCoreData(coreID);
        if (coreData == null) {
            return;
        }
        List<Integer> coreSkillList = new ArrayList<>(coreData.getConnectSkills());
        SixthJobSkillConstants.OriginAscentEntry originEntry = SixthJobSkillConstants.getByOriginCore(coreID);
        if (originEntry != null && !coreSkillList.contains(originEntry.originSkillId())) {
            coreSkillList.add(originEntry.originSkillId());
        }
        if (coreSkillList.isEmpty()) {
            return;
        }
        for (HexaSkill hexaSkill : getHexaSkills()) {
            if (hexaSkill.getSkillId() == coreID) {
                hexaSkill.setSkillLevel(level);
                hexaSkill.saveToSQL();
                hasCore = true;
                break;
            }
        }
        if (!hasCore) {
            HexaSkill hexaSkill = new HexaSkill(coreID, level);
            hexaSkill.setCharId(getId());
            getHexaSkills().add(hexaSkill);
            hexaSkill.saveToSQL();
        }
        int maxLevel = coreData.getMaxLevel();
        int resolvedMaxLevel = maxLevel > 0 ? maxLevel : 30;
        SixthJobSkillConstants.grantConnectSkills(this, coreSkillList, level, resolvedMaxLevel);
        SixthJobSkillConstants.grantAscentSkillsForConnectSkills(this, coreSkillList, level, resolvedMaxLevel);
        if (SixthJobSkillConstants.isOriginCore(coreID)) {
            SixthJobSkillConstants.sendAscentSkillStackUpdate(this);
        }
    }

    public int getHexaSkillLevel(int coreID) {
        for (HexaSkill hexaSkill : getHexaSkills()) {
            if (hexaSkill.getSkillId() == coreID) {
                return hexaSkill.getSkillLevel();
            }
        }
        return 0;
    }

    /**
     * Removes all hexa cores/skills for this character and returns total core levels cleared.
     */
    public int clearHexaSkills() {
        int totalLevels = 0;
        for (HexaSkill hexaSkill : new HashSet<>(getHexaSkills())) {
            totalLevels += hexaSkill.getSkillLevel();
            HexaCore.HexaSkillCoreData coreData = HexaCore.getSkillCoreData(hexaSkill.getSkillId());
            if (coreData != null) {
                for (Integer skillID : coreData.getConnectSkills()) {
                    removeSkillAndSendPacket(skillID);
                }
                for (Integer skillID : coreData.getConnectSkills()) {
                    int ascentSkillId = SixthJobSkillConstants.getAscentSkillId(skillID);
                    if (ascentSkillId > 0) {
                        removeSkillAndSendPacket(ascentSkillId);
                    }
                }
            }
            if (hexaSkill.getId() > 0) {
                DatabaseManager.executeStatement("DELETE FROM hexaskills WHERE id = " + hexaSkill.getId());
            }
            getHexaSkills().remove(hexaSkill);
        }
        write(WvsContext.hexaSkillsUpdate(this));
        return totalLevels;
    }

    public void encodeHexaSkills(OutPacket outPacket) {
        final Collection<HexaSkill> hexaSkills = getHexaSkills();
        outPacket.encodeInt(hexaSkills.size());
        for (HexaSkill hexaSkill : hexaSkills) {
            hexaSkill.encode(outPacket);
        }
        outPacket.encodeByte(0);
        outPacket.encodeInt(0);
    }

    public void encodeUnionArtifacts(OutPacket outPacket) {
        final Map<Integer, UnionArtifact> unionArtifacts = getAccount().getUnionArtifacts();
        outPacket.encodeInt(unionArtifacts.size());
        for (Map.Entry<Integer, UnionArtifact> entry : unionArtifacts.entrySet()) {
            var unionArtifact = entry.getValue();
            outPacket.encodeInt(unionArtifact.getIndex()); // index
            outPacket.encodeInt(unionArtifact.getIndex()); // index
            unionArtifact.encode(outPacket);
        }
    }

    public int getArtifactVal(String key) {
        int qrKey = QuestConstants.UNION_ARTIFACT;
        if (!hasQuest(qrKey)) {
            createQuestWithQRValue(qrKey, "point=0;start=1;level=1;exp=0");
            return 0;
        }
        String val = getQRValueByKey(qrKey, key);
        if (val == null) {
            return 0;
        }
        return Integer.parseInt(val);
    }

    public void incArtifactExp(int addExp) {
        if (addExp <= 0) return;

        int exp = Integer.parseInt(getQRValueByKey(QuestConstants.UNION_ARTIFACT, "exp"));
        int level = Integer.parseInt(getQRValueByKey(QuestConstants.UNION_ARTIFACT, "level"));

        exp += addExp;
        boolean levelUp = false;

        // level QR = 1-based, settings index = 0-based
        while (level < ArtifactData.settings.length) {
            int idx = level - 1;
            if (idx < 0 || idx >= ArtifactData.settings.length) break;

            int expReq = ArtifactData.settings[idx][1];
            if (expReq <= 0) break; // max level
            if (exp < expReq) break;

            exp -= expReq;
            level++;
            levelUp = true;
        }

        // cap max level (max = settings.length)
        if (level >= ArtifactData.settings.length) {
            level = ArtifactData.settings.length;
            exp = 0;
        }

        setQRValueByKey(QuestConstants.UNION_ARTIFACT, "exp", exp);
        setQRValueByKey(QuestConstants.UNION_ARTIFACT, "level", level);

        write(WvsContext.unionArtifactUpdate(5, 0));
        write(WvsContext.unionArtifactMsg(this, addExp, 0, levelUp));
    }

    public void initUnionArtifact() {
        if (!hasQuest(QuestConstants.UNION_ARTIFACT) || getAccount().getUnionArtifacts().isEmpty()) {
            // Log in 1x per week +100 artifactPoint; +100 artifactExp
            var next = FileTime.getNextWeeklyResetTime().weeklyFormat();
            createQuestWithQRValue(QuestConstants.UNION_ARTIFACT, "point=100;start=1;level=1;exp=100");
            createQuestWithQRValue(QuestConstants.UNION_ARTIFACT_DATA, "attendance=1;mobCount=0;resetDate="+next+";resetDate_boss="+next+";attendance_lastDate="+FileTime.currentTime().weeklyFormat());
            createQuestWithQRValue(QuestConstants.UNION_ARTIFACT_COMMON, "missionState=0000200");
            createQuestWithQRValue(QuestConstants.UNION_ARTIFACT_BOSS, "count=0;mobid=0;lasttime="+FileTime.currentTime().toYYYYMMDD_HHMMss());
            createQuestWithQRValue(QuestConstants.UNION_ARTIFACT_SPECIAL_1, "missionState=0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
            createQuestWithQRValue(QuestConstants.UNION_ARTIFACT_SPECIAL_2, "missionState=0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
            createQuestWithQRValue(QuestConstants.UNION_ARTIFACT_SPECIAL_3, "missionState=0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
            createQuestWithQRValue(QuestConstants.UNION_ARTIFACT_HUNT, "missionState=000000000000000000000000000000000000000000000000");
            createQuestWithQRValue(QuestConstants.UNION_ARTIFACT_HUNT_2, "missionState=00");
            createQuestWithQRValue(QuestConstants.UNION_ARTIFACT_SPECIAL, "missionState=00000000");
            var info = ArtifactData.getArtifactInfo();
            var artifacts = getAccount().getUnionArtifacts();
            var expire = FileTime.fromDate(LocalDateTime.now().plusDays(30));
            artifacts.put(0, createArtifact(getAccId(), 0, info, expire));
            addSkill(info.getStatSkillCode(0), 3, 10);
            artifacts.put(1, createArtifact(getAccId(), 1, info, expire));
            addSkill(info.getStatSkillCode(1), 3, 10);
            artifacts.put(2, createArtifact(getAccId(), 2, info, expire));
            addSkill(info.getStatSkillCode(2), 3, 10);
        } else {
            var level = getArtifactVal("level");
            var info = ArtifactData.getArtifactInfo();
            var artifacts = getAccount().getUnionArtifacts();
            var expire = FileTime.fromDate(LocalDateTime.now().plusDays(30));
            var incSLV = 0;
            if (level >= 10 && artifacts.size() < 4) {
                artifacts.put(3, createArtifact(getAccId(), 3, info, expire));
                incSLV += 1;
            }
            if (level >= 20 && artifacts.size() < 5) {
                artifacts.put(4, createArtifact(getAccId(), 4, info, expire));
                incSLV += 1;
            }
            if (level >= 30 && artifacts.size() < 6) {
                artifacts.put(5, createArtifact(getAccId(), 5, info, expire));
                incSLV += 1;
            }
            if (level >= 40 && artifacts.size() < 7) {
                artifacts.put(6, createArtifact(getAccId(), 6, info, expire));
                incSLV += 1;
            }
            if (level >= 50 && artifacts.size() < 8) {
                artifacts.put(7, createArtifact(getAccId(), 7, info, expire));
                incSLV += 1;
            }
            if (level >= 60 && artifacts.size() < 9) {
                artifacts.put(8, createArtifact(getAccId(), 8, info, expire));
                incSLV += 1;
            }
            if (incSLV > 0) {
                var skillID1 = info.getStatSkillCode(0);
                var skill1 = getSkill(skillID1);
                var skillID2 = info.getStatSkillCode(1);
                var skill2 = getSkill(skillID2);
                var skillID3 = info.getStatSkillCode(2);
                var skill3 = getSkill(skillID3);
                if (skill1 == null) {
                    addSkill(skillID1, incSLV, 10);
                } else {
                    skill1.setCurrentLevel(Math.min(skill1.getCurrentLevel() + incSLV, skill1.getMaxLevel()));
                    write(WvsContext.changeSkillRecordResult(skill1));
                }
                if (skill2 == null) {
                    addSkill(skillID2, incSLV, 10);
                } else {
                    skill2.setCurrentLevel(Math.min(skill2.getCurrentLevel() + incSLV, skill2.getMaxLevel()));
                    write(WvsContext.changeSkillRecordResult(skill2));
                }
                if (skill3 == null) {
                    addSkill(skillID3, incSLV, 10);
                } else {
                    skill3.setCurrentLevel(Math.min(skill3.getCurrentLevel() + incSLV, skill3.getMaxLevel()));
                    write(WvsContext.changeSkillRecordResult(skill3));
                }
            }
        }
        write(WvsContext.unionArtifactUpdate(this));
        write(WvsContext.unionArtifactUpdate(7, 0));
    }

    private UnionArtifact createArtifact(int accId, int index, ArtifactInfo info, FileTime expire) {
        UnionArtifact ua = new UnionArtifact();
        ua.setAccId(accId);
        ua.setLevel(1);
        ua.setIndex(index);
        ua.setSkillID1(info.getStatSkillCode(0));
        ua.setSkillID2(info.getStatSkillCode(1));
        ua.setSkillID3(info.getStatSkillCode(2));
        ua.setExpirationTime(expire);
        return ua;
    }

    public void updateArtifactMission(Char chr, ArtifactData.MissionType type, int missionIdx) {
        updateArtifactMission(chr, type.getVal(), missionIdx);
    }

    public void updateArtifactMission(Char chr, int type, int missionIdx) {
        int questId;
        int requiredLen;
        if (type == 0) { // COMMON
            questId = QuestConstants.UNION_ARTIFACT_COMMON;
            requiredLen = 7;
        } else if (type == 1) { // HUNT
            if (missionIdx >= 1000 && missionIdx <= 1001) {
                questId = QuestConstants.UNION_ARTIFACT_HUNT_2;
                requiredLen = 2;
                missionIdx -= 1000;
            } else {
                questId = QuestConstants.UNION_ARTIFACT_HUNT;
                requiredLen = 48;
            }
        } else if (type == 2) { // SPECIAL
            if (missionIdx < 100) {
                questId = QuestConstants.UNION_ARTIFACT_SPECIAL_1;
                requiredLen = 100;
            } else if (missionIdx < 200) {
                questId = QuestConstants.UNION_ARTIFACT_SPECIAL_2;
                requiredLen = 100;
                missionIdx -= 100;
            } else if (missionIdx < 300) {
                questId = QuestConstants.UNION_ARTIFACT_SPECIAL_3;
                requiredLen = 100;
                missionIdx -= 200;
            } else if (missionIdx >= 1000) {
                questId = QuestConstants.UNION_ARTIFACT_SPECIAL;
                requiredLen = 8;
                missionIdx -= 1000;
            } else {
                return;
            }
        } else {
            return;
        }
        String key = "missionState";
        String state = chr.getQRValueByKey(questId, key);
        if (state == null) state = "";
        StringBuilder sb = new StringBuilder(state);
        if (sb.length() < requiredLen) {
            sb.append("0".repeat(requiredLen - sb.length()));
        } else if (sb.length() > requiredLen) {
            sb.setLength(requiredLen);
        }
        if (missionIdx >= 0 && missionIdx < requiredLen) {
            sb.setCharAt(missionIdx, '2');
            chr.setQRValueByKey(questId, key, sb.toString());
        }
    }

    public boolean isArtifactMissionCompleted(Char chr, ArtifactData.MissionType type, int missionIdx) {
        return isArtifactMissionCompleted(chr, type.getVal(), missionIdx);
    }

    public boolean isArtifactMissionCompleted(Char chr, int type, int missionIdx) {
        int questId;
        int requiredLen;
        if (type == 0) { // COMMON
            questId = QuestConstants.UNION_ARTIFACT_COMMON;
            requiredLen = 7;
        } else if (type == 1) { // HUNT
            if (missionIdx >= 1000 && missionIdx <= 1001) {
                questId = QuestConstants.UNION_ARTIFACT_HUNT_2;
                requiredLen = 2;
                missionIdx -= 1000;
            } else {
                questId = QuestConstants.UNION_ARTIFACT_HUNT;
                requiredLen = 48;
            }
        } else if (type == 2) { // SPECIAL
            if (missionIdx < 100) {
                questId = QuestConstants.UNION_ARTIFACT_SPECIAL_1;
                requiredLen = 100;
            } else if (missionIdx < 200) {
                questId = QuestConstants.UNION_ARTIFACT_SPECIAL_2;
                requiredLen = 100;
                missionIdx -= 100;
            } else if (missionIdx < 300) {
                questId = QuestConstants.UNION_ARTIFACT_SPECIAL_3;
                requiredLen = 100;
                missionIdx -= 200;
            } else if (missionIdx >= 1000) {
                questId = QuestConstants.UNION_ARTIFACT_SPECIAL;
                requiredLen = 8;
                missionIdx -= 1000;
            } else {
                return false;
            }
        } else {
            return false;
        }
        String state = chr.getQRValueByKey(questId, "missionState");
        if (state == null || missionIdx < 0 || missionIdx >= requiredLen) {
            return false;
        }
        if (missionIdx >= state.length()) {
            return false;
        }

        char c = state.charAt(missionIdx);
        return c != '0';
    }

    public EventNameTag getEventNameTag() {
        return eventNameTag;
    }

    public void setEventNameTags(EventNameTag eventNameTag) {
        this.eventNameTag = eventNameTag;
    }

    public void addEventNameTag(EventNameTag eventNameTag) {
        this.eventNameTag = eventNameTag;
    }

    /**
     * This is load information about Event Name Tag with Syntax <RED ID, BLUE ID, YELLOW ID, GREEN ID, PURPLE ID>
     * Default: -1 (No name tags). This value default will set when Create new Character
     */
    public void initEventNameTag() {
        if (!isBot()) {
            EventNameTag eventNameTag = EventNameTag.getEventNameTagFromSQLByCharID(getId());
            if (eventNameTag != null) {
                setEventNameTags(eventNameTag);
                write(WvsContext.updateEventNameTag(this, getEventNameTag().getActiveNameTags()));
            }
        }
    }

    public List<Equip> getArcaneSymbols() {
        List<Equip> arcaneSymbols = new LinkedList<>();
        for (Item item : getEquippedInventory().getItems()) {
            if (item.getBagIndex() >= BodyPart.ArcBase.getVal()
                    && item.getBagIndex() < BodyPart.ArcEnd.getVal()) {
                Equip arcaneSymbol = (Equip) item;
                arcaneSymbols.add(arcaneSymbol);
            }
        }
        return arcaneSymbols;
    }

    public List<Equip> getSacredSymbols() {
        List<Equip> authenticSymbols = new LinkedList<>();
        for (Item item : getEquippedInventory().getItems()) {
            if (item.getBagIndex() >= BodyPart.AUSBase.getVal()
                    && item.getBagIndex() < BodyPart.AUSEnd.getVal()
                    // grand authentic symbols 1714xxx in 1750..1752 (282 only; equips README WI-5)
                    || item.getBagIndex() >= ItemConstants.GRAND_AUTHENTIC_SYMBOL_BASE
                    && item.getBagIndex() < ItemConstants.GRAND_AUTHENTIC_SYMBOL_END) {
                Equip arcaneSymbol = (Equip) item;
                authenticSymbols.add(arcaneSymbol);
            }
        }
        return authenticSymbols;
    }

    public int getZeroWeaponType() {
        if (JobConstants.isZero(getJob())) {
            return getEquippedInventory().getFirstItemByBodyPart(BodyPart.Weapon).getItemId() - 1562000;
        }
        return 0;
    }

    public List<Integer> getTotalChair() {
        List<Integer> result = new ArrayList<>();
        for (Item pi : getInstallInventory().getItems()) {
            if (ItemConstants.isPortableChair(pi.getItemId())) {
                result.add(pi.getItemId());
            }
        }
        return result;
    }

    public List<SkillAlarmInfo> getSkillAlarms() {
        return skillAlarms;
    }

    public void setSkillAlarms(List<SkillAlarmInfo> skillAlarms) {
        this.skillAlarms = skillAlarms;
    }

    public List<SequenceSkill> getSequenceSkills() {
        return sequenceSkills;
    }

    public void setSequenceSkills(List<SequenceSkill> sequenceSkills) {
        this.sequenceSkills = sequenceSkills;
    }

    public List<SequenceBuff> getSequenceBuffs() {
        return sequenceBuffs;
    }

    public void setSequenceBuffs(List<SequenceBuff> sequenceBuffs) {
        this.sequenceBuffs = sequenceBuffs;
    }

    public Int2ObjectMap<Int2IntMap> getBuffFavorites() {
        return buffFavorites;
    }

    public void setBuffFavorites(Int2ObjectMap<Int2IntMap> buffFavorites) {
        this.buffFavorites = buffFavorites;
    }

    public SpecialNodeSkill getSpecialNodeSkill() {
        return specialNodeSkill;
    }

    public int getAscentSkillStack() {
        return ascentSkillStack;
    }

    public void setAscentSkillStack(int ascentSkillStack) {
        this.ascentSkillStack = ascentSkillStack;
    }

    public MobZoneDebuff getMobZoneDebuff() {
        return mobZoneDebuff;
    }

    public void setMobZoneDebuff(MobZoneDebuff mobZoneDebuff) {
        this.mobZoneDebuff = mobZoneDebuff;
    }

    public Tuple<Long, Byte> getLastNormalAttack() {
        return lastNormalAttack;
    }

    public void setLastNormalAttack(Tuple<Long, Byte> lastNormalAttack) {
        this.lastNormalAttack = lastNormalAttack;
    }

    public long getDojoStartTime() {
        return dojoStartTime;
    }

    public void setDojoStartTime(long dojoStartTime) {
        this.dojoStartTime = dojoStartTime;
    }

    public long getDojoCoolTime() {
        return dojoCoolTime;
    }

    public void setDojoCoolTime(long dojoCoolTime) {
        this.dojoCoolTime = dojoCoolTime;
    }

    public void setCores(Set<Core> cores) {
        this.cores = cores;
    }

    public Set<Core> getCores() {
        return cores;
    }

    public int getFirstOpenSlot() {
        if (getCores().isEmpty()) {
            return 0;
        }
        List<Integer> corePos = new ArrayList<>();
        for (Core core : getCores()) {
            corePos.add(core.getPos());
        }
        return Collections.max(corePos) + 1;
    }

    public List<Core> getEquippedCores() {
        return getCores().stream().filter(core -> core.getSlotType() == 0).collect(Collectors.toList());
    }

    public CommerceRecord getCommerceRecord() {
        if (commerceRecord == null) {
            return new CommerceRecord();
        }
        return commerceRecord;
    }

    public void setCommerceRecord(CommerceRecord commerceRecord) {
        this.commerceRecord = commerceRecord;
    }

    public boolean isGM() {
        return getUser().getAccountType().getVal() == AccountType.Admin.getVal() || getUser().getAccountType().getVal() == AccountType.GameMaster.getVal();
    }

    public long getLastReviveTime() {
        return lastReviveTime;
    }

    public void setLastReviveTime(long lastReviveTime) {
        this.lastReviveTime = lastReviveTime;
    }

    public ScheduledFuture<?> getComboKillResetTimer() {
        return comboKillResetTimer;
    }

    public void setComboKillResetTimer(ScheduledFuture<?> comboKillResetTimer) {
        this.comboKillResetTimer = comboKillResetTimer;
    }

    public ScheduledFuture<?> getTimeLimitTimer() {
        return timeLimitTimer;
    }

    public void setTimeLimitTimer(ScheduledFuture<?> timeLimitTimer) {
        this.timeLimitTimer = timeLimitTimer;
    }

    public ScheduledFuture<?> getDropFatigueTimer() {
        return dropFatigueTimer;
    }

    public void setDropFatigueTimer(ScheduledFuture<?> dropFatigueTimer) {
        this.dropFatigueTimer = dropFatigueTimer;
    }

    public ScheduledFuture<?> getRuneRecoveryTimer() {
        return runeRecoveryTimer;
    }

    public void setRuneRecoveryTimer(ScheduledFuture<?> runeRecoveryTimer) {
        this.runeRecoveryTimer = runeRecoveryTimer;
    }

    public ScheduledFuture<?> getWillGaugeTimer() {
        return willGaugeTimer;
    }

    public void setWillGaugeTimer(ScheduledFuture<?> willGaugeTimer) {
        this.willGaugeTimer = willGaugeTimer;
    }

    public ScheduledFuture<?> getBonusTimer() {
        return bonusTimer;
    }

    public void setBonusTimer(ScheduledFuture<?> bonusTimer) {
        this.bonusTimer = bonusTimer;
    }

    public ScheduledFuture<?> getScoreTimer() {
        return scoreTimer;
    }

    public void setScoreTimer(ScheduledFuture<?> scoreTimer) {
        this.scoreTimer = scoreTimer;
    }

    public ScheduledFuture<?> getStartEventTimer() {
        return startEventTimer;
    }

    public void setStartEventTimer(ScheduledFuture<?> startEventTimer) {
        this.startEventTimer = startEventTimer;
    }

    public ScheduledFuture<?> getEndEventTimer() {
        return endEventTimer;
    }

    public void setEndEventTimer(ScheduledFuture<?> endEventTimer) {
        this.endEventTimer = endEventTimer;
    }

    // End Timers

    public List<CharacterPotentialValueHolder> getCharacterPotentialValueHolder() {
        return characterPotentialValueHolders;
    }

    public void setCharacterPotentialValueHolders(List<CharacterPotentialValueHolder> characterPotentialValueHolders) {
        this.characterPotentialValueHolders = characterPotentialValueHolders;
    }

    public CharacterPotentialValueHolder getCharacterPotentialValueHolder(byte key) {
        return characterPotentialValueHolders.stream().filter(p -> p.getKey() == key).findAny().orElse(null);
    }

    public int getTraitLevelByExp(int exp) {
        int level = 0;
        while (exp >= GameConstants.getTraitExpByLevel(level)) {
            if (GameConstants.getTraitExpByLevel(level) == 0) {
                break;
            }
            exp -= GameConstants.getTraitExpByLevel(level);
            level++;
        }
        return level;
    }

    public int getTraitExpAtLevel(Stat stat, int exp) {
        int level = 0;
        while (exp >= GameConstants.getTraitExpByLevel(level)) {
            exp -= GameConstants.getTraitExpByLevel(level);
            level++;
        }
        return exp;
    }

    public void initTraits() {
        int willPowerLevel = getTraitLevelByExp(getStat(Stat.willEXP));
        int maxHP = GameConstants.getHpMpByTraitLevel(willPowerLevel);
        int empathyLevel = getTraitLevelByExp(getStat(Stat.senseEXP));
        int maxMP = GameConstants.getHpMpByTraitLevel(empathyLevel);
        int buffDurationR = GameConstants.getBuffDurationByTraitLevel(empathyLevel);
        if (maxHP != 0) {
            addBaseStat(BaseStat.mhp, maxHP);
        }
        if (maxMP != 0) {
            addBaseStat(BaseStat.mmp, maxMP);
        }
        if (buffDurationR != 0) {
            addBaseStat(BaseStat.buffTimeR, buffDurationR);
        }
    }

    public void initKeyBoards() {
        // Quicks Slot Keys:
        if (getQuickslotKeys() == null || getQuickslotKeys().isEmpty()) {
            setQuickslotKeys(new LinkedList<>(Arrays.asList(GameConstants.QuickSlot_basic)));
        }
        // Funckey Mapped Keys:
        if (getFuncKeyMaps().size() < 3) {
            funcKeyMaps.clear();
            initFuncKeyMaps(0, false);
        }
        write(FieldPacket.funcKeyMappedManInit(this));
        // Macro Keys:
        List<Macro> macroList = getMacros();
        macroList.sort(Comparator.comparingLong(Macro::getId));
        if (!getMacros().isEmpty()) {
            write(WvsContext.macroSysDataInit(macroList));
        } else {
            write(WvsContext.macroSysDataInit(new ArrayList<>()));
        }
        // Skill Sequences:
        if (!hasQuest(QuestConstants.SKILL_SEQUENCE_SKILLS)) {
            createQuestWithQRValue(QuestConstants.SKILL_SEQUENCE_SKILLS, String.format(QuestConstants.SKILL_SEQUENCE_SKILLS_FORMAT, 0, 0, 0));
        }
        if (!hasQuest(QuestConstants.SKILL_SEQUENCE_BUFFS)) {
            createQuestWithQRValue(QuestConstants.SKILL_SEQUENCE_BUFFS, String.format(QuestConstants.SKILL_SEQUENCE_BUFFS_FORMAT, 0, 0));
        }
        List<SequenceSkill> skills = getSequenceSkills();
        if (!skills.isEmpty()) {
            write(WvsContext.skillSquenceSkills(this, skills));
        } else {
            write(WvsContext.skillSquenceSkills(this, new ArrayList<>()));
        }
        List<SequenceBuff> buffs = getSequenceBuffs();
        if (!buffs.isEmpty()) {
            write(WvsContext.skillSquenceBuffs(this, buffs));
        } else {
            write(WvsContext.skillSquenceBuffs(this, new ArrayList<>()));
        }
        Int2ObjectMap<Int2IntMap> favorites = getBuffFavorites();
        write(BuffFavoritePacket.encode(favorites));
    }

    public void initHyperRockFields() {
        if (getHyperRockFields().length <= 0) { // Special cases
            setHyperRockFields(new int[]{
                    999999999, 999999999, 999999999, 999999999,
                    999999999, 999999999, 999999999, 999999999,
                    999999999, 999999999, 999999999, 999999999, 999999999});
        }
        if (getHyperRockFields().length != 13) {
            int[] fields = new int[13];
            for (int i = 0; i < getHyperRockFields().length; i++) {
                fields[i] = getHyperRockFields()[i];
            }
            for (int i = getHyperRockFields().length; i < 13; i++) {
                fields[i] = 999999999;
            }
            setHyperRockFields(fields);
        }
        write(WvsContext.mapTransferResult(MapTransferType.RegisterListSend, (byte) 5, getHyperRockFields()));
    }

    private static boolean isDailyQuestDateToday(String date, LocalDateTime now) {
        return DateTimeFormatter.ofPattern("yy/MM/dd").format(now).equals(date);
    }

    public void initUnionQuests() {
        Account acc = getAccount();
        if (hasQuestCompleted(QuestConstants.UNION_FIRST_QUEST)) {
            final String date = getQRValueByKey(QuestConstants.UNION_QUEST, "q1Date"); // yy/MM/dd
            if (date != null) {
                final LocalDateTime now = LocalDateTime.now();
                if (!isDailyQuestDateToday(date, now)) {
                    removeQuest(QuestConstants.UNION_FIRST_QUEST);
                    AccountQuest q = new AccountQuest(acc.getId(), QuestConstants.UNION_FIRST_QUEST, NotStarted);
                    acc.addQuest(q);
                    setQRValueByKey(QuestConstants.UNION_QUEST, "q1", "0");
                }
            }
        }
        if (hasQuestCompleted(QuestConstants.UNION_SECOND_QUEST)) {
            final String date = getQRValueByKey(QuestConstants.UNION_QUEST, "q2Date"); // yy/MM/dd
            if (date != null) {
                final LocalDateTime now = LocalDateTime.now();
                if (!isDailyQuestDateToday(date, now)) {
                    removeQuest(QuestConstants.UNION_SECOND_QUEST);
                    AccountQuest q = new AccountQuest(acc.getId(), QuestConstants.UNION_SECOND_QUEST, NotStarted);
                    acc.addQuest(q);
                    setQRValueByKey(QuestConstants.UNION_QUEST, "q2", "0");
                }
            }
        }
    }

    public void initWeeklyReset() {
        if (hasQuest(QuestConstants.UNION_ARTIFACT)) {
            var dataQRKey = QuestConstants.UNION_ARTIFACT_DATA;
            String resetDate = getQRValueByKey(dataQRKey, "resetDate");
            if (FileTime.isWeeklyResetDue(resetDate)) {
                var next = FileTime.getNextWeeklyResetTime().weeklyFormat();
                String attendance_lastDate = getQRValueByKey(dataQRKey, "attendance_lastDate");
                createQuestWithQRValue(dataQRKey, "attendance=0;mobCount=0;resetDate="+next+";resetDate_boss="+next+";attendance_lastDate="+attendance_lastDate);
                createQuestWithQRValue(QuestConstants.UNION_ARTIFACT_COMMON, "missionState=0000000");
                createQuestWithQRValue(QuestConstants.UNION_ARTIFACT_BOSS, "count=0;mobid=0;lasttime="+FileTime.currentTime().toYYYYMMDD_HHMMss());
                createQuestWithQRValue(QuestConstants.UNION_ARTIFACT_HUNT, "missionState=000000000000000000000000000000000000000000000000");
                createQuestWithQRValue(QuestConstants.UNION_ARTIFACT_HUNT_2, "missionState=00");
            }
        }
        var q = getAccount().getQuestById(QuestConstants.UNION_RAID_WEEKLY);
        if (q != null) {
            String lastTimeWeekly = getQRValueByKey(QuestConstants.UNION_RAID_WEEKLY, "startDate");
            if (FileTime.isNewWeek(lastTimeWeekly)) { // reset tuần
                if (q.getStatus() != Completed) {
                    getScriptManager().updateAvailableUnionCoin(200);
                    chatMessage(GameDesc, "請領取上週的聯盟戰地硬幣 x 200！");
                }
                removeQuest(q.getQRKey());
            }
        }
        if (hasQuest(QuestConstants.CRYSTAL_WEEKLY)) {
            String lastTimeWeekly = getQRValueByKey(QuestConstants.CRYSTAL_WEEKLY, "time");
            if (FileTime.isNewWeek(lastTimeWeekly)) { // reset tuần
                createQuestWithQRValue(QuestConstants.CRYSTAL_WEEKLY, "time="+FileTime.currentTime().toYYYYMMDD()+";max="+QuestConstants.CRYSTAL_MAX_WEEKLY);
                createQuestWithQRValue(QuestConstants.CRYSTAL_ALL, "count="+QuestConstants.CRYSTAL_MAX_ALL+";time="+FileTime.currentTime().toYYYYMMDD()+";max="+QuestConstants.CRYSTAL_MAX_ALL+";type=2");
            }
        }
    }

    public void initDailyReset() {
        initSymbolQuests();
        // VIP Booster Daily Reset
        var vipBoosterQRKey = 101717;
        if (hasQuest(vipBoosterQRKey)) {
            final String date = getQRValueByKey(vipBoosterQRKey, "date");
            final LocalDateTime now = LocalDateTime.now();
            boolean shouldRenew = !isDailyQuestDateToday(date, now);
            if (shouldRenew) {
                createQuestWithQRValue(vipBoosterQRKey, "useCount=0;date=" + FileTime.currentTime().toYYMMDD());
            }
        }
        if (hasQuest(QuestConstants.UNION_ARTIFACT)) {
            var dataQRKey = QuestConstants.UNION_ARTIFACT_DATA;
            int attendance = Integer.parseInt(getQRValueByKey(dataQRKey, "attendance"));
            String attendance_lastDate = getQRValueByKey(dataQRKey, "attendance_lastDate");
            if (attendance >= 4) {
                return;
            }
            if (FileTime.hasCheckedInToday(attendance_lastDate)) {
                return;
            }
            attendance += 1;
            setQRValueByKey(dataQRKey, "attendance_lastDate", FileTime.currentTime().weeklyFormat());
            setQRValueByKey(dataQRKey, "attendance", attendance);
            if (attendance >= 1) {
                boolean increased = false;
                boolean showMessage = false;
                int point = Integer.parseInt(getQRValueByKey(QuestConstants.UNION_ARTIFACT, "point"));
                int incExp = 0;
                var info = ArtifactData.getArtifactInfo();
                for (int missionIdx = 4; missionIdx <= 7; missionIdx++) {
                    if (!isArtifactMissionCompleted(this, ArtifactData.MissionType.Common, missionIdx))  {
                        var commonMission = info.getCommonMission(missionIdx);
                        if (!showMessage) {
                            chatScriptMessage(commonMission.getName());
                            showMessage = true;
                        }
                        if (attendance >= commonMission.getValue()) {
                            updateArtifactMission(this, ArtifactData.MissionType.Common, missionIdx);
                            point += commonMission.getArtifactPoint();
                            incExp += commonMission.getArtifactExp();
                            increased = true;
                            write(WvsContext.unionArtifactQuestMsg(1, ArtifactData.MissionType.Common, missionIdx, incExp, point));
                        }
                    }
                }
                if (increased) {
                    setQRValueByKey(QuestConstants.UNION_ARTIFACT, "point", point);
                    incArtifactExp(incExp);
                }
            }
        }
    }

    public void initSymbolQuests() {
        // [Daily Quest] Vanishing Journey Research
        handleSymbolQuest(new int[]{QuestConstants.VANISHING_JOURNEY_DAILY_QUEST_PREQUEST_NEW},
                QuestConstants.VANISHING_JOURNEY_DAILY_QUEST_NEW,
                QuestConstants.VANISHING_JOURNEY_DAILY_QUEST_NEW_END,
                "[通知] 消逝的旅途每日任務已刷新！");

        // [Daily Quest] Chu Chu's Finest Cuisine
        handleSymbolQuest(new int[]{QuestConstants.CHU_CHU_DAILY_QUEST_PREQUEST_NEW},
                QuestConstants.CHU_CHU_DAILY_QUEST_NEW,
                QuestConstants.CHU_CHU_DAILY_QUEST_NEW_END,
                "[通知] 啾啾艾爾蘭每日任務已刷新！");

        // [Daily Quest] A Night's Peace in Lachelein
        handleSymbolQuest(new int[]{QuestConstants.LACHELEIN_DAILY_QUEST_PREQUEST_NEW},
                QuestConstants.LACHELEIN_DAILY_QUEST_NEW,
                QuestConstants.LACHELEIN_DAILY_QUEST_NEW_END,
                "[通知] 拉契爾恩每日任務已刷新！");

        // [Daily Quest] Peace in Arcana
        handleSymbolQuest(new int[]{QuestConstants.ARCANA_DAILY_QUEST_PREQUEST_NEW},
                QuestConstants.ARCANA_DAILY_QUEST_NEW,
                QuestConstants.ARCANA_DAILY_QUEST_NEW_END,
                "[通知] 阿爾卡娜每日任務已刷新！");

        // [Daily Quest] Save the Morass
        handleSymbolQuest(new int[]{QuestConstants.MORASS_DAILY_QUEST_PREQUEST_NEW},
                QuestConstants.MORASS_DAILY_QUEST_NEW,
                QuestConstants.MORASS_DAILY_QUEST_NEW_END,
                "[通知] 魔菈斯每日任務已刷新！");

        // [Daily Quest] Esfera Research Orders
        handleSymbolQuest(new int[]{QuestConstants.ESFERA_DAILY_QUEST_PREQUEST_NEW},
                QuestConstants.ESFERA_DAILY_QUEST_NEW,
                QuestConstants.ESFERA_DAILY_QUEST_NEW_END,
                "[通知] 艾斯佩拉每日任務已刷新！");

        // [Daily Quest] Moonbridge Research
        handleSymbolQuest(new int[]{QuestConstants.MOONBRIDGE_DAILY_QUEST_PREQUEST},
                QuestConstants.MOONBRIDGE_DAILY_QUEST,
                QuestConstants.MOONBRIDGE_DAILY_QUEST,
                "[通知] 月之橋每日任務已刷新！");

        // [Daily Quest] Labyrinth of Suffering Research
        handleSymbolQuest(new int[]{QuestConstants.LADYRINTH_DAILY_QUEST_PREQUEST},
                QuestConstants.LADYRINTH_DAILY_QUEST,
                QuestConstants.LADYRINTH_DAILY_QUEST,
                "[通知] 苦痛迷宮每日任務已刷新！");

        // [Daily Quest] Limina Research
        handleSymbolQuest(new int[]{QuestConstants.LIMINA_DAILY_QUEST_PREQUEST},
                QuestConstants.LIMINA_DAILY_QUEST,
                QuestConstants.LIMINA_DAILY_QUEST,
                "[通知] 利曼每日任務已刷新！");
    }

    private void handleSymbolQuest(int[] preQuestIds, int checkQuestID, int endQuestID, String renewMessage) {
        boolean allPrequelsCompleted = true;
        Account acc = getAccount();
        for (int preQuestId : preQuestIds) {
            if (!hasQuestCompleted(preQuestId)) {
                allPrequelsCompleted = false;
                break; // Stop checking once one is found to be incomplete
            }
        }
        if (allPrequelsCompleted) {
            boolean questExists = hasQuestCompleted(checkQuestID) || hasQuest(checkQuestID);
            final String date = getQRValueByKey(checkQuestID, "date");
            final LocalDateTime now = LocalDateTime.now();
            boolean shouldRenew = !isDailyQuestDateToday(date, now);
            if (shouldRenew) {
                // Remove old quests
                for (int i = checkQuestID; i <= endQuestID; i++) {
                    removeQuest(i);
                }
                // Renew message and set up new quests
                chatMessage(GameDesc, renewMessage);
                AccountQuest q = new AccountQuest(acc.getId(), checkQuestID, NotStarted);
                acc.addQuest(q);
                String qrValue = "date=" + DateTimeFormatter.ofPattern("yy/MM/dd").format(now) + ";";
                createQuestWithQRValue(checkQuestID, qrValue);
            } else if (!questExists) {
                // If quest doesn't exist at all, create it for the first time
                AccountQuest q = new AccountQuest(acc.getId(), checkQuestID, NotStarted);
                acc.addQuest(q);
                String qrValue = "date=" + DateTimeFormatter.ofPattern("yy/MM/dd").format(now) + ";";
                createQuestWithQRValue(checkQuestID, qrValue);
            }
        }
    }

    public List<RewardInfo> getRewardInfos() {
        return rewardInfos;
    }

    public void setRewardInfos(List<RewardInfo> rewardInfos) {
        this.rewardInfos = rewardInfos;
    }

    public void initRewardSystem() {
        if (rewardSystem != null) {
            return;
        }
        rewardSystem = new RewardSystem(RewardSystemType.Response_Rewards);
        //Load tất cả item đã từng được gửi không phân biệt nhận rồi hay không
        List<RewardInfo> rewards = RewardInfo.getDataFromSQL(getId());
        int quantityF = 0;
        FileTime startF = null;
        FileTime endF = null;
        int quantityD = 0;
        FileTime startD = null;
        FileTime endD = null;
        if (rewards.size() > 0) {
            getRewardInfos().clear();
            for (RewardInfo reward : rewards) {
                if (reward.getCharID() == getId()) {
                    if (reward.getEndTime() != null && reward.getEndTime().toLocalDateTime().isAfter(LocalDateTime.now())) {
                        if (reward.getItemID() == 2028346) {
                            quantityF += reward.getQuantity();
                            if (startF == null) {
                                startF = reward.getStartTime();
                                endF = reward.getEndTime();
                            }
                        } else if (reward.getItemID() == 2028345) {
                            quantityD += reward.getQuantity();
                            if (startD == null) {
                                startD = reward.getStartTime();
                                endD = reward.getEndTime();
                            }
                        } else {
                            getRewardInfos().add(reward);
                        }
                    } else {
                        reward.deleteFromSQL();
                    }
                }
            }
            String guildName = "";
            if (getGuild() != null) {
                guildName = getGuild().getName();
            }
            if (quantityF > 0) {
                getRewardInfos().add(new RewardInfo(this, RewardItemType.Item, 2028346, quantityF,
                        "F 級公會獎勵 - 公會 [" + guildName + "] 成員成功擊敗 Boss [普通]。", startF, endF));
            }
            if (quantityD > 0) {
                getRewardInfos().add(new RewardInfo(this, RewardItemType.Item, 2028345, quantityD,
                        "D 級公會獎勵 - 公會 [" + guildName + "] 成員成功擊敗 Boss [困難]，或購買低於 100,000 元的禮包。", startD, endD));
            }
            if (getRewardInfos().size() > 0) {
                getRewardSystem().setRewards(rewardInfos);
            }
        }
    }

    public RewardSystem getRewardSystem() {
        return rewardSystem;
    }

    public void setRewardSystem(RewardSystem rewardSystem) {
        this.rewardSystem = rewardSystem;
    }

    public void sendPacketRewards() {
        if (getRewardSystem() == null || getRewardSystem().getRewards().size() == 0) {
            return;
        }
        write(WvsContext.userReceiveStuffs(RewardResult.response_Rewards(rewardSystem)));
    }

    public void initFreeNodeStoneDailyGift() {
        if (!hasQuestCompleted(1466)) {
            return;
        }
        final LocalDateTime now = LocalDateTime.now();
        if (hasQuest(QuestConstants.FREE_NODE_STONE_DAILY_GIFT)) {
            final String date = getQRValueByKey(QuestConstants.FREE_NODE_STONE_DAILY_GIFT, "date"); // yy/MM/dd
            if (date != null) {
                int count = Integer.parseInt(getQRValueByKey(QuestConstants.FREE_NODE_STONE_DAILY_GIFT, "count"));
                if (!isDailyQuestDateToday(date, now)) {
                    sendRewardToChar(2435902, 10, 0, "感謝你完成任務「強大的力量」。", 1);
                    int newCount = count + 1;
                    createQuestWithQRValue(QuestConstants.FREE_NODE_STONE_DAILY_GIFT, "date=" + DateTimeFormatter.ofPattern("yy/MM/dd").format(now) + ";count=" + newCount + ";");
                    sendPacketRewards();
                    chatMessage(Tip, "你已獲得完成任務「強大的力量」的每日獎勵，請及時領取。");
                }
                return;
            }
        }
        sendRewardToChar(2435902, 10, 0, "感謝你完成任務「強大的力量」。", 1);
        createQuestWithQRValue(QuestConstants.FREE_NODE_STONE_DAILY_GIFT, "date=" + DateTimeFormatter.ofPattern("yy/MM/dd").format(now) + ";count=1;");
        sendPacketRewards();
        chatMessage(Tip, "你已獲得完成任務「強大的力量」的每日獎勵，請及時領取。");
    }

    public void initMVPDailyGift() {
        if (getUser() == null) return;
        synchronized (getUser()) {
        boolean isUpdated = false;
        final LocalDateTime now = LocalDateTime.now();
        if (getUser().getVipGrade() >= 1) {
            FileTime vipGradeExpireDate = getUser().getVipExpiredDate();
            if (net.swordie.ms.client.MvpMembership.expired(vipGradeExpireDate == null ? null : vipGradeExpireDate.toLocalDateTime(), now)) {
                getUser().setVipGrade(0);
                getUser().setVipExpiredDate(FileTime.MIN_TIME());
                chatMessage(Tip, "你的 MVP 會員資格已過期，將被移除。");
                isUpdated = true;
            }
        }
        FileTime last = getUser().getFreeVipPointDate();
        if (net.swordie.ms.client.MvpMembership.dailyAvailable(last == null ? null : last.toLocalDateTime(), now)
                && getUser().getVipPoints() <= Integer.MAX_VALUE - 100) {
            getUser().addVipPoints(100);
            getUser().setFreeVipPointDate(FileTime.currentTime());
            chatMessage(Tip, "你今天已獲得 100 點免費 MVP 點數。");
            isUpdated = true;
        }
        if (isUpdated) {
            getUser().saveToSQL(false);
        }
        }
    }

    /**
     * Use for send reward to character when Reach lv 200 at Beta
     **/
    public void sendLevelRewardToChar(int level) {
        LocalDateTime end = LocalDateTime.now().plusDays(30);
        List<RewardInfo> rewards = switch (level) {
            case 30 -> List.of(
                    new RewardInfo(this, RewardItemType.Item, 2430447, 1, 0, 0, 0, "服務器贈送的等級 30 達成獎勵！", FileTime.currentTime(), FileTime.fromDate(end)),
                    new RewardInfo(this, RewardItemType.Item, 1112444, 1, 0, 0, 0, "服務器贈送的等級 30 達成獎勵！", FileTime.currentTime(), FileTime.fromDate(end)),
                    new RewardInfo(this, RewardItemType.Item, 2450140, 1, 0, 0, 0, "服務器贈送的等級 30 達成獎勵！", FileTime.currentTime(), FileTime.fromDate(end))
            );
            case 60 -> List.of(
                    new RewardInfo(this, RewardItemType.Item, 2450140, 2, 0, 0, 0, "服務器贈送的等級 60 達成獎勵！", FileTime.currentTime(), FileTime.fromDate(end))
            );
            case 140 -> List.of(
                    new RewardInfo(this, RewardItemType.Item, 2435302, 1, 0, 0, 0, "服務器贈送的等級 140 達成獎勵！", FileTime.currentTime(), FileTime.fromDate(end)),
                    new RewardInfo(this, RewardItemType.Item, 2435303, 1, 0, 0, 0, "服務器贈送的等級 140 達成獎勵！", FileTime.currentTime(), FileTime.fromDate(end))
            );
            case 200 -> List.of(
                    new RewardInfo(this, RewardItemType.Item, 2435719, 10, 0, 0, 0, "服務器贈送的等級 200 達成獎勵！", FileTime.currentTime(), FileTime.fromDate(end))
            );
            case 250 -> List.of(
                    new RewardInfo(this, RewardItemType.Item, 3014005, 1, 0, 0, 0, "服務器贈送的等級 250 達成獎勵！", FileTime.currentTime(), FileTime.fromDate(end))
            );
            case 275 -> List.of(
                    new RewardInfo(this, RewardItemType.Item, 3014028, 1, 0, 0, 0, "服務器贈送的等級 275 達成獎勵！", FileTime.currentTime(), FileTime.fromDate(end)),
                    new RewardInfo(this, RewardItemType.Item, 2023819, 10, 0, 0, 0, "服務器贈送的等級 275 達成獎勵！", FileTime.currentTime(), FileTime.fromDate(end)),
                    new RewardInfo(this, RewardItemType.Item, 3700549, 1, 0, 0, 0, "服務器贈送的等級 275 達成獎勵！", FileTime.currentTime(), FileTime.fromDate(end))
            );
            default -> new ArrayList<>();
        };
        if (!rewards.isEmpty()) {
            if (getRewardSystem() == null) {
                initRewardSystem(); // as the guild-reward path; script level-ups/gifts can come first
            }
            for (RewardInfo rewardInfo : rewards) {
                getRewardSystem().addReward(rewardInfo);
            }
            sendPacketRewards();
        }
    }

    public void sendSpecialRewardToChar(int itemID, int quantity, int meso) {
        sendRewardToChar(itemID, quantity, meso, "來自 GM 的禮物", 30);
    }

    public void sendRewardToChar(int itemID, int quantity, long meso, String msg, int day) {
        LocalDateTime end = LocalDateTime.now().plusDays(day);
        List<RewardInfo> rewards = new ArrayList<>();
        if (itemID != 0) {
            rewards.add(new RewardInfo(this, RewardItemType.Item, itemID, quantity, msg, FileTime.currentTime(), FileTime.fromDate(end)));
        }
        if (meso != 0 && quantity == 0) {
            rewards.add(new RewardInfo(this, RewardItemType.Meso, meso, msg, FileTime.currentTime(), FileTime.fromDate(end)));
        }
        if (meso != 0 && quantity != 0) {
            rewards.add(new RewardInfo(this, RewardItemType.Exp, meso, msg, FileTime.currentTime(), FileTime.fromDate(end), true));
        }
        if (!rewards.isEmpty()) {
            if (getRewardSystem() == null) {
                initRewardSystem(); // as the guild-reward path; script level-ups/gifts can come first
            }
            for (RewardInfo rewardInfo : rewards) {
                getRewardSystem().addReward(rewardInfo);
            }
        }
    }

    public void sendGuildReward(String owner, String guildName, int type) {
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        List<RewardInfo> rewards = switch (type) {
            case 0 -> List.of(
                    new RewardInfo(this, RewardItemType.Item, 2028346, 1,
                            "F 級公會獎勵 - 成員 [" + owner + "] 在公會 [" + guildName + "] 中成功擊敗 Boss [普通]。", FileTime.currentTime(), FileTime.fromDate(end))
            );
            case 1 -> List.of(
                    new RewardInfo(this, RewardItemType.Item, 2028345, 1,
                            "D 級公會獎勵 - 成員 [" + owner + "] 在公會 [" + guildName + "] 中成功擊敗 Boss [困難]，或購買低於 100,000 元的禮包。", FileTime.currentTime(), FileTime.fromDate(end))
                    // Đánh boss Normal | Nạp dưới 100k
            );
            case 2 -> List.of(
                    new RewardInfo(this, RewardItemType.Item, 2028344, 1,
                            "C 級公會獎勵 - 成員 [" + owner + "] 在公會 [" + guildName + "] 中成功擊敗 Boss [超困難]，或購買 100,000 - 500,000 元的禮包。", FileTime.currentTime(), FileTime.fromDate(end))
                    // Đánh boss Hard | Nạp 100 - 500k
            );
            case 3 -> List.of(
                    new RewardInfo(this, RewardItemType.Item, 2028343, 1,
                            "B 級公會獎勵 - 成員 [" + owner + "] 在公會 [" + guildName + "] 中成功購買 500,000 - 1,000,000 元的禮包。", FileTime.currentTime(), FileTime.fromDate(end))
                    // Nạp tiền | Nạp 500k - 1tr
            );
            case 4 -> List.of(
                    new RewardInfo(this, RewardItemType.Item, 2028342, 1,
                            "A 級公會獎勵 - 成員 [" + owner + "] 在公會 [" + guildName + "] 中成功購買 1,000,000 - 2,000,000 元的禮包。", FileTime.currentTime(), FileTime.fromDate(end))
                    // Nạp tiền 1tr
            );
            case 5 -> List.of(
                    new RewardInfo(this, RewardItemType.Item, 2028338, 1,
                            "S 級公會獎勵 - 成員 [" + owner + "] 在公會 [" + guildName + "] 中成功購買超過 2,000,000 元的禮包。", FileTime.currentTime(), FileTime.fromDate(end))
                    // Nạp tiền 2tr
            );
            default -> new ArrayList<>();
        };
        if (rewards.size() > 0) {
            if (getRewardSystem() == null) {
                initRewardSystem();
            }
            for (RewardInfo rewardInfo : rewards) {
                getRewardSystem().addReward(rewardInfo);
            }
        }
    }

    public void initFatigueTimer() {
        if (dropFatigueTimer != null) {
            dropFatigueTimer.cancel(true);
        }
        if (getStat(Stat.fatigue) > 0) {
            ScheduledFuture<?> sf = getTimer().addFixedRateEvent(() -> {
                setStatAndSendPacket(Stat.fatigue, Math.max(getStat(Stat.fatigue) - 20, 0));
            }, 1, 1, TimeUnit.HOURS, false);
            dropFatigueTimer = sf;
            GlobalTimerManager.addCharTimer(this.getId(), sf);
        }
    }

    // Chu Chu PQ & Tayoon Cooking:

    public Tuple<Integer, Integer> getRecipe() {
        return recipe;
    }

    public void setRecipe(Tuple<Integer, Integer> recipe) {
        this.recipe = recipe;
    }

    public void addChuChuRecipe(int itemID) {
        if (itemID >= 2435856 && itemID <= 2435872) {
            if (getRecipe().getLeft() == itemID) {
                getRecipe().setLeft(itemID);
                getRecipe().setRight(getRecipe().getRight() + 1);
            } else {
                setRecipe(new Tuple<>(itemID, 1));
            }
            getField().broadcast(HungryMutoPacket.addItem(this));
        }
    }

    // Nett Pyramid:

    public DefenseEvent getDefenseEvent() {
        return defenseEvent;
    }

    public void setDefenseEvent(DefenseEvent defenseEvent) {
        this.defenseEvent = defenseEvent;
    }

    public DefenseEventMember getDefenseEventMember() {
        if (defenseEventMember == null) {
            this.defenseEventMember = new DefenseEventMember(this);
        }
        return defenseEventMember;
    }

    public void setDefenseEventMember(DefenseEventMember defenseEventMember) {
        this.defenseEventMember = defenseEventMember;
    }

    public Map<Integer, String> getVisitorStageResult() {
        return visitorStageResult;
    }

    public void setVisitorStageResult(Map<Integer, String> visitorStageResult) {
        this.visitorStageResult = visitorStageResult;
    }

    // Random Portals:

    public DefenseTowerWave getDefenseTowerWave() {
        return defenseTowerWave;
    }

    public void setDefenseTowerWave(DefenseTowerWave defenseTowerWave) {
        this.defenseTowerWave = defenseTowerWave;
    }

    public void initDefenseTowerWave() {
        this.defenseTowerWave = new DefenseTowerWave(1, 20);
    }

    public BountyHunting getBountyHunting() {
        return bountyHunting;
    }

    public void setBountyHunting(BountyHunting bountyHunting) {
        this.bountyHunting = bountyHunting;
    }

    public void initBountyHunting() {
        this.bountyHunting = new BountyHunting(1);
    }

    public FrittoEagle getFrittoEagle() {
        return frittoEagle;
    }

    public void setFrittoEagle(FrittoEagle frittoEagle) {
        this.frittoEagle = frittoEagle;
    }

    public void initFrittoEagle() {
        this.frittoEagle = new FrittoEagle(0, 20);
    }

    public FrittoEgg getFrittoEgg() {
        return frittoEgg;
    }

    public void setFrittoEgg(FrittoEgg frittoEgg) {
        this.frittoEgg = frittoEgg;
    }

    public void initFrittoEgg() {
        this.frittoEgg = new FrittoEgg(0);
    }

    public FrittoDancing getFrittoDancing() {
        return frittoDancing;
    }

    public void setFrittoDancing(FrittoDancing frittoDancing) {
        this.frittoDancing = frittoDancing;
    }

    public void initFrittoDancing() {
        this.frittoDancing = new FrittoDancing(2);
    }

    public DreamBreaker getDreamBreaker() {
        if (dreamBreaker == null) {
            this.dreamBreaker = new DreamBreaker(this);
        }
        return dreamBreaker;
    }

    public SpiritSavior getSpiritSavior() {
        if (spiritSavior == null) {
            this.spiritSavior = new SpiritSavior(this);
        }
        return spiritSavior;
    }

    public EvolutionSystem getEvolutionSystem() {
        if (evolutionSystem == null) {
            this.evolutionSystem = new EvolutionSystem(this);
        }
        return evolutionSystem;
    }

    public void initFeverTimeEvent(boolean active) {
        write(UserPacket.effect(Effect.reservedEffectRepeat(WzConstants.EFFECT_FEVER_TIME_ING, active, true, -103, -300, 0)));
        EventConstants.STAR_FORCE_FEVER_TIME_EVENT = active;
    }

    public void initMiracleTimeEvent(boolean active) {
        write(UserPacket.effect(Effect.reservedEffectRepeat(WzConstants.EFFECT_MIRACLE_TIME_ING, active, true, -103, -300, 0)));
        EventConstants.MIRACLE_TIME_EVENT = active;
    }

    public void initExpRateEvent(boolean active) {
        write(UserPacket.effect(Effect.reservedEffectRepeat(WzConstants.EFFECT_EXP_RATE_EVENT_ING, active, true, -119, -300, 0)));
        EventConstants.EXP_RATE_EVENT = active;
        //TODO: Change Exp Rate
    }

    public void initDropRateEvent(boolean active) {
        write(UserPacket.effect(Effect.reservedEffectRepeat(WzConstants.EFFECT_DROP_RATE_EVENT_ING, active, true, -131, -300, 0)));
        EventConstants.DROP_RATE_EVENT = active;
        //TODO: Change Drop Rate
    }

    public void addDiceBaseStat(BaseStat baseStat, int value) {
        diceBaseStats.put(baseStat, diceBaseStats.get(baseStat) != null ? diceBaseStats.get(baseStat) + value : value);
        addBaseStat(baseStat, value);
    }

    public void removeDiceBaseStat(BaseStat baseStat) {
        int value = diceBaseStats.get(baseStat);
        removeBaseStat(baseStat, value);
        diceBaseStats.entrySet().removeIf(x -> x.getKey().equals(baseStat));
    }

    public void sendPopupSay(String msg) {
        write(UserLocal.addPopupSay(9010063, 10000, msg, "FarmSE.img/boxResult"));
    }

    public void sendPopupSay(int npcID, String msg) {
        write(UserLocal.addPopupSay(npcID, 10000, msg, "FarmSE.img/boxResult"));
    }

    public boolean is1HitKOSkill(int damage) {
        int newHP = getMaxHP() - (damage);
        if (newHP <= 0 && getHP() > 0) {
            TemporaryStatManager tsm = getTemporaryStatManager();
            if (tsm.getOptByCTSAndSkill(ReviveOnce, Shade.SUMMON_OTHER_SPIRIT) != null) {
                tsm.removeStatsBySkill(Shade.SUMMON_OTHER_SPIRIT);
                Option o = new Option();
                o.nOption = 1;
                o.rOption = Shade.SUMMON_OTHER_SPIRIT;
                o.tOption = 3;
                tsm.sendStat(NotDamaged, o);
                write(UserPacket.effect(Effect.skillSpecial(Shade.SUMMON_OTHER_SPIRIT)));
                getField().broadcast(UserRemote.effect(getId(), Effect.skillSpecial(Shade.SUMMON_OTHER_SPIRIT)), this);
                write(UserPacket.effect(Effect.skillUse(25111211, getLevel(), (byte) 1)));
                getField().broadcast(UserRemote.effect(getId(), Effect.skillUse(25111211, getLevel(), (byte) 1)), this);
                addSkillCooldown(Shade.SUMMON_OTHER_SPIRIT, 1800000);
                return true;
            }
            if (tsm.getOptByCTSAndSkill(PreReviveOnce, Shade.CLOSE_CALLS) != null || tsm.getOptByCTSAndSkill(PreReviveOnce, Job.CLOSE_CALLS_LINK) != null) {
                int skillID = Job.CLOSE_CALLS_LINK;
                if (hasSkill(Shade.CLOSE_CALLS)) {
                    skillID = Shade.CLOSE_CALLS;
                }
                if (Util.succeedProp(10)) {
                    write(UserPacket.effect(Effect.skillUse(skillID, getSkillLevel(skillID), 0)));
                    getField().broadcast(UserRemote.effect(getId(), Effect.skillUse(skillID, getSkillLevel(skillID), 0)), this);
                    return true;
                } else {
                    return false;
                }
            }
            if (tsm.hasStat(SpiritGuard) && getJobHandler() instanceof Shade shade) {
                shade.deductSpiritWard();
                chatMessage("你已透過靈魂結界復活。");
                return true;
            }
        }
        return false;
    }

    public void addBaseStatByDiceNumber(int number, SkillInfo si, int slv) {
        switch (number) {
            case 2 -> {
                addDiceBaseStat(BaseStat.pddR, si.getValue(pddR, slv));
            }
            case 3 -> {
                addDiceBaseStat(BaseStat.mhpR, si.getValue(mhpR, slv));
                addDiceBaseStat(BaseStat.mmpR, si.getValue(mmpR, slv));
            }
            case 4 -> {
                addDiceBaseStat(BaseStat.cr, si.getValue(cr, slv));
            }
            case 5 -> {
                addDiceBaseStat(BaseStat.padR, si.getValue(damR, slv));
            }
            case 6 -> {
                addDiceBaseStat(BaseStat.expR, si.getValue(expR, slv));
            }
        }
    }

    public void removeBaseStatByDiceNumber(Char chr, int number) {
        switch (number) {
            case 2 -> {
                chr.removeDiceBaseStat(BaseStat.pddR);
            }
            case 3 -> {
                chr.removeDiceBaseStat(BaseStat.mhpR);
                chr.removeDiceBaseStat(BaseStat.mmpR);
            }
            case 4 -> {
                chr.removeDiceBaseStat(BaseStat.cr);
            }
            case 5 -> {
                chr.removeDiceBaseStat(BaseStat.padR);
            }
            case 6 -> {
                chr.removeDiceBaseStat(BaseStat.expR);
            }
        }
    }

    public void removeBaseStatByOption() {
        if (diceBaseStats.size() <= 0) {
            return;
        }
        for (var base : diceBaseStats.entrySet()) {
            removeBaseStat(base.getKey(), base.getValue());
        }
        diceBaseStats.clear();
    }

    public boolean isTurnOffBackground() {
        return isTurnOffBackground;
    }

    public void setTurnOffBackground(boolean turnOffBackground) {
        isTurnOffBackground = turnOffBackground;
        if (turnOffBackground) {
            getScriptManager().setFieldColour(GreyFieldType.Background, (short) 0, (short) 0, (short) 0, 0);
        } else {
            getScriptManager().setFieldColour(GreyFieldType.Background, (short) 250, (short) 250, (short) 250, 0);
        }
    }

    public List<BuffData> getBuffDataList() {
        return buffDataList;
    }

    public void setBuffDataList(List<BuffData> buffDataList) {
        this.buffDataList = buffDataList;
    }

    public void deleteBuffDataList() {
        for (BuffData buffData : getBuffDataList()) {
            buffData.deleteBuffStatFromSQL();
        }
        getBuffDataList().clear();
    }

    public void updateBuffDataList() {
        TemporaryStatManager tsm = getTemporaryStatManager();
        for (var stat : tsm.getCurrentStats().entrySet()) {
            if (stat == null) {
                continue;
            }
            if (stat.getKey() == IndieEXP || stat.getKey() == ItemUpByItem || stat.getKey() == MesoUpByItem) {
                for (var o : stat.getValue()) {
                    ItemInfo io = ItemData.getItemInfoByID(-o.nReason);
                    if (io == null) {
                        continue;
                    }
                    var specStat = io.getSpecStats();
                    if (specStat == null) {
                        continue;
                    }
                    int time = specStat.getOrDefault(SpecStat.time, 0);
                    //Không thể xảy ra nhưng btw cho chắc.
                    if (time == 0 || o.tTerm == 0) {
                        continue;
                    }
                    int timeLeft = (Util.getCurrentTime() - o.tStart) / 1000;
                    //Buff còn lại dưới 30s thì skip không tính trong system này
                    if (time - timeLeft <= 30) {
                        continue;
                    }
                    switch (-o.nReason) {
                        case 2022531: //Meaning of Clovers
                        case 2023558: //MVP Plus EXP Buff
                        case 2023380: //2x EXP Coupon
                        case 2450140: //2x EXP Coupon
                            BuffData buffData = new BuffData();
                            buffData.setCharID(getId());
                            buffData.setStat(String.valueOf(stat.getKey()));
                            buffData.setItemID(-o.nReason);
                            buffData.setStartTime(o.tStart);
                            buffData.setValue(o.nValue);
                            buffData.setDuration(time);
                            buffData.saveToSQL();
                            break;
                    }
                }
            }
        }
    }

    public void handledBuffDataList() {
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = getTemporaryStatManager();
        Option o = null;
        for (BuffData buffData : getBuffDataList()) {
            if (buffData.getDuration() == 0) {
                continue;
            }
            int timeLeft = (Util.getCurrentTime() - buffData.getStartTime());
            int duration = (buffData.getDuration() - timeLeft) / 1000;
            //Buff còn lại dưới 30s thì skip không tính trong system này
            if (duration <= 30 || buffData.getStartTime() <= 0) {
                continue;
            }
            if (o == null || o.nReason != -buffData.getItemID()) {
                o = new Option();
                o.tTerm = duration;
                o.nReason = -buffData.getItemID();
                o.rOption = -buffData.getItemID();
                o.tOption = duration;
                o.tStart = buffData.getStartTime();
            }
            o.nOption = buffData.getValue();
            o.nValue = buffData.getValue();
            switch (buffData.getStat()) {
                case "IndieEXP" -> newStats.put(IndieEXP, o.deepCopy());
                case "ItemUpByItem" -> newStats.put(ItemUpByItem, o.deepCopy());
                case "MesoUpByItem" -> newStats.put(MesoUpByItem, o.deepCopy());
            }
        }
        if (!getBuffDataList().isEmpty()) {
            tsm.sendStat(newStats);
            deleteBuffDataList();
        }
    }

    public BeautySalon getBeautySalon() {
        return beautySalon;
    }

    public void setBeautySalon(BeautySalon beautySalon) {
        this.beautySalon = beautySalon;
    }

    public DressUpInfo getDressUpInfo() {
        return dressUpInfo;
    }

    public void setDressUpInfo(DressUpInfo dressUpInfo) {
        this.dressUpInfo = dressUpInfo;
    }

    public MonsterCarnivalRanking getMonsterCarnivalRanking() {
        return monsterCarnivalRanking;
    }

    public void setMonsterCarnivalRanking(MonsterCarnivalRanking monsterCarnivalRanking) {
        this.monsterCarnivalRanking = monsterCarnivalRanking;
    }

    public IdleHunting getIdleHunting() {
        return idleHunting;
    }

    public boolean isStartedHunting() {
        return isStartedHunting;
    }

    public void setIdleHunting(IdleHunting idleHunting) {
        this.idleHunting = idleHunting;
    }

    public void setupIdleHunting() {
        long start = System.currentTimeMillis();
        setIdleHunting(new IdleHunting(getId(), getFieldID(), start, getTotalStat(BaseStat.mesoR), getTotalStat(BaseStat.dropR), getTotalStat(BaseStat.expR)));
        write(FieldPacket.clock(ClockPacket.secondsClock(60 * 10)));
        isStartedHunting = true;
        getTimer().addEvent(() -> {
            long current = System.currentTimeMillis();
            idleHunting.setEndTimeMillis(current);
            long total = (current - start) / 1000L;
            idleHunting.setMobKillRatePerSecond(idleHunting.getCurrentMobKilled() / total);
            idleHunting.setExpRatePerSecond(idleHunting.getExpRatePerSecond() / total);
            idleHunting.setMesosRatePerSecond(idleHunting.getCurrentMesosLooted() / total);
            write(FieldPacket.destroy());
            isStartedHunting = false;
            String sb = "#b委託狩獵結果#k\r\n" +
                    "1. 執行地圖：" + StringData.getMapStringById(getFieldID()) + " (" + getFieldID() + ")\r\n" +
                    "2. 每秒擊殺怪物數量：" + idleHunting.getMobKillRatePerSecond() + "（已擊殺怪物總數：" + Util.getNumberFormat(idleHunting.getCurrentMobKilled()) + " 只）\r\n" +
                    "3. 每秒獲得 EXP：" + idleHunting.getExpRatePerSecond() + "（已獲得 EXP 總量：" + Util.getNumberFormat(idleHunting.getCurrentEXPGained()) + " EXP）\r\n" +
                    "4. 每秒拾取楓幣：" + idleHunting.getMesosRatePerSecond() + "（已拾取楓幣總量：" + Util.getNumberFormat(idleHunting.getCurrentMesosLooted()) + " 楓幣）\r\n" +
                    "請退出遊戲以執行該委託記錄。";
            getScriptManager().sendOK(sb, 9010000);
        }, 60*10*1000);
    }

    public String checkBaseStats() {
        StringBuilder sb = new StringBuilder();
        List<BaseStat> sortedList = Arrays.stream(BaseStat.values()).collect(Collectors.toList());
        for (BaseStat bs : sortedList) {
            if (bs.getBaseStatName() != null) {
                int dropRateFromCashDropR = 0;
                for (int i : ItemConstants.CASH_DROP_2X_COUPON) {
                    if (hasItem(i)) {
                        dropRateFromCashDropR = 100;
                        break;
                    }
                }
                if (bs.equals(BaseStat.dropR)) {
                    sb.append(String.format("  | - #e%s: #b%d#k#n\r\n", bs.getBaseStatName(), getTotalStat(bs) + (ServerConfig.DROP_RATE * 100 + dropRateFromCashDropR)));
                } else if (bs.equals(BaseStat.mesoR)) {
                    sb.append(String.format("  | - #e%s: #b%d#k#n\r\n", bs.getBaseStatName(), getTotalStat(bs) + ServerConfig.MESO_RATE * 100 + dropRateFromCashDropR));
                } else if (bs.equals(BaseStat.expR)) {
                    int incExpR = 0;
                    for (Item item : getEquippedInventory().getItems()) {
                        double incBonusExpByItem = ItemConstants.getBonusExpByItem(item.getItemId());
                        if (incBonusExpByItem != 0) {
                            incExpR += (int) (incBonusExpByItem);
                        }
                    }
                    for (int id : ItemConstants.EXP_2X_COUPON) {
                        if (hasItem(id)) {
                            incExpR += 1;
                            break; //so coupons won't stack
                        }
                    }
                    sb.append(String.format("  | - #e%s: #b%d#k#n\r\n", bs.getBaseStatName(), getTotalStat(bs) + ServerConfig.EXP_RATE * 100 + getBonusExpByBurningFieldLevel() + incExpR * 100
                    ));
                } else {
                    sb.append(String.format("  | - #e%s: #b%s#k#n\r\n", bs.getBaseStatName(), Util.getNumberFormat(getTotalStat(bs))));
                }
            }
        }
        return sb.toString();
    }

    public void setVioletEquip(Equip violetEquip) {
        this.violetEquip = violetEquip;
    }

    public Equip getVioletEquip() {
        return violetEquip;
    }

    public void setVioletZeroEquip(Equip violetZeroEquip) {
        this.violetZeroEquip = violetZeroEquip;
    }

    public Equip getVioletZeroEquip() {
        return violetZeroEquip;
    }

    public List<PetVac> getPetVacs() {
        return petVacs;
    }

    public void setPetVacs(List<PetVac> petVacs) {
        this.petVacs = petVacs;
    }

    public void setSellingPetVac(boolean isSellingPetVac) {
        this.isSellingPetVac = isSellingPetVac;
    }

    public boolean isSellingPetVac() {
        return isSellingPetVac;
    }

    public List<PetVac> deletePetVacList(List<PetVac> removedList) {
        long totalMesos = 0;
        int totalPetVacItems = removedList.size();
        for (PetVac petVac : removedList) {
            int itemID = petVac.getItemID();
            long quantity = petVac.getQuantity();
            long cost = 1;
            if (ItemConstants.isThrowingItem(itemID)) {
                quantity = 1;
            }
            if (ItemConstants.isEquip(itemID)) {
                Equip e = ItemData.getEquipById(itemID);
                if (e != null) {
                    cost = e.getPrice();
                }
            } else {
                ItemInfo info = ItemData.getItemInfoByID(itemID);
                if (info != null) {
                    cost = info.getPrice() * quantity;
                }
            }
            totalMesos += cost;
            petVac.deletePetVacToSQL();
        }
        getPetVacs().removeAll(removedList);
        chatMessage(AdminChat, "[寵物自動拾取系統] 你已出售 " +
                Util.getNumberFormat(totalPetVacItems) +
                " 個目前頁面寵物自動拾取背包中的物品。你獲得了 " +
                Util.getNumberFormat(totalMesos) +
                " 楓幣。");
        addMoney(totalMesos);
        setSellingPetVac(false);
        return getPetVacs();
    }

    public static boolean hasPetVac(Char chr) {
        if (Arrays.stream(FieldConstants.BLOCKED_RUNE_MAPS).noneMatch(m -> m == chr.getField().getId())) {
            return (chr.hasItem(ItemConstants.PET_VAC)) && (chr.getInstance() == null || chr.getField().getOnUserEnter().equals("mPark_stageEff"));
        }
        return false;
    }

    public List<Integer> getFilterItems() {
        return filterItems;
    }

    public void spiritBondMax() {
        TemporaryStatManager tsm = getTemporaryStatManager();
        if (tsm.hasStatBySkillId(Shade.SPIRIT_BOND_MAX_2)) {
            EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
            Option indieDamR = tsm.getOptByCTSAndSkill(IndieDamR, Shade.SPIRIT_BOND_MAX_2);
            Option indiePAD = tsm.getOptByCTSAndSkill(IndiePAD, Shade.SPIRIT_BOND_MAX_2);
            Option indieBossDamageR = tsm.getOptByCTSAndSkill(IndieBDR, Shade.SPIRIT_BOND_MAX_2);
            Option indieBooster = tsm.getOptByCTSAndSkill(IndieBooster, Shade.SPIRIT_BOND_MAX_2);
            Option indieIgnoreMobpdpR = tsm.getOptByCTSAndSkill(IndieIgnoreMobpdpR, Shade.SPIRIT_BOND_MAX_2);
            Option indieEmpty = tsm.getOptByCTSAndSkill(IndieEmpty, Shade.SPIRIT_BOND_MAX_2);
            int time = 4000;
            long now = System.currentTimeMillis();
            if (indieEmpty != null) {
                //indieEmpty.tTerm += (indieEmpty.tTerm - ((int) System.currentTimeMillis() - indieEmpty.tStart)) + 4000;
                int cost = (int) now - indieEmpty.tStart;
                int current = indieEmpty.tTerm;
                time = current - cost + 4000;
                time = Math.min(time, 1800000);
                indieEmpty.tTerm = time;
                indieEmpty.tStart = (int) now;
                indieEmpty.startTime = now;
                newStats.put(IndieEmpty, indieEmpty);
            }
            if (indieDamR != null) {
                indieDamR.tTerm = time;
                indieDamR.tStart = (int) now;
                indieDamR.startTime = now;
                newStats.put(IndieDamR, indieDamR);
            }
            if (indiePAD != null) {
                indiePAD.tTerm = time;
                indiePAD.tStart = (int) now;
                indiePAD.startTime = now;
                newStats.put(IndiePAD, indiePAD);
            }
            if (indieBossDamageR != null) {
                indieBossDamageR.tTerm = time;
                indieBossDamageR.tStart = (int) now;
                indieBossDamageR.startTime = now;
                newStats.put(IndieBDR, indieBossDamageR);
            }
            if (indieBooster != null) {
                indieBooster.tTerm = time;
                indieBooster.tStart = (int) now;
                indieBooster.startTime = now;
                newStats.put(IndieBooster, indieBooster);
            }
            if (indieIgnoreMobpdpR != null) {
                indieIgnoreMobpdpR.tTerm = time;
                indieIgnoreMobpdpR.tStart = (int) now;
                indieIgnoreMobpdpR.startTime = now;
                newStats.put(IndieIgnoreMobpdpR, indieIgnoreMobpdpR);
            }
            tsm.sendStat(newStats);
        }
    }

    public long getOnlineTime() {
        return onlineTime;
    }

    public void setOnlineTime(long onlineTime) {
        this.onlineTime = onlineTime;
    }

    public int getOnlineDay() {
        return onlineDay;
    }

    public void setOnlineDay(int onlineDay) {
        this.onlineDay = onlineDay;
    }

    public void encodeChatInfo(OutPacket outPacket, String msg) {
        outPacket.encodeString(getName());
        outPacket.encodeString(msg);
        outPacket.encodeInt(getAccount().getId());
        outPacket.encodeInt(getId());
        outPacket.encodeByte(getAccount().getWorldId());
        outPacket.encodeInt(getId());
        outPacket.encodeInt(0);
        outPacket.encodeString("");
        outPacket.encodeInt(0);
        outPacket.encodeString("");
        outPacket.encodeInt(0);
    }

    public void addAchieventDoneByID(int infoID) {
        addAchievementByID(infoID, -1, 2, 0);
    }

    public void addAchievementByID(int infoID, int missionID, int status, long value) {
        AchievementInfo ai = AchievementInfoData.getAchievementInfoByID(infoID);
        if (ai != null) {
            if (ai.getMissions().size() == 1 && status == 2) {
                missionID = -1;
            }
            AchievementData ad = new AchievementData();
            Account acc = getAccount();
            ad.setAccID(acc.getId());
            ad.setInfoID(infoID);
            ad.setMissionID((byte) missionID);
            ad.setStatus((byte) status);
            ad.setUnlockTime(FileTime.currentTime());
            if (missionID == -1 && status == 2) {
                try {
                    acc.incAPoint(ai.getScore());
                } finally {
                    if (acc.getAchievementPoint() >= 36000 && acc.getAchievementPoint() <= 38730) {
                        // Diamond
                        AchievementRank ar = new AchievementRank();
                        ar.setAccID(acc.getId());
                        ar.setRank(5);
                        ar.setStatus(1);
                        ar.setUnlockTime(FileTime.currentTime());
                        acc.getAchievementRanks().add(ar);
                        //ar.updateAchievementRankToSQL();
                    } else if (acc.getAchievementPoint() >= 30000 && acc.getAchievementPoint() <= 35999) {
                        // Platinum
                        AchievementRank ar = new AchievementRank();
                        ar.setAccID(acc.getId());
                        ar.setRank(4);
                        ar.setStatus(1);
                        ar.setUnlockTime(FileTime.currentTime());
                        acc.getAchievementRanks().add(ar);
                        //ar.updateAchievementRankToSQL();
                    } else if (acc.getAchievementPoint() >= 20000 && acc.getAchievementPoint() <= 29999) {
                        // gold
                        AchievementRank ar = new AchievementRank();
                        ar.setAccID(acc.getId());
                        ar.setRank(3);
                        ar.setStatus(1);
                        ar.setUnlockTime(FileTime.currentTime());
                        acc.getAchievementRanks().add(ar);
                        //ar.updateAchievementRankToSQL();
                    } else if (acc.getAchievementPoint() >= 5000 && acc.getAchievementPoint() <= 19999) {
                        // Silver
                        AchievementRank ar = new AchievementRank(acc.getId(), 2, (byte) 1, FileTime.currentTime());
                        ar.setAccID(acc.getId());
                        ar.setRank(2);
                        ar.setStatus(1);
                        ar.setUnlockTime(FileTime.currentTime());
                        acc.getAchievementRanks().add(ar);
                        //ar.updateAchievementRankToSQL();
                    }
                }
                ad.setMsg("已完成！");
                write(WvsContext.achievementMessage(infoID));
            } else {
                for (AchievementInfo.MissionInfo mi : ai.getMissions()) {
                    if (mi.getId() == missionID) {
                        ad.setMsg(mi.getKey() + "=" + value);
                        break;
                    }
                }
            }
            acc.getAchievementDatas().add(ad);
            write(WvsContext.updateAchievement(ad));

            if (hasAchievementDoneByInfoId(infoID) && missionID != -1) {
                addAchieventDoneByID(infoID);
            }
        }
    }

    public void updateAchievementByID(int infoID, int missionID, long value) {
        AchievementInfo ai = AchievementInfoData.getAchievementInfoByID(infoID);
        if (ai != null) {
            final Set<AchievementData> ads = getAccount().getAchievementDatas();
            for (AchievementData ad : ads) {
                if (ad.getInfoID() == infoID && ad.getMissionID() == missionID) {
                    for (AchievementInfo.MissionInfo mi : ai.getMissions()) {
                        if (mi.getId() == missionID) {
                            if (value == mi.getValue()) {
                                ad.setStatus((byte) 2);
                            } else {
                                ad.setMsg(mi.getKey() + "=" + value);
                            }
                            write(WvsContext.updateAchievement(ad));
                            break;
                        }
                    }
                }
            }
        }
    }

    public boolean hasAchievementDoneByInfoId(int infoID) {
        return hasAchiementByInfoIdAndMissionID(infoID, -1);
    }

    public boolean hasAchiementByInfoIdAndMissionID(int infoID) {
        return hasAchiementByInfoIdAndMissionID(infoID, 0);
    }

    public boolean hasAchiementByInfoIdAndMissionID(int infoID, int missionID) {
        AchievementInfo ai = AchievementInfoData.getAchievementInfoByID(infoID);
        if (ai != null) {
            final Set<AchievementData> ads = getAccount().getAchievementDatas();
            for (AchievementData ad : ads) {
                if (ad.getInfoID() == infoID && ad.getMissionID() == missionID) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public void handleDoneAchievementByInfo(int infoID) {
        AchievementInfo ai = AchievementInfoData.getAchievementInfoByID(infoID);
        if (ai != null) {
            int count = 0;
            final Set<AchievementData> ads = getAccount().getAchievementDatas();
            for (AchievementData ad : ads) {
                if (ad.getInfoID() == infoID && ad.getStatus() == 2) {
                    count += 1;
                }
            }
            if (count == ai.getMissions().size()) {
                addAchieventDoneByID(infoID);
            }
        }
    }

    public long getCurrentValueAchiementByInfoIDAndMissionID(int infoID, int missionID) {
        AchievementInfo ai = AchievementInfoData.getAchievementInfoByID(infoID);
        if (ai != null) {
            final Set<AchievementData> ads = getAccount().getAchievementDatas();
            for (AchievementData ad : ads) {
                if (ad.getInfoID() == infoID && ad.getMissionID() == missionID) {
                    return Long.parseLong(ad.getValue());
                }
            }
            return 0;
        }
        return 0;
    }

    public void initLegionRaid() {
        if (!hasQuest(QuestConstants.UNION_RAID)) {
            createQuestWithQRValue(QuestConstants.UNION_RAID, "mod=88863598066;CID=null;lastTime="+FileTime.currentTime().toYYMMDDHHMMSS()+";damage=0;cName="+getName()+";coin=0;index=0");
        }
        if (unionRaidDmg != null) {
            unionRaidDmg.cancel(true);
        }
        ScriptManagerImpl sm = getScriptManager();
        int mobTemplateID = 9833105;
        long mobMaxHP = 1000000000000L;
        int mobTemplateID2 = mobTemplateID + 100;
        long mobMaxHP2 = 250000000000L;
        sm.spawnLinkMobsetHP(mobTemplateID, 2320, 17, mobMaxHP, mobMaxHP);
        sm.spawnLinkMobsetHP(mobTemplateID2, 2320, 17, mobMaxHP2, mobMaxHP2);
        write(UnionPacket.setUnionRaidScore(0));
        write(UnionPacket.showUnionRaidHpUI(mobTemplateID, mobMaxHP2, mobMaxHP2, mobTemplateID2, mobMaxHP, mobMaxHP));
        write(UnionPacket.setUnionRaidCoinNum(0, true));
        ScheduledFuture<?> sf = getTimer().addFixedRateEvent(() -> {
            if (getField().getId() != 921172000 && getField().getId() != 921172100) {
                if (unionRaidDmg != null) {
                    unionRaidDmg.cancel(true);
                }
                unionRaidDmg = null;
                return;
            }
            long totalDmg = 0L;
            long bonusDmg = 0L;
            int mobID = 0;
            long curHP = 0L;
            long curHP2 = 0L;
            boolean isImmune = Util.succeedProp(5);
            for (Mob mob : getField().getMobs()) {
                if (mob.getTemplateId() >= 9833101 && mob.getTemplateId() <= 9833105) {
                    for (Map.Entry<Char, Long> damage : mob.getDamageDone().entrySet()) {
                        totalDmg += damage.getValue();
                    }
                    Union union = getUnion();
                    MobTemporaryStat mts = mob.getTemporaryStat();
                    if (!mts.hasCurrentMobStat(MobStat.PowerImmune)) {
                        long oldHp = mob.getHp();
                        if (oldHp > 0) {
                            UnionBoard unionBoard = union.getBoardByPreset(getActiveUnionPreset());
                            if (unionBoard != null) {
                                bonusDmg = unionBoard.calculateTotalUnionPower();
                            } else {
                                bonusDmg = 0;
                            }
                            if (bonusDmg > 0) {
                                bonusDmg = Util.getRandom((long) (bonusDmg * 0.9), (long) (bonusDmg * 1.05));
                            }
                            mob.addDamage(this, bonusDmg);
                            totalDmg += bonusDmg;
                            long newHp = oldHp - bonusDmg;
                            mob.setHp(newHp);
                            if (isImmune && newHp > 0) {
                                Option o = new Option();
                                if (!mts.hasCurrentMobStat(MobStat.PowerImmune)) {
                                    o.nOption = 1;
                                    o.slv = 13;
                                    o.tOption = 5;
                                    o.rOption = MobSkillID.Invincible.getVal();
                                    mts.addMobSkillOptions(mob, MobStat.PowerImmune, o);
                                }
                            }
                        }
                    }
                    mobID = mob.getTemplateId();
                    curHP = mob.getHp();
                } else if (mob.getTemplateId() >= 9833201 && mob.getTemplateId() <= 9833205) {
                    for (Map.Entry<Char, Long> damage : mob.getDamageDone().entrySet()) {
                        totalDmg += damage.getValue();
                    }
                    curHP2 = mob.getHp();
                    if (isImmune && curHP2 > 0) {
                        Option o = new Option();
                        MobTemporaryStat mts = mob.getTemporaryStat();
                        if (!mts.hasCurrentMobStat(MobStat.PowerImmune)) {
                            o.nOption = 1;
                            o.slv = 13;
                            o.tOption = 5;
                            o.rOption = MobSkillID.Invincible.getVal();
                            mts.addMobSkillOptions(mob, MobStat.PowerImmune, o);
                        }
                    }
                }
            }
            int coin = sm.getUnionCoinByRaid();
            int index = Integer.parseInt(getQRValueByKey(QuestConstants.UNION_RAID, "index"));
            long x = 100000000000L;
            if (totalDmg / x != 0L) {
                for (int i = 1; i <= 35; i++) {
                    if ((totalDmg >= (x * i)) && (totalDmg <= (x * (i + 1))) && index < i) {
                        coin += 1;
                        index += 1;
                        write(UnionPacket.setUnionRaidCoinNum(0, false));
                        write(UnionPacket.setUnionRaidCoinNum(coin, true));
                        setQRValueByKey(QuestConstants.UNION_RAID, "coin", "" + coin);
                        setQRValueByKey(QuestConstants.UNION_RAID, "damage", "" + totalDmg);
                        setQRValueByKey(QuestConstants.UNION_RAID, "index", "" + index);
                        break;
                    }
                }
            }
            int mobID2 = mobID + 100;
            if (curHP <= 0 && curHP2 <= 0) {
                try {
                    field.removeMobsByTemplateID(mobID2);
                    field.removeMobsByTemplateID(mobID);
                } catch (Exception e) {
                    DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                }
                if (mobID == 9833105) {
                    if (getField().getId() == 921172000) {
                        sm.warpInstanceOut(this, 921172200);
                        return;
                    } else if (getField().getId() == 921172100) {
                        sm.warpInstanceOut(this, 921172201);
                        return;
                    }
                } else {
                    mobID += 1;
                    mobID2 += 1;
                    curHP = 10000000000000L;
                    curHP2 = 2500000000000L;
                    totalDmg = 0L;
                    setQRValueByKey(QuestConstants.UNION_RAID, "mod", "" + mobID);
                    setQRValueByKey(QuestConstants.UNION_RAID, "index", "" + 0);
                    write(UnionPacket.setUnionRaidScore(0));
                    sm.spawnLinkMobsetHP(mobID, 2320, 17, 10000000000000L, 10000000000000L);
                    sm.spawnLinkMobsetHP(mobID2, 2320, 17, 2500000000000L, 2500000000000L);
                }
            }
            write(UnionPacket.showUnionRaidHpUI(mobID2, Math.max(curHP2, 0), 2500000000000L, mobID, Math.max(curHP, 0), 10000000000000L));
            write(UnionPacket.setUnionRaidScore(totalDmg));
        }, 0, 1000, false);
        unionRaidDmg = sf;
        GlobalTimerManager.addCharTimer(this.getId(), sf);
    }

    public UnionMember createUnionMember() {
        return new UnionMember(1, this, null);
    }

    public Union getUnion() {
        return getAccount().getUnion();
    }

    public void incrementUnionRank() {
        Union union = getUnion();
        int curUnionRank = union.getUnionRank();
        if (curUnionRank == 0) {
            union.setUnionRank(101);
        } else {
            if (curUnionRank < 105
                    || (curUnionRank >= 201 && curUnionRank < 205)
                    || (curUnionRank >= 301 && curUnionRank < 305)
                    || (curUnionRank >= 401 && curUnionRank < 405)) {
                union.setUnionRank(curUnionRank + 1);
            } else if (curUnionRank == 105) {
                union.setUnionRank(201);
            } else if (curUnionRank == 205) {
                union.setUnionRank(301);
            } else if (curUnionRank == 305) {
                union.setUnionRank(401);
            }
        }
        AccountQuest quest = getAccount().getQuestById(QuestConstants.UNION_RANK);
        quest.setProperty("rank", union.getUnionRank());
        write(WvsContext.questRecordExMessage(quest));
    }

    public int getUnionMaxCoin() {
        int rank = Integer.parseInt(getQRValueByKey(QuestConstants.UNION_RANK, "rank"));
        if (rank >= 101 && rank <= 105) {
            return 200;
        }
        if (rank >= 201 && rank <= 205) {
            return 300;
        }
        if (rank >= 301 && rank <= 305) {
            return 500;
        }
        if (rank >= 401 && rank <= 405) {
            return 700;
        }
        return 0;
    }

    public int getActiveUnionPreset() {
        Quest quest = getQuestById(QuestConstants.UNION_PRESET);
        if (quest == null) {
            quest = getOrCreateQuestById(QuestConstants.UNION_PRESET);
            quest.setProperty("presetNo", 0);
            write(WvsContext.questRecordExMessage(quest));
            return 0;
        }
        return quest.getIntProperty("presetNo");
    }

    public void setActiveUnionPreset(int preset) {
        Quest quest = getOrCreateQuestById(QuestConstants.UNION_PRESET);
        quest.setProperty("presetNo", preset + "");
        write(WvsContext.questRecordExMessage(quest));
    }

    public Int2ObjectMap<ForceAtom> getForceAtoms() {
        var m = forceAtoms;
        if (m == null) {
            forceAtoms = m = new Int2ObjectOpenHashMap<>();
        }
        return m;
    }

    public Int2ObjectMap<SecondAtom> getSecondAtoms() {
        var m = secondAtoms;
        if (m == null) {
            secondAtoms = m = new Int2ObjectOpenHashMap<>();
        }
        return m;
    }

    public Int2ObjectMap<JupiterThunder> getJupiterThunders() {
        var m = jupiterThunders;
        if (m == null) {
            jupiterThunders = m = new Int2ObjectOpenHashMap<>();
        }
        return m;
    }

    public void createJupiterThunder(JupiterThunder jupiterThunder) {
        addJupiterThunder(jupiterThunder);
        write(UserPacket.jupiterThunderCreated(this, jupiterThunder));
    }

    public void addJupiterThunder(JupiterThunder jupiterThunder) {
        getJupiterThunders().put(jupiterThunder.getObjectId(), jupiterThunder);
    }

    public void removeJupiterThunder(int objId) {
        getJupiterThunders().remove(objId);
    }

    public JupiterThunder getJupiterThunderById(int objId) {
        return getJupiterThunders().getOrDefault(objId, null);
    }

    public void clearJupiterThunderMap() {
        getJupiterThunders().clear();
        jupiterThunderCounter = 1000;
    }

    public int getNewJupiterThunderId() {
        return jupiterThunderCounter++;
    }

    public void addForceAtom(ForceAtom forceAtom) {
        for (Integer k : forceAtom.getKeys()) {
            getForceAtoms().put(k, forceAtom);
        }
    }

    public void addForceAtomByKey(int faKey, ForceAtom forceAtom) {
        getForceAtoms().put(faKey, forceAtom);
    }

    public void removeForceAtomByKey(int key) {
        getForceAtoms().remove(key);
    }

    public ForceAtom getForceAtomByKey(int key) {
        return getForceAtoms().get(key);
    }

    public void recreateforceAtom(int faKey, ForceAtom forceAtom) {
        addForceAtomByKey(faKey, forceAtom);
        ForceAtomInfo fai = forceAtom.getFaiByKey(faKey);
        ForceAtom fa = new ForceAtom(forceAtom);
        fa.setFaiList(Collections.singletonList(fai));
        getField().broadcast(FieldPacket.createForceAtom(fa));
    }

    public void createForceAtom(ForceAtom forceAtom) {
        createForceAtom(forceAtom, true);
    }

    public void createForceAtom(ForceAtom forceAtom, boolean broadcastToField) {
        if (broadcastToField) {
            getField().broadcast(FieldPacket.createForceAtom(forceAtom));
        } else {
            write(FieldPacket.createForceAtom(forceAtom));
        }
        addForceAtom(forceAtom);
    }

    public void createSecondAtom(SecondAtom secondAtom) {
        createSecondAtom(List.of(secondAtom), true);
    }

    public void createSecondAtom(List<SecondAtom> secondAtoms) {
        createSecondAtom(secondAtoms, true);
    }

    public void createSecondAtom(List<SecondAtom> secondAtoms, boolean broadcastToField) {
        if (broadcastToField) {
            getField().broadcast(SecondAtomPacket.createSecondAtoms(this.getId(), secondAtoms));
        } else {
            write(SecondAtomPacket.createSecondAtoms(this.getId(), secondAtoms));
        }
        addSecondAtom(secondAtoms);
    }

    public SecondAtom getSecondAtomById(int objectId) {
        return getSecondAtoms().getOrDefault(objectId, null);
    }

    public void addSecondAtom(List<SecondAtom> secondAtoms) {
        for (SecondAtom sa : secondAtoms) {
            getSecondAtoms().putIfAbsent(sa.getObjectID(), sa);
        }
    }

    public void removeSecondAtom(int objectId) {
        SecondAtom sa = getSecondAtomById(objectId);
        if (sa != null) {
            sa.setExpire(0);
            removeSecondAtomInternal(sa);
        }
    }

    public void removeSecondAtom(SecondAtom sa) {
        if (sa != null) {
            sa.setExpire(0);
            removeSecondAtomInternal(sa);
        }
    }

    public void removeSecondAtomInternal(SecondAtom sa) {
        getField().broadcast(SecondAtomPacket.removeSecondAtom(this, sa.getObjectID()));
    }

    public void setSecondAtomKeyCounter(int secondAtomKeyCounter) {
        this.secondAtomKeyCounter = secondAtomKeyCounter;
    }

    public int getNewForceAtomKey() {
        return this.forceAtomKeyCounter++;
    }

    public int getNewSecondAtomKey() {
        if (this.secondAtomKeyCounter == Integer.MAX_VALUE) {
            this.secondAtomKeyCounter = 0;
        }
        return this.secondAtomKeyCounter++;
    }

    public ScheduledFuture<?> getKeyDownTimer() {
        return keyDownTimer;
    }

    public void setKeyDownTimer(ScheduledFuture<?> keyDownTimer) {
        this.keyDownTimer = keyDownTimer;
    }

    public void cancelKeyDownTimer() {
        if (getKeyDownTimer() != null && !getKeyDownTimer().isDone()) {
            getKeyDownTimer().cancel(true);
        }
    }

    public int getLastBossTemplateID() {
        return lastBossTemplateID;
    }

    public void setLastBossTemplateID(int lastBossTemplateID) {
        this.lastBossTemplateID = lastBossTemplateID;
    }

    public int getLucidMode() {
        return lucidMode;
    }

    public void setLucidMode(final int lucidMode) {
        this.lucidMode = lucidMode;
    }

    public int getMoonGauge() {
        return moonGauge;
    }

    public void setMoonGauge(final int moonGauge) {
        this.moonGauge = moonGauge;
    }

    public int getClearSpiderWeb() {
        return clearSpiderWeb;
    }

    public void setClearSpiderWeb(int clearSpiderWeb) {
        this.clearSpiderWeb = clearSpiderWeb;
    }

    public boolean isBot() {
        return isBot;
    }

    public void setBot(boolean isBot) {
        this.isBot = isBot;
    }

    public PartyQuestManager getPartyQuestManager() {
        return partyQuestManager;
    }

    public void startPartyQuest(PartyQuestType partyQuestType, byte typeEnter) {
        getPartyQuestManager().start(this, partyQuestType, typeEnter);
    }

    public void checkIngredients() {
        PartyQuest partyQuest = getPartyQuestManager().getPartyQuest();
        if (partyQuest instanceof HungryMuto hungryMuto) {
            hungryMuto.check(this);
        }
    }

    public void eventOnFlower(int parentID) {
        PartyQuest partyQuest = getPartyQuestManager().getPartyQuest();
        if (partyQuest instanceof MoonBunny moonBunny) {
            moonBunny.event(parentID);
        }
    }

    public int getBonusExpByBurningFieldLevel() {
        return getBurningFieldLevel() * FieldConstants.BURNING_FIELD_BONUS_EXP_MULTIPLIER_PER_LEVEL;
    }

    public void showBurningLevel() {
        if (getBurningFieldLevel() > 0) {
            // 2026-09-29: Traditional Chinese; the ExtraBold font has no CJK glyphs, so use the default font.
            String string = "#fs26#          燃燒場地 第 " + getBurningFieldLevel() + " 階段：額外經驗值 +" + getBonusExpByBurningFieldLevel() + "%          ";
            Effect effect = Effect.createFieldTextEffect(string, 50, 2000, 4,
                    new Position(0, -200), 1, 4, TextEffectType.BurningField, 0, 0);
            write(UserPacket.effect(effect));
        }
    }

    public void increaseBurningLevel() {
        int nextLevel = getBurningFieldLevel() + 1;
        setBurningFieldLevel(nextLevel);
    }

    public void decreaseBurningLevel() {
        setBurningFieldLevel(getBurningFieldLevel() - 1);
    }

    public int getBurningFieldLevel() {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3 && getField() != null
                && getField().getTms282Burning().active()) {
            return getField().getTms282Burning().level(System.currentTimeMillis(), !getField().getChars().isEmpty());
        }
        return burningFieldLevel;
    }

    public void setBurningFieldLevel(int burningFieldLevel) {
        this.burningFieldLevel = burningFieldLevel;
    }

    public void startBurningFieldTimer(Field field) {
        if (field != null && field.getMobGens().size() > 0 && field.getMobs().stream().mapToInt(m -> m.getForcedMobStat().getLevel()).min().orElse(0) >= FieldConstants.BURNING_FIELD_MIN_MOB_LEVEL) {
            if (this.burningFieldTimer != null) {
                this.burningFieldTimer.cancel(true);
                this.burningFieldTimer = null;
            }
            ScheduledFuture<?> sf = getTimer().addFixedRateEvent(() -> {
                changeBurningLevel();
            }, FieldConstants.BURNING_FIELD_TIMER, FieldConstants.BURNING_FIELD_TIMER, TimeUnit.MINUTES, false);
            this.burningFieldTimer = sf;
            GlobalTimerManager.addCharTimer(this.getId(), sf);
        } else {
            if (this.burningFieldTimer != null) {
                this.burningFieldTimer.cancel(true);
                this.burningFieldTimer = null;
            }
        }
    }

    public void changeBurningLevel() {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3 && getField() != null
                && getField().getTms282Burning().active()) {
            showBurningLevel(); // Shared state advances once per field interval, not once per resident character.
            AchievementHandler.handleBurningField(this, getBurningFieldLevel());
            return;
        }
        boolean showMessage = getBurningFieldLevel() > 0;
        //If there are players on the map,  decrease the level  else  increase the level
        if (getBurningFieldLevel() == FieldConstants.BURNING_FIELD_MAX_LEVEL) {
            decreaseBurningLevel();
            showMessage = true;
        } else if (getBurningFieldLevel() < FieldConstants.BURNING_FIELD_MAX_LEVEL) {
            increaseBurningLevel();
            showMessage = true;
        }
        if (showMessage) {
            showBurningLevel();
        }
        AchievementHandler.handleBurningField(this, getBurningFieldLevel());
    }

    public DailyCoin getDailyCoin() {
        return dailyCoin;
    }

    public void setDailyCoin(DailyCoin dailyCoin) {
        this.dailyCoin = dailyCoin;
    }

    public boolean isSubZeroHunt() {
        return isSubZeroHunt;
    }

    public void setSubZeroHunt(boolean isSubZeroHunt) {
        this.isSubZeroHunt = isSubZeroHunt;
    }

    public Timer getTimer() {
        return Server.get().getCharTimer();
    }

    public void maxSkills() {
        List<Skill> list = new ArrayList<>();
        Set<Short> jobs = new HashSet<>();
        short job = getJob();
        if (job == 434) {
            jobs.add(job);
            jobs.add((short) (job - 1));
            jobs.add((short) (job - 2));
            jobs.add((short) (job - 3));
            jobs.add((short) (job - 4));
        } else if (job == 433) {
            jobs.add(job);
            jobs.add((short) (job - 1));
            jobs.add((short) (job - 2));
            jobs.add((short) (job - 3));
        } else if ((job % 100 >= 12 && job % 100 <= 32) || job % 100 == 72 || job == 2218) {
            jobs.add(job);
            jobs.add((short) (job == 2218 ? 2214 : (job - 1)));
            jobs.add((short) (job == 2218 ? 2212 : (job - 2)));
            jobs.add((short) (job == 2218 ? 2210 : (job - (job % 100))));
        } else if ((job % 100 >= 11 && job % 100 <= 31) || job % 100 == 71 || job == 2214) {
            jobs.add(job);
            jobs.add((short) (job == 2214 ? 2212 : (job - 1)));
            jobs.add((short) (job == 2214 ? 2210 : (job - (job % 100))));
        } else if (job % 100 == 10 || job % 100 == 70 || job == 2212) {
            jobs.add(job);
            jobs.add((short) (job == 2212 ? 2210 : job % 100 == 70 ? 508 : job - (job % 100)));
        } else {
            jobs.add(job);
        }
        for (short j : jobs) {
            for (Skill skill : SkillData.getSkillsByJob(j)) {
                byte maxLevel = (byte) skill.getMaxLevel();
                skill.setCurrentLevel(maxLevel);
                skill.setMasterLevel(maxLevel);
                list.add(skill);
                addSkill(skill);
            }
            if (list.size() > 0) {
                write(WvsContext.changeSkillRecordResult(list, true, false, false));
            }
        }
        chatMessage("你的技能已提升至最高等級。");
    }

    public void cancelTimers() {
        if (comboKillResetTimer != null) {
            comboKillResetTimer.cancel(true);
        }
        if (timeLimitTimer != null) {
            timeLimitTimer.cancel(true);
        }
        if (dropFatigueTimer != null) {
            dropFatigueTimer.cancel(true);
        }
        if (runeRecoveryTimer != null) {
            runeRecoveryTimer.cancel(true);
        }
        if (willGaugeTimer != null) {
            willGaugeTimer.cancel(true);
        }
        if (updateTimer != null) {
            updateTimer.cancel(true);
        }
        if (keyDownTimer != null) {
            keyDownTimer.cancel(true);
        }
        if (burningFieldTimer != null) {
            burningFieldTimer.cancel(true);
        }
        if (starDustTimer != null) {
            starDustTimer.cancel(true);
        }
        if (bonusTimer != null) {
            bonusTimer.cancel(true);
        }
        if (scoreTimer != null) {
            scoreTimer.cancel(true);
        }
        if (startEventTimer != null) {
            startEventTimer.cancel(true);
        }
        if (endEventTimer != null) {
            endEventTimer.cancel(true);
        }
        if (unionRaidDmg != null) {
            unionRaidDmg.cancel(true);
        }
        if (deathPenaltyTimer != null) {
            deathPenaltyTimer.cancel(true);
        }
        if (getDreamBreaker() != null) {
            getDreamBreaker().cancelTimer();
        }
        if (getJobHandler() != null) {
            getJobHandler().handleCancelTimer(this);
        }
        if (getScriptManager() != null) {
            getScriptManager().cancelTimer();
        }
        if (getTemporaryStatManager() != null) {
            getTemporaryStatManager().removeAllStats();
        }
        GlobalTimerManager.removeAllCharTimers(this.getId());
    }

    /**
     *  @TODO QuestManager
      */
    public Map<Integer, Quest> getQuests() {
        return quests;
    }

    public void setQuests(Map<Integer, Quest> quests) {
        this.quests = quests;
    }

    private static boolean hasQRValue(Object quest) {
        return (quest instanceof Quest q && q.getQRValue() != null && !q.getQRValue().isEmpty())
            || (quest instanceof AccountQuest aq && aq.getQRValue() != null && !aq.getQRValue().isEmpty());
    }

    public List<Quest> getCompletedQuests() {
        var quests = getQuests().values();
        List<Quest> out = new ArrayList<>(quests.size());
        for (Quest q : quests) {
            if (q.getQRKey() >= 1000 && q.getStatus() == Completed) {
                out.add(q);
            }
        }
        return out;
    }

    public List<Quest> getQuestsInProgress() {
        var quests = getQuests().values();
        List<Quest> out = new ArrayList<>(quests.size());
        for (Quest q : quests) {
            if (q.getQRKey() >= 1000 && q.getStatus() == Started) {
                out.add(q);
            }
        }
        return out;
    }

    public List<Quest> getQuestsEx() {
        var quests = getQuests().values();
        List<Quest> out = new ArrayList<>(quests.size());
        for (Quest q : quests) {
            if (q.getQRKey() >= 1000 && hasQRValue(q)) {
                out.add(q);
            }
        }
        return out;
    }

    public List<AccountQuest> getWorldShareQuests() {
        var accountQuests = getAccount().getQuests().values();
        List<AccountQuest> out = accountQuests.stream().filter(q -> q.getQRKey() >= 0).collect(Collectors.toCollection(ArrayList::new));
        for (var q : accountQuests) {
            if (q.getQRKey() >= 0 && q.getQRKey() < 1000 && hasQRValue(q)) {
                out.add(q);
            }
        }
        return out;
    }

    public boolean hasQuest(int questID) {
        return hasQuestInProgress(questID);
    }
    /**
     * Checks whether or not this Char has a given quest in progress.
     *
     * @param questID The quest ID of the requested quest.
     * @return Whether or not this char is in progress with the quest.
     */
    public boolean hasQuestInProgress(int questID) {
        if (QuestConstants.isAccountQuest(questID)) {
            AccountQuest quest = getAccount().getQuestById(questID);
            return quest != null && quest.getStatus() == Started;
        } else {
            Quest quest = getQuestById(questID);
            return quest != null && quest.getStatus() == Started;
        }
    }

    /**
     * Checks if a quest has been completed, i.e. the status is COMPLETE.
     *
     * @param questID the quest's id to check
     * @return quest completeness
     */
    public boolean hasQuestCompleted(int questID) {
        if (QuestConstants.isAccountQuest(questID)) {
            AccountQuest quest = getAccount().getQuestById(questID);
            return quest != null && quest.getStatus() == Completed;
        } else {
            Quest quest = getQuestById(questID);
            return quest != null && quest.getStatus() == Completed;
        }
    }

    public void addQuest(Quest quest) {
        addQuest(quest, true);
    }

    public void addCustomQuest(Quest quest) {
        addQuest(quest, false);
    }

    public void createQuestWithQRValue(int questId, String qrValue, boolean ex) {
        createQuestWithQRValue(this, questId, qrValue, ex);
    }

    public void createQuestWithQRValue(int questId, String qrValue) {
        createQuestWithQRValue(this, questId, qrValue, true);
    }

    public void createQuestWithQRValue(Char chr, int questId, String qrValue, boolean ex) {
        if (QuestConstants.isAccountQuest(questId)) {
            AccountQuest quest = chr.getAccount().getQuestById(questId);
            if (quest == null) {
                quest = QuestData.createAccQuestFromId(questId, chr.getAccount().getId());
                quest.setQrValue(qrValue);
                chr.getAccount().addCustomQuest(quest);
                updateQRValue(questId, ex);
            } else {
                quest.setQrValue(qrValue);
                updateQRValue(questId, ex);
            }
        } else {
            Quest quest = chr.getQuestById(questId);
            if (quest == null) {
                quest = QuestData.createQuestFromId(questId, chr.getId());
                quest.setQrValue(qrValue);
                chr.addCustomQuest(quest);
                updateQRValue(questId, ex);
            } else {
                quest.setQrValue(qrValue);
                updateQRValue(questId, ex);
            }
        }
        dbgChatMsg("任務ID " + questId + " : " + qrValue);
    }

    public void deleteQuest(int questId) {
        removeQuest(questId);
    }

    public String getQRValue(int questId) {
        if (QuestConstants.isAccountQuest(questId)) {
            AccountQuest quest = getAccount().getQuestById(questId);
            if (quest == null) {
                return "Quest is Null";
            }
            return quest.getQRValue();
        } else {
            Quest quest = getQuestById(questId);
            if (quest == null) {
                return "Quest is Null";
            }
            return quest.getQRValue();
        }
    }

    public void setQRValue(int questId, String qrValue) {
        setQRValue(questId, qrValue, true);
    }

    public void setQRValue(int questId, String qrValue, boolean ex) {
        if (QuestConstants.isAccountQuest(questId)) {
            AccountQuest quest = getAccount().getQuestById(questId);
            quest.setQrValue(qrValue);
            updateQRValue(questId, ex);
        } else {
            Quest quest = getQuestById(questId);
            quest.setQrValue(qrValue);
            updateQRValue(questId, ex);
        }
    }

    public void addQRValue(int questId, String qrValue) {
        addQRValue(questId, qrValue, true);
    }

    public void addQRValue(int questId, String qrValue, boolean ex) {
        String qrVal = getQRValue(questId);
        if (qrVal.equals("") || qrVal.equals("Quest is Null")) {
            createQuestWithQRValue(questId, qrValue);
            return;
        }
        setQRValue(questId, qrValue + ";" + qrVal);
        updateQRValue(questId, ex);
    }

    public void updateQRValue(int questId, boolean ex) {
        if (QuestConstants.isAccountQuest(questId)) {
            AccountQuest quest = getAccount().getQuestById(questId);
            if (quest == null) {
                System.out.printf("The user does not have the quest %d.", questId);
                return;
            }
            if (ex) {
                write(WvsContext.questRecordExMessage(quest));
            } else {
                write(WvsContext.questRecordMessage(quest));
            }
            write(WvsContext.questWorldShareMessage(quest));
            if (hasQuest(QuestConstants.UNION_ARTIFACT)) {
                UnionArtifact.updateSpecialQuestQRMission(this, questId, quest.getQRValue());
            }
        } else {
            Quest quest = getQuestById(questId);
            if (quest == null) {
                System.out.printf("The user does not have the quest %d.", questId);
                return;
            }
            if (ex) {
                write(WvsContext.questRecordExMessage(quest));
            } else {
                write(WvsContext.questRecordMessage(quest));
            }
            if (hasQuest(QuestConstants.UNION_ARTIFACT)) {
                UnionArtifact.updateSpecialQuestQRMission(this, questId, quest.getQRValue());
            }
        }
    }

    public String getQRValueByKey(int questId, String key) {
        if (QuestConstants.isAccountQuest(questId)) {
            AccountQuest quest = getAccount().getQuestById(questId);
            if (quest == null) {
                return null;
            }
            quest.convertQRValueToProperties();
            return quest.getProperty(key);
        } else {
            Quest quest = getQuestById(questId);
            if (quest == null) {
                return null;
            }
            if (questId == QuestConstants.CRYSTAL_WEEKLY) {
                quest.convertSpecialQRValueToProperties();
            } else {
                quest.convertQRValueToProperties();
            }
            return quest.getProperty(key);
        }
    }

    public void setQRValueByKey(int questId, int key, int value) {
        setQRValueByKey(questId, String.valueOf(key), String.valueOf(value));
    }

    public void setQRValueByKey(int questId, int key, String value) {
        setQRValueByKey(questId, String.valueOf(key), value);
    }

    public void setQRValueByKey(int questId, String key, int value) {
        setQRValueByKey(questId, key, String.valueOf(value));
    }

    public void setQRValueByKey(int questId, String key, String value) {
        if (QuestConstants.isAccountQuest(questId)) {
            AccountQuest quest = getAccount().getQuestById(questId);
            if (quest == null) {
                createQuestWithQRValue(questId, key + "=" + value, true);
                return;
            }
            if (getQRValueByKey(questId, key) != null) {
                quest.setProperty(key, value);
                updateQRValue(questId, true);
            } else {
                String qrValue = quest.getQRValue();
                qrValue += ";" + key + "=" + value;
                quest.setQrValue(qrValue);
                updateQRValue(questId, true);
            }
        } else {
            Quest quest = getQuestById(questId);
            if (quest == null) {
                createQuestWithQRValue(questId, key + "=" + value, true);
                return;
            }
            if (getQRValueByKey(questId, key) != null) {
                quest.setProperty(key, value);
                updateQRValue(questId, true);
            } else {
                String qrValue = quest.getQRValue();
                qrValue += ";" + key + "=" + value;
                quest.setQrValue(qrValue);
                updateQRValue(questId, true);
            }
        }
        dbgChatMsg("任務ID " + questId + " 鍵 : " + key + " ，值 : " + value);
    }

    public String getQRValueByKey(String data, long cost, int bossID) {
        String key = String.valueOf(cost);
        String val = String.valueOf(bossID);

        if (data == null || data.isEmpty()) {
            return key + "=" + val;
        }

        String[] parts = data.split("\\|");
        for (int i = 0; i < parts.length; i++) {
            String[] kv = parts[i].split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                parts[i] = key + "=" + val; // update
                return String.join("|", parts);
            }
        }
        return data + "|" + key + "=" + val; // append
    }

    /**
     * Adds a new {@link Quest} to this QuestManager's quests. If it already exists, doesn't do anything.
     * Use {@link #replaceQuest(Quest)} if a given quest should be overridden.
     *
     * @param quest            The Quest to add.
     * @param addRewardsFromWz Whether or not to addRewards from the WzFiles
     */
    private void addQuest(Quest quest, boolean addRewardsFromWz) {
        if (!getQuests().containsKey(quest.getQRKey())) {
            getQuests().put(quest.getQRKey(), quest);
            write(WvsContext.questRecordMessage(quest));
            if (quest.getStatus() == Completed) {
                if (getUser().getAccountType().getVal() != AccountType.Player.getVal()) {
                    chatMessage(SystemNotice, "[資訊] 已完成任務 " + quest.getQRKey());
                }
                if (getGuild() != null) {
                    getGuild().addContributionToChar(this, GuildConstants.CONTRIBUTION_PER_QUEST);
                }
            } else {
                if (getUser().getAccountType().getVal() != AccountType.Player.getVal()) {
                    chatMessage(SystemNotice, "[資訊] 已接受任務 " + quest.getQRKey());
                }
                if (addRewardsFromWz) {
                    QuestInfo qi = QuestData.getQuestInfoById(quest.getQRKey());
                    if (qi != null) {
                        for (QuestReward qr : qi.getQuestRewards()) {
                            if (qr instanceof QuestItemReward qir && qir.getStatus() == 0) {
                                qir.giveReward(this, qi.getQuestName());
                            } else if (qr instanceof QuestBuffItemReward && ((QuestBuffItemReward) qr).getStatus() == 0) {
                                qr.giveReward(this);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds a new {@link Quest} to this QuestManager's quest. If it already exists, overrides the old one with the new one.
     *
     * @param quest The Quest to add/replace.
     */
    public void replaceQuest(Quest quest) {
        getQuests().put(quest.getQRKey(), quest);
        write(WvsContext.questRecordMessage(quest));
    }

    /**
     * Returns whether or not a {@link Char} can start a given quest.
     *
     * @param questID The Quest's ID to check.
     * @return Whether or not the Char can start the quest.
     */
    public boolean canStartQuest(int questID) {
        QuestInfo qi = QuestData.getQuestInfoById(questID);
        if (qi == null) {
            return !QuestShortcut.enabled();
        }
        if (QuestShortcut.enabled() && hasQuestCompleted(questID)) return false;
        if (QuestShortcut.enabled() && QuestData.hasUnsupportedStartGrouping(questID)) return false;
        Set<QuestStartRequirement> questStartReq = new HashSet<>();
        Set<QuestStartJobRequirement> questStartJobReq = new HashSet<>();
        for (QuestStartRequirement questReq : qi.getQuestStartRequirements()) {
            if (questReq instanceof QuestStartCompletionRequirement) {
                questStartReq.add(questReq);
            }
            if (questReq instanceof QuestStartJobRequirement) {

                questStartJobReq.add((QuestStartJobRequirement) questReq);
            }
        }
        boolean hasQuestStart = questStartReq.isEmpty() || (QuestShortcut.enabled()
                ? questStartReq.stream().allMatch(q -> q.hasRequirements(this))
                : questStartReq.stream().anyMatch(q -> q.hasRequirements(this)));
        boolean hasCorrectJob = false;
        if (questStartJobReq.isEmpty()) {
            hasCorrectJob = true;
        } else if (questID == 1460) { //Không biết còn dạng nào khác không nên làm cố định một dạng trước
            for (QuestStartJobRequirement questStartJobRequirement : questStartJobReq) {
                if (questStartJobRequirement.hasRequirements(this)) {
                    hasCorrectJob = true;
                }
            }
        } else {
            if (questStartJobReq.stream().anyMatch(q -> q.hasRequirements(this))) {
                hasCorrectJob = true;
            }
        }
        return hasQuestStart && hasCorrectJob && qi.getQuestStartRequirements().stream().filter(qsr -> !(qsr instanceof QuestStartCompletionRequirement) && !(qsr instanceof QuestStartJobRequirement)).allMatch(qsr -> qsr.hasRequirements(this));
    }

    public void completeQuest(int questID) {
        completeQuest(questID, false);
    }

    public void completeQuest(int questID, boolean noRewards) {
        completeQuest(questID, noRewards, null);
    }

    /** TMS completion persists the owned state before consuming objectives or awarding rewards. */
    public void completeQuest(int questID, boolean noRewards, Runnable beforeRewardSave) {
        if (QuestShortcut.enabled()) {
            // multiplayer-0930 lock order: ECONOMY_MONITOR -> Char -> Account (the packet path's order); a script
            // thread took the Account first and then needed ECONOMY_MONITOR for the rewards (deadlock).
            synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized (this) {
            synchronized (QuestShortcut.monitor(this)) {
                if (!noRewards && hasQuestCompleted(questID)) return;
                completeQuestInternal(questID, noRewards, false, beforeRewardSave);
            }
            } }
        } else {
            completeQuestInternal(questID, noRewards, false, beforeRewardSave);
        }
    }

    void completeQuestInternal(int questID, boolean noRewards, boolean skipObjectives) {
        completeQuestInternal(questID, noRewards, skipObjectives, null);
    }

    private void completeQuestInternal(int questID, boolean noRewards, boolean skipObjectives, Runnable beforeRewardSave) {
        QuestInfo qi = QuestData.getQuestInfoById(questID);
        if (QuestShortcut.enabled() && !noRewards) {
            if (qi == null || !hasQuestInProgress(questID)) return;
            if (!skipObjectives) {
                boolean ready = QuestConstants.isAccountQuest(questID)
                        ? getAccount().getQuestById(questID).isComplete(this) : getQuestById(questID).isComplete(this);
                if (!ready) return;
            }
        }
        if (QuestConstants.isAccountQuest(questID)) {
            AccountQuest quest = getAccount().getOrCreateQuestById(questID);
            QuestStatus previousStatus = quest.getStatus();
            FileTime previousTime = quest.getCompletedTime();
            quest.completeQuest();
            if (QuestShortcut.enabled() && !noRewards && !skipObjectives) {
                try {
                    if (beforeRewardSave != null) beforeRewardSave.run();
                    else AccountQuest.saveToSQL(java.util.Map.of(questID, quest), getAccount().getId());
                } catch (RuntimeException e) {
                    quest.setStatus(previousStatus);
                    quest.setCompletedTime(previousTime);
                    throw e;
                }
            }
            write(WvsContext.questRecordMessage(quest));
            write(WvsContext.questWorldShareMessage(quest));
            dbgChatMsg("[資訊] 已完成帳號任務 " + quest.getQRKey() + " | QR 值: " + quest.getQRValue());
            if (!noRewards) {
                if (qi != null) {
                    getAccount().handleCompleteQuest(qi, questID, skipObjectives);
                }
            }
        } else {
            Quest quest = getOrCreateQuestById(questID);
            QuestStatus previousStatus = quest.getStatus();
            FileTime previousTime = quest.getCompletedTime();
            quest.completeQuest();
            if (QuestShortcut.enabled() && !noRewards && !skipObjectives) {
                try {
                    if (beforeRewardSave != null) beforeRewardSave.run();
                    else Quest.saveToSQL(java.util.Map.of(questID, quest), getId());
                } catch (RuntimeException e) {
                    quest.setStatus(previousStatus);
                    quest.setCompletedTime(previousTime);
                    throw e;
                }
            }
            write(WvsContext.questRecordMessage(quest));
            dbgChatMsg("[資訊] 已完成任務 " + quest.getQRKey() + " | QR 值: " + quest.getQRValue());
            if (!noRewards) {
                if (qi != null) {
                    handleCompleteQuest(qi, questID, skipObjectives);
                }
            }
        }
        if (hasQuest(QuestConstants.UNION_ARTIFACT)) {
            UnionArtifact.updateSpecialQuestCompletedMission(this, questID);
        }
        write(UserPacket.effect(Effect.questCompleteEffect()));
        getField().broadcast(UserRemote.effect(getId(), Effect.questCompleteEffect()), this);
    }

    private void handleCompleteQuest(QuestInfo qi, int questID, boolean skipObjectives) {
        if (qi != null) {
            for (QuestProgressRequirement qsr : qi.getQuestProgressRequirements()) {
                if (!skipObjectives && qsr instanceof QuestProgressItemRequirement qpir) {
                    consumeItem(qpir.getItemID(), qpir.getRequiredCount());
                }
            }
            List<QuestItemReward> tempRewardItems = new ArrayList<>();
            boolean hasEXPQuest = false;
            for (QuestReward qr : qi.getQuestRewards()) {
                if (QuestShortcut.enabled() && qr instanceof QuestBuffItemReward buff && buff.getStatus() == 0) continue;
                if (skipObjectives && QuestShortcut.isSkippedCost(qr)) continue;
                if (qr instanceof QuestItemReward qir && qir.getStatus() != 0) {
                    if (qir.getProp() > 0) {
                        tempRewardItems.add(qir);
                    } else {
                        qir.giveReward(this, qi.getQuestName());
                    }
                } else if (qr instanceof QuestExpReward) {
                    qr.giveReward(this);
                    hasEXPQuest = true;
                } else {
                    qr.giveReward(this);
                }
            }
            if (questID >= 102425 && questID <= 102448) {
                hasEXPQuest = true; // Tera Blink questlines
            }
            if (!tempRewardItems.isEmpty()) {
                Util.getRandomFromCollection(tempRewardItems).giveReward(this, qi.getQuestName());
            }
            if (!hasEXPQuest) {
                int lvlMin = 0;
                for (QuestStartRequirement questReq : qi.getQuestStartRequirements()) {
                    if (questReq instanceof QuestStartMinStatRequirement min && min.getStat() == Stat.level) {
                        lvlMin = min.getReqAmount();
                        break;
                    }
                }
                if (lvlMin != 0) {
                    long baseEXP = GameConstants.charExp[lvlMin] / 100;
                    QuestExpReward qer = new QuestExpReward(baseEXP, "");
                    qer.giveReward(this);
                    qi.addReward(qer);
                }
            }
            if (qi.getMedalItemId() != 0 && findMedalByID(qi.getMedalItemId(), questID) == null) {
                MedalAchievementInfo medal = new MedalAchievementInfo(getId(), questID, qi.getMedalItemId(), FileTime.currentTime());
                addMedalAchievementInfo(medal);
                medal.saveToSQL();
                if (canHold(qi.getMedalItemId(), 1)) {
                    addItemToInventory(qi.getMedalItemId(), 1);
                } else {
                    sendRewardToChar(qi.getMedalItemId(), 1, 0, "背包已滿，由於你完成了任務 " + qi.getQuestName() + "，獎勵將通過郵件發送。", 30);
                }
                chatScriptMessage("你已獲得 " + StringData.getItemStringById(qi.getMedalItemId()));
            }
            if (getGuild() != null) {
                getGuild().addContributionToChar(this, GuildConstants.CONTRIBUTION_PER_QUEST);
            }
            AchievementHandler.handleQuestCompleted(this, questID);
        }
    }

    public void handleMobKillForQuest(Mob mob) {
        int mobID = mob.getTemplateId();
        Collection<Integer> mobQuests = mob.getQuests();
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            // The cached mob index is built from 265 quests. Include active 282 overlay requirements too.
            Set<Integer> relevant = new HashSet<>(mobQuests);
            getQuestsInProgress().stream()
                    .filter(q -> q.getMobReqs().stream().anyMatch(r -> r.getMobID() == mobID
                            || mob.getParentMobSet().contains(r.getMobID())))
                    .forEach(q -> relevant.add(q.getQRKey()));
            getAccount().getQuestsInProgress().stream()
                    .filter(q -> q.getMobReqs().stream().anyMatch(r -> r.getMobID() == mobID
                            || mob.getParentMobSet().contains(r.getMobID())))
                    .forEach(q -> relevant.add(q.getQRKey()));
            mobQuests = relevant;
        }
        for (int questID : mobQuests) {
            if (QuestConstants.isAccountQuest(questID)) {
                AccountQuest q = getAccount().getQuestById(questID);
                if (q != null && !q.isComplete(this)) {
                    q.handleMobKill(mobID);
                    write(WvsContext.questRecordMessage(q));
                    write(WvsContext.questWorldShareMessage(q));
                }
            } else {
                Quest q = getQuestById(questID);
                if (q != null && !q.isComplete(this)) {
                    q.handleMobKill(mobID);
                    write(WvsContext.questRecordMessage(q));
                }
            }
        }
        final Collection<AccountQuest> accQuests = getAccount().getQuests().values();
        for (AccountQuest q : accQuests) {
            if (q != null && !q.isComplete(this) && q.hasMobReq(9101025)) {
                if (Math.abs(mob.getLevel() - getLevel()) <= 20) {
                    q.handleMobKill(9101025);
                    write(WvsContext.questRecordMessage(q));
                    write(WvsContext.questWorldShareMessage(q));
                }
            }
        }
        final Collection<Quest> quests = getQuests().values();
        for (Quest q : quests) {
            if (q != null && !q.isComplete(this) && q.hasMobReq(9101025)) {
                if (Math.abs(mob.getLevel() - getLevel()) <= 20) {
                    q.handleMobKill(9101025);
                    write(WvsContext.questRecordMessage(q));
                }
            }
        }
        if (mob.getTemplateId() == BossConstants.INFERNO_WOLF) {
            ScriptManagerImpl sm = getScriptManager();
            if (hasQuest(QuestConstants.INFERNO_WOLF_MOB_DEAD_)) {
                setQRValue(QuestConstants.INFERNO_WOLF_MOB_DEAD_, "mobDead=1");
            } else {
                createQuestWithQRValue(QuestConstants.INFERNO_WOLF_MOB_DEAD_, "mobDead=1");
            }
            sm.stopEvents();
            sm.warpNoReturn(993000600, 0);
        }
        net.swordie.ms.world.tms282.revival.hub.RevivalHubs.onMobKilled(this, mob); // revival-1005 hub missions
        BossHelper.handleMobKilled(mob, this);
        AchievementHandler.handleMobKilled(this, mob.getTemplateId());
    }

    public void handleMoneyGain(int money) {
        for (Quest q : getQuestsInProgress()) {
            if (q.hasMoneyReq()) {
                q.addMoney(money);
                write(WvsContext.questRecordMessage(q));
            }
        }
        for (AccountQuest q : getAccount().getQuestsInProgress()) {
            if (q.hasMoneyReq()) {
                q.addMoney(money);
                write(WvsContext.questRecordMessage(q));
                write(WvsContext.questWorldShareMessage(q));
            }
        }
    }

    /**
     * Removes a given quest from this QuestManager, and notifies the client of this change. Does nothing if the Char
     * does not currently have the quest.
     *
     * @param questID the id of the quest that should be removed
     */
    public void removeQuest(int questID) {
        if (QuestConstants.isAccountQuest(questID)) {
            AccountQuest accountQuest = getAccount().getQuestById(questID);
            if (accountQuest != null) {
                if (QuestShortcut.enabled()) accountQuest.deleteFromSQL();
                accountQuest.setStatus(NotStarted);
                getAccount().getQuests().entrySet().removeIf(x -> x.getKey() == questID);
                write(WvsContext.questRecordMessage(accountQuest));
                write(WvsContext.questWorldShareMessage(accountQuest));
                if (!QuestShortcut.enabled()) accountQuest.deleteFromSQL();
            }
        } else {
            Quest q = getQuestById(questID);
            if (q != null) {
                if (QuestShortcut.enabled()) q.deleteFromSQL();
                q.setStatus(NotStarted);
                getQuests().entrySet().removeIf(x -> x.getKey() == questID);
                write(WvsContext.questRecordMessage(q));
                if (!QuestShortcut.enabled()) q.deleteFromSQL();
            }
        }
    }

    /**
     * Adds a quest to this QuestManager with a given id. If there is no quest with that id, does nothing.
     *
     * @param questId the quest's id to add
     */
    public void addQuest(int questId) {
        Quest q = QuestData.createQuestFromId(questId, getId());
        addQuest(q);
    }

    public Quest getQuestById(int questID) {
        return quests.get(questID);
    }

    public Quest getOrCreateQuestById(int questId) {
        Quest quest = getQuestById(questId);
        if (quest == null) {
            Quest q = QuestData.createQuestFromId(questId, getId());
            addQuest(q);
            return q;
        }
        return quest;
    }

    public Quest getOrCreateQuestById(int questId, FileTime expires) {
        Quest quest = getQuestById(questId);
        if (quest == null) {
            Quest q = QuestData.createQuestFromId(questId, getId());
            q.setExpireTerm(expires);
            addQuest(q);
            return q;
        }
        quest.setExpireTerm(expires);
        return quest;
    }

    public List<Quest> getSortedQuestSublist(List<Quest> questSet, int startIndex, int limit) {
        return questSet.stream()
                .sorted(Comparator.comparingInt(Quest::getQRKey))
                .skip(startIndex)
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<HyperStat> getHyperStats() {
        return hyperStats;
    }

    public List<HyperStat> getHyperStats(int pos) {
        List<HyperStat> result = new LinkedList<>();
        for (HyperStat hyperStat : getHyperStats()) {
            if (hyperStat.getIndex() == pos) {
                result.add(hyperStat);
            }
        }
        return result;
    }

    public int getTotalHyperStatPoints() {
        var result = 0;
        String str = getQRValueByKey(QuestConstants.HYPER_STATS_PRESET, "hyperstats");
        int preset = str != null ? Integer.parseInt(str) : 0;
        for (HyperStat hyperStat : getHyperStats()) {
            if (hyperStat.getIndex() == preset && hyperStat.getSkillLevel() > 0) {
                result += hyperStat.getSkillLevel();
            }
        }
        return result;
    }

    public HyperStat getHyperStats(int pos, int skillId) {
        for (HyperStat hyperStat : getHyperStats()) {
            if (hyperStat.getIndex() == pos && hyperStat.getSkillID() == skillId) {
                return hyperStat;
            }
        }
        return null;
    }

    public void setHyperStats(List<HyperStat> hyperStats) {
        this.hyperStats = hyperStats;
    }

    public List<LinkedSkill> getLinkedSkills() {
        return linkedSkill;
    }

    public List<LinkedSkill> getLinkedSkills(int pos) {
        List<LinkedSkill> result = new LinkedList<>();
        for (LinkedSkill linkedSkill : getLinkedSkills()) {
            if (linkedSkill.getIndex() == pos) {
                result.add(linkedSkill);
            }
        }
        return result;
    }

    public LinkedSkill getLinkedSkill(int pos, int skillId) {
        for (LinkedSkill linkedSkill : getLinkedSkills()) {
            if (linkedSkill.getIndex() == pos && linkedSkill.getSkillID() == skillId) {
                return linkedSkill;
            }
        }
        return null;
    }

    public void setLinkedSkills(List<LinkedSkill> linkedSkill) {
        this.linkedSkill = linkedSkill;
    }

    public Map<Integer, Integer> getSoulCollection() {
        return soulCollection;
    }

    public Map<Integer, Tuple<ExpIncreaseInfo, Long>> getExpPerMob() {
        return expPerMob;
    }

    public void initSkillAlarms() {
        if (getSkillAlarms().isEmpty()) {
            for (int i = 0; i < SkillAlarmInfo.MAX_INDEX; i++) {
                // id=0, charId=0, index=i, skillId=0, key=-1, enable=false
                SkillAlarmInfo info = new SkillAlarmInfo(getId(), 0, i, false, -1);
                getSkillAlarms().add(info);
                info.saveToSQL();
            }
        } else if (getSkillAlarms().size() < SkillAlarmInfo.MAX_INDEX) {
            // đảm bảo đủ 6 phần tử theo index
            boolean[] seen = new boolean[SkillAlarmInfo.MAX_INDEX];
            for (SkillAlarmInfo sai : getSkillAlarms()) {
                int idx = sai.getIndex();
                if (0 <= idx && idx < SkillAlarmInfo.MAX_INDEX) seen[idx] = true;
            }
            for (int i = 0; i < SkillAlarmInfo.MAX_INDEX; i++) {
                if (!seen[i]) {
                    SkillAlarmInfo info = new SkillAlarmInfo(getId(), 0, i, false, -1);
                    getSkillAlarms().add(info);
                    info.saveToSQL();
                }
            }
        }
        // sắp theo index để encode đúng thứ tự
        getSkillAlarms().sort(Comparator.comparingInt(SkillAlarmInfo::getIndex));
    }

    public void initHyperStats() {
        if (getLevel() < 140) {
            return;
        }
        setHyperStats(HyperStat.getHyperStatsByCharID(getId()));
        if (getHyperStats().isEmpty()) {
            for (int i = 0; i <= 2; i++) {
                for (int skillID = 80000400; skillID <= 80000422; skillID++) {
                    if (!SkillConstants.isHyperStat(skillID)) {
                        continue;
                    }
                    getHyperStats().add(new HyperStat(getId(), i, skillID, getSkillLevel(skillID)));
                }
            }
        }
        int preset = 0;
        if (!hasQuest(QuestConstants.HYPER_STATS_PRESET)) {
            createQuestWithQRValue(QuestConstants.HYPER_STATS_PRESET, "hyperstats=" + preset);
        } else {
            preset = Integer.parseInt(getQRValueByKey(QuestConstants.HYPER_STATS_PRESET, "hyperstats"));
        }
        List<Skill> updateSkills = new ArrayList<>();
        for (HyperStat hyperStat : getHyperStats(preset)) {
            Skill skill = getSkill(hyperStat.getSkillID(), true);
            SkillInfo si = SkillData.getSkillInfoById(hyperStat.getSkillID());
            if (si == null || hyperStat.getSkillLevel() == 0) {
                if (si != null && hyperStat.getSkillLevel() == 0 && skill.getCurrentLevel() > 0
                        && net.swordie.ms.connection.protocol.Protocol.active().version()
                        == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                    removeSkill(hyperStat.getSkillID());
                }
                continue;
            }
            skill.setCurrentLevel(hyperStat.getSkillLevel());
            updateSkills.add(skill);
        }
        updateSkills.forEach(skill -> addSkill(skill, true));
        write(WvsContext.changeSkillRecordResult(updateSkills, true, false, false));
    }

    public void initCharacterPotentials() {
        if (getLevel() < 30) {
            return;
        }
        if (!hasQuest(QuestConstants.CHARACTER_POTENTIAL_PRESET)) {
            createQuestWithQRValue(QuestConstants.CHARACTER_POTENTIAL_PRESET, "potential=0");
        }
        setPotentials(CharacterPotential.getCharacterPotentialsFromSQLByCharID(getId()));
    }

    public void initLinkSkills(boolean isInit) {
        if (isInit) {
            setLinkedSkills(LinkedSkill.getLinkedSkillsByCharID(getId()));
        }
        int preset = 0;
        if (!hasQuest(QuestConstants.LINK_SKILL_PRESET)) {
            createQuestWithQRValue(QuestConstants.LINK_SKILL_PRESET, "preset=0");
        } else {
            preset = Integer.parseInt(getQRValueByKey(QuestConstants.LINK_SKILL_PRESET, "preset"));
        }
        for (Integer skillID : getLinked_LinkSkillIDs()) {
            removeSkill(skillID);
        }
        List<Skill> skills = new ArrayList<>();
        Int2IntMap linkskills = new Int2IntOpenHashMap();
        for (LinkedSkill linkedSkill : getLinkedSkills(preset)) {
            int ordinarySkill = SkillConstants.getStackingLinkSkill(linkedSkill.getSkillID());
            Skill skill = SkillData.getSkillDeepCopyById(ordinarySkill);
            if (linkedSkill.getSkillLevel() == 0 || skill == null) {
                continue;
            }
            linkskills.put(ordinarySkill, linkskills.get(ordinarySkill) + linkedSkill.getSkillLevel());
        }
        for (var entry : linkskills.int2IntEntrySet()) {
            Skill skill = SkillData.getSkillDeepCopyById(entry.getIntKey());
            if (skill == null) {
                continue;
            }
            skill.setCurrentLevel(entry.getIntValue());
            skills.add(skill);
        }
        addListSkill(skills);
    }

    public long getCombatPower() {
        return getAvatarData().getCharacterStat().getCombatPower();
    }

    public void calculateCombatPower() {
        // wpConst / maxWpConst = Current Weapon Constant / Highest Weapon Constant
        // mainStats = 4*(MainStat1 + MainStat2 + MainStat3) + SecStat1 + SecStat2
        // dmg, bossDmg, critDmg dạng số thực (0.20 = 20%)
        // finalDmgMul = (1 + FD1) * (1 + FD2) * …
        // attackPower = ATT đã tính theo công thức job
        var weapon = getEquippedInventory().getFirstItemByBodyPart(BodyPart.Weapon);
        float wpConst, maxWpConst;
        if (weapon == null) {
            wpConst = 0;
            maxWpConst = 1.43f;
        } else {
            WeaponType weaponType = ItemConstants.getWeaponType(weapon.getItemId());
            wpConst = weaponType.getDamageMultiplier();
            maxWpConst = weaponType.getMaxDamageMultiplier(getJob());
        }
        double mainStats = 0;
        if (JobConstants.isWarriorEquipJob(getJob())) {
            if (JobConstants.isDemonAvenger(getJob())) {
                mainStats = 4 * getMaxHP() + getTotalStatCP(BaseStat.str);
            } else {
                mainStats = 4 * getTotalStatCP(BaseStat.str) + getTotalStatCP(BaseStat.dex);
            }
        } else if (JobConstants.isMageEquipJob(getJob())) {
            mainStats = 4 * getTotalStatCP(BaseStat.inte) + getTotalStatCP(BaseStat.luk);
        } else if (JobConstants.isArcherEquipJob(getJob())) {
            mainStats = 4 * getTotalStatCP(BaseStat.dex) + getTotalStatCP(BaseStat.str);
        } else if (JobConstants.isXenon(getJob())) {
            mainStats = 4 * (getTotalStatCP(BaseStat.str) + getTotalStatCP(BaseStat.dex) + getTotalStatCP(BaseStat.luk));
        } else if (JobConstants.isThiefEquipJob(getJob())) {
            mainStats = 4 * getTotalStatCP(BaseStat.luk) + getTotalStatCP(BaseStat.dex);
        } else if (JobConstants.isPirateEquipJob(getJob())) {
            mainStats = 4 * getTotalStatCP(BaseStat.dex) + getTotalStatCP(BaseStat.str);
        }
        double dmg = getTotalStatAsDouble(BaseStat.damR) / 100;
        double bossDmg = getTotalStatAsDouble(BaseStat.bd) / 100;
        double finalDmgMul = 0.7; // magic number
        double critDmg = (getTotalStatAsDouble(BaseStat.crDmg) - 20.0D) / 100;
        double attackPower = Math.max(getTotalStatAsDouble(BaseStat.pad), getTotalStatAsDouble(BaseStat.mad));
        getAvatarData().getCharacterStat().setCombatPower((long)(0.01 * (wpConst / maxWpConst) * mainStats * (1 + dmg + bossDmg) * finalDmgMul * (1.35 + critDmg) * attackPower));
    }

    public boolean tryConsumeAttackEffectBudget(int skillId, long now, int windowMs) {
        long last = lastAttackEffectAt.getOrDefault(skillId, 0L);
        if (now - last < windowMs) return false;
        lastAttackEffectAt.put(skillId, now);
        return true;
    }
}
