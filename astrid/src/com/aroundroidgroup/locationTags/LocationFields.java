package com.aroundroidgroup.locationTags;

import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.Metadata;

public class LocationFields {

    /** metadata key for locations by type */
    public static final String METADATA_KEY_BY_TYPE = "locationByType";  //$NON-NLS-1$

    /** metadata key for locations by specific */
    public static final String METADATA_KEY_BY_SPECIFIC = "locationBySpecific";  //$NON-NLS-1$

    /** metadata key for locations by people */
    public static final String METADATA_KEY_BY_PEOPLE = "locationByPeople";  //$NON-NLS-1$

    /** metadata key for the car radius parameter */
    public static final String CAR_RADIUS_METADATA_KEY = "carRadius";  //$NON-NLS-1$

    /** metadata key for the foot radius parameter */
    public static final String FOOT_RADIUS_METADATA_KEY = "footRadius";  //$NON-NLS-1$

    /** metadata key for the moti's thing *///TODO: be more persice
    public static final String MOTI_METADATA_KEY = "motiKey";  //$NON-NLS-1$

    /** locations by kind of a location. i.e. Bank, of Pharmacy */
    public static final StringProperty locationsType = new StringProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    /** locations by kind of specific locations. A list of pair of coordinates */
    public static final StringProperty specificLocations = new StringProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    /** locations by people. A list of google users */
    public static final StringProperty peopleLocations = new StringProperty(Metadata.TABLE,
            Metadata.VALUE3.name);

    /** moti something *///TODO: be more specific
    public static final StringProperty motiLocations = new StringProperty(Metadata.TABLE,
            Metadata.VALUE4.name);

    /** radius in which we notify about a location, when in walking mode */
    public static final StringProperty footRadius = new StringProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    /** radius in which we notify about a location, when in driving mode */
    public static final StringProperty carRadius = new StringProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    /** delimiter for parsing the location strings */
    public static final String delimiter = "::"; //$NON-NLS-1$
}