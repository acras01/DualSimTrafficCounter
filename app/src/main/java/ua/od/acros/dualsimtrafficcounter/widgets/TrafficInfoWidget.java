package ua.od.acros.dualsimtrafficcounter.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.RemoteViews;

import com.squareup.picasso.Picasso;

import org.acra.ACRA;

import java.io.File;

import ua.od.acros.dualsimtrafficcounter.activities.WidgetConfigActivity;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.MainActivity;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabase;

public class TrafficInfoWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager widgetManager, int[] widgetId) {
        super.onUpdate(context, widgetManager, widgetId);
        Bundle bundle = new Bundle();
        if (!MyDatabase.isEmpty(new MyDatabase(context, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION))) {
            ContentValues dataMap = MyDatabase.readTrafficData(MyDatabase.getInstance(context));
            bundle.putLong(Constants.SIM1RX, (long) dataMap.get(Constants.SIM1RX));
            bundle.putLong(Constants.SIM2RX, (long) dataMap.get(Constants.SIM2RX));
            bundle.putLong(Constants.SIM3RX, (long) dataMap.get(Constants.SIM3RX));
            bundle.putLong(Constants.SIM1TX, (long) dataMap.get(Constants.SIM1TX));
            bundle.putLong(Constants.SIM2TX, (long) dataMap.get(Constants.SIM2TX));
            bundle.putLong(Constants.SIM3TX, (long) dataMap.get(Constants.SIM3TX));
            bundle.putLong(Constants.TOTAL1, (long) dataMap.get(Constants.TOTAL1));
            bundle.putLong(Constants.TOTAL2, (long) dataMap.get(Constants.TOTAL2));
            bundle.putLong(Constants.TOTAL3, (long) dataMap.get(Constants.TOTAL3));
            bundle.putLong(Constants.SIM1RX_N, (long) dataMap.get(Constants.SIM1RX_N));
            bundle.putLong(Constants.SIM2RX_N, (long) dataMap.get(Constants.SIM2RX_N));
            bundle.putLong(Constants.SIM3RX_N, (long) dataMap.get(Constants.SIM3RX_N));
            bundle.putLong(Constants.SIM1TX_N, (long) dataMap.get(Constants.SIM1TX_N));
            bundle.putLong(Constants.SIM2TX_N, (long) dataMap.get(Constants.SIM2TX_N));
            bundle.putLong(Constants.SIM3TX_N, (long) dataMap.get(Constants.SIM3TX_N));
            bundle.putLong(Constants.TOTAL1_N, (long) dataMap.get(Constants.TOTAL1_N));
            bundle.putLong(Constants.TOTAL2_N, (long) dataMap.get(Constants.TOTAL2_N));
            bundle.putLong(Constants.TOTAL3_N, (long) dataMap.get(Constants.TOTAL3_N));
            bundle.putInt(Constants.SIM_ACTIVE, (int) dataMap.get(Constants.LAST_ACTIVE_SIM));
        } else {
            bundle.putLong(Constants.SIM1RX, 0L);
            bundle.putLong(Constants.SIM2RX, 0L);
            bundle.putLong(Constants.SIM3RX, 0L);
            bundle.putLong(Constants.SIM1TX, 0L);
            bundle.putLong(Constants.SIM2TX, 0L);
            bundle.putLong(Constants.SIM3TX, 0L);
            bundle.putLong(Constants.TOTAL1, 0L);
            bundle.putLong(Constants.TOTAL2, 0L);
            bundle.putLong(Constants.TOTAL3, 0L);
            bundle.putLong(Constants.SIM1RX_N, 0L);
            bundle.putLong(Constants.SIM2RX_N, 0L);
            bundle.putLong(Constants.SIM3RX_N, 0L);
            bundle.putLong(Constants.SIM1TX_N, 0L);
            bundle.putLong(Constants.SIM2TX_N, 0L);
            bundle.putLong(Constants.SIM3TX_N, 0L);
            bundle.putLong(Constants.TOTAL1_N, 0L);
            bundle.putLong(Constants.TOTAL2_N, 0L);
            bundle.putLong(Constants.TOTAL3_N, 0L);
            bundle.putInt(Constants.SIM_ACTIVE, 0);
        }
        updateWidget(context, widgetManager, widgetId, bundle);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int[] widgetIds = intent.getIntArrayExtra(Constants.WIDGET_IDS);
        if (action.equals(AppWidgetManager.ACTION_APPWIDGET_DELETED)) {
            final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                deleteWidgetPreferences(context, new int[]{appWidgetId});
            }
        } else if (action.equals(Constants.BROADCAST_ACTION) && widgetIds != null)
            updateWidget(context, AppWidgetManager.getInstance(context), widgetIds, intent.getExtras());
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int[] ids, Bundle bundle) {

        if (bundle.size() == 0) {
            ContentValues dataMap = MyDatabase.readTrafficData(new MyDatabase(context, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION));
            bundle.putLong(Constants.SIM1RX, (long) dataMap.get(Constants.SIM1RX));
            bundle.putLong(Constants.SIM2RX, (long) dataMap.get(Constants.SIM2RX));
            bundle.putLong(Constants.SIM3RX, (long) dataMap.get(Constants.SIM3RX));
            bundle.putLong(Constants.SIM1TX, (long) dataMap.get(Constants.SIM1TX));
            bundle.putLong(Constants.SIM2TX, (long) dataMap.get(Constants.SIM2TX));
            bundle.putLong(Constants.SIM3TX, (long) dataMap.get(Constants.SIM3TX));
            bundle.putLong(Constants.TOTAL1, (long) dataMap.get(Constants.TOTAL1));
            bundle.putLong(Constants.TOTAL2, (long) dataMap.get(Constants.TOTAL2));
            bundle.putLong(Constants.TOTAL3, (long) dataMap.get(Constants.TOTAL3));
            bundle.putLong(Constants.SIM1RX_N, (long) dataMap.get(Constants.SIM1RX_N));
            bundle.putLong(Constants.SIM2RX_N, (long) dataMap.get(Constants.SIM2RX_N));
            bundle.putLong(Constants.SIM3RX_N, (long) dataMap.get(Constants.SIM3RX_N));
            bundle.putLong(Constants.SIM1TX_N, (long) dataMap.get(Constants.SIM1TX_N));
            bundle.putLong(Constants.SIM2TX_N, (long) dataMap.get(Constants.SIM2TX_N));
            bundle.putLong(Constants.SIM3TX_N, (long) dataMap.get(Constants.SIM3TX_N));
            bundle.putLong(Constants.TOTAL1_N, (long) dataMap.get(Constants.TOTAL1_N));
            bundle.putLong(Constants.TOTAL2_N, (long) dataMap.get(Constants.TOTAL2_N));
            bundle.putLong(Constants.TOTAL3_N, (long) dataMap.get(Constants.TOTAL3_N));
            bundle.putInt(Constants.LAST_ACTIVE_SIM, (int) dataMap.get(Constants.LAST_ACTIVE_SIM));
        }

        for (int i : ids) {
            SharedPreferences prefs = context.getSharedPreferences(i + "_" + Constants.WIDGET_PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences prefsSIM = context.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
            if (prefs.getAll().size() == 0) {
                SharedPreferences.Editor edit = prefs.edit();
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
                edit.putBoolean(Constants.PREF_WIDGET[21], true);
                edit.putBoolean(Constants.PREF_WIDGET[22], false);
                edit.putBoolean(Constants.PREF_WIDGET[23], false);
                edit.apply();
            }

            Intent settIntent = new Intent(context, WidgetConfigActivity.class);
            Bundle extras = new Bundle();
            extras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, i);
            settIntent.putExtras(extras);
            settIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent settPIntent = PendingIntent.getActivity(context, i, settIntent, 0);

            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, i, intent, 0);

            int dim = Integer.parseInt(prefs.getString(Constants.PREF_WIDGET[11], Constants.ICON_SIZE));
            int dims = Integer.parseInt(prefs.getString(Constants.PREF_WIDGET[17], Constants.ICON_SIZE));

            String sizestr = prefs.getString(Constants.PREF_WIDGET[12], Constants.TEXT_SIZE);
            String sizestrs = prefs.getString(Constants.PREF_WIDGET[16], Constants.TEXT_SIZE);

            RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.info_widget_layout);
            boolean[] isNight =  TrafficCountService.getIsNight();

            //SIM1
            if ((prefs.getBoolean(Constants.PREF_WIDGET[18], true) && !prefs.getBoolean(Constants.PREF_WIDGET[22], false))
                    || (prefs.getBoolean(Constants.PREF_WIDGET[22], false) && bundle.getInt(Constants.SIM_ACTIVE) == Constants.SIM1)) {
                updateViews.setTextViewText(R.id.totSIM1, DataFormat.formatData(context, isNight[0] ? bundle.getLong(Constants.TOTAL1_N, 0) :
                        bundle.getLong(Constants.TOTAL1, 0)));
                updateViews.setViewVisibility(R.id.txSIM1, View.GONE);
                updateViews.setViewVisibility(R.id.rxSIM1, View.GONE);
                updateViews.setViewVisibility(R.id.vert11, View.GONE);
                updateViews.setViewVisibility(R.id.vert12, View.GONE);
                if (prefs.getBoolean(Constants.PREF_WIDGET[2], true)) {
                    updateViews.setTextViewText(R.id.txSIM1, DataFormat.formatData(context, isNight[0] ? bundle.getLong(Constants.SIM1TX_N, 0) :
                            bundle.getLong(Constants.SIM1TX, 0)));
                    updateViews.setViewVisibility(R.id.txSIM1, View.VISIBLE);
                    updateViews.setTextViewText(R.id.rxSIM1, DataFormat.formatData(context, isNight[0] ? bundle.getLong(Constants.SIM1RX_N, 0) :
                            bundle.getLong(Constants.SIM1RX, 0)));
                    updateViews.setViewVisibility(R.id.rxSIM1, View.VISIBLE);
                    updateViews.setViewVisibility(R.id.vert11, View.VISIBLE);
                    updateViews.setViewVisibility(R.id.vert12, View.VISIBLE);
                }

                String title1 = "";
                updateViews.setViewVisibility(R.id.operSIM1, View.GONE);
                if (prefs.getBoolean(Constants.PREF_WIDGET[1], true)) {
                    if (bundle.getString(Constants.OPERATOR1, "").equals(""))
                        title1 = "SIM1";
                    else
                        title1 = bundle.getString(Constants.OPERATOR1, "");
                    updateViews.setViewVisibility(R.id.operSIM1, View.VISIBLE);
                    updateViews.setTextViewText(R.id.operSIM1, title1);
                }

                if (prefs.getBoolean(Constants.PREF_WIDGET[4], true)) {
                    if (!prefs.getBoolean(Constants.PREF_WIDGET[8], false))
                        Picasso.with(context)
                                .load(context.getResources().getIdentifier(prefs.getString(Constants.PREF_WIDGET[5], "none"), "drawable", context.getPackageName()))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo1, new int[]{i});
                    else
                        Picasso.with(context)
                                .load(new File(prefs.getString(Constants.PREF_WIDGET[5], "")))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo1, new int[]{i});

                    updateViews.setViewVisibility(R.id.logo1, View.VISIBLE);
                    updateViews.setOnClickPendingIntent(R.id.logo1, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.operSIM1, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.txSIM1, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.rxSIM1, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM1, pendingIntent);
                } else {
                    updateViews.setViewVisibility(R.id.logo1, View.GONE);
                    updateViews.setOnClickPendingIntent(R.id.operSIM1, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.txSIM1, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.rxSIM1, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM1, settPIntent);
                }
                if (prefs.getBoolean(Constants.PREF_WIDGET[23], false) && prefsSIM.getBoolean(Constants.PREF_SIM1[17], false)) {
                    if (!isNight[0])
                        Picasso.with(context)
                                .load(R.drawable.day)
                                .resize(dim / 3, dim / 3)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo1_n, new int[]{i});
                    else
                        Picasso.with(context)
                                .load(R.drawable.night)
                                .resize(dim / 3, dim / 3)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo1_n, new int[]{i});
                updateViews.setViewVisibility(R.id.logo1_n, View.VISIBLE);
            } else
                updateViews.setViewVisibility(R.id.logo1_n, View.GONE);
            if (!sizestr.equals("") && !sizestrs.equals("")) {
                    updateViews.setFloat(R.id.txSIM1, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.rxSIM1, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.totSIM1, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.operSIM1, "setTextSize", Float.parseFloat(sizestr));
                } else {
                    updateViews.setFloat(R.id.txSIM1, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.rxSIM1, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.totSIM1, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.operSIM1, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                }
                updateViews.setInt(R.id.totSIM1, "setTextColor", prefs.getInt(Constants.PREF_WIDGET[13], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setInt(R.id.operSIM1, "setTextColor", prefs.getInt(Constants.PREF_WIDGET[13], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setViewVisibility(R.id.simLayout1, View.VISIBLE);
                if (prefs.getBoolean(Constants.PREF_WIDGET[3], true)) {
                    if (prefs.getBoolean(Constants.PREF_WIDGET[21], true))
                        updateViews.setViewVisibility(R.id.stub1, View.VISIBLE);
                    else
                        updateViews.setViewVisibility(R.id.stub1, View.GONE);
                } else
                    updateViews.setViewVisibility(R.id.stub1, View.GONE);
            } else {
                updateViews.setViewVisibility(R.id.simLayout1, View.GONE);
                updateViews.setViewVisibility(R.id.stub1, View.GONE);
            }

            //SIM2
            if ((prefs.getBoolean(Constants.PREF_WIDGET[19], true) && !prefs.getBoolean(Constants.PREF_WIDGET[22], false))
                    || (prefs.getBoolean(Constants.PREF_WIDGET[22], false) && bundle.getInt(Constants.SIM_ACTIVE) == Constants.SIM2)) {
                updateViews.setTextViewText(R.id.totSIM2, DataFormat.formatData(context, isNight[1] ? bundle.getLong(Constants.TOTAL2_N, 0) :
                        bundle.getLong(Constants.TOTAL2, 0)));
                updateViews.setViewVisibility(R.id.txSIM2, View.GONE);
                updateViews.setViewVisibility(R.id.rxSIM2, View.GONE);
                updateViews.setViewVisibility(R.id.vert21, View.GONE);
                updateViews.setViewVisibility(R.id.vert22, View.GONE);
                if (prefs.getBoolean(Constants.PREF_WIDGET[2], true)) {
                    updateViews.setTextViewText(R.id.txSIM2, DataFormat.formatData(context, isNight[1] ? bundle.getLong(Constants.SIM2TX_N, 0) :
                            bundle.getLong(Constants.SIM2TX, 0)));
                    updateViews.setViewVisibility(R.id.txSIM2, View.VISIBLE);
                    updateViews.setTextViewText(R.id.rxSIM2, DataFormat.formatData(context, isNight[1] ? bundle.getLong(Constants.SIM2RX_N, 0) :
                            bundle.getLong(Constants.SIM2RX, 0)));
                    updateViews.setViewVisibility(R.id.rxSIM2, View.VISIBLE);
                    updateViews.setViewVisibility(R.id.vert21, View.VISIBLE);
                    updateViews.setViewVisibility(R.id.vert22, View.VISIBLE);
                }
                String title2 = "";
                updateViews.setViewVisibility(R.id.operSIM2, View.GONE);
                if (prefs.getBoolean(Constants.PREF_WIDGET[1], true)) {
                    if (bundle.getString(Constants.OPERATOR2, "").equals(""))
                        title2 = "SIM2";
                    else
                        title2 = bundle.getString(Constants.OPERATOR2);
                    updateViews.setViewVisibility(R.id.operSIM2, View.VISIBLE);
                    updateViews.setTextViewText(R.id.operSIM2, title2);
                }

                if (prefs.getBoolean(Constants.PREF_WIDGET[4], true)) {
                    if (!prefs.getBoolean(Constants.PREF_WIDGET[9], false))
                        Picasso.with(context)
                                .load(context.getResources().getIdentifier(prefs.getString(Constants.PREF_WIDGET[6], "none"), "drawable", context.getPackageName()))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo2, new int[]{i});
                    else
                        Picasso.with(context)
                                .load(new File(prefs.getString(Constants.PREF_WIDGET[6], "")))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo2, new int[]{i});

                    updateViews.setViewVisibility(R.id.logo2, View.VISIBLE);
                    updateViews.setOnClickPendingIntent(R.id.logo2, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.operSIM2, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.txSIM2, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.rxSIM2, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM2, pendingIntent);
                } else {
                    updateViews.setViewVisibility(R.id.logo2, View.GONE);
                    updateViews.setOnClickPendingIntent(R.id.operSIM2, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.txSIM2, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.rxSIM2, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM2, settPIntent);
                }
                if (prefs.getBoolean(Constants.PREF_WIDGET[23], false) && prefsSIM.getBoolean(Constants.PREF_SIM2[17], false)) {
                    if (!isNight[1])
                        Picasso.with(context)
                                .load(R.drawable.day)
                                .resize(dim / 3, dim / 3)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo2_n, new int[]{i});
                    else
                        Picasso.with(context)
                                .load(R.drawable.night)
                                .resize(dim / 3, dim / 3)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo2_n, new int[]{i});
                updateViews.setViewVisibility(R.id.logo2_n, View.VISIBLE);
            } else
                updateViews.setViewVisibility(R.id.logo2_n, View.GONE);
            if (!sizestr.equals("") && !sizestrs.equals("")) {
                    updateViews.setFloat(R.id.txSIM2, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.rxSIM2, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.totSIM2, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.operSIM2, "setTextSize", Float.parseFloat(sizestr));
                } else {
                    updateViews.setFloat(R.id.txSIM2, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.rxSIM2, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.totSIM2, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.operSIM2, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                }
                updateViews.setInt(R.id.totSIM2, "setTextColor", prefs.getInt(Constants.PREF_WIDGET[13], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setInt(R.id.operSIM2, "setTextColor", prefs.getInt(Constants.PREF_WIDGET[13], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setViewVisibility(R.id.simLayout2, View.VISIBLE);
                if (prefs.getBoolean(Constants.PREF_WIDGET[3], true) ||
                        (prefs.getBoolean(Constants.PREF_WIDGET[18], true) && !prefs.getBoolean(Constants.PREF_WIDGET[22], false))) {
                    if (prefs.getBoolean(Constants.PREF_WIDGET[21], true))
                        updateViews.setViewVisibility(R.id.stub2, View.VISIBLE);
                    else
                        updateViews.setViewVisibility(R.id.stub2, View.GONE);
                } else
                    updateViews.setViewVisibility(R.id.stub2, View.GONE);
            } else {
                updateViews.setViewVisibility(R.id.simLayout2, View.GONE);
                updateViews.setViewVisibility(R.id.stub2, View.GONE);
            }

            //SIM3
            if ((prefs.getBoolean(Constants.PREF_WIDGET[20], true) && !prefs.getBoolean(Constants.PREF_WIDGET[22], false))
                    || (prefs.getBoolean(Constants.PREF_WIDGET[22], false) && bundle.getInt(Constants.SIM_ACTIVE) == Constants.SIM3)) {
                updateViews.setTextViewText(R.id.totSIM3, DataFormat.formatData(context, isNight[2] ? bundle.getLong(Constants.TOTAL3_N, 0) :
                        bundle.getLong(Constants.TOTAL3, 0)));
                updateViews.setViewVisibility(R.id.txSIM3, View.GONE);
                updateViews.setViewVisibility(R.id.rxSIM3, View.GONE);
                updateViews.setViewVisibility(R.id.vert31, View.GONE);
                updateViews.setViewVisibility(R.id.vert32, View.GONE);
                if (prefs.getBoolean(Constants.PREF_WIDGET[2], true)) {
                    updateViews.setTextViewText(R.id.txSIM3, DataFormat.formatData(context, isNight[2] ? bundle.getLong(Constants.SIM3TX_N, 0) :
                            bundle.getLong(Constants.SIM3TX, 0)));
                    updateViews.setViewVisibility(R.id.txSIM3, View.VISIBLE);
                    updateViews.setTextViewText(R.id.rxSIM3, DataFormat.formatData(context, isNight[2] ? bundle.getLong(Constants.SIM3RX_N, 0) :
                            bundle.getLong(Constants.SIM3RX, 0)));
                    updateViews.setViewVisibility(R.id.rxSIM3, View.VISIBLE);
                    updateViews.setViewVisibility(R.id.vert31, View.VISIBLE);
                    updateViews.setViewVisibility(R.id.vert32, View.VISIBLE);
                }
                String title3 = "";
                if (prefs.getBoolean(Constants.PREF_WIDGET[1], true)) {
                    updateViews.setViewVisibility(R.id.operSIM3, View.GONE);
                    if (bundle.getString(Constants.OPERATOR3, "").equals(""))
                        title3 = "SIM3";
                    else
                        title3 = bundle.getString(Constants.OPERATOR3);
                    updateViews.setViewVisibility(R.id.operSIM3, View.VISIBLE);
                    updateViews.setTextViewText(R.id.operSIM3, title3);
                }

                if (prefs.getBoolean(Constants.PREF_WIDGET[4], true)) {

                    if (!prefs.getBoolean(Constants.PREF_WIDGET[10], false))
                        Picasso.with(context)
                                .load(context.getResources().getIdentifier(prefs.getString(Constants.PREF_WIDGET[7], "none"), "drawable", context.getPackageName()))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo3, new int[]{i});
                    else
                        Picasso.with(context)
                                .load(new File(prefs.getString(Constants.PREF_WIDGET[7], "")))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo3, new int[]{i});

                    updateViews.setViewVisibility(R.id.logo3, View.VISIBLE);
                    updateViews.setOnClickPendingIntent(R.id.logo3, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.operSIM3, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.txSIM3, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.rxSIM3, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM3, pendingIntent);
                } else {
                    updateViews.setViewVisibility(R.id.logo3, View.GONE);
                    updateViews.setOnClickPendingIntent(R.id.operSIM3, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.txSIM3, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.rxSIM3, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM3, settPIntent);
                }
                if (prefs.getBoolean(Constants.PREF_WIDGET[23], false) && prefsSIM.getBoolean(Constants.PREF_SIM3[17], false)) {
                    if (!isNight[2])
                        Picasso.with(context)
                                .load(R.drawable.day)
                                .resize(dim / 3, dim / 3)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo3_n, new int[]{i});
                    else
                        Picasso.with(context)
                                .load(R.drawable.night)
                                .resize(dim / 3, dim / 3)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo3_n, new int[]{i});
                    updateViews.setViewVisibility(R.id.logo3_n, View.VISIBLE);
                } else
                    updateViews.setViewVisibility(R.id.logo3_n, View.GONE);
                if (!sizestr.equals("") && !sizestrs.equals("")) {
                    updateViews.setFloat(R.id.txSIM3, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.rxSIM3, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.totSIM3, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.operSIM3, "setTextSize", Float.parseFloat(sizestr));
                } else {
                    updateViews.setFloat(R.id.txSIM3, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.rxSIM3, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.totSIM3, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.operSIM3, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                }
                updateViews.setInt(R.id.totSIM3, "setTextColor", prefs.getInt(Constants.PREF_WIDGET[13], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setInt(R.id.operSIM3, "setTextColor", prefs.getInt(Constants.PREF_WIDGET[13], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setViewVisibility(R.id.simLayout3, View.VISIBLE);
                if (prefs.getBoolean(Constants.PREF_WIDGET[3], true) || ((prefs.getBoolean(Constants.PREF_WIDGET[18], true) ||
                        prefs.getBoolean(Constants.PREF_WIDGET[19], true)) && !prefs.getBoolean(Constants.PREF_WIDGET[22], false))) {
                    if (prefs.getBoolean(Constants.PREF_WIDGET[21], true))
                        updateViews.setViewVisibility(R.id.stub3, View.VISIBLE);
                    else
                        updateViews.setViewVisibility(R.id.stub3, View.GONE);
                } else
                    updateViews.setViewVisibility(R.id.stub3, View.GONE);
            } else {
                updateViews.setViewVisibility(R.id.simLayout3, View.GONE);
                updateViews.setViewVisibility(R.id.stub3, View.GONE);
            }

            //SPEED
            if (prefs.getBoolean(Constants.PREF_WIDGET[3], false)) {
                updateViews.setViewVisibility(R.id.speedLayout, View.VISIBLE);
                String speedRX = String.format(context.getResources().getString(R.string.speed),
                        DataFormat.formatData(context, bundle.getLong(Constants.SPEEDRX, 0L)));
                String speedTX = String.format(context.getResources().getString(R.string.speed),
                        DataFormat.formatData(context, bundle.getLong(Constants.SPEEDTX, 0L)));

                updateViews.setTextViewText(R.id.tvSpeedRX, speedRX);
                updateViews.setTextViewText(R.id.tvSpeedTX, speedTX);
                updateViews.setOnClickPendingIntent(R.id.tvSpeedRX, settPIntent);
                updateViews.setOnClickPendingIntent(R.id.tvSpeedTX, settPIntent);
                updateViews.setOnClickPendingIntent(R.id.ivRX, settPIntent);
                updateViews.setOnClickPendingIntent(R.id.ivTX, settPIntent);

                Picasso.with(context)
                        .load(R.drawable.rx_arrow)
                        .resize(dims, dims)
                        .centerInside()
                        .error(R.drawable.none)
                        .into(updateViews, R.id.ivRX, new int[]{i});
                Picasso.with(context)
                        .load(R.drawable.tx_arrow)
                        .resize(dims, dims)
                        .centerInside()
                        .error(R.drawable.none)
                        .into(updateViews, R.id.ivTX, new int[]{i});

                if (!sizestr.equals("") && !sizestrs.equals("")) {
                    updateViews.setFloat(R.id.tvSpeedRX, "setTextSize", Float.parseFloat(sizestrs));
                    updateViews.setFloat(R.id.tvSpeedTX, "setTextSize", Float.parseFloat(sizestrs));
                } else {
                    updateViews.setFloat(R.id.tvSpeedRX, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.tvSpeedTX, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                }
                updateViews.setInt(R.id.tvSpeedRX, "setTextColor", prefs.getInt(Constants.PREF_WIDGET[13], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setInt(R.id.tvSpeedTX, "setTextColor", prefs.getInt(Constants.PREF_WIDGET[13], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setViewVisibility(R.id.speedLayout, View.VISIBLE);
            } else
                updateViews.setViewVisibility(R.id.speedLayout, View.GONE);

            //BACKGROUND
            if (prefs.getBoolean(Constants.PREF_WIDGET[14], true)) {
                updateViews.setInt(R.id.background, "setColorFilter", prefs.getInt(Constants.PREF_WIDGET[15], ContextCompat.getColor(context, R.color.background)));
                updateViews.setViewVisibility(R.id.background, View.VISIBLE);
            } else
                updateViews.setViewVisibility(R.id.background, View.GONE);

            //UPDATE
            appWidgetManager.updateAppWidget(i, updateViews);
        }
    }

    private void deleteWidgetPreferences(Context context, int[] appWidgetIds) {
        File dir = new File(context.getFilesDir().getParent() + "/shared_prefs/");
        String[] children = dir.list();
        for (String aChildren : children) {
            for (int j : appWidgetIds)
                if (aChildren.replace(".xml", "").equalsIgnoreCase(String.valueOf(j) + "_" + Constants.WIDGET_PREFERENCES))
                    context.getSharedPreferences(aChildren.replace(".xml", ""), Context.MODE_PRIVATE).edit().clear().commit();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
        for (String aChildren : children) {
            for (int j : appWidgetIds)
                if (aChildren.replace(".xml", "").equalsIgnoreCase(String.valueOf(j) + "_" + Constants.WIDGET_PREFERENCES))
                    new File(dir, aChildren).delete();
        }
    }

    @Override
    public void onDisabled(Context context) {
        Picasso.with(context).shutdown();
        super.onDisabled(context);
    }
}
