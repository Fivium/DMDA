package uk.co.fivium.dmda.Server.Enumerations;

public enum BindParams {
    MAIL_ID("mail_id")
  , REPOSITORY("repository")
  , FROM("from_address")
  , RECIPIENT("recipient")
  , REMOTE_HOSTNAME("remote_hostname")
  , REMOTE_ADDRESS("remote_address")
  , MESSAGE_BODY("message_body")
  , SUBJECT("subject")
  , HEADER_XML("header_xml_clob")
  , SENT_DATE("sent_date");

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
