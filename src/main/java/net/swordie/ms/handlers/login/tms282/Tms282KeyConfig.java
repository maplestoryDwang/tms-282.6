package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.Macro;
import net.swordie.ms.client.character.keys.Tms282KeyMapService;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282FuncKeyModifiedRequest;
import net.swordie.ms.connection.packet.Tms282KeyPackets;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * TMS 282.3 key configuration of the field character (the research probe's sendKeyConfig and onFuncKeyModified):
 * <ul>
 *     <li>{@link #sendInit}: after LP02EF, LP0B16 key map (4 presets, 282 layout) and LP02EE macros (at most 5), from
 *     what the 265 load read (FuncKeyMap/Macro). Presets 0..2 without saved bindings and missing quick slots get the
 *     265 defaults in memory ({@link Tms282KeyMapService#prepareForInit}); preset 3 without saved bindings is sent as
 *     u8 1 (client default). Only counts are logged, never key values or macro names.</li>
 *     <li>{@link #onModified}: CP020F. Type 0 updates the preset with the 265 FuncKeyMap API and writes the changed
 *     bindings ({@link Tms282FieldStore#saveKeys}); other types are logged only (265 keeps them in quest QR values);
 *     type 5 is presumed. A malformed packet is logged and dropped. No reply.</li>
 * </ul>
 */
final class Tms282KeyConfig {
    private static final net.swordie.ms.client.character.keys.Tms282CreationKeyMigration CREATION_REPAIR =
            new net.swordie.ms.client.character.keys.Tms282CreationKeyMigration(
                    java.nio.file.Path.of("data", "tms282-key-migrations"));
    private final Charset nameCharset;

    Tms282KeyConfig(Charset nameCharset) {
        this.nameCharset = Objects.requireNonNull(nameCharset, "nameCharset");
    }

    /** LP0B16 then LP02EE; a character whose key data cannot be encoded gets neither (logged, as the probe). */
    void sendInit(Tms282LoginState session, Char chr) {
        try {
            if (CREATION_REPAIR.repairIfUnchanged(chr))
                System.out.println("[TMS282] repaired captured Basic keyboard choice for character=36; persisted/readback verified");
        } catch (java.io.IOException | RuntimeException e) {
            System.out.println("[TMS282] character keyboard repair requires review: " + e.getMessage());
        }
        OutPacket keys;
        OutPacket macros;
        List<Integer> saved = new ArrayList<>();
        List<Integer> filled;
        List<Macro> macroList;
        try {
            if (chr.getFuncKeyMaps() != null) {
                chr.getFuncKeyMaps().forEach((preset, map) -> {
                    if (map != null && map.getKeymap() != null && !map.getKeymap().isEmpty()) {
                        saved.add(preset);
                    }
                });
            }
            Collections.sort(saved);
            filled = Tms282KeyMapService.prepareForInit(chr);
            keys = Tms282KeyPackets.funcKeyMappedInit(chr.getFuncKeyMaps(), chr.getQuickslotKeys());
            macroList = chr.getMacros() == null ? List.of() : chr.getMacros();
            macros = Tms282KeyPackets.macroSysDataInit(macroList, nameCharset);
        } catch (RuntimeException e) {
            System.out.println("[TMS282] SKIPPED LP0B16/LP02EE for character=" + chr.getId() + ": "
                    + e.getClass().getSimpleName() + " " + e.getMessage());
            return;
        }
        session.send(keys);
        session.send(macros);
        System.out.println("[TMS282] SENT LP0B16 key map character=" + chr.getId() + " savedPresets=" + saved
                + " defaultFilled=" + filled + " preset3=" + (saved.contains(3) ? "saved" : "client default")
                + " len=" + keys.getLength() + "; LP02EE macros=" + Math.min(macroList.size(), Tms282KeyPackets.MAX_MACROS)
                + "/" + macroList.size());
    }

    /** CP020F (guarded by IN_FIELD). */
    void onModified(Tms282LoginState session, byte[] packet, Tms282FieldStore store) {
        Tms282FuncKeyModifiedRequest request;
        try {
            request = Tms282FuncKeyModifiedRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP020F len=%d: %s%n", packet.length, e.getMessage());
            return;
        }
        Char chr = session.fieldCharacter();
        switch (request.payload()) {
            case Tms282FuncKeyModifiedRequest.KeyChanges k -> saveKeyChanges(chr, k, store);
            case Tms282FuncKeyModifiedRequest.ItemId item -> System.out.println("[TMS282] CP020F type " + request.updateType()
                    + " pet " + (request.updateType() == 1 ? "HP" : request.updateType() == 2 ? "MP" : "cure") + " potion "
                    + (item.itemId() > 0 ? "set" : "cleared") + " character=" + chr.getId() + " (logged only, not saved)");
            case Tms282FuncKeyModifiedRequest.ActivePreset a -> System.out.println("[TMS282] CP020F type 3 active preset="
                    + a.preset() + " character=" + chr.getId() + " (logged only, not saved)");
            case Tms282FuncKeyModifiedRequest.UnionKey u -> System.out.println("[TMS282] CP020F type 4 unionkey=" + u.value()
                    + " character=" + chr.getId() + " (logged only, not saved)");
            case Tms282FuncKeyModifiedRequest.AlternateKeyChanges a -> System.out.println("[TMS282] CP020F type 5 (PRESUMED) changes="
                    + a.changes().size() + " quickSlots=" + a.quickSlots().length + " character=" + chr.getId()
                    + " (logged only, not saved)");
        }
    }

    /** CP01B0; preserve the previous set on malformed input, unavailable skills or persistence failure. */
    void onMacrosModified(Tms282LoginState session, byte[] packet, Tms282FieldStore store) {
        Char chr = session.fieldCharacter();
        synchronized (chr) {
            List<Macro> macros;
            try {
                macros = net.swordie.ms.connection.packet.Tms282MacroRequest.parse(packet, nameCharset);
                net.swordie.ms.client.character.keys.Tms282MacroService.validate(chr, macros);
            } catch (IllegalArgumentException e) {
                System.out.println("[TMS282] REJECTED CP01B0 character=" + chr.getId() + " len=" + packet.length
                        + " reason=" + e.getMessage());
                session.send(Tms282KeyPackets.macroSysDataInit(chr.getMacros(), nameCharset));
                return;
            }
            try {
                store.saveMacros(chr, macros);
                chr.setMacros(macros);
                System.out.println("[TMS282] SAVED CP01B0 macros character=" + chr.getId() + " count=" + macros.size());
            } catch (RuntimeException e) {
                System.out.println("[TMS282] FAILED CP01B0 save character=" + chr.getId()
                        + " reason=" + e.getClass().getSimpleName() + " (previous macro set retained)");
                session.send(Tms282KeyPackets.macroSysDataInit(chr.getMacros(), nameCharset));
            }
        }
    }

    private static void saveKeyChanges(Char chr, Tms282FuncKeyModifiedRequest.KeyChanges k, Tms282FieldStore store) {
        if (k.changes().isEmpty()) {
            System.out.println("[TMS282] CP020F type 0 preset=" + k.preset() + " with no changes; nothing saved");
            return;
        }
        Tms282KeyMapService.Applied applied = Tms282KeyMapService.apply(chr, k);
        try {
            Tms282KeyMapService.Saved saved = store.saveKeys(applied);
            System.out.println("[TMS282] SAVED keymap character=" + chr.getId() + " preset=" + k.preset() + " changes="
                    + k.changes().size() + " seededDefault=" + applied.seeded() + " rowsWritten=" + saved.rowsWritten()
                    + " funckeymapId=" + saved.funcKeyMapId() + (saved.presetInserted() ? " (new preset row)" : ""));
        } catch (RuntimeException e) {
            System.out.println("[TMS282] FAILED keymap save character=" + chr.getId() + " preset=" + k.preset() + ": "
                    + e.getClass().getSimpleName() + " (in-memory change kept for this session)");
        }
    }
}
