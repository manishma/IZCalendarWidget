package manishma.github.io.izcalendarwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Instances;
import android.text.format.Time;
import android.widget.RemoteViews;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CalendarAppWidgetProvider extends AppWidgetProvider {

    public static final String[] INSTANCE_PROJECTION = new String[]{
            Instances._ID,
            Instances.TITLE,
            Instances.BEGIN,
            Instances.END,
            Instances.ALL_DAY,
            Instances.START_DAY,
            Instances.END_DAY,
            Instances.DISPLAY_COLOR,
    };

    // The indices for the projection array above.
    private static final int INSTANCE_PROJECTION_ID_INDEX = 0;
    private static final int INSTANCE_PROJECTION_TITLE_INDEX = 1;
    private static final int INSTANCE_PROJECTION_BEGIN_INDEX = 2;
    private static final int INSTANCE_PROJECTION_END_INDEX = 3;
    private static final int INSTANCE_PROJECTION_ALL_DAY_INDEX = 4;
    private static final int INSTANCE_PROJECTION_START_DAY_INDEX = 5;
    private static final int INSTANCE_PROJECTION_END_DAY_INDEX = 6;
    private static final int INSTANCE_PROJECTION_DISPLAY_COLOR_INDEX = 7;

    final class EventData {
        public String Title;
        public long Start;
        public long End;
        public int Color;
        public boolean AllDay;
        public long StartDay;
        public long EndDay;
    }

    public static final long WEEK_MILLIS = 7 * 24 * 3600000;
    public static final String APP_WIDGET_ID_KEY = "appWidgetId";
    public static final String ACTION_NEXT_WEEK = "manishma.github.io.izcalendarwidget.ACTION_NEXT_WEEK";
    public static final String ACTION_PREV_WEEK = "manishma.github.io.izcalendarwidget.ACTION_PREV_WEEK";
    public static final String ACTION_TODAY = "manishma.github.io.izcalendarwidget.ACTION_TODAY";

    private static final Map<Integer, Long> WIDGET_STATES = new HashMap<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        int appWidgetId = intent.getIntExtra(APP_WIDGET_ID_KEY, 0);

        if (ACTION_NEXT_WEEK.equals(intent.getAction())) {
            WIDGET_STATES.put(appWidgetId, getShownWeekDate(appWidgetId) + WEEK_MILLIS);
        } else if (ACTION_PREV_WEEK.equals(intent.getAction())) {
            WIDGET_STATES.put(appWidgetId, getShownWeekDate(appWidgetId) - WEEK_MILLIS);
        } else if (ACTION_TODAY.equals(intent.getAction())) {
            WIDGET_STATES.remove(appWidgetId);
        } else {
            return;
        }

        onUpdate(context, AppWidgetManager.getInstance(context), appWidgetId);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);

        for (int i = 0; i < appWidgetIds.length; i++) {
            WIDGET_STATES.remove(appWidgetIds[i]);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int i = 0; i < appWidgetIds.length; i++) {
            onUpdate(context, appWidgetManager, appWidgetIds[i]);
        }
    }

    private void onUpdate(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        Calendar cal = getWeekStart(appWidgetId);
        long now = new Date().getTime();

        DateFormat dateFormat = new SimpleDateFormat("EEE, MMM d");
        DateFormat toolbarDateFormat = android.text.format.DateFormat.getMediumDateFormat(context);
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);

        long fromMillis = cal.getTimeInMillis();
        long toMillis = fromMillis + WEEK_MILLIS;
        List<EventData> events = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();

        // Construct the query with the desired date range.
        Cursor cur = Instances.query(cr, INSTANCE_PROJECTION, fromMillis, toMillis);
        while (cur.moveToNext()) {

            int color = cur.getInt(INSTANCE_PROJECTION_DISPLAY_COLOR_INDEX);
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[1] = Math.max(hsv[2], .7f);
            hsv[2] = Math.min(hsv[2], .7f);
            color = Color.HSVToColor(hsv);

            EventData event = new EventData();
            event.Title = cur.getString(INSTANCE_PROJECTION_TITLE_INDEX);
            event.Start = cur.getLong(INSTANCE_PROJECTION_BEGIN_INDEX);
            event.End = cur.getLong(INSTANCE_PROJECTION_END_INDEX);
            event.AllDay = cur.getInt(INSTANCE_PROJECTION_ALL_DAY_INDEX) > 0;
            event.StartDay = cur.getLong(INSTANCE_PROJECTION_START_DAY_INDEX);
            event.EndDay = cur.getLong(INSTANCE_PROJECTION_END_DAY_INDEX);
            event.Color = color;
            events.add(event);
        }
        cur.close();

        RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.calendar_appwidget);
        RemoteViews cellTitleMaster = new RemoteViews(context.getPackageName(), R.layout.calendar_cell_title);
        RemoteViews cellRowMaster = new RemoteViews(context.getPackageName(), R.layout.calendar_cell_row);

        widgetView.setOnClickPendingIntent(R.id.prevButton, getPendingSelfIntent(context, ACTION_PREV_WEEK, appWidgetId));
        widgetView.setOnClickPendingIntent(R.id.nextButton, getPendingSelfIntent(context, ACTION_NEXT_WEEK, appWidgetId));
        widgetView.setOnClickPendingIntent(R.id.todayButton, getPendingSelfIntent(context, ACTION_TODAY, appWidgetId));

        String toolbarTitle = toolbarDateFormat.format(cal.getTime());

        int[] cellIds = new int[]{R.id.cell_day_1, R.id.cell_day_2, R.id.cell_day_3, R.id.cell_day_4, R.id.cell_day_5, R.id.cell_day_6};

        for (int j = 0; j < cellIds.length; j++, cal.add(Calendar.DAY_OF_WEEK, 1)) {
            int cellId = cellIds[j];

            long dayStartMillis = cal.getTimeInMillis();
            long dayEndMillis = dayStartMillis + (j < 5 ? 1 : 2) * 24 * 3600000;

            long dayStart = Time.getJulianDay(dayStartMillis, cal.getTimeZone().getOffset(cal.getTimeInMillis()) / 1000);
            long dayEnd = dayStart + (j < 5 ? 1 : 2);

            // Create an Intent to launch Calendar
            Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
            builder.appendPath("time");
            ContentUris.appendId(builder, cal.getTimeInMillis());
            Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            widgetView.setOnClickPendingIntent(cellId, pendingIntent);

            widgetView.removeAllViews(cellId);

            RemoteViews cellTitle = cellTitleMaster.clone();
            String title = dateFormat.format(cal.getTime());
            if(j == 5) {
                cal.add(Calendar.DAY_OF_WEEK, 1);
                title += ", " + dateFormat.format(cal.getTime());
                toolbarTitle += " - " + toolbarDateFormat.format(cal.getTime());
            }
            cellTitle.setTextViewText(R.id.title, title);
            if (now >= dayStartMillis && now < dayEndMillis) {
                cellTitle.setInt(R.id.title, "setBackgroundResource", R.drawable.cell_selected_title_background);
            }
            widgetView.addView(cellId, cellTitle);

            for (int k = 0; k < events.size(); k++) {
                EventData event = events.get(k);

                if (event.AllDay
                        ? ((event.StartDay >= dayStart && event.StartDay < dayEnd) || (event.EndDay >= dayStart && event.EndDay < dayEnd) || (event.StartDay < dayStart && event.EndDay >= dayEnd))
                        : ((event.Start >= dayStartMillis && event.Start < dayEndMillis) || (event.End >= dayStartMillis && event.End < dayEndMillis) || event.Start < dayStartMillis && event.End >= dayEndMillis)) {
                    RemoteViews cellRow = cellRowMaster.clone();
                    if (event.AllDay) {
                        cellRow.setInt(R.id.title, "setBackgroundColor", event.Color);
                        cellRow.setTextViewText(R.id.title, event.Title);
                    } else {
                        cellRow.setTextViewText(R.id.title, timeFormat.format(new Date(event.Start)) + " " + event.Title);
                        cellRow.setTextColor(R.id.title, event.Color);
                    }
                    widgetView.addView(cellId, cellRow);
                }
            }

        }

        widgetView.setTextViewText(R.id.toolbarTitle, toolbarTitle);

        appWidgetManager.updateAppWidget(appWidgetId, widgetView);

    }

    private PendingIntent getPendingSelfIntent(Context context, String action, int appWidgetId) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        intent.putExtra(APP_WIDGET_ID_KEY, appWidgetId);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private static long getShownWeekDate(int appWidgetId) {
        Long currWeek = WIDGET_STATES.get(appWidgetId);
        if(currWeek == null) {
            currWeek = new Date().getTime();
        }

        return currWeek;
    }

    private Calendar getWeekStart(int appWidgetId) {

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(getShownWeekDate(appWidgetId));

        // recet to first day of the week
        cal.add(Calendar.DAY_OF_WEEK, 1 - cal.get(Calendar.DAY_OF_WEEK));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal;
    }

}
