package java.lang;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.Permission;

import cc.quarkus.qccrt.annotation.Tracking;

@Tracking("java.base/share/java/lang/SecurityManager.java")
public class SecurityManager {

    public SecurityManager() {
    }

    protected Class<?>[] getClassContext() {
        // todo: use stack walker
        throw new UnsupportedOperationException();
    }

    public Object getSecurityContext() {
        return AccessController.getContext();
    }

    public void checkPermission(Permission perm) {
    }

    public void checkPermission(Permission perm, Object context) {
    }

    public void checkCreateClassLoader() {
    }

    public void checkAccess(Thread t) {
    }

    public void checkAccess(ThreadGroup g) {
    }

    public void checkExit(int status) {
    }

    public void checkExec(String cmd) {
    }

    public void checkLink(String lib) {
    }

    public void checkRead(FileDescriptor fd) {
    }

    public void checkRead(String file) {
    }

    public void checkRead(String file, Object context) {
    }

    public void checkWrite(FileDescriptor fd) {
    }

    public void checkWrite(String file) {
    }

    public void checkDelete(String file) {
    }

    public void checkConnect(String host, int port) {
    }

    public void checkConnect(String host, int port, Object context) {
    }

    public void checkListen(int port) {
    }

    public void checkAccept(String host, int port) {
    }

    public void checkMulticast(InetAddress maddr) {
    }

    public void checkMulticast(InetAddress maddr, byte ttl) {
    }

    public void checkPropertiesAccess() {
    }

    public void checkPropertyAccess(String key) {
    }

    public void checkPrintJobAccess() {
    }

    static void addNonExportedPackages(ModuleLayer layer) {
    }

    static void invalidatePackageAccessCache() {
    }

    public void checkPackageAccess(String pkg) {
    }

    public void checkPackageDefinition(String pkg) {
    }

    public void checkSetFactory() {
    }

    public void checkSecurityAccess(String target) {
    }

    public ThreadGroup getThreadGroup() {
        return Thread.currentThread().getThreadGroup();
    }
}
