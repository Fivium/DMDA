package uk.co.fivium.dmda.DatabaseConnection;

import java.sql.SQLRecoverableException;

public class DatabaseConnectionException extends SQLRecoverableException {
  public DatabaseConnectionDetails mConnectionDetails;
  public DatabaseConnectionException(String pMessage, DatabaseConnectionDetails pConnectionDetails, Exception pEx) {
    super(pMessage, pEx);
    mConnectionDetails = pConnectionDetails;
  }

  @Override
  public String getMessage(){
    return super.getMessage() + " " + mConnectionDetails.toString();
  }
}
