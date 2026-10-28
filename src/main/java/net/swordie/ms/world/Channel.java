package net.swordie.ms.world;

import net.swordie.ms.Server;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.constants.EventConstants;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.FieldData;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.field.AreaBossInfo;
import net.swordie.ms.world.field.Field;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Channel {
    public final int MAX_SIZE = 30;
    //CHANNELITEM struct
    private final int port;
    private final String name;
    private final int worldId;
    private final int channelId;
    private boolean adultChannel;
    private Map<Integer, Field> fields;
    private final Map<Integer, Map<Integer, Long>> areaBossSpawns = new HashMap<>();

    public Channel(String worldName, int worldId, int channelId) {
        this.name = worldName + "-" + channelId;
        this.worldId = worldId;
        this.channelId = channelId;
        this.adultChannel = false;
        this.port = ServerConstants.LOGIN_PORT + 100 + channelId;
        this.fields = new ConcurrentHashMap<>();
    }

    public long update(long now) {
        long count = 0;
        for (Field field : getFields().values()) {
            count += field.update(now);
        }
        return count;
    }

    public String getName() {
        return name;
    }

    public int getGaugePx() {
        return getChars().size() == 0 ? 1 : getChars().size() * 10;
    }

    public int getWorldId() {
        return worldId;
    }

    public int getChannelId() {
        return channelId;
    }

    public boolean isAdultChannel() {
        return adultChannel;
    }

    public void setAdultChannel(boolean adultChannel) {
        this.adultChannel = adultChannel;
    }

    public int getPort() {
        return port;
    }

    public Map<Integer, Field> getFields() {
        return fields;
    }

    // Sửa hàm getField
    public Field getField(int id) {
        // Sử dụng computeIfAbsent để tìm kiếm và tạo Field mới một cách nguyên tử
        return getFields().computeIfAbsent(id, this::createField);
    }

    // Sửa hàm getFieldIfExists để dùng get() trực tiếp
    public Field getFieldIfExists(int fieldId) {
        return getFields().get(fieldId);
    }

    // Hàm này chỉ tạo Field mới và không thêm vào danh sách
    private Field createField(int id) {
        Field newField = FieldData.getFieldCopyById(id, true);
        if (newField != null) {
            newField.setChannel(getChannelId());
            EventConstants.initReactorDropEvent(newField);
        }
        return newField;
    }

    /**
     * Lets {@code client}'s character {@code characterId} enter channel {@code channelId} once (GMS 265 ticket in
     * the server's {@link net.swordie.ms.world.migration.MigrationRegistry}, which replaced the per-channel
     * transfer map).
     */
    public void addClientInTransfer(byte channelId, int characterId, Client client) {
        var migrations = Server.get().getMigrations();
        migrations.issue(net.swordie.ms.world.migration.MigrationTicket.gms265(characterId, channelId, client, migrations.now()));
    }

    public Set<Char> getChars() {
        return Server.get().getWorld().getCharsByChannelId(getChannelId());
    }

    public Char getCharByName(String name) {
        return getChars().stream()
                .filter(l -> l != null && l.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public void clearCache() {
        // Lấy Iterator từ values() của ConcurrentHashMap
        Iterator<Field> iterator = getFields().values().iterator();
        while (iterator.hasNext()) {
            Field field = iterator.next();
            if (field.getChars().isEmpty()) {
                field.clear();
                // Sử dụng phương thức remove() của Iterator để xóa an toàn
                iterator.remove();
            }
        }
    }

    public void trySpawnAreaBoss(Char chr, int targetFieldId, int curChannelId) {
        AreaBossInfo bossInfo = AreaBossInfo.getByFieldId(targetFieldId);

        if (bossInfo == null) {
            return;
        }
        boolean canSpawn = canWarpAreaBoss(chr, targetFieldId, curChannelId);
        if (canSpawn) {
            areaBossSpawns.putIfAbsent(curChannelId, new HashMap<>());
            areaBossSpawns.get(curChannelId).putIfAbsent(targetFieldId, System.currentTimeMillis() + (long) bossInfo.getRespawnTimeMin() * 60 * 1000);
            if (bossInfo.getBossID() <= 0) {
                return;
            }
            getField(targetFieldId).spawnMob(bossInfo.getBossID(), bossInfo.getSpawnPoint(), false, bossInfo.getHealth());
        } else {
            chr.chatMessage("已經有人在裡面了。");
        }
    }

    public void overrideAreaBossTimer(int targetFieldId, int curChannelId) {
        AreaBossInfo bossInfo = AreaBossInfo.getByFieldId(targetFieldId);
        if (bossInfo == null) {
            return;
        }
        areaBossSpawns.putIfAbsent(curChannelId, new HashMap<>());
        areaBossSpawns.get(curChannelId).putIfAbsent(targetFieldId, System.currentTimeMillis() + (long) bossInfo.getRespawnTimeMin() * 60 * 1000);
    }

    public boolean canWarpAreaBoss(Char chr, int targetFieldId, int curChannelId) {
        try {
            long lastSpawnTime = areaBossSpawns.get(curChannelId).get(targetFieldId);
            if (lastSpawnTime - System.currentTimeMillis() > 0) {
                return false;
            }
        } catch (NullPointerException ex) {
            // no entry found, let it spawn
        }
        return true;
    }

    public boolean tryEnterSilentCrusadePortal(Char chr, int targetFieldId, int curChannelId) {
        if (chr.getOrCreateFieldByCurrentInstanceType(targetFieldId).getChars().size() > 0) { // there is already someone inside
            chr.chatMessage("已經有人在裡面了。");
            return false;
        }
        try {
            long lastSpawnTime = areaBossSpawns.get(curChannelId).get(targetFieldId);
            if (lastSpawnTime - System.currentTimeMillis() > 0) {
                return false;
            }
        } catch (NullPointerException ex) {
            // no entry, they can go
        }
        areaBossSpawns.putIfAbsent(curChannelId, new HashMap<>());
        areaBossSpawns.get(curChannelId).putIfAbsent(targetFieldId, System.currentTimeMillis() + GameConstants.SILENT_CRUSADE_BOSS_COOLDOWN * 60 * 1000);
        return true;
    }

    public void clearUnUsedFields() {
        for (Integer fieldId : fields.keySet()) {
            Field field = fields.get(fieldId);
            if (field == null) {
                continue;
            }
            if (field.getChars().isEmpty() && field.getDeprecationStartTime() == 0) {
                field.setDeprecationStartTime(System.currentTimeMillis());
            }
            else if (!field.getChars().isEmpty() && field.getDeprecationStartTime() != 0) {
                field.setDeprecationStartTime(0);
            }
            boolean shouldRemove = field.getDeprecationStartTime() != 0 &&
                    System.currentTimeMillis() - field.getDeprecationStartTime() >= ServerConstants.FIELD_DEPRECATION_TIME_IN_MIN * 60_000;
            if (shouldRemove) {
                try {
                    field.shutdownField();
                    fields.remove(field.getId());
                } catch (Exception e) {
                    DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                }
            }
        }
    }
}
