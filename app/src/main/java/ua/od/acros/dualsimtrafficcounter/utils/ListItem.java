package ua.od.acros.dualsimtrafficcounter.utils;

public class ListItem {
    public String name, number;
    public boolean checked;

    public ListItem(String name, String number, boolean checked) {
        this.name = name;
        this.number = number;
        this.checked = checked;
    }

    public ListItem(String number, boolean checked) {
        this.name = "";
        this.number = number;
        this.checked = checked;
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
