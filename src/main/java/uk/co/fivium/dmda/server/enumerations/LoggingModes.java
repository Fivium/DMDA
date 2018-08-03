package uk.co.fivium.dmda.server.enumerations;

public enum LoggingModes {
    CONSOLE("console")
  , FILE("file")
  , NONE("none");

  private final String mMode;

  LoggingModes(String pMode){
    mMode = pMode;
  }

  public String getText(){
    return mMode;
  }
}
