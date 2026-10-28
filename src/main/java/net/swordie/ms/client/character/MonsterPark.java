package net.swordie.ms.client.character;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.*;

public class MonsterPark {

    public static final int entryQuest = 18805; // count=0;date=26/01/24;tryCount=0
    public static final int freeTicket = 4001949;
    public static final int CSTicket = 5252030;
    public static final int CSTicketPrice = 600;
    private static final Int2IntOpenHashMap mobExpHash = new Int2IntOpenHashMap();
    private static final Int2ObjectOpenHashMap<List<MonsterParkReward>> rewards = new Int2ObjectOpenHashMap<>();

    static {
        //region MobExpHash
        mobExpHash.defaultReturnValue(0);
        mobExpHash.put(9800046, 36625); // Roid
        mobExpHash.put(9800047, 39700); // Neo Huroid
        mobExpHash.put(9800045, 42875); // Rurumo
        mobExpHash.put(9800049, 46170); // D. Roy
        mobExpHash.put(9800048, 71730); // Security Camera
        mobExpHash.put(9800050, 97240); // Deet and Roi
        mobExpHash.put(9800051, 53010); // Mossy Snail
        mobExpHash.put(9800052, 56575); // Tree Rod
        mobExpHash.put(9800053, 60225); // Mossy Mushroom
        mobExpHash.put(9800054, 63960); // Primitive Boar
        mobExpHash.put(9800055, 67800); // Stone Bug
        mobExpHash.put(9800056, 215355); // Poison Golem
        mobExpHash.put(9800059, 62080); // Wooden Target Dummy
        mobExpHash.put(9800060, 157185); // Bamboo Warrior
        mobExpHash.put(9800061, 67800); // Grizzly
        mobExpHash.put(9800062, 69730); // Panda
        mobExpHash.put(9800063, 167105); // Tae Roon
        mobExpHash.put(9800064, 76285); // Sage Cat
        mobExpHash.put(9800065, 11580135); // King Sage Cat
        mobExpHash.put(9800066, 241815); // Giant Centipede
        mobExpHash.put(9800150, 71690); // Ginseng Jar
        mobExpHash.put(9800151, 76285); // Bellflower Root
        mobExpHash.put(9800152, 84055); // Mr. Alli
        mobExpHash.put(9800154, 94835); // Kru
        mobExpHash.put(9800153, 193250); // Calico Mack, the Pirate King
        mobExpHash.put(9800155, 97610); // Captain
        mobExpHash.put(9800156, 281085); // Lord Pirate
        mobExpHash.put(9800157, 103125); // Dinogoth
        mobExpHash.put(9800158, 108600); // Dinodon
        mobExpHash.put(9800159, 113960); // Guerrilla Specter
        mobExpHash.put(9800160, 266880); // Magician Specter
        mobExpHash.put(9800161, 119250); // Specter Engineer
        mobExpHash.put(9800162, 124500); // Power Specter
        mobExpHash.put(9800163, 355845); // Reaper Specter
        mobExpHash.put(9800164, 116625); // Harp
        mobExpHash.put(9800165, 121875); // Blood Harp
        mobExpHash.put(9800166, 266880); // Master Harp
        mobExpHash.put(9800167, 127085); // Blue Kentaurus
        mobExpHash.put(9800168, 132225); // Red Kentaurus
        mobExpHash.put(9800170, 288300); // Kentaurus King
        mobExpHash.put(9800169, 137285); // Black Kentaurus
        mobExpHash.put(9800171, 391440); // Griffey
        mobExpHash.put(9800067, 129675); // Dark Klock
        mobExpHash.put(9800068, 134775); // Death Teddy
        mobExpHash.put(9800069, 139800); // Phantom Watch
        mobExpHash.put(9800070, 144785); // Grim Phantom Watch
        mobExpHash.put(9800071, 155475); // Gatekeeper
        mobExpHash.put(9800072, 450135); // Thanatos
        mobExpHash.put(9800172, 142310); // Wild Monkey
        mobExpHash.put(9800173, 150150); // Mama Monkey
        mobExpHash.put(9800174, 160760); // Mean Mama Monkey
        mobExpHash.put(9800175, 348625); // SnowFro the Fruitnificent
        mobExpHash.put(9800176, 171260); // Stone Goblin
        mobExpHash.put(9800177, 176435); // Strong Stone Goblin
        mobExpHash.put(9800178, 508515); // Ganapati
        mobExpHash.put(9800073, 191850); // Overlord A
        mobExpHash.put(9800074, 201975); // Afterlord
        mobExpHash.put(9800075, 445170); // Bergamot
        mobExpHash.put(9800078, 211985); // Maverick Type A
        mobExpHash.put(9800079, 216935); // Maverick Type S
        mobExpHash.put(9800080, 221850); // Imperial Guard
        mobExpHash.put(9800081, 226760); // Royal Guard
        mobExpHash.put(9800082, 662130); // Nibelung
        mobExpHash.put(9800085, 246110); // Werewolf
        mobExpHash.put(9800086, 250875); // Lycanthrope
        mobExpHash.put(9800087, 260360); // Coolie Zombie
        mobExpHash.put(9800088, 269735); // Miner Zombie
        mobExpHash.put(9800089, 279000); // Fire Poison
        mobExpHash.put(9800090, 595585); // Riche
        mobExpHash.put(9800091, 806925); // Ergoth
        mobExpHash.put(9800092, 269735); // Crocky the Gatekeeper
        mobExpHash.put(9800093, 279000); // Reindeer
        mobExpHash.put(9800094, 283610); // Blood Reindeer
        mobExpHash.put(9800095, 288185); // Bearwolf
        mobExpHash.put(9800096, 298010); // Grey Vulture
        mobExpHash.put(9800097, 307835); // Castle Golem
        mobExpHash.put(9800098, 317585); // Prison Guard Boar
        mobExpHash.put(9800099, 943425); // Prison Guard Ani
        mobExpHash.put(9800100, 298010); // Red Dragon Turtle
        mobExpHash.put(9800101, 307835); // Dark Cornian
        mobExpHash.put(9800102, 327260); // Red Wyvern
        mobExpHash.put(9800103, 346500); // Blue Wyvern
        mobExpHash.put(9800104, 365550); // Dark Wyvern
        mobExpHash.put(9800105, 1076355); // Leviathan
        mobExpHash.put(9800106, 346500); // Qualm Monk
        mobExpHash.put(9800107, 365550); // Qualm Guardian
        mobExpHash.put(9800108, 829550); // Dodo
        mobExpHash.put(9800109, 829550); // Lilynouch
        mobExpHash.put(9800110, 375035); // Oblivion Monk Trainee
        mobExpHash.put(9800111, 385910); // Oblivion Guardian
        mobExpHash.put(9800112, 395855); // Chief Oblivion Guardian
        mobExpHash.put(9800113, 1145830); // Lyka
        mobExpHash.put(9800114, 385910); // Official Knight A
        mobExpHash.put(9800115, 390990); // Official Knight B
        mobExpHash.put(9800116, 396280); // Official Knight C
        mobExpHash.put(9800117, 404715); // Official Knight D
        mobExpHash.put(9800118, 412225); // Official Knight E
        mobExpHash.put(9800119, 1186255); // Mihile
        mobExpHash.put(9800120, 1186255); // Oz
        mobExpHash.put(9800121, 588495); // Ifrit
        mobExpHash.put(9800122, 1186255); // Irena
        mobExpHash.put(9800123, 1186255); // Eckhart
        mobExpHash.put(9800124, 1186255); // Hawkeye
        mobExpHash.put(9800179, 417025); // Swollen Stump
        mobExpHash.put(9800180, 419730); // Pillaging Wild Boar
        mobExpHash.put(9800181, 422225); // Pillaging Fire Boar
        mobExpHash.put(9800182, 426565); // Sinister Wooden Mask
        mobExpHash.put(9800183, 428440); // Sinister Rocky Mask
        mobExpHash.put(9800184, 929190); // Ancient Dark Golem
        mobExpHash.put(9800185, 430080); // Swollen Axe Stump
        mobExpHash.put(9800186, 1216460); // Ghostwood Stumpy
        mobExpHash.put(9800187, 421025); // Happy Erda
        mobExpHash.put(9800188, 442730); // Raging Erda
        mobExpHash.put(9800189, 482225); // Sad Erda
        mobExpHash.put(9800190, 482225); // Joyful Erda
        mobExpHash.put(9800191, 482225); // Stone Erda
        mobExpHash.put(9800192, 521625); // Blazing Erda
        mobExpHash.put(9800193, 521625); // Soulful Erda
        mobExpHash.put(9800194, 521625); // Tranquil Erda
        mobExpHash.put(9800195, 679190); // Lantern Erda
        mobExpHash.put(9800196, 679190); // Arma's Follower
        mobExpHash.put(9800197, 1216460); // Arma
        mobExpHash.put(9800198, 421025); // Pinedeer
        mobExpHash.put(9800199, 442730); // Bighorn Pinedeer
        mobExpHash.put(9800200, 482225); // Ewenana
        mobExpHash.put(9800201, 482225); // Ramanana
        mobExpHash.put(9800202, 482225); // Flyon
        mobExpHash.put(9800203, 601625); // Angry Flyon
        mobExpHash.put(9800204, 521625); // Unripe Wolfruit
        mobExpHash.put(9800205, 521625); // Ripe Wolfruit
        mobExpHash.put(9800206, 679190); // Green Catfish
        mobExpHash.put(9800207, 679190); // Blue Catfish
        mobExpHash.put(9800208, 679190); // Rhyturtle
        mobExpHash.put(9800209, 879190); // Boss Rhyturtle
        mobExpHash.put(9800210, 679190); // Crilia
        mobExpHash.put(9800211, 879190); // Patriarch Crilia
        mobExpHash.put(9800212, 679190); // Birdshark
        mobExpHash.put(9800213, 879190); // Patriarch Birdshark
        mobExpHash.put(9800214, 1216460); // Slurpy Tree (Boss)

        mobExpHash.put(9800215, 12873950); // Paper Bag Alley Citizen
        mobExpHash.put(9800216, 13158750); // Wood Bag Alley Citizen
        mobExpHash.put(9800217, 13158750); // Gallina
        mobExpHash.put(9800218, 13414880); // Gallus
        mobExpHash.put(9800219, 13414880); // Angry Victory Plate
        mobExpHash.put(9800220, 13703750); // Crooked Victory Plate
        mobExpHash.put(9800221, 14257150); // Red Dancing Shoes
        mobExpHash.put(9800222, 13703750); // Angry Masquerade Citizen
        mobExpHash.put(9800223, 13964080); // Insane Masquerade Citizen
        mobExpHash.put(9800224, 14553180); // Dreamkeeper
        mobExpHash.put(9800225, 14818590); // Blue-eyed Gargoyle
        mobExpHash.put(9800226, 15118690); // Red-eyed Gargoyle
        mobExpHash.put(9800227, 25999490); // Dreamkeeper (Boss)

        mobExpHash.put(9800228, 17307880); // Water Spirit
        mobExpHash.put(9800229, 17646420); // Sun Spirit
        mobExpHash.put(9800230, 17986230); // Earth Spirit
        mobExpHash.put(9800231, 18290890); // Snow Cloud Spirit
        mobExpHash.put(9800232, 18635850); // Thunder Cloud Spirit
        mobExpHash.put(9800233, 18984200); // Toxic Spirit
        mobExpHash.put(9800234, 19333690); // Volatile Spirit
        mobExpHash.put(9800235, 19646200); // Befuddled Spirit
        mobExpHash.put(9800236, 20000800); // Anguished Spirit
        mobExpHash.put(9800237, 20357480); // Mournful Spirit
        mobExpHash.put(9800238, 20716260); // Discordant Spirit
        mobExpHash.put(9800239, 73531820); // Corrupt Spirit of Harmony (Boss)

        mobExpHash.put(9800240, 26803540); // Xenoroid Echo Type A
        mobExpHash.put(9800241, 26803540); // Xenoroid Echo Type B
        mobExpHash.put(9800242, 27728420); // Powerful Gangster
        mobExpHash.put(9800243, 27728420); // Strong Gangster
        mobExpHash.put(9800244, 28222880); // Blue Shadow
        mobExpHash.put(9800245, 28720280); // Red Shadow
        mobExpHash.put(9800246, 29622460); // Experiment Gone Wrong
        mobExpHash.put(9800247, 29622460); // Big Experiment Gone Wrong
        mobExpHash.put(9800248, 30134380); // Thralled Guard
        mobExpHash.put(9800249, 30589140); // Thralled Warhammer Knight
        mobExpHash.put(9800250, 31107820); // Thralled Wizard
        mobExpHash.put(9800251, 31629180); // Vanishing Erda
        mobExpHash.put(9800275, 145520640); // Thralled General (Boss)

        mobExpHash.put(9800252, 31074400); // Ahtuin
        mobExpHash.put(9800253, 32050540); // Atus
        mobExpHash.put(9800254, 32604400); // Bellalion
        mobExpHash.put(9800255, 33096440); // Bellalis
        mobExpHash.put(9800256, 33657640); // Aranya
        mobExpHash.put(9800257, 34221740); // Aranea
        mobExpHash.put(9800258, 34789000); // Keeper of Light
        mobExpHash.put(9800259, 35359440); // Keeper of Darknes
        mobExpHash.put(9800260, 35934300); // Light Executor
        mobExpHash.put(9800261, 36512340); // Dark Executor
        mobExpHash.put(9800276, 98997670); // Chaos Executor (Boss)

        mobExpHash.put(9800262, 39406840); // Veritate
        mobExpHash.put(9800263, 40060060); // Volar
        mobExpHash.put(9800264, 40716940); // Oceanleli
        mobExpHash.put(9800265, 41378900); // Hyades
        mobExpHash.put(9800266, 42044500); // Denebola
        mobExpHash.put(9800267, 44445340); // Lilli Borea
        mobExpHash.put(9800268, 45147040); // Angelus
        mobExpHash.put(9800277, 209508940); // Adoratio (Boss)

        mobExpHash.put(9800269, 54971860); // Soot Beast
        mobExpHash.put(9800270, 55839760); // Soot Talon
        mobExpHash.put(9800271, 56711900); // Soot Slug
        mobExpHash.put(9800272, 57588460); // Soot Core
        mobExpHash.put(9800273, 58471860); // Crushing Glare
        mobExpHash.put(9800274, 58471860); // Burst Glare
        mobExpHash.put(9800278, 259385600); // Servant of Darkness (Boss)

        mobExpHash.put(9800279, 66934560); // Entangled Fragment
        mobExpHash.put(9800280, 66934560); // Faith Fragment
        mobExpHash.put(9800281, 67942940); // Dark Miscreation
        mobExpHash.put(9800282, 67942940); // Dark Construct
        mobExpHash.put(9800283, 68953440); // Despairing Wing
        mobExpHash.put(9800284, 68953440); // Despairing Blade
        mobExpHash.put(9800285, 70093680); // Silent Knight
        mobExpHash.put(9800286, 70093680); // Silent Scout
        mobExpHash.put(9800287, 70093680); // Silent Rogue
        mobExpHash.put(9800288, 71119640); // Silent Watchman
        mobExpHash.put(9800289, 71119640); // Silent Assassin
        mobExpHash.put(9800290, 71119640); // Despairing Soul
        mobExpHash.put(9800291, 71119640); // Despairing Prisoner
        mobExpHash.put(9800292, 120000000); // Blackheart Negative (Boss)

        mobExpHash.put(9800293, 74870180); // Ancestion
        mobExpHash.put(9800294, 75947520); // Transcendion
        mobExpHash.put(9800295, 77026620); // Ascendion
        mobExpHash.put(9800296, 78113320); // Foreberion
        mobExpHash.put(9800297, 79337180); // Embrion
        mobExpHash.put(9800298, 130000000); // Trinion (Boss)

        mobExpHash.put(9800299, 180310680); // Ebonstar Foot Soldier
        mobExpHash.put(9800300, 180310680); // Ebonstar Archer
        mobExpHash.put(9800301, 180310680); // Monster Gull
        mobExpHash.put(9800302, 182905320); // Fire Spirit
        mobExpHash.put(9800303, 182905320); // Wandering Scholar Ghost
        mobExpHash.put(9800304, 182905320); // Curious Scholar Ghost
        mobExpHash.put(9800305, 185504120); // Flora Foot Soldier
        mobExpHash.put(9800306, 185504120); // Flora Magician
        mobExpHash.put(9800307, 188121220); // Flora Assassin
        mobExpHash.put(9800308, 188121220); // Flora Heavy Infantry
        mobExpHash.put(9800309, 191068760); // Ebonstar Bombardier
        mobExpHash.put(9800310, 191068760); // Ebonstar Magician
        mobExpHash.put(9800311, 384188460); // Flora Commander (Boss)

        mobExpHash.put(9800312, 204668920); // Sandblade Marauder
        mobExpHash.put(9800313, 204668920); // Ironshot Desperado
        mobExpHash.put(9800314, 207479300); // Puxillian Rover
        mobExpHash.put(9800315, 210300420); // Puxillian Scrounger
        mobExpHash.put(9800316, 213486820); // Conductorbot
        mobExpHash.put(9800317, 216340280); // Houndbot
        mobExpHash.put(9800318, 426504900); // Sheriff of Wastes (Boss)

        mobExpHash.put(9800319, 243299100); // Enhanced Corallite Guardian
        mobExpHash.put(9800320, 243299100); // Enhanced Olivine Guardian
        mobExpHash.put(9800321, 243299100); // Enhanced Diamond Guardian
        mobExpHash.put(9800322, 246893040); // Angler Bomb Tester
        mobExpHash.put(9800323, 246893040); // Angler Melee Tester
        mobExpHash.put(9800324, 246893040); // Angler Electro Tester
        mobExpHash.put(9800325, 250106600); // Guard Corundum
        mobExpHash.put(9800326, 250106600); // Guard Agate
        mobExpHash.put(9800327, 253341840); // Disintegrating Test Subject
        mobExpHash.put(9800328, 256999160); // Failed Subject
        mobExpHash.put(9800329, 448835000); // Odium's Guard (Boss)

        //endregion
        //region Rewards
        List<MonsterParkReward> sunday = new ArrayList<>();
        //1.5x EXP Coupon [1 Hour] x 1 for 14 days
        sunday.add(new MonsterParkReward(2023604, 1, 1, 14));
        //2x EXP Coupon [30 Minutes] x 1 for 14 days
        sunday.add(new MonsterParkReward(2450085, 1, 1, -1));
        //2x EXP Coupon [1 Hour] x 1 for 14 days
        sunday.add(new MonsterParkReward(2450083, 1, 1, -1));
        //Badge of Junna x 1
        sunday.add(new MonsterParkReward(1182199, 1, 1, -1));
        rewards.put(2434745, sunday);

        List<MonsterParkReward> monday = new ArrayList<>();
        //Opal Ore x 5 - 30
        monday.add(new MonsterParkReward(4020004, 5, 30, -1));
        //Silver Ore x 5 - 30
        monday.add(new MonsterParkReward(4010004, 5, 30, -1));
        //Orihalcon Ore x 5 - 30
        monday.add(new MonsterParkReward(4010005, 5, 30, -1));
        //Amethyst Ore x 5 - 30
        monday.add(new MonsterParkReward(4020001, 5, 30, -1));
        //Steel Ore x 5 - 30
        monday.add(new MonsterParkReward(4010001, 5, 30, -1));
        //Sapphire Ore x 5 - 30
        monday.add(new MonsterParkReward(4020005, 5, 30, -1));
        //Adamantium Ore x 5 - 30
        monday.add(new MonsterParkReward(4010003, 5, 30, -1));
        //Bronze Ore x 5 - 30
        monday.add(new MonsterParkReward(4010000, 5, 30, -1));
        //Mithril Ore x 5 - 30
        monday.add(new MonsterParkReward(4010002, 5, 30, -1));
        //DEX Crystal Ore x 5 - 30
        monday.add(new MonsterParkReward(4004002, 5, 30, -1));
        //Emerald Ore x 5 - 30
        monday.add(new MonsterParkReward(4020003, 5, 30, -1));
        //Gold Ore x 5 - 30
        monday.add(new MonsterParkReward(4010006, 5, 30, -1));
        //Topaz Ore x 5 - 30
        monday.add(new MonsterParkReward(4020006, 5, 30, -1));
        //Diamond Ore x 5 - 30
        monday.add(new MonsterParkReward(4020007, 5, 30, -1));
        //AquaMarine Ore x 5 - 30
        monday.add(new MonsterParkReward(4020002, 5, 30, -1));
        //Garnet Ore x 5 - 30
        monday.add(new MonsterParkReward(4020000, 5, 30, -1));
        //Power Crystal Ore x 5 - 30
        monday.add(new MonsterParkReward(4004000, 5, 30, -1));
        //Dark Crystal Ore x 5 - 30
        monday.add(new MonsterParkReward(4004004, 5, 30, -1));
        //Black Crystal Ore x 5 - 30
        monday.add(new MonsterParkReward(4020008, 5, 30, -1));
        //Lidium Ore x 5 - 30
        monday.add(new MonsterParkReward(4010007, 5, 30, -1));
        //Wisdom Crystal Ore x 5 - 30
        monday.add(new MonsterParkReward(4004001, 5, 30, -1));
        //LUK Crystal Ore x 5 - 30
        monday.add(new MonsterParkReward(4004003, 5, 30, -1));
        //Marjoram Seed x 5 - 30
        monday.add(new MonsterParkReward(4022000, 5, 30, -1));
        //Marjoram Flower x 5 - 30
        monday.add(new MonsterParkReward(4022001, 5, 30, -1));
        //Lavender Seed x 5 - 30
        monday.add(new MonsterParkReward(4022002, 5, 30, -1));
        //Lavender Flower x 5 - 30
        monday.add(new MonsterParkReward(4022003, 5, 30, -1));
        //Rosemary Seed x 5 - 30
        monday.add(new MonsterParkReward(4022004, 5, 30, -1));
        //Rosemary Flower x 5 - 30
        monday.add(new MonsterParkReward(4022005, 5, 30, -1));
        //Mandarin Seed x 5 - 30
        monday.add(new MonsterParkReward(4022006, 5, 30, -1));
        //Mandarin Flower x 5 - 30
        monday.add(new MonsterParkReward(4022007, 5, 30, -1));
        //Lemon Balm Seed x 5 - 30
        monday.add(new MonsterParkReward(4022008, 5, 30, -1));
        //Lemon Balm Flower x 5 - 30
        monday.add(new MonsterParkReward(4022009, 5, 30, -1));
        //Peppermint Flower x 5 - 30
        monday.add(new MonsterParkReward(4022010, 5, 30, -1));
        //Jasmine Seed x 5 - 30
        monday.add(new MonsterParkReward(4022011, 5, 30, -1));
        //Jasmine Flower x 5 - 30
        monday.add(new MonsterParkReward(4022012, 5, 30, -1));
        //Tea Tree Seed x 5 - 30
        monday.add(new MonsterParkReward(4022013, 5, 30, -1));
        //Tea Tree Flower x 5 - 30
        monday.add(new MonsterParkReward(4022014, 5, 30, -1));
        //Chamomile Seed x 5 - 30
        monday.add(new MonsterParkReward(4022015, 5, 30, -1));
        //Chamomile Flower x 5 - 30
        monday.add(new MonsterParkReward(4022016, 5, 30, -1));
        //Patchouli Seed x 5 - 30
        monday.add(new MonsterParkReward(4022017, 5, 30, -1));
        //Patchouli Flower x 5 - 30
        monday.add(new MonsterParkReward(4022018, 5, 30, -1));
        //Juniper Berry Seed x 5 - 30
        monday.add(new MonsterParkReward(4022019, 5, 30, -1));
        //Juniper Berry Flower x 5 - 30
        monday.add(new MonsterParkReward(4022020, 5, 30, -1));
        //Hyssop Flower x 5 - 30
        monday.add(new MonsterParkReward(4022021, 5, 30, -1));
        //Intermediate Item Crystal x 5 - 30
        monday.add(new MonsterParkReward(4021014, 5, 30, -1));
        //Twisted Time x 1
        monday.add(new MonsterParkReward(4021031, 1, 1, -1));
        //Confusion Fragment x 1
        monday.add(new MonsterParkReward(4021020, 1, 1, -1));
        //Cubic Blade x 1
        monday.add(new MonsterParkReward(4021041, 1, 1, -1));
        //Cubic Chaos Blade x 1
        monday.add(new MonsterParkReward(4021042, 1, 1, -1));
        //Badge of Mano x 1
        monday.add(new MonsterParkReward(1182193, 1, 1, -1));
        rewards.put(2434746, monday);

        List<MonsterParkReward> tuesday = new ArrayList<>();
        //Spell Trace x 25 - 250
        tuesday.add(new MonsterParkReward(4001832, 25, 250, -1));
        //Chaos Scroll of Goodness 50% x 1
        //tuesday.add(new MonsterParkReward(2049122, 1, 1, -1));
        //Incredible Chaos Scroll of Goodness 60% x 1 for 30 days
        //tuesday.add(new MonsterParkReward(2049119, 1, 1, 30));
        //Golden Hammer 100% x 1 for 30 days
        tuesday.add(new MonsterParkReward(5570000, 1, 1, 30));
        //Master Craftsman's Cube x 1 for 30 days
        tuesday.add(new MonsterParkReward(2710002, 1, 1, 30));
        //Meister's Cube x 1 for 30 days
        tuesday.add(new MonsterParkReward(2710003, 1, 1, 30));
        //Unique Potential Scroll 100% x 1 for 30 days
        tuesday.add(new MonsterParkReward(2049740, 1, 1, 30));
        //Badge of Chiu x 1
        tuesday.add(new MonsterParkReward(1182194, 1, 1, -1));
        rewards.put(2434747, tuesday);

        List<MonsterParkReward> wednesday = new ArrayList<>();
        //Hair Wax x 1 - 50
        wednesday.add(new MonsterParkReward(2022740, 1, 50, -1));
        //Carrot Juice x 1 - 50
        wednesday.add(new MonsterParkReward(2022741, 1, 50, -1));
        //Snake Soup x 1 - 50
        wednesday.add(new MonsterParkReward(2022742, 1, 50, -1));
        //Hand Sanitizer x 1 - 50
        wednesday.add(new MonsterParkReward(2022743, 1, 50, -1));
        //Cup of Coffee x 1 - 50
        wednesday.add(new MonsterParkReward(2022744, 1, 50, -1));
        //Cologne x 1 - 50
        wednesday.add(new MonsterParkReward(2022745, 1, 50, -1));
        //Badge of Wodan x 1
        wednesday.add(new MonsterParkReward(1182195, 1, 1, -1));
        rewards.put(2434748, wednesday);

        List<MonsterParkReward> thursday = new ArrayList<>();
        //50 - 3,000 Honor EXP
        thursday.add(new MonsterParkReward(-2, 50, 3000, -1));
        //Badge of Donarr x 1
        thursday.add(new MonsterParkReward(1182196, 1, 1, -1));
        rewards.put(2434749, thursday);

        List<MonsterParkReward> friday = new ArrayList<>();
        //10,000 ~ 50,000,000 mesos
        friday.add(new MonsterParkReward(-1, 10000, 50000000, -1));
        //2x Drop Coupon [1 Hour] x 1
        friday.add(new MonsterParkReward(2023145, 1, 1, -1));
        //Badge of Pruba x 1
        friday.add(new MonsterParkReward(1182197, 1, 1, -1));
        rewards.put(2434750, friday);

        List<MonsterParkReward> saturday = new ArrayList<>();
        //Badge of Saturnus x 1
        saturday.add(new MonsterParkReward(1182198, 1, 1, -1));
        rewards.put(2434751, saturday);
        //endregion
    }

    public static int getExpByMobId(int templateId) {
        return mobExpHash.get(templateId);
    }

    public static boolean isMonsterParkMob(int templateId) {
        return mobExpHash.containsKey(templateId);
    }

    public static int getRewardByDay() {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        switch (day) {
            case Calendar.SUNDAY: // Growth Box
                return 2434745;
            case Calendar.MONDAY: // Crafting Box
                return 2434746;
            case Calendar.TUESDAY: // Enhancement Box
                return 2434747;
            case Calendar.WEDNESDAY: // Traits Box
                return 2434748;
            case Calendar.THURSDAY: // Honor Box
                return 2434749;
            case Calendar.FRIDAY: // Mesos Box
                return 2434750;
            case Calendar.SATURDAY: // Monster Park Box
                return 2434751;
            default:
                return 2434745; // Sunday's Growth Box
        }
    }

    public static List<MonsterParkReward> getRewardListByItemID(int itemID) {
        return rewards.get(itemID);
    }

    public static class MonsterParkReward {

        private int itemID;
        private int minQuantity;
        private int maxQuantity;
        private int day;

        public MonsterParkReward(int itemID, int minQuantity, int maxQuantity, int day) {
            this.itemID = itemID;
            this.minQuantity = minQuantity;
            this.maxQuantity = maxQuantity;
            this.day = day;
        }

        public void setItemID(int itemID) {
            this.itemID = itemID;
        }

        public int getItemID() {
            return itemID;
        }

        public void setMinQuantity(int minQuantity) {
            this.minQuantity = minQuantity;
        }

        public int getMinQuantity() {
            return minQuantity;
        }

        public void setMaxQuantity(int maxQuantity) {
            this.maxQuantity = maxQuantity;
        }

        public int getMaxQuantity() {
            return maxQuantity;
        }

        public void setDay(int day) {
            this.day = day;
        }

        public int getDay() {
            return day;
        }
    }
}
