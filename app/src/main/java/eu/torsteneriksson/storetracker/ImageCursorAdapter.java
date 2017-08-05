package eu.torsteneriksson.storetracker;

/**
 * Created by torsten on 2015-11-16.
 */
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ImageCursorAdapter extends SimpleCursorAdapter {

    private static final String TAG = "ImageCursorAdapter";
    private Cursor c;
    private Context context;
    static private long mLastRowTime = 999912;

    public ImageCursorAdapter(Context context, int layout, Cursor c,
                              String[] from, int[] to) {
        super(context, layout, c, from, to);
        this.c = c;
        this.context = context;
    }

    public View getView(int pos, View inView, ViewGroup parent) {
        View v = inView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.listrow, null);
        }
        this.c.moveToPosition(pos);
        String headLine;
        Log.d(TAG,"Address ["+this.c.getString(5)+"]");
        Log.d(TAG,"Description ["+this.c.getString(7)+"]");

        if(this.c.getString(7).isEmpty()) {
            headLine = this.c.getString(5);
        } else {
            headLine = this.c.getString(7);
        }

        long time = this.c.getLong(3);
        Log.d(TAG,"time:"+time);
        String timepart = TrackerUtilities.getDateTime(time) + "; " +
                TrackerUtilities.formatSpentTime(this.c.getInt(4)/1000);
        ImageView iv = (ImageView) v.findViewById(R.id.icon);
        iv.setImageResource(FavoriteCategory.getCategoryImage(this.c.getInt(6)));

        TextView addressTextView = (TextView) v.findViewById(R.id.address);
        addressTextView.setText(headLine);
        TextView timeTextView = (TextView) v.findViewById(R.id.time);
        timeTextView.setText(timepart);
        ViewGroup insertPoint = (ViewGroup) v.findViewById(R.id.list_row);
        TextView tmp = (TextView) v.findViewById(R.id.listrow_date);
        long nextTime = 2893456000000L;
        if(!this.c.isFirst()) {
            this.c.moveToPrevious();
            nextTime = this.c.getLong(3);
        }
        if(tmp != null)
            insertPoint.removeView(tmp);
        if(isNewDay(time, nextTime)) {
            addNewMonth(v, TrackerUtilities.getYearMonthString(time));
        }
        c.moveToPrevious();
        return(v);
    }

    private void addNewMonth(View v, String yearMonth) {
        Log.d(TAG,"addNewMonth:"+yearMonth);
        TextView textView1 = new TextView(context);
        textView1.setId(R.id.listrow_date);
        textView1.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        ViewGroup.LayoutParams lp = textView1.getLayoutParams();

        textView1.setPadding(80,0,0,0);
        textView1.setText(yearMonth);
        //textView1.setBackground(context.getResources().getDrawable(R.drawable.border));
        textView1.setBackgroundColor(Color.LTGRAY);
        textView1.setTextColor(Color.BLACK);
        textView1.setTypeface(null, Typeface.BOLD_ITALIC);;
        textView1.setTextSize(15);
        textView1.setId(R.id.listrow_date);
        TextView tmp = (TextView) v.findViewById(R.id.listrow_date);

        if(tmp==null) {
            ViewGroup insertPoint = (ViewGroup) v.findViewById(R.id.list_row);
            insertPoint.addView(textView1, 0, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    private boolean isNewDay(long time, long nextTime) {
        boolean result;
        return (TrackerUtilities.getFullDay(time) < TrackerUtilities.getFullDay(nextTime));
       }
}