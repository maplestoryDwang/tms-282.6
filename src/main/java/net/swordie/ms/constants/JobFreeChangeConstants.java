package net.swordie.ms.constants;

public class JobFreeChangeConstants {
    //Todo: Do this in dat file.
    public static final int QUEST_EX_ID = 25946;
    public static final String QUEST_EX_STRUCT = "count=%d";

    public static class Coin {
        public static final int ID = 4310086; //Job Advancement Coin.
        public static final int BASE = 5;
        public static final int LEVEL = 105;
        public static final int LEVEL_COIN = 2;
        public static final int LEVEL_CONSTANT = 30;
        public static final int NUMBER = 2;
    }

    public static class Meso {
        public static final int BASE = 10000000; //10m
        public static final int LEVEL = 105; //start at lv 105
        public static final int LEVEL_MONEY = 50000;
        public static final int NUMBER = 2;
    }

    public static int getRequireCoin(int level, int count) {
        return 0;
    }

    public static long getRequireMoney(int level, int count) {
        return 0;
    }
}
