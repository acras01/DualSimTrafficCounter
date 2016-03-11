package ua.od.acros.dualsimtrafficcounter.utils;

public class BlackListItem {
    public String number;
    public boolean checked;

    public BlackListItem(String number, boolean checked) {
        this.number = number;
        this.checked = checked;
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
