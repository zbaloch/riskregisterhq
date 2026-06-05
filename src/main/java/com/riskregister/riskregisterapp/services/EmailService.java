package com.riskregister.riskregisterapp.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.riskregister.riskregisterapp.entities.User;

import org.springframework.scheduling.annotation.Async;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Objects;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private Environment environment;

    @Value("${passwordless.email.from}")
    private String from;

    @Value("${host.url}")
    String url;

    private static Logger log = LoggerFactory.getLogger(EmailService.class);

    @Async
    public void sendEmail(User user) {
        String magicLink = url + "/verify-token-and-login?email=" + user.getEmail() + "&token=" + user.getToken();
        String emailText = "Please use this magic link to login: <a href='" + magicLink + "'>" + magicLink + "</a>";

        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            log.info("""
                    ╔══════════════════════════════════════════════════════╗
                    ║              [DEV] Magic Login Email                 ║
                    ╠══════════════════════════════════════════════════════╣
                    ║  To:      {}
                    ║  From:    {}
                    ║  Subject: Login to your RiskRegisterHQ account
                    ╠══════════════════════════════════════════════════════╣
                    ║  Magic Link:
                    ║  {}
                    ╚══════════════════════════════════════════════════════╝
                    """, user.getEmail(), from, magicLink);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(Objects.requireNonNull(from, "passwordless.email.from must be configured"));
            helper.setTo(Objects.requireNonNull(user.getEmail(), "User email must not be null"));
            helper.setSubject("Login to your RiskRegisterHQ account");
            helper.setText(emailText, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            log.error("Failed to send email", e);
        }
    }

}