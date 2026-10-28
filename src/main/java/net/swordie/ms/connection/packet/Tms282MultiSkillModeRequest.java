package net.swordie.ms.connection.packet;

import java.nio.charset.Charset;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.connection.OutPacket;

/** CP03BE/5784EC0: chosen multi-skill ID, no transport trailer. Native5883090 reads QuestRecord21770 by parent ID. */
public record Tms282MultiSkillModeRequest(int skillId) {
    public static final int OPCODE = 0x03BE;
    public static final int QUEST_ID = 21770;

    public static Tms282MultiSkillModeRequest parse(byte[] packet) {
        var r = Tms282FieldRequestReader.open(packet, OPCODE, "CP03BE");
        int skill = r.i32(); r.end();
        if (skill <= 0) throw new IllegalArgumentException("Invalid multi-skill ID");
        return new Tms282MultiSkillModeRequest(skill);
    }

    /** LP00AE13/5EF3520 writes CharacterData+D5B;5883090 uses8BC6D0 to retrieve this parent->selected value. */
    public static OutPacket reply(Quest quest, Charset charset) {
        if (quest.getQRKey() != QUEST_ID) throw new IllegalArgumentException("Wrong multi-skill quest");
        var out=Tms282LoginPacketSupport.packet(0x00AE);
        out.encodeByte(13); out.encodeInt(QUEST_ID);
        Tms282LoginPacketSupport.str(out,quest.getQRValue(),charset);
        return out;
    }
}
