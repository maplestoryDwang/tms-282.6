package net.swordie.ms.world.tms282.dojo;

/** A ranked dojo record: {@code rank} is the overall position, {@code jobRank} the position inside the job line. */
public record DojoRankEntry(DojoRecord record, int rank, int jobRank) {
}
