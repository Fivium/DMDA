package uk.co.fivium.dmda.emailmessages;

import org.apache.log4j.lf5.util.StreamUtils;
import org.junit.Before;
import org.junit.Test;
import org.subethamail.smtp.RejectException;
import uk.co.fivium.dmda.server.ConfigurationException;
import uk.co.fivium.dmda.server.SMTPConfig;
import uk.co.fivium.dmda.TestUtil;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class TestEmailMessage {

  private static final String MAIL_ID = "mailid";

  List<Attachment> mAttachments;

  @Before
  public void loadEMLs() throws ConfigurationException {
    SMTPConfig.getInstance().loadConfig(TestUtil.getTestResourceFile("../config.xml", this.getClass()));
    EmailMessage lMessage = new EmailMessage(MAIL_ID);
    lMessage.addData(TestUtil.getTestResourceStream("attachments.eml", this.getClass()));

    mAttachments = lMessage.getAttachments();
  }

  @Test
  public void testAddingValidRecipient() {
    try {
      String lRecipient = "user1@exact.domain.co.uk";
      EmailMessage lMessage = new EmailMessage(MAIL_ID);
      lMessage.addRecipient(lRecipient);
      List<EmailRecipient> lRecipients = lMessage.getRecipients();
      assertEquals(1, lRecipients.size());
      assertEquals(lRecipient, lRecipients.get(0).mEmailAddress);
    }
    catch (InvalidRecipientException ex) {
      fail("Adding a valid recipient failed");
    }
  }

  @Test
  public void testInvalidEmailAddress() {
    try {
      String lRecipient = "exact.domain.co.uk";
      EmailMessage lEmailMessage = new EmailMessage(MAIL_ID);
      lEmailMessage.addRecipient(lRecipient);
    }
    catch (RejectException ex) {
      return;
    }
    catch (Exception ex) {
      fail("Unexpected exception" + ex.getMessage());
    }
    fail("Invalid email address parsed successfully when it should have failed");
  }

  @Test
  public void testAddingInvalidRecipient() {
    try {
      EmailMessage lMessage = new EmailMessage(MAIL_ID);
      lMessage.addRecipient("user2@invalid.local");
      fail("Adding an invalid recipient succeeded");
    }
    catch (InvalidRecipientException ex) {
      // Failed as required
    }
  }

  @Test
  public void testPlainEmail() throws FileNotFoundException, ParseException {
    EmailMessage lMessage = new EmailMessage(MAIL_ID);
    lMessage.addData(TestUtil.getTestResourceStream("plain.eml", this.getClass()));
    Date lExpectedSentDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2016-07-21 14:30:52");

    assertEquals("Plain Email", lMessage.getSubject());
    assertEquals(lExpectedSentDate, lMessage.getSentDate());
    assertEquals(null, lMessage.getFrom()); // from address should not be scraped from headers
    assertEquals(541, lMessage.getSize());
  }

  @Test
  public void testAttachmentEmail() throws FileNotFoundException, ParseException {
    EmailMessage lMessage = new EmailMessage(MAIL_ID);
    lMessage.addData(TestUtil.getTestResourceStream("attachments.eml", this.getClass()));
    Date lExpectedSentDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2016-07-22 15:23:46");

    assertEquals("Subject", lMessage.getSubject());
    assertEquals(lExpectedSentDate, lMessage.getSentDate());
    assertEquals(null, lMessage.getFrom()); // from address should not be scraped from headers
    assertEquals(10125, lMessage.getSize());
    assertEquals(lMessage.getAttachments().size(), 3);
  }

  @Test
  public void testAttachedBodyStripping() throws ParseException, IOException {
    Attachment lBody = mAttachments.get(0);  // The first attachment should be the body

    assertEquals(lBody.getTextContent(), "Hello Mr/Mrs From,\r\n\r\nMessage body goes here\r\n\r\n\r\nThanks,\r\n\r\nMr User\r\n");
    assertEquals(lBody.getTextContent(), new String(StreamUtils.getBytes(lBody.getDataStream())));
    assertTrue(lBody.getContentType().startsWith("text/plain"));
    assertNull(lBody.getDisposition());

  }

  @Test
  public void testAttachedImageStripping(){
    Attachment lImage = mAttachments.get(1); // The second attachment in the eml is an image
    assertNotNull(lImage.getDataStream());
    assertEquals("attachment", lImage.getDisposition());
    assertEquals("image/png;\r\n name=\"test.png\"", lImage.getContentType());
    assertEquals("test.png", lImage.getFileName());

  }

  @Test
  public void testAttachedTextDocumentStripping(){
    Attachment lTextFile = mAttachments.get(2); // The third is a test.txt file
    assertEquals("attachment", lTextFile.getDisposition());
    assertEquals("text/plain; charset=UTF-8;\r\n name=\"test.txt\"", lTextFile.getContentType());
    assertEquals("test.txt", lTextFile.getFileName());
    assertEquals("the attachment is a circle", lTextFile.getTextContent());
  }

  @Test
  public void testUnicodeSubjectMessage(){
    EmailMessage lMessage = new EmailMessage(MAIL_ID);
    lMessage.addData(TestUtil.getTestResourceStream("unicode_subject_message.eml", this.getClass()));
    assertEquals("℀ ℁ ℂ ℃ ℄ ℅ ℆ ℇ ℈ ℉ ℊ ℋ ℌ ℍ ℎ ℏ ℐ ℑ ℒ ℓ ℔ ℕ № ℗ ℘ ℙ ℚ ℛ ℜ ℝ ℞ ℟ ℠ ℡ ™ ℣ ℤ ℥ Ω ℧ ℨ ℩ K Å ℬ ℭ ℮ ℯ ℰ ℱ Ⅎ ℳ ℴ ℵ ℶ ℷ ℸ", lMessage.getSubject());
  }
}
