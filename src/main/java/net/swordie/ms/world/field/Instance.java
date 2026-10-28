package net.swordie.ms.world.field;

import net.swordie.ms.Server;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.netty.NettyClient;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.loaders.FieldData;
import net.swordie.ms.world.partyquest.HungryMutoRecipes;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static net.swordie.ms.world.field.FieldInstanceType.PARTY;
import static net.swordie.ms.world.field.FieldInstanceType.SOLO;

/**
 * Created on 23-4-2019.
 *
 * @author Asura
 */
public class Instance {

    private int id;
    private Char chr;
    private Party party;
    // expedition
    // guild
    private int enterFieldId;
    private int enterPortalId;
    private int forcedReturn;
    private int achieveRatio;
    private Map<Integer, Field> fields = new HashMap<>();
    private Map<String, Object> properties = new HashMap<>();
    private FieldInstanceType instanceType;
    private ScheduledFuture<?> warpOutTimer;
    private long warpOutTimeout;
    private Map<Integer, Char> chars = new ConcurrentHashMap<>();
    private int forcedReturnPortalId = -1;
    private Char stigmaChar = null;
    private int corruptionCount = 0;
    private int visitorStageCount = 0;

    //User for Muto PQ.
    private List<HungryMutoRecipes> mutoRecipes;
    private int mutoRecipe;
    private boolean isCreateNewRecipe;
    private int mutoScore;

    public Instance(Char chr) {
        this.id = Server.get().getInstanceIdAndIncrement();
        this.chr = chr;
        instanceType = SOLO;
    }

    public Instance(Party party) {
        this.id = Server.get().getInstanceIdAndIncrement();
        this.party = party;
        instanceType = PARTY;
    }

    /**
     * Makes a Char reenter in this instance.
     *
     * @param chr the Char that should enter this instance
     */
    public void reEnter(Char chr) {
        if (canReEnter(chr)) {
            chr.warp(getEnterFieldId(), getEnterPortalId(), false);
        }
    }

    /**
     * Returns whether or not the given Char can re-enter this Instance.
     *
     * @param chr the Char to check
     * @return whether or not the given Char can re-enter this Instance.
     */
    public boolean canReEnter(Char chr) {
        return getChars().get(chr.getId()) != null;
    }

    /**
     * Sets up this Instance. Creates a List of Chars for this Instance, and warps them to the given Field.
     *
     * @param fieldId  the Field's id to warp to
     * @param portalId the Portal's id to warp to
     */
    public void setup(int fieldId, int portalId) {
        Field checkField = FieldData.getFieldCopyById(fieldId);
        if (checkField == null) {
            throw new IllegalArgumentException("Invalid Field id " + fieldId);
        }
        if (this.party != null) {
            for (Char chr : this.party.getOnlineChars()) {
                this.getChars().put(chr.getId(), chr);
            }
            this.party.setInstance(this);
            int i = 0;
            for (Char chr : this.party.getOnlineChars()) {
                chr.getTimer().addEvent(() -> chr.warp(fieldId, portalId, false), i);
                i += 100;
            }
        } else {
            this.getChars().put(chr.getId(), chr);
            chr.setInstance(this);
            chr.warp(fieldId, portalId, false);
        }
        setEnterFieldId(fieldId);
        setEnterPortalId(portalId);
        setForcedReturn(checkField.getForcedReturn());
        setAchieveRatio(0);
        Server.get().getInstances().add(this);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns a List of eligible Chars for this Instance.
     *
     * @return list of eligible Chars
     */
    public Map<Integer, Char> getChars() {
        return chars;
    }

    /**
     * Removes a Char from the eligible Char list.
     *
     * @param chr the Char to remove
     */
    public void removeChar(Char chr) {
        chars.remove(chr.getId());
    }

    /**
     * Returns the Char of this Instance, if it is a SOLO instance. Null otherwise.
     *
     * @return Char of this instance
     */
    public Char getChr() {
        return chr;
    }

    /**
     * Returns the Party of this Instance, if it is a PARTY instance. Null otherwise.
     *
     * @return
     */
    public Party getParty() {
        return party;
    }

    /**
     * Returns a List of active Fields in this Instance.
     *
     * @return List of active Fields
     */
    public Map<Integer, Field> getFields() {
        return fields;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public FieldInstanceType getInstanceType() {
        return instanceType;
    }

    /**
     * Returns the forced return Field of this Instance, for when a Char is forced out of this Instance.
     *
     * @return the forced return Field
     */
    public int getForcedReturn() {
        return forcedReturn;
    }

    /**
     * Sets the forced return Field of this Instance.
     *
     * @param forcedReturn the forced return field's id
     */
    public void setForcedReturn(int forcedReturn) {
        this.forcedReturn = forcedReturn;
    }

    /**
     * Returns the initial enter Field of this Instance.
     *
     * @return the initial enter Field of this Instance.
     */
    public int getEnterFieldId() {
        return enterFieldId;
    }

    /**
     * Sets the enter Field of this Instance
     *
     * @param enterFieldId the enter field's id
     */
    public void setEnterFieldId(int enterFieldId) {
        this.enterFieldId = enterFieldId;
    }

    /**
     * Returns the initial enter Portal of this Instance.
     *
     * @return the initial enter Portal of this Instance.
     */
    public int getEnterPortalId() {
        return enterPortalId;
    }

    /**
     * Sets the enter Portal of this Instance
     *
     * @param enterPortalId the enter portal's id
     */
    public void setEnterPortalId(int enterPortalId) {
        this.enterPortalId = enterPortalId;
    }

    /**
     * Returns the timer of the event that will warp everyone out of this Instance.
     *
     * @return warp out event timer
     */
    public ScheduledFuture<?> getWarpOutTimer() {
        return warpOutTimer;
    }

    /**
     * Adds a property to this Instance.
     *
     * @param key   the property's key
     * @param value the property's value
     */
    public void addProperty(String key, Object value) {
        getProperties().put(key, value);
    }

    /**
     * Checks if this Instance has a property.
     *
     * @param key the key to check
     * @return whether or not this Instance has the property
     */
    public boolean hasProperty(String key) {
        return getProperties().containsKey(key);
    }

    /**
     * Gets the value of a property, or null if there is none.
     *
     * @param key the key of the property
     * @return the value of the property, or null if there is none
     */
    public Object getProperty(String key) {
        return getProperties().getOrDefault(key, null);
    }

    /**
     * Set the value of a property already in the Instance.
     *
     * @param key   the key of the property
     * @param value the property's value
     */
    public void setProperty(String key, Object value) {
        if (getProperties().getOrDefault(key, null) != null) {
            getProperties().replace(key, value);
        } else {
            addProperty(key, value);
        }
    }

    /**
     * Returns whether or not this is a party's instance.
     *
     * @return whether or not this is a party's instance.
     */
    public boolean isParty() {
        return getInstanceType() == PARTY;
    }

    /**
     * Returns whether or not this is a solo instance.
     *
     * @return whether or not this is a solo instance.
     */
    public boolean isSolo() {
        return getInstanceType() == SOLO;
    }

    /**
     * Returns the Field according to the given field id. If there is currently no such Field, tries to make one and
     * add it to the active Field list.
     *
     * @param fieldID the field's id to get
     * @return the corresponding active Field
     */
    public Field getField(int fieldID) {
        Field field;
        if (getFields().containsKey(fieldID)) {
            field = getFields().get(fieldID);
        } else {
            field = FieldData.getFieldCopyById(fieldID);
            getFields().put(field.getId(), field);
        }
        return field;
    }

    /**
     * Clears all information from this Instance, stops any events, and warps every eligible Char to the forced return
     * Field.
     */
    public void clear() {
        int fieldId = getForcedReturn();
        int portalId = getForcedReturnPortalId();
        boolean useFieldReturn = fieldId == 0;
        if (getParty() != null) {
            getParty().setInstance(null);
        }
        stopEvents();
        for (Char chr : getChars().values()) {
            if (chr.getClient() == null) continue;
            Field field = chr.getField();
            chr.setDeathCount(-1);
            chr.getScriptManager().stopEvents(); // Stops the FixedRate Event from the Field Script
            chr.setInstance(null);
            if (getFields().containsKey(field.getId())) {
                if (useFieldReturn) {
                    chr.warp(field.getForcedReturn());
                } else if (portalId == -1) {
                    chr.warp(fieldId);
                } else {
                    chr.warp(fieldId, portalId);
                }
            }
            chr.getScriptManager().stopEvents();
        }
        getFields().clear();
        getChars().clear();
        Server.get().getInstances().remove(this);
    }

    /**
     * Stops all events of this Instance, and each of the eligible Char's ScriptManager's events.
     */
    public void stopEvents() {
        if (warpOutTimer != null) {
            warpOutTimer.cancel(false);
        }
        warpOutTimer = null;
    }

    /**
     * Sets the timeout of this Instance, after which every Char will be forced out. Creates a Clock for everyone.
     *
     * @param seconds the amount of seconds until every Char is forced out
     */
    public void setTimeout(int seconds, boolean showClock) {
        if (warpOutTimer != null) {
            warpOutTimer.cancel(false);
        }
        warpOutTimer = Server.get().getEventTimer().addEvent(this::clear, seconds, TimeUnit.SECONDS);
        warpOutTimeout = System.currentTimeMillis() + seconds * 1000L;
        if (showClock) {
            broadcast(FieldPacket.clock(ClockPacket.secondsClock(seconds)), 0);
        }
    }

    public void setTimeout(int seconds) {
        warpOutTimeout = System.currentTimeMillis() + seconds * 1000L;
    }

    /**
     * Returns the amount of seconds until this Instance closes.
     *
     * @return the remaining time
     */
    public int getRemainingTime() {
        return (int) ((warpOutTimeout - System.currentTimeMillis()) / 1000);
    }

    /**
     * Broadcasts a Packet to everyone in this Instance.
     *
     * @param packet the Packet to send
     */
    public void broadcast(OutPacket packet, int exceptCharID) {
        if (getChars().isEmpty() || (party != null && Server.get().getWorld().getPartyByPartyID(party.getId()) == null )) {
            return;
        }
        packet = net.swordie.ms.connection.protocol.codec.LegacyPacketTranslator.apply(packet); // 265: unchanged
        if (!Protocol.active().outboundGuard().permit(packet, () -> "instance")) {
            return;
        }
        final byte[] original = packet.getData();
        for (Char chr : getChars().values()) {
            Client c = chr.getClient();
            if (c != null && chr.getId() != exceptCharID) {
                byte[] perClient = original.clone();
                chr.getClient().getCh().writeAndFlush(perClient);
            }
        }
        NettyClient.debug("Instance", packet);
    }

    public int getForcedReturnPortalId() {
        return forcedReturnPortalId;
    }

    public void setForcedReturnPortalId(int forcedReturnPortalId) {
        this.forcedReturnPortalId = forcedReturnPortalId;
    }

    public int getAchieveRatio() {
        return achieveRatio;
    }

    public void setAchieveRatio(int achieveRatio) {
        this.achieveRatio = achieveRatio;
    }

    public Char getStigmaChar() {
        return stigmaChar;
    }

    public void setStigmaChar(Char stigmaChar) {
        this.stigmaChar = stigmaChar;
    }

    public int getCorruptionCount() {
        return corruptionCount;
    }

    public void setCorruptionCount(int corruptionCount) {
        this.corruptionCount = corruptionCount;
    }

    public List<HungryMutoRecipes> getMutoRecipes() {
        return mutoRecipes;
    }

    public void setMutoRecipes(List<HungryMutoRecipes> mutoRecipes) {
        this.mutoRecipes = mutoRecipes;
    }

    public int getVisitorStageCount() {
        return visitorStageCount;
    }

    public void setVisitorStageCount(int visitorStageCount) {
        this.visitorStageCount = visitorStageCount;
    }

    public int getMutoRecipe() {
        return mutoRecipe;
    }

    public void setMutoRecipe(int mutoRecipe) {
        this.mutoRecipe = mutoRecipe;
    }

    public boolean isCreateNewRecipe() {
        return isCreateNewRecipe;
    }

    public void setCreateNewRecipe(boolean createNewRecipe) {
        isCreateNewRecipe = createNewRecipe;
    }

    public int getMutoScore() {
        return mutoScore;
    }

    public void setMutoScore(int mutoScore) {
        this.mutoScore = mutoScore;
    }
}
