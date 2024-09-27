package org.moera.node.text.delta.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(value = NON_EMPTY)
public class OpList extends ArrayList<Op> {

    public OpList(List<Op> ops) {
        super(ops);
    }

    public OpList() {
    }

    public void insertFirst(Op element) {
        add(0, element);
    }

    public void removeLast() {
        remove(size() - 1);
    }

    public OpList filter(Predicate<Op> predicate) {
        return new OpList(stream().filter(predicate).collect(toList()));
    }

    @Override
    public Iterator iterator() {
        return new Iterator(this);
    }

    static class Iterator implements java.util.Iterator<Op> {

        private final OpList ops;

        private int index = 0;
        private int offset = 0;

        Iterator(OpList ops) {
            this.ops = ops;
        }

        public Op next(int length) {
            if (index >= ops.size()) {
                return Op.retain(Integer.MAX_VALUE, null);
            }

            final Op nextOp = ops.get(index);
            final int offset = this.offset;
            final int opLength = nextOp.length();

            if (length >= opLength - offset) {
                length = opLength - offset;
                this.index += 1;
                this.offset = 0;
            } else {
                this.offset += length;
            }

            if (nextOp.isDelete()) {
                return Op.delete(length);
            } else {
                Op retOp;
                if (nextOp.isRetain()) {
                    retOp = Op.retain(length, nextOp.attributes());
                } else if (nextOp.isTextInsert()) {
                    retOp = Op.insert(nextOp.argAsString().substring(offset, offset + length), nextOp.attributes());
                } else {
                    retOp = Op.insert(nextOp.arg(), nextOp.attributes());
                }
                return retOp;
            }
        }

        public Op peek() {
            if (index >= ops.size()) {
                return null;
            }
            return ops.get(index);
        }

        public int peekLength() {
            if (index >= ops.size()) {
                return Integer.MAX_VALUE;
            }
            return ops.get(index).length() - offset;
        }

        public Op.Type peekType() {
            if (index >= ops.size()) {
                return Op.Type.RETAIN;
            }
            return ops.get(index).type();
        }

        public OpList rest() {
            if (!hasNext()) {
                return new OpList();
            }
            if (offset == 0) {
                return new OpList(ops.subList(index, ops.size()));
            }
            final int offset = this.offset;
            final int index = this.index;
            final Op next = next();
            final OpList rest = new OpList(ops.subList(this.index, ops.size()));
            this.offset = offset;
            this.index = index;
            OpList returnList = new OpList(List.of(next));
            returnList.addAll(rest);
            return returnList;
        }

        @Override
        public boolean hasNext() {
            return this.peekLength() < Integer.MAX_VALUE;
        }

        @Override
        public Op next() {
            return next(Integer.MAX_VALUE);
        }
    }

}
