package com.aroundroidgroup.astrid.googleAccounts;

import com.todoroo.andlib.utility.DateUtilities;

/***
 * Many Constants and some general methoed of the AroundRoid App :)
 * @author Tomer
 *
 */
public class AroundRoidAppConstants {

    public static final String appMain = "https://aroundroid.appspot.com"; //$NON-NLS-1$
    public static final String gpsUrl = appMain + "/aroundgps"; //$NON-NLS-1$
    public static final String inviterUrl = appMain + "/inviteFriend"; //$NON-NLS-1$
    public static final String loginUrl = appMain+"/_ah/login?continue=http://localhost/&auth="; //$NON-NLS-1$
    public static final String usersDelimiter = "::"; //$NON-NLS-1$

    public static final String STATUS_UNREGISTERED = "Unregistered"; //$NON-NLS-1$
    public static final String STATUS_ONLINE = "Yes"; //$NON-NLS-1$
    public static final String STATUS_OFFLINE = "No"; //$NON-NLS-1$

    public static final String emailRegularExpression = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"; //$NON-NLS-1$

    //it is 10 minutes
    public final static long maximumValidTime = 1000 * 60 * 10;

    /***
     * Checks if the timestamp provided represents a valid object.
     * @param timeStamp
     * @return
     */
    public static boolean timeCheckValid(long timeStamp){
        return DateUtilities.now() - timeStamp <= maximumValidTime;
    }

    /***
     * joins all strings in sArr with delimiter delimiter to one big string
     * @param sArr
     * @param delimiter
     * @return
     */
    //TODO consider moving from one big string to xml
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
