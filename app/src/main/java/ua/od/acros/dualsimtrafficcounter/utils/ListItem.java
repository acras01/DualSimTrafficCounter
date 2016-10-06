package ua.od.acros.dualsimtrafficcounter.utils;

import android.net.Uri;

public class ListItem {
    public Uri icon;
    public String name, number;
    public boolean checked;

    public ListItem(Uri icon, String name, String number, boolean checked) {
        this.icon = icon;
        this.name = name;
        this.number = number;
        this.checked = checked;
    }

    public ListItem(String number, boolean checked) {
        this.name = "";
        this.number = number;
        this.checked = checked;
    }

    public Uri getIcon(){
        return icon;
    }

    public String getName(){
        return name;
    }

    public String getNumber(){
        return number;
    }

    public boolean isChecked(){
        return checked;
    }

    public void setChecked(boolean checked){
        this.checked = checked;
    }
}
