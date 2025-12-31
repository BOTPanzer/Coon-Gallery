package com.botpa.turbophotos.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.botpa.turbophotos.R;

import java.util.List;

public class MoveItemsAdapter extends ArrayAdapter<Album> {

    public MoveItemsAdapter(Context context, List<Album> albums) {
        super(context, 0, albums);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //Inflate the view
        if (convertView == null) convertView = LayoutInflater.from(getContext()).inflate(R.layout.move_item, parent, false);

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