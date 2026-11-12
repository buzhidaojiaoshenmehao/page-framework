package com.usernav.page.internal.compat;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Live collection views for the compact map, retaining the extracted core's contracts. */
abstract class MapCollections<K, V> {
    private EntrySet entrySet;
    private KeySet keySet;
    private ValuesCollection values;

    public final Set<Map.Entry<K, V>> getEntrySet() {
        if (entrySet == null) {
            entrySet = new EntrySet();
        }
        return entrySet;
    }

    public final Set<K> getKeySet() {
        if (keySet == null) {
            keySet = new KeySet();
        }
        return keySet;
    }

    public final Collection<V> getValues() {
        if (values == null) {
            values = new ValuesCollection();
        }
        return values;
    }

    public final class ArrayIterator<T> implements Iterator<T> {
        private final int entryOffset;
        private int expectedSize;
        private int nextIndex;
        private boolean canRemove;

        ArrayIterator(int entryOffset) {
            this.entryOffset = entryOffset;
            expectedSize = getSize();
        }

        @Override
        public boolean hasNext() {
            return nextIndex < expectedSize;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T next() {
            T value = (T) getEntry(nextIndex, entryOffset);
            nextIndex++;
            canRemove = true;
            return value;
        }

        @Override
        public void remove() {
            if (!canRemove) {
                throw new IllegalStateException();
            }
            nextIndex--;
            expectedSize--;
            canRemove = false;
            removeAt(nextIndex);
        }
    }

    public final class EntrySet implements Set<Map.Entry<K, V>> {
        // The original live entry view deliberately supports only bulk insertion and clear.
        @Override
        public boolean add(Map.Entry<K, V> entry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends Map.Entry<K, V>> collection) {
            int previousSize = getSize();
            for (Map.Entry<K, V> entry : collection) {
                put(entry.getKey(), entry.getValue());
            }
            return previousSize != getSize();
        }

        @Override
        public void clear() {
            clearMap();
        }

        @Override
        public boolean contains(Object candidate) {
            if (!(candidate instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) candidate;
            int index = indexOfKey(entry.getKey());
            if (index < 0) {
                return false;
            }
            Object value = getEntry(index, 1);
            Object otherValue = entry.getValue();
            return value == otherValue || (value != null && value.equals(otherValue));
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            for (Object candidate : collection) {
                if (!contains(candidate)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object other) {
            return equalsSet(this, other);
        }

        @Override
        public int hashCode() {
            int result = 0;
            for (int index = getSize() - 1; index >= 0; index--) {
                Object key = getEntry(index, 0);
                Object value = getEntry(index, 1);
                result += (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
            }
            return result;
        }

        @Override
        public boolean isEmpty() {
            return getSize() == 0;
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new MapIterator();
        }

        @Override
        public boolean remove(Object candidate) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return getSize();
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T[] toArray(T[] destination) {
            throw new UnsupportedOperationException();
        }
    }

    public final class KeySet implements Set<K> {
        @Override
        public boolean add(K key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends K> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            clearMap();
        }

        @Override
        public boolean contains(Object key) {
            return indexOfKey(key) >= 0;
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            for (Object key : collection) {
                if (!getMap().containsKey(key)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object other) {
            return equalsSet(this, other);
        }

        @Override
        public int hashCode() {
            int result = 0;
            for (int index = getSize() - 1; index >= 0; index--) {
                Object key = getEntry(index, 0);
                result += key == null ? 0 : key.hashCode();
            }
            return result;
        }

        @Override
        public boolean isEmpty() {
            return getSize() == 0;
        }

        @Override
        public Iterator<K> iterator() {
            return new ArrayIterator<>(0);
        }

        @Override
        public boolean remove(Object key) {
            int index = indexOfKey(key);
            if (index < 0) {
                return false;
            }
            removeAt(index);
            return true;
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            Map<K, V> map = getMap();
            int previousSize = map.size();
            for (Object key : collection) {
                map.remove(key);
            }
            return previousSize != map.size();
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            return retainKeys(getMap(), collection);
        }

        @Override
        public int size() {
            return getSize();
        }

        @Override
        public Object[] toArray() {
            return toArrayForOffset(0);
        }

        @Override
        public <T> T[] toArray(T[] destination) {
            return toArrayForOffset(0, destination);
        }
    }

    /** The iterator itself is the current entry; retaining entries across next() is unsupported. */
    public final class MapIterator implements Iterator<Map.Entry<K, V>>, Map.Entry<K, V> {
        private int lastIndex = getSize() - 1;
        private int currentIndex = -1;
        private boolean entryValid;

        private void requireValidEntry() {
            if (!entryValid) {
                throw new IllegalStateException("This container does not support retaining Map.Entry objects");
            }
        }

        @Override
        public boolean equals(Object other) {
            requireValidEntry();
            if (!(other instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> otherEntry = (Map.Entry<?, ?>) other;
            Object otherKey = otherEntry.getKey();
            Object key = getEntry(currentIndex, 0);
            if (otherKey != key && (otherKey == null || !otherKey.equals(key))) {
                return false;
            }
            Object otherValue = otherEntry.getValue();
            Object value = getEntry(currentIndex, 1);
            return otherValue == value || (otherValue != null && otherValue.equals(value));
        }

        @Override
        @SuppressWarnings("unchecked")
        public K getKey() {
            requireValidEntry();
            return (K) getEntry(currentIndex, 0);
        }

        @Override
        @SuppressWarnings("unchecked")
        public V getValue() {
            requireValidEntry();
            return (V) getEntry(currentIndex, 1);
        }

        @Override
        public boolean hasNext() {
            return currentIndex < lastIndex;
        }

        @Override
        public int hashCode() {
            requireValidEntry();
            Object key = getEntry(currentIndex, 0);
            Object value = getEntry(currentIndex, 1);
            return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }

        @Override
        public Map.Entry<K, V> next() {
            currentIndex++;
            entryValid = true;
            return this;
        }

        @Override
        public void remove() {
            if (!entryValid) {
                throw new IllegalStateException();
            }
            removeAt(currentIndex);
            currentIndex--;
            lastIndex--;
            entryValid = false;
        }

        @Override
        public V setValue(V value) {
            requireValidEntry();
            return setValueAt(currentIndex, value);
        }

        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    public final class ValuesCollection implements Collection<V> {
        @Override
        public boolean add(V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends V> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            clearMap();
        }

        @Override
        public boolean contains(Object value) {
            return indexOfValue(value) >= 0;
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            for (Object value : collection) {
                if (!contains(value)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean isEmpty() {
            return getSize() == 0;
        }

        @Override
        public Iterator<V> iterator() {
            return new ArrayIterator<>(1);
        }

        @Override
        public boolean remove(Object value) {
            int index = indexOfValue(value);
            if (index < 0) {
                return false;
            }
            removeAt(index);
            return true;
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            boolean changed = false;
            int size = getSize();
            for (int index = 0; index < size; index++) {
                if (collection.contains(getEntry(index, 1))) {
                    removeAt(index);
                    index--;
                    size--;
                    changed = true;
                }
            }
            return changed;
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            boolean changed = false;
            int size = getSize();
            for (int index = 0; index < size; index++) {
                if (!collection.contains(getEntry(index, 1))) {
                    removeAt(index);
                    index--;
                    size--;
                    changed = true;
                }
            }
            return changed;
        }

        @Override
        public int size() {
            return getSize();
        }

        @Override
        public Object[] toArray() {
            return toArrayForOffset(1);
        }

        @Override
        public <T> T[] toArray(T[] destination) {
            return toArrayForOffset(1, destination);
        }
    }

    public static <T> boolean equalsSet(Set<T> set, Object other) {
        if (set == other) {
            return true;
        }
        if (!(other instanceof Set)) {
            return false;
        }
        Set<?> otherSet = (Set<?>) other;
        try {
            return set.size() == otherSet.size() && set.containsAll(otherSet);
        } catch (ClassCastException | NullPointerException exception) {
            return false;
        }
    }

    public static <K, V> boolean retainKeys(Map<K, V> map, Collection<?> retainedKeys) {
        int previousSize = map.size();
        Iterator<K> iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            if (!retainedKeys.contains(iterator.next())) {
                iterator.remove();
            }
        }
        return previousSize != map.size();
    }

    public abstract void clearMap();
    public abstract Object getEntry(int index, int entryOffset);
    public abstract ArrayMap<K, V> getMap();
    public abstract int getSize();
    public abstract int indexOfKey(Object key);
    public abstract int indexOfValue(Object value);
    public abstract void put(K key, V value);
    public abstract void removeAt(int index);
    public abstract V setValueAt(int index, V value);

    public final Object[] toArrayForOffset(int entryOffset) {
        int size = getSize();
        Object[] result = new Object[size];
        for (int index = 0; index < size; index++) {
            result[index] = getEntry(index, entryOffset);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public final <T> T[] toArrayForOffset(int entryOffset, T[] destination) {
        int size = getSize();
        T[] result = destination.length < size
                ? (T[]) Array.newInstance(destination.getClass().getComponentType(), size) : destination;
        for (int index = 0; index < size; index++) {
            result[index] = (T) getEntry(index, entryOffset);
        }
        if (result.length > size) {
            result[size] = null;
        }
        return result;
    }
}
