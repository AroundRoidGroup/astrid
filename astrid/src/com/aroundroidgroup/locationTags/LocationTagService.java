package com.aroundroidgroup.locationTags;

import java.util.ArrayList;
import java.util.List;

import com.todoroo.astrid.tags.TagService;

public final class LocationTagService {


    public static String locationTagSeperator = "@"; //$NON-NLS-1$
    public static String tagSeperator = ", "; //$NON-NLS-1$


    public static String[] getLocationTags(long taskId){
        TagService ts = new TagService();
        String tags = ts.getTagsAsString(taskId);
        String[] tagsArr = tags.split(tagSeperator);
        List<String> ls = new ArrayList<String>();
        for(String s : tagsArr){
            if (s.startsWith(locationTagSeperator))
                ls.add(s.substring(1));
        }
        return ls.toArray(new String[ls.size()]);
    }

    public static boolean isLocationTask(long taskId){
        return (getLocationTags(taskId).length>0);
    }
}
