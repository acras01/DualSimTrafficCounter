package ua.od.acros.dualsimtrafficcounter.utils;

import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import java.util.ArrayList;

import ua.od.acros.dualsimtrafficcounter.R;

public class BlackListAdapter extends RecyclerView.Adapter<BlackListAdapter.ViewHolder> {

    private ArrayList<String> mList, mChecked;

    // класс view holder-а с помощью которого мы получаем ссылку на каждый элемент
    // отдельного пункта списка
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // наш пункт состоит только из одного TextView
        public CheckBox checkBox;

        public ViewHolder(View v) {
            super(v);
            checkBox = (CheckBox) v.findViewById(R.id.checkBox);
        }
    }

    // Конструктор
    public BlackListAdapter(ArrayList<String> list) {
        this.mList = list;
        this.mChecked = new ArrayList<>();
    }

    // Создает новые views (вызывается layout manager-ом)
    @Override
    public BlackListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.black_list_row, parent, false);

        // тут можно программно менять атрибуты лэйаута (size, margins, paddings и др.)
        ViewHolder viewHolder = new ViewHolder(v);
        final CheckBox checkBox = viewHolder.checkBox;
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String number = (String) buttonView.getText();
                if (isChecked) {
                    if (!mChecked.contains(number))
                        mChecked.add(number);
                    checkBox.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    if (mChecked.contains(number))
                        mChecked.remove(number);
                    checkBox.setPaintFlags(0);
                }
            }
        });
        return viewHolder;
    }

    // Заменяет контент отдельного view (вызывается layout manager-ом)
    @Override
    public void onBindViewHolder(BlackListAdapter.ViewHolder holder, int position) {
        holder.checkBox.setText(mList.get(position));
    }

    // Возвращает размер данных (вызывается layout manager-ом)
    @Override
    public int getItemCount() {
        return mList.size();
    }


    public ArrayList<String> getCheckedItems(){
        return mChecked;
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