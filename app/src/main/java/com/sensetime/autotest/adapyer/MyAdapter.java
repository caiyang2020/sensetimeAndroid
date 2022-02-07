package com.sensetime.autotest.adapyer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.sensetime.autotest.R;

import java.util.LinkedList;

public class MyAdapter extends BaseAdapter {

    private Context mContext;

    private LinkedList<String> mLogLinklist;


    public MyAdapter(Context mContext, LinkedList<String> mLogLinklist) {
        this.mContext = mContext;
        this.mLogLinklist = mLogLinklist;
    }

    @Override
    public int getCount() {
        return mLogLinklist.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = LayoutInflater.from(mContext).inflate(R.layout.log_text_item,parent,false);
        TextView textView = convertView.findViewById(R.id.textView7);
        textView.setText(mLogLinklist.get(position));

        return convertView;
    }
}
