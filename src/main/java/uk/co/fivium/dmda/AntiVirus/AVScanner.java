package uk.co.fivium.dmda.AntiVirus;

import java.io.IOException;

public interface AVScanner {
  public boolean checkContent(byte[] pData)
  throws IOException;
}
