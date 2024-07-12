package com.smj.bungalow;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Objects;

public class ZoneAdapter extends ArrayAdapter<Zone> {

    private Context context;

    @SuppressWarnings("SameParameterValue")
    //public ZoneAdapter(Context context, int textViewResourceId, ArrayList<Zone> zoneArray) {
    ZoneAdapter(Context context, int textViewResourceId, ArrayList<Zone> zoneArray) {
        super(context, textViewResourceId, zoneArray);
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {
            // Use ViewHolder pattern for smooth scrolling.  This block runs only the
            // first time each row is created.  The row data reference is saved for smooth
            // scrolling by not invoking findViewById and inflating on every list scroll

            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = Objects.requireNonNull(inflater).inflate(R.layout.zone, parent, false);

            holder = new ViewHolder();
            holder.zoneName = convertView.findViewById(R.id.zoneTextView);
            holder.icon1 = convertView.findViewById(R.id.zoneIcon1);
            holder.icon2 = convertView.findViewById(R.id.zoneIcon2);
            holder.icon3 = convertView.findViewById(R.id.zoneIcon3);
            holder.icon4 = convertView.findViewById(R.id.zoneIcon4);
            convertView.setTag(holder); // save for later look-up
        }
        else {
            // view already exists, get the holder instance from the view
            holder = (ViewHolder) convertView.getTag();
        }

        Zone item = getItem(position);

        if (item != null) {
            holder.icon1.setVisibility(View.VISIBLE);
            holder.icon2.setVisibility(View.GONE);
            holder.icon3.setVisibility(View.GONE);
            holder.icon4.setVisibility(View.GONE);
            holder.zoneName.setText(item.Name);

            if (item.Fault) holder.icon1.setImageResource(R.drawable.fault);
            else holder.icon1.setImageResource(R.drawable.ready);

            if (item.Bypass) {
                holder.icon2.setImageResource(R.drawable.bypassed);
                holder.icon2.setVisibility(View.VISIBLE);
            }

            if (item.AlarmMemory) {
                holder.icon2.setImageResource(R.drawable.alarm);
                holder.icon2.setVisibility(View.VISIBLE);
            }

            if (item.Error) {
                holder.icon3.setImageResource(R.drawable.error);
                holder.icon3.setVisibility(View.VISIBLE);
            }
        }
        return convertView;
    }

    private class ViewHolder {
        ImageView icon1, icon2, icon3, icon4;
        TextView zoneName;
    }

}
