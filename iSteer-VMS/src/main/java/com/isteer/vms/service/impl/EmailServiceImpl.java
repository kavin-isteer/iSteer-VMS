package com.isteer.vms.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import com.isteer.vms.dto.ApplicationResponseDto;
import com.isteer.vms.dto.ComputerResponseDto;
import com.isteer.vms.service.EmailService;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMessage.RecipientType;
import lombok.extern.log4j.Log4j2;
@Service
@Log4j2
public class EmailServiceImpl implements EmailService{
	private JavaMailSender javaMailSender;
	
	@Value("${spring.mail.username}")
	private String fromEmail;
	
	public EmailServiceImpl(JavaMailSender javaMailSender) {
		this.javaMailSender = javaMailSender;
	}
	
	@Override
	public void sendVulnEmailNotification(ComputerResponseDto data) {
		try {
			javaMailSender.send(new vulnerabilityEmailPreparator(data, fromEmail));
		} catch (Exception e) { 
			//e.printStackTrace();
			log.error(e.getMessage());
		}
		
	}

	@Override
	public void sendVulnEmailNotifications(List<ComputerResponseDto> data) {
		for(ComputerResponseDto computer : data) {
			try {
				javaMailSender.send(new vulnerabilityEmailPreparator(computer,fromEmail));
			} catch (Exception e) {
			//	e.printStackTrace();
				log.error(e.getMessage());
				
			}
		}
		
	}
}

class vulnerabilityEmailPreparator implements MimeMessagePreparator{
	
	private String fromEmail;
	private String toEmail;
	private String subject = "Urgent Action Required: Vulnerabilities Detected on your Computer";
	private String emailBody;
	
	public vulnerabilityEmailPreparator(ComputerResponseDto data, String fromEmail) {
		 String appRows = generateApplicationRows(data.getApplicationDetails());
		 this.fromEmail = fromEmail;
//		 this.toEmail = data.getUserEmail();
//		 this.toEmail = "kavin.kr@isteer.com";
//		 this.toEmail = "ponvasanth71@gmail.com";
		 this.emailBody = "<html>\n" +
	                "  <body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">\n" +
	                "    <p>Dear " + data.getLoggedInUser() + ",</p>\n" +
	                "    <p>We've detected that your system contains the following vulnerable applications:</p>\n" +
	                "\n" +
	                "    <p><strong>Machine Name:</strong> " + data.getMachineName() + "<br/>\n" +
	                "       <strong>IP Address:</strong> " + data.getIpAddress() + "<br/>\n" +
	                "       <strong>Logged In User:</strong> " + data.getLoggedInUser() + "\n" +
	                "    </p>\n" +
	                "\n" +
	                "    <div style=\"max-width: 100%; max-height: 300px; overflow-x: auto; overflow-y: auto; padding: 5px; font-family: Arial, sans-serif;\">\n" +
	                "      <table cellpadding=\"0\" cellspacing=\"0\" style=\"width: 100%; min-width: 900px; font-size: 14px; text-align: center;\">\n" +
	                "        <thead>\n" +
	                "          <tr style=\"background-color: #004080; color: white;\">\n" +
	                "            <th style=\"padding: 10px; border: 1px solid #ccc;\">Application Name</th>\n" +
	                "            <th style=\"padding: 10px; border: 1px solid #ccc;\">Vendor</th>\n" +
	                "            <th style=\"padding: 10px; border: 1px solid #ccc;\">Version</th>\n" +
	                "            <th style=\"padding: 10px; border: 1px solid #ccc;\">Critical</th>\n" +
	                "            <th style=\"padding: 10px; border: 1px solid #ccc;\">High</th>\n" +
	                "            <th style=\"padding: 10px; border: 1px solid #ccc;\">Medium</th>\n" +
	                "            <th style=\"padding: 10px; border: 1px solid #ccc;\">Low</th>\n" +
	                "          </tr>\n" +
	                "        </thead>\n" +
	                "        <tbody>\n" +
	                appRows +
	                "        </tbody>\n" +
	                "      </table>\n" +
	                "    </div>\n" +
	                "\n" +
	                "    <p style=\"margin-top: 15px; color: #555;\">\n" +
	                "      <strong>Note:</strong> <em>The numbers under Critical, High, Medium, and Low columns represent the count of vulnerabilities detected for each severity level.\n" +
	                "    </em></p>\n" +
	                "\n" +
	                "    <p style=\"margin-top: 15px;\">\n" +
	                "      <strong>Action Required:</strong> Please update or remove these applications as soon as possible to maintain system security.\n" +
	                "    </p>\n" +
	                "\n" +
	                "    <p>Regards,<br />Security Team</p>\n" +
	                "  </body>\n" +
	                "</html>";
	}
	
	@Override
	public void prepare(MimeMessage mimeMessage) throws Exception {
		mimeMessage.setFrom(fromEmail);
		mimeMessage.setRecipient(RecipientType.TO, new InternetAddress(toEmail));
		mimeMessage.setSubject(subject);
		mimeMessage.setContent(emailBody, "text/html; charset=UTF-8");
	}
	
	 public static String generateApplicationRows(List<ApplicationResponseDto> apps) {
	        StringBuilder sb = new StringBuilder();
	        boolean alternate = true;
	        for (ApplicationResponseDto app : apps) {
	            String bgColor = alternate ? "#f0f0f0" : "#ffffff";
	            sb.append("<tr style=\"background-color: ").append(bgColor).append(";\">")
	              .append("<td style=\"padding: 8px; border: 1px solid #ddd;\"><strong>")
	              .append(app.getSoftwareName()).append("</strong></td>")
	              .append("<td style=\"padding: 8px; border: 1px solid #ddd;\">")
	              .append(app.getVendor()).append("</td>")
	              .append("<td style=\"padding: 8px; border: 1px solid #ddd;\">")
	              .append(app.getSoftwareVersion()).append("</td>")
	              .append("<td style=\"padding: 8px; border: 1px solid #ddd;\">")
	              .append(app.getCriticalVulnerabilityCount()).append("</td>")
	              .append("<td style=\"padding: 8px; border: 1px solid #ddd;\">")
	              .append(app.getHighVulnerabilityCount()).append("</td>")
	              .append("<td style=\"padding: 8px; border: 1px solid #ddd;\">")
	              .append(app.getMediumVulnerabilityCount()).append("</td>")
	              .append("<td style=\"padding: 8px; border: 1px solid #ddd;\">")
	              .append(app.getLowVulnerabilityCount()).append("</td>")
	              .append("</tr>");
	            alternate = !alternate;
	        }
	        return sb.toString();
	    }
}
