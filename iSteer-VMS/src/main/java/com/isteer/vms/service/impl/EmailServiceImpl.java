package com.isteer.vms.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import com.isteer.vms.dto.ComputerResponseDto;
import com.isteer.vms.service.EmailService;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMessage.RecipientType;
@Service
public class EmailServiceImpl implements EmailService{
	private JavaMailSender javaMailSender;
	
	@Value("${spring.mail.username}")
	private String fromEmail;
	
	public EmailServiceImpl(JavaMailSender javaMailSender) {
		this.javaMailSender = javaMailSender;
	}
	
	@Override
	public void sendVulnEmailNotification(String to, ComputerResponseDto data) {
		try {
			javaMailSender.send(new vulnerabilityEmailPreparator(to,data));
		} catch (Exception e) { 
			e.printStackTrace();
		}
		
	}

	@Override
	public void sendVulnEmailNotifications(List<String> toEmail, ComputerResponseDto data) {
		for(String email : toEmail) {
			try {
				javaMailSender.send(new vulnerabilityEmailPreparator(email, data));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
}

class vulnerabilityEmailPreparator implements MimeMessagePreparator{
	private String toEmail;
	private String subject;
	private String emailBody;
	
	public vulnerabilityEmailPreparator(String toEmail, ComputerResponseDto data) {
		this.toEmail = toEmail;
		
		//Set subject
		
		//Set email body
	}
	
	@Override
	public void prepare(MimeMessage mimeMessage) throws Exception {
		mimeMessage.setFrom(emailBody);
		mimeMessage.setRecipient(RecipientType.TO, new InternetAddress(toEmail));
		mimeMessage.setSubject(subject);
		mimeMessage.setText(emailBody, "text/html");
	}
}
