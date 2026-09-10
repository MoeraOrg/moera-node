package org.moera.node.liberin.model;

import org.moera.node.data.SearchEngineStatistics;
import org.moera.node.liberin.Liberin;

public class SearchEngineClickedLiberin extends Liberin {

    private SearchEngineStatistics click;

    public SearchEngineClickedLiberin(SearchEngineStatistics click) {
        this.click = click;
    }

    public SearchEngineStatistics getClick() {
        return click;
    }

    public void setClick(SearchEngineStatistics click) {
        this.click = click;
    }

}
