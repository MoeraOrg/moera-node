package org.moera.node.liberin.model;

import java.util.HashMap;
import java.util.Map;

import org.moera.node.data.SearchEngineStatistics;
import org.moera.node.liberin.Liberin;
import org.moera.node.util.Util;

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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);

        Map<String, Object> click = new HashMap<>();
        click.put("nodeName", this.click.getNodeName());
        click.put("engine", this.click.getEngine());
        click.put("ownerName", this.click.getOwnerName());
        click.put("postingId", this.click.getPostingId());
        click.put("commentId", this.click.getCommentId());
        click.put("mediaId", this.click.getMediaId());
        click.put("heading", this.click.getHeading());
        click.put("clickedAt", Util.toEpochSecond(this.click.getClickedAt()));

        model.put("click", click);
    }

}
