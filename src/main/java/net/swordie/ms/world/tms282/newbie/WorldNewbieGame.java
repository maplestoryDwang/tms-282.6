package net.swordie.ms.world.tms282.newbie;

import net.swordie.ms.Server;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.tms282.EconomyLocks;
import net.swordie.ms.client.character.tms282.Tms282CharacterStore;
import net.swordie.ms.client.character.tms282.Tms282JobAdvance;
import net.swordie.ms.client.character.tms282.Tms282LevelService;
import net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.lifecycle.OnlineCharacterSaver;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.world.tms282.items.Tms282ItemAvailability;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** The server's {@link NewbieGame}: the existing grant, level and job-advance paths under the step-6 lock order. */
public final class WorldNewbieGame implements NewbieGame {

    @Override
    public boolean online(Char chr) {
        if (chr == null || chr.getId() <= 0) return false;
        Client client = chr.getClient();
        return client != null && client.getChr() == chr && Server.get().getChars().get(chr.getId()) == chr
                && client.getCh() != null && client.getCh().isActive()
                && client.getCurrentState() == Client.IN_FIELD && client.getNextState() == Client.NONE;
    }

    @Override
    public Gift grantGift(Char chr, long meso, List<Grant> items, GiftClaim claim) {
        Gift[] out = {Gift.OFFLINE};
        SQLException[] failure = {null};
        EconomyLocks.packet(chr, () -> {
            if (!online(chr)) {
                out[0] = Gift.OFFLINE;
                return null;
            }
            try {
                Tms282VMatrixRuntime.requireEconomicAccess(chr);
            } catch (IllegalStateException e) {
                System.out.println("[Newbie] gift for character " + chr.getId() + " waits: " + e.getMessage());
                out[0] = Gift.BLOCKED;
                return null;
            }
            List<Item> probe = new ArrayList<>();
            for (Grant g : items) {
                if (g.quantity() <= 0) continue;
                if (net.swordie.ms.world.tms282.items.ItemDataEffects.isLegacyEffectItem(g.itemId())) continue; // given as its effect
                if (!Tms282ItemAvailability.isGrantable(g.itemId())) {
                    System.out.println("[Newbie] gift item " + g.itemId() + " is not grantable (" + Tms282ItemAvailability.check(g.itemId())
                            + "); nothing given to character " + chr.getId());
                    out[0] = Gift.BLOCKED;
                    return null;
                }
                if (ItemConstants.isEquip(g.itemId())) {
                    for (int i = 0; i < g.quantity(); i++) {
                        Item e = ItemData.getEquipDeepCopyFromID(g.itemId(), false);
                        if (e == null) {
                            out[0] = Gift.BLOCKED;
                            return null;
                        }
                        if (g.periodDays() > 0) e.setDateExpire(net.swordie.ms.util.FileTime.fromDate(java.time.LocalDateTime.now().plusDays(g.periodDays())));
                        probe.add(e);
                    }
                } else {
                    Item it = ItemData.getItemDeepCopy(g.itemId());
                    if (it == null) {
                        out[0] = Gift.BLOCKED;
                        return null;
                    }
                    it.setQuantity(g.quantity());
                    if (g.periodDays() > 0) it.setDateExpire(net.swordie.ms.util.FileTime.fromDate(java.time.LocalDateTime.now().plusDays(g.periodDays())));
                    probe.add(it);
                }
            }
            if (!chr.canHold(probe)) {
                out[0] = Gift.FULL;
                return null;
            }
            if (meso > 0 && chr.getMoney() + meso > GameConstants.MAX_MONEY) {
                out[0] = Gift.FULL;
                return null;
            }
            NewbieStore.Claim c;
            try {
                c = claim.claim();
            } catch (SQLException e) {
                failure[0] = e;
                out[0] = Gift.BLOCKED;
                return null;
            }
            switch (c) {
                case ALREADY -> out[0] = Gift.ALREADY;
                case LIMIT -> out[0] = Gift.LIMIT;
                case NO_ROW -> out[0] = Gift.NO_ROW;
                case CLAIMED -> {
                    if (meso > 0) {
                        chr.addMoney(meso);
                        chr.write(WvsContext.incMoneyMessage((int) meso));
                    }
                    int probeIndex = 0;
                    for (Grant g : items) {
                        if (g.quantity() <= 0) continue;
                        if (net.swordie.ms.world.tms282.items.ItemDataEffects.isLegacyEffectItem(g.itemId())) {
                            // e.g. 2430480: no item node in the 282 client, so the 菇菇寶貝 riding skill is given directly
                            net.swordie.ms.world.tms282.items.ItemDataEffects.grantLegacyEffect(chr, g.itemId());
                            continue;
                        }
                        if (ItemConstants.isEquip(g.itemId())) {
                            for (int i = 0; i < g.quantity(); i++) {
                                if (g.periodDays() > 0) chr.addItemToInventory(g.itemId(), 1, "day", g.periodDays());
                                else chr.addItemToInventory(g.itemId(), 1);
                                probeIndex++;
                            }
                        } else {
                            // Use the exact expiry whose stack capacity was checked, including duplicate lines.
                            chr.addItemToInventory(probe.get(probeIndex++));
                            if (g.periodDays() == 0) chr.write(WvsContext.dropPickupMessage(g.itemId(), (short) g.quantity()));
                        }
                    }
                    // jobs that skip the first-job step (created already advanced) get their weapon here
                    net.swordie.ms.client.jobs.StarterWeapons.grantFor(chr);
                    OnlineCharacterSaver.Result saved = OnlineCharacterSaver.save(chr);
                    if (saved != OnlineCharacterSaver.Result.SAVED) {
                        System.out.println("[Newbie] claimed gift needs save verification: character=" + chr.getId() + " result=" + saved);
                        out[0] = Gift.BLOCKED;
                        return null;
                    }
                    out[0] = Gift.GRANTED;
                }
            }
            return null;
        });
        if (failure[0] != null) {
            System.out.println("[Newbie] gift claim of character " + chr.getId() + " failed in the database: " + failure[0]);
        }
        return out[0];
    }

    @Override
    public Job advanceJob(Char chr, int fromJob, int toJob, List<Integer> quests, int levelTo, boolean autoAssignAp, JobClaim claim) {
        Job[] out = {Job.OFFLINE};
        EconomyLocks.packet(chr, () -> {
            if (!online(chr)) {
                out[0] = Job.OFFLINE;
                return null;
            }
            try {
                Tms282VMatrixRuntime.requireEconomicAccess(chr);
            } catch (IllegalStateException e) {
                out[0] = Job.FAILED;
                return null;
            }
            if (chr.getJob() != fromJob) {
                // A previous attempt may have changed memory but failed its character save.
                out[0] = Job.FAILED;
                if (chr.getJob() == toJob && OnlineCharacterSaver.save(chr) == OnlineCharacterSaver.Result.SAVED) {
                    try {
                        claim.claim();
                        out[0] = Job.ALREADY;
                    } catch (SQLException e) {
                        System.out.println("[Newbie] first job retry receipt failed: character=" + chr.getId() + " " + e);
                    }
                }
                return null;
            }
            try {
                int from = chr.getLevel();
                if (levelTo > from) {
                    Tms282LevelService.Result r = new Tms282LevelService(Tms282CharacterStore.SQL).gainLevels(chr, levelTo - from);
                    for (OutPacket p : r.packets()) {
                        chr.write(p);
                    }
                    Tms282JobAdvance.afterLevelUp(chr, r.fromLevel(), r.toLevel());
                }
                ScriptManagerImpl sm = chr.getScriptManager();
                if (chr.getJob() == fromJob) {
                    for (int q : quests) {
                        sm.completeQuestNoRewards(q);
                    }
                    sm.jobAdvance((short) toJob);
                    if (autoAssignAp) {
                        sm.resetAP(false, (short) toJob);
                    }
                    if (chr.getJob() == toJob) {
                        net.swordie.ms.client.jobs.StarterWeapons.grantFor(chr); // saved with the advancement below
                    }
                }
                if (chr.getJob() != toJob || OnlineCharacterSaver.save(chr) != OnlineCharacterSaver.Result.SAVED) {
                    out[0] = Job.FAILED;
                    return null;
                }
                claim.claim(); // record completion only after the actual advancement was saved
                System.out.println("[Newbie] FIRST JOB character=" + chr.getId() + " " + fromJob + " -> " + chr.getJob()
                        + " level " + from + " -> " + chr.getLevel());
                out[0] = Job.ADVANCED;
            } catch (SQLException | RuntimeException e) {
                System.out.println("[Newbie] first job of character " + chr.getId() + " failed after the claim (job " + chr.getJob()
                        + "): " + e + " - retry on the next dialog");
                out[0] = Job.FAILED;
            }
            return null;
        });
        return out[0];
    }

    @Override
    public Job completeZeroPrologue(Char chr, JobClaim claim) {
        Job[] result = {Job.OFFLINE};
        EconomyLocks.packet(chr, () -> {
            if (!online(chr)) return null;
            result[0] = Job.FAILED;
            try {
                Tms282VMatrixRuntime.requireEconomicAccess(chr);
                int before = chr.getJob();
                if (!(chr.getJobHandler() instanceof net.swordie.ms.client.jobs.Zero zero) || !zero.completePrologue()) return null;
                if (OnlineCharacterSaver.save(chr) != OnlineCharacterSaver.Result.SAVED) return null;
                claim.claim();
                // The receipt inserts directly into rewardinfo. Reload the existing reward-center cache,
                // including an ALREADY retry after a committed receipt whose response/close failed.
                chr.setRewardSystem(null);
                chr.initRewardSystem();
                chr.sendPacketRewards();
                result[0] = before == 10000 ? Job.ADVANCED : Job.ALREADY;
            } catch (SQLException | RuntimeException e) {
                System.out.println("[Newbie] Zero prologue retry required: character=" + chr.getId() + " " + e);
            }
            return null;
        });
        return result[0];
    }

    @Override
    public void saveSoon(Char chr) {
        Thread.ofVirtual().name("newbie-save-" + chr.getId()).start(() -> OnlineCharacterSaver.save(chr));
    }

    @Override
    public Points grantWelcomePoints(Char chr, int amount) {
        Points[] out = {Points.FAILED};
        EconomyLocks.packet(chr, () -> {
            if (!online(chr) || chr.getUser() == null
                    || net.swordie.ms.client.character.tms282.Tms282CashExpansionService.isBalanceUncertain(chr.getUser())) return null;
            try { out[0] = NewbiePoints.grant(chr.getUser(), chr.getId(), amount); }
            catch (SQLException | RuntimeException e) { System.out.println("[Newbie] welcome points retry required for " + chr.getId() + ": " + e.getClass().getSimpleName()); }
            return null;
        });
        return out[0];
    }
}
