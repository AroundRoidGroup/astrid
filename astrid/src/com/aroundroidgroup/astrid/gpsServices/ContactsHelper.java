package com.aroundroidgroup.astrid.gpsServices;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;

public class ContactsHelper {

    ContentResolver cr;

    public ContactsHelper(ContentResolver cr) {
        this.cr = cr;
    }

    private Cursor getContactsCursor(){
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        return cur;
    }

    private Cursor getOneFriendContactCursor(long rowID){
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, ContactsContract.Contacts._ID + "=" + rowID, null, null); //$NON-NLS-1$
        return cur;
    }

    public Set<Entry<String, Long>> getFriends(Cursor cur){
        HashMap<String, Long> hm = new HashMap<String, Long>();
        if (cur.moveToFirst()) {
            do {
                String id = (cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID)));
                Cursor emailCur = cr.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", //$NON-NLS-1$
                        new String[]{id}, null);
                if (emailCur.moveToFirst()){
                    do {
                        // This would allow you get several email addresses
                        // if the email addresses were stored in an array
                        String mail = emailCur.getString(
                                emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                        if (!hm.containsKey(mail)){
                            hm.put(mail, Long.parseLong(id));
                        }
                    } while (emailCur.moveToNext());
                }
                emailCur.close();

            } while (cur.moveToNext()) ;
        }
        return hm.entrySet();
    }

    public String getDisplayName(Cursor cur){
        if (cur.moveToNext()) {
            return cur.getString(
                    cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        }
        else{
            return null;
        }
    }


    public Set<Entry<String, Long>> friendsWithMail(){
        Cursor cur = getContactsCursor();
        Set<Entry<String, Long>>  l = getFriends(cur);
        cur.close();
        return l;
    }

    public String oneDisplayName(long rowId){
        Cursor cur = getOneFriendContactCursor(rowId);
        String name = getDisplayName(cur);
        cur.close();
        return name;
    }




}
