package uk.co.fivium.dmda.databaseconnection;

import java.sql.SQLRecoverableException;

public class DatabaseConnectionException extends SQLRecoverableException {
  private final String mConnectionDetails;

  DatabaseConnectionException(String pMessage, DatabaseConnectionDetails pConnectionDetails, Exception pEx) {
    super(pMessage, pEx);
    mConnectionDetails = pConnectionDetails.toString();
  }

  @Override
  public String getMessage(){
    return super.getMessage() + " " + mConnectionDetails;
  }
}
