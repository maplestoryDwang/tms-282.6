package net.swordie.ms.world.shop;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.jobs.nova.Cadena;
import net.swordie.ms.client.social.Guild.GuildSkill;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.constants.GuildConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.containerclasses.ItemInfo;

import java.util.ArrayList;
import java.util.List;

import static net.swordie.ms.client.character.skills.SkillStat.disCountR;

/**
 * Created on 3/27/2018.
 */
public class NpcShopDlg {

    private Char chr;
    private int shopID;
    private int selectNpcItemID;
    private int npcTemplateID;
    private int starCoin;
    private int shopVerNo;
    private List<NpcShopItem> items = new ArrayList<>();

    public void generateProjectiles() {
        for (int i : ItemConstants.getRechargeablesList()) {
            ItemInfo ii = ItemData.getItemInfoByID(i);
            NpcShopItem nsi = new NpcShopItem();
            nsi.setItemID(i);
            nsi.setUnitPrice(ii.getUnitPrice());
            nsi.setMaxPerSlot((short) ii.getSlotMax());
            addItem(nsi);
        }
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        // sub_14065BFB0
        outPacket.encodeString("");
        outPacket.encodeInt(0);
        
        boolean hasQuest = false;
        outPacket.encodeByte(hasQuest);
        if (hasQuest) {
            byte size = 0;
            outPacket.encodeByte(size);
            for (int i = 0; i < size; i++) {
                // just a guess that this is for quests
                outPacket.encodeInt(0); // questID?
                outPacket.encodeString(""); // questKey?
            }
        }
        outPacket.encodeShort(getItems().size() + chr.getRepurchaseItems().size());
        int index = 0;
        for (NpcShopItem nsi : getItems()) {
            int discountPrec = getDiscountPrec(nsi);
            if (discountPrec > 0) {
                nsi.setDiscountPerc(nsi.getDiscountPerc() + discountPrec);
            }
            nsi.encode(outPacket, index);
            index += 1;
        }
        index = 0;
        for (NpcShopItem nsi : chr.getRepurchaseItems()) {
            nsi.encode(outPacket, index);
            index += 1;
        }
    }

    private int getDiscountPrec(NpcShopItem nsi) {
        if (nsi.getPrice() > 0) {
            int haggleDiscountPrec = 0;
            if (JobConstants.isCadena(chr.getJob()) && chr.hasSkill(Cadena.HAGGLE)) {
                SkillInfo si = SkillData.getSkillInfoById(Cadena.HAGGLE);
                if (si != null) {
                    haggleDiscountPrec = si.getValue(disCountR, chr.getSkillLevel(Cadena.HAGGLE));
                }
            }
            int merchandisingDiscountPrec = 0;
            if (chr.getGuild() != null) {
                GuildSkill gs = chr.getGuild().getSkillById(GuildConstants.MERCHANDISING);
                SkillInfo si = SkillData.getSkillInfoById(GuildConstants.MERCHANDISING);
                if (gs != null && si != null) {
                    merchandisingDiscountPrec = si.getValue(disCountR, gs.getLevel());
                }
            }
            if (haggleDiscountPrec != 0 || merchandisingDiscountPrec != 0) {
                return Math.max(haggleDiscountPrec, merchandisingDiscountPrec);
            }
        }
        return 0;
    }

    public void setChar(Char chr) {
        this.chr = chr;
    }

    /** The character the shop is open for (set by the 265 flow before the shop packets are built). */
    public Char getChar() {
        return chr;
    }

    public int getShopID() {
        return shopID;
    }

    public void setShopID(int shopID) {
        this.shopID = shopID;
    }

    public List<NpcShopItem> getItems() {
        return items;
    }

    public void setItems(List<NpcShopItem> items) {
        this.items = items;
    }

    public int getSelectNpcItemID() {
        return selectNpcItemID;
    }

    public void setSelectNpcItemID(int selectNpcItemID) {
        this.selectNpcItemID = selectNpcItemID;
    }

    public int getNpcTemplateID() {
        return npcTemplateID;
    }

    public void setNpcTemplateID(int npcTemplateID) {
        this.npcTemplateID = npcTemplateID;
    }

    public int getStarCoin() {
        return starCoin;
    }

    public void setStarCoin(int starCoin) {
        this.starCoin = starCoin;
    }

    public int getShopVerNo() {
        return shopVerNo;
    }

    public void setShopVerNo(int shopVerNo) {
        this.shopVerNo = shopVerNo;
    }

    public void addItem(NpcShopItem nsi) {
        getItems().add(nsi);
    }

    public NpcShopItem getItemByIndex(int idx) {
        NpcShopItem nsi = null;
        if (chr.getRepurchaseItems().size() > 0) {
            List<NpcShopItem> npcShopItems = new ArrayList<>();
            npcShopItems.addAll(getItems());
            npcShopItems.addAll(chr.getRepurchaseItems());
            if (idx >= 0 || idx < npcShopItems.size()) {
                return npcShopItems.get(idx);
            }
        } else {
            if (idx >= 0 || idx < getItems().size()) {
                return getItems().get(idx);
            }
        }
        return nsi;
    }
}
