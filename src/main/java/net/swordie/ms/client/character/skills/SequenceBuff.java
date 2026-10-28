package net.swordie.ms.client.character.skills;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.DataPrinter;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SequenceBuff {

    public static final int[] skillList = {
            91001022, // Boss Slayers
            91001023, // Undeterred
            91001024, // For the Guild!
            91001025, // Hard Hitter
            80002362, // Weapon Tempering
            80002363, // Advanced Weapon Tempering
            80002364, // Accessory Enhancement
            80002365, // Advanced Accessory Enhancement
            1005, // Hero's Echo
            10001005, // Hero's Echo
            10001215, // Hero's Echo
            20001005, // Hero's Echo
            20011005, // Hero's Echo
            20021005, // Hero's Echo
            20031005, // Hero's Echo
            20041005, // Hero's Echo
            20051005, // Hero's Echo
            30001005, // Hero's Echo
            30011005, // Hero's Echo
            30021005, // Hero's Echo
            50001005, // Hero's Echo
            50001215, // Hero's Echo
            60001005, // Exclusive Spell
            60011005, // Exclusive Spell
            60021005, // Exclusive Spell
            60031005, // Exclusive Spell
            100001005, // Focused Time
            140001005, // Hero's Echo
            150001005, // Exclusive Spell
            150011005, // Exclusive Spell
            150021005, // Exclusive Spell
            150031005, // Exclusive Spell
            160001005, // Exclusive Spell
            160011005, // Exclusive Spell
            160021005, // Exclusive Spell
            50001075, // Empress's Prayer
            40021005, // Hero's Echo
            170001005, // Hero's Echo
            180001005, // Stellar Equalize
            40011005, // Hero's Echo
            170011005, // Hero's Echo
    };

    public static final int[] itemList = {
            2003583, // Sparkling Gold Star Potion
            2003586, // Sparkling Blue Star Potion
            2003587, // Sparkling Red Star Potion
            2003596, // Advanced Boss Rush Boost Potion
            2003592, // Boss Rush Boost Potion
            2003597, // Advanced Great Hero Boost Potion
            2003593, // Great Hero Boost Potion
            2003598, // Advanced Penetrating Boost Potion
            2003594, // Penetrating Boost Potion
            2003599, // Advanced Great Blessing Potion
            2003595, // Great Blessing Potion
            2003526, // Legendary Hero Potion
            2003529, // Legendary Blessing Potion
            2003535, // Legendary Fortitude Potion
            2003538, // Legendary Insight Potion
            2023125, // Extreme Red Potion
            2023126, // Extreme Green Potion
            2023127, // Extreme Blue Potion
            2023658, // Legion's Might Lv. 1
            2023659, // Legion's Might Lv. 2
            2023660, // Legion's Might Lv. 3
            2023544, // MVP Superpower Buff
            2024163, // VIP Buff (Stats)
            2024234, // Sayram's Elixir
            2024235, // Aurelia's Elixir
            2024290, // Collector's Elixir
            2023136, // Bright Moonlight Potion
            2432290, // Blessing of the Guild
            2631501, // Greater Blessing of the Guild
            5121057, // Masarayu's Gift Atmospheric Effect
            5121008, // Hero's Will
            5122000, // Hearty Party Bear
            5121014, // Snowing Fishbread
            2004100, // Attack Potion I
            2004101, // Attack Potion II
            2004102, // Attack Potion III
            2004103, // Attack Potion IV
            2004104, // Attack Potion V
            2004105, // Advanced Attack Potion I
            2004106, // Advanced Attack Potion II
            2004107, // Advanced Attack Potion III
            2004108, // Advanced Attack Potion IV
            2004109, // Advanced Attack Potion V
            2004220, // Attack Pill I
            2004221, // Attack Pill II
            2004222, // Attack Pill III
            2004223, // Attack Pill IV
            2004224, // Attack Pill V
            2004225, // Advanced Attack Pill I
            2004226, // Advanced Attack Pill II
            2004227, // Advanced Attack Pill III
            2004228, // Advanced Attack Pill IV
            2004229, // Advanced Attack Pill V
            2002004, // Warrior Potion
            2002006, // Warrior Pill
            2022089, // Baby Dragon Food
            2012000, // Drake's Blood
            2004110, // Magic Potion I
            2004111, // Magic Potion II
            2004112, // Magic Potion III
            2004113, // Magic Potion IV
            2004114, // Magic Potion V
            2004115, // Advanced Magic Potion I
            2004116, // Advanced Magic Potion II
            2004117, // Advanced Magic Potion III
            2004118, // Advanced Magic Potion IV
            2004119, // Advanced Magic Potion V
            2004230, // Magic Pill I
            2004231, // Magic Pill II
            2004232, // Magic Pill III
            2004233, // Magic Pill IV
            2004234, // Magic Pill V
            2004235, // Advanced Magic Pill I
            2004236, // Advanced Magic Pill II
            2004237, // Advanced Magic Pill III
            2004238, // Advanced Magic Pill IV
            2004239, // Advanced Magic Pill V
            2002002, // Magic Potion
            2002007, // Magic Pill
            2002003, // Wizard Potion
            2012002, // Ancient Tree Sap
            2004000, // Strength Potion I
            2004001, // Strength Potion II
            2004002, // Strength Potion III
            2004003, // Strength Potion IV
            2004004, // Strength Potion V
            2004005, // Strength Potion VI
            2004006, // Strength Potion VII
            2004007, // Strength Potion VIII
            2004008, // Strength Potion IX
            2004009, // Strength Potion X
            2004010, // Advanced Strength Potion I
            2004011, // Advanced Strength Potion II
            2004012, // Advanced Strength Potion III
            2004013, // Advanced Strength Potion IV
            2004014, // Advanced Strength Potion V
            2004015, // Advanced Strength Potion VI
            2004016, // Advanced Strength Potion VII
            2004017, // Advanced Strength Potion VIII
            2004018, // Advanced Strength Potion IX
            2004019, // Advanced Strength Potion X
            2004120, // Strength Pill I
            2004121, // Strength Pill II
            2004122, // Strength Pill III
            2004123, // Strength Pill IV
            2004124, // Strength Pill V
            2004125, // Strength Pill VI
            2004126, // Strength Pill VII
            2004127, // Strength Pill VIII
            2004128, // Strength Pill IX
            2004129, // Strength Pill X
            2004130, // Advanced Strength Pill I
            2004131, // Advanced Strength Pill II
            2004132, // Advanced Strength Pill III
            2004133, // Advanced Strength Pill IV
            2004134, // Advanced Strength Pill V
            2004135, // Advanced Strength Pill VI
            2004136, // Advanced Strength Pill VII
            2004137, // Advanced Strength Pill VIII
            2004138, // Advanced Strength Pill IX
            2004139, // Advanced Strength Pill X
            2004020, // Dexterity Potion I
            2004021, // Dexterity Potion II
            2004022, // Dexterity Potion III
            2004023, // Dexterity Potion IV
            2004024, // Dexterity Potion V
            2004025, // Dexterity Potion VI
            2004026, // Dexterity Potion VII
            2004027, // Dexterity Potion VIII
            2004028, // Dexterity Potion IX
            2004029, // Dexterity Potion X
            2004030, // Advanced Dexterity Potion I
            2004031, // Advanced Dexterity Potion II
            2004032, // Advanced Dexterity Potion III
            2004033, // Advanced Dexterity Potion IV
            2004034, // Advanced Dexterity Potion V
            2004035, // Advanced Dexterity Potion VI
            2004036, // Advanced Dexterity Potion VII
            2004037, // Advanced Dexterity Potion VIII
            2004038, // Advanced Dexterity Potion IX
            2004039, // Advanced Dexterity Potion X
            2004140, // Dexterity Pill I
            2004141, // Dexterity Pill II
            2004142, // Dexterity Pill III
            2004143, // Dexterity Pill IV
            2004144, // Dexterity Pill V
            2004145, // Dexterity Pill VI
            2004146, // Dexterity Pill VII
            2004147, // Dexterity Pill VIII
            2004148, // Dexterity Pill IX
            2004149, // Dexterity Pill X
            2004150, // Advanced Dexterity Pill I
            2004151, // Advanced Dexterity Pill II
            2004152, // Advanced Dexterity Pill III
            2004153, // Advanced Dexterity Pill IV
            2004154, // Advanced Dexterity Pill V
            2004155, // Advanced Dexterity Pill VI
            2004156, // Advanced Dexterity Pill VII
            2004157, // Advanced Dexterity Pill VIII
            2004158, // Advanced Dexterity Pill IX
            2004159, // Advanced Dexterity Pill X
            2004040, // Intelligence Potion I
            2004041, // Intelligence Potion II
            2004042, // Intelligence Potion III
            2004043, // Intelligence Potion IV
            2004044, // Intelligence Potion V
            2004045, // Intelligence Potion VI
            2004046, // Intelligence Potion VII
            2004047, // Intelligence Potion VIII
            2004048, // Intelligence Potion IX
            2004049, // Intelligence Potion X
            2004050, // Advanced Intelligence Potion I
            2004051, // Advanced Intelligence Potion II
            2004052, // Advanced Intelligence Potion III
            2004053, // Advanced Intelligence Potion IV
            2004054, // Advanced Intelligence Potion V
            2004055, // Advanced Intelligence Potion VI
            2004056, // Advanced Intelligence Potion VII
            2004057, // Advanced Intelligence Potion VIII
            2004058, // Advanced Intelligence Potion IX
            2004059, // Advanced Intelligence Potion X
            2004160, // Intelligence Pill I
            2004161, // Intelligence Pill II
            2004162, // Intelligence Pill III
            2004163, // Intelligence Pill IV
            2004164, // Intelligence Pill V
            2004165, // Intelligence Pill VI
            2004166, // Intelligence Pill VII
            2004167, // Intelligence Pill VIII
            2004168, // Intelligence Pill IX
            2004169, // Intelligence Pill X
            2004170, // Advanced Intelligence Pill I
            2004171, // Advanced Intelligence Pill II
            2004172, // Advanced Intelligence Pill III
            2004173, // Advanced Intelligence Pill IV
            2004174, // Advanced Intelligence Pill V
            2004175, // Advanced Intelligence Pill VI
            2004176, // Advanced Intelligence Pill VII
            2004177, // Advanced Intelligence Pill VIII
            2004178, // Advanced Intelligence Pill IX
            2004179, // Advanced Intelligence Pill X
            2004060, // Luck Potion I
            2004061, // Luck Potion II
            2004062, // Luck Potion III
            2004063, // Luck Potion IV
            2004064, // Luck Potion V
            2004065, // Luck Potion VI
            2004066, // Luck Potion VII
            2004067, // Luck Potion VIII
            2004068, // Luck Potion IX
            2004069, // Luck Potion X
            2004070, // Advanced Luck Potion I
            2004071, // Advanced Luck Potion II
            2004072, // Advanced Luck Potion III
            2004073, // Advanced Luck Potion IV
            2004074, // Advanced Luck Potion V
            2004075, // Advanced Luck Potion VI
            2004076, // Advanced Luck Potion VII
            2004077, // Advanced Luck Potion VIII
            2004078, // Advanced Luck Potion IX
            2004079, // Advanced Luck Potion X
            2004180, // Luck Pill I
            2004181, // Luck Pill II
            2004182, // Luck Pill III
            2004183, // Luck Pill IV
            2004184, // Luck Pill V
            2004185, // Luck Pill VI
            2004186, // Luck Pill VII
            2004187, // Luck Pill VIII
            2004188, // Luck Pill IX
            2004189, // Luck Pill X
            2004190, // Advanced Luck Pill I
            2004191, // Advanced Luck Pill II
            2004192, // Advanced Luck Pill III
            2004193, // Advanced Luck Pill IV
            2004194, // Advanced Luck Pill V
            2004195, // Advanced Luck Pill VI
            2004196, // Advanced Luck Pill VII
            2004197, // Advanced Luck Pill VIII
            2004198, // Advanced Luck Pill IX
            2004199, // Advanced Luck Pill X
            2003551, // Wealth Acquisition Potion
            2003611, // Small Wealth Acquisition Potion
            2003550, // EXP Accumulation Potion
            2003607, // Small EXP Accumulation Potion
            2003612, // Sm. Conc. EXP Accumulation Potion
            2450211, // 1.5x EXP Coupon (30 min)
            2450212, // 2x EXP Coupon (10 min)
            2450213, // 2x EXP Coupon (15 min)
            2450214, // 2x EXP Coupon (20 min)
            2450215, // 2x EXP Coupon (30 min)
            2450216, // 2x EXP Coupon (1 Hour)
            2450217, // 3x EXP Coupon (15 min)
            2450218, // 3x EXP Coupon (30 min)
            2450219, // 3x EXP Coupon (1 Hour)
            2450220, // 4x EXP Coupon (15 min)
            2450221, // 4x EXP Coupon (30 min)
            2450222, // 4x EXP Coupon (1 Hour)
            2024084, // 50% Bonus EXP Coupon
            2023926, // MVP 50% Bonus EXP Coupon
            2024275, // MVP 70% Bonus EXP Coupon
            5121104, // MVP 50% Bonus EXP Atmospheric Effect
            5121168, // MVP 70% Bonus EXP Atmospheric Effect
            2634036, // Alicia's Blessing
            2631185, // Mu Gong-Certified Wellness Tonic
            2024164, // VIP Buff (EXP)
            2023128, // Extreme Gold Potion
            2023661, // Legion's Luck Lv. 1
            2023662, // Legion's Luck Lv. 2
            2023663, // Legion's Luck Lv. 3
            2023664, // Legion's Wealth Lv. 1
            2023665, // Legion's Wealth Lv. 2
            2023666, // Legion's Wealth Lv. 3
            2024238, // Azmoth Potion
            2022179, // Onyx Apple
            2023908, // Candied Apple
            2022002, // Cider
            2012008, // Unripe Onyx Apple
            5121016, // Hook Bomber
            5121010, // Time Leap
            5121009, // Speed Infusion
            5121002, // Energy Orb
    };

    public static final int MAX_ROW = 5;
    public int id;
    public int charid;
    public int index;
    public String name = "";
    public List<Integer> buffs = new LinkedList<>();

    public static List<SequenceBuff> getSkillSequenceBuffsByCharID(int charID) {
        List<SequenceBuff> out = new ArrayList<>();
        String sql = "SELECT id,charid,`index`,name,"
                + "buff1,buff2,buff3,buff4,buff5,buff6,buff7,buff8,buff9,buff10,"
                + "buff11,buff12,buff13,buff14,buff15,buff16,buff17,buff18,buff19,buff20,"
                + "buff21,buff22,buff23,buff24,buff25,buff26,buff27,buff28,buff29,buff30 "
                + "FROM skillsequences_buffs WHERE charid=? ORDER BY `index`";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SequenceBuff seq = new SequenceBuff();
                    seq.setId(rs.getInt("id"));
                    seq.setCharId(rs.getInt("charid"));
                    seq.setIndex(rs.getInt("index"));
                    seq.setName(rs.getString("name"));
                    List<Integer> buffs = new ArrayList<>(30);
                    for (int k = 1; k <= 30; k++) {
                        Integer v = rs.getObject("buff" + k, Integer.class);
                        buffs.add(v != null ? v : 0);
                    }
                    seq.setBuffs(buffs);
                    out.add(seq);
                }
            }
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        return out;
    }

    public void saveToSQL() {
        final String INS = "INSERT INTO skillsequences_buffs "
                + "(charid,`index`,name,buff1,buff2,buff3,buff4,buff5,buff6,buff7,buff8,buff9,buff10,"
                + "buff11,buff12,buff13,buff14,buff15,buff16,buff17,buff18,buff19,buff20,"
                + "buff21,buff22,buff23,buff24,buff25,buff26,buff27,buff28,buff29,buff30) VALUES "
                + "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        final String UPD = "UPDATE skillsequences_buffs SET "
                + "charid=?,`index`=?,name=?,"
                + "buff1=?,buff2=?,buff3=?,buff4=?,buff5=?,buff6=?,buff7=?,buff8=?,buff9=?,buff10=?,"
                + "buff11=?,buff12=?,buff13=?,buff14=?,buff15=?,buff16=?,buff17=?,buff18=?,buff19=?,buff20=?,"
                + "buff21=?,buff22=?,buff23=?,buff24=?,buff25=?,buff26=?,buff27=?,buff28=?,buff29=?,buff30=? "
                + "WHERE id=?";

        List<Integer> buffs = getBuffs();
        if (buffs == null || buffs.size() != 30) throw new IllegalStateException("buffIds must have 30 items");

        try (Connection con = DatabaseManager.getConnection()) {
            if (getId() == 0) {
                try (PreparedStatement ps = con.prepareStatement(INS, Statement.RETURN_GENERATED_KEYS)) {
                    int p = 1;
                    ps.setInt(p++, getCharId());
                    ps.setInt(p++, getIndex());
                    ps.setString(p++, getName());
                    for (int k = 0; k < 30; k++) ps.setInt(p++, buffs.get(k));
                    ps.executeUpdate();
                    try (ResultSet gk = ps.getGeneratedKeys()) {
                        if (gk.next()) setId(gk.getInt(1));
                    }
                }
            } else {
                try (PreparedStatement ps = con.prepareStatement(UPD)) {
                    int p = 1;
                    ps.setInt(p++, getCharId());
                    ps.setInt(p++, getIndex());
                    ps.setString(p++, getName());
                    for (int k = 0; k < 30; k++) ps.setInt(p++, buffs.get(k));
                    ps.setInt(p++, getId());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public void deleteFromSQL() {
        if (getId() == 0) return;
        String sql = "DELETE FROM skillsequences_buffs WHERE id=?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, getId());
            ps.executeUpdate();
            setId(0);
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public SequenceBuff() {
    }

    public SequenceBuff(int charid, int index, String name, List<Integer> buffs) {
        this.charid = charid;
        this.index = index;
        this.name = name;
        this.buffs = buffs;
    }

    public void encode(OutPacket outPacket, Char chr) {
        outPacket.encodeString(getName(), 9);
        for (int i = 0; i < 30; i++) {
            int buffID = buffs.get(i);
            if (buffID == 0) {
                outPacket.encodeInt(0);
                continue;
            }
            SkillInfo si = SkillData.getSkillInfoById(buffID);
            if (si != null) {
                outPacket.encodeInt(chr.getSkillLevel(buffID));
            } else if (ItemData.getItemInfoByID(buffID) != null) {
                outPacket.encodeInt(chr.getScriptManager().getQuantityOfItem(buffID));
            } else {
                outPacket.encodeInt(0);
            }
        }
        for (int i = 0; i < 30; i++) {
            int buffID = buffs.get(i);
            outPacket.encodeInt(buffID);
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCharId() {
        return charid;
    }

    public void setCharId(int charid) {
        this.charid = charid;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Integer> getBuffs() {
        return buffs;
    }

    public void setBuffs(List<Integer> buffs) {
        this.buffs = buffs;
    }
}
