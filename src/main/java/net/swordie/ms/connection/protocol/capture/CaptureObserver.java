package net.swordie.ms.connection.protocol.capture;

import java.io.IOException;

/**
 * Where a captured packet goes (Observer / Strategy): the evidence folder ({@link CaptureSession}) or the GUI's
 * packet trace ({@link CaptureTraceObserver}). {@link PacketCapture} calls observers on its writer thread, one
 * packet at a time in socket order; a failing observer is switched off for that socket and never ends it.
 */
public interface CaptureObserver {
    void accept(CapturedPacket packet) throws IOException;

    /** Short name for the log line written when this observer fails. */
    String describe();
}
