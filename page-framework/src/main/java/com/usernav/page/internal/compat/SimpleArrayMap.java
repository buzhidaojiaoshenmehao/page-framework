package com.usernav.page.internal.compat;

import java.util.Map;

/** Compact map storage: sorted hashes and interleaved key/value slots. */
public class SimpleArrayMap<K, V> {
    private static final int BASE_CAPACITY = 4;
    private static final int ARRAY_CACHE_LIMIT = 10;
    static Object[] baseArrayCache;
    static int baseArrayCacheSize;
    static Object[] doubleBaseArrayCache;
    static int doubleBaseArrayCacheSize;
    Object[] entries;
    int[] hashes;
    int entryCount;

    public SimpleArrayMap() {
        hashes = ContainerHelpers.EMPTY_INTS;
        entries = ContainerHelpers.EMPTY_OBJECTS;
    }

    public SimpleArrayMap(int capacity) {
        if (capacity == 0) {
            hashes = ContainerHelpers.EMPTY_INTS;
            entries = ContainerHelpers.EMPTY_OBJECTS;
        } else {
            allocateArrays(capacity);
        }
    }

    public SimpleArrayMap(SimpleArrayMap<? extends K, ? extends V> source) {
        this();
        if (source != null) {
            putAll(source);
        }
    }

    private void allocateArrays(int capacity) {
        if (capacity == BASE_CAPACITY * 2) {
            synchronized (ArrayMap.class) {
                if (doubleBaseArrayCache != null) {
                    entries = doubleBaseArrayCache;
                    doubleBaseArrayCache = (Object[]) entries[0];
                    hashes = (int[]) entries[1];
                    entries[0] = null;
                    entries[1] = null;
                    doubleBaseArrayCacheSize--;
                    return;
                }
            }
        } else if (capacity == BASE_CAPACITY) {
            synchronized (ArrayMap.class) {
                if (baseArrayCache != null) {
                    entries = baseArrayCache;
                    baseArrayCache = (Object[]) entries[0];
                    hashes = (int[]) entries[1];
                    entries[0] = null;
                    entries[1] = null;
                    baseArrayCacheSize--;
                    return;
                }
            }
        }
        hashes = new int[capacity];
        entries = new Object[capacity << 1];
    }

    private static void recycleArrays(int[] oldHashes, Object[] oldEntries, int size) {
        if (oldHashes.length == BASE_CAPACITY * 2) {
            synchronized (ArrayMap.class) {
                if (doubleBaseArrayCacheSize < ARRAY_CACHE_LIMIT) {
                    oldEntries[0] = doubleBaseArrayCache;
                    oldEntries[1] = oldHashes;
                    for (int slot = (size << 1) - 1; slot >= 2; slot--) {
                        oldEntries[slot] = null;
                    }
                    doubleBaseArrayCache = oldEntries;
                    doubleBaseArrayCacheSize++;
                }
            }
        } else if (oldHashes.length == BASE_CAPACITY) {
            synchronized (ArrayMap.class) {
                if (baseArrayCacheSize < ARRAY_CACHE_LIMIT) {
                    oldEntries[0] = baseArrayCache;
                    oldEntries[1] = oldHashes;
                    for (int slot = (size << 1) - 1; slot >= 2; slot--) {
                        oldEntries[slot] = null;
                    }
                    baseArrayCache = oldEntries;
                    baseArrayCacheSize++;
                }
            }
        }
    }

    public void clear() {
        if (entryCount != 0) {
            recycleArrays(hashes, entries, entryCount);
            hashes = ContainerHelpers.EMPTY_INTS;
            entries = ContainerHelpers.EMPTY_OBJECTS;
            entryCount = 0;
        }
    }

    public boolean containsKey(Object key) {
        return indexOfKey(key) >= 0;
    }

    public boolean containsValue(Object value) {
        return indexOfValue(value) >= 0;
    }

    public void ensureCapacity(int minimumCapacity) {
        if (hashes.length < minimumCapacity) {
            int[] oldHashes = hashes;
            Object[] oldEntries = entries;
            allocateArrays(minimumCapacity);
            if (entryCount > 0) {
                System.arraycopy(oldHashes, 0, hashes, 0, entryCount);
                System.arraycopy(oldEntries, 0, entries, 0, entryCount << 1);
            }
            recycleArrays(oldHashes, oldEntries, entryCount);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Map)) {
            return false;
        }
        Map<?, ?> otherMap = (Map<?, ?>) other;
        if (size() != otherMap.size()) {
            return false;
        }
        for (int index = 0; index < entryCount; index++) {
            try {
                K key = keyAt(index);
                V value = valueAt(index);
                Object otherValue = otherMap.get(key);
                if (value == null) {
                    if (otherValue != null || !otherMap.containsKey(key)) {
                        return false;
                    }
                } else if (!value.equals(otherValue)) {
                    return false;
                }
            } catch (ClassCastException | NullPointerException exception) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public V get(Object key) {
        int index = indexOfKey(key);
        return index >= 0 ? (V) entries[(index << 1) + 1] : null;
    }

    @Override
    public int hashCode() {
        int result = 0;
        for (int index = 0; index < entryCount; index++) {
            Object value = entries[(index << 1) + 1];
            result += hashes[index] ^ (value == null ? 0 : value.hashCode());
        }
        return result;
    }

    public int indexOf(Object key, int hash) {
        if (entryCount == 0) {
            return -1;
        }
        int index = ContainerHelpers.binarySearch(hashes, entryCount, hash);
        if (index < 0 || key.equals(entries[index << 1])) {
            return index;
        }
        int collisionEnd = index + 1;
        while (collisionEnd < entryCount && hashes[collisionEnd] == hash) {
            if (key.equals(entries[collisionEnd << 1])) {
                return collisionEnd;
            }
            collisionEnd++;
        }
        for (int collisionIndex = index - 1;
                collisionIndex >= 0 && hashes[collisionIndex] == hash; collisionIndex--) {
            if (key.equals(entries[collisionIndex << 1])) {
                return collisionIndex;
            }
        }
        return ~collisionEnd;
    }

    public int indexOfKey(Object key) {
        return key == null ? indexOfNull() : indexOf(key, key.hashCode());
    }

    public int indexOfNull() {
        if (entryCount == 0) {
            return -1;
        }
        int index = ContainerHelpers.binarySearch(hashes, entryCount, 0);
        if (index < 0 || entries[index << 1] == null) {
            return index;
        }
        int collisionEnd = index + 1;
        while (collisionEnd < entryCount && hashes[collisionEnd] == 0) {
            if (entries[collisionEnd << 1] == null) {
                return collisionEnd;
            }
            collisionEnd++;
        }
        for (int collisionIndex = index - 1;
                collisionIndex >= 0 && hashes[collisionIndex] == 0; collisionIndex--) {
            if (entries[collisionIndex << 1] == null) {
                return collisionIndex;
            }
        }
        return ~collisionEnd;
    }

    public int indexOfValue(Object value) {
        for (int slot = 1; slot < entryCount * 2; slot += 2) {
            if (value == null ? entries[slot] == null : value.equals(entries[slot])) {
                return slot >> 1;
            }
        }
        return -1;
    }

    public boolean isEmpty() {
        return entryCount <= 0;
    }

    @SuppressWarnings("unchecked")
    public K keyAt(int index) {
        return (K) entries[index << 1];
    }

    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        int hash = key == null ? 0 : key.hashCode();
        int index = key == null ? indexOfNull() : indexOf(key, hash);
        if (index >= 0) {
            int valueSlot = (index << 1) + 1;
            V previousValue = (V) entries[valueSlot];
            entries[valueSlot] = value;
            return previousValue;
        }
        int insertionIndex = ~index;
        if (entryCount >= hashes.length) {
            int newCapacity = entryCount >= 8 ? entryCount + (entryCount >> 1)
                    : (entryCount >= 4 ? 8 : 4);
            int[] oldHashes = hashes;
            Object[] oldEntries = entries;
            allocateArrays(newCapacity);
            if (hashes.length > 0) {
                System.arraycopy(oldHashes, 0, hashes, 0, oldHashes.length);
                System.arraycopy(oldEntries, 0, entries, 0, oldEntries.length);
            }
            recycleArrays(oldHashes, oldEntries, entryCount);
        }
        if (insertionIndex < entryCount) {
            System.arraycopy(hashes, insertionIndex, hashes, insertionIndex + 1,
                    entryCount - insertionIndex);
            System.arraycopy(entries, insertionIndex << 1, entries, (insertionIndex + 1) << 1,
                    (entryCount - insertionIndex) << 1);
        }
        hashes[insertionIndex] = hash;
        entries[insertionIndex << 1] = key;
        entries[(insertionIndex << 1) + 1] = value;
        entryCount++;
        return null;
    }

    public void putAll(SimpleArrayMap<? extends K, ? extends V> source) {
        int sourceSize = source.entryCount;
        ensureCapacity(entryCount + sourceSize);
        if (entryCount != 0) {
            for (int index = 0; index < sourceSize; index++) {
                put(source.keyAt(index), source.valueAt(index));
            }
        } else if (sourceSize > 0) {
            System.arraycopy(source.hashes, 0, hashes, 0, sourceSize);
            System.arraycopy(source.entries, 0, entries, 0, sourceSize << 1);
            entryCount = sourceSize;
        }
    }

    public V remove(Object key) {
        int index = indexOfKey(key);
        return index >= 0 ? removeAt(index) : null;
    }

    @SuppressWarnings("unchecked")
    public V removeAt(int index) {
        int keySlot = index << 1;
        Object[] oldEntries = entries;
        V previousValue = (V) oldEntries[keySlot + 1];
        if (entryCount <= 1) {
            recycleArrays(hashes, oldEntries, entryCount);
            hashes = ContainerHelpers.EMPTY_INTS;
            entries = ContainerHelpers.EMPTY_OBJECTS;
            entryCount = 0;
        } else if (hashes.length <= 8 || entryCount >= hashes.length / 3) {
            entryCount--;
            if (index < entryCount) {
                System.arraycopy(hashes, index + 1, hashes, index, entryCount - index);
                System.arraycopy(entries, (index + 1) << 1, entries, keySlot,
                        (entryCount - index) << 1);
            }
            entries[entryCount << 1] = null;
            entries[(entryCount << 1) + 1] = null;
        } else {
            int[] oldHashes = hashes;
            int newCapacity = entryCount > 8 ? entryCount + (entryCount >> 1) : 8;
            allocateArrays(newCapacity);
            entryCount--;
            if (index > 0) {
                System.arraycopy(oldHashes, 0, hashes, 0, index);
                System.arraycopy(oldEntries, 0, entries, 0, keySlot);
            }
            if (index < entryCount) {
                System.arraycopy(oldHashes, index + 1, hashes, index, entryCount - index);
                System.arraycopy(oldEntries, (index + 1) << 1, entries, keySlot,
                        (entryCount - index) << 1);
            }
        }
        return previousValue;
    }

    @SuppressWarnings("unchecked")
    public V setValueAt(int index, V value) {
        int valueSlot = (index << 1) + 1;
        V previousValue = (V) entries[valueSlot];
        entries[valueSlot] = value;
        return previousValue;
    }

    public int size() {
        return entryCount;
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "{}";
        }
        StringBuilder description = new StringBuilder(entryCount * 28);
        description.append('{');
        for (int index = 0; index < entryCount; index++) {
            if (index > 0) {
                description.append(", ");
            }
            K key = keyAt(index);
            description.append(key == this ? "(this Map)" : key);
            description.append('=');
            V value = valueAt(index);
            description.append(value == this ? "(this Map)" : value);
        }
        return description.append('}').toString();
    }

    @SuppressWarnings("unchecked")
    public V valueAt(int index) {
        return (V) entries[(index << 1) + 1];
    }
}
