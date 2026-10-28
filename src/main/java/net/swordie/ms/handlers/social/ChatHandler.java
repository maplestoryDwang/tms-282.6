package net.swordie.ms.handlers.social;

import net.swordie.ms.Server;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.commands.*;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.social.Friend.Friend;
import net.swordie.ms.client.social.Guild.Guild;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.connection.packet.UserPacket;
import net.swordie.ms.enums.*;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.world.tms282.audit.AuditCommands;
import net.swordie.ms.world.tms282.audit.AuditLog;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import static net.swordie.ms.enums.ChatType.*;
import static net.swordie.ms.enums.InvType.EQUIP;
import static net.swordie.ms.enums.InvType.EQUIPPED;


public class ChatHandler {

    /** AdminCommands plus extra registries (ban / audit commands); resolved once. */
    private static final Class[] ADMIN_COMMAND_CLASSES = concatCommandClasses(concatCommandClasses(concatCommandClasses(
            AdminCommands.class.getClasses(), AuditCommands.class.getClasses()),
            net.swordie.ms.world.tms282.bossrun.BossRunCommands.class.getClasses()), // [bossrun] !bosslog !bossreset
            net.swordie.ms.world.tms282.revival.RevivalCommands.class.getClasses()); // [revival] !revival (docs/revival)

    private static Class[] concatCommandClasses(Class[] a, Class[] b) {
        Class[] all = new Class[a.length + b.length];
        System.arraycopy(a, 0, all, 0, a.length);
        System.arraycopy(b, 0, all, a.length, b.length);
        return all;
    }

    @Handler(op = InHeader.USER_CHAT)
    public static void handleUserChat(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // timestamp
        String msg = inPacket.decodeString();
        if (msg.length() <= 0) {
            chr.dispose();
            return;
        }
        String command = msg.split(" ")[0].replace(String.valueOf(msg.charAt(0)), "");
        Class[] commandClasses = null;
        if (msg.startsWith(String.valueOf(ServerConfig.PLAYER_COMMAND))) {
            commandClasses = PlayerCommands.class.getClasses();
        } else if (msg.startsWith(String.valueOf(ServerConfig.ADMIN_COMMAND)) || msg.startsWith(String.valueOf(ServerConfig.ADMIN_COMMAND_2))) {
            commandClasses = ADMIN_COMMAND_CLASSES;
        }
        final boolean adminCommand = commandClasses == ADMIN_COMMAND_CLASSES;
        boolean permissionDenied = false;
        // doesn't start with command prefix
        if (commandClasses == null) {
            if (chr.isGM()) {
                chr.getField().broadcast(UserPacket.chat(chr, ChatUserType.Admin, msg, 3, 0, chr.getClient().getWorldId()));
            } else {
                chr.getField().broadcast(UserPacket.chat(chr, ChatUserType.User, msg, 3, 0, chr.getClient().getWorldId()));
                DataPrinter.send(DataPrinter.LOG_CHAT, chr.getName() + ": " + msg);
            }
            return;
        }
        for (Class commandClass : commandClasses) {
            Command cmd = (Command) commandClass.getAnnotation(Command.class);
            for (String name : cmd.names()) {
                if (!name.equalsIgnoreCase(command)) {
                    continue;
                }
                if (chr.getUser().getAccountType().ordinal() < cmd.requiredType().ordinal()) {
                    permissionDenied = true;
                    continue;
                }
                String auditResult = "OK";
                try {
                    ICommand iCommand = null;
                    if (msg.charAt(0) == ServerConfig.PLAYER_COMMAND) {
                        iCommand = (PlayerCommand) commandClass.getConstructor().newInstance();
                    } else if (msg.charAt(0) == ServerConfig.ADMIN_COMMAND) {
                        iCommand = (AdminCommand) commandClass.getConstructor().newInstance();
                    }
                    commandClass.getDeclaredMethod("execute", Char.class, String[].class).invoke(iCommand, chr, msg.split(" "));
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    Throwable cause = e instanceof InvocationTargetException && e.getCause() != null ? e.getCause() : e;
                    auditResult = "ERROR " + cause.getClass().getSimpleName();
                    DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                } finally {
                    if (adminCommand) {
                        AuditLog.command(chr, msg, auditResult);
                    }
                }
                return;
            }
        }
        if (adminCommand && (permissionDenied || chr.isGM())) {
            AuditLog.command(chr, msg, permissionDenied ? "DENIED" : "UNKNOWN");
        }
        // only reaches this point if no matching command was found
        chr.chatMessage(Expedition, "語法錯誤 \"" + command + "\"");
    }

    @Handler(op = InHeader.USER_ITEM_LINKED_CHAT)
    public static void handleUserItemLinkedChat(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        String msg = inPacket.decodeString();
        byte type = inPacket.decodeByte();
        InvType invType = InvType.getInvTypeByVal(inPacket.decodeInt());
        inPacket.decodeInt();
        int pos = inPacket.decodeInt();
        if (invType == EQUIP && pos < 0) {
            invType = EQUIPPED;
            pos = -pos;
        }
        Item item = chr.getInventoryByType(invType).getItemBySlot(pos);
        if (item == null) {
            chr.chatMessage("物品不存在。");
            return;
        }
        chr.getField().broadcast(UserPacket.itemLinkedChat(chr, ChatUserType.User, msg,
                type, 0, chr.getClient().getWorldId(), item));
    }

    @Handler(op = InHeader.WHISPER)
    public static void handleWhisper(Char chr, InPacket inPacket, InHeader header) {
        byte type = inPacket.decodeByte();
        inPacket.decodeInt(); // tick
        String destName = inPacket.decodeString();
        Char other = Server.get().getWorld().getCharByName(destName);
        if (other == null) {
            chr.chatMessage("無法找到角色 " + destName + "。");
            return;
        }
        switch (type) {
            case 5: // /find command
            case 68:
                int fieldId = other.getField().getId();
                int channel = other.getClient().getChannel();
                if (channel != chr.getClient().getChannel()) {
                    chr.chatMessage(ChatType.Tip, "%s 在頻道 %d。", other.getName(), channel);
                } else {
                    if (chr.getField().getSN() == other.getField().getSN()) {
                        chr.chatMessage(ChatType.Tip, "%s 和你在同一張地圖。", destName);
                        break;
                    }
                    String fieldString = StringData.getMapStringById(fieldId);
                    if (fieldString == null) {
                        fieldString = "未知錯誤";
                    }
                    chr.chatMessage(ChatType.Tip, "%s 在地圖 %s。", destName, fieldString);
                }
                break;
            case 6: // whisper
                String msg = inPacket.decodeString();
                if (header == InHeader.WHISPER_ITEM) {
                    final String itemText = inPacket.decodeString();
                }
                if (net.swordie.ms.client.character.social.tms282.SocialGuards.refuseWhisper(chr, other)) { // social: block list
                    break;
                }
                other.write(FieldPacket.whisper(chr, (byte) (chr.getClient().getChannel() - 1), msg, false, null));
                chr.chatMessage(Whisper, String.format("%s<< %s", destName, msg));
                break;
        }

    }

    @Handler(op = InHeader.GROUP_MESSAGE)
    public static void handleGroupMessage(Char chr, InPacket inPacket) {
        byte type = inPacket.decodeByte(); // party = 1, alliance = 3
        short loopSize = inPacket.decodeShort();
        Set<Integer> charIds = new HashSet<>();
        for (int i = 0; i < loopSize; i++) {
            charIds.add(inPacket.decodeInt());
        }
        String msg = inPacket.decodeString();
        if (msg.length() > 1000) {
            return;
        }
        DataPrinter.send(DataPrinter.LOG_CHAT, chr.getName() + ": " + msg);
        OutPacket packet;
        GroupMessageType groupMessageType;
        switch (type) {
            case 0:
                groupMessageType = GroupMessageType.Buddy;
                break;
            case 1:
                groupMessageType = GroupMessageType.Party;
                break;
            case 2:
                groupMessageType = GroupMessageType.Guild;
                break;
            case 3:
                groupMessageType = GroupMessageType.Alliance;
                break;
            default:
                System.out.println("Unhandled group message type " + type);
                return;
        }
        packet = FieldPacket.groupMessage(groupMessageType, chr, msg);
        // multiplayer-0930: the recipients come from the server, not from the client's list (it could be forged to
        // message anyone): party chat -> the online party members, guild chat -> members of the sender's guild.
        if (groupMessageType == GroupMessageType.Party) {
            charIds.clear();
            if (chr.getParty() != null) {
                for (Char member : chr.getParty().getOnlineChars()) {
                    if (member != null && member.getId() != chr.getId()) {
                        charIds.add(member.getId());
                    }
                }
            }
        } else if (groupMessageType == GroupMessageType.Guild) {
            charIds.removeIf(id -> chr.getGuild() == null || chr.getGuild().getMemberByCharID(id) == null);
        }
        for (Integer charId : charIds) {
            Char other = chr.getWorld().getCharById(charId);
            if (other != null) {
                other.write(packet);
            }
        }
    }

    @Handler(op = InHeader.ITEM_LINKED_GROUP_MESSAGE)
    public static void handleItemLinkedGroupMessage(Char chr, InPacket inPacket) {
        byte type = inPacket.decodeByte(); // party = 1, alliance = 3
        short loopSize = inPacket.decodeShort();
        Set<Integer> charIds = new HashSet<>();
        for (int i = 0; i < loopSize; i++) {
            charIds.add(inPacket.decodeInt());
        }
        String msg = inPacket.decodeString();
        if (msg.length() > 1000) {
            return;
        }
        InvType invType = InvType.getInvTypeByVal(inPacket.decodeInt());
        int pos = inPacket.decodeInt();
        if (invType == EQUIP && pos < 0) {
            invType = EQUIPPED;
            pos = -pos;
        }
        Item item = chr.getInventoryByType(invType).getItemBySlot(pos);
        if (item == null) {
            chr.chatMessage("找不到該道具。");
            return;
        }
        switch (type) {
            case 0: // buddy
                for (Friend friend : chr.getAllFriends()) {
                    Char friendChr = chr.getWorld().getCharById(friend.getOwnerID());
                    if (friendChr != null) {
                        friendChr.write(FieldPacket.groupMessage(GroupMessageType.Buddy, chr, msg));
                    }
                }
                break;
            case 1: // party
                if (chr.getParty() != null) {
                    chr.getParty().broadcast(FieldPacket.itemLinkedGroupMessage(GroupMessageType.Party, chr, msg, item), chr);
                }
                break;
            case 2: // guild
                if (chr.getGuild() != null) {
                    chr.getGuild().broadcast(FieldPacket.itemLinkedGroupMessage(GroupMessageType.Guild, chr, msg, item), chr);
                }
                break;
            case 3: // alliance
                if (chr.getGuild() != null && chr.getGuild().getAlliance() != null) {
                    chr.getGuild().getAlliance().broadcast(FieldPacket.itemLinkedGroupMessage(GroupMessageType.Alliance, chr, msg, item), chr);
                }
                break;
            default:
                System.out.println("Unhandled group message type " + type);
                break;
        }
        OutPacket packet;
        GroupMessageType groupMessageType;
        switch (type) {
            case 0:
                groupMessageType = GroupMessageType.Buddy;
                break;
            case 1:
                groupMessageType = GroupMessageType.Party;
                break;
            case 2:
                groupMessageType = GroupMessageType.Guild;
                break;
            case 3:
                groupMessageType = GroupMessageType.Alliance;
                break;
            default:
                System.out.println("Unhandled group message type " + type);
                return;
        }
        packet = FieldPacket.itemLinkedGroupMessage(groupMessageType, chr, msg, item);
        for (Integer charId : charIds) {
            Char other = chr.getWorld().getCharById(charId);
            if (other != null) {
                other.write(packet);
            }
        }
    }

    @Handler(op = InHeader.FRIEND_CHAT)
    public static void handleFriendChat(Char chr, InPacket inPacket) {
        int accID = inPacket.decodeInt();
        String msg = inPacket.decodeString();
        int size = inPacket.decodeInt();
        for (int i = 0; i < size; i++) {
            if (chr.getWorld().getConnectedChatClients().containsKey(i)) {
                //chr.getWorld().getConnectedChatClients().get(i).write(ChatSocket.friendChatMessage(accID, chr.getId(), null, msg, false));
            }
        }
    }

    @Handler(op = InHeader.GUILD_CHAT)
    public static void handleGuildChat(Char chr, InPacket inPacket) {
        int charID = inPacket.decodeInt();
        int guildID = inPacket.decodeInt();
        String msg = inPacket.decodeString();
        Guild g = chr.getGuild();
        if (g != null) {
            g.broadcast(FieldPacket.groupMessage(GroupMessageType.Guild, chr, msg));
            DataPrinter.send(DataPrinter.LOG_CHAT, "[Guild " + g.getName() + " ] " + chr.getName() + ": " + msg);
        }
    }
}
