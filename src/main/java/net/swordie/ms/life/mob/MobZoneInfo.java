package net.swordie.ms.life.mob;

import net.swordie.ms.util.Rect;

import java.util.HashMap;
import java.util.Map;

public class MobZoneInfo {

    private int mobId;
    private In in;
    private Out out;
    private Map<Integer, Rect> mobZoneRect = new HashMap<>();

    public MobZoneInfo() {
    }

    public MobZoneInfo(int mobId, In in, Out out, Map<Integer, Rect> mobZoneRect) {
        this.mobId = mobId;
        this.in = in;
        this.out = out;
        this.mobZoneRect = mobZoneRect;
    }

    public int getMobId() {
        return mobId;
    }

    public void setMobId(int mobId) {
        this.mobId = mobId;
    }

    public In getIn() {
        return in;
    }

    public void setIn(In in) {
        this.in = in;
    }

    public Out getOut() {
        return out;
    }

    public void setOut(Out out) {
        this.out = out;
    }

    public Map<Integer, Rect> getMobZoneRect() {
        return mobZoneRect;
    }

    public void setMobZoneRect(Map<Integer, Rect> mobZoneRect) {
        this.mobZoneRect = mobZoneRect;
    }

    public static class In {
        private int damage;
        private int consume;
        private int autoDec;
        private int healRate;

        public In() {
        }

        public In(int damage, int consume, int autoDec, int healRate) {
            this.damage = damage;
            this.consume = consume;
            this.autoDec = autoDec;
            this.healRate = healRate;
        }

        public int getDamage() {
            return damage;
        }

        public void setDamage(int damage) {
            this.damage = damage;
        }

        public int getConsume() {
            return consume;
        }

        public void setConsume(int consume) {
            this.consume = consume;
        }

        public int getAutoDec() {
            return autoDec;
        }

        public void setAutoDec(int autoDec) {
            this.autoDec = autoDec;
        }

        public int getHealRate() {
            return healRate;
        }

        public void setHealRate(int healRate) {
            this.healRate = healRate;
        }
    }

    public static class Out {
        private int damage;
        private int consume;
        private int autoDec;
        private int healRate;

        public Out() {
        }

        public Out(int damage, int consume, int autoDec, int healRate) {
            this.damage = damage;
            this.consume = consume;
            this.autoDec = autoDec;
            this.healRate = healRate;
        }

        public int getDamage() {
            return damage;
        }

        public void setDamage(int damage) {
            this.damage = damage;
        }

        public int getConsume() {
            return consume;
        }

        public void setConsume(int consume) {
            this.consume = consume;
        }

        public int getAutoDec() {
            return autoDec;
        }

        public void setAutoDec(int autoDec) {
            this.autoDec = autoDec;
        }

        public int getHealRate() {
            return healRate;
        }

        public void setHealRate(int healRate) {
            this.healRate = healRate;
        }
    }

}
