package com.botpa.turbophotos.gallery;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.botpa.turbophotos.R;

import java.util.List;

public class AlbumSelectionAdapter extends ArrayAdapter<Album> {

    private final int layout;

    public AlbumSelectionAdapter(Context context, List<Album> albums, int layout) {
        super(context, 0, albums);
        this.layout = layout;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        //Inflate the view
        if (convertView == null) convertView = LayoutInflater.from(getContext()).inflate(layout, parent, false);

        //Get album
        Album album = getItem(position);

        //Update name text
        if (album != null) {
            TextView name = convertView.findViewById(R.id.album_name);
            name.setText(album.getName());
        }

        return convertView;
    }
}