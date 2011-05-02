package com.todoroo.astrid.core;

import android.os.Parcel;
import android.os.Parcelable;

import com.todoroo.astrid.api.FilterListItem;

public class MapFilter extends FilterListItem {
    /**
     * Constructor for creating a new MapFilter
     *
     * @param listingTitle
     *            Title of this item as displayed on the lists page, e.g. Inbox
     */
    public MapFilter(String listingTitle) {
        this.listingTitle = listingTitle;
    }

    /**
     * Constructor for creating a new MapFilter
     */
    protected MapFilter() {
        //
    }



    /**
     * {@inheritDoc}
     */
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    /**
     * Parcelable creator
     */
    public static final Parcelable.Creator<MapFilter> CREATOR = new Parcelable.Creator<MapFilter>() {

        /**
         * {@inheritDoc}
         */
        public MapFilter createFromParcel(Parcel source) {
            MapFilter item = new MapFilter();
            item.readFromParcel(source);
            return item;
        }

        /**
         * {@inheritDoc}
         */
        public MapFilter[] newArray(int size) {
            return new MapFilter[size];
        }

    };

}
