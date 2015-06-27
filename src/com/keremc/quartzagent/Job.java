package com.keremc.quartzagent;

/**
 * Represents a job running on the quartz scheduler.
 */
public class Job {
    private boolean running;
    private String name;
    private String path;

    public Job(boolean running, String name, String path) {
        this.running = running;
        this.name = name;
        this.path = path;
    }


    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public String toString() {
        return "Job=" + name + ",running=" + running + ",path=" + path;
    }

}
