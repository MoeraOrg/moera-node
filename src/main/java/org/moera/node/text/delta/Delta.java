package org.moera.node.text.delta;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.moera.node.text.delta.Op.Type.DELETE;
import static org.moera.node.text.delta.Op.Type.INSERT;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@JsonInclude(value = NON_NULL)
public class Delta {

    @JsonProperty("ops")
    final OpList ops;

    public Delta(OpList ops) {
        if (ops == null) {
            throw new IllegalArgumentException("Ops cannot be null, use Delta() for empty ops");
        }
        this.ops = ops;
    }

    public Delta() {
        this(new OpList());
    }

    public Delta(Delta delta) {
        this(delta.ops);
    }

    public OpList getOps() {
        return new OpList(ops);
    }

    public Delta insert(Object arg, AttributeMap attributes) {
        if (arg == null) {
            return this;
        }
        // 0x200b is NOT a white space character
        if (arg instanceof String && ((String) arg).isBlank()) {
            return this;
        }
        return push(Op.insert(arg, attributes));
    }

    public Delta insert(String arg) {
        return insert(arg, null);
    }

    public Delta insert(Map<String, Object> object) {
        return insert(object, null);
    }

    public Delta delete(int length) {
        if (length <= 0) {
            return this;
        }
        return push(Op.delete(length));
    }

    public Delta retain(int length, AttributeMap attributes) {
        if (length <= 0) {
            return this;
        }
        return push(Op.retain(length, attributes));
    }

    public Delta retain(int length) {
        return retain(length, null);
    }

    // TODO : P1 - TEST
    public Delta push(Op newOp) {
        if (ops.isEmpty()) {
            ops.add(newOp);
            return this;
        }
        int index = ops.size();
        Op lastOp = ops.get(index - 1);
        newOp = newOp.copy();
        if (newOp.isDelete() && lastOp.isDelete()) {
            ops.set(index - 1, Op.delete(lastOp.length() + newOp.length()));
            return this;
        }
        // Since it does not matter if we insert before or after deleting at the same index,
        // always prefer to insert first
        if (lastOp.isDelete() && newOp.isInsert()) {
            index -= 1;
            if (index == 0) {
                ops.insertFirst(newOp);
                return this;
            }
            lastOp = ops.get(index - 1);
        }

        if (Objects.equals(newOp.attributes(), lastOp.attributes())) {
            if (newOp.isInsert() && lastOp.isInsert()) {
                if (newOp.arg() instanceof String && lastOp.arg() instanceof String) {
                    final Op mergedOp =
                            Op.insert(lastOp.argAsString() + newOp.argAsString(), newOp.attributes());
                    this.ops.set(index - 1, mergedOp);
                    return this;
                }
            }
            if (lastOp.isRetain() && newOp.isRetain()) {
                final Op mergedOp = Op.retain(lastOp.length() + newOp.length(), newOp.attributes());
                ops.set(index - 1, mergedOp);
                return this;
            }
        }
        if (index == ops.size()) {
            ops.add(newOp);
        } else {
            ops.add(index, newOp);
        }
        return this;
    }

    public Delta chop() {
        if (ops.isEmpty()) {
            return this;
        }
        Op lastOp = ops.get(ops.size() - 1);
        if (lastOp.isRetain() && lastOp.attributes() == null) {
            ops.removeLast();
        }
        return this;
    }

    public OpList filter(Predicate<Op> predicate) {
        return ops.filter(predicate);
    }

    public void forEach(Consumer<Op> consumer) {
        ops.forEach(consumer);
    }

    public <T> List<T> map(Function<Op, T> mapper) {
        return ops.stream().map(mapper).collect(Collectors.toList());
    }

    public List<Op>[] partition(Predicate<Op> predicate) {
        final OpList passed = new OpList();
        final OpList failed = new OpList();
        forEach(
            op -> {
                if (predicate.test(op)) {
                    passed.add(op);
                } else {
                    failed.add(op);
                }
            });
        return new OpList[]{passed, failed};
    }

    public <T> T reduce(T initialValue, BiFunction<T, Op, T> accumulator) {
        return ops.stream().reduce(initialValue, accumulator, (value1, value2) -> value2);
    }

    public int changeLength() {
        return reduce(
            0,
            (length, op) -> {
                if (op.isInsert()) {
                    return length + op.length();
                }
                if (op.isDelete()) {
                    return length - op.length();
                }
                return length;
            });
    }

    public int length() {
        return reduce(0, (length, op) -> length + op.length());
    }

    public Delta slice(int start) {
        return slice(start, Integer.MAX_VALUE);
    }

    public Delta compose(Delta other) {
        final OpList.Iterator it = ops.iterator();
        final OpList.Iterator otherIt = other.ops.iterator();

        final OpList combined = new OpList();
        final Op firstOther = otherIt.peek();
        if (firstOther != null && firstOther.isRetain() && firstOther.attributes() == null) {
            int firstLeft = firstOther.length();
            while (it.peekType() == INSERT && it.peekLength() <= firstLeft) {
                firstLeft -= it.peekLength();
                combined.add(it.next());
            }
            if (firstOther.length() - firstLeft > 0) {
                otherIt.next(firstOther.length() - firstLeft);
            }
        }
        final Delta delta = new Delta(combined);

        while (it.hasNext() || otherIt.hasNext()) {
            if (otherIt.peekType() == INSERT) {
                delta.push(otherIt.next());
            } else if (it.peekType() == DELETE) {
                delta.push(it.next());
            } else {
                final int length = Math.min(it.peekLength(), otherIt.peekLength());
                final Op thisOp = it.next(length);
                final Op otherOp = otherIt.next(length);

                if (otherOp.isRetain()) {
                    Op newOp;
                    // Preserve null when composing with a retain, otherwise remove it for inserts
                    AttributeMap attributes =
                            AttributeMap.compose(thisOp.attributes(), otherOp.attributes(), thisOp.isRetain());
                    if (thisOp.isRetain()) {
                        newOp = Op.retain(length, attributes);
                    } else {
                        newOp = Op.insert(thisOp.arg(), attributes);
                    }
                    delta.push(newOp);
                    // Optimization if rest of other is just retain
                    if (!otherIt.hasNext() && delta.ops.get(delta.ops.size() - 1).equals(newOp)) {
                        final Delta rest = new Delta(it.rest());
                        return delta.concat(rest).chop();
                    }
                } else if (otherOp.isDelete() && thisOp.isRetain()) {
                    delta.push(otherOp);
                }
            }
        }
        return delta.chop();
    }

    public void eachLine(BiFunction<Delta, AttributeMap, Boolean> predicate, String newLine) {
        final OpList.Iterator it = ops.iterator();
        Delta line = new Delta();
        while (it.hasNext()) {
            if (it.peekType() != INSERT) {
                return;
            }
            final Op thisOp = it.peek();
            final int start = thisOp.length() - it.peekLength();
            final int index =
                    thisOp.isTextInsert() ? thisOp.argAsString().indexOf(newLine, start) - start : -1;
            if (index < 0) {
                line.push(it.next());
            } else if (index > 0) {
                line.push(it.next(index));
            } else {
                if (!predicate.apply(line, it.next(1).attributes())) {
                    return;
                }
                line = new Delta();
            }
        }
        if (line.length() > 0) {
            predicate.apply(line, null);
        }
    }

    public void eachLine(BiFunction<Delta, AttributeMap, Boolean> applyFunction) {
        eachLine(applyFunction, "\n");
    }

    public Delta invert(Delta base) {
        final Delta inverted = new Delta();
        reduce(
            0,
            (Integer baseIndex, Op op) -> {
                if (op.isInsert()) {
                    inverted.delete(op.length());
                } else if (op.isRetain() && op.attributes() == null) {
                    inverted.retain(op.length());
                    return baseIndex + op.length();
                } else if (op.isDelete() || op.isRetain() && op.hasAttributes()) {
                    int length = op.length();
                    final Delta slice = base.slice(baseIndex, baseIndex + length);
                    slice.forEach(
                        baseOp -> {
                            if (op.isDelete()) {
                                inverted.push(baseOp);
                            } else if (op.isRetain() && op.hasAttributes()) {
                                inverted.retain(
                                        baseOp.length(), AttributeMap.invert(op.attributes(), baseOp.attributes()));
                            }
                        });
                    return baseIndex + length;
                }
                return baseIndex;
            });
        return inverted.chop();
    }

    public Delta slice(int start, int end) {
        final OpList ops = new OpList();
        final OpList.Iterator it = this.ops.iterator();
        int index = 0;
        while (index < end && it.hasNext()) {
            Op nextOp;
            if (index < start) {
                nextOp = it.next(start - index);
            } else {
                nextOp = it.next(end - index);
                ops.add(nextOp);
            }
            index += nextOp.length();
        }
        return new Delta(ops);
    }

    public Delta concat(Delta other) {
        final Delta delta = new Delta(new OpList(ops));
        if (!other.ops.isEmpty()) {
            delta.push(other.ops.get(0));
            delta.ops.addAll(other.ops.subList(1, other.ops.size()));
        }
        return delta;
    }

    public String plainText() {
        StringBuilder builder = new StringBuilder();
        for (Op op : ops) {
            if (op.isTextInsert()) {
                builder.append(op.argAsString());
            } else {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(ops);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Delta delta = (Delta) o;
        return Objects.equals(ops, delta.ops);
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        try {
            return writer.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "Error while generating json:\n" + e.getMessage();
        }
    }

}
