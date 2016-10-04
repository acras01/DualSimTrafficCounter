package ua.od.acros.dualsimtrafficcounter.utils;

import org.joda.time.DateTime;

public class DataResetObject {
    int period;
    DateTime dt;

    DataResetObject(int period, DateTime dt) {
        this.period = period;
        this.dt = dt;
    }

    public DateTime getDate() {
        return dt;
    }

    public int getPeriod() {
        return period;
    }
}
