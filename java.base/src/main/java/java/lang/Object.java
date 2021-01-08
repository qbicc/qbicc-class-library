package java.lang;

public class Object {

    public Object() {}

    public final native Class<?> getClass();

    public native int hashCode();

    public boolean equals(Object other) {
        return this == other;
    }

    public final native void notify();

    public final native void notifyAll();

    public native String toString();

    public final void wait() throws InterruptedException {
        wait(0L);
    }

    public final native void wait(long millis) throws InterruptedException;

    public final native void wait(long millis, int nanos) throws InterruptedException;

    protected native Object clone() throws CloneNotSupportedException;

    @Deprecated
    protected void finalize() throws Throwable {}
}
