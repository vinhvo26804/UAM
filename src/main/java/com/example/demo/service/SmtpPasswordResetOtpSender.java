package com.example.demo.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SmtpPasswordResetOtpSender implements PasswordResetOtpSender {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String from;

    public SmtpPasswordResetOtpSender(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.mail.from:no-reply@example.invalid}") String from) {
        this.mailSenderProvider = mailSenderProvider;
        this.from = from;
    }

    @Override
    public void checkAvailable() {
        if (mailSenderProvider.getIfAvailable() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Password reset email is not configured");
        }
    }

    @Override
    public void send(String email, String code) {
        checkAvailable();
        JavaMailSender mailSender = mailSenderProvider.getObject();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Your password reset code");
        message.setText("Your password reset code is: " + code
                + "\n\nThis code expires in 5 minutes and can only be used once."
                + "\nIf you did not request a password reset, ignore this email."
                + "\nDo not share this code with anyone.");

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Password reset email could not be sent");
        }
    }
}
