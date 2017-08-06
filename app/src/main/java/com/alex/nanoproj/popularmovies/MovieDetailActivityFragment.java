package com.alex.nanoproj.popularmovies;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.alex.nanoproj.popularmovies.data.MovieContract;
import com.alex.nanoproj.popularmovies.service.MovieService;
import com.alex.nanoproj.popularmovies.utils.JSONParser;
import com.alex.nanoproj.popularmovies.utils.ParcelableParser;
import com.alex.nanoproj.popularmovies.widget.ExpandedListView;
import com.squareup.picasso.Picasso;

import org.json.JSONException;

import java.util.HashMap;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * A placeholder fragment containing a simple view.
 */
public class MovieDetailActivityFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {

    @Bind(R.id.text_title) TextView titleView;
    @Bind(R.id.image_poster) ImageView posterView;
    @Bind(R.id.text_release_date) TextView dateView;
    @Bind(R.id.text_vote_avarage) TextView voteView;
    @Bind(R.id.text_plot_synopsis) TextView plotView;
    @Bind(R.id.button_add_favorite) CheckBox addFavButton;
    @Bind(R.id.list_videos) ListView videoList;
    @Bind(R.id.list_reviews) ExpandedListView reviewList;

    private Bundle mBundle;
    private ShareActionProvider mShareActionProvider;
    private String mFirstTrailerKey = null;

    private String movieId = "";

    private VideosReviewsBroadcastReceiver mReceiver;

    public MovieDetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.setHasOptionsMenu(true);

        View contentView = inflater.inflate(R.layout.fragment_movie_detail, container, false);

        ButterKnife.bind(this, contentView);

        mBundle = getArguments();

        String posterURL = "http://image.tmdb.org/t/p/w342/" + mBundle.getString(MovieContract.MovieEntry.COLUMN_POSTER_PATH);

        titleView.setText(mBundle.getString(MovieContract.MovieEntry.COLUMN_TITLE));
        dateView.setText(mBundle.getString(MovieContract.MovieEntry.COLUMN_RELEASE_DATE));
        voteView.setText(mBundle.getString(MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE) + "/10");
        plotView.setText(mBundle.getString(MovieContract.MovieEntry.COLUMN_OVERVIEW));

        movieId = mBundle.getString(MovieContract.MovieEntry.COLUMN_MOVIE_ID);

        Cursor cursor = getContext().getContentResolver().query(
                MovieContract.MovieEntry.CONTENT_URI,
                null,
                MovieContract.MovieEntry.COLUMN_MOVIE_ID + " = ?",
                new String[] {mBundle.getString(MovieContract.MovieEntry.COLUMN_MOVIE_ID)},
                null
        );
        if (cursor != null && cursor.getCount() > 0) {
            addFavButton.setChecked(true);
        }
        else {
            addFavButton.setChecked(false);
        }
        addFavButton.setOnCheckedChangeListener(this);

        Picasso.with(getActivity())
                .load(posterURL)
                .placeholder(R.drawable.item_empty_poster)
                .error(R.drawable.item_empty_poster)
                .resize(getResources().getDimensionPixelOffset(R.dimen.item_poster_width), getResources().getDimensionPixelOffset(R.dimen.item_poster_height))
                .into(posterView);

        // register receiver for MovieService
        mReceiver = new VideosReviewsBroadcastReceiver();
        IntentFilter mFilter = new IntentFilter(MovieService.ACTION_RETURN_VIDEOS_REVIEWS);
        mFilter.addCategory(Intent.CATEGORY_DEFAULT);
        getContext().registerReceiver(mReceiver, mFilter);

        return contentView;
    }

    @Override
    public void onDestroyView() {
        getContext().unregisterReceiver(mReceiver);
        super.onDestroyView();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // send message to MovieService to fetch videos and reviews
        Intent sendIntent = new Intent(getContext(), MovieService.class);
        sendIntent.putExtra(MovieService.EXTRA_FETCH_TYPE, MovieService.FETCH_TYPE_VIDEOS_REVIEWS)
                .putExtra(MovieService.EXTRA_MOVIE_ID, movieId);
        getContext().startService(sendIntent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_fragment_detail, menu);
        MenuItem menuItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
    }

    private Intent createShareTrailerIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=" + mFirstTrailerKey);
        return shareIntent;
    }

    public static final String VIDEO_COLUMN_NAME = "VIDEO_COLUMN_NAME";
    public static final String VIDEO_COLUMN_KEY = "VIDEO_COLUMN_KEY";

    public static final String REVIEW_COLUMN_AUTHOR = "REVIEW_COLUMN_AUTHOR";
    public static final String REVIEW_COLUMN_CONTENT = "REVIEW_COLUMN_CONTENT";

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.e(MovieDetailActivityFragment.class.getSimpleName(), "onCheckedChanged "+isChecked);
        if (isChecked) {
            getContext().getContentResolver().insert(
                    MovieContract.MovieEntry.CONTENT_URI,
                    ParcelableParser.createContentValuesFromBundle(mBundle)
            );
        }
        else {
            getContext().getContentResolver().delete(
                    MovieContract.MovieEntry.CONTENT_URI,
                    MovieContract.MovieEntry.COLUMN_MOVIE_ID + " = ?",
                    new String[] {mBundle.getString(MovieContract.MovieEntry.COLUMN_MOVIE_ID)}
            );
        }
    }

    public class VideosReviewsBroadcastReceiver extends BroadcastReceiver implements AdapterView.OnItemClickListener {

        @Override
        public void onReceive(Context context, Intent intent) {
            String jsonStrVideos = intent.getStringExtra(MovieService.EXTRA_JSON_VIDEOS);
            try {
                List<HashMap<String,String>> videoMapList = JSONParser.getVideosMapListFromJSON(jsonStrVideos);
                if (videoMapList != null && videoMapList.size() > 0) {
                    mFirstTrailerKey = videoMapList.get(0).get(VIDEO_COLUMN_KEY);
                    Intent shareIntent = createShareTrailerIntent();
                    if (shareIntent != null)
                        mShareActionProvider.setShareIntent(shareIntent);
                }
                SimpleAdapter videoAdapter = new SimpleAdapter(
                        getContext(),
                        videoMapList,
                        R.layout.item_video,
                        new String[]{VIDEO_COLUMN_NAME},
                        new int[]{R.id.text_video_name}
                );
                videoList.setAdapter(videoAdapter);
                videoList.setOnItemClickListener(this);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String jsonStrReviews = intent.getStringExtra(MovieService.EXTRA_JSON_REVIEWS);
            try {
                List<HashMap<String,String>> reviewMapList = JSONParser.getReviewsMapListFromJSON(jsonStrReviews);
                SimpleAdapter reviewAdapter = new SimpleAdapter(
                        getContext(),
                        reviewMapList,
                        R.layout.item_review,
                        new String[]{REVIEW_COLUMN_AUTHOR, REVIEW_COLUMN_CONTENT},
                        new int[]{R.id.text_review_author, R.id.text_review_content}
                );
                reviewList.setAdapter(reviewAdapter);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            HashMap<String,String> videoMap = (HashMap<String,String>)parent.getItemAtPosition(position);
            String videoKey = videoMap.containsKey(VIDEO_COLUMN_KEY) ? videoMap.get(VIDEO_COLUMN_KEY) : null;
            if (videoKey != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + videoKey));
                startActivity(intent);
            }
        }
    }
}
