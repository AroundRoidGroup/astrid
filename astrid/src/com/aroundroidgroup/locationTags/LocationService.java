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
        return getLocationPropertyAsArray(taskID,LocationFields.locationsType);
    }

    public String[] getLocationsByPeopleAsArray(long taskID){
        return getLocationPropertyAsArray(taskID,LocationFields.peopleLocations);
    }

    public String[] getLocationsBySpecificAsArray(long taskID){
        return getLocationPropertyAsArray(taskID,LocationFields.specificLocations);
    }

    private String[] getLocationPropertyAsArray(long taskId, StringProperty prop){

        TodorooCursor<Metadata> cursor = getLocations(taskId, prop);

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
        return syncLocations(taskId, locations, LocationFields.locationsType);
    }

    public boolean syncLocationsBySpecific(long taskId, LinkedHashSet<String> locations){
        return syncLocations(taskId, locations, LocationFields.specificLocations);
    }

    public boolean syncLocationsByPeople(long taskId, LinkedHashSet<String> locations){
        return syncLocations(taskId, locations, LocationFields.peopleLocations);
    }

    private boolean syncLocations(long taskId, LinkedHashSet<String> locations,StringProperty prop) {
        MetadataService service = PluginServices.getMetadataService();

        ArrayList<Metadata> metadata = new ArrayList<Metadata>();
        for(String location : locations) {
            Metadata item = new Metadata();
            item.setValue(Metadata.KEY, LocationFields.METADATA_KEY);
            item.setValue(prop, location);
            metadata.add(item);
        }

        return service.synchronizeMetadata(taskId, metadata, Metadata.KEY.eq(LocationFields.METADATA_KEY)) > 0;
    }

    public TodorooCursor<Metadata> getLocationsByType(long taskId){
        return getLocations(taskId, LocationFields.locationsType);
    }

    public TodorooCursor<Metadata> getLocationsBySpecific(long taskId){
        return getLocations(taskId, LocationFields.specificLocations);
    }

    public TodorooCursor<Metadata> getLocationsByPeople(long taskId){
        return getLocations(taskId, LocationFields.peopleLocations);
    }

    private TodorooCursor<Metadata> getLocations(long taskId, StringProperty prop) {
        Query query = Query.select(prop).where(Criterion.
                and(MetadataCriteria.withKey(LocationFields.METADATA_KEY),
                        MetadataCriteria.byTask(taskId)));
        return new MetadataDao().query(query);//TODO: maybe dont need to create metadatadao every time
    }

    public String[] getAllLocationsByType(){
        return getAllLocations(LocationFields.locationsType);
    }

    public String[] getAllLocationsBySpecific(){
        return getAllLocations(LocationFields.specificLocations);
    }

    public String[] getAllLocationsByPeople(){
        return getAllLocations(LocationFields.peopleLocations);
    }

    private String[] getAllLocations(StringProperty prop){
        Query query = Query.select(prop.as(prop.name)).
        join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
        where(Criterion.and(Criterion.all, MetadataCriteria.withKey(LocationFields.METADATA_KEY)))
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
