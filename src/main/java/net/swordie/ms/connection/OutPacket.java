package net.swordie.ms.connection;

import net.swordie.ms.connection.api.ApiOutHeader;
import net.swordie.ms.connection.protocol.OpcodeTable;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolProfile;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OutPacket {

    private final ByteArrayOutputStream baos;
    private short op;
    private byte[] auxiliaryData;
    /** Header a 265 builder asked for; null for raw-opcode and data packets. */
    private OutHeader origin;
    /** Profile whose codec built this packet ({@link #forProfile}); null for generic builders. */
    private ProtocolVersion builtFor;
    /** True when the active profile has no opcode for {@link #origin}; the OutboundGuard drops it. */
    private boolean suppressed;

    /** Separate CInPacket cursor, carried after the main body in an extended frame. */
    public void setAuxiliaryData(byte[] data) {
        auxiliaryData = java.util.Objects.requireNonNull(data, "auxiliary data").clone();
    }

    public boolean hasAuxiliaryData() {
        return auxiliaryData != null;
    }

    public byte[] getAuxiliaryData() {
        return auxiliaryData == null ? new byte[0] : auxiliaryData.clone();
    }

    public byte[] getFrameData() {
        byte[] main = getData();
        if (auxiliaryData == null) return main;
        long length = (long) main.length + auxiliaryData.length + 4;
        if (main.length < 2 || length > 0xFFFFFF) throw new IllegalArgumentException("Invalid auxiliary packet length");
        return java.nio.ByteBuffer.allocate((int) length).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                .put(main).put(auxiliaryData).putInt(auxiliaryData.length).array();
    }

    /**
     * Creates a new OutPacket with a given op. Immediately encodes the op.
     *
     * @param op The opcode of this OutPacket.
     */
    public OutPacket(short op) {
        this.baos = new ByteArrayOutputStream(32);
        encodeShort(op);
        this.op = op;
    }

    /**
     * Creates a new OutPacket with a given op. Immediately encodes the op.
     *
     * @param op The opcode of this OutPacket.
     */
    public OutPacket(int op) {
        this((short) op);
    }

    /**
     * Creates a new OutPacket, and initializes the data as empty.
     */
    public OutPacket() {
        this.baos = new ByteArrayOutputStream(32);
    }

    /**
     * Creates a new OutPacket with given data.
     *
     * @param data The data this net.swordie.ms.connection.packet has to be initialized with.
     */
    public OutPacket(byte[] data) {
        this.baos = new ByteArrayOutputStream(32);
        encodeArr(data);
    }

    /**
     * Creates a new OutPacket with a given header. Immediately encodes the header's wire opcode in the
     * active protocol profile (GMS 265: the header's own value). When the profile has no opcode for it,
     * the 265 value is written as a placeholder and the packet is marked {@link #isSuppressed() suppressed}.
     *
     * @param header The header of this OutPacket.
     */
    public OutPacket(OutHeader header) {
        this(header, Protocol.active().opcodes());
    }

    private OutPacket(OutHeader header, OpcodeTable opcodes) {
        this.baos = new ByteArrayOutputStream(32);
        this.origin = header;
        int wire = opcodes.wire(header);
        if (wire == OpcodeTable.NO_OPCODE) {
            this.suppressed = true;
            wire = header.getValue();
        }
        encodeShort((short) wire);
        this.op = (short) wire;
    }

    /**
     * A packet built by a profile codec with that profile's raw wire opcode. Only the OutboundGuard of
     * {@code version} lets it through.
     */
    public static OutPacket forProfile(ProtocolVersion version, int wireOp) {
        OutPacket packet = new OutPacket(wireOp);
        packet.builtFor = java.util.Objects.requireNonNull(version, "version");
        return packet;
    }

    /**
     * A packet built by a profile codec for a 265 header, using that profile's opcode for it (suppressed when
     * the profile has none).
     */
    public static OutPacket forProfile(ProtocolProfile profile, OutHeader header) {
        OutPacket packet = new OutPacket(header, profile.opcodes());
        packet.builtFor = profile.version();
        return packet;
    }

    public OutPacket(ApiOutHeader requestTokenResult) {
        this(requestTokenResult.getVal());
    }

    /**
     * Returns the header of this OutPacket.
     *
     * @return the header of this OutPacket.
     */
    public int getHeader() {
        return op;
    }

    public void setHeader(int op) {
        this.op = (short) op;
    }

    /** Header a 265 builder used, or null for raw-opcode and data packets. */
    public OutHeader getOrigin() {
        return origin;
    }

    /** Profile whose codec built this packet, or null for generic builders. */
    public ProtocolVersion getBuiltFor() {
        return builtFor;
    }

    /** Whether the active profile has no opcode for {@link #getOrigin()}; such a packet is never sent. */
    public boolean isSuppressed() {
        return suppressed;
    }

    /**
     * Encodes a single byte to this OutPacket.
     *
     * @param b The int to encode as a byte. Will be downcast, so be careful.
     */
    public void encodeByte(int b) {
        encodeByte((byte) b);
    }

    /**
     * Encodes a byte to this OutPacket.
     *
     * @param b The byte to encode.
     */
    public void encodeByte(byte b) {
        this.baos.write(b);
    }

    /**
     * Encodes a byte array to this OutPacket.
     * Named like this to prevent autocompletion of "by" to "byteArray" or similar names.
     *
     * @param b The byte array to encode.
     */
    public void encodeArr(byte[] b) {
        for (int x = 0; x < b.length; x++) {
            this.baos.write(b[x]);
        }
    }

    /**
     * Encodes a byte array to this OutPacket.
     *
     * @param arr the byte array, in string format (may contain '|' and whitespace to seperate bytes)
     */
    public void encodeArr(String arr) {
        encodeArr(Util.getByteArrayByString(arr));
    }

    /**
     * Encodes a character to this OutPacket, UTF-8.
     *
     * @param c The character to encode
     */
    public void encodeChar(char c) {
        this.baos.write(c);
    }

    /**
     * Encodes a boolean to this OutPacket.
     *
     * @param b The boolean to encode (0/1)
     */
    public void encodeByte(boolean b) {
        this.baos.write(b ? 1 : 0);
    }

    /**
     * Encodes a short to this OutPacket, in little endian.
     *
     * @param s The short to encode.
     */
    public void encodeShort(short s) {
        this.baos.write((byte)(s & 0xFF));
        this.baos.write((byte)(s >>> 8 & 0xFF));
    }

    /**
     * Encodes an integer to this OutPacket, in little endian.
     *
     * @param i The integer to encode.
     */
    public void encodeInt(int i) {
        if (i != -88888) {
            this.baos.write((byte)(i & 0xFF));
            this.baos.write((byte)(i >>> 8 & 0xFF));
            this.baos.write((byte)(i >>> 16 & 0xFF));
            this.baos.write((byte)(i >>> 24 & 0xFF));
        }
    }

    public void encodeInt(long i) {
        this.baos.write((byte)(i & 255L));
        this.baos.write((byte)(i >>> 8 & 255L));
        this.baos.write((byte)(i >>> 16 & 255L));
        this.baos.write((byte)(i >>> 24 & 255L));
    }

    /**
     * Encodes a long to this OutPacket, in little endian.
     *
     * @param l The long to encode.
     */
    public void encodeLong(long l) {
        this.baos.write((byte)(l & 255L));
        this.baos.write((byte)(l >>> 8 & 255L));
        this.baos.write((byte)(l >>> 16 & 255L));
        this.baos.write((byte)(l >>> 24 & 255L));
        this.baos.write((byte)(l >>> 32 & 255L));
        this.baos.write((byte)(l >>> 40 & 255L));
        this.baos.write((byte)(l >>> 48 & 255L));
        this.baos.write((byte)(l >>> 56 & 255L));
    }

    /**
     * Encodes a String to this OutPacket.
     * Structure: short(size) + char array of <code>s</code>.
     *
     * @param s The String to encode.
     */
    public void encodeString(String s) {
        if (s == null) {
            s = "";
        }
        if (!isMojibakeVietnamese(s)) {
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > Short.MAX_VALUE) {
                System.out.println("Tried to encode a string that is too big.");
                return;
            }
            encodeShort(bytes.length);
            for (byte b : bytes) {
                encodeByte(b);
            }
        } else {
            if (s.length() > Short.MAX_VALUE) {
                System.out.println("Tried to encode a string that is too big.");
                return;
            }
            encodeShort((short) s.length());
            for (char c : s.toCharArray()) {
                encodeChar(c);
            }
        }
    }

    private boolean isMojibakeVietnamese(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        String mojibakeRegex = "[º£Æª«»‡´¶¢®æø¯Øçïö¬¡¤¸¹²³€¥°™©±½¾Üî]";
        Pattern pattern = Pattern.compile(mojibakeRegex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(s);
        return matcher.find();
    }

    /**
     * Writes a String as a character array to this OutPacket.
     * If <code>s.length()</code> is smaller than length, the open spots are filled in with zeros.
     *
     * @param s      The String to encode.
     * @param length The maximum length of the buffer.
     */
    public void encodeString(String s, int length) {
        if (s == null) {
            s = "";
        }
        if (!isMojibakeVietnamese(s)) {
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > Short.MAX_VALUE) {
                System.out.println("Tried to encode a string that is too big.");
                return;
            }
            for (byte b : bytes) {
                encodeByte(b);
            }
            for (int i = bytes.length; i < length; i++) {
                encodeByte((byte) 0);
            }
        } else {
            if (s.length() > Short.MAX_VALUE) {
                System.out.println("Tried to encode a string that is too big.");
                return;
            }
            encodeShort((short) s.length());
            for (char c : s.toCharArray()) {
                encodeChar(c);
            }
            for (int i = s.length(); i < length; i++) {
                encodeByte((byte) 0);
            }
        }
    }

    public byte[] getData() {
        return this.baos.toByteArray();
    }

    /**
     * Returns the length of the ByteArrayOutputStream.
     *
     * @return The length of baos.
     */
    public int getLength() {
        return this.baos.size();
    }

    @Override
    public String toString() {
        return String.format("%s, %s/0x%s\t| %s", OutHeader.getOutHeaderByOp(op), op, Integer.toHexString(op).toUpperCase()
                , Util.readableByteArray(Arrays.copyOfRange(getData(), 2, getData().length)));
    }

    public void encodeShort(int value) {
        encodeShort((short) value);
    }

    public void encodeFT(FileTime fileTime) {
        if (fileTime == null) {
            encodeLong(0);
        } else {
            fileTime.encode(this);
        }
    }

    public void encodePosition(Position position) {
        if (position != null) {
            encodeShort(position.getX());
            encodeShort(position.getY());
        } else {
            encodeShort(0);
            encodeShort(0);
        }
    }

    public void encodeRectInt(Rect rect) {
        encodeInt(rect.getLeft());
        encodeInt(rect.getTop());
        encodeInt(rect.getRight());
        encodeInt(rect.getBottom());
    }

    public void encodePositionInt(Position position) {
        encodeInt(position.getX());
        encodeInt(position.getY());
    }

    public void encodeFT(long currentTime) {
        encodeFT(new FileTime(currentTime));
    }

    public void encodeTime(boolean dynamicTerm, int time) {
        encodeByte(dynamicTerm);
        encodeInt(time);
    }

    public void encodeTime(int time) {
        encodeByte(false);
        encodeInt(time);
    }

    public void encodeFT(LocalDateTime localDateTime) {
        encodeFT(FileTime.fromDate(localDateTime));
    }

    public void encode(Encodable encodable) {
        encodable.encode(this);
    }
}
