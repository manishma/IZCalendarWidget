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
import java.util.List;


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

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int i = 0; i < appWidgetIds.length; i++) {

            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            int today = cal.get(Calendar.DAY_OF_WEEK);
            // recet to first day of the week
            cal.add(Calendar.DAY_OF_WEEK, 1 - cal.get(Calendar.DAY_OF_WEEK));
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            DateFormat dateFormat = new SimpleDateFormat("EEE, MMM d");
            DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);

            long fromMillis = cal.getTimeInMillis();
            long toMillis = fromMillis + 7 * 24 * 3600000;
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
                }
                cellTitle.setTextViewText(R.id.title, title);
                if (today == cal.get(Calendar.DAY_OF_WEEK)) {
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

            appWidgetManager.updateAppWidget(appWidgetIds[i], widgetView);
        }
    }
}
