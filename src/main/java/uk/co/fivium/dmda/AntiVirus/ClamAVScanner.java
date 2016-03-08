package uk.co.fivium.dmda.AntiVirus;

import fi.solita.clamav.ClamAVClient;
import uk.co.fivium.dmda.Server.SMTPConfig;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class ClamAVScanner implements AVScanner {
  private ClamAVClient mClamAVClient;

  protected ClamAVScanner(SMTPConfig pSMTPConfig){
    mClamAVClient = new ClamAVClient(pSMTPConfig.getAVServer(), pSMTPConfig.getAVPort(), pSMTPConfig.getAVTimeoutMS());
  }

  private byte[] scan(byte[] pData)
  throws IOException {
    return mClamAVClient.scan(pData);
  }

  private boolean isClean(byte[] lReply)
  throws UnsupportedEncodingException {
    return mClamAVClient.isCleanReply(lReply);
  }

  @Override
  public boolean checkContent(byte[] pData)
  throws IOException {
    byte[] lReply = scan(pData);
    return isClean(lReply);
  }

}
