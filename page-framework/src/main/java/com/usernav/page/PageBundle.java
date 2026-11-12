package com.usernav.page;

import android.content.Intent;
import android.os.Bundle;
import com.usernav.page.internal.PageAnimationOptions;
import com.usernav.page.internal.PageTelemetry;
import com.usernav.page.internal.StringBuilderUtils;
import com.usernav.page.internal.compat.ArrayMap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;

/**
 * In-memory page arguments and launch metadata reconstructed from the extracted core.
 *
 * <p>The copy constructors and {@link #clone()} copy argument values, action, package,
 * class loader and descriptor status only. Launch flags and animation metadata retain
 * their defaults, matching the original core; PageNavigator copies those separately.
 */
@SuppressWarnings("unchecked")
public final class PageBundle {
    static final int BUNDLE_MAGIC = 1279544898;
    public static final PageBundle EMPTY;
    public static final int FLAG_LAUNCH_MODE_SINGLE_INSTANCE = 16;
    public static final int FLAG_LAUNCH_MODE_SINGLE_INSTANCE_WITHOUT_REUSE = 32;
    public static final int FLAG_LAUNCH_MODE_SINGLE_TASK = 4;
    public static final int FLAG_LAUNCH_MODE_SINGLE_TASK_WITHOUT_REUSE = 8;
    public static final int FLAG_LAUNCH_MODE_SINGLE_TOP = 2;
    public static final int FLAG_LAUNCH_MODE_SINGLE_TOP_ALLOW_DUPLICATE = 64;
    public static final int FLAG_LAUNCH_MODE_STANDARD = 1;
    public static final int FLAG_LAUNCH_MODE_TRANSPARENT = 128;
    public static final int FLAG_TRANSITION_ANIMATION_STYLE_DEFAULT = 0;
    public static final int FLAG_TRANSITION_ANIMATION_STYLE_MAP_TO_MAP = 2;
    public static final int FLAG_TRANSITION_ANIMATION_STYLE_MAP_TO_PAGE = 4;
    public static final int FLAG_TRANSITION_ANIMATION_STYLE_PAGE_TO_MAP = 3;
    public static final int FLAG_TRANSITION_ANIMATION_STYLE_PAGE_TO_PAGE = 1;
    public static final String KEY_EXECUTE_IMMEDIATELY = "execute_immediately";
    public static final String KEY_FROM_PAGE = "page_framework_from_page";
    public static final String KEY_PAGE_INSTANCE_ID = "page_instance_id";
    public static final String KEY_START_PAGE_SYNC = "start_page_sync";
    public static final String KEY_START_TIME = "page_framework_start_time";
    public static final String KEY_TIME_LINE_EXTRA = "__time_line_extra__";
    private static final String TAG = "Bundle";
    private String action;
    private boolean allowFileDescriptors;
    private PageAnimationOptions animationOptions;
    private ClassLoader classLoader;
    private boolean fileDescriptorsKnown;
    private int flags;
    private boolean hasFileDescriptors;
    ArrayMap<String, Object> values;
    private int maxCount;
    private String maxCountId;
    private boolean pushAnimationNeeded;
    private int pageCount;
    private int transitionAnimationStyle;
    private String uniqueId;
    private String packageName;

    static {
        PageBundle pageBundle = new PageBundle();
        EMPTY = pageBundle;
        pageBundle.values = new ArrayMap<>();
    }

    public PageBundle() {
        this.values = null;
        this.hasFileDescriptors = false;
        this.fileDescriptorsKnown = true;
        this.allowFileDescriptors = true;
        this.flags = 0;
        this.pageCount = 0;
        this.transitionAnimationStyle = 0;
        this.pushAnimationNeeded = false;
        this.maxCount = 0;
        this.values = new ArrayMap<>();
        this.classLoader = PageBundle.class.getClassLoader();
    }

    public PageBundle(int initialCapacity) {
        this.values = null;
        this.hasFileDescriptors = false;
        this.fileDescriptorsKnown = true;
        this.allowFileDescriptors = true;
        this.flags = 0;
        this.pageCount = 0;
        this.transitionAnimationStyle = 0;
        this.pushAnimationNeeded = false;
        this.maxCount = 0;
        this.values = new ArrayMap<>(initialCapacity);
        this.classLoader = PageBundle.class.getClassLoader();
    }

    public PageBundle(Intent intent) {
        Set<String> extraKeys;
        this.values = null;
        this.hasFileDescriptors = false;
        this.fileDescriptorsKnown = true;
        this.allowFileDescriptors = true;
        this.flags = 0;
        this.pageCount = 0;
        this.transitionAnimationStyle = 0;
        this.pushAnimationNeeded = false;
        this.maxCount = 0;
        this.action = intent.getAction();
        this.packageName = intent.getPackage();
        this.values = new ArrayMap<>();
        Bundle extras = intent.getExtras();
        if (extras == null || (extraKeys = extras.keySet()) == null) {
            return;
        }
        for (String key : extraKeys) {
            this.values.put(key, extras.get(key));
        }
    }

    public PageBundle(PageBundle source) {
        this.values = null;
        this.hasFileDescriptors = false;
        this.fileDescriptorsKnown = true;
        this.allowFileDescriptors = true;
        this.flags = 0;
        this.pageCount = 0;
        this.transitionAnimationStyle = 0;
        this.pushAnimationNeeded = false;
        this.maxCount = 0;
        ArrayMap<String, Object> sourceValues = source.values;
        if (sourceValues != null) {
            this.values = new ArrayMap<>(sourceValues);
        } else {
            this.values = null;
        }
        this.hasFileDescriptors = source.hasFileDescriptors;
        this.fileDescriptorsKnown = source.fileDescriptorsKnown;
        this.classLoader = source.classLoader;
        this.action = source.getAction();
        this.packageName = source.getPackageName();
    }

    public PageBundle(ClassLoader classLoader) {
        this.values = null;
        this.hasFileDescriptors = false;
        this.fileDescriptorsKnown = true;
        this.allowFileDescriptors = true;
        this.flags = 0;
        this.pageCount = 0;
        this.transitionAnimationStyle = 0;
        this.pushAnimationNeeded = false;
        this.maxCount = 0;
        this.values = new ArrayMap<>();
        this.classLoader = classLoader;
    }

    public PageBundle(String action, String packageName) {
        this();
        this.action = action;
        this.packageName = packageName;
    }

    public PageBundle(String action, String packageName, PageBundle source) {
        this.values = null;
        this.hasFileDescriptors = false;
        this.fileDescriptorsKnown = true;
        this.allowFileDescriptors = true;
        this.flags = 0;
        this.pageCount = 0;
        this.transitionAnimationStyle = 0;
        this.pushAnimationNeeded = false;
        this.maxCount = 0;
        ArrayMap<String, Object> sourceValues = source.values;
        if (sourceValues != null) {
            this.values = new ArrayMap<>(sourceValues);
        } else {
            this.values = null;
        }
        this.hasFileDescriptors = source.hasFileDescriptors;
        this.fileDescriptorsKnown = source.fileDescriptorsKnown;
        this.classLoader = source.classLoader;
        this.action = action;
        this.packageName = packageName;
    }

    public static PageBundle forPair(String key, String value) {
        PageBundle bundle = new PageBundle(1);
        bundle.putString(key, value);
        return bundle;
    }

    private void typeWarning(String key, Object value, String expectedType, ClassCastException exception) {
        typeWarning(key, value, expectedType, "<null>", exception);
    }

    private void typeWarning(String key, Object value, String expectedType, Object defaultValue, ClassCastException exception) {
        StringBuilder warning = StringBuilderUtils.concat("Key ", key, " expected ", expectedType, " but value was a ");
        warning.append(value.getClass().getName());
        warning.append(".  The default value ");
        warning.append(defaultValue);
        warning.append(" was returned.");
        PageTelemetry.warn(TAG, warning.toString());
        PageTelemetry.warn(TAG, "Attempt to cast generated internal exception:" + exception);
    }

    public void clear() {
        this.values.clear();
        this.hasFileDescriptors = false;
        this.fileDescriptorsKnown = true;
    }

    @Override
    public Object clone() {
        return new PageBundle(this);
    }

    public boolean containsKey(String key) {
        return this.values.containsKey(key);
    }

    public Object get(String key) {
        return this.values.get(key);
    }

    public String getAction() {
        return this.action;
    }

    public PageAnimationOptions getAnimation() {
        return this.animationOptions;
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = this.values.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return ((Boolean) value).booleanValue();
        } catch (ClassCastException exception) {
            typeWarning(key, value, "Boolean", Boolean.valueOf(defaultValue), exception);
            return defaultValue;
        }
    }

    public boolean[] getBooleanArray(String key) {
        Object value = this.values.get(key);
        if (value == null) {
            return null;
        }
        try {
            return (boolean[]) value;
        } catch (ClassCastException exception) {
            typeWarning(key, value, "byte[]", exception);
            return null;
        }
    }

    public PageBundle getBundle(String key) {
        Object value = this.values.get(key);
        if (value == null) {
            return null;
        }
        try {
            return (PageBundle) value;
        } catch (ClassCastException exception) {
            typeWarning(key, value, TAG, exception);
            return null;
        }
    }

    public byte getByte(String key) {
        return getByte(key, (byte) 0).byteValue();
    }

    public Byte getByte(String key, byte defaultValue) {
        Object value = this.values.get(key);
        if (value == null) {
            return Byte.valueOf(defaultValue);
        }
        try {
            return (Byte) value;
        } catch (ClassCastException exception) {
            typeWarning(key, value, "Byte", Byte.valueOf(defaultValue), exception);
            return Byte.valueOf(defaultValue);
        }
    }

    public byte[] getByteArray(String key) {
        Object value = this.values.get(key);
        if (value == null) {
            return null;
        }
        try {
            return (byte[]) value;
        } catch (ClassCastException exception) {
            typeWarning(key, value, "byte[]", exception);
            return null;
        }
    }

    public char getChar(String key) {
        return getChar(key, (char) 0);
    }

    public char getChar(String key, char defaultValue) {
        Object value = this.values.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return ((Character) value).charValue();
        } catch (ClassCastException exception) {
            typeWarning(key, value, "Character", Character.valueOf(defaultValue), exception);
            return defaultValue;
        }
    }

    public char[] getCharArray(String key) {
        Object value = this.values.get(key);
        if (value == null) {
            return null;
        }
        try {
            return (char[]) value;
        } catch (ClassCastException exception) {
            typeWarning(key, value, "char[]", exception);
            return null;
        }
    }

    public CharSequence getCharSequence(String key) {
        Object value = this.values.get(key);
        try {
            return (CharSequence) value;
        } catch (ClassCastException exception) {
            typeWarning(key, value, "CharSequence", exception);
            return null;
        }
    }

    public CharSequence getCharSequence(String key, CharSequence defaultValue) {
        CharSequence value = getCharSequence(key);
        if (value != null) {
            defaultValue = value;
        }
        return defaultValue;
    }

    public CharSequence[] getCharSequenceArray(String key) {
        Object value = this.values.get(key);
        if (value == null) {
            return null;
        }
        try {
            return (CharSequence[]) value;
        } catch (ClassCastException exception) {
            typeWarning(key, value, "CharSequence[]", exception);
            return null;
        }
    }

    public ArrayList<CharSequence> getCharSequenceArrayList(String key) {
        Object value = this.values.get(key);
        if (value == null) {
            return null;
        }
        try {
            return (ArrayList) value;
        } catch (ClassCastException exception) {
            typeWarning(key, value, "ArrayList<CharSequence>", exception);
            return null;
        }
    }

    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    public double getDouble(String key) {
        return getDouble(key, 0.0d);
    }

    public double getDouble(String key, double defaultValue) {
        Object value = this.values.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return ((Double) value).doubleValue();
        } catch (ClassCastException exception) {
            typeWarning(key, value, "Double", Double.valueOf(defaultValue), exception);
            return defaultValue;
        }
    }

    public double[] getDoubleArray(String key) {
        Object value = this.values.get(key);
        if (value == null) {
            return null;
        }
        try {
            return (double[]) value;
        } catch (ClassCastException exception) {
            typeWarning(key, value, "double[]", exception);
            return null;
        }
    }

    public int getFlags() {
        return this.flags;
    }

    public float getFloat(String key) {
        return getFloat(key, 0.0f);
    }

    public float getFloat(String key, float defaultValue) {
        Object value = this.values.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return ((Float) value).floatValue();
        } catch (ClassCastException exception) {
            typeWarning(key, value, "Float", Float.valueOf(defaultValue), exception);
            return defaultValue;
        }
    }

    public float[] getFloatArray(String key) {
        Object value = this.values.get(key);
        if (value == null) {
            return null;
        }
        try {
            return (float[]) value;
        } catch (ClassCastException exception) {
            typeWarning(key, value, "float[]", exception);
            return null;
        }
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int defaultValue) {
        Object value = this.values.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return ((Integer) value).intValue();
        } catch (ClassCastException exception) {
            typeWarning(key, value, "Integer", Integer.valueOf(defaultValue), exception);
            return defaultValue;
        }
    }

    public int[] getIntArray(String key) {
        Object value = this.values.get(key);
        if (value == null) {
            return null;
        }
        try {
            return (int[]) value;
        } catch (ClassCastException exception) {
            typeWarning(key, value, "int[]", exception);
            return null;
        }
    }

    public ArrayList<Integer> getIntegerArrayList(String key) {
        Object value = this.values.get(key);
        if (value == null) {
            return null;
        }
        try {
            return (ArrayList) value;
        } catch (ClassCastException exception) {
            typeWarning(key, value, "ArrayList<Integer>", exception);
            return null;
        }
    }

    public long getLong(String key) {
        return getLong(key, 0L);
    }

    public long getLong(String key, long defaultValue) {
        Object value = this.values.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return ((Long) value).longValue();
        } catch (ClassCastException exception) {
            typeWarning(key, value, "Long", Long.valueOf(defaultValue), exception);
            return defaultValue;
        }
    }

    public long[] getLongArray(String key) {
        Object value = this.values.get(key);
        if (value == null) {
            return null;
        }
        try {
            return (long[]) value;
        } catch (ClassCastException exception) {
            typeWarning(key, value, "long[]", exception);
            return null;
        }
    }

    public int getMaxCount() {
        return this.maxCount;
    }

    public String getMaxCountID() {
        return this.maxCountId;
    }

    public Object getObject(String key) {
        return this.values.get(key);
    }

    public String getPackageName() {
        return this.packageName;
    }

    public int getPageCnt() {
        return this.pageCount;
    }

    public String getPairValue() {
        int size = this.values.size();
        if (size > 1) {
            PageTelemetry.warn(TAG, "getPairValue() used on Bundle with multiple pairs.");
        }
        if (size == 0) {
            return null;
        }
        Object value = this.values.valueAt(0);
        try {
            return (String) value;
        } catch (ClassCastException exception) {
            typeWarning("getPairValue()", value, "String", exception);
            return null;
        }
    }

    public Serializable getSerializable(String key) {
        Object value = this.values.get(key);
        if (value == null) {
            return null;
        }
        try {
            return (Serializable) value;
        } catch (ClassCastException exception) {
            typeWarning(key, value, "Serializable", exception);
            return null;
        }
    }

    public short getShort(String key) {
        return getShort(key, (short) 0);
    }

    public short getShort(String key, short defaultValue) {
        Object value = this.values.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return ((Short) value).shortValue();
        } catch (ClassCastException exception) {
            typeWarning(key, value, "Short", Short.valueOf(defaultValue), exception);
            return defaultValue;
        }
    }

    public short[] getShortArray(String key) {
        Object value = this.values.get(key);
        if (value == null) {
            return null;
        }
        try {
            return (short[]) value;
        } catch (ClassCastException exception) {
            typeWarning(key, value, "short[]", exception);
            return null;
        }
    }

    public String getString(String key) {
        Object value = this.values.get(key);
        try {
            return (String) value;
        } catch (ClassCastException exception) {
            typeWarning(key, value, "String", exception);
            return null;
        }
    }

    public String getString(String key, String defaultValue) {
        String value = getString(key);
        if (value != null) {
            defaultValue = value;
        }
        return defaultValue;
    }

    public String[] getStringArray(String key) {
        Object value = this.values.get(key);
        if (value == null) {
            return null;
        }
        try {
            return (String[]) value;
        } catch (ClassCastException exception) {
            typeWarning(key, value, "String[]", exception);
            return null;
        }
    }

    public ArrayList<String> getStringArrayList(String key) {
        Object value = this.values.get(key);
        if (value == null) {
            return null;
        }
        try {
            return (ArrayList) value;
        } catch (ClassCastException exception) {
            typeWarning(key, value, "ArrayList<String>", exception);
            return null;
        }
    }

    public int getTransitionAnimationStyle() {
        return this.transitionAnimationStyle;
    }

    public String getUniqueID() {
        return this.uniqueId;
    }

    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    public boolean isNeedPushAnimation() {
        return this.pushAnimationNeeded;
    }

    public Set<String> keySet() {
        return this.values.keySet();
    }

    public void putBoolean(String key, boolean value) {
        this.values.put(key, Boolean.valueOf(value));
    }

    public void putBooleanArray(String key, boolean[] value) {
        this.values.put(key, value);
    }

    public void putBundle(String key, PageBundle value) {
        this.values.put(key, value);
    }

    public void putByte(String key, byte value) {
        this.values.put(key, Byte.valueOf(value));
    }

    public void putByteArray(String key, byte[] value) {
        this.values.put(key, value);
    }

    public void putChar(String key, char value) {
        this.values.put(key, Character.valueOf(value));
    }

    public void putCharArray(String key, char[] value) {
        this.values.put(key, value);
    }

    public void putCharSequence(String key, CharSequence value) {
        this.values.put(key, value);
    }

    public void putCharSequenceArray(String key, CharSequence[] value) {
        this.values.put(key, value);
    }

    public void putCharSequenceArrayList(String key, ArrayList<CharSequence> value) {
        this.values.put(key, value);
    }

    public void putDouble(String key, double value) {
        this.values.put(key, Double.valueOf(value));
    }

    public void putDoubleArray(String key, double[] value) {
        this.values.put(key, value);
    }

    public void putFloat(String key, float value) {
        this.values.put(key, Float.valueOf(value));
    }

    public void putFloatArray(String key, float[] value) {
        this.values.put(key, value);
    }

    public void putInt(String key, int value) {
        this.values.put(key, Integer.valueOf(value));
    }

    public void putIntArray(String key, int[] value) {
        this.values.put(key, value);
    }

    public void putIntegerArrayList(String key, ArrayList<Integer> value) {
        this.values.put(key, value);
    }

    public void putLong(String key, long value) {
        this.values.put(key, Long.valueOf(value));
    }

    public void putLongArray(String key, long[] value) {
        this.values.put(key, value);
    }

    public void putObject(String key, Object value) {
        this.values.put(key, value);
    }

    public void putSerializable(String key, Serializable value) {
        this.values.put(key, value);
    }

    public void putShort(String key, short value) {
        this.values.put(key, Short.valueOf(value));
    }

    public void putShortArray(String key, short[] value) {
        this.values.put(key, value);
    }

    public void putString(String key, String value) {
        this.values.put(key, value);
    }

    public void putStringArray(String key, String[] value) {
        this.values.put(key, value);
    }

    public void putStringArrayList(String key, ArrayList<String> value) {
        this.values.put(key, value);
    }

    public void remove(String key) {
        this.values.remove(key);
    }

    public boolean setAllowFds(boolean allowFileDescriptors) {
        boolean previousValue = this.allowFileDescriptors;
        this.allowFileDescriptors = allowFileDescriptors;
        return previousValue;
    }

    public void setAnimationProvider(PageAnimationOptions animationOptions) {
        this.animationOptions = animationOptions;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public void setFlags(int flags, int pageCount) {
        this.flags = flags;
        this.pageCount = pageCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public void setMaxCountID(String maxCountId) {
        this.maxCountId = maxCountId;
    }

    public void setNeedPushAnimation(boolean pushAnimationNeeded) {
        this.pushAnimationNeeded = pushAnimationNeeded;
    }

    public void setTransitionAnimationStyle(int transitionAnimationStyle) {
        this.transitionAnimationStyle = transitionAnimationStyle;
    }

    public void setUniqueID(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public int size() {
        return this.values.size();
    }

    @Override
    public String toString() {
        StringBuilder description = new StringBuilder("PageBundle{action='");
        description.append(this.action);
        description.append("', packageName='");
        description.append(this.packageName);
        description.append("', mMap=");
        description.append(this.values);
        description.append(", mHasFds=");
        description.append(this.hasFileDescriptors);
        description.append(", mFdsKnown=");
        description.append(this.fileDescriptorsKnown);
        description.append(", mAllowFds=");
        description.append(this.allowFileDescriptors);
        description.append(", mFlags=");
        description.append(this.flags);
        description.append(", mPageCnt=");
        description.append(this.pageCount);
        description.append(", mUniqueID='");
        description.append(this.uniqueId);
        description.append("', mMaxCount=");
        description.append(this.maxCount);
        description.append(", mMaxCountID='");
        description.append(this.maxCountId);
        description.append("', ajxurl=");
        description.append(this.values == null ? "null" : getString("url"));
        description.append(", ajxdata=");
        description.append(this.values == null ? "null" : getString("jsData"));
        description.append(", mAnimationProvider=");
        description.append(this.animationOptions);
        description.append('}');
        return description.toString();
    }
}
