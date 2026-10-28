package net.swordie.ms.loaders.containerclasses;

import net.swordie.ms.life.drop.DropInfo;
import net.swordie.ms.util.Rect;

import java.util.HashSet;
import java.util.Set;

/**
 * Created on 4/21/2018.
 */
public class ReactorInfo {
    private int id;
    private int link;
    private int level;
    private int resetTime;
    private int overlapHitTime;
    private int actMark;
    private int hitCount;
    private int overlapHit;
    private int maxHitCount;
    private int quest;
    private String info = "";
    private String name = "";
    private String viewName = "";
    private String action = "";
    private boolean notFatigue;
    private boolean dcMark;
    private boolean removeInFieldSet;
    private boolean activateByTouch;
    private boolean notHittable;
    private Rect rect;
    private Set<DropInfo> drops = new HashSet<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public boolean isDcMark() {
        return dcMark;
    }

    public void setDcMark(boolean dcMark) {
        this.dcMark = dcMark;
    }

    public int getLink() {
        return link;
    }

    public void setLink(int link) {
        this.link = link;
    }

    public boolean isActivateByTouch() {
        return activateByTouch;
    }

    public void setActivateByTouch(boolean activateByTouch) {
        this.activateByTouch = activateByTouch;
    }

    public boolean isRemoveInFieldSet() {
        return removeInFieldSet;
    }

    public void setRemoveInFieldSet(boolean removeInFieldSet) {
        this.removeInFieldSet = removeInFieldSet;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isNotFatigue() {
        return notFatigue;
    }

    public void setNotFatigue(boolean notFatigue) {
        this.notFatigue = notFatigue;
    }

    public int getResetTime() {
        return resetTime;
    }

    public void setResetTime(int resetTime) {
        this.resetTime = resetTime;
    }

    public boolean isNotHittable() {
        return notHittable;
    }

    public void setNotHittable(boolean notHittable) {
        this.notHittable = notHittable;
    }

    public Rect getRect() {
        return rect;
    }

    public void setRect(Rect rect) {
        this.rect = rect;
    }

    public int getActMark() {
        return actMark;
    }

    public void setActMark(int actMark) {
        this.actMark = actMark;
    }

    public int getHitCount() {
        return hitCount;
    }

    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }

    public int getOverlapHit() {
        return overlapHit;
    }

    public void setOverlapHit(int overlapHit) {
        this.overlapHit = overlapHit;
    }

    public int getOverlapHitTime() {
        return overlapHitTime;
    }

    public void setOverlapHitTime(int overlapHitTime) {
        this.overlapHitTime = overlapHitTime;
    }

    public int getMaxHitCount() {
        return maxHitCount;
    }

    public void setMaxHitCount(int maxHitCount) {
        this.maxHitCount = maxHitCount;
    }

    public int getQuest() {
        return quest;
    }

    public void setQuest(int quest) {
        this.quest = quest;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Set<DropInfo> getDrops() {
        return drops;
    }

    public void setDrops(Set<DropInfo> drops) {
        this.drops = drops;
    }

    @Override
    public String toString() {
        return "ReactorInfo{" +
                "id=" + id +
                '}';
    }
}
