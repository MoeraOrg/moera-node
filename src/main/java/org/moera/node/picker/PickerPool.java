package org.moera.node.picker;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;

import org.moera.node.global.RequestContext;
import org.moera.node.task.TaskAutowire;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class PickerPool {

    private ConcurrentMap<PickingDirection, Picker> pickers = new ConcurrentHashMap<>();

    @Inject
    @Qualifier("pickerTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    private RequestContext requestContext;

    public void pick(Pick pick) {
        while (true) {
            Picker picker;
            do {
                picker = pickers.computeIfAbsent(
                        new PickingDirection(requestContext.nodeId(), pick.getRemoteNodeName()),
                        d -> createPicker(d.getNodeName()));
            } while (picker.isStopped());
            try {
                picker.put(pick);
            } catch (InterruptedException e) {
                continue;
            }
            break;
        }
    }

    private Picker createPicker(String nodeName) {
        Picker sender = new Picker(this, nodeName);
        taskAutowire.autowire(sender);
        taskExecutor.execute(sender);
        return sender;
    }

    void deletePicker(UUID nodeId, String nodeName) {
        pickers.remove(new PickingDirection(nodeId, nodeName));
    }

}
