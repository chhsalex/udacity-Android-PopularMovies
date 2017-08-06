package com.alex.nanoproj.popularmovies.utils;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import java.util.Set;

public class ParcelableParser {

    public static Bundle createBundleFromCursor(Cursor cursor, String[] keys) {
        Bundle bundle = new Bundle();
        for (String key : keys) {
            bundle.putString(key, cursor.getString(cursor.getColumnIndex(key)));
        }
        return bundle;
    }
    
    public static ContentValues createContentValuesFromBundle(Bundle bundle) {
        ContentValues values = new ContentValues();
        
        Set<String> keys = bundle.keySet();
        for(String key: keys) {
            values.put(key, bundle.getString(key));
            Log.e(ParcelableParser.class.getSimpleName(), "put " + key + ": " + bundle.getString(key));
        }
        
        return values;
    }

}
