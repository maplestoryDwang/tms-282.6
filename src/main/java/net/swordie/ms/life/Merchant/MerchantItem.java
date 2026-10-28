package net.swordie.ms.life.Merchant;

import net.swordie.ms.client.character.items.Item;

public class MerchantItem {
    public Item item;
    public short bundles;
    public int price;
    private int id;

    public MerchantItem(Item item, short bundles, int price) {
        this.item = item;
        this.bundles = bundles;
        this.price = price;
    }

    public MerchantItem() {
    }
}
