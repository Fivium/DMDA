package uk.co.fivium.dmda.EmailMessages;

import org.junit.Before;
import org.junit.Test;
import uk.co.fivium.dmda.Server.ConfigurationException;
import uk.co.fivium.dmda.Server.SMTPConfig;
import uk.co.fivium.dmda.TestUtil;

import static org.junit.Assert.assertEquals;

public class TestWildCardRecipients {
  SMTPConfig mConfig;

  @Before
  public void loadConfig() throws ConfigurationException {
    mConfig = SMTPConfig.getInstance();
    mConfig.loadConfig(TestUtil.getTestResourceFile("wildcard_config.xml", this.getClass()));
  }

  /*
  * These three methods check that the wild card matching for recipients is working as configured in wildcard_config.xml
  */
  @Test
  public void testExactDomain(){
    String lDatabaseName = mConfig.getDatabaseForRecipient("exact.domain.co.uk");
    assertEquals("db1", lDatabaseName);
  }
  @Test
  public void testFuzzyDomainMatch(){
    String lDatabaseName = mConfig.getDatabaseForRecipient("not_exact.domain.co.uk");
    assertEquals("db2", lDatabaseName);
  }
  @Test
  public void testAnyDomainMatch(){
    String lDatabaseName = mConfig.getDatabaseForRecipient("not.exact.co.uk");
    assertEquals("db3", lDatabaseName);
  }

}
