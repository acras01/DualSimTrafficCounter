package ua.od.acros.dualsimtrafficcounter.activities;

import android.support.v4.app.DialogFragment;
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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
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

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.dialogs.SetSizeDialog;
import ua.od.acros.dualsimtrafficcounter.dialogs.ShowSimDialog;
import ua.od.acros.dualsimtrafficcounter.fragments.IconsListFragment;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomDatabaseHelper;
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
            speedTextSum, speedIconsSum, showSimSum, divSum, activesum, daynightSum, remainSum, rxtxSum;
    private RelativeLayout simLogoL, speedFontL, speedArrowsL, showSimL, backColorL, logoL1, logoL2, logoL3,
            remainL, rxtxL;
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
    private CheckBox remain, rxtx;
    private Context mContext;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = getApplicationContext();

        if (!CustomApplication.isMyServiceRunning(TrafficCountService.class, mContext))
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
        SharedPreferences prefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
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

        CheckBox names = (CheckBox) findViewById(R.id.names);
        names.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[1], true));
        CheckBox info = (CheckBox) findViewById(R.id.info);
        info.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[2], true));
        CheckBox icons = (CheckBox) findViewById(R.id.icons);
        icons.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[4], true));
        CheckBox speed = (CheckBox) findViewById(R.id.speed);
        speed.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[3], true));
        CheckBox back = (CheckBox) findViewById(R.id.useBack);
        back.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[14], true));
        CheckBox div = (CheckBox) findViewById(R.id.divider);
        div.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[21], true));
        CheckBox active = (CheckBox) findViewById(R.id.activesim);
        active.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[22], false));
        CheckBox daynight = (CheckBox) findViewById(R.id.daynight_icons);
        daynight.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[23], false));
        remain = (CheckBox) findViewById(R.id.remain_data);
        remain.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[24], false));
        rxtx = (CheckBox) findViewById(R.id.rx_tx);
        rxtx.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[25], true));

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
        remainSum = (TextView) findViewById(R.id.remain_data_summary);
        if (remain.isChecked())
            remainSum.setText(R.string.remain);
        else
            remainSum.setText(R.string.used);
        rxtxSum = (TextView) findViewById(R.id.rx_tx_summary);
        if (rxtx.isChecked())
            rxtxSum.setText(R.string.show_rx_tx_sum);
        else
            rxtxSum.setText(R.string.show_used_left);

        logoL1 = (RelativeLayout) findViewById(R.id.logoLayout1);
        logoL2 = (RelativeLayout) findViewById(R.id.logoLayout2);
        logoL3 = (RelativeLayout) findViewById(R.id.logoLayout3);
        RelativeLayout simFontL = (RelativeLayout) findViewById(R.id.simFontSize);
        simLogoL = (RelativeLayout) findViewById(R.id.simLogoSize);
        speedFontL = (RelativeLayout) findViewById(R.id.speedFontSize);
        speedArrowsL = (RelativeLayout) findViewById(R.id.speedArrowsSize);
        showSimL = (RelativeLayout) findViewById(R.id.showSim);
        backColorL = (RelativeLayout) findViewById(R.id.backColorLayout);
        remainL = (RelativeLayout) findViewById(R.id.remainlayout);
        rxtxL = (RelativeLayout) findViewById(R.id.rxtxlayout);

        onOff(logoL1, icons.isChecked());
        onOff(logoL2, mSimQuantity >= 2 && icons.isChecked());
        onOff(logoL3, mSimQuantity == 3 && icons.isChecked());
        onOff(speedFontL, speed.isChecked());
        onOff(speedArrowsL, speed.isChecked());
        onOff(showSimL, !active.isChecked());
        onOff(backColorL, back.isChecked());
        onOff(rxtxL, info.isChecked());
        onOff(remainL, !info.isChecked());

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

        names.setOnCheckedChangeListener(this);
        info.setOnCheckedChangeListener(this);
        icons.setOnCheckedChangeListener(this);
        speed.setOnCheckedChangeListener(this);
        back.setOnCheckedChangeListener(this);
        div.setOnCheckedChangeListener(this);
        active.setOnCheckedChangeListener(this);
        daynight.setOnCheckedChangeListener(this);
        remain.setOnCheckedChangeListener(this);
        rxtx.setOnCheckedChangeListener(this);

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
                if (!CustomDatabaseHelper.isTrafficTableEmpty(CustomDatabaseHelper.getInstance(mContext))) {
                    ContentValues dataMap = CustomDatabaseHelper.readTrafficData(CustomDatabaseHelper.getInstance(mContext));
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
                        mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[5], path);
                        Picasso.with(this)
                                .load(new File(path))
                                .resize(mDim, mDim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(logo1);
                        logoSum3.setText(getResources().getString(R.string.userpick));
                    } else if (mUserPickedImage.equals(Constants.PREF_WIDGET_TRAFFIC[6])) {
                        mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[9], true);
                        String path = getRealPathFromURI(mContext, selectedImage);
                        mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[6], path);
                        Picasso.with(this)
                                .load(new File(path))
                                .resize(mDim, mDim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(logo2);
                        logoSum3.setText(getResources().getString(R.string.userpick));
                    } else if (mUserPickedImage.equals(Constants.PREF_WIDGET_TRAFFIC[7])) {
                        mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[10], true);
                        String path = getRealPathFromURI(mContext, selectedImage);
                        mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[7], path);
                        Picasso.with(this)
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
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[24], !isChecked);
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[25], isChecked);
                onOff(remainL, !isChecked);
                remain.setChecked(!isChecked);
                rxtx.setChecked(isChecked);
                onOff(rxtxL, isChecked);
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
                if (isChecked)
                    rxtxSum.setText(R.string.show_rx_tx_sum);
                else
                    rxtxSum.setText(R.string.show_used_left);
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
