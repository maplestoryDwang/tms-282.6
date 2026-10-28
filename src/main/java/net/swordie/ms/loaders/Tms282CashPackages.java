package net.swordie.ms.loaders;

import net.swordie.ms.loaders.overlay.DataOverlays;
import net.swordie.ms.util.XMLApi;
import org.w3c.dom.Node;
import java.io.File;
import java.util.*;

/** Authoritative package -> component commodity SNs from the same client's CashPackage.img. */
public final class Tms282CashPackages {
    private Tms282CashPackages() { }
    private static final Map<String,Map<Integer,List<Integer>>> cache=new java.util.concurrent.ConcurrentHashMap<>();
    public static List<Integer> components(int itemId) {
        List<File> files=DataOverlays.current().wzLayers("Etc.wz/CashPackage.img.xml");
        for(int i=files.size()-1;i>=0;i--) {
            File file=files.get(i);if(!file.isFile())continue;
            var row=cache.computeIfAbsent(file.getAbsolutePath(),ignored->read(file)).get(itemId);
            if(row!=null)return row;
        }
        return List.of();
    }
    public static Map<Integer,List<Integer>> read(File file) {
        Map<Integer,List<Integer>> result=new TreeMap<>();
        Node root=XMLApi.getAllChildren(XMLApi.getRoot(file)).getFirst();
        for(Node node:XMLApi.getAllChildren(root)) {
            List<Integer> contents=new ArrayList<>();
            for(Node child:XMLApi.getAllChildren(node)) if("SN".equals(XMLApi.getNamedAttribute(child,"name")))
                for(Node value:XMLApi.getAllChildren(child)) contents.add(Integer.parseInt(XMLApi.getNamedAttribute(value,"value")));
            if(contents.isEmpty())throw new IllegalArgumentException("Empty cash package");
            result.put(Integer.parseInt(XMLApi.getNamedAttribute(node,"name")),List.copyOf(contents));
        }
        return Map.copyOf(result);
    }
}
