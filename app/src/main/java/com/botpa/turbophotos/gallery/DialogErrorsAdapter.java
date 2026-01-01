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

public class DialogErrorsAdapter extends ArrayAdapter<ActionError> {

    public DialogErrorsAdapter(Context context, List<ActionError> errors) {
        super(context, 0, errors);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        //Inflate the view
        if (convertView == null) convertView = LayoutInflater.from(getContext()).inflate(R.layout.library_error_item, parent, false);

        //Get error
        ActionError error = getItem(position);

        //Update name & reason texts
        if (error != null) {
            //Update item name
            TextView name = convertView.findViewById(R.id.fail_item);
            name.setText(error.getItem().name);

            //Update reason
            TextView reason = convertView.findViewById(R.id.fail_reason);
            reason.setText(error.getReason());
        }

        return convertView;
    }
}