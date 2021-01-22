package java.security;

import cc.quarkus.qccrt.annotation.Tracking;

@Tracking("java.base/share/java/security/AccessControlContext.java")
public final class AccessControlContext {
    static final ProtectionDomain[] FAKE_CONTEXT = new ProtectionDomain[0];

    public AccessControlContext(ProtectionDomain[] context) {
    }

    public AccessControlContext(AccessControlContext acc, DomainCombiner combiner) {
    }

    AccessControlContext(AccessControlContext acc, DomainCombiner combiner, boolean preauthorized) {
    }

    AccessControlContext(ProtectionDomain caller, DomainCombiner combiner, AccessControlContext parent, AccessControlContext context, Permission[] perms) {
    }

    AccessControlContext(ProtectionDomain[] context, AccessControlContext other) {}

    AccessControlContext(ProtectionDomain[] context, boolean isPrivileged) {}

    public DomainCombiner getDomainCombiner() {
        return null;
    }

    DomainCombiner getCombiner() {
        return null;
    }

    ProtectionDomain[] getContext() {
        return FAKE_CONTEXT;
    }

    public void checkPermission(Permission perm) throws AccessControlException {}

    AccessControlContext optimize() {
        return this;
    }

    public boolean equals(Object obj) {
        return true;
    }

    public int hashCode() {
        return 0;
    }
}
