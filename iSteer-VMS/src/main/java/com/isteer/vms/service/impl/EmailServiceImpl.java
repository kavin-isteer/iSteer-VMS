package com.isteer.vms.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import com.isteer.vms.dto.ApplicationResponseDto;
import com.isteer.vms.dto.ComputerResponseDto;
import com.isteer.vms.exception.EmailServiceException;
import com.isteer.vms.response.ResponseCode;
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
	
	@Value("${spring.mail.ccmail}")
	private String ccEmail;
	
	@Value("${ui.base.url}")
	private String baseUrl;

	public EmailServiceImpl(JavaMailSender javaMailSender) {
		this.javaMailSender = javaMailSender;
	}
	
	@Override
	public void sendVulnEmailNotification(ComputerResponseDto data) {
		log.info("Sending vulnerability email notification to computer: {}", data.getMachineName());
		try {
			javaMailSender.send(new VulnerabilityEmailPreparator(data, fromEmail, ccEmail, baseUrl));
			log.info("Vulnerability email sent successfully to {}", data.getLoggedInUserName());
		} catch (MailSendException e) { 
			log.error(e.getMessage());
			throw new EmailServiceException(ResponseCode.EMAIL_NOT_SENT.getMessage(), 500);
		} catch (MailParseException e) { 
			log.error(e.getMessage());
			throw new EmailServiceException(ResponseCode.INVALID_EMAIL.getMessage(), 400);
		} catch (MailException e) { 
			log.error(e.getMessage());
			throw new EmailServiceException(ResponseCode.INTERNAL_ERROR.getMessage(), 500);
		} catch (Exception e) { 
			log.error(e.getMessage());
			throw new EmailServiceException(ResponseCode.INTERNAL_ERROR.getMessage(), 500);
		}
		
	}

	@Override
	public void sendVulnEmailNotifications(List<ComputerResponseDto> data) {
		log.info("Sending vulnerability email notifications to {} computers", data.size());
		for(ComputerResponseDto computer : data) {
			try {
				javaMailSender.send(new VulnerabilityEmailPreparator(computer,fromEmail, ccEmail, baseUrl));
			} catch (MailSendException e) { 
				log.error(e.getMessage());
				throw new EmailServiceException(ResponseCode.EMAIL_NOT_SENT.getMessage(), 500);
			} catch (MailParseException e) { 
				log.error(e.getMessage());
				throw new EmailServiceException(ResponseCode.INVALID_EMAIL.getMessage(), 400);
			} catch (MailException e) { 
				log.error(e.getMessage());
				throw new EmailServiceException(ResponseCode.INTERNAL_ERROR.getMessage(), 500);
			} catch (Exception e) {
				log.error(e.getMessage());
				throw new EmailServiceException(ResponseCode.INTERNAL_ERROR.getMessage(), 500);
			}
		}
		
	}
}

class VulnerabilityEmailPreparator implements MimeMessagePreparator{
	
	private String fromEmail;
	private String ccEmail;
	private String toEmail;
	private String subject = "Urgent Action Required: Vulnerabilities Detected on your Computer";
	private String emailBody;
	
	public VulnerabilityEmailPreparator(ComputerResponseDto data, String fromEmail, String ccEmail, String devUiUrl) {
		 String appRows = generateApplicationRows(data.getApplicationDetails());
		 this.fromEmail = fromEmail;
		 this.ccEmail = ccEmail;
		 this.toEmail = data.getLoggedInUserEmail();
		 
		 StringBuilder sb = new StringBuilder();

		 sb.append("<html>\r\n")
		   .append("  <body style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; font-size: 14px; line-height: 1.5; color: #333; margin: 0; padding: 20px 5px; background-color: #ededed; height: 100%;\">\r\n")
		   .append("    <div style=\"border: 1px solid #d3eaf5; border-radius: 8px; max-width: 800px; margin: 50px auto; box-shadow: 0 0.5px 8px #bbb; background-color: #fff\">\r\n")
		   .append("      <div style=\"border-top: 8px solid #118ac3; border-radius: 8px; padding: 24px;\">\r\n")
		   .append("        <div style=\"display: flex; align-items: center;\">\r\n")
		   .append("          <div style=\"display: flex; align-items: center;\">\r\n")
		   .append("            <img src=\"https://isteer.com/wp-content/uploads/2024/10/isteer_logo-1.png\" alt=\"iSteer Logo\" style=\"height: 60px; width: auto;\">\r\n")
		   .append("          </div>\r\n")
		   .append("          <div style=\"width: 1px; height: 40px; background-color: #969393; margin: 10px 20px;\"></div>\r\n")
		   .append("          <div>\r\n")
		   .append("            <h2 style=\"color: #118ac3; margin: 10px; font-weight: 600; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\">\r\n")
		   .append("              Vulnerability Monitoring System\r\n")
		   .append("            </h2>\r\n")
		   .append("          </div>\r\n")
		   .append("        </div>\r\n")
		   .append("        <p>Dear <strong>").append(data.getLoggedInUserName()).append("</strong>,</p>\r\n")
		   .append("        <p>\r\n")
		   .append("          To ensure a secure and compliant environment, we have detected vulnerable applications on your system that require your attention. Please review the details below and take the necessary action.\r\n")
		   .append("        </p>\r\n")
		   .append("        <ul style=\"padding-left: 12px; margin-bottom: 16px; list-style-type: none;\">\r\n")
		   .append("          <li><strong>Machine Name:</strong> ").append(data.getMachineName()).append("</li>\r\n")
		   .append("          <li><strong>IP Address:</strong> ").append(data.getIpAddress()).append("</li>\r\n")
		   .append("          <li><strong>MAC Address:</strong> ").append(data.getMacAddress()).append("</li>\r\n")
		   .append("          <li><strong>Logged In User:</strong> ").append(data.getLoggedInUserName()).append("</li>\r\n")
		   .append("        </ul>\r\n")
		   .append("        <div style=\"overflow-x: auto; overflow-y: auto; max-height: 400px; border-radius: 8px; box-shadow: 0 0.5px 3.5px #bbb;\">\r\n")
		   .append("          <table style=\"width: 100%; border-collapse: collapse; font-size: 14px; border-radius: 8px; background: #fff;\">\r\n")
		   .append("            <thead>\r\n")
		   .append("              <tr style=\"background-color: #118ac3; color: #fff;\">\r\n")
		   .append("                <th style=\"padding: 10px 14px; border: 1px solid #ccc;\">Application Name</th>\r\n")
		   .append("                <th style=\"padding: 10px 14px; border: 1px solid #ccc;\">Vendor</th>\r\n")
		   .append("                <th style=\"padding: 10px 14px; border: 1px solid #ccc;\">Version</th>\r\n")
		   .append("                <th style=\"padding: 10px 14px; border: 1px solid #ccc;\">Critical</th>\r\n")
		   .append("                <th style=\"padding: 10px 14px; border: 1px solid #ccc;\">High</th>\r\n")
		   .append("                <th style=\"padding: 10px 14px; border: 1px solid #ccc;\">Medium</th>\r\n")
		   .append("                <th style=\"padding: 10px 24px; border: 1px solid #ccc;\">Low</th>\r\n")
		   .append("              </tr>\r\n")
		   .append("            </thead>\r\n")
		   .append("            <tbody>\r\n")
		   .append(appRows)
		   .append("            </tbody>\r\n")
		   .append("          </table>\r\n")
		   .append("        </div>\r\n")
		   .append("        <p style=\"color: #444; margin-top: 16px;\">\r\n")
		   .append("          <strong>Note:</strong>\r\n")
		   .append("          <em>The numbers under Critical, High, Medium, and Low indicate the count of vulnerabilities detected at each severity level.</em>\r\n")
		   .append("        </p>\r\n")
		   .append("        <p><a href=\"").append(devUiUrl).append("/user/report/").append(data.getUuid()).append("\" target=\"_blank\">Click here</a> to access the full report detailing your system vulnerabilities.</p>\r\n")
		   .append("        <p style=\"margin-bottom: 0;\">Thanks and Regards,<br>It-Ops</p>\r\n")
		   .append("      </div>\r\n")
		   .append("    </div>\r\n")
		   .append("  </body>\r\n")
		   .append("</html>");

		 this.emailBody = sb.toString();

	}
	
	@Override
	public void prepare(MimeMessage mimeMessage) throws Exception {
		mimeMessage.setFrom(new InternetAddress(fromEmail, "Isteer-VMS"));
		mimeMessage.setRecipient(RecipientType.TO, new InternetAddress(toEmail));
		mimeMessage.setRecipient(RecipientType.CC, new InternetAddress(ccEmail));
		mimeMessage.setSubject(subject);
		mimeMessage.setContent(emailBody, "text/html; charset=UTF-8");
	}
	
	 public static String generateApplicationRows(List<ApplicationResponseDto> apps) {
	        StringBuilder sb = new StringBuilder();
	        boolean alternate = true;
	        for (ApplicationResponseDto app : apps) {
	            String bgColor = alternate ? "#f0f0f0" : "#ffffff";
	            sb.append("<tr style=\"background-color: ").append(bgColor).append(";\">")
	              .append("<td style=\"padding: 8px; border: 1px solid #ddd; text-align: center; vertical-align: middle;\"><strong>")
	              .append(app.getSoftwareName()).append("</strong></td>")
	              .append("<td style=\"padding: 8px; border: 1px solid #ddd; text-align: center; vertical-align: middle;\">")
	              .append(app.getVendor()).append("</td>")
	              .append("<td style=\"padding: 8px; border: 1px solid #ddd; text-align: center; vertical-align: middle;\">")
	              .append(app.getSoftwareVersion()).append("</td>")
	              .append("<td style=\"padding: 8px; border: 1px solid #ddd; text-align: center; vertical-align: middle;\">")
	              .append(app.getCriticalVulnerabilityCount()).append("</td>")
	              .append("<td style=\"padding: 8px; border: 1px solid #ddd; text-align: center; vertical-align: middle;\">")
	              .append(app.getHighVulnerabilityCount()).append("</td>")
	              .append("<td style=\"padding: 8px; border: 1px solid #ddd; text-align: center; vertical-align: middle;\">")
	              .append(app.getMediumVulnerabilityCount()).append("</td>")
	              .append("<td style=\"padding: 8px; border: 1px solid #ddd; text-align: center; vertical-align: middle;\">")
	              .append(app.getLowVulnerabilityCount()).append("</td>")
	              .append("</tr>");
	            alternate = !alternate;
	        }
	        return sb.toString();
	    }
}
