package app.com.ark.android.sunshine;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends CursorAdapter {

    private final int VIEW_TYPE_TODAY =0;
    private final int VIEW_TYPE_FUTURE_DAY=1;
    private static final int VIEW_TYPE_COUNT = 2;
    private boolean mUseTodayLayout;

    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }


    public void setUseTodayLayout(boolean useTodayLayout){
        mUseTodayLayout = useTodayLayout;
    }

    @Override
    public int getItemViewType(int position) {
        return (position==0&&mUseTodayLayout)?VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    /*
        Remember that these views are reused as needed.
    */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int viewType = getItemViewType(cursor.getPosition());
        int layoutId = -1;

       //TODO: Determine layoutId from viewType
        if(viewType==VIEW_TYPE_TODAY)
            layoutId=R.layout.list_item_forecast_today;
        else {
            layoutId=R.layout.list_item_forecast;
        }

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewholder = (ViewHolder) view.getTag();
        int viewType = getItemViewType(cursor.getPosition());
        // Read weather condition ID from cursor
        int weatherId = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        int fallbackIconId;
        switch (viewType) {
            case VIEW_TYPE_TODAY: {
                // Get weather icon
                fallbackIconId = Utility.getArtResourceForWeatherCondition(
                        weatherId);
                break;
            }
            default: {
                // Get weather icon
                fallbackIconId = Utility.getIconResourceForWeatherCondition(
                        weatherId);
                break;
            }
        }

        Glide.with(mContext)
                .load(Utility.getArtUrlForWeatherCondition(mContext, weatherId))
                .error(fallbackIconId)
                .crossFade()
                .into(viewholder.iconView);


        // TODO Read date from cursor
        long dateInMillis = cursor.getLong(ForecastFragment.COL_WEATHER_DATE);
        viewholder.dateView.setText(Utility.getFriendlyDayString(context, dateInMillis));


        // TODO Read weather forecast from cursor
        String description = Utility.getStringForWeatherCondition(context, weatherId);
        //String forecast = cursor.getString(ForecastFragment.COL_WEATHER_DESC);
        viewholder.descriptionView.setText(description);
        viewholder.descriptionView.setContentDescription(context.getString(R.string.a11y_forecast, description));

        // Read high temperature from cursor
        String high = Utility.formatTemperature(
                context, cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP));
        viewholder.highTempView.setText(high);
        viewholder.highTempView.setContentDescription(context.getString(R.string.a11y_high_temp, high));

        // TODO Read low temperature from cursor
        String low = Utility.formatTemperature(
                context, cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP));
        viewholder.lowTempView.setText(low);
        viewholder.lowTempView.setContentDescription(context.getString(R.string.a11y_low_temp, low));

    }

    /**
     * Cache of the children views for a forecast list item.
     */
    public static class ViewHolder {
        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView highTempView;
        public final TextView lowTempView;

        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            descriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            highTempView = (TextView) view.findViewById(R.id.list_item_high_textview);
            lowTempView = (TextView) view.findViewById(R.id.list_item_low_textview);
        }
    }

}