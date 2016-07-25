package uk.co.fivium.dmda.EmailMessages;

import org.junit.BeforeClass;
import org.junit.Test;
import uk.co.fivium.dmda.Server.ConfigurationException;
import uk.co.fivium.dmda.Server.SMTPConfig;
import uk.co.fivium.dmda.TestUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestEmailMessage {
  @BeforeClass
  public static void loadConfig() throws ConfigurationException {
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
  }
}
