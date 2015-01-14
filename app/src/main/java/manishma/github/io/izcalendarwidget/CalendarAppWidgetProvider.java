package manishma.github.io.izcalendarwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;
import android.widget.RemoteViews;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class CalendarAppWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int i = 0; i < appWidgetIds.length; i++) {

            RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.calendar_appwidget);
            RemoteViews cellViewMaster = new RemoteViews(context.getPackageName(), R.layout.calendar_cell);

            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            int today = cal.get(Calendar.DAY_OF_WEEK);
            // recet to first day of the week
            cal.setTimeInMillis(cal.getTimeInMillis() - cal.get(Calendar.DAY_OF_WEEK) * 24 * 60 * 60 * 1000);
            DateFormat format = DateFormat.getDateInstance();

            int[] cellIds = new int[]{R.id.cell_day_1, R.id.cell_day_2, R.id.cell_day_3, R.id.cell_day_4, R.id.cell_day_5, R.id.cell_day_6};

            for (int j = 0; j < cellIds.length; j++) {
                int cellId = cellIds[j];

                cal.add(Calendar.DAY_OF_WEEK, 1);

                // Create an Intent to launch Calendar
                Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                builder.appendPath("time");
                ContentUris.appendId(builder, cal.getTimeInMillis());
                Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

                widgetView.setOnClickPendingIntent(cellId, pendingIntent);

                RemoteViews cellView = cellViewMaster.clone();
                cellView.setTextViewText(R.id.title, format.format(cal.getTime()));
                if(today == cal.get(Calendar.DAY_OF_WEEK)) {
                    cellView.setInt(R.id.title, "setBackgroundResource", android.R.color.holo_orange_dark);
                }

                widgetView.removeAllViews(cellId);
                widgetView.addView(cellId, cellView);

            }

            appWidgetManager.updateAppWidget(appWidgetIds[i], widgetView);
        }
    }
}
