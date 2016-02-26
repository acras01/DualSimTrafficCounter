package ua.od.acros.dualsimtrafficcounter.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import ua.od.acros.dualsimtrafficcounter.R;

public class IconsListFragment extends DialogFragment implements AdapterView.OnItemClickListener {

    private String[] mListItems;
    private ListView lv;
    private String mLogo;

    private OnCompleteListener mListener;

    private static final String ID = "id";

    public static IconsListFragment newInstance(String id) {
        IconsListFragment f = new IconsListFragment();
        Bundle args = new Bundle();
        args.putString(ID, id);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get back arguments
        mLogo = getArguments().getString(ID, "");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mListItems = getResources().getStringArray(R.array.icons);
        View view = inflater.inflate(R.layout.icons_list_layout, container, false);
        lv = (ListView) view.findViewById(R.id.list);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, mListItems);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        dismiss();
        this.mListener.onComplete(position, mLogo);
    }

    public interface OnCompleteListener {
        void onComplete(int position, String logo);
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnCompleteListener) activity;
        }
        catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnCompleteListener");
        }
    }

}