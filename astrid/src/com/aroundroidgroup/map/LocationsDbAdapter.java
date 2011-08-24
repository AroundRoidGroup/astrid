package com.aroundroidgroup.map;

import java.util.Calendar;
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
    public static final String KEY_COORDINATES = "coordinates"; //$NON-NLS-1$
    public static final String KEY_BUSINESS_NAME = "businessName"; //$NON-NLS-1$
    public static final String KEY_TYPE_ID = "idOfTypeByCenterAndRadius"; //$NON-NLS-1$

    public static final String KEY_TYPE = "type"; //$NON-NLS-1$
    public static final String KEY_CENTER_POINT = "centerPoint"; //$NON-NLS-1$
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

    private final Context mCtx;

    private static final String CREATE_TABLE_TRANSLATIONS =
        "create table " + DATABASE_TABLE_TRANSLATIONS + " (" + KEY_ROWID + " integer primary key autoincrement, " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        + KEY_COORDINATES + " text not null, " + KEY_ADDRESS + " text not null, "  //$NON-NLS-1$ //$NON-NLS-2$
        + KEY_LAST_USE_TIME + " text not null) ;"; //$NON-NLS-1$
    private static final String CREATE_TABLE_TYPES =
        "create table " + DATABASE_TABLE_TYPES + " (" + KEY_ROWID + " integer primary key autoincrement, " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        + KEY_TYPE + " text not null, " + KEY_CENTER_POINT + " text not null, "  //$NON-NLS-1$ //$NON-NLS-2$
        + KEY_RADIUS + " text not null, " + KEY_LAST_USE_TIME + " text not null) ;";  //$NON-NLS-1$ //$NON-NLS-2$

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_TRANSLATIONS);
            db.execSQL(CREATE_TABLE_TYPES);
//            db.execSQL(
//                    "create table " + "_bank" + " (" + KEY_ROWID + " integer primary key autoincrement, " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
//                    + KEY_BUSINESS_NAME + " text not null, " + KEY_COORDINATES + " text not null, " //$NON-NLS-1$ //$NON-NLS-2$
//                    + KEY_TYPE_ID + " text not null) ;" //$NON-NLS-1$
//            );
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
    public long createTranslate(String coordinate, String address) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_COORDINATES, coordinate);
        initialValues.put(KEY_ADDRESS, address);
        initialValues.put(KEY_LAST_USE_TIME, Calendar.getInstance().getTime().toGMTString());
        return mDb.insert(DATABASE_TABLE_TRANSLATIONS, null, initialValues);
    }

    public long createType(String type, String centerPoint, double radius, Map<String, DPoint> data) {
        //TODO do the search by the radius with respect to some delta, because radius 100 and 100.5
        //TODO is actually the same for us
        boolean toCreateTableForType = false;
        ContentValues initialValues = new ContentValues();
        Cursor c = fetchByType(type);
        if (c == null)
            toCreateTableForType = true;
        else if (!c.moveToFirst())
            toCreateTableForType = true;
        if (c != null)
            c.close();
        initialValues.put(KEY_TYPE, type);
        initialValues.put(KEY_CENTER_POINT, centerPoint);
        initialValues.put(KEY_RADIUS, radius);
        initialValues.put(KEY_LAST_USE_TIME, Calendar.getInstance().getTime().toGMTString());
        Long rowID = mDb.insert(DATABASE_TABLE_TYPES, null, initialValues);
        String typeTableName = "_" + type; //$NON-NLS-1$
        if (data != null) {
            /* creating the table for the given type. the table contains business names and their coords */
            if (toCreateTableForType) {
                String typeTableSQL =
                    "create table if not exists " + typeTableName + " (" + KEY_ROWID + " integer primary key autoincrement, " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    + KEY_BUSINESS_NAME + " text not null, " + KEY_COORDINATES + " text not null) ;"; //$NON-NLS-1$ //$NON-NLS-2$
                mDb.execSQL(typeTableSQL);
            }
            for (Map.Entry<String, DPoint> p : data.entrySet()) {
                ContentValues pair = new ContentValues();
                pair.put(KEY_BUSINESS_NAME, p.getKey());
                pair.put(KEY_COORDINATES, p.getValue().toString());
                pair.put(KEY_TYPE_ID, rowID);
                mDb.insert(typeTableName, null, pair);
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
        return mDb.query(DATABASE_TABLE_TRANSLATIONS, new String[] {KEY_ROWID, KEY_COORDINATES,
                KEY_ADDRESS}, null, null, null, null, null);
    }

    public Cursor fetchAllTypes() {
        return mDb.query(DATABASE_TABLE_TYPES, new String[] {KEY_ROWID, KEY_CENTER_POINT,
                KEY_RADIUS}, null, null, null, null, null);
    }

    public String fetchByCoordinateFromType(String type, String coordinate) throws SQLException {
        Cursor mCursor =
            mDb.query(true, "_" + type, new String[] {KEY_BUSINESS_NAME}, //$NON-NLS-1$
                    KEY_COORDINATES + " ='" + coordinate + "'", //$NON-NLS-1$ //$NON-NLS-2$
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

    public Cursor fetchByCoordinate(String coordinate) throws SQLException{
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE_TRANSLATIONS, new String[] {KEY_ROWID,
                    KEY_COORDINATES, KEY_ADDRESS}, KEY_COORDINATES + " ='" + coordinate + "'", //$NON-NLS-1$ //$NON-NLS-2$
                    null, null, null, null, null);
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                updateTranslate(mCursor.getInt(mCursor.getColumnIndex(KEY_ROWID)),
                        mCursor.getString(mCursor.getColumnIndex(KEY_COORDINATES)),
                        mCursor.getString(mCursor.getColumnIndex(KEY_ADDRESS)));
            }
        }
        return mCursor;
    }

    public String fetchByCoordinateAsString(String coordinate) throws SQLException {
        Cursor mCursor = fetchByCoordinate(coordinate);
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
                    KEY_COORDINATES, KEY_ADDRESS}, KEY_ADDRESS + "='" + address + "'", //$NON-NLS-1$ //$NON-NLS-2$
                    null, null, null, null, null);
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                updateTranslate(mCursor.getInt(mCursor.getColumnIndex(KEY_ROWID)),
                        mCursor.getString(mCursor.getColumnIndex(KEY_COORDINATES)),
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
                    KEY_COORDINATES, KEY_ADDRESS}, KEY_ROWID + "=" + rowId, null, //$NON-NLS-1$
                    null, null, null, null);
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                updateTranslate(rowId, mCursor.getString(mCursor.getColumnIndex(KEY_COORDINATES)),
                        mCursor.getString(mCursor.getColumnIndex(KEY_ADDRESS)));
            }
        }
        return mCursor;

    }

    public Cursor fetchByType(String type) throws SQLException{
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE_TYPES, new String[] {KEY_ROWID, KEY_TYPE,
                    KEY_CENTER_POINT, KEY_RADIUS}, KEY_TYPE + "='" + type + "'", //$NON-NLS-1$ //$NON-NLS-2$
                    null, null, null, null, null);
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                updateType(mCursor.getInt(mCursor.getColumnIndex(KEY_ROWID)),
                        mCursor.getString(mCursor.getColumnIndex(KEY_TYPE)),
                        mCursor.getString(mCursor.getColumnIndex(KEY_CENTER_POINT)),
                        mCursor.getString(mCursor.getColumnIndex(KEY_RADIUS)));
                mCursor.close();
                mCursor = mDb.query("_" + type, new String[] {KEY_BUSINESS_NAME, KEY_COORDINATES}, //$NON-NLS-1$
                        null, null, null, null, null);
            }
        }
        return mCursor;
    }

    public Cursor fetchByTypeComplex(String type, String centerPoint, String radius) throws SQLException {
        if (type == null || centerPoint == null || radius == null)
            return null;
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE_TYPES, new String[] {KEY_ROWID, KEY_TYPE, KEY_CENTER_POINT, KEY_RADIUS},
                    KEY_TYPE + "='" + type + "' AND " + KEY_CENTER_POINT + "='" + centerPoint + "' AND " + KEY_RADIUS + "='" + radius + "'", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
                    null, null, null, null, null);
        Cursor mCursor2 =
            mDb.query(true, DATABASE_TABLE_TYPES, new String[] {KEY_ROWID, KEY_TYPE, KEY_CENTER_POINT, KEY_RADIUS},
                    null,
                    null, null, null, null, null);
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                Toast.makeText(mCtx, "Type = " + mCursor.getString(mCursor.getColumnIndex(KEY_TYPE))
                        + " Center = " + mCursor.getString(mCursor.getColumnIndex(KEY_CENTER_POINT))
                        + " Radius = " + mCursor.getString(mCursor.getColumnIndex(KEY_RADIUS)), Toast.LENGTH_LONG).show();
                mCursor.moveToNext();
            }
            mCursor.close();
        }
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                updateType(mCursor.getInt(mCursor.getColumnIndex(KEY_ROWID)),
                        mCursor.getString(mCursor.getColumnIndex(KEY_TYPE)),
                        mCursor.getString(mCursor.getColumnIndex(KEY_CENTER_POINT)),
                        mCursor.getString(mCursor.getColumnIndex(KEY_RADIUS)));
                mCursor.close();
                mCursor = mDb.query("_" + type, new String[] {KEY_BUSINESS_NAME, KEY_COORDINATES}, //$NON-NLS-1$
                        null, null, null, null, null);
            }
            else mCursor.close();
        }
        return mCursor;
    }

    public Cursor fetchType(long rowId) throws SQLException {

        Cursor mCursor =

            mDb.query(true, DATABASE_TABLE_TYPES, new String[] {KEY_ROWID, KEY_TYPE,
                    KEY_CENTER_POINT, KEY_RADIUS}, KEY_ROWID + "=" + rowId, null, //$NON-NLS-1$
                    null, null, null, null);
        if (mCursor != null) {
            if (mCursor.moveToFirst()) {
                updateType(rowId, mCursor.getString(mCursor.getColumnIndex(KEY_TYPE)),
                        mCursor.getString(mCursor.getColumnIndex(KEY_CENTER_POINT)),
                        mCursor.getString(mCursor.getColumnIndex(KEY_RADIUS)));
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
    public boolean updateTranslate(long rowId, String coordinate, String address) {
        ContentValues args = new ContentValues();
        args.put(KEY_COORDINATES, coordinate);
        args.put(KEY_ADDRESS, address);
        args.put(KEY_LAST_USE_TIME, DateUtilities.now());
        return mDb.update(DATABASE_TABLE_TRANSLATIONS, args, KEY_ROWID + "=" + rowId, null) > 0; //$NON-NLS-1$
    }

    public boolean updateType(long rowId, String type, String centerPoint, String radius) {
        ContentValues args = new ContentValues();
        args.put(KEY_TYPE, type);
        args.put(KEY_CENTER_POINT, centerPoint);
        args.put(KEY_RADIUS, radius);
        args.put(KEY_LAST_USE_TIME, Calendar.getInstance().getTime().toGMTString());
        return mDb.update(DATABASE_TABLE_TYPES, args, KEY_ROWID + "=" + rowId, null) > 0; //$NON-NLS-1$
    }
}
