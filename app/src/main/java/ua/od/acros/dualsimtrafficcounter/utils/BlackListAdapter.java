package ua.od.acros.dualsimtrafficcounter.utils;

import android.graphics.Paint;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ua.od.acros.dualsimtrafficcounter.R;

public class BlackListAdapter extends RecyclerView.Adapter<BlackListAdapter.ViewHolder> {

    private List<BlackListItem> mList;

    // класс view holder-а с помощью которого мы получаем ссылку на каждый элемент
    // отдельного пункта списка
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // наш пункт состоит только из одного TextView
        public AppCompatCheckBox checkBox;
        public TextView textView;

        public ViewHolder(View v) {
            super(v);
            checkBox = (AppCompatCheckBox) v.findViewById(R.id.checkBox);
            textView = (TextView) v.findViewById(R.id.textView);
        }
    }

    // Конструктор
    public BlackListAdapter(List<BlackListItem> list) {
        this.mList = list;
    }

    // Создает новые views (вызывается layout manager-ом)
    @Override
    public BlackListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.black_list_row, parent, false);

        // тут можно программно менять атрибуты лэйаута (size, margins, paddings и др.)
        ViewHolder viewHolder = new ViewHolder(v);
        final TextView textView = viewHolder.textView;
        viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                BlackListItem item = (BlackListItem) buttonView.getTag();
                item.setChecked(isChecked);
                if (isChecked)
                    textView.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
                else
                    textView.setPaintFlags(0);

            }
        });
        return viewHolder;
    }

    // Заменяет контент отдельного view (вызывается layout manager-ом)
    @Override
    public void onBindViewHolder(BlackListAdapter.ViewHolder holder, int position) {
        holder.textView.setText(mList.get(position).getNumber());
        boolean checked = mList.get(position).isChecked();
        holder.checkBox.setTag(mList.get(position));
        holder.checkBox.setChecked(checked);
        if (checked)
            holder.textView.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
        else
            holder.textView.setPaintFlags(0);
    }

    // Возвращает размер данных (вызывается layout manager-ом)
    @Override
    public int getItemCount() {
        if (mList != null)
            return mList.size();
        else
            return 0;
    }


    public ArrayList<String> getCheckedItems(){
        ArrayList<String> list = new ArrayList<>();
        for (BlackListItem item : mList)
        if (item.isChecked())
            list.add(item.getNumber());
        return list;
    }

    // элемент по позиции
    public Object getItem(int position) {
        return mList.get(position);
    }

    // id по позиции
    @Override
    public long getItemId(int position) {
        return position;
    }
}