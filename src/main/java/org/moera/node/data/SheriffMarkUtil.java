package org.moera.node.data;

import org.moera.lib.node.types.SheriffMark;

public class SheriffMarkUtil {

    public static SheriffMark build(String sheriffName) {
        SheriffMark sheriffMark = new SheriffMark();
        sheriffMark.setSheriffName(sheriffName);
        return sheriffMark;
    }

}
