package net.swordie.ms.handlers.user;

import net.swordie.ms.Server;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.HyperTPRock;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.skills.TownPortal;
import net.swordie.ms.client.jobs.JobManager;
import net.swordie.ms.client.jobs.adventurer.thief.DualBlade;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.FieldOption;
import net.swordie.ms.enums.MapTransferType;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.loaders.FieldData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.World;
import net.swordie.ms.world.boss.Gollux;
import net.swordie.ms.world.event.OlaOlaEvent;
import net.swordie.ms.world.event.PhysicalFitnessEvent;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.FieldInstanceType;
import net.swordie.ms.world.field.Portal;
import net.swordie.ms.world.migration.Gms265FieldEntry;
import net.swordie.ms.world.migration.MigrateInDecoder;
import net.swordie.ms.world.migration.MigrateInRequest;
import net.swordie.ms.world.migration.MigrationService;

import java.util.concurrent.TimeUnit;

public class MigrationHandler {

    /**
     * GMS 265 MIGRATE_IN: decoding ({@link MigrateInDecoder#GMS_265}) and entering ({@link MigrationService} with
     * {@link Gms265FieldEntry}) are separate so the TMS 282.3 overlay reuses the entry with its own decoder.
     */
    @Handler(op = InHeader.MIGRATE_IN)
    public static void handleMigrateIn(Client c, InPacket inPacket) {
        MigrateInRequest request = MigrateInDecoder.GMS_265.decode(inPacket);
        MigrationService.standard().enter(c, request, Gms265FieldEntry.INSTANCE);
    }

    @Handler(op = InHeader.USER_TRANSFER_FIELD_REQUEST)
    public static void handleUserTransferFieldRequest(Char chr, InPacket inPacket) {
        Client c = chr.getClient();
        if (c.getCurrentState() == Client.IN_CASH_SHOP) {
            c.migrateOut(chr);
            return;
        }
        inPacket.decodeByte();
        byte fieldKey = inPacket.decodeByte();
        int targetField = inPacket.decodeInt();
        if (targetField == 106020100 || targetField == 106020400) {
            // same issue as below, its in mushroom kingdom so maybe the maps are just outdated or w/e
            targetField = 106020403;
        } else if (targetField == 106020402) {
            // warping from portal in 106020403 is unable to find defined portal (not a scripted portal)
            targetField = 106020000;
        }
        String portalName = inPacket.decodeString();
        if (portalName != null && !"".equals(portalName)) {
            ScriptManagerImpl sm = chr.getScriptManager();
            if (sm.isOlaOlaOpen() && portalName.equals(OlaOlaEvent.JOIN_PORTAL)) {
                chr.chatMessage("歐拉歐拉活動尚未開始。");
                chr.dispose();
            } else if (sm.isOlaOlaActive() && portalName.equals(OlaOlaEvent.JOIN_PORTAL)) {
                Field field = chr.getField();
                Portal portal = field.getPortalByName(portalName);
                if (portal.getScript() != null && !portal.getScript().equals("")) {
                    chr.getScriptManager().startScript(chr, portal.getId(), portal.getScript(), ScriptType.Portal);
                    chr.dispose();
                } else {
                    Field toField = chr.getOrCreateFieldByCurrentInstanceType(portal.getTargetMapId());
                    if (toField == null) {
                        return;
                    }
                    Portal toPortal = toField.getPortalByName(portal.getTargetPortalName());
                    if (toPortal == null) {
                        toPortal = toField.getPortalByName("sp");
                    }
                    chr.warp(toField, toPortal, false, false);
                }
            } else if (sm.isPhysicalFitnessOpen() && portalName.equals(PhysicalFitnessEvent.JOIN_PORTAL)) {
                chr.chatMessage("體能測驗活動尚未開始。");
                chr.dispose();
            } else if (sm.isPhysicalFitnessActive() && portalName.equals(PhysicalFitnessEvent.JOIN_PORTAL)) {
                Field field = chr.getField();
                Portal portal = field.getPortalByName(portalName);
                if (portal.getScript() != null && !portal.getScript().equals("")) {
                    chr.getScriptManager().startScript(chr, portal.getId(), portal.getScript(), ScriptType.Portal);
                    chr.dispose();
                } else {
                    Field toField = chr.getOrCreateFieldByCurrentInstanceType(portal.getTargetMapId());
                    if (toField == null) {
                        return;
                    }
                    Portal toPortal = toField.getPortalByName(portal.getTargetPortalName());
                    if (toPortal == null) {
                        toPortal = toField.getPortalByName("sp");
                    }
                    chr.warp(toField, toPortal, false, false);
                }
            } else {
                Field field = chr.getField();
                Portal portal = field.getPortalByName(portalName);
                if (portal == null) {
                    portal = field.getDefaultPortal();
                }
                if (portal.getScript() != null && !portal.getScript().equals("")) {
                    chr.getScriptManager().startScript(chr, portal.getId(), portal.getScript(), ScriptType.Portal);
                    chr.dispose();
                } else {
                    Field toField = chr.getOrCreateFieldByCurrentInstanceType(portal.getTargetMapId());
                    if (toField == null) {
                        return;
                    }
                    Portal toPortal = toField.getPortalByName(portal.getTargetPortalName());
                    if (toPortal == null) {
                        toPortal = toField.getPortalByName("sp");
                    }
                    chr.warp(toField, toPortal);
                }
            }
        } else if (chr.getHP() <= 0) {
            chr.revive(targetField);
        } else if (chr.getTransferField() == targetField && chr.getTransferFieldReq() == chr.getField().getId()) {
            Field toField = chr.getOrCreateFieldByCurrentInstanceType(chr.getTransferField());
            if (toField != null && chr.getTransferField() > 0) {
                chr.warp(toField);
            }
            chr.setTransferField(0);
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_PORTAL_SCRIPT_REQUEST)
    public static void handleUserPortalScriptRequest(Char chr, InPacket inPacket) {
        if (chr.getHP() <= 0) {
            chr.dispose();
            return;
        }
        byte portalID = inPacket.decodeByte();
        String portalName = inPacket.decodeString();
        Portal portal = chr.getField().getPortalByName(portalName);
        String script;
        if (portal != null) {
            portalID = (byte) portal.getId();
            script = "".equals(portal.getScript()) ? portalName : portal.getScript();
            if (script.equals("pt_mutoHotPot")) {
                chr.checkIngredients();
            } else {
                chr.getScriptManager().startScript(chr, portalID, script, ScriptType.Portal);
            }
        } else {
            chr.chatMessage("未知傳送門錯誤：" + portalName);
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_PORTAL_TELEPORT_REQUEST)
    public static void handleUserPortalTeleportRequest(Char chr, InPacket inPacket) {
        if (chr.getHP() <= 0) {
            chr.dispose();
            return;
        }
        byte portalID = inPacket.decodeByte();
        int unk = inPacket.decodeShort();
        int unk2 = inPacket.decodeInt();
        Position toPos = new Position(inPacket.decodeInt(), inPacket.decodeInt());
        chr.write(FieldPacket.teleport(toPos, chr));
    }

    @Handler(op = InHeader.USER_TRANSFER_CHANNEL_REQUEST)
    public static void handleUserTransferChannelRequest(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        byte channelId = (byte) (inPacket.decodeByte() + 1);
        if (c.getWorld().getChannelById(channelId) == null) {
            return;
        }
        if (chr == null) {
            c.close();
            return;
        }

        if (chr.getHP() <= 0) {
            chr.dispose();
            return;
        }

        Field field = chr.getField();
        if ((field.getFieldLimit() & FieldOption.MigrateLimit.getVal()) > 0
                || channelId < 1 || channelId > c.getWorld().getChannels().size()) {
            chr.dispose();
            return;
        }

        chr.changeChannel(channelId);
    }

    /** The v265.3 cash shop protocol is only partially implemented in this core and the
     *  client crashes a few seconds after entering. All commerce runs through the NPC
     *  menu panels instead, so entry is blocked with a friendly message.
     *  Set to false to re-enable the real cash shop for debugging. */
    private static final boolean BLOCK_CASH_SHOP = false;

    @Handler(op = InHeader.USER_MIGRATE_TO_CASH_SHOP_REQUEST)
    public static void handleUserMigrateToCashShopRequest(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        if (chr == null) {
            c.close();
            return;
        }
        if (!net.swordie.ms.GameSettings.cashShopEnabled()) { // feature.cashshop
            chr.chatMessage(net.swordie.ms.GameSettings.CASH_SHOP_DISABLED);
            chr.dispose();
            return;
        }
        if (BLOCK_CASH_SHOP) {
            chr.chatMessage("現金商城尚未開放，請使用主選單的【充值中心 / 商店大全 / 贊助系統】。");
            chr.dispose();
            return;
        }
        Account acc = chr.getAccount();
        Field field = chr.getField();
        if ((field.getFieldLimit() & FieldOption.MigrateLimit.getVal()) > 0 || chr.getHP() <= 0) {
            chr.dispose();
            return;
        }
        c.migrateIn(false);
        c.write(Stage.setCashShop(chr));
        c.write(CCashShop.queryCashResult(chr));
        c.write(CCashShop.loadLockerDone(acc));
        for (Item item : chr.getEquipInventory().getItems()) {
            if (item instanceof Equip equip && !equip.hasPotential()) {
                if (equip.isCash()) {
                    continue;
                }
                chr.write(WvsContext.addItemToInventory(item));
            }
        }
    }

    @Handler(op = InHeader.USER_MIGRATE_TO_AUCTION_HOUSE_REQUEST)
    public static void handleUserMigrateToAuctionHouseRequest(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        if (chr == null) {
            c.close();
            return;
        }
        if (!net.swordie.ms.GameSettings.auctionEnabled()) { // feature.auction
            chr.chatMessage(net.swordie.ms.GameSettings.AUCTION_DISABLED);
            chr.dispose();
            return;
        }
        Field field = chr.getField();
        if ((field.getFieldLimit() & FieldOption.MigrateLimit.getVal()) > 0 || chr.getHP() <= 0) {
            chr.dispose();
            return;
        }
        c.migrateIn(true);
        c.write(Stage.setAuctionField(chr));
        for (Item item : chr.getEquipInventory().getItems()) {
            if (item instanceof Equip equip && !equip.hasPotential()) {
                if (equip.isCash()) {
                    continue;
                }
                chr.write(WvsContext.addItemToInventory(item));
            }
        }
    }

    @Handler(op = InHeader.AUCTION_LEAVE_REQUEST)
    public static void handleUserAuctionLeaveRequest(Client c, InPacket inPacket) {
        c.auctionHouseOut(c.getChr());
    }

    @Handler(op = InHeader.USER_MAP_TRANSFER_REQUEST)
    public static void handleUserMapTransferRequest(Char chr, InPacket inPacket) {
        chr.punishLieDetectorEvasion();

        if (chr.getHP() <= 0) {
            chr.dispose();
            return;
        }

        byte mtType = inPacket.decodeByte();
        byte itemType = inPacket.decodeByte();

        MapTransferType mapTransferType = MapTransferType.getByVal(mtType);
        switch (mapTransferType) {
            case DeleteListRecv: //Delete request that's received
                int targetFieldID = inPacket.decodeInt();
                HyperTPRock.removeFieldId(chr, targetFieldID);
                chr.write(WvsContext.mapTransferResult(MapTransferType.DeleteListSend, itemType, chr.getHyperRockFields()));
                break;

            case RegisterListRecv: //Register request that's received
                targetFieldID = chr.getFieldID();
                Field field = chr.getField();
                if (field == null || (field.getFieldLimit() & FieldOption.TeleportItemLimit.getVal()) > 0) {
                    chr.chatMessage("你無法移動到該地圖。");
                    chr.dispose();
                    return;
                }
                int levelLimit = field.getLvLimit();
                if (chr.getLevel() < levelLimit) {
                    chr.chatMessage(String.format("你的等級：%d，地圖需求等級：%d", chr.getLevel(), levelLimit));
                    chr.dispose();
                    return;
                }
                HyperTPRock.addFieldId(chr, targetFieldID);
                chr.write(WvsContext.mapTransferResult(MapTransferType.RegisterListSend, itemType, chr.getHyperRockFields()));
                break;

        }
    }

    @Handler(op = InHeader.USER_FIELD_TRANSFER_REQUEST)
    public static void handleUserFieldTransferRequest(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        if ((field.getFieldLimit() & FieldOption.TeleportItemLimit.getVal()) > 0
                || (field.getFieldLimit() & FieldOption.MigrateLimit.getVal()) > 0
                || (field.getFieldLimit() & FieldOption.PortalScrollLimit.getVal()) > 0
                || !field.isChannelField()) {
            chr.chatMessage("你無法傳送到該地圖。");
            chr.dispose();
            return;
        }

        if (chr.getHP() <= 0) {
            chr.dispose();
            return;
        }

        int fieldID = inPacket.decodeInt();
        if (fieldID == 7860) {
            Field ardentmill = chr.getOrCreateFieldByCurrentInstanceType(FieldConstants.ARDENTMILL);
            chr.warp(ardentmill);
        } else if (fieldID == 26015) {
            chr.warp(200000301);
        } else if (FieldData.getFieldById(fieldID) != null) {
            chr.warp(fieldID);
        }
    }

    @Handler(op = InHeader.ENTER_TOWN_PORTAL_REQUEST)
    public static void handleEnterTownPortalRequest(Char chr, InPacket inPacket) {
        int chrId = inPacket.decodeInt(); // Char id
        boolean town = inPacket.decodeByte() != 0;

        if (chr.getHP() <= 0) {
            chr.dispose();
            return;
        }

        Field field = chr.getField();
        TownPortal townPortal = field.getTownPortalByChrId(chrId);
        if (townPortal != null) {       // TODO Using teleports, as grabbing the TownPortalPoint portal id is not working
            if (town) {
                // townField -> fieldField
                Field fieldField = townPortal.getChannel().getField(townPortal.getFieldFieldId());

                chr.warp(fieldField); // Back to the original Door
                chr.write(FieldPacket.teleport(townPortal.getFieldPosition(), chr)); // Teleports player to the position of the TownPortal
            } else {
                // fieldField -> townField
                Field returnField = townPortal.getChannel().getField(townPortal.getTownFieldId()); // Initialise the Town Map,

                chr.warp(returnField); // warp Char
                chr.write(FieldPacket.teleport(townPortal.getTownPosition(), chr));
                if (returnField.getTownPortalByChrId(chrId) == null) { // So that every re-enter into the TownField doesn't spawn another TownPortal
                    returnField.broadcast(WvsContext.townPortal(townPortal)); // create the TownPortal
                    returnField.addTownPortal(townPortal);
                }
            }
        } else {
            chr.dispose();
            System.out.println("Character {" + chrId + "} tried entering a Town Portal in field {" + field.getId() + "} which does not exist."); // Potential Hacking Log
        }
    }

    @Handler(op = InHeader.USER_TRANSFER_FREE_MARKET_REQUEST)
    public static void handleTransferFreeMarketRequest(Char chr, InPacket inPacket) {
        byte toChannelID = (byte) (inPacket.decodeByte() + 1);
        int fieldID = inPacket.decodeInt();

        if (chr.getHP() <= 0) {
            chr.dispose();
            return;
        }

        if (chr.getWorld().getChannelById(toChannelID) != null && GameConstants.isFreeMarketField(fieldID)
                && GameConstants.isFreeMarketField(chr.getField().getId())) {
            Field toField = chr.getClient().getChannelInstance().getField(fieldID);
            if (toField == null) {
                chr.dispose();
                return;
            }
            int currentChannelID = chr.getClient().getChannel();
            if (currentChannelID != toChannelID) {
                chr.changeChannelAndWarp(toChannelID, fieldID);
            } else {
                chr.warp(toField);
            }
        }

        inPacket.decodeInt(); // tick
    }

    @Handler(op = InHeader.GOLLUX_OUT_REQUEST)
    public static void handleGolluxOutReqeust(Char chr, InPacket inPacket) {
        chr.getScriptManager().exitBoss();
    }

    @Handler(op = InHeader.REQUEST_RELOGIN_COOKIE)
    public static void handleReloginCookieRequest(Char chr, InPacket inPacket) {
        int characterID = inPacket.decodeInt();
        Client client = chr.getClient();
        Server.get().getSwitchChars().put(chr.getUser().getId(), new Tuple<>(0, client.getChannel()));
        client.write(WvsContext.returnToTitle());
        finalizeCurrentCharacterForSwitch(client);
    }

    @Handler(op = InHeader.SWITCH_CHARACTER)
    public static void handleSwitchCharacter(Char chr, InPacket inPacket) {
        int characterID = inPacket.decodeInt();
        Client client = chr.getClient();
        Server.get().getSwitchChars().put(chr.getUser().getId(), new Tuple<>(characterID, client.getChannel()));
        client.write(WvsContext.returnToTitle());
        finalizeCurrentCharacterForSwitch(client);
    }

    private static void finalizeCurrentCharacterForSwitch(Client client) {
        if (client == null || client.getChr() == null || client.isMigrating()) {
            return;
        }
        client.finalizeSession(false);
    }

    @Handler(op = InHeader.FPS_LOG)
    public static void handleFPSLog(Char chr, InPacket inPacket) {
        int n = inPacket.decodeInt();
        byte v16 = inPacket.decodeByte();
        int v15 = inPacket.decodeInt();
        int v19 = inPacket.decodeInt();
        int dwCPUCores = inPacket.decodeInt();
        int v21 = inPacket.decodeInt();
        int v10 = inPacket.decodeInt();
        int v8 = inPacket.decodeInt();
        int v9 = inPacket.decodeInt();
        int v7 = inPacket.decodeInt();
        short v12 = inPacket.decodeShort();
        short idk = inPacket.decodeShort();
        //System.out.printf("n: %d, v16: %d, v15: %d, v19: %d, dwCPUCores: %d, v21: %d, v10: %d, v8: %d, v9: %d, v7: %d, v12: %d, idk: %d",n,v16,v15,v19,dwCPUCores,v21,v10,v8,v9,v7,v12,idk));
    }

}
