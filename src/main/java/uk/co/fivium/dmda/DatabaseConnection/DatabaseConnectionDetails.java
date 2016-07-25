package uk.co.fivium.dmda.DatabaseConnection;

public class DatabaseConnectionDetails {
  public final String mName;
  public final String mJdbcUrl;
  public final String mUsername;
  public final String mPassword;
  public final String mStoreQuery;
  public final String mAttachmentStoreQuery;

  public DatabaseConnectionDetails(String pName, String pJdbcUrl, String pUsername, String pPassword, String pStoreQuery, String pAttachmentStoreQuery) {
    mName = pName;
    mJdbcUrl = pJdbcUrl;
    mPassword = pPassword;
    mUsername = pUsername;
    mStoreQuery = pStoreQuery;
    mAttachmentStoreQuery = pAttachmentStoreQuery;
  }

  @Override
  public String toString(){
    return "Connection Details: <Database Name: " + mName + "; JDBC URL: " + mJdbcUrl + "; Username: " + mUsername + ">";
  }
}
