package net.swordie.ms.client.social.Alliance;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Guild.Guild;
import net.swordie.ms.connection.Encodable;
import net.swordie.ms.connection.OutPacket;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Alliance implements Encodable {

    private int id;
    private String name;
    private Set<Guild> guilds = new HashSet<>();
    private List<String> gradeNames;
    private int maxMemberNum;
    private String notice;

    public Alliance() {
        gradeNames = Arrays.asList("Master", "Junior", "Veteran", "Regular", "New");
    }

    @Override
    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getId());
        outPacket.encodeString(getName());
        getGradeNames().forEach(gradeName -> outPacket.encodeString(gradeName));
        outPacket.encodeByte(getGuilds().size());
        getGuilds().forEach(guild -> outPacket.encodeInt(guild.getId()));
        outPacket.encodeInt(getMaxMemberNum());
        outPacket.encodeString(getNotice());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Guild> getGuilds() {
        return guilds;
    }

    public void setGuilds(Set<Guild> guilds) {
        this.guilds = guilds;
    }

    public List<String> getGradeNames() {
        return gradeNames;
    }

    public void setGradeNames(List<String> gradeNames) {
        this.gradeNames = gradeNames;
    }

    public int getMaxMemberNum() {
        return maxMemberNum;
    }

    public void setMaxMemberNum(int maxMemberNum) {
        this.maxMemberNum = maxMemberNum;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public Guild getGuildByID(int guildID) {
        return getGuilds().stream().filter(g -> g.getId() == guildID).findAny().orElse(null);
    }

    public void broadcast(OutPacket outPacket) {
        getGuilds().forEach(guild -> guild.broadcast(outPacket));
    }

    public void broadcast(OutPacket outPacket, Char exceptChar) {
        getGuilds().forEach(guild -> guild.broadcast(outPacket, exceptChar));
    }

    public void addGuild(Guild guild) {
        getGuilds().add(guild);
    }

    public void removeGuild(Guild guild) {
        Guild g = getGuildByID(guild.getId()); // to ensure it's the same instance as the one in the set
        g.setAllianceID(0);
        getGuilds().remove(g);
    }
}
