package uk.co.fivium.dmda.server.enumerations;

public enum LoggingLevels {
    DEBUG("debug")
  , INFO("info")
  , ERROR("error");

  private final String mLevel;

  LoggingLevels(String pLevel) {
    mLevel = pLevel;
  }

  public String getText(){
    return mLevel;
  }
}
