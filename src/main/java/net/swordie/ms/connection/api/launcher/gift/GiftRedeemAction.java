package net.swordie.ms.connection.api.launcher.gift;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherAction;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherContext;
import net.swordie.ms.connection.api.launcher.LauncherRateLimits;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.LauncherSession;
import net.swordie.ms.connection.api.launcher.LauncherTransport;
import net.swordie.ms.connection.api.launcher.LocalDatabaseGuard;

import java.util.Map;

/**
 * {@code giftRedeem} (P7, session) {code, characterId}: redeems a gift code for the session's account onto one of
 * its characters ({@link GiftCodeService#redeem}); the items are queued and handed over at that character's next
 * field entry ({@link LauncherRewardDelivery}). Answers {@code redeemed} (title, items, character, delivery
 * "nextLogin"), {@code alreadyRedeemed}, {@code invalidOrExpired}, {@code noReceiver}, {@code rateLimited}
 * (retryAfterSec: too many failed codes for this account in an hour) or {@code invalidInput} (code missing / not a
 * string / longer than 64, characterId missing). SERVER_WHEN_RUNNING; bucket {@link LauncherRateLimits#GIFT}.
 */
public final class GiftRedeemAction implements LauncherAction {

    @Override
    public String name() {
        return "giftRedeem";
    }

    @Override
    public boolean requiresSession() {
        return true;
    }

    @Override
    public boolean mutating() {
        return true;
    }

    @Override
    public String rateBucket() {
        return LauncherRateLimits.GIFT;
    }

    @Override
    public boolean remoteAllowed() {
        return true;
    }

    @Override
    public LauncherTransport transport() {
        return LauncherTransport.SERVER_WHEN_RUNNING;
    }

    @Override
    public LauncherResult execute(LauncherContext ctx, JsonNode request) throws Exception {
        LauncherSession session = ctx.session();
        if (session == null) {
            return LauncherResult.fail(LauncherCodes.NEED_SESSION);
        }
        JsonNode code = request == null ? null : request.get("code");
        JsonNode chr = request == null ? null : request.get("characterId");
        if (code == null || !code.isTextual() || code.textValue().isEmpty() || code.textValue().length() > 64) {
            return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("problem", "code");
        }
        if (chr == null || !chr.canConvertToInt() || !chr.isIntegralNumber() || chr.intValue() <= 0) {
            return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("problem", "characterId");
        }
        if (!LocalDatabaseGuard.accepted()) {
            return LauncherResult.fail(LauncherCodes.LOCAL_ONLY);
        }
        GiftCodeService.Redemption r = GiftCodeService.standard().redeem(session.userId(), chr.intValue(), code.textValue(),
                ctx.sourceIp(), GiftCodeKey.standard(), ctx.services().memory());
        return switch (r.status()) {
            case REDEEMED -> LauncherResult.ok(LauncherCodes.REDEEMED).with("title", r.title())
                    .with("items", r.items().stream().map(GiftCodeService.Item::toMap).toList())
                    .with("character", Map.of("id", r.receiver().id(), "name", r.receiver().name(), "world", r.receiver().world()))
                    .with("delivery", "nextLogin");
            case ALREADY_REDEEMED -> LauncherResult.fail(LauncherCodes.ALREADY_REDEEMED);
            case NO_RECEIVER -> LauncherResult.fail(LauncherCodes.NO_RECEIVER);
            case RATE_LIMITED -> LauncherResult.fail(LauncherCodes.RATE_LIMITED).with("retryAfterSec", Math.max(1, (r.retryAfterMs() + 999) / 1000));
            case INVALID_OR_EXPIRED -> LauncherResult.fail(LauncherCodes.INVALID_OR_EXPIRED);
        };
    }
}
