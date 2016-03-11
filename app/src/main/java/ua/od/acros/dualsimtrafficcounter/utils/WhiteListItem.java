package ua.od.acros.dualsimtrafficcounter.utils;

public class WhiteListItem {
    public String name, number;
    public boolean checked;

    public WhiteListItem(String name, String number, boolean checked) {
        this.name = name;
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
