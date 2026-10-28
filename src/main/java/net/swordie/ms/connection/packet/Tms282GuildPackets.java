package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import java.util.List;
import java.nio.charset.Charset;

/** Native 282 GuildData (9B43E0), not the incompatible 265 Guild.encode layout.
 * Fields whose meaning is not established retain native offset names. This codec
 * deliberately does not invent a conversion from the legacy persistence model.
 */
public final class Tms282GuildPackets {
    public static final int OPCODE = 0x00DD;
    private Tms282GuildPackets() { }

    /** Native 5FBD5AE (result 98): guild ID, editing character ID, new notice. */
    public static OutPacket notice(int guildId, int editorId, String notice, Charset charset) {
        if (guildId <= 0 || editorId <= 0) throw new IllegalArgumentException("Invalid guild notice identity");
        OutPacket out = Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeInt(98);
        out.encodeInt(guildId);
        out.encodeInt(editorId);
        Tms282LoginPacketSupport.str(out, notice, charset);
        return out;
    }

    public record Grade(String name, int permissions) { }
    public record Applicant(int characterId, String name, int job, int level, boolean flag, String message) { }
    public record Member(int characterId, String name, int job, int level, int rank,
                         int value44, long time64, boolean online, long time3c,
                         int value48, int value4c, long time50, int value58, long time5c) { }
    public record Skill(int id, int level, long expire, String purchaser, String extender) { }
    public record Data(int id, String name, List<Grade> grades, List<Applicant> applicants,
                       List<Member> members, int capacity, int markBackground, int markBackgroundColor,
                       int mark, int markColor, String notice, int value68, int value6c, int level,
                       int value74, int value78, int value7c, boolean flag80, long time84, long time8c,
                       int value98, int value94, int value9c, List<Skill> skills,
                       int emblemType, byte[] emblem, int emblemVersion, int valueA0, int valueA4,
                       int valueA8, boolean flagAC) {
        public Data {
            grades = List.copyOf(grades);
            applicants = List.copyOf(applicants);
            members = List.copyOf(members);
            skills = List.copyOf(skills);
            emblem = emblem.clone();
            if (id <= 0 || grades.size() != 10 || applicants.size() > 65535 || members.isEmpty()
                    || members.size() > 65535 || capacity < members.size() || skills.size() > 65535
                    || level < 1 || level > 255 || emblem.length > 65536) {
                throw new IllegalArgumentException("Invalid native guild snapshot");
            }
            if (members.stream().filter(m -> m.rank() == 1).count() != 1
                    || members.stream().map(Member::characterId).distinct().count() != members.size()
                    || members.stream().anyMatch(m -> m.characterId() <= 0 || m.rank() < 1 || m.rank() > 10)) {
                throw new IllegalArgumentException("Invalid guild membership");
            }
        }
        @Override public byte[] emblem() { return emblem.clone(); }
    }

    /** Result 1 is a membership snapshot; native compares recipient ID before decoding GuildData. */
    public static OutPacket membership(int version, int recipientId, Data data) {
        return membership(version, recipientId, data, Tms282LoginPacketSupport.DEFAULT_CHARSET);
    }

    public static OutPacket membership(int version, int recipientId, Data data, Charset charset) {
        return membership(1, version, recipientId, data, charset);
    }

    public static OutPacket membership(int type, int version, int recipientId, Data data, Charset charset) {
        if (type != 1 && type != 7 && type != 13) throw new IllegalArgumentException("Unknown membership result");
        if (recipientId <= 0 || data.members().stream().noneMatch(m -> m.characterId() == recipientId)) {
            throw new IllegalArgumentException("Recipient is not a guild member");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeInt(type);
        out.encodeInt(version);
        out.encodeInt(recipientId);
        out.encodeInt(data.id());
        out.encodeByte(0); // ordinary GuildData; extended branch 5FBB1A0 is not used
        encodeData(out, data, charset);
        return out;
    }

    public static OutPacket noGuild(int recipientId) {
        if(recipientId<=0)throw new IllegalArgumentException("Invalid guild recipient");
        OutPacket out=Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeInt(1);out.encodeInt(0);out.encodeInt(recipientId);out.encodeInt(0);return out;
    }

    /** 5FBB5AE opens the native name dialog. */
    public static OutPacket createDialog() {
        OutPacket out=Tms282LoginPacketSupport.packet(OPCODE);out.encodeInt(0);return out;
    }
    /** 5FBB5C0: client presents name and emits CP01F6/type2 or type42. */
    public static OutPacket confirmCreate(String name, Charset charset) {
        OutPacket out=Tms282LoginPacketSupport.packet(OPCODE);out.encodeInt(4);
        Tms282LoginPacketSupport.str(out,name,charset);return out;
    }
    /** 5FBC013: foreign guild data followed by four ranking counters (2FEC240). */
    public static OutPacket information(Data data, Charset charset) {
        OutPacket out=Tms282LoginPacketSupport.packet(OPCODE);out.encodeInt(3);encodeData(out,data,charset);
        for(int i=0;i<4;i++)out.encodeInt(0);return out;
    }
    /** 5FBB713 -> 37073A0, popup type25. Acceptance opens info via CP01F6/type33. */
    public static OutPacket invitation(int guildId,String guildName,int inviterId,String inviterName,
                                       int inviterJob,int inviterLevel,int guildLevel,int inviterAccountId,Charset charset) {
        OutPacket out=Tms282LoginPacketSupport.packet(OPCODE);out.encodeInt(52);out.encodeInt(guildId);
        Tms282LoginPacketSupport.str(out,guildName,charset);out.encodeInt(inviterId);
        Tms282LoginPacketSupport.str(out,inviterName,charset);out.encodeInt(inviterJob);out.encodeInt(inviterLevel);
        out.encodeInt(guildLevel);out.encodeInt(inviterAccountId);return out;
    }
    /** 5FBC5E4: leave(33) / kick(34) share guild ID, character ID and name. */
    public static OutPacket departure(boolean kicked,int guildId,int characterId,String name,Charset charset) {
        OutPacket out=Tms282LoginPacketSupport.packet(OPCODE);out.encodeInt(kicked?34:33);
        out.encodeInt(guildId);out.encodeInt(characterId);Tms282LoginPacketSupport.str(out,name,charset);return out;
    }

    /** Native LP00DD130 ->5FC7880: old rank3/new rank1; this standalone-guild branch never transfers alliance roles. */
    public static OutPacket leadership(int guildId,int formerId,int successorId,boolean notifySuccessor) {
        if(guildId<=0||formerId<=0||successorId<=0||formerId==successorId)throw new IllegalArgumentException("Invalid guild leadership identity");
        OutPacket out=Tms282LoginPacketSupport.packet(OPCODE);out.encodeInt(130);out.encodeInt(guildId);
        out.encodeInt(formerId);out.encodeInt(successorId);out.encodeByte(notifySuccessor);out.encodeByte(false);return out;
    }

    public static void encodeData(OutPacket out, Data data) {
        encodeData(out, data, Tms282LoginPacketSupport.DEFAULT_CHARSET);
    }

    public static void encodeData(OutPacket out, Data data, Charset charset) {
        out.encodeInt(data.id());
        Tms282LoginPacketSupport.str(out, data.name(), charset);
        for (Grade grade : data.grades()) {
            Tms282LoginPacketSupport.str(out, grade.name(), charset);
            out.encodeInt(grade.permissions());
        }
        out.encodeShort(data.applicants().size());
        for (Applicant applicant : data.applicants()) {
            out.encodeInt(applicant.characterId());
            Tms282LoginPacketSupport.str(out, applicant.name(), charset);
            out.encodeInt(applicant.job());
            out.encodeInt(applicant.level());
            out.encodeByte(applicant.flag());
            Tms282LoginPacketSupport.str(out, applicant.message(), charset);
        }
        out.encodeShort(data.members().size());
        for (Member member : data.members()) {
            out.encodeInt(member.characterId());
            Tms282LoginPacketSupport.str(out, member.name(), charset);
            out.encodeInt(member.job());
            out.encodeInt(member.level());
            out.encodeInt(member.rank());
            out.encodeInt(member.value44());
            out.encodeLong(member.time64());
            out.encodeByte(member.online());
            out.encodeLong(member.time3c());
            out.encodeInt(member.value48());
            out.encodeInt(member.value4c());
            out.encodeLong(member.time50());
            out.encodeInt(member.value58());
            out.encodeLong(member.time5c());
        }
        out.encodeInt(data.capacity());
        out.encodeShort(data.markBackground());
        out.encodeByte(data.markBackgroundColor());
        out.encodeShort(data.mark());
        out.encodeByte(data.markColor());
        Tms282LoginPacketSupport.str(out, data.notice(), charset);
        out.encodeInt(data.value68());
        out.encodeInt(data.value6c());
        out.encodeByte(data.level());
        out.encodeInt(data.value74());
        out.encodeInt(data.value78());
        out.encodeInt(data.value7c());
        out.encodeByte(data.flag80());
        out.encodeLong(data.time84());
        out.encodeLong(data.time8c());
        out.encodeInt(data.value98());
        out.encodeInt(data.value94());
        out.encodeInt(data.value9c());
        out.encodeShort(data.skills().size());
        for (Skill skill : data.skills()) {
            out.encodeInt(skill.id());
            out.encodeShort(skill.level());
            out.encodeLong(skill.expire());
            Tms282LoginPacketSupport.str(out, skill.purchaser(), charset);
            Tms282LoginPacketSupport.str(out, skill.extender(), charset);
        }
        out.encodeByte(data.emblemType());
        out.encodeInt(data.emblem().length);
        out.encodeArr(data.emblem());
        out.encodeInt(data.emblemVersion());
        out.encodeInt(data.valueA0());
        out.encodeInt(data.valueA4());
        out.encodeInt(data.valueA8());
        out.encodeByte(data.flagAC());
        out.encodeByte(0); // optional castle descriptor absent, not an invented castle
    }
}
