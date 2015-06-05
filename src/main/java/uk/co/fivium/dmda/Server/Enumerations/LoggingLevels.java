package uk.co.fivium.dmda.Server.Enumerations;

public enum LoggingLevels {
    DEBUG("debug")
  , INFO("info")
  , ERROR("error");

  private final String mLevel;

  private LoggingLevels(String pLevel) {
    mLevel = pLevel;
  }

  public String getText(){
    return mLevel;
  }
}
