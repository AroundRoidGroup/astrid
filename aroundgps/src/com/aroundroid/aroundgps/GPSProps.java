package com.aroundroid.aroundgps;

import java.io.Serializable;
import java.util.Comparator;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.users.User;

/***
 * Hold GPS data and status for the datastore.
 * @author Tomer
 *
 */
@SuppressWarnings("serial")
@PersistenceCapable
public class GPSProps implements Serializable{
	//row key
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;
    
    //mail
    @Persistent
    private String mail;

    //Google user
    @Persistent
    private User user;

    //longitude
    @Persistent
    private Double lon;

    //latitude
    @Persistent
    private Double lat;
    
    //timestamp
    @Persistent
    private Long timeStamp;
    
    //true if and only if user was reminded sometime that he is about to be deleted from the database
    @Persistent
    private boolean reminded;

    private final static GPSProps noProps;
    
    static{
    	noProps = new GPSProps(null, null, 0.0, 0.0, 0);
    }
    
    /***
     * 
     * @return empty gps props representation singleton
     */
    public static GPSProps getNoPROPSGps(){
    	return noProps;
    }
    
    /***
     * checks if the gps props is an empty one
     * @param gpsP
     * @return gpsP.user==null && gpsP.mail == null
     */
    public static boolean isNoProps(GPSProps gpsP){
    	return gpsP.user==null && gpsP.mail == null;
    }

    /***
     * constuctor for gpsProps
     * @param user
     * @param mail
     * @param lon
     * @param lat
     * @param timeStamp
     */
    public GPSProps(User user,String mail, double lon, double lat,long timeStamp) {
    	this.setMail(mail);
        this.user = user;
        this.lon = lon;
        this.lat = lat;
        this.setTimeStamp(timeStamp);
        this.setReminded(false);
    }

    /***
     * 
     * @return database key
     */
    public Key getKey() {
        return key;
    }

    /***
     * 
     * @return stored user
     */
    public User getUser() {
        return user;
    }

    /***
     * 
     * @return longitude
     */
    public double getLong() {
        return lon;
    }

    /***
     * 
     * @return latitude
     */
    public double getLat() {
        return lat;
    }

    /***
     * sets user
     * @param user
     */
    public void setAuthor(User user) {
        this.user = user;
    }

    /***
     * sets longitude
     * @param lon
     */
    public void setLon(double lon) {
        this.lon = lon;
    }

    /***
     * sets latitude
     * @param lat
     */
    public void setLat(double lat) {
        this.lat = lat;
    }

    /***
     * sets email address
     * @param mail
     */
	public void setMail(String mail) {
		this.mail = mail;
	}

	/***
	 * 
	 * @return email address
	 */
	public String getMail() {
		return mail;
	}

	/***
	 * sets timestamp
	 * @param timeStamp
	 */
	public void setTimeStamp(Long timeStamp) {
		this.timeStamp = timeStamp;
	}

	/***
	 * 
	 * @return timestamp
	 */
	public Long getTimeStamp() {
		return timeStamp;
	}

	/***
	 * sets user reminded parameter
	 * @param reminded
	 */
	public void setReminded(boolean reminded) {
		this.reminded = reminded;
	}

	/***
	 * 
	 * @return reminded
	 */
	public boolean isReminded() {
		return reminded;
	}
	
	
    private final static Comparator<GPSProps> mailComparator = new Comparator<GPSProps>(){
        @Override
        public int compare(GPSProps object1, GPSProps object2) {
            return object1.getMail().compareTo(object2.getMail());
        }
    };

    /***
     * 
     * @return mail cmpartor to compare gps props by email (mail) parameter
     */
    public static Comparator<GPSProps> getMailComparator(){
        return mailComparator;
    }
}