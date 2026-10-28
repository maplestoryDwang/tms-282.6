package net.swordie.ms.connection.packet;

import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.loaders.Tms282MakeCharInfo;

import java.util.Objects;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

/**
 * Server-side checks for a TMS 282.3 CP008E creation request, before any character is built or saved.
 * <p>
 * The checks run in a fixed order and the first failure wins:
 * <ol>
 *     <li>the native race maps to a {@link JobConstants.LoginJob} whose creation is enabled ("job not enabled")</li>
 *     <li>race, gender and items form a look listed in the 282 MakeCharInfo ("look not listed in 282 MakeCharInfo")</li>
 *     <li>the items contain a face and a hair ("missing face or hair")</li>
 *     <li>the skin is in range ("invalid skin")</li>
 *     <li>the name passes {@link Tms282CharacterNameRequest#isValidName} ("invalid name", LP0017 0x7B)</li>
 *     <li>the name is still free ("name in use", LP0017 0x7A)</li>
 *     <li>the account has a free character slot ("no free character slot")</li>
 * </ol>
 * A rejection is answered through LP0017 with {@link Result#nameResult()} ({@link #NAME_IN_USE},
 * {@link #NAME_NOT_ALLOWED}, else {@link #REJECTED}). Name availability is injected as a {@code Predicate} so the checks need no database.
 */
public final class Tms282CreateCharacterValidator {
    /*
     * LP0017 results, from the 282.3/282.4 reader's jump table (3CA85F0/3CA85CC; zero-create-0930/README.md):
     * 0 ok, 0x7A -> notice 3 "name in use", 0x79/0x7B -> notice 6 "name not allowed", 0x45/0x46/0x47/0x8B special,
     * every other value (including the old 1 and 2) -> notice 0x0C "failed for an unknown reason".
     */
    /** LP0017 result for an accepted request. */
    public static final int NAME_OK = 0;
    /** LP0017 result for a name that is already taken (notice 3). */
    public static final int NAME_IN_USE = 0x7A;
    /** LP0017 result for a name the rules do not allow (notice 6). */
    public static final int NAME_NOT_ALLOWED = 0x7B;
    /** LP0017 result for every other rejection (notice 0x0C, unknown failure). */
    public static final int REJECTED = 2;

    private Tms282CreateCharacterValidator() {
    }

    /**
     * Outcome of {@link #validate}. {@code face}/{@code hair} are -1 when the items hold none;
     * {@code loginJob} is null for an unknown race. {@code problem} is null exactly when {@code ok}.
     */
    public record Result(boolean ok, String problem, int nameResult, int face, int hair,
                         JobConstants.LoginJob loginJob) {
        static Result rejected(String problem, int nameResult, int face, int hair, JobConstants.LoginJob loginJob) {
            return new Result(false, Objects.requireNonNull(problem, "problem"), nameResult, face, hair, loginJob);
        }
    }

    /** Validation with job creation flags from {@link ServerRuntimeConfig#isJobCreationEnabled}. */
    public static Result validate(Tms282CreateCharacterRequest request, Tms282MakeCharInfo info,
                                  Predicate<String> nameAvailable, int usedSlots, int maxSlots) {
        return validate(request, info, nameAvailable, usedSlots, maxSlots, ServerRuntimeConfig::isJobCreationEnabled);
    }

    public static Result validate(Tms282CreateCharacterRequest request, Tms282MakeCharInfo info,
                                  Predicate<String> nameAvailable, int usedSlots, int maxSlots, Charset charset) {
        return validate(request, info, nameAvailable, usedSlots, maxSlots,
                ServerRuntimeConfig::isJobCreationEnabled, charset);
    }

    /** As {@link #validate(Tms282CreateCharacterRequest, Tms282MakeCharInfo, Predicate, int, int)} with injected job flags. */
    public static Result validate(Tms282CreateCharacterRequest request, Tms282MakeCharInfo info,
                                  Predicate<String> nameAvailable, int usedSlots, int maxSlots,
                                  Predicate<JobConstants.LoginJob> jobEnabled) {
        return validate(request, info, nameAvailable, usedSlots, maxSlots, jobEnabled, StandardCharsets.UTF_8);
    }

    public static Result validate(Tms282CreateCharacterRequest request, Tms282MakeCharInfo info,
                                  Predicate<String> nameAvailable, int usedSlots, int maxSlots,
                                  Predicate<JobConstants.LoginJob> jobEnabled, Charset charset) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(info, "info");
        Objects.requireNonNull(nameAvailable, "nameAvailable");
        Objects.requireNonNull(jobEnabled, "jobEnabled");
        Objects.requireNonNull(charset, "charset");
        JobConstants.LoginJob loginJob = JobConstants.LoginJob.getLoginJobById(request.race());
        int[] items = request.items();
        int face = face(items);
        int hair = hair(items);
        String problem = null;
        int nameResult = REJECTED;
        if (loginJob == null || !jobEnabled.test(loginJob)) {
            problem = "job not enabled";
        } else if (!net.swordie.ms.client.character.keys.Tms282CreationKeys.isSupported(request.keySettingType())) {
            problem = "unsupported creation key type";
        } else if (!info.isValidLook(request.race(), request.gender(), items)) {
            problem = "look not listed in 282 MakeCharInfo";
        } else if (face < 0 || hair < 0) {
            problem = "missing face or hair";
        } else if ((request.skin() < 0 || request.skin() > ItemConstants.MAX_SKIN)
                && !info.isNativeDefaultSkin(request.race(), request.skin())) {
            problem = "invalid skin";
        } else if (!Tms282CharacterNameRequest.isValidName(request.name(), charset)) {
            problem = "invalid name";
            nameResult = NAME_NOT_ALLOWED;
        } else if (!nameAvailable.test(request.name())) {
            problem = "name in use";
            nameResult = NAME_IN_USE;
        } else if (usedSlots >= maxSlots) {
            problem = "no free character slot";
        }
        return problem == null
                ? new Result(true, null, NAME_OK, face, hair, loginJob)
                : Result.rejected(problem, nameResult, face, hair, loginJob);
    }

    /** The last item in a face range, or -1 (the probe's selection rule). */
    public static int face(int[] items) {
        int face = -1;
        for (int item : items) {
            if (isFace(item)) {
                face = item;
            }
        }
        return face;
    }

    /** The last item in a hair range that is not also a face, or -1. */
    public static int hair(int[] items) {
        int hair = -1;
        for (int item : items) {
            if (!isFace(item) && isHair(item)) {
                hair = item;
            }
        }
        return hair;
    }

    private static boolean isFace(int item) {
        return item >= ItemConstants.MIN_FACE && item <= ItemConstants.MAX_FACE
                || item >= ItemConstants.MIN_FACE_2 && item <= ItemConstants.MAX_FACE_2;
    }

    private static boolean isHair(int item) {
        return item >= ItemConstants.MIN_HAIR && item <= ItemConstants.MAX_HAIR
                || item >= ItemConstants.MIN_HAIR_2 && item <= ItemConstants.MAX_HAIR_2;
    }
}
