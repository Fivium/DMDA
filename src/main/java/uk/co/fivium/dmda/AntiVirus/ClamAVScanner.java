package uk.co.fivium.dmda.AntiVirus;

import fi.solita.clamav.ClamAVClient;
import uk.co.fivium.dmda.Server.SMTPConfig;

import java.io.IOException;

public class ClamAVScanner implements AVScanner {
  private ClamAVClient mClamAVClient;

  protected ClamAVScanner(SMTPConfig pSMTPConfig){
    mClamAVClient = new ClamAVClient(pSMTPConfig.getAVServer(), pSMTPConfig.getAVPort(), pSMTPConfig.getAVTimeoutMS());
  }

  private byte[] scan(byte[] pData)
  throws IOException {
    return mClamAVClient.scan(pData);
  }

  @Override
  public boolean checkContent(byte[] pData)
  throws IOException {
    byte[] lReply = scan(pData);
    return ClamAVClient.isCleanReply(lReply);
  }

  @Override
  public void testConnection() throws IOException {
    if(!mClamAVClient.ping()){
      throw new IOException("Ping to CLAM server failed. Are the connection details correct?");
    }
  }

}
