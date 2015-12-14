package ua.od.acros.dualsimtrafficcounter.widget;

import android.app.Activity;
import android.app.DialogFragment;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import ua.od.acros.dualsimtrafficcounter.CountService;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.dialogs.SetSizeDialog;
import ua.od.acros.dualsimtrafficcounter.dialogs.ShowSimDialog;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.IconsList;
import ua.od.acros.dualsimtrafficcounter.utils.MobileDataControl;
import ua.od.acros.dualsimtrafficcounter.utils.TrafficDatabase;
import yuku.ambilwarna.AmbilWarnaDialog;

public class WidgetConfigActivity extends Activity implements IconsList.OnCompleteListener,
        CompoundButton.OnCheckedChangeListener, Button.OnClickListener {

    private static final int SELECT_PHOTO = 101;
    int widgetID = AppWidgetManager.INVALID_APPWIDGET_ID;
    Intent resultValue;
    ImageView tiv, biv, logo1, logo2, logo3;
    CheckBox names, info, icons, speed, back;
    TextView infoSum, namesSum, iconsSum, logoSum1, logoSum2,
            logoSum3, textSizeSum, iconsSizeSum, speedSum,
            backSum, speedTextSum, speedIconsSum, showSimSum;
    RelativeLayout ll1, ll2, ll3, ll4, ll5, ll6, ll7, ll8, logoL1, logoL2, logoL3;
    private SharedPreferences prefs;
    private SharedPreferences.Editor edit;
    private int textColor, backColor;
    private final int KEY_TEXT = 0;
    private final int KEY_ICON = 1;
    private final int KEY_TEXT_S = 2;
    private final int KEY_ICON_S = 3;
    private String user_pick;

    final Context context = this;
    private int dim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dim = (int) getResources().getDimension(R.dimen.logo_size);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            widgetID = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (widgetID == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        prefs = getSharedPreferences(String.valueOf(widgetID) + "_" + Constants.WIDGET_PREFERENCES, Context.MODE_PRIVATE);
        edit = prefs.edit();
        if (prefs.getAll().size() == 0) {
            edit.putBoolean(Constants.PREF_WIDGET[1], true);
            edit.putBoolean(Constants.PREF_WIDGET[2], true);
            edit.putBoolean(Constants.PREF_WIDGET[3], false);
            edit.putBoolean(Constants.PREF_WIDGET[4], true);
            edit.putString(Constants.PREF_WIDGET[5], "none");
            edit.putString(Constants.PREF_WIDGET[6], "none");
            edit.putString(Constants.PREF_WIDGET[7], "none");
            edit.putBoolean(Constants.PREF_WIDGET[8], false);
            edit.putBoolean(Constants.PREF_WIDGET[9], false);
            edit.putBoolean(Constants.PREF_WIDGET[10], false);
            edit.putString(Constants.PREF_WIDGET[11], Constants.ICON_SIZE);
            edit.putString(Constants.PREF_WIDGET[12], Constants.TEXT_SIZE);
            edit.putInt(Constants.PREF_WIDGET[13], Color.WHITE);
            edit.putBoolean(Constants.PREF_WIDGET[14], true);
            edit.putInt(Constants.PREF_WIDGET[15], Color.TRANSPARENT);
            edit.putString(Constants.PREF_WIDGET[16], Constants.TEXT_SIZE);
            edit.putString(Constants.PREF_WIDGET[17], Constants.ICON_SIZE);
            edit.putBoolean(Constants.PREF_WIDGET[18], true);
            edit.putBoolean(Constants.PREF_WIDGET[19], true);
            edit.putBoolean(Constants.PREF_WIDGET[20], true);
            edit.apply();
        }

        textColor = prefs.getInt(Constants.PREF_WIDGET[13], Color.WHITE);
        backColor = prefs.getInt(Constants.PREF_WIDGET[15], Color.TRANSPARENT);

        resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);

        setResult(RESULT_CANCELED, resultValue);

        setContentView(R.layout.activity_widget_config);

        names = (CheckBox) findViewById(R.id.names);
        names.setChecked(prefs.getBoolean(Constants.PREF_WIDGET[1], true));
        info = (CheckBox) findViewById(R.id.info);
        info.setChecked(prefs.getBoolean(Constants.PREF_WIDGET[2], true));
        icons = (CheckBox) findViewById(R.id.icons);
        icons.setChecked(prefs.getBoolean(Constants.PREF_WIDGET[4], true));
        speed = (CheckBox) findViewById(R.id.speed);
        speed.setChecked(prefs.getBoolean(Constants.PREF_WIDGET[3], true));
        back = (CheckBox) findViewById(R.id.useBack);
        back.setChecked(prefs.getBoolean(Constants.PREF_WIDGET[14], true));

        namesSum = (TextView) findViewById(R.id.names_summary);
        if (names.isChecked())
            namesSum.setText(R.string.on);
        else
            namesSum.setText(R.string.off);
        infoSum = (TextView) findViewById(R.id.info_summary);
        if (info.isChecked())
            infoSum.setText(R.string.all);
        else
            infoSum.setText(R.string.only_total);
        iconsSum = (TextView) findViewById(R.id.icons_summary);
        if (icons.isChecked())
            iconsSum.setText(R.string.on);
        else
            iconsSum.setText(R.string.off);
        speedSum = (TextView) findViewById(R.id.speed_summary);
        if (speed.isChecked())
            speedSum.setText(R.string.on);
        else
            speedSum.setText(R.string.off);


        logoL1 = (RelativeLayout) findViewById(R.id.logoLayout1);
        logoL2 = (RelativeLayout) findViewById(R.id.logoLayout2);
        logoL3 = (RelativeLayout) findViewById(R.id.logoLayout3);

        ll3 = (RelativeLayout) findViewById(R.id.ll3);
        ll4 = (RelativeLayout) findViewById(R.id.ll4);
        ll5 = (RelativeLayout) findViewById(R.id.ll5);
        ll6 = (RelativeLayout) findViewById(R.id.ll6);
        ll7 = (RelativeLayout) findViewById(R.id.ll7);
        ll8 = (RelativeLayout) findViewById(R.id.backColorLayout);


        onOff(logoL1, icons.isChecked());
        onOff(logoL2, MobileDataControl.isMultiSim(context) >= 2 && icons.isChecked());
        onOff(logoL3, MobileDataControl.isMultiSim(context) == 3 && icons.isChecked());
        onOff(ll5, speed.isChecked());
        onOff(ll6, speed.isChecked());
        onOff(ll8, back.isChecked());

        backSum = (TextView) findViewById(R.id.back_summary);
        if (back.isChecked())
            backSum.setText(R.string.on);
        else
            backSum.setText(R.string.off);


        textSizeSum = (TextView) findViewById(R.id.textSizeSum);
        textSizeSum.setText(prefs.getString(Constants.PREF_WIDGET[12], Constants.TEXT_SIZE));

        iconsSizeSum = (TextView) findViewById(R.id.iconSizeSum);
        iconsSizeSum.setText(prefs.getString(Constants.PREF_WIDGET[11], Constants.ICON_SIZE));

        speedTextSum = (TextView) findViewById(R.id.speedTextSizeSum);
        speedTextSum.setText(prefs.getString(Constants.PREF_WIDGET[16], Constants.TEXT_SIZE));

        speedIconsSum = (TextView) findViewById(R.id.speedIconsSizeSum);
        speedIconsSum.setText(prefs.getString(Constants.PREF_WIDGET[17], Constants.ICON_SIZE));

        showSimSum = (TextView) findViewById(R.id.simChooseSum);
        String sum = "";
        if (prefs.getBoolean(Constants.PREF_WIDGET[18], true))
            sum = "SIM1";
        if (prefs.getBoolean(Constants.PREF_WIDGET[19], true))
            if (sum.equals(""))
                sum = "SIM2";
            else
                sum += ", SIM2";
        if (prefs.getBoolean(Constants.PREF_WIDGET[20], true))
            if (sum.equals(""))
                sum = "SIM3";
            else
                sum += ", SIM3";
        showSimSum.setText(sum);

        names.setOnCheckedChangeListener(this);
        info.setOnCheckedChangeListener(this);
        icons.setOnCheckedChangeListener(this);
        speed.setOnCheckedChangeListener(this);
        back.setOnCheckedChangeListener(this);

        tiv = (ImageView) findViewById(R.id.textColorPreview);
        biv = (ImageView) findViewById(R.id.backColorPreview);
        tiv.setBackgroundColor(textColor);
        biv.setBackgroundColor(backColor);

        logo1 = (ImageView) findViewById(R.id.logoPreview1);
        logo2 = (ImageView) findViewById(R.id.logoPreview2);
        logo3 = (ImageView) findViewById(R.id.logoPreview3);
        int resourceId1 = getApplicationContext().getResources().getIdentifier(prefs.getString(Constants.PREF_WIDGET[5], "none"), "drawable", getApplicationContext().getPackageName());
        Bitmap b1 = BitmapFactory.decodeResource(getApplicationContext().getResources(), resourceId1);
        int resourceId2 = getApplicationContext().getResources().getIdentifier(prefs.getString(Constants.PREF_WIDGET[6], "none"), "drawable", getApplicationContext().getPackageName());
        Bitmap b2 = BitmapFactory.decodeResource(getApplicationContext().getResources(), resourceId2);
        int resourceId3 = getApplicationContext().getResources().getIdentifier(prefs.getString(Constants.PREF_WIDGET[7], "none"), "drawable", getApplicationContext().getPackageName());
        Bitmap b3 = BitmapFactory.decodeResource(getApplicationContext().getResources(), resourceId3);
        logo1.setImageBitmap(b1);
        logo2.setImageBitmap(b2);
        logo3.setImageBitmap(b3);

        logoSum1 = (TextView) findViewById(R.id.logoSum1);
        logoSum2 = (TextView) findViewById(R.id.logoSum2);
        logoSum3 = (TextView) findViewById(R.id.logoSum3);
        String[] listitems = getResources().getStringArray(R.array.icons_values);
        String[] list = getResources().getStringArray(R.array.icons);
        for (int i = 0; i < list.length; i++) {
            if (listitems[i].equals(prefs.getString(Constants.PREF_WIDGET[5], "none")))
                logoSum1.setText(list[i]);
            if (listitems[i].equals(prefs.getString(Constants.PREF_WIDGET[6], "none")))
                logoSum2.setText(list[i]);
            if (listitems[i].equals(prefs.getString(Constants.PREF_WIDGET[7], "none")))
                logoSum3.setText(list[i]);
        }

        tiv.setOnClickListener(this);
        biv.setOnClickListener(this);
        logo1.setOnClickListener(this);
        logo2.setOnClickListener(this);
        logo3.setOnClickListener(this);
        ll3.setOnClickListener(this);
        ll4.setOnClickListener(this);
        ll5.setOnClickListener(this);
        ll6.setOnClickListener(this);
        ll7.setOnClickListener(this);
        ll8.setOnClickListener(this);

        if (prefs.getBoolean(Constants.PREF_WIDGET[8], false)) {
            Picasso.with(context).load(Uri.parse(prefs.getString(Constants.PREF_WIDGET[5], "none")))
                    .resize(dim, dim).centerInside().into(logo1);
            logoSum1.setText(getResources().getString(R.string.userpick));
        }
        if (prefs.getBoolean(Constants.PREF_WIDGET[9], false)) {
            Picasso.with(context).load(Uri.parse(prefs.getString(Constants.PREF_WIDGET[6], "none")))
                    .resize(dim, dim).centerInside().into(logo2);
            logoSum2.setText(getResources().getString(R.string.userpick));
        }
        if (prefs.getBoolean(Constants.PREF_WIDGET[10], false)) {
            Picasso.with(context).load(Uri.parse(prefs.getString(Constants.PREF_WIDGET[7], "none")))
                    .resize(dim, dim).centerInside().into(logo3);
            logoSum3.setText(getResources().getString(R.string.userpick));
        }

        prefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                String sum = "";
                if (sharedPreferences.getBoolean(Constants.PREF_WIDGET[18], true))
                    sum = "SIM1";
                if (sharedPreferences.getBoolean(Constants.PREF_WIDGET[19], true))
                    if (sum.equals(""))
                        sum = "SIM2";
                    else
                        sum += ", SIM2";
                if (sharedPreferences.getBoolean(Constants.PREF_WIDGET[20], true))
                    if (sum.equals(""))
                        sum = "SIM3";
                    else
                        sum += ", SIM3";
                showSimSum.setText(sum);
            }
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.widget_config_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.save) {
            edit.apply();
            setResult(RESULT_OK, resultValue);
            Map<String, Object> dataMap = new HashMap<>();
            dataMap = TrafficDatabase.read_writeTrafficData(Constants.READ, dataMap,
                    new TrafficDatabase(context, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION));
            Intent intent = new Intent(Constants.BROADCAST_ACTION);
            intent.putExtra(Constants.SPEEDRX, 0L);
            intent.putExtra(Constants.SPEEDTX, 0L);
            intent.putExtra(Constants.SIM1RX, (long) dataMap.get(Constants.SIM1RX));
            intent.putExtra(Constants.SIM2RX, (long) dataMap.get(Constants.SIM2RX));
            intent.putExtra(Constants.SIM3RX, (long) dataMap.get(Constants.SIM3RX));
            intent.putExtra(Constants.SIM1TX, (long) dataMap.get(Constants.SIM1TX));
            intent.putExtra(Constants.SIM2TX, (long) dataMap.get(Constants.SIM2TX));
            intent.putExtra(Constants.SIM3TX, (long) dataMap.get(Constants.SIM3TX));
            intent.putExtra(Constants.TOTAL1, (long) dataMap.get(Constants.TOTAL1));
            intent.putExtra(Constants.TOTAL2, (long) dataMap.get(Constants.TOTAL2));
            intent.putExtra(Constants.TOTAL3, (long) dataMap.get(Constants.TOTAL3));
            intent.putExtra(Constants.OPERATOR1, CountService.getName(Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1));
            if (MobileDataControl.getMobileDataInfo(getApplicationContext())[1] >= 2)
                intent.putExtra(Constants.OPERATOR2, CountService.getName(Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2));
            if (MobileDataControl.getMobileDataInfo(getApplicationContext())[1] == 3)
                intent.putExtra(Constants.OPERATOR3, CountService.getName(Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3));

            sendBroadcast(intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private static void onOff(ViewGroup layout, boolean state) {
        layout.setEnabled(false);
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof ViewGroup) {
                onOff((ViewGroup) child, state);
            } else {
                child.setEnabled(state);
            }
        }
    }

    public void showDialog(View view) {
        DialogFragment dialog = null;
        switch (view.getId()) {
            case R.id.logoPreview1:
                dialog = IconsList.newInstance(Constants.PREF_WIDGET[5]);
                break;
            case R.id.logoPreview2:
                dialog = IconsList.newInstance(Constants.PREF_WIDGET[6]);
                break;
            case R.id.logoPreview3:
                dialog = IconsList.newInstance(Constants.PREF_WIDGET[7]);
                break;
            case R.id.ll3:
                dialog = SetSizeDialog.newInstance(prefs.getString(Constants.PREF_WIDGET[12], Constants.TEXT_SIZE), KEY_TEXT);
                break;
            case R.id.ll4:
                dialog = SetSizeDialog.newInstance(prefs.getString(Constants.PREF_WIDGET[11], Constants.ICON_SIZE), KEY_ICON);
                break;
            case R.id.ll5:
                dialog = SetSizeDialog.newInstance(prefs.getString(Constants.PREF_WIDGET[16], Constants.TEXT_SIZE), KEY_TEXT_S);
                break;
            case R.id.ll6:
                dialog = SetSizeDialog.newInstance(prefs.getString(Constants.PREF_WIDGET[17], Constants.ICON_SIZE), KEY_ICON_S);
                break;
            case R.id.ll7:
                dialog = ShowSimDialog.newInstance(widgetID);
                break;
        }
        if (dialog != null) {
            dialog.show(getFragmentManager(), "dialog");
        }
    }

    public void onFinishEditDialog(String inputText, int dialog) {
        if (!inputText.equals("")) {
            switch (dialog) {
                case KEY_TEXT:
                    edit.putString(Constants.PREF_WIDGET[12], inputText);
                    textSizeSum.setText(inputText);
                    break;
                case KEY_ICON:
                    edit.putString(Constants.PREF_WIDGET[11], inputText);
                    iconsSizeSum.setText(inputText);
                    break;
                case KEY_TEXT_S:
                    edit.putString(Constants.PREF_WIDGET[16], inputText);
                    speedTextSum.setText(inputText);
                    break;
                case KEY_ICON_S:
                    edit.putString(Constants.PREF_WIDGET[17], inputText);
                    speedIconsSum.setText(inputText);
                    break;
            }
        }
    }

    @Override
    public void onComplete(int position, String logo) {
        String[] listitems = getResources().getStringArray(R.array.icons_values);
        String[] list = getResources().getStringArray(R.array.icons);
        Bitmap b;
        if (position < list.length - 1) {
            user_pick = "";
            int resourceId = getApplicationContext().getResources().getIdentifier(listitems[position], "drawable", getApplicationContext().getPackageName());
            if (logo.equals(Constants.PREF_WIDGET[5])) {
                edit.putBoolean(Constants.PREF_WIDGET[8], false);
                edit.putString(Constants.PREF_WIDGET[5], listitems[position]);
                Picasso.with(context).load(resourceId).resize(dim, dim).centerInside().into(logo1);
                logoSum1.setText(list[position]);
            } else if (logo.equals(Constants.PREF_WIDGET[6])) {
                edit.putBoolean(Constants.PREF_WIDGET[9], false);
                edit.putString(Constants.PREF_WIDGET[6], listitems[position]);
                Picasso.with(context).load(resourceId).resize(dim, dim).centerInside().into(logo2);
                logoSum2.setText(list[position]);
            } else if (logo.equals(Constants.PREF_WIDGET[7])) {
                edit.putBoolean(Constants.PREF_WIDGET[10], false);
                edit.putString(Constants.PREF_WIDGET[7], listitems[position]);
                Picasso.with(context).load(resourceId).resize(dim, dim).centerInside().into(logo3);
                logoSum3.setText(list[position]);
            }
        } else {
            user_pick = logo;
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, SELECT_PHOTO);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    if (user_pick.equals(Constants.PREF_WIDGET[5])) {
                        edit.putBoolean(Constants.PREF_WIDGET[8], true);
                        edit.putString(Constants.PREF_WIDGET[5], selectedImage.toString());
                        Picasso.with(context).load(selectedImage).resize(dim, dim).centerInside().into(logo1);
                        logoSum3.setText(getResources().getString(R.string.userpick));
                    } else if (user_pick.equals(Constants.PREF_WIDGET[6])) {
                        edit.putBoolean(Constants.PREF_WIDGET[9], true);
                        edit.putString(Constants.PREF_WIDGET[6], selectedImage.toString());
                        Picasso.with(context).load(selectedImage).resize(dim, dim).centerInside().into(logo2);
                        logoSum3.setText(getResources().getString(R.string.userpick));
                    } else if (user_pick.equals(Constants.PREF_WIDGET[7])) {
                        edit.putBoolean(Constants.PREF_WIDGET[10], true);
                        edit.putString(Constants.PREF_WIDGET[7], selectedImage.toString());
                        Picasso.with(context).load(selectedImage).resize(dim, dim).centerInside().into(logo3);
                        logoSum3.setText(getResources().getString(R.string.userpick));
                    }
                    user_pick = "";
                }
                break;
        }
    }

    /**
     * Called when the checked state of a compound button has changed.
     *
     * @param buttonView The compound button view whose state has changed.
     * @param isChecked  The new checked state of buttonView.
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.names:
                edit.putBoolean(Constants.PREF_WIDGET[1], isChecked);
                if (isChecked)
                    namesSum.setText(R.string.on);
                else
                    namesSum.setText(R.string.off);
                break;
            case R.id.info:
                edit.putBoolean(Constants.PREF_WIDGET[2], isChecked);
                if (isChecked)
                    infoSum.setText(R.string.all);
                else
                    infoSum.setText(R.string.only_total);
                break;
            case R.id.speed:
                edit.putBoolean(Constants.PREF_WIDGET[3], isChecked);
                onOff(ll5, isChecked);
                onOff(ll6, isChecked);
                if (isChecked)
                    speedSum.setText(R.string.on);
                else
                    speedSum.setText(R.string.off);
                break;
            case R.id.useBack:
                edit.putBoolean(Constants.PREF_WIDGET[14], isChecked);
                if (isChecked)
                    backSum.setText(R.string.on);
                else
                    backSum.setText(R.string.off);
                onOff(ll8, isChecked);
                break;
            case R.id.icons:
                edit.putBoolean(Constants.PREF_WIDGET[4], isChecked);

                onOff(logoL1, isChecked);
                onOff(logoL2, isChecked);
                onOff(logoL3, isChecked);
                onOff(ll4, isChecked);
                if (isChecked)
                    iconsSum.setText(R.string.on);
                else
                    iconsSum.setText(R.string.off);
                break;
        }
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        AmbilWarnaDialog dialog = null;
        switch (v.getId()) {
            case R.id.textColorPreview:
                dialog = new AmbilWarnaDialog(context, textColor, true, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        edit.putInt(Constants.PREF_WIDGET[13], color);
                        tiv.setBackgroundColor(color);
                    }
                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // cancel was selected by the user
                    }
                });
                break;
            case R.id.backColorPreview:
                dialog = new AmbilWarnaDialog(context, backColor, true, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        edit.putInt(Constants.PREF_WIDGET[15], color);
                        biv.setBackgroundColor(color);
                    }
                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // cancel was selected by the user
                    }
                });
                break;
            case R.id.logoPreview1:
            case R.id.logoPreview2:
            case R.id.logoPreview3:
            case R.id.ll3:
            case R.id.ll4:
            case R.id.ll5:
            case R.id.ll6:
            case R.id.ll7:
                showDialog(v);
                break;
        }
        if (dialog != null)
            dialog.show();
    }
}
