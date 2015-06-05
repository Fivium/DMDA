package uk.co.fivium.dmda.AntiVirus;

public class NoAVScanner implements AVScanner {
  protected NoAVScanner(){};

  @Override
  public boolean checkContent(byte[] pData){
    return true;
  }
}
