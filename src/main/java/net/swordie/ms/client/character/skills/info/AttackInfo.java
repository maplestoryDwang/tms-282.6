package net.swordie.ms.client.character.skills.info;

import net.swordie.ms.client.character.skills.ProcessType;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.Summon;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;

import java.util.ArrayList;
import java.util.List;

public class AttackInfo {

    public InHeader inHeader;
    public byte fieldKey;
    public byte hits;
    public int mobCount;
    public int skillId;
    public int summonSpecialSkillId;
    public int slv;
    public int keyDown;
    public byte idk;
    public int mask;
    public boolean left;
    public short attackAction;
    public byte attackActionType;
    public byte idk0;
    public byte attackSpeed;

    public List<MobAttackInfo> mobAttackInfo = new ArrayList<>();

    public int y;
    public int x;
    public short forcedY;
    public short forcedX;
    public short rcDstRight;
    public short rectRight;
    public int option;
    public int[] mists;
    public short forcedYSh;
    public short forcedXSh;
    public byte force;
    public short delay;
    public short[] shortArr;
    public byte addAttackProc;
    public int crc1;
    public int crc2;
    public int crc3;
    public int grenadeId;
    public byte zeroTag;
    public int bySummonedID;
    public Position ptTarget = new Position();
    public int finalAttackLastSkillID;
    public byte finalAttackByte;
    public boolean ignorePCounter;
    public int spiritCoreEnhance;
    public Position idkPos = new Position();
    public Position pos = new Position();
    public Long value1;
    public Long value2;
    public Long value3;
    public Long value4;
    public byte fh;
    public byte teleportByte;
    public int shootObjId;
    public Position teleportPt = new Position();
    public byte pathFinderBool;
    public short Vx;

    public List<Position> positions;

    public Position grenadePos;
    public Rect rect;
    public int elemAttr;
    public int areaPAD;
    public int attackCount;
    public int wt;
    public int ar01Mad;
    public Summon summon;
    public int updateTime;
    public int bulletID;
    public short mobMove;
    public boolean isJablin;
    public int bulletSlot;
    public int maximumMobCount;
    public int bulletCount;
    public Position bodyRelMove;
    public Position keyDownRectMoveXY;
    public int tick;
    public byte someMask;
    public byte buckShot;
    public int option3;
    public byte passiveAddAttackCount;
    public byte showFixedDamage;
    public boolean dragon;
    public byte dragonAttackStart;
    public byte dragonAttackActionType;
    public byte dragonAttackProgess;
    public byte mastery;
    public byte actionSpeed;
    public byte shootRange;
    public byte byteIdk2;
    public byte byteIdk3;
    public byte byteIdk4;
    public byte byteIdk5;
    public OutHeader attackHeader;
    public int requestTime;
    public int summonID;
    public boolean boxAttack;

    public int shadowSpear1;
    public int shadowSpear2;

    public int throwableAttack;
    public byte throwableAttackFootHold;

    public boolean across = false;
    public Rect acrossRect;
    public Position acrossPos;

    public boolean rift1 = false;
    public boolean rift2 = false;

    public Position psychicTornadoXY = new Position();

    public Position charPosition = new Position();

    public boolean byUnreliableMemory;

    public short ptStart_X;
    public short hitRange;

    public int isMimickedBy;
    public int affectedAreaObjId;

    public ProcessType processType;
    public SkillInfo skillInfo;

    public long totalDamageDealt; // all damages over all mobs combined
    public boolean poolmakerEnabled;
}
