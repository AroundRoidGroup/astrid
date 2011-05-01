package com.aroundroidgroup.locationTags;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.MetadataApiDao.MetadataCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;

public class LocationService {

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



}
