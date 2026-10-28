package net.swordie.ms.constants;

public class DevAccounts {

    public static boolean isForbiddenID(String username) {
        return username.contains("admin") || username.contains("dev");
    }

}
