package eu.torsteneriksson.storetracker;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

public class FilterActivity extends Activity {
    private final static String TAG  = "FilterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);
        setSelectedDate();
        setSearchTextHandler();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        setSelectedDate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        Log.d(TAG, "onCreateOptionsMenu");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        OptionsMenuHandler omh = new OptionsMenuHandler();
        return(omh.handleOptionsSelected(this,item));
    }

    public void onClickCalendar(View view) {
        Log.d(TAG, "onClickCalendar");
        Intent calendarIntent = new Intent(this, CalendarActivity.class);
        startActivity(calendarIntent);
    }

    public void onClickClearCalendar(View view) {
        Log.d(TAG, "onClickClearCalendar");
        MainActivity.getFilter().setMillis(0);
        MainActivity.getFilter().setTodayOnly(false);
        setSelectedDate();
    }

    public void onToggleTodayOnly(View view) {
        Log.d(TAG, "onToggleTodayOnly");
        ToggleButton tb = (ToggleButton) findViewById(R.id.toogle_today_only);
        if(tb.isChecked()) {
            MainActivity.getFilter().setMillis(TrackerUtilities.getStartOfToday());
            MainActivity.getFilter().setTodayOnly(true);
        } else {
            MainActivity.getFilter().setMillis(0);
            MainActivity.getFilter().setTodayOnly(false);
        }
        setSelectedDate();
        Log.d(TAG, "mSelectedDateInMillis" + String.valueOf(MainActivity.getFilter().getMillis()));
    }


    private void setSelectedDate() {
        TextView selectedDates = (TextView) findViewById(R.id.selected_dates);
        ToggleButton tb = (ToggleButton) findViewById(R.id.toogle_today_only);
        tb.setChecked(MainActivity.getFilter().getTodayOnly());
        if(MainActivity.getFilter().getMillis() != 0)
            if(MainActivity.getFilter().getTodayOnly()) {
                selectedDates.setText(getString(R.string.today));
            } else {
                selectedDates.setText(TrackerUtilities.getDate(MainActivity.getFilter().getMillis()));
            }
        else
            selectedDates.setText(getString(R.string.allDates));
        // Set the Categor Radiobuttons as well.
        RadioGroup rg = (RadioGroup) findViewById(R.id.category_selector);
        switch (MainActivity.getFilter().getCategory()) {
            case -1:
                rg.check(R.id.allCategories);
                break;
            case 0:
                rg.check(R.id.other);
                break;
            case 1:
                rg.check(R.id.store);
                break;
            case 2:
                rg.check(R.id.restaurants);
                break;
            case 3:
                rg.check(R.id.cafe);
                break;
            case 4:
                rg.check(R.id.nature_spot);
                break;
            case 5:
                rg.check(R.id.home);
                break;
            case 6:
                rg.check(R.id.friend);
                break;
            case 7:
                rg.check(R.id.attraction);
                break;
            case 8:
                rg.check(R.id.culture);
                break;
        }
    }

    public void onFilterCategoryClicked(View view) {

        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.allCategories:
                if (checked)
                    MainActivity.getFilter().setFavoriteCategory(-1);
                break;
            case R.id.other:
                if (checked)
                    MainActivity.getFilter().setFavoriteCategory(0);
                break;
            case R.id.store:
                if (checked)
                    MainActivity.getFilter().setFavoriteCategory(1);
                break;
            case R.id.restaurants:
                if (checked)
                    MainActivity.getFilter().setFavoriteCategory(2);
                break;
            case R.id.cafe:
                if (checked)
                    MainActivity.getFilter().setFavoriteCategory(3);
                break;
            case R.id.nature_spot:
                if (checked)
                    MainActivity.getFilter().setFavoriteCategory(4);
                break;
            case R.id.home:
                if (checked)
                    MainActivity.getFilter().setFavoriteCategory(5);
                break;
            case R.id.friend:
                if (checked)
                    MainActivity.getFilter().setFavoriteCategory(6);
                break;
            case R.id.attraction:
                if (checked)
                    MainActivity.getFilter().setFavoriteCategory(7);
                break;
        }
    }
    private void setSearchTextHandler() {
        EditText description = (EditText) findViewById(R.id.filter_search);
        description.setText(MainActivity.getFilter().getSearchText());
        description.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;

                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // Update the database now.
                    Log.d(TAG, "Description updated" + v.getText().toString());
                    MainActivity.getFilter().setSearchText(v.getText().toString());
                    //handled = true;
                }
                //updateEditFavoriteView();
                return handled;
            }
        });
    }
}
