package com.alex.nanoproj.popularmovies.utils;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.util.Log;

import com.alex.nanoproj.popularmovies.MovieDetailActivityFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JSONParser {

    public static Cursor getMovieListCursorFromJSON(String jsonStr, String[] keys) throws JSONException {
        MatrixCursor cursor = new MatrixCursor(keys);
        if (jsonStr == null) {
            return cursor;
        }

        Log.e(JSONParser.class.getSimpleName(), "jsonStr = " + jsonStr);
        JSONObject jObject = new JSONObject(jsonStr);
        JSONArray jArray = jObject.optJSONArray("results");
        int num = jArray.length();

        for (int i = 0; i < num; i++) {
            jObject = (JSONObject)jArray.get(i);
            cursor.addRow(new Object[] {
                    String.valueOf(i),
                    jObject.getString("id"),
                    jObject.getString("poster_path"),
                    jObject.getString("title"),
                    jObject.getString("release_date"),
                    jObject.getString("overview"),
                    jObject.getString("popularity"),
                    jObject.getString("vote_average")
            });
        }

        return cursor;
    }

    public static List<HashMap<String,String>> getVideosMapListFromJSON(String jsonStr) throws JSONException {
        List<HashMap<String,String>> videoMapList = new ArrayList<HashMap<String,String>>();
        if (jsonStr == null) {
            return videoMapList;
        }

        Log.e(JSONParser.class.getSimpleName(), "jsonStr = " + jsonStr);
        JSONObject jObject = new JSONObject(jsonStr);
        JSONArray jArray = jObject.optJSONArray("results");
        int num = jArray.length();

        for (int i = 0; i < num; i++) {
            jObject = (JSONObject)jArray.get(i);
            if (((String) jObject.get("type")).contentEquals("Trailer")) {
                HashMap<String, String> videoMap = new HashMap<String, String>();
                videoMap.put(MovieDetailActivityFragment.VIDEO_COLUMN_NAME, jObject.getString("name"));
                videoMap.put(MovieDetailActivityFragment.VIDEO_COLUMN_KEY, jObject.getString("key"));
                videoMapList.add(videoMap);
            }
        }

        return videoMapList;
    }

    public static List<HashMap<String,String>> getReviewsMapListFromJSON(String jsonStr) throws JSONException {
        List<HashMap<String,String>> reviewMapList = new ArrayList<HashMap<String,String>>();
        if (jsonStr == null) {
            return reviewMapList;
        }

        Log.e(JSONParser.class.getSimpleName(), "jsonStr = " + jsonStr);
        JSONObject jObject = new JSONObject(jsonStr);
        JSONArray jArray = jObject.optJSONArray("results");
        int num = jArray.length();

        for (int i = 0; i < num; i++) {
            jObject = (JSONObject)jArray.get(i);
            HashMap<String,String> reviewMap = new HashMap<String,String>();
            reviewMap.put(MovieDetailActivityFragment.REVIEW_COLUMN_AUTHOR, jObject.getString("author"));
            reviewMap.put(MovieDetailActivityFragment.REVIEW_COLUMN_CONTENT, jObject.getString("content"));
            reviewMapList.add(reviewMap);
        }

        return reviewMapList;
    }

}
