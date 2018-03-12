package org.jdbcmon;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

class Utils {

    static void checkArgument(boolean arg, String msg, Object... args) {
        if (!arg) {
            String fullMsg = String.format(msg, args);
            throw new IllegalArgumentException(fullMsg);
        }
    }

    static <T, K extends Comparable<K>, V> Collector<T, ?, TreeMap<K, V>> toTreeMap(Function<? super T, ? extends K> keyMapper,
                                                                                    Function<? super T, ? extends V> valueMapper) {
        return Collectors.toMap(keyMapper, valueMapper, throwingMerger(), TreeMap::new);
    }

    private static <T> BinaryOperator<T> throwingMerger() {
        return (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key, existing value=[%s], new value=[%s]", u, v));
        };
    }

    static Object invokeTarget(Method method, Object obj, Object... args) throws Throwable {
        try {
            return method.invoke(obj, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    static boolean matches(Method method, String methodName, Class<?>... argTypes) {
        return method.getName().equals(methodName) &&
                Arrays.equals(method.getParameterTypes(), argTypes);
    }

    static boolean matches(Method method, Class<?> returnType, String methodName, Class<?>... argTypes) {
        return method.getReturnType() == returnType && matches(method, methodName, argTypes);
    }
}
