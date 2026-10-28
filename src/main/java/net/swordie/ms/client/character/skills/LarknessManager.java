package net.swordie.ms.client.character.skills;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.info.LarknessInfo;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.UserLocal;

/**
 * Created on 2/10/2018.
 */
public class LarknessManager {
    private final Char chr;
    private final LarknessInfo darkInfo = new LarknessInfo(20040217, 0, true);
    private final LarknessInfo lightInfo = new LarknessInfo(20040216, 0, false);
    private int gauge;
    private int feathers;
    private boolean dark;

    public LarknessManager(Char chr) {
        this.chr = chr;
    }

    public LarknessInfo getDarkInfo() {
        return darkInfo;
    }

    public LarknessInfo getLightInfo() {
        return lightInfo;
    }

    public int getGauge() {
        return gauge;
    }

    public void setGauge(int gauge) {
        this.gauge = gauge;
    }

    public int getFeathers() {
        return feathers;
    }

    public void setFeathers(int feathers) {
        this.feathers = feathers;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(isDark() ? Math.min(getGauge(), 10000) : Math.max(getGauge(), 0));
        outPacket.encodeInt(isDark() ? 0 : 10000);
        outPacket.encodeInt(getFeathers());
    }

    public boolean isDark() {
        return dark;
    }

    public void setDark(boolean dark) {
        this.dark = dark;
    }

    /**
     * Adds a specified amount to the given gauge. Note: Max value of a gauge is 10000.
     *
     * @param amount The amount to add to the gauge
     * @param dark   Which gauge to add the amount to
     */
    public void addGauge(int amount, boolean dark) {
        if (dark) {
            setGauge(getGauge() - amount);
        } else {
            setGauge(getGauge() + amount);
        }
        if (getGauge() == 10000 || getGauge() == 0) {
            setFeathers(1);
        }
        updateInfo();
    }

    /**
     * Changes mode to dark if light, and vice versa. Includes decrementing feathers, and updating
     * the client.
     */
    public void changeMode() {
        if (getFeathers() > 0) {
            setFeathers(getFeathers() - 1);
        }
        setDark(!isDark());
        updateInfo();
    }

    /**
     * Sends a packet to update the client's larkness state
     */
    public void updateInfo() {
        chr.write(UserLocal.incLarknessReponse(this, isDark()));
    }
}
