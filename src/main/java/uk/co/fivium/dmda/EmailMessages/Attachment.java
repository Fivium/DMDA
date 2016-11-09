package uk.co.fivium.dmda.EmailMessages;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class Attachment {
  private byte[] mData;
  private String mContentType;
  private String mFileName;
  private String mTextContent;
  private String mDisposition;

  public void setContentType(String pContentType) {
    this.mContentType = pContentType;
  }

  public void setFileName(String pFileName) {
    this.mFileName = pFileName;
  }

  public void setTextContent(String pTextContent) {
    this.mTextContent = pTextContent;
  }

  public void setData(byte[] pData) {
    this.mData = pData;
  }

  public void setDisposition(String mDisposition) {
    this.mDisposition = mDisposition;
  }

  public String getFileName() {
    return mFileName;
  }

  public String getContentType() {
    return mContentType;
  }

  public InputStream getDataStream() {
    return new ByteArrayInputStream(mData);
  }

  public String getTextContent() {
    return mTextContent;
  }

  public String getDisposition() {
    return mDisposition;
  }
}