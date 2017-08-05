package eu.torsteneriksson.storetracker;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.CalendarView.OnDateChangeListener;
import android.app.Activity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;


public class CalendarActivity extends Activity {
    private final static String TAG = "CalendarActivity";
    private CalendarView calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        //sets the main layout of the activity
        setContentView(R.layout.activity_calendar);

        //initializes the calendarview
        updateDateFontSize();
        initializeCalendar();
    }

    @Override
    protected void onPause() {
        Log.d(TAG,"onPause");
        calendar = (CalendarView) findViewById(R.id.calendar);
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.setTimeInMillis(calendar.getDate());
        // Change the date so it will be the first second of that day.
        selectedDate.set(selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH),
                0, 0, 0);
        MainActivity.getFilter().setMillis(selectedDate.getTimeInMillis());
        MainActivity.getFilter().setTodayOnly(false);
        Log.d(TAG,"onPause:" + TrackerUtilities.getDate(MainActivity.getFilter().getMillis()));
        super.onPause();
    }

    private void initializeCalendar() {
        Log.d(TAG, "initializeCalendar");

        calendar = (CalendarView) findViewById(R.id.calendar);

        // sets whether to show the week number.
        calendar.setShowWeekNumber(true);

        if(MainActivity.getFilter().getMillis() != 0)
            calendar.setDate(MainActivity.getFilter().getMillis());

        // sets the first day of week according to Calendar.
        // here we set Monday as the first day of the Calendar
        calendar.setFirstDayOfWeek(2);

        //The background color for the selected week.
        calendar.setSelectedWeekBackgroundColor(getResources().getColor(R.color.transparent));

        //sets the color for the dates of an unfocused month.
        calendar.setUnfocusedMonthDateColor(getResources().getColor(R.color.transparent));

        // sets the textsize of the dates
        //calendar.setDateTextAppearance(R.attr.textAppearanceListItemSmall);

        //sets the color for the separator line between weeks.
        calendar.setWeekSeparatorLineColor(getResources().getColor(R.color.darkgreen));

        //sets the color for the vertical bar shown at the beginning and at the end of the selected date.
        calendar.setSelectedDateVerticalBar(R.color.darkgreen);

        //sets the listener to be notified upon selected date change.
        calendar.setOnDateChangeListener(new OnDateChangeListener() {
            //show the selected date as a toast
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int day) {
                Log.d(TAG,"onSelectedDayChange");
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(year, month, day, 0, 0, 0);
                MainActivity.getFilter().setMillis(selectedDate.getTimeInMillis());
                MainActivity.getFilter().setTodayOnly(false);
                Log.d(TAG,"onSelectedDayChange mSelectedDateInMillis" +
                        String.valueOf(MainActivity.getFilter().getMillis()));
            }
        });
    }

    private void updateDateFontSize() {
        CalendarView calendarView = (CalendarView) findViewById(R.id.calendar);

        if (true /*Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN*/) { // this bug exists only in Android 4.1
            try {
                Object object = calendarView;
                Field[] fields = object.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if (field.getName().equals("mDelegate")) { // the CalendarViewLegacyDelegate instance is stored in this variable
                        field.setAccessible(true);
                        object = field.get(object);

                        break;
                    }
                }

                Field field = object.getClass().getDeclaredField("mDateTextSize"); // text size integer value
                field.setAccessible(true);
                final int mDateTextSize = (Integer) field.get(object);

                field = object.getClass().getDeclaredField("mListView"); // main ListView
                field.setAccessible(true);
                Object innerObject = field.get(object);

                Method method = innerObject.getClass().getMethod(
                        "setOnHierarchyChangeListener", ViewGroup.OnHierarchyChangeListener.class); // we need to set the OnHierarchyChangeListener
                method.setAccessible(true);
                method.invoke(innerObject, (Object) new ViewGroup.OnHierarchyChangeListener() {
                    @Override
                    public void onChildViewAdded(View parent, View child) { // apply text size every time when a new child view is added
                        try {
                            Object object = child;
                            Field[] fields = object.getClass().getDeclaredFields();
                            for (Field field : fields) {
                                if (field.getName().equals("mMonthNumDrawPaint")) { // the paint is stored inside the view
                                    field.setAccessible(true);
                                    object = field.get(object);
                                    Method method = object.getClass().
                                            getDeclaredMethod("setTextSize", float.class); // finally set text size
                                    method.setAccessible(true);
                                    method.invoke(object, (Object) mDateTextSize);

                                    break;
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage(), e);
                        }
                    }

                    @Override
                    public void onChildViewRemoved(View parent, View child) {
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }
}
