package org.moera.node.data;

public enum UpgradeType {

    UPDATE_SIGNATURE,  /* 0 */
    JSON_BODY,         /* 1 */
    UPDATE_DIGEST,     /* 2 */
    @Deprecated
    PROFILE_SUBSCRIBE, /* 3 */
    AVATAR_DOWNLOAD,   /* 4 */
    GENDER_DOWNLOAD,   /* 5 */
    PROFILE_DOWNLOAD   /* 6 */

}
