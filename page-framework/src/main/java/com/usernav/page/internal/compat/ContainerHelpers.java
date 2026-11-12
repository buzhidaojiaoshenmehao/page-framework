package com.usernav.page.internal.compat;

/** Shared empty arrays and binary search used by the compact map implementation. */
final class ContainerHelpers {
    static final int[] EMPTY_INTS = new int[0];
    static final long[] EMPTY_LONGS = new long[0];
    static final Object[] EMPTY_OBJECTS = new Object[0];

    private ContainerHelpers() {
    }

    static int binarySearch(int[] values, int size, int target) {
        int low = 0;
        int high = size - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int value = values[middle];
            if (value < target) {
                low = middle + 1;
            } else if (value > target) {
                high = middle - 1;
            } else {
                return middle;
            }
        }
        return ~low;
    }

    static int binarySearch(long[] values, int size, long target) {
        int low = 0;
        int high = size - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            long value = values[middle];
            if (value < target) {
                low = middle + 1;
            } else if (value > target) {
                high = middle - 1;
            } else {
                return middle;
            }
        }
        return ~low;
    }
}
