package uk.co.fivium.dmda.server.enumerations;

public enum BindParams {
    MAIL_ID("mail_id")
  , USER("repository")
  , FROM("from_address")
  , RECIPIENT("recipient")
  , REMOTE_HOSTNAME("remote_hostname")
  , REMOTE_ADDRESS("remote_address")
  , MESSAGE_BODY("message_body")
  , SUBJECT("subject")
  , HEADER_XML("header_xml_clob")
  , SENT_DATE("sent_date")
  // Attachment Params
  , FILE_NAME("file_name")
  , CONTENT_TYPE("content_type")
  , CONTENT_DATA("content_data")
  , TEXT_CONTENT("text_content")
  , CONTENT_DISPOSITION("content_disposition");

  private final String mParamText;

  BindParams(String pParamText){
    mParamText = pParamText;
  }

  public String getText() {
    return mParamText;
  }

  public static boolean contains(String pParamText) {

    for (BindParams lParam : BindParams.values()) {
      if (lParam.getText().equals(pParamText)) {
        return true;
      }
    }

    return false;
  }
}
