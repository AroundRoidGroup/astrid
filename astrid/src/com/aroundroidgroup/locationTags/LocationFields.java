package com.aroundroidgroup.locationTags;

import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.Metadata;

/**
 * Provides Metadata entries and constants for location related data
 *
 */
public class LocationFields {

    /** Metadata key for locations by type */
    public static final String METADATA_KEY_BY_TYPE = "locationByType";  //$NON-NLS-1$

    /** Metadata key for locations by specific */
    public static final String METADATA_KEY_BY_SPECIFIC = "locationBySpecific";  //$NON-NLS-1$

    /** Metadata key for locations by people */
    public static final String METADATA_KEY_BY_PEOPLE = "locationByPeople";  //$NON-NLS-1$

    /** Metadata key for the car radius parameter */
    public static final String CAR_RADIUS_METADATA_KEY = "carRadius";  //$NON-NLS-1$

    /** Metadata key for the foot radius parameter */
    public static final String FOOT_RADIUS_METADATA_KEY = "footRadius";  //$NON-NLS-1$

    /** Metadata key for the blacklist */
    public static final String BLACKLIST_METADATA_KEY = "blacklistKey";  //$NON-NLS-1$

    /** Locations by kind of a location. i.e. Bank, of Pharmacy */
    public static final StringProperty locationsType = new StringProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    /** Locations by kind of specific locations. A list of pair of coordinates */
    public static final StringProperty specificLocations = new StringProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    /** Locations by people. A list of google users */
    public static final StringProperty peopleLocations = new StringProperty(Metadata.TABLE,
            Metadata.VALUE3.name);

    /** Blacklisted locations under a certain type */
    public static final StringProperty blacklistLocations = new StringProperty(Metadata.TABLE,
            Metadata.VALUE4.name);

    /** Radius in which we notify about a location, when in walking mode */
    public static final StringProperty footRadius = new StringProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    /** Radius in which we notify about a location, when in driving mode */
    public static final StringProperty carRadius = new StringProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    /** Delimiter for parsing the location strings */
    public static final String delimiter = "::"; //$NON-NLS-1$

    /** The speed at which the application goes from walking mode to driving mode */
    public static final int carSpeedThreshold = 25*1000/60/60; // 25 km/h

}