package ua.od.acros.dualsimtrafficcounter.activities;

import android.app.Activity;
import android.app.DialogFragment;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.acra.ACRA;

import java.io.File;

import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.dialogs.SetSizeDialog;
import ua.od.acros.dualsimtrafficcounter.dialogs.ShowSimDialog;
import ua.od.acros.dualsimtrafficcounter.utils.CheckServiceRunning;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.IconsList;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabase;
import yuku.ambilwarna.AmbilWarnaDialog;

public class WidgetConfigActivity extends Activity implements IconsList.OnCompleteListener,
        CompoundButton.OnCheckedChangeListener, View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int SELECT_PHOTO = 101;
    private int mWidgetID = AppWidgetManager.INVALID_APPWIDGET_ID;
    private Intent mResultValueIntent;
    private ImageView tiv, biv, logo1, logo2, logo3;
    private TextView infoSum, namesSum, iconsSum, logoSum1, logoSum2,
            logoSum3, textSizeSum, iconsSizeSum, speedSum, backSum,
            speedTextSum, speedIconsSum, showSimSum, divSum, activesum, daynightSum;
    private RelativeLayout simLogoL;
    private RelativeLayout speedFontL;
    private RelativeLayout speedArrowsL;
    private RelativeLayout showSimL;
    private RelativeLayout backColorL;
    private RelativeLayout logoL1;
    private RelativeLayout logoL2;
    private RelativeLayout logoL3;
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mEdit;
    private int mTextColor, mBackColor;
    private final int KEY_TEXT = 0;
    private final int KEY_ICON = 1;
    private final int KEY_TEXT_S = 2;
    private final int KEY_ICON_S = 3;
    private int mSimQuantity;
    private int mDim;
    private String mUserPickedImage;

    private final Context mContext = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!CheckServiceRunning.isMyServiceRunning(TrafficCountService.class, mContext))
            mContext.startService(new Intent(mContext, TrafficCountService.class));

        mDim = (int) getResources().getDimension(R.dimen.logo_size);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mWidgetID = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (mWidgetID == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        mPrefs = getSharedPreferences(String.valueOf(mWidgetID) + "_" + Constants.WIDGET_PREFERENCES, Context.MODE_PRIVATE);
        mSimQuantity = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));
        mEdit = mPrefs.edit();
        if (mPrefs.getAll().size() == 0) {
            mEdit.putBoolean(Constants.PREF_WIDGET[1], true);
            mEdit.putBoolean(Constants.PREF_WIDGET[2], true);
            mEdit.putBoolean(Constants.PREF_WIDGET[3], false);
            mEdit.putBoolean(Constants.PREF_WIDGET[4], true);
            mEdit.putString(Constants.PREF_WIDGET[5], "none");
            mEdit.putString(Constants.PREF_WIDGET[6], "none");
            mEdit.putString(Constants.PREF_WIDGET[7], "none");
            mEdit.putBoolean(Constants.PREF_WIDGET[8], false);
            mEdit.putBoolean(Constants.PREF_WIDGET[9], false);
            mEdit.putBoolean(Constants.PREF_WIDGET[10], false);
            mEdit.putString(Constants.PREF_WIDGET[11], Constants.ICON_SIZE);
            mEdit.putString(Constants.PREF_WIDGET[12], Constants.TEXT_SIZE);
            mEdit.putInt(Constants.PREF_WIDGET[13], Color.WHITE);
            mEdit.putBoolean(Constants.PREF_WIDGET[14], true);
            mEdit.putInt(Constants.PREF_WIDGET[15], Color.TRANSPARENT);
            mEdit.putString(Constants.PREF_WIDGET[16], Constants.TEXT_SIZE);
            mEdit.putString(Constants.PREF_WIDGET[17], Constants.ICON_SIZE);
            mEdit.putBoolean(Constants.PREF_WIDGET[18], true);
            mEdit.putBoolean(Constants.PREF_WIDGET[19], true);
            mEdit.putBoolean(Constants.PREF_WIDGET[20], true);
            mEdit.putBoolean(Constants.PREF_WIDGET[21], true);
            mEdit.putBoolean(Constants.PREF_WIDGET[22], false);
            mEdit.putBoolean(Constants.PREF_WIDGET[23], false);
            mEdit.apply();
        }

        mTextColor = mPrefs.getInt(Constants.PREF_WIDGET[13], Color.WHITE);
        mBackColor = mPrefs.getInt(Constants.PREF_WIDGET[15], Color.TRANSPARENT);

        mResultValueIntent = new Intent();
        mResultValueIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetID);

        setResult(RESULT_CANCELED, mResultValueIntent);

        setContentView(R.layout.activity_widget_config);

        CheckBox names = (CheckBox) findViewById(R.id.names);
        names.setChecked(mPrefs.getBoolean(Constants.PREF_WIDGET[1], true));
        CheckBox info = (CheckBox) findViewById(R.id.info);
        info.setChecked(mPrefs.getBoolean(Constants.PREF_WIDGET[2], true));
        CheckBox icons = (CheckBox) findViewById(R.id.icons);
        icons.setChecked(mPrefs.getBoolean(Constants.PREF_WIDGET[4], true));
        CheckBox speed = (CheckBox) findViewById(R.id.speed);
        speed.setChecked(mPrefs.getBoolean(Constants.PREF_WIDGET[3], true));
        CheckBox back = (CheckBox) findViewById(R.id.useBack);
        back.setChecked(mPrefs.getBoolean(Constants.PREF_WIDGET[14], true));
        CheckBox div = (CheckBox) findViewById(R.id.divider);
        div.setChecked(mPrefs.getBoolean(Constants.PREF_WIDGET[21], true));
        CheckBox active = (CheckBox) findViewById(R.id.activesim);
        active.setChecked(mPrefs.getBoolean(Constants.PREF_WIDGET[22], false));
        CheckBox daynight = (CheckBox) findViewById(R.id.daynight_icons);
        daynight.setChecked(mPrefs.getBoolean(Constants.PREF_WIDGET[23], false));

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
        divSum = (TextView) findViewById(R.id.divider_summary);
        if (div.isChecked())
            divSum.setText(R.string.on);
        else
            divSum.setText(R.string.off);
        activesum = (TextView) findViewById(R.id.activesim_summary);
        if (active.isChecked())
            activesum.setText(R.string.on);
        else
            activesum.setText(R.string.off);
        backSum = (TextView) findViewById(R.id.back_summary);
        if (back.isChecked())
            backSum.setText(R.string.on);
        else
            backSum.setText(R.string.off);
        daynightSum = (TextView) findViewById(R.id.daynight_icons_summary);
        if (daynight.isChecked())
            daynightSum.setText(R.string.on);
        else
            daynightSum.setText(R.string.off);


        logoL1 = (RelativeLayout) findViewById(R.id.logoLayout1);
        logoL2 = (RelativeLayout) findViewById(R.id.logoLayout2);
        logoL3 = (RelativeLayout) findViewById(R.id.logoLayout3);

        RelativeLayout simFontL = (RelativeLayout) findViewById(R.id.simFontSize);
        simLogoL = (RelativeLayout) findViewById(R.id.simLogoSize);
        speedFontL = (RelativeLayout) findViewById(R.id.speedFontSize);
        speedArrowsL = (RelativeLayout) findViewById(R.id.speedArrowsSize);
        showSimL = (RelativeLayout) findViewById(R.id.showSim);
        backColorL = (RelativeLayout) findViewById(R.id.backColorLayout);


        onOff(logoL1, icons.isChecked());
        onOff(logoL2, mSimQuantity >= 2 && icons.isChecked());
        onOff(logoL3, mSimQuantity == 3 && icons.isChecked());
        onOff(speedFontL, speed.isChecked());
        onOff(speedArrowsL, speed.isChecked());
        onOff(showSimL, !active.isChecked());
        onOff(backColorL, back.isChecked());

        textSizeSum = (TextView) findViewById(R.id.textSizeSum);
        textSizeSum.setText(mPrefs.getString(Constants.PREF_WIDGET[12], Constants.TEXT_SIZE));

        iconsSizeSum = (TextView) findViewById(R.id.iconSizeSum);
        iconsSizeSum.setText(mPrefs.getString(Constants.PREF_WIDGET[11], Constants.ICON_SIZE));

        speedTextSum = (TextView) findViewById(R.id.speedTextSizeSum);
        speedTextSum.setText(mPrefs.getString(Constants.PREF_WIDGET[16], Constants.TEXT_SIZE));

        speedIconsSum = (TextView) findViewById(R.id.speedIconsSizeSum);
        speedIconsSum.setText(mPrefs.getString(Constants.PREF_WIDGET[17], Constants.ICON_SIZE));

        showSimSum = (TextView) findViewById(R.id.simChooseSum);
        String sum = "";
        if (mPrefs.getBoolean(Constants.PREF_WIDGET[18], true))
            sum = "SIM1";
        if (mPrefs.getBoolean(Constants.PREF_WIDGET[19], true))
            if (sum.equals(""))
                sum = "SIM2";
            else
                sum += ", SIM2";
        if (mPrefs.getBoolean(Constants.PREF_WIDGET[20], true))
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
        div.setOnCheckedChangeListener(this);
        active.setOnCheckedChangeListener(this);
        daynight.setOnCheckedChangeListener(this);

        tiv = (ImageView) findViewById(R.id.textColorPreview);
        biv = (ImageView) findViewById(R.id.backColorPreview);
        tiv.setBackgroundColor(mTextColor);
        biv.setBackgroundColor(mBackColor);

        logo1 = (ImageView) findViewById(R.id.logoPreview1);
        logo2 = (ImageView) findViewById(R.id.logoPreview2);
        logo3 = (ImageView) findViewById(R.id.logoPreview3);
        logoSum1 = (TextView) findViewById(R.id.logoSum1);
        logoSum2 = (TextView) findViewById(R.id.logoSum2);
        logoSum3 = (TextView) findViewById(R.id.logoSum3);

        if (mPrefs.getBoolean(Constants.PREF_WIDGET[8], false)) {
            Picasso.with(mContext)
                    .load(new File(mPrefs.getString(Constants.PREF_WIDGET[5], "")))
                    .resize(mDim, mDim)
                    .centerInside()
                    .error(R.drawable.none)
                    .into(logo1);
            logoSum1.setText(getResources().getString(R.string.userpick));
        } else
            Picasso.with(mContext)
                    .load(getResources().getIdentifier(mPrefs.getString(Constants.PREF_WIDGET[5], "none"), "drawable", getApplicationContext().getPackageName()))
                    .resize(mDim, mDim)
                    .centerInside()
                    .error(R.drawable.none)
                    .into(logo1);
        if (mPrefs.getBoolean(Constants.PREF_WIDGET[9], false)) {
            Picasso.with(mContext)
                    .load(new File(mPrefs.getString(Constants.PREF_WIDGET[6], "")))
                    .resize(mDim, mDim)
                    .centerInside()
                    .error(R.drawable.none)
                    .into(logo2);
            logoSum2.setText(getResources().getString(R.string.userpick));
        } else
            Picasso.with(mContext)
                    .load(getResources().getIdentifier(mPrefs.getString(Constants.PREF_WIDGET[6], "none"), "drawable", getApplicationContext().getPackageName()))
                    .resize(mDim, mDim)
                    .centerInside()
                    .error(R.drawable.none)
                    .into(logo2);
        if (mPrefs.getBoolean(Constants.PREF_WIDGET[10], false)) {
            Picasso.with(mContext)
                    .load(new File(mPrefs.getString(Constants.PREF_WIDGET[7], "")))
                    .resize(mDim, mDim)
                    .centerInside()
                    .error(R.drawable.none)
                    .into(logo3);
            logoSum3.setText(getResources().getString(R.string.userpick));
        } else
            Picasso.with(mContext)
                    .load(getResources().getIdentifier(mPrefs.getString(Constants.PREF_WIDGET[7], "none"), "drawable", getApplicationContext().getPackageName()))
                    .resize(mDim, mDim)
                    .centerInside()
                    .error(R.drawable.none)
                    .into(logo3);

        String[] listitems = getResources().getStringArray(R.array.icons_values);
        String[] list = getResources().getStringArray(R.array.icons);
        for (int i = 0; i < list.length; i++) {
            if (!mPrefs.getBoolean(Constants.PREF_WIDGET[8], false) && listitems[i].equals(mPrefs.getString(Constants.PREF_WIDGET[5], "none")))
                logoSum1.setText(list[i]);
            if (!mPrefs.getBoolean(Constants.PREF_WIDGET[9], false) && listitems[i].equals(mPrefs.getString(Constants.PREF_WIDGET[6], "none")))
                logoSum2.setText(list[i]);
            if (!mPrefs.getBoolean(Constants.PREF_WIDGET[10], false) && listitems[i].equals(mPrefs.getString(Constants.PREF_WIDGET[7], "none")))
                logoSum3.setText(list[i]);
        }

        tiv.setOnClickListener(this);
        biv.setOnClickListener(this);
        logo1.setOnClickListener(this);
        logo2.setOnClickListener(this);
        logo3.setOnClickListener(this);
        setOnClickListenerWithChild(simFontL);
        setOnClickListenerWithChild(simLogoL);
        setOnClickListenerWithChild(speedFontL);
        setOnClickListenerWithChild(speedArrowsL);
        setOnClickListenerWithChild(showSimL);
        backColorL.setOnClickListener(this);

        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    private void setOnClickListenerWithChild(ViewGroup v) {
        for (int i = 0; i < v.getChildCount(); i++) {
            View child = v.getChildAt(i);
            if (child instanceof ViewGroup) {
                setOnClickListenerWithChild((ViewGroup) child);
            } else {
                child.setOnClickListener(this);
            }
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.widget_config_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            if (item.getItemId() == R.id.save) {
                mEdit.apply();
                Intent intent = new Intent(Constants.BROADCAST_ACTION);
                intent.putExtra(Constants.WIDGET_IDS, new int[]{mWidgetID});
                if (!MyDatabase.isEmpty(new MyDatabase(mContext, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION))) {
                    ContentValues dataMap = MyDatabase.readTrafficData(MyDatabase.getInstance(mContext));
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
                    intent.putExtra(Constants.SIM1RX_N, (long) dataMap.get(Constants.SIM1RX_N));
                    intent.putExtra(Constants.SIM2RX_N, (long) dataMap.get(Constants.SIM2RX_N));
                    intent.putExtra(Constants.SIM3RX_N, (long) dataMap.get(Constants.SIM3RX_N));
                    intent.putExtra(Constants.SIM1TX_N, (long) dataMap.get(Constants.SIM1TX_N));
                    intent.putExtra(Constants.SIM2TX_N, (long) dataMap.get(Constants.SIM2TX_N));
                    intent.putExtra(Constants.SIM3TX_N, (long) dataMap.get(Constants.SIM3TX_N));
                    intent.putExtra(Constants.TOTAL1_N, (long) dataMap.get(Constants.TOTAL1_N));
                    intent.putExtra(Constants.TOTAL2_N, (long) dataMap.get(Constants.TOTAL2_N));
                    intent.putExtra(Constants.TOTAL3_N, (long) dataMap.get(Constants.TOTAL3_N));
                    intent.putExtra(Constants.SIM_ACTIVE, (int) dataMap.get(Constants.LAST_ACTIVE_SIM));
                    intent.putExtra(Constants.OPERATOR1, MobileUtils.getName(this, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1));
                    if (mSimQuantity >= 2)
                        intent.putExtra(Constants.OPERATOR2, MobileUtils.getName(this, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2));
                    if (mSimQuantity == 3)
                        intent.putExtra(Constants.OPERATOR3, MobileUtils.getName(this, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3));
                } else {
                    intent.putExtra(Constants.SPEEDRX, 0L);
                    intent.putExtra(Constants.SPEEDTX, 0L);
                    intent.putExtra(Constants.SIM1RX, 0L);
                    intent.putExtra(Constants.SIM2RX, 0L);
                    intent.putExtra(Constants.SIM3RX, 0L);
                    intent.putExtra(Constants.SIM1TX, 0L);
                    intent.putExtra(Constants.SIM2TX, 0L);
                    intent.putExtra(Constants.SIM3TX, 0L);
                    intent.putExtra(Constants.TOTAL1, 0L);
                    intent.putExtra(Constants.TOTAL2, 0L);
                    intent.putExtra(Constants.TOTAL3, 0L);
                    intent.putExtra(Constants.SIM1RX_N, 0L);
                    intent.putExtra(Constants.SIM2RX_N, 0L);
                    intent.putExtra(Constants.SIM3RX_N, 0L);
                    intent.putExtra(Constants.SIM1TX_N, 0L);
                    intent.putExtra(Constants.SIM2TX_N, 0L);
                    intent.putExtra(Constants.SIM3TX_N, 0L);
                    intent.putExtra(Constants.TOTAL1_N, 0L);
                    intent.putExtra(Constants.TOTAL2_N, 0L);
                    intent.putExtra(Constants.TOTAL3_N, 0L);
                    intent.putExtra(Constants.SIM_ACTIVE, 0);
                }
                sendBroadcast(intent);
                setResult(RESULT_OK, mResultValueIntent);
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
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
            case R.id.simFontSize:
            case R.id.textSize:
            case R.id.textSizeSum:
                dialog = SetSizeDialog.newInstance(mPrefs.getString(Constants.PREF_WIDGET[12], Constants.TEXT_SIZE), KEY_TEXT);
                break;
            case R.id.simLogoSize:
            case R.id.iconSize:
            case R.id.iconSizeSum:
                dialog = SetSizeDialog.newInstance(mPrefs.getString(Constants.PREF_WIDGET[11], Constants.ICON_SIZE), KEY_ICON);
                break;
            case R.id.speedFontSize:
            case R.id.speedTextSize:
            case R.id.speedTextSizeSum:
                dialog = SetSizeDialog.newInstance(mPrefs.getString(Constants.PREF_WIDGET[16], Constants.TEXT_SIZE), KEY_TEXT_S);
                break;
            case R.id.speedArrowsSize:
            case R.id.speedIconsSize:
            case R.id.speedIconsSizeSum:
                dialog = SetSizeDialog.newInstance(mPrefs.getString(Constants.PREF_WIDGET[17], Constants.ICON_SIZE), KEY_ICON_S);
                break;
            case R.id.showSim:
            case R.id.simChoose:
            case R.id.simChooseSum:
                dialog = ShowSimDialog.newInstance(mWidgetID);
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
                    mEdit.putString(Constants.PREF_WIDGET[12], inputText);
                    textSizeSum.setText(inputText);
                    break;
                case KEY_ICON:
                    mEdit.putString(Constants.PREF_WIDGET[11], inputText);
                    iconsSizeSum.setText(inputText);
                    break;
                case KEY_TEXT_S:
                    mEdit.putString(Constants.PREF_WIDGET[16], inputText);
                    speedTextSum.setText(inputText);
                    break;
                case KEY_ICON_S:
                    mEdit.putString(Constants.PREF_WIDGET[17], inputText);
                    speedIconsSum.setText(inputText);
                    break;
            }
        }
    }

    @Override
    public void onComplete(int position, String logo) {
        String[] listitems = getResources().getStringArray(R.array.icons_values);
        String[] list = getResources().getStringArray(R.array.icons);
        if (position < list.length - 1) {
            mUserPickedImage = "";
            int sim = Constants.DISABLED;
            switch (logo) {
                case "logo1":
                    sim = Constants.SIM1;
                    break;
                case "logo2":
                    sim = Constants.SIM2;
                    break;
                case "logo3":
                    sim = Constants.SIM3;
                    break;
            }
            String opLogo;
            if (listitems[position].equals("auto"))
                opLogo = MobileUtils.getLogoFromCode(getApplicationContext(), sim);
            else
                opLogo = listitems[position];
            int resourceId = getApplicationContext().getResources().getIdentifier(opLogo, "drawable", getApplicationContext().getPackageName());
            if (logo.equals(Constants.PREF_WIDGET[5])) {
                mEdit.putBoolean(Constants.PREF_WIDGET[8], false);
                mEdit.putString(Constants.PREF_WIDGET[5], opLogo);
                Picasso.with(mContext)
                        .load(resourceId)
                        .resize(mDim, mDim)
                        .centerInside()
                        .error(R.drawable.none)
                        .into(logo1);
                logoSum1.setText(list[position]);
            } else if (logo.equals(Constants.PREF_WIDGET[6])) {
                mEdit.putBoolean(Constants.PREF_WIDGET[9], false);
                mEdit.putString(Constants.PREF_WIDGET[6], opLogo);
                Picasso.with(mContext)
                        .load(resourceId)
                        .resize(mDim, mDim)
                        .centerInside()
                        .error(R.drawable.none)
                        .into(logo2);
                logoSum2.setText(list[position]);
            } else if (logo.equals(Constants.PREF_WIDGET[7])) {
                mEdit.putBoolean(Constants.PREF_WIDGET[10], false);
                mEdit.putString(Constants.PREF_WIDGET[7], opLogo);
                Picasso.with(mContext)
                        .load(resourceId)
                        .resize(mDim, mDim)
                        .centerInside()
                        .error(R.drawable.none)
                        .into(logo3);
                logoSum3.setText(list[position]);
            }
        } else {
            mUserPickedImage = logo;
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
                    if (mUserPickedImage.equals(Constants.PREF_WIDGET[5])) {
                        mEdit.putBoolean(Constants.PREF_WIDGET[8], true);
                        String path = getRealPathFromURI(getApplicationContext(), selectedImage);
                        mEdit.putString(Constants.PREF_WIDGET[5], path);
                        Picasso.with(mContext)
                                .load(new File(path))
                                .resize(mDim, mDim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(logo1);
                        logoSum3.setText(getResources().getString(R.string.userpick));
                    } else if (mUserPickedImage.equals(Constants.PREF_WIDGET[6])) {
                        mEdit.putBoolean(Constants.PREF_WIDGET[9], true);
                        String path = getRealPathFromURI(getApplicationContext(), selectedImage);
                        mEdit.putString(Constants.PREF_WIDGET[6], path);
                        Picasso.with(mContext)
                                .load(new File(path))
                                .resize(mDim, mDim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(logo2);
                        logoSum3.setText(getResources().getString(R.string.userpick));
                    } else if (mUserPickedImage.equals(Constants.PREF_WIDGET[7])) {
                        mEdit.putBoolean(Constants.PREF_WIDGET[10], true);
                        String path = getRealPathFromURI(getApplicationContext(), selectedImage);
                        mEdit.putString(Constants.PREF_WIDGET[7], path);
                        Picasso.with(mContext)
                                .load(new File(path))
                                .resize(mDim, mDim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(logo3);
                        logoSum3.setText(getResources().getString(R.string.userpick));
                    }
                    mUserPickedImage = "";
                }
                break;
        }
    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index;
            if (cursor != null) {
                column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            } else
                return null;
        } catch (Exception e) {
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.names:
                mEdit.putBoolean(Constants.PREF_WIDGET[1], isChecked);
                if (isChecked)
                    namesSum.setText(R.string.on);
                else
                    namesSum.setText(R.string.off);
                break;
            case R.id.info:
                mEdit.putBoolean(Constants.PREF_WIDGET[2], isChecked);
                if (isChecked)
                    infoSum.setText(R.string.all);
                else
                    infoSum.setText(R.string.only_total);
                break;
            case R.id.speed:
                mEdit.putBoolean(Constants.PREF_WIDGET[3], isChecked);
                onOff(speedFontL, isChecked);
                onOff(speedArrowsL, isChecked);
                if (isChecked) {
                    speedSum.setText(R.string.on);
                    speedFontL.setOnClickListener(this);
                    speedArrowsL.setOnClickListener(this);
                }
                else
                    speedSum.setText(R.string.off);
                break;
            case R.id.divider:
                mEdit.putBoolean(Constants.PREF_WIDGET[21], isChecked);
                if (isChecked)
                    divSum.setText(R.string.on);
                else
                    divSum.setText(R.string.off);
                break;
            case R.id.useBack:
                mEdit.putBoolean(Constants.PREF_WIDGET[14], isChecked);
                if (isChecked)
                    backSum.setText(R.string.on);
                else
                    backSum.setText(R.string.off);
                onOff(backColorL, isChecked);
                break;
            case R.id.icons:
                mEdit.putBoolean(Constants.PREF_WIDGET[4], isChecked);
                onOff(logoL1, isChecked);
                onOff(logoL2, isChecked);
                onOff(logoL3, isChecked);
                onOff(simLogoL, isChecked);
                if (isChecked) {
                    iconsSum.setText(R.string.on);
                    simLogoL.setOnClickListener(this);
                    logoL1.setOnClickListener(this);
                    logoL2.setOnClickListener(this);
                    logoL3.setOnClickListener(this);
                }
                else
                    iconsSum.setText(R.string.off);
                break;
            case R.id.activesim:
                mEdit.putBoolean(Constants.PREF_WIDGET[22], isChecked);
                onOff(showSimL, !isChecked);
                if (isChecked)
                    activesum.setText(R.string.on);
                else {
                    activesum.setText(R.string.off);
                    showSimL.setOnClickListener(this);
                }
                break;
            case R.id.daynight_icons:
                mEdit.putBoolean(Constants.PREF_WIDGET[23], isChecked);
                if (isChecked)
                    daynightSum.setText(R.string.on);
                else
                    daynightSum.setText(R.string.off);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        AmbilWarnaDialog dialog = null;
        switch (v.getId()) {
            case R.id.textColorPreview:
                dialog = new AmbilWarnaDialog(mContext, mTextColor, true, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        mEdit.putInt(Constants.PREF_WIDGET[13], color);
                        tiv.setBackgroundColor(color);
                    }
                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // cancel was selected by the user
                    }
                });
                break;
            case R.id.backColorPreview:
                dialog = new AmbilWarnaDialog(mContext, mBackColor, true, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        mEdit.putInt(Constants.PREF_WIDGET[15], color);
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
            case R.id.simFontSize:
            case R.id.simLogoSize:
            case R.id.speedFontSize:
            case R.id.speedArrowsSize:
            case R.id.showSim:
            case R.id.simChoose:
            case R.id.simChooseSum:
            case R.id.textSize:
            case R.id.textSizeSum:
            case R.id.iconSize:
            case R.id.iconSizeSum:
            case R.id.speedIconsSize:
            case R.id.speedIconsSizeSum:
            case R.id.speedTextSize:
            case R.id.speedTextSizeSum:
                showDialog(v);
                break;
        }
        if (dialog != null)
            dialog.show();
    }

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }
}