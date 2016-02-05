package ua.od.acros.dualsimtrafficcounter.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

import ua.od.acros.dualsimtrafficcounter.R;

public class MyArrayAdapter extends BaseAdapter {


    public List<String> names, numbers, list;
    public LayoutInflater inflater;

    public MyArrayAdapter(Context context, List<String> names,
                          List<String> numbers, List<String> list) {
        super();
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.names = names;
        this.numbers = numbers;
        if (list != null)
            this.list = list;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    public void toggleChecked(int position) {
        if (list.contains(numbers.get(position)))
            list.remove(numbers.get(position));
        else
            list.add(numbers.get(position));
        notifyDataSetChanged();
    }

    public List<String> getCheckedItems(){
        return list;
    }

    public static class ViewHolder {
        CheckBox checkBox;
        TextView txtViewName;
        TextView txtViewNumber;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.list_row, null);
            holder.checkBox = (CheckBox) convertView.findViewById(R.id.checkBox);
            holder.txtViewName = (TextView) convertView.findViewById(R.id.name);
            holder.txtViewNumber = (TextView) convertView.findViewById(R.id.number);
            convertView.setTag(holder);
        }
        else
            holder=(ViewHolder)convertView.getTag();

        holder.checkBox.setChecked(list.contains(numbers.get(position)));
        holder.txtViewName.setText(names.get(position));
        holder.txtViewNumber.setText(numbers.get(position));

        return convertView;
    }

    // кол-во элементов
    @Override
    public int getCount() {
        return numbers.size();
    }

    // элемент по позиции
    @Override
    public Object getItem(int position) {
        return numbers.get(position);
    }

    // id по позиции
    @Override
    public long getItemId(int position) {
        return position;
    }
}
