package java.io;

/**
 *
 */
class UnixFileSystem$_native {
    /**
     * Canonicalize the given path.  Removes all {@code .} and {@code ..} segments from the path.
     *
     * @param path the relative or absolute possibly non-canonical path
     * @return the canonical path
     */
    private String canonicalize0(String path) {
        final int length = path.length();
        // 0 - start
        // 1 - got one .
        // 2 - got two .
        // 3 - got /
        int state = 0;
        if (length == 0) {
            return path;
        }
        final char[] targetBuf = new char[length];
        // string segment end exclusive
        int e = length;
        // string cursor position
        int i = length;
        // buffer cursor position
        int a = length - 1;
        // number of segments to skip
        int skip = 0;
        loop:
        while (--i >= 0) {
            char c = path.charAt(i);
            outer:
            switch (c) {
                case '/': {
                    inner:
                    switch (state) {
                        case 0:
                        case 1:
                            state = 3;
                            e = i;
                            break outer;
                        case 2:
                            state = 3;
                            e = i;
                            skip++;
                            break outer;
                        case 3:
                            e = i;
                            break outer;
                        default:
                            throw new IllegalStateException();
                    }
                    // not reached!
                }
                case '.': {
                    inner:
                    switch (state) {
                        case 0:
                        case 3:
                            state = 1;
                            break outer;
                        case 1:
                            state = 2;
                            break outer;
                        case 2:
                            break inner; // emit!
                        default:
                            throw new IllegalStateException();
                    }
                    // fall thru
                }
                default: {
                    final int newE = e > 0 ? path.lastIndexOf('/', e - 1) : -1;
                    final int segmentLength = e - newE - 1;
                    if (skip > 0) {
                        skip--;
                    } else {
                        if (state == 3) {
                            targetBuf[a--] = '/';
                        }
                        path.getChars(newE + 1, e, targetBuf, (a -= segmentLength) + 1);
                    }
                    state = 0;
                    i = newE + 1;
                    e = newE;
                    break;
                }
            }
        }
        if (state == 3) {
            targetBuf[a--] = '/';
        }
        return new String(targetBuf, a + 1, length - a - 1);
    }

}
