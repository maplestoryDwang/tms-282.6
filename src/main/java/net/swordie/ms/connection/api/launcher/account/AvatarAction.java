package net.swordie.ms.connection.api.launcher.account;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherContext;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.SessionReadAction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * {@code avatar} (1.2.8, session) {characterId}: the paper doll of one of the caller's own characters as a PNG
 * ({@code png}: base64, standing pose, no weapon), drawn by {@link AvatarRenderService} from the look as last saved
 * (avatarlook + hairequips; a cash piece covers the normal equip of its slot exactly as in the game, because the
 * renderer applies the client's own slot rules). Answers {@code ok} with {@code png}, {@code invalidInput}
 * (characterId), {@code notFound} (not the caller's character) or {@code unavailable} (no renderer on this host, or
 * the look could not be drawn). Read-only.
 */
public final class AvatarAction extends SessionReadAction {
    public static final String NOT_FOUND = "notFound";
    private final AvatarRenderService renderer;

    public AvatarAction() {
        this(AvatarRenderService.standard());
    }

    public AvatarAction(AvatarRenderService renderer) {
        this.renderer = renderer;
    }

    @Override
    public String name() {
        return "avatar";
    }

    @Override
    protected LauncherResult read(LauncherContext ctx, JsonNode request, Connection con, int userId) throws Exception {
        JsonNode id = request == null ? null : request.get("characterId");
        if (id == null || !id.isIntegralNumber() || !id.canConvertToInt() || id.intValue() <= 0) {
            return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("problem", "characterId");
        }
        // in the game right now: the look as the server holds it (the database only has it as of the last save)
        AvatarRenderService.Look look = ctx.services().memory().available() ? live(userId, id.intValue()) : null;
        if (look == null) {
            look = look(con, userId, id.intValue());
        }
        if (look == null) {
            return LauncherResult.fail(NOT_FOUND);
        }
        byte[] png = renderer.png(look);
        if (png == null) {
            return LauncherResult.fail(LauncherCodes.UNAVAILABLE);
        }
        return LauncherResult.ok(LauncherCodes.OK).with("characterId", id.intValue()).with("png", Base64.getEncoder().encodeToString(png));
    }

    /** The look of the user's character while it is online, else null (also when it is not theirs). */
    static AvatarRenderService.Look live(int userId, int characterId) {
        try {
            net.swordie.ms.client.character.Char chr = net.swordie.ms.Server.get().getChars().get(characterId);
            if (chr == null || chr.getUser() == null || chr.getUser().getId() != userId) {
                return null;
            }
            var al = chr.getAvatarData().getAvatarLook();
            return new AvatarRenderService.Look(al.getSkin(), al.getFace(), al.getHair(), al.isDrawElfEar(), new ArrayList<>(al.getHairEquips()));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The saved look of the user's character, or null when the character is not theirs. */
    static AvatarRenderService.Look look(Connection con, int userId, int characterId) throws Exception {
        int lookId, skin, face, hair;
        boolean elf;
        try (PreparedStatement ps = con.prepareStatement("SELECT al.id, al.skin, al.face, al.hair, al.drawelfear FROM accounts a"
                + " JOIN characters c ON c.accid = a.id JOIN avatardata ad ON ad.id = c.avatardata"
                + " JOIN avatarlook al ON al.id = ad.avatarlook WHERE a.userid = ? AND c.id = ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                lookId = rs.getInt(1);
                skin = rs.getInt(2);
                face = rs.getInt(3);
                hair = rs.getInt(4);
                elf = rs.getBoolean(5);
            }
        }
        List<Integer> items = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT equipid FROM hairequips WHERE alid = ? LIMIT 80")) {
            ps.setInt(1, lookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(rs.getInt(1));
                }
            }
        }
        return new AvatarRenderService.Look(skin, face, hair, elf, items);
    }
}
