package org.moera.node.picker;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.moera.node.model.Result;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Picker extends Task {

    private static Logger log = LoggerFactory.getLogger(Picker.class);

    private String remoteNodeName;
    private BlockingQueue<Pick> queue = new LinkedBlockingQueue<>();
    private boolean stopped = false;
    private PickerPool pool;

    public Picker(PickerPool pool, String remoteNodeName) {
        this.pool = pool;
        this.remoteNodeName = remoteNodeName;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void put(@NotNull Pick pick) throws InterruptedException {
        queue.put(pick);
    }

    @Override
    public void run() {
        while (!stopped) {
            Pick pick;
            try {
                pick = queue.poll(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                continue;
            }
            if (pick == null) {
                stopped = true;
                if (!queue.isEmpty()) { // queue may receive content before the previous statement
                    stopped = false;
                }
            } else {
                download(pick);
            }
        }
        pool.deletePicker(nodeId, remoteNodeName);
    }

    private void download(Pick pick) {
        log.info("Downloading from node '{}', postingId = {}", remoteNodeName, pick.getRemotePostingId());

        try {
            succeeded(Result.OK); // TODO
        } catch (Exception e) {
            error(e);
        }
    }

    private void succeeded(Result result) {
        initLoggingDomain();
        if (result.isOk()) {
            log.info("Posting downloaded successfully");
        } else {
            log.info("Remote node returned error: {}", result.getMessage());
        }
    }

    private void error(Throwable e) {
        failed(e.getMessage());
    }

    private void failed(String message) {
        initLoggingDomain();
        log.error(message);
    }

}
