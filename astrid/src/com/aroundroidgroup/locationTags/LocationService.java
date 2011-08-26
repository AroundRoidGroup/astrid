package com.aroundroidgroup.locationTags;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.aroundroidgroup.map.DPoint;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.api.R;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.MetadataApiDao.MetadataCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;

/***
 * Provides operations for working with location based reminders
 * for tasks
 */
public class LocationService {

        /**
         *  Returns an array of all the locations-by-type associated with
         *  the task who's id is taskID
         */
    public String[] getLocationsByTypeAsArray(long taskID){
        return getLocationPropertyAsArray(taskID,LocationFields.locationsType,
                LocationFields.METADATA_KEY_BY_TYPE);
    }

    /**
     *  Returns an array of all the locations-by-people associated with
     *  the task who's id is taskID
     */
    public String[] getLocationsByPeopleAsArray(long taskID){
        return getLocationPropertyAsArray(taskID,LocationFields.peopleLocations,
                LocationFields.METADATA_KEY_BY_PEOPLE);
    }

    /**
     * Returns an array of all the specific locations associated with
     *  the task who's id is taskID
     */
    public String[] getLocationsBySpecificAsArray(long taskID){
        return getLocationPropertyAsArray(taskID,LocationFields.specificLocations,
                LocationFields.METADATA_KEY_BY_SPECIFIC);
    }

    /**
     * Returns an array of all the values of Property prop under the
     * metadata key KEY  associated with the task who's id is taskID
     */
    private String[] getLocationPropertyAsArray(long taskID, StringProperty prop, String KEY){

        TodorooCursor<Metadata> cursor = getLocations(taskID, prop, KEY);

        try {
            String[] array = new String[cursor.getCount()];
            for (int i = 0; i < array.length; i++) {
                cursor.moveToNext();
                array[i] = cursor.get(prop);
            }
            return array;
        } finally {
            cursor.close();
        }
    }

    /**
     * Sets the locations-by-type associated with the task who's id is taskID
     * returns true if the saving was successful
     */
    public boolean syncLocationsByType(long taskID, LinkedHashSet<String> locations){
        return syncLocations(taskID, locations, LocationFields.locationsType,
                LocationFields.METADATA_KEY_BY_TYPE);
    }

    /**
     * Sets the specific location associated with the task who's id is taskID
     * returns true if the saving was successful
     */
    public boolean syncLocationsBySpecific(long taskID, LinkedHashSet<String> locations){
        return syncLocations(taskID, locations, LocationFields.specificLocations,
                LocationFields.METADATA_KEY_BY_SPECIFIC);
    }

    /**
     * Sets the locations-by-people associated with the task who's id is taskID
     * returns true if the saving was successful
     */
    public boolean syncLocationsByPeople(long taskID, LinkedHashSet<String> locations){
        return syncLocations(taskID, locations, LocationFields.peopleLocations,
                LocationFields.METADATA_KEY_BY_PEOPLE);
    }

    /**
     * Sets the values of Property prop under the metadata key KEY
     * associated with the task who's id is taskID
     * returns true if the saving was successful
     */
    private boolean syncLocations(long taskID, LinkedHashSet<String> locations,StringProperty prop, String KEY) {
        MetadataService service = PluginServices.getMetadataService();

        ArrayList<Metadata> metadata = new ArrayList<Metadata>();
        for(String location : locations) {
            Metadata item = new Metadata();
            item.setValue(Metadata.KEY, KEY);
            item.setValue(prop, location);
            metadata.add(item);
        }

        return service.synchronizeMetadata(taskID, metadata, Metadata.KEY.eq(KEY)) > 0;
    }

    /**
     * Returns a cursor of all the locations-by-type associated with
     *  the task who's id is taskID
     */
    public TodorooCursor<Metadata> getLocationsByType(long taskID){
        return getLocations(taskID, LocationFields.locationsType,
                LocationFields.METADATA_KEY_BY_TYPE);
    }

    /**
     * Returns a cursor of all the specific locations associated with
     *  the task who's id is taskID
     */
    public TodorooCursor<Metadata> getLocationsBySpecific(long taskID){
        return getLocations(taskID, LocationFields.specificLocations,
                LocationFields.METADATA_KEY_BY_SPECIFIC);
    }

    /**
     * Returns a cursor of all the locations-by-people associated with
     *  the task who's id is taskID
     */
    public TodorooCursor<Metadata> getLocationsByPeople(long taskID){
        return getLocations(taskID, LocationFields.peopleLocations,
                LocationFields.METADATA_KEY_BY_PEOPLE);
    }

    /**
     * Returns a cursor of all the values of Property prop under the
     * metadata key KEY associated with the task who's id is taskID
     */
    private TodorooCursor<Metadata> getLocations(long taskID, StringProperty prop, String KEY) {
        Query query = Query.select(prop).where(Criterion.
                and(MetadataCriteria.withKey(KEY),
                        MetadataCriteria.byTask(taskID)));
        return new MetadataDao().query(query);
    }

    /**
     * Returns an array of all the locations-by-type associated with
     * any of the tasks
     */
    public String[] getAllLocationsByType(){
        return getAllLocations(LocationFields.locationsType,
                LocationFields.METADATA_KEY_BY_TYPE);
    }

    /**
     * Returns an array of all the specific locations associated with
     * any of the tasks
     */
    public String[] getAllLocationsBySpecific(){
        return getAllLocations(LocationFields.specificLocations,
                LocationFields.METADATA_KEY_BY_SPECIFIC);
    }

    /**
     * Returns an array of all the locations-by-people associated with
     * any of the tasks
     */
    public String[] getAllLocationsByPeople(){
        return getAllLocations(LocationFields.peopleLocations,
                LocationFields.METADATA_KEY_BY_PEOPLE);
    }

    /**
     * Returns an array of all the values of property prop ender the metadata
     * key KEY associated to any of the tasks
     */
    private String[] getAllLocations(StringProperty prop, String KEY){
        Query query = Query.select(prop.as(prop.name)).
        join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
        where(MetadataCriteria.withKey(KEY))
        .groupBy(prop);
        TodorooCursor<Metadata> cursor = new MetadataDao().query(query);
        try {
            String[] array = new String[cursor.getCount()];
            for (int i = 0; i < array.length; i++) {
                cursor.moveToNext();
                array[i] = cursor.get(prop);
            }
            return array;
        } finally {
            cursor.close();
        }
    }

    /**
     * Returns true if and only if there is some kind of location associated
     * with the task who's id is taskID
     */
    public boolean isLocationTask(long taskID) {
        return contaionsLocationsByType(taskID) ||
        containsLocationsByPeople(taskID) ||
        containsLocationsBySpecific(taskID);
    }

    /**
     * Returns true if and only if there are specific location associated
     * with the task who's id is taskID
     */
    public boolean containsLocationsBySpecific(long taskID) {
        return getLocationsBySpecificAsArray(taskID).length>0;
    }

    /**
     * Returns true if and only if there are locations-by-people associated
     * with the task who's id is taskID
     */
    public boolean containsLocationsByPeople(long taskID) {
        return getLocationsByPeopleAsArray(taskID).length>0;
    }

    /**
     * Returns true if and only if there are values of property
     * PROP under the metadata key KEY associated
     * with the task who's id is taskID
     */
    public boolean contaionsLocationsByType(long taskID) {
        return getLocationsByTypeAsArray(taskID).length>0;
    }

    /**
     * Returns the alert distance to be used when driving for the task
     * who's id is taskID
     */
    public int getCarRadius(long taskID) {
        Query query = Query.select(LocationFields.carRadius).where(Criterion.
                and(MetadataCriteria.withKey(LocationFields.CAR_RADIUS_METADATA_KEY),
                        MetadataCriteria.byTask(taskID)));
        TodorooCursor<Metadata> cursor = new MetadataDao().query(query);
        try {
            int defaultR = Integer.parseInt(Preferences.getStringValue(R.string.p_rmd_default_car_radius_key));
            if (cursor.getCount()==0)
                return defaultR;
            cursor.moveToNext();
            String str = cursor.get(LocationFields.carRadius);
            return str==null?defaultR:Integer.parseInt(str);
        } finally {
            cursor.close();
        }
    }

    /**
     * Returns the alert distance to be used when walking for the task
     * who's id is taskID
     */
    public int getFootRadius(long taskID) {
        Query query = Query.select(LocationFields.footRadius).where(Criterion.
                and(MetadataCriteria.withKey(LocationFields.FOOT_RADIUS_METADATA_KEY),
                        MetadataCriteria.byTask(taskID)));
        TodorooCursor<Metadata> cursor = new MetadataDao().query(query);
        try {
            int defaultR = Integer.parseInt(Preferences.getStringValue(R.string.p_rmd_default_foot_radius_key));
            if (cursor.getCount()==0)
                return defaultR;
            cursor.moveToNext();
            String str = cursor.get(LocationFields.footRadius);
            return str==null?defaultR:Integer.parseInt(str);
        } finally {
            cursor.close();
        }
    }

    /**
     * Sets the alert distance to be used when driving for the task
     * who's id is taskID
     * returns true if and only if the saving was successful
     */
    public boolean syncCarRadius(long taskID, int radius) {
        MetadataService service = PluginServices.getMetadataService();

        ArrayList<Metadata> metadata = new ArrayList<Metadata>();
        Metadata item = new Metadata();
        item.setValue(Metadata.KEY, LocationFields.CAR_RADIUS_METADATA_KEY);
        item.setValue(LocationFields.carRadius, radius+""); //$NON-NLS-1$
        metadata.add(item);
        return service.synchronizeMetadata(taskID, metadata, Metadata.KEY.eq(LocationFields.CAR_RADIUS_METADATA_KEY)) > 0;
    }

    /**
     * Sets the alert distance to be used when walking for the task
     * who's id is taskID
     * returns true if and only if the saving was successful
     */
    public boolean syncFootRadius(long taskID, int radius) {
        MetadataService service = PluginServices.getMetadataService();

        ArrayList<Metadata> metadata = new ArrayList<Metadata>();
        Metadata item = new Metadata();
        item.setValue(Metadata.KEY, LocationFields.FOOT_RADIUS_METADATA_KEY);
        item.setValue(LocationFields.footRadius, radius+""); //$NON-NLS-1$
        metadata.add(item);


        return service.synchronizeMetadata(taskID, metadata, Metadata.KEY.eq(LocationFields.FOOT_RADIUS_METADATA_KEY)) > 0;
    }

    /**
     * Returns the minimal distance relevant to any task at the moment
     */
    public int minimalRadiusRelevant(double speed) {
        StringProperty prop;
        String metadataKey;
        int defaultRadKey;
        if (speed<LocationFields.carSpeedThreshold){
            prop=LocationFields.footRadius;
            metadataKey = LocationFields.FOOT_RADIUS_METADATA_KEY;
            defaultRadKey = R.string.p_rmd_default_foot_radius_key;
        }else{
            prop=LocationFields.carRadius;
            metadataKey = LocationFields.CAR_RADIUS_METADATA_KEY;
            defaultRadKey = R.string.p_rmd_default_car_radius_key;
        }
        Query query = Query.select(prop).where(
                MetadataCriteria.withKey(metadataKey)
        ).orderBy(Order.desc(prop));
        TodorooCursor<Metadata> cursor = new MetadataDao().query(query);
        try {
            int defaultR = Integer.parseInt(Preferences.getStringValue(defaultRadKey));
            int min = 10*defaultR;
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                String str = cursor.get(prop);
                if(str!=null)
                    min = Math.min(min, Integer.parseInt(str));
                else
                    min = Math.min(min, defaultR);
            }
            return min;
        } finally {
            cursor.close();
        }
    }

    /**
     * Returns the list of coordinates (latitude,longitude) of the places
     * under the type locType that are blacklisted for the task who's
     * id is taskID
     */
    public List<DPoint> getLocationsByTypeBlacklist(long taskID,String locType){
        return getBlacklistForType(getLocationPropertyAsArray(taskID,LocationFields.blacklistLocations,
                LocationFields.BLACKLIST_METADATA_KEY),locType);
    }

    /**
     * Returns the list of coordinates (latitude,longitude) of the places
     * under the type locType that are blacklisted
     */
    private List<DPoint> getBlacklistForType(
            String[] strings, String locType) {
        for (String str : strings){
            int dividor = str.indexOf(LocationFields.delimiter);
            if (locType.compareTo(str.substring(0, dividor))==0)
                return parseLocations(str.substring(dividor+1));
        }
        return new ArrayList<DPoint>();
    }

    /**
     * Parses the string str to the list of coordinates which are
     * returned as a list of DPoints
     */
    private List<DPoint> parseLocations(String str) {
        int index = 0;
        ArrayList<DPoint> arr = new ArrayList<DPoint>();
        while(index!=-1){
            index = str.indexOf(LocationFields.delimiter);
            if (index==-1)
                arr.add(new DPoint(str));
            else{
                arr.add(new DPoint(str.substring(0,index)));
                str = str.substring(index+1);
            }
        }

        return arr;
    }

    /**
     * Sets the list of coordinates (latitude,longitude) of the places
     * under the type locType that are blacklisted for the task who's
     * id is taskID
     */
    public boolean syncLocationsByTypeBlacklist(long taskID, String locType, DPoint[] arr){
        String[] stringsArr = new String[arr.length];
        for (int i=0; i<arr.length;i++)
            stringsArr[i]=arr[i].toString();
        String[] strings = getLocationPropertyAsArray(taskID,LocationFields.blacklistLocations,
                LocationFields.BLACKLIST_METADATA_KEY);

        LinkedHashSet<String> set = new LinkedHashSet<String>();
        String newStr = locType;

        for (String str : strings){
            int dividor = str.indexOf(LocationFields.delimiter);
            if (locType.compareTo(str.substring(0, dividor-1))!=0)
                set.add(str);
        }

        for(String place: stringsArr){
            newStr=newStr+LocationFields.delimiter+place;
        }
        set.add(newStr);
        return syncLocations(taskID, set, LocationFields.blacklistLocations,
                LocationFields.BLACKLIST_METADATA_KEY);
    }

}
