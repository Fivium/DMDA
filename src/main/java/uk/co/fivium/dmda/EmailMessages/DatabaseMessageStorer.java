package uk.co.fivium.dmda.EmailMessages;

import uk.co.fivium.dmda.DatabaseConnection.DatabaseConnectionDetails;
import uk.co.fivium.dmda.DatabaseConnection.DatabaseConnectionHandler;
import uk.co.fivium.dmda.Server.Enumerations.BindParams;
import uk.co.fivium.dmda.Server.SMTPConfig;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import org.apache.log4j.Logger;
import org.subethamail.smtp.RejectException;
import org.w3c.dom.Document;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseMessageStorer implements MessageStorer{
  private DatabaseConnectionHandler mDatabaseConnectionHandler;
  private SMTPConfig mSMTPConfig;
  private Logger mLogger = Logger.getLogger(DatabaseMessageStorer.class);

  public DatabaseMessageStorer(){
    mDatabaseConnectionHandler = DatabaseConnectionHandler.getInstance();
    mSMTPConfig = SMTPConfig.getInstance();
  }

  @Override
  public void storeMessage(EmailMessage pEmailMessage) {
    for (String lRecipient : pEmailMessage.getRecipients()) {
      String lRecipientDomain = lRecipient.substring(lRecipient.indexOf('@')+1);
      String lRepository = lRecipient.substring(0, lRecipient.indexOf('@'));

      DatabaseConnectionDetails lConnectionDetails = mSMTPConfig.getConnectionDetailsForRecipient(lRecipientDomain);
      Connection lConnection = null;
      try {
        lConnection = mDatabaseConnectionHandler.getConnection(lRecipientDomain);
        // Unwrap the connection into an oracle connection so we can do oracle specific operations
        OracleConnection lOracleConnection = lConnection.unwrap(OracleConnection.class);

        String lStoreQuery = lConnectionDetails.mStoreQuery;

        OraclePreparedStatement lStatement = (OraclePreparedStatement)lOracleConnection.prepareStatement(lStoreQuery);

        // Bind values to the bind variables if they exist in the store query.
        setStringAtNameIfExists(lStoreQuery, lStatement, BindParams.MAIL_ID.getText(), pEmailMessage.getMailId());
        setStringAtNameIfExists(lStoreQuery, lStatement, BindParams.REPOSITORY.getText(), lRepository);
        setStringAtNameIfExists(lStoreQuery, lStatement, BindParams.FROM.getText(), pEmailMessage.getFrom());
        setStringAtNameIfExists(lStoreQuery, lStatement, BindParams.RECIPIENT.getText(), lRecipient);
        setStringAtNameIfExists(lStoreQuery, lStatement, BindParams.REMOTE_HOSTNAME.getText(), pEmailMessage.getRemoteHostname());
        setStringAtNameIfExists(lStoreQuery, lStatement, BindParams.REMOTE_ADDRESS.getText(), pEmailMessage.getRemoteAddress());
        setBlobAtNameIfExists(lStoreQuery, lStatement, BindParams.MESSAGE_BODY.getText(), pEmailMessage.getDataStream());
        setStringAtNameIfExists(lStoreQuery, lStatement, BindParams.SUBJECT.getText(), pEmailMessage.getSubject());



        if (lStoreQuery.contains(":" + BindParams.HEADER_XML.getText())){
          try {
            setXMLTypeAtName(lStatement, BindParams.HEADER_XML.getText(), pEmailMessage.getHeadersXML());
          }
          catch (ParserConfigurationException ex) {
            mLogger.error("Error building header XML, defaulting to null " + lConnectionDetails.toString(), ex);
            lStatement.setStringForClobAtName(BindParams.HEADER_XML.getText(), "");
          }
        }


        lStatement.execute();
        lStatement.close();
      }
      catch (SQLException ex) {
        mLogger.error("Error storing email in database " + lConnectionDetails.toString(), ex);
        throw new RejectException();
      }
      finally {
        try {
          if (lConnection!=null && !lConnection.isClosed()){
            lConnection.rollback();
            lConnection.close();
          }
        }
        catch (SQLException ex) {
          mLogger.error("Error closing database connection " + lConnectionDetails.toString(), ex);
          throw new RejectException();
        }
      }
    }
  }

  private void setStringAtNameIfExists(String pStoreQuery, OraclePreparedStatement pStatement, String pName, String pParam)
  throws SQLException {
    if (pStoreQuery.contains(":" + pName)){
      pStatement.setStringAtName(pName, pParam);
    }
  }

  private void setBlobAtNameIfExists(String pStoreQuery, OraclePreparedStatement pStatement, String pName, InputStream pParam)
  throws SQLException {
    if (pStoreQuery.contains(":" + pName)){
      pStatement.setBlobAtName(pName, pParam);
    }
  }

  private void setXMLTypeAtName(OraclePreparedStatement pStatement, String pName, Document pParam)
  throws SQLException {
    try{
      pStatement.setStringForClobAtName(pName, domToString(pParam));
    }
    catch (TransformerException ex) {
      mLogger.error("Error transforming heading xml ", ex);
      throw new RejectException();
    }
  }

  private String domToString(Document pDocument)
  throws TransformerException {
    String lXMLAsString;
    StringWriter lXMLStringWriter = new StringWriter();

    Transformer transformer = TransformerFactory.newInstance().newTransformer();

    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

    transformer.transform(new DOMSource(pDocument), new StreamResult(lXMLStringWriter));
    lXMLAsString = lXMLStringWriter.toString();

    return lXMLAsString;
  }
}
