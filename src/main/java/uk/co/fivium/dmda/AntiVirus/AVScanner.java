package uk.co.fivium.dmda.AntiVirus;

import java.io.IOException;

public interface AVScanner {
  boolean checkContent(byte[] pData) throws IOException;

  void testConnection() throws IOException;
}
