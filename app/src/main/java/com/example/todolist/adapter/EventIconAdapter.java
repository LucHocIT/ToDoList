package com.example.todolist.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.todolist.R;
import com.example.todolist.model.EventIcon;

import java.util.List;

public class EventIconAdapter extends BaseAdapter {
    
    private Context context;
    private List<EventIcon> icons;
    private LayoutInflater inflater;
    private int selectedPosition = 0; 

    public EventIconAdapter(Context context, List<EventIcon> icons) {
        this.context = context;
        this.icons = icons;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return icons.size();
    }

    @Override
    public Object getItem(int position) {
        return icons.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_event_icon, parent, false);
            holder = new ViewHolder();
            holder.iconImageView = convertView.findViewById(R.id.iv_event_icon);
            holder.nameTextView = convertView.findViewById(R.id.tv_icon_name);
            holder.selectionIndicator = convertView.findViewById(R.id.selection_indicator);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        EventIcon icon = icons.get(position);
        holder.iconImageView.setImageResource(icon.getResourceId());
        holder.nameTextView.setText(icon.getName());
        if (position == selectedPosition) {
            holder.selectionIndicator.setVisibility(View.VISIBLE);
            convertView.setBackgroundResource(R.drawable.circle_background_light);
        } else {
            holder.selectionIndicator.setVisibility(View.GONE);
            convertView.setBackgroundResource(R.drawable.circle_background);
        }

        return convertView;
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    private static class ViewHolder {
        ImageView iconImageView;
        TextView nameTextView;
        View selectionIndicator;
    }
}
