package ua.od.acros.dualsimtrafficcounter.receivers;

public class NewOutgoingCallEvent {
    public final String number;

    public NewOutgoingCallEvent(String number) {
        this.number = number;
    }
}
