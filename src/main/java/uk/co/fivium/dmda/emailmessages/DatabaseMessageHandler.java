package uk.co.fivium.dmda.emailmessages;

import uk.co.fivium.dmda.server.SMTPConfig;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.RejectException;

import java.io.InputStream;
import java.util.UUID;

public class DatabaseMessageHandler
implements MessageHandler {
  private final UUID mUUID;
  private EmailMessage mEmailMessage;

  private DatabaseMessageStorer mMessageStorer;
  private Logger mLogger;


  public DatabaseMessageHandler(DatabaseMessageStorer pStorer, MessageContext pMessageContext) {
    mUUID = UUID.randomUUID();
    mMessageStorer = pStorer;
    mLogger = LogManager.getLogger(DatabaseMessageHandler.class);
    Thread.currentThread().setName("MessageHandler." + mUUID.toString());
    mEmailMessage = new EmailMessage(mUUID.toString());

    mEmailMessage.addContext(pMessageContext);
  }

  /**
   * Called once the SMTP server has been told who the email is from
   *
   * @param pFrom The senders email address
   */
  @Override
  public void from(String pFrom) {
    mEmailMessage.setFromAddress(pFrom);
  }

  /**
   * Called once for each recipient of the email. This method will check that there is configuration for the recipient
   * and will reject the email if there is not.
   *
   * @param pRecipient The recipients email address
   * @throws RejectException when the recipient cannot be found at this domain
   */
  @Override
  public void recipient(String pRecipient)
  throws RejectException {
    try {
      mEmailMessage.addRecipient(pRecipient.toLowerCase());
    }
    catch (InvalidRecipientException e) {
      mLogger.error("Email recipient has no configured database: " + pRecipient + ". " + mEmailMessage.toString());
      rejectEmail("The recipient " + pRecipient + " cannot be found at this domain.");
    }
  }


  /**
   * Called when the sender starts streaming the body of the email. This method will call the message storer and store
   * the message in the database.
   *
   * @param pDataStream The data stream in which the body is send down
   * @throws RejectException when the message cannot be read or stored correctly
   */
  @Override
  public void data(InputStream pDataStream)
  throws RejectException {
    try{
      mLogger.debug("Message Received - " + mEmailMessage.toString());
      mEmailMessage.addData(pDataStream);
      mMessageStorer.storeMessage(mEmailMessage);
      mLogger.info("Message Successfully Stored - " + mEmailMessage.toString());
    }
    catch (RejectException ex) {
      rejectEmail(ex.getMessage());
    }
  }

  /**
   * This method is called once the SMTP negotiation has ended. There is nothing that needs doing at this point.
   */
  @Override
  public void done() {
    // Nothing to do here
  }

  private void rejectEmail(String pMessage){
    mLogger.error("Message Rejected - " + mEmailMessage.toString());
    StringBuilder lErrorMessageBuilder = new StringBuilder();

    lErrorMessageBuilder.append(SMTPConfig.getInstance().getEmailRejectionMessage());

    if (pMessage != null) {
      lErrorMessageBuilder.append(" The server gave the following reason: ");
      lErrorMessageBuilder.append(pMessage);
    }

    lErrorMessageBuilder.append(" If you contact support, please provide them with this email reference: ");
    lErrorMessageBuilder.append(mUUID.toString());

    throw new RejectException(lErrorMessageBuilder.toString());
  }
}
