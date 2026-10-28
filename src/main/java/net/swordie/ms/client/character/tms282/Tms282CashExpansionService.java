package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.Tms282CashItemRequest;
import net.swordie.ms.loaders.Etc.Commodity.CommodityInfo;
import net.swordie.ms.world.shop.cashshop.CashShopItem;
import javax.sql.DataSource;
import java.sql.*;
import java.util.logging.Logger;

/** Existing capacity is a free panel benefit: persist it first, then conditionally debit an optional shop purchase.
 * A failed/unknown debit may leave free capacity, never a confirmed charge without the capacity benefit.
 */
public final class Tms282CashExpansionService {
    private static final Logger LOG=Logger.getLogger(Tms282CashExpansionService.class.getName());
    private static final java.util.Set<net.swordie.ms.client.User> UNCERTAIN=java.util.Collections.synchronizedSet(
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>())); // User retains Object identity equals/hashCode.
    /** Stale instances must not save currency after an unknown debit plus failed authoritative readback. */
    public static boolean isBalanceUncertain(net.swordie.ms.client.User user){return UNCERTAIN.contains(user);}
    public record Result(boolean paid,boolean storage,int inventoryType,int slots,int gained) { }
    private final DataSource source;
    public Tms282CashExpansionService(){source=null;}
    public Tms282CashExpansionService(DataSource source){this.source=java.util.Objects.requireNonNull(source);}
    public Result buy(Char chr,Tms282CashItemRequest.Expansion request,CommodityInfo commodity) {
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR){synchronized(chr){synchronized(chr.getUser()){
            Tms282VMatrixRuntime.requireEconomicAccess(chr);
            if(commodity==null || commodity.getSN()!=request.commoditySn() || commodity.getOnSale()==0 || commodity.getTerm()!=0)
                throw new IllegalArgumentException("Expansion commodity unavailable");
            int item=commodity.getItemId(),type=(item-9110000)/1000,suffix=(item-9110000)%1000;
            boolean storage=request.kind()==6;
            if(request.kind()!=5 && !storage || item<9110000 || item>9116008 || suffix!=0&&suffix!=4&&suffix!=8
                    || storage && type!=0 || !storage && (type<1||type>6))
                throw new IllegalArgumentException("Unsupported expansion commodity");
            int amount=suffix==4?4:8,cost=new CashShopItem(commodity).getNewPrice();
            return purchase(chr,storage,type,amount,cost,request.currency());
        }}}
    }

    /** Inventory-window "+" (CSNotice) item ids: 9110002 storage, 911t001/911t002 inventory type t; 8 slots each. */
    public static final java.util.Set<Integer> DIALOG_ITEMS=java.util.Set.of(9110002,9111001,9112001,9113002,9114001);

    /** CP00B0 from the inventory "+" dialog: the item id and its commodity SN; one purchase = 8 slots at the SN price. */
    public Result buyFromInventoryDialog(Char chr,int itemId,CommodityInfo commodity,int currency) {
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR){synchronized(chr){synchronized(chr.getUser()){
            Tms282VMatrixRuntime.requireEconomicAccess(chr);
            if(!DIALOG_ITEMS.contains(itemId))throw new IllegalArgumentException("Unsupported dialog expansion item");
            if(commodity==null || commodity.getItemId()!=itemId || commodity.getOnSale()==0 || commodity.getTerm()!=0)
                throw new IllegalArgumentException("Expansion commodity unavailable");
            int type=(itemId-9110000)/1000;
            return purchase(chr,type==0,type,8,new CashShopItem(commodity).getNewPrice(),currency);
        }}}
    }

    /** Capacity first (persisted), then the conditional debit; caller holds the economy, character and user locks. */
    private Result purchase(Char chr,boolean storage,int type,int amount,int cost,int currency) {
            if(cost<=0 || currency<1 || currency>2)throw new IllegalArgumentException("Invalid expansion price/currency");
            var user=chr.getUser();int balance=currency==1?user.getNxPrepaid():user.getMaplePoints();
            if(isBalanceUncertain(user))throw new IllegalStateException("Cash balance requires a fresh login");
            if(user.getId()<=0 || balance<cost)throw new IllegalArgumentException("Not enough cash points");
            int remaining=storage?Tms282InventoryExpansion.storageRemaining(chr):Tms282InventoryExpansion.remainingForCash(chr,type);
            if(remaining<amount)throw new IllegalArgumentException("Insufficient remaining capacity for the full purchase");
            if(!storage && type==6 && chr.getDecorationInventory().getSlots()+amount>255)
                throw new IllegalArgumentException("256-slot failed-payment synchronization is not yet supported");
            int gained;
            if(source==null)gained=storage?Tms282InventoryExpansion.growStorage(chr,amount):Tms282InventoryExpansion.growInventoryForCash(chr,amount,type);
            else {var expansion=new Tms282InventoryExpansion(source);gained=storage?expansion.growStorageWith(chr,amount):expansion.growInventoryForCashWith(chr,amount,type);}
            if(gained!=amount)throw new IllegalStateException("Expansion benefit not fully persisted");
            int slots=storage?chr.getAccount().getTrunk().getSlotCount():chr.getInventoryByType(net.swordie.ms.enums.InvType.getInvTypeByVal(type)).getSlots();
            boolean paid=false;
            String column=currency==1?"nxPrepaid":"maplePoints";
            UNCERTAIN.add(user);
            try(var c=source==null?DatabaseManager.getConnection():source.getConnection()){
                if(!c.getAutoCommit())throw new SQLException("Cash debit requires autocommit");
                try(var s=c.prepareStatement("UPDATE users SET "+column+"=? WHERE id=? AND "+column+"=?")){
                    s.setInt(1,balance-cost);s.setInt(2,user.getId());s.setInt(3,balance);
                    if(s.executeUpdate()!=1)throw new SQLException("Cash balance changed");
                    paid=true;
                    if(currency==1)user.setNxPrepaid(balance-cost);else user.setMaplePoints(balance-cost);
                    UNCERTAIN.remove(user);
                }
            }catch(SQLException e){
                LOG.warning("Cash expansion character="+chr.getId()+" capacity="+slots+" debitConfirmed="+paid+" SQLState="+e.getSQLState());
                if(!paid)try{refreshBalance(user,currency);}
                catch(SQLException refresh){LOG.warning("Cash balance readback failed; stale User save blocked id="+user.getId());}
            }
            return new Result(paid,storage,type,slots,gained);
    }
    /** Explicit recovery of the affected currency; failed reads never release an uncertain instance. */
    public void refreshBalance(net.swordie.ms.client.User user,int currency)throws SQLException {
        if(currency<1||currency>2||user.getId()<=0)throw new IllegalArgumentException("Invalid cash authority");
        synchronized(user){
            String column=currency==1?"nxPrepaid":"maplePoints";
            try(var c=source==null?DatabaseManager.getConnection():source.getConnection();
                var s=c.prepareStatement("SELECT "+column+" FROM users WHERE id=?")){
                s.setInt(1,user.getId());try(var rows=s.executeQuery()){
                    if(!rows.next())throw new SQLException("Missing cash authority");long value=rows.getLong(1);
                    if(value<0||value>Integer.MAX_VALUE||rows.wasNull()||rows.next())throw new SQLException("Invalid cash authority");
                    if(currency==1)user.setNxPrepaid((int)value);else user.setMaplePoints((int)value);
                    UNCERTAIN.remove(user);
                }
            }
        }
    }
}
