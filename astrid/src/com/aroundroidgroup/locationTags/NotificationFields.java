package com.aroundroidgroup.locationTags;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.astrid.data.Metadata;

/**
 * Provides constants and Metadata entries for the location
 * based notification system
 *
 */
public class NotificationFields {

    /** Metadata key */
    public static final String METADATA_KEY = "notificationStatus"; //$NON-NLS-1$

    /** Notification status of the task */
    public static final IntegerProperty notificationStatus = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    // --- constants

    /** No notifications active for the task */
    public static final int none = 0;

    /** Only location based notifications active for the task */
    public static final int locationOnly = 1;

    /** Only non-location based notifications active for the task */
    public static final int otherOnly = 2;

    /** Both location and non-location based notifications active for the task */
    public static final int locationAndOther = 3;

}