package ua.od.acros.dualsimtrafficcounter.activities;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.acra.ACRA;

import java.io.File;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.dialogs.SetSizeDialog;
import ua.od.acros.dualsimtrafficcounter.dialogs.ShowSimDialog;
import ua.od.acros.dualsimtrafficcounter.fragments.IconsListFragment;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import yuku.ambilwarna.AmbilWarnaDialog;

public class TrafficWidgetConfigActivity extends AppCompatActivity implements IconsListFragment.OnCompleteListener,
        CompoundButton.OnCheckedChangeListener, View.OnClickListener,
        SetSizeDialog.TextSizeDialogListener, ShowSimDialog.ShowSimDialogClosedListener {

    private static final int SELECT_PHOTO = 101;
    private int mWidgetID = AppWidgetManager.INVALID_APPWIDGET_ID;
    private Intent mResultValueIntent;
    private ImageView tiv, biv, logo1, logo2, logo3;
    private TextView infoSum, namesSum, iconsSum, logoSum1, logoSum2,
            logoSum3, textSizeSum, iconsSizeSum, speedSum, backSum,
            speedTextSum, speedIconsSum, showSimSum, divSum, activesum, daynightSum, remainSum, rxtxSum, minusSum;
    private RelativeLayout simLogoL, speedFontL, speedArrowsL, showSimL, backColorL, logoL1, logoL2, logoL3,
            remainL, rxtxL, minusL;
    private SharedPreferences.Editor mEdit;
    private int mTextColor, mBackColor;
    private final int KEY_TEXT = 0;
    private final int KEY_ICON = 1;
    private final int KEY_TEXT_S = 2;
    private final int KEY_ICON_S = 3;
    private int mSimQuantity;
    private int mDim;
    private String mUserPickedImage;
    private boolean[] mSim;
    private Context mContext;
    private AppCompatCheckBox rxtx, remain, minus;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = CustomApplication.getAppContext();

        if (!CustomApplication.isMyServiceRunning(TrafficCountService.class))
            startService(new Intent(this, TrafficCountService.class));

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

        SharedPreferences prefsWidget = getSharedPreferences(String.valueOf(mWidgetID) + Constants.TRAFFIC_TAG + Constants.WIDGET_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (icicle == null) {
            if (prefs.getBoolean(Constants.PREF_OTHER[29], true))
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
            else {
                if (prefs.getBoolean(Constants.PREF_OTHER[28], false))
                    getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                else
                    getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            // Now recreate for it to take effect
            recreate();
        }
        mSimQuantity = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(prefsWidget.getString(Constants.PREF_OTHER[14], "1"));

        mEdit = prefsWidget.edit();
        if (prefsWidget.getAll().size() == 0) {
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[1], true);//Show mNames
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[2], true);//Show full/short info
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[3], false);//Show speed
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[4], true);//Show sim icons
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[5], "none");//SIM1 icon
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[6], "none");//SIM2 icon
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[7], "none");//SIM3 icon
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[8], false);//SIM1 user icon
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[9], false);//SIM2 user icon
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[10], false);//SIM3 user icon
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[11], Constants.ICON_SIZE);//Icon size
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[12], Constants.TEXT_SIZE);//Font size
            mEdit.putInt(Constants.PREF_WIDGET_TRAFFIC[13], Color.WHITE);//Text color
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[14], true);//Use background
            mEdit.putInt(Constants.PREF_WIDGET_TRAFFIC[15], Color.TRANSPARENT);//Background color
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[16], Constants.TEXT_SIZE);//Speed text size
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[17], Constants.ICON_SIZE);//Speed arrows size
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[18], true);//show sim1
            if (mSimQuantity >= 2)
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[19], true);//show sim2
            else
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[19], false);
            if (mSimQuantity == 3)
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[20], true);//Show sim3
            else
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[20], false);
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[21], true);//Show divider
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[22], false);//Show only active SIM
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[23], false);//Show day/night icons
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[24], false);//Show remaining
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[25], true);//Show RX/TX
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[26], true);//Show over-limit traffic
            mEdit.apply();
        }

        mSim = new boolean[]{prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[18], true),
                prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[19], true), prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[20], true)};

        mTextColor = prefsWidget.getInt(Constants.PREF_WIDGET_TRAFFIC[13], Color.WHITE);
        mBackColor = prefsWidget.getInt(Constants.PREF_WIDGET_TRAFFIC[15], Color.TRANSPARENT);

        mResultValueIntent = new Intent();
        mResultValueIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetID);

        setResult(RESULT_CANCELED, mResultValueIntent);

        setContentView(R.layout.traffic_info_widget_configure);
        Toolbar toolBar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);

        AppCompatCheckBox names = (AppCompatCheckBox) findViewById(R.id.names);
        namesSum = (TextView) findViewById(R.id.names_summary);
        if (names != null) {
            names.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[1], true));
            names.setOnCheckedChangeListener(this);
            if (names.isChecked())
                namesSum.setText(R.string.on);
            else
                namesSum.setText(R.string.off);
        }

        AppCompatCheckBox icons = (AppCompatCheckBox) findViewById(R.id.icons);
        iconsSum = (TextView) findViewById(R.id.icons_summary);
        logoL1 = (RelativeLayout) findViewById(R.id.logoLayout1);
        logoL2 = (RelativeLayout) findViewById(R.id.logoLayout2);
        logoL3 = (RelativeLayout) findViewById(R.id.logoLayout3);
        simLogoL = (RelativeLayout) findViewById(R.id.simLogoSize);
        if (icons != null) {
            icons.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[4], true));
            icons.setOnCheckedChangeListener(this);
            onOff(logoL1, icons.isChecked());
            onOff(logoL2, mSimQuantity >= 2 && icons.isChecked());
            onOff(logoL3, mSimQuantity == 3 && icons.isChecked());
            onOff(simLogoL, icons.isChecked());
            if (icons.isChecked())
                iconsSum.setText(R.string.on);
            else
                iconsSum.setText(R.string.off);
        }

        AppCompatCheckBox speed = (AppCompatCheckBox) findViewById(R.id.speed);
        speedSum = (TextView) findViewById(R.id.speed_summary);
        speedFontL = (RelativeLayout) findViewById(R.id.speedFontSize);
        speedArrowsL = (RelativeLayout) findViewById(R.id.speedArrowsSize);
        if (speed != null) {
            speed.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[3], true));
            speed.setOnCheckedChangeListener(this);
            onOff(speedFontL, speed.isChecked());
            onOff(speedArrowsL, speed.isChecked());
            if (speed.isChecked())
                speedSum.setText(R.string.on);
            else
                speedSum.setText(R.string.off);
        }

        AppCompatCheckBox back = (AppCompatCheckBox) findViewById(R.id.useBack);
        backSum = (TextView) findViewById(R.id.back_summary);
        backColorL = (RelativeLayout) findViewById(R.id.backColorLayout);
        if (back != null) {
            back.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[14], true));
            back.setOnCheckedChangeListener(this);
            onOff(backColorL, back.isChecked());
            if (back.isChecked())
                backSum.setText(R.string.on);
            else
                backSum.setText(R.string.off);
        }

        AppCompatCheckBox div = (AppCompatCheckBox) findViewById(R.id.divider);
        divSum = (TextView) findViewById(R.id.divider_summary);
        if (div != null) {
            div.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[21], true));
            div.setOnCheckedChangeListener(this);
            if (div.isChecked())
                divSum.setText(R.string.on);
            else
                divSum.setText(R.string.off);
        }

        AppCompatCheckBox active = (AppCompatCheckBox) findViewById(R.id.activesim);
        activesum = (TextView) findViewById(R.id.activesim_summary);
        showSimL = (RelativeLayout) findViewById(R.id.showSim);
        if (active != null) {
            active.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[22], false));
            active.setOnCheckedChangeListener(this);
            onOff(showSimL, !active.isChecked());
            if (active.isChecked())
                activesum.setText(R.string.on);
            else
                activesum.setText(R.string.off);
        }

        AppCompatCheckBox daynight = (AppCompatCheckBox) findViewById(R.id.daynight_icons);
        daynightSum = (TextView) findViewById(R.id.daynight_icons_summary);
        if (daynight != null) {
            daynight.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[23], false));
            daynight.setOnCheckedChangeListener(this);
            if (daynight.isChecked())
                daynightSum.setText(R.string.on);
            else
                daynightSum.setText(R.string.off);
        }

        remain = (AppCompatCheckBox) findViewById(R.id.remain_data);
        if (remain != null) {
            remain.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[24], false));
            remain.setOnCheckedChangeListener(this);
            remainSum = (TextView) findViewById(R.id.remain_data_summary);
            if (remain.isChecked())
                remainSum.setText(R.string.remain);
            else
                remainSum.setText(R.string.used);
        }

        rxtx = (AppCompatCheckBox) findViewById(R.id.rx_tx);
        rxtx.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[25], true));
        rxtx.setOnCheckedChangeListener(this);
        rxtxSum = (TextView) findViewById(R.id.rx_tx_summary);
        if (rxtx.isChecked())
            rxtxSum.setText(R.string.show_rx_tx_sum);
        else
            rxtxSum.setText(R.string.show_used_left);

        AppCompatCheckBox info = (AppCompatCheckBox) findViewById(R.id.info);
        infoSum = (TextView) findViewById(R.id.info_summary);
        rxtxL = (RelativeLayout) findViewById(R.id.rxtxlayout);
        remainL = (RelativeLayout) findViewById(R.id.remainlayout);
        minusL = (RelativeLayout) findViewById(R.id.minus_layout);
        if (info != null) {
            info.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[2], true));
            info.setOnCheckedChangeListener(this);
            onOff(rxtxL, info.isChecked());
            onOff(remainL, !info.isChecked());
            onOff(minusL, !rxtx.isChecked() && info.isChecked());
            if (info.isChecked())
                infoSum.setText(R.string.all);
            else
                infoSum.setText(R.string.only_total);
        }

        minus = (AppCompatCheckBox) findViewById(R.id.minus);
        minusSum = (TextView) findViewById(R.id.minus_summary);
        if (minus != null) {
            minus.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[26], true));
            minus.setOnCheckedChangeListener(this);
            if (minus.isChecked())
                minusSum.setText(R.string.on);
            else
                minusSum.setText(R.string.off);
        }

        RelativeLayout simFontL = (RelativeLayout) findViewById(R.id.simFontSize);

        textSizeSum = (TextView) findViewById(R.id.textSizeSum);
        textSizeSum.setText(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[12], Constants.TEXT_SIZE));

        iconsSizeSum = (TextView) findViewById(R.id.iconSizeSum);
        iconsSizeSum.setText(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[11], Constants.ICON_SIZE));

        speedTextSum = (TextView) findViewById(R.id.speedTextSizeSum);
        speedTextSum.setText(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[16], Constants.TEXT_SIZE));

        speedIconsSum = (TextView) findViewById(R.id.speedIconsSizeSum);
        speedIconsSum.setText(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[17], Constants.ICON_SIZE));

        showSimSum = (TextView) findViewById(R.id.simChooseSum);
        String sum = "";
        if (prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[18], true))
            sum = "SIM1";
        if (mSimQuantity >= 2 && prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[19], true))
            if (sum.equals(""))
                sum = "SIM2";
            else
                sum += ", SIM2";
        if (mSimQuantity == 3 && prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[20], true))
            if (sum.equals(""))
                sum = "SIM3";
            else
                sum += ", SIM3";
        showSimSum.setText(sum);

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

        if (prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[8], false)) {
            Picasso.with(this)
                    .load(new File(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[5], "")))
                    .resize(mDim, mDim)
                    .centerInside()
                    .error(R.drawable.none)
                    .into(logo1);
            logoSum1.setText(getResources().getString(R.string.userpick));
        } else
            Picasso.with(this)
                    .load(getResources().getIdentifier(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[5], "none"), "drawable", mContext.getPackageName()))
                    .resize(mDim, mDim)
                    .centerInside()
                    .error(R.drawable.none)
                    .into(logo1);
        if (prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[9], false)) {
            Picasso.with(this)
                    .load(new File(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[6], "")))
                    .resize(mDim, mDim)
                    .centerInside()
                    .error(R.drawable.none)
                    .into(logo2);
            logoSum2.setText(getResources().getString(R.string.userpick));
        } else
            Picasso.with(this)
                    .load(getResources().getIdentifier(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[6], "none"), "drawable", mContext.getPackageName()))
                    .resize(mDim, mDim)
                    .centerInside()
                    .error(R.drawable.none)
                    .into(logo2);
        if (prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[10], false)) {
            Picasso.with(this)
                    .load(new File(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[7], "")))
                    .resize(mDim, mDim)
                    .centerInside()
                    .error(R.drawable.none)
                    .into(logo3);
            logoSum3.setText(getResources().getString(R.string.userpick));
        } else
            Picasso.with(this)
                    .load(getResources().getIdentifier(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[7], "none"), "drawable", mContext.getPackageName()))
                    .resize(mDim, mDim)
                    .centerInside()
                    .error(R.drawable.none)
                    .into(logo3);

        String[] listitems = getResources().getStringArray(R.array.icons_values);
        String[] list = getResources().getStringArray(R.array.icons);
        for (int i = 0; i < list.length; i++) {
            if (!prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[8], false) && listitems[i].equals(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[5], "none")))
                logoSum1.setText(list[i]);
            if (!prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[9], false) && listitems[i].equals(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[6], "none")))
                logoSum2.setText(list[i]);
            if (!prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[10], false) && listitems[i].equals(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[7], "none")))
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
        setOnClickListenerWithChild(minusL);
        backColorL.setOnClickListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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
                Intent intent = new Intent(Constants.TRAFFIC_BROADCAST_ACTION);
                intent.putExtra(Constants.WIDGET_IDS, new int[]{mWidgetID});
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

    private void showDialog(View view) {
        DialogFragment dialog = null;
        switch (view.getId()) {
            case R.id.logoPreview1:
                dialog = IconsListFragment.newInstance(Constants.PREF_WIDGET_TRAFFIC[5]);
                break;
            case R.id.logoPreview2:
                dialog = IconsListFragment.newInstance(Constants.PREF_WIDGET_TRAFFIC[6]);
                break;
            case R.id.logoPreview3:
                dialog = IconsListFragment.newInstance(Constants.PREF_WIDGET_TRAFFIC[7]);
                break;
            case R.id.simFontSize:
            case R.id.textSize:
            case R.id.textSizeSum:
                dialog = SetSizeDialog.newInstance(textSizeSum.getText().toString(),
                        KEY_TEXT, Constants.TRAFFIC_TAG);
                break;
            case R.id.simLogoSize:
            case R.id.iconSize:
            case R.id.iconSizeSum:
                dialog = SetSizeDialog.newInstance(iconsSizeSum.getText().toString(),
                        KEY_ICON, Constants.TRAFFIC_TAG);
                break;
            case R.id.speedFontSize:
            case R.id.speedTextSize:
            case R.id.speedTextSizeSum:
                dialog = SetSizeDialog.newInstance(speedTextSum.getText().toString(),
                        KEY_TEXT_S, Constants.TRAFFIC_TAG);
                break;
            case R.id.speedArrowsSize:
            case R.id.speedIconsSize:
            case R.id.speedIconsSizeSum:
                dialog = SetSizeDialog.newInstance(speedIconsSum.getText().toString(),
                        KEY_ICON_S, Constants.TRAFFIC_TAG);
                break;
            case R.id.showSim:
            case R.id.simChoose:
            case R.id.simChooseSum:
                dialog = ShowSimDialog.newInstance(Constants.TRAFFIC_TAG, mSim);
                break;
        }
        if (dialog != null) {
            dialog.show(getSupportFragmentManager(), "dialog");
        }
    }

    @Override
    public void onFinishEditDialog(String inputText, int dialog, String activity) {
        if (activity.equals(Constants.TRAFFIC_TAG) && !inputText.equals("")) {
            switch (dialog) {
                case KEY_TEXT:
                    mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[12], inputText);
                    textSizeSum.setText(inputText);
                    break;
                case KEY_ICON:
                    mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[11], inputText);
                    iconsSizeSum.setText(inputText);
                    break;
                case KEY_TEXT_S:
                    mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[16], inputText);
                    speedTextSum.setText(inputText);
                    break;
                case KEY_ICON_S:
                    mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[17], inputText);
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
                opLogo = MobileUtils.getLogoFromCode(mContext, sim);
            else
                opLogo = listitems[position];
            int resourceId = getResources().getIdentifier(opLogo, "drawable", mContext.getPackageName());
            if (logo.equals(Constants.PREF_WIDGET_TRAFFIC[5])) {
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[8], false);
                mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[5], opLogo);
                Picasso.with(this)
                        .load(resourceId)
                        .resize(mDim, mDim)
                        .centerInside()
                        .error(R.drawable.none)
                        .into(logo1);
                logoSum1.setText(list[position]);
            } else if (logo.equals(Constants.PREF_WIDGET_TRAFFIC[6])) {
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[9], false);
                mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[6], opLogo);
                Picasso.with(this)
                        .load(resourceId)
                        .resize(mDim, mDim)
                        .centerInside()
                        .error(R.drawable.none)
                        .into(logo2);
                logoSum2.setText(list[position]);
            } else if (logo.equals(Constants.PREF_WIDGET_TRAFFIC[7])) {
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[10], false);
                mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[7], opLogo);
                Picasso.with(this)
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
                    if (mUserPickedImage.equals(Constants.PREF_WIDGET_TRAFFIC[5])) {
                        mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[8], true);
                        String path = getRealPathFromURI(mContext, selectedImage);
                        if (path != null) {
                            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[5], path);
                            Picasso.with(this)
                                    .load(new File(path))
                                    .resize(mDim, mDim)
                                    .centerInside()
                                    .error(R.drawable.none)
                                    .into(logo1);
                        }
                        logoSum1.setText(getResources().getString(R.string.userpick));
                    } else if (mUserPickedImage.equals(Constants.PREF_WIDGET_TRAFFIC[6])) {
                        mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[9], true);
                        String path = getRealPathFromURI(mContext, selectedImage);
                        if (path != null) {
                            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[6], path);
                            Picasso.with(this)
                                    .load(new File(path))
                                    .resize(mDim, mDim)
                                    .centerInside()
                                    .error(R.drawable.none)
                                    .into(logo2);
                        }
                        logoSum2.setText(getResources().getString(R.string.userpick));
                    } else if (mUserPickedImage.equals(Constants.PREF_WIDGET_TRAFFIC[7])) {
                        mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[10], true);
                        String path = getRealPathFromURI(mContext, selectedImage);
                        if (path != null) {
                            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[7], path);
                            Picasso.with(this)
                                    .load(new File(path))
                                    .resize(mDim, mDim)
                                    .centerInside()
                                    .error(R.drawable.none)
                                    .into(logo3);
                        }
                        logoSum3.setText(getResources().getString(R.string.userpick));
                    }
                    mUserPickedImage = "";
                }
                break;
        }
    }

    private String getRealPathFromURI(Context context, Uri contentUri) {
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
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[1], isChecked);
                if (isChecked)
                    namesSum.setText(R.string.on);
                else
                    namesSum.setText(R.string.off);
                break;
            case R.id.info:
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[2], isChecked);
                if (isChecked)
                    infoSum.setText(R.string.all);
                else
                    infoSum.setText(R.string.only_total);
                onOff(remainL, !isChecked);
                onOff(rxtxL, isChecked);
                onOff(minusL, isChecked && !rxtx.isChecked());
                if (!isChecked) {
                    mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[26], isChecked);
                    minus.setChecked(isChecked);
                    mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[25], isChecked);
                    rxtx.setChecked(isChecked);
                } else {
                    mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[24], !isChecked);
                    remain.setChecked(!isChecked);
                }
                break;
            case R.id.speed:
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[3], isChecked);
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
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[21], isChecked);
                if (isChecked)
                    divSum.setText(R.string.on);
                else
                    divSum.setText(R.string.off);
                break;
            case R.id.useBack:
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[14], isChecked);
                if (isChecked)
                    backSum.setText(R.string.on);
                else
                    backSum.setText(R.string.off);
                onOff(backColorL, isChecked);
                break;
            case R.id.icons:
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[4], isChecked);
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
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[22], isChecked);
                onOff(showSimL, !isChecked);
                if (isChecked)
                    activesum.setText(R.string.on);
                else {
                    activesum.setText(R.string.off);
                    showSimL.setOnClickListener(this);
                }
                break;
            case R.id.daynight_icons:
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[23], isChecked);
                if (isChecked)
                    daynightSum.setText(R.string.on);
                else
                    daynightSum.setText(R.string.off);
                break;
            case R.id.remain_data:
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[24], isChecked);
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[25], !isChecked);
                if (isChecked)
                    remainSum.setText(R.string.remain);
                else
                    remainSum.setText(R.string.used);
                break;
            case R.id.rx_tx:
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[24], !isChecked);
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[25], isChecked);
                if (isChecked) {
                    minus.setChecked(!isChecked);
                    mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[26], !isChecked);
                }
                onOff(minusL, !isChecked);
                if (isChecked)
                    rxtxSum.setText(R.string.show_rx_tx_sum);
                else
                    rxtxSum.setText(R.string.show_used_left);
                break;
            case R.id.minus:
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[26], isChecked);
                if (isChecked)
                    minusSum.setText(R.string.on);
                else
                    minusSum.setText(R.string.off);
                break;

        }
    }

    @Override
    public void onClick(View v) {
        AmbilWarnaDialog dialog = null;
        switch (v.getId()) {
            case R.id.textColorPreview:
                dialog = new AmbilWarnaDialog(this, mTextColor, true, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        mEdit.putInt(Constants.PREF_WIDGET_TRAFFIC[13], color);
                        tiv.setBackgroundColor(color);
                    }
                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // cancel was selected by the user
                    }
                });
                break;
            case R.id.backColorPreview:
                dialog = new AmbilWarnaDialog(this, mBackColor, true, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        mEdit.putInt(Constants.PREF_WIDGET_TRAFFIC[15], color);
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
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void OnDialogClosed(String activity, boolean[] sim) {
        if (activity.equals(Constants.TRAFFIC_TAG)) {
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[18], sim[0]);
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[19], sim[1]);
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[20], sim[2]);
            mSim = sim;
            String sum = "";
            if (sim[0])
                sum = "SIM1";
            if (sim[1])
                if (sum.equals(""))
                    sum = "SIM2";
                else
                    sum += ", SIM2";
            if (sim[2])
                if (sum.equals(""))
                    sum = "SIM3";
                else
                    sum += ", SIM3";
            showSimSum.setText(sum);
        }
    }
}
