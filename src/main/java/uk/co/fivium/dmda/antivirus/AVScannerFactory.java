package uk.co.fivium.dmda.antivirus;

import uk.co.fivium.dmda.server.SMTPConfig;

public class AVScannerFactory {

  private static AVScanner mAVScanner;

  private AVScannerFactory() {}

  /**
   * Encapsulates the instantiation of the AV Scanner. Will return the scanner based on the configuration. All scanners
   * constructors should be private.
   *
   * @return Returns an AV scanner
   */
  public static AVScanner getScanner() {
    if (mAVScanner == null){
      mAVScanner = getNewScanner();
    }

    return mAVScanner;
  }

  private static AVScanner getNewScanner() {
    SMTPConfig lConf = SMTPConfig.getInstance();
    String lScannerMode = lConf.getAVMode();
    if (AVModes.CLAM.getText().equalsIgnoreCase(lScannerMode)) {
      return new ClamAVScanner(lConf);
    }
    else if (AVModes.NONE.getText().equalsIgnoreCase(lScannerMode)) {
      return new NoAVScanner();
    }
    else {
      throw new RuntimeException("Error creating virus scanner instance. Configuration invalid past startup. This should be handled and there is a bug. Scanner Mode: " + lScannerMode);
    }
  }

}
