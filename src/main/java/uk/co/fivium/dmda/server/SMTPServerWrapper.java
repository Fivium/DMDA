package uk.co.fivium.dmda.server;

import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.server.SMTPServer;
import uk.co.fivium.dmda.emailmessages.DatabaseMessageHandler;
import uk.co.fivium.dmda.emailmessages.DatabaseMessageStorer;

/*
* The purpose of this class is to wrap the SMTP library so that it can easily be replaced should it prove inadequate.
* All usages of the library should go through this class.
*/
public class SMTPServerWrapper {
  private SMTPConfig mSMTPConfig;
  private SMTPServer mSMTPServer;

  public SMTPServerWrapper(){
    mSMTPConfig = SMTPConfig.getInstance();
  }

  public void start()
  throws ServerStartupException {
    final MessageHandlerFactory lMessageHandlerFactory = pMessageContext -> {
      DatabaseMessageStorer lMessageStorer = new DatabaseMessageStorer();
      return new DatabaseMessageHandler(lMessageStorer, pMessageContext);
    };
    mSMTPServer = new SMTPServer(lMessageHandlerFactory);
    mSMTPServer.setPort(mSMTPConfig.getSmtpPort());
    mSMTPServer.start();
      }


  public void stop(){
    mSMTPServer.stop();
  }

  public boolean isRunning() {
    return mSMTPServer != null && mSMTPServer.isRunning();
  }

}
