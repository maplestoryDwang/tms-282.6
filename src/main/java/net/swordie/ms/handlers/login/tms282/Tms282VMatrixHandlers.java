package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime;
import net.swordie.ms.connection.packet.Tms282VMatrixRequest;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.handlers.header.InHeader;

/** Native282 V point/core operations; never reinterpret them as the265 node/slot protocol. */
final class Tms282VMatrixHandlers {
    void register(Tms282Routes routes) {
        routes.channel(Tms282VMatrixRequest.OPCODE,InHeader.CP02A3).when(Tms282Guards.IN_FIELD)
                .handle("native VMatrix operation",this::operate);
    }
    private void operate(Tms282LoginState session,int opcode,byte[] packet) {
        try { Tms282VMatrixRuntime.handle(session.fieldCharacter(),Tms282VMatrixRequest.parse(packet)); }
        catch(IllegalArgumentException e) {
            session.fieldCharacter().chatMessage("V矩陣請求不正確，未變更資料。");
            session.send(Tms282StatPackets.unlock());
        }
    }
}
