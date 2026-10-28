import net.swordie.ms.ServerConstants;
import net.swordie.ms.connection.packet.Tms282ClientBuild;
import net.swordie.ms.connection.packet.Tms282FileReport;
import net.swordie.ms.connection.packet.Tms282LoginSystemPackets;
import net.swordie.ms.connection.protocol.ProtocolConfig;
import net.swordie.ms.handlers.login.tms282.Tms282LoginSettings;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Verifies the portable login assets and the 282.6 handshake offline; the CP0078 reference file is still 282.5's. */
public class LoginAssetsCheck {
    public static void main(String[] args) throws Exception {
        System.setProperty("tms282.tests.rejectRealDatabase", "true");
        check(Tms282ClientBuild.build() == 28206 && Tms282ClientBuild.protocol().equals("282.6"), "282.6 build");
        check(ServerConstants.version == 282 && ServerConstants.patch.equals("6"), "282.6 server label");
        check(ProtocolConfig.load().version().id().equals("tms282.3"), "existing compatible protocol table");
        byte[] hello = Tms282LoginSystemPackets.loginHello(new byte[]{1, 2, 3, 4}, new byte[]{5, 6, 7, 8}).getData();
        ByteBuffer wire = ByteBuffer.wrap(hello).order(ByteOrder.LITTLE_ENDIAN);
        check(hello.length == 54 && wire.getShort(0) == 52 && wire.getShort(2) == 282, "login hello framing");
        check(new String(hello, 6, 3, StandardCharsets.US_ASCII).equals("6:1")
                && new String(hello, 36, 3, StandardCharsets.US_ASCII).equals("6:1"), "both native patch fields");
        check(wire.getInt(39) == 28206 && wire.getInt(43) == 28206, "both native hello builds");
        check(hello[17] == 6 && hello[53] == 5, "TMS login locale and socket type");
        var settings = Tms282LoginSettings.load();
        Path root = Path.of("").toAbsolutePath().normalize();
        check(settings.preauthDir().startsWith(root) && settings.fileReference().startsWith(root), "assets inside this folder");
        String[] files = {"send-000-0026.bin", "send-001-004e.bin", "send-002-004f.bin", "send-003-0038.bin",
                "send-004-0027.bin", "send-005-002a.bin"};
        int[] opcodes = {0x26, 0x4e, 0x4f, 0x38, 0x27, 0x2a};
        int[] sizes = {7, 1005, 2126, 12363, 4, 2};
        for (int i = 0; i < files.length; i++) {
            byte[] packet = Files.readAllBytes(settings.preauthDir().resolve(files[i]));
            check(packet.length == sizes[i] && Short.toUnsignedInt(ByteBuffer.wrap(packet)
                    .order(ByteOrder.LITTLE_ENDIAN).getShort()) == opcodes[i], "preauth " + files[i]);
        }
        byte[] reference = Files.readAllBytes(settings.fileReference());
        check(ByteBuffer.wrap(reference).order(ByteOrder.LITTLE_ENDIAN).getInt(6) == 0x68860C02, "282.5 file report magic");
        check(!Tms282FileReport.parse(reference).fileCrcs().isEmpty(), "trusted file manifest parses");
        try (var scripts = Files.list(Path.of("data/scripts"))) {
            check(scripts.findAny().isEmpty(), "empty gameplay scripts folder");
        }
        System.out.println("PASS: 282.6 native handshake and standalone login assets (offline)");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
