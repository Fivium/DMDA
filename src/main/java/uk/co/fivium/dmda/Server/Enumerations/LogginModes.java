package uk.co.fivium.dmda.Server.Enumerations;

public enum LogginModes {
    CONSOLE("console")
  , FILE("file")
  , NONE("none");

  private final String mMode;
  private LogginModes(String pMode){
    mMode = pMode;
  }

  public String getText(){
    return mMode;
  }
}
