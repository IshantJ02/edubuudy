package com.edu.edubuddy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatTextView;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

public class GroupAdapter extends BaseAdapter implements Filterable {

    private Context context;
    private List<String> originalGroupList;
    private List<String> filteredGroupList;
    private LayoutInflater inflater;

    public GroupAdapter(Context context, List<String> groupList) {
        this.context = context;
        this.originalGroupList = groupList;
        this.filteredGroupList = groupList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return filteredGroupList.size();
    }

    @Override
    public Object getItem(int position) {
        return filteredGroupList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        TextView groupNameTextView = convertView.findViewById(android.R.id.text1);
        groupNameTextView.setText(filteredGroupList.get(position));

        return convertView;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<String> filteredGroups = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    filteredGroups.addAll(originalGroupList);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();

                    for (String group : originalGroupList) {
                        if (group.toLowerCase().contains(filterPattern)) {
                            filteredGroups.add(group);
                        }
                    }
                }

                results.values = filteredGroups;
                results.count = filteredGroups.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredGroupList = (List<String>) results.values;
                notifyDataSetChanged();
            }
        };
    }
}
