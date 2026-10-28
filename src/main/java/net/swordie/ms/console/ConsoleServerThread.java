package net.swordie.ms.console;

public final class ConsoleServerThread extends Thread {
    private final Runnable onSuccess;
    private final Runnable onFailure;

    public ConsoleServerThread(Runnable onSuccess, Runnable onFailure) {
        super("TMS282.3-ConsoleServer");
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
        setDaemon(false);
    }

    @Override
    public void run() {
        try {
            ConsoleServerBootstrap.run();
            onSuccess.run();
        } catch (Throwable t) {
            t.printStackTrace();
            onFailure.run();
        }
    }
}