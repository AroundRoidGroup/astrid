package com.aroundroidgroup.astrid.googleAccounts;

import java.util.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.timsu.astrid.R;


public class FriendAdapter extends ArrayAdapter<FriendProps> {
        private final Activity context;
        private final List<FriendProps> props;

        public FriendAdapter(Activity context, List<FriendProps> props) {
            super(context, R.layout.friendrowlayout, props);
            this.context = context;
            this.props = props;
        }

        // static to save the reference to the outer class and to avoid access to
        // any members of the containing class
        static class ViewHolder {
            public ImageView imageView;
            public TextView textView;
            //TODO add the secondary text view
            public TextView secondaryTextView;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // ViewHolder will buffer the assess to the individual fields of the row
            // layout

            ViewHolder holder;
            // Recycle existing view if passed as parameter
            // This will save memory and time on Android
            // This only works if the base layout for all classes are the same
            View rowView = convertView;
            if (rowView == null) {
                LayoutInflater inflater = context.getLayoutInflater();
                rowView = inflater.inflate(R.layout.friendrowlayout, null, true);
                holder = new ViewHolder();
                holder.textView = (TextView) rowView.findViewById(R.id.label);
                holder.imageView = (ImageView) rowView.findViewById(R.id.icon);
                rowView.setTag(holder);
            } else {
                holder = (ViewHolder) rowView.getTag();
            }

            FriendProps currectFP = props.get(position);

            holder.textView.setText(currectFP.getMail());
            // Change the icon for Windows and iPhone
            String valid = currectFP.getValid();
            if (valid.compareTo("Yes")==0){
                holder.imageView.setImageResource(R.drawable.btn_green_button);
            } else if (valid.compareTo("No")==0) {
                holder.imageView.setImageResource(R.drawable.btn_red_button);
            } else {
                holder.imageView.setImageResource(R.drawable.btn_sry_sign);
            }

            return rowView;
        }
}
