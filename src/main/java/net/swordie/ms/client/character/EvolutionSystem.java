package net.swordie.ms.client.character;

import net.swordie.ms.connection.packet.EvolvingPacket;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.scripts.ScriptManagerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class EvolutionSystem {

    private final Char chr;
    private ScheduledFuture<?> startEvent;

    public EvolutionSystem(Char chr) {
        this.chr = chr;
    }

    public void start() {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getParty() != null) {
            sm.chatScript("請先退出隊伍才能進入進化系統。");
        } else if (chr.getLevel() >= 100 && chr.getLevel() <= 200) {
            List<Core> cores = chr.getCores().stream().filter(core -> core.getSlotType() == 0).toList();
            List<Integer> coreList = new ArrayList<>(10);
            for (Core core : cores) {
                coreList.add(core.getCoreID());
            }
            if (coreList.size() < 10) {
                for (int i = 0; i < 10; i++) {
                    coreList.add(0);
                }
            }
            chr.write(EvolvingPacket.evolvingSystemUIOperation(true, 1));
            this.startEvent = chr.getTimer().addEvent(() -> {
                chr.write(EvolvingPacket.tryEnterEvolvingResult(coreList));
                sm.warpInstanceIn(chr, GameConstants.EVOLVING_CENTRAL_CONTROL_MAP, true);
                sm.setInstanceTime(GameConstants.EVOLVING_TIME, GameConstants.EVOLVING_ENTRANCE_MAP);
                chr.write(EvolvingPacket.evolvingSystemUIOperation(false, 0));
                for (int i = 1820; i <= 1829; i++) {
                    if (chr.hasQuest(i) || chr.hasQuestCompleted(i)) {
                        chr.deleteQuest(i);
                    }
                }
                sm.modifiedCharacter();
            }, 5, TimeUnit.SECONDS);
        } else {
            sm.chatScript("有隊員的等級低於 100 或高於 200。");
        }
    }

    public void exit() {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (sm.sendAskYesNo("已經要放棄了嗎？")) {
            for (int i = 1820; i <= 1829; i++) {
                if (chr.hasQuest(i) || chr.hasQuestCompleted(i)) {
                    chr.deleteQuest(i);
                }
            }
            sm.warpInstanceOut(chr, GameConstants.EVOLVING_ENTRANCE_MAP);
        }
    }

    public void event() {
        if (startEvent != null) {
            startEvent.cancel(true);
        }
        chr.write(EvolvingPacket.evolvingSystemUIOperation(false, 0));
    }

    public void end() {
        if (chr.getFieldID() >= GameConstants.EVOLVING_LINK_MAP_1 && chr.getFieldID() <= GameConstants.EVOLVING_CENTRAL_CONTROL_MAP) {
            ScriptManagerImpl sm = chr.getScriptManager();
            for (int i = 1820; i <= 1829; i++) {
                if (chr.hasQuest(i) || chr.hasQuestCompleted(i)) {
                    chr.deleteQuest(i);
                }
            }
            sm.warpInstanceOut(chr, GameConstants.EVOLVING_ENTRANCE_MAP);
        }
    }
}