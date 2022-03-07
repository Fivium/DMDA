package uk.co.fivium.dmda.emailmessages;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.RejectException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import uk.co.fivium.dmda.antivirus.AVScanner;
import uk.co.fivium.dmda.antivirus.AVScannerFactory;
import uk.co.fivium.dmda.server.SMTPConfig;

import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmailMessage {
  private AVScanner mAVScanner;
  private String mSubject;
  private String mFrom;
  private String mRemoteAddress;
  private ArrayList<EmailRecipient> mRecipients;
  private byte[] mData;
  private ArrayList<Attachment> mAttachments;
  private HashMap<String, String> mHeaderMap;
  private Document mHeaderXML;
  private Date mSentDate;

  private Logger mLogger = LoggerFactory.getLogger(DatabaseMessageHandler.class);
  private SMTPConfig mSMTPConfig;
  private String mMailId;
  private String mRemoteHostname;

  EmailMessage(String pMailId) {
    mRecipients = new ArrayList<>();
    mAttachments = new ArrayList<>();
    mSMTPConfig = SMTPConfig.getInstance();
    mAVScanner = AVScannerFactory.getScanner();
    mData = new byte[0];
    mMailId = pMailId;
    mHeaderMap = new HashMap<>();
  }

  /**
   * Will read and parse the email message body. The message body will be virus-scanned and rejected if it doesn't
   * come back clean.
   *
   * @param pDataStream A data stream of the email message body
   */
  void addData(InputStream pDataStream) {
    try {
      mData = readInputStream(pDataStream);
      readMessageBody();
    }
    catch (IOException ex) {
      mLogger.error("IO Error reading message body - " + toString(), ex);
      throw new RejectException();
    }
    catch (MessageBodyTooLargeException ex) {
      mLogger.info("Message body too large - " + this.toString());
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
    System.arraycopy(lUntrimmedData, 0, lData, 0, lBytesRead);

    return lData;
  }

  private void readMessageBody(){
    try {
      MimeMessage lMimeMessage = new MimeMessage(
        null // The session is not required as we're not passing the email on
      , new ByteArrayInputStream(mData) // Can't use the data stream as it's read once
      );

      mSubject = lMimeMessage.getSubject();
      mSentDate = lMimeMessage.getSentDate();
      Enumeration lHeaderEnumeration = lMimeMessage.getAllHeaders();

      while(lHeaderEnumeration.hasMoreElements()){
        Header lCurrentHeader = Header.class.cast(lHeaderEnumeration.nextElement());
        mHeaderMap.put(lCurrentHeader.getName(), decodeHeaderValue(lCurrentHeader));
      }

      stripAttachments(lMimeMessage);
    }
    catch (MessagingException|IOException ex) {
      mLogger.error("Messaging exception reading message body - " + toString(), ex);
      throw new RejectException();
    }
  }

  private String decodeHeaderValue(Header pHeader) throws MessagingException {
    try {
      return MimeUtility.decodeText(pHeader.getValue());
    }
    catch (UnsupportedEncodingException ex) {
      throw new MessagingException("Error decoding header " + pHeader.getName(), ex);
    }
  }

  private void stripAttachments(MimeMessage pMimeMessage) throws IOException, MessagingException {
    Object lContent = pMimeMessage.getContent();


    // Check if the email is multi-part or not
    if (lContent instanceof String || lContent instanceof InputStream) {
      Attachment lAttachment = processMimePart(pMimeMessage);
      mAttachments.add(lAttachment);
    }
    // If it is multi-part, strip the parts out into attachments
    else if (lContent instanceof MimeMultipart){
      MimeMultipart lMimeMultipart = (MimeMultipart) lContent;

      for(int i = 0; i < lMimeMultipart.getCount(); i++){
        BodyPart lBodyPart = lMimeMultipart.getBodyPart(i);

        processMimePart(lBodyPart);

        mAttachments.add(processMimePart(lBodyPart));
      }
    }
  }

  private Attachment processMimePart(Part pPart) throws IOException, MessagingException {
    Object lContent = pPart.getContent();
    Attachment lAttachment = new Attachment();

    lAttachment.setContentType(pPart.getContentType());
    lAttachment.setFileName(pPart.getFileName());
    lAttachment.setDisposition(pPart.getDisposition());

    if (lContent instanceof String){
      String lStringContent = (String) lContent;
      lAttachment.setTextContent(lStringContent);
      lAttachment.setData(lStringContent.getBytes());
    }
    else if (lContent instanceof InputStream) {
      InputStream lDataStream = (InputStream) lContent;
      byte[] lData = IOUtils.toByteArray(lDataStream);
      lAttachment.setData(lData);
    }
    else {
      /* Message attachments could be multi part themselves or be another message entirely. For our purposes, we don't
       * need to handle these cases.
       */
      lAttachment.setData(IOUtils.toByteArray(pPart.getInputStream()));
      mLogger.warn("Email has a nested attachment which is not implemented yet" + toString());
    }

    return lAttachment;
  }

  private void buildHeaderXML()
  throws ParserConfigurationException {
    mHeaderXML = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Element lRootElement = mHeaderXML.createElement("HEADER_LIST");
    mHeaderXML.appendChild(lRootElement);

    for (Map.Entry<String, String> lHeaderEntry : mHeaderMap.entrySet()) {
      Element lHeaderElement = mHeaderXML.createElement("HEADER");
      Element lHeaderNameElement = mHeaderXML.createElement("NAME");
      Element lHeaderValueElement = mHeaderXML.createElement("VALUE");

      Text lHeaderNameText = mHeaderXML.createTextNode(lHeaderEntry.getKey());
      Text lHeaderValueText = mHeaderXML.createTextNode(lHeaderEntry.getValue());

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
   * @param pMessageContext The message context
   */
  void addContext(MessageContext pMessageContext){
    if (pMessageContext.getRemoteAddress() instanceof InetSocketAddress){
      InetSocketAddress lInetSocketAddress = (InetSocketAddress)pMessageContext.getRemoteAddress();
      mRemoteAddress = lInetSocketAddress.getAddress().getHostAddress();
      mRemoteHostname = lInetSocketAddress.getHostName();
    }
    else {
      mRemoteAddress = pMessageContext.getRemoteAddress().toString();
    }
  }

  void setFromAddress(String pFrom){
    mFrom = pFrom;
  }

  /**
   * Adds the recipient to the email message. Will check that it is a configured recipient.
   *
   * @param pRecipientEmail The recipient email address
   * @throws InvalidRecipientException if the recipient is not valid for the domain
   */
  void addRecipient(String pRecipientEmail)
  throws InvalidRecipientException {
    EmailRecipient lEmailRecipient = new EmailRecipient(pRecipientEmail);

    if(mSMTPConfig.isValidRecipient(lEmailRecipient.mDomain)){
      mRecipients.add(lEmailRecipient);
    }
    else {
      throw new InvalidRecipientException();
    }

  }

  public List<EmailRecipient> getRecipients() {
    return mRecipients;
  }

  public String getFrom() {
    return mFrom;
  }

  InputStream getDataStream() {
    return new ByteArrayInputStream(mData);
  }

  String getSubject() {
    return mSubject;
  }

  private double getMessageBodySize2dp(){
    double lSize = ((double)mData.length)/SMTPConfig.BYTES_IN_MEGABYTE;
    BigDecimal lBigDecimal = BigDecimal.valueOf(lSize);
    lBigDecimal = lBigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP);
    return lBigDecimal.doubleValue();
  }

  /**
   * @return A summary of the email
   */
  @Override
  public String toString() {
    return "Email Details: <from: " +
      mFrom +
      "(" + mRemoteAddress + ")" +
      "; To: " +
      mRecipients.toString() +
      "; Size: " +
      getMessageBodySize2dp() +
      "MB" +
      "; Subject: " +
      mSubject +
      ">";
  }

  String getMailId() {
    return mMailId;
  }

  String getRemoteAddress() {
    return mRemoteAddress;
  }

  String getRemoteHostname(){
    return mRemoteHostname;
  }

  /**
   * Produces the XML of the email message. XML is lazily generated on the first call to this method.
   *
   * @return An XML document containing the headers of the email message
   * @throws ParserConfigurationException when the message body cannot be read
   */
  Document getHeadersXML()
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

  Date getSentDate() {
    return mSentDate;
  }

  public int getSize() {
    return mData.length;
  }

  List<Attachment> getAttachments() {
    return mAttachments;
  }
}
