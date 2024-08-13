package org.moera.node.util;

import org.moera.node.auth.Scope;

public interface CarteGenerator {

    String generate(String nodeName, Scope clientScope);

}
