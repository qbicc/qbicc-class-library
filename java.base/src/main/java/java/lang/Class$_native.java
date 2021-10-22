package java.lang;

import java.lang.reflect.Field;

// TODO: These are all stubs so we don't turn them into
//       an UnsatisfiedLinkError before the interpreter can
//       intercept them at build time.
//       Eventually we need a real runtime implementation too...
public final class Class$_native<T> {
    public boolean isAssignableFrom(Class<?> cls) {
        throw new UnsupportedOperationException();
    }

    private Field[] getDeclaredFields0(boolean publicOnly) {
        throw new UnsupportedOperationException();
    }

    public Class<?> getSuperclass() {
        throw new UnsupportedOperationException();
    }

    public int getModifiers() {
        throw new UnsupportedOperationException();
    }
}
