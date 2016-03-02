package ua.od.acros.dualsimtrafficcounter.activities;

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
import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.utils.CheckServiceRunning;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabaseHelper;
import yuku.ambilwarna.AmbilWarnaDialog;

public class CallsWidgetConfigActivity extends AppCompatActivity implements IconsListFragment.OnCompleteListener,
        View.OnClickListener, CompoundButton.OnCheckedChangeListener, SetSizeDialog.TextSizeDialogListener,
        ShowSimDialog.ShowSimDialogClosedListener {

    private static final int SELECT_PHOTO = 101;
    private final int KEY_TEXT = 0;
    private final int KEY_ICON = 1;
    private int mDim;
    private int mWidgetID = AppWidgetManager.INVALID_APPWIDGET_ID;
    private SharedPreferences mPrefs;
    private int mSimQuantity;
    private SharedPreferences.Editor mEdit;
    private Context mContext;
    private int mTextColor;
    private int mBackColor;
    private Intent mResultValueIntent;
    private TextView namesSum, iconsSum, divSum, backSum, textSizeSum, iconsSizeSum, logoSum1, logoSum2, logoSum3, showSimSum, remainSum;
    private RelativeLayout logoL1, logoL2, logoL3, simLogoL, backColorL;
    private ImageView tiv, biv, logo1, logo2, logo3;
    private String mUserPickedImage;
    private boolean[] mSim;

    public CallsWidgetConfigActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = getApplicationContext();

        if (!CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, mContext))
            startService(new Intent(mContext, CallLoggerService.class));

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

        mPrefs = getSharedPreferences(String.valueOf(mWidgetID) + Constants.CALLS_TAG + Constants.WIDGET_PREFERENCES, Context.MODE_PRIVATE);
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
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));
        mEdit = mPrefs.edit();
        if (mPrefs.getAll().size() == 0) {
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[1], true); //Show mNames
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[2], true); //Show icons
            mEdit.putString(Constants.PREF_WIDGET_CALLS[3], "none"); //SIM1 icon
            mEdit.putString(Constants.PREF_WIDGET_CALLS[4], "none"); //SIM2 icon
            mEdit.putString(Constants.PREF_WIDGET_CALLS[5], "none"); //SIM3 icon
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[6], false); //SIM1 user icon
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[7], false); //SIM2 user icon
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[8], false); //SIM3 user icon
            mEdit.putString(Constants.PREF_WIDGET_CALLS[9], Constants.ICON_SIZE); //Icon size
            mEdit.putString(Constants.PREF_WIDGET_CALLS[10], Constants.TEXT_SIZE); //Font size
            mEdit.putInt(Constants.PREF_WIDGET_CALLS[11], Color.WHITE); //Text color
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[12], true); //Use background
            mEdit.putInt(Constants.PREF_WIDGET_CALLS[13], Color.TRANSPARENT); //Background color
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[14], true); //Show divider
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[15], true); //Show SIM1
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[16], true); //Show SIM2
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[17], true); //Show SIM3
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[18], false); //Show remaining
            mEdit.apply();
        }

        mSim = new boolean[]{mPrefs.getBoolean(Constants.PREF_WIDGET_CALLS[15], true),
                mPrefs.getBoolean(Constants.PREF_WIDGET_CALLS[16], true), mPrefs.getBoolean(Constants.PREF_WIDGET_CALLS[17], true)};

        mTextColor = mPrefs.getInt(Constants.PREF_WIDGET_CALLS[11], Color.WHITE);
        mBackColor = mPrefs.getInt(Constants.PREF_WIDGET_CALLS[13], Color.TRANSPARENT);

        mResultValueIntent = new Intent();
        mResultValueIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetID);

        setResult(RESULT_CANCELED, mResultValueIntent);

        setContentView(R.layout.calls_info_widget_configure);
        Toolbar toolBar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);

        CheckBox names = (CheckBox) findViewById(R.id.names);
        names.setChecked(mPrefs.getBoolean(Constants.PREF_WIDGET_CALLS[1], true));
        CheckBox icons = (CheckBox) findViewById(R.id.icons);
        icons.setChecked(mPrefs.getBoolean(Constants.PREF_WIDGET_CALLS[2], true));
        CheckBox back = (CheckBox) findViewById(R.id.useBack);
        back.setChecked(mPrefs.getBoolean(Constants.PREF_WIDGET_CALLS[12], true));
        CheckBox div = (CheckBox) findViewById(R.id.divider);
        div.setChecked(mPrefs.getBoolean(Constants.PREF_WIDGET_CALLS[14], true));
        CheckBox remain = (CheckBox) findViewById(R.id.remain_calls);
        remain.setChecked(mPrefs.getBoolean(Constants.PREF_WIDGET_CALLS[18], false));

        namesSum = (TextView) findViewById(R.id.names_summary);
        if (names.isChecked())
            namesSum.setText(R.string.on);
        else
            namesSum.setText(R.string.off);
        iconsSum = (TextView) findViewById(R.id.icons_summary);
        if (icons.isChecked())
            iconsSum.setText(R.string.on);
        else
            iconsSum.setText(R.string.off);
        divSum = (TextView) findViewById(R.id.divider_summary);
        if (div.isChecked())
            divSum.setText(R.string.on);
        else
            divSum.setText(R.string.off);
        backSum = (TextView) findViewById(R.id.back_summary);
        if (back.isChecked())
            backSum.setText(R.string.on);
        else
            backSum.setText(R.string.off);
        remainSum = (TextView) findViewById(R.id.remain_calls_summary);
        if (remain.isChecked())
            remainSum.setText(R.string.remain);
        else
            remainSum.setText(R.string.used);


        logoL1 = (RelativeLayout) findViewById(R.id.logoLayout1);
        logoL2 = (RelativeLayout) findViewById(R.id.logoLayout2);
        logoL3 = (RelativeLayout) findViewById(R.id.logoLayout3);

        RelativeLayout simFontL = (RelativeLayout) findViewById(R.id.simFontSize);
        simLogoL = (RelativeLayout) findViewById(R.id.simLogoSize);
        RelativeLayout showSimL = (RelativeLayout) findViewById(R.id.showSim);
        backColorL = (RelativeLayout) findViewById(R.id.backColorLayout);


        onOff(logoL1, icons.isChecked());
        onOff(logoL2, mSimQuantity >= 2 && icons.isChecked());
        onOff(logoL3, mSimQuantity == 3 && icons.isChecked());
        onOff(backColorL, back.isChecked());

        showSimSum = (TextView) findViewById(R.id.simChooseSum);
        String sum = "";
        if (mPrefs.getBoolean(Constants.PREF_WIDGET_CALLS[15], true))
            sum = "SIM1";
        if (mSimQuantity >= 2 && mPrefs.getBoolean(Constants.PREF_WIDGET_CALLS[16], true))
            if (sum.equals(""))
                sum = "SIM2";
            else
                sum += ", SIM2";
        if (mSimQuantity == 3 && mPrefs.getBoolean(Constants.PREF_WIDGET_CALLS[17], true))
            if (sum.equals(""))
                sum = "SIM3";
            else
                sum += ", SIM3";
        showSimSum.setText(sum);

        textSizeSum = (TextView) findViewById(R.id.textSizeSum);
        textSizeSum.setText(mPrefs.getString(Constants.PREF_WIDGET_CALLS[10], Constants.TEXT_SIZE));

        iconsSizeSum = (TextView) findViewById(R.id.iconSizeSum);
        iconsSizeSum.setText(mPrefs.getString(Constants.PREF_WIDGET_CALLS[9], Constants.ICON_SIZE));

        names.setOnCheckedChangeListener(this);
        icons.setOnCheckedChangeListener(this);
        back.setOnCheckedChangeListener(this);
        remain.setOnCheckedChangeListener(this);

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

        if (mPrefs.getBoolean(Constants.PREF_WIDGET_CALLS[6], false)) {
            Picasso.with(this)
                    .load(new File(mPrefs.getString(Constants.PREF_WIDGET_CALLS[3], "")))
                    .resize(mDim, mDim)
                    .centerInside()
                    .error(R.drawable.none)
                    .into(logo1);
            logoSum1.setText(getResources().getString(R.string.userpick));
        } else
            Picasso.with(this)
                    .load(getResources().getIdentifier(mPrefs.getString(Constants.PREF_WIDGET_CALLS[3], "none"), "drawable", mContext.getPackageName()))
                    .resize(mDim, mDim)
                    .centerInside()
                    .error(R.drawable.none)
                    .into(logo1);
        if (mPrefs.getBoolean(Constants.PREF_WIDGET_CALLS[7], false)) {
            Picasso.with(this)
                    .load(new File(mPrefs.getString(Constants.PREF_WIDGET_CALLS[4], "")))
                    .resize(mDim, mDim)
                    .centerInside()
                    .error(R.drawable.none)
                    .into(logo2);
            logoSum2.setText(getResources().getString(R.string.userpick));
        } else
            Picasso.with(this)
                    .load(getResources().getIdentifier(mPrefs.getString(Constants.PREF_WIDGET_CALLS[4], "none"), "drawable", mContext.getPackageName()))
                    .resize(mDim, mDim)
                    .centerInside()
                    .error(R.drawable.none)
                    .into(logo2);
        if (mPrefs.getBoolean(Constants.PREF_WIDGET_CALLS[8], false)) {
            Picasso.with(this)
                    .load(new File(mPrefs.getString(Constants.PREF_WIDGET_CALLS[5], "")))
                    .resize(mDim, mDim)
                    .centerInside()
                    .error(R.drawable.none)
                    .into(logo3);
            logoSum3.setText(getResources().getString(R.string.userpick));
        } else
            Picasso.with(this)
                    .load(getResources().getIdentifier(mPrefs.getString(Constants.PREF_WIDGET_CALLS[5], "none"), "drawable", mContext.getPackageName()))
                    .resize(mDim, mDim)
                    .centerInside()
                    .error(R.drawable.none)
                    .into(logo3);

        String[] listitems = getResources().getStringArray(R.array.icons_values);
        String[] list = getResources().getStringArray(R.array.icons);
        for (int i = 0; i < list.length; i++) {
            if (!mPrefs.getBoolean(Constants.PREF_WIDGET_CALLS[6], false) && listitems[i].equals(mPrefs.getString(Constants.PREF_WIDGET_CALLS[3], "none")))
                logoSum1.setText(list[i]);
            if (!mPrefs.getBoolean(Constants.PREF_WIDGET_CALLS[7], false) && listitems[i].equals(mPrefs.getString(Constants.PREF_WIDGET_CALLS[4], "none")))
                logoSum2.setText(list[i]);
            if (!mPrefs.getBoolean(Constants.PREF_WIDGET_CALLS[8], false) && listitems[i].equals(mPrefs.getString(Constants.PREF_WIDGET_CALLS[5], "none")))
                logoSum3.setText(list[i]);
        }

        tiv.setOnClickListener(this);
        biv.setOnClickListener(this);
        logo1.setOnClickListener(this);
        logo2.setOnClickListener(this);
        logo3.setOnClickListener(this);
        setOnClickListenerWithChild(simFontL);
        setOnClickListenerWithChild(simLogoL);
        setOnClickListenerWithChild(showSimL);
        backColorL.setOnClickListener(this);
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

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.widget_config_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            if (item.getItemId() == R.id.save) {
                mEdit.apply();
                Intent intent = new Intent(Constants.CALLS_BROADCAST_ACTION);
                intent.putExtra(Constants.WIDGET_IDS, new int[]{mWidgetID});
                if (!MyDatabaseHelper.isCallsTableEmpty(MyDatabaseHelper.getInstance(mContext))) {
                    ContentValues dataMap = MyDatabaseHelper.readCallsData(MyDatabaseHelper.getInstance(mContext));
                    intent.putExtra(Constants.CALLS1, (long) dataMap.get(Constants.CALLS1));
                    intent.putExtra(Constants.CALLS2, (long) dataMap.get(Constants.CALLS2));
                    intent.putExtra(Constants.CALLS3, (long) dataMap.get(Constants.CALLS3));
                    intent.putExtra(Constants.OPERATOR1, MobileUtils.getName(this, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1));
                    if (mSimQuantity >= 2)
                        intent.putExtra(Constants.OPERATOR2, MobileUtils.getName(this, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2));
                    if (mSimQuantity == 3)
                        intent.putExtra(Constants.OPERATOR3, MobileUtils.getName(this, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3));
                } else {
                    intent.putExtra(Constants.CALLS1, 0L);
                    intent.putExtra(Constants.CALLS2, 0L);
                    intent.putExtra(Constants.CALLS3, 0L);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
            if (logo.equals(Constants.PREF_WIDGET_CALLS[3])) {
                mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[6], false);
                mEdit.putString(Constants.PREF_WIDGET_CALLS[3], opLogo);
                Picasso.with(this)
                        .load(resourceId)
                        .resize(mDim, mDim)
                        .centerInside()
                        .error(R.drawable.none)
                        .into(logo1);
                logoSum1.setText(list[position]);
            } else if (logo.equals(Constants.PREF_WIDGET_CALLS[4])) {
                mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[7], false);
                mEdit.putString(Constants.PREF_WIDGET_CALLS[4], opLogo);
                Picasso.with(this)
                        .load(resourceId)
                        .resize(mDim, mDim)
                        .centerInside()
                        .error(R.drawable.none)
                        .into(logo2);
                logoSum2.setText(list[position]);
            } else if (logo.equals(Constants.PREF_WIDGET_CALLS[5])) {
                mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[8], false);
                mEdit.putString(Constants.PREF_WIDGET_CALLS[5], opLogo);
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
                    if (mUserPickedImage.equals(Constants.PREF_WIDGET_CALLS[5])) {
                        mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[8], true);
                        String path = getRealPathFromURI(mContext, selectedImage);
                        mEdit.putString(Constants.PREF_WIDGET_CALLS[5], path);
                        Picasso.with(this)
                                .load(new File(path))
                                .resize(mDim, mDim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(logo1);
                        logoSum3.setText(getResources().getString(R.string.userpick));
                    } else if (mUserPickedImage.equals(Constants.PREF_WIDGET_CALLS[6])) {
                        mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[9], true);
                        String path = getRealPathFromURI(mContext, selectedImage);
                        mEdit.putString(Constants.PREF_WIDGET_CALLS[6], path);
                        Picasso.with(this)
                                .load(new File(path))
                                .resize(mDim, mDim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(logo2);
                        logoSum3.setText(getResources().getString(R.string.userpick));
                    } else if (mUserPickedImage.equals(Constants.PREF_WIDGET_CALLS[7])) {
                        mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[10], true);
                        String path = getRealPathFromURI(mContext, selectedImage);
                        mEdit.putString(Constants.PREF_WIDGET_CALLS[7], path);
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
                mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[1], isChecked);
                if (isChecked)
                    namesSum.setText(R.string.on);
                else
                    namesSum.setText(R.string.off);
                break;
            case R.id.divider:
                mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[14], isChecked);
                if (isChecked)
                    divSum.setText(R.string.on);
                else
                    divSum.setText(R.string.off);
                break;
            case R.id.useBack:
                mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[12], isChecked);
                if (isChecked)
                    backSum.setText(R.string.on);
                else
                    backSum.setText(R.string.off);
                onOff(backColorL, isChecked);
                break;
            case R.id.remain_calls:
                mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[18], isChecked);
                if (isChecked)
                    remainSum.setText(R.string.remain);
                else
                    remainSum.setText(R.string.used);
                break;
            case R.id.icons:
                mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[2], isChecked);
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
                        mEdit.putInt(Constants.PREF_WIDGET_CALLS[11], color);
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
                        mEdit.putInt(Constants.PREF_WIDGET_CALLS[13], color);
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
            case R.id.textSize:
            case R.id.textSizeSum:
            case R.id.iconSize:
            case R.id.iconSizeSum:
            case R.id.showSim:
            case R.id.simChoose:
            case R.id.simChooseSum:
                showDialog(v);
                break;
        }
        if (dialog != null)
            dialog.show();
    }

    private void showDialog(View view) {
        DialogFragment dialog = null;
        switch (view.getId()) {
            case R.id.logoPreview1:
                dialog = IconsListFragment.newInstance(Constants.PREF_WIDGET_CALLS[3]);
                break;
            case R.id.logoPreview2:
                dialog = IconsListFragment.newInstance(Constants.PREF_WIDGET_CALLS[4]);
                break;
            case R.id.logoPreview3:
                dialog = IconsListFragment.newInstance(Constants.PREF_WIDGET_CALLS[5]);
                break;
            case R.id.simFontSize:
            case R.id.textSize:
            case R.id.textSizeSum:
                dialog = SetSizeDialog.newInstance(mPrefs.getString(Constants.PREF_WIDGET_CALLS[10],Constants.TEXT_SIZE),
                        KEY_TEXT, Constants.CALLS_TAG);
                break;
            case R.id.simLogoSize:
            case R.id.iconSize:
            case R.id.iconSizeSum:
                dialog = SetSizeDialog.newInstance(mPrefs.getString(Constants.PREF_WIDGET_CALLS[9], Constants.ICON_SIZE),
                        KEY_ICON, Constants.CALLS_TAG);
                break;
            case R.id.showSim:
            case R.id.simChoose:
            case R.id.simChooseSum:
                dialog = ShowSimDialog.newInstance(Constants.CALLS_TAG, mSim);
                break;
        }
        if (dialog != null) {
            dialog.show(getFragmentManager(), "dialog");
        }
    }

    public void onFinishEditDialog(String inputText, int dialog, String activity) {
        if (activity.equals(Constants.CALLS_TAG) && !inputText.equals("")) {
            switch (dialog) {
                case KEY_TEXT:
                    mEdit.putString(Constants.PREF_WIDGET_CALLS[10], inputText);
                    textSizeSum.setText(inputText);
                    break;
                case KEY_ICON:
                    mEdit.putString(Constants.PREF_WIDGET_CALLS[9], inputText);
                    iconsSizeSum.setText(inputText);
                    break;
            }
        }
    }

    @Override
    public void OnDialogClosed(String activity, boolean[] sim) {
        if (activity.equals(Constants.CALLS_TAG)) {
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[15], sim[0]);
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[16], sim[1]);
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[17], sim[2]);
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

