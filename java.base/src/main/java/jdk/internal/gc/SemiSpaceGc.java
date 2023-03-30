package jdk.internal.gc;

import static jdk.internal.gc.Gc.*;
import static jdk.internal.sys.posix.SysMman.*;
import static jdk.internal.thread.ThreadNative.*;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdlib.*;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.NoSafePoint;
import org.qbicc.runtime.NoThrow;
import org.qbicc.runtime.gc.heap.Heap;

/**
 * A basic garbage collector which moves live objects between two equally-sized spaces.
 */
public final class SemiSpaceGc {

    @export(withScope = ExportScope.LOCAL)
    private static void collect() {
        // mark threads and thread stacks; all objects are in "from" space
        for (ptr<thread_native> current = thread_list_terminus.next; current != addr_of(thread_list_terminus); current = deref(current).next) {
            //todo: we could come up with a way for each thread to mark its own stack before entering safepoint
            //todo: threads must release their TLABs
            markStack(current);
            mark(deref(current).ref);
        }

        // mark all permanent objects
        markRegion(addr_of(classes));
        // todo: these will eventually be on the regular heap
        markRegion(addr_of(strings));
        markRegion(addr_of(initial));

        // mark all refs
        for (int i = 0; i < getReferenceTypedVariablesCount(); i ++) {
            ptr<reference<?>> refPtr = getReferenceTypedVariablesStart().plus(i);
            Object obj = refPtr.loadPlain();
            if (obj != null) {
                mark(obj);
            }
        }

        // move live objects to _to_ space
        relocate_objects();

        // update all objects in _to_ space
        update_region(addr_of(to));

        // update all permanent objects
        update_region(addr_of(classes));
     // update_region(addr_of(strings)); // this region only contains interior pointers!
        update_region(addr_of(initial));

        for (int i = 0; i < getReferenceTypedVariablesCount(); i ++) {
            update_reference(getReferenceTypedVariablesStart().plus(i));
        }

        // update threads and thread stacks
        for (ptr<thread_native> current = thread_list_terminus.next; current != addr_of(thread_list_terminus); current = deref(current).next) {
            //todo: we could come up with a way for each thread to update its own stack before leaving safepoint
            update_reference(addr_of(deref(current).ref));
            update_stack(current);
        }

        // swap spaces
        swap();

        clearBitmap();
    }

    static {
        gc_thread.setDaemon(true);
    }

    /**
     * The space where objects are being allocated to.
     */
    private static struct_region from;
    /**
     * The space where objects will move to during GC.
     */
    private static struct_region to;

    public static final struct_gc gc = zero();

    static {
        // set up the structure during build time
        gc.name = utf8z("semi");
        gc.initialize_heap = addr_of(function.of(SemiSpaceGc::initialize_heap));
        gc.collect = addr_of(function.of(SemiSpaceGc::collect));
        gc.allocate = addr_of(function.of(SemiSpaceGc::allocate));
    }

    @export
    private static void initialize_heap(ptr<struct_gc_attr> attr_ptr) {
        // todo: clean this part up
        ptr<?> start;
        long heapSize = Heap.getConfiguredMaxHeapSize();
        // we need to round heap size down to a page * 2 boundary
        long pageSize = Gc.getPageSize();
        heapSize &= ~((pageSize << 1) - 1);
        if (! Build.Target.isWasm()) {
            start = mmap(zero(), word(heapSize), word(PROT_READ.longValue() | PROT_WRITE.longValue()), word(MAP_PRIVATE.longValue() | MAP_ANON.longValue()), word(-1), zero()).cast();
        } else {
            // TODO: WASI load region
            start = zero();
        }
        if (start == MAP_FAILED) {
            // todo: fail gracefully (errno)
            abort();
        }
        deref(attr_ptr).lowest_heap_addr = start.cast();
        deref(attr_ptr).highest_heap_addr = start.plus(heapSize).cast();
        // since heapSize is a page * 2 boundary, semiSize is on a page boundary
        long semiSize = heapSize >>> 1;
        if (! region_init(addr_of(from), start, semiSize) || ! region_init(addr_of(to), start.plus(semiSize), semiSize)) {
            // heap failure
            abort();
        }
        // other regions are initialized by common code
    }

    @NoSafePoint
    @NoThrow
    private static void swap() {
        // todo: swap(addr_of(from), addr_of(to))
        @SuppressWarnings("UnusedAssignment")
        struct_region tmp = auto();
        tmp = to;
        to = from;
        from = tmp;
        region_reset(addr_of(to));
        if (! Build.Target.isWasm()) {
            // give memory back to OS while clearing
            final ptr<?> res = mmap(to.start.cast(), word(to.limit), word(PROT_READ.longValue() | PROT_WRITE.longValue()), word(MAP_FIXED.longValue() | MAP_PRIVATE.longValue() | MAP_ANON.longValue()), zero(), zero());
            if (res == MAP_FAILED || res != to.start) {
                abort();
            }
        }
    }

    @export
    private static reference<?> allocate(long size) {
        ptr<?> ptr = region_allocate(addr_of(from), size);
        if (ptr == null) {
            // will it *ever* fit?
            if (size > from.limit) {
                // no; forbid it for this simple collector
                throw Heap.OOME;
            }
            // we need to trigger a collection manually
            final ptr<thread_native> gcThread = getThreadNativePtr(Gc.gc_thread);
            enterSafePoint(currentThreadNativePtr(), STATE_SAFEPOINT_REQUEST_GC | STATE_SAFEPOINT_REQUEST, 0);
            // signal to the GC thread that we want a GC
            requestSafePoint(gcThread, STATE_SAFEPOINT_REQUEST_GC);
            // now wait for it to finish
            exitSafePoint(currentThreadNativePtr(), 0, 0);
            // retry the allocation
            ptr = region_allocate(addr_of(from), size);
            // now if it's null, we've done all we can
            if (ptr == null) {
                throw Heap.OOME;
            }
        }
        return reference.of(ptrToRef(ptr));
    }

    @export
    private static void relocate_objects() {
        // move objects from "from" to "to" (we haven't swapped them yet)
        struct_region_iter sri = auto();
        region_iter_init(addr_of(sri), addr_of(from));

        ptr<?> ptr;
        while ((ptr = region_iter_next(addr_of(sri))) != null) {
            final reference<?> ref = reference.of(ptrToRef(ptr));
            if (isMarked(ref)) {
                move_object(ref, addr_of(to));
            }
        }
    }
}
