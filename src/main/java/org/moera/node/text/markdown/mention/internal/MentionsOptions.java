package org.moera.node.text.markdown.mention.internal;

import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSetter;
import org.jetbrains.annotations.NotNull;
import org.moera.node.api.naming.NamingCache;
import org.moera.node.text.markdown.mention.MentionsExtension;

class MentionsOptions implements MutableDataSetter {

    public final NamingCache namingCache;

    MentionsOptions(DataHolder options) {
        namingCache = MentionsExtension.NAMING_CACHE.get(options);
    }

    @NotNull
    @Override
    public MutableDataHolder setIn(@NotNull MutableDataHolder dataHolder) {
        dataHolder.set(MentionsExtension.NAMING_CACHE, namingCache);
        return dataHolder;
    }

}
