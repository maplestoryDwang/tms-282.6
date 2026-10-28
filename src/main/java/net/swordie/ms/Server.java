package net.swordie.ms;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.GlobalEventExecutor;
import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.avatar.AvatarData;
import net.swordie.ms.client.character.avatar.AvatarLook;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.social.Guild.Guild;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.SharedPacket;
import net.swordie.ms.connection.ServerNetworkBootstrap;
import net.swordie.ms.connection.crypto.MapleCrypto;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.netty.NettyClient;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.console.ConsoleDebugMode;
import net.swordie.ms.console.config.BossRuntimeConfig;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.handlers.GlobalTimerManager;
import net.swordie.ms.handlers.Timer;
import net.swordie.ms.life.mob.skill.ButterFly;
import net.swordie.ms.life.mob.skill.SpiderWeb;
import net.swordie.ms.loaders.*;
import net.swordie.ms.loaders.Etc.Achievement.AchievementInfoData;
import net.swordie.ms.loaders.Etc.Artifact.ArtifactData;
import net.swordie.ms.loaders.Etc.HexaCore.HexaCore;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.*;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.Channel;
import net.swordie.ms.world.World;
import net.swordie.ms.world.shop.CustomShopData;
import net.swordie.ms.world.event.SunnySunday;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Instance;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static net.swordie.ms.ServerConstants.*;
import static net.swordie.ms.connection.netty.NettyClient.CLIENT_KEY;

public class Server {

    public static LocalDateTime startTime;
    private static final Server server = new Server();
    private static World world;
    private List<byte[]> bannedMacs;
    protected List<CharacterStat> rankings = new ArrayList<>();
    protected FileTime lastUpdateRankings;
    private ConcurrentHashMap<String, Tuple<Integer, FileTime>> authTokens = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Tuple<Integer, FileTime>> authTokensForx64 = new ConcurrentHashMap<>();
    public ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private int instanceId = 0;
    private int fieldId = 0;
    protected Timer serverTimer;
    protected Timer eventTimer;
    protected Timer lifeTimer;
    protected Timer fieldTimer;
    protected Timer charTimer;
    protected Timer pingTimer;
    private ConcurrentHashMap<Integer, Char> chars = new ConcurrentHashMap<>();
    private List<Instance> instances = new ArrayList<>();
    private ConcurrentHashMap<Integer, Char> bots = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Tuple<Integer, Byte>> switchChars = new ConcurrentHashMap<>();
    public Char bot;
    private final net.swordie.ms.world.migration.MigrationRegistry migrations = new net.swordie.ms.world.migration.MigrationRegistry();

    public static Server get() {
        return server;
    }

    public World getWorld() {
        return world;
    }

    public ChannelGroup getChannels() {
        return channels;
    }

    public List<byte[]> getBannedMacs() {
        return bannedMacs;
    }

    public void setBannedMacs(List<byte[]> bannedMacs) {
        this.bannedMacs = bannedMacs;
    }

    public Timer getServerTimer() {
        return serverTimer;
    }

    public Timer getEventTimer() {
        return eventTimer;
    }

    public Timer getLifeTimer() {
        return lifeTimer;
    }

    public Timer getFieldTimer() {
        return fieldTimer;
    }

    public Timer getCharTimer() {
        return charTimer;
    }

    public Timer getPingTimer() {
        return pingTimer;
    }

    public Set<Client> getClients() {
        Set<Client> clients = new HashSet<>();
        for (io.netty.channel.Channel channel : getChannels()) {
            Client c = (Client) channel.attr(CLIENT_KEY).get();
            if (c != null) {
                clients.add(c);
            }
        }
        return clients;
    }

    public void shutdownOnlineClients() {
        try {
            saveAndDisconnectOnlineClients();
        } finally {
            try {
                // [eventfw] after every session was saved (finalizeSession -> dispose -> EventManager.onDisconnect)
                net.swordie.ms.scripts.event.EventManager.get().destroyAll();
            } catch (RuntimeException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
    }

    private void saveAndDisconnectOnlineClients() {
        Set<Client> onlineClients = getClients();
        if (onlineClients.isEmpty()) {
            System.out.println("[Shutdown] No online client needs saving.");
            return;
        }
        int threads = Math.max(1, Math.min(onlineClients.size(), ServerLoadExecutor.startupThreads()));
        ExecutorService savePool = Executors.newFixedThreadPool(threads, runnable -> {
            Thread thread = new Thread(runnable, "GMS265-ShutdownSave");
            thread.setDaemon(false);
            return thread;
        });
        CountDownLatch latch = new CountDownLatch(onlineClients.size());
        System.out.printf("[Shutdown] Saving and disconnecting %d online client(s) with %d thread(s).%n", onlineClients.size(), threads);
        for (Client client : onlineClients) {
            savePool.execute(() -> {
                try {
                    client.finalizeSession();
                } catch (Exception e) {
                    DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                } finally {
                    try {
                        client.getCh().close().syncUninterruptibly();
                    } catch (Exception ignored) {
                    }
                    latch.countDown();
                }
            });
        }
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            savePool.shutdown();
            try {
                if (!savePool.awaitTermination(10, TimeUnit.SECONDS)) {
                    savePool.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                savePool.shutdownNow();
            }
        }
        channels.close().awaitUninterruptibly(10, TimeUnit.SECONDS);
        System.out.println("[Shutdown] Online clients have been saved and disconnected.");
    }

    public Map<Integer, Char> getChars() {
        return chars;
    }

    public Map<Integer, Tuple<Integer, Byte>> getSwitchChars() {
        return switchChars;
    }

    public Tuple<Integer, Byte> getSwitchCharID(int userID) {
        for (Map.Entry<Integer, Tuple<Integer, Byte>> entry : switchChars.entrySet()) {
            if (entry.getKey() == userID) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void addChar(Char chr) {
        if (chr != null) {
            chars.put(chr.getId(), chr);
        }
    }

    public void removeChar(int charID) {
        chars.remove(charID);
    }

    /**
     * 清理賬號下已過期、可安全下線的在線角色。
     * 不會打斷現金商城/拍賣行/換頻道遷移（CHANGING_CHANNEL）。
     */
    public void finalizeOnlineCharsForAccount(Account acc, boolean notifyDisconnect) {
        if (acc == null || world == null) {
            return;
        }
        int accId = acc.getId();
        for (Char onlineChr : new java.util.ArrayList<>(getChars().values())) {
            if (onlineChr == null || onlineChr.getAccId() != accId) {
                continue;
            }
            Client onlineClient = onlineChr.getClient();
            if (onlineClient != null) {
                if (onlineClient.isMigrating()) {
                    continue;
                }
                onlineClient.finalizeSession(notifyDisconnect);
            } else {
                removeChar(onlineChr.getId());
            }
        }
    }

    /**
     * 清理賬號在頻道遷移表中的過期條目，保留進行中的 CHANGING_CHANNEL 遷移。
     */
    public void clearAccountTransfers(Account acc) {
        if (acc == null || world == null) {
            return;
        }
        java.util.Set<Integer> charIds = new java.util.HashSet<>();
        for (Char chr : acc.getCharacters()) {
            charIds.add(chr.getId());
        }
        migrations.discardIf(ticket -> charIds.contains(ticket.characterId())
                && !(ticket.origin() != null && ticket.origin().isMigrating()));
    }

    /**
     * 登錄服選角前：清理同賬號殘留會話，且不影響進行中的頻道遷移。
     */
    public void prepareAccountForCharacterLogin(Account acc) {
        finalizeOnlineCharsForAccount(acc, false);
        clearAccountTransfers(acc);
    }

    /**
     * 若目標角色仍殘留在世界在線列表中，清理後再走 MIGRATE_IN 首次登錄流程。
     */
    public void finalizeStaleCharSession(int charId, Client migratingClient) {
        Char existing = world != null ? world.getCharById(charId) : null;
        if (existing == null) {
            return;
        }
        Client existingClient = existing.getClient();
        if (existingClient == null) {
            removeChar(charId);
            return;
        }
        if (existingClient == migratingClient || existingClient.isMigrating()) {
            return;
        }
        existingClient.finalizeSession(false);
    }

    public Map<Integer, Char> getBots() {
        return bots;
    }

    public final Set<User> getUsers() {
        Set<User> users = new HashSet<>();
        for (Client c : getClients()) {
            if (c != null && c.getUser() != null) {
                users.add(c.getUser());
            }
        }
        return users;
    }

    public final User getUserById(int id) {
        User user = null;
        for (Client c : getClients()) {
            if (c != null && c.getUser() != null && c.getUser().getId() == id) {
                user = c.getUser();
            }
        }
        return user;
    }

    public boolean isUserLoggedIn(User user) {
        for (User currentUser : getUsers()) {
            if (currentUser != null && currentUser.getId() == user.getId()) {
                return true;
            }
        }
        return false;
    }

    public Set<Char> getBotsByFieldId(int fieldSerialNumberId) {
        return Server.get().getBots().values().stream()
                .filter(chr -> chr.getField() != null && chr.getField().getSN() == fieldSerialNumberId)
                .collect(Collectors.toSet());
    }

    public Set<Char> getCharsByFieldId(int fieldSerialNumberId) {
        return Server.get().getChars().values().stream()
                .filter(chr -> chr.getField() != null && chr.getField().getSN() == fieldSerialNumberId)
                .collect(Collectors.toSet());
    }

    public Set<Char> getCharsByGuildId(int guildID) {
        return Server.get().getChars().values().stream()
                .filter(chr -> chr.getGuild() != null && chr.getGuild().getId() == guildID)
                .collect(Collectors.toSet());
    }

    public Set<Char> getCharsByPartyId(int partyID) {
        return Server.get().getChars().values().stream()
                .filter(chr -> chr.getParty() != null && chr.getParty().getId() == partyID)
                .collect(Collectors.toSet());
    }

    public void broadcast(OutPacket packet) {
        packet = net.swordie.ms.connection.protocol.codec.LegacyPacketTranslator.apply(packet); // 265: unchanged
        if (!Protocol.active().outboundGuard().permit(packet, () -> "broadcast:all")) {
            return;
        }
        if (packet.getLength() > NettyClient.MAX_PACKET_SIZE) {
            DataPrinter.send(DataPrinter.PACKET_LOG_ERR, String.format("Packet is too big! ([Out] %d size = %d)",
                    packet.getHeader(), packet.getLength()), false);
            return;
        }
        final byte[] data = packet.getFrameData();
        for (io.netty.channel.Channel channel : getChannels()) {
            channel.writeAndFlush(new SharedPacket(data, packet.hasAuxiliaryData()));
        }
        NettyClient.debug("All", packet);
    }

    public void broadcast(OutPacket packet, Set<io.netty.channel.Channel> channels, String name) {
        if (channels.isEmpty()) {
            return;
        }
        packet = net.swordie.ms.connection.protocol.codec.LegacyPacketTranslator.apply(packet); // 265: unchanged
        if (!Protocol.active().outboundGuard().permit(packet, () -> "broadcast:" + name)) {
            return;
        }
        if (packet.getLength() > NettyClient.MAX_PACKET_SIZE) {
            DataPrinter.send(DataPrinter.PACKET_LOG_ERR, String.format("Packet is too big! ([Out] %d size = %d)",
                    packet.getHeader(), packet.getLength()), false);
            return;
        }
        final byte[] data = packet.getFrameData();
        if (channels.size() == 1) {
            io.netty.channel.Channel channel = channels.iterator().next();
            Client client = (Client) channel.attr(CLIENT_KEY).get();
            if (client != null) {
                client.write(packet);
            }
        } else {
            for (io.netty.channel.Channel channel : channels) {
                channel.writeAndFlush(new SharedPacket(data, packet.hasAuxiliaryData()));
            }
            NettyClient.debug(name, packet);
        }
    }

    public void broadcastForAH(OutPacket packet) {
        packet = net.swordie.ms.connection.protocol.codec.LegacyPacketTranslator.apply(packet); // 265: unchanged
        if (!Protocol.active().outboundGuard().permit(packet, () -> "broadcast:auction")) {
            return;
        }
        if (packet.getLength() > NettyClient.MAX_PACKET_SIZE) {
            DataPrinter.send(DataPrinter.PACKET_LOG_ERR, String.format("Packet is too big! ([Out] %d size = %d)",
                    packet.getHeader(), packet.getLength()), false);
            return;
        }
        final byte[] data = packet.getFrameData();
        for (io.netty.channel.Channel channel : getChannels()) {
            Client client = (Client) channel.attr(CLIENT_KEY).get();
            if (client.getCurrentState() == Client.IN_AUCTION_HOUSE) {
                channel.writeAndFlush(new SharedPacket(data, packet.hasAuxiliaryData()));
            }
        }
        NettyClient.debug("AuctionHouse", packet);
    }

    public void broadcastForWorld(OutPacket packet) {
        packet = net.swordie.ms.connection.protocol.codec.LegacyPacketTranslator.apply(packet); // 265: unchanged
        if (!Protocol.active().outboundGuard().permit(packet, () -> "broadcast:world")) {
            return;
        }
        if (packet.getLength() > NettyClient.MAX_PACKET_SIZE) {
            DataPrinter.send(DataPrinter.PACKET_LOG_ERR, String.format("Packet is too big! ([Out] %d size = %d)",
                    packet.getHeader(), packet.getLength()), false);
            return;
        }
        final byte[] data = packet.getFrameData();
        for (io.netty.channel.Channel channel : getChannels()) {
            Client client = (Client) channel.attr(CLIENT_KEY).get();
            if (client.getCurrentState() == Client.IN_FIELD) {
                channel.writeAndFlush(new SharedPacket(data, packet.hasAuxiliaryData()));
            }
        }
        NettyClient.debug("World", packet);
    }

    public void broadcastForField(OutPacket packet, Field field, Char exceptChar) {
        if (field == null) {
            return;
        }
        Set<io.netty.channel.Channel> channelsInField = new HashSet<>();
        Set<Char> charsInField = getCharsByFieldId(field.getSN());
        for (Char chr : charsInField) {
            if (chr == null || chr.getClient() == null) {
                continue;
            }
            if (exceptChar == null) {
                channelsInField.add(chr.getClient().getCh());
            } else if (chr.getId() != exceptChar.getId()) {
                channelsInField.add(chr.getClient().getCh());
            }
        }
        broadcast(packet, channelsInField, "Fields");
    }

    public void broadcastForGuild(OutPacket packet, Guild guild, Char exceptChar) {
        if (guild == null) {
            return;
        }
        Set<io.netty.channel.Channel> channelsInGuild = new HashSet<>();
        Set<Char> charsInGuild = getCharsByGuildId(guild.getId());
        for (Char chr : charsInGuild) {
            if (chr == null || chr.getClient() == null) {
                continue;
            }
            if (exceptChar == null) {
                channelsInGuild.add(chr.getClient().getCh());
            } else if (chr.getId() != exceptChar.getId()) {
                channelsInGuild.add(chr.getClient().getCh());
            }
        }
        broadcast(packet, channelsInGuild, "Guild_" + guild.getName());
    }

    public void broadcastForParty(OutPacket packet, Party party, Char exceptChar) {
        if (party == null) {
            return;
        }
        Set<io.netty.channel.Channel> channelsInParty = new HashSet<>();
        Set<Char> charsInParty = getCharsByPartyId(party.getId());
        for (Char chr : charsInParty) {
            if (chr == null || chr.getClient() == null) {
                continue;
            }
            if (exceptChar == null) {
                channelsInParty.add(chr.getClient().getCh());
            } else if (chr.getId() != exceptChar.getId()) {
                channelsInParty.add(chr.getClient().getCh());
            }
        }
        broadcast(packet, channelsInParty, "Parties");
    }

    public List<CharacterStat> getRankings() {
        return rankings;
    }

    public void setRankings(List<CharacterStat> rankings) {
        this.rankings = rankings;
    }

    public FileTime getLastUpdateRankings() {
        return lastUpdateRankings;
    }

    public int getInstanceIdAndIncrement() {
        return instanceId++;
    }

    public int getFieldIdAndIncrement() {
        return fieldId++;
    }

    public static void main(String[] args) {
//        net.swordie.ms.lifecycle.SignaturePolicy.verifyRunningArtifact();
        get().init();
    }

    private synchronized void init() {
        ServerRuntimeConfig.reload();
        ServerRuntimeConfig.applyToRuntime();
        BossRuntimeConfig.reload();
        System.out.printf("Starting %s - ver:%d.%s at %s.%n", ServerConstants.SERVER_NAME, version, patch, FileTime.currentTime().toYYYYMMDD_HHMMSS());
        DatabaseManager.init();
        CustomShopData.load();
        MapleCrypto.initialize(version);
        initTimers();
        net.swordie.ms.lifecycle.ServerLifecycle.installShutdownHook(); // step 2: save everyone on System.exit / Ctrl+C
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
        System.out.printf("[Scripting] Script engine name : %s%n", ScriptManagerImpl.SCRIPT_ENGINE_NAME);
        try {
            FastStartup.logCacheStatus();
            if (!FastStartup.isEnabled() || !FastStartup.isDatCacheReady()) {
                checkAndCreateDat();
            }
            if (FastStartup.isEnabled()) {
                initWithFastStartup();
            } else {
                finishStartupAfterGameData();
            }
            net.swordie.ms.connection.api.HttpAuthApi.start();
            net.swordie.ms.lifecycle.ServerLifecycle.startAfterNetwork(); // step 2: periodic save + stop-file watcher
        } catch (Exception e) {
            e.printStackTrace();
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            System.exit(1);
        }
    }

    /**
     * 快速啓動：與串行相同的數據加載順序，同時重疊 World 初始化、網絡綁定與封禁 MAC 查詢。
     */
    private void initWithFastStartup() throws Exception {
        AtomicReference<Throwable> dataError = new AtomicReference<>();
        CountDownLatch dataLatch = new CountDownLatch(1);
        Thread dataThread = new Thread(() -> {
            try {
                loadAllGameData();
            } catch (Throwable t) {
                dataError.set(t);
            } finally {
                dataLatch.countDown();
            }
        }, "GameDataLoad");
        dataThread.setDaemon(false);
        dataThread.start();

        initWorlds();
        World boundWorld = world;

        AtomicReference<Throwable> networkError = new AtomicReference<>();
        CountDownLatch networkLatch = new CountDownLatch(1);
        Thread networkThread = new Thread(() -> {
            try {
                ServerNetworkBootstrap.startAllAndAwait(boundWorld);
            } catch (Throwable t) {
                networkError.set(t);
            } finally {
                networkLatch.countDown();
            }
        }, "NetworkBootstrap");
        networkThread.setDaemon(false);
        networkThread.start();

        AtomicReference<List<byte[]>> bannedMacsRef = new AtomicReference<>();
        AtomicReference<Throwable> bannedError = new AtomicReference<>();
        Thread bannedThread = new Thread(() -> {
            try {
                bannedMacsRef.set(BannedMachines.getBannedMachinesFromSQL());
            } catch (Throwable t) {
                bannedError.set(t);
            }
        }, "BannedMacsLoad");
        bannedThread.setDaemon(true);
        bannedThread.start();

        dataLatch.await();
        if (dataError.get() != null) {
            throw asException(dataError.get());
        }

        addPhantomBot();
        startTime = LocalDateTime.now();

        networkLatch.await();
        if (networkError.get() != null) {
            throw asException(networkError.get());
        }

        bannedThread.join(30_000L);
        if (bannedThread.isAlive()) {
            throw new IllegalStateException("封禁 MAC 列表加載超時");
        }
        if (bannedError.get() != null) {
            throw asException(bannedError.get());
        }
        setBannedMacs(bannedMacsRef.get());
    }

    private void loadAllGameData() throws Exception {
        loadWzData();
        loadJson();
        if (!ServerConfig.DEBUG_MODE) {
            ItemData.loadFacesAndHairs();
        }
    }

    private void finishStartupAfterGameData() throws Exception {
        loadAllGameData();
        initWorlds();
        addPhantomBot();
        startTime = LocalDateTime.now();
        setBannedMacs(BannedMachines.getBannedMachinesFromSQL());
        ServerNetworkBootstrap.startAllAndAwait(world);
    }

    private static Exception asException(Throwable t) throws Exception {
        if (t instanceof Exception e) {
            throw e;
        }
        if (t instanceof Error err) {
            throw err;
        }
        throw new Exception(t);
    }

    public void addPhantomBot() {
        if (!ScriptManagerImpl.GAME_SCRIPTS_ENABLED) {
            System.out.println("[Server] Phantom gameplay bot skipped in login-only mode");
            return;
        }
        Position pos = new Position(945, 94);
        Char phantom = Char.getCharDataByID(5);
        if (phantom == null) {
            phantom = new Char(0, "CướpSkill", 0, 0, 112, (short) 0, (byte) 0, (byte) 0, 0, 0, new int[]{});
            phantom.setId(5);
            System.out.println("[Server] Phantom bot character not found in database, created in-memory (id=5)");
        } else {
            phantom.loadCharacterData();
            phantom.loadCharacterGuildData();
        }
        if (phantom.getAvatarData() == null) {
            phantom.setAvatarData(new AvatarData());
            phantom.getAvatarData().setAvatarLook(new AvatarLook());
            phantom.getAvatarData().setCharacterStat(new CharacterStat("CướpSkill", 112));
        }
        for (SkillInfo skillInfo : SkillData.getLoadedSkillInfos()) {
            Skill skill = SkillData.getSkillDeepCopyById(skillInfo.getSkillId());
            if (skill != null) {
                skill.setCurrentLevel(skill.getMaxLevel());
                phantom.putSkill(skill);
            }
        }
        Account account = Account.getAccountFromSQLByUserID(3);
        if (account != null) {
            phantom.setAccount(account);
        }
        User user = User.getUserFromSQLByID(3);
        if (user != null) {
            phantom.setUser(user);
        }
        phantom.setPosition(pos);
        phantom.setMoveAction((byte) 5);
        phantom.setFoothold((short) 268);
        phantom.setFieldID(FieldConstants.HENESYS_ID);
        phantom.setBot(true);
        phantom.getAvatarData().getCharacterStat().setJob(112);
        phantom.getAvatarData().getAvatarLook().setJob(112);
        phantom.getAvatarData().getCharacterStat().setLevel(300);
        phantom.setName("CướpSkill");
        bot = phantom;
    }

    public void initTimers() {
        long startNow = System.currentTimeMillis();
        try {
            this.serverTimer = new Timer("Server");
            this.eventTimer = new Timer("Event");
            this.lifeTimer = new Timer("Life");
            this.fieldTimer = new Timer("Field");
            this.charTimer = new Timer("Character");
            this.pingTimer = new Timer("Ping");
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            System.exit(1);
            return;
        }
        this.serverTimer.addFixedRateEvent(() -> {
            for (Map.Entry<Integer, CopyOnWriteArrayList<ScheduledFuture<?>>> entry : GlobalTimerManager.managedCharacterTimers.entrySet()) {
                if (Server.get().getWorld().getCharById(entry.getKey()) == null) {
                    for (ScheduledFuture<?> timer : entry.getValue()) {
                        if (timer != null) {
                            timer.cancel(true);
                        }
                    }
                    GlobalTimerManager.managedCharacterTimers.remove(entry.getKey());
                    System.out.println("[Cleaner] 已清理不存在角色的定時器: " + entry.getKey());
                }
            }
            for (Map.Entry<Integer, CopyOnWriteArrayList<ScheduledFuture<?>>> entry : GlobalTimerManager.managedFieldTimers.entrySet()) {
                if (Server.get().getCharsByFieldId(entry.getKey()).isEmpty()) {
                    for (ScheduledFuture<?> timer : entry.getValue()) {
                        if (timer != null) {
                            timer.cancel(true);
                        }
                    }
                    GlobalTimerManager.managedFieldTimers.remove(entry.getKey());
                    System.out.println("[Cleaner] 已清理無玩家地圖的定時器: " + entry.getKey());
                }
            }
        }, 30, 30, TimeUnit.MINUTES, true);
        this.serverTimer.addFixedRateEvent(() -> {
            long count = 0;
            final long now = System.currentTimeMillis();
            for (Channel channel : getWorld().getChannels()) {
                count += channel.update(now);
            }
            for (Instance instance : getInstances()) {
                for (Field field : instance.getFields().values()) {
                    count += field.update(now);
                }
            }
            for (Char chr : getChars().values()) {
                chr.update(now);
            }
            if (count > 0 && ConsoleDebugMode.isActive()) {
                System.out.println("[Cleaner] 已清理各地圖中 " + count + " 個掉落物品。");
            }
        }, 5000, 500, TimeUnit.MILLISECONDS, true);
        if (ScriptManagerImpl.GAME_SCRIPTS_ENABLED)
            net.swordie.ms.world.event.hottime.HotTimeScheduler.start(this.eventTimer); // TMS 273 import: HotTime + 排程公告 (每分鐘)
        net.swordie.ms.world.online.OnlinePopulationLogger.start(this.eventTimer); // TMS 273 import: logs/online/online-YYYY-MM-DD.csv (每 10 分鐘)
        if (ScriptManagerImpl.GAME_SCRIPTS_ENABLED)
            net.swordie.ms.world.online.OnlineRewardService.start(this.eventTimer); // leftovers-1005: 累積上線獎勵（每分鐘，data/tms282-online-reward/online-reward.json）
        //clearUnUsedFields();
        System.out.printf("[Timer] Loaded Timers in %d ms%n", System.currentTimeMillis() - startNow);
    }

    public void clearUnUsedFields() {
        long initialDelay = 15;
        long delay = 15;
        TimeUnit timeUnit = TimeUnit.MINUTES;
        this.serverTimer.addFixedRateEvent(() -> {
            System.out.println("[Cleaning System] Start cleanup of all the unused Fields... |>");
            long startTime = System.currentTimeMillis();
            Server.get().getWorld().clearUnUsedFields();
            System.out.println("[Cleaning System] Finished Fields cleanup! took " + (System.currentTimeMillis() - startTime) + "ms ~");
        }, initialDelay, delay, timeUnit, true);
        System.out.println("Started cleanup system!");
    }

    private void initWorlds() {
        world = new World(ServerConstants.WORLD_ID, ServerConstants.SERVER_NAME, GameConstants.CHANNELS_PER_WORLD, ServerConstants.EVENT_MSG);
        world.initGuilds();
        world.initAuctionHouse();
    }

    private void checkAndCreateDat() {
        File equipDir = new File(ServerConstants.DAT_DIR, "equips");
        if (equipDir.exists()) {
            return;
        }
        System.out.println("Dat files cannot be found (at least not the equip dats). All dats will now be generated. This may take a long while.");
        Util.makeDirIfAbsent(ServerConstants.DAT_DIR);
        for (Class<?> creator : DataClasses.datCreators) {
            try {
                Method m = creator.getMethod("generateDatFiles");
                m.invoke(null);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
    }

    public void loadJson() throws Exception {
        long start = System.currentTimeMillis();
        DropData.load();
        SunnySunday.load();
        long total = System.currentTimeMillis() - start;
        System.out.printf("[JSON Data] Finished Loaded Json data in %dms%n", total);
    }

    public void loadWzData() throws IllegalAccessException, InvocationTargetException {
        String datFolder = ServerConstants.DAT_DIR;
        long start = System.currentTimeMillis();
        for (Class<?> dataClass : DataClasses.dataClasses) {
            for (Method method : dataClass.getMethods()) {
                Loader annotation = method.getAnnotation(Loader.class);
                if (annotation == null) {
                    continue;
                }
                File file = new File(datFolder, annotation.varName() + ".dat");
                try {
                    method.invoke(null, file, file.exists());
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    throw new InvocationTargetException(cause, "Failed to load " + file.getPath() + " via " + dataClass.getSimpleName() + "." + method.getName());
                }
            }
        }
        loadWzDataPostSteps();
        long total = System.currentTimeMillis() - start;
        System.out.printf("[WZ Data] Finished Loaded WZ data in %dms%n", total);
    }

    private void loadWzDataPostSteps() throws InvocationTargetException {
        MobData.loadNormalMobsFromWz();
        ContentsGuide.load();
        StringData.load();
        ButterFly.load();
        FieldData.loadWorldMap();
        FieldData.loadNPCFromSQL();
        AchievementInfoData.load();
        SkillData.loadSkillsFromWz();
        SkillData.loadSkillStatData();
        SkillOverrideCache.applyAllToMemory();
        HexaCore.load();
        ArtifactData.load();
        SpiderWeb.load();
        if (!ServerConfig.DEBUG_MODE) {
            try {
                ItemData.loadItemsFromWZ();
                ItemData.loadEquipsFromWz();
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
        }
    }

    /** Pending channel migrations (MIGRATE_IN tickets) of every channel; replaces the former per-channel maps. */
    public net.swordie.ms.world.migration.MigrationRegistry getMigrations() {
        return migrations;
    }

    /** The character's pending (unexpired, unclaimed) migration ticket, or null. */
    public net.swordie.ms.world.migration.MigrationTicket getChannelFromTransfer(int charId) {
        return migrations.peek(charId).orElse(null);
    }

    private Map<String, Tuple<Integer, FileTime>> getAuthTokens() {
        return authTokens;
    }

    public void addAuthToken(byte[] token, int userID) {
        String tokenStr = new String(token);
        FileTime expiryDate = FileTime.fromDate(LocalDateTime.now().plusMinutes(ServerConstants.TOKEN_EXPIRY_TIME));
        Tuple<Integer, FileTime> entry = new Tuple<>(userID, expiryDate);
        getAuthTokens().put(tokenStr, entry);
    }

    public int getUserIdFromAuthToken(String token) {
        Tuple<Integer, FileTime> value = getAuthTokens().getOrDefault(token, null);
        if (value == null || value.getRight() == null || value.getRight().isExpired()) {
            return 0;
        } else {
            return value.getLeft();
        }
    }

    private Map<Integer, Tuple<Integer, FileTime>> getAuthTokensForx64() {
        return authTokensForx64;
    }

    public boolean isTokenExist(int token) {
        return getAuthTokensForx64().containsKey(token);
    }

    public void addAuthToken(int token, int userID) {
        FileTime expiryDate = FileTime.fromDate(LocalDateTime.now().plusMinutes(ServerConstants.TOKEN_EXPIRY_TIME));
        Tuple<Integer, FileTime> entry = new Tuple<>(userID, expiryDate);
        getAuthTokensForx64().put(token, entry);
    }

    public int getUserIdFromAuthToken(int token) {
        Tuple<Integer, FileTime> value = getAuthTokensForx64().getOrDefault(token, null);
        if (value == null || value.getRight() == null || value.getRight().isExpired()) {
            return 0;
        } else {
            return value.getLeft();
        }
    }

    public List<Instance> getInstances() {
        return instances;
    }

    public void setInstances(List<Instance> instances) {
        this.instances = instances;
    }
}
