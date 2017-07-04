package uk.co.fivium.dmda.HealthChecks;

import fi.iki.elonen.NanoHTTPD;
import uk.co.fivium.dmda.Server.SMTPServerWrapper;

public class SMTPStatusHealthCheck implements HealthCheck {

  private final SMTPServerWrapper mSMTPServerWrapper;

  public SMTPStatusHealthCheck(SMTPServerWrapper pSMTPServerWrapper) {
    mSMTPServerWrapper = pSMTPServerWrapper;
  }

  /**
   * Checks the embedded SMTP server is running.
   *
   * @return A 200 OK if the SMTP server is running or a 503 SERVICE UNAVAILABLE if the server is not running.
   */
  @Override
  public NanoHTTPD.Response check() {

    NanoHTTPD.Response.Status lResponseCode;
    String lResponseContent;

    if (mSMTPServerWrapper.isRunning()) {
      lResponseCode = NanoHTTPD.Response.Status.OK;
      lResponseContent = "SMTP server running";
    }
    else {
      lResponseCode = NanoHTTPD.Response.Status.SERVICE_UNAVAILABLE;
      lResponseContent = "SMTP server not running";
    }

    return NanoHTTPD.newFixedLengthResponse(lResponseCode, NanoHTTPD.MIME_PLAINTEXT, lResponseContent);
  }

  @Override
  public boolean isSecurityTokenRequired() {
    return false;
  }

}
