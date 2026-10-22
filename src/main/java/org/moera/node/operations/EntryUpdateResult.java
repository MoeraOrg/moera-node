package org.moera.node.operations;

import org.moera.node.data.Entry;

public record EntryUpdateResult<T extends Entry>(T entry, MediaPostingIds mediaPostingIds) {
}
