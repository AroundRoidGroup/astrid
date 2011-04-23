package com.aroundroidgroup.locationTags;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.astrid.data.Metadata;

public class NotificationFields {

    /** metadata key */
    public static final String METADATA_KEY = "notificationStatus"; //$NON-NLS-1$

    /** notification status of the task */
    public static final IntegerProperty notificationStatus = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    // --- constants

    /** no notifications active for the task */
    public static final int none = 0;

    /** only location based notifications active for the task */
    public static final int locationOnly = 1;

    /** only non-location based notifications active for the task */
    public static final int otherOnly = 2;

    /** both location and non-location based notifications active for the task */
    public static final int locationAndOther = 3;

}