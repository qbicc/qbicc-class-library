package sun.nio.ch;

import java.nio.ByteBuffer;

import org.qbicc.runtime.patcher.PatchClass;

/**
 *
 */
@PatchClass(ByteBuffer.class)
abstract class ByteBuffer$_aliases {

    int offset;
}
