package uk.co.fivium.dmda.EmailMessages;

public class EmailRecipient {
    public final String mDomain;
    public final String mUser;
    public String mEmailAdrress;

    public EmailRecipient(String pEmailAddress){
        mDomain = pEmailAddress.substring(pEmailAddress.indexOf('@') + 1).toLowerCase();
        mUser = pEmailAddress.substring(0, pEmailAddress.indexOf('@'));
        mEmailAdrress = pEmailAddress;
    }

    public String toString(){
        return mEmailAdrress;
    }
}
