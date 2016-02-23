package ua.od.acros.dualsimtrafficcounter.receivers;

import android.os.Bundle;

public class NewOutgoingCallEvent {
    public final Bundle bundle;

    public NewOutgoingCallEvent(Bundle bundle) {
        this.bundle = bundle;
    }
}
