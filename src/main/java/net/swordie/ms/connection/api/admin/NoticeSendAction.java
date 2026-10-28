package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.packet.Tms282Notice;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code notice.send} {text, kind?} (全服公告, notice-1005): one world announcement through {@link Tms282Notice}.
 * {@code kind} is a comma list (chat, tip, notice, slide, weather); without it {@code tms282.notice.kinds} (default
 * chat = LP04F0 type 11, the system-chat line proven to display on 282). Refusals: {@code invalidInput} with
 * {@code problem} = {@code text} (empty), {@code tooLong} (more than {@code tms282.notice.maxBytes} MS950 bytes, with
 * {@code bytes} and {@code maxBytes}), {@code control} (control / format characters), {@code charset} (not
 * encodable in MS950) or {@code kind} (unknown kind, with {@code unknown}). Success returns the kinds, the MS950 byte
 * length and the characters online. Mutating: audited with the kinds and the length (not the text).
 */
public final class NoticeSendAction implements AdminAction {
    @Override
    public String name() {
        return "notice.send";
    }

    @Override
    public boolean mutating() {
        return true;
    }

    @Override
    public String auditSummary(JsonNode request) {
        String text = AdminParams.text(request, "text");
        String kind = AdminParams.text(request, "kind");
        return "kind=" + (kind.isBlank() ? "default" : kind.replaceAll("[^a-z,]", "")) + " chars=" + text.strip().length();
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) {
        return send(ctx.env().server(), request);
    }

    /** The action without an {@link AdminContext} (checks drive it with their own server view). */
    public static LauncherResult send(AdminServerView server, JsonNode request) {
        List<Tms282Notice.Kind> kinds;
        String kindText = AdminParams.text(request, "kind");
        if (kindText.isBlank()) {
            kinds = Tms282Notice.defaultKinds();
        } else {
            List<String> unknown = new ArrayList<>();
            kinds = Tms282Notice.parseKinds(kindText, unknown);
            if (!unknown.isEmpty()) {
                return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "kind").with("unknown", String.join(",", unknown));
            }
        }
        int max = Tms282Notice.maxBytes();
        Tms282Notice.Checked c = Tms282Notice.validate(AdminParams.text(request, "text"), max);
        if (!c.ok()) {
            LauncherResult r = LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "empty".equals(c.problem()) ? "text" : c.problem());
            return "tooLong".equals(c.problem()) ? r.with("bytes", c.bytes()).with("maxBytes", max) : r;
        }
        int online = server.sendNotice(kinds, c.text());
        return LauncherResult.ok(AdminCodes.OK)
                .with("kinds", String.join(",", kinds.stream().map(Tms282Notice.Kind::id).toList()))
                .with("proven", kinds.stream().allMatch(Tms282Notice.Kind::proven))
                .with("bytes", c.bytes())
                .with("online", online);
    }
}
