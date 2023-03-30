package jdk.internal.gc;

import static jdk.internal.sys.linux.Futex.futex_wait_bits;
import static jdk.internal.sys.posix.Errno.*;

import static jdk.internal.sys.posix.PThread.*;
import static jdk.internal.sys.posix.SysMman.*;
import static jdk.internal.sys.posix.Unistd.*;
import static jdk.internal.thread.ThreadNative.*;
import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.ExtModifier.*;
import static org.qbicc.runtime.stackwalk.CallSiteTable.*;
import static org.qbicc.runtime.stdc.Errno.*;
import static org.qbicc.runtime.stdc.Stddef.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Stdio.*;
import static org.qbicc.runtime.stdc.Stdlib.*;
import static org.qbicc.runtime.stdc.String.*;
import static org.qbicc.runtime.unwind.LibUnwind.*;

import jdk.internal.thread.ThreadNative;
import org.qbicc.runtime.AutoQueued;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.Inline;
import org.qbicc.runtime.InlineCondition;
import org.qbicc.runtime.NoSafePoint;
import org.qbicc.runtime.NoThrow;
import org.qbicc.runtime.gc.heap.Heap;
import org.qbicc.runtime.main.CompilerIntrinsics;
import org.qbicc.runtime.stackwalk.StackWalker;

public final class Gc {
    private Gc() {}

    static final ptr<struct_gc> gc;
    static long pageSize;

    static {
        // build time initialization
        final String gcAlgorithmName = getGcAlgorithmName();
        gc = switch (gcAlgorithmName) {
            case "semi" -> addr_of(SemiSpaceGc.gc);
            default -> throw new IllegalArgumentException("Unknown GC \"" + gcAlgorithmName + "\"");
        };
    }

    static native String getGcAlgorithmName();

    static final Thread gc_thread = new Thread(ThreadNative.getSystemThreadGroup(), "GC thread") {
        public void start() {
            if (Build.isHost()) {
                throw new IllegalStateException();
            }
            super.start();
        }

        @NoSafePoint
        public void run() {
            // never exit safepoint
            ptr<thread_native> threadNativePtr = currentThreadNativePtr();
            enterSafePoint(threadNativePtr, 0, 0);
            // now wait for a GC request
            for (;;) {
                // TODO: CAS status word atomically with await...?
                // assert isSafePoint();
                final ptr<uint32_t> statusPtr = addr_of(deref(threadNativePtr).state);
                int state = statusPtr.loadVolatile().intValue();
                while ((state & STATE_SAFEPOINT_REQUEST_GC) == 0) {
                    if (Build.Target.isLinux()) {
                        // use futex operations to await our desired bit pattern
                        if (! futex_wait_bits(statusPtr, word(STATE_SAFEPOINT_REQUEST_GC), word(STATE_SAFEPOINT_REQUEST_GC), zero()) && errno != EINTR.intValue()) {
                            // fatal error
                            abort();
                        }
                    } else if (Build.Target.isWasi()) {
                        // TODO
                        //  memory.atomic.wait32(statusPtr, state & mask | bits, -1)
                        abort();
                    } else if (Build.Target.isPosix()) {
                        // double-checked pattern, but using pthread_mutex
                        int res = pthread_mutex_lock(addr_of(deref(threadNativePtr).mutex)).intValue();
                        if (res != 0) {
                            // fatal error
                            abort();
                        }
                        state = statusPtr.loadVolatile().intValue();
                        while ((state & STATE_SAFEPOINT_REQUEST_GC) != STATE_SAFEPOINT_REQUEST_GC) {
                            res = pthread_cond_wait(addr_of(deref(threadNativePtr).inbound_cond), addr_of(deref(threadNativePtr).mutex)).intValue();
                            if (res != 0) {
                                // fatal error
                                abort();
                            }
                            state = statusPtr.loadVolatile().intValue();
                        }
                        res = pthread_mutex_unlock(addr_of(deref(threadNativePtr).mutex)).intValue();
                        if (res != 0) {
                            // fatal error
                            abort();
                        }
                        // done by interior loop
                        break;
                    }
                    state = statusPtr.loadVolatile().intValue();
                }
                addr_of(deref(threadNativePtr).state).getAndBitwiseAnd(word(~STATE_SAFEPOINT_REQUEST_GC));
                // a GC was requested; carry it out
                pthread_mutex_lock(addr_of(thread_list_mutex));
                // request safepoints of all threads
                for (ptr<thread_native> current = thread_list_terminus.next; current != addr_of(thread_list_terminus); current = deref(current).next) {
                    requestSafePoint(current, STATE_SAFEPOINT_REQUEST_GC);
                    current = deref(current).next;
                }
                // await safepoints of all threads
                for (ptr<thread_native> current = thread_list_terminus.next; current != addr_of(thread_list_terminus); current = deref(current).next) {
                    awaitSafePoint(current);
                    current = deref(current).next;
                }
                // now we are paused and free to manipulate the heap

                deref(deref(gc).collect).asInvokable().run();

                // release safepoint of all threads
                for (ptr<thread_native> current = thread_list_terminus.next; current != addr_of(thread_list_terminus); current = deref(current).next) {
                    releaseSafePoint(current, STATE_SAFEPOINT_REQUEST_GC);
                    current = deref(current).next;
                }

                // todo: reference queues
            }

        }
    };

    @NoThrow
    @NoSafePoint
    public static long getPageSize() {
        return pageSize;
    }

    public static void start() {
        gc_thread.start();
    }

    /**
     * A garbage collector definition.
     */
    @internal
    public static final class struct_gc extends struct {
        /**
         * A pointer to the name of the GC implementation.
         */
        public ptr<@c_const c_char> name;
        /**
         * A pointer to a function which initializes the image heap.
         */
        public ptr<function<Initializer>> initialize_heap;
        /**
         * A pointer to a function which performs a collection when a GC is requested.
         * This function is called under a safepoint from the GC thread.
         */
        public ptr<function<Runnable>> collect;
        /**
         * A pointer to a function which performs an allocation.
         * The allocated memory must be zeroed.
         */
        public ptr<function<Allocator>> allocate;
    }

    /**
     * Attributes which must be configured by the garbage collector.
     */
    @internal
    public static final class struct_gc_attr extends struct {
        /**
         * The lowest address of any allocatable object on the heap.
         */
        public ptr<c_char> lowest_heap_addr;
        /**
         * The highest address of any allocatable object on the heap.
         */
        public ptr<c_char> highest_heap_addr;
    }

    @FunctionalInterface
    public interface Initializer {
        void initialize(ptr<struct_gc_attr> attr_ptr);
    }

    @FunctionalInterface
    public interface Allocator {
        reference<?> allocate(long size);
    }

    /**
     * The special region for the root class set.
     * These objects are not copied.
     */
    static struct_region classes;
    /**
     * The special region for the initial heap object set.
     * The handling and placement of this region is dependent on the GC algorithm, which may copy its contents to other
     * regions or use it in place.
     */
    static struct_region initial;
    static struct_region strings;

    /**
     * The global marking bitmap.
     */
    static ptr<uintptr_t> bitmap;
    /**
     * The number of words in the global marking bitmap.
     */
    static size_t bitmapSize;

    /**
     * The lowest address of any object.
     */
    static ptr<uint8_t> heapBase;

    /**
     * The allocate method.
     * This method is used to implement {@code new} and other related operations.
     *
     * @param size the allocation size
     * @return a reference to the allocated memory
     */
    @Hidden
    @AutoQueued
    @NoSafePoint
    public static Object allocate(long size) {
        reference<?> allocated = deref(deref(gc).allocate).asInvokable().allocate(size);
        if (allocated == null) {
            // GC failure
            abort();
        }
        return allocated;
    }

    @Hidden
    @AutoQueued
    @NoSafePoint
    public static void clear(Object ptr, long size) {
        memset(refToPtr(ptr), word(0), word(size));
    }

    @Hidden
    @AutoQueued
    @NoSafePoint
    public static void copy(Object to, Object from, long size) {
        memcpy(refToPtr(to), refToPtr(from), word(size));
    }

    @export
    public static long getBitmapOffset(reference<?> ref) {
        if (ref == null) abort();
        ptr<uint8_t> refBytePtr = refToPtr(ref).cast();
        long byteOffset = refBytePtr.minus(heapBase).longValue();
        // the remaining computations are expected to be flattened to a single shift
        // each bit represents N bytes where N is the minimum object alignment
        int align = Heap.getConfiguredObjectAlignment();
        // there could be 32 or 64 bits per word
        int bitsPerWord = sizeof(uintptr_t.class).intValue() << 3;
        long shift = Integer.numberOfTrailingZeros(align) + Integer.numberOfTrailingZeros(bitsPerWord);
        // divide
        return byteOffset >>> shift;
    }

    @export
    public static uintptr_t getBitmapBit(reference<?> ref) {
        if (ref == null) abort();
        ptr<uint8_t> refBytePtr = refToPtr(ref).cast();
        long byteOffset = refBytePtr.minus(heapBase).longValue();
        int align = Heap.getConfiguredObjectAlignment();
        long shift = Integer.numberOfTrailingZeros(align);
        int bitsPerWord = sizeof(uintptr_t.class).intValue() << 3;
        return word(1L << (byteOffset >> shift & bitsPerWord - 1));
    }

    @NoSafePoint
    @NoThrow
    @Hidden
    public static void clearBitmap() {
        if (Build.Target.isPosix()) {
            ptr<?> res = mmap(bitmap, bitmapSize, word(PROT_READ.intValue() | PROT_WRITE.intValue()), word(MAP_FIXED.intValue() | MAP_PRIVATE.intValue() | MAP_ANON.intValue()), word(- 1), zero());
            if (res == MAP_FAILED) {
                // should generally be impossible, because we have the memory already
                abort();
            }
        }
    }

    // intrinsics which help with setting up the initial regions and memory areas.

    static native ptr<?> getClassRegionStart();
    static native long getClassRegionSize();
    static native ptr<?> getInitialHeapRegionStart();
    static native long getInitialHeapRegionSize();
    static native ptr<?> getInitialHeapStringsRegionStart();
    static native long getInitialHeapStringsRegionSize();

    static native ptr<reference<?>> getReferenceTypedVariablesStart();
    static native int getReferenceTypedVariablesCount();

    /**
     * Initialize the heap at program start.
     */
    @export
    public static void qbicc_initialize_heap() {
        if (Build.Target.isPosix()) {
            int pageSize = sysconf(_SC_PAGE_SIZE).intValue();
            if (pageSize == -1) {
                abort();
            }
            if (Integer.bitCount(pageSize) != 1) {
                // not a power of 2? but not likely enough to be worth spending memory on an error message
                abort();
            }
            Gc.pageSize = pageSize;
        } else {
            abort();
        }

        struct_gc_attr gc_attr = auto(zero());

        // initialize our built-in heap regions
        classes.start = getClassRegionStart();
        classes.limit = classes.position = getClassRegionSize();

        initial.start = getInitialHeapRegionStart();
        initial.limit = initial.position = getInitialHeapRegionSize();

        strings.start = getInitialHeapStringsRegionStart();
        strings.limit = strings.position = getInitialHeapStringsRegionSize();

        // now, call GC-specific heap init routine
        deref(deref(gc).initialize_heap).asInvokable().initialize(addr_of(gc_attr));

        // find the lowest possible heap address
        ptr<c_char> lowest = gc_attr.lowest_heap_addr;
        if (classes.start.isLt(lowest)) {
            lowest = classes.start.cast();
        }
        if (initial.start.isLt(lowest)) {
            lowest = initial.start.cast();
        }
        if (strings.start.isLt(lowest)) {
            lowest = strings.start.cast();
        }
        heapBase = lowest.cast();
        // find the highest possible heap address
        ptr<c_char> highest = gc_attr.highest_heap_addr;
        if (classes.start.plus(classes.limit).isGt(highest)) {
            highest = classes.start.plus(classes.limit).cast();
        }
        if (initial.start.plus(initial.limit).isGt(highest)) {
            highest = initial.start.plus(initial.limit).cast();
        }
        if (strings.start.plus(strings.limit).isGt(highest)) {
            highest = strings.start.plus(strings.limit).cast();
        }
        bitmapSize = word(highest.minus(lowest).longValue() / 8);

        // this mapping could be fairly large; but, we only use pages corresponding to places where objects may reside
        bitmap = mmap(zero(), bitmapSize, word(PROT_READ.intValue() | PROT_WRITE.intValue()), word(MAP_PRIVATE.intValue() | MAP_ANON.intValue()), word(- 1), zero());
        if (bitmap == MAP_FAILED) {
            // not able to allocate memory
            // todo: set errno, fail gracefully...
            abort();
        }
    }

    /**
     * Initialize an iterator for walking the reference values reachable from the given object.
     *
     * @param iter the iterator pointer, which typically should be stack-allocated (must not be {@code null})
     * @param obj the object reference to iterate
     */
    @NoSafePoint
    @NoThrow
    @Hidden
    @export(withScope = ExportScope.LOCAL)
    public static void clv_iterator_init(ptr<clv_iterator> iter, Object obj) {
        if (obj == null) {
            deref(iter).base = zero();
            deref(iter).state = 0;
            deref(iter).next = zero();
        } else {
            deref(iter).base = refToPtr(obj).cast();
            // determine if the class has an extended bitmap
            ClassAccess ca = cast(CompilerIntrinsics.getClassFromTypeIdSimple(((ObjectAccess)obj).typeId));
            if ((ca.modifiers & I_ACC_EXTENDED_BITMAP) != 0) {
                // extended bitmap!
                ptr<uint64_t> bmp = word(ca.referenceBitMap);
                deref(iter).state = bmp.loadUnshared().longValue();
                deref(iter).next = bmp.plus(1);
            } else {
                // normal bitmap!
                deref(iter).state = ca.referenceBitMap;
                deref(iter).next = zero();
            }
        }
    }

    /**
     * Iterate to the next reference which is contained by the object referenced in the iterator (if any).
     *
     * @param iter the iterator pointer, which typically should be stack-allocated (must not be {@code null})
     * @return the next reference, or {@code null} if there are no more references reachable from this object
     */
    @NoSafePoint
    @NoThrow
    @Hidden
    @export(withScope = ExportScope.LOCAL)
    public static reference<?> clv_iterator_next(ptr<clv_iterator> iter) {
        long bits = deref(iter).state;
        do {
            while (bits != 0) {
                final long lob = Long.lowestOneBit(bits);
                bits &= ~lob;
                int offset = Long.numberOfTrailingZeros(lob);
                final ptr<reference<?>> refLoc = deref(iter).base.plus(offset);
                reference<?> ref = refLoc.loadUnshared();
                if (ref != null) {
                    deref(iter).state = bits;
                    deref(iter).current = refLoc;
                    return ref;
                }
            }
            ptr<uint64_t> next = deref(iter).next;
            if (next.isNonNull()) {
                bits = next.loadUnshared().longValue();
                if (bits != 0) {
                    deref(iter).next = next.plus(1);
                }
            }
        } while (bits != 0);
        // done
        deref(iter).state = 0;
        deref(iter).next = zero();
        deref(iter).current = zero();
        return null;
    }

    /**
     * Change the value of the reference location corresponding to the reference most recently returned by
     * {@link #clv_iterator_next(ptr)}.
     *
     * @param iter the iterator pointer, which typically should be stack-allocated (must not be {@code null})
     * @param newVal the updated reference value
     */
    @NoSafePoint
    @NoThrow
    @Hidden
    @export(withScope = ExportScope.LOCAL)
    public static void clv_iterator_set(ptr<clv_iterator> iter, reference<?> newVal) {
        deref(iter).current.storePlain(newVal);
    }

    /**
     * Implementation-specific operation to mark an object.
     *
     * @param ref the reference to the object to mark (must not be {@code null})
     * @return {@code true} if the ref was newly marked as a result of this operation, or {@code false}
     *      if it was already marked or is permanent
     */
    @NoSafePoint
    @NoThrow
    @Hidden
    static boolean setMark(reference<?> ref) {
        long off = getBitmapOffset(ref);
        uintptr_t bitVal = getBitmapBit(ref);
        uintptr_t observed = bitmap.plus(off).getAndBitwiseOrOpaque(bitVal);
        return (observed.longValue() & bitVal.longValue()) == 0;
    }

    @NoSafePoint
    @NoThrow
    @Hidden
    static boolean isMarked(reference<?> ref) {
        long off = getBitmapOffset(ref);
        uintptr_t bitVal = getBitmapBit(ref);
        uintptr_t observed = bitmap.plus(off).loadOpaque();
        return (observed.longValue() & bitVal.longValue()) != 0;
    }

    @NoSafePoint
    @NoThrow
    @Hidden
    static void setHeaderMovedBit(reference<?> ref) {
        final ptr<header_type> headerPtr = addr_of(deref(refToPtr((ObjectAccess)ref.toObject())).header);
        headerPtr.getAndBitwiseOrOpaque(headerMovedBit());
    }

    @NoSafePoint
    @NoThrow
    @Hidden
    static boolean getHeaderMovedBit(reference<?> ref) {
        final ptr<header_type> headerPtr = addr_of(deref(refToPtr((ObjectAccess)ref.toObject())).header);
        return (headerPtr.loadUnshared().longValue() & headerMovedBit().longValue()) != 0;
    }

    /**
     * Intrinsic which returns the header "object moved" bit as the sole set bit of the returned word.
     * If the header has no such bit, then the returned value is a zero constant.
     *
     * @return the "object moved" bit, or zero if there is no "object moved" bit
     */
    @NoSafePoint
    @NoThrow
    static native header_type headerMovedBit();

    /**
     * Intrinsic which returns the header "stack allocated" bit as the sole set bit of the returned word.
     * If the header has no such bit, then the returned value is a zero constant.
     *
     * @return the "stack allocated" bit, or zero if there is no "stack allocated" bit
     */
    @NoSafePoint
    @NoThrow
    static native header_type headerStackAllocatedBit();

    @NoSafePoint
    @NoThrow
    @Hidden
    static boolean isStackAllocated(reference<?> ref) {
        final ptr<header_type> headerPtr = addr_of(deref(refToPtr((ObjectAccess)ref.toObject())).header);
        return (headerPtr.loadUnshared().longValue() & headerStackAllocatedBit().longValue()) != 0;
    }

    /**
     * A basic recursive mark routine for objects.
     *
     * @param object the object to mark (must not be {@code null})
     */
    @NoSafePoint
    @NoThrow
    @export
    static void mark(Object object) {
        if (! setMark(reference.of(object))) {
            return;
        }
        markBasic0(reference.of(object));
    }

    /**
     * Mark all the objects which are currently reachable by the stack of this thread.
     */
    @NoSafePoint
    @NoThrow
    @export
    static void markCurrentStack() {
        StackWalker sw = new StackWalker();
        lvi_iterator lviIter = auto();
        unw_cursor_t cursor = auto();
        ptr<struct_call_site> call_site_ptr;
        reference<?> ref;
        while (sw.next()) {
            call_site_ptr = sw.getCallSite();
            if (call_site_ptr != null) {
                lvi_iterator_init(addr_of(lviIter), call_site_ptr);
                sw.getCursor(addr_of(cursor));
                while ((ref = lvi_iterator_next(addr_of(lviIter), addr_of(cursor))) != null) {
                    if (setMark(ref)) {
                        markBasic0(ref);
                    }
                }
            }
        }
    }

    /**
     * Mark all the objects which are currently reachable by the stack of the given safepointed thread.
     *
     * @param threadNativePtr the safepointed thread native pointer (must not be {@code null})
     */
    @NoSafePoint
    @NoThrow
    @export
    static void markStack(ptr<thread_native> threadNativePtr) {
        StackWalker sw = new StackWalker(addr_of(deref(threadNativePtr).saved_context));
        lvi_iterator lviIter = auto();
        unw_cursor_t cursor = auto();
        ptr<struct_call_site> call_site_ptr;
        reference<?> ref;
        while (sw.next()) {
            call_site_ptr = sw.getCallSite();
            if (call_site_ptr != null) {
                lvi_iterator_init(addr_of(lviIter), call_site_ptr);
                sw.getCursor(addr_of(cursor));
                while ((ref = lvi_iterator_next(addr_of(lviIter), addr_of(cursor))) != null) {
                    if (setMark(ref)) {
                        markBasic0(ref);
                    }
                }
            }
        }
    }

    /**
     * Mark all the allocates objects within the given region.
     *
     * @param regionPtr the region pointer (must not be {@code null})
     */
    @NoSafePoint
    @NoThrow
    static void markRegion(ptr<struct_region> regionPtr) {
        struct_region_iter iter = auto();
        region_iter_init(addr_of(iter), regionPtr);
        for (ptr<?> next = region_iter_next(addr_of(iter)); next != null; next = region_iter_next(addr_of(iter))) {
            mark(ptrToRef(next));
        }
    }

    @NoSafePoint
    @NoThrow
    @export
    private static void markBasic0(final reference<?> ref) {
        clv_iterator iter = auto();
        clv_iterator_init(addr_of(iter), ref.toObject());
        reference<?> cur = clv_iterator_next(addr_of(iter));
        reference<?> next;
        while (cur != null) {
            next = clv_iterator_next(addr_of(iter));
            if (setMark(cur)) {
                if (next == null) {
                    // we have nothing else to mark, so mark `cur` now
                    clv_iterator_init(addr_of(iter), cur.toObject());
                    next = clv_iterator_next(addr_of(iter));
                } else {
                    markBasic0(cur);
                }
            }
            cur = next;
        }
        // special case: arrays
        // todo: eventually generalize this using a second bitmap, to support future arrays of user primitive types
        if (ref.toObject() instanceof Object[] arr) {
            for (Object o : arr) {
                if (o != null && setMark(reference.of(o))) {
                    markBasic0(reference.of(o));
                }
            }
        }
    }

    /**
     * An iterator for a class live value bitmap.
     */
    @internal
    public static final class clv_iterator extends struct {
        ptr<reference<?>> base;
        long state;
        ptr<uint64_t> next;
        ptr<reference<?>> current;
    }

    /**
     * A linear region of memory that can be allocated from.
     */
    @internal
    public static final class struct_region extends struct {
        /**
         * The start of the memory region.
         */
        public ptr<?> start;
        /**
         * The position of the next allocation (aligned to object alignment).
         * The unit is bytes. TODO: maybe make it object-alignment increments instead?
         */
        public long position;
        /**
         * The size of this memory region; the region is full when {@code position == limit}.
         * The unit is bytes. TODO: maybe make it object-alignment increments instead?
         */
        public long limit;
    }

    /**
     * Determine whether the given pointer falls within the given region.
     *
     * @param region_ptr the region pointer (must not be {@code null})
     * @param ptr the pointer to test
     * @return {@code true} if the pointer falls within the region, or {@code false} if it does not
     */
    @NoSafePoint
    @NoThrow
    @export
    public static boolean region_contains(ptr<struct_region> region_ptr, ptr<?> ptr) {
        final long diff = ptr.minus(deref(region_ptr).start.cast()).longValue();
        return diff >= 0 && diff < deref(region_ptr).limit;
    }

    /**
     * Establish a heap region at the given pre-allocated start address.
     *
     * @param region_ptr the region pointer (must not be {@code null})
     * @param start the region start
     * @param limit the region size
     * @return {@code true} if the region was established, or {@code false} if there was an error
     * ({@code errno} will be set accordingly)
     */
    @NoSafePoint
    @NoThrow
    @export
    public static boolean region_init(ptr<struct_region> region_ptr, ptr<?> start, long limit) {
        if (limit == 0 || (limit & Gc.getPageSize() - 1) != 0 || region_ptr.isNull() || start.isNull()) {
            errno = EINVAL.intValue();
            return false;
        }
        // clear the structure
        region_ptr.storeUnshared(zero());
        // establish the start and limit
        deref(region_ptr).start = start;
        deref(region_ptr).limit = limit;
        return true;
    }

    /**
     * Allocate from this region if possible.
     *
     * @param region_ptr the region to allocate from (must not be {@code null})
     * @param size the number of bytes to allocate
     * @return a pointer to the allocated item, or {@code null} if allocation did not succeed
     */
    @NoSafePoint
    @NoThrow
    @export
    public static <P extends ptr<?>> P region_allocate(ptr<struct_region> region_ptr, long size) {
        final long limit = deref(region_ptr).limit;
        // round up the size so the next allocation is aligned
        final int mask = Heap.getConfiguredObjectAlignment() - 1;
        size = (size + mask) & ~mask;
        if (size > limit) {
            // fail fast; it can never fit
            return null;
        }
        ptr<int64_t> position_ptr = addr_of(deref(region_ptr).position);
        long oldVal, newVal;
        long witness;
        oldVal = position_ptr.loadSingleAcquire().longValue();
        for (;;) {
            newVal = oldVal + size;
            if (newVal > limit) {
                return null;
            }
            witness = position_ptr.compareAndSwapRelease(word(oldVal), word(newVal)).longValue();
            if (oldVal == witness) {
                // success; return the pointer
                return deref(region_ptr).start.plus(oldVal).cast();
            }
            oldVal = witness;
        }
    }

    /**
     * Reset the region so that it can be allocated from again (typically after it is cleared by garbage collection).
     *
     * @param region_ptr the region pointer (must not be {@code null})
     */
    @NoSafePoint
    @NoThrow
    @export
    public static void region_reset(ptr<struct_region> region_ptr) {
        addr_of(deref(region_ptr).position).storeRelease(zero());
    }

    /**
     * An object iterator for a region.
     */
    @internal
    public static final class struct_region_iter extends struct {
        public ptr<struct_region> region_ptr;
        public long current_size;
        public long position;
    }

    /**
     * The structure of a relocated object.
     */
    @internal
    public static final class struct_relocated extends struct {
        public header_type header;
        public reference<?> relocation;
    }

    /**
     * Initialize an object iterator for a region.
     *
     * @param iter_ptr the iterator pointer, typically stack-allocated (must not be {@code null})
     * @param region_ptr the region pointer (must not be {@code null})
     */
    @export
    public static void region_iter_init(ptr<struct_region_iter> iter_ptr, ptr<struct_region> region_ptr) {
        deref(iter_ptr).region_ptr = region_ptr;
        deref(iter_ptr).position = 0;
    }

    /**
     * Get a pointer to the next object in the region.
     *
     * @param iter_ptr the iterator pointer, typically stack-allocated (must not be {@code null})
     * @return the pointer to the next object in the region, or {@code null} if there are no more objects in the region
     * @param <P> the pointer type to return
     */
    @export
    public static <P extends ptr<?>> P region_iter_next(ptr<struct_region_iter> iter_ptr) {
        final long endOfObjects = deref(deref(iter_ptr).region_ptr).position;
        if (deref(iter_ptr).position >= endOfObjects) {
            return null;
        }
        // save the pointer
        ptr<?> next = deref(deref(iter_ptr).region_ptr).start.plus(deref(iter_ptr).position);
        long size = deref(iter_ptr).current_size = instance_size(ptrToRef(next));
        final int mask = Heap.getConfiguredObjectAlignment() - 1;
        size = (size + mask) & ~mask;
        deref(iter_ptr).position += size;
        return next.cast();
    }

    /**
     * Get the size of the last object returned by {@link #region_iter_next(ptr)}.
     *
     * @param iter_ptr the iterator pointer (must not be {@code null})
     * @return the size of the last object returned
     */
    @NoSafePoint
    @NoThrow
    @export
    @Inline(InlineCondition.ALWAYS)
    public static long region_iter_size(ptr<struct_region_iter> iter_ptr) {
        return deref(iter_ptr).current_size;
    }

    /**
     * Get the size in bytes of the object, not including trailing padding required for alignment.
     *
     * @param obj the object to test (must not be {@code null})
     * @return the size in bytes
     */
    @NoSafePoint
    @NoThrow
    @export
    public static long instance_size(Object obj) {
        ObjectAccess oa = cast(obj);
        type_id baseId = oa.typeId;
        ClassAccess ca = cast(CompilerIntrinsics.getClassFromTypeIdSimple(baseId));
        long baseSize = ca.instanceSize;
        // todo: use a modifier flag to identify arrays
        long elemSize;
        if (CompilerIntrinsics.isPrimArray(baseId)) {
            // get element size
            ClassAccess ct = ca.componentType;
            elemSize = ct.instanceSize;
        } else if (CompilerIntrinsics.isReferenceArray(baseId)) {
            elemSize = sizeof(reference.class).longValue();
        } else {
            // done
            return baseSize;
        }
        // get array length
        ArrayAccess aa = cast(obj);
        int length = aa.length;
        return baseSize + length * elemSize;
    }

    /**
     * Move an object to the destination region and set the relocation pointer on the original object.
     *
     * @param original the original object reference (must not be {@code null})
     * @param destination the region to move the object to (must not be {@code null}, must have sufficient space)
     */
    @NoSafePoint
    @NoThrow
    @export
    public static void move_object(reference<?> original, ptr<struct_region> destination) {
        final ptr<Object> oldPtr = refToPtr(original.toObject());
        // we must move it; allocate space for the new object
        final long size = instance_size(original.toObject());
        // todo: allocate extra size for lock, hashCode, etc. if needed, resulting in newSize
        // todo: inflating objects could theoretically cause the "to" space to be bigger than the "from" space!
        final ptr<?> newPtr = region_allocate(destination, /*newSize*/size);
        if (newPtr == null) {
            // no free space is a fatal error! there is nothing else we can do
            fprintf(stderr, utf8z("Failed to allocate during GC move\n"));
            abort();
        }
        // move the object to its new home
        // todo: update header bits as needed for new information like lock, hashCode
        memcpy(newPtr, oldPtr, word(/*newSize*/size));
        // set the moved-to location
        setHeaderMovedBit(original);
        deref(oldPtr, struct_relocated.class).relocation = reference.of(ptrToRef(newPtr));
    }

    /**
     * Update a single reference if the target has been relocated.
     */
    @NoSafePoint
    @NoThrow
    @export
    public static void update_reference(ptr<reference<?>> ref_ptr) {
        reference<?> ref = ref_ptr.loadPlain();
        if (ref != null && getHeaderMovedBit(ref)) {
            ref_ptr.storePlain(deref(refToPtr(ref.toObject()), struct_relocated.class).relocation);
        }
    }

    /**
     * Update all the reference-typed fields of all the objects within a region with relocated references
     * if their target object has been relocated.
     */
    @NoSafePoint
    @NoThrow
    @export
    public static void update_region(ptr<struct_region> region_ptr) {
        clv_iterator iter = auto();
        struct_region_iter region_iter = auto();
        region_iter_init(addr_of(region_iter), region_ptr);
        ptr<?> objPtr;
        while ((objPtr = region_iter_next(addr_of(region_iter))) != null) {
            clv_iterator_init(addr_of(iter), ptrToRef(objPtr));
            reference<?> ref;
            while ((ref = clv_iterator_next(addr_of(iter))) != null) {
                if (getHeaderMovedBit(ref)) {
                    clv_iterator_set(addr_of(iter), deref(refToPtr(ref.toObject()), struct_relocated.class).relocation);
                }
            }
        }
    }

    /**
     * Update all the reference-typed fields of all the objects on this thread's stack with relocated references
     * if their target object has been relocated.
     */
    @NoSafePoint
    @NoThrow
    public static void update_stack(ptr<thread_native> threadNativePtr) {
        StackWalker sw = new StackWalker(addr_of(deref(threadNativePtr).saved_context));
        lvi_iterator lvi_iter = auto();
        unw_cursor_t cursor = auto();
        ptr<struct_call_site> call_site_ptr;
        reference<?> ref;
        while (sw.next()) {
            call_site_ptr = sw.getCallSite();
            if (call_site_ptr != null) {
                lvi_iterator_init(addr_of(lvi_iter), call_site_ptr);
                sw.getCursor(addr_of(cursor));
                while ((ref = lvi_iterator_next(addr_of(lvi_iter), addr_of(cursor))) != null) {
                    if (getHeaderMovedBit(ref)) {
                        lvi_iterator_set(addr_of(lvi_iter), addr_of(cursor), deref(refToPtr(ref.toObject()), struct_relocated.class).relocation);
                    } else if (isStackAllocated(ref)) {
                        update_stack_allocated(ref);
                    }
                }
            }
        }
    }

    @NoSafePoint
    @NoThrow
    private static void update_stack_allocated(reference<?> ref) {
        clv_iterator clv_iter = auto();
        // iterate the fields of the object and update them
        clv_iterator_init(addr_of(clv_iter), ref);
        while ((ref = clv_iterator_next(addr_of(clv_iter))) != null) {
            if (getHeaderMovedBit(ref)) {
                clv_iterator_set(addr_of(clv_iter), deref(refToPtr(ref.toObject()), struct_relocated.class).relocation);
            } else if (isStackAllocated(ref)) {
                // TODO: cyclic refs are a problem; visited bit
                update_stack_allocated(ref);
            }
        }
    }
}
