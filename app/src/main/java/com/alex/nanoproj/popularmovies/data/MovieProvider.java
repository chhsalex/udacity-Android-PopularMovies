package com.alex.nanoproj.popularmovies.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;

public class MovieProvider extends ContentProvider{

    private MovieDBHelper mDBHelper;

    private static final SQLiteQueryBuilder sMovieQueryBuilder;

    static {
        sMovieQueryBuilder = new SQLiteQueryBuilder();
        sMovieQueryBuilder.setTables(MovieContract.MovieEntry.TABLE_NAME);
    }

    @Override
    public boolean onCreate() {
        mDBHelper = new MovieDBHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return sMovieQueryBuilder.query(mDBHelper.getReadableDatabase(), projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return MovieContract.MovieEntry.CONTENT_TYPE;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mDBHelper.getWritableDatabase();
        long _id = db.insert(MovieContract.MovieEntry.TABLE_NAME, null, values);
        Uri returnUri;

        if (_id >= 0) {
            returnUri = MovieContract.MovieEntry.buildMovieUri(_id);
        }
        else {
            throw new android.database.SQLException("Failed to insert row into " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mDBHelper.getWritableDatabase();
        int numRows = db.delete(MovieContract.MovieEntry.TABLE_NAME, selection, selectionArgs);

        if (numRows != 0)
            getContext().getContentResolver().notifyChange(uri, null);
        return numRows;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mDBHelper.getWritableDatabase();
        int numRows = db.update(MovieContract.MovieEntry.TABLE_NAME, values, selection, selectionArgs);

        if (numRows != 0)
            getContext().getContentResolver().notifyChange(uri, null);
        return numRows;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mDBHelper.getWritableDatabase();
        db.beginTransaction();

        int returnCount = 0;
        try {
            for (ContentValues value : values) {
                long _id = db.insert(MovieContract.MovieEntry.TABLE_NAME, null, value);
                if (_id != -1) {
                    returnCount++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnCount;
    }
}
