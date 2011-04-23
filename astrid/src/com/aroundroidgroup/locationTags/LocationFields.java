package com.aroundroidgroup.locationTags;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.Metadata;

public class LocationFields {

    /** metadata key */
    public static final String METADATA_KEY = "location";  //$NON-NLS-1$

    /** locations by kind of a location. i.e. Bank, of Pharmacy */
    public static final StringProperty locationsType = new StringProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    /** locations by kind of specific locations. A list of pair of coordinates */
    public static final StringProperty specificLocations = new StringProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    /** locations by people. A list of google users */
    public static final StringProperty peopleLocations = new StringProperty(Metadata.TABLE,
            Metadata.VALUE3.name);

    /** radius in which we notify about a location, when in walking mode */
    public static final IntegerProperty footRadius = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE4.name);

    /** radius in which we notify about a location, when in driving mode */
    public static final IntegerProperty carRadius = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE5.name);
}