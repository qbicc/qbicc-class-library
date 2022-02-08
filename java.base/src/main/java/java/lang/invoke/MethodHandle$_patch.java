package java.lang.invoke;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdint.*;

import org.qbicc.runtime.patcher.Add;
import org.qbicc.runtime.patcher.PatchClass;

/**
 * Patches for method handle objects.
 */
@PatchClass(MethodHandle.class)
abstract class MethodHandle$_patch {

    // alias
    public native MethodType type();

    /**
     * Check an exact call site type at run time.  If this method returns, then the proposed call to
     * {@link #dispatchExact(ptr, ptr)} with the corresponding call structure and return type is safe.
     *
     * @param rType the return type that will be given
     * @param pTypes the parameter types that will be given
     * @throws WrongMethodTypeException if the types do not match exactly
     */
    @Add
    final void checkType(Class<?> rType, Class<?>[] pTypes) {
        MethodType type = type();
        outer: if (type.returnType() == rType) {
            // OK so far
            int length = pTypes.length;
            if (length == type.parameterCount()) {
                // still OK, now check the parameters
                for (int i = 0; i < length; i ++) {
                    if (pTypes[i] != type.parameterType(i)) {
                        break outer;
                    }
                }
                // all OK
                return;
            }
        }
        throw new WrongMethodTypeException();
    }

    /**
     * Dispatch the call to this method handle.  Call sites which call {@link MethodHandle#invoke} or
     * {@link MethodHandle#invokeWithArguments} will dispatch to this method.
     *
     * <p>The implementation of this method will use the {@code #getAndConvert*Argument(Class, ptr)}
     * methods to convert the call arguments.
     *
     * @param returnType the method handle target return type
     * @param retPtr the pointer to the location where the return value should be stored, or {@code null} to discard it
     * @param argsCnt the argument count
     * @param argsPtr the base pointer for the passed-in arguments
     * @param argTypesPtr a pointer to an array of {@code Class} references representing the argument types
     */
    @Add
    abstract void dispatch(Class<?> returnType, ptr<?> retPtr, int argsCnt, ptr<@c_const ptr<@c_const ?>> argsPtr, ptr<@c_const Class<?>> argTypesPtr);

    /**
     * Dispatch the call to this method handle.  Call sites which call {@link MethodHandle#invokeExact)} will dispatch
     * to this method.
     *
     * @param retPtr the pointer to the location where the correctly-typed return value should be stored, or {@code null} to discard it
     * @param argsPtr the pointer to the correctly-typed arguments structure for this call, or {@code null} if there are no arguments
     */
    @Add
    abstract void dispatchExact(ptr<?> retPtr, ptr<@c_const ?> argsPtr);




    // These methods are used by implementations to convert the arguments and return types as needed.
    //
    // There is one method for each requested return type (T0 in the MethodHandle#asType docs).
    // The argument type and pointer refer to the input type (T1 in the docs).
    // Each argument is passed through the given argument method to assemble the final call site.

    /**
     * Get an argument of type {@code byte}. If the argument type is not {@code byte.class}, a cast exception is thrown.
     *
     * @param argType the argument type
     * @param argPtr the argument pointer
     * @return the byte value
     */
    @Add
    static byte getByteArgument(Class<?> argType, ptr<@c_const ?> argPtr) {
        if (argType == byte.class) {
            return argPtr.loadPlain(int8_t.class).byteValue();
        } else {
            throw new WrongMethodTypeException();
        }
    }

    /**
     * Get an argument of type {@code short}. If the argument type is not {@code short.class}, a cast exception is thrown.
     *
     * @param argType the argument type
     * @param argPtr the argument pointer
     * @return the short value
     */
    @Add
    static short getShortArgument(Class<?> argType, ptr<@c_const ?> argPtr) {
        if (argType == short.class) {
            return argPtr.loadPlain(int16_t.class).shortValue();
        } else {
            throw new WrongMethodTypeException();
        }
    }

    /**
     * Get an argument of type {@code int}. If the argument type is not {@code int.class}, a cast exception is thrown.
     *
     * @param argType the argument type
     * @param argPtr the argument pointer
     * @return the int value
     */
    @Add
    static int getIntArgument(Class<?> argType, ptr<@c_const ?> argPtr) {
        if (argType == int.class) {
            return argPtr.loadPlain(int32_t.class).intValue();
        } else {
            throw new WrongMethodTypeException();
        }
    }

    /**
     * Get an argument of type {@code long}. If the argument type is not {@code long.class}, a cast exception is thrown.
     *
     * @param argType the argument type
     * @param argPtr the argument pointer
     * @return the long value
     */
    @Add
    static long getLongArgument(Class<?> argType, ptr<@c_const ?> argPtr) {
        if (argType == long.class) {
            return argPtr.loadPlain(int64_t.class).longValue();
        } else {
            throw new WrongMethodTypeException();
        }
    }

    /**
     * Get an argument of type {@code char}. If the argument type is not {@code char.class}, a cast exception is thrown.
     *
     * @param argType the argument type
     * @param argPtr the argument pointer
     * @return the char value
     */
    @Add
    static char getCharArgument(Class<?> argType, ptr<@c_const ?> argPtr) {
        if (argType == char.class) {
            return argPtr.loadPlain(uint16_t.class).charValue();
        } else {
            throw new WrongMethodTypeException();
        }
    }

    /**
     * Get an argument of type {@code float}. If the argument type is not {@code float.class}, a cast exception is thrown.
     *
     * @param argType the argument type
     * @param argPtr the argument pointer
     * @return the float value
     */
    @Add
    static float getFloatArgument(Class<?> argType, ptr<@c_const ?> argPtr) {
        if (argType == float.class) {
            return argPtr.loadPlain(_Float32.class).floatValue();
        } else {
            throw new WrongMethodTypeException();
        }
    }

    /**
     * Get an argument of type {@code double}. If the argument type is not {@code double.class}, a cast exception is thrown.
     *
     * @param argType the argument type
     * @param argPtr the argument pointer
     * @return the double value
     */
    @Add
    static double getDoubleArgument(Class<?> argType, ptr<@c_const ?> argPtr) {
        if (argType == double.class) {
            return argPtr.loadPlain(_Float64.class).doubleValue();
        } else {
            throw new WrongMethodTypeException();
        }
    }

    /**
     * Get an argument of type {@code boolean}. If the argument type is not {@code boolean.class}, a cast exception is thrown.
     *
     * @param argType the argument type
     * @param argPtr the argument pointer
     * @return the boolean value
     */
    @Add
    static boolean getBooleanArgument(Class<?> argType, ptr<@c_const ?> argPtr) {
        if (argType == boolean.class) {
            return argPtr.loadPlain(_Bool.class).booleanValue();
        } else {
            throw new WrongMethodTypeException();
        }
    }

    /**
     * Get an argument of type {@code Object} (reference). If the argument type is not an object class or is the wrong class,
     * a cast exception is thrown.
     *
     * @param argType the argument type
     * @param argPtr the argument pointer
     * @return the object value
     */
    @SuppressWarnings("unchecked")
    @Add
    static Object getObjectArgument(Class<?> argType, ptr<@c_const ?> argPtr) {
        if (! argType.isPrimitive()) {
            return argType.cast(((ptr<@c_const Object>) argPtr).loadPlain());
        } else {
            throw new WrongMethodTypeException();
        }
    }

    // now the conversion ones

    /**
     * Get an argument of type {@code byte}, performing implicit conversions if necessary and possible.
     *
     * @param argType the argument type
     * @param argPtr the argument pointer
     * @return the byte value
     */
    @Add
    static byte getAndConvertByteArgument(Class<?> argType, ptr<@c_const ?> argPtr) {
        if (argType == byte.class) {
            return argPtr.loadPlain(int8_t.class).byteValue();
        } else if (argType == Byte.class) {
            return argPtr.loadPlain(Byte.class).byteValue();
        } else if (argType == Object.class) {
            Object obj = argPtr.loadPlain(Object.class);
            if (obj instanceof Byte v) {
                return v.byteValue();
            } else {
                throw new ClassCastException();
            }
        } else {
            throw new WrongMethodTypeException();
        }
    }

    /**
     * Get an argument of type {@code short}, performing implicit conversions if necessary and possible.
     *
     * @param argType the argument type
     * @param argPtr the argument pointer
     * @return the short value
     */
    @Add
    static short getAndConvertShortArgument(Class<?> argType, ptr<@c_const ?> argPtr) {
        if (argType == byte.class) {
            return argPtr.loadPlain(int8_t.class).shortValue();
        } else if (argType == short.class) {
            return argPtr.loadPlain(int16_t.class).shortValue();
        } else if (argType == Byte.class) {
            return argPtr.loadPlain(Byte.class).shortValue();
        } else if (argType == Short.class) {
            return argPtr.loadPlain(Short.class).shortValue();
        } else if (argType == Object.class) {
            Object obj = argPtr.loadPlain(Object.class);
            if (obj instanceof Byte v) {
                return v.shortValue();
            } else if (obj instanceof Short v) {
                return v.shortValue();
            } else {
                throw new ClassCastException();
            }
        } else {
            throw new WrongMethodTypeException();
        }
    }

    /**
     * Get an argument of type {@code int}, performing implicit conversions if necessary and possible.
     *
     * @param argType the argument type
     * @param argPtr the argument pointer
     * @return the int value
     */
    @Add
    static int getAndConvertIntArgument(Class<?> argType, ptr<@c_const ?> argPtr) {
        if (argType == byte.class) {
            return argPtr.loadPlain(int8_t.class).intValue();
        } else if (argType == short.class) {
            return argPtr.loadPlain(int16_t.class).intValue();
        } else if (argType == char.class) {
            return argPtr.loadPlain(uint16_t.class).charValue();
        } else if (argType == int.class) {
            return argPtr.loadPlain(int32_t.class).intValue();
        } else if (argType == Byte.class) {
            return argPtr.loadPlain(Byte.class).intValue();
        } else if (argType == Short.class) {
            return argPtr.loadPlain(Short.class).intValue();
        } else if (argType == Character.class) {
            return argPtr.loadPlain(Character.class).charValue();
        } else if (argType == Integer.class) {
            return argPtr.loadPlain(Integer.class).intValue();
        } else if (argType == Object.class) {
            Object obj = argPtr.loadPlain(Object.class);
            if (obj instanceof Byte v) {
                return v.intValue();
            } else if (obj instanceof Short v) {
                return v.intValue();
            } else if (obj instanceof Character v) {
                return v.charValue();
            } else if (obj instanceof Integer v) {
                return v.intValue();
            } else {
                throw new ClassCastException();
            }
        } else {
            throw new WrongMethodTypeException();
        }
    }

    /**
     * Get an argument of type {@code long}, performing implicit conversions if necessary and possible.
     *
     * @param argType the argument type
     * @param argPtr the argument pointer
     * @return the long value
     */
    @Add
    static long getAndConvertLongArgument(Class<?> argType, ptr<@c_const ?> argPtr) {
        if (argType == byte.class) {
            return argPtr.loadPlain(int8_t.class).longValue();
        } else if (argType == short.class) {
            return argPtr.loadPlain(int16_t.class).longValue();
        } else if (argType == char.class) {
            return argPtr.loadPlain(uint16_t.class).charValue();
        } else if (argType == int.class) {
            return argPtr.loadPlain(int32_t.class).longValue();
        } else if (argType == long.class) {
            return argPtr.loadPlain(int64_t.class).longValue();
        } else if (argType == Byte.class) {
            return argPtr.loadPlain(Byte.class).longValue();
        } else if (argType == Short.class) {
            return argPtr.loadPlain(Short.class).longValue();
        } else if (argType == Character.class) {
            return argPtr.loadPlain(Character.class).charValue();
        } else if (argType == Integer.class) {
            return argPtr.loadPlain(Short.class).longValue();
        } else if (argType == Long.class) {
            return argPtr.loadPlain(Long.class).longValue();
        } else if (argType == Object.class) {
            Object obj = argPtr.loadPlain(Object.class);
            if (obj instanceof Byte v) {
                return v.longValue();
            } else if (obj instanceof Short v) {
                return v.longValue();
            } else if (obj instanceof Character v) {
                return v.charValue();
            } else if (obj instanceof Integer v) {
                return v.longValue();
            } else if (obj instanceof Long v) {
                return v.longValue();
            } else {
                throw new ClassCastException();
            }
        } else {
            throw new WrongMethodTypeException();
        }
    }

    /**
     * Get an argument of type {@code float}, performing implicit conversions if necessary and possible.
     *
     * @param argType the argument type
     * @param argPtr the argument pointer
     * @return the float value
     */
    @Add
    static float getAndConvertFloatArgument(Class<?> argType, ptr<@c_const ?> argPtr) {
        if (argType == byte.class) {
            return argPtr.loadPlain(int8_t.class).byteValue();
        } else if (argType == short.class) {
            return argPtr.loadPlain(int16_t.class).shortValue();
        } else if (argType == char.class) {
            return argPtr.loadPlain(uint16_t.class).charValue();
        } else if (argType == int.class) {
            return argPtr.loadPlain(int32_t.class).intValue();
        } else if (argType == long.class) {
            return argPtr.loadPlain(int64_t.class).longValue();
        } else if (argType == float.class) {
            return argPtr.loadPlain(_Float32.class).floatValue();
        } else if (argType == Byte.class) {
            return argPtr.loadPlain(Byte.class).byteValue();
        } else if (argType == Short.class) {
            return argPtr.loadPlain(Short.class).shortValue();
        } else if (argType == Character.class) {
            return argPtr.loadPlain(Character.class).charValue();
        } else if (argType == Integer.class) {
            return argPtr.loadPlain(Short.class).intValue();
        } else if (argType == Long.class) {
            return argPtr.loadPlain(Long.class).longValue();
        } else if (argType == Float.class) {
            return argPtr.loadPlain(Float.class).floatValue();
        } else if (argType == Object.class) {
            Object obj = argPtr.loadPlain(Object.class);
            if (obj instanceof Byte v) {
                return v.longValue();
            } else if (obj instanceof Short v) {
                return v.longValue();
            } else if (obj instanceof Character v) {
                return v.charValue();
            } else if (obj instanceof Integer v) {
                return v.longValue();
            } else if (obj instanceof Long v) {
                return v.longValue();
            } else if (obj instanceof Float v) {
                return v.floatValue();
            } else {
                throw new ClassCastException();
            }
        } else {
            throw new WrongMethodTypeException();
        }
    }
    /**
     * Get an argument of type {@code double}, performing implicit conversions if necessary and possible.
     *
     * @param argType the argument type
     * @param argPtr the argument pointer
     * @return the double value
     */
    @Add
    static double getAndConvertDoubleArgument(Class<?> argType, ptr<@c_const ?> argPtr) {
        if (argType == byte.class) {
            return argPtr.loadPlain(int8_t.class).byteValue();
        } else if (argType == short.class) {
            return argPtr.loadPlain(int16_t.class).shortValue();
        } else if (argType == char.class) {
            return argPtr.loadPlain(uint16_t.class).charValue();
        } else if (argType == int.class) {
            return argPtr.loadPlain(int32_t.class).intValue();
        } else if (argType == long.class) {
            return argPtr.loadPlain(int64_t.class).longValue();
        } else if (argType == float.class) {
            return argPtr.loadPlain(_Float32.class).floatValue();
        } else if (argType == double.class) {
            return argPtr.loadPlain(_Float64.class).doubleValue();
        } else if (argType == Byte.class) {
            return argPtr.loadPlain(Byte.class).byteValue();
        } else if (argType == Short.class) {
            return argPtr.loadPlain(Short.class).shortValue();
        } else if (argType == Character.class) {
            return argPtr.loadPlain(Character.class).charValue();
        } else if (argType == Integer.class) {
            return argPtr.loadPlain(Short.class).intValue();
        } else if (argType == Long.class) {
            return argPtr.loadPlain(Long.class).longValue();
        } else if (argType == Float.class) {
            return argPtr.loadPlain(Float.class).floatValue();
        } else if (argType == Double.class) {
            return argPtr.loadPlain(Double.class).doubleValue();
        } else if (argType == Object.class) {
            Object obj = argPtr.loadPlain(Object.class);
            if (obj instanceof Byte v) {
                return v.longValue();
            } else if (obj instanceof Short v) {
                return v.longValue();
            } else if (obj instanceof Character v) {
                return v.charValue();
            } else if (obj instanceof Integer v) {
                return v.longValue();
            } else if (obj instanceof Long v) {
                return v.longValue();
            } else if (obj instanceof Float v) {
                return v.floatValue();
            } else if (obj instanceof Double v) {
                return v.doubleValue();
            } else {
                throw new ClassCastException();
            }
        } else {
            throw new WrongMethodTypeException();
        }
    }

    /**
     * Get an argument of type {@code boolean}, performing implicit conversions if necessary and possible.
     *
     * @param argType the argument type
     * @param argPtr the argument pointer
     * @return the boolean value
     */
    @Add
    static boolean getAndConvertBooleanArgument(Class<?> argType, ptr<@c_const ?> argPtr) {
        if (argType == boolean.class) {
            return argPtr.loadPlain(_Bool.class).booleanValue();
        } else if (argType == Boolean.class) {
            return argPtr.loadPlain(Boolean.class).booleanValue();
        } else if (argType == Object.class) {
            Object obj = argPtr.loadPlain(Object.class);
            if (obj instanceof Boolean v) {
                return v.booleanValue();
            } else {
                throw new ClassCastException();
            }
        } else {
            throw new WrongMethodTypeException();
        }
    }

    /**
     * Get an argument of type {@code Object}, performing implicit conversions if necessary and possible.
     *
     * @param argType the argument type
     * @param argPtr the argument pointer
     * @return the converted value value
     */
    @Add
    static <T> T getAndConvertObjectArgument(Class<?> argType, Class<T> expectType, ptr<@c_const ?> argPtr) {
        if (argType == byte.class) {
            return expectType.cast(Byte.valueOf(argPtr.loadPlain(int8_t.class).byteValue()));
        } else if (argType == short.class) {
            return expectType.cast(Short.valueOf(argPtr.loadPlain(int16_t.class).shortValue()));
        } else if (argType == char.class) {
            return expectType.cast(Character.valueOf(argPtr.loadPlain(uint16_t.class).charValue()));
        } else if (argType == int.class) {
            return expectType.cast(Integer.valueOf(argPtr.loadPlain(int32_t.class).intValue()));
        } else if (argType == long.class) {
            return expectType.cast(Long.valueOf(argPtr.loadPlain(int64_t.class).longValue()));
        } else if (argType == float.class) {
            return expectType.cast(Float.valueOf(argPtr.loadPlain(_Float32.class).floatValue()));
        } else if (argType == double.class) {
            return expectType.cast(Boolean.valueOf(argPtr.loadPlain(_Bool.class).booleanValue()));
        } else if (argType == boolean.class) {
            return expectType.cast(Boolean.valueOf(argPtr.loadPlain(_Bool.class).booleanValue()));
        } else if (argType == void.class) {
            // do not read the argument pointer, which is likely to be {@code null}
            return null;
        } else {
            Object obj = argPtr.loadPlain(Object.class);
            return expectType.cast(argType.cast(obj));
        }
    }
}
