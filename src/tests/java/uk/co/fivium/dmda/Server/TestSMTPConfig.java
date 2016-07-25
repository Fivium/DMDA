package uk.co.fivium.dmda.Server;


import org.junit.*;
import uk.co.fivium.dmda.DatabaseConnection.DatabaseConnectionDetails;
import uk.co.fivium.dmda.TestUtil;

import java.io.File;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestSMTPConfig {
  private SMTPConfig mConfig;

  @Before
  public void loadTestConfig() {
    mConfig = SMTPConfig.getInstance();
  }

  @Test
  public void testLoadGoodConfig() {
    try {
      mConfig.loadConfig(TestUtil.getTestResourceFile("minimal_good_config.xml", this.getClass()));
    } catch (ConfigurationException e) {
      // the good config should not fail
      fail("Good configuration failed to parse");
    }
  }

  @Test
  public void testLoadBadConfig() {
    try {
      mConfig.loadConfig(TestUtil.getTestResourceFile("no_database_config.xml", this.getClass()));
      fail("Bad configuration successfully parsed");
    } catch (ConfigurationException e) {
      // this config should have failed
    }
  }

  @Test
  public void testLoadConfigValues() {
    try {
      mConfig.loadConfig(TestUtil.getTestResourceFile("well_populated_config.xml", this.getClass()));
      assertEquals(mConfig.getSmtpPort(), 2601);
      assertEquals(mConfig.getEmailRejectionMessage(), "rejected email");
      assertEquals(mConfig.getMessageSizeLimit(), 50000000);
      assertEquals(mConfig.getMessageSizeLimitString(), "50MB");
      assertEquals(mConfig.getAVMode(), "clamd");
      assertEquals(mConfig.getAVServer(), "clamd_server");
      assertEquals(mConfig.getAVTimeoutMS(), 1000);
    } catch (ConfigurationException e) {
      fail("Good configuration failed to parse");
    }
  }

  @Test
  public void testRecipientSet() {
    try {
      mConfig.loadConfig(TestUtil.getTestResourceFile("well_populated_config.xml", this.getClass()));

      for (int i = 1; i < 5; i++) {
        assertTrue(mConfig.isValidRecipient(i + "exact.domain.co.uk"));
      }
    } catch (ConfigurationException e) {
      fail("Good configuration failed to parse");
    }
  }

  @Test
  public void testDatabases() {
    try {
      mConfig.loadConfig(TestUtil.getTestResourceFile("well_populated_config.xml", this.getClass()));
      Map<String, DatabaseConnectionDetails> lMapping = mConfig.getDatabaseConnectionDetailsMapping();
      assertEquals(lMapping.size(), 2);

      testDatabaseDetails(1, lMapping);
      testDatabaseDetails(2, lMapping);

    } catch (ConfigurationException e) {
      fail("Good configuration failed to parse");
    }
  }

  public void testDatabaseDetails(int pDbNumber, Map<String, DatabaseConnectionDetails> pMapping) {
    assertTrue(pMapping.containsKey("db" + pDbNumber));
    DatabaseConnectionDetails lDetails = pMapping.get("db" + pDbNumber);

    assertEquals(lDetails.mJdbcUrl, "jdbc:oracle:thin:@database.local:1521:db" + pDbNumber);
    assertEquals(lDetails.mName, "db" + pDbNumber);
    assertEquals(lDetails.mPassword, "password" + pDbNumber);
    assertEquals(lDetails.mStoreQuery, "query" + pDbNumber);
    assertEquals(lDetails.mUsername, "SCHEMA" + pDbNumber);
  }
}
