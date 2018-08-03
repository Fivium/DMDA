package uk.co.fivium.dmda.healthchecks;

import fi.iki.elonen.NanoHTTPD;
import org.apache.log4j.Logger;
import uk.co.fivium.dmda.server.SMTPConfig;
import uk.co.fivium.dmda.server.ServerStartupException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HealthCheckService extends NanoHTTPD {

  private Map<String, HealthCheck> mHealthChecks = new HashMap<>();

  /**
   * Create a new health check handler listening on the given port.
   *
   * @param pPort The port to listen on
   */
  public HealthCheckService(int pPort) {
    super(pPort);
  }

  /**
   * Adds a health check to the registry, specifying the URI which it should respond to.
   *
   * If a health check is already registered for the given URI it will be overwritten.
   *
   * @param pUri The URI which this health check will respond to.
   * @param pHealthCheck The health check implementation invoked when the URI is requested.
   */
  public void registerHealthCheck(String pUri, HealthCheck pHealthCheck) {
    mHealthChecks.put(pUri, pHealthCheck);
  }

  /**
   * Handle a http request. Try to route it to a registered health check.
   * @param pSession The session object
   * @return A fixed length text response, containing the result of a health check, or an empty error response.
   */
  @Override
  public Response serve(IHTTPSession pSession) {
    try {
      String requestUri = pSession.getUri();

      HealthCheck lHealthCheck = mHealthChecks.get(requestUri);
      if (lHealthCheck != null) {
        // Check if the security token is required to access this endpoint.
        if (lHealthCheck.isSecurityTokenRequired()) {
          // Token check is required. Return a 401 if the token is not valid.
          if (isSecurityTokenValid(pSession)) {
            return lHealthCheck.check();
          }
          else {
            return newFixedLengthResponse(Response.Status.UNAUTHORIZED, NanoHTTPD.MIME_PLAINTEXT, "");
          }
        }
        // No token check required.
        else {
          return lHealthCheck.check();
        }
      }
      else {
        Logger.getRootLogger().warn("No health check registered for URI: " + pSession.getUri());
        return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "");
      }
    }
    catch (Exception ex) {
      Logger.getRootLogger().warn("Error during health check request for URI: " + pSession.getUri(), ex);
      return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "");
    }
  }

  /**
   * Checks if the token provided in the request matches the one set in the config.
   * If no security token is set in the config or one is not provided in the request, this will return false.
   * @param pSession The http request object
   * @return True if the provided security token matches the one set in the config. False otherwise.
   */
  private static boolean isSecurityTokenValid(IHTTPSession pSession) {

    String lSecurityToken = SMTPConfig.getInstance().getHealthCheckSecurityToken();
    if (lSecurityToken == null) {
      return false;
    }

    List<String> lParam = pSession.getParameters().get("security-token");
    if (lParam != null && lParam.size() == 1) {
      String lProvidedToken = lParam.get(0);
      return lSecurityToken.equals(lProvidedToken);
    }
    else {
      return false;
    }
  }

  public void startHealthCheckService() throws ServerStartupException {
    try {
      super.start();
      Logger.getRootLogger().info("Health check service started, listening on port: " + super.getListeningPort());
    }
    catch (IOException ex) {
      throw new ServerStartupException("Failed to start health check service.", ex);
    }
  }

  public void stopHealthCheckService() {
    Logger.getRootLogger().info("Stopping health check service...");
    super.stop();
    Logger.getRootLogger().info("Health check service stopped.");
  }

}
