package com.aroundroidgroup.astrid.gpsServices;

import java.util.ArrayList;
import java.util.List;

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
                null, ContactsContract.Contacts._ID + "=" + rowID, null, null);
        return cur;
    }

    public static class idNameMail{
        public String id;
        public String name;
        public String mail;
    }

    public List<idNameMail> getFriends(Cursor cur){
        List<idNameMail> friends = new ArrayList<idNameMail>();
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                idNameMail idm = new idNameMail();
                idm.id = (cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID)));
                idm.name = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                Cursor emailCur = cr.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                        new String[]{idm.id}, null);
                boolean googleMail = false;
                while (emailCur.moveToNext() && ! googleMail) {
                    // This would allow you get several email addresses
                    // if the email addresses were stored in an array
                    idm.mail = emailCur.getString(
                            emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                    String emailType = emailCur.getString(
                            emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
                    if (idm.mail.endsWith("@gmail.com")){
                        googleMail = true;
                    }
                }
                emailCur.close();
                if (googleMail){
                    friends.add(idm);
                }

            }
        }
        return friends;
    }

    public List<idNameMail> friendsWithGoogle(){
        Cursor cur = getContactsCursor();
        List<idNameMail>  l = getFriends(cur);
        cur.close();
        return l;
    }

    public List<idNameMail> oneFriendWithGoogle(long rowID){
        Cursor cur = getOneFriendContactCursor(rowID);
        List<idNameMail>  l = getFriends(cur);
        cur.close();
        return l;
    }




}
