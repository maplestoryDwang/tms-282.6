package net.swordie.ms.console.admin;

import java.io.IOException;
import java.nio.file.Path;

/** Outbound character downloads are unavailable in this local-only login package. */
final class KitDownload {
    private KitDownload() {
    }

    static long fetch(String characterName, Path home, String file) throws IOException, InterruptedException {
        throw new IOException("本機限定登入版停用外部角色資料下載。");
    }
}
