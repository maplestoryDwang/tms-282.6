package net.swordie.ms.connection.packet;

/** Native 5934BB0: empty request + transport trailer; no skill id or client expiry clock. */
public record Tms282OverdriveEndRequest() {
    public static Tms282OverdriveEndRequest parse(byte[] packet) {
        var r = Tms282FieldRequestReader.open(packet, 0x0441, "CP0441 Overdrive end");
        r.i32(); r.end();
        return new Tms282OverdriveEndRequest();
    }
}
