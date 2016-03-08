package uk.co.fivium.dmda.Server.Enumerations;

public enum LoggingModes {
    CONSOLE("console")
  , FILE("file")
  , NONE("none");

  private final String mMode;
  private LoggingModes(String pMode){
    mMode = pMode;
  }

  public String getText(){
    return mMode;
  }
}
