package java.util.zip;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.zlib.ZLib.*;

public class CRC32$_native {
    private static int update(int crc, int b) {
        Bytef bf = word(b);
        return crc32(word(crc), addr_of(bf), word(1)).intValue();
    }

    private static int updateBytes0(int crc, byte[] b, int off, int len) {
        return crc32(word(crc), addr_of(b[0]).plus(off).cast(), word(len)).intValue();
    }

    private static int updateByteBuffer0(int crc, long addr, int off, int len) {
        final ptr<@c_const Bytef> x = word(addr);
        return crc32(word(crc), x.plus(off), word(len)).intValue();
    }
}
