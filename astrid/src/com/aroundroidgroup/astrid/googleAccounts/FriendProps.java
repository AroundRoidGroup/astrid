package com.aroundroidgroup.astrid.googleAccounts;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/***
 * Contains information about someone's location.
 * intended to be used both for reading from cursor (without getting rowid and contact id)
 * and for reading from xml server response.
 * @author Tomer
 *
 */
public class FriendProps{

    public final static String root = "Friend"; //$NON-NLS-1$
    public final static String[] props = new String[]{"Latitude","Longtitude","Mail","Timestamp","Valid"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

    private String lat,lon;
    private String valid;
    private double dlat,dlon;
    private String mail;
    private long timestamp;
    private String time;

    public FriendProps() {
        //empty
    }

    /***
     * load lat lon mail time and valid from an array of size props.length
     * @param arr
     */
    public void loadArr (String[] arr){
        if (arr.length!=props.length){
            return;
        }
        setLat(arr[0]);
        setLon(arr[1]);
        setMail(arr[2]);
        setTime(arr[3]);
        setValid(arr[4]);
    }

    /***
     * converts a list of string array to a list of friend props (uses loadArr)
     * @param arrLst
     * @return list of friend props
     */
    public static List<FriendProps> fromArrList(List<String[]> arrLst){
        List<FriendProps> fpl = new ArrayList<FriendProps>(arrLst.size());
        for(String[] arr : arrLst){
            FriendProps fp = new FriendProps();
            fp.loadArr(arr);
            fpl.add(fp);
        }
        return fpl;
    }

    private final static Comparator<FriendProps> mailComparator = new Comparator<FriendProps>(){
        @Override
        public int compare(FriendProps object1, FriendProps object2) {
            return object1.getMail().compareTo(object2.getMail());
        }
    };

    /***
     * returns a compator for comparing friend props by mail addresses
     * @return
     */
    public static Comparator<FriendProps> getMailComparator(){
        return mailComparator;
    }

    /***
     *
     * @return latitude
     */
    public String getLat() {
        return lat;
    }

    /***
     * sets the latitude
     * @param lat
     */
    public void setLat(String lat) {
        this.lat = lat;
        this.dlat = (Double.parseDouble(lat));
    }

    /***
     *
     * @return logitude
     */
    public String getLon() {
        return lon;
    }

    /***
     * sets the logitude
     * @param lon
     */
    public void setLon(String lon) {
        this.lon = lon;
        this.dlon = (Double.parseDouble(lon));
    }

    /***
     *
     * @return email address
     */
    public String getMail() {
        return mail;
    }

    /***
     * sets the mail address
     * @param mail
     */
    public void setMail(String mail) {
        this.mail = mail;
    }

    /***
     * sets the latitude in double representation
     * @param dlat
     */
    public void setDlat(double dlat) {
        this.dlat = dlat;
        this.lat = String.valueOf(dlat);
    }

    /***
     * @return latitude as double
     */
    public double getDlat() {
        return dlat;
    }

    /***
     * sets longitude in double representation
     * @param dlon
     */
    public void setDlon(double dlon) {
        this.dlon = dlon;
        this.lon = String.valueOf(dlon);
    }

    /***
     *
     * @return longitude in double representation
     */
    public double getDlon() {
        return dlon;
    }

    /***
     * sets timestamp in long representation
     * @param timestamp
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        this.time = String.valueOf(timestamp);
    }

    /***
     *
     * @return timestamp in long representation
     */
    public long getTimestamp() {
        return timestamp;
    }

    /***
     * sets the timestamp
     * @param time
     */
    public void setTime(String time) {
        this.time = time;
        this.timestamp = Long.parseLong(time);
    }

    /***
     *
     * @return the timestamp
     */
    public String getTime() {
        return time;
    }

    /***
     * sets valid
     * @param valid
     */
    public void setValid(String valid) {
        this.valid = valid;
    }

    /***
     *
     * @return the valid parameter
     */
    public String getValid() {
        return valid;
    }

    /***
     * checks if the friend props indicates a registered user
     * @return getValid()!=null && getValid().compareTo(AroundRoidAppConstants.STATUS_UNREGISTERED)!=0
     */
    public boolean isRegistered(){
        return getValid()!=null && getValid().compareTo(AroundRoidAppConstants.STATUS_UNREGISTERED)!=0;
    }

    /***
     * checks if the user is valid
     * @return getValid().compareTo(AroundRoidAppConstants.STATUS_ONLINE)==0
     */
    public boolean isValid(){
        return getValid().compareTo(AroundRoidAppConstants.STATUS_ONLINE)==0;
    }

    @Override
    public String toString(){
        return getMail() + "::" + getLat() + "::" + getLon(); //$NON-NLS-1$ //$NON-NLS-2$
    }



}

