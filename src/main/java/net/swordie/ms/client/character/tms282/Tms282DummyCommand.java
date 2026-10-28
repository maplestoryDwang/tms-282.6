package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.life.mob.TrainingDummies;

import java.util.List;
import java.util.Optional;

/**
 * {@code @dummy [bag item id]}: places a training dummy at the admin's position without the summon bag (for damage
 * tests), with the same rules as the bag scripts ({@link TrainingDummies}). Default bag 2434325 (橡樹 100%, dummy
 * 9305650). The spawn itself reaches the field through the profile's mob codecs; the reply is text only.
 */
public final class Tms282DummyCommand implements Tms282ChatCommand {
    public static final int DEFAULT_BAG = 2434325;

    @Override
    public String name() {
        return "dummy";
    }

    @Override
    public int minAccountType() {
        return 3;
    }

    @Override
    public String usage() {
        return "@dummy [2434325..2434330 | 2436812 | 2436813 | 2437511 | 2630283..2630285]";
    }

    @Override
    public Reply execute(Char chr, List<String> args) {
        if (args.size() > 1) {
            throw new IllegalArgumentException("at most one item id");
        }
        int bag = DEFAULT_BAG;
        if (!args.isEmpty()) {
            try {
                bag = Integer.parseInt(args.get(0));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("not a number: " + args.get(0));
            }
        }
        if (TrainingDummies.mobFor(bag).isEmpty()) {
            throw new IllegalArgumentException("not a summon bag: " + bag);
        }
        Optional<TrainingDummies.Refusal> refusal = TrainingDummies.summon(chr, bag);
        if (refusal.isPresent()) {
            return Reply.text(refusal.get().message(), "@dummy " + bag + ": refused " + refusal.get());
        }
        return Reply.text("已召喚稻草人（15 分鐘後消失）。", "@dummy " + bag + ": dummy " + TrainingDummies.mobFor(bag).getAsInt()
                + " at " + chr.getPosition());
    }
}
