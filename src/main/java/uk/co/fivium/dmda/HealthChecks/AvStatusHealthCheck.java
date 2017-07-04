package uk.co.fivium.dmda.HealthChecks;

import fi.iki.elonen.NanoHTTPD;
import org.apache.log4j.Logger;
import uk.co.fivium.dmda.AntiVirus.AVScannerFactory;

import java.io.IOException;

public class AvStatusHealthCheck implements HealthCheck {

  public AvStatusHealthCheck() {}

  /**
   * Checks that the application has a valid connection to a virus scanner.
   * @return A 200 OK if the AV is running or a 503 SERVICE UNAVAILABLE if not.
   */
  @Override
  public NanoHTTPD.Response check() {

    boolean lIsAvRunning = false;
    NanoHTTPD.Response.Status lResponseCode;
    String lResponseContent;

    try {
      AVScannerFactory.getScanner().testConnection();
      lIsAvRunning = true;
    }
    catch (IOException ex) {
      Logger.getRootLogger().info("Error when testing AV service.");
    }

    if (lIsAvRunning) {
      lResponseCode = NanoHTTPD.Response.Status.OK;
      lResponseContent = "AV connection OK";
    }
    else {
      lResponseCode = NanoHTTPD.Response.Status.SERVICE_UNAVAILABLE;
      lResponseContent = "Failed to connect to AV scanner";
    }

    return NanoHTTPD.newFixedLengthResponse(lResponseCode, NanoHTTPD.MIME_PLAINTEXT, lResponseContent);

  }

  @Override
  public boolean isSecurityTokenRequired() {
    return false;
  }

}
