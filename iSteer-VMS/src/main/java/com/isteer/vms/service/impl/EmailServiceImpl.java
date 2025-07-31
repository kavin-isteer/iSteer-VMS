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
	public void sendVulnEmailNotification(ComputerResponseDto data) {
		try {
			javaMailSender.send(new vulnerabilityEmailPreparator(data));
		} catch (Exception e) { 
			e.printStackTrace();
		}
		
	}

	@Override
	public void sendVulnEmailNotifications(List<ComputerResponseDto> data) {
		for(ComputerResponseDto computer : data) {
			try {
				javaMailSender.send(new vulnerabilityEmailPreparator(computer));
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
	
	public vulnerabilityEmailPreparator(ComputerResponseDto data) {
		//Set to email from computer response dto
		
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
