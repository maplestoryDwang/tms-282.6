package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.account.CharactersAction;
import net.swordie.ms.connection.api.launcher.account.DashboardAction;
import net.swordie.ms.connection.api.launcher.account.MeAction;
import net.swordie.ms.connection.api.launcher.account.StatusAction;
import net.swordie.ms.connection.api.launcher.daily.DailyRewardsAction;
import net.swordie.ms.connection.api.launcher.daily.DailyStatusAction;
import net.swordie.ms.connection.api.launcher.news.NewsAction;
import net.swordie.ms.connection.api.launcher.release.LauncherVersionAction;
import net.swordie.ms.connection.api.launcher.release.ManifestMetaAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * {@code action} name -> {@link LauncherAction}, plus the {@link LauncherPolicy} pipeline every dispatch runs.
 * {@link #standard()} is the one table LocalAccountCli and HttpAuthApi share; {@link #only(String...)} gives an
 * endpoint a restricted view (the legacy {@code /api/v2/account} exposes login / register / reset only).
 */
public final class LauncherActionRegistry {
    private static final Pattern NAME = Pattern.compile("[a-z][A-Za-z0-9]{0,31}(\\.[a-z][A-Za-z0-9]{0,31})?");
    private static final LauncherActionRegistry STANDARD = builder()
            .register(new LoginAction())
            .register(new RegisterAction())
            .register(new ResetAction())
            .register(new SessionLogoutAction())
            // P5b: read-only views (status public; me / characters / dashboard need a session)
            .register(new StatusAction())
            .register(new MeAction())
            .register(new CharactersAction())
            .register(new DashboardAction())
            // P6: daily gift (read-only: rewards public, status needs a session) and news (public)
            .register(new DailyRewardsAction())
            .register(new DailyStatusAction())
            // 2026-10-08: icons of the sign-in rewards (public; only the board's and the streak bonuses' items)
            .register(new net.swordie.ms.connection.api.launcher.daily.ItemIconAction())
            .register(new NewsAction())
            // P8 / P8b: the launcher release (release.json, signed exe download) and the client file manifest (public)
            .register(new LauncherVersionAction())
            .register(new ManifestMetaAction())
            // P7: account writes, live name check, gift codes (daily claim stays in the game: DECISIONS D3)
            .register(new ChangePasswordAction())
            .register(new ChangePinAction())
            .register(new CheckNameAction())
            .register(new net.swordie.ms.connection.api.launcher.gift.GiftRedeemAction())
            .register(new net.swordie.ms.connection.api.launcher.gift.GiftHistoryAction())
            .register(new net.swordie.ms.connection.api.launcher.checkin.CheckinStatusAction())
            .register(new net.swordie.ms.connection.api.launcher.checkin.CheckinAction())
            // 1.2.3: the signed client file list (public; launcherVersion carries only its summary)
            .register(new net.swordie.ms.connection.api.launcher.release.ClientFilesAction())
            // 1.2.8: the problem report button (session; stored for the admin console)
            .register(new net.swordie.ms.connection.api.launcher.report.ReportAction())
            // 1.2.8: the paper doll of one of the caller's characters (session, read)
            .register(new net.swordie.ms.connection.api.launcher.account.AvatarAction())
            // launcher 1.3: the outfit wardrobe (the in-game @outfit shares; list / picture read, like writes)
            .register(new net.swordie.ms.connection.api.launcher.outfit.OutfitsAction())
            .register(new net.swordie.ms.connection.api.launcher.outfit.OutfitImageAction())
            .register(new net.swordie.ms.connection.api.launcher.outfit.OutfitLikeAction())
            // launcher 1.4: the player's own account closure and the select screen's 24 h wait (session + current password)
            .register(new DeleteCharacterAction())
            .register(new DeleteAccountAction())
            .build();

    private final Map<String, LauncherAction> actions;
    private final List<LauncherPolicy> policies;

    private LauncherActionRegistry(Map<String, LauncherAction> actions, List<LauncherPolicy> policies) {
        this.actions = Collections.unmodifiableMap(actions);
        this.policies = List.copyOf(policies);
    }

    public static LauncherActionRegistry standard() {
        return STANDARD;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Dispatch: unknown action -> invalidInput; each policy may refuse; then the action runs. */
    public LauncherResult dispatch(LauncherContext ctx, JsonNode request) throws Exception {
        LauncherAction action = actions.get(LauncherJson.text(request, "action"));
        if (action == null) {
            return LauncherResult.fail(LauncherCodes.INVALID_INPUT);
        }
        for (LauncherPolicy policy : policies) {
            LauncherResult refusal = policy.check(action, ctx, request);
            if (refusal != null) {
                return refusal;
            }
        }
        return action.execute(ctx, request);
    }

    public Optional<LauncherAction> find(String name) {
        return Optional.ofNullable(actions.get(name));
    }

    /** Registered names in registration order. */
    public List<String> names() {
        return List.copyOf(actions.keySet());
    }

    /** {@code /api/ping} features: the actions a remote launcher may call. */
    public List<String> features() {
        List<String> out = new ArrayList<>();
        for (LauncherAction a : actions.values()) {
            if (a.remoteAllowed()) {
                out.add(a.name());
            }
        }
        return List.copyOf(out);
    }

    /**
     * Capability flags (fixes10, 2026-10-05) that {@code /api/ping} lists after the action names: an optional request
     * field an action understands, named {@code <action>.<capability>}. Listed only when the action is registered and
     * remote-allowed. {@code dailyStatus.month}: dailyStatus takes {@code "month":"YYYY-MM"};
     * {@code clientFiles.pathRules2}: launcherVersion / clientFiles take {@code "pathRules":2} (segments may start with
     * {@code _}).
     */
    public static final Map<String, List<String>> CAPABILITIES = Map.of(
            "dailyStatus", List.of("dailyStatus.month"),
            "clientFiles", List.of("clientFiles.pathRules2"));

    /** {@code /api/ping} features: {@link #features()}, then the {@link #CAPABILITIES} of those actions. */
    public List<String> pingFeatures() {
        List<String> out = new ArrayList<>(features());
        for (LauncherAction a : actions.values()) {
            if (a.remoteAllowed()) {
                out.addAll(CAPABILITIES.getOrDefault(a.name(), List.of()));
            }
        }
        return List.copyOf(out);
    }

    /** The same actions and policies restricted to {@code names} (each must exist). */
    public LauncherActionRegistry only(String... names) {
        Map<String, LauncherAction> subset = new LinkedHashMap<>();
        for (String name : names) {
            LauncherAction a = actions.get(name);
            if (a == null) {
                throw new IllegalArgumentException("unknown action " + name);
            }
            subset.put(name, a);
        }
        return new LauncherActionRegistry(subset, policies);
    }

    public static final class Builder {
        private final Map<String, LauncherAction> actions = new LinkedHashMap<>();
        private List<LauncherPolicy> policies = LauncherPolicies.standard();

        private Builder() {
        }

        public Builder register(LauncherAction action) {
            Objects.requireNonNull(action, "action");
            String name = action.name();
            if (name == null || !NAME.matcher(name).matches()) {
                throw new IllegalArgumentException("invalid action name " + name);
            }
            if (actions.containsKey(name)) {
                throw new IllegalArgumentException("duplicate action " + name);
            }
            if (!LauncherRateLimits.BUCKETS.contains(action.rateBucket())) {
                throw new IllegalArgumentException(name + ": unknown rate bucket " + action.rateBucket());
            }
            Objects.requireNonNull(action.transport(), name + ": transport");
            actions.put(name, action);
            return this;
        }

        public Builder policies(List<LauncherPolicy> policies) {
            this.policies = List.copyOf(policies);
            return this;
        }

        public LauncherActionRegistry build() {
            return new LauncherActionRegistry(new LinkedHashMap<>(actions), policies);
        }
    }
}
