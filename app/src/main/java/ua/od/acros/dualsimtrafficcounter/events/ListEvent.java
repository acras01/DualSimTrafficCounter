package ua.od.acros.dualsimtrafficcounter.events;

import android.os.Bundle;

import java.util.ArrayList;

public class ListEvent {
    public boolean outgoing;
    public ArrayList<String> list;
    public Bundle bundle;

    public ListEvent(boolean outgoing, ArrayList<String> list, Bundle bundle) {
        this.outgoing = outgoing;
        this.list = list;
        this.bundle = bundle;
    }
}
