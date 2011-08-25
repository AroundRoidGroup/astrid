package com.aroundroidgroup.astrid.googleAccounts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AroundroidDbAdapter {

    public static final String KEY_MAIL = "mail";
    public static final String KEY_LAT = "lat";
    public static final String KEY_LON= "lon";
    public static final String KEY_TIME= "time";
    public static final String KEY_CONTACTID= "contactid";
    public static final String KEY_VALIDS = "valids";

    public static final String KEY_ROWID = "_id";

    public static final long CONTACTID_INVALID_CONTACT = -2L;

    private static final String TAG = "AroundroidDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
        "create table peopleloc (_id integer primary key autoincrement, "
        + "mail text not null, contactid long not null, valids text not null, "
        + "lon double , lat double , time long "+");";

    private static final String DATABASE_NAME = "aroundroiddata";
    private static final String DATABASE_TABLE = "peopleloc";
    private static final int DATABASE_VERSION = 2;

    private final Context mCtx;

    //TODO consider change in the table of astrid that mails will be saved as row numbers in the database?

    //TODO check why when on first time installed there is a database cursor sql error

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
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS peopleloc");
            onCreate(db);
        }

        public void kill(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS peopleloc");
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
    //TODO remove lower case check
    public long createPeople(String mail , Long contactId) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_MAIL, mail.toLowerCase());
        if (contactId==null){
            initialValues.put(KEY_CONTACTID, CONTACTID_INVALID_CONTACT);
        }
        else{
            initialValues.put(KEY_CONTACTID, contactId);
        }
        initialValues.put(KEY_VALIDS,"Unregistered");
        long l = mDb.insert(DATABASE_TABLE, null, initialValues);
        //TODO remove debug
        if (l==-1L){
            int x  = 5;
        }
        return l;
    }

    /**
     * Create a new people using the title and body provided. If the people is
     * successfully created return the new rowId for that people, otherwise return
     * a -1 to indicate failure.
     *
     * @return rowId or -1 if failed
     */
    public long createPeople( String mail) {
        return createPeople(mail, null);
    }

    /**
     * Delete the people with the given rowId
     *
     * @param rowId id of people to delete
     * @return true if deleted, false otherwise
     */
    public boolean deletePeople(long rowId) {

        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all people in the database
     *
     * @return Cursor over all people
     */
    public Cursor fetchAllPeople() {

        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_MAIL,KEY_LAT,KEY_LON,KEY_TIME, KEY_CONTACTID, KEY_VALIDS}, null, null, null, null, null);
    }

    /**
     * Return a Cursor over the list of all people in the database with real contact id connected to them
     *
     * @return Cursor over all people
     */
    public Cursor fetchAllPeopleWContact() {

        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_MAIL,KEY_LAT,KEY_LON,KEY_TIME, KEY_CONTACTID , KEY_VALIDS}, KEY_CONTACTID + ">=0", null, null, null, null);
    }

    /**
     * Return a Cursor over the list of all people in the database with real contact id connected to them
     *
     * @return Cursor over all people
     */
    public Cursor fetchAllPeopleWContactRegistered() {

        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_MAIL,KEY_LAT,KEY_LON,KEY_TIME, KEY_CONTACTID , KEY_VALIDS}, "("+ KEY_VALIDS +"<>'Unregistered') AND (" + KEY_CONTACTID + ">=0)", null, null, null, null);
    }

    /**
     * Return a Cursor over the list of all people in the database where the mail
     * @param mail mail
     * @return Cursor over all people
     */
    public Cursor fetchByMail(String mail) {

        //TODO remove lower case
        Cursor mCursor =

            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                    KEY_MAIL, KEY_LAT, KEY_LON, KEY_TIME , KEY_CONTACTID, KEY_VALIDS}, KEY_MAIL + "=" + "'"+mail.toLowerCase()+"'", null,
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
                    KEY_MAIL, KEY_LAT, KEY_LON, KEY_TIME , KEY_CONTACTID , KEY_VALIDS}, KEY_ROWID + "=" + rowId, null,
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
        boolean ef =  mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
        //TODO remove debug
        if (!ef){
            int z  =  2;
        }
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


        boolean ef =  mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
        //TODO remove debug
        if (!ef){
            int z  =  2;
        }
        return ef;
    }
    /**
     * Update the people using the details provided.

     * @return true if the people was successfully updated, false otherwise
     */
    public boolean updatePeople(long rowId,double lat , double lon , long time) {
        return updatePeople(rowId,lat, lon, time,null,null);
    }



    public void dropPeople(){
        mDbHelper.kill(mDb);
    }

    private long createSpecialUser(){
        long rowID = this.createPeople("me", -1L);
        if (rowID==-1){
            return -1;
        }
        //begining of time!
        this.updatePeople(rowID, 0.0, 0.0, 21600L, null,"No");
        return rowID;
    }

    //TODO change to fetch by contact id
    public Cursor createAndfetchSpecialUser(){
        Cursor c = null;
        c = fetchByMail("me");
        if (c!=null && c.moveToFirst()){
            return c;
        }
        else{
            if (c!=null) c.close();
            long rowID = createSpecialUser();
            return this.fetchPeople(rowID);
        }
    }

    public FriendProps userToFP(long rowId){
        //TODO deal with Timestamp
        Cursor cur = fetchPeople(rowId);
        if (cur==null || !cur.moveToFirst()){
            if (cur!=null) cur.close();
            return null;
        }
        FriendProps fp = userToFP(cur);
        cur.close();
        return fp;
    }

    public static FriendProps userToFP(Cursor cur){
        FriendProps fp  = new FriendProps();
        fp.setDlat(cur.getDouble(cur.getColumnIndex(KEY_LAT)));
        fp.setDlon(cur.getDouble(cur.getColumnIndex(KEY_LON)));
        fp.setMail(cur.getString(cur.getColumnIndex(KEY_MAIL)));
        fp.setTimestamp(cur.getLong(cur.getColumnIndex(KEY_TIME)));
        fp.setValid(cur.getString(cur.getColumnIndex(KEY_VALIDS)));
        return fp;
    }

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
