package app.com.ark.android.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
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
        private TextView mDateView;
        private TextView mDescriptionView;
        private TextView mHighTempView;
        private TextView mLowTempView;
        private TextView mHumidityView;
        private TextView mHumidityLabelView;
        private TextView mWindView;
        private TextView mWindLabelView;
        private TextView mPressureView;
        private TextView mPressureLabelView;


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
            mDescriptionView = (TextView) rootView.findViewById(R.id.detail_forecast_textview);
            mHighTempView = (TextView) rootView.findViewById(R.id.detail_high_textview);
            mLowTempView = (TextView) rootView.findViewById(R.id.detail_low_textview);
            mHumidityView = (TextView) rootView.findViewById(R.id.detail_humidity_textview);
            mHumidityLabelView = (TextView) rootView.findViewById(R.id.detail_humidity_label_textview);
            mWindView = (TextView) rootView.findViewById(R.id.detail_wind_textview);
            mWindLabelView = (TextView) rootView.findViewById(R.id.detail_wind_label_textview);
            mPressureView = (TextView) rootView.findViewById(R.id.detail_pressure_textview);
            mPressureLabelView = (TextView) rootView.findViewById(R.id.detail_pressure_label_textview);
            return rootView;
        }

        private Intent createShareForecastIntent(){
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, mForecast + FORECAST_SHARE_HASHTAG);
            return shareIntent;
        }

        private void finishCreatingMenu(Menu menu) {
        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);
            menuItem.setIntent(createShareForecastIntent());
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

            if (getActivity() instanceof DetailActivity) {
                // Inflate the menu; this adds items to the action bar if it is present.
                inflater.inflate(R.menu.detailfragment, menu);
                finishCreatingMenu(menu);

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
            getView().setVisibility(View.INVISIBLE);
            return null;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            Log.v(LOG_TAG, "In onLoadFinished");
            if (data!=null&&data.moveToFirst()) {
                getView().setVisibility(View.VISIBLE);
                // Read weather condition ID from cursor
                int weatherId = data.getInt(COL_WEATHER_CONDITION_ID);
                //mIconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));

                if ( Utility.usingLocalGraphics(getActivity()) ) {
                    mIconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));
                } else {
                    // Use weather art image
                    Glide.with(this)
                            .load(Utility.getArtUrlForWeatherCondition(getActivity(), weatherId))
                            .error(Utility.getArtResourceForWeatherCondition(weatherId))
                            .crossFade()
                            .into(mIconView);            }

                // Read date from cursor and update views for day of week and date
                long date = data.getLong(COL_WEATHER_DATE);
                String dateText = Utility.getFullFriendlyDayString(getActivity(), date);
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
                mHumidityView.setContentDescription(getString(R.string.a11y_humidity, mHumidityView.getText()));
                mHumidityLabelView.setContentDescription(mHumidityView.getContentDescription());


                // Read wind speed and direction from cursor and update view
                float windSpeedStr = data.getFloat(COL_WEATHER_WIND_SPEED);
                float windDirStr = data.getFloat(COL_WEATHER_DEGREES);
                mWindView.setText(Utility.getFormattedWind(getActivity(), windSpeedStr, windDirStr));
                mWindView.setContentDescription(getString(R.string.a11y_wind, mWindView.getText()));
                mWindLabelView.setContentDescription(mWindView.getContentDescription());


                // Read pressure from cursor and update view
                float pressure = data.getFloat(COL_WEATHER_PRESSURE);
                mPressureView.setText(getString(R.string.format_pressure, pressure));
                mPressureView.setContentDescription(getString(R.string.a11y_pressure, mPressureView.getText()));
                mPressureLabelView.setContentDescription(mPressureView.getContentDescription());

                // We still need this for the share intent
                mForecast = String.format("%s - %s - %s/%s", dateText, description, high, low);

                // If onCreateOptionsMenu has already happened, we need to update the share intent now.
                if (mShareActionProvider != null) {
                    mShareActionProvider.setShareIntent(createShareForecastIntent());
                }
            }

            AppCompatActivity activity = (AppCompatActivity)getActivity();
            Toolbar toolbarView = (Toolbar) getView().findViewById(R.id.toolbar);
            // We need to start the enter transition after the data has loaded
            if (activity instanceof DetailActivity) {
                activity.supportStartPostponedEnterTransition();
                if ( null != toolbarView ) {
                    activity.setSupportActionBar(toolbarView);
                    activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
                    activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
            } else {
                if ( null != toolbarView ) {
                    Menu menu = toolbarView.getMenu();
                    if ( null != menu ) menu.clear();
                    toolbarView.inflateMenu(R.menu.detailfragment);
                    finishCreatingMenu(toolbarView.getMenu());
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
