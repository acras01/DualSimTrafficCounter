package ua.od.acros.dualsimtrafficcounter.events;

public class DurationCallEvent {
    public final int sim;
    public final long duration;

    public DurationCallEvent(int sim, long duration) {
        this.sim = sim;
        this.duration = duration;
    }
}
