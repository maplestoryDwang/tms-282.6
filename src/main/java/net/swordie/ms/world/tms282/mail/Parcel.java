package net.swordie.ms.world.tms282.mail;

import java.time.LocalDateTime;

/**
 * One Duey parcel (tms282_parcel row).
 *
 * @param sender           display name of the sender (for a returned parcel: the original receiver)
 * @param senderId         character id the parcel came from (who gets it back on expiry)
 * @param originalSenderId character who first sent it (kept through a return)
 * @param itemRow          items.id held by the parcel, 0 for a meso-only parcel (column sn)
 * @param deliveryType     {@link #NORMAL}, {@link #QUICK} or {@link #RETURNED}
 */
public record Parcel(int id, LocalDateTime createdAt, LocalDateTime availableAt, int receiverId, String receiverName,
                     String sender, int senderId, int originalSenderId, long money, LocalDateTime expire,
                     int deliveryType, String memo, long itemRow, int itemId, int quantity) {
    public static final int NORMAL = 0;
    public static final int QUICK = 1;
    public static final int RETURNED = 2;

    public boolean hasItem() {
        return itemRow > 0;
    }

    public boolean available(LocalDateTime now) {
        return !availableAt.isAfter(now);
    }
}
