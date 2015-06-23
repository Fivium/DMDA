package uk.co.fivium.dmda.EmailMessages;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import org.apache.log4j.Logger;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.RejectException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import uk.co.fivium.dmda.AntiVirus.AVScanner;
import uk.co.fivium.dmda.AntiVirus.AVScannerFactory;
import uk.co.fivium.dmda.Server.SMTPConfig;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;

public class EmailMessage {
  private AVScanner mAVScanner;
  private String mSubject;
  private String mFrom;
  private String mRemoteAddress;
  private ArrayList<String> mRecipients;
  private byte[] mData;
  private HashMap<String, String> mHeaderMap;
  private Document mHeaderXML;
  private Date mSentDate;

  private Logger mLogger = Logger.getLogger(DatabaseMessageHandler.class);
  private SMTPConfig mSMTPConfig;
  private String mMailId;
  private String mRemoteHostname;

  public EmailMessage(String pMailId) {
    mRecipients = new ArrayList<String>();
    mSMTPConfig = SMTPConfig.getInstance();
    mAVScanner = AVScannerFactory.getScanner();
    mData = new byte[0];
    mMailId = pMailId;
    mHeaderMap = new HashMap<String, String>();
  }

  /**
   * Will read and parse the email message body. The message body will be virus-scanned and rejected if it doesn't
   * come back clean.
   *
   * @param pDataStream A data stream of the email message body
   */
  public void addData(InputStream pDataStream) {
    try {
      mData = readInputStream(pDataStream);
      readMessageBody();
    }
    catch (IOException ex) {
      mLogger.error("IO Error reading message body - " + toString(), ex);
      throw new RejectException();
    }
    catch (MessageBodyTooLargeException ex) {
      mLogger.info("Message body too large - " + toString());
      throw new RejectException("The email exceeded size limit of " + mSMTPConfig.getMessageSizeLimitString());
    }

    try {
      if (!mAVScanner.checkContent(mData)){
        mLogger.error("Anti-virus detected a threat - " + toString());
        throw new RejectException();
      }
    }
    catch (IOException ex) {
      mLogger.error("IO Error scanning message body - " + toString(), ex);
      throw new RejectException();
    }
  }

  private byte[] readInputStream(InputStream pDataStream)
  throws IOException, MessageBodyTooLargeException {
    int lMaxInputSize = mSMTPConfig.getMessageSizeLimit();
    byte[] lUntrimmedData = new byte[lMaxInputSize];
    byte[] lData;

    int lNextByte;
    int lBytesRead;
    for (lBytesRead = 0; (lNextByte = pDataStream.read()) != -1; lBytesRead++){
      if (lBytesRead >= lMaxInputSize){
        throw new MessageBodyTooLargeException();
      }
      lUntrimmedData[lBytesRead] = (byte) lNextByte;
    }

    lData = new byte[lBytesRead];
    for (int i = 0; i < lBytesRead; i++){
      lData[i] = lUntrimmedData[i];
    }

    return lData;
  }

  private void readMessageBody(){
    MimeMessage lMimeMessage = null;
    try {
      lMimeMessage = new MimeMessage(
        null // The session is not required as we're not passing the email on
      , new ByteInputStream(mData, mData.length) // Can't use the data stream as it's read once
      );

      mSubject = lMimeMessage.getSubject();
      mSentDate = lMimeMessage.getSentDate();
      Enumeration<Header> lHeaderEnumeration = lMimeMessage.getAllHeaders();

      while(lHeaderEnumeration.hasMoreElements()){
        Header lCurrentHeader = lHeaderEnumeration.nextElement();
        String lName = null;
        String lValue;
        try {
          lName = lCurrentHeader.getName();
          lValue = MimeUtility.decodeText(lCurrentHeader.getValue());
          mHeaderMap.put(lName, lValue);
        }
        catch (UnsupportedEncodingException ex) {
          throw new MessagingException("Error decoding header " + lName, ex);
        }
      }
    }
    catch (MessagingException ex) {
      mLogger.error("Messaging exception reading message body - " + toString(), ex);
      throw new RejectException();
    }
  }

  private void buildHeaderXML()
  throws ParserConfigurationException {
    mHeaderXML = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Element lRootElement = mHeaderXML.createElement("HEADER_LIST");
    mHeaderXML.appendChild(lRootElement);

    for (String lHeaderName : mHeaderMap.keySet()){
      String lHeaderValue = mHeaderMap.get(lHeaderName);

      Element lHeaderElement = mHeaderXML.createElement("HEADER");
      Element lHeaderNameElement = mHeaderXML.createElement("NAME");
      Element lHeaderValueElement = mHeaderXML.createElement("VALUE");

      Text lHeaderNameText = mHeaderXML.createTextNode(lHeaderName);
      Text lHeaderValueText = mHeaderXML.createTextNode(lHeaderValue);

      lHeaderNameElement.appendChild(lHeaderNameText);
      lHeaderValueElement.appendChild(lHeaderValueText);

      lHeaderElement.appendChild(lHeaderNameElement);
      lHeaderElement.appendChild(lHeaderValueElement);

      lRootElement.appendChild(lHeaderElement);
    }
  }

  /**
   * Takes in the message context provided by subethasmtp and extract some information about the mail relay.
   *
   * @param pMessageContext
   */
  public void addContext(MessageContext pMessageContext){
    if (pMessageContext.getRemoteAddress() instanceof InetSocketAddress){
      InetSocketAddress lInetSocketAddress = (InetSocketAddress)pMessageContext.getRemoteAddress();
      mRemoteAddress = lInetSocketAddress.getAddress().getHostAddress();
      mRemoteHostname = lInetSocketAddress.getHostName();
    }
    else {
      mRemoteAddress = pMessageContext.getRemoteAddress().toString();
    }
  }

  public void setFromAddress(String pFrom){
    mFrom = pFrom;
  }

  /**
   * Adds the recipient to the email message. Will check that it is a configured recipient.
   *
   * @param pRecipient The recipient email address
   * @throws InvalidRecipientException
   */
  public void addRecipient(String pRecipient)
  throws InvalidRecipientException {
    String lRecipientDomain = pRecipient.substring(pRecipient.indexOf('@')+1);
    if(mSMTPConfig.getRecipientSet().contains(lRecipientDomain)){
      mRecipients.add(pRecipient);
    }
    else {
      throw new InvalidRecipientException();
    }
  }

  public ArrayList<String> getRecipients() {
    return mRecipients;
  }

  public String getFrom() {
    return mFrom;
  }

  public InputStream getDataStream() {
    return new ByteInputStream(mData, mData.length);
  }

  public String getSubject() {
    return mSubject;
  }

  private double getMessageBodySize2dp(){
    double lSize = ((double)mData.length)/SMTPConfig.BYTES_IN_MEGABYTE;
    BigDecimal lBigDecimal = new BigDecimal(lSize);
    lBigDecimal = lBigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP);
    return lBigDecimal.doubleValue();
  }

  /**
   * @return A summary of the email
   */
  @Override
  public String toString() {
    StringBuilder lMessageDetailStringBuilder = new StringBuilder();

    lMessageDetailStringBuilder.append("Email Details: <from: ");
    lMessageDetailStringBuilder.append(mFrom);
    lMessageDetailStringBuilder.append("(" + mRemoteAddress + ")");

    lMessageDetailStringBuilder.append("; To: ");
    lMessageDetailStringBuilder.append(mRecipients.toString());

    lMessageDetailStringBuilder.append("; Size: ");
    lMessageDetailStringBuilder.append(getMessageBodySize2dp());
    lMessageDetailStringBuilder.append("MB");

    lMessageDetailStringBuilder.append("; Subject: ");
    lMessageDetailStringBuilder.append(mSubject);

    lMessageDetailStringBuilder.append(">");

    return lMessageDetailStringBuilder.toString();
  }

  public String getMailId() {
    return mMailId;
  }

  public String getRemoteAddress() {
    return mRemoteAddress;
  }

  public String getRemoteHostname(){
    return mRemoteHostname;
  }

  /**
   * Produces the XML of the email message. XML is lazily generated on the first call to this method.
   *
   * @return An XML document containing the headers of the email message
   * @throws ParserConfigurationException
   */
  public Document getHeadersXML()
  throws ParserConfigurationException {
    if (mHeaderXML == null) {
      try {
        // Only build the header XML on request
        buildHeaderXML();
      }
      catch (ParserConfigurationException ex) {
        mLogger.error("Messaging exception reading message body - " + toString(), ex);
        throw ex;
      }
    }
    return mHeaderXML;
  }

  public Date getSentDate() {
    return mSentDate;
  }
}
