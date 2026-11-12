package com.usernav.page.internal.compat;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/** Map interface and live views over {@link SimpleArrayMap}'s compact storage. */
public class ArrayMap<K, V> extends SimpleArrayMap<K, V> implements Map<K, V> {
    private MapCollections<K, V> collections;

    public ArrayMap() {
    }

    public ArrayMap(int capacity) {
        super(capacity);
    }

    public ArrayMap(SimpleArrayMap<? extends K, ? extends V> source) {
        super(source);
    }

    private MapCollections<K, V> getCollections() {
        if (collections == null) {
            collections = new MapCollections<K, V>() {
                @Override
                public void clearMap() {
                    ArrayMap.this.clear();
                }

                @Override
                public Object getEntry(int index, int entryOffset) {
                    return entries[(index << 1) + entryOffset];
                }

                @Override
                public ArrayMap<K, V> getMap() {
                    return ArrayMap.this;
                }

                @Override
                public int getSize() {
                    return entryCount;
                }

                @Override
                public int indexOfKey(Object key) {
                    return ArrayMap.this.indexOfKey(key);
                }

                @Override
                public int indexOfValue(Object value) {
                    return ArrayMap.this.indexOfValue(value);
                }

                @Override
                public void put(K key, V value) {
                    ArrayMap.this.put(key, value);
                }

                @Override
                public void removeAt(int index) {
                    ArrayMap.this.removeAt(index);
                }

                @Override
                public V setValueAt(int index, V value) {
                    return ArrayMap.this.setValueAt(index, value);
                }
            };
        }
        return collections;
    }

    public boolean containsAll(Collection<?> keys) {
        for (Object key : keys) {
            if (!containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return getCollections().getEntrySet();
    }

    @Override
    public Set<K> keySet() {
        return getCollections().getKeySet();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> source) {
        ensureCapacity(source.size() + entryCount);
        for (Map.Entry<? extends K, ? extends V> entry : source.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public boolean removeAll(Collection<?> keys) {
        int previousSize = size();
        for (Object key : keys) {
            remove(key);
        }
        return previousSize != size();
    }

    public boolean retainAll(Collection<?> retainedKeys) {
        return MapCollections.retainKeys(this, retainedKeys);
    }

    @Override
    public Collection<V> values() {
        return getCollections().getValues();
    }
}
