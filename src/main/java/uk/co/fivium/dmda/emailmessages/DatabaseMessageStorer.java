package uk.co.fivium.dmda.emailmessages;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.RejectException;
import org.w3c.dom.Document;
import uk.co.fivium.dmda.databaseconnection.DatabaseConnectionDetails;
import uk.co.fivium.dmda.databaseconnection.DatabaseConnectionHandler;
import uk.co.fivium.dmda.server.SMTPConfig;
import uk.co.fivium.dmda.server.enumerations.BindParams;

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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

public class DatabaseMessageStorer implements MessageStorer{
  private SMTPConfig mSMTPConfig;
  private Logger mLogger = LoggerFactory.getLogger(DatabaseMessageStorer.class);

  public DatabaseMessageStorer(){
    mSMTPConfig = SMTPConfig.getInstance();
  }

  @Override
  public void storeMessage(EmailMessage pEmailMessage) {
    for (String lDatabaseName : getUniqueDatabaseDestinations(pEmailMessage)) {
      DatabaseConnectionDetails lConnectionDetails = mSMTPConfig.getConnectionDetailsForDatabase(lDatabaseName);
      Connection lConnection = null;
      OraclePreparedStatement lBodyStatement = null;
      OraclePreparedStatement lAttachmentStatement = null;
      try {
        // Unwrap the connection into an oracle connection so we can do oracle specific operations
        lConnection = DatabaseConnectionHandler.getInstance().getConnection(lConnectionDetails.mName);
        OracleConnection lOracleConnection = lConnection.unwrap(OracleConnection.class);

        String lBodyStoreQuery = lConnectionDetails.mStoreQuery;
        lBodyStatement = (OraclePreparedStatement) lOracleConnection.prepareStatement(lBodyStoreQuery);
        storeMessageBody(pEmailMessage, lDatabaseName, lBodyStatement, lBodyStoreQuery);

        // check to see if the optional attachment store query is included.
        String lAttachmentStoreQuery = lConnectionDetails.mAttachmentStoreQuery;
        if(!("".equals(lAttachmentStoreQuery) || lAttachmentStoreQuery == null)) {
          lAttachmentStatement = (OraclePreparedStatement) lOracleConnection.prepareStatement(lAttachmentStoreQuery);
          storeAttachments(pEmailMessage, lAttachmentStatement, lAttachmentStoreQuery);
        }

        lOracleConnection.commit();

      }
      catch (SQLException|ParserConfigurationException ex) {
        mLogger.error("Error storing email in database " + lConnectionDetails.toString(), ex);
        throw new RejectException();
      }
      finally {
        try {
          if (lConnection != null){
            lConnection.close();
          }
          if (lBodyStatement != null){
            lBodyStatement.close();
          }
          if (lAttachmentStatement != null){
            lAttachmentStatement.close();
          }
        }
        catch (SQLException ex) {
          // Ignore errors on closing, there's nothing we can do
        }
      }
    }
  }

  private ArrayList<String> getUniqueDatabaseDestinations(EmailMessage pEmailMessage){
    ArrayList<String> lDatabases = new ArrayList<>();
    for (EmailRecipient lRecipient : pEmailMessage.getRecipients()){
      String lDatabaseName = SMTPConfig.getInstance().getDatabaseForRecipient(lRecipient.mDomain);
      if (!lDatabases.contains(lDatabaseName)){
        lDatabases.add(lDatabaseName);
      }
    }

    return lDatabases;
  }

  private void setHeaderBindIfExists(String pStoreQuery, OraclePreparedStatement pStatement, EmailMessage pEmailMessage)
  throws SQLException, ParserConfigurationException {
    if (pStoreQuery.contains(":" + BindParams.HEADER_XML.getText())) {
      setXMLTypeAtName(pStatement, BindParams.HEADER_XML.getText(), pEmailMessage.getHeadersXML());
    }
  }

  private void storeMessageBody(EmailMessage pEmailMessage, String lDatabaseName, OraclePreparedStatement pStatement, String pStoreQuery)
  throws SQLException, ParserConfigurationException {
    for (EmailRecipient lRecipient : pEmailMessage.getRecipients()){
      String lRecipientDatabase = SMTPConfig.getInstance().getDatabaseForRecipient(lRecipient.mDomain);
      if(lDatabaseName.equals(lRecipientDatabase)){
        // Bind values to the bind variables if they exist in the store query.
        setStringAtNameIfExists(pStoreQuery, pStatement, BindParams.MAIL_ID.getText(), pEmailMessage.getMailId());
        setStringAtNameIfExists(pStoreQuery, pStatement, BindParams.USER.getText(), lRecipient.mUser);
        setStringAtNameIfExists(pStoreQuery, pStatement, BindParams.FROM.getText(), pEmailMessage.getFrom());
        setStringAtNameIfExists(pStoreQuery, pStatement, BindParams.RECIPIENT.getText(), lRecipient.mEmailAddress);
        setStringAtNameIfExists(pStoreQuery, pStatement, BindParams.REMOTE_HOSTNAME.getText(), pEmailMessage.getRemoteHostname());
        setStringAtNameIfExists(pStoreQuery, pStatement, BindParams.REMOTE_ADDRESS.getText(), pEmailMessage.getRemoteAddress());
        setBlobAtNameIfExists(pStoreQuery, pStatement, BindParams.MESSAGE_BODY.getText(), pEmailMessage.getDataStream());
        setStringAtNameIfExists(pStoreQuery, pStatement, BindParams.SUBJECT.getText(), pEmailMessage.getSubject());
        setDateAtNameIfExists(pStoreQuery, pStatement, BindParams.SENT_DATE.getText(), pEmailMessage.getSentDate());
        setHeaderBindIfExists(pStoreQuery, pStatement, pEmailMessage);

        pStatement.execute();
      }
    }
  }


  private void storeAttachments(EmailMessage pEmailMessage, OraclePreparedStatement pStatement, String pStoreQuery) throws SQLException {
    for (Attachment lAttachment : pEmailMessage.getAttachments()){
      setStringAtNameIfExists(pStoreQuery, pStatement, BindParams.MAIL_ID.getText(), pEmailMessage.getMailId());
      setStringAtNameIfExists(pStoreQuery, pStatement, BindParams.FILE_NAME.getText(), lAttachment.getFileName());
      setStringAtNameIfExists(pStoreQuery, pStatement, BindParams.CONTENT_TYPE.getText(), lAttachment.getContentType());
      setBlobAtNameIfExists(pStoreQuery, pStatement, BindParams.CONTENT_DATA.getText(), lAttachment.getDataStream());
      setStringAtNameIfExists(pStoreQuery, pStatement, BindParams.TEXT_CONTENT.getText(), lAttachment.getTextContent());
      setStringAtNameIfExists(pStoreQuery, pStatement, BindParams.CONTENT_DISPOSITION.getText(), lAttachment.getDisposition());

      pStatement.execute();
    }
  }

  private void setDateAtNameIfExists(String pStoreQuery, OraclePreparedStatement pStatement, String pName, Date pSentDate)
  throws SQLException {
    if (pStoreQuery.contains(":" + pName)){
      if (pSentDate != null){
        java.sql.Timestamp lSQLDate = new Timestamp(pSentDate.getTime());
        pStatement.setTimestampAtName(pName, lSQLDate);
      }
      else {
        pStatement.setDateAtName(pName, null);
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
