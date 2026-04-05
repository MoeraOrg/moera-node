package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.RemoteMediaDownloadFailedLiberin;
import org.moera.node.liberin.model.RemoteMediaDownloadedLiberin;
import org.moera.node.model.event.RemoteMediaDownloadFailedEvent;
import org.moera.node.model.event.RemoteMediaDownloadedEvent;

@LiberinReceptor
public class RemoteMediaReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void downloaded(RemoteMediaDownloadedLiberin liberin) {
        send(liberin, new RemoteMediaDownloadedEvent(
            liberin.getNodeName(), liberin.getMediaId(), liberin.getMediaInfo()
        ));
    }

    @LiberinMapping
    public void downloadFailed(RemoteMediaDownloadFailedLiberin liberin) {
        send(liberin, new RemoteMediaDownloadFailedEvent(
            liberin.getNodeName(), liberin.getMediaId(), liberin.getErrorCode(), liberin.getErrorMessage()
        ));
    }

}
