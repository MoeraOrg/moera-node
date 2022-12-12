package org.moera.node.data;

public interface ContactRelated {

    String getRemoteFullName();

    void setRemoteFullName(String remoteFullName);

    String getRemoteGender();

    void setRemoteGender(String remoteGender);

    MediaFile getRemoteAvatarMediaFile();

    void setRemoteAvatarMediaFile(MediaFile remoteAvatarMediaFile);

    String getRemoteAvatarShape();

    void setRemoteAvatarShape(String remoteAvatarShape);

    Contact getContact();

    void setContact(Contact contact);

}
