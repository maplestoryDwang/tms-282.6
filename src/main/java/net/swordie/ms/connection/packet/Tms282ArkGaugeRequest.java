package net.swordie.ms.connection.packet;

/** CP02A5/5FF1DE0: current client gauge snapshot, followed by346A070. It is not a skill ID or trusted gauge write. */
public record Tms282ArkGaugeRequest(int clientGauge) {
    public static final int OPCODE=0x02A5;
    public static Tms282ArkGaugeRequest parse(byte[] packet) {
        var r=Tms282FieldRequestReader.open(packet,OPCODE,"CP02A5");
        int gauge=r.i32(); r.i32(); r.end();
        if(gauge<0||gauge>1000)throw new IllegalArgumentException("Invalid Ark gauge snapshot");
        return new Tms282ArkGaugeRequest(gauge);
    }
}
