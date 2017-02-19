package com.arcuscomputing.dictionary;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import com.arcuscomputing.dictionary.io.ArcusDictionary;
import com.arcuscomputing.dictionary.io.DataFileManager;

import com.arcuscomputing.dictionarypro.parent.R;
import com.arcuscomputing.models.WordModel;


public class ArcusWotdWidget extends AppWidgetProvider {

    public static final String REFRESH = "com.arcuscomputing.dictionary.REFRESH";
    public static final String WIDGET_LAUNCH = "com.arcuscomputing.dictionary.WIDGETLAUNCH";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (REFRESH.equals(intent.getAction())) {
            context.startService(new Intent(context, UpdateService.class));
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {

        context.startService(new Intent(context, UpdateService.class));
    }

    public static class UpdateService extends IntentService {

        public UpdateService() {
            super("ArcusWotdWidget$UpdateService");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            ComponentName me = new ComponentName(this, ArcusWotdWidget.class);
            AppWidgetManager mgr = AppWidgetManager.getInstance(this);
            mgr.updateAppWidget(me, buildUpdate(this));

        }

        private RemoteViews buildUpdate(Context context) {

            RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.arcus_wotd_layout);

            String title;
            String definition;
            boolean error = false;

            ArcusDictionary dict = new ArcusDictionary(new DataFileManager(context));
            DataFileManager datafileManager = dict.getDataFileManager();

            if (datafileManager.quickCheck()) {

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                boolean adultFilter = prefs.getBoolean(getString(R.string.pref_adult_key), false);

                WordModel word = dict.getWordOfTheDay(adultFilter);

                title = word.getWord();
                definition = "(" + word.getType() + ") " + word.getDefinition();

            } else {
                title = context.getString(R.string.wotd_default_title);
                definition = context.getString(R.string.word_default_description);
                error = true;
            }

            // Onclick
            Intent intent = new Intent(context, ArcusSearchActivity.class);
            intent.setAction(WIDGET_LAUNCH);

            if (!error) {
                intent.putExtra(DictionaryConstants.INITIAL_WORD, title);
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            updateViews.setOnClickPendingIntent(R.id.widget, pendingIntent);


            Intent refreshIntent = new Intent(context, ArcusWotdWidget.class);
            refreshIntent.setAction(REFRESH);
            PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(this, 0, refreshIntent, 0);
            updateViews.setOnClickPendingIntent(R.id.wotdRefreshIcon, refreshPendingIntent);

            updateViews.setTextViewText(R.id.wotd_title, title);
            updateViews.setTextViewText(R.id.wotd_definition, definition);


            return updateViews;
        }
    }
}
