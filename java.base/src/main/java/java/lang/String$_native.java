package java.lang;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Native bindings for {@link String}.
 */
public final class String$_native {
    private static final ReferenceQueue<String> REF_QUEUE = new ReferenceQueue<>();
    private static final ConcurrentHashMap<Key, Ref> INTERNED = new ConcurrentHashMap<>();

    public String intern() {
        String self = (String) (Object) this;
        byte coder = self.coder();
        byte[] bytes = self.value();
        int hc = self.hashCode();
        Key key = new Key(bytes, coder, hc);
        for (;;) {
            Ref strRef = INTERNED.get(key);
            String existing = strRef.get();
            if (existing != null) {
                cleanQueue();
                return existing;
            }
            if (INTERNED.replace(key, strRef, new Ref(key, self, REF_QUEUE))) {
                cleanQueue();
                return self;
            }
        }
    }

    private void cleanQueue() {
        Ref item;
        while ((item = (Ref) REF_QUEUE.poll()) != null) {
            INTERNED.remove(item.key, item);
        }
    }

    static final class Ref extends WeakReference<String> {
        final Key key;

        Ref(final Key key, final String referent, final ReferenceQueue<? super String> q) {
            super(referent, q);
            this.key = key;
        }
    }

    static final class Key {
        private final byte[] bytes;
        private final byte coder;
        private final int hashCode;

        Key(final byte[] bytes, final byte coder, final int hashCode) {
            this.bytes = bytes;
            this.coder = coder;
            this.hashCode = hashCode;
        }

        public boolean equals(final Object other) {
            return other instanceof Key key && equals(key);
        }

        boolean equals(final Key other) {
            return this == other || other != null && hashCode == other.hashCode && coder == other.coder && Arrays.equals(bytes, other.bytes);
        }

        public int hashCode() {
            return hashCode;
        }
    }
}
