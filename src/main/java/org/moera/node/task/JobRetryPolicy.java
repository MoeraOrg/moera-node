package org.moera.node.task;

import java.time.Duration;

public interface JobRetryPolicy {

    boolean tryAgain();

    Duration waitTime();

}
