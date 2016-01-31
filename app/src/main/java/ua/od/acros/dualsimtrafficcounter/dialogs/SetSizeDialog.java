package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.widget.WidgetConfigActivity;

public class SetSizeDialog extends DialogFragment implements TextView.OnEditorActionListener {

    private String mSize;
    private int mDialog;
    private static final String mKey1 = "size";
    private static final String mKey2 = "id";

    private EditText mEditText;

    public static DialogFragment newInstance(String size, int dialog) {
        SetSizeDialog f = new SetSizeDialog();
        Bundle args = new Bundle();
        args.putString(mKey1, size);
        args.putInt(mKey2, dialog);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get back arguments
        mSize = getArguments().getString(mKey1, "");
        mDialog = getArguments().getInt(mKey2, -1);
    }

    public interface TextSizeDialogListener {
        void onFinishEditDialog(String inputText, int dialog);
    }

    public SetSizeDialog() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.text_size_fragment, container);
        mEditText = (EditText) view.findViewById(R.id.txtSize);
        mEditText.setText(mSize);
        getDialog().setTitle(R.string.text_size);
        // Show soft keyboard automatically
        mEditText.requestFocus();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mEditText.setOnEditorActionListener(this);

        return view;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE || (event.getAction() == KeyEvent.ACTION_DOWN &&
                event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            // Return input text to activity
            WidgetConfigActivity activity = (WidgetConfigActivity) getActivity();
            activity.onFinishEditDialog(mEditText.getText().toString(), mDialog);
            this.dismiss();
            return true;
        }
        return false;
    }
}