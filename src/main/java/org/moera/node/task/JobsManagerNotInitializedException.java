package org.moera.node.task;

public class JobsManagerNotInitializedException extends RuntimeException {

    public JobsManagerNotInitializedException() {
        super("Jobs manager is not initialized yet");
    }

}
