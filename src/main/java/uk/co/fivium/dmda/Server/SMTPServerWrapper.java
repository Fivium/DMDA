package uk.co.fivium.dmda.Server;

import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.server.SMTPServer;
import uk.co.fivium.dmda.EmailMessages.DatabaseMessageHandler;
import uk.co.fivium.dmda.EmailMessages.DatabaseMessageStorer;

/*
* The purpose of this class is to wrap the SMTP library so that it can easily be replaced should it prove inadequate.
* All usages of the library should go through this class.
*/
public class SMTPServerWrapper {
  private SMTPConfig mSMTPConfig;

  public SMTPServerWrapper(){
    mSMTPConfig = SMTPConfig.getInstance();
  }

  public void start()
  throws ServerStartupException {
    final MessageHandlerFactory lMessageHandlerFactory = pMessageContext -> {
      DatabaseMessageStorer lMessageStorer = new DatabaseMessageStorer();
      return new DatabaseMessageHandler(lMessageStorer, pMessageContext);
    };
    SMTPServer lSMTPServer = new SMTPServer(lMessageHandlerFactory);
    lSMTPServer.setPort(mSMTPConfig.getSmtpPort());
    lSMTPServer.start();
  }
}
