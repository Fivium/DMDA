package uk.co.fivium.dmda.AntiVirus;

import org.junit.Before;
import org.junit.Test;
import uk.co.fivium.dmda.Server.ConfigurationException;
import uk.co.fivium.dmda.Server.SMTPConfig;
import uk.co.fivium.dmda.TestUtil;

import static org.junit.Assert.fail;

public class TestAVScanner {
  private SMTPConfig mSMTPConfig;

  @Before
  public void getConfig() {
    mSMTPConfig = SMTPConfig.getInstance();
  }

  @Test
  public void testScannerFactory() throws ConfigurationException {
    mSMTPConfig.loadConfig(TestUtil.getTestResourceFile("../config.xml", this.getClass()));
    AVScanner lScanner = AVScannerFactory.getScanner();

    if (!(lScanner instanceof NoAVScanner)) {
      fail("expected no av scanner");
    }

    mSMTPConfig.loadConfig(TestUtil.getTestResourceFile("av_config.xml", this.getClass()));
    lScanner = AVScannerFactory.getScanner();

    if (!(lScanner instanceof ClamAVScanner)) {
      fail("expected clamd scanner");
    }
  }
}
