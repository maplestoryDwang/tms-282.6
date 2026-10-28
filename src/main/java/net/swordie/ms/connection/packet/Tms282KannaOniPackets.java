package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.util.Position;

/** UserCommon actor records. See docs/kanna-oni-native-1007.md; no 265 or LP0664 encoding. */
public final class Tms282KannaOniPackets {
    public static final int OPCODE = 0x0425;
    public static final int CONTROLLER_SIGNATURE = 0xA5C63793;
    public static final int ONI_SIGNATURE = 0x4C2EEE9C;

    private Tms282KannaOniPackets() { }

    private static void id(long actor) {
        if (actor <= 0 || actor > 0xFFFFFFFFL) throw new IllegalArgumentException("Invalid Kanna actor ID");
    }

    private static OutPacket packet(int character) {
        if (character <= 0) throw new IllegalArgumentException("Invalid Kanna character ID");
        OutPacket out = Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeInt(character);
        return out;
    }

    private static void record(OutPacket out, long actor, int signature) {
        id(actor);
        out.encodeInt((int) actor);
        out.encodeInt(signature);
        out.encodeByte(3);
        out.encodeByte(0);
        out.encodeByte(0);
    }

    private static void owner(OutPacket out, int user, int character, int role) {
        if (user <= 0) throw new IllegalArgumentException("Invalid Kanna User.id");
        out.encodeShort(16);
        out.encodeInt(user);
        out.encodeInt(character);
        out.encodeShort(17);
        out.encodeByte(role);
    }

    private static void endRecord(OutPacket out) {
        out.encodeShort(0);
        out.encodeShort(0);
        out.encodeByte(0);
    }

    /** Create the owner controller, create Oni referencing it, then bind controller's enum2 slot to Oni. */
    public static OutPacket bootstrap(int character, int user, long controller, long oni, Position position) {
        return bootstrap(character, user, controller, oni, position, 3);
    }

    /** The observer gets this owner's remote UserCommon actors; role2 cannot send owner RPCs. */
    public static OutPacket observerBootstrap(int character, int user, long controller, long oni, Position position) {
        return bootstrap(character, user, controller, oni, position, 2);
    }

    private static OutPacket bootstrap(int character, int user, long controller, long oni, Position position, int role) {
        id(controller);
        id(oni);
        if (controller == oni || position == null) throw new IllegalArgumentException("Invalid Kanna actor pair");
        OutPacket out = packet(character);
        out.encodeByte(0);
        record(out, controller, CONTROLLER_SIGNATURE);
        owner(out, user, character, role);
        endRecord(out);
        record(out, oni, ONI_SIGNATURE);
        owner(out, user, character, role);
        out.encodeShort(8); // AActor.ReplicatedMovement.Location, original atomic Vector2D codec
        out.encodeInt(Float.floatToRawIntBits(position.getX()));
        out.encodeInt(Float.floatToRawIntBits(position.getY()));
        out.encodeShort(70);
        out.encodeByte(2);
        out.encodeShort(71);
        out.encodeByte(3); // FName literal UTF16 form: original6E0EEC0->63CD3B0
        out.encodeShort(3);
        out.encodeArr(new byte[]{'O', 0, 'n', 0, 'i', 0});
        out.encodeByte(0); // unsigned FName numeric suffix0
        out.encodeShort(72);
        out.encodeLong(controller);
        endRecord(out);
        record(out, controller, CONTROLLER_SIGNATURE);
        out.encodeShort(22); // Shikigamis[Oni=2], typed TObjectPtr
        out.encodeLong(oni);
        endRecord(out);
        out.encodeInt(0);
        return out;
    }

    /** Native Broadcast_ReserveAttackSkill128: observers enqueue the same command without sending it back. */
    public static OutPacket reserve(int character, long oni, byte[] command) {
        id(oni);
        if (command == null || command.length != 20) throw new IllegalArgumentException("Invalid Oni reserve command");
        OutPacket out = packet(character);
        out.encodeByte(9);
        out.encodeShort(30);
        out.encodeInt((int) oni);
        out.encodeInt(ONI_SIGNATURE);
        out.encodeShort(128);
        out.encodeArr(command);
        return out;
    }
}
