package com.isteer.vms.service;

public interface EmailService {

	void sendEmail(String toEmail, String subject, String emailBody);

}
