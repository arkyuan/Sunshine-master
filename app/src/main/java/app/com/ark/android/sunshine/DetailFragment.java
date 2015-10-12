package app.com.ark.android.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import app.com.ark.android.sunshine.data.WeatherContract;
import app.com.ark.android.sunshine.data.WeatherContract.WeatherEntry;

/**
 * Created by ark on 8/8/2015.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

        private static final String LOG_TAG = DetailFragment.class.getSimpleName();
        private Uri mUri;
        static final String DETAIL_URI = "URI";
        private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";
        private static final int DETAIL_LOADER =0;

        private static final String[] DETAIL_COLUMNS = {
                // In this case the id needs to be fully qualified with a table name, since
                // the content provider joins the location & weather tables in the background
                // (both have an _id column)
                // On the one hand, that's annoying.  On the other, you can search the weather table
                // using the location set by the user, which is only in the Location table.
                // So the convenience is worth it.
                WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
                WeatherEntry.COLUMN_DATE,
                WeatherEntry.COLUMN_SHORT_DESC,
                WeatherEntry.COLUMN_MAX_TEMP,
                WeatherEntry.COLUMN_MIN_TEMP,
                WeatherEntry.COLUMN_HUMIDITY,
                WeatherEntry.COLUMN_PRESSURE,
                WeatherEntry.COLUMN_WIND_SPEED,
                WeatherEntry.COLUMN_DEGREES,
                WeatherEntry.COLUMN_WEATHER_ID,
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING
        };

        // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
        // must change.
        public static final int COL_WEATHER_ID = 0;
        public static final int COL_WEATHER_DATE = 1;
        public static final int COL_WEATHER_DESC = 2;
        public static final int COL_WEATHER_MAX_TEMP = 3;
        public static final int COL_WEATHER_MIN_TEMP = 4;
        public static final int COL_WEATHER_HUMIDITY = 5;
        public static final int COL_WEATHER_PRESSURE = 6;
        public static final int COL_WEATHER_WIND_SPEED = 7;
        public static final int COL_WEATHER_DEGREES = 8;
        public static final int COL_WEATHER_CONDITION_ID = 9;

        private ImageView mIconView;
        private TextView mFriendlyDateView;
        private TextView mDateView;
        private TextView mDescriptionView;
        private TextView mHighTempView;
        private TextView mLowTempView;
        private TextView mHumidityView;
        private TextView mWindView;
        private TextView mPressureView;


        private ShareActionProvider mShareActionProvider;
        private String mForecast;

        public DetailFragment() {
            setHasOptionsMenu(true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            Bundle arguments = getArguments();
            if(arguments!=null){
                mUri = arguments.getParcelable(DetailFragment.DETAIL_URI);
            }

            View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
            mIconView = (ImageView) rootView.findViewById(R.id.detail_icon);
            mDateView = (TextView) rootView.findViewById(R.id.detail_date_textview);
            mFriendlyDateView = (TextView) rootView.findViewById(R.id.detail_day_textview);
            mDescriptionView = (TextView) rootView.findViewById(R.id.detail_forecast_textview);
            mHighTempView = (TextView) rootView.findViewById(R.id.detail_high_textview);
            mLowTempView = (TextView) rootView.findViewById(R.id.detail_low_textview);
            mHumidityView = (TextView) rootView.findViewById(R.id.detail_humidity_textview);
            mWindView = (TextView) rootView.findViewById(R.id.detail_wind_textview);
            mPressureView = (TextView) rootView.findViewById(R.id.detail_pressure_textview);
            return rootView;
        }

        private Intent createShareForecastIntent(){
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, mForecast + FORECAST_SHARE_HASHTAG);
            return shareIntent;
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){

            inflater.inflate(R.menu.detailfragment, menu);

            MenuItem menuItem = menu.findItem(R.id.menu_item_share);
            // Fetch and store ShareActionProvider
            mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

            if (mForecast != null) {
                mShareActionProvider.setShareIntent(createShareForecastIntent());
            } else {
                Log.d(LOG_TAG, "Share Action Provider is null?");
            }

        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState){
            getLoaderManager().initLoader(DETAIL_LOADER, null, this);
            super.onActivityCreated(savedInstanceState);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Log.v(LOG_TAG, "In onCreateLoader");
            //Now create and return a CursorLoader tha will take care of
            //creating a Cursor for the data being displayed
            if(mUri!=null) {
                return new CursorLoader(
                        getActivity(),
                        mUri,
                        DETAIL_COLUMNS,
                        null,
                        null,
                        null
                );
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            Log.v(LOG_TAG, "In onLoadFinished");
            if (data!=null&&data.moveToFirst()) {
                // Read weather condition ID from cursor
                int weatherId = data.getInt(COL_WEATHER_CONDITION_ID);
                //mIconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));

                Glide.with(this)
                        .load(Utility.getArtUrlForWeatherCondition(getActivity(), weatherId))
                        .error(Utility.getArtResourceForWeatherCondition(weatherId))
                        .crossFade()
                        .into(mIconView);

                // Read date from cursor and update views for day of week and date
                long date = data.getLong(COL_WEATHER_DATE);
                String friendlyDateText = Utility.getDayName(getActivity(), date);
                String dateText = Utility.getFormattedMonthDay(getActivity(), date);
                mFriendlyDateView.setText(friendlyDateText);
                mDateView.setText(dateText);

                // Read description from cursor and update view
                String description = data.getString(COL_WEATHER_DESC);
                mDescriptionView.setText(description);
                mDescriptionView.setContentDescription(getString(R.string.a11y_forecast, description));

                mIconView.setContentDescription(getString(R.string.a11y_forecast_icon, description));

                // Read high temperature from cursor and update view
                boolean isMetric = Utility.isMetric(getActivity());
                double high = data.getDouble(COL_WEATHER_MAX_TEMP);
                String highString = Utility.formatTemperature(getActivity(), high);
                mHighTempView.setText(highString);
                mHighTempView.setContentDescription(getString(R.string.a11y_high_temp, highString));

                // Read low temperature from cursor and update view
                double low = data.getDouble(COL_WEATHER_MIN_TEMP);
                String lowString = Utility.formatTemperature(getActivity(), low);
                mLowTempView.setText(lowString);
                mLowTempView.setContentDescription(getString(R.string.a11y_low_temp, lowString));

                // Read humidity from cursor and update view
                float humidity = data.getFloat(COL_WEATHER_HUMIDITY);
                mHumidityView.setText(getActivity().getString(R.string.format_humidity, humidity));
                mHumidityView.setContentDescription(mHumidityView.getText());
                mWindView.setContentDescription(mWindView.getText());

                // Read wind speed and direction from cursor and update view
                float windSpeedStr = data.getFloat(COL_WEATHER_WIND_SPEED);
                float windDirStr = data.getFloat(COL_WEATHER_DEGREES);
                mWindView.setText(Utility.getFormattedWind(getActivity(), windSpeedStr, windDirStr));
                mPressureView.setContentDescription(mPressureView.getText());

                // Read pressure from cursor and update view
                float pressure = data.getFloat(COL_WEATHER_PRESSURE);
                mPressureView.setText(getActivity().getString(R.string.format_pressure, pressure));

                // We still need this for the share intent
                mForecast = String.format("%s - %s - %s/%s", dateText, description, high, low);

                // If onCreateOptionsMenu has already happened, we need to update the share intent now.
                if (mShareActionProvider != null) {
                    mShareActionProvider.setShareIntent(createShareForecastIntent());
                }
            }

        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }

    void onLocationChanged( String newLocation ) {
        // replace the uri, since the location has changed
        Uri uri = mUri;
        if (null != uri) {
            long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
            Uri updatedUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
            mUri = updatedUri;
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }


    }
