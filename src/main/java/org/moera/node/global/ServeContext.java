package org.moera.node.global;

import org.moera.node.media.DirectServeOperations;
import org.moera.node.option.Options;

public record ServeContext(DirectServeOperations directServeOperations, Options options) {
}
