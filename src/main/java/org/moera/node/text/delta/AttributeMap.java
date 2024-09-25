package org.moera.node.text.delta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AttributeMap extends HashMap<String, Object> {

    public AttributeMap(AttributeMap b) {
        super(b);
    }

    public AttributeMap() {
    }

    public AttributeMap(Map<String, Object> input) {
        super(input);
    }

    public static AttributeMap of(String key, Object value) {
        Map<String, Object> temp = new HashMap<>();
        temp.put(key, value);
        return new AttributeMap(temp);
    }

    public static AttributeMap of(String key0, Object value0, String key1, Object value1) {
        Map<String, Object> temp = new HashMap<>();
        temp.put(key0, value0);
        temp.put(key1, value1);
        return new AttributeMap(temp);
    }

    public static AttributeMap of(
            String key0, Object value0, String key1, Object value1, String key2, Object value2) {
        Map<String, Object> temp = new HashMap<>();
        temp.put(key0, value0);
        temp.put(key1, value1);
        temp.put(key2, value2);
        return new AttributeMap(temp);
    }

    public static AttributeMap of(
            String key0,
            Object value0,
            String key1,
            Object value1,
            String key2,
            Object value2,
            String key3,
            Object value3) {
        Map<String, Object> temp = new HashMap<>();
        temp.put(key0, value0);
        temp.put(key1, value1);
        temp.put(key2, value2);
        temp.put(key3, value3);
        return new AttributeMap(temp);
    }

    /**
     * Union of attributes, where conflict are overridden by second argument
     *
     * @param a        an attribute map
     * @param b        an attribute map
     * @param keepNull if inputB has {@code null} key, keep it
     * @return the composed attribute map
     */
    static AttributeMap compose(AttributeMap a, AttributeMap b, boolean keepNull) {
        AttributeMap aN = a != null ? a : new AttributeMap();
        AttributeMap bN = b != null ? b : new AttributeMap();
        AttributeMap attributes = new AttributeMap(bN);
        if (!keepNull) {
            Set<String> keysToRemove = new HashSet<>();
            attributes.forEach(
                (key, value) -> {
                    if (value == null) {
                        keysToRemove.add(key);
                    }
                });
            keysToRemove.forEach(attributes::remove);
        }

        for (String key : aN.keySet()) {
            if (aN.get(key) != null && !bN.containsKey(key)) {
                attributes.put(key, aN.get(key));
            }
        }

        return attributes.isEmpty() ? null : new AttributeMap(attributes);
    }

    static AttributeMap compose(AttributeMap a, AttributeMap b) {
        return compose(a, b, false);
    }

    /**
     * @param a
     * @param b
     * @return
     */
    static AttributeMap diff(AttributeMap a, AttributeMap b) {
        AttributeMap aN = a != null ? a : new AttributeMap();
        AttributeMap bN = b != null ? b : new AttributeMap();
        AttributeMap attributes = new AttributeMap();
        Set<String> keys = new HashSet<>(aN.keySet());
        keys.addAll(bN.keySet());
        for (String k : keys) {
            if (!Objects.equals(aN.get(k), bN.get(k))) {
                attributes.put(k, bN.get(k));
            }
        }
        return attributes.isEmpty() ? null : attributes;
    }

    /**
     * @param attr
     * @param base
     * @return
     */
    static AttributeMap invert(AttributeMap attr, AttributeMap base) {
        AttributeMap attrN = attr != null ? attr : new AttributeMap();
        AttributeMap baseN = base != null ? base : new AttributeMap();
        AttributeMap baseInverted = new AttributeMap();
        for (String k : baseN.keySet()) {
            if (!baseN.get(k).equals(attrN.get(k)) && attrN.containsKey(k)) {
                baseInverted.put(k, baseN.get(k));
            }
        }
        for (String k : attrN.keySet()) {
            if (!Objects.equals(attrN.get(k), baseN.get(k)) && !baseN.containsKey(k)) {
                baseInverted.put(k, null);
            }
        }
        return baseInverted;
    }

    static AttributeMap transform(AttributeMap a, AttributeMap b) {
        return transform(a, b, false);
    }

    static AttributeMap transform(AttributeMap a, AttributeMap b, boolean priority) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return null;
        }
        if (!priority) {
            return b; // b simply overwrites us without priority
        }
        AttributeMap attributes = new AttributeMap();
        for (String k : b.keySet()) {
            if (!a.containsKey(k)) {
                attributes.put(k, b.get(k));
            }
        }
        return attributes.isEmpty() ? null : attributes;
    }

    public AttributeMap copy() {
        return new AttributeMap(this);
    }

}
