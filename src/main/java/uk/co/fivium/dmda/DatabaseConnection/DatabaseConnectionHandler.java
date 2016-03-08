package uk.co.fivium.dmda.DatabaseConnection;

import uk.co.fivium.dmda.Server.SMTPConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DatabaseConnectionHandler {
  private SMTPConfig mSMTPConfig;
  private HashMap<String, HikariDataSource> mDatabaseConnectionPoolMapping;

  private static final DatabaseConnectionHandler mInstance = new DatabaseConnectionHandler();
  private static final int POOL_SIZE = 1;

  public static DatabaseConnectionHandler getInstance(){
    return mInstance;
  }

  private DatabaseConnectionHandler() {
    mSMTPConfig = SMTPConfig.getInstance();
    mDatabaseConnectionPoolMapping = new HashMap<>();
  }

  /**
   * Creates the connection pools using the database to connection details mapping in SMTPConfig
   *
   * @throws DatabaseConnectionException
   */
  public void createConnectionPools()
  throws DatabaseConnectionException {
      Map<String, DatabaseConnectionDetails> lDatabaseConnectionDetailsMapping = mSMTPConfig.getDatabaseConnectionDetailsMapping();

      for (String lDatabase : lDatabaseConnectionDetailsMapping.keySet()){
        DatabaseConnectionDetails lConnectionDetails = lDatabaseConnectionDetailsMapping.get(lDatabase);

        HikariDataSource lDataSource = new HikariDataSource();
        lDataSource.setMaximumPoolSize(POOL_SIZE);
        lDataSource.setJdbcUrl(lConnectionDetails.mJdbcUrl);
        lDataSource.setUsername(lConnectionDetails.mUsername);
        lDataSource.setPassword(lConnectionDetails.mPassword);
        lDataSource.setPoolName(lConnectionDetails.mName);

      try {
        lDataSource.getConnection().close();
        mDatabaseConnectionPoolMapping.put(lDatabase, lDataSource);
      }
      catch (Exception ex) {
        throw new DatabaseConnectionException("Exception getting database connection", lConnectionDetails, ex);
      }
    }
  }

  /**
   * Gets a database connection for the provided recipient
   *
   * @param pDestinationDomain The recipients domain name
   * @return A connection to the database for the provided recipient domain
   * @throws DatabaseConnectionException
   */
  public Connection getConnection(String pDestinationDomain)
  throws DatabaseConnectionException {
    String lDatabaseName = mSMTPConfig.getDatabaseForRecipient(pDestinationDomain);
    try {
      HikariDataSource lDataSource = mDatabaseConnectionPoolMapping.get(lDatabaseName);
      return lDataSource.getConnection();
    }
    catch (SQLException ex) {
      DatabaseConnectionDetails lDatabaseConnectionDetails = mSMTPConfig.getConnectionDetailsForDatabase(lDatabaseName);
      throw new DatabaseConnectionException("Exception getting database connection", lDatabaseConnectionDetails, ex);
    }
  }

  /**
   * Shuts down the connection pools
   */
  public void shutDown() {
    mDatabaseConnectionPoolMapping.values().forEach(HikariDataSource::shutdown);
  }
}
