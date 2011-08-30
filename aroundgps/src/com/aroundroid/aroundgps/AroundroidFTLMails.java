package com.aroundroid.aroundgps;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import freemarker.template.Template;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;



public class AroundroidFTLMails {
	
	private static final String invitationFtl = "invitation_aroundroid.ftl";
	private static final String welcomeFtl = "welcome_aroundroid.ftl";
	private static final String reminderFtl = "reminder_aroundroid.ftl";
	private static final String deletedFtl = "deleted_aroundroid.ftl";

	public static String getInviteMail(String userName,String friendMail){
		// Add the values in the datamodel
		Map datamodel = new HashMap();		
		datamodel.put("username", userName);
		datamodel.put("friendmail", friendMail);
		// Process the template using FreeMarker
			try {
				return freemarkerDo(datamodel, invitationFtl);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TemplateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return null;
	}
	
	public static String getWelcome(String userName){
		// Add the values in the datamodel
		Map datamodel = new HashMap();		
		datamodel.put("username", userName);
		// Process the template using FreeMarker
			try {
				return freemarkerDo(datamodel, welcomeFtl);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TemplateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return null;
	}
	
	public static String getReminderMail(String userName){
		// Add the values in the datamodel
		Map datamodel = new HashMap();		
		datamodel.put("username", userName);
		// Process the template using FreeMarker
			try {
				return freemarkerDo(datamodel, reminderFtl);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TemplateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return null;
	}
	
	public static String getDeletedMail(String userName){
		// Add the values in the datamodel
		Map datamodel = new HashMap();		
		datamodel.put("username", userName);
		// Process the template using FreeMarker
			try {
				return freemarkerDo(datamodel, deletedFtl);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TemplateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return null;
	}



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
