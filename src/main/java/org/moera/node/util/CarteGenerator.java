package org.moera.node.util;

import org.moera.lib.node.types.Scope;

public interface CarteGenerator {

    String generate(String nodeName, Scope clientScope);

}
