package com.aroundroidgroup.astrid.googleAccounts;

public class FriendPropsWithContactId extends FriendProps{
    private long contactId;

    public FriendPropsWithContactId(long contactID) {
        this.setContactId(contactID);

    }

    public FriendPropsWithContactId(long contactID, FriendProps fp) {
        this(contactID);
        this.setLat(fp.getLat());
        this.setLon(fp.getLon());
        this.setMail(fp.getMail());
        this.setTime(fp.getTime());
        this.setValid(fp.getValid());


    }

    public void setContactId(long contactId) {
        this.contactId = contactId;
    }

    public long getContactId() {
        return contactId;
    }
}