package uk.co.fivium.dmda.emailmessages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import uk.co.fivium.dmda.TestUtil;
import uk.co.fivium.dmda.server.ConfigurationException;
import uk.co.fivium.dmda.server.SMTPConfig;

public class TestPriorityRecipients {
  SMTPConfig mConfig;

  @Before
  public void loadConfig() throws ConfigurationException {
    mConfig = SMTPConfig.getInstance();
    mConfig.loadConfig(TestUtil.getTestResourceFile("priority_config.xml", this.getClass()));
  }

  @Test
  public void testDuplicatePriorities(){
    List<String> lDatabaseNames = mConfig.getDatabasesForRecipient("exact.domain.co.uk");
    assertEquals(2, lDatabaseNames.size());
    assertTrue(lDatabaseNames.contains("db1"));
    assertTrue(lDatabaseNames.contains("db2"));
  }
  @Test
  public void testCorrectMatching(){
    List<String> lDatabaseNames = mConfig.getDatabasesForRecipient("not_exact.domain.co.uk");
    assertEquals(1, lDatabaseNames.size());
    assertTrue(lDatabaseNames.contains("db3"));
  }
  @Test
  public void testDefault(){
    List<String> lDatabaseNames = mConfig.getDatabasesForRecipient("not.exact.co.uk");
    assertEquals(1, lDatabaseNames.size());
    assertTrue(lDatabaseNames.contains("db4"));
  }

}
