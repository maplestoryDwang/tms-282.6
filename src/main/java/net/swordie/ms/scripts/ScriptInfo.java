package net.swordie.ms.scripts;

import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.util.DataPrinter;

import javax.script.Bindings;
import javax.script.Invocable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static net.swordie.ms.ServerConstants.EXPIRED_SCRIPT_TIME;
import static net.swordie.ms.scripts.ScriptManagerImpl.INTENDED_NPE_MSG;

public class ScriptInfo {
    private final ReentrantLock lock = new ReentrantLock();
    private final java.util.concurrent.locks.Condition responseChanged = lock.newCondition();
    private ScriptType scriptType;
    private Bindings bindings;
    private int parentID;
    private String scriptName;
    private Invocable invocable;
    private Integer responseInteger;
    private String responseString;
    private Mob responseMob;
    private int objectID;
    private String fileDir;
    private volatile boolean isActive;

    public ScriptInfo(ScriptType scriptType, Bindings bindings, int parentID, String scriptName) {
        this.scriptType = scriptType;
        this.parentID = parentID;
        this.scriptName = scriptName;
        this.bindings = bindings;
        this.responseInteger = null;
        this.responseString = null;
        this.responseMob = null;
    }

    public ScriptType getScriptType() {
        return scriptType;
    }

    public void setScriptType(ScriptType scriptType) {
        this.scriptType = scriptType;
    }

    public Bindings getBindings() {
        return bindings;
    }

    public void setBindings(Bindings bindings) {
        this.bindings = bindings;
    }

    public int getParentID() {
        return parentID;
    }

    public void setParentID(int parentID) {
        this.parentID = parentID;
    }

    public String getScriptName() {
        return scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    public Invocable getInvocable() {
        return invocable;
    }

    public void setInvocable(Invocable invocable) {
        this.invocable = invocable;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public int getObjectID() {
        return objectID;
    }

    public void setObjectID(int objectID) {
        this.objectID = objectID;
    }

    public String getFileDir() {
        return fileDir;
    }

    public void setFileDir(String fileDir) {
        this.fileDir = fileDir;
    }

    public void reset() {
        lock.lock();
        try {
            isActive = false;
            responseInteger = null;
            responseString = null;
            responseMob = null;
            setParentID(0);
            setScriptName("");
            setInvocable(null);
            responseChanged.signalAll();
        } finally { lock.unlock(); }
    }

    public void addResponseInteger(Integer response) {
        lock.lock();
        try {
            if (!isActive) return;
            responseInteger = response;
            responseChanged.signalAll();
        } finally { lock.unlock(); }
    }

    public Integer awaitResponseInteger(int timeout) {
        long remaining = java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(timeout > 0 ? timeout : EXPIRED_SCRIPT_TIME);
        long start = System.nanoTime();
        lock.lock();
        try {
            if (!isActive) return null;
            System.out.printf("[Started Wait] script %s%n", getScriptName());
            while (isActive && responseInteger == null && remaining > 0) {
                try { remaining = responseChanged.awaitNanos(remaining); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
            }
            Integer result = isActive ? responseInteger : null;
            responseInteger = null;
            System.out.printf("[Completed Wait] script %s trong %d ms%n", getScriptName(),
                    java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            return result;
        } finally { lock.unlock(); }
    }

    public void addResponseString(String response) {
        lock.lock();
        try {
            if (!isActive) return;
            responseString = response;
            responseChanged.signalAll();
        } finally { lock.unlock(); }
    }

    public String awaitResponseString(int timeout) {
        long remaining = java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(timeout > 0 ? timeout : EXPIRED_SCRIPT_TIME);
        long start = System.nanoTime();
        lock.lock();
        try {
            if (!isActive) return null;
            System.out.printf("[Started Wait] script %s%n", getScriptName());
            while (isActive && responseString == null && remaining > 0) {
                try { remaining = responseChanged.awaitNanos(remaining); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
            }
            String result = isActive ? responseString : null;
            responseString = null;
            System.out.printf("[Completed Wait] script %s trong %d ms%n", getScriptName(),
                    java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            return result;
        } finally { lock.unlock(); }
    }

    public void addResponseMob(Mob response) {
        lock.lock();
        try {
            if (!isActive) return;
            responseMob = response;
            responseChanged.signalAll();
        } finally { lock.unlock(); }
    }

    public Mob awaitResponseMob(int timeout) {
        long remaining = java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(timeout > 0 ? timeout : EXPIRED_SCRIPT_TIME);
        long start = System.nanoTime();
        lock.lock();
        try {
            if (!isActive) return null;
            System.out.printf("[Started Wait] script %s%n", getScriptName());
            while (isActive && responseMob == null && remaining > 0) {
                try { remaining = responseChanged.awaitNanos(remaining); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
            }
            Mob result = isActive ? responseMob : null;
            responseMob = null;
            System.out.printf("[Completed Wait] script %s trong %d ms%n", getScriptName(),
                    java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            return result;
        } finally { lock.unlock(); }
    }

}
