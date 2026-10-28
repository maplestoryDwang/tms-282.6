package net.swordie.ms.world;

import net.swordie.ms.Server;
import net.swordie.ms.ServerStatus;
import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Alliance.Alliance;
import net.swordie.ms.client.social.Friend.Friend;
import net.swordie.ms.client.social.Friend.FriendResult;
import net.swordie.ms.client.social.Guild.Guild;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Party.PartyMember;
import net.swordie.ms.client.social.Party.PartyResult;
import net.swordie.ms.connection.packet.MiniroomPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.enums.social.Friend.FriendFlag;
import net.swordie.ms.life.Merchant.Merchant;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.world.shop.auctionhouse.AuctionItem;
import net.swordie.ms.world.shop.auctionhouse.AuctionItemHistory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created on 11/2/2017.
 */
public class World {

    private int worldId;
    private int worldState;
    private int worldEventEXP_WSE;
    private int worldEventDrop_WSE;
    private int boomUpEventNotice;
    private boolean starplanet;
    private String name;
    private String worldEventDescription;
    private List<Channel> channels;
    private List<Party> parties = new ArrayList<>();
    private Map<Integer, Guild> guilds = new HashMap<>();
    private Map<Integer, Alliance> alliances = new HashMap<>();
    private Map<Integer, Client> connectedChatClients = new HashMap<>();
    private int partyIDCounter = 1;
    private boolean charCreateBlock;
    private ArrayList<Merchant> merchants = new ArrayList<>();
    private Set<AuctionItem> auctionItems = new HashSet<>();
    private Set<AuctionItemHistory> auctionItemHistories = new HashSet<>();

    public World(int worldId, String name, int worldState, String worldEventDescription, int worldEventEXP_WSE,
                 int worldEventDrop_WSE, int boomUpEventNotice, int amountOfChannels, boolean starplanet, boolean reboot) {
        this.worldId = worldId;
        this.name = name;
        this.worldState = worldState;
        this.worldEventDescription = worldEventDescription;
        this.worldEventEXP_WSE = worldEventEXP_WSE;
        this.worldEventDrop_WSE = worldEventDrop_WSE;
        this.boomUpEventNotice = boomUpEventNotice;
        List<Channel> channelList = new ArrayList<>();
        for (int i = 1; i <= amountOfChannels; i++) {
            channelList.add(new Channel(name, worldId, i));
        }
        this.channels = channelList;
        this.starplanet = starplanet;
    }

    public World(int worldId, String name, int amountOfChannels, String worldEventMsg) {
        this(worldId, name, 0, worldEventMsg, 100, 100, 0, amountOfChannels, false, false);
    }

    public void initGuilds() {
        List<Guild> guilds = Guild.loadGuildsFromSQL();
        for (Guild g : guilds) {
            addGuild(g);
        }
    }

    public void initAuctionHouse() {
        Set<AuctionItem> auctionItems = AuctionItem.getAuctionItemsFromSQL();
        for (AuctionItem auctionItem : auctionItems) {
            getAuctionItems().add(auctionItem);
        }
    }

    public int getWorldId() {
        return worldId;
    }

    public String getName() {
        return name;
    }

    public int getWorldState() {
        return worldState;
    }

    public boolean isStarPlanet() {
        return starplanet;
    }

    public String getWorldEventDescription() {
        return worldEventDescription;
    }

    public Channel getChannelById(byte id) {
        return getChannels().stream().filter(c -> c.getChannelId() == id).findFirst().orElse(null);
    }

    public List<Channel> getChannels() {
        return channels;
    }

    public ServerStatus getStatus() {
        return ServerStatus.NORMAL;
    }

    public Collection<Char> getChars() {
        return Server.get().getChars().values();
    }

    // Tìm một nhân vật theo tên
    public Char getCharByName(String name) {
        // Lấy tất cả các giá trị từ Map và lọc
        return Server.get().getChars().values().stream()
                .filter(chr -> chr.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    // Tìm một nhân vật theo ID
    public Char getCharById(int id) {
        return Server.get().getChars().get(id); // O(1)
    }

    // Lấy tất cả nhân vật trong một kênh
    public Set<Char> getCharsByChannelId(int channelId) {
        return Server.get().getChars().values().stream()
                .filter(chr -> chr.getClient().getChannel() == channelId)
                .collect(Collectors.toSet());
    }

    // Lấy tất cả nhân vật có cùng tên
    public Set<Char> getCharsByName(String name) {
        return Server.get().getChars().values().stream()
                .filter(chr -> chr.getName().equalsIgnoreCase(name))
                .collect(Collectors.toSet());
    }

    public List<Party> getParties() {
        return parties;
    }

    public void addParty(Party party) {
        int id = getPartyIdAndIncrement(); // sequential IDs should be fine here
        party.setId(id);
        synchronized (getParties()) {
            getParties().add(party);
        }
    }

    public void removeParty(int id) {
        getParties().removeIf(x -> x.getId() == id);
    }

    public void removeALlParty() {
        synchronized (getParties()) {
            getParties().clear();
        }
    }

    public Party getPartyByPartyID(int partyID) {
        final List<Party> parties = getParties();
        for (Party party : parties) {
            if (party.getId() == partyID) {
                return party;
            }
        }
        return null;
    }

    public Map<Integer, Guild> getGuilds() {
        return guilds;
    }

    public Collection<Guild> getGuildsWithCriteria(int levMin, int levMax, int sizeMin, int sizeMax, int avgLevMin, int avgLevMax) {
        Collection<Guild> guilds = getGuilds().values();
        Set<Guild> res = new HashSet<>(guilds);
        for (Guild g : guilds) {
            //calculate average level of guild members
            int averageLevel = g.getAverageMemberLevel();
            if (levMin != 0 && levMin > g.getReqLevel() + 1 //getReqLevel is automatically set to 0
                    || levMax != 0 && levMax < g.getReqLevel()
                    || sizeMin != 0 && sizeMin > g.getMembers().size()
                    || sizeMax != 0 && sizeMax < g.getMembers().size()
                    || avgLevMin != 0 && avgLevMin > averageLevel
                    || avgLevMax != 0 && avgLevMax < averageLevel) {
                res.remove(g);
            }
        }
        return res;
    }

    public Collection<Guild> getGuildsByString(int searchType, boolean exactWord, String searchTerm) {
        Collection<Guild> guilds = getGuilds().values();
        Set<Guild> res = new HashSet<>(guilds);
        for (Guild g : guilds) {
            if (searchType == 1) {
                String guildName = g.getName();
                String leaderName = g.getGuildLeader().getName();
                if ((exactWord && !guildName.equals(searchTerm) && !leaderName.equals(searchTerm)
                        || (!exactWord && !guildName.contains(searchTerm) && !leaderName.contains(searchTerm)))) {
                    res.remove(g);
                }
            } else {
                String name = searchType == 2
                        ? g.getName()
                        : searchType == 3
                        ? g.getGuildLeader().getName()
                        : "";
                if ((exactWord && !name.equals(searchTerm)) || (!exactWord && !name.contains(searchTerm))) {
                    res.remove(g);
                }
            }
        }
        return res;
    }

    public Guild getGuildByID(int id) {
        Guild guild = getGuilds().get(id);
        if (guild == null) {
            guild = Guild.getGuildByGuildID(id);
            if (guild != null) {
                getGuilds().put(id, guild);
                if (guild.getAllianceID() != 0) {
                    guild.setAlliance(getAlliance(guild.getAllianceID()));
                }
            }
        }
        return guild;
    }

    public Guild getGuildByName(String name) {
        Guild guild = getGuilds().values().stream().filter(g -> g.getName().equals(name)).findAny().orElse(null);
        if (guild == null) {
            guild = Guild.getGuildByGuildName(name);
            if (guild != null) {
                getGuilds().put(guild.getId(), guild);
                if (guild.getAllianceID() != 0) {
                    guild.setAlliance(getAlliance(guild.getAllianceID()));
                }
            }
        }
        return guild;
    }

    public int getPartyIdAndIncrement() {
        return partyIDCounter++;
    }

    public Account getAccountByID(int accID) {
        for (Char chr : getChars()) {
            Account acc = chr.getAccount();
            if (acc != null && acc.getId() == accID) {
                return acc;
            }
        }
        return null;
    }

    public boolean isCharCreateBlock() {
        return charCreateBlock;
    }

    public void setCharCreateBlock(boolean charCreateBlock) {
        this.charCreateBlock = charCreateBlock;
    }

    public boolean isFull() {
        boolean full = true;
        for (Channel channel : getChannels()) {
            if (channel.getChars().size() < channel.MAX_SIZE) {
                full = false;
                break;
            }
        }
        return full;
    }

    public Map<Integer, Alliance> getAlliances() {
        return alliances;
    }

    public void addGuild(Guild guild) {
        getGuilds().put(guild.getId(), guild);
    }

    private void addAlliance(Alliance ally) {
        getAlliances().put(ally.getId(), ally);
        // Initialize guilds to be the same instance as the ones we currently have
        Set<Guild> guilds = new HashSet<>();
        for (Guild guild : ally.getGuilds()) {
            Guild ourGuild = getGuildByID(guild.getId());
            ourGuild.setAlliance(ally);
            guilds.add(ourGuild);
        }
        ally.setGuilds(guilds);
    }

    public Alliance getAlliance(int id) {
        Alliance ally = getAlliances().getOrDefault(id, null);
        if (ally == null) {
            //ally = (Alliance) DatabaseManager.getObjFromDB(Alliance.class, id);
            if (ally != null) {
                addAlliance(ally);
            }
        }
        return ally;
    }

    public Alliance getAlliance(String name) {
        Alliance ally = getAlliances().values().stream().filter(a -> a.getName().equals(name)).findAny().orElse(null);
        if (ally == null) {
            //ally = (Alliance) DatabaseManager.getObjFromDB(Alliance.class, name);
            if (ally != null) {
                addAlliance(ally);
            }
        }
        return ally;
    }

    public void clearCache() {
        for (Channel channel : getChannels()) {
            channel.clearCache();
        }
    }

    public Map<Integer, Client> getConnectedChatClients() {
        return connectedChatClients;
    }

    public void addMerchant(Merchant merchant) {
        this.merchants.add(merchant);
    }

    public void removeMerchant(Merchant merchant) {
        this.merchants.remove(merchant);
    }

    public void removeAllMerchants() {
        this.merchants.clear();
    }

    public ArrayList<Merchant> getMerchants() {
        return this.merchants;
    }

    public void shutdown() {
        try {
            for (Channel ch : getChannels()) {
                for (Char chr : ch.getChars()) {
                    try {
                        chr.getClient().close();
                    } catch (Exception e) {
                        System.out.println("Unable to disconnect Char (" + chr.getName() + ") due to " + e);
                        DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                    }
                }
            }
            removeALlParty();
            removeAllMerchants();
            clearCache();
            System.out.println("Successfully shutting down world " + worldId + "\r\n");
        } catch (Exception e) {
            System.out.println("Unable to shutting down world " + worldId + " due to " + e + ". Please close the console manually.");
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public Set<AuctionItem> getAuctionItems() {
        return auctionItems;
    }

    public Set<AuctionItemHistory> getAuctionItemHistories() {
        return auctionItemHistories;
    }

    public void clearUnUsedFields() {
        getChannels().forEach(Channel::clearUnUsedFields);
    }
}
