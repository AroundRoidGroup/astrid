package com.aroundroidgroup.locationTags;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

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

public class LocationService {

    private static final String MOTI_DIVIDOR = "@";

    //TODO : check synchronized??

    public String[] getLocationsByTypeAsArray(long taskID){
        return getLocationPropertyAsArray(taskID,LocationFields.locationsType,
                LocationFields.METADATA_KEY_BY_TYPE);
    }

    public String[] getLocationsByPeopleAsArray(long taskID){
        return getLocationPropertyAsArray(taskID,LocationFields.peopleLocations,
                LocationFields.METADATA_KEY_BY_PEOPLE);
    }

    public String[] getLocationsBySpecificAsArray(long taskID){
        return getLocationPropertyAsArray(taskID,LocationFields.specificLocations,
                LocationFields.METADATA_KEY_BY_SPECIFIC);
    }

    private String[] getLocationPropertyAsArray(long taskId, StringProperty prop, String KEY){

        TodorooCursor<Metadata> cursor = getLocations(taskId, prop, KEY);

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

    public boolean syncLocationsByType(long taskId, LinkedHashSet<String> locations){
        return syncLocations(taskId, locations, LocationFields.locationsType,
                LocationFields.METADATA_KEY_BY_TYPE);
    }

    public boolean syncLocationsBySpecific(long taskId, LinkedHashSet<String> locations){
        return syncLocations(taskId, locations, LocationFields.specificLocations,
                LocationFields.METADATA_KEY_BY_SPECIFIC);
    }

    public boolean syncLocationsByPeople(long taskId, LinkedHashSet<String> locations){
        return syncLocations(taskId, locations, LocationFields.peopleLocations,
                LocationFields.METADATA_KEY_BY_PEOPLE);
    }

    private boolean syncLocations(long taskId, LinkedHashSet<String> locations,StringProperty prop, String KEY) {
        MetadataService service = PluginServices.getMetadataService();

        ArrayList<Metadata> metadata = new ArrayList<Metadata>();
        for(String location : locations) {
            Metadata item = new Metadata();
            item.setValue(Metadata.KEY, KEY);
            item.setValue(prop, location);
            metadata.add(item);
        }

        return service.synchronizeMetadata(taskId, metadata, Metadata.KEY.eq(KEY)) > 0;
    }

    public TodorooCursor<Metadata> getLocationsByType(long taskId){
        return getLocations(taskId, LocationFields.locationsType,
                LocationFields.METADATA_KEY_BY_TYPE);
    }

    public TodorooCursor<Metadata> getLocationsBySpecific(long taskId){
        return getLocations(taskId, LocationFields.specificLocations,
                LocationFields.METADATA_KEY_BY_SPECIFIC);
    }

    public TodorooCursor<Metadata> getLocationsByPeople(long taskId){
        return getLocations(taskId, LocationFields.peopleLocations,
                LocationFields.METADATA_KEY_BY_PEOPLE);
    }
    private TodorooCursor<Metadata> getLocations(long taskId, StringProperty prop, String KEY) {
        Query query = Query.select(prop).where(Criterion.
                and(MetadataCriteria.withKey(KEY),
                        MetadataCriteria.byTask(taskId)));
        return new MetadataDao().query(query);//TODO: maybe dont need to create metadatadao every time
    }

    public String[] getAllLocationsByType(){
        return getAllLocations(LocationFields.locationsType,
                LocationFields.METADATA_KEY_BY_TYPE);
    }

    public String[] getAllLocationsBySpecific(){
        return getAllLocations(LocationFields.specificLocations,
                LocationFields.METADATA_KEY_BY_SPECIFIC);
    }

    public String[] getAllLocationsByPeople(){
        return getAllLocations(LocationFields.peopleLocations,
                LocationFields.METADATA_KEY_BY_PEOPLE);
    }

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

    public boolean isLocationTask(long id) {
        return contaionsLocationsByType(id) ||
        containsLocationsByPeople(id) ||
        containsLocationsBySpecific(id);
    }

    public boolean containsLocationsBySpecific(long id) {
        return getLocationsBySpecificAsArray(id).length>0;
    }

    public boolean containsLocationsByPeople(long id) {
        return getLocationsByPeopleAsArray(id).length>0;
    }

    public boolean contaionsLocationsByType(long id) {
        return getLocationsByTypeAsArray(id).length>0;
    }

    public int getCarRadius(long taskId) {
        Query query = Query.select(LocationFields.carRadius).where(Criterion.
                and(MetadataCriteria.withKey(LocationFields.CAR_RADIUS_METADATA_KEY),
                        MetadataCriteria.byTask(taskId)));
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

    public int getFootRadius(long taskId) {
        Query query = Query.select(LocationFields.footRadius).where(Criterion.
                and(MetadataCriteria.withKey(LocationFields.FOOT_RADIUS_METADATA_KEY),
                        MetadataCriteria.byTask(taskId)));
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

    public boolean syncCarRadius(long taskId, int radius) {
        MetadataService service = PluginServices.getMetadataService();

        ArrayList<Metadata> metadata = new ArrayList<Metadata>();
        Metadata item = new Metadata();
        item.setValue(Metadata.KEY, LocationFields.CAR_RADIUS_METADATA_KEY);
        item.setValue(LocationFields.carRadius, radius+""); //$NON-NLS-1$
        metadata.add(item);
        return service.synchronizeMetadata(taskId, metadata, Metadata.KEY.eq(LocationFields.CAR_RADIUS_METADATA_KEY)) > 0;
    }

    public boolean syncFootRadius(long taskId, int radius) {
        MetadataService service = PluginServices.getMetadataService();

        ArrayList<Metadata> metadata = new ArrayList<Metadata>();
        Metadata item = new Metadata();
        item.setValue(Metadata.KEY, LocationFields.FOOT_RADIUS_METADATA_KEY);
        item.setValue(LocationFields.footRadius, radius+""); //$NON-NLS-1$
        metadata.add(item);


        return service.synchronizeMetadata(taskId, metadata, Metadata.KEY.eq(LocationFields.FOOT_RADIUS_METADATA_KEY)) > 0;
    }

    public int minimalRadiusRelevant(double speed) {
        StringProperty prop;
        String metadataKey;
        int defaultRadKey;
        if (speed<25*1000/60/60){
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



//////////////////////////////////////TODO:make it all better


    public List<String> getLocationsByTypeSpecial(long taskId,String key){//TODO: be more percise
        return getLocationsListByType(getLocationPropertyAsArray(taskId,LocationFields.motiLocations,
                LocationFields.MOTI_METADATA_KEY),key);
    }
    /* returns the string array mapped to the String key inside the DP represented in the parameter strings */
    private List<String> getLocationsListByType(
            String[] strings, String key) {
        for (String str : strings){
            int dividor = str.indexOf(MOTI_DIVIDOR);
            if (key.compareTo(str.substring(0, dividor))==0)
                return parseLocations(str.substring(dividor+1));
        }
        return new ArrayList<String>();
    }

    private List<String> parseLocations(String str) {
        int index = 0;
        ArrayList<String> arr = new ArrayList<String>();
        while(index!=-1){
            index = str.indexOf(MOTI_DIVIDOR);
            if (index==-1)
                arr.add(str);
            else{
                arr.add(str.substring(0,index));
                str = str.substring(index+1);
            }
        }

        return arr;
    }

    public boolean syncLocationsByTypeSpecial(long taskID, String key, String[] arr){
        String[] strings = getLocationPropertyAsArray(taskID,LocationFields.motiLocations,
                LocationFields.MOTI_METADATA_KEY);

        LinkedHashSet<String> set = new LinkedHashSet<String>();
        String newStr = key;

        for (String str : strings){
            int dividor = str.indexOf(MOTI_DIVIDOR);
            if (key.compareTo(str.substring(0, dividor-1))!=0)
                set.add(str);
        }

        for(String place: arr){
            newStr=newStr+MOTI_DIVIDOR+place;
        }
        set.add(newStr);
        return syncLocations(taskID, set, LocationFields.motiLocations,
                LocationFields.MOTI_METADATA_KEY);


    }


}
