package uk.co.fivium.dmda.AntiVirus;

import java.io.IOException;

public class NoAVScanner implements AVScanner {
  protected NoAVScanner(){}

  @Override
  public boolean checkContent(byte[] pData){
    return true;
  }

  @Override
  public void testConnection() throws IOException { }
}
