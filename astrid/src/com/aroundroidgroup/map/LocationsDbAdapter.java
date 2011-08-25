package com.aroundroidgroup.map;

import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import com.todoroo.andlib.utility.DateUtilities;

public class LocationsDbAdapter {

    public static final String KEY_ROWID = "_id"; //$NON-NLS-1$
    public static final String KEY_LAST_USE_TIME = "time"; //$NON-NLS-1$

    public static final String KEY_ADDRESS = "address"; //$NON-NLS-1$
    //    public static final String KEY_COORDINATES = "coordinates"; //$NON-NLS-1$
    public static final String KEY_LATITUDE = "lat"; //$NON-NLS-1$
    public static final String KEY_LONGITUDE = "lon"; //$NON-NLS-1$
    public static final String KEY_BUSINESS_NAME = "businessName"; //$NON-NLS-1$
    public static final String KEY_TYPE_ID = "idOfTypeByCenterAndRadius"; //$NON-NLS-1$

    public static final String KEY_TYPE = "type"; //$NON-NLS-1$
    //    public static final String KEY_CENTER_POINT = "centerPoint"; //$NON-NLS-1$
    public static final String KEY_RADIUS = "radius"; //$NON-NLS-1$

    private static final String TAG = "NotesDbAdapter"; //$NON-NLS-1$
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    private static final String DATABASE_NAME = "data"; //$NON-NLS-1$
    private static final String DATABASE_TABLE_TRANSLATIONS = "translations"; //$NON-NLS-1$
    private static final String DATABASE_TABLE_TYPES = "types"; //$NON-NLS-1$
    private static final int DATABASE_VERSION = 2;
    public static final String DATABASE_ADDRESS_GEOCODE_FAILURE = "$addrFailure$"; //$NON-NLS-1$
    public static final String DATABASE_COORDINATE_GEOCODE_FAILURE = "$coordFailure$"; //$NON-NLS-1$

    private final int mRadiusError = 5000;
    private final int mCoordinateError = 5000;
    private final Context mCtx;

    private static final String CREATE_TABLE_TRANSLATIONS =
        "create table " + DATABASE_TABLE_TRANSLATIONS + " (" + KEY_ROWID + " integer primary key autoincrement, " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        + KEY_LATITUDE + " integer, " + KEY_LONGITUDE + " integer, " + KEY_ADDRESS + " text not null, "  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        + KEY_LAST_USE_TIME + " text not null) ;"; //$NON-NLS-1$
    private static final String CREATE_TABLE_TYPES =
        "CREATE TABLE " + DATABASE_TABLE_TYPES + " (" + KEY_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        + KEY_TYPE + " TEXT NOT NULL, " + KEY_LATITUDE + " INTEGER, " + KEY_LONGITUDE + " INTEGER, "   //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        + KEY_RADIUS + " DOUBLE, " + KEY_LAST_USE_TIME + " TEXT NOT NULL) ;";  //$NON-NLS-1$ //$NON-NLS-2$

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_TRANSLATIONS);
            db.execSQL(CREATE_TABLE_TYPES);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " //$NON-NLS-1$ //$NON-NLS-2$
                    + newVersion + ", which will destroy all old data"); //$NON-NLS-1$
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_TRANSLATIONS); //$NON-NLS-1$
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_TYPES); //$NON-NLS-1$
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     *
     * @param ctx the Context within which to work
     */
    public LocationsDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     *
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public LocationsDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }


    /**
     * Create a new note using the title and body provided. If the note is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.
     *
     * @param title the title of the note
     * @param body the body of the note
     * @return rowId or -1 if failed
     */
    public long createTranslate(int latitude, int longitude, String address) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_LATITUDE, latitude);
        initialValues.put(KEY_LONGITUDE, longitude);
        initialValues.put(KEY_ADDRESS, address);
        initialValues.put(KEY_LAST_USE_TIME, DateUtilities.now());
        return mDb.insert(DATABASE_TABLE_TRANSLATIONS, null, initialValues);
    }

    public long createType(String type, long latitude, long longitude, double radius, Map<String, DPoint> data) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TYPE, type);
        initialValues.put(KEY_LATITUDE, latitude);
        initialValues.put(KEY_LONGITUDE, longitude);
        initialValues.put(KEY_RADIUS, radius);
        initialValues.put(KEY_LAST_USE_TIME, DateUtilities.now());
        Long rowID = mDb.insert(DATABASE_TABLE_TYPES, null, initialValues);
        Toast.makeText(mCtx, rowID + "", Toast.LENGTH_LONG).show(); //$NON-NLS-1$
        String typeTableName = "_" + type; //$NON-NLS-1$
        if (data != null) {
            /* creating the table for the given type. the table contains business names and their coords */
            String typeTableSQL =
                "create table if not exists " + typeTableName + " (" + KEY_ROWID + " integer primary key autoincrement, " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                + KEY_BUSINESS_NAME + " text not null, " + KEY_LATITUDE + " integer, " + KEY_LONGITUDE + " integer, " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                KEY_TYPE_ID + " integer) ;"; //$NON-NLS-1$
            mDb.execSQL(typeTableSQL);
            for (Map.Entry<String, DPoint> p : data.entrySet()) {
                ContentValues pair = new ContentValues();
                pair.put(KEY_BUSINESS_NAME, p.getKey());
                pair.put(KEY_LATITUDE, Misc.degToGeo(p.getValue()).getLatitudeE6());
                pair.put(KEY_LONGITUDE, Misc.degToGeo(p.getValue()).getLongitudeE6());
                pair.put(KEY_TYPE_ID, rowID);
                Toast.makeText(mCtx, mDb.insert(typeTableName, null, pair) + "", Toast.LENGTH_LONG).show(); //$NON-NLS-1$

            }
        }
        return rowID;
    }

    /**
     * Delete the note with the given rowId
     *
     * @param rowId id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteTranslation(long rowId) {
        return mDb.delete(DATABASE_TABLE_TRANSLATIONS, KEY_ROWID + "=" + rowId, null) > 0; //$NON-NLS-1$
    }

    public boolean deleteType(long rowId) {
        return mDb.delete(DATABASE_TABLE_TYPES, KEY_ROWID + "=" + rowId, null) > 0; //$NON-NLS-1$
    }

    /**
     * Return a Cursor over the list of all notes in the database
     *
     * @return Cursor over all notes
     */
    public Cursor fetchAllTranslations() {
        return mDb.query(DATABASE_TABLE_TRANSLATIONS, new String[] {KEY_ROWID, KEY_LATITUDE, KEY_LONGITUDE,
                KEY_ADDRESS}, null, null, null, null, null);
    }

    public Cursor fetchAllTypes() {
        return mDb.query(DATABASE_TABLE_TYPES, new String[] {KEY_ROWID, KEY_LATITUDE, KEY_LONGITUDE,
                KEY_RADIUS}, null, null, null, null, null);
    }

    public String fetchByCoordinateFromType(String type, int latitude, int longitude) throws SQLException {
        Cursor mCursor =
            mDb.query(true, "_" + type, new String[] {KEY_BUSINESS_NAME}, //$NON-NLS-1$
                    "(" + KEY_LATITUDE + "<=" + (latitude + mCoordinateError) + " and " + KEY_LATITUDE + ">=" + (latitude - mCoordinateError) + ") and " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                    + "(" + KEY_LONGITUDE + "<=" + (longitude + mCoordinateError) + " and " + KEY_LONGITUDE + ">=" + (longitude - mCoordinateError) + ")", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                    null, null, null, null, null);
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                String retValue = mCursor.getString(mCursor.getColumnIndex(KEY_BUSINESS_NAME));
                mCursor.close();
                return retValue;
            }
            mCursor.close();
        }
        return null;
    }

    public Cursor fetchByCoordinate(int latitude, int longitude) throws SQLException{
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE_TRANSLATIONS, new String[] {KEY_ROWID,
                    KEY_LATITUDE, KEY_LONGITUDE, KEY_ADDRESS},
                    "(" + KEY_LATITUDE + "<=" + (latitude + mCoordinateError) + " and " + KEY_LATITUDE + ">=" + (latitude - mCoordinateError) + ") and " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                    + "(" + KEY_LONGITUDE + "<=" + (longitude + mCoordinateError) + " and " + KEY_LONGITUDE + ">=" + (longitude - mCoordinateError) + ")", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                    null, null, null, null, null);
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                updateTranslate(mCursor.getInt(mCursor.getColumnIndex(KEY_ROWID)),
                        mCursor.getInt(mCursor.getColumnIndex(KEY_LATITUDE)),
                        mCursor.getInt(mCursor.getColumnIndex(KEY_LONGITUDE)),
                        mCursor.getString(mCursor.getColumnIndex(KEY_ADDRESS)));
            }
        }
        return mCursor;
    }

    public String fetchByCoordinateAsString(int latitude, int longitude) throws SQLException {
        Cursor mCursor = fetchByCoordinate(latitude, longitude);
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                String data = mCursor.getString(mCursor.getColumnIndex(KEY_ADDRESS));
                mCursor.close();
                return data;
            }
            mCursor.close();
        }
        return null;
    }

    public Cursor fetchByAddress(String address) throws SQLException {
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE_TRANSLATIONS, new String[] {KEY_ROWID,
                    KEY_LATITUDE, KEY_LONGITUDE, KEY_ADDRESS}, KEY_ADDRESS + "='" + address + "'", //$NON-NLS-1$ //$NON-NLS-2$
                    null, null, null, null, null);
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                updateTranslate(mCursor.getInt(mCursor.getColumnIndex(KEY_ROWID)),
                        mCursor.getInt(mCursor.getColumnIndex(KEY_LATITUDE)),
                        mCursor.getInt(mCursor.getColumnIndex(KEY_LONGITUDE)),
                        mCursor.getString(mCursor.getColumnIndex(KEY_ADDRESS)));
            }
        }
        return mCursor;
    }

    public String fetchByAddressAsString(String address) throws SQLException {
        Cursor mCursor = fetchByAddress(address);
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                String data = mCursor.getString(mCursor.getColumnIndex(KEY_ADDRESS));
                mCursor.close();
                return data;
            }
            mCursor.close();
        }
        return null;
    }

    /**
     * Return a Cursor positioned at the note that matches the given rowId
     *
     * @param rowId id of note to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchTranslation(long rowId) throws SQLException {
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE_TRANSLATIONS, new String[] {KEY_ROWID,
                    KEY_LATITUDE, KEY_LONGITUDE, KEY_ADDRESS}, KEY_ROWID + "=" + rowId, null, //$NON-NLS-1$
                    null, null, null, null);
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                updateTranslate(rowId,
                        mCursor.getInt(mCursor.getColumnIndex(KEY_LATITUDE)),
                        mCursor.getInt(mCursor.getColumnIndex(KEY_LONGITUDE)),
                        mCursor.getString(mCursor.getColumnIndex(KEY_ADDRESS)));
            }
        }
        return mCursor;

    }

    public Cursor fetchByType(String type) throws SQLException{
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE_TYPES, new String[] {KEY_ROWID, KEY_TYPE,
                    KEY_LATITUDE, KEY_LONGITUDE, KEY_RADIUS}, KEY_TYPE + "='" + type + "'", //$NON-NLS-1$ //$NON-NLS-2$
                    null, null, null, null, null);
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                updateType(mCursor.getInt(mCursor.getColumnIndex(KEY_ROWID)),
                        mCursor.getString(mCursor.getColumnIndex(KEY_TYPE)),
                        mCursor.getInt(mCursor.getColumnIndex(KEY_LATITUDE)),
                        mCursor.getInt(mCursor.getColumnIndex(KEY_LONGITUDE)),
                        mCursor.getDouble(mCursor.getColumnIndex(KEY_RADIUS)));
                mCursor.close();
                mCursor = mDb.query("_" + type, new String[] {KEY_BUSINESS_NAME, KEY_LATITUDE, KEY_LONGITUDE}, //$NON-NLS-1$
                        null, null, null, null, null);
            }
            else mCursor.close();
        }
        return mCursor;
    }

    public Cursor fetchByTypeComplex(String type, int latitude, int longitude, double radius) throws SQLException {
        if (type == null)
            return null;
        Cursor mCursor =
        mDb.query(true, DATABASE_TABLE_TYPES, new String[] {KEY_ROWID, KEY_TYPE, KEY_LATITUDE, KEY_LONGITUDE, KEY_RADIUS},
                KEY_LATITUDE + "<=" + (latitude + mCoordinateError) + " AND " + KEY_LATITUDE + ">=" + (latitude - mCoordinateError) + " AND " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                KEY_LONGITUDE + "<=" + (longitude + mCoordinateError) + " AND " + KEY_LONGITUDE + ">=" + (longitude - mCoordinateError) + " AND " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                KEY_RADIUS + "<=" + (radius + mRadiusError) + " AND " + KEY_RADIUS + ">=" + (radius - mRadiusError), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                null, null, null, null, null);
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                Toast.makeText(mCtx, "kjkj", Toast.LENGTH_LONG).show(); //$NON-NLS-1$
                int rowID = mCursor.getInt(mCursor.getColumnIndex(KEY_ROWID));
                updateType(rowID,
                        mCursor.getString(mCursor.getColumnIndex(KEY_TYPE)),
                        mCursor.getInt(mCursor.getColumnIndex(KEY_LATITUDE)),
                        mCursor.getInt(mCursor.getColumnIndex(KEY_LONGITUDE)),
                        mCursor.getDouble(mCursor.getColumnIndex(KEY_RADIUS)));
                mCursor.close();
                mCursor = mDb.query("_" + type, new String[] {KEY_BUSINESS_NAME, KEY_LATITUDE, KEY_LONGITUDE}, //$NON-NLS-1$
                        KEY_ROWID + "=" + rowID , null, null, null, null); //$NON-NLS-1$
            }
            else mCursor.close();
        }
        return mCursor;
    }

    public Cursor fetchType(long rowId) throws SQLException {

        Cursor mCursor =

            mDb.query(true, DATABASE_TABLE_TYPES, new String[] {KEY_ROWID, KEY_TYPE,
                    KEY_LATITUDE, KEY_LONGITUDE, KEY_RADIUS}, KEY_ROWID + "=" + rowId, null, //$NON-NLS-1$
                    null, null, null, null);
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                updateType(rowId, mCursor.getString(mCursor.getColumnIndex(KEY_TYPE)),
                        mCursor.getInt(mCursor.getColumnIndex(KEY_LATITUDE)),
                        mCursor.getInt(mCursor.getColumnIndex(KEY_LONGITUDE)),
                        mCursor.getDouble(mCursor.getColumnIndex(KEY_RADIUS)));
            }
        }
        return mCursor;

    }

    /**
     * Update the note using the details provided. The note to be updated is
     * specified using the rowId, and it is altered to use the title and body
     * values passed in
     *
     * @param rowId id of note to update
     * @param title value to set note title to
     * @param body value to set note body to
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateTranslate(long rowId, int latitude, int longitude, String address) {
        ContentValues args = new ContentValues();
        args.put(KEY_LATITUDE, latitude);
        args.put(KEY_LONGITUDE, longitude);
        args.put(KEY_ADDRESS, address);
        args.put(KEY_LAST_USE_TIME, DateUtilities.now());
        return mDb.update(DATABASE_TABLE_TRANSLATIONS, args, KEY_ROWID + "=" + rowId, null) > 0; //$NON-NLS-1$
    }

    public boolean updateType(long rowId, String type, int latitude, int longitude, double radius) {
        ContentValues args = new ContentValues();
        args.put(KEY_TYPE, type);
        args.put(KEY_LATITUDE, latitude);
        args.put(KEY_LONGITUDE, longitude);
        args.put(KEY_RADIUS, radius);
        args.put(KEY_LAST_USE_TIME, DateUtilities.now());
        return mDb.update(DATABASE_TABLE_TYPES, args, KEY_ROWID + "=" + rowId, null) > 0; //$NON-NLS-1$
    }
}
