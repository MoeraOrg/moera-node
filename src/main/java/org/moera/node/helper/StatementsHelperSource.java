package org.moera.node.helper;

import java.io.IOException;

import com.github.jknack.handlebars.Handlebars.SafeString;
import com.github.jknack.handlebars.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@HelperSource
public class StatementsHelperSource {

    private static Logger log = LoggerFactory.getLogger(StatementsHelperSource.class);

    public CharSequence assign(String variableName, Options options) throws IOException {
        CharSequence finalValue = options.apply(options.fn);
        finalValue = finalValue instanceof SafeString ? finalValue : new SafeString(finalValue.toString().trim());
        options.data(variableName, finalValue);
        return "";
    }

    public CharSequence assignIeq(String variableName, Object value1, Object value2, Options options)
            throws IOException {
        boolean condition = HelperUtils.intArg(0, value1) == HelperUtils.intArg(1, value2);
        options.data(variableName, condition);
        return "";
    }

    public CharSequence assignIne(String variableName, Object value1, Object value2, Options options)
            throws IOException {
        boolean condition = HelperUtils.intArg(0, value1) != HelperUtils.intArg(1, value2);
        options.data(variableName, condition);
        return "";
    }

    public CharSequence assignDivisive(String variableName, Object value1, Object value2, Options options)
            throws IOException {
        boolean condition = HelperUtils.intArg(0, value1) % HelperUtils.intArg(1, value2) == 0;
        options.data(variableName, condition);
        return "";
    }

    public CharSequence not(Object value) {
        return HelperUtils.boolResult(!HelperUtils.boolArg(value));
    }

    public CharSequence and(Object left, Object right) {
        return HelperUtils.boolResult(HelperUtils.boolArg(left) && HelperUtils.boolArg(right));
    }

    public CharSequence or(Object left, Object right) {
        return HelperUtils.boolResult(HelperUtils.boolArg(left) || HelperUtils.boolArg(right));
    }

    public CharSequence neg(Object value) {
        return Long.toString(-HelperUtils.intArg(1, value));
    }

    public CharSequence ifset(Object value, Options options) throws IOException {
        return value != null ? options.apply(options.fn) : options.apply(options.inverse);
    }

    public CharSequence ifeq(Object value1, Object value2, Options options) throws IOException {
        value1 = value1 instanceof SafeString ? value1.toString() : value1;
        value2 = value2 instanceof SafeString ? value2.toString() : value2;
        boolean condition = value1 == null && value2 == null || value1 != null && value1.equals(value2);
        return condition ? options.apply(options.fn) : options.apply(options.inverse);
    }

    public CharSequence ifne(Object value1, Object value2, Options options) throws IOException {
        value1 = value1 instanceof SafeString ? value1.toString() : value1;
        value2 = value2 instanceof SafeString ? value2.toString() : value2;
        boolean condition = value1 == null && value2 != null || value1 != null && !value1.equals(value2);
        return condition ? options.apply(options.fn) : options.apply(options.inverse);
    }

    public CharSequence ifieq(Object value1, Object value2, Options options) throws IOException {
        boolean condition = HelperUtils.intArg(0, value1) == HelperUtils.intArg(1, value2);
        return condition ? options.apply(options.fn) : options.apply(options.inverse);
    }

    public CharSequence ifine(Object value1, Object value2, Options options) throws IOException {
        boolean condition = HelperUtils.intArg(0, value1) != HelperUtils.intArg(1, value2);
        return condition ? options.apply(options.fn) : options.apply(options.inverse);
    }

    public CharSequence ifgt(Object value1, Object value2, Options options) throws IOException {
        boolean condition = HelperUtils.intArg(0, value1) > HelperUtils.intArg(1, value2);
        return condition ? options.apply(options.fn) : options.apply(options.inverse);
    }

    public CharSequence iflt(Object value1, Object value2, Options options) throws IOException {
        boolean condition = HelperUtils.intArg(0, value1) < HelperUtils.intArg(1, value2);
        return condition ? options.apply(options.fn) : options.apply(options.inverse);
    }

    public CharSequence ifge(Object value1, Object value2, Options options) throws IOException {
        boolean condition = HelperUtils.intArg(0, value1) >= HelperUtils.intArg(1, value2);
        return condition ? options.apply(options.fn) : options.apply(options.inverse);
    }

    public CharSequence ifle(Object value1, Object value2, Options options) throws IOException {
        boolean condition = HelperUtils.intArg(0, value1) <= HelperUtils.intArg(1, value2);
        return condition ? options.apply(options.fn) : options.apply(options.inverse);
    }

    public CharSequence ifbtw(Object value1, Object value2, Object value3, Options options) throws IOException {
        boolean condition = HelperUtils.intArg(0, value1) >= HelperUtils.intArg(1, value2)
                && HelperUtils.intArg(0, value1) < HelperUtils.intArg(2, value3);
        return condition ? options.apply(options.fn) : options.apply(options.inverse);
    }

}
