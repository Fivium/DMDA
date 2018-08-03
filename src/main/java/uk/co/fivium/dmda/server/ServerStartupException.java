package uk.co.fivium.dmda.server;

public class ServerStartupException extends Exception {
  public ServerStartupException(String pMessage, Exception pEx) {
    super(pMessage, pEx);
  }
}
