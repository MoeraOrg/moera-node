package org.moera.node.config;

public class DebugConfig {

    private boolean mockNetworkLatency = false;
    private boolean logSlowRequests = false;
    private long slowRequestDuration = 500;

    public boolean isMockNetworkLatency() {
        return mockNetworkLatency;
    }

    public void setMockNetworkLatency(boolean mockNetworkLatency) {
        this.mockNetworkLatency = mockNetworkLatency;
    }

    public boolean isLogSlowRequests() {
        return logSlowRequests;
    }

    public void setLogSlowRequests(boolean logSlowRequests) {
        this.logSlowRequests = logSlowRequests;
    }

    public long getSlowRequestDuration() {
        return slowRequestDuration;
    }

    public void setSlowRequestDuration(long slowRequestDuration) {
        this.slowRequestDuration = slowRequestDuration;
    }

}
