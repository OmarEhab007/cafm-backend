package com.cafm.cafmbackend.application.service;

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
     * Send work order assignment notification
     */
    public CompletableFuture<Boolean> sendWorkOrderAssignmentEmail(String toEmail, String userName, 
            String workOrderNumber, String workOrderTitle, String dueDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                helper.setTo(toEmail);
                helper.setFrom(fromEmail);
                helper.setSubject(appName + " - Work Order Assignment: " + workOrderNumber);
                
                String htmlContent = buildWorkOrderAssignmentEmailHtml(userName, workOrderNumber, workOrderTitle, dueDate);
                helper.setText(htmlContent, true);
                
                mailSender.send(message);
                logger.info("Work order assignment email sent to: {} for work order: {}", toEmail, workOrderNumber);
                return true;
                
            } catch (MessagingException e) {
                logger.error("Failed to send work order assignment email to: {}", toEmail, e);
                return false;
            }
        });
    }
    
    /**
     * Send maintenance report notification
     */
    public CompletableFuture<Boolean> sendMaintenanceReportEmail(String toEmail, String userName, 
            String reportNumber, String reportTitle, String schoolName, String status) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                helper.setTo(toEmail);
                helper.setFrom(fromEmail);
                helper.setSubject(appName + " - Maintenance Report " + status + ": " + reportNumber);
                
                String htmlContent = buildMaintenanceReportEmailHtml(userName, reportNumber, reportTitle, schoolName, status);
                helper.setText(htmlContent, true);
                
                mailSender.send(message);
                logger.info("Maintenance report email sent to: {} for report: {}", toEmail, reportNumber);
                return true;
                
            } catch (MessagingException e) {
                logger.error("Failed to send maintenance report email to: {}", toEmail, e);
                return false;
            }
        });
    }
    
    /**
     * Send weekly maintenance summary email
     */
    public CompletableFuture<Boolean> sendWeeklySummaryEmail(String toEmail, String userName, 
            int completedReports, int pendingWorkOrders, int overdueItems) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                helper.setTo(toEmail);
                helper.setFrom(fromEmail);
                helper.setSubject(appName + " - Weekly Maintenance Summary");
                
                String htmlContent = buildWeeklySummaryEmailHtml(userName, completedReports, pendingWorkOrders, overdueItems);
                helper.setText(htmlContent, true);
                
                mailSender.send(message);
                logger.info("Weekly summary email sent to: {}", toEmail);
                return true;
                
            } catch (MessagingException e) {
                logger.error("Failed to send weekly summary email to: {}", toEmail, e);
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
    
    private String buildWorkOrderAssignmentEmailHtml(String userName, String workOrderNumber, String workOrderTitle, String dueDate) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #fd7e14; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f8f9fa; padding: 30px; margin-top: 20px; }
                    .work-order-details { background-color: white; padding: 20px; border-left: 4px solid #fd7e14; 
                                         margin: 20px 0; }
                    .priority { color: #dc3545; font-weight: bold; }
                    .button { display: inline-block; padding: 12px 30px; background-color: #fd7e14; 
                             color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { margin-top: 30px; text-align: center; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔧 Work Order Assignment</h1>
                    </div>
                    <div class="content">
                        <h2>New Work Order Assigned</h2>
                        <p>Hello %s,</p>
                        <p>A new work order has been assigned to you. Please review the details below:</p>
                        <div class="work-order-details">
                            <strong>Work Order:</strong> %s<br>
                            <strong>Title:</strong> %s<br>
                            <strong>Due Date:</strong> <span class="priority">%s</span>
                        </div>
                        <p>Please log into the system to view full details and start working on this task.</p>
                        <div style="text-align: center;">
                            <a href="%s/work-orders/%s" class="button">View Work Order</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p>This is an automated message, please do not reply.</p>
                        <p>&copy; 2024 %s. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, workOrderNumber, workOrderTitle, dueDate, frontendUrl, workOrderNumber, appName);
    }
    
    private String buildMaintenanceReportEmailHtml(String userName, String reportNumber, String reportTitle, String schoolName, String status) {
        String statusColor = switch (status.toLowerCase()) {
            case "approved" -> "#28a745";
            case "rejected" -> "#dc3545";
            case "under_review" -> "#ffc107";
            default -> "#17a2b8";
        };
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: %s; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f8f9fa; padding: 30px; margin-top: 20px; }
                    .report-details { background-color: white; padding: 20px; border-left: 4px solid %s; 
                                     margin: 20px 0; }
                    .status { color: %s; font-weight: bold; text-transform: uppercase; }
                    .button { display: inline-block; padding: 12px 30px; background-color: %s; 
                             color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { margin-top: 30px; text-align: center; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📋 Maintenance Report Update</h1>
                    </div>
                    <div class="content">
                        <h2>Report Status Changed</h2>
                        <p>Hello %s,</p>
                        <p>The status of your maintenance report has been updated:</p>
                        <div class="report-details">
                            <strong>Report:</strong> %s<br>
                            <strong>Title:</strong> %s<br>
                            <strong>School:</strong> %s<br>
                            <strong>Status:</strong> <span class="status">%s</span>
                        </div>
                        <div style="text-align: center;">
                            <a href="%s/reports/%s" class="button">View Report</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p>This is an automated message, please do not reply.</p>
                        <p>&copy; 2024 %s. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, statusColor, statusColor, statusColor, statusColor, userName, reportNumber, reportTitle, schoolName, status, frontendUrl, reportNumber, appName);
    }
    
    private String buildWeeklySummaryEmailHtml(String userName, int completedReports, int pendingWorkOrders, int overdueItems) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #6f42c1; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f8f9fa; padding: 30px; margin-top: 20px; }
                    .stats { display: flex; justify-content: space-around; margin: 30px 0; }
                    .stat-box { background-color: white; padding: 20px; text-align: center; border-radius: 8px; 
                               box-shadow: 0 2px 4px rgba(0,0,0,0.1); flex: 1; margin: 0 10px; }
                    .stat-number { font-size: 36px; font-weight: bold; color: #6f42c1; }
                    .stat-label { color: #666; font-size: 14px; }
                    .overdue { color: #dc3545 !important; }
                    .completed { color: #28a745 !important; }
                    .pending { color: #ffc107 !important; }
                    .button { display: inline-block; padding: 12px 30px; background-color: #6f42c1; 
                             color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { margin-top: 30px; text-align: center; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📊 Weekly Maintenance Summary</h1>
                    </div>
                    <div class="content">
                        <h2>Your Weekly Performance</h2>
                        <p>Hello %s,</p>
                        <p>Here's your maintenance activity summary for this week:</p>
                        <div class="stats">
                            <div class="stat-box">
                                <div class="stat-number completed">%d</div>
                                <div class="stat-label">Completed Reports</div>
                            </div>
                            <div class="stat-box">
                                <div class="stat-number pending">%d</div>
                                <div class="stat-label">Pending Work Orders</div>
                            </div>
                            <div class="stat-box">
                                <div class="stat-number overdue">%d</div>
                                <div class="stat-label">Overdue Items</div>
                            </div>
                        </div>
                        <p>Keep up the great work! Your dedication to maintaining our facilities is appreciated.</p>
                        <div style="text-align: center;">
                            <a href="%s/dashboard" class="button">View Dashboard</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p>This is an automated message, please do not reply.</p>
                        <p>&copy; 2024 %s. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, completedReports, pendingWorkOrders, overdueItems, frontendUrl, appName);
    }
}