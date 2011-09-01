package com.aroundroidgroup.astrid.googleAccounts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/***
 * Adapter for Aroundroid with fields of email, latitude longitude, timestamp, valid state, and attached contact id from the device contacts.
 * Handles database connection, creation of new records, querys, updates and deletes.
 * @author Tomer
 *
 */
public class AroundroidDbAdapter {

    public static final String KEY_MAIL = "mail"; //$NON-NLS-1$
    public static final String KEY_LAT = "lat"; //$NON-NLS-1$
    public static final String KEY_LON= "lon"; //$NON-NLS-1$
    public static final String KEY_TIME= "time"; //$NON-NLS-1$
    public static final String KEY_CONTACTID= "contactid"; //$NON-NLS-1$
    public static final String KEY_VALIDS = "valids"; //$NON-NLS-1$

    public static final String KEY_ROWID = "_id"; //$NON-NLS-1$

    public static final long CONTACTID_INVALID_CONTACT = -2L;
    public static final long CONTACTID_MY_ID = -1L;
    public static final String MAIL_MY_FAKE_MAIL= "me"; //$NON-NLS-1$

    private static final String TAG = "AroundroidDbAdapter"; //$NON-NLS-1$
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
        "create table peopleloc (_id integer primary key autoincrement, " //$NON-NLS-1$
        + "mail text not null, contactid long not null, valids text not null, " //$NON-NLS-1$
        + "lon double , lat double , time long "+");"; //$NON-NLS-1$ //$NON-NLS-2$

    private static final String DATABASE_NAME = "aroundroiddata"; //$NON-NLS-1$
    private static final String DATABASE_TABLE = "peopleloc"; //$NON-NLS-1$
    private static final int DATABASE_VERSION = 2;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " //$NON-NLS-1$ //$NON-NLS-2$
                    + newVersion + ", which will destroy all old data"); //$NON-NLS-1$
            db.execSQL("DROP TABLE IF EXISTS peopleloc"); //$NON-NLS-1$
            onCreate(db);
        }

        public void kill(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS peopleloc"); //$NON-NLS-1$
            onCreate(db);
        }


    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     *
     * @param ctx the Context within which to work
     */
    public AroundroidDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the people database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     *
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public AroundroidDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }


    /**
     * Create a new people using the title and body provided. If the people is
     * successfully created return the new rowId for that people, otherwise return
     * a -1 to indicate failure.
     *
     * @return rowId or -1 if failed
     */
    public long createPeople(String mail , Long contactId) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_MAIL, mail.toLowerCase());
        if (contactId==null){
            initialValues.put(KEY_CONTACTID, CONTACTID_INVALID_CONTACT);
        }
        else{
            initialValues.put(KEY_CONTACTID, contactId);
        }
        initialValues.put(KEY_VALIDS,AroundRoidAppConstants.STATUS_UNREGISTERED);
        long l = mDb.insert(DATABASE_TABLE, null, initialValues);
        return l;
    }

    /**
     * Create a new person using mail provided. If the people is
     * successfully created return the new rowId for that people, otherwise return
     * a -1 to indicate failure.
     *
     * @return rowId or -1 if failed
     */
    public long createPeople( String mail) {
        return createPeople(mail, null);
    }

    /**
     * Delete the person with the given rowId
     *
     * @param rowId id of people to delete
     * @return true if deleted, false otherwise
     */
    public boolean deletePeople(long rowId) {

        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0; //$NON-NLS-1$
    }

    /**
     * Return a Cursor over the list of all the people in the database
     *
     * @return Cursor over all people
     */
    public Cursor fetchAllPeople() {

        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_MAIL,KEY_LAT,KEY_LON,KEY_TIME, KEY_CONTACTID, KEY_VALIDS}, null, null, null, null, null);
    }

    /**
     * Return a Cursor over the list of all people in the database with real contact id attached to them
     *
     * @return Cursor over all people
     */
    public Cursor fetchAllPeopleWContact() {

        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_MAIL,KEY_LAT,KEY_LON,KEY_TIME, KEY_CONTACTID , KEY_VALIDS}, KEY_CONTACTID + ">=0", null, null, null, null); //$NON-NLS-1$
    }

    /**
     * Return a Cursor over the list of all people in the database with real contact id connected to them, that are in a registered valid state
     *
     * @return Cursor over all people
     */
    public Cursor fetchAllPeopleWContactRegistered() {

        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_MAIL,KEY_LAT,KEY_LON,KEY_TIME, KEY_CONTACTID , KEY_VALIDS}, "("+ KEY_VALIDS +"<>'Unregistered') AND (" + KEY_CONTACTID + ">=0)", null, null, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /**
     * Return a Cursor over the list of all people in the database where the email address is 'mail'
     * @param mail mail
     * @return Cursor over all people
     */
    public Cursor fetchByMail(String mail) {
        Cursor mCursor =

            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                    KEY_MAIL, KEY_LAT, KEY_LON, KEY_TIME , KEY_CONTACTID, KEY_VALIDS}, KEY_MAIL + "=" + "'"+mail.toLowerCase()+"'", null,  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /**
     * Return a Cursor over the list of all the people where contact_id is 'contactId'
     * @param contactId
     * @return Cursor over him
     */
    public Cursor fetchByContactId(long contactId) {
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                    KEY_MAIL, KEY_LAT, KEY_LON, KEY_TIME , KEY_CONTACTID, KEY_VALIDS}, KEY_CONTACTID + "=" + contactId, null,  //$NON-NLS-1$
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /**
     * Return a Cursor positioned at the people that matches the given rowId
     *
     * @param rowId id of note to retrieve
     * @return Cursor positioned to matching people, if found
     * @throws SQLException if people could not be found/retrieved
     */
    public Cursor fetchPeople(long rowId) throws SQLException {

        Cursor mCursor =

            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                    KEY_MAIL, KEY_LAT, KEY_LON, KEY_TIME , KEY_CONTACTID , KEY_VALIDS}, KEY_ROWID + "=" + rowId, null, //$NON-NLS-1$
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }


    /**
     * Update the people using the details provided.

     * @return true if the people was successfully updated, false otherwise
     */
    public boolean updatePeople(long rowId ,long contactId ) {
        ContentValues args = new ContentValues();
        args.put(KEY_CONTACTID, contactId);
        boolean ef =  mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0; //$NON-NLS-1$
        return ef;
    }

    /**
     * Update the people using the details provided.

     * @return true if the people was successfully updated, false otherwise
     */
    public boolean updatePeople(long rowId ,double lat , double lon , long time, Long contactId , String valids) {
        ContentValues args = new ContentValues();
        args.put(KEY_LAT, lat);
        args.put(KEY_LON, lon);
        args.put(KEY_TIME,time);
        if (contactId!=null){
            args.put(KEY_CONTACTID, contactId);
        }
        if (valids!=null){
            args.put(KEY_VALIDS, valids);
        }
        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0; //$NON-NLS-1$
    }
    /**
     * Update the people using the details provided.

     * @return true if the people was successfully updated, false otherwise
     */
    public boolean updatePeople(long rowId,double lat , double lon , long time) {
        return updatePeople(rowId,lat, lon, time,null,null);
    }

    /*
    public void dropPeople(){
        mDbHelper.kill(mDb);
    }
     */

    /***
     * creates the special user (device user) record
     * @return  rowId of the user if successful. otherwise -1
     */
    private long createSpecialUser(){
        long rowID = this.createPeople(MAIL_MY_FAKE_MAIL, CONTACTID_MY_ID);
        if (rowID==-1){
            return -1;
        }
        boolean res = this.updatePeople(rowID, 0.0, 0.0,0L, null,AroundRoidAppConstants.STATUS_OFFLINE);
        return (res?rowID:-1);
    }

    /***
     * Creates the special user (device user) record if needed, then fetches a cursor of it's record
     * @return cursor over the special user on success, otherwise null
     */
    public Cursor createAndfetchSpecialUser(){
        Cursor c = null;
        c = fetchByContactId(CONTACTID_MY_ID);
        if (c!=null && c.moveToFirst()){
            return c;
        }
        else{
            if (c!=null) c.close();
            long rowID = createSpecialUser();
            return this.fetchPeople(rowID);
        }
    }

    /***
     * converts a specific user to friend props
     * @param rowId the row id of the user
     * @return friendprops of the user if user is found and no erros occurd. otherwise returns null.
     */
    public FriendProps userToFP(long rowId){
        Cursor cur = fetchPeople(rowId);
        if (cur==null || !cur.moveToFirst()){
            if (cur!=null) cur.close();
            return null;
        }
        FriendProps fp = userToFP(cur);
        cur.close();
        return fp;
    }

    /***
     * builds a new friend props of the first row of the cursor
     * @param cur
     * @return friend props if cursor is not empty otherwise null
     */
    public static FriendProps userToFP(Cursor cur){
            FriendProps fp  = new FriendProps();
            fp.setDlat(cur.getDouble(cur.getColumnIndex(KEY_LAT)));
            fp.setDlon(cur.getDouble(cur.getColumnIndex(KEY_LON)));
            fp.setMail(cur.getString(cur.getColumnIndex(KEY_MAIL)));
            fp.setTimestamp(cur.getLong(cur.getColumnIndex(KEY_TIME)));
            fp.setValid(cur.getString(cur.getColumnIndex(KEY_VALIDS)));
            return fp;
    }

    /***
     * builds a new friend props with contact id of the first row of the cursor
     * @param cur
     * @return friend props with contact id if cursor is not empty otherwise null
     */
    public static FriendPropsWithContactId userToFPWithContactId(Cursor cur){
        FriendProps fp  = userToFP(cur);
        FriendPropsWithContactId fpwci = new FriendPropsWithContactId(cur.getLong(cur.getColumnIndex(KEY_CONTACTID)), fp);
        return fpwci;
    }

    /***
     * builds a new friend props of the special user
     * @return friend props of the special user if cursor is not empty and no error otherwise null
     */
    public FriendProps specialUserToFP(){
        Cursor cur = createAndfetchSpecialUser();
        if (cur==null){
            return null;
        }
        FriendProps fp =  userToFP(cur);
        cur.close();
        return fp;
    }


}
