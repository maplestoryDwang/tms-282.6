package net.swordie.ms.world.tms282.newbie;

import net.swordie.ms.scripts.ScriptManagerImpl;

import java.util.List;
import java.util.Objects;

/**
 * {@link NewbieDialog} on the character's {@link ScriptManagerImpl} while {@link ScriptManagerImpl#startJavaDialog}
 * runs the flow: the same LP0960 say / yes-no / menu packets and CP012C answers as every NPC script (sendNext,
 * sendSayOkay, sendAskYesNo, a {@code #L} menu). A close / logout makes the manager throw its intended stop exception.
 */
public final class ScriptDialog implements NewbieDialog {
    private final ScriptManagerImpl sm;

    public ScriptDialog(ScriptManagerImpl sm) {
        this.sm = Objects.requireNonNull(sm, "sm");
    }

    @Override
    public void next(String text) {
        sm.sendNext(text);
    }

    @Override
    public void ok(String text) {
        sm.sendSayOkay(text);
    }

    @Override
    public boolean yesNo(String text) {
        return sm.sendAskYesNo(text);
    }

    @Override
    public int menu(String text, List<String> options) {
        StringBuilder b = new StringBuilder(text == null ? "" : text);
        for (int i = 0; i < options.size(); i++) {
            b.append("\r\n#L").append(i).append('#').append(options.get(i)).append("#l");
        }
        int answer = sm.sendNext(b.toString());
        return answer >= 0 && answer < options.size() ? answer : -1;
    }

    @Override
    public void warpAfter(int map, int portal) {
        sm.warp(map, portal);
    }
}
