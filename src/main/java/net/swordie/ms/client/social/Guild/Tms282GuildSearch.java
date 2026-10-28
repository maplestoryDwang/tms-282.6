package net.swordie.ms.client.social.Guild;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282GuildApplicationPackets.Summary;
import net.swordie.ms.connection.packet.Tms282GuildSearchPackets;
import net.swordie.ms.connection.packet.Tms282GuildSearchPackets.*;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Read-only native search. Native client copies pagination tokens without interpreting their fields. */
public final class Tms282GuildSearch {
    private static final int PAGE_SIZE=10; // original GuildUI2 popup/search/table/listCount
    private static final Map<Char,Map<Integer,State>> SEARCHES=new WeakHashMap<>();
    private record Key(int worldId,int mode,int destination,String text,boolean exact) { }
    private static final class State {
        final Key key;final List<Summary> invited,matches;final Map<Cursor,Integer> issued=new HashMap<>();
        int delivered;Cursor next;
        State(Key key,List<Summary> invited,List<Summary> matches){this.key=key;this.invited=List.copyOf(invited);this.matches=List.copyOf(matches);}
    }
    private Tms282GuildSearch() { }
    public static OutPacket search(Char chr,Criteria request,Charset charset) throws SQLException {
        if(!Tms282GuildRuntime.enabled())throw new IllegalArgumentException("Native guild search is not enabled");
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) {
            var key=new Key(chr.getWorld().getWorldId(),request.mode(),request.destination(),request.text(),request.exact());
            var sessions=SEARCHES.computeIfAbsent(chr,k->new HashMap<>());int slot=key.destination()*8+key.mode();
            State state=sessions.get(slot);boolean replace=request.cursor()==null;
            if(replace){state=load(chr,key);sessions.put(slot,state);}
            else if(state==null||!state.key.equals(key)||!state.issued.containsKey(request.cursor()))throw new IllegalArgumentException("Guild search cursor expired; start a new search");
            int start=replace?0:state.issued.get(request.cursor());
            boolean replay=!replace&&start<state.delivered;
            // TCP preserves replies in order. A consumed cursor returns an empty append with the latest
            // continuation: no duplicate rows, stale replacement or ever-growing replay packet.
            if(replay)start=state.delivered;
            int end=replay?state.delivered:Math.min(state.matches.size(),start+PAGE_SIZE);
            if(!replay){
                state.delivered=end;
                state.next=end<state.matches.size()?new Cursor(0,end,ThreadLocalRandom.current().nextLong(),false,false,0):null;
                if(state.next!=null)state.issued.put(state.next,end);
            }
            Criteria response=new Criteria(key.mode(),key.destination(),key.text(),key.exact(),request.cursor(),replace,state.next);
            return Tms282GuildSearchPackets.result(response,replace?state.invited:List.of(),state.matches.subList(start,end),charset);
        }
    }
    private static State load(Char chr,Key key) throws SQLException {
        var applied=new HashSet<Integer>();for(var app:Tms282GuildApplications.active(chr.getId()))applied.add(app.guildId());
        var invited=new ArrayList<Summary>();var matches=new ArrayList<Summary>();long now=System.currentTimeMillis();
        // ponytail: scan this local world's already-published guild catalogue; indexed DB search only if scale requires it.
        var guilds=chr.getWorld().getGuilds().values().stream().filter(g->g.getWorldID()==chr.getWorld().getWorldId())
                .sorted(Comparator.comparingInt(Guild::getId)).toList();
        String text=key.text().toLowerCase(Locale.ROOT);
        for(Guild guild:guilds){
            GuildMember leader=guild.getGuildLeader();if(leader==null||leader.getRank()!=1||guild.getMembers().isEmpty())continue;
            var summary=Tms282GuildApplications.summary(guild,applied.contains(guild.getId()));
            if(key.mode()==4){
                if(guild.getId()==chr.getGuildID())continue;
                boolean invitation=chr.getGuildID()==0&&guild.hasInvitation(chr.getId(),now)&&guild.hasPermission(guild.inviterId(chr.getId()),1);
                if(invitation&&invited.size()<5)invited.add(summary);
                else if(guild.isAppliable()&&guild.getMembers().size()<guild.getMaxMembers()&&chr.getLevel()>=Math.max(101,guild.getReqLevel()))matches.add(summary);
            }else if((key.mode()==1||key.mode()==3)&&matches(guild.getName(),text,key.exact())
                    ||(key.mode()==2||key.mode()==3)&&matches(leader.getName(),text,key.exact()))matches.add(summary);
        }
        return new State(key,invited,matches);
    }
    private static boolean matches(String value,String text,boolean exact){String candidate=value.toLowerCase(Locale.ROOT);return exact?candidate.equals(text):candidate.contains(text);}
}
