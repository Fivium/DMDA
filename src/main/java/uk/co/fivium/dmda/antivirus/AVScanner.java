package uk.co.fivium.dmda.antivirus;

import java.io.IOException;

public interface AVScanner {
  boolean checkContent(byte[] pData) throws IOException;

  void testConnection() throws IOException;
}
