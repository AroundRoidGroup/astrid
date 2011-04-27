package com.aroundroidgroup.astrid.googleAccounts;

import java.util.Collection;
import java.util.Iterator;


public class AroundRoidAppConstants {
    public static final String appMain = "https://aroundroid.appspot.com"; //$NON-NLS-1$
    public static final String gpsUrl = appMain + "/aroundgps"; //$NON-NLS-1$
    public static final String loginUrl = appMain+"/_ah/login?continue=http://localhost/&auth="; //$NON-NLS-1$
    public static final String usersDelimiter = "::"; //$NON-NLS-1$

    //TODO : find proper method
    public static String join(Collection<String> s, String delimiter) {
        StringBuffer buffer = new StringBuffer();
        Iterator<String> iter = s.iterator();
        while (iter.hasNext()) {
            buffer.append(iter.next());
            if (iter.hasNext()) {
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }

    public static String join(String[] sArr, String delimiter) {
        StringBuffer buffer = new StringBuffer();
        for ( int i = 0; i < sArr.length ; i++){
            buffer.append(sArr[i]);
            if (i < sArr.length-1){
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }
}
