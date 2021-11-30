package java.lang;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

// TODO: These are all stubs so we don't turn them into
//       an UnsatisfiedLinkError before the interpreter can
//       intercept them at build time.
//       Eventually we need a real runtime implementation too...
public final class Class$_native<T> {
    public boolean isAssignableFrom(Class<?> cls) {
        throw new UnsupportedOperationException();
    }

    private Field[] getDeclaredFields0(boolean publicOnly) {
        return new Field[0];
    }

    private Method[] getDeclaredMethods0(boolean publicOnly) {
        return new Method[0];
    }

    private Constructor<T>[] getDeclaredConstructors0(boolean publicOnly) {
        return new Constructor[0];
    }

    private Class<?>[] getDeclaredClasses0() {
        return new Class[0];
    }

    public Class<?> getSuperclass() {
        throw new UnsupportedOperationException();
    }

    public int getModifiers() {
        throw new UnsupportedOperationException();
    }
}
