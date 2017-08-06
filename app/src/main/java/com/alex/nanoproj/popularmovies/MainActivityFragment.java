package com.alex.nanoproj.popularmovies;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.alex.nanoproj.popularmovies.data.MovieContract;
import com.alex.nanoproj.popularmovies.service.MovieService;
import com.alex.nanoproj.popularmovies.utils.JSONParser;
import com.alex.nanoproj.popularmovies.utils.ParcelableParser;

import org.json.JSONException;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor>{

    public static final String[] MOVIE_LIST_COLUMNS = {
            MovieContract.MovieEntry._ID,
            MovieContract.MovieEntry.COLUMN_MOVIE_ID,
            MovieContract.MovieEntry.COLUMN_POSTER_PATH,
            MovieContract.MovieEntry.COLUMN_TITLE,
            MovieContract.MovieEntry.COLUMN_RELEASE_DATE,
            MovieContract.MovieEntry.COLUMN_OVERVIEW,
            MovieContract.MovieEntry.COLUMN_POPULARITY,
            MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE
    };

    private static final int MOVIE_FAVORITE_LIST_LOADER = 0;

    static final int COL_ID = 0;
    static final int COL_MOVIE_ID = 1;
    static final int COL_POSTER_PATH = 2;

    public final String PREFERENCE_KEY_SORT_BY = "preference_sort_by";
    public final int PREFERENCE_VALUE_SORT_BY_POPULARITY = 0;
    public final int PREFERENCE_VALUE_SORT_BY_RATING = 1;
    public final int PREFERENCE_VALUE_SORT_BY_FAVORITE = 2;

    public final String QUERY_VALUE_SORT_BY_POPULARITY = "popular";
    public final String QUERY_VALUE_SORT_BY_RATING = "top_rated";

    private final String SELECTED_KEY = "SELECTED_KEY";

    private GridView gridView;
    private MoviesAdapter adapter;
    int mPosition;

    private SharedPreferences mPref;

    private MovieListBroadcastReceiver mReceiver;

    public MainActivityFragment() {
    }

    public interface Callback {
        public void onItemSelected(Bundle bundle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.e(MainActivityFragment.class.getSimpleName(), "++ onCreateView ++");
        this.setHasOptionsMenu(true);
        View fragmentView = inflater.inflate(R.layout.fragment_main, container, false);

        gridView = (GridView)fragmentView.findViewById(R.id.grid);
        adapter = new MoviesAdapter(getActivity(), null, 0);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(this);

        mPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mReceiver = new MovieListBroadcastReceiver();
        IntentFilter mFilter = new IntentFilter(MovieService.ACTION_RETURN_MOVIE_LIST);
        mFilter.addCategory(Intent.CATEGORY_DEFAULT);
        getContext().registerReceiver(mReceiver, mFilter);

        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        return fragmentView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getContext().unregisterReceiver(mReceiver);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.e(MainActivityFragment.class.getSimpleName(), "++ onActivityCreated ++");
        updateMovieInfoList();
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mPosition != GridView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    private void startLoader(int id) {
        Loader<Cursor> loader = getLoaderManager().getLoader(id);
        Log.e(MainActivityFragment.class.getSimpleName(), "id = " + id + " loader = " + loader);
        if (loader != null && loader.isReset())
            getLoaderManager().restartLoader(id, null, this);
        else
            getLoaderManager().initLoader(id, null, this).forceLoad();
    }

    private void updateSortingPreference(int sortBy) {
        SharedPreferences.Editor editor = mPref.edit();
        editor.putInt(PREFERENCE_KEY_SORT_BY, sortBy);
        editor.commit();
    }

    private void updateMovieInfoList() {
        Intent sendIntent;

        // to display favorite movies, run cursor loader to fetch data from content provider
        // to display most popular or highest rated movies, send intent to MovieService to fetch data by calling API
        switch (mPref.getInt(PREFERENCE_KEY_SORT_BY, PREFERENCE_VALUE_SORT_BY_POPULARITY)) {
            case PREFERENCE_VALUE_SORT_BY_FAVORITE:
                startLoader(MOVIE_FAVORITE_LIST_LOADER);
                return;
            case PREFERENCE_VALUE_SORT_BY_POPULARITY:
                sendIntent = new Intent(getContext(), MovieService.class);
                sendIntent.putExtra(MovieService.EXTRA_FETCH_TYPE, MovieService.FETCH_TYPE_MOVIE_LIST)
                        .putExtra(MovieService.EXTRA_SORT_BY, QUERY_VALUE_SORT_BY_POPULARITY);
                getContext().startService(sendIntent);
                break;
            case PREFERENCE_VALUE_SORT_BY_RATING:
                sendIntent = new Intent(getContext(), MovieService.class);
                sendIntent.putExtra(MovieService.EXTRA_FETCH_TYPE, MovieService.FETCH_TYPE_MOVIE_LIST)
                        .putExtra(MovieService.EXTRA_SORT_BY, QUERY_VALUE_SORT_BY_RATING);
                getContext().startService(sendIntent);
                break;
        }

    }

    public class MovieListBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String jsonStr = intent.getStringExtra(MovieService.EXTRA_JSON_MOVIE_LIST);
            try {
                Cursor resultCursor = JSONParser.getMovieListCursorFromJSON(jsonStr, MOVIE_LIST_COLUMNS);
                adapter.swapCursor(resultCursor);
                if (mPosition != GridView.INVALID_POSITION) {
                    gridView.smoothScrollToPosition(mPosition);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mPosition = position;
        ((MainActivity)getActivity())
                .onItemSelected(ParcelableParser.createBundleFromCursor((Cursor) adapter.getItem(position), MOVIE_LIST_COLUMNS));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_fragment_main, menu);
        MenuItem item;
        switch (mPref.getInt(PREFERENCE_KEY_SORT_BY, PREFERENCE_VALUE_SORT_BY_POPULARITY)) {
            case PREFERENCE_VALUE_SORT_BY_POPULARITY:
                item = menu.findItem(R.id.action_sort_popularity);
                break;
            case PREFERENCE_VALUE_SORT_BY_RATING:
                item = menu.findItem(R.id.action_sort_rating);
                break;
            case PREFERENCE_VALUE_SORT_BY_FAVORITE:
                item = menu.findItem(R.id.action_favorite);
                break;
            default:
                return;
        }
        item.setChecked(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        boolean isChecked = item.isChecked();
        if (!isChecked) {
            item.setChecked(true);
            switch (id) {
                case R.id.action_sort_popularity:
                    updateSortingPreference(PREFERENCE_VALUE_SORT_BY_POPULARITY);
                    break;
                case R.id.action_sort_rating:
                    updateSortingPreference(PREFERENCE_VALUE_SORT_BY_RATING);
                    break;
                case R.id.action_favorite:
                    updateSortingPreference(PREFERENCE_VALUE_SORT_BY_FAVORITE);
                    break;
                default:
                    return super.onOptionsItemSelected(item);

            }
            updateMovieInfoList();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.e(MainActivityFragment.class.getSimpleName(), "++ onCreateLoader ++");
        return new CursorLoader(
                getContext(),
                MovieContract.MovieEntry.CONTENT_URI,
                MOVIE_LIST_COLUMNS,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        adapter.swapCursor(cursor);
        if (mPosition != GridView.INVALID_POSITION) {
            gridView.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

}
