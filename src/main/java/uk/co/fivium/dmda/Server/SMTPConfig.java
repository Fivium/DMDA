package uk.co.fivium.dmda.Server;

import org.apache.log4j.*;
import org.apache.log4j.varia.NullAppender;
import org.apache.log4j.xml.DOMConfigurator;
import uk.co.fivium.dmda.AntiVirus.AVModes;
import uk.co.fivium.dmda.DatabaseConnection.DatabaseConnectionDetails;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.co.fivium.dmda.Server.Enumerations.BindParams;
import uk.co.fivium.dmda.Server.Enumerations.LoggingModes;
import uk.co.fivium.dmda.Server.Enumerations.LoggingLevels;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Singleton for parsing and holding configuration data for DMDA
 */
public class SMTPConfig {
  public static final int BYTES_IN_MEGABYTE = 1000 * 1000;

  private static final SMTPConfig gSMTPConfig  = new SMTPConfig();

  private XPath mXPath = XPathFactory.newInstance().newXPath();
  private int mSmtpPort;
  private String mLoggingLevel;
  private String mLoggingMode;
  private List<DomainMatcher> mRecipientDatabaseMapping;
  private Map<String, DatabaseConnectionDetails> mDatabaseConnectionDetailsMap;
  private String mEmailRejectionMessage;
  private String mAVMode;
  private String mAVServer;
  private int mAVPort;
  private int mAVTimeoutMS;
  private int mMessageSizeLimit;

  // Make this a singleton
  private SMTPConfig(){}

  public boolean isValidRecipient(String lRecipientDomain) {
    return getDatabaseForRecipient(lRecipientDomain) != null;
  }

  public static SMTPConfig getInstance() {
    return gSMTPConfig;
  }

  /**
   * Loads the config from ./config.xml
   *
   * @throws ConfigurationException
   */
  // TODO make config reloadable (reload on receiving SIGUSR2?)
  public void loadConfig(File pFile)
  throws ConfigurationException {
    try {
      DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document lRootDoc = dBuilder.parse(pFile);
      Element lRootElement = lRootDoc.getDocumentElement();

      mSmtpPort = getUniqueChildNodeInt(lRootElement, "port");
      mLoggingLevel = getUniqueChildNodeText(lRootElement, "logging_level");
      mLoggingMode = getUniqueChildNodeText(lRootElement, "logging_mode");
      mEmailRejectionMessage = getUniqueChildNodeText(lRootElement, "email_rejection_message");
      mMessageSizeLimit = BYTES_IN_MEGABYTE * getUniqueChildNodeInt(lRootElement, "message_size_limit_mb");

      mDatabaseConnectionDetailsMap = parseConnectionDet(lRootDoc);
      mRecipientDatabaseMapping = parseRecipientDatabaseMapping(lRootDoc, mDatabaseConnectionDetailsMap.keySet());

      loadAVConfig(lRootElement);
      configureLoggingFromConfig();
    }
    catch (ConfigurationException ex) {
      throw ex;
    }
    catch (Exception ex) {
      throw new ConfigurationException("Unexpected Error during configuration", ex);
    }
  }

  private void loadAVConfig(Element pRootElement)
  throws ConfigurationException {
    Element lAVConfig = getUniqueChildElement(pRootElement, "anti_virus");
    mAVMode = getUniqueChildNodeText(lAVConfig, "mode");

    if (AVModes.CLAM.getText().equals(mAVMode)) {
      mAVPort = getUniqueChildNodeInt(lAVConfig, "port");
      mAVServer = getUniqueChildNodeText(lAVConfig, "server");
      mAVTimeoutMS = getUniqueChildNodeInt(lAVConfig, "timeout_ms");
    }
    else if (!AVModes.NONE.getText().equals(mAVMode)){
      throw new ConfigurationException("Unknown anti-virus mode " + mAVMode);
    }
  }

  private HashMap<String, DatabaseConnectionDetails> parseConnectionDet(Document pRootDoc)
  throws ConfigurationException {
    HashMap<String, DatabaseConnectionDetails> lConnectionDetailsHashMap = new HashMap<>();
    NodeList lDatabaseNodeList;
    try {
      lDatabaseNodeList = (NodeList) mXPath.evaluate("/*/database_list/database", pRootDoc.getDocumentElement(), XPathConstants.NODESET);
      for (int i = 0; i < lDatabaseNodeList.getLength(); i++){
        Node lDatabaseNode = lDatabaseNodeList.item(i);
        if (lDatabaseNode instanceof Element) {
          Element lDatabaseElement = (Element) lDatabaseNode;

          String lJdbcURL = getUniqueChildNodeText(lDatabaseElement, "jdbc_url");
          String lUsername = getUniqueChildNodeText(lDatabaseElement, "username");
          String lPassword = getUniqueChildNodeText(lDatabaseElement, "password");
          String lStoreQuery = getUniqueChildNodeText(lDatabaseElement, "store_query");
          String lAttachmentStoreQuery = getUniqueChildNodeTextIfExists(lDatabaseElement, "attachment_store_query");
          String lDatabaseName = getUniqueChildNodeText(lDatabaseElement, "name");

          // Disallow duplicate database names
          if (lConnectionDetailsHashMap.containsKey(lDatabaseName)) {
            throw new ConfigurationException("Duplicate database name: " + lDatabaseName);
          }
          try {
            validateStoreQuery(lStoreQuery);
          }
          catch (ConfigurationException ex) {
            throw new ConfigurationException("Invalid store query string in database " + lDatabaseName, ex);
          }


          DatabaseConnectionDetails lConnectionDetails = new DatabaseConnectionDetails(lDatabaseName, lJdbcURL, lUsername, lPassword, lStoreQuery, lAttachmentStoreQuery);
          lConnectionDetailsHashMap.put(lDatabaseName, lConnectionDetails);
        }
        else {
          throw new ConfigurationException("Invalid database configuration XML");
        }
      }
      return lConnectionDetailsHashMap;
    }
    catch (XPathExpressionException ex) {
      throw new ConfigurationException("XPath error loading database list", ex);
    }
  }

  private void validateStoreQuery(String pStoreQuery)
  throws ConfigurationException {
    Pattern lBindParamPattern = Pattern.compile(":(\\w+)");
    Matcher lBindParamMatcher = lBindParamPattern.matcher(pStoreQuery);
    while(lBindParamMatcher.find()){
      String lBindParam = lBindParamMatcher.group(1);
      if (!BindParams.contains(lBindParam)){
        throw new ConfigurationException("Invalid bind param " + lBindParam);
      }
    }
  }

  private List<DomainMatcher> parseRecipientDatabaseMapping(Document pRootDoc, Set<String> pDatabaseSet)
  throws ConfigurationException {
    List<DomainMatcher> lRecipientDatabaseMap = new ArrayList<>();

    try {
      NodeList lRecipientNodeList = (NodeList) mXPath.evaluate("/*/recipient_list/recipient", pRootDoc.getDocumentElement(), XPathConstants.NODESET);

      for (int i = 0; i < lRecipientNodeList.getLength(); i++){
        Node lRecipientNode = lRecipientNodeList.item(i);
        if (lRecipientNode instanceof Element) {
          Element lRecipientElement = (Element) lRecipientNode;

          String lDatabaseName = getUniqueChildNodeText(lRecipientElement, "database");

          String lDomain;
          boolean lIsRegexDomain;

          XPath lXPath = XPathFactory.newInstance().newXPath();
          Node lDomainNode = (Node) lXPath.evaluate("./domain", lRecipientElement, XPathConstants.NODE);
          Node lDomainRegexNode = (Node) lXPath.evaluate("./domain_regex", lRecipientElement, XPathConstants.NODE);

          if (lDomainNode != null && lDomainRegexNode == null){
            lDomain = lDomainNode.getTextContent();
            lIsRegexDomain = false;
          }
          else if (lDomainRegexNode != null && lDomainNode == null) {
            lDomain = lDomainRegexNode.getTextContent();
            lIsRegexDomain = true;
          }
          else {
            throw new ConfigurationException("Only one of domain or domain_regex elements may be declared inside a recipient");
          }

          if (!pDatabaseSet.contains(lDatabaseName)) {
            throw new ConfigurationException("Unknown database " + lDatabaseName);
          }

          for (DomainMatcher lDomainMatcher : lRecipientDatabaseMap){
            if (lDomain.equals(lDomainMatcher.getDatabase())){
              throw new ConfigurationException("Duplicate recipient " + lDomain);
            }
          }

          lRecipientDatabaseMap.add(new DomainMatcher(lDomain, lIsRegexDomain, lDatabaseName));
        }
        else {
          throw new ConfigurationException("Invalid recipient XML");
        }
      }

      return lRecipientDatabaseMap;
    }
    catch (XPathExpressionException ex) {
      throw new ConfigurationException("XPath error loading recipient list", ex);
    }
  }

  private void configureLoggingFromConfig()
  throws ConfigurationException {
    SMTPConfig lSMTPConfig = SMTPConfig.getInstance();
    String lLoggingMode = lSMTPConfig.getLoggingMode();

    if (LoggingModes.FILE.getText().equals(lLoggingMode)){
      FileAppender lFileAppender = new FileAppender();
      lFileAppender.setFile("logs/dmda.log");
      lFileAppender.activateOptions();
      configureLoggingWithAppender(lFileAppender);
    }
    else if (LoggingModes.CONSOLE.getText().equals(lLoggingMode)){
      ConsoleAppender lConsoleAppender = new ConsoleAppender();
      lConsoleAppender.activateOptions();
      configureLoggingWithAppender(lConsoleAppender);
    }
    else if(!LoggingModes.NONE.getText().equals(lLoggingMode)){
      if(lLoggingMode.endsWith(".xml")){
        DOMConfigurator.configure(lLoggingMode);
      }
      else if(lLoggingMode.endsWith(".properties")){
        PropertyConfigurator.configure(lLoggingMode);
      }
      else {
        throw new ConfigurationException("Logging mode " + lLoggingMode + " is not a valid logging mode");
      }
    }
  }

  private void configureLoggingWithAppender(Appender pAppender){
    SMTPConfig lSMTPConfig = SMTPConfig.getInstance();
    String lLoggingLevel = lSMTPConfig.getLoggingLevel();
    Logger lRootLogger = Logger.getRootLogger();

    Layout lLayout = new PatternLayout("%d{dd-MMM-yyyy HH:mm} %5p [%t] %m%n");
    pAppender.setLayout(lLayout);

    lRootLogger.removeAllAppenders();
    lRootLogger.addAppender(pAppender);

    if (LoggingLevels.DEBUG.getText().equals(lLoggingLevel)) {
      lRootLogger.setLevel(Level.DEBUG);
    }
    else if (LoggingLevels.ERROR.getText().equals(lLoggingLevel)) {
      lRootLogger.setLevel(Level.ERROR);
    }
    else if (LoggingLevels.INFO.getText().equals(lLoggingLevel)){
      lRootLogger.setLevel(Level.INFO);
    }
    else {
      lRootLogger.setLevel(Level.INFO);
      lRootLogger.error("Unknown logging level " + lLoggingLevel + ", defaulting to info.");
    }
  }

  public int getSmtpPort() {
    return mSmtpPort;
  }

  public String getLoggingLevel() {
    return mLoggingLevel;
  }

  public String getEmailRejectionMessage(){
    return mEmailRejectionMessage;
  }

  public int getAVPort() {
    return mAVPort;
  }

  public int getAVTimeoutMS() {
    return mAVTimeoutMS;
  }

  public String getAVServer() {
    return mAVServer;
  }

  public String getAVMode() {
    return mAVMode;
  }

  public int getMessageSizeLimit() {
    return mMessageSizeLimit;
  }

  /**
   * @return the size limit in a user readable format (i.e. 15MB)
   */
  public String getMessageSizeLimitString() {
    double sizeLimitFloat = mMessageSizeLimit;
    int sizeLimitMb = (int)Math.floor(sizeLimitFloat/BYTES_IN_MEGABYTE);
    return sizeLimitMb + "MB";
  }

  /**
   * Fetches the text content of a child node. Errors on missing child node or multiple node of the same name.
   *
   * @param pElement The element to fetch the child node from
   * @param pChildTagName The name of the element to fetch
   * @return the text content of the child node
   * @throws ConfigurationException
   */
  private String getUniqueChildNodeText(Element pElement, String pChildTagName)
  throws ConfigurationException {
    return getUniqueChildElement(pElement, pChildTagName).getTextContent();
  }

  private String getUniqueChildNodeTextIfExists(Element pElement, String pChildTagName){
    try{
      return getUniqueChildNodeText(pElement, pChildTagName);
    } catch (ConfigurationException e) {
      return null;
    }
  }

  /**
   * Fetches a child node
   *
   * @param pElement The element to fetch the child node from
   * @param pChildTagName The name of the element to fetch
   * @return the text content of the child node
   * @throws ConfigurationException
   */
  private Element getUniqueChildElement(Element pElement, String pChildTagName)
  throws ConfigurationException {
    XPath lXPath = XPathFactory.newInstance().newXPath();
    NodeList lChildNodes;
    try {
      lChildNodes = (NodeList) lXPath.evaluate("./" + pChildTagName, pElement, XPathConstants.NODESET);
    }
    catch (XPathExpressionException ex) {
      throw new ConfigurationException("Configuration XML error. XPATH error fetching " + pChildTagName, ex);
    }

    if (lChildNodes.getLength() == 0){
      throw new ConfigurationException("Configuration XML error. Missing " + pChildTagName + " element in " + pElement.getTagName());
    }
    else if (lChildNodes.getLength() > 1){
      throw new ConfigurationException("Configuration XML error. Multiple " + pChildTagName + " in " + pElement.getTagName());
    }

    return (Element) lChildNodes.item(0);
  }

  /**
   * Fetches the text content of a child node and casts it to int
   *
   * @param pElement The element to fetch the child node from
   * @param pChildTagName The name of the element to fetch
   * @return the text content of the child node
   * @throws ConfigurationException
   */
  private int getUniqueChildNodeInt(Element pElement, String pChildTagName)
  throws ConfigurationException {
    return Integer.parseInt(getUniqueChildNodeText(pElement, pChildTagName));
  }

  /**
   * Returns the database configured for the given destination domain
   *
   * @param pRecipientDomain Domain that maps to a database
   * @return the database configured for the given destination domain
   */
  public String getDatabaseForRecipient(String pRecipientDomain) {
    for (DomainMatcher lMatcher : mRecipientDatabaseMapping){
      if(lMatcher.match(pRecipientDomain)){
        return lMatcher.getDatabase();
      }
    }
    return null;
  }

  /**
   * Returns the mapping of databases to their connection details
   *
   * @return The mapping of databases to their connection details
   */
  public Map<String, DatabaseConnectionDetails> getDatabaseConnectionDetailsMapping() {
    return Collections.unmodifiableMap(mDatabaseConnectionDetailsMap);
  }

  /**
   * Returns the connection details for the given database name
   *
   * @param pDatabaseName Name of database to get connection details for
   * @return the connection details for the given database name
   */
  public DatabaseConnectionDetails getConnectionDetailsForDatabase(String pDatabaseName) {
    return mDatabaseConnectionDetailsMap.get(pDatabaseName);
  }

  /**
   * Returns a set of all configured recipients
   *
   * @return A set of all configured recipients
   */

  public String getLoggingMode() {
    return mLoggingMode;
  }
}
