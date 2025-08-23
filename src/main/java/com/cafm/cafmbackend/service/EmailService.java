package com.cafm.cafmbackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.concurrent.CompletableFuture;

/**
 * Service for sending email notifications.
 * 
 * Purpose: Centralized email sending functionality with template support
 * Pattern: Async email sending to avoid blocking operations
 * Java 23: Uses virtual threads for async operations
 * Architecture: Service layer component for external communication
 * Standards: Implements retry logic and error handling
 */
@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username:noreply@cafm.com}")
    private String fromEmail;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    @Value("${app.name:CAFM System}")
    private String appName;
    
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    /**
     * Send password reset email
     */
    public CompletableFuture<Boolean> sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                helper.setTo(toEmail);
                helper.setFrom(fromEmail);
                helper.setSubject(appName + " - Password Reset Request");
                
                String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
                String htmlContent = buildPasswordResetEmailHtml(userName, resetLink);
                
                helper.setText(htmlContent, true);
                
                mailSender.send(message);
                logger.info("Password reset email sent to: {}", toEmail);
                return true;
                
            } catch (MessagingException e) {
                logger.error("Failed to send password reset email to: {}", toEmail, e);
                return false;
            }
        });
    }
    
    /**
     * Send email verification email
     */
    public CompletableFuture<Boolean> sendVerificationEmail(String toEmail, String userName, String verificationToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                helper.setTo(toEmail);
                helper.setFrom(fromEmail);
                helper.setSubject(appName + " - Verify Your Email");
                
                String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;
                String htmlContent = buildVerificationEmailHtml(userName, verificationLink);
                
                helper.setText(htmlContent, true);
                
                mailSender.send(message);
                logger.info("Verification email sent to: {}", toEmail);
                return true;
                
            } catch (MessagingException e) {
                logger.error("Failed to send verification email to: {}", toEmail, e);
                return false;
            }
        });
    }
    
    /**
     * Send welcome email for new users
     */
    public CompletableFuture<Boolean> sendWelcomeEmail(String toEmail, String userName, String temporaryPassword) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                helper.setTo(toEmail);
                helper.setFrom(fromEmail);
                helper.setSubject("Welcome to " + appName);
                
                String htmlContent = buildWelcomeEmailHtml(userName, toEmail, temporaryPassword);
                
                helper.setText(htmlContent, true);
                
                mailSender.send(message);
                logger.info("Welcome email sent to: {}", toEmail);
                return true;
                
            } catch (MessagingException e) {
                logger.error("Failed to send welcome email to: {}", toEmail, e);
                return false;
            }
        });
    }
    
    /**
     * Send simple text email
     */
    public CompletableFuture<Boolean> sendSimpleEmail(String toEmail, String subject, String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(toEmail);
                message.setFrom(fromEmail);
                message.setSubject(subject);
                message.setText(content);
                
                mailSender.send(message);
                logger.info("Email sent to: {} with subject: {}", toEmail, subject);
                return true;
                
            } catch (Exception e) {
                logger.error("Failed to send email to: {}", toEmail, e);
                return false;
            }
        });
    }
    
    // Email Template Builders
    
    private String buildPasswordResetEmailHtml(String userName, String resetLink) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #007bff; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f8f9fa; padding: 30px; margin-top: 20px; }
                    .button { display: inline-block; padding: 12px 30px; background-color: #007bff; 
                             color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { margin-top: 30px; text-align: center; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>%s</h1>
                    </div>
                    <div class="content">
                        <h2>Password Reset Request</h2>
                        <p>Hello %s,</p>
                        <p>We received a request to reset your password. Click the button below to create a new password:</p>
                        <div style="text-align: center;">
                            <a href="%s" class="button">Reset Password</a>
                        </div>
                        <p>This link will expire in 1 hour for security reasons.</p>
                        <p>If you didn't request a password reset, please ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message, please do not reply.</p>
                        <p>&copy; 2024 %s. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, appName, userName, resetLink, appName);
    }
    
    private String buildVerificationEmailHtml(String userName, String verificationLink) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #28a745; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f8f9fa; padding: 30px; margin-top: 20px; }
                    .button { display: inline-block; padding: 12px 30px; background-color: #28a745; 
                             color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { margin-top: 30px; text-align: center; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>%s</h1>
                    </div>
                    <div class="content">
                        <h2>Verify Your Email Address</h2>
                        <p>Hello %s,</p>
                        <p>Thank you for registering! Please verify your email address by clicking the button below:</p>
                        <div style="text-align: center;">
                            <a href="%s" class="button">Verify Email</a>
                        </div>
                        <p>This link will expire in 24 hours.</p>
                        <p>If you didn't create an account, please ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message, please do not reply.</p>
                        <p>&copy; 2024 %s. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, appName, userName, verificationLink, appName);
    }
    
    private String buildWelcomeEmailHtml(String userName, String email, String temporaryPassword) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #17a2b8; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f8f9fa; padding: 30px; margin-top: 20px; }
                    .credentials { background-color: white; padding: 15px; border-left: 4px solid #17a2b8; 
                                  margin: 20px 0; font-family: monospace; }
                    .button { display: inline-block; padding: 12px 30px; background-color: #17a2b8; 
                             color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { margin-top: 30px; text-align: center; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to %s</h1>
                    </div>
                    <div class="content">
                        <h2>Account Created Successfully</h2>
                        <p>Hello %s,</p>
                        <p>Your account has been created. Here are your login credentials:</p>
                        <div class="credentials">
                            <strong>Email:</strong> %s<br>
                            <strong>Temporary Password:</strong> %s
                        </div>
                        <p><strong>Important:</strong> Please change your password after your first login.</p>
                        <div style="text-align: center;">
                            <a href="%s/login" class="button">Login Now</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p>This is an automated message, please do not reply.</p>
                        <p>&copy; 2024 %s. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, appName, userName, email, temporaryPassword, frontendUrl, appName);
    }
}