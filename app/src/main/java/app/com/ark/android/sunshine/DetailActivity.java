package app.com.ark.android.sunshine;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        if (savedInstanceState == null) {
            //create the detail fragment and add it to the activity
            //using a fragment transaction.

            Bundle argments = new Bundle();
            argments.putParcelable(DetailFragment.DETAIL_URI, getIntent().getData());
            argments.putBoolean(DetailFragment.DETAIL_TRANSITION_ANIMATION, true);

            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(argments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.weather_detail_container, fragment)
                    .commit();
            // Being here means we are in animation mode
            supportPostponeEnterTransition();
        }
    }


}
