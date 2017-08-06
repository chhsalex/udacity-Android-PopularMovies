package com.alex.nanoproj.popularmovies.service;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.alex.nanoproj.popularmovies.BuildConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MovieService extends IntentService{
    private static final String LOG_TAG = MovieService.class.getSimpleName();

    public static final String EXTRA_FETCH_TYPE = "EXTRA_FETCH_TYPE";
    public static final String EXTRA_SORT_BY = "EXTRA_SORT_BY";
    public static final String EXTRA_MOVIE_ID = "EXTRA_MOVIE_ID";

    public static final String ACTION_RETURN_MOVIE_LIST = "com.alex.nanoproj.popularmovies.service.RETURN_MOVIE_LIST";
    public static final String ACTION_RETURN_VIDEOS_REVIEWS = "com.alex.nanoproj.popularmovies.service.RETURN_VIDEOS_REVIEWS";

    public static final String EXTRA_JSON_MOVIE_LIST = "EXTRA_JSON_MOVIE_LIST";
    public static final String EXTRA_JSON_VIDEOS = "EXTRA_JSON_VIDEOS";
    public static final String EXTRA_JSON_REVIEWS = "EXTRA_JSON_REVIEWS";

    public static final int FETCH_TYPE_MOVIE_LIST = 0;
    public static final int FETCH_TYPE_VIDEOS_REVIEWS = 1;

    public static final int API_MOVIE_LIST = 0;
    public static final int API_MOVIE_VIDEOS = 1;
    public static final int API_MOVIE_REVIEWS = 2;

    private int fetchType;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public MovieService() {
        super("PopularMovies");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Intent returnIntent = new Intent();

        fetchType = intent.getIntExtra(EXTRA_FETCH_TYPE, FETCH_TYPE_MOVIE_LIST);
        switch (fetchType) {
            case FETCH_TYPE_MOVIE_LIST:
                returnIntent.setAction(ACTION_RETURN_MOVIE_LIST)
                        .addCategory(Intent.CATEGORY_DEFAULT)
                        .putExtra(EXTRA_JSON_MOVIE_LIST, getJSONStringFromAPI(API_MOVIE_LIST, intent.getStringExtra(EXTRA_SORT_BY)));
                break;
            case FETCH_TYPE_VIDEOS_REVIEWS:
                returnIntent.setAction(ACTION_RETURN_VIDEOS_REVIEWS)
                        .addCategory(Intent.CATEGORY_DEFAULT)
                        .putExtra(EXTRA_JSON_VIDEOS, getJSONStringFromAPI(API_MOVIE_VIDEOS, intent.getStringExtra(EXTRA_MOVIE_ID)))
                        .putExtra(EXTRA_JSON_REVIEWS, getJSONStringFromAPI(API_MOVIE_REVIEWS, intent.getStringExtra(EXTRA_MOVIE_ID)));
                break;
        }

        sendBroadcast(returnIntent);
    }

    /*
        this function calls the apis, and get corresponding JSON strings back

        parameters:
            type: whether this function is fetching a list of movies, or detail info / trailer videos / reviews of a movie
            param: this can be
                1. sort order, if type is FETCH_MOVIE_LIST, or
                2. movie id, if type is one of the others
     */
    private String getJSONStringFromAPI(int type, String param) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String jsonStr;

        Uri.Builder uriBuilder =
                new Uri.Builder()
                        .scheme("http")
                        .authority("api.themoviedb.org")
                        .appendPath("3")
                        .appendPath("movie")
                        .appendPath(param);

        switch (type) {
            case API_MOVIE_LIST:
                break;
            case API_MOVIE_VIDEOS:
                uriBuilder
                        .appendPath("videos");
                break;
            case API_MOVIE_REVIEWS:
                uriBuilder
                        .appendPath("reviews");
                break;
            default:
                return null;
        }
        uriBuilder
                .appendQueryParameter("api_key", BuildConfig.MOVIE_API_KEY);

        try {
            URL url = new URL(uriBuilder.toString());
            Log.e(LOG_TAG, uriBuilder.toString());

            // Create the request to TheMovieDB, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                jsonStr = null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }
            if (buffer.length() == 0) {
                jsonStr = null;
            }
            jsonStr = buffer.toString();
            Log.e(LOG_TAG, jsonStr);

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            jsonStr = null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        return jsonStr;
    }
}
