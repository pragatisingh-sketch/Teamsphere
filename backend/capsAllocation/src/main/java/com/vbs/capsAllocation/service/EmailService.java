package com.vbs.capsAllocation.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.List;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String body, String cc) {
        try {
            System.out.println("Email started");
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            System.out.println("EMail");
            helper.setReplyTo("no-reply@google.com");
            helper.setTo(to);

            if (cc != null && !cc.trim().isEmpty()) {
                String[] ccArray = cc.split(",\\s*");
                helper.setCc(ccArray);
            }

            helper.setSubject(subject);
            helper.setText(body, true);
            System.out.println("This is my message " + message);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Error sending email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendEmail(List<String> toList, List<String> ccList, String subject, String body) {
        try {

            System.out.println("Email started");
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setReplyTo("no-reply@google.com");
            helper.setTo(toList.toArray(new String[0]));

            if (ccList != null && !ccList.isEmpty()) {
                helper.setCc(ccList.toArray(new String[0]));
            }

            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
        }
        catch (MessagingException e) {
            System.err.println("Error sending email: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
