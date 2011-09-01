package com.aroundroidgroup.astrid.googleAccounts;

/***
 * Like friend props, but with contactId (for advanced cursor use)
 * @author Tomer
 *
 */
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

    /***
     * sets the contact id
     * @param contactId
     */
    public void setContactId(long contactId) {
        this.contactId = contactId;
    }

    /***
     *
     * @return the contact id
     */
    public long getContactId() {
        return contactId;
    }
}
