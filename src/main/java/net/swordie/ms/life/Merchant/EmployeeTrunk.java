package net.swordie.ms.life.Merchant;

import net.swordie.ms.constants.GameConstants;

import java.util.ArrayList;
import java.util.List;

public class EmployeeTrunk {

    private int id;
    private long money;
    private List<MerchantItem> items = new ArrayList<>();

    public EmployeeTrunk() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void removeItem(int pos) {
        getItems().remove(pos);
    }

    public List<MerchantItem> getItems() {
        return items;
    }

    public void setItems(ArrayList<MerchantItem> items) {
        this.items = items;
    }

    public long getMoney() {
        return money;
    }

    public void setMoney(long money) {
        if (money < 0 || money > GameConstants.MAX_MONEY) {
            return;
        }
        this.money = money;
    }

    public boolean canAddMoney(long amount) {
        return getMoney() + amount <= GameConstants.MAX_MONEY;
    }

    public void addMoney(long reqMoney) {
        if (canAddMoney(reqMoney)) {
            setMoney(getMoney() + reqMoney);
        }
    }


}
