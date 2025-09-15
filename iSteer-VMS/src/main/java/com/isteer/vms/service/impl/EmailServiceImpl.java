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
	
	@Value("${development.ui.url}")
	private String devUiUrl;
	
	@Value("${production.ui.url}")
	private String prodUiUrl;
	
	@Value("${app.environment}")
	private String environment;

	private String getBaseUrl() {
	    return "prod".equalsIgnoreCase(environment) ? prodUiUrl : devUiUrl;
	}
	
	public EmailServiceImpl(JavaMailSender javaMailSender) {
		this.javaMailSender = javaMailSender;
	}
	
	@Override
	public void sendVulnEmailNotification(ComputerResponseDto data) {
		log.info("Sending vulnerability email notification to computer: {}", data.getMachineName());
		String baseUrl = getBaseUrl();
		try {
			javaMailSender.send(new VulnerabilityEmailPreparator(data, fromEmail, baseUrl));
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
		String baseUrl = getBaseUrl();
		for(ComputerResponseDto computer : data) {
			try {
				javaMailSender.send(new VulnerabilityEmailPreparator(computer,fromEmail, baseUrl));
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
	private String toEmail;
	private String subject = "Urgent Action Required: Vulnerabilities Detected on your Computer";
	private String emailBody;
	
	public VulnerabilityEmailPreparator(ComputerResponseDto data, String fromEmail, String devUiUrl) {
		 String appRows = generateApplicationRows(data.getApplicationDetails());
		 this.fromEmail = fromEmail;
		 this.toEmail = data.getLoggedInUserEmail();
//		 this.toEmail = "kavin.kr@isteer.com";
//		 this.toEmail = "ponvasanth.rangasamy@isteer.com";
		 this.emailBody = "<html>\r\n"
		 		+ "  <body style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; font-size: 14px; line-height: 1.5; color: #333; margin: 0; padding: 20px 5px; background-color: #ededed; height: 100%;\">\r\n"
		 		+ "    <div style=\"border: 1px solid #d3eaf5; border-radius: 8px; max-width: 800px; margin: 50px auto; box-shadow: 0 0.5px 8px #bbb; background-color: #fff\">\r\n"
		 		+ "      <div style=\"border-top: 8px solid #118ac3; border-radius: 8px; padding: 24px;\">\r\n"
		 		+ "        <!-- Header: Logo left, text centered -->\r\n"
		 		+ "        <div style=\"display: flex; align-items: center;\">\r\n"
		 		+ "  <!-- Logo -->\r\n"
		 		+ "  <div style=\"display: flex; align-items: center;\">\r\n"
		 		+ "    <img src=\"https://isteer.com/wp-content/uploads/2024/10/isteer_logo-1.png\" alt=\"iSteer Logo\" style=\"height: 60px; width: auto;\">\r\n"
		 		+ "  </div>\r\n"
		 		+ "\r\n"
		 		+ "  <!-- Vertical line as a div -->\r\n"
		 		+ "  <div style=\"\r\n"
		 		+ "    width: 1px;\r\n"
		 		+ "    height: 40px;\r\n"
		 		+ "    background-color: #969393;\r\n"
		 		+ "    margin: 10px 20px;\r\n"
		 		+ "  \"></div>\r\n"
		 		+ "\r\n"
		 		+ "  <!-- Text -->\r\n"
		 		+ "  <div>\r\n"
		 		+ "    <h2 style=\"color: #118ac3; margin: 10px; font-weight: 600; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\">\r\n"
		 		+ "      Vulnerability Monitoring System\r\n"
		 		+ "    </h2>\r\n"
		 		+ "  </div>\r\n"
		 		+ "</div>\r\n"
		 		+ "        <!-- Greeting -->\r\n"
		 		+ "        <p>Dear <strong>"+ data.getLoggedInUserName() +"</strong>,</p>\r\n"
		 		+ "        <!-- Context and Importance -->\r\n"
		 		+ "        <p>\r\n"
		 		+ "          To ensure a secure and compliant environment, we have detected vulnerable applications on your system that require your attention. Please review the details below and take the necessary action.\r\n"
		 		+ "        </p>\r\n"
		 		+ "        <!-- Machine Details -->\r\n"
		 		+ "        <ul style=\"padding-left: 12px; margin-bottom: 16px; list-style-type: none;\">\r\n"
		 		+ "          <li><strong>Machine Name:</strong> "+data.getMachineName()+"</li>\r\n"
		 		+ "          <li><strong>IP Address:</strong> "+ data.getIpAddress() +"</li>\r\n"
		 		+ "          <li><strong>Logged In User:</strong> "+ data.getLoggedInUserName() +"</li>\r\n"
		 		+ "        </ul>\r\n"
		 		+ "        <!-- Vulnerabilities Table with Grey Shadow-->\r\n"
		 		+ "        <div style=\"overflow-x: auto; border-radius: 8px; box-shadow: 0 0.5px 3.5px #bbb;\">\r\n"
		 		+ "          <table style=\"width: 100%; border-collapse: collapse; font-size: 14px; border-radius: 8px; background: #fff;\">\r\n"
		 		+ "            <thead>\r\n"
		 		+ "              <tr style=\"background-color: #118ac3; color: #fff;\">\r\n"
		 		+ "                <th style=\"padding: 10px 14px; border: 1px solid #ccc;\">Application Name</th>\r\n"
		 		+ "                <th style=\"padding: 10px 14px; border: 1px solid #ccc;\">Vendor</th>\r\n"
		 		+ "                <th style=\"padding: 10px 14px; border: 1px solid #ccc;\">Version</th>\r\n"
		 		+ "                <th style=\"padding: 10px 14px; border: 1px solid #ccc;\">Critical</th>\r\n"
		 		+ "                <th style=\"padding: 10px 14px; border: 1px solid #ccc;\">High</th>\r\n"
		 		+ "                <th style=\"padding: 10px 14px; border: 1px solid #ccc;\">Medium</th>\r\n"
		 		+ "                <th style=\"padding: 10px 24px; border: 1px solid #ccc;\">Low</th>\r\n"
		 		+ "              </tr>\r\n"
		 		+ "            </thead>\r\n"
		 		+ "            <tbody>\r\n"
		 		+appRows
		 		+ "            </tbody>\r\n"
		 		+ "          </table>\r\n"
		 		+ "        </div>\r\n"
		 		+ "\r\n"
		 		+ "        <!-- Notes and Action -->\r\n"
		 		+ "        <p style=\"color: #444; margin-top: 16px;\">\r\n"
		 		+ "          <strong>Note:</strong>\r\n"
		 		+ "          <em>The numbers under Critical, High, Medium, and Low indicate the count of vulnerabilities detected at each severity level.</em>\r\n"
		 		+ "        </p>\r\n"
		 		+ "		   <p> <a href=\"" + devUiUrl + "/user-report/" + data.getUuid() + "\" target=\"_blank\">Click here</a> to access the full report detailing your system vulnerabilities.</p>\r\n"
		 		+ "        <!-- Closing -->\r\n"
		 		+ "        <p style=\"margin-bottom: 0;\">Thanks and Regards,<br>It-Ops</p>\r\n"
		 		+ "      </div>\r\n"
		 		+ "    </div>\r\n"
		 		+ "  </body>\r\n"
		 		+ "</html>";
		
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
