package app.com.ark.android.sunshine;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.TextView;

import app.com.ark.android.sunshine.data.WeatherContract;
import app.com.ark.android.sunshine.sync.SunshineSyncAdapter;

/**
 * Created by ark on 7/25/2015.
 */

    /**
     * A placeholder fragment containing a simple view.
     */
    public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener{

        private ForecastAdapter mForecastAdapter;
        private static final int FORECAST_LOADER =0;
        private boolean mUseTodayLayout, mAutoSelectView;
        private int mChoiceMode;
        private boolean mHoldForTransition;
        RecyclerView mForecast_entry;
        private long mInitialSelectedDate = -1;
        private static final String SELECTED_KEY = "selected_position";

        private static final String[] FORECAST_COLUMNS = {
                // In this case the id needs to be fully qualified with a table name, since
                // the content provider joins the location & weather tables in the background
                // (both have an _id column)
                // On the one hand, that's annoying.  On the other, you can search the weather table
                // using the location set by the user, which is only in the Location table.
                // So the convenience is worth it.
                WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
                WeatherContract.WeatherEntry.COLUMN_DATE,
                WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
                WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
                WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
                WeatherContract.LocationEntry.COLUMN_COORD_LAT,
                WeatherContract.LocationEntry.COLUMN_COORD_LONG
        };

        // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
        // must change.
        static final int COL_WEATHER_ID = 0;
        static final int COL_WEATHER_DATE = 1;
        static final int COL_WEATHER_DESC = 2;
        static final int COL_WEATHER_MAX_TEMP = 3;
        static final int COL_WEATHER_MIN_TEMP = 4;
        static final int COL_LOCATION_SETTING = 5;
        static final int COL_WEATHER_CONDITION_ID = 6;
        static final int COL_COORD_LAT = 7;
        static final int COL_COORD_LONG = 8;

        public ForecastFragment() {
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (null != mForecast_entry) {
                mForecast_entry.clearOnScrollListeners();
            }
        }

        @Override
        public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
            super.onInflate(activity, attrs, savedInstanceState);
            TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.ForecastFragment,
                    0, 0);
            mChoiceMode = a.getInt(R.styleable.ForecastFragment_android_choiceMode, AbsListView.CHOICE_MODE_NONE);
            mAutoSelectView = a.getBoolean(R.styleable.ForecastFragment_autoSelectView, false);
            mHoldForTransition = a.getBoolean(R.styleable.ForecastFragment_sharedElementTransitions, false);
            a.recycle();
        }

        @Override
        public void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.forecastfragment, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item){
            int id = item.getItemId();
//            if(id==R.id.action_refresh){
//                //updateWeather();
//                refreshWeather();
//                return true;
//            }

            if(id==R.id.action_map){
                openPreferredLocationInMap();
            }

            return super.onOptionsItemSelected(item);
        }

        private void openPreferredLocationInMap(){

            if(null!=mForecastAdapter) {
                Cursor c = mForecastAdapter.getCursor();
                if(null!=c) {
                    c.moveToPosition(0);
                    String posLat = c.getString(COL_COORD_LAT);
                    String posLong = c.getString(COL_COORD_LONG);
                    Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);

//                    Uri geoLocation = Uri.parse("geo:0,0?").buildUpon()
//                            .appendQueryParameter("q", location)
//                            .build();

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(geoLocation);
                    if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        Log.d("location", "Couldn't call" + geoLocation.toString() + "no valid");
                    }
                }
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            mForecastAdapter.onSaveInstanceState(outState);
            super.onSaveInstanceState(outState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            mForecast_entry = (RecyclerView) rootView.findViewById(R.id.recyclerview_forecast);


            // Set the layout manager
            mForecast_entry.setLayoutManager(new LinearLayoutManager(getActivity()));
            View emptyView = rootView.findViewById(R.id.recyclerview_forecast_empty);
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            mForecast_entry.setHasFixedSize(true);

            // The ForecastAdapter will take data from a source and
            // use it to populate the RecyclerView it's attached to.
            mForecastAdapter = new ForecastAdapter(getActivity(), new ForecastAdapter.ForecastAdapterOnClickHandler() {
                @Override
                public void onClick(Long date, ForecastAdapter.ForecastAdapterViewHolder vh) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    ((Callback) getActivity())
                            .onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                                    locationSetting, date),
                                    vh
                            );
                }
            }, emptyView,mChoiceMode);

            mForecast_entry.setAdapter(mForecastAdapter);

            final View parallaxView = rootView.findViewById(R.id.parallax_bar);

            if (null != parallaxView) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    mForecast_entry.addOnScrollListener(new RecyclerView.OnScrollListener() {
                        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                        @Override
                        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                            super.onScrolled(recyclerView, dx, dy);
                            int max = parallaxView.getHeight();
                            if (dy > 0) {
                                parallaxView.setTranslationY(Math.max(-max, parallaxView.getTranslationY() - dy / 2));
                            } else {
                                parallaxView.setTranslationY(Math.min(0, parallaxView.getTranslationY() - dy / 2));
                            }
                        }
                    });
                }
            }

            final AppBarLayout appbarView = (AppBarLayout)rootView.findViewById(R.id.appbar);
            if (null != appbarView) {
                ViewCompat.setElevation(appbarView, 0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mForecast_entry.addOnScrollListener(new RecyclerView.OnScrollListener() {
                        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                            if (0 == mForecast_entry.computeVerticalScrollOffset()) {
                                appbarView.setElevation(0);
                            } else {
                                appbarView.setElevation(appbarView.getTargetElevation());
                            }
                        }
                    });
                }
            }


            if (savedInstanceState != null) {
                mForecastAdapter.onRestoreInstanceState(savedInstanceState);
            }


            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState){
            // We hold for transition here just in-case the activity
            // needs to be re-created. In a standard return transition,
            // this doesn't actually make a difference.
            if ( mHoldForTransition ) {
                getActivity().supportPostponeEnterTransition();
            }
            getLoaderManager().initLoader(FORECAST_LOADER, null, this);
            super.onActivityCreated(savedInstanceState);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String locationSetting = Utility.getPreferredLocation(getActivity());
            // Sort order:  Ascending, by date.
            String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
            Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                    locationSetting, System.currentTimeMillis());
            return new CursorLoader(getActivity(),
                    weatherForLocationUri,
                    FORECAST_COLUMNS,
                    null,
                    null,
                    sortOrder);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor_data) {
            mForecastAdapter.swapCursor(cursor_data);
            updateEmptyView();

            if ( cursor_data.getCount() == 0 ) {
                getActivity().supportStartPostponedEnterTransition();
            }
            else{
                mForecast_entry.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        // Since we know we're going to get items, we keep the listener around until
                        // we see Children.
                        if (mForecast_entry.getChildCount() > 0) {
                            mForecast_entry.getViewTreeObserver().removeOnPreDrawListener(this);
                            int position = mForecastAdapter.getSelectedItemPosition();
                            if (position == RecyclerView.NO_POSITION &&
                                    -1 != mInitialSelectedDate) {
                                Cursor data = mForecastAdapter.getCursor();
                                int count = data.getCount();
                                int dateColumn = data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE);
                                for ( int i = 0; i < count; i++ ) {
                                    data.moveToPosition(i);
                                    if ( data.getLong(dateColumn) == mInitialSelectedDate ) {
                                        position = i;
                                        break;
                                    }
                                }
                            }
                            if (position == RecyclerView.NO_POSITION) position = 0;
                            // If we don't need to restart the loader, and there's a desired position to restore
                            // to, do so now.
                            mForecast_entry.smoothScrollToPosition(position);
                            RecyclerView.ViewHolder vh = mForecast_entry.findViewHolderForAdapterPosition(position);
                            if (null != vh && mAutoSelectView) {
                                mForecastAdapter.selectView(vh);
                            }
                            if ( mHoldForTransition ) {
                                getActivity().supportStartPostponedEnterTransition();
                            }
                            return true;
                        }
                        return false;
                    }
                });
            }

        }

        private void updateEmptyView() {
            if(mForecastAdapter.getItemCount()==0){
                TextView tv = (TextView) getView().findViewById(R.id.recyclerview_forecast_empty);
                if(tv!=null){
                    // if cursor is empty, why? do we have an invalid location
                    int message = R.string.empty_forecast_list;
                    @SunshineSyncAdapter.LocationStatus int location = Utility.getLocationStatus(getActivity());
                    switch (location) {
                        case SunshineSyncAdapter.LOCATION_STATUS_SERVER_DOWN:
                            message = R.string.empty_forecast_list_server_down;
                            break;
                        case SunshineSyncAdapter.LOCATION_STATUS_SERVER_INVALID:
                            message = R.string.empty_forecast_list_server_error;
                            break;
                        case SunshineSyncAdapter.LOCATION_STATUS_INVALID:
                            message= R.string.empty_forecast_list_invalid_location;
                        default:
                            if (!Utility.isNetworkAvailable(getActivity()) ) {
                                message = R.string.empty_forecast_list_no_network;
                            }
                    }
                    tv.setText(message);
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mForecastAdapter.swapCursor(null);

        }

        public void setUseTodayLayout(boolean useTodayLayout) {
            mUseTodayLayout = useTodayLayout;
            if (mForecastAdapter != null) {
                mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
            }
        }

        public void onLocationChanged(){
            //updateWeather();
            refreshWeather();
            getLoaderManager().restartLoader(FORECAST_LOADER,null,this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if ( key.equals(getString(R.string.pref_location_status_key)) ) {
                updateEmptyView();
            }
        }

        @Override
        public void onResume() {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            sp.registerOnSharedPreferenceChangeListener(this);
            super.onResume();
        }

        @Override
        public void onPause() {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            sp.unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        public void setInitialSelectedDate(long initialSelectedDate) {
            mInitialSelectedDate = initialSelectedDate;
        }

        /**
         * A callback interface that all activities containing this fragment must
         * implement. This mechanism allows activities to be notified of item
         * selections.
         */
        public interface Callback {
            /**
             * DetailFragmentCallback for when an item has been selected.
             */
            public void onItemSelected(Uri dateUri, ForecastAdapter.ForecastAdapterViewHolder vh);
        }

        public void refreshWeather(){
//            String mLocation = Utility.getPreferredLocation(getActivity());
//            Intent alarmIntent = new Intent(getActivity(),SunshineService.AlarmReceiver.class);
//            alarmIntent.putExtra(SunshineService.LOCATION_QUERY_EXTRA,mLocation);
//            PendingIntent pi = PendingIntent.getBroadcast(getActivity(),0,alarmIntent,PendingIntent.FLAG_ONE_SHOT);
//
//            AlarmManager am = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
//            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, pi);

            SunshineSyncAdapter.syncImmediately(getActivity());


        }


    }
