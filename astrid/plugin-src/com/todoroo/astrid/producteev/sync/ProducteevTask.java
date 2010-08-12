package com.todoroo.astrid.producteev.sync;

import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.astrid.model.Metadata;

/**
 * Metadata entries for a Producteev Task
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ProducteevTask {

    /** metadata key */
    public static final String METADATA_KEY = "producteev"; //$NON-NLS-1$

    /** task id in producteev */
    public static final LongProperty ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    /** dashboard id */
    public static final LongProperty DASHBOARD_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    /** creator id */
    public static final LongProperty CREATOR_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE3.name);

    /** responsible id */
    public static final LongProperty RESPONSIBLE_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE4.name);

}