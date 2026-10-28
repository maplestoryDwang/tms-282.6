package net.swordie.ms.connection.api.admin;

import java.util.Objects;

/** One admin request: the (loopback) source and the collaborators the actions use. */
public record AdminContext(String sourceIp, AdminEnvironment env) {
    public AdminContext {
        Objects.requireNonNull(env, "env");
        sourceIp = sourceIp == null ? "" : sourceIp;
    }
}
