package com.aroundroidgroup.locationTags;

import java.util.ArrayList;
import java.util.List;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.MetadataApiDao.MetadataCriteria;

public class PeopleLocationService {

    public static String locationTagSeperator = "::"; //$NON-NLS-1$
    public static String tagSeperator = ", "; //$NON-NLS-1$


    public static String[] getPeopleFollowed(long taskId){
        String people = getPeopleToCheckAsString(taskId);
        String[] tagsArr = people.split(tagSeperator);
        List<String> ls = new ArrayList<String>();
        for(String s : tagsArr){
            if (s.startsWith(locationTagSeperator))
                ls.add(s.substring(1));
        }
        return ls.toArray(new String[ls.size()]);
    }

    private static String getPeopleToCheckAsString(long taskId) {
        TodorooCursor<Metadata> cursor = (new MetadataDao()).query(
                Query.select(Metadata.PROPERTIES).where(
                        MetadataCriteria.byTaskAndwithKey(taskId,
                                LocationFields.METADATA_KEY)));
        Metadata metadata = new Metadata();
        try {
            cursor.moveToFirst();
            if (cursor.isAfterLast())
                return new String();
            metadata.readFromCursor(cursor);
            return metadata.getValue(LocationFields.peopleLocations);
        } finally {
            cursor.close();
        }
    }

    public static boolean isPeopleTask(long taskId){
        return (getPeopleFollowed(taskId).length>0);
    }
}
