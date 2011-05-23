package com.aroundroid.aroundgps;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.users.User;


import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
public class GPSProps {
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;
    
    @Persistent
    private String mail;

    @Persistent
    private User user;

    @Persistent
    private Double lon;

    @Persistent
    private Double lat;
    
    @Persistent
    private Long timeStamp;

    public GPSProps(User user,String mail, double lon, double lat,long timeStamp) {
    	this.setMail(mail);
        this.user = user;
        this.lon = lon;
        this.lat = lat;
        this.setTimeStamp(timeStamp);
    }

    public Key getKey() {
        return key;
    }

    public User getUser() {
        return user;
    }

    public double getLong() {
        return lon;
    }

    public double getLat() {
        return lat;
    }

    public void setAuthor(User user) {
        this.user = user;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getMail() {
		return mail;
	}

	public void setTimeStamp(Long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public Long getTimeStamp() {
		return timeStamp;
	}
}