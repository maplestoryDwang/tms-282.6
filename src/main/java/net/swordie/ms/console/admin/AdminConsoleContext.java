package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import java.awt.Component;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * What a tab needs from the console: calls to the admin API off the event thread (or inline in the screenshot mode),
 * the shared "server down / retry" state, the status line and confirmation dialogs.
 */
final class AdminConsoleContext {
    /** Reports the connection state to the header. */
    interface StatusSink {
        void connected(boolean up, String message);

        void status(String message);
    }

    private final AdminGateway gateway;
    private final boolean synchronous;
    private final StatusSink sink;
    private Component dialogParent;

    AdminConsoleContext(AdminGateway gateway, boolean synchronous, StatusSink sink) {
        this.gateway = gateway;
        this.synchronous = synchronous;
        this.sink = sink;
    }

    AdminGateway gateway() {
        return gateway;
    }

    void dialogParent(Component c) {
        this.dialogParent = c;
    }

    /** Calls {@code action}; the result (ok or not) goes to {@code onResult} on the event thread. */
    void call(String action, Map<String, Object> params, Consumer<JsonNode> onResult) {
        run(() -> gateway.call(action, params), onResult);
    }

    void call(String action, Consumer<JsonNode> onResult) {
        call(action, Map.of(), onResult);
    }

    <T> void run(Callable<T> work, Consumer<T> onResult) {
        run(work, onResult, null);
    }

    /** Like {@link #run(Callable, Consumer)}; {@code onFailure} (may be null) also hears of a failure, on the event thread. */
    <T> void run(Callable<T> work, Consumer<T> onResult, Consumer<Exception> onFailure) {
        if (synchronous) {
            try {
                T r = work.call();
                sink.connected(true, null);
                onResult.accept(r);
            } catch (AdminCallException e) {
                failed(e);
                reportFailure(onFailure, e);
            } catch (Exception e) {
                sink.status("錯誤：" + e);
                reportFailure(onFailure, e);
            }
            return;
        }
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                try {
                    T r = get();
                    sink.connected(true, null);
                    onResult.accept(r);
                } catch (java.util.concurrent.ExecutionException e) {
                    if (e.getCause() instanceof AdminCallException a) {
                        failed(a);
                    } else {
                        sink.status("錯誤：" + e.getCause());
                    }
                    reportFailure(onFailure, e.getCause() instanceof Exception x ? x : e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException e) {
                    sink.status("畫面更新失敗：" + e);
                }
            }
        }.execute();
    }

    private static void reportFailure(Consumer<Exception> onFailure, Exception e) {
        if (onFailure != null) {
            onFailure.accept(e);
        }
    }

    private void failed(AdminCallException e) {
        if (e.kind() == AdminCallException.Kind.SERVER_DOWN) {
            sink.connected(false, e.getMessage());
        } else {
            sink.status(e.getMessage());
        }
    }

    void status(String message) {
        sink.status(message);
    }

    /** OK = true. */
    boolean confirm(String title, String message) {
        if (synchronous) {
            return false;
        }
        return JOptionPane.showConfirmDialog(dialogParent, message, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
    }

    void info(String title, String message) {
        if (synchronous) {
            sink.status(message);
            return;
        }
        JOptionPane.showMessageDialog(dialogParent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    void error(String title, String message) {
        if (synchronous) {
            sink.status(message);
            return;
        }
        JOptionPane.showMessageDialog(dialogParent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /** A text the user has to type exactly (second confirmation). */
    boolean typed(String title, String message, String expected) {
        if (synchronous) {
            return false;
        }
        Object in = JOptionPane.showInputDialog(dialogParent, message, title, JOptionPane.WARNING_MESSAGE, null, null, "");
        return in != null && expected.equals(in.toString().strip());
    }
}
