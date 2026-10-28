package net.swordie.ms;

import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.constants.JobConstants;

public class ServerConstants {

    public static final int UnkValue = -1;

    public static final short version = 282;
    public static final String patch = "6";
//    public static final String patch = "5";

    public static final byte WORLD_ID = 19; // Scania
    public static String SERVER_NAME = ServerRuntimeConfig.stringValue("server.name");
    public static final String EVENT_MSG = "MapleStory_" + version + "." + patch;

    public static final String DIR = System.getProperty("user.dir");
    public static final byte LOCALE = 6; // TMS
    // Protocol version and installed data are separate during the 282.3 migration.
    // Only the existing 265 data is present; replace this after exporting 282 data.
    public static final short DATA_VERSION = 265;
    public static final String WZ_DIR = STR."\{DIR}/data/wz\{DATA_VERSION}";
    public static final String DAT_DIR = STR."\{DIR}/data/dat\{DATA_VERSION}";
    public static final int MAX_CHARACTERS = JobConstants.LoginJob.values().length * 3;
    public static final String SCRIPT_DIR = STR."\{DIR}/data/scripts";
    public static final String RESOURCES_DIR = STR."\{DIR}/data/resources";
    public static final String HANDLERS_DIR = STR."\{DIR}/src/main/java/net/swordie/ms/handlers";
    public static int LOGIN_PORT = ServerRuntimeConfig.intValue("login.port", 8484);
    public static int API_PORT = ServerRuntimeConfig.intValue("api.port", 8483);
    public static final byte[] CHANNEL_IP = {127, 0, 0, 1};
    public static final short CHAT_PORT = 0;
    public static final short AUCTION_HOUSE_PORT = 0;
    public static final int BCRYPT_ITERATIONS = 10;
    public static final long TOKEN_EXPIRY_TIME = 60 * 24; // minutes
    public static final int FIELD_DEPRECATION_TIME_IN_MIN = 20;
    public static final boolean LOCAL_HOST_SERVER = true;
    public static final long EXPIRED_SCRIPT_TIME = 3600000L; // 1 giờ
}
