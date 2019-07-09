package org.jdbcmon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Limited size map.
 * If size exceeds limits, just creates a new object with supplier and returns it, but does not replace existing old
 * key values to prevent gc troubles with old generation.
 * Non-thread safe, requires external synchronization.
 *
 * @param <K>
 * @param <V>
 */
class LimitedMap<K, V> {

    private final int size;
    private final Map<K, V> map;

    LimitedMap(int size) {
        this.size = size;
        this.map = new HashMap<>();
    }

    V computeIfAbsent(K key, Supplier<? extends V> supplier) {
        V value = map.get(key);
        if (value != null) {
            return value;
        }
        value = supplier.get();
        if (value != null && map.size() < size) {
            map.put(key, value);
        }
        return value;
    }

    List<V> copyValues() {
        return new ArrayList<>(map.values());
    }

    void forEach(BiConsumer<? super K, ? super V> visitor) {
        map.forEach(visitor);
    }

    boolean isEmpty() {
        return map.isEmpty();
    }

    void clear() {
        map.clear();
    }
}
