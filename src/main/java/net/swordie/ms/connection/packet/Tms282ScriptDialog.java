package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.life.npc.NpcMessageType;
import net.swordie.ms.life.npc.NpcScriptInfo;

import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.str;

/**
 * The TMS 282.3 NPC dialog kinds the server sends in LP0960 SCRIPT_MESSAGE, each with both directions of its wire
 * layout (strategy per kind):
 * <ul>
 *     <li>{@link #encodeBody}: the body after the common header, as the client's msgType handler reads it (jump table
 *     of 42B49F0 at 42B4B2E: index bytes 42B5250, targets 42B517C); every layout traced CLEAN in
 *     research/tms2823/script-trace;</li>
 *     <li>{@link #readAnswer}: the CP012C USER_SCRIPT_MESSAGE_ANSWER tail the same handler builds after its dialog
 *     closes ({@code u32 0, u8 wireType} are read by {@link Tms282ScriptAnswerRequest}).</li>
 * </ul>
 * 282 numbers differ from the 265 {@link NpcMessageType} values after 15: the accept dialog is 17 (265 sends 18,
 * which 282 reads as a text box: {@code str str i16 i16}) and the illustration dialogs moved up by one (31..38).
 * The 265 illustration variants have no traced 282 layout; they are shown as the plain dialog of the same kind
 * ({@link #of}) so their scripts keep working.
 * <p>
 * Three kinds are <em>answer-only</em> ({@link #isAnswerOnly}): LP0960 never shows them (42B4B2E sends 20, 21 and 42
 * to its default case); the client sends their CP012C when a cutscene step the script waits on ends:
 * {@link #IN_GAME_DIRECTION} (LP0502 delay / camera, 265 AskIngameDirection 21), {@link #PLAY_MOVIE} (LP0506 video,
 * 265 PlayMovieClip 22) and {@link #MONOLOGUE} (LP0502 type 15, 265 Monologue). Their answers always continue the
 * script (action 1): no script reads the value (research/tms2823/stuck-scene-audit).
 */
public enum Tms282ScriptDialog {
    /** 0 (42B52B0): {@code i32 index, [param&4: i32 speaker], str, u8 prev, u8 next, i32 delay, u8}. Answer (42B54E6):
     *  {@code i32 index, str, i8 action} (1 next 0x2001, 0 prev 0x2000, -1 close). */
    SAY(0) {
        @Override
        void encodeBody(OutPacket out, NpcScriptInfo nsi, NpcMessageType type, Charset charset) {
            out.encodeInt(nsi.getIndex());
            speakerIfParam(out, nsi);
            str(out, text(nsi), charset);
            out.encodeByte(type.isPrevPossible());
            out.encodeByte(type.isNextPossible());
            out.encodeInt(type.getDelay());
            out.encodeByte(0);
        }

        @Override
        Answer readAnswer(Tms282FieldRequestReader in, Charset charset) {
            in.i32();
            String text = in.str(charset);
            int action = (byte) in.u8();
            return new Answer(action, action, text);
        }
    },
    /** 2 (42B5A90): {@code u8 n, n x str}. Answer (42B5D64): {@code i8 action} (1 ok, -1 close). */
    SAY_IMAGE(2) {
        @Override
        void encodeBody(OutPacket out, NpcScriptInfo nsi, NpcMessageType type, Charset charset) {
            String[] images = nsi.getImages() == null ? new String[0] : nsi.getImages();
            Tms282LoginPacketSupport.checkByte(images.length, "image count");
            out.encodeByte(images.length);
            for (String image : images) {
                str(out, image, charset);
            }
        }
    },
    /** 3 (42B61C0, accept flag 0): {@code [param&4: i32 speaker], str}. Answer (42B63F0): {@code i8} (1 yes 6, 0 no 7, -1 close). */
    ASK_YES_NO(3) {
        @Override
        void encodeBody(OutPacket out, NpcScriptInfo nsi, NpcMessageType type, Charset charset) {
            speakerIfParam(out, nsi);
            str(out, text(nsi), charset);
        }
    },
    /** 4 (42B67B0): {@code [param&4: i32], str, str default, i16 min, i16 max}. Answer (42B69D9): {@code u8 ok, [str]}. */
    ASK_TEXT(4) {
        @Override
        void encodeBody(OutPacket out, NpcScriptInfo nsi, NpcMessageType type, Charset charset) {
            speakerIfParam(out, nsi);
            str(out, text(nsi), charset);
            str(out, nsi.getDefaultText() == null ? "" : nsi.getDefaultText(), charset);
            out.encodeShort(nsi.getMin());
            out.encodeShort(nsi.getMax());
        }

        @Override
        Answer readAnswer(Tms282FieldRequestReader in, Charset charset) {
            boolean ok = in.u8() == 1;
            return ok ? new Answer(1, 0, in.str(charset)) : Answer.CLOSED;
        }
    },
    /** 5 (42B8CA0): {@code [param&4: i32], str, i64 default, i64 min, i64 max}. Answer (42B8EC6): {@code u8 ok, [i64]}. */
    ASK_NUMBER(5) {
        @Override
        void encodeBody(OutPacket out, NpcScriptInfo nsi, NpcMessageType type, Charset charset) {
            speakerIfParam(out, nsi);
            str(out, text(nsi), charset);
            out.encodeLong(nsi.getDefaultNumber());
            out.encodeLong(nsi.getMin());
            out.encodeLong(nsi.getMax());
        }

        @Override
        Answer readAnswer(Tms282FieldRequestReader in, Charset charset) {
            if (in.u8() != 1) {
                return Answer.CLOSED;
            }
            long low = Integer.toUnsignedLong(in.i32());
            long value = low | (long) in.i32() << 32;
            return new Answer(1, Math.max(value, 0L), "");
        }
    },
    /** 6 (42B8FD0): {@code [param&4: i32], str} (the #L..#l links). Answer (42B91F3): {@code u8 ok, [i32 selection]}. */
    ASK_MENU(6) {
        @Override
        void encodeBody(OutPacket out, NpcScriptInfo nsi, NpcMessageType type, Charset charset) {
            speakerIfParam(out, nsi);
            str(out, text(nsi), charset);
        }

        @Override
        Answer readAnswer(Tms282FieldRequestReader in, Charset charset) {
            if (in.u8() != 1) {
                return Answer.CLOSED;
            }
            return new Answer(1, Math.max(in.i32(), 0), "");
        }
    },
    /** 10 (2BEA6B0): {@code i32 index, str, u8 angelic (DecodeBuffer 1), u8 n, n x i32, i32 zeroBeta}. Answer (2BEA948):
     *  {@code u8 ok, [u8 angelic, u8 flag, u8 selection]}. */
    ASK_AVATAR(10) {
        @Override
        void encodeBody(OutPacket out, NpcScriptInfo nsi, NpcMessageType type, Charset charset) {
            int[] options = nsi.getOptions() == null ? new int[0] : nsi.getOptions();
            Tms282LoginPacketSupport.checkByte(options.length, "avatar option count");
            out.encodeInt(nsi.getIndex());
            str(out, text(nsi), charset);
            out.encodeByte(nsi.isAngelicBuster());
            out.encodeByte(options.length);
            for (int option : options) {
                out.encodeInt(option);
            }
            out.encodeInt(nsi.isZeroBeta() ? 1 : 0);
        }

        @Override
        Answer readAnswer(Tms282FieldRequestReader in, Charset charset) {
            if (in.u8() != 1) {
                return Answer.CLOSED;
            }
            in.u8();
            in.u8();
            return new Answer(1, in.u8(), "");
        }
    },
    /** 17 (42B64F0): {@code i32 speaker, str} (the 265 AskAccept body). Answer (42B66D1): {@code i8} as {@link #ASK_YES_NO}. */
    ASK_ACCEPT(17) {
        @Override
        void encodeBody(OutPacket out, NpcScriptInfo nsi, NpcMessageType type, Charset charset) {
            out.encodeInt(nsi.getOverrideSpeakerTemplateID());
            str(out, text(nsi), charset);
        }
    },
    /**
     * 20, answer-only: the in-game direction ask, built by 3B349F0(this, flag, ask, value) for LP0502 (3B2FC80) steps:
     * {@code u8 ask, u8 skip, u8 flag}, then {@code flag != 0}: ask 3 {@code i32}, ask 4 {@code i32 i32};
     * {@code flag == 0}: ask 1/3 {@code i32 i32 i32}, ask 4 {@code i32 i32}; ask 2 nothing. {@code skip} is
     * 5E63580 (direction skip mode). ask 1 = delay (type 2: 3B33560 {@code 1,1} when the timer ends, 3B2FE6E
     * {@code 1,0} at once for a negative delay or one already pending), 2 = pattern input (type 5), 3 = camera
     * (types 7 move, 9 zoom, 11 release), 4 = 282 type 40 only. Live 09-29: {@code 14 01 00 01} after
     * enter_450003000 {@code sm.sendDelay(5000)}.
     */
    IN_GAME_DIRECTION(20) {
        @Override
        Answer readAnswer(Tms282FieldRequestReader in, Charset charset) {
            int ask = in.u8();
            in.u8(); // skip mode
            boolean flag = in.u8() != 0;
            int ints = switch (ask) {
                case 1 -> flag ? 0 : 3;
                case 2 -> 0;
                case 3 -> flag ? 1 : 3;
                case 4 -> 2;
                default -> throw new IllegalArgumentException("CP012C direction ask " + ask + " has no 282 layout");
            };
            for (int i = 0; i < ints; i++) {
                in.i32();
            }
            return new Answer(1, ask, "");
        }
    },
    /** 21, answer-only: the LP0506 video (5788CCD {@code str, u8}) ended, 58A3D80: {@code u8 result} (1 played, 0 failed). */
    PLAY_MOVIE(21) {
        @Override
        Answer readAnswer(Tms282FieldRequestReader in, Charset charset) {
            return new Answer(1, in.u8(), "");
        }
    },
    /** 42, answer-only: the LP0502 type 15 monologue was shown, 4921E00 (called at 3B30B08): no tail. */
    MONOLOGUE(42) {
        @Override
        Answer readAnswer(Tms282FieldRequestReader in, Charset charset) {
            return new Answer(1, 0, "");
        }
    };

    /** Wire type 16 (42B61C0 with the accept flag) answers like {@link #ASK_YES_NO}; the server does not send it. */
    public static final int ACCEPT_YES_NO_WIRE_TYPE = 16;

    private static final Map<NpcMessageType, Tms282ScriptDialog> BY_265_TYPE = new EnumMap<>(NpcMessageType.class);

    static {
        for (NpcMessageType t : new NpcMessageType[]{NpcMessageType.Say, NpcMessageType.SayOk, NpcMessageType.SayNext,
                NpcMessageType.SayPrev, NpcMessageType.SayIllustration, NpcMessageType.SayIllustrationOk,
                NpcMessageType.SayIllustrationNext, NpcMessageType.SayIllustrationPrev, NpcMessageType.SayDualIllustration}) {
            BY_265_TYPE.put(t, SAY);
        }
        BY_265_TYPE.put(NpcMessageType.SayImage, SAY_IMAGE);
        for (NpcMessageType t : new NpcMessageType[]{NpcMessageType.AskYesNo, NpcMessageType.AskYesNoIllustration,
                NpcMessageType.AskYesNoDualIllustration}) {
            BY_265_TYPE.put(t, ASK_YES_NO);
        }
        BY_265_TYPE.put(NpcMessageType.AskText, ASK_TEXT);
        BY_265_TYPE.put(NpcMessageType.AskNumber, ASK_NUMBER);
        for (NpcMessageType t : new NpcMessageType[]{NpcMessageType.AskMenu, NpcMessageType.AskMenuIllustration,
                NpcMessageType.AskMenuDualIllustration}) {
            BY_265_TYPE.put(t, ASK_MENU);
        }
        BY_265_TYPE.put(NpcMessageType.AskAvatar, ASK_AVATAR);
        for (NpcMessageType t : new NpcMessageType[]{NpcMessageType.AskAccept, NpcMessageType.AskAcceptIllustration,
                NpcMessageType.AskAcceptDualIllustration}) {
            BY_265_TYPE.put(t, ASK_ACCEPT);
        }
    }

    private final int wireType;

    Tms282ScriptDialog(int wireType) {
        this.wireType = wireType;
    }

    /** The msgType byte of LP0960 (and of the CP012C answer). */
    public int wireType() {
        return wireType;
    }

    /** The 282 dialog that shows a 265 {@code type}; empty when 282 has no traced layout for it. */
    public static Optional<Tms282ScriptDialog> of(NpcMessageType type) {
        return Optional.ofNullable(type == null ? null : BY_265_TYPE.get(type));
    }

    /** The dialog whose answer has {@code wireType} (16 = the accept flavour of {@link #ASK_YES_NO}; 20/21/42 answer-only). */
    public static Optional<Tms282ScriptDialog> byWireType(int wireType) {
        if (wireType == ACCEPT_YES_NO_WIRE_TYPE) {
            return Optional.of(ASK_YES_NO);
        }
        for (Tms282ScriptDialog d : values()) {
            if (d.wireType == wireType) {
                return Optional.of(d);
            }
        }
        return Optional.empty();
    }

    /** The LP0960 body; the answer-only kinds have none (LP0960 does not show them). */
    void encodeBody(OutPacket out, NpcScriptInfo nsi, NpcMessageType type, Charset charset) {
        throw new UnsupportedOperationException("LP0960 does not show " + this + " (answer-only CP012C type " + wireType + ")");
    }

    /** True for the kinds the client answers after a cutscene step (LP0502 / LP0506), never shown by LP0960. */
    public boolean isAnswerOnly() {
        return this == IN_GAME_DIRECTION || this == PLAY_MOVIE || this == MONOLOGUE;
    }

    /**
     * The kind whose CP012C a script that showed {@code shown} waits for: {@link #of} for the LP0960 dialogs, the
     * answer-only kind for the 265 ScriptManagerImpl waits that set AskIngameDirection (sendDelay, moveCamera,
     * zoomCamera), PlayMovieClip (playVideoByScript) or Monologue (sayMonologue).
     */
    public static Optional<Tms282ScriptDialog> awaitedBy(NpcMessageType shown) {
        if (shown == null) {
            return Optional.empty();
        }
        return switch (shown) {
            case AskIngameDirection -> Optional.of(IN_GAME_DIRECTION);
            case PlayMovieClip -> Optional.of(PLAY_MOVIE);
            case Monologue -> Optional.of(MONOLOGUE);
            default -> of(shown);
        };
    }

    /**
     * Whether this answer may be handed to a script that showed {@code shown}. An answer-only kind is delivered only
     * to the wait it ends: the client also sends them unasked (LP0502 type 11 camera release answers ask 3, a
     * second delay is answered at once), and delivered to a Say they would skip the page.
     */
    public boolean answers(NpcMessageType shown) {
        return !isAnswerOnly() || shown == null || awaitedBy(shown).orElse(null) == this;
    }

    /** The answer tail after {@code u32, u8 wireType}. Default: one signed action byte (yes/no, accept, image). */
    Answer readAnswer(Tms282FieldRequestReader in, Charset charset) {
        int action = (byte) in.u8();
        return new Answer(action, action, "");
    }

    /**
     * What the 265 ScriptManagerImpl.handleAction takes: {@code action} (-1 closes the script; for Say 1 next / 0
     * back; for yes/no 1 / 0), {@code answer} (menu selection, number, avatar choice), {@code text} (AskText).
     */
    public record Answer(int action, long answer, String text) {
        public static final Answer CLOSED = new Answer(-1, 0, "");
    }

    private static void speakerIfParam(OutPacket out, NpcScriptInfo nsi) {
        if ((nsi.getParam() & 4) != 0) {
            out.encodeInt(nsi.getOverrideSpeakerTemplateID());
        }
    }

    private static String text(NpcScriptInfo nsi) {
        return nsi.getText() == null ? "" : nsi.getText();
    }
}
