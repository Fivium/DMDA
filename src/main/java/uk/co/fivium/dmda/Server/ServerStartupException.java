package uk.co.fivium.dmda.Server;

public class ServerStartupException extends Exception {
  public ServerStartupException(String pMessage, Exception pEx) {
    super(pMessage, pEx);
  }
}
