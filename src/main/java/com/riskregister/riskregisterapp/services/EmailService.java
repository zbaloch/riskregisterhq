package com.riskregister.riskregisterapp.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.riskregister.riskregisterapp.entities.User;

import org.springframework.scheduling.annotation.Async;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${passwordless.email.from}")
    private String from;

    @Value("${host.url}")
    String url;


    private static Logger log = LoggerFactory.getLogger(EmailService.class);

    @Async
    public void sendEmail(User user) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            
            helper.setFrom(from);
            helper.setTo(user.getEmail()); 
            helper.setSubject("Login to your RiskRegisterHQ account");

            String magicLink = url + "/verify-token-and-login?email=" + user.getEmail() + "&token=" + user.getToken();

            String emailText = "Please use this magic link to login: <a href='" + magicLink + "'>" + magicLink + "</a>";

            helper.setText(emailText, true); // true indicates HTML content
            
            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            log.error("Failed to send email", e);
        }
    }

}