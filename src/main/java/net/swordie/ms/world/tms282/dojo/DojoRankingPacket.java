package net.swordie.ms.world.tms282.dojo;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.List;
import java.util.Optional;

/**
 * DOJANG_RANKING_RESULT filled from {@link DojoRankService}. The layout mirrors the placeholder builder
 * {@code WvsContext.dojangRankingResult(int, Char)} field for field (that builder only takes a Char, so it cannot
 * carry ranking data); the layout itself is NOT verified against the 282 client yet.
 * Blocks: 1 = this week overall, 2 = this week job line, 3 = last week overall, 4 = last week job line.
 */
public final class DojoRankingPacket {
    private DojoRankingPacket() {
    }

    public static OutPacket build(int typeID, Char chr, int listLimit) throws java.sql.SQLException {
        DojoRankService svc = DojoRankService.get();
        List<DojoRankEntry> now = svc.currentRanking();
        List<DojoRankEntry> last = svc.previousRanking();
        int line = DojoRecord.jobLineOf(chr.getJob());
        List<DojoRankEntry> nowJob = now.stream().filter(e -> e.record().jobLine() == line).toList();
        List<DojoRankEntry> lastJob = last.stream().filter(e -> e.record().jobLine() == line).toList();

        OutPacket out = new OutPacket(OutHeader.DOJANG_RANKING_RESULT);
        block(out, 1, typeID, chr, now, last, false, listLimit);
        block(out, 2, typeID, chr, nowJob, lastJob, true, listLimit);
        block(out, 3, typeID, chr, last, List.of(), false, listLimit);
        out.encodeByte(true);
        block(out, 4, typeID, chr, lastJob, List.of(), true, listLimit);
        return out;
    }

    private static void block(OutPacket out, int block, int typeID, Char chr, List<DojoRankEntry> ranking,
                              List<DojoRankEntry> previous, boolean jobRanking, int listLimit) {
        DojoRankService svc = DojoRankService.get();
        Optional<DojoRankEntry> me = svc.find(ranking, chr.getId());
        Optional<DojoRankEntry> before = svc.find(previous, chr.getId());
        out.encodeInt(block);
        out.encodeByte(typeID);
        out.encodeInt(chr.getJob());
        out.encodeInt(chr.getLevel());
        out.encodeInt(me.map(e -> e.record().stage()).orElse(0)); // point
        out.encodeInt(me.map(e -> position(e, jobRanking)).orElse(0)); // ranking
        out.encodeInt(me.map(e -> percent(position(e, jobRanking), ranking.size())).orElse(0)); // percent
        out.encodeInt(before.map(e -> e.record().stage()).orElse(0)); // lastpoint
        out.encodeInt(before.map(e -> position(e, jobRanking)).orElse(0)); // lastranking
        out.encodeInt(before.map(e -> percent(position(e, jobRanking), previous.size())).orElse(0)); // lastpercent
        out.encodeByte(typeID);
        List<DojoRankEntry> top = ranking.subList(0, Math.min(Math.max(0, listLimit), ranking.size()));
        out.encodeInt(top.size());
        for (DojoRankEntry e : top) {
            out.encodeInt(e.record().job());
            out.encodeInt(e.record().level());
            out.encodeInt(e.record().stage());
            out.encodeInt(position(e, jobRanking));
            out.encodeString(e.record().name());
            out.encodeByte(false);
        }
    }

    private static int position(DojoRankEntry e, boolean jobRanking) {
        return jobRanking ? e.jobRank() : e.rank();
    }

    private static int percent(int position, int total) {
        return total <= 0 ? 0 : Math.max(1, position * 100 / total);
    }
}
