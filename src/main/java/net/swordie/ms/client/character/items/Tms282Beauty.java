package net.swordie.ms.client.character.items;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.enums.AvatarModifiedMask;
import net.swordie.ms.enums.Stat;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.*;

/** Positive installed282 asset catalogue; independent of the debug-disabled 265 face/hair startup list. */
public final class Tms282Beauty {
    public static final int PAGE_SIZE = 20;
    private static volatile Map<Integer, NavigableMap<Integer, String>> catalogue;
    private Tms282Beauty() {}

    private static Map<Integer, NavigableMap<Integer, String>> catalogue() {
        if (catalogue != null) return catalogue;
        synchronized (Tms282Beauty.class) {
            if (catalogue != null) return catalogue;
            Map<Integer, NavigableMap<Integer, String>> loaded = new HashMap<>();
            for (int kind=0;kind<3;kind++) loaded.put(kind,new TreeMap<>());
            Path file = Path.of(System.getProperty("tms282.beautyCatalog", "data/resources/tms282/beauty-catalog.tsv"));
            try {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] row = line.split("\t",3);
                    int kind = switch(row[0]) { case "hair" -> 0; case "face" -> 1; case "skin" -> 2; default -> -1; };
                    int id = Integer.parseInt(row[1]);
                    if (kind<0 || row.length!=3 || row[2].isBlank() || !validRange(kind,id))
                        throw new IllegalArgumentException("Invalid beauty catalogue row");
                    loaded.get(kind).put(id,row[2].replace('#','＃'));
                }
                if (loaded.values().stream().anyMatch(Map::isEmpty)) throw new IOException("Empty beauty catalogue");
            } catch (IOException | RuntimeException e) {
                throw new IllegalStateException("造型目錄暫不可用，未變更外觀。",e);
            }
            catalogue = Map.copyOf(loaded);
            return catalogue;
        }
    }

    private static boolean validRange(int kind,int id) {
        return switch(kind) {
            case 0 -> id>=30000 && id<50000 || id>=60000 && id<70000;
            case 1 -> id>=20000 && id<30000 || id>=50000 && id<60000;
            case 2 -> id>=0 && id<=255 && id!=6 && id!=7 && id!=8 && (id<42 || id>49);
            default -> false;
        };
    }
    private static int category(int kind) {
        if (kind<0 || kind>4) throw new IllegalArgumentException("請選擇有效的美容服務。");
        return kind==3 ? 0 : kind==4 ? 1 : kind;
    }
    private static int family(int kind,int id) { return kind==0 ? id-id%10 : kind==1 ? id-id%1000+id%100 : id; }

    private static List<Integer> choices(int kind,int current,String query) {
        int category=category(kind); var available=catalogue().get(category);
        List<Integer> ids=new ArrayList<>();
        if (kind>=3) {
            for (int id:available.keySet()) if (family(category,id)==family(category,current)) ids.add(id);
        } else if (category==2) ids.addAll(available.keySet());
        else {
            Map<Integer,Integer> styles=new TreeMap<>();
            int colour=category==0 ? Math.floorMod(current,10) : Math.floorMod(current,1000)/100*100;
            for(int id:available.keySet()) styles.putIfAbsent(family(category,id),id);
            for(var entry:styles.entrySet()) {
                int preferred=entry.getKey()+colour;
                ids.add(available.containsKey(preferred)?preferred:entry.getValue());
            }
        }
        String text=query==null?"":query.strip().toLowerCase(Locale.ROOT);
        if (kind < 2 && text.matches("[0-9]{1,5}") && available.containsKey(Integer.parseInt(text)))
            return List.of(Integer.parseInt(text));
        if(!text.isEmpty()) ids.removeIf(id -> !available.get(id).toLowerCase(Locale.ROOT).contains(text)
                && !Integer.toString(id).contains(text) && !Integer.toString(family(category,id)).contains(text));
        return ids;
    }
    public static int count(int kind,int current,String query) { return choices(kind,current,query).size(); }
    public static int[] page(int kind,int current,String query,int page) {
        if(page<0) throw new IllegalArgumentException("頁碼不正確。");
        var choices=choices(kind,current,query);
        long first=(long)page*PAGE_SIZE;
        if(first>=choices.size())return new int[0];
        return choices.subList((int)first,Math.min(choices.size(),(int)first+PAGE_SIZE)).stream().mapToInt(Integer::intValue).toArray();
    }
    public static String name(int kind,int id) {
        String value=catalogue().get(category(kind)).get(id);
        if(value==null)throw new IllegalArgumentException("此造型不在可用目錄，未變更外觀。");
        return value;
    }
    public static int current(Char chr,int kind) {
        var look=chr.getAvatarData().getAvatarLook();
        return switch(category(kind)) { case 0 -> look.getHair(); case 1 -> look.getFace(); default -> look.getSkin(); };
    }
    /** Free for all characters; callers preview and confirm before this sole mutation point. Gender is unchanged. */
    public static void apply(Char chr,int kind,int id) {
        name(kind,id);
        int category=category(kind);
        if(kind>=3 && family(category,current(chr,kind))!=family(category,id))
            throw new IllegalArgumentException("只能變更目前造型的顏色。");
        if(current(chr,kind)==id)return;
        var look=chr.getAvatarData().getAvatarLook();
        if(category==0) { look.setHair(id); chr.setStatAndSendPacket(Stat.hair,id); }
        else if(category==1) { look.setFace(id); chr.setStatAndSendPacket(Stat.face,id); }
        else { look.setSkin(id); chr.setStatAndSendPacket(Stat.skin,id); }
        if(chr.getField()!=null)chr.getField().broadcast(UserRemote.avatarModified(chr,AvatarModifiedMask.AvatarLook.getVal(),(byte)0),chr);
        chr.dispose();
    }
}
