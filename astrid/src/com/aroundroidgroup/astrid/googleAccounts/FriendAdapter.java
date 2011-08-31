package com.aroundroidgroup.astrid.googleAccounts;

import java.util.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.aroundroidgroup.astrid.gpsServices.ContactsHelper;
import com.timsu.astrid.R;

/***
 * This is an adapter for the list in ManageContactsActivity.
 * It displays mail and a connection status icon, plus optional contact Display name.
 * @author Tomer
 */
public class FriendAdapter extends ArrayAdapter<FriendPropsWithContactId> {
    private final Activity context;
    private final List<FriendPropsWithContactId> props;
    private final ContactsHelper conHel;

    public FriendAdapter(Activity context, ContactsHelper conHel, List<FriendPropsWithContactId> props) {
        super(context, R.layout.friendrowlayout, props);
        this.context = context;
        this.props = props;
        this.conHel = conHel;
    }

    // static to save the reference to the outer class and to avoid access to
    // any members of the containing class
    static class ViewHolder {
        public ImageView imageView;
        public TextView textView;
        public TextView secondaryTextView;
    }

    @Override
    public FriendPropsWithContactId getItem(int position) {
        return super.getItem(position);
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
            holder.textView = (TextView) rowView.findViewById(R.id.label_friend_mail);
            holder.imageView = (ImageView) rowView.findViewById(R.id.icon);
            holder.secondaryTextView = (TextView) rowView.findViewById(R.id.label_contact);
            rowView.setTag(holder);
        } else {
            holder = (ViewHolder) rowView.getTag();
        }
        FriendPropsWithContactId currectFP = props.get(position);
        holder.textView.setText(currectFP.getMail());
        Long conId = currectFP.getContactId();
        if (conId!=AroundroidDbAdapter.CONTACTID_INVALID_CONTACT){
            String displayName = conHel.oneDisplayName(conId);
            if (displayName==null){
                displayName = context.getResources().getString(R.string.AD_no_contact_info);;
            }
            holder.secondaryTextView.setText(displayName);
            holder.secondaryTextView.setVisibility(View.VISIBLE);
        }
        else{
            holder.secondaryTextView.setVisibility(View.GONE);
            //holder.secondaryTextView.setText(context.getResources().getString(R.string.no_contact_info));
        }
        // Change the icon for online, offline and non registered users
        if (currectFP.isValid()){
            holder.imageView.setImageResource(R.drawable.btn_green_button);
        } else if (currectFP.isRegistered()) {
            holder.imageView.setImageResource(R.drawable.btn_red_button);
        } else {
            holder.imageView.setImageResource(R.drawable.btn_gray_button);
        }
        return rowView;
    }
}
