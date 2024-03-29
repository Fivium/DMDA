package uk.co.fivium.dmda.server;


import com.github.stefanbirkner.systemlambda.SystemLambda;
import org.junit.Before;
import org.junit.Test;
import uk.co.fivium.dmda.TestUtil;
import uk.co.fivium.dmda.databaseconnection.DatabaseConnectionDetails;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestSMTPConfig {

  private static final String WELL_POPULATED_CONFIG_FILENAME = "well_populated_config.xml";
  private static final String GOOD_CONFIG_FAILED_TO_PARSE_ERROR_MSG = "Good configuration failed to parse";

  private SMTPConfig mConfig;

  @Before
  public void loadTestConfig() {
    mConfig = SMTPConfig.getInstance();
  }

  @Test
  public void testLoadGoodConfig() {
    try {
      mConfig.loadConfig(TestUtil.getTestResourceFile("minimal_good_config.xml", this.getClass()));
    }
    catch (ConfigurationException e) {
      // the good config should not fail
      fail(GOOD_CONFIG_FAILED_TO_PARSE_ERROR_MSG);
    }
  }

  @Test
  public void testLoadBadConfig() {
    try {
      mConfig.loadConfig(TestUtil.getTestResourceFile("no_database_config.xml", this.getClass()));
      fail("Bad configuration successfully parsed");
    }
    catch (ConfigurationException e) {
      // this config should have failed
    }
  }

  @Test
  public void testLoadConfigValues() {
    try {
      mConfig.loadConfig(TestUtil.getTestResourceFile(WELL_POPULATED_CONFIG_FILENAME, this.getClass()));
      assertEquals(mConfig.getSmtpPort(), 2601);
      assertEquals(mConfig.getEmailRejectionMessage(), "rejected email");
      assertEquals(mConfig.getMessageSizeLimit(), 50000000);
      assertEquals(mConfig.getMessageSizeLimitString(), "50MB");
      assertEquals(mConfig.getAVMode(), "clamd");
      assertEquals(mConfig.getAVServer(), "clamd_server");
      assertEquals(mConfig.getAVTimeoutMS(), 1000);
      assertEquals(mConfig.isHealthCheckEnabled(), true);
      assertEquals(mConfig.getHealthCheckPort(), 8080);
      assertEquals(mConfig.getHealthCheckSecurityToken(), "hunter2");
    }
    catch (ConfigurationException e) {
      fail(GOOD_CONFIG_FAILED_TO_PARSE_ERROR_MSG);
    }
  }

  @Test
  public void testAvConfig_envVars() throws Exception {
    SystemLambda
      .withEnvironmentVariable("DMDA_AV_MODE", "clamd")
      .and("DMDA_AV_PORT", "3310")
      .and("DMDA_AV_SERVER", "clamd_server")
      .and("DMDA_AV_TIMEOUT_MS", "1000")
      .execute(() -> {
        mConfig.loadConfig(TestUtil.getTestResourceFile("no_av_config.xml", this.getClass()));
        assertEquals("clamd", mConfig.getAVMode());
        assertEquals("clamd_server", mConfig.getAVServer());
        assertEquals(3310, mConfig.getAVPort());
        assertEquals(1000, mConfig.getAVTimeoutMS());
      });
  }

  @Test
  public void testAvConfig_envVars_configSourcePrecedence() throws Exception {
    SystemLambda
      .withEnvironmentVariable("DMDA_AV_MODE", "clamd")
      .and("DMDA_AV_PORT", "1234")
      .and("DMDA_AV_SERVER", "env.example.com")
      .and("DMDA_AV_TIMEOUT_MS", "3000")
      .execute(() -> {
        mConfig.loadConfig(TestUtil.getTestResourceFile("well_populated_config.xml", this.getClass()));
        // AV config from env should be sourced over those in the config file
        assertEquals("clamd", mConfig.getAVMode());
        assertEquals("env.example.com", mConfig.getAVServer());
        assertEquals(1234, mConfig.getAVPort());
        assertEquals(3000, mConfig.getAVTimeoutMS());
      });
  }

  @Test
  public void testRecipientSet() {
    try {
      mConfig.loadConfig(TestUtil.getTestResourceFile(WELL_POPULATED_CONFIG_FILENAME, this.getClass()));

      for (int i = 1; i < 5; i++) {
        assertTrue(mConfig.isValidRecipient(i + "exact.domain.co.uk"));
      }
    }
    catch (ConfigurationException e) {
      fail(GOOD_CONFIG_FAILED_TO_PARSE_ERROR_MSG);
    }
  }

  @Test
  public void testDatabases() {
    try {
      mConfig.loadConfig(TestUtil.getTestResourceFile(WELL_POPULATED_CONFIG_FILENAME, this.getClass()));
      Map<String, DatabaseConnectionDetails> lMapping = mConfig.getDatabaseConnectionDetailsMapping();
      assertEquals(lMapping.size(), 2);

      /*
       * well_populated_config.xml is set up such that each field is <db_number> concatenated with the field name. For
       * example, the password for the first database is password1. These methods check that this information is being
       * read correctly for both databases.
       */
      testDatabaseDetails(1, lMapping);
      testDatabaseDetails(2, lMapping);

    }
    catch (ConfigurationException e) {
      fail(GOOD_CONFIG_FAILED_TO_PARSE_ERROR_MSG);
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
