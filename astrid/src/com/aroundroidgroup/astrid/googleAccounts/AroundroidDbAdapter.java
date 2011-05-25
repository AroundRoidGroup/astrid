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

    public static final String KEY_ROWID = "_id";

    private static final String TAG = "AroundroidDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
            "create table peopleloc (_id integer primary key AUTO_INCREMENT, "
                    + "mail text not null, "
                    + "lon double , lat double , time long "+");";

    private static final String DATABASE_NAME = "aroundroiddata";
    private static final String DATABASE_TABLE = "peopleloc";
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
    public long createPeople(int key , String mail) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_ROWID, key);
        initialValues.put(KEY_MAIL, mail);

        return mDb.insert(DATABASE_TABLE, null, initialValues);
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

        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_MAIL,KEY_LAT,KEY_LON,KEY_TIME}, null, null, null, null, null);
    }

    /**
     * Return a Cursor over the list of all people in the database where the mail
     * @param mail mail
     * @return Cursor over all people
     */
    public Cursor fetchAllMail(String mail) {

        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_MAIL,
               KEY_LAT,KEY_LON,KEY_TIME},"mail == "+mail, null, null, null, null);
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
                        KEY_MAIL, KEY_LAT, KEY_LON, KEY_TIME}, KEY_ROWID + "=" + rowId, null,
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
    public boolean updatePeople(String lat , String lon , String time) {
        ContentValues args = new ContentValues();
        args.put(KEY_LAT, lat);
        args.put(KEY_LON, lon);
        args.put(KEY_TIME,time);

        return mDb.update(DATABASE_TABLE, args,null, null) > 0;
    }

    public void dropPeople(){
        mDbHelper.kill(mDb);
    }
}
