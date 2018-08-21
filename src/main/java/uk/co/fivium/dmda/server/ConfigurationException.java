package uk.co.fivium.dmda.server;

public class ConfigurationException extends Exception {
  ConfigurationException(String pReason, Throwable pThrowable) {
    super(pReason, pThrowable);
  }

  ConfigurationException(String pReason) {
    super(pReason);
  }
}
