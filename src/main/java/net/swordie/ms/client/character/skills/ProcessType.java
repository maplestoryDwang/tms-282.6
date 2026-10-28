package net.swordie.ms.client.character.skills;

import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.container.Tuple;

import java.util.HashSet;

public class ProcessType {

    public SkillUseInfo skillUseInfo;
    public int crc;
    /** Native 282 process identifier between the presence flag and payload tags; never gameplay authority by itself. */
    public int nativeProcessId;

    public ProcessType(SkillUseInfo skillUseInfo) {
        this.skillUseInfo = skillUseInfo;
    }

    public void decode(InPacket inPacket) {
        nativeProcessId = 0;
        this.crc = inPacket.decodeInt(); // unk
        if (inPacket.decodeByte() != 0) {
            int processId = inPacket.decodeInt();
            boolean understoodTypes = true;
            int processType = -1;
            while ((processType = inPacket.decodeInt()) > 0) {
                switch (processType) {
                    case 1:
                        boolean bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeByte();
                            byte len = inPacket.decodeByte();
                            for (byte i = 0; i < len; i++) {
                                inPacket.decodeInt();
                            }
                        }
                        break;
                    case 2:
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            inPacket.decodeByte();
                            inPacket.decodeByte();
                            skillUseInfo.skillId = inPacket.decodeInt(); // skillId
                            inPacket.decodeInt();
                            inPacket.decodeByte();
                            skillUseInfo.endingPosition = inPacket.decodePositionInt(); // spawn locations
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            // Native1204EE0 appends presence B and optional pointed-object I to282 tag2.
                            if (net.swordie.ms.connection.protocol.Protocol.active().version()
                                    == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                                    && inPacket.decodeByte() != 0) inPacket.decodeInt();
                        }
                        break;
                    case 3:
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            inPacket.decodeByte();
                            inPacket.decodeInt();
                            // Native2821207F20 tag3 writes B I I; the final I is absent from the265 union.
                            if (net.swordie.ms.connection.protocol.Protocol.active().version()
                                    == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) inPacket.decodeInt();
                        }
                        break;
                    case 4: // 219+
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            skillUseInfo.rect = inPacket.decodeIntRect(); // rect probably
                            skillUseInfo.endingPosition = inPacket.decodePositionInt(); // spawn locations
                            inPacket.decodeInt();
                        }
                        break;
                    case 5:
                    case 6:
                        inPacket.decodeByte();
                        break;
                    case 7:
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            skillUseInfo.endingPosition = inPacket.decodePositionInt(); // spawn locations
                            inPacket.decodeByte();
                            inPacket.decodeByte();
                            inPacket.decodeByte();
                            inPacket.decodeByte();
                            skillUseInfo.isLeft = inPacket.decodeByte() != 0;
                        }
                        break;
                    case 8:
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            skillUseInfo.rect = inPacket.decodeIntRect(); // rect probably
                        }
                        break;
                    case 9:
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            skillUseInfo.endingPosition = inPacket.decodePositionInt(); // ending Position
                            skillUseInfo.isLeft = inPacket.decodeInt() == -1; // isLeft   -1 = Left  |  1 = Right
                        }
                        break;
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                        inPacket.decodeByte();
                        break;
                    case 15: // 219
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            int size = inPacket.decodeInt();
                            for (int i = 0; i < size; i++) {
                                inPacket.decodeInt();
                                inPacket.decodeInt();
                                inPacket.decodeInt();
                                inPacket.decodeInt();
                            }
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                        }
                        break;
                    case 19:
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                        }
                        break;
                    case 20:
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            int size = inPacket.decodeInt();
                            for (int i = 0; i < size; i++) {
                                inPacket.decodeInt();
                            }
                        }
                        break;
                    case 22:
                    case 23:
                        inPacket.decodeByte();
                        break;
                    case 24:
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                        }
                        break;
                    case 25:
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            skillUseInfo.shardsPositions = new HashSet<>();
                            int shardsSize = inPacket.decodeInt();
                            for (int i = 0; i < shardsSize; i++) {
                                int mobObjectId = inPacket.decodeInt();
                                Position position = inPacket.decodePositionInt();
                                skillUseInfo.shardsPositions.add(new Tuple<>(mobObjectId, position));
                            }
                            skillUseInfo.isLeft = inPacket.decodeByte() != 0;
                        }
                        break;
                    case 29:
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            inPacket.decodeInt();
                        }
                        break;
                    case 34:
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            int size = inPacket.decodeInt();
                            for (int i = 0; i < size; i++) {
                                inPacket.decodeInt();
                                inPacket.decodeInt();
                            }
                        }
                        break;
                    case 37:
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            inPacket.decodeInt();

                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeByte();
                            inPacket.decodeByte();
                            inPacket.decodeByte();
                        }
                        break;
                    case 39:
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            skillUseInfo.count = inPacket.decodeInt();
                            inPacket.decodeInt();
                            int size = inPacket.decodeInt();
                            for (int i = 0; i < size; i++) {
                                inPacket.decodeLong();
                            }
                        }
                        break;
                    case 42:
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            int size = inPacket.decodeInt();
                            for (int i = 0; i < size; i++) {
                                inPacket.decodeInt();
                            }
                        }
                        break;
                    case 43:
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            inPacket.decodeInt();
                        }
                        break;
                    case 45:
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeByte();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                        }
                        break;
                    case 48:
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            int size = inPacket.decodeInt();
                            for (int i = 0; i < size; i++) {
                                inPacket.decodeLong();
                            }
                        }
                        break;
                    case 49:
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            int size = inPacket.decodeInt();
                            for (int i = 0; i < size; i++) {
                                inPacket.decodeLong();
                            }
                            size = inPacket.decodeInt();
                            for (int i = 0; i < size; i++) {
                                inPacket.decodeInt();
                            }
                        }
                        break;
                    case 51:
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            inPacket.decodeInt();

                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                            inPacket.decodeByte();
                            inPacket.decodeByte();
                        }
                        break;
                    case 52:
                        bool = inPacket.decodeByte() != 0;
                        if (bool) {
                            inPacket.decodeInt();
                            inPacket.decodeInt();
                        }
                        break;
                    default:
                        understoodTypes = false;
                        break;
                }
            }
            int unk340 = 0;
            int unk338 = 0;
            int v8 = 0, result = 0;
            boolean understoodTail = true;
            if (net.swordie.ms.connection.protocol.Protocol.active().version()
                    == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                // The captured native process ends with an empty byte blob. Nonempty blobs remain unsupported.
                understoodTail = inPacket.decodeByte() == 0;
            } else sub_1408CA760(inPacket, unk340 - unk338);
            if (unk338 != unk340) {
                do {
                    inPacket.decodeByte();
                    v8++;
                    result = unk340 - unk338;
                } while (v8 < result);
            }
            // Anchor recovery may keep this object after a truncated prefix. Publish only a fully decoded process.
            if (understoodTypes && understoodTail && net.swordie.ms.connection.protocol.Protocol.active().version()
                    == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) nativeProcessId = processId;
        }
    }

    public static void sub_1408CA760(InPacket inPacket, int a2) {
        int v3 = (2 * a2) ^ (a2 >> 31);
        if (v3 >= 128) {
            do {
                inPacket.decodeByte(); // v3 | 0x80
                v3 >>= 7;
            }
            while (v3 >= 0x80);
        }
        inPacket.decodeByte(); // v3 & 0x7F
    }
}
