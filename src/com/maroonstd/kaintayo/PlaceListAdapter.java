package com.maroonstd.kaintayo;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.gms.maps.model.Marker;

public class PlaceListAdapter extends BaseAdapter {
	 
    private ArrayList<Marker> listData;
 
    private LayoutInflater layoutInflater;
 
    public PlaceListAdapter(Context context, ArrayList<Marker> listData) {
        this.listData = listData;
        layoutInflater = LayoutInflater.from(context);
    }
 
    @Override
    public int getCount() {
        return listData.size();
    }
 
    @Override
    public Object getItem(int position) {
        return listData.get(position);
    }
 
    @Override
    public long getItemId(int position) {
        return position;
    }
 
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_layout, null);
            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.title);
            holder.snippet = (TextView) convertView.findViewById(R.id.snippet);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
 
        holder.title.setText(listData.get(position).getTitle());
        holder.snippet.setText(listData.get(position).getSnippet());
 
        return convertView;
    }
 
    static class ViewHolder {
        TextView title;
        TextView snippet;
    }
 
}