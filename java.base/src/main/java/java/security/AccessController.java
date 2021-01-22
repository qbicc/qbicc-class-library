package java.security;

import cc.quarkus.qccrt.annotation.Tracking;

@Tracking("java.base/share/java/security/AccessController.java")
public final class AccessController {
    private AccessController() { }

    public static <T> T doPrivileged(PrivilegedAction<T> action) {
        return action.run();
    }

    public static <T> T doPrivilegedWithCombiner(PrivilegedAction<T> action) {
        return action.run();
    }

    public static <T> T doPrivileged(PrivilegedAction<T> action, AccessControlContext context) {
        return action.run();
    }

    public static <T> T doPrivileged(PrivilegedAction<T> action, AccessControlContext context, Permission... perms) {
        return action.run();
    }

    public static <T> T doPrivilegedWithCombiner(PrivilegedAction<T> action, AccessControlContext context, Permission... perms) {
        return action.run();
    }

    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action) throws PrivilegedActionException {
        try {
            return action.run();
        } catch (Exception e) {
            throw new PrivilegedActionException(e);
        }
    }

    public static <T> T doPrivilegedWithCombiner(PrivilegedExceptionAction<T> action) throws PrivilegedActionException {
        try {
            return action.run();
        } catch (Exception e) {
            throw new PrivilegedActionException(e);
        }
    }

    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action, AccessControlContext context) throws PrivilegedActionException {
        try {
            return action.run();
        } catch (Exception e) {
            throw new PrivilegedActionException(e);
        }
    }

    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action, AccessControlContext context, Permission... perms) throws PrivilegedActionException {
        try {
            return action.run();
        } catch (Exception e) {
            throw new PrivilegedActionException(e);
        }
    }

    public static <T> T doPrivilegedWithCombiner(PrivilegedExceptionAction<T> action, AccessControlContext context, Permission... perms) throws PrivilegedActionException {
        try {
            return action.run();
        } catch (Exception e) {
            throw new PrivilegedActionException(e);
        }
    }

    public static AccessControlContext getContext() {
        return new AccessControlContext(null, true);
    }

    public static void checkPermission(Permission perm) throws AccessControlException {
    }
}