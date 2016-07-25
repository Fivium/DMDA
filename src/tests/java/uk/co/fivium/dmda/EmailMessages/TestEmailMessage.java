package uk.co.fivium.dmda.EmailMessages;

import org.apache.log4j.lf5.util.StreamUtils;
import org.junit.Before;
import org.junit.Test;
import uk.co.fivium.dmda.Server.ConfigurationException;
import uk.co.fivium.dmda.Server.SMTPConfig;
import uk.co.fivium.dmda.TestUtil;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.Assert.*;

public class TestEmailMessage {
  ArrayList<Attachment> mAttachments;

  @Before
  public void loadEMLs(){
    EmailMessage lMessage = new EmailMessage("mailid");
    lMessage.addData(TestUtil.getTestResourceStream("attachments.eml", this.getClass()));

    mAttachments = lMessage.getAttachments();
  }

  @Before
  public void loadConfig() throws ConfigurationException {
    SMTPConfig.getInstance().loadConfig(TestUtil.getTestResourceFile("../config.xml", TestEmailMessage.class));
  }

  @Test
  public void testAddingValidRecipient() {
    try {
      String lRecipient = "user1@exact.domain.co.uk";
      EmailMessage lMessage = new EmailMessage("mailid");
      lMessage.addRecipient(lRecipient);
      ArrayList<EmailRecipient> lRecipients = lMessage.getRecipients();
      assertEquals(1, lRecipients.size());
      assertEquals(lRecipient, lRecipients.get(0).mEmailAdrress);
    } catch (InvalidRecipientException ex) {
      fail("Adding a valid recipient failed");
    }
  }

  @Test
  public void testAddingInvalidRecipient() {
    try {
      EmailMessage lMessage = new EmailMessage("mailid");
      lMessage.addRecipient("user2@invalid.local");
      fail("Adding an invalid recipient succeeded");
    } catch (InvalidRecipientException ex) {
      // Failed as required
    }
  }

  @Test
  public void testPlainEmail() throws FileNotFoundException, ParseException {
    EmailMessage lMessage = new EmailMessage("mailid");
    lMessage.addData(TestUtil.getTestResourceStream("plain.eml", this.getClass()));
    Date lExpectedSentDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2016-07-21 14:30:52");

    assertEquals("Plain Email", lMessage.getSubject());
    assertEquals(lExpectedSentDate, lMessage.getSentDate());
    assertEquals(null, lMessage.getFrom()); // from address should not be scraped from headers
    assertEquals(541, lMessage.getSize());
  }

  @Test
  public void testAttachmentEmail() throws FileNotFoundException, ParseException {
    EmailMessage lMessage = new EmailMessage("mailid");
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
    Attachment lBody = mAttachments.get(0);

    assertEquals(lBody.getTextContent(), "Hello Mr/Mrs From,\r\n\r\nMessage body goes here\r\n\r\n\r\nThanks,\r\n\r\nMr User\r\n");
    assertEquals(lBody.getTextContent(), new String(StreamUtils.getBytes(lBody.getDataStream())));
    assertTrue(lBody.getContentType().startsWith("text/plain"));
    assertNull(lBody.getDisposition());

  }

  @Test
  public void testAttachedImageStripping(){
    Attachment lImage = mAttachments.get(1);
    assertNotNull(lImage.getDataStream());
    assertEquals("attachment", lImage.getDisposition());
    assertEquals("image/png;\r\n name=\"test.png\"", lImage.getContentType());
    assertEquals("test.png", lImage.getFileName());

  }

  @Test
  public void testAttachedTextDocumentStripping(){
    Attachment lTextFile = mAttachments.get(2);
    assertEquals("attachment", lTextFile.getDisposition());
    assertEquals("text/plain; charset=UTF-8;\r\n name=\"test.txt\"", lTextFile.getContentType());
    assertEquals("test.txt", lTextFile.getFileName());
    assertEquals("the attachment is a circle", lTextFile.getTextContent());
  }
}
