package com.aroundroid.aroundgps;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;


/***
 * This class build html email (string output) using Freemarker template mechanism
 * @author Tomer
 *
 */
public class AroundroidFTLMails {

	private static final String invitationFtl = "invitation_aroundroid.ftl";
	private static final String welcomeFtl = "welcome_aroundroid.ftl";
	private static final String reminderFtl = "reminder_aroundroid.ftl";
	private static final String deletedFtl = "deleted_aroundroid.ftl";

	/***
	 * builds the invitation mail for inviting new friend to use Aroundroid
	 * @param userName
	 * @param friendMail
	 * @return string representation of the html document or null on error
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String getInviteMail(String userName,String friendMail){
		// Add the values in the datamodel
		Map datamodel = new HashMap();		
		datamodel.put("username", userName);
		datamodel.put("friendmail", friendMail);
		// Process the template using FreeMarker
		try {
			return freemarkerDo(datamodel, invitationFtl);
		} catch (IOException e) {
		} catch (TemplateException e) {
		}
		return null;
	}

	/***
	 * build the Welcome mail for first time users
	 * @param userName
	 * @return string representation of the html document or null on error
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String getWelcome(String userName){
		// Add the values in the datamodel
		Map datamodel = new HashMap();		
		datamodel.put("username", userName);
		// Process the template using FreeMarker
		try {
			return freemarkerDo(datamodel, welcomeFtl);
		} catch (IOException e) {
		} catch (TemplateException e) {
		}
		return null;
	}

	/***
	 * build reminder mail for users that have not used the app for a long time.
	 * @param userName
	 * @return string representation of the html document or null on error
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String getReminderMail(String userName){
		// Add the values in the datamodel
		Map datamodel = new HashMap();		
		datamodel.put("username", userName);
		// Process the template using FreeMarker
		try {
			return freemarkerDo(datamodel, reminderFtl);
		} catch (IOException e) {
		} catch (TemplateException e) {
		}
		return null;
	}

	/***
	 * build mail for users that their database record is going to be deleted
	 * @param userName
	 * @return string representation of the html document or null on error
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String getDeletedMail(String userName){
		// Add the values in the datamodel
		Map datamodel = new HashMap();		
		datamodel.put("username", userName);
		// Process the template using FreeMarker
		try {
			return freemarkerDo(datamodel, deletedFtl);
		} catch (IOException e) {
		} catch (TemplateException e) {
		}
		return null;
	}

	/***
	 * using map of values and template file name to build the correct html document
	 * @param datamodel
	 * @param template
	 * @return  string representation of the html document
	 * @throws IOException
	 * @throws TemplateException
	 */
	@SuppressWarnings("rawtypes")
	public static String freemarkerDo(Map datamodel, String template) throws IOException, TemplateException
	{
		Configuration cfg = new Configuration();
		Template tpl = cfg.getTemplate(template);
		ByteArrayOutputStream bAo = new ByteArrayOutputStream();
		OutputStreamWriter output = new OutputStreamWriter(bAo);
		tpl.process(datamodel, output);
		return (bAo.toString());
	}

}
