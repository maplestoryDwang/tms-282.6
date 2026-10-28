package net.swordie.ms.world.gach;

import net.swordie.ms.util.Randomizer;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.gach.result.GachaponDlgType;

import java.util.*;

public class GachaponConstants {
    private static final Map<GachaponDlgType, GachaponInfo> gachaponInfo = new HashMap<>();
    private static List<GachaponInfo.GachItem> Nebulites;
    private static List<GachaponInfo.GachItem> Chairs;
    private static List<GachaponInfo.GachItem> Mounts;
    private static List<GachaponInfo.GachItem> Specials;
    private static List<GachaponInfo.GachItem> equipments;
    private static List<GachaponInfo.GachItem> consumes;

    static {
        init();
    }

    public static Map<GachaponDlgType, GachaponInfo> getGachaponInfo() {
        return gachaponInfo;
    }

    public static void init() {
        initNebulite();
        initChair();
        initMount();
        initSpecial();
        initEquipment();
        initEquipmentHotItem();
        initConsume();
        initScroll();
        String[] messages = initMessages();
        List<GachaponInfo.GachItem> items;
        // Town Gachapon
        items = new ArrayList<>();
        items.addAll(equipments);
        items.addAll(consumes);
        items.addAll(Nebulites);
        items.addAll(Chairs);
        gachaponInfo.put(GachaponDlgType.TOWN, new GachaponInfo(Arrays.asList(messages), items));

        // Special Gachapon
        items = new ArrayList<>(Specials);
        items.addAll(Specials);
        gachaponInfo.put(GachaponDlgType.SPECIAL, new GachaponInfo(Arrays.asList(messages), items));

        // Remote Gachapon
        items = new ArrayList<>();
        items.addAll(equipments);
        items.addAll(consumes);
        items.addAll(Nebulites);
        items.addAll(Chairs);
        gachaponInfo.put(GachaponDlgType.REMOTE, new GachaponInfo(Arrays.asList(messages), items));
    }

    private static void initEquipment() {
        equipments = new ArrayList<>();
        equipments.add(new GachaponInfo.GachItem(1000039)); // Awakening Hat for Warrior
        equipments.add(new GachaponInfo.GachItem(1000040)); // Blizzard Helmet
        equipments.add(new GachaponInfo.GachItem(1000054)); // Mesoranger Red Helmet
        equipments.add(new GachaponInfo.GachItem(1000055)); // Mesoranger Green Helmet
        equipments.add(new GachaponInfo.GachItem(1000056)); // Mesoranger Blue Helmet
        equipments.add(new GachaponInfo.GachItem(1000057)); // Mesoranger Black Helmet
        equipments.add(new GachaponInfo.GachItem(1000063)); // Master Onmyouji Hat
        equipments.add(new GachaponInfo.GachItem(1000066)); // Reed Onmyouji Hat
        equipments.add(new GachaponInfo.GachItem(1000067)); // Onmyouji Performance Hat
        equipments.add(new GachaponInfo.GachItem(1000068)); // Kanna's Onmyouji Hat
        equipments.add(new GachaponInfo.GachItem(1001059)); // Awakening Hat for Warrior
        equipments.add(new GachaponInfo.GachItem(1001060)); // Snow Ice's Fur Hat
        equipments.add(new GachaponInfo.GachItem(1001079)); // Mesoranger Pink Helmet
        equipments.add(new GachaponInfo.GachItem(1001080)); // Mesoranger Yellow Helmet
        equipments.add(new GachaponInfo.GachItem(1001081)); // Mesoranger Shadow Helmet
        equipments.add(new GachaponInfo.GachItem(1002004)); // Great Brown Helmet
        equipments.add(new GachaponInfo.GachItem(1002006)); // Bone Helm
        equipments.add(new GachaponInfo.GachItem(1002013)); // Golden Pride
        equipments.add(new GachaponInfo.GachItem(1002021)); // Steel Nordic Helm
        equipments.add(new GachaponInfo.GachItem(1002022)); // Old Steel Nordic Helm
        equipments.add(new GachaponInfo.GachItem(1002023)); // Jousting Helmet
        equipments.add(new GachaponInfo.GachItem(1002024)); // Emerald Dome
        equipments.add(new GachaponInfo.GachItem(1002025)); // Red Duke
        equipments.add(new GachaponInfo.GachItem(1002028)); // Silver Crusader Helm
        equipments.add(new GachaponInfo.GachItem(1002029)); // Red Oriental Helmet
        equipments.add(new GachaponInfo.GachItem(1002030)); // Silver Planet
        equipments.add(new GachaponInfo.GachItem(1002034)); // Blue Jester
        equipments.add(new GachaponInfo.GachItem(1002035)); // Pink Jester
        equipments.add(new GachaponInfo.GachItem(1002036)); // Green Jester
        equipments.add(new GachaponInfo.GachItem(1002037)); // Black Jester
        equipments.add(new GachaponInfo.GachItem(1002038)); // Brown Jester
        equipments.add(new GachaponInfo.GachItem(1002045)); // Blue Bone Helm
        equipments.add(new GachaponInfo.GachItem(1002046)); // Red Bone Helm
        equipments.add(new GachaponInfo.GachItem(1002047)); // Great Red Helmet
        equipments.add(new GachaponInfo.GachItem(1002048)); // Great Blue Helmet
        equipments.add(new GachaponInfo.GachItem(1002064)); // Bronze Pride
        equipments.add(new GachaponInfo.GachItem(1002065)); // Steel Pride
        equipments.add(new GachaponInfo.GachItem(1002084)); // Blue Oriental Helmet
        equipments.add(new GachaponInfo.GachItem(1002085)); // Mithril Crusader Helm
        equipments.add(new GachaponInfo.GachItem(1002086)); // Bronze Crusader Helm
        equipments.add(new GachaponInfo.GachItem(1002091)); // Dark Dome
        equipments.add(new GachaponInfo.GachItem(1002092)); // Yellow Duke
        equipments.add(new GachaponInfo.GachItem(1002093)); // Blue Duke
        equipments.add(new GachaponInfo.GachItem(1002094)); // Bronze Planet
        equipments.add(new GachaponInfo.GachItem(1002095)); // Mithril Planet
        equipments.add(new GachaponInfo.GachItem(1002098)); // Gold Nordic Helm
        equipments.add(new GachaponInfo.GachItem(1002099)); // Mithril Nordic Helm
        equipments.add(new GachaponInfo.GachItem(1002100)); // Old Bronze Nordic Helm
        equipments.add(new GachaponInfo.GachItem(1002101)); // Old Mithril Nordic Helm
        equipments.add(new GachaponInfo.GachItem(1002135)); // Brown Pole-Feather Hat
        equipments.add(new GachaponInfo.GachItem(1002136)); // Dark Pole-Feather Hat
        equipments.add(new GachaponInfo.GachItem(1002137)); // Green Pole-Feather Hat
        equipments.add(new GachaponInfo.GachItem(1002138)); // Blue Pole-Feather Hat
        equipments.add(new GachaponInfo.GachItem(1002139)); // Red Pole-Feather Hat
        equipments.add(new GachaponInfo.GachItem(1002141)); // Red Matty
        equipments.add(new GachaponInfo.GachItem(1002142)); // Blue Matty
        equipments.add(new GachaponInfo.GachItem(1002143)); // Green Matty
        equipments.add(new GachaponInfo.GachItem(1002144)); // Brown Matty
        equipments.add(new GachaponInfo.GachItem(1002145)); // Dark Matty
        equipments.add(new GachaponInfo.GachItem(1002151)); // Brown Guiltian
        equipments.add(new GachaponInfo.GachItem(1002152)); // Blue Guiltian
        equipments.add(new GachaponInfo.GachItem(1002153)); // Red Guiltian
        equipments.add(new GachaponInfo.GachItem(1002154)); // Dark Guiltian
        equipments.add(new GachaponInfo.GachItem(1002155)); // White Guiltian
        equipments.add(new GachaponInfo.GachItem(1002161)); // Red Hawkeye
        equipments.add(new GachaponInfo.GachItem(1002162)); // Blue Hawkeye
        equipments.add(new GachaponInfo.GachItem(1002163)); // Green Hawkeye
        equipments.add(new GachaponInfo.GachItem(1002164)); // Brown Hawkeye
        equipments.add(new GachaponInfo.GachItem(1002165)); // Dark Hawkeye
        equipments.add(new GachaponInfo.GachItem(1002166)); // Red Distinction
        equipments.add(new GachaponInfo.GachItem(1002167)); // Blue Distinction
        equipments.add(new GachaponInfo.GachItem(1002168)); // Green Distinction
        equipments.add(new GachaponInfo.GachItem(1002169)); // Brown Distinction
        equipments.add(new GachaponInfo.GachItem(1002170)); // Dark Distinction
        equipments.add(new GachaponInfo.GachItem(1002171)); // Red Guise
        equipments.add(new GachaponInfo.GachItem(1002172)); // Blue Guise
        equipments.add(new GachaponInfo.GachItem(1002173)); // Green Guise
        equipments.add(new GachaponInfo.GachItem(1002174)); // Brown Guise
        equipments.add(new GachaponInfo.GachItem(1002175)); // Dark Guise
        equipments.add(new GachaponInfo.GachItem(1002176)); // Red Burgler
        equipments.add(new GachaponInfo.GachItem(1002177)); // Blue Burglar
        equipments.add(new GachaponInfo.GachItem(1002178)); // Green Burgler
        equipments.add(new GachaponInfo.GachItem(1002179)); // Brown Burgler
        equipments.add(new GachaponInfo.GachItem(1002180)); // Dark Burglar
        equipments.add(new GachaponInfo.GachItem(1002181)); // Red Pilfer
        equipments.add(new GachaponInfo.GachItem(1002182)); // Blue Pilfer
        equipments.add(new GachaponInfo.GachItem(1002183)); // Green Pilfer
        equipments.add(new GachaponInfo.GachItem(1002184)); // Brown Pilfer
        equipments.add(new GachaponInfo.GachItem(1002185)); // Dark Pilfer
        equipments.add(new GachaponInfo.GachItem(1002207)); // Red Sonata
        equipments.add(new GachaponInfo.GachItem(1002208)); // Blue Sonata
        equipments.add(new GachaponInfo.GachItem(1002209)); // Green Sonata
        equipments.add(new GachaponInfo.GachItem(1002210)); // Brown Sonata
        equipments.add(new GachaponInfo.GachItem(1002211)); // Blue Maro
        equipments.add(new GachaponInfo.GachItem(1002212)); // Red Maro
        equipments.add(new GachaponInfo.GachItem(1002213)); // Green Maro
        equipments.add(new GachaponInfo.GachItem(1002214)); // Black Maro
        equipments.add(new GachaponInfo.GachItem(1002215)); // Flame Golden Circlet
        equipments.add(new GachaponInfo.GachItem(1002216)); // Aqua Golden Circlet
        equipments.add(new GachaponInfo.GachItem(1002217)); // Orange Golden Circlet
        equipments.add(new GachaponInfo.GachItem(1002218)); // Dark Golden Circlet
        equipments.add(new GachaponInfo.GachItem(1002242)); // Red Seraphis
        equipments.add(new GachaponInfo.GachItem(1002243)); // Blue Seraphis
        equipments.add(new GachaponInfo.GachItem(1002244)); // Green Seraphis
        equipments.add(new GachaponInfo.GachItem(1002245)); // White Seraphis
        equipments.add(new GachaponInfo.GachItem(1002246)); // Dark Seraphis
        equipments.add(new GachaponInfo.GachItem(1002247)); // Bronze Identity
        equipments.add(new GachaponInfo.GachItem(1002248)); // Silver Identity
        equipments.add(new GachaponInfo.GachItem(1002249)); // Dark Identity
        equipments.add(new GachaponInfo.GachItem(1002252)); // Red Infinium Circlet
        equipments.add(new GachaponInfo.GachItem(1002253)); // Blue Infinium Circlet
        equipments.add(new GachaponInfo.GachItem(1002254)); // Dark Infinium Circlet
        equipments.add(new GachaponInfo.GachItem(1002267)); // Red Polyfeather Hat
        equipments.add(new GachaponInfo.GachItem(1002268)); // Brown Polyfeather Hat
        equipments.add(new GachaponInfo.GachItem(1002269)); // White Polyfeather Hat
        equipments.add(new GachaponInfo.GachItem(1002270)); // Black Polyfeather Hat
        equipments.add(new GachaponInfo.GachItem(1002271)); // Green Galaxy
        equipments.add(new GachaponInfo.GachItem(1002272)); // Blue Galaxy
        equipments.add(new GachaponInfo.GachItem(1002273)); // Purple Galaxy
        equipments.add(new GachaponInfo.GachItem(1002274)); // Dark Galaxy
        equipments.add(new GachaponInfo.GachItem(1002275)); // Blue Falcon
        equipments.add(new GachaponInfo.GachItem(1002276)); // Red Falcon
        equipments.add(new GachaponInfo.GachItem(1002277)); // Green Falcon
        equipments.add(new GachaponInfo.GachItem(1002278)); // Dark Falcon
        equipments.add(new GachaponInfo.GachItem(1002281)); // Brown Nightfox
        equipments.add(new GachaponInfo.GachItem(1002282)); // Blue Nightfox
        equipments.add(new GachaponInfo.GachItem(1002283)); // Purple Nightfox
        equipments.add(new GachaponInfo.GachItem(1002284)); // White Nightfox
        equipments.add(new GachaponInfo.GachItem(1002285)); // Blood Nightfox
        equipments.add(new GachaponInfo.GachItem(1002286)); // Blue Patriot
        equipments.add(new GachaponInfo.GachItem(1002287)); // Beige Patriot
        equipments.add(new GachaponInfo.GachItem(1002288)); // Green Patriot
        equipments.add(new GachaponInfo.GachItem(1002289)); // Dark Patriot
        equipments.add(new GachaponInfo.GachItem(1002323)); // Green Osfa Hat
        equipments.add(new GachaponInfo.GachItem(1002324)); // Brown Osfa Hat
        equipments.add(new GachaponInfo.GachItem(1002325)); // Purple Osfa Hat
        equipments.add(new GachaponInfo.GachItem(1002326)); // Red Osfa Hat
        equipments.add(new GachaponInfo.GachItem(1002327)); // Brown Pireta Hat
        equipments.add(new GachaponInfo.GachItem(1002328)); // Green Pireta Hat
        equipments.add(new GachaponInfo.GachItem(1002329)); // Red Pireta Hat
        equipments.add(new GachaponInfo.GachItem(1002330)); // Dark Pireta Hat
        equipments.add(new GachaponInfo.GachItem(1002338)); // Red Dragon Barbute
        equipments.add(new GachaponInfo.GachItem(1002339)); // Blue Dragon Barbute
        equipments.add(new GachaponInfo.GachItem(1002340)); // Dark Dragon Barbute
        equipments.add(new GachaponInfo.GachItem(1002363)); // Green Oriental Fury Hat
        equipments.add(new GachaponInfo.GachItem(1002364)); // Blue Oriental Fury Hat
        equipments.add(new GachaponInfo.GachItem(1002365)); // Red Oriental Fury Hat
        equipments.add(new GachaponInfo.GachItem(1002366)); // Black Oriental Fury Hat
        equipments.add(new GachaponInfo.GachItem(1002377)); // Green Valhalla Helmet
        equipments.add(new GachaponInfo.GachItem(1002378)); // Blue Valhalla Helmet
        equipments.add(new GachaponInfo.GachItem(1002379)); // Dark Valhalla Helmet
        equipments.add(new GachaponInfo.GachItem(1002380)); // Green Canal Hood
        equipments.add(new GachaponInfo.GachItem(1002381)); // Blue Canal Hood
        equipments.add(new GachaponInfo.GachItem(1002382)); // Red Canal Hood
        equipments.add(new GachaponInfo.GachItem(1002383)); // Dark Canal Hood
        equipments.add(new GachaponInfo.GachItem(1002398)); // Green Varr Hat
        equipments.add(new GachaponInfo.GachItem(1002399)); // Blue Varr Hat
        equipments.add(new GachaponInfo.GachItem(1002400)); // Red Varr Hat
        equipments.add(new GachaponInfo.GachItem(1002401)); // Dark Varr Hat
        equipments.add(new GachaponInfo.GachItem(1002402)); // Red Arlic Helmet
        equipments.add(new GachaponInfo.GachItem(1002403)); // Blue Arlic Helmet
        equipments.add(new GachaponInfo.GachItem(1002404)); // Green Arlic Helmet
        equipments.add(new GachaponInfo.GachItem(1002405)); // Dark Arlic Helmet
        equipments.add(new GachaponInfo.GachItem(1002406)); // Red Arnah Cap
        equipments.add(new GachaponInfo.GachItem(1002407)); // Blue Arnah Cap
        equipments.add(new GachaponInfo.GachItem(1002408)); // Green Arnah Cap
        equipments.add(new GachaponInfo.GachItem(1002436)); // Chief Stan Hat
        equipments.add(new GachaponInfo.GachItem(1002454)); // Red Starry Bandana
        equipments.add(new GachaponInfo.GachItem(1002455)); // Black Starry Bandana
        equipments.add(new GachaponInfo.GachItem(1002471)); // Shapka for Hunters
        equipments.add(new GachaponInfo.GachItem(1002483)); // Goblin Cap
        equipments.add(new GachaponInfo.GachItem(1002510)); // Maple Hat (Level 2)
        equipments.add(new GachaponInfo.GachItem(1002511)); // Maple Hat (Level 3)
        equipments.add(new GachaponInfo.GachItem(1002517)); // Maple Bandana Red
        equipments.add(new GachaponInfo.GachItem(1002528)); // Green Grace Helmet
        equipments.add(new GachaponInfo.GachItem(1002529)); // Blue Grace Helmet
        equipments.add(new GachaponInfo.GachItem(1002530)); // Red Grace Helmet
        equipments.add(new GachaponInfo.GachItem(1002531)); // Silver Grace Helmet
        equipments.add(new GachaponInfo.GachItem(1002532)); // Dark Grace Helmet
        equipments.add(new GachaponInfo.GachItem(1002554)); // Muey Thai String
        equipments.add(new GachaponInfo.GachItem(1002563)); // Delphinus Bandana
        equipments.add(new GachaponInfo.GachItem(1002564)); // Super Delphinus Bandana
        equipments.add(new GachaponInfo.GachItem(1002571)); // Lord Pirate Hat
        equipments.add(new GachaponInfo.GachItem(1002572)); // Lord Pirate Hat
        equipments.add(new GachaponInfo.GachItem(1002573)); // Average Lord Pirate Hat
        equipments.add(new GachaponInfo.GachItem(1002574)); // Acceptable Lord Pirate Hat
        equipments.add(new GachaponInfo.GachItem(1002577)); // Pickpocket Pilfer
        equipments.add(new GachaponInfo.GachItem(1002578)); // Herculean Helmet
        equipments.add(new GachaponInfo.GachItem(1002579)); // LeFay Jester
        equipments.add(new GachaponInfo.GachItem(1002580)); // Lockewood Hat
        equipments.add(new GachaponInfo.GachItem(1002622)); // White Oceania Cap
        equipments.add(new GachaponInfo.GachItem(1002625)); // Blue Den Marine
        equipments.add(new GachaponInfo.GachItem(1002628)); // Red Misty
        equipments.add(new GachaponInfo.GachItem(1002631)); // Brown Leather Ocean Hat
        equipments.add(new GachaponInfo.GachItem(1002634)); // Purple Cast Linen
        equipments.add(new GachaponInfo.GachItem(1002637)); // Black Pirate's Bandana
        equipments.add(new GachaponInfo.GachItem(1002640)); // Blue Sun Boat Hat
        equipments.add(new GachaponInfo.GachItem(1002643)); // Red Brave Hamal
        equipments.add(new GachaponInfo.GachItem(1002646)); // Black Polax Hat
        equipments.add(new GachaponInfo.GachItem(1002651)); // White Starry Bandana
        equipments.add(new GachaponInfo.GachItem(1002656)); // White Identity
        equipments.add(new GachaponInfo.GachItem(1002675)); // Antellion Miter
        equipments.add(new GachaponInfo.GachItem(1002676)); // Infinity Circlet
        equipments.add(new GachaponInfo.GachItem(1002677)); // Toymaker Cap
        equipments.add(new GachaponInfo.GachItem(1002702)); // Legends Bandana
        equipments.add(new GachaponInfo.GachItem(1002723)); // Rice Cake Hat
        equipments.add(new GachaponInfo.GachItem(1002732)); // Traditional Rice-cake Hat
        equipments.add(new GachaponInfo.GachItem(1002739)); // Bosshunter Helm
        equipments.add(new GachaponInfo.GachItem(1002740)); //  Bosshunter Faceguard
        equipments.add(new GachaponInfo.GachItem(1002749)); // Elf Bandana
        equipments.add(new GachaponInfo.GachItem(1002750)); //  Bosshunter Faceguard
        equipments.add(new GachaponInfo.GachItem(1002758)); // Maple Hat (Level 4)
        equipments.add(new GachaponInfo.GachItem(1002782)); // Eridanus Bandana
        equipments.add(new GachaponInfo.GachItem(1002783)); // Super Eridanus Bandana
        equipments.add(new GachaponInfo.GachItem(1002788)); // Necomimi
        equipments.add(new GachaponInfo.GachItem(1002800)); // Agent N's Receiver
        equipments.add(new GachaponInfo.GachItem(1002801)); // Raven Ninja Bandana
        equipments.add(new GachaponInfo.GachItem(1002856)); // Miner's Hat
        equipments.add(new GachaponInfo.GachItem(1002857)); // Hard Hat
        equipments.add(new GachaponInfo.GachItem(1002865)); // Fornax Bandana
        equipments.add(new GachaponInfo.GachItem(1002866)); // Super Fornax Bandana
        equipments.add(new GachaponInfo.GachItem(1002868)); // Amarantia
        equipments.add(new GachaponInfo.GachItem(1002897)); // Orange Hair-band
        equipments.add(new GachaponInfo.GachItem(1002898)); // Green Hair-band
        equipments.add(new GachaponInfo.GachItem(1002899)); // Yellow Hair-band
        equipments.add(new GachaponInfo.GachItem(1002900)); // Blue Hair-band
        equipments.add(new GachaponInfo.GachItem(1002901)); // White hair-band
        equipments.add(new GachaponInfo.GachItem(1002902)); // Black Hair-band
        equipments.add(new GachaponInfo.GachItem(1002905)); // Targa Hat
        equipments.add(new GachaponInfo.GachItem(1002906)); // Scarlion Boss Hat
        equipments.add(new GachaponInfo.GachItem(1002926)); // Targa Hat
        equipments.add(new GachaponInfo.GachItem(1002927)); // Scarlion Boss Hat
        equipments.add(new GachaponInfo.GachItem(1002947)); // Arcana Crown
        equipments.add(new GachaponInfo.GachItem(1002948)); // Blitz Helm
        equipments.add(new GachaponInfo.GachItem(1002949)); // Power Mane
        equipments.add(new GachaponInfo.GachItem(1002965)); // Gemini Bandana
        equipments.add(new GachaponInfo.GachItem(1002966)); // Super Gemini Bandana
        equipments.add(new GachaponInfo.GachItem(1003003)); // Hercules Bandana
        equipments.add(new GachaponInfo.GachItem(1003004)); // Super Hercules Bandana
        equipments.add(new GachaponInfo.GachItem(1003011)); // Traditional Cake Hat
        equipments.add(new GachaponInfo.GachItem(1003016)); // Wild Wolf Bandana
        equipments.add(new GachaponInfo.GachItem(1003023)); // Targa Hat(INT)
        equipments.add(new GachaponInfo.GachItem(1003024)); // Targa Hat(LUK)
        equipments.add(new GachaponInfo.GachItem(1003025)); // Scarlion Hat(DEX)
        equipments.add(new GachaponInfo.GachItem(1003026)); // Scarlion Hat(STR)
        equipments.add(new GachaponInfo.GachItem(1003039)); // Time Traveler's Circlet
        equipments.add(new GachaponInfo.GachItem(1003041)); // Izar Bandana
        equipments.add(new GachaponInfo.GachItem(1003042)); // Super Izar Bandana
        equipments.add(new GachaponInfo.GachItem(1003046)); // Tengu Mask
        equipments.add(new GachaponInfo.GachItem(1003055)); // Fish Pin
        equipments.add(new GachaponInfo.GachItem(1003068)); // Ravana Helmet
        equipments.add(new GachaponInfo.GachItem(1003091)); // Dunas Hat
        equipments.add(new GachaponInfo.GachItem(1003108)); // Android Headgear
        equipments.add(new GachaponInfo.GachItem(1003111)); // White Maple Bandana
        equipments.add(new GachaponInfo.GachItem(1003137)); // Evan Headband
        equipments.add(new GachaponInfo.GachItem(1003138)); // Nova Bandana
        equipments.add(new GachaponInfo.GachItem(1003140)); // Cosmo Bandana
        equipments.add(new GachaponInfo.GachItem(1003143)); // Androa Bandana
        equipments.add(new GachaponInfo.GachItem(1003151)); // Royal Moonlight Flower Hairpin
        equipments.add(new GachaponInfo.GachItem(1003159)); // Empress's Fine Hat
        equipments.add(new GachaponInfo.GachItem(1003160)); // Empress's Brilliant Hat
        equipments.add(new GachaponInfo.GachItem(1003169)); // Red Ball Mask Hat
        equipments.add(new GachaponInfo.GachItem(1003188)); // El Nido Bandana
        equipments.add(new GachaponInfo.GachItem(1003189)); // Zenith Bandana
        equipments.add(new GachaponInfo.GachItem(1003190)); // Arcania Bandana
        equipments.add(new GachaponInfo.GachItem(1003191)); // Nova Bandana
        equipments.add(new GachaponInfo.GachItem(1003195)); // Legendary Ravana Helmet
        equipments.add(new GachaponInfo.GachItem(1003197)); // Force Walker Hairband
        equipments.add(new GachaponInfo.GachItem(1003198)); // Arcane Walker Hairband
        equipments.add(new GachaponInfo.GachItem(1003199)); // Wind Walker Hairband
        equipments.add(new GachaponInfo.GachItem(1003200)); // Mist Walker Hairband
        equipments.add(new GachaponInfo.GachItem(1003201)); // Fortune Walker Hairband
        equipments.add(new GachaponInfo.GachItem(1003205)); // Chaos Bandanna
        equipments.add(new GachaponInfo.GachItem(1003206)); // Titan Bandana
        equipments.add(new GachaponInfo.GachItem(1003219)); // Jumbo Primary Ribbon
        equipments.add(new GachaponInfo.GachItem(1003224)); // Maple Super Hat
        equipments.add(new GachaponInfo.GachItem(1003228)); // Bugeyed Frog Hat
        equipments.add(new GachaponInfo.GachItem(1003229)); // Bugeyed Frog Hat
        equipments.add(new GachaponInfo.GachItem(1003236)); // Awesome Bandana
        equipments.add(new GachaponInfo.GachItem(1003242)); // Beryl Maple Beret
        equipments.add(new GachaponInfo.GachItem(1003243)); // Crimson Maple Beret
        equipments.add(new GachaponInfo.GachItem(1003267)); // Fashionable Lord Pirate Hat
        equipments.add(new GachaponInfo.GachItem(1003296)); // Legendary Maple Commemorative Hat
        equipments.add(new GachaponInfo.GachItem(1003316)); // Jousting Helmet
        equipments.add(new GachaponInfo.GachItem(1003317)); // Green Jester
        equipments.add(new GachaponInfo.GachItem(1003318)); // Green Hawkeye
        equipments.add(new GachaponInfo.GachItem(1003319)); // Brown Guise
        equipments.add(new GachaponInfo.GachItem(1003320)); // White Oceania Cap
        equipments.add(new GachaponInfo.GachItem(1003321)); // Great Brown Helmet
        equipments.add(new GachaponInfo.GachItem(1003322)); // Green Matty
        equipments.add(new GachaponInfo.GachItem(1003323)); // Red Pole-Feather Hat
        equipments.add(new GachaponInfo.GachItem(1003324)); // Dark Burglar
        equipments.add(new GachaponInfo.GachItem(1003325)); // Blue Den Marine
        equipments.add(new GachaponInfo.GachItem(1003326)); // Gold Nordic Helm
        equipments.add(new GachaponInfo.GachItem(1003327)); // Brown Guiltian
        equipments.add(new GachaponInfo.GachItem(1003328)); // Green Distinction
        equipments.add(new GachaponInfo.GachItem(1003329)); // Dark Pilfer
        equipments.add(new GachaponInfo.GachItem(1003330)); // Red Misty
        equipments.add(new GachaponInfo.GachItem(1003331)); // Silver Crusader Helmet
        equipments.add(new GachaponInfo.GachItem(1003332)); // Aqua Golden Circlet
        equipments.add(new GachaponInfo.GachItem(1003333)); // Red Maro
        equipments.add(new GachaponInfo.GachItem(1003334)); // Red Sonata
        equipments.add(new GachaponInfo.GachItem(1003335)); // Brown Leather Ocean Hat
        equipments.add(new GachaponInfo.GachItem(1003336)); // Blue Oriental Helmet
        equipments.add(new GachaponInfo.GachItem(1003337)); // Blue Seraphis
        equipments.add(new GachaponInfo.GachItem(1003338)); // Brown Polyfeather Hat
        equipments.add(new GachaponInfo.GachItem(1003339)); // Bronze Identity
        equipments.add(new GachaponInfo.GachItem(1003340)); // Purple Cast Linen
        equipments.add(new GachaponInfo.GachItem(1003341)); // Silver Planet
        equipments.add(new GachaponInfo.GachItem(1003342)); // Dark Infinium Circlet
        equipments.add(new GachaponInfo.GachItem(1003343)); // Dark Patriot
        equipments.add(new GachaponInfo.GachItem(1003344)); // Blood Nightfox
        equipments.add(new GachaponInfo.GachItem(1003345)); // Black Pirate's Bandana
        equipments.add(new GachaponInfo.GachItem(1003349)); // Legendary Maple Commemorative Hat
        equipments.add(new GachaponInfo.GachItem(1003350)); // Legendary Maple Commemorative Hat
        equipments.add(new GachaponInfo.GachItem(1003359)); // Royal Elven Tiara
        equipments.add(new GachaponInfo.GachItem(1003360)); // Devil Horns
        equipments.add(new GachaponInfo.GachItem(1003364)); // Legend Maple Beret
        equipments.add(new GachaponInfo.GachItem(1003371)); // Evan Headband STR
        equipments.add(new GachaponInfo.GachItem(1003372)); // Evan Headband DEX
        equipments.add(new GachaponInfo.GachItem(1003373)); // Evan Headband INT
        equipments.add(new GachaponInfo.GachItem(1003374)); // Evan Headband LUK
        equipments.add(new GachaponInfo.GachItem(1003394)); // Evan Headband STR
        equipments.add(new GachaponInfo.GachItem(1003395)); // Evan Headband DEX
        equipments.add(new GachaponInfo.GachItem(1003396)); // Evan Headband INT
        equipments.add(new GachaponInfo.GachItem(1003397)); // Evan Headband LUK
        equipments.add(new GachaponInfo.GachItem(1003409)); // Raven Persona
        equipments.add(new GachaponInfo.GachItem(1003411)); // Renegades Bandanna
        equipments.add(new GachaponInfo.GachItem(1003412)); // Raven Bandanna
        equipments.add(new GachaponInfo.GachItem(1003413)); // <Renegades> Bugeyed Frog Hat
        equipments.add(new GachaponInfo.GachItem(1003414)); // Thief Goggles
        equipments.add(new GachaponInfo.GachItem(1003415)); // Pirate Goggles
        equipments.add(new GachaponInfo.GachItem(1003418)); // Rice Cake on Top of My Head
        equipments.add(new GachaponInfo.GachItem(1003419)); // Two Rice Cakes on Top of My Head
        equipments.add(new GachaponInfo.GachItem(1003423)); // Silver Clover Hat
        equipments.add(new GachaponInfo.GachItem(1003424)); // Golden Clover Hat
        equipments.add(new GachaponInfo.GachItem(1003449)); // Bugeyed Frog Hat
        equipments.add(new GachaponInfo.GachItem(1003453)); // Maple Hat (Level 2)
        equipments.add(new GachaponInfo.GachItem(1003454)); // Maple Hat (Level 3)
        equipments.add(new GachaponInfo.GachItem(1003455)); // Ravana's Golden Crown
        equipments.add(new GachaponInfo.GachItem(1003479)); // Regal Golden Headband
        equipments.add(new GachaponInfo.GachItem(1003507)); // Mihile's Silver Helm
        equipments.add(new GachaponInfo.GachItem(1003529)); // Maple Amethysian Gold Cap
        equipments.add(new GachaponInfo.GachItem(1003550)); // Jett's Special Hat
        equipments.add(new GachaponInfo.GachItem(1003552)); // Grand Maple Amethysian Gold Cap
        equipments.add(new GachaponInfo.GachItem(1003555)); // Warrior's Helm
        equipments.add(new GachaponInfo.GachItem(1003556)); // Fearless Warrior's Helm
        equipments.add(new GachaponInfo.GachItem(1003557)); // The Bladed Falcon's Helm
        equipments.add(new GachaponInfo.GachItem(1003561)); // Tempest Feather Hat
        equipments.add(new GachaponInfo.GachItem(1003562)); // Maple Commemorative Hat
        equipments.add(new GachaponInfo.GachItem(1003563)); // Maple Commemorative Hat
        equipments.add(new GachaponInfo.GachItem(1003564)); // Maple Commemorative Hat
        equipments.add(new GachaponInfo.GachItem(1003565)); // Tempest Bandanna
        equipments.add(new GachaponInfo.GachItem(1003566)); // Pantheon Bandanna
        equipments.add(new GachaponInfo.GachItem(1003571)); // Reed Hair Band
        equipments.add(new GachaponInfo.GachItem(1003572)); // Performance Hair Band
        equipments.add(new GachaponInfo.GachItem(1003573)); // Kanna's Hair Band
        equipments.add(new GachaponInfo.GachItem(1003574)); // Hayato's Helmet
        equipments.add(new GachaponInfo.GachItem(1003575)); // Kanna's Ribbon
        equipments.add(new GachaponInfo.GachItem(1003576)); // Bugeyed Frog Hat
        equipments.add(new GachaponInfo.GachItem(1003577)); // Azalea Hair Pin
        equipments.add(new GachaponInfo.GachItem(1003609)); // Musashi Hat
        equipments.add(new GachaponInfo.GachItem(1003610)); // Dogma Cap
        equipments.add(new GachaponInfo.GachItem(1003611)); // Green General Cap
        equipments.add(new GachaponInfo.GachItem(1003612)); // Green General Cap
        equipments.add(new GachaponInfo.GachItem(1003613)); // Green General Cap
        equipments.add(new GachaponInfo.GachItem(1003614)); // Green General Cap
        equipments.add(new GachaponInfo.GachItem(1003615)); // Green General Cap
        equipments.add(new GachaponInfo.GachItem(1003616)); // Purple Senior Cap
        equipments.add(new GachaponInfo.GachItem(1003617)); // Purple Senior Cap
        equipments.add(new GachaponInfo.GachItem(1003618)); // Purple Senior Cap
        equipments.add(new GachaponInfo.GachItem(1003619)); // Purple Senior Cap
        equipments.add(new GachaponInfo.GachItem(1003620)); // Purple Senior Cap
        equipments.add(new GachaponInfo.GachItem(1003624)); // Kaivesurnet
        equipments.add(new GachaponInfo.GachItem(1003625)); // Old Kaiser Helmet
        equipments.add(new GachaponInfo.GachItem(1003627)); //  Feeble Beginning Knight's Helm
        equipments.add(new GachaponInfo.GachItem(1003635)); // Angel Wing Band
        equipments.add(new GachaponInfo.GachItem(1003637)); // Old Kaiser Helmet
        equipments.add(new GachaponInfo.GachItem(1003638)); // Old Kaiser Helmet
        equipments.add(new GachaponInfo.GachItem(1003644)); // Aran Hero Feather
        equipments.add(new GachaponInfo.GachItem(1003645)); // Evan Hero Feather
        equipments.add(new GachaponInfo.GachItem(1003646)); // Mercedes Hero Feather
        equipments.add(new GachaponInfo.GachItem(1003647)); // Phantom Hero Feather
        equipments.add(new GachaponInfo.GachItem(1003648)); // Luminous Hero Feather
        equipments.add(new GachaponInfo.GachItem(1003659)); //  Legendary Beginning Knight's Helm
        equipments.add(new GachaponInfo.GachItem(1003674)); // Zombie Hunter Hat
        equipments.add(new GachaponInfo.GachItem(1003679)); // Spiderweb Pointy Hat
        equipments.add(new GachaponInfo.GachItem(1003680)); // Mini Pumpkin Pointy Hat
        equipments.add(new GachaponInfo.GachItem(1003692)); // Spiderweb Pointy Hat
        equipments.add(new GachaponInfo.GachItem(1003693)); // Mini Pumpkin Pointy Hat
        equipments.add(new GachaponInfo.GachItem(1003694)); // Big eyed Frog Hat
        equipments.add(new GachaponInfo.GachItem(1003715)); // Pierre Hat
        equipments.add(new GachaponInfo.GachItem(1003716)); // Von Bon Helmet
        equipments.add(new GachaponInfo.GachItem(1003717)); // Queen's Tiara
        equipments.add(new GachaponInfo.GachItem(1003718)); // Vellum's Helm
        equipments.add(new GachaponInfo.GachItem(1003723)); // Stellar Hat
        equipments.add(new GachaponInfo.GachItem(1003734)); // Ultimate Devil Horns
        equipments.add(new GachaponInfo.GachItem(1003740)); // Pinnacle Hat
        equipments.add(new GachaponInfo.GachItem(1003741)); // Unleashed Hat
        equipments.add(new GachaponInfo.GachItem(1003744)); // Black Sheep Hat
        equipments.add(new GachaponInfo.GachItem(1003745)); // White Sheep Hat
        equipments.add(new GachaponInfo.GachItem(1003752)); // Belial Devil Horns
        equipments.add(new GachaponInfo.GachItem(1003753)); // Xenon Cap
        equipments.add(new GachaponInfo.GachItem(1003754)); // Pinnacle Snow
        equipments.add(new GachaponInfo.GachItem(1003755)); // Unleashed Snow
        equipments.add(new GachaponInfo.GachItem(1003758)); // Aether Snow
        equipments.add(new GachaponInfo.GachItem(1003762)); // Tangyoon's Chef Hat
        equipments.add(new GachaponInfo.GachItem(1003764)); // Infinite Rice Cake Head
        equipments.add(new GachaponInfo.GachItem(1003780)); // Frilly Pink Bean Hat
        equipments.add(new GachaponInfo.GachItem(1003781)); // Frilly Black Bean Hat
        equipments.add(new GachaponInfo.GachItem(1003786)); // Fairy Rice Cake Head
        equipments.add(new GachaponInfo.GachItem(1003787)); // Fairy Bugeyed Frog Hat
        equipments.add(new GachaponInfo.GachItem(1003788)); // Spark Fairy Hat
        equipments.add(new GachaponInfo.GachItem(1003805)); // Thunderous Focus Hat
        equipments.add(new GachaponInfo.GachItem(1003806)); // Strikeback Goggles
        equipments.add(new GachaponInfo.GachItem(1003822)); // Galeforce Focus Hat
        equipments.add(new GachaponInfo.GachItem(1003828)); // Master Onmyouji Hat
        equipments.add(new GachaponInfo.GachItem(1003840)); // Shadow Knight Hat
        equipments.add(new GachaponInfo.GachItem(1003856)); // Common Lord Pirate Hat
        equipments.add(new GachaponInfo.GachItem(1003857)); // Ratty Lord Pirate Hat
        equipments.add(new GachaponInfo.GachItem(1003863)); // Onyx Maple Hat
        equipments.add(new GachaponInfo.GachItem(1003864)); // Pearl Maple Hat
        equipments.add(new GachaponInfo.GachItem(1003879)); // Intense Focus Dawn Warrior
        equipments.add(new GachaponInfo.GachItem(1003916)); // Werebeast
        equipments.add(new GachaponInfo.GachItem(1003922)); // Maple Racer Helmet
        equipments.add(new GachaponInfo.GachItem(1003923)); // Alchemist Hat
        equipments.add(new GachaponInfo.GachItem(1003924)); // Alien in My Head
        equipments.add(new GachaponInfo.GachItem(1003925)); // Transforming Sword
        equipments.add(new GachaponInfo.GachItem(1003932)); // Ruby Clover Hat
        equipments.add(new GachaponInfo.GachItem(1003978)); // Sealed Murgoth Mask
        equipments.add(new GachaponInfo.GachItem(1003979)); // Rainbow Rice Cake Topper
        equipments.add(new GachaponInfo.GachItem(1003981)); // Space Pirate Topper
        equipments.add(new GachaponInfo.GachItem(1003982)); // Space Pirate Open Helmet
        equipments.add(new GachaponInfo.GachItem(1003999)); // Sealed Nine-Tailed Fox Mask
        equipments.add(new GachaponInfo.GachItem(1004006)); // Andras Mask
        equipments.add(new GachaponInfo.GachItem(1004007)); // Marbas Mask
        equipments.add(new GachaponInfo.GachItem(1004008)); // Belfor Mask
        equipments.add(new GachaponInfo.GachItem(1004009)); // Amdusias Mask
        equipments.add(new GachaponInfo.GachItem(1004010)); // Crocell Mask
        equipments.add(new GachaponInfo.GachItem(1004012)); // Murgoth Mask
        equipments.add(new GachaponInfo.GachItem(1004013)); // Nine-Tailed Fox Mask
        equipments.add(new GachaponInfo.GachItem(1004057)); // Pointy Ear Hat
        equipments.add(new GachaponInfo.GachItem(1004079)); // Versalmas Hat
        equipments.add(new GachaponInfo.GachItem(1004080)); // Maplemas Hat
        equipments.add(new GachaponInfo.GachItem(1004107)); // Nature Guardian’s Crown
        equipments.add(new GachaponInfo.GachItem(1004138)); // Nature Guardian’s Crown
        equipments.add(new GachaponInfo.GachItem(1004172)); // Maple Saint Cap
        equipments.add(new GachaponInfo.GachItem(1004297)); // Blazing Focus Hat
        equipments.add(new GachaponInfo.GachItem(1004383)); // Dark Focus Hat
        equipments.add(new GachaponInfo.GachItem(1004404)); // Frozen Hat
        equipments.add(new GachaponInfo.GachItem(1004412)); // Android Helmet
        equipments.add(new GachaponInfo.GachItem(1004427)); // Cosmos Knockout Hat
        equipments.add(new GachaponInfo.GachItem(1004484)); // Singles Army Helm
        equipments.add(new GachaponInfo.GachItem(1004485)); // Couples Army Helm
        equipments.add(new GachaponInfo.GachItem(1004516)); // Musashi's Head Decoration
        equipments.add(new GachaponInfo.GachItem(1004517)); // Warrior's Head Decoration
        equipments.add(new GachaponInfo.GachItem(1004518)); // Captain's Head Decoration
        equipments.add(new GachaponInfo.GachItem(1004520)); // Tsukishiro's Head Decoration
        equipments.add(new GachaponInfo.GachItem(1004521)); // Chuyukusa's Head Decoration
        equipments.add(new GachaponInfo.GachItem(1004522)); // Enju's Head Decoration
        equipments.add(new GachaponInfo.GachItem(1004531)); // Alishan Traditional Hat
        equipments.add(new GachaponInfo.GachItem(1004551)); // Ayanokouji Family Hat
        equipments.add(new GachaponInfo.GachItem(1004552)); // Tsukishiro's Head Decoration
        equipments.add(new GachaponInfo.GachItem(1004553)); // Chuyukasa's Head Decoration
        equipments.add(new GachaponInfo.GachItem(1004554)); // Enju's Head Decoration
        equipments.add(new GachaponInfo.GachItem(1004579)); // Pure Sheep Hat
        equipments.add(new GachaponInfo.GachItem(1004582)); // Black Oriental Fury Hat
        equipments.add(new GachaponInfo.GachItem(1004604)); // Sky Pinball Guard Helmet
        equipments.add(new GachaponInfo.GachItem(1004605)); // Blue Pinball Guard Helmet
        equipments.add(new GachaponInfo.GachItem(1004615)); // Honey Rice Cake Hat
        equipments.add(new GachaponInfo.GachItem(1004616)); // Bean Rice Cake Hat
        equipments.add(new GachaponInfo.GachItem(1004617)); // Chestnut Rice Cake Hat
        equipments.add(new GachaponInfo.GachItem(1004650)); // Pop Jelly Hat
        equipments.add(new GachaponInfo.GachItem(1004653)); // Aran Hero Feather
        equipments.add(new GachaponInfo.GachItem(1004654)); // Evan Hero Feather
        equipments.add(new GachaponInfo.GachItem(1004670)); // Mercedes Hero Feather
        equipments.add(new GachaponInfo.GachItem(1004707)); // Phantom Hero Feather
        equipments.add(new GachaponInfo.GachItem(1004715)); // Shade Hero Feather
        equipments.add(new GachaponInfo.GachItem(1004719)); // Luminous Hero Feather
        equipments.add(new GachaponInfo.GachItem(1004764)); // Vibrant Dark Matter Clover
        equipments.add(new GachaponInfo.GachItem(1004765)); // Supple Dark Matter Clover
        equipments.add(new GachaponInfo.GachItem(1004766)); // Canny Dark Matter Clover
        equipments.add(new GachaponInfo.GachItem(1004767)); // Lucky Dark Matter Clover
        equipments.add(new GachaponInfo.GachItem(1004768)); // Serene Dark Matter Clover
        equipments.add(new GachaponInfo.GachItem(1004769)); // Delicate Dark Matter Clover
        equipments.add(new GachaponInfo.GachItem(1004770)); // Superior Dark Matter Clover
        equipments.add(new GachaponInfo.GachItem(1004771)); // Superb Dark Matter Clover
        equipments.add(new GachaponInfo.GachItem(1004772)); // Perfect Dark Matter Clover
        equipments.add(new GachaponInfo.GachItem(1004773)); // Eternal Dark Matter Clover
        equipments.add(new GachaponInfo.GachItem(1004780)); // Royal Elven Tiara
        equipments.add(new GachaponInfo.GachItem(1004782)); // Raven Persona
        equipments.add(new GachaponInfo.GachItem(1004783)); // Dogma Cap
        equipments.add(new GachaponInfo.GachItem(1004784)); // Evan Wing Headband
        equipments.add(new GachaponInfo.GachItem(1004785)); // Pointy Ear Hat
        equipments.add(new GachaponInfo.GachItem(1004786)); // Devil Horns
        equipments.add(new GachaponInfo.GachItem(1004822)); // Floral Fireworks Hat
        equipments.add(new GachaponInfo.GachItem(1102000)); // Green Adventurer Cape
        equipments.add(new GachaponInfo.GachItem(1102001)); // Blue Adventurer Cape
        equipments.add(new GachaponInfo.GachItem(1102002)); // Red Adventurer Cape
        equipments.add(new GachaponInfo.GachItem(1102003)); // White Adventurer Cape
        equipments.add(new GachaponInfo.GachItem(1102004)); // Black Adventurer Cape
        equipments.add(new GachaponInfo.GachItem(1102011)); // Blue Justice Cape
        equipments.add(new GachaponInfo.GachItem(1102012)); // Red Justice Cape
        equipments.add(new GachaponInfo.GachItem(1102013)); // White Justice Cape
        equipments.add(new GachaponInfo.GachItem(1102014)); // Black Justice Cape
        equipments.add(new GachaponInfo.GachItem(1102015)); // Blue Magic Cape
        equipments.add(new GachaponInfo.GachItem(1102016)); // Red Magic Cape
        equipments.add(new GachaponInfo.GachItem(1102017)); // White Magic Cape
        equipments.add(new GachaponInfo.GachItem(1102018)); // Black Magic Cape
        equipments.add(new GachaponInfo.GachItem(1102021)); // Blue Gaia Cape
        equipments.add(new GachaponInfo.GachItem(1102022)); // Red Gaia Cape
        equipments.add(new GachaponInfo.GachItem(1102023)); // White Gaia Cape
        equipments.add(new GachaponInfo.GachItem(1102024)); // Black Gaia Cape
        equipments.add(new GachaponInfo.GachItem(1102026)); // Green Seraph Cape
        equipments.add(new GachaponInfo.GachItem(1102027)); // Blue Seraph Cape
        equipments.add(new GachaponInfo.GachItem(1102028)); // Red Seraph Cape
        equipments.add(new GachaponInfo.GachItem(1102029)); // White Seraph Cape
        equipments.add(new GachaponInfo.GachItem(1102030)); // Black Seraph Cape
        equipments.add(new GachaponInfo.GachItem(1102031)); // Green Giles Cape
        equipments.add(new GachaponInfo.GachItem(1102032)); // Purple Giles Cape
        equipments.add(new GachaponInfo.GachItem(1102033)); // Red Giles Cape
        equipments.add(new GachaponInfo.GachItem(1102034)); // Blue Giles Cape
        equipments.add(new GachaponInfo.GachItem(1102035)); // Black Giles Cape
        equipments.add(new GachaponInfo.GachItem(1102040)); // Yellow Adventurer Cape
        equipments.add(new GachaponInfo.GachItem(1102041)); // Pink Adventurer Cape
        equipments.add(new GachaponInfo.GachItem(1102042)); // Purple Adventurer Cape
        equipments.add(new GachaponInfo.GachItem(1102043)); // Brown Adventurer Cape
        equipments.add(new GachaponInfo.GachItem(1102046)); // Blue Musketeer Cape
        equipments.add(new GachaponInfo.GachItem(1102047)); // Turquoise Musketeer Cape
        equipments.add(new GachaponInfo.GachItem(1102048)); // Red Musketeer Cape
        equipments.add(new GachaponInfo.GachItem(1102054)); // Icarus Cape (1)
        equipments.add(new GachaponInfo.GachItem(1102055)); // Icarus Cape (2)
        equipments.add(new GachaponInfo.GachItem(1102056)); // Icarus Cape (3)
        equipments.add(new GachaponInfo.GachItem(1102057)); // Ludibrium Cape
        equipments.add(new GachaponInfo.GachItem(1102061)); // Oxygen Tank
        equipments.add(new GachaponInfo.GachItem(1102064)); // Goblin Cape
        equipments.add(new GachaponInfo.GachItem(1102078)); // Eclipse Cloak
        equipments.add(new GachaponInfo.GachItem(1102084)); // Pink Gaia Cape
        equipments.add(new GachaponInfo.GachItem(1102085)); // Yellow Gaia Cape
        equipments.add(new GachaponInfo.GachItem(1102086)); // Purple Gaia Cape
        equipments.add(new GachaponInfo.GachItem(1102087)); // Green Gaia Cape
        equipments.add(new GachaponInfo.GachItem(1102099)); // Amos's Royal Cape
        equipments.add(new GachaponInfo.GachItem(1102100)); // Amos's Spirit Cape
        equipments.add(new GachaponInfo.GachItem(1102101)); // The Legendary Elias Cape 1
        equipments.add(new GachaponInfo.GachItem(1102102)); // The Legendary Elias Cape 2
        equipments.add(new GachaponInfo.GachItem(1102103)); // The Legendary Elias Cape 3
        equipments.add(new GachaponInfo.GachItem(1102104)); // Cecelia Cloak 1
        equipments.add(new GachaponInfo.GachItem(1102105)); // Cecelia Cloak 2
        equipments.add(new GachaponInfo.GachItem(1102106)); // Cecelia Cloak 3
        equipments.add(new GachaponInfo.GachItem(1102109)); // Cape of Warmness
        equipments.add(new GachaponInfo.GachItem(1102135)); // Zenumist's Cape
        equipments.add(new GachaponInfo.GachItem(1102136)); // Alcadno's Cape
        equipments.add(new GachaponInfo.GachItem(1102139)); // Zenumist's Cape
        equipments.add(new GachaponInfo.GachItem(1102140)); // Alcadno's Cape
        equipments.add(new GachaponInfo.GachItem(1102145)); // Sirius Cloak
        equipments.add(new GachaponInfo.GachItem(1102146)); // Zeta Cape
        equipments.add(new GachaponInfo.GachItem(1102147)); // Toymaker Cape
        equipments.add(new GachaponInfo.GachItem(1102165)); // Taru Spirit Cape
        equipments.add(new GachaponInfo.GachItem(1102167)); // Maple Cape
        equipments.add(new GachaponInfo.GachItem(1102168)); // Maple Cape
        equipments.add(new GachaponInfo.GachItem(1102178)); // Stirgeman Cape Mk II
        equipments.add(new GachaponInfo.GachItem(1102179)); // Stirgeman Cape Mk III
        equipments.add(new GachaponInfo.GachItem(1102180)); // Stirgeman Cape Mk IV
        equipments.add(new GachaponInfo.GachItem(1102181)); // Stirgeman's Cloak of Wiliness
        equipments.add(new GachaponInfo.GachItem(1102182)); // Stirgeman's Cloak of Darkness
        equipments.add(new GachaponInfo.GachItem(1102183)); // Stirgeman's Cloak of Justice
        equipments.add(new GachaponInfo.GachItem(1102191)); // El Nathian Cape
        equipments.add(new GachaponInfo.GachItem(1102192)); // Wrath of El Nath
        equipments.add(new GachaponInfo.GachItem(1102193)); // Cloak of Corruption
        equipments.add(new GachaponInfo.GachItem(1102205)); // Crimsonheart Cloak
        equipments.add(new GachaponInfo.GachItem(1102206)); // Blackfist Cloak
        equipments.add(new GachaponInfo.GachItem(1102226)); // Ensign's Scarf
        equipments.add(new GachaponInfo.GachItem(1102227)); // Captain's Scarf
        equipments.add(new GachaponInfo.GachItem(1102235)); // Awakening Cape for Warrior
        equipments.add(new GachaponInfo.GachItem(1102241)); // Dunas Cape
        equipments.add(new GachaponInfo.GachItem(1102246)); // Blizzard Cape
        equipments.add(new GachaponInfo.GachItem(1102260)); // Crimsonheart Cape
        equipments.add(new GachaponInfo.GachItem(1102294)); // Beryl Maple Cloak
        equipments.add(new GachaponInfo.GachItem(1102295)); // Crimson Maple Cloak
        equipments.add(new GachaponInfo.GachItem(1102317)); // Crimson Maple Cape
        equipments.add(new GachaponInfo.GachItem(1102322)); // Legends Maple Cloak
        equipments.add(new GachaponInfo.GachItem(1102327)); // Spiegelmann's Luxury Cape
        equipments.add(new GachaponInfo.GachItem(1102328)); // Maple Cape STR
        equipments.add(new GachaponInfo.GachItem(1102329)); // Maple Cape DEX
        equipments.add(new GachaponInfo.GachItem(1102330)); // Maple Cape INT
        equipments.add(new GachaponInfo.GachItem(1102331)); // Maple Cape LUK
        equipments.add(new GachaponInfo.GachItem(1102333)); // Taru Spirit Cape
        equipments.add(new GachaponInfo.GachItem(1102337)); // Super Pink Adventurer Cape
        equipments.add(new GachaponInfo.GachItem(1102339)); // Maple Cape STR
        equipments.add(new GachaponInfo.GachItem(1102340)); // Maple Cape DEX
        equipments.add(new GachaponInfo.GachItem(1102341)); // Maple Cape INT
        equipments.add(new GachaponInfo.GachItem(1102342)); // Maple Cape LUK
        equipments.add(new GachaponInfo.GachItem(1102360)); // Silver Clover Backpack
        equipments.add(new GachaponInfo.GachItem(1102361)); // Golden Clover Wing
        equipments.add(new GachaponInfo.GachItem(1102394)); // Maple Amethysian Star Cloak
        equipments.add(new GachaponInfo.GachItem(1102441)); // Grand Maple Amethysian Star Cloak
        equipments.add(new GachaponInfo.GachItem(1102467)); // Tempest Cape
        equipments.add(new GachaponInfo.GachItem(1102469)); // Maple Cape
        equipments.add(new GachaponInfo.GachItem(1102470)); // Maple Cape
        equipments.add(new GachaponInfo.GachItem(1102471)); // Elite Heliseum Warrior Cape
        equipments.add(new GachaponInfo.GachItem(1102472)); // Elite Heliseum Magician Cape
        equipments.add(new GachaponInfo.GachItem(1102473)); // Elite Heliseum Bowman Cape
        equipments.add(new GachaponInfo.GachItem(1102474)); // Elite Heliseum Thief Cape
        equipments.add(new GachaponInfo.GachItem(1102475)); // Elite Heliseum Pirate Cape
        equipments.add(new GachaponInfo.GachItem(1102497)); // Zombie Hunter Cape
        equipments.add(new GachaponInfo.GachItem(1102502)); // Stellar Cloak
        equipments.add(new GachaponInfo.GachItem(1102506)); // Pinnacle Cape
        equipments.add(new GachaponInfo.GachItem(1102507)); // Unleashed Cape
        equipments.add(new GachaponInfo.GachItem(1102530)); // Spark Fairy Cape
        equipments.add(new GachaponInfo.GachItem(1102533)); // Tot's Cape
        equipments.add(new GachaponInfo.GachItem(1102552)); // Shadow Knight Cape
        equipments.add(new GachaponInfo.GachItem(1102562)); // Onyx Maple Cape
        equipments.add(new GachaponInfo.GachItem(1102563)); // Pearl Maple Cape
        equipments.add(new GachaponInfo.GachItem(1102590)); // Ellinel Wings
        equipments.add(new GachaponInfo.GachItem(1102595)); // Alchemist Cape
        equipments.add(new GachaponInfo.GachItem(1102601)); // Two-handed Sword
        equipments.add(new GachaponInfo.GachItem(1102602)); // Ruby Clover Backpack
        equipments.add(new GachaponInfo.GachItem(1102660)); // Pointy Tail Cape
        equipments.add(new GachaponInfo.GachItem(1102681)); // Singles Army Combat Cape
        equipments.add(new GachaponInfo.GachItem(1102689)); // Heavy Violetta Cape
        equipments.add(new GachaponInfo.GachItem(1102691)); // Maple Saint Cape
        equipments.add(new GachaponInfo.GachItem(1102760)); // Masked Gentleman's Cape
        equipments.add(new GachaponInfo.GachItem(1102777)); // Cosmos Knockout Cape
        equipments.add(new GachaponInfo.GachItem(1102799)); // Frozen Cape
        equipments.add(new GachaponInfo.GachItem(1102821)); // Couples Army Cape
        equipments.add(new GachaponInfo.GachItem(1040000)); // Yellow Jangoon Armor
        equipments.add(new GachaponInfo.GachItem(1040021)); // Red Hwarang Shirt
        equipments.add(new GachaponInfo.GachItem(1040026)); // Green Hwarang Shirt
        equipments.add(new GachaponInfo.GachItem(1040028)); // Blue Sky
        equipments.add(new GachaponInfo.GachItem(1040029)); // Blue Dragon
        equipments.add(new GachaponInfo.GachItem(1040030)); // Gold Dragon
        equipments.add(new GachaponInfo.GachItem(1040057)); // Dark Brown Stealer
        equipments.add(new GachaponInfo.GachItem(1040058)); // Dark Silver Stealer
        equipments.add(new GachaponInfo.GachItem(1040059)); // Red Gold Stealer
        equipments.add(new GachaponInfo.GachItem(1040060)); // Silver Black Stealer
        equipments.add(new GachaponInfo.GachItem(1040061)); // Green Knucklevest
        equipments.add(new GachaponInfo.GachItem(1040062)); // Red Knucklevest
        equipments.add(new GachaponInfo.GachItem(1040063)); // Black Knucklevest
        equipments.add(new GachaponInfo.GachItem(1040067)); // Green Hunter's Armor
        equipments.add(new GachaponInfo.GachItem(1040068)); // Dark Hunter's Armor
        equipments.add(new GachaponInfo.GachItem(1040069)); // Red Hunter's Armor
        equipments.add(new GachaponInfo.GachItem(1040070)); // Blue Hunter's Armor
        equipments.add(new GachaponInfo.GachItem(1040072)); // Red Legolier
        equipments.add(new GachaponInfo.GachItem(1040073)); // Blue Legolier
        equipments.add(new GachaponInfo.GachItem(1040074)); // Green Legolier
        equipments.add(new GachaponInfo.GachItem(1040075)); // Dark Legolier
        equipments.add(new GachaponInfo.GachItem(1040076)); // Brown Legolier
        equipments.add(new GachaponInfo.GachItem(1040079)); // Brown Piette
        equipments.add(new GachaponInfo.GachItem(1040080)); // Dark Piette
        equipments.add(new GachaponInfo.GachItem(1040081)); // White Piette
        equipments.add(new GachaponInfo.GachItem(1040082)); // Khaki Shadow
        equipments.add(new GachaponInfo.GachItem(1040083)); // Marine Shadow
        equipments.add(new GachaponInfo.GachItem(1040084)); // Dark Shadow
        equipments.add(new GachaponInfo.GachItem(1040085)); // Maroon Jangoon Armor
        equipments.add(new GachaponInfo.GachItem(1040086)); // Blue Jangoon Armor
        equipments.add(new GachaponInfo.GachItem(1040087)); // Blue Shouldermail
        equipments.add(new GachaponInfo.GachItem(1040088)); // Ocher Shouldermail
        equipments.add(new GachaponInfo.GachItem(1040089)); // Umber Shouldermail
        equipments.add(new GachaponInfo.GachItem(1040090)); // Green Orientican
        equipments.add(new GachaponInfo.GachItem(1040091)); // Red Orientican
        equipments.add(new GachaponInfo.GachItem(1040092)); // Blue Orientican
        equipments.add(new GachaponInfo.GachItem(1040093)); // Dark Orientican
        equipments.add(new GachaponInfo.GachItem(1040094)); // Red China
        equipments.add(new GachaponInfo.GachItem(1040095)); // Blue China
        equipments.add(new GachaponInfo.GachItem(1040096)); // Brown China
        equipments.add(new GachaponInfo.GachItem(1040097)); // Green China
        equipments.add(new GachaponInfo.GachItem(1040098)); // Light Scorpio
        equipments.add(new GachaponInfo.GachItem(1040099)); // Ocher Scorpio
        equipments.add(new GachaponInfo.GachItem(1040100)); // Dark Scorpio
        equipments.add(new GachaponInfo.GachItem(1040102)); // Bronze Platine
        equipments.add(new GachaponInfo.GachItem(1040103)); // Mithril Platine
        equipments.add(new GachaponInfo.GachItem(1040104)); // Orihalcon Platine
        equipments.add(new GachaponInfo.GachItem(1040105)); // Brown Studded Top
        equipments.add(new GachaponInfo.GachItem(1040106)); // Blue Studded Top
        equipments.add(new GachaponInfo.GachItem(1040107)); // Dark Studded Top
        equipments.add(new GachaponInfo.GachItem(1040108)); // Green Pirate Top
        equipments.add(new GachaponInfo.GachItem(1040109)); // Red Pirate Top
        equipments.add(new GachaponInfo.GachItem(1040110)); // Dark Pirate Top
        equipments.add(new GachaponInfo.GachItem(1040111)); // Green Commodore
        equipments.add(new GachaponInfo.GachItem(1040112)); // Blue Commodore
        equipments.add(new GachaponInfo.GachItem(1040113)); // Dark Commodore
        equipments.add(new GachaponInfo.GachItem(1040115)); // Green Osfa Suit
        equipments.add(new GachaponInfo.GachItem(1040116)); // Brown Osfa Suit
        equipments.add(new GachaponInfo.GachItem(1040117)); // Purple Osfa Suit
        equipments.add(new GachaponInfo.GachItem(1040118)); // Red Osfa Suit
        equipments.add(new GachaponInfo.GachItem(1040120)); // Green Neos
        equipments.add(new GachaponInfo.GachItem(1040121)); // Blue Neos
        equipments.add(new GachaponInfo.GachItem(1040122)); // Black Neos
        equipments.add(new GachaponInfo.GachItem(1040173)); // Dark Hunter's Armor
        equipments.add(new GachaponInfo.GachItem(1040174)); // Dark Brown Stealer
        equipments.add(new GachaponInfo.GachItem(1040175)); // Red Legolier
        equipments.add(new GachaponInfo.GachItem(1040176)); // Green Knucklevest
        equipments.add(new GachaponInfo.GachItem(1040177)); // Maroon Jangoon Armor
        equipments.add(new GachaponInfo.GachItem(1040178)); // Brown Piette
        equipments.add(new GachaponInfo.GachItem(1040179)); // Dark Shadow
        equipments.add(new GachaponInfo.GachItem(1040180)); // Umber Shouldermail
        equipments.add(new GachaponInfo.GachItem(1040181)); // Green China
        equipments.add(new GachaponInfo.GachItem(1040182)); // Red Orientican
        equipments.add(new GachaponInfo.GachItem(1040183)); // Dark Scorpio
        equipments.add(new GachaponInfo.GachItem(1040184)); // Orihalcon Platine
        equipments.add(new GachaponInfo.GachItem(1040185)); // Dark Studded Top
        equipments.add(new GachaponInfo.GachItem(1041047)); // Red Steal
        equipments.add(new GachaponInfo.GachItem(1041048)); // Black Steal
        equipments.add(new GachaponInfo.GachItem(1041049)); // Blue Steal
        equipments.add(new GachaponInfo.GachItem(1041050)); // Purple Steal
        equipments.add(new GachaponInfo.GachItem(1041051)); // Red Amoria Top
        equipments.add(new GachaponInfo.GachItem(1041052)); // Blue Amoria Top
        equipments.add(new GachaponInfo.GachItem(1041053)); // Black Amoria Top
        equipments.add(new GachaponInfo.GachItem(1041054)); // Green Huntress Armor
        equipments.add(new GachaponInfo.GachItem(1041055)); // Black Huntress Armor
        equipments.add(new GachaponInfo.GachItem(1041056)); // Red Huntress Armor
        equipments.add(new GachaponInfo.GachItem(1041065)); // Red Legolia
        equipments.add(new GachaponInfo.GachItem(1041066)); // Blue Legolia
        equipments.add(new GachaponInfo.GachItem(1041067)); // Green Legolia
        equipments.add(new GachaponInfo.GachItem(1041068)); // Dark Legolia
        equipments.add(new GachaponInfo.GachItem(1041069)); // Brown Legolia
        equipments.add(new GachaponInfo.GachItem(1041074)); // Purple Shadow
        equipments.add(new GachaponInfo.GachItem(1041075)); // Red Shadow
        equipments.add(new GachaponInfo.GachItem(1041076)); // Dark Shadow
        equipments.add(new GachaponInfo.GachItem(1041077)); // Maroon Moon
        equipments.add(new GachaponInfo.GachItem(1041078)); // Blue Moon
        equipments.add(new GachaponInfo.GachItem(1041079)); // Brown Moon
        equipments.add(new GachaponInfo.GachItem(1041080)); // Red Moon
        equipments.add(new GachaponInfo.GachItem(1041081)); // White Piettra
        equipments.add(new GachaponInfo.GachItem(1041082)); // Brown Piettra
        equipments.add(new GachaponInfo.GachItem(1041083)); // Dark Piettra
        equipments.add(new GachaponInfo.GachItem(1041084)); // Red Jangoon Armor
        equipments.add(new GachaponInfo.GachItem(1041085)); // Brown Jangoon Armor
        equipments.add(new GachaponInfo.GachItem(1041086)); // Black Jangoon Armor
        equipments.add(new GachaponInfo.GachItem(1041087)); // Red Shouldermail
        equipments.add(new GachaponInfo.GachItem(1041088)); // Ivory Shouldermail
        equipments.add(new GachaponInfo.GachItem(1041089)); // Dark Shouldermail
        equipments.add(new GachaponInfo.GachItem(1041091)); // Green Ice Queen
        equipments.add(new GachaponInfo.GachItem(1041092)); // Red Ice Queen
        equipments.add(new GachaponInfo.GachItem(1041093)); // Blue Ice Queen
        equipments.add(new GachaponInfo.GachItem(1041094)); // Light Mantis
        equipments.add(new GachaponInfo.GachItem(1041095)); // Bloody Mantis
        equipments.add(new GachaponInfo.GachItem(1041096)); // Umber Mantis
        equipments.add(new GachaponInfo.GachItem(1041097)); // Aqua Platina
        equipments.add(new GachaponInfo.GachItem(1041098)); // Violet Platina
        equipments.add(new GachaponInfo.GachItem(1041099)); // Bloody Platina
        equipments.add(new GachaponInfo.GachItem(1041100)); // Purple Mystique
        equipments.add(new GachaponInfo.GachItem(1041101)); // Blue Mystique
        equipments.add(new GachaponInfo.GachItem(1041102)); // Pink Mystique
        equipments.add(new GachaponInfo.GachItem(1041103)); // Red Mystique
        equipments.add(new GachaponInfo.GachItem(1041105)); // Green Pirate Blouse
        equipments.add(new GachaponInfo.GachItem(1041106)); // Red Pirate Blouse
        equipments.add(new GachaponInfo.GachItem(1041107)); // Dark Pirate Blouse
        equipments.add(new GachaponInfo.GachItem(1041115)); // Green Osfa Suit
        equipments.add(new GachaponInfo.GachItem(1041116)); // Brown Osfa Suit
        equipments.add(new GachaponInfo.GachItem(1041117)); // Purple Osfa Suit
        equipments.add(new GachaponInfo.GachItem(1041118)); // Red Osfa Suit
        equipments.add(new GachaponInfo.GachItem(1041119)); // Green Valkyrie
        equipments.add(new GachaponInfo.GachItem(1041120)); // Purple Valkyrie
        equipments.add(new GachaponInfo.GachItem(1041121)); // Dark Valkyrie
        equipments.add(new GachaponInfo.GachItem(1041122)); // Green Lucida
        equipments.add(new GachaponInfo.GachItem(1041123)); // Purple Lucida
        equipments.add(new GachaponInfo.GachItem(1041124)); // Dark Lucida
        equipments.add(new GachaponInfo.GachItem(1041176)); // Green Huntress Armor
        equipments.add(new GachaponInfo.GachItem(1041177)); // Black Steal
        equipments.add(new GachaponInfo.GachItem(1041178)); // Red Amoria Top
        equipments.add(new GachaponInfo.GachItem(1041179)); // Red Legolia
        equipments.add(new GachaponInfo.GachItem(1041180)); // Black Jangoon Armor
        equipments.add(new GachaponInfo.GachItem(1041181)); // Dark Piettra
        equipments.add(new GachaponInfo.GachItem(1041182)); // Dark Shadow
        equipments.add(new GachaponInfo.GachItem(1041183)); // Dark Shouldermail
        equipments.add(new GachaponInfo.GachItem(1041184)); // Red Moon
        equipments.add(new GachaponInfo.GachItem(1041185)); // Red Ice Queen
        equipments.add(new GachaponInfo.GachItem(1041186)); // Umber Mantis
        equipments.add(new GachaponInfo.GachItem(1041187)); // Bloody Platina
        equipments.add(new GachaponInfo.GachItem(1041188)); // Red Mystique
        equipments.add(new GachaponInfo.GachItem(1042191)); // Android Vest
        equipments.add(new GachaponInfo.GachItem(1042231)); // Homecoming Victory Jacket
        equipments.add(new GachaponInfo.GachItem(1042233)); // Silver Clover Hood T-shirt
        equipments.add(new GachaponInfo.GachItem(1042234)); // Golden Clover T-shirt
        equipments.add(new GachaponInfo.GachItem(1042244)); // Homecoming Victory Jacket
        equipments.add(new GachaponInfo.GachItem(1042253)); // Tot's Trial Top
        equipments.add(new GachaponInfo.GachItem(1042352)); // O. Mushroom T-Shirt
        equipments.add(new GachaponInfo.GachItem(1042353)); // Slime T-Shirt
        equipments.add(new GachaponInfo.GachItem(1042365)); // Ruby Clover T-shirt
    }

    private static void initEquipmentHotItem() {
        equipments.add(new GachaponInfo.GachItem(1002547, true)); // Red Hunter
        equipments.add(new GachaponInfo.GachItem(1002550, true)); // Black Garina Hood
        equipments.add(new GachaponInfo.GachItem(1002551, true)); // Blue Dragon Helmet
        equipments.add(new GachaponInfo.GachItem(1002649, true)); // Canopus Hat
        equipments.add(new GachaponInfo.GachItem(1002773, true)); // Gold Dragon Crown
        equipments.add(new GachaponInfo.GachItem(1002776, true)); // Timeless Fennel
        equipments.add(new GachaponInfo.GachItem(1002777, true)); // Timeless Coral
        equipments.add(new GachaponInfo.GachItem(1002778, true)); // Timeless Rapido
        equipments.add(new GachaponInfo.GachItem(1002779, true)); // Timeless Chive
        equipments.add(new GachaponInfo.GachItem(1002780, true)); // Timeless Conrad Henkel
        equipments.add(new GachaponInfo.GachItem(1002790, true)); // Reverse Fennel
        equipments.add(new GachaponInfo.GachItem(1002791, true)); // Reverse Coral
        equipments.add(new GachaponInfo.GachItem(1002792, true)); // Reverse Rapido
        equipments.add(new GachaponInfo.GachItem(1002793, true)); // Reverse Chive
        equipments.add(new GachaponInfo.GachItem(1002794, true)); // Reverse Conrad Henkel
        equipments.add(new GachaponInfo.GachItem(1002940, true)); // GMS Conrad Henkel 2
        equipments.add(new GachaponInfo.GachItem(1002972, true)); // Auf Haven Circlet
        equipments.add(new GachaponInfo.GachItem(1003139, true)); // Time Traveler's Laurel
        equipments.add(new GachaponInfo.GachItem(1003154, true)); // Marx Von Leon Helmet
        equipments.add(new GachaponInfo.GachItem(1003155, true)); // Alma Von Leon Helmet
        equipments.add(new GachaponInfo.GachItem(1003156, true)); // Fox Von Leon Helmet
        equipments.add(new GachaponInfo.GachItem(1003157, true)); // Nox Von Leon Helmet
        equipments.add(new GachaponInfo.GachItem(1003158, true)); // Cora Von Leon Helmet
        equipments.add(new GachaponInfo.GachItem(1003177, true)); // Agares Bloody Gear
        equipments.add(new GachaponInfo.GachItem(1003178, true)); // Eligos Bloody Gear
        equipments.add(new GachaponInfo.GachItem(1003179, true)); // Ipos Bloody Gear
        equipments.add(new GachaponInfo.GachItem(1003180, true)); // Halphas Bloody Gear
        equipments.add(new GachaponInfo.GachItem(1003181, true)); // Vepar Bloody Gear
        equipments.add(new GachaponInfo.GachItem(1003280, true)); // Abyss Fennel
        equipments.add(new GachaponInfo.GachItem(1003281, true)); // Abyss Coral
        equipments.add(new GachaponInfo.GachItem(1003282, true)); // Abyss Rapido
        equipments.add(new GachaponInfo.GachItem(1003283, true)); // Abyss Chive
        equipments.add(new GachaponInfo.GachItem(1003284, true)); // Abyss Conrad Henkel
        equipments.add(new GachaponInfo.GachItem(1003285, true)); // Fearless Fennel
        equipments.add(new GachaponInfo.GachItem(1003286, true)); // Fearless Coral
        equipments.add(new GachaponInfo.GachItem(1003287, true)); // Fearless Rapido
        equipments.add(new GachaponInfo.GachItem(1003288, true)); // Fearless Chive
        equipments.add(new GachaponInfo.GachItem(1003289, true)); // Fearless Conrad Henkel
        equipments.add(new GachaponInfo.GachItem(1003290, true)); // Victor Von Leon Helm
        equipments.add(new GachaponInfo.GachItem(1003291, true)); // Hex Von Leon Helm
        equipments.add(new GachaponInfo.GachItem(1003292, true)); // Celine Von Leon Helm
        equipments.add(new GachaponInfo.GachItem(1003293, true)); // Scar Von Leon Helm
        equipments.add(new GachaponInfo.GachItem(1003294, true)); // Mer Von Leon Helm
        equipments.add(new GachaponInfo.GachItem(1003410, true)); // Time Traveler's Laurel
        equipments.add(new GachaponInfo.GachItem(1003443, true)); // Imperial Brave Gear
        equipments.add(new GachaponInfo.GachItem(1003444, true)); // Imperial Memories Gear
        equipments.add(new GachaponInfo.GachItem(1003445, true)); // Imperial Sharpness Gear
        equipments.add(new GachaponInfo.GachItem(1003446, true)); // Imperial Swift Gear
        equipments.add(new GachaponInfo.GachItem(1003447, true)); // Imperial Fervent Gear
        equipments.add(new GachaponInfo.GachItem(1003534, true)); // Ludi Targa Hat
        equipments.add(new GachaponInfo.GachItem(1003535, true)); // Ludi Scarlion Hat
        equipments.add(new GachaponInfo.GachItem(1003579, true)); // Azure Loop
        equipments.add(new GachaponInfo.GachItem(1003589, true)); // Grand Agares Gear
        equipments.add(new GachaponInfo.GachItem(1003590, true)); // Grand Eligos Gear
        equipments.add(new GachaponInfo.GachItem(1003591, true)); // Grand Ipos Gear
        equipments.add(new GachaponInfo.GachItem(1003592, true)); // Grand Halphas Gear
        equipments.add(new GachaponInfo.GachItem(1003593, true)); // Grand Vepar Gear
        equipments.add(new GachaponInfo.GachItem(1003621, true)); // Chaos Pink Bean Hat
        equipments.add(new GachaponInfo.GachItem(1003622, true)); // Black Bean Hat
        equipments.add(new GachaponInfo.GachItem(1003628, true)); //  Feeble Intermediate Knight's Helm
        equipments.add(new GachaponInfo.GachItem(1003629, true)); //  Feeble High Knight's Helm
        equipments.add(new GachaponInfo.GachItem(1003660, true)); //  Intermediate Knight's Helm
        equipments.add(new GachaponInfo.GachItem(1003661, true)); //  Brave Intermediate Knight's Helm
        equipments.add(new GachaponInfo.GachItem(1003662, true)); //  Wise Intermediate Knight's Helm
        equipments.add(new GachaponInfo.GachItem(1003663, true)); //  Brave High Knight's Helm
        equipments.add(new GachaponInfo.GachItem(1003664, true)); //  Wise High Knight's Helm
        equipments.add(new GachaponInfo.GachItem(1003665, true)); //  Legendary High Knight's Helm
        equipments.add(new GachaponInfo.GachItem(1003689, true)); // Kaiser Brave Gear
        equipments.add(new GachaponInfo.GachItem(1003690, true)); // Luminous Memory Gear
        equipments.add(new GachaponInfo.GachItem(1003691, true)); // Angelic Buster Fervent Gear
        equipments.add(new GachaponInfo.GachItem(1003770, true)); // Sovereign Brave Gear
        equipments.add(new GachaponInfo.GachItem(1003771, true)); // Sovereign Memory Gear
        equipments.add(new GachaponInfo.GachItem(1003772, true)); // Sovereign Sharpness Gear
        equipments.add(new GachaponInfo.GachItem(1003773, true)); // Sovereign Swift Gear
        equipments.add(new GachaponInfo.GachItem(1003774, true)); // Sovereign Fervent Gear
        equipments.add(new GachaponInfo.GachItem(1003858, true)); // Greedy Lord Pirate Hat
        equipments.add(new GachaponInfo.GachItem(1003911, true)); // Saint Wing Knight Hat
        equipments.add(new GachaponInfo.GachItem(1003928, true)); // FrontierA Amethysian Cap
        equipments.add(new GachaponInfo.GachItem(1003933, true)); // Sapphire Clover Hat
        equipments.add(new GachaponInfo.GachItem(1003938, true)); // Opulent White Hat
        equipments.add(new GachaponInfo.GachItem(1003946, true)); // Revolution Hat
        equipments.add(new GachaponInfo.GachItem(1003947, true)); // Wings of Harmony
        equipments.add(new GachaponInfo.GachItem(1004030, true)); // Mean Evasion Helm
        equipments.add(new GachaponInfo.GachItem(1004031, true)); // Adroit Evasion Helm
        equipments.add(new GachaponInfo.GachItem(1004032, true)); // Absolute Evasion Helm
        equipments.add(new GachaponInfo.GachItem(1004161, true)); // Wood Theme Park Authority
        equipments.add(new GachaponInfo.GachItem(1004214, true)); // Necromancer Warrior Hat
        equipments.add(new GachaponInfo.GachItem(1004215, true)); // Necromancer Magician Hat
        equipments.add(new GachaponInfo.GachItem(1004216, true)); // Necromancer Sentinel Hat
        equipments.add(new GachaponInfo.GachItem(1004217, true)); // Necromancer Chaser Hat
        equipments.add(new GachaponInfo.GachItem(1004218, true)); // Necromancer Skipper Hat
        equipments.add(new GachaponInfo.GachItem(1004219, true)); // Eclectic Fennel
        equipments.add(new GachaponInfo.GachItem(1004220, true)); // Eclectic Coral
        equipments.add(new GachaponInfo.GachItem(1004221, true)); // Eclectic Rapid
        equipments.add(new GachaponInfo.GachItem(1004222, true)); // Eclectic Chive
        equipments.add(new GachaponInfo.GachItem(1004223, true)); // Eclectic Conrad Henkel
        equipments.add(new GachaponInfo.GachItem(1004224, true)); // Muspell Warrior Hat
        equipments.add(new GachaponInfo.GachItem(1004225, true)); // Muspell Magician Hat
        equipments.add(new GachaponInfo.GachItem(1004226, true)); // Muspell Bowman Hat
        equipments.add(new GachaponInfo.GachItem(1004227, true)); // Muspell Thief Hat
        equipments.add(new GachaponInfo.GachItem(1004228, true)); // Muspell Pirate Hat
        equipments.add(new GachaponInfo.GachItem(1004234, true)); // Royal Von Leon Warrior Helm
        equipments.add(new GachaponInfo.GachItem(1004235, true)); // Royal Von Leon Magician Helm
        equipments.add(new GachaponInfo.GachItem(1004236, true)); // Royal Von Leon Sentinel Helm
        equipments.add(new GachaponInfo.GachItem(1004237, true)); // Royal Von Leon Chaser Helm
        equipments.add(new GachaponInfo.GachItem(1004238, true)); // Royal Von Leon Skipper Helm
        equipments.add(new GachaponInfo.GachItem(1004492, true)); // Maple Treasure Cap
        equipments.add(new GachaponInfo.GachItem(1004549, true)); // Blackgate Cap
        equipments.add(new GachaponInfo.GachItem(1102172, true)); // Timeless Moonlight
        equipments.add(new GachaponInfo.GachItem(1102207, true)); // Goldensoul Cape
        equipments.add(new GachaponInfo.GachItem(1102231, true)); // Sirius Cape
        equipments.add(new GachaponInfo.GachItem(1102256, true)); // Ludibrium Cape
        equipments.add(new GachaponInfo.GachItem(1102262, true)); // Marx Von Leon Cape
        equipments.add(new GachaponInfo.GachItem(1102263, true)); // Alma Von Leon Cape
        equipments.add(new GachaponInfo.GachItem(1102264, true)); // Fox Von Leon Cape
        equipments.add(new GachaponInfo.GachItem(1102265, true)); // Nox Von Leon Cape
        equipments.add(new GachaponInfo.GachItem(1102266, true)); // Cora Von Leon Cape
        equipments.add(new GachaponInfo.GachItem(1102280, true)); // Agares Bloody Cape
        equipments.add(new GachaponInfo.GachItem(1102281, true)); // Eligos Bloody Cape
        equipments.add(new GachaponInfo.GachItem(1102282, true)); // Ipos Bloody Cape
        equipments.add(new GachaponInfo.GachItem(1102283, true)); // Halphas Bloody Cape
        equipments.add(new GachaponInfo.GachItem(1102284, true)); // Vepar Bloody Cape
        equipments.add(new GachaponInfo.GachItem(1102302, true)); // Navy Captain Cape
        equipments.add(new GachaponInfo.GachItem(1102311, true)); // Fearless Moonlight
        equipments.add(new GachaponInfo.GachItem(1102312, true)); // Victor Von Leon Cape
        equipments.add(new GachaponInfo.GachItem(1102313, true)); // Hex Von Leon Cape
        equipments.add(new GachaponInfo.GachItem(1102314, true)); // Celine Von Leon Cape
        equipments.add(new GachaponInfo.GachItem(1102315, true)); // Scar Von Leon Cape
        equipments.add(new GachaponInfo.GachItem(1102316, true)); // Mer Von Leon Cape
        equipments.add(new GachaponInfo.GachItem(1102362, true)); // Imperial Brave Cape
        equipments.add(new GachaponInfo.GachItem(1102363, true)); // Imperial Memories Cape
        equipments.add(new GachaponInfo.GachItem(1102364, true)); // Imperial Sharpness Cape
        equipments.add(new GachaponInfo.GachItem(1102365, true)); // Imperial Swift Cape
        equipments.add(new GachaponInfo.GachItem(1102366, true)); // Imperial Fervent Cape
        equipments.add(new GachaponInfo.GachItem(1102445, true)); // Grand Agares Cape
        equipments.add(new GachaponInfo.GachItem(1102446, true)); // Grand Eligos Cape
        equipments.add(new GachaponInfo.GachItem(1102447, true)); // Grand Ipos Cape
        equipments.add(new GachaponInfo.GachItem(1102448, true)); // Grand Halphas Cape
        equipments.add(new GachaponInfo.GachItem(1102449, true)); // Grand Vepar Cape
        equipments.add(new GachaponInfo.GachItem(1102476, true)); // Nova Hyades Cloak
        equipments.add(new GachaponInfo.GachItem(1102477, true)); // Nova Hermes Cloak
        equipments.add(new GachaponInfo.GachItem(1102478, true)); // Nova Charon Cloak
        equipments.add(new GachaponInfo.GachItem(1102479, true)); // Nova Lycaon Cloak
        equipments.add(new GachaponInfo.GachItem(1102480, true)); // Nova Altair Cloak
        equipments.add(new GachaponInfo.GachItem(1102489, true)); // Vampire Manteau
        equipments.add(new GachaponInfo.GachItem(1102498, true)); // Kaiser Brave Cape
        equipments.add(new GachaponInfo.GachItem(1102499, true)); // Luminous Memory Cape
        equipments.add(new GachaponInfo.GachItem(1102500, true)); // Angelic Buster Fervent Cape
        equipments.add(new GachaponInfo.GachItem(1102514, true)); // Sovereign Brave Cape
        equipments.add(new GachaponInfo.GachItem(1102515, true)); // Sovereign Memory Cape
        equipments.add(new GachaponInfo.GachItem(1102516, true)); // Sovereign Sharpness Cape
        equipments.add(new GachaponInfo.GachItem(1102517, true)); // Sovereign Swift Cape
        equipments.add(new GachaponInfo.GachItem(1102518, true)); // Sovereign Fervent Cape
        equipments.add(new GachaponInfo.GachItem(1102556, true)); // Spiegelmann's Cape of Moxy
        equipments.add(new GachaponInfo.GachItem(1102594, true)); // Saint Wing Knight Cape
        equipments.add(new GachaponInfo.GachItem(1102598, true)); // FrontierA Amethysian Star Cloak
        equipments.add(new GachaponInfo.GachItem(1102603, true)); // Sapphire Clover Wing
        equipments.add(new GachaponInfo.GachItem(1102606, true)); // Opulent White Cape
        equipments.add(new GachaponInfo.GachItem(1102612, true)); // Revolution Cape
        equipments.add(new GachaponInfo.GachItem(1102647, true)); // Absolute Evasion Cape
        equipments.add(new GachaponInfo.GachItem(1102713, true)); // Royal Von Leon Warrior Cape
        equipments.add(new GachaponInfo.GachItem(1102714, true)); // Royal Von Leon Mage Cape
        equipments.add(new GachaponInfo.GachItem(1102715, true)); // Royal Von Leon Sentinel Cape
        equipments.add(new GachaponInfo.GachItem(1102716, true)); // Royal Von Leon Chaser Cape
        equipments.add(new GachaponInfo.GachItem(1102717, true)); // Royal Von Leon Skipper Cape
        equipments.add(new GachaponInfo.GachItem(1102828, true)); // Maple Treasure cape
        equipments.add(new GachaponInfo.GachItem(1102840, true)); // Blackgate Cape
        equipments.add(new GachaponInfo.GachItem(1042243, true)); // Horntail's Ab T-Shirt
        equipments.add(new GachaponInfo.GachItem(1042366, true)); // Sapphire Clover T-shirt
        equipments.add(new GachaponInfo.GachItem(1942002, true)); // Reverse Mask
        equipments.add(new GachaponInfo.GachItem(1942004, true)); // Abyss Mask
        equipments.add(new GachaponInfo.GachItem(1952002, true)); // Reverse Pendant
        equipments.add(new GachaponInfo.GachItem(1952004, true)); // Abyss Pendant
        equipments.add(new GachaponInfo.GachItem(1962002, true)); // Reverse Wings
        equipments.add(new GachaponInfo.GachItem(1962004, true)); // Abyss Wings
        equipments.add(new GachaponInfo.GachItem(1972002, true)); // Reverse Tail
        equipments.add(new GachaponInfo.GachItem(1972004, true)); // Abyss Tail
        equipments.add(new GachaponInfo.GachItem(1082163, true)); // Red Hunter Gloves
        equipments.add(new GachaponInfo.GachItem(1082164, true)); // Blue Elemental Gloves
        equipments.add(new GachaponInfo.GachItem(1082167, true)); // Black Garina Gloves
        equipments.add(new GachaponInfo.GachItem(1082168, true)); // Blue Dragon Gauntlet
        equipments.add(new GachaponInfo.GachItem(1082216, true)); // Canopus Gloves
        equipments.add(new GachaponInfo.GachItem(1082234, true)); // Timeless Bergamot
        equipments.add(new GachaponInfo.GachItem(1082235, true)); // Timeless Hermosa
        equipments.add(new GachaponInfo.GachItem(1082236, true)); // Timeless Presto
        equipments.add(new GachaponInfo.GachItem(1082237, true)); // Timeless Lubav
        equipments.add(new GachaponInfo.GachItem(1082238, true)); // Timeless Charlston
        equipments.add(new GachaponInfo.GachItem(1082239, true)); // Reverse Bergamot
        equipments.add(new GachaponInfo.GachItem(1082240, true)); // Reverse Hermosa
        equipments.add(new GachaponInfo.GachItem(1082241, true)); // Reverse Presto
        equipments.add(new GachaponInfo.GachItem(1082242, true)); // Reverse Lubav
        equipments.add(new GachaponInfo.GachItem(1082243, true)); // Reverse Charlston
        equipments.add(new GachaponInfo.GachItem(1082285, true)); // Marx Von Leon Gloves
        equipments.add(new GachaponInfo.GachItem(1082286, true)); // Alma Von Leon Gloves
        equipments.add(new GachaponInfo.GachItem(1082287, true)); // Fox Von Leon Gloves
        equipments.add(new GachaponInfo.GachItem(1082288, true)); // Nox Von Leon Gloves
        equipments.add(new GachaponInfo.GachItem(1082289, true)); // Cora Von Leon Gloves
        equipments.add(new GachaponInfo.GachItem(1082300, true)); // Agares Bloody Arm Cannon
        equipments.add(new GachaponInfo.GachItem(1082301, true)); // Eligos Bloody Arm Cannon
        equipments.add(new GachaponInfo.GachItem(1082302, true)); // Ipos Bloody Arm Cannon
        equipments.add(new GachaponInfo.GachItem(1082303, true)); // Halphas Bloody Arm Cannon
        equipments.add(new GachaponInfo.GachItem(1082304, true)); // Vepar Bloody Arm Cannon
        equipments.add(new GachaponInfo.GachItem(1082328, true)); // Abyss Bergamot
        equipments.add(new GachaponInfo.GachItem(1082329, true)); // Abyss Hermosa
        equipments.add(new GachaponInfo.GachItem(1082330, true)); // Abyss Presto
        equipments.add(new GachaponInfo.GachItem(1082331, true)); // Abyss Lubav
        equipments.add(new GachaponInfo.GachItem(1082332, true)); // Abyss Charlston
        equipments.add(new GachaponInfo.GachItem(1082333, true)); // Fearless Bergamot
        equipments.add(new GachaponInfo.GachItem(1082334, true)); // Fearless Hermosa
        equipments.add(new GachaponInfo.GachItem(1082335, true)); // Fearless Presto
        equipments.add(new GachaponInfo.GachItem(1082336, true)); // Fearless Lubav
        equipments.add(new GachaponInfo.GachItem(1082337, true)); // Fearless Charlston
        equipments.add(new GachaponInfo.GachItem(1082338, true)); // Victor Von Leon Hand Guard
        equipments.add(new GachaponInfo.GachItem(1082339, true)); // Hex Von Leon Hand Guard
        equipments.add(new GachaponInfo.GachItem(1082340, true)); // Celine Von Leon Hand Guard
        equipments.add(new GachaponInfo.GachItem(1082341, true)); // Scar Von Leon Hand Guard
        equipments.add(new GachaponInfo.GachItem(1082342, true)); // Mer Von Leon Hand Guard
        equipments.add(new GachaponInfo.GachItem(1082392, true)); // Hero's Gloves
        equipments.add(new GachaponInfo.GachItem(1082393, true)); // Mu Gong's Gloves
        equipments.add(new GachaponInfo.GachItem(1082394, true)); // So Gong's Gloves
        equipments.add(new GachaponInfo.GachItem(1082400, true)); // Navy Captain Gloves
        equipments.add(new GachaponInfo.GachItem(1082402, true)); // Flamekeeper Glove
        equipments.add(new GachaponInfo.GachItem(1082416, true)); // Imperial Brave Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082417, true)); // Imperial Memories Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082418, true)); // Imperial Sharpness Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082419, true)); // Imperial Swift Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082420, true)); // Imperial Fervent Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082431, true)); // Fantastic Friends Bracelet
        equipments.add(new GachaponInfo.GachItem(1082445, true)); //  Feeble Intermediate Knight's Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082446, true)); //  Feeble High Knight's Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082455, true)); //  Intermediate Knight's Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082456, true)); //  Brave Intermediate Knight's Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082457, true)); //  Wise Intermediate Knight's Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082458, true)); //  Brave High Knight's Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082459, true)); //  Wise High Knight's Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082460, true)); //  Legendary High Knight's Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082466, true)); // Grand Agares Gauntlet
        equipments.add(new GachaponInfo.GachItem(1082467, true)); // Grand Eligos Gauntlet
        equipments.add(new GachaponInfo.GachItem(1082468, true)); // Grand Ipos Gauntlet
        equipments.add(new GachaponInfo.GachItem(1082469, true)); // Grand Helphas Gauntlet
        equipments.add(new GachaponInfo.GachItem(1082470, true)); // Grand Vepar Gauntlet
        equipments.add(new GachaponInfo.GachItem(1082490, true)); // Kaiser Brave Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082491, true)); // Luminous Memory Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082492, true)); // Angelic Buster Fervent Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082506, true)); // Sovereign Brave Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082507, true)); // Sovereign Memory Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082508, true)); // Sovereign Sharpness Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082509, true)); // Sovereign Swift Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082510, true)); // Sovereign Fervent Gauntlets
        equipments.add(new GachaponInfo.GachItem(1082535, true)); // Saint Wing Knight Gloves
        equipments.add(new GachaponInfo.GachItem(1082537, true)); // FrontierA Amethysian Tattoo
        equipments.add(new GachaponInfo.GachItem(1082539, true)); // Sapphire Clover Gloves
        equipments.add(new GachaponInfo.GachItem(1082540, true)); // Revolution Gloves
        equipments.add(new GachaponInfo.GachItem(1082566, true)); // Mean Evasion Gloves
        equipments.add(new GachaponInfo.GachItem(1082567, true)); // Adroit Evasion Gloves
        equipments.add(new GachaponInfo.GachItem(1082568, true)); // Absolute Evasion Gloves
        equipments.add(new GachaponInfo.GachItem(1082569, true)); // Adroit Evasion Gloves
        equipments.add(new GachaponInfo.GachItem(1082570, true)); // Absolute Evasion Gloves
        equipments.add(new GachaponInfo.GachItem(1082593, true)); // Necromancer Warrior Gloves
        equipments.add(new GachaponInfo.GachItem(1082594, true)); // Necromancer Magician Gloves
        equipments.add(new GachaponInfo.GachItem(1082595, true)); // Necromancer Sentinel Gloves
        equipments.add(new GachaponInfo.GachItem(1082596, true)); // Necromancer Chaser Gloves
        equipments.add(new GachaponInfo.GachItem(1082597, true)); // Necromancer Skipper Gloves
        equipments.add(new GachaponInfo.GachItem(1082598, true)); // Eclectic Bergamot
        equipments.add(new GachaponInfo.GachItem(1082599, true)); // Eclectic Hermosa
        equipments.add(new GachaponInfo.GachItem(1082600, true)); // Eclectic Presto
        equipments.add(new GachaponInfo.GachItem(1082601, true)); // Eclectic Lubav
        equipments.add(new GachaponInfo.GachItem(1082602, true)); // Eclectic Charlston
        equipments.add(new GachaponInfo.GachItem(1082603, true)); // Muspell Warrior Gloves
        equipments.add(new GachaponInfo.GachItem(1082604, true)); // Muspell Magician Gloves
        equipments.add(new GachaponInfo.GachItem(1082605, true)); // Muspell Bowman Gloves
        equipments.add(new GachaponInfo.GachItem(1082606, true)); // Muspell Thief Gloves
        equipments.add(new GachaponInfo.GachItem(1082607, true)); // Muspell Pirate Gloves
        equipments.add(new GachaponInfo.GachItem(1082613, true)); // Royal Von Leon Warrior Hands
        equipments.add(new GachaponInfo.GachItem(1082614, true)); // Royal Von Leon Mage Hands
        equipments.add(new GachaponInfo.GachItem(1082615, true)); // Royal Von Leon Sentinel Hands
        equipments.add(new GachaponInfo.GachItem(1082616, true)); // Royal Von Leon Chaser Hands
        equipments.add(new GachaponInfo.GachItem(1082617, true)); // Royal Von Leon Skipper Hands
        equipments.add(new GachaponInfo.GachItem(1082647, true)); // Maple Treasure gloves
        equipments.add(new GachaponInfo.GachItem(1082658, true)); // Blackgate Gloves
        equipments.add(new GachaponInfo.GachItem(1082660, true)); // Expert's Gloves
        equipments.add(new GachaponInfo.GachItem(1082661, true)); // Expert Apprentice's Gloves
        equipments.add(new GachaponInfo.GachItem(1082662, true)); // Unidentified Training Gloves
        equipments.add(new GachaponInfo.GachItem(1050253, true)); // Proton Suit
        equipments.add(new GachaponInfo.GachItem(1051309, true)); // Proton Suit
        equipments.add(new GachaponInfo.GachItem(1052071, true)); // Red Mantle
        equipments.add(new GachaponInfo.GachItem(1052072, true)); // Black Garina
        equipments.add(new GachaponInfo.GachItem(1052075, true)); // Blue Dragon Armor
        equipments.add(new GachaponInfo.GachItem(1052076, true)); // Blue Czar
        equipments.add(new GachaponInfo.GachItem(1052134, true)); // Canopus Suit
        equipments.add(new GachaponInfo.GachItem(1052155, true)); // Timeless Taragon
        equipments.add(new GachaponInfo.GachItem(1052156, true)); // Timeless Myst Blue
        equipments.add(new GachaponInfo.GachItem(1052157, true)); // Timeless Evernew
        equipments.add(new GachaponInfo.GachItem(1052158, true)); // Timeless Prinsid
        equipments.add(new GachaponInfo.GachItem(1052159, true)); // Timeless Burgunt
        equipments.add(new GachaponInfo.GachItem(1052160, true)); // Reverse Taragon
        equipments.add(new GachaponInfo.GachItem(1052161, true)); // Reverse Myst Blue
        equipments.add(new GachaponInfo.GachItem(1052162, true)); // Reverse Evernew
        equipments.add(new GachaponInfo.GachItem(1052163, true)); // Reverse Prinsid
        equipments.add(new GachaponInfo.GachItem(1052164, true)); // Reverse Burgunt
        equipments.add(new GachaponInfo.GachItem(1052299, true)); // Marx Von Leon Battle Suit
        equipments.add(new GachaponInfo.GachItem(1052300, true)); // Alma Von Leon Battle Suit
        equipments.add(new GachaponInfo.GachItem(1052301, true)); // Fox Von Leon Battle Suit
        equipments.add(new GachaponInfo.GachItem(1052302, true)); // Nox Von Leon Battle Suit
        equipments.add(new GachaponInfo.GachItem(1052303, true)); // Cora Von Leon Battle Suit
        equipments.add(new GachaponInfo.GachItem(1052319, true)); // Agares Bloody Mail
        equipments.add(new GachaponInfo.GachItem(1052320, true)); // Eligos Bloody Mail
        equipments.add(new GachaponInfo.GachItem(1052321, true)); // Ipos Bloody Mail
        equipments.add(new GachaponInfo.GachItem(1052322, true)); // Halphas Bloody Mail
        equipments.add(new GachaponInfo.GachItem(1052323, true)); // Vepar Bloody Mail
        equipments.add(new GachaponInfo.GachItem(1052374, true)); // Abyss Taragon
        equipments.add(new GachaponInfo.GachItem(1052375, true)); // Abyss Myst Blue
        equipments.add(new GachaponInfo.GachItem(1052376, true)); // Abyss Evernew
        equipments.add(new GachaponInfo.GachItem(1052377, true)); // Abyss Prinsid
        equipments.add(new GachaponInfo.GachItem(1052378, true)); // Abyss Burgunt
        equipments.add(new GachaponInfo.GachItem(1052379, true)); // Fearless Taragon
        equipments.add(new GachaponInfo.GachItem(1052380, true)); // Fearless Myst Blue
        equipments.add(new GachaponInfo.GachItem(1052381, true)); // Fearless Evernew
        equipments.add(new GachaponInfo.GachItem(1052382, true)); // Fearless Prinsid
        equipments.add(new GachaponInfo.GachItem(1052383, true)); // Fearless Burgunt
        equipments.add(new GachaponInfo.GachItem(1052384, true)); // Victor Von Leon Battle Suit
        equipments.add(new GachaponInfo.GachItem(1052385, true)); // Hex Von Leon Battle Suit
        equipments.add(new GachaponInfo.GachItem(1052386, true)); // Celine Von Leon Battle Suit
        equipments.add(new GachaponInfo.GachItem(1052387, true)); // Scar Von Leon Battle Suit
        equipments.add(new GachaponInfo.GachItem(1052388, true)); // Mer Von Leon Battle Suit
        equipments.add(new GachaponInfo.GachItem(1052429, true)); // Imperial Brave Mail
        equipments.add(new GachaponInfo.GachItem(1052430, true)); // Imperial Memories Mail
        equipments.add(new GachaponInfo.GachItem(1052431, true)); // Imperial Sharpness Mail
        equipments.add(new GachaponInfo.GachItem(1052432, true)); // Imperial Swift Mail
        equipments.add(new GachaponInfo.GachItem(1052433, true)); // Imperial Fervent Mail
        equipments.add(new GachaponInfo.GachItem(1052488, true)); //  Intermediate Knight's Full Body Armor
        equipments.add(new GachaponInfo.GachItem(1052489, true)); //  Brave Intermediate Knight's Full Body Armor
        equipments.add(new GachaponInfo.GachItem(1052490, true)); //  Wise Intermediate Knight's Full Body Armor
        equipments.add(new GachaponInfo.GachItem(1052491, true)); //  Brave High Knight's Full Body Armor
        equipments.add(new GachaponInfo.GachItem(1052492, true)); //  Wise High Knight's Full Body Armor
        equipments.add(new GachaponInfo.GachItem(1052493, true)); //  Legendary High Knight's Full Body Armor
        equipments.add(new GachaponInfo.GachItem(1052498, true)); // Grand Agares Mail
        equipments.add(new GachaponInfo.GachItem(1052499, true)); // Grand Eligos Mail
        equipments.add(new GachaponInfo.GachItem(1052500, true)); // Grand Ipos Mail
        equipments.add(new GachaponInfo.GachItem(1052501, true)); // Grand Helphas Mail
        equipments.add(new GachaponInfo.GachItem(1052502, true)); // Grand Vepar Mail
        equipments.add(new GachaponInfo.GachItem(1052526, true)); // Chaos Pink Bean Suit
        equipments.add(new GachaponInfo.GachItem(1052527, true)); // Black Bean Suit
        equipments.add(new GachaponInfo.GachItem(1052534, true)); //  Feeble Intermediate Knight's Full Body Armor
        equipments.add(new GachaponInfo.GachItem(1052535, true)); //  Feeble High Knight's Full Body Armor
        equipments.add(new GachaponInfo.GachItem(1052545, true)); // Kaiser Brave Mail
        equipments.add(new GachaponInfo.GachItem(1052546, true)); // Luminous Memory Mail
        equipments.add(new GachaponInfo.GachItem(1052547, true)); // Angelic Buster Fervent Mail
        equipments.add(new GachaponInfo.GachItem(1052580, true)); // Sovereign Brave Mail
        equipments.add(new GachaponInfo.GachItem(1052581, true)); // Sovereign Memory Mail
        equipments.add(new GachaponInfo.GachItem(1052582, true)); // Sovereign Sharpness Mail
        equipments.add(new GachaponInfo.GachItem(1052583, true)); // Sovereign Swift Mail
        equipments.add(new GachaponInfo.GachItem(1052584, true)); // Sovereign Fervent Mail
        equipments.add(new GachaponInfo.GachItem(1052632, true)); // Saint Wing Knight Suit
        equipments.add(new GachaponInfo.GachItem(1052633, true)); // Saint Wing Knight Suit
        equipments.add(new GachaponInfo.GachItem(1052640, true)); // FrontierA Amethysian Suit
        equipments.add(new GachaponInfo.GachItem(1052645, true)); // Opulent White Suit
        equipments.add(new GachaponInfo.GachItem(1052647, true)); // Revolution Suit
        equipments.add(new GachaponInfo.GachItem(1052688, true)); // Mean Evasion Full Armor
        equipments.add(new GachaponInfo.GachItem(1052689, true)); // Adroit Evasion Full Armor
        equipments.add(new GachaponInfo.GachItem(1052690, true)); // Absolute Evasion Full Armor
        equipments.add(new GachaponInfo.GachItem(1052784, true)); // Necromancer Warrior Suit
        equipments.add(new GachaponInfo.GachItem(1052785, true)); // Necromancer Magician Suit
        equipments.add(new GachaponInfo.GachItem(1052786, true)); // Necromancer Sentinel Suit
        equipments.add(new GachaponInfo.GachItem(1052787, true)); // Necromancer Chaser Suit
        equipments.add(new GachaponInfo.GachItem(1052788, true)); // Necromancer Skipper Suit
        equipments.add(new GachaponInfo.GachItem(1052789, true)); // Eclectic Taragon
        equipments.add(new GachaponInfo.GachItem(1052790, true)); // Eclectic Myst Blue
        equipments.add(new GachaponInfo.GachItem(1052791, true)); // Eclectic Evernew
        equipments.add(new GachaponInfo.GachItem(1052792, true)); // Eclectic Prinsid
        equipments.add(new GachaponInfo.GachItem(1052793, true)); // Eclectic Burgunt
        equipments.add(new GachaponInfo.GachItem(1052794, true)); // Muspell Warrior Suit
        equipments.add(new GachaponInfo.GachItem(1052795, true)); // Muspell Magician Suit
        equipments.add(new GachaponInfo.GachItem(1052796, true)); // Muspell Bowman Suit
        equipments.add(new GachaponInfo.GachItem(1052797, true)); // Muspell Thief Suit
        equipments.add(new GachaponInfo.GachItem(1052798, true)); // Muspell Pirate Suit
        equipments.add(new GachaponInfo.GachItem(1052804, true)); // Royal Von Leon Warrior Suit
        equipments.add(new GachaponInfo.GachItem(1052805, true)); // Royal Von Leon Mage Suit
        equipments.add(new GachaponInfo.GachItem(1052806, true)); // Royal Von Leon Sentinel Suit
        equipments.add(new GachaponInfo.GachItem(1052807, true)); // Royal Von Leon Chaser Suit
        equipments.add(new GachaponInfo.GachItem(1052808, true)); // Royal Von Leon Skipper Suit
        equipments.add(new GachaponInfo.GachItem(1052929, true)); // Maple Treasure outfit
        equipments.add(new GachaponInfo.GachItem(1052952, true)); // Blackgate Armor
        equipments.add(new GachaponInfo.GachItem(1612004, true)); // Pure Gold Engine
        equipments.add(new GachaponInfo.GachItem(1622004, true)); // Pure Gold Machine Arm
        equipments.add(new GachaponInfo.GachItem(1632003, true)); // Pure Gold Machine Leg
        equipments.add(new GachaponInfo.GachItem(1642003, true)); // Pure Gold Body Frame
        equipments.add(new GachaponInfo.GachItem(1652004, true)); // Pure Gold Transistor
        equipments.add(new GachaponInfo.GachItem(1062178, true)); // Sapphire Clover Pants
        equipments.add(new GachaponInfo.GachItem(1112021, true)); // Bounty Master's Ring
        equipments.add(new GachaponInfo.GachItem(1112438, true)); // Last Unwelcome Guest Ring
        equipments.add(new GachaponInfo.GachItem(1112439, true)); // VIP Ring
        equipments.add(new GachaponInfo.GachItem(1112568, true)); // Nimble Ring IV
        equipments.add(new GachaponInfo.GachItem(1112569, true)); // Hyper Ring I
        equipments.add(new GachaponInfo.GachItem(1112570, true)); // Strength Ring V
        equipments.add(new GachaponInfo.GachItem(1112571, true)); // Intelligence Ring V
        equipments.add(new GachaponInfo.GachItem(1112572, true)); // Dexterity Ring V
        equipments.add(new GachaponInfo.GachItem(1112573, true)); // Luck Ring V
        equipments.add(new GachaponInfo.GachItem(1112574, true)); // Swift Deadshot Ring II
        equipments.add(new GachaponInfo.GachItem(1112575, true)); // Strength Ring VI
        equipments.add(new GachaponInfo.GachItem(1112576, true)); // Intelligence Ring VI
        equipments.add(new GachaponInfo.GachItem(1112577, true)); // Dexterity Ring VI
        equipments.add(new GachaponInfo.GachItem(1112578, true)); // Luck Ring VI
        equipments.add(new GachaponInfo.GachItem(1112579, true)); // Strength Ring VII
        equipments.add(new GachaponInfo.GachItem(1112580, true)); // Intelligence Ring VII
        equipments.add(new GachaponInfo.GachItem(1112581, true)); // Dexterity Ring VII
        equipments.add(new GachaponInfo.GachItem(1112582, true)); // Luck Ring VII
        equipments.add(new GachaponInfo.GachItem(1112584, true)); // Rising Sun Ring
        equipments.add(new GachaponInfo.GachItem(1112588, true)); // Mighty Ring II
        equipments.add(new GachaponInfo.GachItem(1112589, true)); // Mighty Ring III
        equipments.add(new GachaponInfo.GachItem(1112590, true)); // Mighty Ring IV
        equipments.add(new GachaponInfo.GachItem(1112612, true)); // Legendary Crusader Ring
        equipments.add(new GachaponInfo.GachItem(1112613, true)); // Crusader's Zeal Ring
        equipments.add(new GachaponInfo.GachItem(1112662, true)); // Blazing Sun Ring
        equipments.add(new GachaponInfo.GachItem(1112673, true)); // Legendary Maple Gold Ring
        equipments.add(new GachaponInfo.GachItem(1112678, true)); // Gold Cross Ring
        equipments.add(new GachaponInfo.GachItem(1112679, true)); // Platinum Cross Ring
        equipments.add(new GachaponInfo.GachItem(1112683, true)); // Ephenia's Ring
        equipments.add(new GachaponInfo.GachItem(1112712, true)); // Imperial Ring
        equipments.add(new GachaponInfo.GachItem(1112759, true)); // Darkness Ring 2
        equipments.add(new GachaponInfo.GachItem(1112760, true)); // Darkness Ring 3
        equipments.add(new GachaponInfo.GachItem(1112782, true)); // Kaiser Imperial Ring
        equipments.add(new GachaponInfo.GachItem(1112783, true)); // Luminous Imperial Ring
        equipments.add(new GachaponInfo.GachItem(1112784, true)); // Angelic Buster Imperial Ring
        equipments.add(new GachaponInfo.GachItem(1112792, true)); // Master First Ring
        equipments.add(new GachaponInfo.GachItem(1113016, true)); // Sovereign Ring
        equipments.add(new GachaponInfo.GachItem(1113039, true)); // Fairy Queen's Ring
        equipments.add(new GachaponInfo.GachItem(1113054, true)); // Sapphire Clover Ring
        equipments.add(new GachaponInfo.GachItem(1113069, true)); // Pivotal Adventure Ring
        equipments.add(new GachaponInfo.GachItem(1113070, true)); // Scarlet Ring
        equipments.add(new GachaponInfo.GachItem(1113071, true)); // Zero Gratias Ring
        equipments.add(new GachaponInfo.GachItem(1113072, true)); // Cracked Gollux Ring
        equipments.add(new GachaponInfo.GachItem(1113073, true)); // Solid Gollux Ring
        equipments.add(new GachaponInfo.GachItem(1113089, true)); // Ifia's Ring
        equipments.add(new GachaponInfo.GachItem(1113094, true)); // Beast Tamer's Dark Critical Ring
        equipments.add(new GachaponInfo.GachItem(1113098, true)); // Ring of Restraint
        equipments.add(new GachaponInfo.GachItem(1113099, true)); // Ultimatum Ring
        equipments.add(new GachaponInfo.GachItem(1113100, true)); // Limit Ring
        equipments.add(new GachaponInfo.GachItem(1113101, true)); // Health Cut Ring
        equipments.add(new GachaponInfo.GachItem(1113102, true)); // Mana Cut Ring
        equipments.add(new GachaponInfo.GachItem(1113103, true)); // Durability Ring
        equipments.add(new GachaponInfo.GachItem(1113104, true)); // Critical Damage Ring
        equipments.add(new GachaponInfo.GachItem(1113105, true)); // Critical Defense Ring
        equipments.add(new GachaponInfo.GachItem(1113106, true)); // Critical Shift Ring
        equipments.add(new GachaponInfo.GachItem(1113107, true)); // Stance Shift Ring
        equipments.add(new GachaponInfo.GachItem(1113108, true)); // Totalling Ring
        equipments.add(new GachaponInfo.GachItem(1113109, true)); // Level Jump S Ring
        equipments.add(new GachaponInfo.GachItem(1113110, true)); // Level Jump D Ring
        equipments.add(new GachaponInfo.GachItem(1113111, true)); // Level Jump I Ring
        equipments.add(new GachaponInfo.GachItem(1113112, true)); // Level Jump L Ring
        equipments.add(new GachaponInfo.GachItem(1113113, true)); // Weapon Jump S Ring
        equipments.add(new GachaponInfo.GachItem(1113114, true)); // Weapon Jump D Ring
        equipments.add(new GachaponInfo.GachItem(1113115, true)); // Weapon Jump I Ring
        equipments.add(new GachaponInfo.GachItem(1113116, true)); // Weapon Jump L Ring
        equipments.add(new GachaponInfo.GachItem(1113117, true)); // Swift Ring
        equipments.add(new GachaponInfo.GachItem(1113118, true)); // Overdrive Ring
        equipments.add(new GachaponInfo.GachItem(1113119, true)); // Berserker Ring
        equipments.add(new GachaponInfo.GachItem(1113120, true)); // Reflective Ring
        equipments.add(new GachaponInfo.GachItem(1113121, true)); // Cleansing Ring
        equipments.add(new GachaponInfo.GachItem(1113122, true)); // Risk Taker Ring
        equipments.add(new GachaponInfo.GachItem(1113123, true)); // Crisis H Ring
        equipments.add(new GachaponInfo.GachItem(1113124, true)); // Crisis M Ring
        equipments.add(new GachaponInfo.GachItem(1113125, true)); // Crisis HM Ring
        equipments.add(new GachaponInfo.GachItem(1113126, true)); // Clean Stance Ring
        equipments.add(new GachaponInfo.GachItem(1113127, true)); // Clean Defense Ring
        equipments.add(new GachaponInfo.GachItem(1113128, true)); // Tower Boost Ring
        equipments.add(new GachaponInfo.GachItem(1113149, true)); // Silver Blossom Ring
        equipments.add(new GachaponInfo.GachItem(1113170, true)); // Pivotal Adventure Ring
        equipments.add(new GachaponInfo.GachItem(1113172, true)); // Grand Adventure Ring
        equipments.add(new GachaponInfo.GachItem(1113185, true)); // Blackgate Ring
        equipments.add(new GachaponInfo.GachItem(1113210, true)); // Golden Flower Ring
        equipments.add(new GachaponInfo.GachItem(1113236, true)); // Treasure Hunter John's Ring
        equipments.add(new GachaponInfo.GachItem(1113254, true)); // Very Grand Ribbit Ring
        equipments.add(new GachaponInfo.GachItem(1113255, true)); // Very Sturdy Krrr Ring
        equipments.add(new GachaponInfo.GachItem(1113256, true)); // Very Fatal Rawr Ring
        equipments.add(new GachaponInfo.GachItem(1113257, true)); // Very Grand Pew Pew Ring
        equipments.add(new GachaponInfo.GachItem(1113278, true)); // Fishing Ring
        equipments.add(new GachaponInfo.GachItem(1114300, true)); // Vengeful Ring
        equipments.add(new GachaponInfo.GachItem(1114301, true)); // Reboot Vengeful Ring
        equipments.add(new GachaponInfo.GachItem(1092041, true)); // Skill-Earning Shield
        equipments.add(new GachaponInfo.GachItem(1092042, true)); // Gellerhead Shield
        equipments.add(new GachaponInfo.GachItem(1092049, true)); // Dragon Khanjar
        equipments.add(new GachaponInfo.GachItem(1092057, true)); // Timeless Prelude
        equipments.add(new GachaponInfo.GachItem(1092058, true)); // Timeless Kite Shield
        equipments.add(new GachaponInfo.GachItem(1092059, true)); // Timeless List
        equipments.add(new GachaponInfo.GachItem(1092060, true)); // Blue Dragon Shield
        equipments.add(new GachaponInfo.GachItem(1092073, true)); // Last Unwelcome Guest Warrior Shield
        equipments.add(new GachaponInfo.GachItem(1092074, true)); // VIP Warrior Shield
        equipments.add(new GachaponInfo.GachItem(1092078, true)); // Last Unwelcome Guest Magician Shield
        equipments.add(new GachaponInfo.GachItem(1092079, true)); // VIP Magician Shield
        equipments.add(new GachaponInfo.GachItem(1092083, true)); // Last Unwelcome Guest Thief Shield
        equipments.add(new GachaponInfo.GachItem(1092084, true)); // VIP Thief Shield
        equipments.add(new GachaponInfo.GachItem(1092087, true)); // Deimos Warrior Shield
        equipments.add(new GachaponInfo.GachItem(1092088, true)); // Deimos Shadow Shield
        equipments.add(new GachaponInfo.GachItem(1092089, true)); // Deimos Sage Shield
        equipments.add(new GachaponInfo.GachItem(1092092, true)); // Fearless Prelude
        equipments.add(new GachaponInfo.GachItem(1092093, true)); // Fearless Kite Shield
        equipments.add(new GachaponInfo.GachItem(1092094, true)); // Fearless Wristguard
        equipments.add(new GachaponInfo.GachItem(1098008, true)); // Maple Treasure Soul Shield
        equipments.add(new GachaponInfo.GachItem(1099014, true)); // Maple Treasure Force Shield
        equipments.add(new GachaponInfo.GachItem(1072268, true)); // Blue Elemental Shoes
        equipments.add(new GachaponInfo.GachItem(1072269, true)); // Red Hunter Shoes
        equipments.add(new GachaponInfo.GachItem(1072272, true)); // Black Garina Shoes
        equipments.add(new GachaponInfo.GachItem(1072273, true)); // Blue Dragon Boots
        equipments.add(new GachaponInfo.GachItem(1072321, true)); // Canopus Boots
        equipments.add(new GachaponInfo.GachItem(1072355, true)); // Timeless Grabbe
        equipments.add(new GachaponInfo.GachItem(1072356, true)); // Timeless Cabatina
        equipments.add(new GachaponInfo.GachItem(1072357, true)); // Timeless Rontano
        equipments.add(new GachaponInfo.GachItem(1072358, true)); // Timeless Moonsteed
        equipments.add(new GachaponInfo.GachItem(1072359, true)); // Timeless Faraon
        equipments.add(new GachaponInfo.GachItem(1072361, true)); // Reverse Grabbe
        equipments.add(new GachaponInfo.GachItem(1072362, true)); // Reverse Cabatina
        equipments.add(new GachaponInfo.GachItem(1072363, true)); // Reverse Rontano
        equipments.add(new GachaponInfo.GachItem(1072364, true)); // Reverse Moonsteed
        equipments.add(new GachaponInfo.GachItem(1072365, true)); // Reverse Faraon
        equipments.add(new GachaponInfo.GachItem(1072471, true)); // Marx Von Leon War Boots
        equipments.add(new GachaponInfo.GachItem(1072472, true)); // Alma Von Leon War Boots
        equipments.add(new GachaponInfo.GachItem(1072473, true)); // Fox Von Leon War Boots
        equipments.add(new GachaponInfo.GachItem(1072474, true)); // Nox Von Leon War Boots
        equipments.add(new GachaponInfo.GachItem(1072475, true)); // Cora Von Leon War Boots
        equipments.add(new GachaponInfo.GachItem(1072490, true)); // Agares Bloody Boots
        equipments.add(new GachaponInfo.GachItem(1072491, true)); // Eligos Bloody Boots
        equipments.add(new GachaponInfo.GachItem(1072492, true)); // Ipos Bloody Boots
        equipments.add(new GachaponInfo.GachItem(1072493, true)); // Halphas Bloody Boots
        equipments.add(new GachaponInfo.GachItem(1072494, true)); // Vepar Bloody Boots
        equipments.add(new GachaponInfo.GachItem(1072544, true)); // Abyss Grabbe
        equipments.add(new GachaponInfo.GachItem(1072545, true)); // Abyss Cabatina
        equipments.add(new GachaponInfo.GachItem(1072546, true)); // Abyss Rontano
        equipments.add(new GachaponInfo.GachItem(1072547, true)); // Abyss Moonsteed
        equipments.add(new GachaponInfo.GachItem(1072548, true)); // Abyss Faraon
        equipments.add(new GachaponInfo.GachItem(1072549, true)); // Fearless Grabbe
        equipments.add(new GachaponInfo.GachItem(1072550, true)); // Fearless Cabatina
        equipments.add(new GachaponInfo.GachItem(1072551, true)); // Fearless Rontano
        equipments.add(new GachaponInfo.GachItem(1072552, true)); // Fearless Moonsteed
        equipments.add(new GachaponInfo.GachItem(1072553, true)); // Fearless Faraon
        equipments.add(new GachaponInfo.GachItem(1072554, true)); // Victor Von Leon War Boots
        equipments.add(new GachaponInfo.GachItem(1072555, true)); // Hex Von Leon War Boots
        equipments.add(new GachaponInfo.GachItem(1072556, true)); // Celine Von Leon War Boots
        equipments.add(new GachaponInfo.GachItem(1072557, true)); // Scar Von Leon War Boots
        equipments.add(new GachaponInfo.GachItem(1072558, true)); // Mer Von Leon War Boots
        equipments.add(new GachaponInfo.GachItem(1072641, true)); // Imperial Brave Boots
        equipments.add(new GachaponInfo.GachItem(1072642, true)); // Imperial Memories Boots
        equipments.add(new GachaponInfo.GachItem(1072643, true)); // Imperial Sharpness Boots
        equipments.add(new GachaponInfo.GachItem(1072644, true)); // Imperial Swift Boots
        equipments.add(new GachaponInfo.GachItem(1072645, true)); // Imperial Fervent Boots
        equipments.add(new GachaponInfo.GachItem(1072679, true)); // Lion King's Foot
        equipments.add(new GachaponInfo.GachItem(1072689, true)); //  Intermediate Knight's Greaves
        equipments.add(new GachaponInfo.GachItem(1072690, true)); //  Brave Intermediate Knight's Greaves
        equipments.add(new GachaponInfo.GachItem(1072691, true)); //  Wise Intermediate Knight's Greaves
        equipments.add(new GachaponInfo.GachItem(1072692, true)); //  Brave High Knight's Greaves
        equipments.add(new GachaponInfo.GachItem(1072693, true)); //  Wise High Knight's Greaves
        equipments.add(new GachaponInfo.GachItem(1072694, true)); //  Legendary High Knight's Greaves
        equipments.add(new GachaponInfo.GachItem(1072695, true)); // Kaiser Brave Boots
        equipments.add(new GachaponInfo.GachItem(1072696, true)); // Luminous Memory Boots
        equipments.add(new GachaponInfo.GachItem(1072697, true)); // Angelic Buster Fervent Boots
        equipments.add(new GachaponInfo.GachItem(1072703, true)); // Grand Agares Boots
        equipments.add(new GachaponInfo.GachItem(1072704, true)); // Grand Eligos Boots
        equipments.add(new GachaponInfo.GachItem(1072705, true)); // Grand Ipos Boots
        equipments.add(new GachaponInfo.GachItem(1072706, true)); // Grand Helphas Boots
        equipments.add(new GachaponInfo.GachItem(1072707, true)); // Grand Vepar Boots
        equipments.add(new GachaponInfo.GachItem(1072737, true)); // Nova Hyades Boots
        equipments.add(new GachaponInfo.GachItem(1072738, true)); // Nova Hermes Boots
        equipments.add(new GachaponInfo.GachItem(1072739, true)); // Nova Charon Boots
        equipments.add(new GachaponInfo.GachItem(1072740, true)); // Nova Lycaon Boots
        equipments.add(new GachaponInfo.GachItem(1072741, true)); // Nova Altair Boots
        equipments.add(new GachaponInfo.GachItem(1072753, true)); //  Feeble Intermediate Knight's Greaves
        equipments.add(new GachaponInfo.GachItem(1072754, true)); //  Feeble High Knight's Greaves
        equipments.add(new GachaponInfo.GachItem(1072776, true)); // Proton Boots
        equipments.add(new GachaponInfo.GachItem(1072786, true)); // Sovereign Brave Boots
        equipments.add(new GachaponInfo.GachItem(1072787, true)); // Sovereign Memory Boots
        equipments.add(new GachaponInfo.GachItem(1072788, true)); // Sovereign Sharpness Boots
        equipments.add(new GachaponInfo.GachItem(1072789, true)); // Sovereign Swift Boots
        equipments.add(new GachaponInfo.GachItem(1072790, true)); // Sovereign Fervent Boots
        equipments.add(new GachaponInfo.GachItem(1072837, true)); // Saint Wing Knight Shoes
        equipments.add(new GachaponInfo.GachItem(1072845, true)); // FrontierA Amethysian Shoes
        equipments.add(new GachaponInfo.GachItem(1072847, true)); // Sapphire Clover Sneakers
        equipments.add(new GachaponInfo.GachItem(1072853, true)); // Revolution Shoes
        equipments.add(new GachaponInfo.GachItem(1072885, true)); // Mean Evasion Shoes
        equipments.add(new GachaponInfo.GachItem(1072886, true)); // Adroit Evasion Shoes
        equipments.add(new GachaponInfo.GachItem(1072887, true)); // Absolute Evasion Shoes
        equipments.add(new GachaponInfo.GachItem(1072952, true)); // Necromancer Warrior Shoes
        equipments.add(new GachaponInfo.GachItem(1072953, true)); // Necromancer Magician Shoes
        equipments.add(new GachaponInfo.GachItem(1072954, true)); // Necromancer Sentinel Shoes
        equipments.add(new GachaponInfo.GachItem(1072955, true)); // Necromancer Chaser Shoes
        equipments.add(new GachaponInfo.GachItem(1072956, true)); // Necromancer Skipper Shoes
        equipments.add(new GachaponInfo.GachItem(1072957, true)); // Eclectic Grabbe
        equipments.add(new GachaponInfo.GachItem(1072958, true)); // Eclectic Cabatina
        equipments.add(new GachaponInfo.GachItem(1072959, true)); // Eclectic Rontano
        equipments.add(new GachaponInfo.GachItem(1072960, true)); // Eclectic Moonsteed
        equipments.add(new GachaponInfo.GachItem(1072961, true)); // Eclectic Faraon
        equipments.add(new GachaponInfo.GachItem(1072962, true)); // Muspell Warrior Shoes
        equipments.add(new GachaponInfo.GachItem(1072963, true)); // Muspell Magician Shoes
        equipments.add(new GachaponInfo.GachItem(1072964, true)); // Muspell Bowman Shoes
        equipments.add(new GachaponInfo.GachItem(1072965, true)); // Muspell Thief Shoes
        equipments.add(new GachaponInfo.GachItem(1072966, true)); // Muspell Pirate Shoes
        equipments.add(new GachaponInfo.GachItem(1072972, true)); // Royal Von Leon Warrior Boots
        equipments.add(new GachaponInfo.GachItem(1072973, true)); // Royal Von Leon Mage Boots
        equipments.add(new GachaponInfo.GachItem(1072974, true)); // Royal Von Leon Sentinel Boots
        equipments.add(new GachaponInfo.GachItem(1072975, true)); // Royal Von Leon Chaser Boots
        equipments.add(new GachaponInfo.GachItem(1072976, true)); // Royal Von Leon Skipper Boots
        equipments.add(new GachaponInfo.GachItem(1073057, true)); // Maple Treasure Shoes
        equipments.add(new GachaponInfo.GachItem(1073077, true)); // Blackgate Boots
        equipments.add(new GachaponInfo.GachItem(1202023, true)); // Mighty Joe Joe Totem
        equipments.add(new GachaponInfo.GachItem(1202027, true)); // Clever Hermoninny Totem
        equipments.add(new GachaponInfo.GachItem(1202031, true)); // Tenacious Little Dragon Totem
        equipments.add(new GachaponInfo.GachItem(1202035, true)); // Beefy Ika Totem
        equipments.add(new GachaponInfo.GachItem(1202063, true)); // Mighty Joe Joe Totem
        equipments.add(new GachaponInfo.GachItem(1202067, true)); // Clever Hermoninny Totem
        equipments.add(new GachaponInfo.GachItem(1202071, true)); // Tenacious Little Dragon Totem
        equipments.add(new GachaponInfo.GachItem(1202075, true)); // Beefy Ika Totem
        equipments.add(new GachaponInfo.GachItem(1202083, true)); // Pointy Captain Finger Totem
        equipments.add(new GachaponInfo.GachItem(1202173, true)); // Horseback Riding Doll Totem
        equipments.add(new GachaponInfo.GachItem(1202174, true)); // Jade Kettle Totem
        equipments.add(new GachaponInfo.GachItem(1202175, true)); // Bronze Incense Burner Totem
        equipments.add(new GachaponInfo.GachItem(1212009, true)); // Crescent Shining Rod
        equipments.add(new GachaponInfo.GachItem(1212010, true)); // Shining Dragon Rod
        equipments.add(new GachaponInfo.GachItem(1212011, true)); // Timeless Dead End
        equipments.add(new GachaponInfo.GachItem(1212012, true)); // Reverse Dead End
        equipments.add(new GachaponInfo.GachItem(1212013, true)); // Eligos Bloody Shining Rod
        equipments.add(new GachaponInfo.GachItem(1212015, true)); // Alma Von Leon Glorier
        equipments.add(new GachaponInfo.GachItem(1212016, true)); // Hex Von Leon Glorier
        equipments.add(new GachaponInfo.GachItem(1212017, true)); // Abyss Dead End
        equipments.add(new GachaponInfo.GachItem(1212018, true)); // Fearless Dead End
        equipments.add(new GachaponInfo.GachItem(1212019, true)); // Dragonic Crescent
        equipments.add(new GachaponInfo.GachItem(1212034, true)); // Imperial Shining Rod
        equipments.add(new GachaponInfo.GachItem(1212035, true)); // Necro Shining Rod
        equipments.add(new GachaponInfo.GachItem(1212043, true)); // Grand Eligos Bloody Shining Rod
        equipments.add(new GachaponInfo.GachItem(1212044, true)); // Loveless Dead End
        equipments.add(new GachaponInfo.GachItem(1212052, true)); // Luminous Shining Rod
        equipments.add(new GachaponInfo.GachItem(1212060, true)); // Sovereign Shining Rod
        equipments.add(new GachaponInfo.GachItem(1212074, true)); // FrontierA Amethysian Shining Rod
        equipments.add(new GachaponInfo.GachItem(1212078, true)); // Opulent White Shining Rod
        equipments.add(new GachaponInfo.GachItem(1212079, true)); // Revolution Shining Rod
        equipments.add(new GachaponInfo.GachItem(1212092, true)); // Amaranthine Shining Rod
        equipments.add(new GachaponInfo.GachItem(1212093, true)); // Azure Shining Rod
        equipments.add(new GachaponInfo.GachItem(1212099, true)); // Briser Shining Rod
        equipments.add(new GachaponInfo.GachItem(1212100, true)); // Jaihin Shining Rod
        equipments.add(new GachaponInfo.GachItem(1212102, true)); // Royal Von Leon Glorier
        equipments.add(new GachaponInfo.GachItem(1212117, true)); // Maple Treasure Shining Rod
        equipments.add(new GachaponInfo.GachItem(1212122, true)); // Special Dragon Tail Thanatos
        equipments.add(new GachaponInfo.GachItem(1222009, true)); // Jade Worm
        equipments.add(new GachaponInfo.GachItem(1222010, true)); // Iron Dragon
        equipments.add(new GachaponInfo.GachItem(1222011, true)); // Timeless Purple Dragon
        equipments.add(new GachaponInfo.GachItem(1222012, true)); // Reverse Purple Dragon
        equipments.add(new GachaponInfo.GachItem(1222013, true)); // Vepar Bloody Moon
        equipments.add(new GachaponInfo.GachItem(1222015, true)); // Cora Von Leon White Worm
        equipments.add(new GachaponInfo.GachItem(1222016, true)); // Mer Von Leon White Worm
        equipments.add(new GachaponInfo.GachItem(1222017, true)); // Abyss Purple Dragon
        equipments.add(new GachaponInfo.GachItem(1222018, true)); // Fearless Purple Dragon
        equipments.add(new GachaponInfo.GachItem(1222019, true)); // Dragonic Jade Worm
        equipments.add(new GachaponInfo.GachItem(1222034, true)); // Imperial Moon
        equipments.add(new GachaponInfo.GachItem(1222035, true)); // Necro Purple Dragon
        equipments.add(new GachaponInfo.GachItem(1222043, true)); // Grand Vepar Bloody Moon
        equipments.add(new GachaponInfo.GachItem(1222044, true)); // Loveless Purple Dragon
        equipments.add(new GachaponInfo.GachItem(1222048, true)); // Angelic Buster Bloody Boom
        equipments.add(new GachaponInfo.GachItem(1222055, true)); // Sovereign Blood Moon
        equipments.add(new GachaponInfo.GachItem(1222069, true)); // FrontierA Amethysian Soul Shooter
        equipments.add(new GachaponInfo.GachItem(1222073, true)); // Opulent White Soul Shooter
        equipments.add(new GachaponInfo.GachItem(1222074, true)); // Revolution Soul Shooter
        equipments.add(new GachaponInfo.GachItem(1222086, true)); // Amaranthine Soul Shooter
        equipments.add(new GachaponInfo.GachItem(1222087, true)); // Azure Soul Shooter
        equipments.add(new GachaponInfo.GachItem(1222093, true)); // Briser Dragon Soul
        equipments.add(new GachaponInfo.GachItem(1222094, true)); // Jaihin Dragon Soul
        equipments.add(new GachaponInfo.GachItem(1222096, true)); // Royal Von Leon White Worm
        equipments.add(new GachaponInfo.GachItem(1222111, true)); // Maple Treasure Soul Shooter
        equipments.add(new GachaponInfo.GachItem(1222115, true)); // Special Shark Tooth Soul Drinker
        equipments.add(new GachaponInfo.GachItem(1232009, true)); // Primm Vagabond
        equipments.add(new GachaponInfo.GachItem(1232010, true)); // Dragon Rage
        equipments.add(new GachaponInfo.GachItem(1232011, true)); // Timeless Grim Seeker
        equipments.add(new GachaponInfo.GachItem(1232012, true)); // Reverse Grim Seeker
        equipments.add(new GachaponInfo.GachItem(1232013, true)); // Agares Bloody Crimson Zodiac
        equipments.add(new GachaponInfo.GachItem(1232015, true)); // Marx Von Leon Blood Fury
        equipments.add(new GachaponInfo.GachItem(1232016, true)); // Victor Von Leon Blood Fury
        equipments.add(new GachaponInfo.GachItem(1232017, true)); // Abyss Grim Seeker
        equipments.add(new GachaponInfo.GachItem(1232018, true)); // Fearless Grim Seeker
        equipments.add(new GachaponInfo.GachItem(1232019, true)); // Dragonic Primm Vagabond
        equipments.add(new GachaponInfo.GachItem(1232034, true)); // Imperial Crimson Zodiac
        equipments.add(new GachaponInfo.GachItem(1232035, true)); // Necro Grim Seeker
        equipments.add(new GachaponInfo.GachItem(1232040, true)); // Grand Agares Crimson Zodiac
        equipments.add(new GachaponInfo.GachItem(1232041, true)); // Loveless Grim Seeker
        equipments.add(new GachaponInfo.GachItem(1232055, true)); // Sovereign Crimson Zodiac
        equipments.add(new GachaponInfo.GachItem(1232066, true)); // FrontierA Amethysian Devil Sword
        equipments.add(new GachaponInfo.GachItem(1232073, true)); // Opulent White Devil Sword
        equipments.add(new GachaponInfo.GachItem(1232074, true)); // Revolution Demon Sword
        equipments.add(new GachaponInfo.GachItem(1232086, true)); // Amaranthine Grim Seeker
        equipments.add(new GachaponInfo.GachItem(1232087, true)); // Azure Grim Seeker
        equipments.add(new GachaponInfo.GachItem(1232092, true)); // Zakun' Poisonic Desperado
        equipments.add(new GachaponInfo.GachItem(1232093, true)); // Briser Grim Seeker
        equipments.add(new GachaponInfo.GachItem(1232094, true)); // Jaihin Desperado
        equipments.add(new GachaponInfo.GachItem(1232096, true)); // Royal Von Leon Blood Fury
        equipments.add(new GachaponInfo.GachItem(1232111, true)); // Maple Treasure Devil Sword
        equipments.add(new GachaponInfo.GachItem(1232115, true)); // Special Lionheart Painful Destiny
        equipments.add(new GachaponInfo.GachItem(1242009, true)); // Fish Fin
        equipments.add(new GachaponInfo.GachItem(1242010, true)); // Dragon Energy Skull
        equipments.add(new GachaponInfo.GachItem(1242011, true)); // Timeless Hefty Head
        equipments.add(new GachaponInfo.GachItem(1242012, true)); // Reverse Hefty Head
        equipments.add(new GachaponInfo.GachItem(1242013, true)); // Vepar Bloody Manticore
        equipments.add(new GachaponInfo.GachItem(1242015, true)); // Cora Von Leon Energy Chain
        equipments.add(new GachaponInfo.GachItem(1242016, true)); // Mer Von Leon Energy Chain
        equipments.add(new GachaponInfo.GachItem(1242017, true)); // Abyss Hefty Head
        equipments.add(new GachaponInfo.GachItem(1242018, true)); // Fearless Hefty Head
        equipments.add(new GachaponInfo.GachItem(1242019, true)); // Dragonic Fish Fin
        equipments.add(new GachaponInfo.GachItem(1242034, true)); // Imperial Fervent Manticore
        equipments.add(new GachaponInfo.GachItem(1242035, true)); // Necro Hefty Head
        equipments.add(new GachaponInfo.GachItem(1242043, true)); // Grand Helphas Manticore
        equipments.add(new GachaponInfo.GachItem(1242044, true)); // Loveless Hefty Head
        equipments.add(new GachaponInfo.GachItem(1242045, true)); // Helphas Bloody Manticore
        equipments.add(new GachaponInfo.GachItem(1242046, true)); // Grand Vepar Manticore
        equipments.add(new GachaponInfo.GachItem(1242047, true)); // Nox Von Leon Energy Chain
        equipments.add(new GachaponInfo.GachItem(1242051, true)); // Scar Von Leon Energy Chain
        equipments.add(new GachaponInfo.GachItem(1242052, true)); // Imperial Swift Manticore
        equipments.add(new GachaponInfo.GachItem(1242057, true)); // Sovereign Manticore
        equipments.add(new GachaponInfo.GachItem(1242058, true)); // Sovereign Manticore
        equipments.add(new GachaponInfo.GachItem(1242071, true)); // FrontierA Amethysian Chain Sword
        equipments.add(new GachaponInfo.GachItem(1242079, true)); // Opulent White Chain Sword
        equipments.add(new GachaponInfo.GachItem(1242080, true)); // Revolution Chain Sword
        equipments.add(new GachaponInfo.GachItem(1242092, true)); // Amaranthine Energy Sword
        equipments.add(new GachaponInfo.GachItem(1242093, true)); // Azure Energy Sword
        equipments.add(new GachaponInfo.GachItem(1242100, true)); // Briser Hefty Head
        equipments.add(new GachaponInfo.GachItem(1242101, true)); // Jaihin Energy Chain
        equipments.add(new GachaponInfo.GachItem(1242103, true)); // Royal Von Leon Energy Chain
        equipments.add(new GachaponInfo.GachItem(1242118, true)); // Maple Treasure Chain Sword
        equipments.add(new GachaponInfo.GachItem(1242125, true)); // Special Shark Tooth Fallen Queen
        equipments.add(new GachaponInfo.GachItem(1252009, true)); // Lion Glass Scepter
        equipments.add(new GachaponInfo.GachItem(1252010, true)); // Dragon Kitty Soul Scepter
        equipments.add(new GachaponInfo.GachItem(1252011, true)); // Timeless Kitty Pride Scepter
        equipments.add(new GachaponInfo.GachItem(1252012, true)); // Reverse Kitty Pride Scepter
        equipments.add(new GachaponInfo.GachItem(1252013, true)); // Ergoth Bloody Kitten Soul Scepter
        equipments.add(new GachaponInfo.GachItem(1252016, true)); // Alma Von Leon's Scepter
        equipments.add(new GachaponInfo.GachItem(1252017, true)); // Hex Von Leon's Scepter
        equipments.add(new GachaponInfo.GachItem(1252018, true)); // Abyss Kitty Pride Scepter
        equipments.add(new GachaponInfo.GachItem(1252019, true)); // Imperial Kitten Soul Scepter
        equipments.add(new GachaponInfo.GachItem(1252020, true)); // Necro Kitten Soul Scepter
        equipments.add(new GachaponInfo.GachItem(1252021, true)); // Dragonic Sage Scepter
        equipments.add(new GachaponInfo.GachItem(1252022, true)); // Fearless Kitty Pride Scepter
        equipments.add(new GachaponInfo.GachItem(1252030, true)); // Grand Eligos Kitten Soul Scepter
        equipments.add(new GachaponInfo.GachItem(1252031, true)); // Loveless Kitty Pride
        equipments.add(new GachaponInfo.GachItem(1252046, true)); // Revolution Kitty Soul Scepter
        equipments.add(new GachaponInfo.GachItem(1252059, true)); // Sovereign Kitty Soul Scepter
        equipments.add(new GachaponInfo.GachItem(1252084, true)); // Briser Shining Stick
        equipments.add(new GachaponInfo.GachItem(1252085, true)); // Jaihin Shining Stick
        equipments.add(new GachaponInfo.GachItem(1252087, true)); // Royal Von Leon Stick
        equipments.add(new GachaponInfo.GachItem(1252097, true)); // Maple Treasure Scepter
        equipments.add(new GachaponInfo.GachItem(1262008, true)); // Dragon Psy-limiter
        equipments.add(new GachaponInfo.GachItem(1262009, true)); // Briser Psy-limiter
        equipments.add(new GachaponInfo.GachItem(1262010, true)); // Jaihin Psy-limiter
        equipments.add(new GachaponInfo.GachItem(1262013, true)); // Necromancer Psy-limiter
        equipments.add(new GachaponInfo.GachItem(1262014, true)); // Royal Von Leon Psy-limiter
        equipments.add(new GachaponInfo.GachItem(1262037, true)); // Maple Treasure Psy-limiter
        equipments.add(new GachaponInfo.GachItem(1262041, true)); // Special Dragon Tail Psy-limiter
        equipments.add(new GachaponInfo.GachItem(1302059, true)); // Dragon Carabella
        equipments.add(new GachaponInfo.GachItem(1302081, true)); // Timeless Executioner
        equipments.add(new GachaponInfo.GachItem(1302086, true)); // Reverse Executioner
        equipments.add(new GachaponInfo.GachItem(1302101, true)); // Lunch box (Lv 120)
        equipments.add(new GachaponInfo.GachItem(1302108, true)); // Light Beam Saver
        equipments.add(new GachaponInfo.GachItem(1302109, true)); // Flairgrave
        equipments.add(new GachaponInfo.GachItem(1302114, true)); // GMS Carabella
        equipments.add(new GachaponInfo.GachItem(1302146, true)); // Last Unwelcome Guest One-Handed Sword
        equipments.add(new GachaponInfo.GachItem(1302147, true)); // VIP One-Handed Sword
        equipments.add(new GachaponInfo.GachItem(1302149, true)); // Marx Von Leon Sabre
        equipments.add(new GachaponInfo.GachItem(1302153, true)); // Agares Bloody Terror
        equipments.add(new GachaponInfo.GachItem(1302173, true)); // Abyss Executioner
        equipments.add(new GachaponInfo.GachItem(1302174, true)); // Fearless Executioner
        equipments.add(new GachaponInfo.GachItem(1302175, true)); // Victor Von Leon Sabre
        equipments.add(new GachaponInfo.GachItem(1302179, true)); // Legendary Dragon Carabella
        equipments.add(new GachaponInfo.GachItem(1302193, true)); // Marx Von Leon Sabre
        equipments.add(new GachaponInfo.GachItem(1302199, true)); // <Renegades> Dragon Carabella
        equipments.add(new GachaponInfo.GachItem(1302200, true)); // <Renegades> Maple Lightbringer
        equipments.add(new GachaponInfo.GachItem(1302206, true)); // Golden Clover Executioners
        equipments.add(new GachaponInfo.GachItem(1302207, true)); // Imperial Terror
        equipments.add(new GachaponInfo.GachItem(1302213, true)); // Necro Terror
        equipments.add(new GachaponInfo.GachItem(1302226, true)); // Lapis Sword
        equipments.add(new GachaponInfo.GachItem(1302228, true)); // Grand Agares Terror
        equipments.add(new GachaponInfo.GachItem(1302233, true)); // Fire Katana
        equipments.add(new GachaponInfo.GachItem(1302238, true)); // Evergreen Sabre
        equipments.add(new GachaponInfo.GachItem(1302239, true)); // Slate Thunder Sabre
        equipments.add(new GachaponInfo.GachItem(1302240, true)); // Dark Magenta Sabre
        equipments.add(new GachaponInfo.GachItem(1302241, true)); // Twilight Sabre
        equipments.add(new GachaponInfo.GachItem(1302242, true)); // Bloody Ruby Sabre
        equipments.add(new GachaponInfo.GachItem(1302243, true)); // Ombra & Luce Sabre
        equipments.add(new GachaponInfo.GachItem(1302244, true)); // Soild Black Sabre
        equipments.add(new GachaponInfo.GachItem(1302245, true)); // Rainbow Sabre
        equipments.add(new GachaponInfo.GachItem(1302248, true)); // Loveless Executioner
        equipments.add(new GachaponInfo.GachItem(1302264, true)); // Sovereign Terror
        equipments.add(new GachaponInfo.GachItem(1302281, true)); // FrontierA Amethysian Gladius
        equipments.add(new GachaponInfo.GachItem(1302288, true)); // Opulent White Sword
        equipments.add(new GachaponInfo.GachItem(1302289, true)); // Revolution Sword
        equipments.add(new GachaponInfo.GachItem(1302292, true)); // Courage Wings
        equipments.add(new GachaponInfo.GachItem(1302298, true)); // Kirito's Sword
        equipments.add(new GachaponInfo.GachItem(1302301, true)); // Amaranthine Gladius
        equipments.add(new GachaponInfo.GachItem(1302302, true)); // Azure Gladius
        equipments.add(new GachaponInfo.GachItem(1302313, true)); // Briser Terror
        equipments.add(new GachaponInfo.GachItem(1302314, true)); // Jaihin Saber
        equipments.add(new GachaponInfo.GachItem(1302316, true)); // Royal Von Leon Saber
        equipments.add(new GachaponInfo.GachItem(1302336, true)); // Maple Treasure Sword
        equipments.add(new GachaponInfo.GachItem(1302345, true)); // Special Lionheart Cutlass
        equipments.add(new GachaponInfo.GachItem(1312031, true)); // Dragon Axe
        equipments.add(new GachaponInfo.GachItem(1312037, true)); // Timeless Bardiche
        equipments.add(new GachaponInfo.GachItem(1312038, true)); // Reverse Bardiche
        equipments.add(new GachaponInfo.GachItem(1312040, true)); // Light Beam Saver
        equipments.add(new GachaponInfo.GachItem(1312041, true)); // Flairgrave
        equipments.add(new GachaponInfo.GachItem(1312044, true)); // GMS Axe
        equipments.add(new GachaponInfo.GachItem(1312061, true)); // Last Unwelcome Guest One-Handed Axe
        equipments.add(new GachaponInfo.GachItem(1312062, true)); // VIP One-Handed Axe
        equipments.add(new GachaponInfo.GachItem(1312066, true)); // Agares Headsplitter
        equipments.add(new GachaponInfo.GachItem(1312072, true)); // Abyss Bardiche
        equipments.add(new GachaponInfo.GachItem(1312073, true)); // Fearless Bardiche
        equipments.add(new GachaponInfo.GachItem(1312077, true)); // Legendary Dragon Axe
        equipments.add(new GachaponInfo.GachItem(1312094, true)); // Lapis Axe
        equipments.add(new GachaponInfo.GachItem(1312095, true)); // Marx Von Leon Axe
        equipments.add(new GachaponInfo.GachItem(1312096, true)); // Victor Von Leon Axe
        equipments.add(new GachaponInfo.GachItem(1312097, true)); // Dragonic Lapis Axe
        equipments.add(new GachaponInfo.GachItem(1312099, true)); // Marx Von Leon Axe
        equipments.add(new GachaponInfo.GachItem(1312105, true)); // <Renegades> Dragon Axe
        equipments.add(new GachaponInfo.GachItem(1312106, true)); // <Renegades> Maple Skyrender
        equipments.add(new GachaponInfo.GachItem(1312109, true)); // Golden Clover Bardiche
        equipments.add(new GachaponInfo.GachItem(1312110, true)); // Imperial Headsplitter
        equipments.add(new GachaponInfo.GachItem(1312111, true)); // Imperial One hand Axe
        equipments.add(new GachaponInfo.GachItem(1312112, true)); // Necro Headsplitter
        equipments.add(new GachaponInfo.GachItem(1312117, true)); // Grand Agares Headsplitter
        equipments.add(new GachaponInfo.GachItem(1312125, true)); // Evergreen Sabre
        equipments.add(new GachaponInfo.GachItem(1312126, true)); // Slate Thunder Sabre
        equipments.add(new GachaponInfo.GachItem(1312127, true)); // Dark Magenta Sabre
        equipments.add(new GachaponInfo.GachItem(1312128, true)); // Twilight Sabre
        equipments.add(new GachaponInfo.GachItem(1312129, true)); // Bloody Ruby Sabre
        equipments.add(new GachaponInfo.GachItem(1312130, true)); // Ombra & Luce Sabre
        equipments.add(new GachaponInfo.GachItem(1312131, true)); // Soild Black Sabre
        equipments.add(new GachaponInfo.GachItem(1312132, true)); // Rainbow Sabre
        equipments.add(new GachaponInfo.GachItem(1312135, true)); // Loveless Bardiche
        equipments.add(new GachaponInfo.GachItem(1312150, true)); // Sovereign Headsplitter
        equipments.add(new GachaponInfo.GachItem(1312159, true)); // FrontierA Amethysian Counter
        equipments.add(new GachaponInfo.GachItem(1312163, true)); // Opulent White Axe
        equipments.add(new GachaponInfo.GachItem(1312165, true)); // Revolution Axe
        equipments.add(new GachaponInfo.GachItem(1312168, true)); // Courage Wings
        equipments.add(new GachaponInfo.GachItem(1312176, true)); // Amaranthine Counter
        equipments.add(new GachaponInfo.GachItem(1312177, true)); // Azure Counter
        equipments.add(new GachaponInfo.GachItem(1312183, true)); // Briser Headsplitter
        equipments.add(new GachaponInfo.GachItem(1312184, true)); // Jaihin Axe
        equipments.add(new GachaponInfo.GachItem(1312186, true)); // Royal Von Leon Axe
        equipments.add(new GachaponInfo.GachItem(1312201, true)); // Maple Treasure Axe
        equipments.add(new GachaponInfo.GachItem(1312205, true)); // Special Lionheart Champion Axe
        equipments.add(new GachaponInfo.GachItem(1322052, true)); // Dragon Mace
        equipments.add(new GachaponInfo.GachItem(1322060, true)); // Timeless Allargando
        equipments.add(new GachaponInfo.GachItem(1322061, true)); // Reverse Allargando
        equipments.add(new GachaponInfo.GachItem(1322062, true)); // Crushed Skull
        equipments.add(new GachaponInfo.GachItem(1322066, true)); // Light Beam Saver
        equipments.add(new GachaponInfo.GachItem(1322067, true)); // Flairgrave
        equipments.add(new GachaponInfo.GachItem(1322072, true)); // GMS Flame
        equipments.add(new GachaponInfo.GachItem(1322089, true)); // Last Unwelcome Guest One-Handed Blunt Weapon
        equipments.add(new GachaponInfo.GachItem(1322090, true)); // VIP One-Handed Blunt Weapon
        equipments.add(new GachaponInfo.GachItem(1322097, true)); // Agares Bloody Hammer
        equipments.add(new GachaponInfo.GachItem(1322107, true)); // Abyss Allargando
        equipments.add(new GachaponInfo.GachItem(1322108, true)); // Fearless Allargando
        equipments.add(new GachaponInfo.GachItem(1322112, true)); // Legendary Dragon Mace
        equipments.add(new GachaponInfo.GachItem(1322134, true)); // Lapis Hammer
        equipments.add(new GachaponInfo.GachItem(1322135, true)); // Marx Von Leon Hammer
        equipments.add(new GachaponInfo.GachItem(1322136, true)); // Victor Von Leon Hammer
        equipments.add(new GachaponInfo.GachItem(1322137, true)); // Dragonic Lapis Hammer
        equipments.add(new GachaponInfo.GachItem(1322139, true)); // Marx Von Leon Hammer
        equipments.add(new GachaponInfo.GachItem(1322145, true)); // <Renegades> Dragon Mace
        equipments.add(new GachaponInfo.GachItem(1322146, true)); // <Renegades> Maple Starcrusher
        equipments.add(new GachaponInfo.GachItem(1322149, true)); // Golden Clover Allargando
        equipments.add(new GachaponInfo.GachItem(1322150, true)); // Imperial Hammer
        equipments.add(new GachaponInfo.GachItem(1322151, true)); // Necro Hammer
        equipments.add(new GachaponInfo.GachItem(1322163, true)); // Grand Agares Hammer
        equipments.add(new GachaponInfo.GachItem(1322171, true)); // Evergreen Sabre
        equipments.add(new GachaponInfo.GachItem(1322172, true)); // Slate Thunder Sabre
        equipments.add(new GachaponInfo.GachItem(1322173, true)); // Dark Magenta Sabre
        equipments.add(new GachaponInfo.GachItem(1322174, true)); // Twilight Sabre
        equipments.add(new GachaponInfo.GachItem(1322175, true)); // Bloody Ruby Sabre
        equipments.add(new GachaponInfo.GachItem(1322176, true)); // Ombra & Luce Sabre
        equipments.add(new GachaponInfo.GachItem(1322177, true)); // Soild Black Sabre
        equipments.add(new GachaponInfo.GachItem(1322178, true)); // Rainbow Sabre
        equipments.add(new GachaponInfo.GachItem(1322181, true)); // Loveless Allargando
        equipments.add(new GachaponInfo.GachItem(1322199, true)); // Sovereign Hammer
        equipments.add(new GachaponInfo.GachItem(1322210, true)); // FrontierA Amethysian Golden Hammer
        equipments.add(new GachaponInfo.GachItem(1322214, true)); // Opulent White Mace
        equipments.add(new GachaponInfo.GachItem(1322215, true)); // Revolution Mace
        equipments.add(new GachaponInfo.GachItem(1322218, true)); // Courage Wings
        equipments.add(new GachaponInfo.GachItem(1322228, true)); // Amaranthine Mace
        equipments.add(new GachaponInfo.GachItem(1322229, true)); // Azure Mace
        equipments.add(new GachaponInfo.GachItem(1322234, true)); // Briser Hammer
        equipments.add(new GachaponInfo.GachItem(1322235, true)); // Jaihin Hair
        equipments.add(new GachaponInfo.GachItem(1322237, true)); // Royal Von Leon Hammer
        equipments.add(new GachaponInfo.GachItem(1322253, true)); // Maple Treasure Mace
        equipments.add(new GachaponInfo.GachItem(1322257, true)); // Special Lionheart Battle Hammer
        equipments.add(new GachaponInfo.GachItem(1332049, true)); // Dragon Kanzir
        equipments.add(new GachaponInfo.GachItem(1332050, true)); // Dragon Kreda
        equipments.add(new GachaponInfo.GachItem(1332073, true)); // Timeless Pescas
        equipments.add(new GachaponInfo.GachItem(1332074, true)); // Timeless Killic
        equipments.add(new GachaponInfo.GachItem(1332075, true)); // Reverse Pescas
        equipments.add(new GachaponInfo.GachItem(1332076, true)); // Reverse Killic
        equipments.add(new GachaponInfo.GachItem(1332082, true)); // Light Beam Saver
        equipments.add(new GachaponInfo.GachItem(1332083, true)); // Flairgrave
        equipments.add(new GachaponInfo.GachItem(1332100, true)); // Nageling
        equipments.add(new GachaponInfo.GachItem(1332119, true)); // Last Unwelcome Guest Dagger (LUK)
        equipments.add(new GachaponInfo.GachItem(1332120, true)); // VIP Dagger (LUK)
        equipments.add(new GachaponInfo.GachItem(1332124, true)); // Last Unwelcome Guest Dagger (STR)
        equipments.add(new GachaponInfo.GachItem(1332125, true)); // VIP Dagger (STR)
        equipments.add(new GachaponInfo.GachItem(1332126, true)); // Nox Von Leon Dagger
        equipments.add(new GachaponInfo.GachItem(1332131, true)); // Halphas Bloody Slayer
        equipments.add(new GachaponInfo.GachItem(1332148, true)); // Abyss Pescas
        equipments.add(new GachaponInfo.GachItem(1332149, true)); // Abyss Killic
        equipments.add(new GachaponInfo.GachItem(1332150, true)); // Fearless Pescas
        equipments.add(new GachaponInfo.GachItem(1332151, true)); // Fearless Killic
        equipments.add(new GachaponInfo.GachItem(1332152, true)); // Scar Von Leon Dagger
        equipments.add(new GachaponInfo.GachItem(1332153, true)); // Dragonic Nageling
        equipments.add(new GachaponInfo.GachItem(1332157, true)); // Legendary Dragon Kreda
        equipments.add(new GachaponInfo.GachItem(1332170, true)); // Nox Von Leon Dagger
        equipments.add(new GachaponInfo.GachItem(1332176, true)); // <Renegades> Dragon Kreda
        equipments.add(new GachaponInfo.GachItem(1332177, true)); // <Renegades> Maple Lightsplitter
        equipments.add(new GachaponInfo.GachItem(1332182, true)); // Golden Clover Pescas
        equipments.add(new GachaponInfo.GachItem(1332183, true)); // Golden Clover Killic
        equipments.add(new GachaponInfo.GachItem(1332184, true)); // Imperial Slayer
        equipments.add(new GachaponInfo.GachItem(1332187, true)); // Necro Slayer
        equipments.add(new GachaponInfo.GachItem(1332188, true)); // Necro Pescas
        equipments.add(new GachaponInfo.GachItem(1332194, true)); // Grand Helphas Slayer
        equipments.add(new GachaponInfo.GachItem(1332201, true)); // Liu Bei Sword
        equipments.add(new GachaponInfo.GachItem(1332202, true)); // Bone Weapon
        equipments.add(new GachaponInfo.GachItem(1332205, true)); // Loveless Pescas
        equipments.add(new GachaponInfo.GachItem(1332206, true)); // Dual Plasma Blade
        equipments.add(new GachaponInfo.GachItem(1332222, true)); // Sovereign Slayer
        equipments.add(new GachaponInfo.GachItem(1332232, true)); // FrontierA Amethysian Kanzir
        equipments.add(new GachaponInfo.GachItem(1332236, true)); // Opulent White Cutter
        equipments.add(new GachaponInfo.GachItem(1332238, true)); // Revolution Cutter
        equipments.add(new GachaponInfo.GachItem(1332241, true)); // Courage Wings
        equipments.add(new GachaponInfo.GachItem(1332248, true)); // Leafa's Sword
        equipments.add(new GachaponInfo.GachItem(1332251, true)); // Amaranthine Dagger
        equipments.add(new GachaponInfo.GachItem(1332252, true)); // Azure Dagger
        equipments.add(new GachaponInfo.GachItem(1332258, true)); // Briser Slayer
        equipments.add(new GachaponInfo.GachItem(1332259, true)); // Jaihin Dagger
        equipments.add(new GachaponInfo.GachItem(1332261, true)); // Royal Von Leon Dagger
        equipments.add(new GachaponInfo.GachItem(1332277, true)); // Maple Treasure Cutter
        equipments.add(new GachaponInfo.GachItem(1332281, true)); // Special Raven Horn Baselard
        equipments.add(new GachaponInfo.GachItem(1342009, true)); // Dragon Katara
        equipments.add(new GachaponInfo.GachItem(1342010, true)); // Moonshadow Katara
        equipments.add(new GachaponInfo.GachItem(1342011, true)); // Timeless Katara
        equipments.add(new GachaponInfo.GachItem(1342012, true)); // Reverse Katara
        equipments.add(new GachaponInfo.GachItem(1342032, true)); // Last Unwelcome Guest Katara
        equipments.add(new GachaponInfo.GachItem(1342033, true)); // VIP Katara
        equipments.add(new GachaponInfo.GachItem(1342035, true)); // Blood Blossom Katara
        equipments.add(new GachaponInfo.GachItem(1342040, true)); // Abyss Katara
        equipments.add(new GachaponInfo.GachItem(1342041, true)); // Fearless Katara
        equipments.add(new GachaponInfo.GachItem(1342042, true)); // Dragonic Dragon Katara
        equipments.add(new GachaponInfo.GachItem(1342046, true)); // Legendary Moonshadow Katara
        equipments.add(new GachaponInfo.GachItem(1342058, true)); // <Renegades> Moonshadow Katara
        equipments.add(new GachaponInfo.GachItem(1342059, true)); // <Renegades> Maple Katara
        equipments.add(new GachaponInfo.GachItem(1342064, true)); // Imperial Katara
        equipments.add(new GachaponInfo.GachItem(1342066, true)); // Necro Katara
        equipments.add(new GachaponInfo.GachItem(1342075, true)); // Loveless Katara
        equipments.add(new GachaponInfo.GachItem(1342080, true)); // Sovereign Katara
        equipments.add(new GachaponInfo.GachItem(1342098, true)); // Briser Katara
        equipments.add(new GachaponInfo.GachItem(1342099, true)); // Jaihin Katara
        equipments.add(new GachaponInfo.GachItem(1342103, true)); // Maple Treasure Katara
        equipments.add(new GachaponInfo.GachItem(1362014, true)); // Ange de la Mort
        equipments.add(new GachaponInfo.GachItem(1362015, true)); // Dragon Permanche
        equipments.add(new GachaponInfo.GachItem(1362016, true)); // Timeless Persona
        equipments.add(new GachaponInfo.GachItem(1362017, true)); // Reverse Persona
        equipments.add(new GachaponInfo.GachItem(1362018, true)); // Halphas Bloody Jester
        equipments.add(new GachaponInfo.GachItem(1362020, true)); // Nox Von Leon Cane
        equipments.add(new GachaponInfo.GachItem(1362021, true)); // Scar Von Leon Cane
        equipments.add(new GachaponInfo.GachItem(1362022, true)); // Abyss Persona
        equipments.add(new GachaponInfo.GachItem(1362023, true)); // Fearless Persona
        equipments.add(new GachaponInfo.GachItem(1362024, true)); // Dragonic Ange de la Mort
        equipments.add(new GachaponInfo.GachItem(1362044, true)); // <Renegades> Dragon Peurmancie
        equipments.add(new GachaponInfo.GachItem(1362045, true)); // <Renegades> Maple Cailey
        equipments.add(new GachaponInfo.GachItem(1362056, true)); // Imperial Jester
        equipments.add(new GachaponInfo.GachItem(1362061, true)); // Necro Jester
        equipments.add(new GachaponInfo.GachItem(1362068, true)); // Grand Helphas Jester
        equipments.add(new GachaponInfo.GachItem(1362074, true)); // Loveless Persona
        equipments.add(new GachaponInfo.GachItem(1362087, true)); // Sovereign Jester
        equipments.add(new GachaponInfo.GachItem(1362096, true)); // FrontierA Amethysian Arc 'en Ciel
        equipments.add(new GachaponInfo.GachItem(1362100, true)); // Opulent White Cane
        equipments.add(new GachaponInfo.GachItem(1362101, true)); // Revolution Cane
        equipments.add(new GachaponInfo.GachItem(1362112, true)); // Amaranthine Canne Mystique
        equipments.add(new GachaponInfo.GachItem(1362113, true)); // Azure Canne Mystique
        equipments.add(new GachaponInfo.GachItem(1362119, true)); // Briser Jester
        equipments.add(new GachaponInfo.GachItem(1362120, true)); // Jaihin Cane
        equipments.add(new GachaponInfo.GachItem(1362122, true)); // Royal Von Leon Cane
        equipments.add(new GachaponInfo.GachItem(1362137, true)); // Maple Treasure Cane
        equipments.add(new GachaponInfo.GachItem(1362142, true)); // Special Raven Horn Crimson Cane
        equipments.add(new GachaponInfo.GachItem(1372032, true)); // Dragon Wand
        equipments.add(new GachaponInfo.GachItem(1372039, true)); // Elemental Wand 5
        equipments.add(new GachaponInfo.GachItem(1372040, true)); // Elemental Wand 6
        equipments.add(new GachaponInfo.GachItem(1372041, true)); // Elemental Wand 7
        equipments.add(new GachaponInfo.GachItem(1372042, true)); // Elemental Wand 8
        equipments.add(new GachaponInfo.GachItem(1372044, true)); // Timeless Enlil Tear
        equipments.add(new GachaponInfo.GachItem(1372045, true)); // Reverse Enlil Tear
        equipments.add(new GachaponInfo.GachItem(1372047, true)); // Light Beam Saver
        equipments.add(new GachaponInfo.GachItem(1372048, true)); // Flairgrave
        equipments.add(new GachaponInfo.GachItem(1372052, true)); // Iceberg Wand
        equipments.add(new GachaponInfo.GachItem(1372077, true)); // Last Unwelcome Guest Wand
        equipments.add(new GachaponInfo.GachItem(1372078, true)); // VIP Wand
        equipments.add(new GachaponInfo.GachItem(1372080, true)); // Alma Von Leon Wand
        equipments.add(new GachaponInfo.GachItem(1372085, true)); // Eligos Bloody Wand
        equipments.add(new GachaponInfo.GachItem(1372100, true)); // Abyss Enlil Tear
        equipments.add(new GachaponInfo.GachItem(1372101, true)); // Fearless Enlil Tear
        equipments.add(new GachaponInfo.GachItem(1372102, true)); // Hex Von Leon Wand
        equipments.add(new GachaponInfo.GachItem(1372106, true)); // Legendary Dragon Wand
        equipments.add(new GachaponInfo.GachItem(1372119, true)); // Alma Von Leon Wand
        equipments.add(new GachaponInfo.GachItem(1372125, true)); // <Renegades> Dragon Wand
        equipments.add(new GachaponInfo.GachItem(1372126, true)); // <Renegades> Maple Manareaper
        equipments.add(new GachaponInfo.GachItem(1372129, true)); // Golden Clover Enreal Tear
        equipments.add(new GachaponInfo.GachItem(1372130, true)); // Imperial Wand
        equipments.add(new GachaponInfo.GachItem(1372132, true)); // Necro Wand
        equipments.add(new GachaponInfo.GachItem(1372140, true)); // Grand Eligos Wand
        equipments.add(new GachaponInfo.GachItem(1372148, true)); // Serpent Staff
        equipments.add(new GachaponInfo.GachItem(1372149, true)); // Skull Staff
        equipments.add(new GachaponInfo.GachItem(1372150, true)); // Evan Wand
        equipments.add(new GachaponInfo.GachItem(1372151, true)); // Pink Angel Stick
        equipments.add(new GachaponInfo.GachItem(1372152, true)); // Burning Breeze Fan
        equipments.add(new GachaponInfo.GachItem(1372153, true)); // Sunset Seraphim
        equipments.add(new GachaponInfo.GachItem(1372154, true)); // Heartbreak Sword
        equipments.add(new GachaponInfo.GachItem(1372155, true)); // MapleGirl Wand
        equipments.add(new GachaponInfo.GachItem(1372156, true)); // Patriot Seraphim
        equipments.add(new GachaponInfo.GachItem(1372157, true)); // Lord Tempest
        equipments.add(new GachaponInfo.GachItem(1372158, true)); // Galactic Legend
        equipments.add(new GachaponInfo.GachItem(1372161, true)); // Loveless Enlil Tear
        equipments.add(new GachaponInfo.GachItem(1372174, true)); // Sovereign Wand
        equipments.add(new GachaponInfo.GachItem(1372183, true)); // FrontierA Amethysian Double Wing
        equipments.add(new GachaponInfo.GachItem(1372187, true)); // Opulent White Wand
        equipments.add(new GachaponInfo.GachItem(1372188, true)); // Revolution Wand
        equipments.add(new GachaponInfo.GachItem(1372191, true)); // Wings of Wisdom
        equipments.add(new GachaponInfo.GachItem(1372198, true)); // Amaranthine Magic Wand
        equipments.add(new GachaponInfo.GachItem(1372199, true)); // Azure Magic Wand
        equipments.add(new GachaponInfo.GachItem(1372205, true)); // Briser Wand
        equipments.add(new GachaponInfo.GachItem(1372206, true)); // Jaihin Wand
        equipments.add(new GachaponInfo.GachItem(1372208, true)); // Royal Von Leon Wand
        equipments.add(new GachaponInfo.GachItem(1372225, true)); // Maple Treasure Wand
        equipments.add(new GachaponInfo.GachItem(1372230, true)); // Special Dragon Tail Arc Wand
        equipments.add(new GachaponInfo.GachItem(1382036, true)); // Dragon Staff
        equipments.add(new GachaponInfo.GachItem(1382037, true)); // Doomsday Staff
        equipments.add(new GachaponInfo.GachItem(1382045, true)); // Elemental Staff 1
        equipments.add(new GachaponInfo.GachItem(1382046, true)); // Elemental Staff 2
        equipments.add(new GachaponInfo.GachItem(1382047, true)); // Elemental Staff 3
        equipments.add(new GachaponInfo.GachItem(1382048, true)); // Elemental Staff 4
        equipments.add(new GachaponInfo.GachItem(1382057, true)); // Timeless Aeas Hand
        equipments.add(new GachaponInfo.GachItem(1382058, true)); // Laevateinn
        equipments.add(new GachaponInfo.GachItem(1382059, true)); // Reverse Aeas Hand
        equipments.add(new GachaponInfo.GachItem(1382063, true)); // Light Beam Saver
        equipments.add(new GachaponInfo.GachItem(1382064, true)); // Flairgrave
        equipments.add(new GachaponInfo.GachItem(1382082, true)); // Night Raven Staff
        equipments.add(new GachaponInfo.GachItem(1382098, true)); // Last Unwelcome Guest Staff
        equipments.add(new GachaponInfo.GachItem(1382099, true)); // VIP Staff
        equipments.add(new GachaponInfo.GachItem(1382102, true)); // Alma Von Leon Staff
        equipments.add(new GachaponInfo.GachItem(1382105, true)); // Eligos Bloody Rod
        equipments.add(new GachaponInfo.GachItem(1382124, true)); // Abyss Aeas Hand
        equipments.add(new GachaponInfo.GachItem(1382125, true)); // Fearless Aeas Hand
        equipments.add(new GachaponInfo.GachItem(1382126, true)); // Hex Von Leon Staff
        equipments.add(new GachaponInfo.GachItem(1382127, true)); // Dragonic Laevateinn
        equipments.add(new GachaponInfo.GachItem(1382131, true)); // Legendary Dragon Staff
        equipments.add(new GachaponInfo.GachItem(1382145, true)); // Alma Von Leon Staff
        equipments.add(new GachaponInfo.GachItem(1382151, true)); // <Renegades> Dragon Staff
        equipments.add(new GachaponInfo.GachItem(1382152, true)); // <Renegades> Maple Manabreaker
        equipments.add(new GachaponInfo.GachItem(1382157, true)); // Golden Clover Aeas Hand
        equipments.add(new GachaponInfo.GachItem(1382158, true)); // Imperial Rod
        equipments.add(new GachaponInfo.GachItem(1382159, true)); // Necro Rod
        equipments.add(new GachaponInfo.GachItem(1382169, true)); // Grand Eligos Rod
        equipments.add(new GachaponInfo.GachItem(1382173, true)); // Royal Oaken Staff
        equipments.add(new GachaponInfo.GachItem(1382178, true)); // Serpent Staff
        equipments.add(new GachaponInfo.GachItem(1382179, true)); // Skull Staff
        equipments.add(new GachaponInfo.GachItem(1382180, true)); // Evan Wand
        equipments.add(new GachaponInfo.GachItem(1382181, true)); // Pink Angel Stick
        equipments.add(new GachaponInfo.GachItem(1382182, true)); // Burning Breeze Fan
        equipments.add(new GachaponInfo.GachItem(1382183, true)); // Sunset Seraphim
        equipments.add(new GachaponInfo.GachItem(1382184, true)); // Heartbreak Sword
        equipments.add(new GachaponInfo.GachItem(1382185, true)); // MapleGirl Wand
        equipments.add(new GachaponInfo.GachItem(1382186, true)); // Patriot Seraphim
        equipments.add(new GachaponInfo.GachItem(1382187, true)); // Lord Tempest
        equipments.add(new GachaponInfo.GachItem(1382188, true)); // Galactic Legend
        equipments.add(new GachaponInfo.GachItem(1382192, true)); // Loveless Aeas Hand
        equipments.add(new GachaponInfo.GachItem(1382206, true)); // Sovereign Rod
        equipments.add(new GachaponInfo.GachItem(1382217, true)); // FrontierA Amethysian Pain Killer
        equipments.add(new GachaponInfo.GachItem(1382221, true)); // Opulent White Staff
        equipments.add(new GachaponInfo.GachItem(1382222, true)); // Revolution Koras
        equipments.add(new GachaponInfo.GachItem(1382225, true)); // Wings of Wisdom
        equipments.add(new GachaponInfo.GachItem(1382236, true)); // Amaranthine Magic Staff
        equipments.add(new GachaponInfo.GachItem(1382237, true)); // Azure Magic Staff
        equipments.add(new GachaponInfo.GachItem(1382243, true)); // Briser Rod
        equipments.add(new GachaponInfo.GachItem(1382244, true)); // Jaihin Staff
        equipments.add(new GachaponInfo.GachItem(1382246, true)); // Royal Von Leon Staff
        equipments.add(new GachaponInfo.GachItem(1382263, true)); // Maple Treasure Staff
        equipments.add(new GachaponInfo.GachItem(1382267, true)); // Special Dragon Tail War Staff
        equipments.add(new GachaponInfo.GachItem(1402036, true)); // Dragon Claymore
        equipments.add(new GachaponInfo.GachItem(1402046, true)); // Timeless Nibleheim
        equipments.add(new GachaponInfo.GachItem(1402047, true)); // Reverse Nibleheim
        equipments.add(new GachaponInfo.GachItem(1402054, true)); // Light Beam Saver
        equipments.add(new GachaponInfo.GachItem(1402055, true)); // Flairgrave
        equipments.add(new GachaponInfo.GachItem(1402061, true)); // GMS Claymore
        equipments.add(new GachaponInfo.GachItem(1402073, true)); // Askaron
        equipments.add(new GachaponInfo.GachItem(1402089, true)); // Last Unwelcome Guest Two-Handed Sword
        equipments.add(new GachaponInfo.GachItem(1402090, true)); // VIP Two-Handed Sword
        equipments.add(new GachaponInfo.GachItem(1402091, true)); // Marx Von Leon Sword
        equipments.add(new GachaponInfo.GachItem(1402096, true)); // Agares Bloody Zweihander
        equipments.add(new GachaponInfo.GachItem(1402111, true)); // Abyss Nibleheim
        equipments.add(new GachaponInfo.GachItem(1402112, true)); // Fearless Nibleheim
        equipments.add(new GachaponInfo.GachItem(1402113, true)); // Victor Von Leon Sword
        equipments.add(new GachaponInfo.GachItem(1402114, true)); // Dragonic Askaron
        equipments.add(new GachaponInfo.GachItem(1402118, true)); // Legendary Dragon Claymore
        equipments.add(new GachaponInfo.GachItem(1402131, true)); // Marx Von Leon Sword
        equipments.add(new GachaponInfo.GachItem(1402137, true)); // <Renegades> Dragon Claymore
        equipments.add(new GachaponInfo.GachItem(1402138, true)); // <Renegades> Maple Lightcaller
        equipments.add(new GachaponInfo.GachItem(1402141, true)); // Golden Clover Nibleheim
        equipments.add(new GachaponInfo.GachItem(1402142, true)); // Imperial Zweihander
        equipments.add(new GachaponInfo.GachItem(1402143, true)); // Necro Zweihander
        equipments.add(new GachaponInfo.GachItem(1402152, true)); // Grand Agares Zweihander
        equipments.add(new GachaponInfo.GachItem(1402158, true)); // Blue Guitar
        equipments.add(new GachaponInfo.GachItem(1402163, true)); // Laser Sword
        equipments.add(new GachaponInfo.GachItem(1402164, true)); // Moon Baton
        equipments.add(new GachaponInfo.GachItem(1402165, true)); // Flame Tongue
        equipments.add(new GachaponInfo.GachItem(1402166, true)); // Crissagrim Blade
        equipments.add(new GachaponInfo.GachItem(1402167, true)); // Mercury Sword
        equipments.add(new GachaponInfo.GachItem(1402168, true)); // Tania Sword
        equipments.add(new GachaponInfo.GachItem(1402169, true)); // Fire Katana
        equipments.add(new GachaponInfo.GachItem(1402172, true)); // Loveless Nibleheim
        equipments.add(new GachaponInfo.GachItem(1402184, true)); // Kaiser Two Hander
        equipments.add(new GachaponInfo.GachItem(1402191, true)); // Sovereign Zweihander
        equipments.add(new GachaponInfo.GachItem(1402205, true)); // Kaiserion
        equipments.add(new GachaponInfo.GachItem(1402206, true)); // FrontierA Amethysian Claymore
        equipments.add(new GachaponInfo.GachItem(1402209, true)); // Opulent White Two-handed Sword
        equipments.add(new GachaponInfo.GachItem(1402210, true)); // Revolution Two-handed Sword
        equipments.add(new GachaponInfo.GachItem(1402213, true)); // Valiant Wings
        equipments.add(new GachaponInfo.GachItem(1402219, true)); // Last Unwelcome Guest Two-Handed Sword
        equipments.add(new GachaponInfo.GachItem(1402221, true)); // Asuna's Sword
        equipments.add(new GachaponInfo.GachItem(1402225, true)); // Amaranthine Claymore
        equipments.add(new GachaponInfo.GachItem(1402226, true)); // Azure Claymore
        equipments.add(new GachaponInfo.GachItem(1402234, true)); // Briser Zweihander
        equipments.add(new GachaponInfo.GachItem(1402235, true)); // Jaihin Two-handed Sword
        equipments.add(new GachaponInfo.GachItem(1402237, true)); // Royal Von Leon Sword
        equipments.add(new GachaponInfo.GachItem(1402253, true)); // Maple Treasure Two-Handed Sword
        equipments.add(new GachaponInfo.GachItem(1402261, true)); // Special Lionheart Battle Scimitar
        equipments.add(new GachaponInfo.GachItem(1412026, true)); // Dragon Battle Axe
        equipments.add(new GachaponInfo.GachItem(1412033, true)); // Timeless Tabarzin
        equipments.add(new GachaponInfo.GachItem(1412034, true)); // Reverse Tabarzin
        equipments.add(new GachaponInfo.GachItem(1412036, true)); // Light Beam Saver
        equipments.add(new GachaponInfo.GachItem(1412037, true)); // Flairgrave
        equipments.add(new GachaponInfo.GachItem(1412041, true)); // GMS Battle Axe
        equipments.add(new GachaponInfo.GachItem(1412061, true)); // Last Unwelcome Guest Two-Handed Axe
        equipments.add(new GachaponInfo.GachItem(1412062, true)); // VIP Two-Handed Axe
        equipments.add(new GachaponInfo.GachItem(1412066, true)); // Agares Bloody Giant Axe
        equipments.add(new GachaponInfo.GachItem(1412071, true)); // Abyss Tabarzin
        equipments.add(new GachaponInfo.GachItem(1412072, true)); // Fearless Tabarzin
        equipments.add(new GachaponInfo.GachItem(1412076, true)); // Legendary Dragon Battle Axe
        equipments.add(new GachaponInfo.GachItem(1412093, true)); // <Renegades> Dragon Battle Axe
        equipments.add(new GachaponInfo.GachItem(1412094, true)); // <Renegades> Maple Skyslicer
        equipments.add(new GachaponInfo.GachItem(1412098, true)); // Golden Clover Tabarzin
        equipments.add(new GachaponInfo.GachItem(1412099, true)); // Imperial Giant Axe
        equipments.add(new GachaponInfo.GachItem(1412100, true)); // Necro Giant Axe
        equipments.add(new GachaponInfo.GachItem(1412105, true)); // Grand Agares Giant Axe
        equipments.add(new GachaponInfo.GachItem(1412113, true)); // Laser Sword
        equipments.add(new GachaponInfo.GachItem(1412114, true)); // Moon Baton
        equipments.add(new GachaponInfo.GachItem(1412115, true)); // Flame Tongue
        equipments.add(new GachaponInfo.GachItem(1412116, true)); // Crissagrim Blade
        equipments.add(new GachaponInfo.GachItem(1412117, true)); // Mercury Sword
        equipments.add(new GachaponInfo.GachItem(1412118, true)); // Tania Sword
        equipments.add(new GachaponInfo.GachItem(1412119, true)); // Skull Axe
        equipments.add(new GachaponInfo.GachItem(1412122, true)); // Loveless Tabarzin
        equipments.add(new GachaponInfo.GachItem(1412132, true)); // Sovereign Giant Axe
        equipments.add(new GachaponInfo.GachItem(1412141, true)); // FrontierA Amethysian Butterfly
        equipments.add(new GachaponInfo.GachItem(1412145, true)); // Opulent White Two-handed Axe
        equipments.add(new GachaponInfo.GachItem(1412147, true)); // Revolution Two-handed Axe
        equipments.add(new GachaponInfo.GachItem(1412150, true)); // Valiant Wings
        equipments.add(new GachaponInfo.GachItem(1412155, true)); // Amaranthine Great Axe
        equipments.add(new GachaponInfo.GachItem(1412156, true)); // Azure Great Axe
        equipments.add(new GachaponInfo.GachItem(1412162, true)); // Briser Giant Axe
        equipments.add(new GachaponInfo.GachItem(1412163, true)); // Jaihin Two-handed Axe
        equipments.add(new GachaponInfo.GachItem(1412179, true)); // Royal Von Leon Two-Handed Axe
        equipments.add(new GachaponInfo.GachItem(1412180, true)); // Maple Treasure Two-Handed Axe
        equipments.add(new GachaponInfo.GachItem(1422028, true)); // Dragon Flame
        equipments.add(new GachaponInfo.GachItem(1422030, true)); // Pink Seal Cushion
        equipments.add(new GachaponInfo.GachItem(1422031, true)); // Blue Seal Cushion
        equipments.add(new GachaponInfo.GachItem(1422037, true)); // Timeless Bellocce
        equipments.add(new GachaponInfo.GachItem(1422038, true)); // Reverse Bellocce
        equipments.add(new GachaponInfo.GachItem(1422040, true)); // Light Beam Saver
        equipments.add(new GachaponInfo.GachItem(1422041, true)); // Flairgrave
        equipments.add(new GachaponInfo.GachItem(1422044, true)); // GMS Maul
        equipments.add(new GachaponInfo.GachItem(1422062, true)); // Last Unwelcome Guest Two-Handed Blunt Weapon
        equipments.add(new GachaponInfo.GachItem(1422063, true)); // VIP Two-Handed Blunt Weapon
        equipments.add(new GachaponInfo.GachItem(1422067, true)); // Agares Bloody Maul
        equipments.add(new GachaponInfo.GachItem(1422073, true)); // Abyss Bellocce
        equipments.add(new GachaponInfo.GachItem(1422074, true)); // Fearless Bellocce
        equipments.add(new GachaponInfo.GachItem(1422078, true)); // Legendary Dragon Flame
        equipments.add(new GachaponInfo.GachItem(1422096, true)); // <Renegades> Dragon Flame
        equipments.add(new GachaponInfo.GachItem(1422097, true)); // <Renegades> Maple Starsmasher
        equipments.add(new GachaponInfo.GachItem(1422101, true)); // Golden Clover Bellocce
        equipments.add(new GachaponInfo.GachItem(1422102, true)); // Imperial Maul
        equipments.add(new GachaponInfo.GachItem(1422103, true)); // Necro Maul
        equipments.add(new GachaponInfo.GachItem(1422108, true)); // Grand Agares Maul
        equipments.add(new GachaponInfo.GachItem(1422116, true)); // Laser Sword
        equipments.add(new GachaponInfo.GachItem(1422117, true)); // Moon Baton
        equipments.add(new GachaponInfo.GachItem(1422118, true)); // Flame Tongue
        equipments.add(new GachaponInfo.GachItem(1422119, true)); // Crissagrim Blade
        equipments.add(new GachaponInfo.GachItem(1422120, true)); // Mercury Sword
        equipments.add(new GachaponInfo.GachItem(1422121, true)); // Tania Sword
        equipments.add(new GachaponInfo.GachItem(1422124, true)); // Loveless Bellocce
        equipments.add(new GachaponInfo.GachItem(1422136, true)); // Sovereign Maul
        equipments.add(new GachaponInfo.GachItem(1422146, true)); // FrontierA Amethysian Big Maul
        equipments.add(new GachaponInfo.GachItem(1422150, true)); // Opulent White Maul
        equipments.add(new GachaponInfo.GachItem(1422152, true)); // Revolution Maul
        equipments.add(new GachaponInfo.GachItem(1422155, true)); // Valiant Wings
        equipments.add(new GachaponInfo.GachItem(1422162, true)); // Amaranthine Great Maul
        equipments.add(new GachaponInfo.GachItem(1422163, true)); // Azure Great Maul
        equipments.add(new GachaponInfo.GachItem(1422169, true)); // Briser Maul
        equipments.add(new GachaponInfo.GachItem(1422170, true)); // Jaihin Two-handed Hammer
        equipments.add(new GachaponInfo.GachItem(1422186, true)); // Royal Von Leon Two-Handed Hammer
        equipments.add(new GachaponInfo.GachItem(1422187, true)); // Maple Treasure Maul
        equipments.add(new GachaponInfo.GachItem(1432038, true)); // Dragon Faltizan
        equipments.add(new GachaponInfo.GachItem(1432047, true)); // Timeless Alchupiz
        equipments.add(new GachaponInfo.GachItem(1432048, true)); // Pooh Pooh Shovel
        equipments.add(new GachaponInfo.GachItem(1432049, true)); // Reverse Alchupiz
        equipments.add(new GachaponInfo.GachItem(1432051, true)); // Light Beam Saver
        equipments.add(new GachaponInfo.GachItem(1432052, true)); // Flairgrave
        equipments.add(new GachaponInfo.GachItem(1432063, true)); // Night Raven Stormshear
        equipments.add(new GachaponInfo.GachItem(1432066, true)); // Bellum Spear
        equipments.add(new GachaponInfo.GachItem(1432080, true)); // Last Unwelcome Guest Spear
        equipments.add(new GachaponInfo.GachItem(1432081, true)); // VIP Spear
        equipments.add(new GachaponInfo.GachItem(1432084, true)); // Marx Von Leon Spear
        equipments.add(new GachaponInfo.GachItem(1432087, true)); // Agares Bloody Spear
        equipments.add(new GachaponInfo.GachItem(1432099, true)); // Abyss Alchupiz
        equipments.add(new GachaponInfo.GachItem(1432100, true)); // Fearless Alchupiz
        equipments.add(new GachaponInfo.GachItem(1432101, true)); // Victor Von Leon Spear
        equipments.add(new GachaponInfo.GachItem(1432102, true)); // Dragonic Bellum Spear
        equipments.add(new GachaponInfo.GachItem(1432106, true)); // Legendary Dragon Faltizan
        equipments.add(new GachaponInfo.GachItem(1432119, true)); // Marx Von Leon Spear
        equipments.add(new GachaponInfo.GachItem(1432125, true)); // <Renegades> Dragon Faltizan
        equipments.add(new GachaponInfo.GachItem(1432126, true)); // <Renegades> Maple Windreaver
        equipments.add(new GachaponInfo.GachItem(1432130, true)); // Golden Clover Alchupiz
        equipments.add(new GachaponInfo.GachItem(1432131, true)); // Imperial Spear
        equipments.add(new GachaponInfo.GachItem(1432133, true)); // Necro Spear
        equipments.add(new GachaponInfo.GachItem(1432139, true)); // Grand Agares Spear
        equipments.add(new GachaponInfo.GachItem(1432143, true)); // Crissagrim Blade
        equipments.add(new GachaponInfo.GachItem(1432144, true)); // Mercury Sword
        equipments.add(new GachaponInfo.GachItem(1432146, true)); // Guan Yu Spear
        equipments.add(new GachaponInfo.GachItem(1432147, true)); // Dragon's Fury
        equipments.add(new GachaponInfo.GachItem(1432150, true)); // Loveless Alchupiz
        equipments.add(new GachaponInfo.GachItem(1432164, true)); // Sovereign Spear
        equipments.add(new GachaponInfo.GachItem(1432173, true)); // FrontierA Amethysian Omni Pierce
        equipments.add(new GachaponInfo.GachItem(1432177, true)); // Opulent White Spear
        equipments.add(new GachaponInfo.GachItem(1432178, true)); // Revolution Spear
        equipments.add(new GachaponInfo.GachItem(1432181, true)); // Valiant Wings
        equipments.add(new GachaponInfo.GachItem(1432191, true)); // Amaranthine Spear
        equipments.add(new GachaponInfo.GachItem(1432192, true)); // Azure Spear
        equipments.add(new GachaponInfo.GachItem(1432198, true)); // Briser Spear
        equipments.add(new GachaponInfo.GachItem(1432199, true)); // Jaihin Spear
        equipments.add(new GachaponInfo.GachItem(1432201, true)); // Royal Von Leon Spear
        equipments.add(new GachaponInfo.GachItem(1432216, true)); // Maple Treasure Spear
        equipments.add(new GachaponInfo.GachItem(1432220, true)); // Special Lionheart Fuscina
        equipments.add(new GachaponInfo.GachItem(1442002, true)); // Eviscerator
        equipments.add(new GachaponInfo.GachItem(1442045, true)); // Dragon Hellslayer
        equipments.add(new GachaponInfo.GachItem(1442063, true)); // Timeless Diesra
        equipments.add(new GachaponInfo.GachItem(1442067, true)); // Reverse Diesra
        equipments.add(new GachaponInfo.GachItem(1442072, true)); // Light Beam Saver
        equipments.add(new GachaponInfo.GachItem(1442073, true)); // Flairgrave
        equipments.add(new GachaponInfo.GachItem(1442090, true)); // Machlear
        equipments.add(new GachaponInfo.GachItem(1442110, true)); // Last Unwelcome Guest Polearm
        equipments.add(new GachaponInfo.GachItem(1442111, true)); // VIP Polearm
        equipments.add(new GachaponInfo.GachItem(1442113, true)); // Marx Von Leon Halberd
        equipments.add(new GachaponInfo.GachItem(1442117, true)); // Agares Bloody Polearm
        equipments.add(new GachaponInfo.GachItem(1442136, true)); // Abyss Diesra
        equipments.add(new GachaponInfo.GachItem(1442137, true)); // Fearless Diesra
        equipments.add(new GachaponInfo.GachItem(1442138, true)); // Victor Von Leon Hellslayer
        equipments.add(new GachaponInfo.GachItem(1442139, true)); // Dragonic Machlear
        equipments.add(new GachaponInfo.GachItem(1442143, true)); // Legendary Dragon Hellslayer
        equipments.add(new GachaponInfo.GachItem(1442156, true)); // Marx Von Leon Halberd
        equipments.add(new GachaponInfo.GachItem(1442163, true)); // <Renegades> Dragon Hellslayer
        equipments.add(new GachaponInfo.GachItem(1442164, true)); // <Renegades> Maple Worldcutter
        equipments.add(new GachaponInfo.GachItem(1442168, true)); // Golden Clover Diesra
        equipments.add(new GachaponInfo.GachItem(1442169, true)); // Eviscerator
        equipments.add(new GachaponInfo.GachItem(1442170, true)); // Imperial Polearm
        equipments.add(new GachaponInfo.GachItem(1442171, true)); // Necro Polearm
        equipments.add(new GachaponInfo.GachItem(1442183, true)); // Grand Agares Polearm
        equipments.add(new GachaponInfo.GachItem(1442192, true)); // Crissagrim Blade
        equipments.add(new GachaponInfo.GachItem(1442193, true)); // Mercury Sword
        equipments.add(new GachaponInfo.GachItem(1442195, true)); // Guan Yu Spear
        equipments.add(new GachaponInfo.GachItem(1442196, true)); // Dragon's Fury
        equipments.add(new GachaponInfo.GachItem(1442202, true)); // Loveless Diesra
        equipments.add(new GachaponInfo.GachItem(1442219, true)); // Sovereign Polearm
        equipments.add(new GachaponInfo.GachItem(1442229, true)); // FrontierA Amethysian Halfmoon
        equipments.add(new GachaponInfo.GachItem(1442233, true)); // Opulent White Polearm
        equipments.add(new GachaponInfo.GachItem(1442234, true)); // Revolution Polearm
        equipments.add(new GachaponInfo.GachItem(1442237, true)); // Valiant Wings
        equipments.add(new GachaponInfo.GachItem(1442245, true)); // Amaranthine Polearm
        equipments.add(new GachaponInfo.GachItem(1442246, true)); // Azure Polearm
        equipments.add(new GachaponInfo.GachItem(1442252, true)); // Briser Polearm
        equipments.add(new GachaponInfo.GachItem(1442253, true)); // Jaihin Hellslayer
        equipments.add(new GachaponInfo.GachItem(1442255, true)); // Royal Von Leon Hellslayer
        equipments.add(new GachaponInfo.GachItem(1442270, true)); // Maple Treasure Polearm
        equipments.add(new GachaponInfo.GachItem(1442278, true)); // Special Lionheart Partisan
        equipments.add(new GachaponInfo.GachItem(1452044, true)); // Dragon Shiner Bow
        equipments.add(new GachaponInfo.GachItem(1452057, true)); // Timeless Engaw
        equipments.add(new GachaponInfo.GachItem(1452058, true)); // Barisetter
        equipments.add(new GachaponInfo.GachItem(1452059, true)); // Reverse Engaw
        equipments.add(new GachaponInfo.GachItem(1452063, true)); // Light Beam Saver
        equipments.add(new GachaponInfo.GachItem(1452064, true)); // Flairgrave
        equipments.add(new GachaponInfo.GachItem(1452087, true)); // Night Raven Bow
        equipments.add(new GachaponInfo.GachItem(1452105, true)); // Last Unwelcome Guest Bow
        equipments.add(new GachaponInfo.GachItem(1452106, true)); // VIP Bow
        equipments.add(new GachaponInfo.GachItem(1452107, true)); // Fox Von Leon Bow
        equipments.add(new GachaponInfo.GachItem(1452112, true)); // Ipos Bloody Longbow
        equipments.add(new GachaponInfo.GachItem(1452129, true)); // Abyss Engaw
        equipments.add(new GachaponInfo.GachItem(1452130, true)); // Fearless Engaw
        equipments.add(new GachaponInfo.GachItem(1452131, true)); // Celine Von Leon Bow
        equipments.add(new GachaponInfo.GachItem(1452132, true)); // Dragonic Barisetter
        equipments.add(new GachaponInfo.GachItem(1452136, true)); // Legendary Dragon Shiner Bow
        equipments.add(new GachaponInfo.GachItem(1452149, true)); // Fox Von Leon Bow
        equipments.add(new GachaponInfo.GachItem(1452155, true)); // <Renegades> Dragon Shiner Bow
        equipments.add(new GachaponInfo.GachItem(1452156, true)); // <Renegades> Maple Stormsinger
        equipments.add(new GachaponInfo.GachItem(1452161, true)); // Golden Clover Engaw
        equipments.add(new GachaponInfo.GachItem(1452162, true)); // Imperial Longbow
        equipments.add(new GachaponInfo.GachItem(1452163, true)); // Necro Longbow
        equipments.add(new GachaponInfo.GachItem(1452171, true)); // Grand Ipos Longbow
        equipments.add(new GachaponInfo.GachItem(1452180, true)); // Shiner
        equipments.add(new GachaponInfo.GachItem(1452181, true)); // Green Shiner
        equipments.add(new GachaponInfo.GachItem(1452182, true)); // Blue Shiner
        equipments.add(new GachaponInfo.GachItem(1452183, true)); // Purple Shiner
        equipments.add(new GachaponInfo.GachItem(1452184, true)); // Red Shiner
        equipments.add(new GachaponInfo.GachItem(1452185, true)); // Rainbow Bow
        equipments.add(new GachaponInfo.GachItem(1452186, true)); // Wild Hunter Crossbow
        equipments.add(new GachaponInfo.GachItem(1452189, true)); // Loveless Engaw
        equipments.add(new GachaponInfo.GachItem(1452202, true)); // Sovereign Longbow
        equipments.add(new GachaponInfo.GachItem(1452211, true)); // FrontierA Amethysian Ash Lord
        equipments.add(new GachaponInfo.GachItem(1452215, true)); // Opulent White Longbow
        equipments.add(new GachaponInfo.GachItem(1452216, true)); // Revolution Bow
        equipments.add(new GachaponInfo.GachItem(1452219, true)); // Swift Wings
        equipments.add(new GachaponInfo.GachItem(1452225, true)); // Last Unwelcome Guest Bow
        equipments.add(new GachaponInfo.GachItem(1452230, true)); // Amaranthine Ashen Bow
        equipments.add(new GachaponInfo.GachItem(1452231, true)); // Azure Ashen Bow
        equipments.add(new GachaponInfo.GachItem(1452236, true)); // Briser Longbow
        equipments.add(new GachaponInfo.GachItem(1452237, true)); // Jaihin Bow
        equipments.add(new GachaponInfo.GachItem(1452239, true)); // Royal Von Leon Bow
        equipments.add(new GachaponInfo.GachItem(1452255, true)); // Maple Treasure Longbow
        equipments.add(new GachaponInfo.GachItem(1452259, true)); // Special Falcon Wing Composite Bow
        equipments.add(new GachaponInfo.GachItem(1462039, true)); // Dragon Shiner Cross
        equipments.add(new GachaponInfo.GachItem(1462050, true)); // Timeless Black Beauty
        equipments.add(new GachaponInfo.GachItem(1462051, true)); // Reverse Black Beauty
        equipments.add(new GachaponInfo.GachItem(1462057, true)); // Light Beam Saver
        equipments.add(new GachaponInfo.GachItem(1462058, true)); // Flairgrave
        equipments.add(new GachaponInfo.GachItem(1462076, true)); // Inferna
        equipments.add(new GachaponInfo.GachItem(1462090, true)); // Last Unwelcome Guest Crossbow
        equipments.add(new GachaponInfo.GachItem(1462091, true)); // VIP Crossbow
        equipments.add(new GachaponInfo.GachItem(1462094, true)); // Fox Von Leon Crossbow
        equipments.add(new GachaponInfo.GachItem(1462100, true)); // Ipos Bloody Crossbow
        equipments.add(new GachaponInfo.GachItem(1462118, true)); // Abyss Black Beauty
        equipments.add(new GachaponInfo.GachItem(1462119, true)); // Fearless Black Beauty
        equipments.add(new GachaponInfo.GachItem(1462120, true)); // Celine Von Leon Crossbow
        equipments.add(new GachaponInfo.GachItem(1462121, true)); // Dragonic Inferna
        equipments.add(new GachaponInfo.GachItem(1462125, true)); // Legendary Dragon Shiner Cross
        equipments.add(new GachaponInfo.GachItem(1462139, true)); // Fox Von Leon Crossbow
        equipments.add(new GachaponInfo.GachItem(1462145, true)); // <Renegades> Dragon Shiner Cross
        equipments.add(new GachaponInfo.GachItem(1462146, true)); // <Renegades> Maple Stormdriver
        equipments.add(new GachaponInfo.GachItem(1462151, true)); // Golden Clover Black Beauty
        equipments.add(new GachaponInfo.GachItem(1462152, true)); // Imperial Crossbow
        equipments.add(new GachaponInfo.GachItem(1462153, true)); // Necro Crossbow
        equipments.add(new GachaponInfo.GachItem(1462160, true)); // Grand Ipos Crossbow
        equipments.add(new GachaponInfo.GachItem(1462169, true)); // Horoscope Crossbow
        equipments.add(new GachaponInfo.GachItem(1462170, true)); // Toy Machine Gun
        equipments.add(new GachaponInfo.GachItem(1462171, true)); // Water Gun
        equipments.add(new GachaponInfo.GachItem(1462172, true)); // Rainbow Bow
        equipments.add(new GachaponInfo.GachItem(1462173, true)); // Wild Hunter Crossbow
        equipments.add(new GachaponInfo.GachItem(1462177, true)); // Loveless Black Beauty
        equipments.add(new GachaponInfo.GachItem(1462190, true)); // Sovereign Crossbow
        equipments.add(new GachaponInfo.GachItem(1462199, true)); // FrontierA Amethysian Lock
        equipments.add(new GachaponInfo.GachItem(1462203, true)); // Opulent White Crossbow
        equipments.add(new GachaponInfo.GachItem(1462204, true)); // Revolution Dark Crow
        equipments.add(new GachaponInfo.GachItem(1462207, true)); // Swift Wings
        equipments.add(new GachaponInfo.GachItem(1462216, true)); // Amaranthine Crossbow
        equipments.add(new GachaponInfo.GachItem(1462217, true)); // Azure Crossbow
        equipments.add(new GachaponInfo.GachItem(1462223, true)); // Briser Crossbow
        equipments.add(new GachaponInfo.GachItem(1462224, true)); // Jaihin Crossbow
        equipments.add(new GachaponInfo.GachItem(1462226, true)); // Royal Von Leon Crossbow
        equipments.add(new GachaponInfo.GachItem(1462241, true)); // Maple Treasure Crossbow
        equipments.add(new GachaponInfo.GachItem(1462245, true)); // Special Falcon Wing Heavy Crossbow
        equipments.add(new GachaponInfo.GachItem(1472051, true)); // Dragon Green Sleeve
        equipments.add(new GachaponInfo.GachItem(1472052, true)); // Dragon Purple Sleeve
        equipments.add(new GachaponInfo.GachItem(1472068, true)); // Timeless Lampion
        equipments.add(new GachaponInfo.GachItem(1472069, true)); // Crypto
        equipments.add(new GachaponInfo.GachItem(1472071, true)); // Reverse Lampion
        equipments.add(new GachaponInfo.GachItem(1472078, true)); // Light Beam Saver
        equipments.add(new GachaponInfo.GachItem(1472079, true)); // Flairgrave
        equipments.add(new GachaponInfo.GachItem(1472085, true)); // Dragon Purple Sleve
        equipments.add(new GachaponInfo.GachItem(1472116, true)); // Last Unwelcome Guest Claw
        equipments.add(new GachaponInfo.GachItem(1472117, true)); // VIP Claw
        equipments.add(new GachaponInfo.GachItem(1472118, true)); // Nox Von Leon Guards
        equipments.add(new GachaponInfo.GachItem(1472123, true)); // Halphas Bloody Mist
        equipments.add(new GachaponInfo.GachItem(1472141, true)); // Abyss Lampion
        equipments.add(new GachaponInfo.GachItem(1472142, true)); // Fearless Rampion
        equipments.add(new GachaponInfo.GachItem(1472143, true)); // Scar Von Leon Guards
        equipments.add(new GachaponInfo.GachItem(1472144, true)); // Dragonic Crypto
        equipments.add(new GachaponInfo.GachItem(1472148, true)); // Legendary Green Sleeve
        equipments.add(new GachaponInfo.GachItem(1472161, true)); // Nox Von Leon Guards
        equipments.add(new GachaponInfo.GachItem(1472167, true)); // <Renegades> Dragon Green Sleeve
        equipments.add(new GachaponInfo.GachItem(1472168, true)); // <Renegades> Maple Soulchaser
        equipments.add(new GachaponInfo.GachItem(1472173, true)); // Golden Clover Lampion
        equipments.add(new GachaponInfo.GachItem(1472174, true)); // Imperial Bloody Mist
        equipments.add(new GachaponInfo.GachItem(1472175, true)); // Necro Bloody Mist
        equipments.add(new GachaponInfo.GachItem(1472180, true)); // Grand Helphas Myst
        equipments.add(new GachaponInfo.GachItem(1472189, true)); // Horoscope Claw
        equipments.add(new GachaponInfo.GachItem(1472190, true)); // Smackdown Fist
        equipments.add(new GachaponInfo.GachItem(1472191, true)); // Bionic Claw
        equipments.add(new GachaponInfo.GachItem(1472192, true)); // Blue Fist of Fury
        equipments.add(new GachaponInfo.GachItem(1472193, true)); // Red Fist of Fury
        equipments.add(new GachaponInfo.GachItem(1472194, true)); // Ice Flower
        equipments.add(new GachaponInfo.GachItem(1472197, true)); // Loveless Lampion
        equipments.add(new GachaponInfo.GachItem(1472211, true)); // Sovereign Bloody Mist
        equipments.add(new GachaponInfo.GachItem(1472220, true)); // FrontierA Amethysian Black Justice
        equipments.add(new GachaponInfo.GachItem(1472224, true)); // Opulent White Steer
        equipments.add(new GachaponInfo.GachItem(1472226, true)); // Revolution Steer
        equipments.add(new GachaponInfo.GachItem(1472229, true)); // Triumphant Wings
        equipments.add(new GachaponInfo.GachItem(1472238, true)); // Amaranthine Claw
        equipments.add(new GachaponInfo.GachItem(1472239, true)); // Azure Claw
        equipments.add(new GachaponInfo.GachItem(1472245, true)); // Briser Bloody Mist
        equipments.add(new GachaponInfo.GachItem(1472246, true)); // Jaihin Guards
        equipments.add(new GachaponInfo.GachItem(1472248, true)); // Royal Von Leon Guards
        equipments.add(new GachaponInfo.GachItem(1472263, true)); // Maple Treasure Thief Claw
        equipments.add(new GachaponInfo.GachItem(1472267, true)); // Special Raven Horn Metal Fist
        equipments.add(new GachaponInfo.GachItem(1482013, true)); // Dragon Slash Claw
        equipments.add(new GachaponInfo.GachItem(1482023, true)); // Timeless Equinox
        equipments.add(new GachaponInfo.GachItem(1482024, true)); // Reverse Equinox
        equipments.add(new GachaponInfo.GachItem(1482033, true)); // Emperor's Claw
        equipments.add(new GachaponInfo.GachItem(1482034, true)); // GMS Knuckle
        equipments.add(new GachaponInfo.GachItem(1482035, true)); // Flairgrave
        equipments.add(new GachaponInfo.GachItem(1482036, true)); // Light Beam Saver
        equipments.add(new GachaponInfo.GachItem(1482048, true)); // Night Raven Knuckle
        equipments.add(new GachaponInfo.GachItem(1482051, true)); // Crusio
        equipments.add(new GachaponInfo.GachItem(1482078, true)); // Last Unwelcome Guest Knuckle
        equipments.add(new GachaponInfo.GachItem(1482079, true)); // VIP Knuckle
        equipments.add(new GachaponInfo.GachItem(1482080, true)); // Cora Von Leon Claw
        equipments.add(new GachaponInfo.GachItem(1482085, true)); // Vepar Bloody Hands
        equipments.add(new GachaponInfo.GachItem(1482102, true)); // Abyss Equinox
        equipments.add(new GachaponInfo.GachItem(1482103, true)); // Fearless Equinox
        equipments.add(new GachaponInfo.GachItem(1482104, true)); // Mer Von Leon Claw
        equipments.add(new GachaponInfo.GachItem(1482105, true)); // Dragonic Crusio
        equipments.add(new GachaponInfo.GachItem(1482109, true)); // Legendary Dragon Slash Claw
        equipments.add(new GachaponInfo.GachItem(1482122, true)); // Cora Von Leon Claw
        equipments.add(new GachaponInfo.GachItem(1482128, true)); // <Renegades> Dragon Slash Claw
        equipments.add(new GachaponInfo.GachItem(1482129, true)); // <Renegades> Maple Soulclaimer
        equipments.add(new GachaponInfo.GachItem(1482134, true)); // Golden Clover Equinox
        equipments.add(new GachaponInfo.GachItem(1482135, true)); // Imperial Wolf
        equipments.add(new GachaponInfo.GachItem(1482136, true)); // Necro Wolf
        equipments.add(new GachaponInfo.GachItem(1482141, true)); // Grand Vepar Wolf
        equipments.add(new GachaponInfo.GachItem(1482146, true)); // Tiger Paw Knuckle
        equipments.add(new GachaponInfo.GachItem(1482147, true)); // Metallic Arm
        equipments.add(new GachaponInfo.GachItem(1482148, true)); // Lightning Soul
        equipments.add(new GachaponInfo.GachItem(1482151, true)); // Loveless Equinox
        equipments.add(new GachaponInfo.GachItem(1482165, true)); // Sovereign Wolf
        equipments.add(new GachaponInfo.GachItem(1482174, true)); // FrontierA Amethysian Bloody Claw
        equipments.add(new GachaponInfo.GachItem(1482178, true)); // Opulent White Grip
        equipments.add(new GachaponInfo.GachItem(1482179, true)); // Revolution Grip
        equipments.add(new GachaponInfo.GachItem(1482182, true)); // Triumphant Wings
        equipments.add(new GachaponInfo.GachItem(1482188, true)); // Last Unwelcome Guest Knuckle
        equipments.add(new GachaponInfo.GachItem(1482193, true)); // Amaranthine Fist
        equipments.add(new GachaponInfo.GachItem(1482194, true)); // Azure Fist
        equipments.add(new GachaponInfo.GachItem(1482200, true)); // Briser Knuckle
        equipments.add(new GachaponInfo.GachItem(1482201, true)); // Jaihin Claw
        equipments.add(new GachaponInfo.GachItem(1482203, true)); // Royal Von Leon Claw
        equipments.add(new GachaponInfo.GachItem(1482218, true)); // Maple Treasure Grip
        equipments.add(new GachaponInfo.GachItem(1482223, true)); // Special Shark Tooth Wild Talon
        equipments.add(new GachaponInfo.GachItem(1492013, true)); // Dragon Revolver
        equipments.add(new GachaponInfo.GachItem(1492023, true)); // Timeless Blindness
        equipments.add(new GachaponInfo.GachItem(1492024, true)); // Tempest
        equipments.add(new GachaponInfo.GachItem(1492025, true)); // Reverse Blindness
        equipments.add(new GachaponInfo.GachItem(1492031, true)); // Judgement
        equipments.add(new GachaponInfo.GachItem(1492047, true)); // Night Raven Shooter
        equipments.add(new GachaponInfo.GachItem(1492078, true)); // Last Unwelcome Guest Gun
        equipments.add(new GachaponInfo.GachItem(1492079, true)); // VIP Gun
        equipments.add(new GachaponInfo.GachItem(1492081, true)); // Cora Von Leon Pistol
        equipments.add(new GachaponInfo.GachItem(1492086, true)); // Vepar Bloody Eagle
        equipments.add(new GachaponInfo.GachItem(1492101, true)); // Abyss Blindness
        equipments.add(new GachaponInfo.GachItem(1492102, true)); // Fearless Blindness
        equipments.add(new GachaponInfo.GachItem(1492103, true)); // Mer Von Leon Pistol
        equipments.add(new GachaponInfo.GachItem(1492104, true)); // Dragonic Tempest
        equipments.add(new GachaponInfo.GachItem(1492108, true)); // Legendary Dragon Revolver
        equipments.add(new GachaponInfo.GachItem(1492122, true)); // Cora Von Leon Pistol
        equipments.add(new GachaponInfo.GachItem(1492128, true)); // <Renegades> Dragonfire Revolver
        equipments.add(new GachaponInfo.GachItem(1492129, true)); // <Renegades> Maple Firerunner
        equipments.add(new GachaponInfo.GachItem(1492134, true)); // Golden Clover Blindness
        equipments.add(new GachaponInfo.GachItem(1492135, true)); // Imperial Eagle
        equipments.add(new GachaponInfo.GachItem(1492136, true)); // Necro Eagle
        equipments.add(new GachaponInfo.GachItem(1492143, true)); // Gaussfield Mark 5
        equipments.add(new GachaponInfo.GachItem(1492153, true)); // Grand Vepar Eagle
        equipments.add(new GachaponInfo.GachItem(1492158, true)); // Saw Machine Gun
        equipments.add(new GachaponInfo.GachItem(1492162, true)); // Loveless Blindness
        equipments.add(new GachaponInfo.GachItem(1492176, true)); // Sovereign Eagle
        equipments.add(new GachaponInfo.GachItem(1492185, true)); // FrontierA Amethysian Queen's Finger
        equipments.add(new GachaponInfo.GachItem(1492189, true)); // Opulent White Shooter
        equipments.add(new GachaponInfo.GachItem(1492190, true)); // Revolution Shooting Star
        equipments.add(new GachaponInfo.GachItem(1492193, true)); // Triumphant Wings
        equipments.add(new GachaponInfo.GachItem(1492202, true)); // Amaranthine Revolver
        equipments.add(new GachaponInfo.GachItem(1492203, true)); // Azure Revolver
        equipments.add(new GachaponInfo.GachItem(1492210, true)); // Briser Gun
        equipments.add(new GachaponInfo.GachItem(1492211, true)); // Jaihin Pistol
        equipments.add(new GachaponInfo.GachItem(1492213, true)); // Royal Von Leon Pistol
        equipments.add(new GachaponInfo.GachItem(1492233, true)); // Maple Treasure Shooter
        equipments.add(new GachaponInfo.GachItem(1492237, true)); // Special Shark Tooth Sharpshooter
        equipments.add(new GachaponInfo.GachItem(1522013, true)); // Celestials
        equipments.add(new GachaponInfo.GachItem(1522014, true)); // Dragon Majesty
        equipments.add(new GachaponInfo.GachItem(1522015, true)); // Timeless Blooms
        equipments.add(new GachaponInfo.GachItem(1522016, true)); // Reverse Blooms
        equipments.add(new GachaponInfo.GachItem(1522017, true)); // Ipos Bloody Dual Bowguns
        equipments.add(new GachaponInfo.GachItem(1522019, true)); // Fox Von Leon Dual Bowguns
        equipments.add(new GachaponInfo.GachItem(1522020, true)); // Abyss Blooms
        equipments.add(new GachaponInfo.GachItem(1522021, true)); // Fearless Blooms
        equipments.add(new GachaponInfo.GachItem(1522022, true)); // Celine Von Leon Dual Bowguns
        equipments.add(new GachaponInfo.GachItem(1522023, true)); // Dragonic Celestials
        equipments.add(new GachaponInfo.GachItem(1522027, true)); // Legendary Dragon Majesty
        equipments.add(new GachaponInfo.GachItem(1522056, true)); // Fox Von Leon Dual Bowguns
        equipments.add(new GachaponInfo.GachItem(1522062, true)); // <Renegades> Dragon Majesty
        equipments.add(new GachaponInfo.GachItem(1522063, true)); // <Renegades> Maple Majesty
        equipments.add(new GachaponInfo.GachItem(1522064, true)); // Imperial Dual Bowguns
        equipments.add(new GachaponInfo.GachItem(1522067, true)); // Necro Dual Bowguns
        equipments.add(new GachaponInfo.GachItem(1522072, true)); // Grand Ipos Dual Bowguns
        equipments.add(new GachaponInfo.GachItem(1522078, true)); // Loveless Blooms
        equipments.add(new GachaponInfo.GachItem(1522091, true)); // Sovereign Dual Bowguns
        equipments.add(new GachaponInfo.GachItem(1522100, true)); // FrontierA Amethysian Argents
        equipments.add(new GachaponInfo.GachItem(1522104, true)); // Opulent White Twin Angels
        equipments.add(new GachaponInfo.GachItem(1522105, true)); // Revolution Twin Angels
        equipments.add(new GachaponInfo.GachItem(1522115, true)); // Amaranthine Dual Bowguns
        equipments.add(new GachaponInfo.GachItem(1522116, true)); // zure Dual Bowguns
        equipments.add(new GachaponInfo.GachItem(1522122, true)); // Briser Dual Bowguns
        equipments.add(new GachaponInfo.GachItem(1522123, true)); // Jaihin Dual Bowguns
        equipments.add(new GachaponInfo.GachItem(1522125, true)); // Royal Von Leon Dual Bowguns
        equipments.add(new GachaponInfo.GachItem(1522140, true)); // Maple Treasure Twin Angels
        equipments.add(new GachaponInfo.GachItem(1522145, true)); // Special Falcon Wing Dual Bowguns
        equipments.add(new GachaponInfo.GachItem(1532013, true)); // Gilded Titan
        equipments.add(new GachaponInfo.GachItem(1532014, true)); // Dragon Breaker
        equipments.add(new GachaponInfo.GachItem(1532015, true)); // Timeless Eradicator
        equipments.add(new GachaponInfo.GachItem(1532016, true)); // Reverse Eradicator
        equipments.add(new GachaponInfo.GachItem(1532017, true)); // Vepar Bloody Oblivion
        equipments.add(new GachaponInfo.GachItem(1532019, true)); // Cora Von Leon Siege Gun
        equipments.add(new GachaponInfo.GachItem(1532037, true)); // Abyss Eradicator
        equipments.add(new GachaponInfo.GachItem(1532038, true)); // Fearless Eradicator
        equipments.add(new GachaponInfo.GachItem(1532039, true)); // Mer Von Leon Siege Gun
        equipments.add(new GachaponInfo.GachItem(1532040, true)); // Dragonic Gilded Titan
        equipments.add(new GachaponInfo.GachItem(1532044, true)); // Legendary Dragon Breaker
        equipments.add(new GachaponInfo.GachItem(1532060, true)); // Cora Von Leon Siege Gun
        equipments.add(new GachaponInfo.GachItem(1532066, true)); // <Renegades> Dragon Breaker
        equipments.add(new GachaponInfo.GachItem(1532067, true)); // <Renegades> Maple Gustav
        equipments.add(new GachaponInfo.GachItem(1532068, true)); // Imperial Oblivion
        equipments.add(new GachaponInfo.GachItem(1532071, true)); // Necro Oblivion
        equipments.add(new GachaponInfo.GachItem(1532075, true)); // Grand Vepar Oblivion
        equipments.add(new GachaponInfo.GachItem(1532081, true)); // Loveless Eradicator
        equipments.add(new GachaponInfo.GachItem(1532095, true)); // Sovereign Oblivion
        equipments.add(new GachaponInfo.GachItem(1532103, true)); // FrontierA Amethysian Crash
        equipments.add(new GachaponInfo.GachItem(1532107, true)); // Opulent White Cannon
        equipments.add(new GachaponInfo.GachItem(1532109, true)); // Revolution Cannon
        equipments.add(new GachaponInfo.GachItem(1532121, true)); // Amaranthine Cannon
        equipments.add(new GachaponInfo.GachItem(1532122, true)); // Azure Cannon
        equipments.add(new GachaponInfo.GachItem(1532128, true)); // Briser Hand Cannon
        equipments.add(new GachaponInfo.GachItem(1532129, true)); // Jaihin Siege Gun
        equipments.add(new GachaponInfo.GachItem(1532131, true)); // Royal Von Leon Siege Gun
        equipments.add(new GachaponInfo.GachItem(1532146, true)); // Maple Treasure Cannon
        equipments.add(new GachaponInfo.GachItem(1532149, true)); // Special Shark Tooth Supernova
        equipments.add(new GachaponInfo.GachItem(1542010, true)); // Phoenix Rising
        equipments.add(new GachaponInfo.GachItem(1542012, true)); // Reverse Great Sword of Creation
        equipments.add(new GachaponInfo.GachItem(1542013, true)); // Timeless Great Sword of Creation
        equipments.add(new GachaponInfo.GachItem(1542014, true)); // Marx Von Leon's Ethereal Great Sword
        equipments.add(new GachaponInfo.GachItem(1542016, true)); // Agares Bloody Munetsuna
        equipments.add(new GachaponInfo.GachItem(1542033, true)); // Abyss Great Sword of Creation
        equipments.add(new GachaponInfo.GachItem(1542034, true)); // Fearless Great Sword of Creation
        equipments.add(new GachaponInfo.GachItem(1542035, true)); // Victor Von Leon's Ethereal Great Sword
        equipments.add(new GachaponInfo.GachItem(1542039, true)); // Imperial Katana
        equipments.add(new GachaponInfo.GachItem(1542040, true)); // Necromancer's Blade
        equipments.add(new GachaponInfo.GachItem(1542046, true)); // Dragonic Lapis Renewal
        equipments.add(new GachaponInfo.GachItem(1542047, true)); // Lapis Renewal
        equipments.add(new GachaponInfo.GachItem(1542056, true)); // Phoenix Ring
        equipments.add(new GachaponInfo.GachItem(1542057, true)); // Reverse Great Sword of Creation
        equipments.add(new GachaponInfo.GachItem(1542060, true)); // Loveless Great Sword of Creation
        equipments.add(new GachaponInfo.GachItem(1542074, true)); // Revolution Katana
        equipments.add(new GachaponInfo.GachItem(1542077, true)); // FrontierA Amethysian Katana
        equipments.add(new GachaponInfo.GachItem(1542080, true)); // Amaranthine Katana
        equipments.add(new GachaponInfo.GachItem(1542081, true)); // Azure Katana
        equipments.add(new GachaponInfo.GachItem(1542099, true)); // Briser Katana
        equipments.add(new GachaponInfo.GachItem(1542100, true)); // Jaihin Katana
        equipments.add(new GachaponInfo.GachItem(1542102, true)); // Royal Von Leon Katana
        equipments.add(new GachaponInfo.GachItem(1542116, true)); // Maple Treasure Katana
        equipments.add(new GachaponInfo.GachItem(1552010, true)); // Crossed Fan
        equipments.add(new GachaponInfo.GachItem(1552012, true)); // Reverse Fan of Altruism
        equipments.add(new GachaponInfo.GachItem(1552013, true)); // Timeless Fan of Altruism
        equipments.add(new GachaponInfo.GachItem(1552014, true)); // Alma Von Leon Paranormal Fan
        equipments.add(new GachaponInfo.GachItem(1552016, true)); // Eligos Bloody Banishment Fan
        equipments.add(new GachaponInfo.GachItem(1552033, true)); // Abyss Fan of Altruism
        equipments.add(new GachaponInfo.GachItem(1552034, true)); // Fearless Fan of Altruism
        equipments.add(new GachaponInfo.GachItem(1552035, true)); // Hex Von Leon Paranormal Fan
        equipments.add(new GachaponInfo.GachItem(1552039, true)); // Imperial Fan
        equipments.add(new GachaponInfo.GachItem(1552040, true)); // Necromancer's Fan
        equipments.add(new GachaponInfo.GachItem(1552046, true)); // Transcendent Dragon God
        equipments.add(new GachaponInfo.GachItem(1552047, true)); // Newborn Dragon God
        equipments.add(new GachaponInfo.GachItem(1552056, true)); // Crossed Fan
        equipments.add(new GachaponInfo.GachItem(1552057, true)); // Reverse Fan of Altruism
        equipments.add(new GachaponInfo.GachItem(1552060, true)); // Loveless Fan of Altruism
        equipments.add(new GachaponInfo.GachItem(1552077, true)); // FrontierA Amethysian Fan
        equipments.add(new GachaponInfo.GachItem(1552080, true)); // Amaranthine Fan
        equipments.add(new GachaponInfo.GachItem(1552081, true)); // Azure Fan
        equipments.add(new GachaponInfo.GachItem(1552100, true)); // Briser Fan
        equipments.add(new GachaponInfo.GachItem(1552101, true)); // Jaihin Fan
        equipments.add(new GachaponInfo.GachItem(1552103, true)); // Royal Von Leon Fan
        equipments.add(new GachaponInfo.GachItem(1552118, true)); // Maple Treasure Fan
        equipments.add(new GachaponInfo.GachItem(1582008, true)); // Valore Punch
        equipments.add(new GachaponInfo.GachItem(1582009, true)); // Briser Surtr
        equipments.add(new GachaponInfo.GachItem(1582010, true)); // Jaihin Jager
        equipments.add(new GachaponInfo.GachItem(1582013, true)); // Necromancer Gigas
        equipments.add(new GachaponInfo.GachItem(1582014, true)); // Royal Von Leon Ymir
        equipments.add(new GachaponInfo.GachItem(1582029, true)); // Special Lionheart Valore
    }

    private static void initConsume() {
        consumes = new ArrayList<>();
        consumes.add(new GachaponInfo.GachItem(2000000));
        consumes.add(new GachaponInfo.GachItem(2000001));
        consumes.add(new GachaponInfo.GachItem(2000002));
        consumes.add(new GachaponInfo.GachItem(2000003));
        consumes.add(new GachaponInfo.GachItem(2000004));
        consumes.add(new GachaponInfo.GachItem(2000005));
        consumes.add(new GachaponInfo.GachItem(2000006));
        consumes.add(new GachaponInfo.GachItem(2000007));
        consumes.add(new GachaponInfo.GachItem(2000008));
        consumes.add(new GachaponInfo.GachItem(2000009));
        consumes.add(new GachaponInfo.GachItem(2000010));
        consumes.add(new GachaponInfo.GachItem(2000011));
        consumes.add(new GachaponInfo.GachItem(2000013));
        consumes.add(new GachaponInfo.GachItem(2000014));
        consumes.add(new GachaponInfo.GachItem(2000015));
        consumes.add(new GachaponInfo.GachItem(2000016));
        consumes.add(new GachaponInfo.GachItem(2000017));
        consumes.add(new GachaponInfo.GachItem(2000018));
        consumes.add(new GachaponInfo.GachItem(2000019));
        consumes.add(new GachaponInfo.GachItem(2000020));
        consumes.add(new GachaponInfo.GachItem(2000021));
        consumes.add(new GachaponInfo.GachItem(2000022));
        consumes.add(new GachaponInfo.GachItem(2000023));
        consumes.add(new GachaponInfo.GachItem(2000024));
        consumes.add(new GachaponInfo.GachItem(2000025));
        consumes.add(new GachaponInfo.GachItem(2000026));
        consumes.add(new GachaponInfo.GachItem(2000027));
        consumes.add(new GachaponInfo.GachItem(2000028));
        consumes.add(new GachaponInfo.GachItem(2000029));
        consumes.add(new GachaponInfo.GachItem(2000030));
        consumes.add(new GachaponInfo.GachItem(2000031));
        consumes.add(new GachaponInfo.GachItem(2000032));
        consumes.add(new GachaponInfo.GachItem(2001000));
        consumes.add(new GachaponInfo.GachItem(2001001));
        consumes.add(new GachaponInfo.GachItem(2001002));
        consumes.add(new GachaponInfo.GachItem(2001003));
        consumes.add(new GachaponInfo.GachItem(2002000));
        consumes.add(new GachaponInfo.GachItem(2002001));
        consumes.add(new GachaponInfo.GachItem(2002002));
        consumes.add(new GachaponInfo.GachItem(2002003));
        consumes.add(new GachaponInfo.GachItem(2002004));
        consumes.add(new GachaponInfo.GachItem(2002005));
        consumes.add(new GachaponInfo.GachItem(2002006));
        consumes.add(new GachaponInfo.GachItem(2002007));
        consumes.add(new GachaponInfo.GachItem(2002008));
        consumes.add(new GachaponInfo.GachItem(2002009));
        consumes.add(new GachaponInfo.GachItem(2002010));
        consumes.add(new GachaponInfo.GachItem(2002011));
        consumes.add(new GachaponInfo.GachItem(2002015));
        consumes.add(new GachaponInfo.GachItem(2002016));
        consumes.add(new GachaponInfo.GachItem(2002017));
        consumes.add(new GachaponInfo.GachItem(2002018));
        consumes.add(new GachaponInfo.GachItem(2002019));
        consumes.add(new GachaponInfo.GachItem(2002020));
        consumes.add(new GachaponInfo.GachItem(2002021));
        consumes.add(new GachaponInfo.GachItem(2002022));
        consumes.add(new GachaponInfo.GachItem(2002023));
        consumes.add(new GachaponInfo.GachItem(2002024));
        consumes.add(new GachaponInfo.GachItem(2002025));
        consumes.add(new GachaponInfo.GachItem(2002026));
        consumes.add(new GachaponInfo.GachItem(2002027));
        consumes.add(new GachaponInfo.GachItem(2002028));
        consumes.add(new GachaponInfo.GachItem(2002029));
        consumes.add(new GachaponInfo.GachItem(2002030));
        consumes.add(new GachaponInfo.GachItem(2002035));
        consumes.add(new GachaponInfo.GachItem(2000012));
        consumes.add(new GachaponInfo.GachItem(2001500));
        consumes.add(new GachaponInfo.GachItem(2001501));
        consumes.add(new GachaponInfo.GachItem(2001502));
        consumes.add(new GachaponInfo.GachItem(2001503));
        consumes.add(new GachaponInfo.GachItem(2001504));
        consumes.add(new GachaponInfo.GachItem(2001505));
        consumes.add(new GachaponInfo.GachItem(2001506));
        consumes.add(new GachaponInfo.GachItem(2001507));
        consumes.add(new GachaponInfo.GachItem(2001508));
        consumes.add(new GachaponInfo.GachItem(2001509));
        consumes.add(new GachaponInfo.GachItem(2001510));
        consumes.add(new GachaponInfo.GachItem(2001511));
        consumes.add(new GachaponInfo.GachItem(2001512));
        consumes.add(new GachaponInfo.GachItem(2001513));
        consumes.add(new GachaponInfo.GachItem(2001514));
        consumes.add(new GachaponInfo.GachItem(2001515));
        consumes.add(new GachaponInfo.GachItem(2001516));
        consumes.add(new GachaponInfo.GachItem(2001517));
        consumes.add(new GachaponInfo.GachItem(2001518));
        consumes.add(new GachaponInfo.GachItem(2001519));
        consumes.add(new GachaponInfo.GachItem(2001520));
        consumes.add(new GachaponInfo.GachItem(2001521));
        consumes.add(new GachaponInfo.GachItem(2001522));
        consumes.add(new GachaponInfo.GachItem(2001523));
        consumes.add(new GachaponInfo.GachItem(2001524));
        consumes.add(new GachaponInfo.GachItem(2001525));
        consumes.add(new GachaponInfo.GachItem(2001526));
        consumes.add(new GachaponInfo.GachItem(2001527));
        consumes.add(new GachaponInfo.GachItem(2001528));
        consumes.add(new GachaponInfo.GachItem(2001529));
        consumes.add(new GachaponInfo.GachItem(2001530));
        consumes.add(new GachaponInfo.GachItem(2001531));
        consumes.add(new GachaponInfo.GachItem(2001532));
        consumes.add(new GachaponInfo.GachItem(2001533));
        consumes.add(new GachaponInfo.GachItem(2001534));
        consumes.add(new GachaponInfo.GachItem(2001535));
        consumes.add(new GachaponInfo.GachItem(2001536));
        consumes.add(new GachaponInfo.GachItem(2001537));
        consumes.add(new GachaponInfo.GachItem(2001538));
        consumes.add(new GachaponInfo.GachItem(2001539));
        consumes.add(new GachaponInfo.GachItem(2001540));
        consumes.add(new GachaponInfo.GachItem(2001541));
        consumes.add(new GachaponInfo.GachItem(2001542));
        consumes.add(new GachaponInfo.GachItem(2001543));
        consumes.add(new GachaponInfo.GachItem(2001544));
        consumes.add(new GachaponInfo.GachItem(2001545));
        consumes.add(new GachaponInfo.GachItem(2001546));
        consumes.add(new GachaponInfo.GachItem(2001547));
        consumes.add(new GachaponInfo.GachItem(2001548));
        consumes.add(new GachaponInfo.GachItem(2001549));
        consumes.add(new GachaponInfo.GachItem(2001550));
        consumes.add(new GachaponInfo.GachItem(2001551));
        consumes.add(new GachaponInfo.GachItem(2001552));
        consumes.add(new GachaponInfo.GachItem(2001553));
        consumes.add(new GachaponInfo.GachItem(2001554));
        consumes.add(new GachaponInfo.GachItem(2001555));
        consumes.add(new GachaponInfo.GachItem(2001558));
        consumes.add(new GachaponInfo.GachItem(2001557));
        consumes.add(new GachaponInfo.GachItem(2001563));
        consumes.add(new GachaponInfo.GachItem(2001564));
        consumes.add(new GachaponInfo.GachItem(2001565));
        consumes.add(new GachaponInfo.GachItem(2001566));
        consumes.add(new GachaponInfo.GachItem(2001567));
        consumes.add(new GachaponInfo.GachItem(2001568));
        consumes.add(new GachaponInfo.GachItem(2001588));
        consumes.add(new GachaponInfo.GachItem(2001589));
        consumes.add(new GachaponInfo.GachItem(2001590));
        consumes.add(new GachaponInfo.GachItem(2002039));
        consumes.add(new GachaponInfo.GachItem(2002040));
        consumes.add(new GachaponInfo.GachItem(2002041));
        consumes.add(new GachaponInfo.GachItem(2002042));
        consumes.add(new GachaponInfo.GachItem(2002043));
        consumes.add(new GachaponInfo.GachItem(2002044));
        consumes.add(new GachaponInfo.GachItem(2002045));
        consumes.add(new GachaponInfo.GachItem(2002046));
        consumes.add(new GachaponInfo.GachItem(2002047));
        consumes.add(new GachaponInfo.GachItem(2002048));
        consumes.add(new GachaponInfo.GachItem(2002049));
        consumes.add(new GachaponInfo.GachItem(2002050));
        consumes.add(new GachaponInfo.GachItem(2002051));
        consumes.add(new GachaponInfo.GachItem(2002093));
        consumes.add(new GachaponInfo.GachItem(2000036));
        consumes.add(new GachaponInfo.GachItem(2000037));
        consumes.add(new GachaponInfo.GachItem(2000040));
        consumes.add(new GachaponInfo.GachItem(2000041));
        consumes.add(new GachaponInfo.GachItem(2000042));
        consumes.add(new GachaponInfo.GachItem(2000038));
        consumes.add(new GachaponInfo.GachItem(2000044));
        consumes.add(new GachaponInfo.GachItem(2000035));
        consumes.add(new GachaponInfo.GachItem(2003500, true));
        consumes.add(new GachaponInfo.GachItem(2003501, true));
        consumes.add(new GachaponInfo.GachItem(2003502, true));
        consumes.add(new GachaponInfo.GachItem(2003503, true));
        consumes.add(new GachaponInfo.GachItem(2003504, true));
        consumes.add(new GachaponInfo.GachItem(2003505, true));
        consumes.add(new GachaponInfo.GachItem(2003506, true));
        consumes.add(new GachaponInfo.GachItem(2003507, true));
        consumes.add(new GachaponInfo.GachItem(2003508, true));
        consumes.add(new GachaponInfo.GachItem(2003509, true));
        consumes.add(new GachaponInfo.GachItem(2003510, true));
        consumes.add(new GachaponInfo.GachItem(2003511, true));
        consumes.add(new GachaponInfo.GachItem(2003512, true));
        consumes.add(new GachaponInfo.GachItem(2003513, true));
        consumes.add(new GachaponInfo.GachItem(2003514, true));
        consumes.add(new GachaponInfo.GachItem(2003515, true));
        consumes.add(new GachaponInfo.GachItem(2003516, true));
        consumes.add(new GachaponInfo.GachItem(2003517, true));
        consumes.add(new GachaponInfo.GachItem(2003518, true));
        consumes.add(new GachaponInfo.GachItem(2003519, true));
        consumes.add(new GachaponInfo.GachItem(2003520, true));
        consumes.add(new GachaponInfo.GachItem(2003521, true));
        consumes.add(new GachaponInfo.GachItem(2003522, true));
        consumes.add(new GachaponInfo.GachItem(2003523, true));
        consumes.add(new GachaponInfo.GachItem(2003524, true));
        consumes.add(new GachaponInfo.GachItem(2003525, true));
        consumes.add(new GachaponInfo.GachItem(2003526, true));
        consumes.add(new GachaponInfo.GachItem(2003527, true));
        consumes.add(new GachaponInfo.GachItem(2003528, true));
        consumes.add(new GachaponInfo.GachItem(2003529, true));
        consumes.add(new GachaponInfo.GachItem(2003530, true));
        consumes.add(new GachaponInfo.GachItem(2003531, true));
        consumes.add(new GachaponInfo.GachItem(2003532, true));
        consumes.add(new GachaponInfo.GachItem(2003533, true));
        consumes.add(new GachaponInfo.GachItem(2003534, true));
        consumes.add(new GachaponInfo.GachItem(2003535, true));
        consumes.add(new GachaponInfo.GachItem(2003536, true));
        consumes.add(new GachaponInfo.GachItem(2003537, true));
        consumes.add(new GachaponInfo.GachItem(2003538, true));
        consumes.add(new GachaponInfo.GachItem(2003539, true));
        consumes.add(new GachaponInfo.GachItem(2003540, true));
        consumes.add(new GachaponInfo.GachItem(2003541, true));
        consumes.add(new GachaponInfo.GachItem(2003542, true));
        consumes.add(new GachaponInfo.GachItem(2003543, true));
        consumes.add(new GachaponInfo.GachItem(2003544, true));
        consumes.add(new GachaponInfo.GachItem(2003545, true));
        consumes.add(new GachaponInfo.GachItem(2003546, true));
        consumes.add(new GachaponInfo.GachItem(2003547, true));
        consumes.add(new GachaponInfo.GachItem(2003549, true));
        consumes.add(new GachaponInfo.GachItem(2003550, true));
        consumes.add(new GachaponInfo.GachItem(2003551, true));
        consumes.add(new GachaponInfo.GachItem(2003552, true));
        consumes.add(new GachaponInfo.GachItem(2003553, true));
        consumes.add(new GachaponInfo.GachItem(2003554, true));
        consumes.add(new GachaponInfo.GachItem(2003555, true));
        consumes.add(new GachaponInfo.GachItem(2003556, true));
        consumes.add(new GachaponInfo.GachItem(2003557, true));
        consumes.add(new GachaponInfo.GachItem(2003558, true));
        consumes.add(new GachaponInfo.GachItem(2003559, true));
        consumes.add(new GachaponInfo.GachItem(2003560, true));
        consumes.add(new GachaponInfo.GachItem(2003561, true));
        consumes.add(new GachaponInfo.GachItem(2003562, true));
        consumes.add(new GachaponInfo.GachItem(2003563, true));
        consumes.add(new GachaponInfo.GachItem(2003564, true));
        consumes.add(new GachaponInfo.GachItem(2003565, true));
        consumes.add(new GachaponInfo.GachItem(2003566, true));
        consumes.add(new GachaponInfo.GachItem(2003567, true));
        consumes.add(new GachaponInfo.GachItem(2003568, true));
        consumes.add(new GachaponInfo.GachItem(2003570, true));
        consumes.add(new GachaponInfo.GachItem(2003571, true));
        consumes.add(new GachaponInfo.GachItem(2003572, true));
        consumes.add(new GachaponInfo.GachItem(2003573, true));
        consumes.add(new GachaponInfo.GachItem(2003574, true));
        consumes.add(new GachaponInfo.GachItem(2003575, true));
        consumes.add(new GachaponInfo.GachItem(2003576, true));
        consumes.add(new GachaponInfo.GachItem(2004240, true));
        consumes.add(new GachaponInfo.GachItem(2004241, true));
        consumes.add(new GachaponInfo.GachItem(2003577, true));
        consumes.add(new GachaponInfo.GachItem(2003578, true));
        consumes.add(new GachaponInfo.GachItem(2003579, true));
        consumes.add(new GachaponInfo.GachItem(2003580, true));
        consumes.add(new GachaponInfo.GachItem(2003581, true));
        consumes.add(new GachaponInfo.GachItem(2003582, true));
        consumes.add(new GachaponInfo.GachItem(2003583, true));
        consumes.add(new GachaponInfo.GachItem(2003584, true));
        consumes.add(new GachaponInfo.GachItem(2003585, true));
        consumes.add(new GachaponInfo.GachItem(2003586, true));
        consumes.add(new GachaponInfo.GachItem(2003587, true));
        consumes.add(new GachaponInfo.GachItem(2003588, true));
        consumes.add(new GachaponInfo.GachItem(2003589, true));
        consumes.add(new GachaponInfo.GachItem(2003048, true));
        consumes.add(new GachaponInfo.GachItem(2003049, true));
        consumes.add(new GachaponInfo.GachItem(2003592, true));
        consumes.add(new GachaponInfo.GachItem(2003593, true));
        consumes.add(new GachaponInfo.GachItem(2003594, true));
        consumes.add(new GachaponInfo.GachItem(2003595, true));
        consumes.add(new GachaponInfo.GachItem(2003596, true));
        consumes.add(new GachaponInfo.GachItem(2003597, true));
        consumes.add(new GachaponInfo.GachItem(2003598, true));
        consumes.add(new GachaponInfo.GachItem(2003599, true));
        consumes.add(new GachaponInfo.GachItem(2003047, true));
        consumes.add(new GachaponInfo.GachItem(2003046, true));
        consumes.add(new GachaponInfo.GachItem(2003591, true));
        consumes.add(new GachaponInfo.GachItem(2003604, true));
        consumes.add(new GachaponInfo.GachItem(2003605, true));
        consumes.add(new GachaponInfo.GachItem(2003606, true));
        consumes.add(new GachaponInfo.GachItem(2010000, true));
        consumes.add(new GachaponInfo.GachItem(2010001, true));
        consumes.add(new GachaponInfo.GachItem(2010002, true));
        consumes.add(new GachaponInfo.GachItem(2010003, true));
        consumes.add(new GachaponInfo.GachItem(2010004, true));
        consumes.add(new GachaponInfo.GachItem(2010005, true));
        consumes.add(new GachaponInfo.GachItem(2010006, true));
        consumes.add(new GachaponInfo.GachItem(2010007, true));
        consumes.add(new GachaponInfo.GachItem(2012000, true));
        consumes.add(new GachaponInfo.GachItem(2012001, true));
        consumes.add(new GachaponInfo.GachItem(2012002, true));
        consumes.add(new GachaponInfo.GachItem(2012003, true));
        consumes.add(new GachaponInfo.GachItem(2010009, true));
        consumes.add(new GachaponInfo.GachItem(2012005, true));
        consumes.add(new GachaponInfo.GachItem(2012006, true));
        consumes.add(new GachaponInfo.GachItem(2012008, true));
        consumes.add(new GachaponInfo.GachItem(2010010, true));
        consumes.add(new GachaponInfo.GachItem(2010012, true));
        consumes.add(new GachaponInfo.GachItem(2010013, true));
        consumes.add(new GachaponInfo.GachItem(2010017, true));
        consumes.add(new GachaponInfo.GachItem(2010018, true));
        consumes.add(new GachaponInfo.GachItem(2010021, true));
        consumes.add(new GachaponInfo.GachItem(2010022, true));
        consumes.add(new GachaponInfo.GachItem(2010023, true));
        consumes.add(new GachaponInfo.GachItem(2010024, true));
        consumes.add(new GachaponInfo.GachItem(2010025, true));
        consumes.add(new GachaponInfo.GachItem(2010026, true));
        consumes.add(new GachaponInfo.GachItem(2010027, true));
        consumes.add(new GachaponInfo.GachItem(2010028, true));
        consumes.add(new GachaponInfo.GachItem(2010029, true));
        consumes.add(new GachaponInfo.GachItem(2010030, true));
        consumes.add(new GachaponInfo.GachItem(2010034, true));
        consumes.add(new GachaponInfo.GachItem(2010035, true));
        consumes.add(new GachaponInfo.GachItem(2010037, true));
        consumes.add(new GachaponInfo.GachItem(2010038, true));
        consumes.add(new GachaponInfo.GachItem(2010039, true));
        consumes.add(new GachaponInfo.GachItem(2010041, true));
        consumes.add(new GachaponInfo.GachItem(2010042, true));
        consumes.add(new GachaponInfo.GachItem(2010043, true));
        consumes.add(new GachaponInfo.GachItem(2010044, true));
        consumes.add(new GachaponInfo.GachItem(2010045, true));
        consumes.add(new GachaponInfo.GachItem(2010052, true));
        consumes.add(new GachaponInfo.GachItem(2010014, true));
        consumes.add(new GachaponInfo.GachItem(2010032, true));
        consumes.add(new GachaponInfo.GachItem(2010033, true));
        consumes.add(new GachaponInfo.GachItem(2010051, true));
        consumes.add(new GachaponInfo.GachItem(2010040, true));
        consumes.add(new GachaponInfo.GachItem(2010047, true));
        consumes.add(new GachaponInfo.GachItem(2010048, true));
        consumes.add(new GachaponInfo.GachItem(2010049, true));
        consumes.add(new GachaponInfo.GachItem(2010050, true));
        consumes.add(new GachaponInfo.GachItem(2012015, true));
        consumes.add(new GachaponInfo.GachItem(2012016, true));
        consumes.add(new GachaponInfo.GachItem(2012017, true));
        consumes.add(new GachaponInfo.GachItem(2012018, true));
        consumes.add(new GachaponInfo.GachItem(2010016, true));
        consumes.add(new GachaponInfo.GachItem(2010015, true));
        consumes.add(new GachaponInfo.GachItem(2020000, true));
        consumes.add(new GachaponInfo.GachItem(2020001, true));
        consumes.add(new GachaponInfo.GachItem(2020002, true));
        consumes.add(new GachaponInfo.GachItem(2020003, true));
        consumes.add(new GachaponInfo.GachItem(2020004, true));
        consumes.add(new GachaponInfo.GachItem(2020005, true));
        consumes.add(new GachaponInfo.GachItem(2020006, true));
        consumes.add(new GachaponInfo.GachItem(2020007, true));
        consumes.add(new GachaponInfo.GachItem(2020008, true));
        consumes.add(new GachaponInfo.GachItem(2020009, true));
        consumes.add(new GachaponInfo.GachItem(2020010, true));
        consumes.add(new GachaponInfo.GachItem(2020011, true));
        consumes.add(new GachaponInfo.GachItem(2020012, true));
        consumes.add(new GachaponInfo.GachItem(2020013, true));
        consumes.add(new GachaponInfo.GachItem(2020014, true));
        consumes.add(new GachaponInfo.GachItem(2020015, true));
        consumes.add(new GachaponInfo.GachItem(2020016, true));
        consumes.add(new GachaponInfo.GachItem(2020017, true));
        consumes.add(new GachaponInfo.GachItem(2020018, true));
        consumes.add(new GachaponInfo.GachItem(2020019, true));
        consumes.add(new GachaponInfo.GachItem(2020020, true));
        consumes.add(new GachaponInfo.GachItem(2020021, true));
        consumes.add(new GachaponInfo.GachItem(2020022, true));
        consumes.add(new GachaponInfo.GachItem(2020023, true));
        consumes.add(new GachaponInfo.GachItem(2020024, true));
        consumes.add(new GachaponInfo.GachItem(2020025, true));
        consumes.add(new GachaponInfo.GachItem(2020026, true));
        consumes.add(new GachaponInfo.GachItem(2020027, true));
        consumes.add(new GachaponInfo.GachItem(2020028, true));
        consumes.add(new GachaponInfo.GachItem(2020029, true));
        consumes.add(new GachaponInfo.GachItem(2020030, true));
        consumes.add(new GachaponInfo.GachItem(2020031, true));
        consumes.add(new GachaponInfo.GachItem(2020032, true));
        consumes.add(new GachaponInfo.GachItem(2022000, true));
        consumes.add(new GachaponInfo.GachItem(2022001, true));
        consumes.add(new GachaponInfo.GachItem(2022002, true));
        consumes.add(new GachaponInfo.GachItem(2022003, true));
        consumes.add(new GachaponInfo.GachItem(2022004, true));
        consumes.add(new GachaponInfo.GachItem(2022005, true));
        consumes.add(new GachaponInfo.GachItem(2022006, true));
        consumes.add(new GachaponInfo.GachItem(2022007, true));
        consumes.add(new GachaponInfo.GachItem(2022008, true));
        consumes.add(new GachaponInfo.GachItem(2022009, true));
        consumes.add(new GachaponInfo.GachItem(2022010, true));
        consumes.add(new GachaponInfo.GachItem(2022011, true));
        consumes.add(new GachaponInfo.GachItem(2022012, true));
        consumes.add(new GachaponInfo.GachItem(2022013, true));
        consumes.add(new GachaponInfo.GachItem(2022014, true));
        consumes.add(new GachaponInfo.GachItem(2022015, true));
        consumes.add(new GachaponInfo.GachItem(2022016, true));
        consumes.add(new GachaponInfo.GachItem(2022017, true));
        consumes.add(new GachaponInfo.GachItem(2022018, true));
        consumes.add(new GachaponInfo.GachItem(2022019, true));
        consumes.add(new GachaponInfo.GachItem(2022020, true));
        consumes.add(new GachaponInfo.GachItem(2022021, true));
        consumes.add(new GachaponInfo.GachItem(2022022, true));
        consumes.add(new GachaponInfo.GachItem(2022023, true));
        consumes.add(new GachaponInfo.GachItem(2022024, true));
        consumes.add(new GachaponInfo.GachItem(2022025, true));
        consumes.add(new GachaponInfo.GachItem(2022026, true));
        consumes.add(new GachaponInfo.GachItem(2022027, true));
        consumes.add(new GachaponInfo.GachItem(2022028, true));
        consumes.add(new GachaponInfo.GachItem(2022029, true));
        consumes.add(new GachaponInfo.GachItem(2022030, true));
        consumes.add(new GachaponInfo.GachItem(2022031, true));
        consumes.add(new GachaponInfo.GachItem(2022032, true));
        consumes.add(new GachaponInfo.GachItem(2022033, true));
        consumes.add(new GachaponInfo.GachItem(2022035, true));
        consumes.add(new GachaponInfo.GachItem(2022037, true));
        consumes.add(new GachaponInfo.GachItem(2022038, true));
        consumes.add(new GachaponInfo.GachItem(2022039, true));
        consumes.add(new GachaponInfo.GachItem(2022040, true));
        consumes.add(new GachaponInfo.GachItem(2022041, true));
        consumes.add(new GachaponInfo.GachItem(2022042, true));
        consumes.add(new GachaponInfo.GachItem(2022043, true));
        consumes.add(new GachaponInfo.GachItem(2022044, true));
        consumes.add(new GachaponInfo.GachItem(2022045, true));
        consumes.add(new GachaponInfo.GachItem(2022047, true));
        consumes.add(new GachaponInfo.GachItem(2022048, true));
        consumes.add(new GachaponInfo.GachItem(2022049, true));
        consumes.add(new GachaponInfo.GachItem(2022050, true));
        consumes.add(new GachaponInfo.GachItem(2022051, true));
        consumes.add(new GachaponInfo.GachItem(2022052, true));
        consumes.add(new GachaponInfo.GachItem(2022053, true));
        consumes.add(new GachaponInfo.GachItem(2022054, true));
        consumes.add(new GachaponInfo.GachItem(2022055, true));
        consumes.add(new GachaponInfo.GachItem(2022056, true));
        consumes.add(new GachaponInfo.GachItem(2022057, true));
        consumes.add(new GachaponInfo.GachItem(2022058, true));
        consumes.add(new GachaponInfo.GachItem(2022060, true));
        consumes.add(new GachaponInfo.GachItem(2022061, true));
        consumes.add(new GachaponInfo.GachItem(2022062, true));
        consumes.add(new GachaponInfo.GachItem(2022063, true));
        consumes.add(new GachaponInfo.GachItem(2022064, true));
        consumes.add(new GachaponInfo.GachItem(2022065, true));
        consumes.add(new GachaponInfo.GachItem(2022066, true));
        consumes.add(new GachaponInfo.GachItem(2022068, true));
        consumes.add(new GachaponInfo.GachItem(2022069, true));
        consumes.add(new GachaponInfo.GachItem(2022070, true));
        consumes.add(new GachaponInfo.GachItem(2022071, true));
        consumes.add(new GachaponInfo.GachItem(2022072, true));
        consumes.add(new GachaponInfo.GachItem(2022073, true));
        consumes.add(new GachaponInfo.GachItem(2022074, true));
        consumes.add(new GachaponInfo.GachItem(2022075, true));
        consumes.add(new GachaponInfo.GachItem(2022076, true));
        consumes.add(new GachaponInfo.GachItem(2022077, true));
        consumes.add(new GachaponInfo.GachItem(2022078, true));
        consumes.add(new GachaponInfo.GachItem(2022079, true));
        consumes.add(new GachaponInfo.GachItem(2022089, true));
        consumes.add(new GachaponInfo.GachItem(2020033, true));
        consumes.add(new GachaponInfo.GachItem(2020034, true));
        consumes.add(new GachaponInfo.GachItem(2020035, true));
        consumes.add(new GachaponInfo.GachItem(2020045, true));
        consumes.add(new GachaponInfo.GachItem(2020046, true));
        consumes.add(new GachaponInfo.GachItem(2022036, true));
        consumes.add(new GachaponInfo.GachItem(2020048, true));
        consumes.add(new GachaponInfo.GachItem(2020049, true));
        consumes.add(new GachaponInfo.GachItem(2020053, true));
        consumes.add(new GachaponInfo.GachItem(2020054, true));
        consumes.add(new GachaponInfo.GachItem(2020044, true));
        consumes.add(new GachaponInfo.GachItem(2020047, true));
    }

    private static void initScroll() {
        consumes.add(new GachaponInfo.GachItem(2040000)); // Scroll for Helmet for DEF 100%
        consumes.add(new GachaponInfo.GachItem(2040001)); // Scroll for Helmet for DEF 60%
        consumes.add(new GachaponInfo.GachItem(2040002)); // Scroll for Helmet for DEF 10%
        consumes.add(new GachaponInfo.GachItem(2040003)); // Scroll for Helmet for HP 100%
        consumes.add(new GachaponInfo.GachItem(2040004)); // Scroll for Helmet for HP 60%
        consumes.add(new GachaponInfo.GachItem(2040005)); // Scroll for Helmet for HP 10%
        consumes.add(new GachaponInfo.GachItem(2040006)); // Scroll for Helmet for DEF 100%
        consumes.add(new GachaponInfo.GachItem(2040007)); // Scroll for Helmet for HP 100%
        consumes.add(new GachaponInfo.GachItem(2040008)); // Scroll for Helmet for DEF 70%
        consumes.add(new GachaponInfo.GachItem(2040009)); // Dark Scroll for Helmet for DEF 30%
        consumes.add(new GachaponInfo.GachItem(2040010)); // Scroll for Helmet for HP 70%
        consumes.add(new GachaponInfo.GachItem(2040011)); // Dark Scroll for Helmet for HP 30%
        consumes.add(new GachaponInfo.GachItem(2040012)); // Dark Scroll for Helmet for INT 70%
        consumes.add(new GachaponInfo.GachItem(2040013)); // Dark Scroll for Helmet for INT 30%
        consumes.add(new GachaponInfo.GachItem(2040014)); // Dark Scroll for Helmet for Accuracy 70%
        consumes.add(new GachaponInfo.GachItem(2040015)); // Dark Scroll for Helmet for Accuracy 30%
        consumes.add(new GachaponInfo.GachItem(2040016)); // Scroll for Helmet for Accuracy 10%
        consumes.add(new GachaponInfo.GachItem(2040017)); // Scroll for Helmet for Accuracy 60%
        consumes.add(new GachaponInfo.GachItem(2040018)); // Scroll for Helmet for Accuracy 100%
        consumes.add(new GachaponInfo.GachItem(2040019)); // Scroll for Helmet for DEF 65%
        consumes.add(new GachaponInfo.GachItem(2040020)); // Scroll for Helmet for DEF 15%
        consumes.add(new GachaponInfo.GachItem(2040021)); // Scroll for Helmet for MaxHP 65%
        consumes.add(new GachaponInfo.GachItem(2040022)); // Scroll for Helmet for MaxHP 15%
        consumes.add(new GachaponInfo.GachItem(2040023)); // Scroll for Rudolph's Horn 60%
        consumes.add(new GachaponInfo.GachItem(2040024)); // Scroll for Helmet for INT 100%
        consumes.add(new GachaponInfo.GachItem(2040025)); // Scroll for Helmet for INT 60%
        consumes.add(new GachaponInfo.GachItem(2040026)); // Scroll for Helmet for INT 10%
        consumes.add(new GachaponInfo.GachItem(2040027)); // Scroll for Helmet for DEX 100%
        consumes.add(new GachaponInfo.GachItem(2040028)); // Scroll for Helmet for DEX 70%
        consumes.add(new GachaponInfo.GachItem(2040029)); // Scroll for Helmet for DEX 60%
        consumes.add(new GachaponInfo.GachItem(2040030)); // Scroll for Helmet for DEX 30%
        consumes.add(new GachaponInfo.GachItem(2040031)); // Scroll for Helmet for DEX 10%
        consumes.add(new GachaponInfo.GachItem(2040041)); // Scroll for Helmet for DEF 100%
        consumes.add(new GachaponInfo.GachItem(2040042)); // Scroll for Helmet for HP 100%
        consumes.add(new GachaponInfo.GachItem(2040045)); // Scroll for Helmet for DEF 50%
        consumes.add(new GachaponInfo.GachItem(2040046)); // Scroll for Helmet for HP 50%
        consumes.add(new GachaponInfo.GachItem(2040100)); // Scroll for Face Accessory for HP 10%
        consumes.add(new GachaponInfo.GachItem(2040101)); // Scroll for Face Accessory for HP 60%
        consumes.add(new GachaponInfo.GachItem(2040102)); // Scroll for Face Accessory for HP 100%
        consumes.add(new GachaponInfo.GachItem(2040103)); // Dark Scroll for Face Accessory for HP 30%
        consumes.add(new GachaponInfo.GachItem(2040104)); // Dark Scroll for Face Accessory for HP 70%
        consumes.add(new GachaponInfo.GachItem(2040105)); // Scroll for Face Accessory for Avoidability 10%
        consumes.add(new GachaponInfo.GachItem(2040106)); // Scroll for Face Accessory for Avoidability 60%
        consumes.add(new GachaponInfo.GachItem(2040107)); // Scroll for Face Accessory for Avoidability 100%
        consumes.add(new GachaponInfo.GachItem(2040108)); // Dark Scroll for Face Accessory for Avoidability 30%
        consumes.add(new GachaponInfo.GachItem(2040109)); // Dark Scroll for Face Accessory for Avoidability 70%
        consumes.add(new GachaponInfo.GachItem(2040200)); // Scroll for Eye Accessory for Accuracy 10%
        consumes.add(new GachaponInfo.GachItem(2040201)); // Scroll for Eye Accessory for Accuracy 60%
        consumes.add(new GachaponInfo.GachItem(2040202)); // Scroll for Eye Accessory for Accuracy 100%
        consumes.add(new GachaponInfo.GachItem(2040203)); // Dark Scroll for Eye Accessory for Accuracy 30%
        consumes.add(new GachaponInfo.GachItem(2040204)); // Dark Scroll for Eye Accessory for Accuracy 70%
        consumes.add(new GachaponInfo.GachItem(2040205)); // Scroll for Eye Accessory for INT 10%
        consumes.add(new GachaponInfo.GachItem(2040206)); // Scroll for Eye Accessory for INT 60%
        consumes.add(new GachaponInfo.GachItem(2040207)); // Scroll for Eye Accessory for INT 100%
        consumes.add(new GachaponInfo.GachItem(2040208)); // Dark Scroll for Eye Accessory for INT 30%
        consumes.add(new GachaponInfo.GachItem(2040209)); // Dark Scroll for Eye Accessory for INT 70%
        consumes.add(new GachaponInfo.GachItem(2040300)); // Scroll for Earring for INT 100%
        consumes.add(new GachaponInfo.GachItem(2040301)); // Scroll for Earring for INT 60%
        consumes.add(new GachaponInfo.GachItem(2040302)); // Scroll for Earring for INT 10%
        consumes.add(new GachaponInfo.GachItem(2040303)); // Scroll for Earring for INT 30%
        consumes.add(new GachaponInfo.GachItem(2040304)); // Dark scroll for Earring for INT 70%
        consumes.add(new GachaponInfo.GachItem(2040305)); // Dark scroll for Earring for INT 30%
        consumes.add(new GachaponInfo.GachItem(2040306)); // Dark scroll for Earring for DEX 70%
        consumes.add(new GachaponInfo.GachItem(2040307)); // Dark scroll for Earring for DEX 30%
        consumes.add(new GachaponInfo.GachItem(2040308)); // Dark Scroll for Earrings for DEF 70%
        consumes.add(new GachaponInfo.GachItem(2040309)); // Dark Scroll for Earrings for DEF 30%
        consumes.add(new GachaponInfo.GachItem(2040310)); // Scroll for Earring for DEF 10%
        consumes.add(new GachaponInfo.GachItem(2040311)); // Scroll for Earring for DEF 60%
        consumes.add(new GachaponInfo.GachItem(2040312)); // Scroll for Earring for DEF 100%
        consumes.add(new GachaponInfo.GachItem(2040313)); // Scroll for Earring for INT 65%
        consumes.add(new GachaponInfo.GachItem(2040314)); // Scroll for Earring for INT 15%
        consumes.add(new GachaponInfo.GachItem(2040316)); // Scroll for Earring for DEX 100%
        consumes.add(new GachaponInfo.GachItem(2040317)); // Scroll for Earring for DEX 60%
        consumes.add(new GachaponInfo.GachItem(2040318)); // Scroll for Earring for DEX 10%
        consumes.add(new GachaponInfo.GachItem(2040319)); // Scroll for Earring for LUK 100%
        consumes.add(new GachaponInfo.GachItem(2040320)); // Scroll for Earring for LUK 70%
        consumes.add(new GachaponInfo.GachItem(2040321)); // Scroll for Earring for LUK 60%
        consumes.add(new GachaponInfo.GachItem(2040322)); // Scroll for Earring for LUK 30%
        consumes.add(new GachaponInfo.GachItem(2040323)); // Scroll for Earring for LUK 10%
        consumes.add(new GachaponInfo.GachItem(2040324)); // Scroll for Earring for HP 100%
        consumes.add(new GachaponInfo.GachItem(2040325)); // Scroll for Earring for HP 70%
        consumes.add(new GachaponInfo.GachItem(2040326)); // Scroll for Earring for HP 60%
        consumes.add(new GachaponInfo.GachItem(2040327)); // Scroll for Earring for HP 30%
        consumes.add(new GachaponInfo.GachItem(2040328)); // Scroll for Earring for HP 10%
        consumes.add(new GachaponInfo.GachItem(2040329)); // Scroll for Earring for DEX 10%
        consumes.add(new GachaponInfo.GachItem(2040330)); // Scroll for Earring for INT 10%
        consumes.add(new GachaponInfo.GachItem(2040331)); // Scroll for Earring for LUK 10%
        consumes.add(new GachaponInfo.GachItem(2040333)); // Scroll for Earring for INT 50%
        consumes.add(new GachaponInfo.GachItem(2040334)); // Scroll for Earring for INT 100%
        consumes.add(new GachaponInfo.GachItem(2040400)); // Scroll for Topwear for DEF
        consumes.add(new GachaponInfo.GachItem(2040401)); // Scroll for Topwear for DEF 60%
        consumes.add(new GachaponInfo.GachItem(2040402)); // Scroll for Topwear for DEF 10%
        consumes.add(new GachaponInfo.GachItem(2040403)); // Scroll for Topwear for DEF 100%
        consumes.add(new GachaponInfo.GachItem(2040404)); // Dark scroll for Topwear for DEF 70%
        consumes.add(new GachaponInfo.GachItem(2040405)); // Dark scroll for Topwear for DEF 30%
        consumes.add(new GachaponInfo.GachItem(2040406)); // Dark scroll for Topwear for STR 70%
        consumes.add(new GachaponInfo.GachItem(2040407)); // Dark scroll for Topwear for STR 30%
        consumes.add(new GachaponInfo.GachItem(2040408)); // Dark scroll for Topwear for HP 70%
        consumes.add(new GachaponInfo.GachItem(2040409)); // Dark scroll for Topwear for HP 30%
        consumes.add(new GachaponInfo.GachItem(2040410)); // Dark Scroll for Topwear for LUK 70%
        consumes.add(new GachaponInfo.GachItem(2040411)); // Dark Scroll for Topwear for LUK 30%
        consumes.add(new GachaponInfo.GachItem(2040412)); // Scroll for Topwear for LUK 10%
        consumes.add(new GachaponInfo.GachItem(2040413)); // Scroll for Topwear for LUK 60%
        consumes.add(new GachaponInfo.GachItem(2040414)); // Scroll for Topwear for LUK 100%
        consumes.add(new GachaponInfo.GachItem(2040415)); // Scroll for Topwear for DEF 65%
        consumes.add(new GachaponInfo.GachItem(2040416)); // Scroll for Topwear for DEF 15%
        consumes.add(new GachaponInfo.GachItem(2040417)); // Scroll for Topwear for STR 100%
        consumes.add(new GachaponInfo.GachItem(2040418)); // Scroll for Topwear for STR 60%
        consumes.add(new GachaponInfo.GachItem(2040419)); // Scroll for Topwear for STR 10%
        consumes.add(new GachaponInfo.GachItem(2040420)); // Scroll for Topwear for HP 100%
        consumes.add(new GachaponInfo.GachItem(2040421)); // Scroll for Topwear for HP 60%
        consumes.add(new GachaponInfo.GachItem(2040422)); // Scroll for Topwear for HP 10%
        consumes.add(new GachaponInfo.GachItem(2040423)); // Scroll for Topwear for LUK 100%
        consumes.add(new GachaponInfo.GachItem(2040424)); // Scroll for Topwear for LUK 70%
        consumes.add(new GachaponInfo.GachItem(2040425)); // Scroll for Topwear for LUK 60%
        consumes.add(new GachaponInfo.GachItem(2040426)); // Scroll for Topwear for LUK 30%
        consumes.add(new GachaponInfo.GachItem(2040427)); // Scroll for Topwear for LUK 10%
        consumes.add(new GachaponInfo.GachItem(2040429)); // Scroll for Topwear for DEF 50%
        consumes.add(new GachaponInfo.GachItem(2040430)); // Scroll for Topwear for DEF 100%
        consumes.add(new GachaponInfo.GachItem(2040500)); // Scroll for Overall Armor for DEX 100%
        consumes.add(new GachaponInfo.GachItem(2040501)); // Scroll for Overall Armor for DEX 60%
        consumes.add(new GachaponInfo.GachItem(2040502)); // Scroll for Overall Armor for DEX 10%
        consumes.add(new GachaponInfo.GachItem(2040503)); // Scroll for Overall Armor for DEF 100%
        consumes.add(new GachaponInfo.GachItem(2040504)); // Scroll for Overall Armor for DEF 60%
        consumes.add(new GachaponInfo.GachItem(2040505)); // Scroll for Overall Armor for DEF 10%
        consumes.add(new GachaponInfo.GachItem(2040506)); // Scroll for Overall Armor for DEX 100%
        consumes.add(new GachaponInfo.GachItem(2040507)); // Scroll for Overall Armor for DEF 30%
        consumes.add(new GachaponInfo.GachItem(2040508)); // Scroll for Overall Armor for DEX 70%
        consumes.add(new GachaponInfo.GachItem(2040509)); // Scroll for Overall Armor for DEX 30%
        consumes.add(new GachaponInfo.GachItem(2040510)); // Dark scroll for Overall Armor for DEF 70%
        consumes.add(new GachaponInfo.GachItem(2040511)); // Dark scroll for Overall Armor for DEF 30%
        consumes.add(new GachaponInfo.GachItem(2040512)); // Scroll for Overall Armor for INT 100%
        consumes.add(new GachaponInfo.GachItem(2040513)); // Scroll for Overall Armor for INT 60%
        consumes.add(new GachaponInfo.GachItem(2040514)); // Scroll for Overall Armor for INT 10%
        consumes.add(new GachaponInfo.GachItem(2040515)); // Scroll for Overall Armor for LUK 100%
        consumes.add(new GachaponInfo.GachItem(2040516)); // Scroll for Overall Armor for LUK 60%
        consumes.add(new GachaponInfo.GachItem(2040517)); // Scroll for Overall Armor for LUK 10%
        consumes.add(new GachaponInfo.GachItem(2040518)); // Dark scroll for Overall Armor for INT 70%
        consumes.add(new GachaponInfo.GachItem(2040519)); // Dark scroll for Overall Armor for INT 30%
        consumes.add(new GachaponInfo.GachItem(2040520)); // Dark scroll for Overall Armor for LUK 70%
        consumes.add(new GachaponInfo.GachItem(2040521)); // Dark scroll for Overall Armor for LUK 30%
        consumes.add(new GachaponInfo.GachItem(2040522)); // Scroll for Overall Armor for DEX 65%
        consumes.add(new GachaponInfo.GachItem(2040523)); // Scroll for Overall Armor for DEX 15%
        consumes.add(new GachaponInfo.GachItem(2040524)); // Overall Armor Scroll for DEF 65%
        consumes.add(new GachaponInfo.GachItem(2040525)); // Overall Armor Scroll for DEF 15%
        consumes.add(new GachaponInfo.GachItem(2040526)); // Scroll for Overall Armor for INT 65%
        consumes.add(new GachaponInfo.GachItem(2040527)); // Scroll for Overall Armor for INT 15%
        consumes.add(new GachaponInfo.GachItem(2040528)); // Scroll for Overall Armor for LUK 65%
        consumes.add(new GachaponInfo.GachItem(2040529)); // Scroll for Overall Armor for LUK 15%
        consumes.add(new GachaponInfo.GachItem(2040530)); // Scroll for Overall for STR 100%
        consumes.add(new GachaponInfo.GachItem(2040531)); // Scroll for Overall for STR 70%
        consumes.add(new GachaponInfo.GachItem(2040532)); // Scroll for Overall for STR 60%
        consumes.add(new GachaponInfo.GachItem(2040533)); // Scroll for Overall for STR 30%
        consumes.add(new GachaponInfo.GachItem(2040534)); // Scroll for Overall for STR 10%
        consumes.add(new GachaponInfo.GachItem(2040538)); // Scroll for Overall Armor for DEX 100%
        consumes.add(new GachaponInfo.GachItem(2040539)); // Scroll for Overall Armor for DEF 100%
        consumes.add(new GachaponInfo.GachItem(2040542)); // Scroll for Overall Armor for DEX 50%
        consumes.add(new GachaponInfo.GachItem(2040543)); // Scroll for Overall Armor for DEF 50%
        consumes.add(new GachaponInfo.GachItem(2040600)); // Scroll for Bottomwear for DEF 100%
        consumes.add(new GachaponInfo.GachItem(2040601)); // Scroll for Bottomwear for DEF 60%
        consumes.add(new GachaponInfo.GachItem(2040602)); // Scroll for Bottomwear for DEF 10%
        consumes.add(new GachaponInfo.GachItem(2040603)); // Scroll for Bottomwear for DEF 100%
        consumes.add(new GachaponInfo.GachItem(2040604)); // Dark scroll for Bottomwear for DEF 70%
        consumes.add(new GachaponInfo.GachItem(2040605)); // Dark scroll for Bottomwear for DEF 30%
        consumes.add(new GachaponInfo.GachItem(2040606)); // Dark scroll for Bottomwear for Jump 70%
        consumes.add(new GachaponInfo.GachItem(2040607)); // Dark scroll for Bottomwear for Jump 30%
        consumes.add(new GachaponInfo.GachItem(2040608)); // Dark scroll for Bottomwear for HP 70%
        consumes.add(new GachaponInfo.GachItem(2040609)); // Dark scroll for Bottomwear for HP 30%
        consumes.add(new GachaponInfo.GachItem(2040610)); // Dark Scroll for Bottomwear for DEX 70%
        consumes.add(new GachaponInfo.GachItem(2040611)); // Dark Scroll for Bottomwear for DEX 30%
        consumes.add(new GachaponInfo.GachItem(2040612)); // Scroll for Bottomwear for DEX 10%
        consumes.add(new GachaponInfo.GachItem(2040613)); // Scroll for Bottomwear for DEX 60%
        consumes.add(new GachaponInfo.GachItem(2040614)); // Scroll for Bottomwear for DEX 100%
        consumes.add(new GachaponInfo.GachItem(2040615)); // Scroll for Bottomwear for DEF 65%
        consumes.add(new GachaponInfo.GachItem(2040616)); // Scroll for Bottomwear for DEF 15%
        consumes.add(new GachaponInfo.GachItem(2040617)); // Scroll for Bottomwear for Jump 100%
        consumes.add(new GachaponInfo.GachItem(2040618)); // Scroll for Bottomwear for Jump 60%
        consumes.add(new GachaponInfo.GachItem(2040619)); // Scroll for Bottomwear for Jump 10%
        consumes.add(new GachaponInfo.GachItem(2040620)); // Scroll for Bottomwear for HP 100%
        consumes.add(new GachaponInfo.GachItem(2040621)); // Scroll for Bottomwear for HP 60%
        consumes.add(new GachaponInfo.GachItem(2040622)); // Scroll for Bottomwear for HP 10%
        consumes.add(new GachaponInfo.GachItem(2040623)); // Scroll for Bottomwear for DEX 100%
        consumes.add(new GachaponInfo.GachItem(2040624)); // Scroll for Bottomwear for DEX 70%
        consumes.add(new GachaponInfo.GachItem(2040625)); // Scroll for Bottomwear for DEX 60%
        consumes.add(new GachaponInfo.GachItem(2040626)); // Scroll for Bottomwear for DEX 30%
        consumes.add(new GachaponInfo.GachItem(2040627)); // Scroll for Bottomwear for DEX 10%
        consumes.add(new GachaponInfo.GachItem(2040629)); // Scroll for Bottomwear for DEF 50%
        consumes.add(new GachaponInfo.GachItem(2040630)); // Scroll for Bottomwear for DEF 100%
        consumes.add(new GachaponInfo.GachItem(2040700)); // Scroll for Shoes for Avoidability 100%
        consumes.add(new GachaponInfo.GachItem(2040701)); // Scroll for Shoes for Avoidability 60%
        consumes.add(new GachaponInfo.GachItem(2040702)); // Scroll for Shoes for Avoidability 10%
        consumes.add(new GachaponInfo.GachItem(2040703)); // Scroll for Shoes for Jump 100%
        consumes.add(new GachaponInfo.GachItem(2040704)); // Scroll for Shoes for Jump 60%
        consumes.add(new GachaponInfo.GachItem(2040705)); // Scroll for Shoes for Jump 10%
        consumes.add(new GachaponInfo.GachItem(2040706)); // Scroll for Shoes for Speed 100%
        consumes.add(new GachaponInfo.GachItem(2040707)); // Scroll for Shoes for Speed 60%
        consumes.add(new GachaponInfo.GachItem(2040708)); // Scroll for Shoes for Speed 10%
        consumes.add(new GachaponInfo.GachItem(2040709)); // Scroll for Shoes for DEX 100%
        consumes.add(new GachaponInfo.GachItem(2040710)); // Scroll for Shoes for Jump 100%
        consumes.add(new GachaponInfo.GachItem(2040711)); // Scroll for Shoes for Speed 100%
        consumes.add(new GachaponInfo.GachItem(2040712)); // Scroll for Shoes for DEX 70%
        consumes.add(new GachaponInfo.GachItem(2040713)); // Scroll for Shoes for DEX 30%
        consumes.add(new GachaponInfo.GachItem(2040714)); // Dark scroll for Shoes for Jump 70%
        consumes.add(new GachaponInfo.GachItem(2040715)); // Dark scroll for Shoes for Jump 30%
        consumes.add(new GachaponInfo.GachItem(2040716)); // Dark scroll for Shoes for Speed 70%
        consumes.add(new GachaponInfo.GachItem(2040717)); // Dark scroll for Shoes for Speed 30%
        consumes.add(new GachaponInfo.GachItem(2040718)); // Scroll for Shoes for DEX 65%
        consumes.add(new GachaponInfo.GachItem(2040719)); // Scroll for Shoes for DEX 15%
        consumes.add(new GachaponInfo.GachItem(2040720)); // Scroll for Shoes for Jump and DEX 65%
        consumes.add(new GachaponInfo.GachItem(2040721)); // Scroll for Shoes for Jump and DEX 15%
        consumes.add(new GachaponInfo.GachItem(2040722)); // Scroll for Speed for DEX 65%
        consumes.add(new GachaponInfo.GachItem(2040723)); // Scroll for Speed for DEX 15%
        consumes.add(new GachaponInfo.GachItem(2040740)); // Scroll for Shoes for DEX 100%
        consumes.add(new GachaponInfo.GachItem(2040741)); // Scroll for Shoes for Jump 100%
        consumes.add(new GachaponInfo.GachItem(2040742)); // Scroll for Shoes for Speed 100%
        consumes.add(new GachaponInfo.GachItem(2040755)); // Scroll for Shoes for DEX 50%
        consumes.add(new GachaponInfo.GachItem(2040756)); // Scroll for Shoes for Jump 50%
        consumes.add(new GachaponInfo.GachItem(2040757)); // Scroll for Shoes for Speed 50%
        consumes.add(new GachaponInfo.GachItem(2040800)); // Scroll for Gloves for DEX 100%
        consumes.add(new GachaponInfo.GachItem(2040801)); // Scroll for Gloves for DEX 60%
        consumes.add(new GachaponInfo.GachItem(2040802)); // Scroll for Gloves for DEX 10%
        consumes.add(new GachaponInfo.GachItem(2040803)); // Scroll for Gloves for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2040804)); // Scroll for Gloves for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2040805)); // Scroll for Gloves for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2040806)); // Scroll for Gloves for DEX 100%
        consumes.add(new GachaponInfo.GachItem(2040807)); // Scroll for Gloves for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2040808)); // Scroll for Gloves for DEX 70%
        consumes.add(new GachaponInfo.GachItem(2040809)); // Scroll for Gloves for DEX 30%
        consumes.add(new GachaponInfo.GachItem(2040810)); // Dark scroll for Gloves for ATT 70%
        consumes.add(new GachaponInfo.GachItem(2040811)); // Dark scroll for Gloves for ATT 30%
        consumes.add(new GachaponInfo.GachItem(2040812)); // Dark scroll for Gloves for HP 70%
        consumes.add(new GachaponInfo.GachItem(2040813)); // Dark scroll for Gloves for HP 30%
        consumes.add(new GachaponInfo.GachItem(2040814)); // Dark Scroll for Gloves for Magic Attack 70%
        consumes.add(new GachaponInfo.GachItem(2040815)); // Dark Scroll for Gloves for Magic Attack 30%
        consumes.add(new GachaponInfo.GachItem(2040816)); // Scroll for Gloves for Magic Attack 10%
        consumes.add(new GachaponInfo.GachItem(2040817)); // Scroll for Gloves for Magic Attack 60%
        consumes.add(new GachaponInfo.GachItem(2040818)); // Scroll for Gloves for Magic Attack 100%
        consumes.add(new GachaponInfo.GachItem(2040819)); // Scroll for Gloves for DEX 65%
        consumes.add(new GachaponInfo.GachItem(2040820)); // Scroll for Gloves for DEX 15%
        consumes.add(new GachaponInfo.GachItem(2040821)); // Scroll for Gloves for ATT 65%
        consumes.add(new GachaponInfo.GachItem(2040822)); // Scroll for Gloves for ATT 15%
        consumes.add(new GachaponInfo.GachItem(2040823)); // Scroll for Gloves for HP 100%
        consumes.add(new GachaponInfo.GachItem(2040824)); // Scroll for Gloves for HP 60%
        consumes.add(new GachaponInfo.GachItem(2040825)); // Scroll for Gloves for HP 10%
        consumes.add(new GachaponInfo.GachItem(2040826)); // Scroll for Gloves for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2040829)); // Scroll for Gloves for DEX 100%
        consumes.add(new GachaponInfo.GachItem(2040830)); // Scroll for Gloves for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2040833)); // Scroll for Gloves for DEX 50%
        consumes.add(new GachaponInfo.GachItem(2040834)); // Scroll for Gloves for ATT 50%
        consumes.add(new GachaponInfo.GachItem(2040900)); // Scroll for Shield for DEF 100%
        consumes.add(new GachaponInfo.GachItem(2040901)); // Scroll for Shield for DEF 60%
        consumes.add(new GachaponInfo.GachItem(2040902)); // Scroll for Shield for DEF 10%
        consumes.add(new GachaponInfo.GachItem(2040903)); // Scroll for Shield for DEF 100%
        consumes.add(new GachaponInfo.GachItem(2040904)); // Dark scroll for Shield for DEF 70%
        consumes.add(new GachaponInfo.GachItem(2040905)); // Dark scroll for Shield for DEF 30%
        consumes.add(new GachaponInfo.GachItem(2040906)); // Dark scroll for Shield for LUK 70%
        consumes.add(new GachaponInfo.GachItem(2040907)); // Dark scroll for Shield for LUK 30%
        consumes.add(new GachaponInfo.GachItem(2040908)); // Dark scroll for Shield for HP 70%
        consumes.add(new GachaponInfo.GachItem(2040909)); // Dark scroll for Shield for HP 30%
        consumes.add(new GachaponInfo.GachItem(2040910)); // Scroll for Shield for DEF 65%
        consumes.add(new GachaponInfo.GachItem(2040911)); // Scroll for Shield for DEF 15%
        consumes.add(new GachaponInfo.GachItem(2040914)); // Scroll for Shield for Weapon ATT 60%
        consumes.add(new GachaponInfo.GachItem(2040915)); // Scroll for Shield for Weapon ATT 10%
        consumes.add(new GachaponInfo.GachItem(2040916)); // Dark Scroll for Shield for Weapon ATT 70%
        consumes.add(new GachaponInfo.GachItem(2040917)); // Dark Scroll for Shield for Weapon ATT 30%
        consumes.add(new GachaponInfo.GachItem(2040918)); // Scroll for Shield for Magic Attack 100%
        consumes.add(new GachaponInfo.GachItem(2040919)); // Scroll for Shield for Magic Attack 60%
        consumes.add(new GachaponInfo.GachItem(2040920)); // Scroll for Shield for Magic Attack 10%
        consumes.add(new GachaponInfo.GachItem(2040921)); // Dark Scroll for Shield for Magic Attack 70%
        consumes.add(new GachaponInfo.GachItem(2040922)); // Dark Scroll for Shield for Magic Attack 50%
        consumes.add(new GachaponInfo.GachItem(2040923)); // Scroll for Shield for LUK 100%
        consumes.add(new GachaponInfo.GachItem(2040924)); // Scroll for Shield for LUK 60%
        consumes.add(new GachaponInfo.GachItem(2040925)); // Scroll for Shield for LUK 10%
        consumes.add(new GachaponInfo.GachItem(2040926)); // Scroll for Shield for HP 100%
        consumes.add(new GachaponInfo.GachItem(2040927)); // Scroll for Shield for HP 60%
        consumes.add(new GachaponInfo.GachItem(2040928)); // Scroll for Shield for HP 10%
        consumes.add(new GachaponInfo.GachItem(2040929)); // Scroll for Shield for STR 100%
        consumes.add(new GachaponInfo.GachItem(2040930)); // Scroll for Shield for STR 70%
        consumes.add(new GachaponInfo.GachItem(2040931)); // Scroll for Shield for STR 60%
        consumes.add(new GachaponInfo.GachItem(2040932)); // Scroll for Shield for STR 30%
        consumes.add(new GachaponInfo.GachItem(2040933)); // Scroll for Shield for STR 10%
        consumes.add(new GachaponInfo.GachItem(2040936)); // Scroll for Shield for DEF 100%
        consumes.add(new GachaponInfo.GachItem(2040943)); // Scroll for Shield for DEF 50%
        consumes.add(new GachaponInfo.GachItem(2041000)); // Scroll for Cape for Magic DEF 100%
        consumes.add(new GachaponInfo.GachItem(2041001)); // Scroll for Cape for DEF 60%
        consumes.add(new GachaponInfo.GachItem(2041002)); // Scroll for Cape for DEF 10%
        consumes.add(new GachaponInfo.GachItem(2041003)); // Scroll for Cape for DEF 100%
        consumes.add(new GachaponInfo.GachItem(2041004)); // Scroll for Cape for DEF 60%
        consumes.add(new GachaponInfo.GachItem(2041005)); // Scroll for Cape for DEF 10%
        consumes.add(new GachaponInfo.GachItem(2041006)); // Scroll for Cape for HP 100%
        consumes.add(new GachaponInfo.GachItem(2041007)); // Scroll for Cape for HP 60%
        consumes.add(new GachaponInfo.GachItem(2041008)); // Scroll for Cape for HP 10%
        consumes.add(new GachaponInfo.GachItem(2041009)); // Scroll for Cape for MP 100%
        consumes.add(new GachaponInfo.GachItem(2041010)); // Scroll for Cape for MP 60%
        consumes.add(new GachaponInfo.GachItem(2041011)); // Scroll for Cape for MP 10%
        consumes.add(new GachaponInfo.GachItem(2041012)); // Scroll for Cape for STR 100%
        consumes.add(new GachaponInfo.GachItem(2041013)); // Scroll for Cape for STR 60%
        consumes.add(new GachaponInfo.GachItem(2041014)); // Scroll for Cape for STR 10%
        consumes.add(new GachaponInfo.GachItem(2041015)); // Scroll for Cape for INT 100%
        consumes.add(new GachaponInfo.GachItem(2041016)); // Scroll for Cape for INT 60%
        consumes.add(new GachaponInfo.GachItem(2041017)); // Scroll for Cape for INT 10%
        consumes.add(new GachaponInfo.GachItem(2041018)); // Scroll for Cape for DEX 100%
        consumes.add(new GachaponInfo.GachItem(2041019)); // Scroll for Cape for DEX 60%
        consumes.add(new GachaponInfo.GachItem(2041020)); // Scroll for Cape for DEX 10%
        consumes.add(new GachaponInfo.GachItem(2041021)); // Scroll for Cape for LUK 100%
        consumes.add(new GachaponInfo.GachItem(2041022)); // Scroll for Cape for LUK 60%
        consumes.add(new GachaponInfo.GachItem(2041023)); // Scroll for Cape for LUK 10%
        consumes.add(new GachaponInfo.GachItem(2041024)); // Scroll for Cape for DEF 100%
        consumes.add(new GachaponInfo.GachItem(2041025)); // Scroll for Cape for DEF 100%
        consumes.add(new GachaponInfo.GachItem(2041026)); // Scroll for Cape for DEF 70%
        consumes.add(new GachaponInfo.GachItem(2041027)); // Scroll for Cape for DEF 30%
        consumes.add(new GachaponInfo.GachItem(2041028)); // Scroll for Cape for DEF 70%
        consumes.add(new GachaponInfo.GachItem(2041029)); // Scroll for Cape for DEF 30%
        consumes.add(new GachaponInfo.GachItem(2041030)); // Dark scroll for Cape for HP 70%
        consumes.add(new GachaponInfo.GachItem(2041031)); // Dark scroll for Cape for HP 30%
        consumes.add(new GachaponInfo.GachItem(2041032)); // Dark scroll for Cape for MP 70%
        consumes.add(new GachaponInfo.GachItem(2041033)); // Dark scroll for Cape for MP 30%
        consumes.add(new GachaponInfo.GachItem(2041034)); // Dark scroll for Cape for STR 70%
        consumes.add(new GachaponInfo.GachItem(2041035)); // Dark scroll for Cape for STR 30%
        consumes.add(new GachaponInfo.GachItem(2041036)); // Dark scroll for Cape for INT 70%
        consumes.add(new GachaponInfo.GachItem(2041037)); // Dark scroll for Cape for INT 30%
        consumes.add(new GachaponInfo.GachItem(2041038)); // Scroll for Cape for DEX 70%
        consumes.add(new GachaponInfo.GachItem(2041039)); // Scroll for Cape for DEX 30%
        consumes.add(new GachaponInfo.GachItem(2041040)); // Dark scroll for Cape for LUK 70%
        consumes.add(new GachaponInfo.GachItem(2041041)); // Dark scroll for Cape for LUK 30%
        consumes.add(new GachaponInfo.GachItem(2041042)); // Scroll for Cape for DEF 65%
        consumes.add(new GachaponInfo.GachItem(2041043)); // Scroll for Cape for DEF 15%
        consumes.add(new GachaponInfo.GachItem(2041044)); // Scroll for Cape for DEF 65%
        consumes.add(new GachaponInfo.GachItem(2041045)); // Scroll for Cape for DEF 15%
        consumes.add(new GachaponInfo.GachItem(2041046)); // Scroll for Cape for MaxHP 65%
        consumes.add(new GachaponInfo.GachItem(2041047)); // Scroll for Cape for MaxHP 15%
        consumes.add(new GachaponInfo.GachItem(2041048)); // Scroll for Cape for MP 65%
        consumes.add(new GachaponInfo.GachItem(2041049)); // Scroll for Cape for MP 15%
        consumes.add(new GachaponInfo.GachItem(2041050)); // Scroll for Cape for STR 65%
        consumes.add(new GachaponInfo.GachItem(2041051)); // Scroll for Cape for STR 15%
        consumes.add(new GachaponInfo.GachItem(2041052)); // Scroll for Cape for INT 65%
        consumes.add(new GachaponInfo.GachItem(2041053)); // Scroll for Cape for INT 15%
        consumes.add(new GachaponInfo.GachItem(2041054)); // Scroll for Cape for DEX 65%
        consumes.add(new GachaponInfo.GachItem(2041055)); // Scroll for Cape for DEX 15%
        consumes.add(new GachaponInfo.GachItem(2041056)); // Scroll for Cape for LUK 65%
        consumes.add(new GachaponInfo.GachItem(2041057)); // Scroll for Cape for LUK 15%
        consumes.add(new GachaponInfo.GachItem(2041058)); // Scroll for Cape for Cold Protection 10%
        consumes.add(new GachaponInfo.GachItem(2041074)); // Crimson Maple Cape Scroll for ATT 30%
        consumes.add(new GachaponInfo.GachItem(2041075)); // Crimson Maple Cape Scroll for M. ATT 30%
        consumes.add(new GachaponInfo.GachItem(2041076)); // Crimson Maple Cape Scroll for STR 70%
        consumes.add(new GachaponInfo.GachItem(2041077)); // Crimson Maple Cape Scroll for INT 70%
        consumes.add(new GachaponInfo.GachItem(2041078)); // Crimson Maple Cape Scroll for DEX 70%
        consumes.add(new GachaponInfo.GachItem(2041079)); // Crimson Maple Cape Scroll for LUK 70%
        consumes.add(new GachaponInfo.GachItem(2041080)); // Scroll for Christmas Snow Fur Lump for ATT 30%
        consumes.add(new GachaponInfo.GachItem(2041081)); // Scroll for Christmas Snow Fur Lump for M. ATT 30%
        consumes.add(new GachaponInfo.GachItem(2041082)); // Scroll for Christmas Snow Fur Lump for STR 70%
        consumes.add(new GachaponInfo.GachItem(2041083)); // Scroll for Christmas Snow Fur Lump for INT 70%
        consumes.add(new GachaponInfo.GachItem(2041084)); // Scroll for Christmas Snow Fur Lump for DEX 70%
        consumes.add(new GachaponInfo.GachItem(2041085)); // Scroll for Christmas Snow Fur Lump for LUK 70%
        consumes.add(new GachaponInfo.GachItem(2041086)); // Scroll for Whiteday Heart Balloon for ATT 30%
        consumes.add(new GachaponInfo.GachItem(2041087)); // Scroll for Whiteday Heart Balloon for M. ATT 30%
        consumes.add(new GachaponInfo.GachItem(2041088)); // Scroll for Whiteday Heart Balloon for STR 70%
        consumes.add(new GachaponInfo.GachItem(2041089)); // Scroll for Whiteday Heart Balloon for INT 70%
        consumes.add(new GachaponInfo.GachItem(2041090)); // Scroll for Whiteday Heart Balloon for DEX 70%
        consumes.add(new GachaponInfo.GachItem(2041091)); // Scroll for Whiteday Heart Balloon for LUK 70%
        consumes.add(new GachaponInfo.GachItem(2041066)); // Scroll for Cape for Magic DEF 100%
        consumes.add(new GachaponInfo.GachItem(2041067)); // Scroll for Cape for Weapon DEF 100%
        consumes.add(new GachaponInfo.GachItem(2041068)); // Scroll for Cape for Magic DEF 50%
        consumes.add(new GachaponInfo.GachItem(2041069)); // Scroll for Cape for Weapon DEF 50%
        consumes.add(new GachaponInfo.GachItem(2041100)); // Scroll for Ring for STR 100%
        consumes.add(new GachaponInfo.GachItem(2041101)); // Scroll for Rings for STR 60%
        consumes.add(new GachaponInfo.GachItem(2041102)); // Scroll for Rings for STR 10%
        consumes.add(new GachaponInfo.GachItem(2041103)); // Scroll for Rings for INT 100%
        consumes.add(new GachaponInfo.GachItem(2041104)); // Scroll for Rings for INT 60%
        consumes.add(new GachaponInfo.GachItem(2041105)); // Scroll for Rings for INT 10%
        consumes.add(new GachaponInfo.GachItem(2041106)); // Scroll for Rings for DEX 100%
        consumes.add(new GachaponInfo.GachItem(2041107)); // Scroll for Rings for DEX 60%
        consumes.add(new GachaponInfo.GachItem(2041108)); // Scroll for Rings for DEX 10%
        consumes.add(new GachaponInfo.GachItem(2041109)); // Scroll for Rings for LUK 100%
        consumes.add(new GachaponInfo.GachItem(2041110)); // Scroll for Rings for LUK 60%
        consumes.add(new GachaponInfo.GachItem(2041111)); // Scroll for Rings for LUK 10%
        consumes.add(new GachaponInfo.GachItem(2041112)); // Dark Scroll for Rings for STR 70%
        consumes.add(new GachaponInfo.GachItem(2041113)); // Dark Scroll for Rings for STR 30%
        consumes.add(new GachaponInfo.GachItem(2041114)); // Dark Scroll for Rings for INT 70%
        consumes.add(new GachaponInfo.GachItem(2041115)); // Dark Scroll for Rings for INT 30%
        consumes.add(new GachaponInfo.GachItem(2041116)); // Scroll for Rings for DEX 70%
        consumes.add(new GachaponInfo.GachItem(2041117)); // Scroll for Rings for DEX 30%
        consumes.add(new GachaponInfo.GachItem(2041118)); // Dark Scroll for Rings for LUK 70%
        consumes.add(new GachaponInfo.GachItem(2041119)); // Dark Scroll for Rings for LUK 30%
        consumes.add(new GachaponInfo.GachItem(2041220)); // Scroll for Golden Song Pyun Necklace for ATT 20%
        consumes.add(new GachaponInfo.GachItem(2041221)); // Scroll for Golden Song Pyun Necklace for M. ATT 20%
        consumes.add(new GachaponInfo.GachItem(2041222)); // Scroll for Golden Song Pyun Necklace for STR 25%
        consumes.add(new GachaponInfo.GachItem(2041223)); // Scroll for Golden Song Pyun Necklace for INT 25%
        consumes.add(new GachaponInfo.GachItem(2041224)); // Scroll for Golden Song Pyun Necklace for DEX 25%
        consumes.add(new GachaponInfo.GachItem(2041225)); // Scroll for Golden Song Pyun Necklace for LUK 25%
        consumes.add(new GachaponInfo.GachItem(2041300)); // Scroll for Belts for STR 100%
        consumes.add(new GachaponInfo.GachItem(2041301)); // Scroll for Belts for STR 60%
        consumes.add(new GachaponInfo.GachItem(2041302)); // Scroll for Belts for STR 10%
        consumes.add(new GachaponInfo.GachItem(2041303)); // Scroll for Belts for INT 100%
        consumes.add(new GachaponInfo.GachItem(2041304)); // Scroll for Belts for INT 60%
        consumes.add(new GachaponInfo.GachItem(2041305)); // Scroll for Belts for INT 10%
        consumes.add(new GachaponInfo.GachItem(2041306)); // Scroll for Belts for DEX 100%
        consumes.add(new GachaponInfo.GachItem(2041307)); // Scroll for Belts for DEX 60%
        consumes.add(new GachaponInfo.GachItem(2041308)); // Scroll for Belts for DEX 10%
        consumes.add(new GachaponInfo.GachItem(2041309)); // Scroll for Belts for LUK 100%
        consumes.add(new GachaponInfo.GachItem(2041310)); // Scroll for Belts for LUK 60%
        consumes.add(new GachaponInfo.GachItem(2041311)); // Scroll for Belts for LUK 10%
        consumes.add(new GachaponInfo.GachItem(2041312)); // Dark Scroll for Belts for STR 70%
        consumes.add(new GachaponInfo.GachItem(2041313)); // Dark Scroll for Belts for STR 30%
        consumes.add(new GachaponInfo.GachItem(2041314)); // Dark Scroll for Belts for INT 70%
        consumes.add(new GachaponInfo.GachItem(2041315)); // Dark Scroll for Belts for INT 30%
        consumes.add(new GachaponInfo.GachItem(2041316)); // Scroll for Belts for DEX 70%
        consumes.add(new GachaponInfo.GachItem(2041317)); // Scroll for Belts for DEX 30%
        consumes.add(new GachaponInfo.GachItem(2041318)); // Dark Scroll for Belts for LUK 70%
        consumes.add(new GachaponInfo.GachItem(2041319)); // Dark Scroll for Belts for LUK 30%
        consumes.add(new GachaponInfo.GachItem(2042100)); // Scroll for Shining Rod for M. ATT 100%
        consumes.add(new GachaponInfo.GachItem(2042101)); // Scroll for Shining Rod for M. ATT 60%
        consumes.add(new GachaponInfo.GachItem(2042102)); // Scroll for Shining Rod for M. ATT 10%
        consumes.add(new GachaponInfo.GachItem(2042103)); // Scroll for Shining Rod for M. ATT 70%
        consumes.add(new GachaponInfo.GachItem(2042104)); // Scroll for Shining Rod for M. ATT 30%
        consumes.add(new GachaponInfo.GachItem(2042105)); // Scroll for Shining Rod for M. ATT 65%
        consumes.add(new GachaponInfo.GachItem(2042106)); // Scroll for Shining Rod for M. ATT 15%
        consumes.add(new GachaponInfo.GachItem(2042200)); // Scroll for Soul Shooter for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2042201)); // Scroll for Soul Shooter for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2042202)); // Scroll for Soul Shooter for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2042203)); // Scroll for Soul Shooter for ATT 70%
        consumes.add(new GachaponInfo.GachItem(2042204)); // Scroll for Soul Shooter for ATT 30%
        consumes.add(new GachaponInfo.GachItem(2042205)); // Scroll for Soul Shooter for ATT 65%
        consumes.add(new GachaponInfo.GachItem(2042206)); // Scroll for Soul Shooter for ATT 15%
        consumes.add(new GachaponInfo.GachItem(2042300)); // Scroll for Desperado for Weapon ATT 100%
        consumes.add(new GachaponInfo.GachItem(2042301)); // Scroll for Desperado for Weapon ATT 60%
        consumes.add(new GachaponInfo.GachItem(2042302)); // Scroll for Desperado for Weapon ATT 10%
        consumes.add(new GachaponInfo.GachItem(2042303)); // Scroll for Desperado for Weapon ATT 70%
        consumes.add(new GachaponInfo.GachItem(2042304)); // Scroll for Desperado for Weapon ATT 30%
        consumes.add(new GachaponInfo.GachItem(2042305)); // Scroll for Desperado for Weapon ATT 65%
        consumes.add(new GachaponInfo.GachItem(2042306)); // Scroll for Desperado for Weapon ATT 15%
        consumes.add(new GachaponInfo.GachItem(2042400)); // Scroll for Whip Blade for Weapon ATT 100%
        consumes.add(new GachaponInfo.GachItem(2042401)); // Scroll for Whip Blade for Weapon ATT 60%
        consumes.add(new GachaponInfo.GachItem(2042402)); // Scroll for Whip Blade for Weapon ATT 10%
        consumes.add(new GachaponInfo.GachItem(2042403)); // Scroll for Whip Blade for Weapon ATT 70%
        consumes.add(new GachaponInfo.GachItem(2042404)); // Scroll for Whip Blade for Weapon ATT 30%
        consumes.add(new GachaponInfo.GachItem(2042405)); // Scroll for Whip Blade for Weapon ATT 65%
        consumes.add(new GachaponInfo.GachItem(2042406)); // Scroll for Whip Blade for Weapon ATT 15%
        consumes.add(new GachaponInfo.GachItem(2043000)); // Scroll for One-Handed Sword for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2043001)); // Scroll for One-Handed Sword for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2043002)); // Scroll for One-Handed Sword for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2043003)); // Scroll for One-Handed Sword for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2043004)); // Dark scroll for One-Handed Sword for ATT 70%
        consumes.add(new GachaponInfo.GachItem(2043005)); // Dark scroll for One-Handed Sword for ATT 30%
        consumes.add(new GachaponInfo.GachItem(2043006)); // Dark Scroll for One-Handed Sword for Magic Attack 70%
        consumes.add(new GachaponInfo.GachItem(2043007)); // Dark Scroll for One-Handed Sword for Magic Attack 30%
        consumes.add(new GachaponInfo.GachItem(2043008)); // Scroll for One-Handed Sword for Magic Attack 10%
        consumes.add(new GachaponInfo.GachItem(2043009)); // Scroll for One-Handed Sword for Magic Attack 60%
        consumes.add(new GachaponInfo.GachItem(2043010)); // Scroll for One-Handed Sword for Magic Attack 100%
        consumes.add(new GachaponInfo.GachItem(2043011)); // Scroll for One-Handed Sword for ATT 65%
        consumes.add(new GachaponInfo.GachItem(2043012)); // Scroll for One-Handed Sword for ATT 15%
        consumes.add(new GachaponInfo.GachItem(2043015)); // Scroll for One-Handed Sword for Accuracy 100%
        consumes.add(new GachaponInfo.GachItem(2043016)); // Scroll for One-Handed Sword for Accuracy 70%
        consumes.add(new GachaponInfo.GachItem(2043017)); // Scroll for One-Handed Sword for Accuracy 60%
        consumes.add(new GachaponInfo.GachItem(2043018)); // Scroll for One-Handed Sword for Accuracy 30%
        consumes.add(new GachaponInfo.GachItem(2043019)); // Scroll for One-Handed Sword for Accuracy 10%
        consumes.add(new GachaponInfo.GachItem(2043022)); // Scroll for One-Handed Sword for ATT 50%
        consumes.add(new GachaponInfo.GachItem(2043023)); // Scroll for One-Handed Sword for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2043100)); // Scroll for One-Handed Axe for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2043101)); // Scroll for One-Handed Axe for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2043102)); // Scroll for One-Handed Axe for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2043103)); // Scroll for One-Handed Axe for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2043104)); // Dark scroll for One-Handed Axe for ATT 70%
        consumes.add(new GachaponInfo.GachItem(2043105)); // Dark scroll for One-Handed Axe for ATT 30%
        consumes.add(new GachaponInfo.GachItem(2043106)); // Scroll for One-Handed Axe for ATT 65%
        consumes.add(new GachaponInfo.GachItem(2043107)); // Scroll for One-Handed Axe for ATT 15%
        consumes.add(new GachaponInfo.GachItem(2043110)); // Scroll for One-Handed Axe for Accuracy 100%
        consumes.add(new GachaponInfo.GachItem(2043111)); // Scroll for One-Handed Axe for Accuracy 70%
        consumes.add(new GachaponInfo.GachItem(2043112)); // Scroll for One-Handed Axe for Accuracy 60%
        consumes.add(new GachaponInfo.GachItem(2043113)); // Scroll for One-Handed Axe for Accuracy 30%
        consumes.add(new GachaponInfo.GachItem(2043114)); // Scroll for One-Handed Axe for Accuracy 10%
        consumes.add(new GachaponInfo.GachItem(2043117)); // Scroll for One-Handed Axe for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2043120)); // Scroll for One-Handed Axe for ATT 50%
        consumes.add(new GachaponInfo.GachItem(2043200)); // Scroll for One-Handed BW for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2043201)); // Scroll for One-Handed BW for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2043202)); // Scroll for One-Handed BW for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2043203)); // Scroll for One-Handed BW for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2043204)); // Dark scroll for One-Handed BW for ATT
        consumes.add(new GachaponInfo.GachItem(2043205)); // Dark scroll for One-Handed BW for ATT
        consumes.add(new GachaponInfo.GachItem(2043206)); // Scroll for One-Handed BW for ATT 65%
        consumes.add(new GachaponInfo.GachItem(2043207)); // Scroll for One-Handed BW for ATT 15%
        consumes.add(new GachaponInfo.GachItem(2043210)); // Scroll for One-Handed BW for Accuracy 100%
        consumes.add(new GachaponInfo.GachItem(2043211)); // Scroll for One-Handed BW for Accuracy 70%
        consumes.add(new GachaponInfo.GachItem(2043212)); // Scroll for One-Handed BW for Accuracy 60%
        consumes.add(new GachaponInfo.GachItem(2043213)); // Scroll for One-Handed BW for Accuracy 30%
        consumes.add(new GachaponInfo.GachItem(2043214)); // Scroll for One-Handed BW for Accuracy 10%
        consumes.add(new GachaponInfo.GachItem(2043217)); // Scroll for One-Handed BW for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2043220)); // Scroll for One-Handed BW for ATT 50%
        consumes.add(new GachaponInfo.GachItem(2043300)); // Scroll for Dagger for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2043301)); // Scroll for Dagger for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2043302)); // Scroll for Dagger for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2043303)); // Scroll for Dagger for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2043304)); // Dark scroll for Dagger for ATT 70%
        consumes.add(new GachaponInfo.GachItem(2043305)); // Dark scroll for Dagger for ATT 30%
        consumes.add(new GachaponInfo.GachItem(2043306)); // Scroll for Dagger for ATT 65%
        consumes.add(new GachaponInfo.GachItem(2043307)); // Scroll for Dagger for ATT 15%
        consumes.add(new GachaponInfo.GachItem(2043316)); // Scroll for Dagger for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2043312)); // Scroll for Dagger for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2043313)); // Scroll for Dagger for ATT 50%
        consumes.add(new GachaponInfo.GachItem(2043700)); // Scroll for Wand for Magic Attack 100%
        consumes.add(new GachaponInfo.GachItem(2043701)); // Scroll for Wand for Magic Attack 60%
        consumes.add(new GachaponInfo.GachItem(2043702)); // Scroll for Wand for Magic Attack 10%
        consumes.add(new GachaponInfo.GachItem(2043703)); // Scroll for Wand for Magic Attack 100%
        consumes.add(new GachaponInfo.GachItem(2043704)); // Dark scroll for Wand for Magic Attack
        consumes.add(new GachaponInfo.GachItem(2043712)); // Scroll for Wand for Magic Attack 100%
        consumes.add(new GachaponInfo.GachItem(2043713)); // Scroll for Wand for Magic Attack 50%
        consumes.add(new GachaponInfo.GachItem(2043812)); // Scroll for Staff for Magic Attack 100%
        consumes.add(new GachaponInfo.GachItem(2043813)); // Scroll for Staff for Magic Attack 50%
        consumes.add(new GachaponInfo.GachItem(2044025)); // Scroll for Two-handed Sword for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044028)); // Scroll for Two-handed Sword for ATT 50%
        consumes.add(new GachaponInfo.GachItem(2044117)); // Scroll for Two-handed Axe for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044120)); // Scroll for Two-handed Axe for ATT 50%
        consumes.add(new GachaponInfo.GachItem(2044217)); // Scroll for Two-handed BW for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044220)); // Scroll for Two-handed BW for ATT 50%
        consumes.add(new GachaponInfo.GachItem(2044317)); // Scroll for Spear for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044320)); // Scroll for Spear for ATT 50%
        consumes.add(new GachaponInfo.GachItem(2044417)); // Scroll for Pole Arm for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044420)); // Scroll for Pole Arm for ATT 50%
        consumes.add(new GachaponInfo.GachItem(2044512)); // Scroll for Bow for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044513)); // Scroll for Bow for ATT 50%
        consumes.add(new GachaponInfo.GachItem(2044612)); // Scroll for Crossbow for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044613)); // Scroll for Crossbow for ATT 50%
        consumes.add(new GachaponInfo.GachItem(2044712)); // Scroll for Claw for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044713)); // Scroll for Claw for ATT 50%
        consumes.add(new GachaponInfo.GachItem(2044908)); // Scroll for Gun for Attack 100%
        consumes.add(new GachaponInfo.GachItem(2044815)); // Scroll for Knuckler for Attack 100%
        consumes.add(new GachaponInfo.GachItem(2044817)); // Scroll for Knuckler for Attack 50%
        consumes.add(new GachaponInfo.GachItem(2044910)); // Scroll for Gun for Attack 50%
        consumes.add(new GachaponInfo.GachItem(2040758)); // Scroll for Shoes for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2040759)); // Scroll for Shoes for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2040760)); // Scroll for Shoes for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2040043)); // Scroll for Helmet for DEX 65%
        consumes.add(new GachaponInfo.GachItem(2040044)); // Scroll for Helmet for DEX 15%
        consumes.add(new GachaponInfo.GachItem(2040335)); // Scroll for Earring for DEX 65%
        consumes.add(new GachaponInfo.GachItem(2040336)); // Scroll for Earring for DEX 15%
        consumes.add(new GachaponInfo.GachItem(2040337)); // Scroll for Earring for LUK 65%
        consumes.add(new GachaponInfo.GachItem(2040338)); // Scroll for Earring for LUK 15%
        consumes.add(new GachaponInfo.GachItem(2040339)); // Scroll for Earring for HP 65%
        consumes.add(new GachaponInfo.GachItem(2040340)); // Scroll for Earring for HP 15%
        consumes.add(new GachaponInfo.GachItem(2040431)); // Scroll for Topwear for STR 65%
        consumes.add(new GachaponInfo.GachItem(2040432)); // Scroll for Topwear for STR 15%
        consumes.add(new GachaponInfo.GachItem(2040433)); // Scroll for Topwear for HP 65%
        consumes.add(new GachaponInfo.GachItem(2040434)); // Scroll for Topwear for HP 15%
        consumes.add(new GachaponInfo.GachItem(2040435)); // Scroll for Topwear for LUK 65%
        consumes.add(new GachaponInfo.GachItem(2040436)); // Scroll for Topwear for LUK 15%
        consumes.add(new GachaponInfo.GachItem(2040540)); // Scroll for Overall Armor for STR 65%
        consumes.add(new GachaponInfo.GachItem(2040541)); // Scroll for Overall Armor for STR 15%
        consumes.add(new GachaponInfo.GachItem(2040631)); // Scroll for Bottomwear for Jump 65%
        consumes.add(new GachaponInfo.GachItem(2040632)); // Scroll for Bottomwear for Jump 15%
        consumes.add(new GachaponInfo.GachItem(2040633)); // Scroll for Bottomwear for HP 65%
        consumes.add(new GachaponInfo.GachItem(2040634)); // Scroll for Bottomwear for HP 15%
        consumes.add(new GachaponInfo.GachItem(2040635)); // Scroll for Bottomwear for DEX 65%
        consumes.add(new GachaponInfo.GachItem(2040636)); // Scroll for Bottomwear for DEX 15%
        consumes.add(new GachaponInfo.GachItem(2040831)); // Scroll for Gloves for HP 65%
        consumes.add(new GachaponInfo.GachItem(2040832)); // Scroll for Gloves for HP 15%
        consumes.add(new GachaponInfo.GachItem(2040845)); // Scroll for Gloves for Magic ATT 60%
        consumes.add(new GachaponInfo.GachItem(2040937)); // Scroll for Shield for LUK 65%
        consumes.add(new GachaponInfo.GachItem(2040938)); // Scroll for Shield for LUK 15%
        consumes.add(new GachaponInfo.GachItem(2040939)); // Scroll for Shield for HP 65%
        consumes.add(new GachaponInfo.GachItem(2040940)); // Scroll for Shield for HP 15%
        consumes.add(new GachaponInfo.GachItem(2040941)); // Scroll for Shield for STR 65%
        consumes.add(new GachaponInfo.GachItem(2040942)); // Scroll for Shield for STR 15%
        consumes.add(new GachaponInfo.GachItem(2043024)); // Scroll for One-Handed Sword for Accuracy 65%
        consumes.add(new GachaponInfo.GachItem(2043025)); // Scroll for One-Handed Sword for Accuracy 15%
        consumes.add(new GachaponInfo.GachItem(2043118)); // Scroll for One-Handed Axe for Accuracy 65%
        consumes.add(new GachaponInfo.GachItem(2043119)); // Scroll for One-Handed Axe for Accuracy 15%
        consumes.add(new GachaponInfo.GachItem(2043218)); // Scroll for One-Handed BW for Accuracy 65%
        consumes.add(new GachaponInfo.GachItem(2043219)); // Scroll for One-Handed BW for Accuracy 15%
        consumes.add(new GachaponInfo.GachItem(2040211)); // Dragon Glasses Scroll 100%
        consumes.add(new GachaponInfo.GachItem(2040212)); // Dragon Glasses Special Scroll 100%
        consumes.add(new GachaponInfo.GachItem(2040233)); // Scroll for Eye Accessory STR 60%
        consumes.add(new GachaponInfo.GachItem(2040234)); // Scroll for Eye Accessory DEX 60%
        consumes.add(new GachaponInfo.GachItem(2040235)); // Scroll for Eye Accessory INT 60%
        consumes.add(new GachaponInfo.GachItem(2040236)); // Scroll for Eye Accessory LUK 60%
        consumes.add(new GachaponInfo.GachItem(2043705)); // Dark scroll for Wand for Magic Attack 30%
        consumes.add(new GachaponInfo.GachItem(2043706)); // Scroll for Wand for Magic Attack 65%
        consumes.add(new GachaponInfo.GachItem(2043707)); // Scroll for Wand for Magic Attack 15%
        consumes.add(new GachaponInfo.GachItem(2043714)); // Scroll for Wand for Magic Attack 60%
        consumes.add(new GachaponInfo.GachItem(2043800)); // Scroll for Staff for Magic Attack 100%
        consumes.add(new GachaponInfo.GachItem(2043801)); // Scroll for Staff for Magic Attack 60%
        consumes.add(new GachaponInfo.GachItem(2043802)); // Scroll for Staff for Magic Attack 10%
        consumes.add(new GachaponInfo.GachItem(2043803)); // Scroll for Staff for Magic Attack
        consumes.add(new GachaponInfo.GachItem(2043804)); // Dark scroll for Staff for Magic Attack 70%
        consumes.add(new GachaponInfo.GachItem(2043805)); // Dark scroll for Staff for Magic Attack 30%
        consumes.add(new GachaponInfo.GachItem(2043806)); // Scroll for Staff for Magic Attack 65%
        consumes.add(new GachaponInfo.GachItem(2043807)); // Scroll for Staff for Magic Attack 15%
        consumes.add(new GachaponInfo.GachItem(2043814)); // Scroll for Staff for Magic Attack 60%
        consumes.add(new GachaponInfo.GachItem(2044000)); // Scroll for Two-handed Sword for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044001)); // Scroll for Two-handed Sword for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2044002)); // Scroll for Two-handed Sword for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2044003)); // Scroll for Two-handed Sword for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044004)); // Dark scroll for Two-handed Sword for ATT 70%
        consumes.add(new GachaponInfo.GachItem(2044005)); // Dark scroll for Two-handed Sword for ATT 30%
        consumes.add(new GachaponInfo.GachItem(2044006)); // Scroll for Two-Handed Sword for ATT 65%
        consumes.add(new GachaponInfo.GachItem(2044007)); // Scroll for Two-Handed Sword for ATT 15%
        consumes.add(new GachaponInfo.GachItem(2044010)); // Scroll for Two-Handed Sword for Accuracy 100%
        consumes.add(new GachaponInfo.GachItem(2044011)); // Scroll for Two-Handed Sword for Accuracy 70%
        consumes.add(new GachaponInfo.GachItem(2044012)); // Scroll for Two-Handed Sword for Accuracy 60%
        consumes.add(new GachaponInfo.GachItem(2044013)); // Scroll for Two-Handed Sword for Accuracy 30%
        consumes.add(new GachaponInfo.GachItem(2044014)); // Scroll for Two-Handed Sword for Accuracy 10%
        consumes.add(new GachaponInfo.GachItem(2044015)); // Scroll for Two-Handed Swords for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2044026)); // Scroll for Two-Handed Sword for Accuracy 65%
        consumes.add(new GachaponInfo.GachItem(2044027)); // Scroll for Two-Handed Sword for Accuracy 15%
        consumes.add(new GachaponInfo.GachItem(2044100)); // Scroll for Two-handed Axe for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044101)); // Scroll for Two-handed Axe for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2044102)); // Scroll for Two-handed Axe for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2044103)); // Scroll for Two-handed Axe for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044104)); // Dark scroll for Two-handed Axe for ATT 70%
        consumes.add(new GachaponInfo.GachItem(2044105)); // Dark scroll for Two-handed Axe for ATT
        consumes.add(new GachaponInfo.GachItem(2044106)); // Scroll for Two-Handed Axe for ATT 65%
        consumes.add(new GachaponInfo.GachItem(2044107)); // Scroll for Two-Handed Axe for ATT 15%
        consumes.add(new GachaponInfo.GachItem(2044110)); // Scroll for Two-Handed Axe for Accuracy 100%
        consumes.add(new GachaponInfo.GachItem(2044111)); // Scroll for Two-Handed Axe for Accuracy 70%
        consumes.add(new GachaponInfo.GachItem(2044112)); // Scroll for Two-Handed Axe for Accuracy 60%
        consumes.add(new GachaponInfo.GachItem(2044113)); // Scroll for Two-Handed Axe for Accuracy 30%
        consumes.add(new GachaponInfo.GachItem(2044114)); // Scroll for Two-Handed Axe for Accuracy 10%
        consumes.add(new GachaponInfo.GachItem(2044118)); // Scroll for Two-Handed Axe for Accuracy 65%
        consumes.add(new GachaponInfo.GachItem(2044119)); // Scroll for Two-Handed Axe for Accuracy 15%
        consumes.add(new GachaponInfo.GachItem(2044200)); // Scroll for Two-handed BW for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044201)); // Scroll for Two-handed BW for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2044202)); // Scroll for Two-handed BW for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2044203)); // Scroll for Two-handed BW for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044206)); // Scroll for Two-Handed BW for ATT 65%
        consumes.add(new GachaponInfo.GachItem(2044207)); // Scroll for Two-Handed BW for ATT 15%
        consumes.add(new GachaponInfo.GachItem(2044210)); // Scroll for Two-Handed BW for Accuracy 100%
        consumes.add(new GachaponInfo.GachItem(2044211)); // Scroll for Two-Handed BW for Accuracy 70%
        consumes.add(new GachaponInfo.GachItem(2044212)); // Scroll for Two-Handed BW for Accuracy 60%
        consumes.add(new GachaponInfo.GachItem(2044213)); // Scroll for Two-Handed BW for Accuracy 30%
        consumes.add(new GachaponInfo.GachItem(2044214)); // Scroll for Two-Handed BW for Accuracy 10%
        consumes.add(new GachaponInfo.GachItem(2044218)); // Scroll for Two-Handed BW for Accuracy 65%
        consumes.add(new GachaponInfo.GachItem(2044219)); // Scroll for Two-Handed BW for Accuracy 15%
        consumes.add(new GachaponInfo.GachItem(2044300)); // Scroll for Spear for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044301)); // Scroll for Spear for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2044302)); // Scroll for Spear for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2044303)); // Scroll for Spear for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044306)); // Scroll for Spear for ATT 65%
        consumes.add(new GachaponInfo.GachItem(2044307)); // Scroll for Spear for ATT 15%
        consumes.add(new GachaponInfo.GachItem(2044310)); // Scroll for Spear for Accuracy 100%
        consumes.add(new GachaponInfo.GachItem(2044311)); // Scroll for Spear for Accuracy 70%
        consumes.add(new GachaponInfo.GachItem(2044312)); // Scroll for Spear for Accuracy 60%
        consumes.add(new GachaponInfo.GachItem(2044313)); // Scroll for Spear for Accuracy 30%
        consumes.add(new GachaponInfo.GachItem(2044314)); // Scroll for Spear for Accuracy 10%
        consumes.add(new GachaponInfo.GachItem(2044318)); // Scroll for Spears for Accuracy 65%
        consumes.add(new GachaponInfo.GachItem(2044319)); // Scroll for Spears for Accuracy 15%
        consumes.add(new GachaponInfo.GachItem(2044400)); // Scroll for Pole Arm for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044401)); // Scroll for Pole Arm for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2044402)); // Scroll for Pole Arm for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2044403)); // Scroll for Pole Arm for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044404)); // Dark scroll for Pole Arm for ATT 70%
        consumes.add(new GachaponInfo.GachItem(2044405)); // Dark scroll for Pole Arm for ATT 30%
        consumes.add(new GachaponInfo.GachItem(2044406)); // Scroll for Pole Arm for ATT 65%
        consumes.add(new GachaponInfo.GachItem(2044407)); // Scroll for Pole Arm for ATT 15%
        consumes.add(new GachaponInfo.GachItem(2044410)); // Scroll for Pole-Arm for Accuracy 100%
        consumes.add(new GachaponInfo.GachItem(2044411)); // Scroll for Pole-Arm for Accuracy 70%
        consumes.add(new GachaponInfo.GachItem(2044412)); // Scroll for Pole-Arm for Accuracy 60%
        consumes.add(new GachaponInfo.GachItem(2044413)); // Scroll for Pole-Arm for Accuracy 30%
        consumes.add(new GachaponInfo.GachItem(2044414)); // Scroll for Pole-Arm for Accuracy 10%
        consumes.add(new GachaponInfo.GachItem(2044418)); // Scroll for Polearm for Accuracy 65%
        consumes.add(new GachaponInfo.GachItem(2044419)); // Scroll for Polearm for Accuracy 15%
        consumes.add(new GachaponInfo.GachItem(2044500)); // Scroll for Bow for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044501)); // Scroll for Bow for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2044502)); // Scroll for Bow for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2044503)); // Scroll for Bow for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044504)); // Dark scroll for Bow for ATT 70%
        consumes.add(new GachaponInfo.GachItem(2044505)); // Dark scroll for Bow for ATT 30%
        consumes.add(new GachaponInfo.GachItem(2044506)); // Scroll for Bow for ATT 65%
        consumes.add(new GachaponInfo.GachItem(2044507)); // Scroll for Bow for ATT 15%
        consumes.add(new GachaponInfo.GachItem(2044600)); // Scroll for Crossbow for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044601)); // Scroll for Crossbow for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2044602)); // Scroll for Crossbow for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2044603)); // Scroll for Crossbow for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044604)); // Dark scroll for Crossbow for ATT 70%
        consumes.add(new GachaponInfo.GachItem(2044605)); // Dark scroll for Crossbow for ATT 30%
        consumes.add(new GachaponInfo.GachItem(2044606)); // Scroll for Crossbow for ATT 65%
        consumes.add(new GachaponInfo.GachItem(2044607)); // Scroll for Crossbow for ATT 15%
        consumes.add(new GachaponInfo.GachItem(2044700)); // Scroll for Claw for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044701)); // Scroll for Claw for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2044702)); // Scroll for Claw for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2044703)); // Scroll for Claw for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2044704)); // Dark Scroll for Claw for ATT 70%
        consumes.add(new GachaponInfo.GachItem(2044705)); // Dark scroll for Claw for ATT 30%
        consumes.add(new GachaponInfo.GachItem(2044706)); // Scroll for Claw for ATT 65%
        consumes.add(new GachaponInfo.GachItem(2044707)); // Scroll for Claw for ATT 15%
        consumes.add(new GachaponInfo.GachItem(2044800)); // Scroll for Knuckle for Attack 100%
        consumes.add(new GachaponInfo.GachItem(2044801)); // Scroll for Knuckle for Attack 60%
        consumes.add(new GachaponInfo.GachItem(2044802)); // Scroll for Knuckle for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2044803)); // Scroll for Knuckle for Attack 70%
        consumes.add(new GachaponInfo.GachItem(2044804)); // Scroll for Knuckle for Attack 30%
        consumes.add(new GachaponInfo.GachItem(2044805)); // Scroll for Knuckle for Accuracy 100%
        consumes.add(new GachaponInfo.GachItem(2044806)); // Scroll for Knuckle for Accuracy 70%
        consumes.add(new GachaponInfo.GachItem(2044807)); // Scroll for Knuckle for Accuracy 60%
        consumes.add(new GachaponInfo.GachItem(2044808)); // Scroll for Knuckle for Accuracy 30%
        consumes.add(new GachaponInfo.GachItem(2044809)); // Scroll for Knuckle for Accuracy 10%
        consumes.add(new GachaponInfo.GachItem(2044811)); // Scroll for Knuckles for ATT 65%
        consumes.add(new GachaponInfo.GachItem(2044812)); // Scroll for Knuckles for ATT 15%
        consumes.add(new GachaponInfo.GachItem(2044813)); // Scroll for Knuckles for Accuracy 65%
        consumes.add(new GachaponInfo.GachItem(2044814)); // Scroll for Knuckles for Accuracy 15%
        consumes.add(new GachaponInfo.GachItem(2044900)); // Scroll for Gun for Attack 100%
        consumes.add(new GachaponInfo.GachItem(2044901)); // Scroll for Gun for Attack 60%
        consumes.add(new GachaponInfo.GachItem(2044902)); // Scroll for Gun for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2044903)); // Scroll for Gun for Attack 70%
        consumes.add(new GachaponInfo.GachItem(2044904)); // Scroll for Gun for Attack 30%
        consumes.add(new GachaponInfo.GachItem(2044906)); // Gun ATT Scroll 65%
        consumes.add(new GachaponInfo.GachItem(2044907)); // Gun ATT Scroll 15%
        consumes.add(new GachaponInfo.GachItem(2045200)); // Scroll for Dual Bowgun ATT 100%
        consumes.add(new GachaponInfo.GachItem(2045201)); // Scroll for Dual Bowgun ATT 60%
        consumes.add(new GachaponInfo.GachItem(2045202)); // Scroll for Dual Bowgun ATT 10%
        consumes.add(new GachaponInfo.GachItem(2045203)); // Dark Scroll for Dual Bowgun ATT 70%
        consumes.add(new GachaponInfo.GachItem(2045204)); // Dark Scroll for Dual Bowgun ATT 30%
        consumes.add(new GachaponInfo.GachItem(2045205)); // Scroll for Dual Bowgun ATT 65%
        consumes.add(new GachaponInfo.GachItem(2045206)); // Scroll for Dual Bowgun ATT 15%
        consumes.add(new GachaponInfo.GachItem(2045207)); // Scroll for Pepe King's Dual Bowgun ATT 60%
        consumes.add(new GachaponInfo.GachItem(2045208)); // Scroll for Dual Bowgun ATT 60%
        consumes.add(new GachaponInfo.GachItem(2045300)); // Scroll for Hand Cannon for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2045301)); // Scroll for Hand Cannon for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2045302)); // Scroll for Hand Cannon for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2045303)); // Scroll for Hand Cannon for ATT 70%
        consumes.add(new GachaponInfo.GachItem(2043400)); // Scroll for Katara for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2043401)); // Scroll for Katara for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2043402)); // Scroll for Katara for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2046000)); // Scroll for One-Handed Weapon for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2046001)); // Scroll for One-Handed Weapon for MAGIC ATT 100%
        consumes.add(new GachaponInfo.GachItem(2046002)); // Scroll for One-Handed Weapon ATT 50%
        consumes.add(new GachaponInfo.GachItem(2046003)); // Scroll for One-Handed Weapon Magic ATT 50%
        consumes.add(new GachaponInfo.GachItem(2046100)); // Scroll for Two-Handed Weapon for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2046101)); // Scroll for Two-Handed Weapon for MAGIC ATT 100%
        consumes.add(new GachaponInfo.GachItem(2046102)); // Scroll for Two-Handed Weapon ATT 50%
        consumes.add(new GachaponInfo.GachItem(2046103)); // Scroll for Two-Handed Weapon for MAGIC ATT 50%
        consumes.add(new GachaponInfo.GachItem(2046200)); // Scroll for Armor for STR 100%
        consumes.add(new GachaponInfo.GachItem(2046201)); // Scroll for Armor for INT 100%
        consumes.add(new GachaponInfo.GachItem(2046202)); // Scroll for Armor for DEX 100%
        consumes.add(new GachaponInfo.GachItem(2046203)); // Scroll for Armor for LUK 100%
        consumes.add(new GachaponInfo.GachItem(2046204)); // Scroll for Armor for STR 50%
        consumes.add(new GachaponInfo.GachItem(2046205)); // Scroll for Armor for INT 50%
        consumes.add(new GachaponInfo.GachItem(2046206)); // Scroll for Armor for DEX 50%
        consumes.add(new GachaponInfo.GachItem(2046207)); // Scroll for Armor for LUK 50%
        consumes.add(new GachaponInfo.GachItem(2046300)); // Scroll for Accessory for STR 100%
        consumes.add(new GachaponInfo.GachItem(2046301)); // Scroll for Accessory for INT 100%
        consumes.add(new GachaponInfo.GachItem(2046302)); // Scroll for Accessory for DEX 100%
        consumes.add(new GachaponInfo.GachItem(2046303)); // Scroll for Accessory for LUK 100%
        consumes.add(new GachaponInfo.GachItem(2046304)); // Scroll for Accessory for STR 50%
        consumes.add(new GachaponInfo.GachItem(2046305)); // Scroll for Accessory for INT 50%
        consumes.add(new GachaponInfo.GachItem(2046306)); // Scroll for Accessory for DEX 50%
        consumes.add(new GachaponInfo.GachItem(2046307)); // Scroll for Accessory for LUK 50%
        consumes.add(new GachaponInfo.GachItem(2046208)); // Dark Scroll for Armor STR 40%
        consumes.add(new GachaponInfo.GachItem(2046209)); // Dark Scroll for Armor INT 40%
        consumes.add(new GachaponInfo.GachItem(2046210)); // Dark Scroll for Armor DEX 40%
        consumes.add(new GachaponInfo.GachItem(2046211)); // Dark Scroll for Armor LUK 40%
        consumes.add(new GachaponInfo.GachItem(2046212)); // Dark Scroll for Armor HP 40%
        consumes.add(new GachaponInfo.GachItem(2046223)); // Scroll for Armor STR 60%
        consumes.add(new GachaponInfo.GachItem(2046224)); // Scroll for Armor INT 60%
        consumes.add(new GachaponInfo.GachItem(2046225)); // Scroll for Armor for DEX 60%
        consumes.add(new GachaponInfo.GachItem(2046226)); // Scroll for Armor LUK 60%
        consumes.add(new GachaponInfo.GachItem(2046227)); // Scroll for Armor ATT 15%
        consumes.add(new GachaponInfo.GachItem(2046228)); // Scroll for Armor Magic ATT 15%
        consumes.add(new GachaponInfo.GachItem(2046229)); // Scroll for Armor STR 60%
        consumes.add(new GachaponInfo.GachItem(2046230)); // Scroll for Armor INT 60%
        consumes.add(new GachaponInfo.GachItem(2046231)); // Scroll for Armor for DEX 60%
        consumes.add(new GachaponInfo.GachItem(2046232)); // Scroll for Armor LUK 60%
        consumes.add(new GachaponInfo.GachItem(2046314)); // Scroll for Accessory STR 60%
        consumes.add(new GachaponInfo.GachItem(2046315)); // Scroll for Accessory INT 60%
        consumes.add(new GachaponInfo.GachItem(2046316)); // Scroll for Accessory for DEX 60%
        consumes.add(new GachaponInfo.GachItem(2046317)); // Scroll for Accessory LUK 60%
        consumes.add(new GachaponInfo.GachItem(2046318)); // Scroll for Accessory for ATT 15%
        consumes.add(new GachaponInfo.GachItem(2046319)); // Scroll for Accessory Magic ATT 15%
        consumes.add(new GachaponInfo.GachItem(2046320)); // Scroll for Accessory STR 60%
        consumes.add(new GachaponInfo.GachItem(2046321)); // Scroll for Accessory INT 60%
        consumes.add(new GachaponInfo.GachItem(2046322)); // Scroll for Accessory for DEX 60%
        consumes.add(new GachaponInfo.GachItem(2046323)); // Scroll for Accessory LUK 60%
        consumes.add(new GachaponInfo.GachItem(2043403)); // Scroll for Katara for ATT 65%
        consumes.add(new GachaponInfo.GachItem(2043404)); // Scroll for Katara for ATT 15%
        consumes.add(new GachaponInfo.GachItem(2043407)); // Scroll for Katara for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2043600)); // Scroll for Cane for ATT 100%
        consumes.add(new GachaponInfo.GachItem(2043601)); // Scroll for Cane for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2043602)); // Scroll for Cane for ATT 10%
        consumes.add(new GachaponInfo.GachItem(2043603)); // Scroll for Cane for ATT 70%
        consumes.add(new GachaponInfo.GachItem(2043604)); // Scroll for Cane for ATT 30%
        consumes.add(new GachaponInfo.GachItem(2043605)); // Scroll for Cane for ATT 65%
        consumes.add(new GachaponInfo.GachItem(2043606)); // Scroll for Cane for ATT 15%l
        consumes.add(new GachaponInfo.GachItem(2046927)); // Scroll for One-Handed Weapon for ATT 20%
        consumes.add(new GachaponInfo.GachItem(2046928)); // Scroll for One-Handed Weapon for Magic ATT 20%
        consumes.add(new GachaponInfo.GachItem(2046929)); // Scroll for One-Handed Weapon for ATT 40%
        consumes.add(new GachaponInfo.GachItem(2046930)); // Scroll for One-Handed Weapon for Magic ATT 40%
        consumes.add(new GachaponInfo.GachItem(2046931)); // Scroll for One-Handed Weapon for ATT 70%
        consumes.add(new GachaponInfo.GachItem(2046932)); // Scroll for One-Handed Weapon for Magic ATT 70%
        consumes.add(new GachaponInfo.GachItem(2046939)); // Scroll for One-Handed Weapon for ATT 20%
        consumes.add(new GachaponInfo.GachItem(2046940)); // Scroll for One-Handed Weapon for Magic ATT 20%
        consumes.add(new GachaponInfo.GachItem(2046941)); // Scroll for One-Handed Weapon for ATT 70%
        consumes.add(new GachaponInfo.GachItem(2046942)); // Scroll for One-Handed Weapon for Magic ATT 70%
        consumes.add(new GachaponInfo.GachItem(2046185)); // Scroll for Two-Handed Weapon for ATT 20%
        consumes.add(new GachaponInfo.GachItem(2046186)); // Scroll for Two-Handed Weapon for ATT 40%
        consumes.add(new GachaponInfo.GachItem(2046187)); // Scroll for Two-Handed Weapon for ATT 70%
        consumes.add(new GachaponInfo.GachItem(2046192)); // Scroll for Two-Handed Weapon for ATT 20%
        consumes.add(new GachaponInfo.GachItem(2046193)); // Scroll for Two-Handed Weapon for ATT 70%
        consumes.add(new GachaponInfo.GachItem(2046839)); // Scroll for Accessory for ATT 30%
        consumes.add(new GachaponInfo.GachItem(2046840)); // Scroll for Accessory Magic ATT 30%
        consumes.add(new GachaponInfo.GachItem(2045400)); // Scroll for Katana for ATT
        consumes.add(new GachaponInfo.GachItem(2045401)); // Scroll for Katana for ATT
        consumes.add(new GachaponInfo.GachItem(2045402)); // Scroll for Katana for ATT
        consumes.add(new GachaponInfo.GachItem(2045403)); // Scroll for Katana for ATT
        consumes.add(new GachaponInfo.GachItem(2045404)); // Scroll for Katana for ATT
        consumes.add(new GachaponInfo.GachItem(2045405)); // Scroll for Katana for ATT
        consumes.add(new GachaponInfo.GachItem(2045406)); // Scroll for Katana for ATT
        consumes.add(new GachaponInfo.GachItem(2045407)); // Scroll for Katana for ATT
        consumes.add(new GachaponInfo.GachItem(2045408)); // Scroll for Katana for ATT
        consumes.add(new GachaponInfo.GachItem(2045500)); // Scroll for Fan for Magic ATT
        consumes.add(new GachaponInfo.GachItem(2045501)); // Scroll for Fan for Magic ATT
        consumes.add(new GachaponInfo.GachItem(2045502)); // Scroll for Fan for Magic ATT
        consumes.add(new GachaponInfo.GachItem(2045503)); // Scroll for Fan for Magic ATT
        consumes.add(new GachaponInfo.GachItem(2045504)); // Scroll for Fan for Magic ATT
        consumes.add(new GachaponInfo.GachItem(2045505)); // Scroll for Fan for Magic ATT
        consumes.add(new GachaponInfo.GachItem(2045506)); // Scroll for Fan for Magic ATT
        consumes.add(new GachaponInfo.GachItem(2045507)); // Scroll for Fan for Magic ATT
        consumes.add(new GachaponInfo.GachItem(2045508)); // Scroll for Fan for Magic ATT
        consumes.add(new GachaponInfo.GachItem(2046621)); // Scroll for Armor for STR
        consumes.add(new GachaponInfo.GachItem(2046622)); // Scroll for Armor for STR
        consumes.add(new GachaponInfo.GachItem(2046623)); // Scroll for Armor for DEX
        consumes.add(new GachaponInfo.GachItem(2046624)); // Scroll for Armor for DEX
        consumes.add(new GachaponInfo.GachItem(2046625)); // Scroll for Armor for INT
        consumes.add(new GachaponInfo.GachItem(2046626)); // Scroll for Armor for INT
        consumes.add(new GachaponInfo.GachItem(2046627)); // Scroll for Armor for LUK
        consumes.add(new GachaponInfo.GachItem(2046628)); // Scroll for Armor for LUK
        consumes.add(new GachaponInfo.GachItem(2046966)); // One-Handed Weapon Enchant Scroll 50%
        consumes.add(new GachaponInfo.GachItem(2046967)); // Pinnacle Scroll for One-Handed Weapon for ATT 20%
        consumes.add(new GachaponInfo.GachItem(2046969)); // Scroll for One-Handed Weapon for ATT 70%
        consumes.add(new GachaponInfo.GachItem(2046970)); // Scroll for One-Handed Weapon for Magic ATT 70%
        consumes.add(new GachaponInfo.GachItem(2046971)); // Pinnacle Scroll for One-Handed Weapon for Magic ATT 20%
        consumes.add(new GachaponInfo.GachItem(2046978)); // Unleashed Scroll for One-Handed Weapon for ATT 50%
        consumes.add(new GachaponInfo.GachItem(2046979)); // Unleashed Scroll for One-Handed Weapon for Magic ATT 50%
        consumes.add(new GachaponInfo.GachItem(2046981)); // Scroll for One-handed Weapon ATT 90%
        consumes.add(new GachaponInfo.GachItem(2046982)); // Scroll for One-handed Weapon ATT 70%
        consumes.add(new GachaponInfo.GachItem(2046983)); // Scroll for One-Handed Weapon for Magic ATT 90%
        consumes.add(new GachaponInfo.GachItem(2046984)); // Scroll for One-Handed Weapon for Magic ATT 70%
        consumes.add(new GachaponInfo.GachItem(2041131)); // Scroll for Happy Ring for Magic DEF 100%
        consumes.add(new GachaponInfo.GachItem(2041130)); // Scroll for Happy Ring for Weapon DEF 100%
        consumes.add(new GachaponInfo.GachItem(2041124)); // Scroll for Happy Ring for HP 70%
        consumes.add(new GachaponInfo.GachItem(2041125)); // Scroll for Happy Ring for Magic ATT 70%
        consumes.add(new GachaponInfo.GachItem(2041126)); // Scroll for Happy Ring for Jump 70%
        consumes.add(new GachaponInfo.GachItem(2041127)); // Scroll for Happy Ring for Speed 70%
        consumes.add(new GachaponInfo.GachItem(2041128)); // Scroll for Happy Ring for Avoidability 70%
        consumes.add(new GachaponInfo.GachItem(2041129)); // Scroll for Happy Ring for ACC 70%
        consumes.add(new GachaponInfo.GachItem(2041122)); // Scroll for Happy Ring for INT 30%
        consumes.add(new GachaponInfo.GachItem(2041123)); // Scroll for Happy Ring for LUK 30%
        consumes.add(new GachaponInfo.GachItem(2040033)); // Scroll for Auf Haven Circlet for ATT 60%
        consumes.add(new GachaponInfo.GachItem(2040034)); // Scroll for Auf Haven Circlet for INT 60%
        consumes.add(new GachaponInfo.GachItem(2040035)); // Scroll for Auf Haven Circlet for DEX 60%
        consumes.add(new GachaponInfo.GachItem(2040036)); // Scroll for Auf Haven Circlet for LUK 60%
        consumes.add(new GachaponInfo.GachItem(2040213)); // Scroll for glasses for STR 30%
        consumes.add(new GachaponInfo.GachItem(2040214)); // Scroll for glasses for LUK 30%
        consumes.add(new GachaponInfo.GachItem(2040215)); // Scroll for glasses for INT 30%
        consumes.add(new GachaponInfo.GachItem(2040216)); // Scroll for glasses for DEX 30%
        consumes.add(new GachaponInfo.GachItem(2040217)); // Scroll for glasses for STR 60%
        consumes.add(new GachaponInfo.GachItem(2040218)); // Scroll for glasses for DEX 60%
        consumes.add(new GachaponInfo.GachItem(2040219)); // Scroll for glasses for INT 60%
        consumes.add(new GachaponInfo.GachItem(2040220)); // Scroll for glasses for DEX 60%
        consumes.add(new GachaponInfo.GachItem(2040221)); // Scroll for glasses for STR 80%
        consumes.add(new GachaponInfo.GachItem(2040222)); // Scroll for glasses for LUK 80%
        consumes.add(new GachaponInfo.GachItem(2040223)); // Scroll for glasses for INT 80%
        consumes.add(new GachaponInfo.GachItem(2040224)); // Scroll for glasses for DEX 80%
        consumes.add(new GachaponInfo.GachItem(2040225)); // Scroll for glasses for Attack 30%
        consumes.add(new GachaponInfo.GachItem(2040226)); // Scroll for glasses for Magic Attack 30%
        consumes.add(new GachaponInfo.GachItem(2041120)); // Scroll for Happy Ring for STR 30%
        consumes.add(new GachaponInfo.GachItem(2041121)); // Scroll for Happy Ring for DEX 30%
        consumes.add(new GachaponInfo.GachItem(2046994, true));
        consumes.add(new GachaponInfo.GachItem(2046995, true));
        consumes.add(new GachaponInfo.GachItem(2047816, true));
        consumes.add(new GachaponInfo.GachItem(2047817, true));
    }

    private static void initNebulite() {
        Nebulites = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Nebulites.add(new GachaponInfo.GachItem(2430748));
        }
        Nebulites.add(new GachaponInfo.GachItem(4420000, true));
        Nebulites.add(new GachaponInfo.GachItem(3060000));
        Nebulites.add(new GachaponInfo.GachItem(3060001));
        Nebulites.add(new GachaponInfo.GachItem(3060002));
        Nebulites.add(new GachaponInfo.GachItem(3060010));
        Nebulites.add(new GachaponInfo.GachItem(3060011));
        Nebulites.add(new GachaponInfo.GachItem(3060012));
        Nebulites.add(new GachaponInfo.GachItem(3060020));
        Nebulites.add(new GachaponInfo.GachItem(3060021));
        Nebulites.add(new GachaponInfo.GachItem(3060022));
        Nebulites.add(new GachaponInfo.GachItem(3060030));
        Nebulites.add(new GachaponInfo.GachItem(3060031));
        Nebulites.add(new GachaponInfo.GachItem(3060032));
        Nebulites.add(new GachaponInfo.GachItem(3060040));
        Nebulites.add(new GachaponInfo.GachItem(3060041));
        Nebulites.add(new GachaponInfo.GachItem(3060042));
        Nebulites.add(new GachaponInfo.GachItem(3060050));
        Nebulites.add(new GachaponInfo.GachItem(3060051));
        Nebulites.add(new GachaponInfo.GachItem(3060052));
        //Nebulites.add(new GachaponInfo.GachItem(3060060));
        //Nebulites.add(new GachaponInfo.GachItem(3060061));
        //Nebulites.add(new GachaponInfo.GachItem(3060070));
        //Nebulites.add(new GachaponInfo.GachItem(3060071));
        Nebulites.add(new GachaponInfo.GachItem(3060080));
        Nebulites.add(new GachaponInfo.GachItem(3060081));
        Nebulites.add(new GachaponInfo.GachItem(3060090));
        Nebulites.add(new GachaponInfo.GachItem(3060091));
        Nebulites.add(new GachaponInfo.GachItem(3060100));
        Nebulites.add(new GachaponInfo.GachItem(3060110));
        Nebulites.add(new GachaponInfo.GachItem(3060120));
        Nebulites.add(new GachaponInfo.GachItem(3060121));
        Nebulites.add(new GachaponInfo.GachItem(3060122));
        Nebulites.add(new GachaponInfo.GachItem(3060130));
        Nebulites.add(new GachaponInfo.GachItem(3060131));
        Nebulites.add(new GachaponInfo.GachItem(3060132));
        Nebulites.add(new GachaponInfo.GachItem(3060140));
        Nebulites.add(new GachaponInfo.GachItem(3060150));
        Nebulites.add(new GachaponInfo.GachItem(3060160));
        Nebulites.add(new GachaponInfo.GachItem(3060170));
        Nebulites.add(new GachaponInfo.GachItem(3060180));
        Nebulites.add(new GachaponInfo.GachItem(3061000));
        Nebulites.add(new GachaponInfo.GachItem(3061001));
        Nebulites.add(new GachaponInfo.GachItem(3061010));
        Nebulites.add(new GachaponInfo.GachItem(3061011));
        Nebulites.add(new GachaponInfo.GachItem(3061020));
        Nebulites.add(new GachaponInfo.GachItem(3061021));
        Nebulites.add(new GachaponInfo.GachItem(3061030));
        Nebulites.add(new GachaponInfo.GachItem(3061031));
        Nebulites.add(new GachaponInfo.GachItem(3061040));
        Nebulites.add(new GachaponInfo.GachItem(3061041));
        Nebulites.add(new GachaponInfo.GachItem(3061042));
        Nebulites.add(new GachaponInfo.GachItem(3061050));
        Nebulites.add(new GachaponInfo.GachItem(3061051));
        Nebulites.add(new GachaponInfo.GachItem(3061052));
        //Nebulites.add(new GachaponInfo.GachItem(3061060));
        //Nebulites.add(new GachaponInfo.GachItem(3061061));
        //Nebulites.add(new GachaponInfo.GachItem(3061070));
        //Nebulites.add(new GachaponInfo.GachItem(3061071));
        Nebulites.add(new GachaponInfo.GachItem(3061080));
        Nebulites.add(new GachaponInfo.GachItem(3061081));
        Nebulites.add(new GachaponInfo.GachItem(3061090));
        Nebulites.add(new GachaponInfo.GachItem(3061091));
        Nebulites.add(new GachaponInfo.GachItem(3061100));
        Nebulites.add(new GachaponInfo.GachItem(3061110));
        Nebulites.add(new GachaponInfo.GachItem(3061120));
        Nebulites.add(new GachaponInfo.GachItem(3061121));
        Nebulites.add(new GachaponInfo.GachItem(3061122));
        Nebulites.add(new GachaponInfo.GachItem(3061123));
        Nebulites.add(new GachaponInfo.GachItem(3061124));
        Nebulites.add(new GachaponInfo.GachItem(3061125));
        Nebulites.add(new GachaponInfo.GachItem(3061130));
        Nebulites.add(new GachaponInfo.GachItem(3061131));
        Nebulites.add(new GachaponInfo.GachItem(3061132));
        Nebulites.add(new GachaponInfo.GachItem(3061133));
        Nebulites.add(new GachaponInfo.GachItem(3061134));
        Nebulites.add(new GachaponInfo.GachItem(3061135));
        Nebulites.add(new GachaponInfo.GachItem(3061140));
        Nebulites.add(new GachaponInfo.GachItem(3061150));
        Nebulites.add(new GachaponInfo.GachItem(3061160));
        Nebulites.add(new GachaponInfo.GachItem(3061170));
        Nebulites.add(new GachaponInfo.GachItem(3061180));
        Nebulites.add(new GachaponInfo.GachItem(3061190));
        Nebulites.add(new GachaponInfo.GachItem(3061200));
        Nebulites.add(new GachaponInfo.GachItem(3061210));
        Nebulites.add(new GachaponInfo.GachItem(3061220));
        Nebulites.add(new GachaponInfo.GachItem(3061230));
        Nebulites.add(new GachaponInfo.GachItem(3061240));
        Nebulites.add(new GachaponInfo.GachItem(3061250));
        Nebulites.add(new GachaponInfo.GachItem(3061260));
        Nebulites.add(new GachaponInfo.GachItem(3061270));
        Nebulites.add(new GachaponInfo.GachItem(3061271));
        Nebulites.add(new GachaponInfo.GachItem(3061280));
        Nebulites.add(new GachaponInfo.GachItem(3061290));
        Nebulites.add(new GachaponInfo.GachItem(3061291));
        Nebulites.add(new GachaponInfo.GachItem(3061292));
        Nebulites.add(new GachaponInfo.GachItem(3061293));
        Nebulites.add(new GachaponInfo.GachItem(3061294));
        Nebulites.add(new GachaponInfo.GachItem(3061295));
        Nebulites.add(new GachaponInfo.GachItem(3061300));
        Nebulites.add(new GachaponInfo.GachItem(3061301));
        Nebulites.add(new GachaponInfo.GachItem(3061302));
        Nebulites.add(new GachaponInfo.GachItem(3061303));
        Nebulites.add(new GachaponInfo.GachItem(3061304));
        Nebulites.add(new GachaponInfo.GachItem(3061305));
        Nebulites.add(new GachaponInfo.GachItem(3061310));
        Nebulites.add(new GachaponInfo.GachItem(3061311));
        Nebulites.add(new GachaponInfo.GachItem(3061312));
        Nebulites.add(new GachaponInfo.GachItem(3061313));
        Nebulites.add(new GachaponInfo.GachItem(3061314));
        Nebulites.add(new GachaponInfo.GachItem(3061315));
        Nebulites.add(new GachaponInfo.GachItem(3061320));
        Nebulites.add(new GachaponInfo.GachItem(3061321));
        Nebulites.add(new GachaponInfo.GachItem(3061322));
        Nebulites.add(new GachaponInfo.GachItem(3061323));
        Nebulites.add(new GachaponInfo.GachItem(3061324));
        Nebulites.add(new GachaponInfo.GachItem(3061325));
        Nebulites.add(new GachaponInfo.GachItem(3061330));
        Nebulites.add(new GachaponInfo.GachItem(3061331));
        Nebulites.add(new GachaponInfo.GachItem(3061332));
        Nebulites.add(new GachaponInfo.GachItem(3061333));
        Nebulites.add(new GachaponInfo.GachItem(3061334));
        Nebulites.add(new GachaponInfo.GachItem(3061335));
        Nebulites.add(new GachaponInfo.GachItem(3061340));
        Nebulites.add(new GachaponInfo.GachItem(3061341));
        Nebulites.add(new GachaponInfo.GachItem(3061350));
        Nebulites.add(new GachaponInfo.GachItem(3061351));
        Nebulites.add(new GachaponInfo.GachItem(3061360));
        Nebulites.add(new GachaponInfo.GachItem(3061361));
        Nebulites.add(new GachaponInfo.GachItem(3061362));
        Nebulites.add(new GachaponInfo.GachItem(3061370));
        Nebulites.add(new GachaponInfo.GachItem(3061371));
        Nebulites.add(new GachaponInfo.GachItem(3061380));
        Nebulites.add(new GachaponInfo.GachItem(3061381));
        Nebulites.add(new GachaponInfo.GachItem(3061390));
        Nebulites.add(new GachaponInfo.GachItem(3062000));
        Nebulites.add(new GachaponInfo.GachItem(3062001));
        Nebulites.add(new GachaponInfo.GachItem(3062010));
        Nebulites.add(new GachaponInfo.GachItem(3062011));
        Nebulites.add(new GachaponInfo.GachItem(3062020));
        Nebulites.add(new GachaponInfo.GachItem(3062021));
        Nebulites.add(new GachaponInfo.GachItem(3062030));
        Nebulites.add(new GachaponInfo.GachItem(3062031));
        Nebulites.add(new GachaponInfo.GachItem(3062040));
        Nebulites.add(new GachaponInfo.GachItem(3062041));
        Nebulites.add(new GachaponInfo.GachItem(3062042));
        Nebulites.add(new GachaponInfo.GachItem(3062050));
        Nebulites.add(new GachaponInfo.GachItem(3062051));
        Nebulites.add(new GachaponInfo.GachItem(3062052));
        //Nebulites.add(new GachaponInfo.GachItem(3062060));
        //Nebulites.add(new GachaponInfo.GachItem(3062061));
        //Nebulites.add(new GachaponInfo.GachItem(3062062));
        Nebulites.add(new GachaponInfo.GachItem(3062070));
        Nebulites.add(new GachaponInfo.GachItem(3062071));
        Nebulites.add(new GachaponInfo.GachItem(3062072));
        Nebulites.add(new GachaponInfo.GachItem(3062080));
        Nebulites.add(new GachaponInfo.GachItem(3062081));
        Nebulites.add(new GachaponInfo.GachItem(3062090));
        Nebulites.add(new GachaponInfo.GachItem(3062091));
        Nebulites.add(new GachaponInfo.GachItem(3062100));
        Nebulites.add(new GachaponInfo.GachItem(3062101));
        Nebulites.add(new GachaponInfo.GachItem(3062110));
        Nebulites.add(new GachaponInfo.GachItem(3062111));
        Nebulites.add(new GachaponInfo.GachItem(3062120));
        Nebulites.add(new GachaponInfo.GachItem(3062121));
        Nebulites.add(new GachaponInfo.GachItem(3062122));
        Nebulites.add(new GachaponInfo.GachItem(3062130));
        Nebulites.add(new GachaponInfo.GachItem(3062131));
        Nebulites.add(new GachaponInfo.GachItem(3062132));
        Nebulites.add(new GachaponInfo.GachItem(3062140));
        Nebulites.add(new GachaponInfo.GachItem(3062150));
        Nebulites.add(new GachaponInfo.GachItem(3062160));
        Nebulites.add(new GachaponInfo.GachItem(3062170));
        Nebulites.add(new GachaponInfo.GachItem(3062180));
        Nebulites.add(new GachaponInfo.GachItem(3062190));
        Nebulites.add(new GachaponInfo.GachItem(3062200));
        Nebulites.add(new GachaponInfo.GachItem(3062210));
        Nebulites.add(new GachaponInfo.GachItem(3062220));
        Nebulites.add(new GachaponInfo.GachItem(3062230));
        Nebulites.add(new GachaponInfo.GachItem(3062240));
        Nebulites.add(new GachaponInfo.GachItem(3062250));
        Nebulites.add(new GachaponInfo.GachItem(3062260));
        Nebulites.add(new GachaponInfo.GachItem(3062261));
        Nebulites.add(new GachaponInfo.GachItem(3062270));
        Nebulites.add(new GachaponInfo.GachItem(3062271));
        Nebulites.add(new GachaponInfo.GachItem(3062280));
        Nebulites.add(new GachaponInfo.GachItem(3062281));
        Nebulites.add(new GachaponInfo.GachItem(3062290));
        Nebulites.add(new GachaponInfo.GachItem(3062291));
        Nebulites.add(new GachaponInfo.GachItem(3062292));
        Nebulites.add(new GachaponInfo.GachItem(3062293));
        Nebulites.add(new GachaponInfo.GachItem(3062294));
        Nebulites.add(new GachaponInfo.GachItem(3062295));
        Nebulites.add(new GachaponInfo.GachItem(3062300));
        Nebulites.add(new GachaponInfo.GachItem(3062301));
        Nebulites.add(new GachaponInfo.GachItem(3062302));
        Nebulites.add(new GachaponInfo.GachItem(3062303));
        Nebulites.add(new GachaponInfo.GachItem(3062304));
        Nebulites.add(new GachaponInfo.GachItem(3062305));
        Nebulites.add(new GachaponInfo.GachItem(3062310));
        Nebulites.add(new GachaponInfo.GachItem(3062320));
        Nebulites.add(new GachaponInfo.GachItem(3062321));
        Nebulites.add(new GachaponInfo.GachItem(3062322));
        Nebulites.add(new GachaponInfo.GachItem(3062323));
        Nebulites.add(new GachaponInfo.GachItem(3062324));
        Nebulites.add(new GachaponInfo.GachItem(3062325));
        Nebulites.add(new GachaponInfo.GachItem(3062330));
        Nebulites.add(new GachaponInfo.GachItem(3062331));
        Nebulites.add(new GachaponInfo.GachItem(3062332));
        Nebulites.add(new GachaponInfo.GachItem(3062333));
        Nebulites.add(new GachaponInfo.GachItem(3062334));
        Nebulites.add(new GachaponInfo.GachItem(3062335));
        Nebulites.add(new GachaponInfo.GachItem(3062340));
        Nebulites.add(new GachaponInfo.GachItem(3062341));
        Nebulites.add(new GachaponInfo.GachItem(3062342));
        Nebulites.add(new GachaponInfo.GachItem(3062343));
        Nebulites.add(new GachaponInfo.GachItem(3062344));
        Nebulites.add(new GachaponInfo.GachItem(3062345));
        Nebulites.add(new GachaponInfo.GachItem(3062350));
        Nebulites.add(new GachaponInfo.GachItem(3062360));
        Nebulites.add(new GachaponInfo.GachItem(3062370));
        Nebulites.add(new GachaponInfo.GachItem(3062371));
        Nebulites.add(new GachaponInfo.GachItem(3062372));
        Nebulites.add(new GachaponInfo.GachItem(3062373));
        Nebulites.add(new GachaponInfo.GachItem(3062374));
        Nebulites.add(new GachaponInfo.GachItem(3062375));
        Nebulites.add(new GachaponInfo.GachItem(3062380));
        Nebulites.add(new GachaponInfo.GachItem(3062381));
        Nebulites.add(new GachaponInfo.GachItem(3062382));
        Nebulites.add(new GachaponInfo.GachItem(3062383));
        Nebulites.add(new GachaponInfo.GachItem(3062384));
        Nebulites.add(new GachaponInfo.GachItem(3062385));
        Nebulites.add(new GachaponInfo.GachItem(3063000, true));
        Nebulites.add(new GachaponInfo.GachItem(3063001, true));
        Nebulites.add(new GachaponInfo.GachItem(3063010, true));
        Nebulites.add(new GachaponInfo.GachItem(3063011, true));
        Nebulites.add(new GachaponInfo.GachItem(3063020, true));
        Nebulites.add(new GachaponInfo.GachItem(3063021, true));
        Nebulites.add(new GachaponInfo.GachItem(3063030, true));
        Nebulites.add(new GachaponInfo.GachItem(3063031, true));
        Nebulites.add(new GachaponInfo.GachItem(3063040, true));
        Nebulites.add(new GachaponInfo.GachItem(3063041, true));
        Nebulites.add(new GachaponInfo.GachItem(3063042, true));
        Nebulites.add(new GachaponInfo.GachItem(3063050, true));
        Nebulites.add(new GachaponInfo.GachItem(3063051, true));
        Nebulites.add(new GachaponInfo.GachItem(3063052, true));
        Nebulites.add(new GachaponInfo.GachItem(3063060, true));
        Nebulites.add(new GachaponInfo.GachItem(3063061, true));
        Nebulites.add(new GachaponInfo.GachItem(3063062, true));
        //Nebulites.add(new GachaponInfo.GachItem(3063063, true));
        //Nebulites.add(new GachaponInfo.GachItem(3063070, true));
        //Nebulites.add(new GachaponInfo.GachItem(3063071, true));
        //Nebulites.add(new GachaponInfo.GachItem(3063072, true));
        //Nebulites.add(new GachaponInfo.GachItem(3063073, true));
        Nebulites.add(new GachaponInfo.GachItem(3063080, true));
        Nebulites.add(new GachaponInfo.GachItem(3063081, true));
        Nebulites.add(new GachaponInfo.GachItem(3063090, true));
        Nebulites.add(new GachaponInfo.GachItem(3063091, true));
        Nebulites.add(new GachaponInfo.GachItem(3063100, true));
        Nebulites.add(new GachaponInfo.GachItem(3063101, true));
        Nebulites.add(new GachaponInfo.GachItem(3063110, true));
        Nebulites.add(new GachaponInfo.GachItem(3063111, true));
        Nebulites.add(new GachaponInfo.GachItem(3063120, true));
        Nebulites.add(new GachaponInfo.GachItem(3063121, true));
        Nebulites.add(new GachaponInfo.GachItem(3063122, true));
        Nebulites.add(new GachaponInfo.GachItem(3063130, true));
        Nebulites.add(new GachaponInfo.GachItem(3063131, true));
        Nebulites.add(new GachaponInfo.GachItem(3063132, true));
        Nebulites.add(new GachaponInfo.GachItem(3063140, true));
        Nebulites.add(new GachaponInfo.GachItem(3063141, true));
        Nebulites.add(new GachaponInfo.GachItem(3063150, true));
        Nebulites.add(new GachaponInfo.GachItem(3063151, true));
        Nebulites.add(new GachaponInfo.GachItem(3063160, true));
        Nebulites.add(new GachaponInfo.GachItem(3063161, true));
        Nebulites.add(new GachaponInfo.GachItem(3063170, true));
        Nebulites.add(new GachaponInfo.GachItem(3063171, true));
        Nebulites.add(new GachaponInfo.GachItem(3063180, true));
        Nebulites.add(new GachaponInfo.GachItem(3063181, true));
        Nebulites.add(new GachaponInfo.GachItem(3063190, true));
        Nebulites.add(new GachaponInfo.GachItem(3063191, true));
        //Nebulites.add(new GachaponInfo.GachItem(3063200, true));
        //Nebulites.add(new GachaponInfo.GachItem(3063201, true));
        //Nebulites.add(new GachaponInfo.GachItem(3063210, true));
        //Nebulites.add(new GachaponInfo.GachItem(3063211, true));
        Nebulites.add(new GachaponInfo.GachItem(3063220, true));
        Nebulites.add(new GachaponInfo.GachItem(3063221, true));
        Nebulites.add(new GachaponInfo.GachItem(3063230, true));
        Nebulites.add(new GachaponInfo.GachItem(3063231, true));
        Nebulites.add(new GachaponInfo.GachItem(3063240, true));
        Nebulites.add(new GachaponInfo.GachItem(3063241, true));
        Nebulites.add(new GachaponInfo.GachItem(3063250, true));
        Nebulites.add(new GachaponInfo.GachItem(3063251, true));
        Nebulites.add(new GachaponInfo.GachItem(3063260, true));
        Nebulites.add(new GachaponInfo.GachItem(3063261, true));
        Nebulites.add(new GachaponInfo.GachItem(3063270, true));
        Nebulites.add(new GachaponInfo.GachItem(3063271, true));
        Nebulites.add(new GachaponInfo.GachItem(3063280, true));
        Nebulites.add(new GachaponInfo.GachItem(3063281, true));
        Nebulites.add(new GachaponInfo.GachItem(3063290, true));
        Nebulites.add(new GachaponInfo.GachItem(3063300, true));
        Nebulites.add(new GachaponInfo.GachItem(3063310, true));
        Nebulites.add(new GachaponInfo.GachItem(3063320, true));
        Nebulites.add(new GachaponInfo.GachItem(3063330, true));
        Nebulites.add(new GachaponInfo.GachItem(3063340, true));
        Nebulites.add(new GachaponInfo.GachItem(3063341, true));
        Nebulites.add(new GachaponInfo.GachItem(3063350, true));
        Nebulites.add(new GachaponInfo.GachItem(3063351, true));
        Nebulites.add(new GachaponInfo.GachItem(3063360, true));
        Nebulites.add(new GachaponInfo.GachItem(3063361, true));
        Nebulites.add(new GachaponInfo.GachItem(3063370, true));
        Nebulites.add(new GachaponInfo.GachItem(3063380, true));
        Nebulites.add(new GachaponInfo.GachItem(3063390, true));
        Nebulites.add(new GachaponInfo.GachItem(3063400, true));
        Nebulites.add(new GachaponInfo.GachItem(3064000, true));
        Nebulites.add(new GachaponInfo.GachItem(3064001, true));
        Nebulites.add(new GachaponInfo.GachItem(3064002, true));
        Nebulites.add(new GachaponInfo.GachItem(3064010, true));
        Nebulites.add(new GachaponInfo.GachItem(3064011, true));
        Nebulites.add(new GachaponInfo.GachItem(3064012, true));
        Nebulites.add(new GachaponInfo.GachItem(3064020, true));
        Nebulites.add(new GachaponInfo.GachItem(3064021, true));
        Nebulites.add(new GachaponInfo.GachItem(3064022, true));
        Nebulites.add(new GachaponInfo.GachItem(3064030, true));
        Nebulites.add(new GachaponInfo.GachItem(3064031, true));
        Nebulites.add(new GachaponInfo.GachItem(3064032, true));
        Nebulites.add(new GachaponInfo.GachItem(3064040, true));
        Nebulites.add(new GachaponInfo.GachItem(3064041, true));
        Nebulites.add(new GachaponInfo.GachItem(3064042, true));
        Nebulites.add(new GachaponInfo.GachItem(3064050, true));
        Nebulites.add(new GachaponInfo.GachItem(3064051, true));
        Nebulites.add(new GachaponInfo.GachItem(3064052, true));
        //Nebulites.add(new GachaponInfo.GachItem(3064060, true));
        //Nebulites.add(new GachaponInfo.GachItem(3064061, true));
        //Nebulites.add(new GachaponInfo.GachItem(3064062, true));
        //Nebulites.add(new GachaponInfo.GachItem(3064070, true));
        //Nebulites.add(new GachaponInfo.GachItem(3064071, true));
        Nebulites.add(new GachaponInfo.GachItem(3064072, true));
        Nebulites.add(new GachaponInfo.GachItem(3064080, true));
        Nebulites.add(new GachaponInfo.GachItem(3064081, true));
        Nebulites.add(new GachaponInfo.GachItem(3064090, true));
        Nebulites.add(new GachaponInfo.GachItem(3064091, true));
        Nebulites.add(new GachaponInfo.GachItem(3064100, true));
        Nebulites.add(new GachaponInfo.GachItem(3064101, true));
        Nebulites.add(new GachaponInfo.GachItem(3064110, true));
        Nebulites.add(new GachaponInfo.GachItem(3064111, true));
        Nebulites.add(new GachaponInfo.GachItem(3064120, true));
        Nebulites.add(new GachaponInfo.GachItem(3064121, true));
        Nebulites.add(new GachaponInfo.GachItem(3064122, true));
        Nebulites.add(new GachaponInfo.GachItem(3064123, true));
        Nebulites.add(new GachaponInfo.GachItem(3064124, true));
        Nebulites.add(new GachaponInfo.GachItem(3064130, true));
        Nebulites.add(new GachaponInfo.GachItem(3064131, true));
        Nebulites.add(new GachaponInfo.GachItem(3064132, true));
        Nebulites.add(new GachaponInfo.GachItem(3064133, true));
        Nebulites.add(new GachaponInfo.GachItem(3064134, true));
        Nebulites.add(new GachaponInfo.GachItem(3064140, true));
        Nebulites.add(new GachaponInfo.GachItem(3064141, true));
        Nebulites.add(new GachaponInfo.GachItem(3064150, true));
        Nebulites.add(new GachaponInfo.GachItem(3064151, true));
        Nebulites.add(new GachaponInfo.GachItem(3064160, true));
        Nebulites.add(new GachaponInfo.GachItem(3064161, true));
        Nebulites.add(new GachaponInfo.GachItem(3064170, true));
        Nebulites.add(new GachaponInfo.GachItem(3064171, true));
        Nebulites.add(new GachaponInfo.GachItem(3064180, true));
        Nebulites.add(new GachaponInfo.GachItem(3064181, true));
        Nebulites.add(new GachaponInfo.GachItem(3064190, true));
        Nebulites.add(new GachaponInfo.GachItem(3064191, true));
        //Nebulites.add(new GachaponInfo.GachItem(3064200, true));
        //Nebulites.add(new GachaponInfo.GachItem(3064201, true));
        //Nebulites.add(new GachaponInfo.GachItem(3064210, true));
        //Nebulites.add(new GachaponInfo.GachItem(3064211, true));
        Nebulites.add(new GachaponInfo.GachItem(3064220, true));
        Nebulites.add(new GachaponInfo.GachItem(3064221, true));
        Nebulites.add(new GachaponInfo.GachItem(3064230, true));
        Nebulites.add(new GachaponInfo.GachItem(3064231, true));
        Nebulites.add(new GachaponInfo.GachItem(3064240, true));
        Nebulites.add(new GachaponInfo.GachItem(3064241, true));
        Nebulites.add(new GachaponInfo.GachItem(3064250, true));
        Nebulites.add(new GachaponInfo.GachItem(3064251, true));
        Nebulites.add(new GachaponInfo.GachItem(3064260, true));
        Nebulites.add(new GachaponInfo.GachItem(3064261, true));
        Nebulites.add(new GachaponInfo.GachItem(3064290, true));
        Nebulites.add(new GachaponInfo.GachItem(3064291, true));
        Nebulites.add(new GachaponInfo.GachItem(3064300, true));
        Nebulites.add(new GachaponInfo.GachItem(3064301, true));
        Nebulites.add(new GachaponInfo.GachItem(3064302, true));
        Nebulites.add(new GachaponInfo.GachItem(3064310, true));
        Nebulites.add(new GachaponInfo.GachItem(3064311, true));
        Nebulites.add(new GachaponInfo.GachItem(3064320, true));
        Nebulites.add(new GachaponInfo.GachItem(3064330, true));
        Nebulites.add(new GachaponInfo.GachItem(3064331, true));
        Nebulites.add(new GachaponInfo.GachItem(3064340, true));
        Nebulites.add(new GachaponInfo.GachItem(3064341, true));
        Nebulites.add(new GachaponInfo.GachItem(3064350, true));
        Nebulites.add(new GachaponInfo.GachItem(3064360, true));
        Nebulites.add(new GachaponInfo.GachItem(3064370, true));
        Nebulites.add(new GachaponInfo.GachItem(3064380, true));
        Nebulites.add(new GachaponInfo.GachItem(3064390, true));
        Nebulites.add(new GachaponInfo.GachItem(3064391, true));
        Nebulites.add(new GachaponInfo.GachItem(3064392, true));
        Nebulites.add(new GachaponInfo.GachItem(3064393, true));
        Nebulites.add(new GachaponInfo.GachItem(3064400, true));
        Nebulites.add(new GachaponInfo.GachItem(3064401, true));
        Nebulites.add(new GachaponInfo.GachItem(3064410, true));
        Nebulites.add(new GachaponInfo.GachItem(3064420, true));
        Nebulites.add(new GachaponInfo.GachItem(3064421, true));
        Nebulites.add(new GachaponInfo.GachItem(3064430, true));
        Nebulites.add(new GachaponInfo.GachItem(3064431, true));
        Nebulites.add(new GachaponInfo.GachItem(3064440, true));
        Nebulites.add(new GachaponInfo.GachItem(3064441, true));
        Nebulites.add(new GachaponInfo.GachItem(3064442, true));
        Nebulites.add(new GachaponInfo.GachItem(3064450, true));
        Nebulites.add(new GachaponInfo.GachItem(3064451, true));
        Nebulites.add(new GachaponInfo.GachItem(3064452, true));
        Nebulites.add(new GachaponInfo.GachItem(3064460, true));
        Nebulites.add(new GachaponInfo.GachItem(3064461, true));
        Nebulites.add(new GachaponInfo.GachItem(3064470, true));
        Nebulites.add(new GachaponInfo.GachItem(3064480, true));
        Nebulites.add(new GachaponInfo.GachItem(3064490, true));
    }

    private static void initChair() {
        Chairs = new ArrayList<>();
        Chairs.add(new GachaponInfo.GachItem(3010000));
        Chairs.add(new GachaponInfo.GachItem(3010001));
        Chairs.add(new GachaponInfo.GachItem(3010002));
        Chairs.add(new GachaponInfo.GachItem(3010003));
        Chairs.add(new GachaponInfo.GachItem(3010004));
        Chairs.add(new GachaponInfo.GachItem(3010005));
        Chairs.add(new GachaponInfo.GachItem(3010006));
        Chairs.add(new GachaponInfo.GachItem(3010007));
        Chairs.add(new GachaponInfo.GachItem(3010008));
        Chairs.add(new GachaponInfo.GachItem(3010009));
        Chairs.add(new GachaponInfo.GachItem(3010010));
        Chairs.add(new GachaponInfo.GachItem(3010011));
        Chairs.add(new GachaponInfo.GachItem(3010012));
        Chairs.add(new GachaponInfo.GachItem(3010013));
        Chairs.add(new GachaponInfo.GachItem(3010014));
        Chairs.add(new GachaponInfo.GachItem(3010015));
        Chairs.add(new GachaponInfo.GachItem(3010016));
        Chairs.add(new GachaponInfo.GachItem(3010017));
        Chairs.add(new GachaponInfo.GachItem(3010018));
        Chairs.add(new GachaponInfo.GachItem(3010019, true));
        Chairs.add(new GachaponInfo.GachItem(3010020, true));
        Chairs.add(new GachaponInfo.GachItem(3010021, true));
        Chairs.add(new GachaponInfo.GachItem(3010024, true));
        Chairs.add(new GachaponInfo.GachItem(3010025, true));
        Chairs.add(new GachaponInfo.GachItem(3010029, true));
        Chairs.add(new GachaponInfo.GachItem(3010030, true));
        Chairs.add(new GachaponInfo.GachItem(3010031, true));
        Chairs.add(new GachaponInfo.GachItem(3010032, true));
        Chairs.add(new GachaponInfo.GachItem(3010033, true));
        Chairs.add(new GachaponInfo.GachItem(3010034, true));
        Chairs.add(new GachaponInfo.GachItem(3010035, true));
        Chairs.add(new GachaponInfo.GachItem(3010036, true));
        Chairs.add(new GachaponInfo.GachItem(3010040, true));
        Chairs.add(new GachaponInfo.GachItem(3010041, true));
        Chairs.add(new GachaponInfo.GachItem(3010043, true));
        Chairs.add(new GachaponInfo.GachItem(3010044, true));
        Chairs.add(new GachaponInfo.GachItem(3010045, true));
        Chairs.add(new GachaponInfo.GachItem(3010046, true));
        Chairs.add(new GachaponInfo.GachItem(3010047, true));
        Chairs.add(new GachaponInfo.GachItem(3010048, true));
        Chairs.add(new GachaponInfo.GachItem(3010049, true));
        Chairs.add(new GachaponInfo.GachItem(3010050, true));
        Chairs.add(new GachaponInfo.GachItem(3010051, true));
        Chairs.add(new GachaponInfo.GachItem(3010052, true));
        Chairs.add(new GachaponInfo.GachItem(3010053, true));
        Chairs.add(new GachaponInfo.GachItem(3010054, true));
        Chairs.add(new GachaponInfo.GachItem(3010055, true));
        Chairs.add(new GachaponInfo.GachItem(3010056, true));
        Chairs.add(new GachaponInfo.GachItem(3010057, true));
        Chairs.add(new GachaponInfo.GachItem(3010058, true));
        Chairs.add(new GachaponInfo.GachItem(3010060, true));
        Chairs.add(new GachaponInfo.GachItem(3010061, true));
        Chairs.add(new GachaponInfo.GachItem(3010062, true));
        Chairs.add(new GachaponInfo.GachItem(3010063, true));
        Chairs.add(new GachaponInfo.GachItem(3010064, true));
        Chairs.add(new GachaponInfo.GachItem(3010065, true));
        Chairs.add(new GachaponInfo.GachItem(3010066, true));
        Chairs.add(new GachaponInfo.GachItem(3010067, true));
        Chairs.add(new GachaponInfo.GachItem(3010068, true));
        Chairs.add(new GachaponInfo.GachItem(3010069, true));
        Chairs.add(new GachaponInfo.GachItem(3010071, true));
        Chairs.add(new GachaponInfo.GachItem(3010072, true));
        Chairs.add(new GachaponInfo.GachItem(3010073, true));
        Chairs.add(new GachaponInfo.GachItem(3010074, true));
        Chairs.add(new GachaponInfo.GachItem(3010075, true));
        Chairs.add(new GachaponInfo.GachItem(3010077, true));
        Chairs.add(new GachaponInfo.GachItem(3010078, true));
        Chairs.add(new GachaponInfo.GachItem(3010079, true));
        Chairs.add(new GachaponInfo.GachItem(3010080, true));
        Chairs.add(new GachaponInfo.GachItem(3010081, true));
        Chairs.add(new GachaponInfo.GachItem(3010082, true));
        Chairs.add(new GachaponInfo.GachItem(3010083, true));
        Chairs.add(new GachaponInfo.GachItem(3010084, true));
        Chairs.add(new GachaponInfo.GachItem(3010085, true));
        Chairs.add(new GachaponInfo.GachItem(3010086, true));
        Chairs.add(new GachaponInfo.GachItem(3010092, true));
        Chairs.add(new GachaponInfo.GachItem(3010093, true));
        Chairs.add(new GachaponInfo.GachItem(3010094, true));
        Chairs.add(new GachaponInfo.GachItem(3010095, true));
        Chairs.add(new GachaponInfo.GachItem(3010096, true));
        Chairs.add(new GachaponInfo.GachItem(3010097, true));
        Chairs.add(new GachaponInfo.GachItem(3010098, true));
        Chairs.add(new GachaponInfo.GachItem(3010099, true));
        Chairs.add(new GachaponInfo.GachItem(3010100, true));
        Chairs.add(new GachaponInfo.GachItem(3010101, true));
        Chairs.add(new GachaponInfo.GachItem(3010102, true));
        Chairs.add(new GachaponInfo.GachItem(3010103, true));
        Chairs.add(new GachaponInfo.GachItem(3010104, true));
        Chairs.add(new GachaponInfo.GachItem(3010105, true));
        Chairs.add(new GachaponInfo.GachItem(3010106, true));
        Chairs.add(new GachaponInfo.GachItem(3010107, true));
        Chairs.add(new GachaponInfo.GachItem(3010108, true));
        Chairs.add(new GachaponInfo.GachItem(3010109, true));
        Chairs.add(new GachaponInfo.GachItem(3010110, true));
        Chairs.add(new GachaponInfo.GachItem(3010111, true));
        Chairs.add(new GachaponInfo.GachItem(3010112, true));
        Chairs.add(new GachaponInfo.GachItem(3010113, true));
        Chairs.add(new GachaponInfo.GachItem(3010114, true));
        Chairs.add(new GachaponInfo.GachItem(3010115, true));
        Chairs.add(new GachaponInfo.GachItem(3010116, true));
        Chairs.add(new GachaponInfo.GachItem(3010117, true));
        Chairs.add(new GachaponInfo.GachItem(3010118, true));
        Chairs.add(new GachaponInfo.GachItem(3010119, true));
        Chairs.add(new GachaponInfo.GachItem(3010120, true));
    }

    private static void initMount() {
        Mounts = new ArrayList<>();
    }

    private static void initSpecial() {
        Specials = new ArrayList<>();
        Specials.add(new GachaponInfo.GachItem(1012261));
        Specials.add(new GachaponInfo.GachItem(1012261));
        Specials.add(new GachaponInfo.GachItem(1012263));
        Specials.add(new GachaponInfo.GachItem(1012264));
        Specials.add(new GachaponInfo.GachItem(1132161));
        Specials.add(new GachaponInfo.GachItem(1132211));
        Specials.add(new GachaponInfo.GachItem(1132212));
        Specials.add(new GachaponInfo.GachItem(1132213));
        Specials.add(new GachaponInfo.GachItem(1132214, true));
        Specials.add(new GachaponInfo.GachItem(1132215, true));
        Specials.add(new GachaponInfo.GachItem(1142204));
        Specials.add(new GachaponInfo.GachItem(1142205));
        Specials.add(new GachaponInfo.GachItem(1142206));
        Specials.add(new GachaponInfo.GachItem(1142207));
        Specials.add(new GachaponInfo.GachItem(1142298));
        Specials.add(new GachaponInfo.GachItem(1142620));
        Specials.add(new GachaponInfo.GachItem(1142802, true));
        Specials.add(new GachaponInfo.GachItem(1142803, true));
        Specials.add(new GachaponInfo.GachItem(1152120));
        Specials.add(new GachaponInfo.GachItem(1152121));
        Specials.add(new GachaponInfo.GachItem(1152122));
        Specials.add(new GachaponInfo.GachItem(1152123, true));
        Specials.add(new GachaponInfo.GachItem(1152124, true));
        Specials.add(new GachaponInfo.GachItem(1190302));
        Specials.add(new GachaponInfo.GachItem(1092084));
        Specials.add(new GachaponInfo.GachItem(1672020)); // Lidium Heart
        Specials.add(new GachaponInfo.GachItem(1662002));
        Specials.add(new GachaponInfo.GachItem(1662003));
        Specials.add(new GachaponInfo.GachItem(1662064));
        Specials.add(new GachaponInfo.GachItem(1662065));
        Specials.add(new GachaponInfo.GachItem(3010002));
        Specials.add(new GachaponInfo.GachItem(3010003));
        Specials.add(new GachaponInfo.GachItem(3010006));
        Specials.add(new GachaponInfo.GachItem(3010007));
        Specials.add(new GachaponInfo.GachItem(3010008));
        Specials.add(new GachaponInfo.GachItem(3010017));
        Specials.add(new GachaponInfo.GachItem(3010016));
        Specials.add(new GachaponInfo.GachItem(3010057));
        Specials.add(new GachaponInfo.GachItem(3010058));
        Specials.add(new GachaponInfo.GachItem(3010065));
        Specials.add(new GachaponInfo.GachItem(3010068));
        Specials.add(new GachaponInfo.GachItem(3010075));
        Specials.add(new GachaponInfo.GachItem(3010079));
        Specials.add(new GachaponInfo.GachItem(3010085));
        Specials.add(new GachaponInfo.GachItem(3010098));
        Specials.add(new GachaponInfo.GachItem(3010110, true));
        Specials.add(new GachaponInfo.GachItem(3010115));
        Specials.add(new GachaponInfo.GachItem(3010122, true));
        Specials.add(new GachaponInfo.GachItem(3010234));
        Specials.add(new GachaponInfo.GachItem(3010235));
        Specials.add(new GachaponInfo.GachItem(3010290, true));
        Specials.add(new GachaponInfo.GachItem(3010301));
        Specials.add(new GachaponInfo.GachItem(3010302));
        Specials.add(new GachaponInfo.GachItem(3010417, true));
        Specials.add(new GachaponInfo.GachItem(3010449));
        Specials.add(new GachaponInfo.GachItem(3010545));
        Specials.add(new GachaponInfo.GachItem(3010670, true));
        Specials.add(new GachaponInfo.GachItem(3010706, true));
        Specials.add(new GachaponInfo.GachItem(3010967));
        Specials.add(new GachaponInfo.GachItem(3012003, true));
        Specials.add(new GachaponInfo.GachItem(3012011));
        Specials.add(new GachaponInfo.GachItem(3012020, true));
        Specials.add(new GachaponInfo.GachItem(3012031, true));
        Specials.add(new GachaponInfo.GachItem(3012032, true));
        Specials.add(new GachaponInfo.GachItem(3015002, true));
        Specials.add(new GachaponInfo.GachItem(3015051, true));
        Specials.add(new GachaponInfo.GachItem(3015102, true));
        Specials.add(new GachaponInfo.GachItem(3015104, true));
        Specials.add(new GachaponInfo.GachItem(3015122));
        Specials.add(new GachaponInfo.GachItem(3015152));
        Specials.add(new GachaponInfo.GachItem(3015182));
        Specials.add(new GachaponInfo.GachItem(3015195));
        Specials.add(new GachaponInfo.GachItem(3015213));
        Specials.add(new GachaponInfo.GachItem(3015215));
        Specials.add(new GachaponInfo.GachItem(3015236, true));
        Specials.add(new GachaponInfo.GachItem(3015238, true));
        Specials.add(new GachaponInfo.GachItem(3015272, true));
        Specials.add(new GachaponInfo.GachItem(3015286));
        Specials.add(new GachaponInfo.GachItem(3015328));
        Specials.add(new GachaponInfo.GachItem(3015431, true));
        Specials.add(new GachaponInfo.GachItem(3015504));
        Specials.add(new GachaponInfo.GachItem(3015505));
        Specials.add(new GachaponInfo.GachItem(3015506));
        Specials.add(new GachaponInfo.GachItem(3015507));
        Specials.add(new GachaponInfo.GachItem(3015508));
        Specials.add(new GachaponInfo.GachItem(3015509));
        Specials.add(new GachaponInfo.GachItem(3015510));
        Specials.add(new GachaponInfo.GachItem(3015511));
        Specials.add(new GachaponInfo.GachItem(3015512, true));
        Specials.add(new GachaponInfo.GachItem(3015552, true));
        Specials.add(new GachaponInfo.GachItem(3015587));
        Specials.add(new GachaponInfo.GachItem(3015586));
        Specials.add(new GachaponInfo.GachItem(3015643));
        Specials.add(new GachaponInfo.GachItem(3015644));
        Specials.add(new GachaponInfo.GachItem(3015665, true));
        Specials.add(new GachaponInfo.GachItem(3015738));
        Specials.add(new GachaponInfo.GachItem(3016101, true));
        Specials.add(new GachaponInfo.GachItem(3015521));
        Specials.add(new GachaponInfo.GachItem(3015526));
        Specials.add(new GachaponInfo.GachItem(3015525));
        Specials.add(new GachaponInfo.GachItem(3015523));
        Specials.add(new GachaponInfo.GachItem(3015541));

        Specials.add(new GachaponInfo.GachItem(3015355));
        Specials.add(new GachaponInfo.GachItem(3015356));
        Specials.add(new GachaponInfo.GachItem(3015357));
        Specials.add(new GachaponInfo.GachItem(3015358));
        Specials.add(new GachaponInfo.GachItem(3015359));
        Specials.add(new GachaponInfo.GachItem(3015560));
        Specials.add(new GachaponInfo.GachItem(3015561));
        Specials.add(new GachaponInfo.GachItem(3015562));
        Specials.add(new GachaponInfo.GachItem(3015563));
        Specials.add(new GachaponInfo.GachItem(3015564));
        Specials.add(new GachaponInfo.GachItem(3015565));
        Specials.add(new GachaponInfo.GachItem(3015566));
        Specials.add(new GachaponInfo.GachItem(3015504));
        Specials.add(new GachaponInfo.GachItem(3015505));
        Specials.add(new GachaponInfo.GachItem(3015506));
        Specials.add(new GachaponInfo.GachItem(3015507));
        Specials.add(new GachaponInfo.GachItem(3015508));
        Specials.add(new GachaponInfo.GachItem(3015509));
        Specials.add(new GachaponInfo.GachItem(3015510));
        Specials.add(new GachaponInfo.GachItem(3015511));
        Specials.add(new GachaponInfo.GachItem(3015512));
        Specials.add(new GachaponInfo.GachItem(3015513));
        Specials.add(new GachaponInfo.GachItem(3015514));
        Specials.add(new GachaponInfo.GachItem(3015722));
        Specials.add(new GachaponInfo.GachItem(3015755));
        Specials.add(new GachaponInfo.GachItem(3015756));
        Specials.add(new GachaponInfo.GachItem(3015787, true));
        Specials.add(new GachaponInfo.GachItem(3015801, true));
        Specials.add(new GachaponInfo.GachItem(3015968));
        Specials.add(new GachaponInfo.GachItem(3015995));
        Specials.add(new GachaponInfo.GachItem(3018017));
        Specials.add(new GachaponInfo.GachItem(3018056));
        Specials.add(new GachaponInfo.GachItem(3018075, true));
        Specials.add(new GachaponInfo.GachItem(3018230, true));
        Specials.add(new GachaponInfo.GachItem(3018272));
        Specials.add(new GachaponInfo.GachItem(3018474));
        Specials.add(new GachaponInfo.GachItem(3015971));
        Specials.add(new GachaponInfo.GachItem(3015971));
        Specials.add(new GachaponInfo.GachItem(3015970));
        Specials.add(new GachaponInfo.GachItem(3016209));
        Specials.add(new GachaponInfo.GachItem(3017000, true));
        Specials.add(new GachaponInfo.GachItem(3017001, true));
        Specials.add(new GachaponInfo.GachItem(3017002, true));
        Specials.add(new GachaponInfo.GachItem(3017003, true));
        Specials.add(new GachaponInfo.GachItem(3017004, true));
        Specials.add(new GachaponInfo.GachItem(3017005, true));
        Specials.add(new GachaponInfo.GachItem(3017006, true));
        Specials.add(new GachaponInfo.GachItem(3017007, true));
        Specials.add(new GachaponInfo.GachItem(3017008, true));
        Specials.add(new GachaponInfo.GachItem(3017009, true));
        Specials.add(new GachaponInfo.GachItem(3017010, true));
        Specials.add(new GachaponInfo.GachItem(3017011, true));
        Specials.add(new GachaponInfo.GachItem(3017012, true));
        Specials.add(new GachaponInfo.GachItem(3017013, true));
        Specials.add(new GachaponInfo.GachItem(3017014, true));
        Specials.add(new GachaponInfo.GachItem(3017015, true));
        Specials.add(new GachaponInfo.GachItem(3017016, true));
        Specials.add(new GachaponInfo.GachItem(3017017, true));
        Specials.add(new GachaponInfo.GachItem(3017018, true));
        Specials.add(new GachaponInfo.GachItem(3017019, true));
        Specials.add(new GachaponInfo.GachItem(3017020, true));
        Specials.add(new GachaponInfo.GachItem(3017021, true));
        Specials.add(new GachaponInfo.GachItem(3017022, true));
        Specials.add(new GachaponInfo.GachItem(3017027, true));
        Specials.add(new GachaponInfo.GachItem(3017028, true));
        Specials.add(new GachaponInfo.GachItem(3017029, true));
        Specials.add(new GachaponInfo.GachItem(3017030, true));
        Specials.add(new GachaponInfo.GachItem(3017031, true));
        Specials.add(new GachaponInfo.GachItem(3017032, true));
        Specials.add(new GachaponInfo.GachItem(3017033, true));
        Specials.add(new GachaponInfo.GachItem(3017034, true));
        Specials.add(new GachaponInfo.GachItem(3017035, true));
        Specials.add(new GachaponInfo.GachItem(3017036, true));
        Specials.add(new GachaponInfo.GachItem(3017037, true));
        Specials.add(new GachaponInfo.GachItem(3017038, true));
        Specials.add(new GachaponInfo.GachItem(3017039, true));
        Specials.add(new GachaponInfo.GachItem(3017040, true));
        Specials.add(new GachaponInfo.GachItem(3017041, true));
        Specials.add(new GachaponInfo.GachItem(3017042, true));
        Specials.add(new GachaponInfo.GachItem(3017043, true));
        Specials.add(new GachaponInfo.GachItem(3017044, true));
        Specials.add(new GachaponInfo.GachItem(3017045, true));
        Specials.add(new GachaponInfo.GachItem(3017046, true));
        Specials.add(new GachaponInfo.GachItem(3017047, true));
        Specials.add(new GachaponInfo.GachItem(3017048, true));
        Specials.add(new GachaponInfo.GachItem(3017049, true));
        Specials.add(new GachaponInfo.GachItem(3017050, true));
        Specials.add(new GachaponInfo.GachItem(3017051, true));
        Specials.add(new GachaponInfo.GachItem(3017052, true));
        Specials.add(new GachaponInfo.GachItem(3017053, true));
        Specials.add(new GachaponInfo.GachItem(3017054, true));
        Specials.add(new GachaponInfo.GachItem(3017055, true));
        Specials.add(new GachaponInfo.GachItem(3017060, true));
        Specials.add(new GachaponInfo.GachItem(3017061, true));
        Specials.add(new GachaponInfo.GachItem(3017062, true));
        Specials.add(new GachaponInfo.GachItem(3017063, true));
        Specials.add(new GachaponInfo.GachItem(3017064, true));
        Specials.add(new GachaponInfo.GachItem(3017065, true));
        Specials.add(new GachaponInfo.GachItem(3017066, true));
        Specials.add(new GachaponInfo.GachItem(3017067, true));
        Specials.add(new GachaponInfo.GachItem(3017069, true));
        Specials.add(new GachaponInfo.GachItem(3017070, true));
        Specials.add(new GachaponInfo.GachItem(3017071, true));
        Specials.add(new GachaponInfo.GachItem(3017072, true));
        Specials.add(new GachaponInfo.GachItem(3017073, true));
    }

    private static String[] initMessages() {
        String[] messages = new String[]{"喵～喵～", "哈哈，是我啦！", "一起來跳舞吧！", "結束囉，各位！"};
        List<String> messagesList = Arrays.asList(messages);
        Collections.shuffle(messagesList);
        messages = messagesList.toArray(new String[messagesList.size()]);
        return messages;
    }

    public static GachaponInfo.GachItem getRandomItem(GachaponDlgType dlg) {
        GachaponInfo gachapon = gachaponInfo.get(dlg);
        if (gachapon == null) {
            return null;
        }
        List<GachaponInfo.GachItem> rewards = gachapon.getRewards();
        if (rewards == null || rewards.size() <= 0) {
            return null;
        }
        List<GachaponInfo.GachItem> hotRewards = rewards.stream().filter(GachaponInfo.GachItem::isHotTime).toList();
        List<GachaponInfo.GachItem> normalRewards = rewards.stream().filter(i -> !i.isHotTime()).toList();
        if (Util.succeedProp(5)) {
            return hotRewards.get(Randomizer.nextInt(hotRewards.size()));
        } else {
            return normalRewards.get(Randomizer.nextInt(normalRewards.size()));
        }
    }

    public static GachaponDlgType getDlgByTicket(final int ticketID) {
        return switch (ticketID) {
            case 5220000 -> GachaponDlgType.TOWN;
            case 5220100 -> GachaponDlgType.SPECIAL;
            case 5451000 -> GachaponDlgType.REMOTE;
            default -> null;
        };
    }
}
