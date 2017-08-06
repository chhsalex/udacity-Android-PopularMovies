package com.alex.nanoproj.popularmovies;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MoviesAdapter extends CursorAdapter {

    public MoviesAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
    }

    public MoviesAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_grid_movie, parent, false);
        MovieHolder holder = new MovieHolder(view);
        view.setTag(holder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        MovieHolder holder = (MovieHolder)view.getTag();
        String path = cursor.getString(MainActivityFragment.COL_POSTER_PATH);
        Log.e(MoviesAdapter.class.getSimpleName(), "path = " + path);
        String posterURL = "http://image.tmdb.org/t/p/w185/"+path;
        Picasso.with(context)
                .load(posterURL)
                .placeholder(R.drawable.item_empty_poster)
                .error(R.drawable.item_empty_poster)
                .resize(context.getResources().getDimensionPixelOffset(R.dimen.item_poster_width), context.getResources().getDimensionPixelOffset(R.dimen.item_poster_height))
                .into(holder.imageView);
    }

    public class MovieHolder {
        @Bind(R.id.image_poster) ImageView imageView;

        public MovieHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
