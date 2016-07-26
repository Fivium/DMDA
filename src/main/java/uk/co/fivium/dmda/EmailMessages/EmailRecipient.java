package uk.co.fivium.dmda.EmailMessages;

public class EmailRecipient {
    public final String mDomain;
    public final String mUser;
    public String mEmailAddress;

    public EmailRecipient(String pEmailAddress){
        String lEmailAddress = pEmailAddress.toLowerCase();
        mDomain = pEmailAddress.substring(lEmailAddress.indexOf('@') + 1);
        mUser = pEmailAddress.substring(0, pEmailAddress.indexOf('@'));
        mEmailAddress = pEmailAddress;
    }

    public String toString(){
        return mEmailAddress;
    }
}
