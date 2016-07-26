package uk.co.fivium.dmda.AntiVirus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.co.fivium.dmda.Server.ConfigurationException;
import uk.co.fivium.dmda.Server.SMTPConfig;
import uk.co.fivium.dmda.TestUtil;

import java.lang.reflect.Field;

import static org.junit.Assert.fail;

public class TestAVScanner {
  private SMTPConfig mSMTPConfig;

  @Before
  public void getConfig() {
    mSMTPConfig = SMTPConfig.getInstance();
  }

  @Test
  public void testScannerFactoryNoAv() throws ConfigurationException, NoSuchFieldException, IllegalAccessException {
    resetAVScannerFactory();

    // load in the config that has no anti-virus configured
    mSMTPConfig.loadConfig(TestUtil.getTestResourceFile("../config.xml", this.getClass()));
    AVScanner lScanner = AVScannerFactory.getScanner();

    if (!(lScanner instanceof NoAVScanner)) {
      fail("expected no av scanner");
    }
  }

  @Test
  public void testScannerFactoryClamd() throws ConfigurationException, NoSuchFieldException, IllegalAccessException {
    resetAVScannerFactory();

    // load in the config that has the clamd anti-virus configured
    mSMTPConfig.loadConfig(TestUtil.getTestResourceFile("av_config.xml", this.getClass()));
    AVScanner lScanner = AVScannerFactory.getScanner();

    if (!(lScanner instanceof ClamAVScanner)) {
      fail("expected clamd scanner");
    }
  }

  public void resetAVScannerFactory() throws NoSuchFieldException, IllegalAccessException {
    Field lField = AVScannerFactory.class.getDeclaredField("mAVScanner");
    lField.setAccessible(true);
    lField.set(null, null); // set the static field to null
  }
}
