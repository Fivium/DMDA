package uk.co.fivium.dmda.antivirus;

public enum AVModes {
    CLAM("clamd")
  , NONE("none");

  private final String mMode;
  AVModes(String pMode){
    mMode = pMode;
  }

  public String getText(){
    return  mMode;
  }
}
