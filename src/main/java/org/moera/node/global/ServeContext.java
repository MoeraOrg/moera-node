package org.moera.node.global;

import org.moera.node.config.DirectServeConfig;
import org.moera.node.option.Options;

public record ServeContext(DirectServeConfig directServeConfig, Options options) {
}
