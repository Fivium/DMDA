package uk.co.fivium.dmda.Server;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import uk.co.fivium.dmda.AntiVirus.AVScannerFactory;
import uk.co.fivium.dmda.DatabaseConnection.DatabaseConnectionException;
import uk.co.fivium.dmda.DatabaseConnection.DatabaseConnectionHandler;
import uk.co.fivium.dmda.HealthChecks.SMTPStatusHealthCheck;
import uk.co.fivium.dmda.HealthChecks.AvStatusHealthCheck;
import uk.co.fivium.dmda.HealthChecks.DatabaseStatusHealthCheck;
import uk.co.fivium.dmda.HealthChecks.HealthCheckService;

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
    BasicConfigurator.configure(); // Enable some sort of basic logging
    try {
      loadServerConfiguration();
      startSMTPServer();
    }
    catch (Exception ex){
      Logger.getRootLogger().error("Error during startup. Server shutting down.", ex);
    }
  }

  public void stop() {
    DatabaseConnectionHandler.getInstance().shutDown();
    mSMTPServer.stop();
    mHealthCheckService.stopHealthCheckService();
    Logger.getRootLogger().info("Shutdown signal received. Server shutting down.");
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
