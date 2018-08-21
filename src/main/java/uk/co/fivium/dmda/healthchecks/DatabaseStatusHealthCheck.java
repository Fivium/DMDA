package uk.co.fivium.dmda.healthchecks;

import fi.iki.elonen.NanoHTTPD;
import uk.co.fivium.dmda.databaseconnection.DatabaseConnectionHandler;

import java.util.Map;

public class DatabaseStatusHealthCheck implements HealthCheck {

  public DatabaseStatusHealthCheck() {}

  /**
   * Tests that a connection can be established to all the registered databases.
   *
   * @return A list of databases and a boolean indicating if the application can connect to this database.
   */
  @Override
  public NanoHTTPD.Response check() {
    Map<String, Boolean> lConnectionTestResults = DatabaseConnectionHandler.getInstance().testConnections();
    return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, lConnectionTestResults.toString());
  }

  @Override
  public boolean isSecurityTokenRequired() {
    return true;
  }
}
