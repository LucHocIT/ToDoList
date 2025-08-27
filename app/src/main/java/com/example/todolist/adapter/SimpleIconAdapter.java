package com.example.todolist.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.example.todolist.R;

public class SimpleIconAdapter extends BaseAdapter {
    
    private Context context;
    private int[] iconResources;
    private LayoutInflater inflater;
    private int selectedPosition = 0;

    public SimpleIconAdapter(Context context, int[] iconResources) {
        this.context = context;
        this.iconResources = iconResources;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return iconResources.length;
    }

    @Override
    public Object getItem(int position) {
        return iconResources[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        
        if (view == null) {
            view = inflater.inflate(R.layout.item_simple_icon, parent, false);
        }

        ImageView iconImageView = view.findViewById(R.id.icon_image);
        iconImageView.setImageResource(iconResources[position]);
        
        // Highlight selected item
        if (position == selectedPosition) {
            view.setBackgroundResource(R.drawable.selected_icon_background);
        } else {
            view.setBackgroundResource(R.drawable.circle_background);
        }

        return view;
    }
    
    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }
}
