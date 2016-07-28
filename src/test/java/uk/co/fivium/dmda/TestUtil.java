package uk.co.fivium.dmda;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

public class TestUtil {
  public static InputStream getTestResourceStream(String pName, Class pClass){
    return pClass.getResourceAsStream(pName);
  }

  public static File getTestResourceFile(String pName, Class pClass) {
    URL lFile = pClass.getResource(pName);
    return new File(lFile.getFile());
  }
}
