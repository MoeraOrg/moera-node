package org.moera.node.model;

import org.moera.lib.node.types.SearchHistoryInfo;
import org.moera.node.data.SearchHistory;
import org.moera.node.util.Util;

public class SearchHistoryInfoUtil {

    public static SearchHistoryInfo build(SearchHistory searchHistory) {
        SearchHistoryInfo info = new SearchHistoryInfo();
        info.setQuery(searchHistory.getQuery());
        info.setCreatedAt(Util.toEpochSecond(searchHistory.getCreatedAt()));
        return info;
    }

}
