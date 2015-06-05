package uk.co.fivium.dmda.AntiVirus;

import uk.co.fivium.dmda.Server.SMTPConfig;

public class AVScannerFactory {
  /**
   * Encapsulates the instantiation of the AV Scanner. Will return the scanner based on the configuration. All scanners
   * constructors should be private.
   *
   * @return Returns an AV scanner
   */
  public static AVScanner getScanner() {
    SMTPConfig lConf = SMTPConfig.getInstance();
    String lScannerMode = lConf.getAVMode();
    if (AVModes.CLAM.getText().equals(lScannerMode.toLowerCase())) {
      return new ClamAVScanner(lConf);
    }
    else if (AVModes.NONE.getText().equals(lScannerMode.toLowerCase())) {
      return new NoAVScanner();
    }
    else {
      throw new RuntimeException("Error creating virus scanner instance. Configuration invalid past startup. This should be handled and there is a bug. Scnner Mode: " + lScannerMode);
    }
  }
}
