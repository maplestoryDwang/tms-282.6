package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.tms282.Tms282ChatCommands;

import java.time.Duration;
import java.util.Objects;

/**
 * Collaborators of the TMS 282.3 in-field handlers ({@link Tms282FieldHandlers}): persistence, the chat audience,
 * the chat-command extension point, the skill services and the HP/MP write interval. {@link #standard()} is the server's; checks
 * replace single parts with the {@code with*} methods.
 *
 * @param store            database writes (quest, key map, hp/mp)
 * @param audience         who receives LP03D5 chat lines
 * @param chatCommands     '@' chat commands; the server's are {@link Tms282ChatCommands#admin} (@lv, @sp; accounttype >= 3)
 * @param skills           CP0196 / CP04B4 services (skill agent's Tms282SkillService / Tms282LevelService)
 * @param hpMpSaveInterval CP0193 hp/mp are written at most this often (probe HP_MP_SAVE_NANOS, 60 s); the write on
 *                         disconnect is the 265 {@code Client.finalizeSession} character save
 */
public record Tms282FieldServices(Tms282FieldStore store, Tms282FieldAudience audience, Tms282ChatCommandHook chatCommands,
                                  Tms282SkillRuntime skills, Duration hpMpSaveInterval) {
    public static final Duration HP_MP_SAVE_INTERVAL = Duration.ofSeconds(60);

    public Tms282FieldServices {
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(audience, "audience");
        Objects.requireNonNull(chatCommands, "chatCommands");
        Objects.requireNonNull(skills, "skills");
        Objects.requireNonNull(hpMpSaveInterval, "hpMpSaveInterval");
        if (hpMpSaveInterval.isNegative() || hpMpSaveInterval.isZero()) {
            throw new IllegalArgumentException("hp/mp save interval must be positive");
        }
    }

    /**
     * 265 database, field broadcast, the admin chat commands (as the probe: {@code Tms282ChatCommands.admin} over the
     * same level service), the standard skill runtime, 60 s hp/mp interval.
     */
    public static Tms282FieldServices standard() {
        Tms282SkillRuntime skills = Tms282SkillRuntime.standard();
        return new Tms282FieldServices(Tms282FieldStore.SQL, Tms282FieldAudience.FIELD,
                Tms282ChatCommandHook.of(Tms282ChatCommands.admin(skills.levels())), skills, HP_MP_SAVE_INTERVAL);
    }

    public Tms282FieldServices withStore(Tms282FieldStore store) {
        return new Tms282FieldServices(store, audience, chatCommands, skills, hpMpSaveInterval);
    }

    public Tms282FieldServices withAudience(Tms282FieldAudience audience) {
        return new Tms282FieldServices(store, audience, chatCommands, skills, hpMpSaveInterval);
    }

    public Tms282FieldServices withChatCommands(Tms282ChatCommandHook chatCommands) {
        return new Tms282FieldServices(store, audience, chatCommands, skills, hpMpSaveInterval);
    }

    /** Other skill services; the chat commands are kept (pass new ones with {@link #withChatCommands}). */
    public Tms282FieldServices withSkills(Tms282SkillRuntime skills) {
        return new Tms282FieldServices(store, audience, chatCommands, skills, hpMpSaveInterval);
    }

    public Tms282FieldServices withHpMpSaveInterval(Duration interval) {
        return new Tms282FieldServices(store, audience, chatCommands, skills, interval);
    }
}
