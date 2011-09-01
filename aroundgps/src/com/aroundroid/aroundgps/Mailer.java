package com.aroundroid.aroundgps;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/***
 * A helper class for sending emails, for one servlet session, and a single sender mail address.
 * @author Tomer
 */
public class Mailer {
	
	private static final String emailRegularExpression = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"; //$NON-NLS-1$
	
	private final Properties props = new Properties();
	private final Session session = Session.getDefaultInstance(props, null);
	
	private final InternetAddress iA;
	
	/***
	 * builds a new mailer base on sender's mailing information (mail address and nickname) 
	 * @param mailAddress sender's mail address
	 * @param Personal nickname
	 * @throws UnsupportedEncodingException
	 */
	public Mailer(String mailAddress,String Personal) throws UnsupportedEncodingException{
		iA = new InternetAddress(mailAddress, Personal);
	}
	
	/***
	 * 
	 * @param recpMailaddress
	 * @param subject
	 * @param htmlBody
	 * @return
	 * @throws MessagingException
	 */
	public boolean sendOneHtmlMail(String recpMailaddress, String subject, String htmlBody) throws MessagingException{
		if (htmlBody==null){
			return false;
		}
		Message msg = new MimeMessage(session);
		msg.setFrom(iA);
		msg.addRecipient(Message.RecipientType.TO,
				new InternetAddress(recpMailaddress));
		msg.setSubject(subject);
        Multipart mp = new MimeMultipart();
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html");
        mp.addBodyPart(htmlPart);
        msg.setContent(mp);
        Transport.send(msg);
        return true;
	}
	

	public void sendOneMail(String recpMailAdress,String subject,String msgBody) throws MessagingException{
		Message msg = new MimeMessage(session);
		msg.setFrom(iA);
		msg.addRecipient(Message.RecipientType.TO,
				new InternetAddress(recpMailAdress));
		msg.setSubject(subject);
		
		msg.setText(msgBody);
		Transport.send(msg);
	}
	
	/***
	 * checks that the mail address matches the email regular expression
	 * @param mailAdress
	 * @return true if and only if mailAddress matches emailRegularExpression
	 */
	public static boolean validMail(String mailAddress){
		return mailAddress.matches(emailRegularExpression);
	}
}
