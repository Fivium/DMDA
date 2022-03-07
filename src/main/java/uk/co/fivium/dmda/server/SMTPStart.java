package uk.co.fivium.dmda.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.fivium.dmda.antivirus.AVScannerFactory;
import uk.co.fivium.dmda.databaseconnection.DatabaseConnectionException;
import uk.co.fivium.dmda.databaseconnection.DatabaseConnectionHandler;
import uk.co.fivium.dmda.healthchecks.AvStatusHealthCheck;
import uk.co.fivium.dmda.healthchecks.DatabaseStatusHealthCheck;
import uk.co.fivium.dmda.healthchecks.HealthCheckService;
import uk.co.fivium.dmda.healthchecks.SMTPStatusHealthCheck;

import java.io.File;
import java.io.IOException;


public class SMTPStart {
  private SMTPServerWrapper mSMTPServer;
  private HealthCheckService mHealthCheckService;

  public static void main(String[] args) {
    SMTPStart lSMTPStart = new SMTPStart();
    Thread lShutdownHook = new Thread(lSMTPStart::stop);
    lShutdownHook.setName("Shutdown Hook");
    Runtime.getRuntime().addShutdownHook(lShutdownHook);
    lSMTPStart.start();
  }

  public void start(){
    try {
      loadServerConfiguration();
      startSMTPServer();
    }
    catch (Exception ex){
      LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).error("Error during startup. Server shutting down.", ex);
    }
  }

  public void stop() {
    DatabaseConnectionHandler.getInstance().shutDown();
    mSMTPServer.stop();
    mHealthCheckService.stopHealthCheckService();
    LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).info("Shutdown signal received. Server shutting down.");
  }

  private void startSMTPServer()
  throws ServerStartupException {
    try {
      DatabaseConnectionHandler.getInstance().createConnectionPools();
    }
    catch (DatabaseConnectionException ex) {
      throw new ServerStartupException("Failed to create database connection pools", ex);
    }


    try{
      AVScannerFactory.getScanner().testConnection();
    }
    catch (IOException ex) {
      throw new ServerStartupException("Failed to connect to anti-virus scanner", ex);
    }

    mSMTPServer = new SMTPServerWrapper();

    if (SMTPConfig.getInstance().isHealthCheckEnabled()) {
      mHealthCheckService = new HealthCheckService(SMTPConfig.getInstance().getHealthCheckPort());
      registerHealthChecks();

      mHealthCheckService.startHealthCheckService();
    }

    mSMTPServer.start();
  }

  private void loadServerConfiguration()
  throws ServerStartupException {
    try {
      SMTPConfig.getInstance().loadConfig(new File("config.xml"));
    }
    catch (ConfigurationException ex) {
      throw new ServerStartupException("Failed to load server configuration", ex);
    }
  }

  private void registerHealthChecks() {
    mHealthCheckService.registerHealthCheck("/smtp-status", new SMTPStatusHealthCheck(mSMTPServer));
    mHealthCheckService.registerHealthCheck("/db-status", new DatabaseStatusHealthCheck());
    mHealthCheckService.registerHealthCheck("/av-status", new AvStatusHealthCheck());
  }

}
