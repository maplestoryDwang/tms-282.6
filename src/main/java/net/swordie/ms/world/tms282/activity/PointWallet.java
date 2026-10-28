package net.swordie.ms.world.tms282.activity;

import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * One currency a {@link PointOrder} can charge or credit (樂豆, 點券, 星語幣 …). Implementations live in
 * {@link PointWallets}; scripts name them by {@link #id()}.
 * Every method that takes a connection runs inside the order's InnoDB transaction (auto-commit off), after the
 * order has locked the {@code users} row, so wallets of the same user are always taken in the same order.
 */
public interface PointWallet {
    /** Script name, e.g. {@code maple}. */
    String id();

    /** Balance; {@code forUpdate} locks the rows that hold it until the transaction ends. Missing rows read as 0. */
    long balance(Connection c, Char chr, boolean forUpdate) throws SQLException;

    /** Moves the balance from {@code before} by {@code delta} (negative = charge). The result is never below 0. */
    void apply(Connection c, Char chr, long before, int delta, String receipt) throws SQLException;

    /**
     * Gives back {@code amount} (> 0) that a {@link PaidService#charge} took ({@link PaidService#refund}): by default the
     * same as a credit of {@code amount}; a wallet that keeps a "spent" total (hypay payUsed) takes it back out too.
     * Runs once per receipt (PaidService marks the receipt refunded in the same transaction).
     */
    default void refund(Connection c, Char chr, long before, int amount, String receipt) throws SQLException {
        apply(c, chr, before, amount, receipt);
    }

    /** Tables this wallet reads or writes; each must be InnoDB for the transaction to mean anything. */
    List<String> tables();

    /** After the commit: the in-memory copies of the user (only wallets cached on {@link User} need this). */
    default void sync(List<User> copies, long after) {}
}
