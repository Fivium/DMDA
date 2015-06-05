package uk.co.fivium.dmda.AntiVirus;

public enum AVModes {
    CLAM("clamd")
  , NONE("none");

  private final String mMode;
  private AVModes(String pMode){
    mMode = pMode;
  }

  public String getText(){
    return  mMode;
  }
}
