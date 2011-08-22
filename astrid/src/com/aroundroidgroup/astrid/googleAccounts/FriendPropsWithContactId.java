package com.aroundroidgroup.astrid.googleAccounts;

public class FriendPropsWithContactId extends FriendProps{
    private long contactId;

    public FriendPropsWithContactId(long contactID) {
        this.setContactId(contactID);

    }

    public void setContactId(long contactId) {
        this.contactId = contactId;
    }

    public long getContactId() {
        return contactId;
    }
}
