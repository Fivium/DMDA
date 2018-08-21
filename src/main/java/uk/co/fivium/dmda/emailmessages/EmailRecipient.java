package uk.co.fivium.dmda.emailmessages;

import org.subethamail.smtp.RejectException;

public class EmailRecipient {
  public final String mDomain;
  public final String mUser;
  public final String mEmailAddress;

  public EmailRecipient(String pEmailAddress)
  throws RejectException {
    if (pEmailAddress.contains("@")) {
      String lEmailAddress = pEmailAddress.toLowerCase();
      mDomain = pEmailAddress.substring(lEmailAddress.indexOf('@') + 1);
      mUser = pEmailAddress.substring(0, pEmailAddress.indexOf('@'));
      mEmailAddress = pEmailAddress;
    }
    else {
      throw new RejectException("Invalid email address '" + pEmailAddress + "'");
    }
  }

  public String toString() {
    return mEmailAddress;
  }
}
