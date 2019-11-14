package uk.co.fivium.dmda.emailmessages;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import uk.co.fivium.dmda.server.ConfigurationException;
import uk.co.fivium.dmda.server.SMTPConfig;
import uk.co.fivium.dmda.TestUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    List<String> lDatabaseNames = mConfig.getDatabasesForRecipient("exact.domain.co.uk");
    assertEquals(3, lDatabaseNames.size());
    assertTrue(lDatabaseNames.contains("db1"));
    assertTrue(lDatabaseNames.contains("db2"));
    assertTrue(lDatabaseNames.contains("db3"));
  }
  @Test
  public void testFuzzyDomainMatch(){
    List<String> lDatabaseNames = mConfig.getDatabasesForRecipient("not_exact.domain.co.uk");
    assertEquals(2, lDatabaseNames.size());
    assertTrue(lDatabaseNames.contains("db2"));
    assertTrue(lDatabaseNames.contains("db3"));
  }
  @Test
  public void testAnyDomainMatch(){
    List<String> lDatabaseNames = mConfig.getDatabasesForRecipient("not.exact.co.uk");
    assertEquals(1, lDatabaseNames.size());
    assertTrue(lDatabaseNames.contains("db3"));
  }

}
