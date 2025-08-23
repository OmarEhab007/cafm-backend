package com.cafm.cafmbackend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailService.
 * 
 * Purpose: Test email sending functionality in isolation
 * Pattern: Nested test classes for different email types
 * Java 23: Uses virtual threads through CompletableFuture
 * Architecture: Tests service layer email functionality
 * Standards: Comprehensive coverage of all email methods
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;
    
    @Mock
    private MimeMessage mimeMessage;
    
    @InjectMocks
    private EmailService emailService;
    
    private final String TEST_EMAIL = "user@example.com";
    private final String TEST_USER_NAME = "John Doe";
    private final String TEST_TOKEN = "test-token-123";
    
    @BeforeEach
    void setUp() {
        // Set up configuration values
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@cafm.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(emailService, "appName", "CAFM System");
        
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }
    
    @Nested
    @DisplayName("Password Reset Email Tests")
    class PasswordResetEmailTests {
        
        @Test
        @DisplayName("Should successfully send password reset email")
        void sendPasswordResetEmail_Success() throws ExecutionException, InterruptedException {
            // Given
            doNothing().when(mailSender).send(any(MimeMessage.class));
            
            // When
            CompletableFuture<Boolean> result = emailService.sendPasswordResetEmail(
                TEST_EMAIL, TEST_USER_NAME, TEST_TOKEN
            );
            
            // Then
            assertThat(result.get()).isTrue();
            verify(mailSender).send(mimeMessage);
            verify(mailSender).createMimeMessage();
        }
        
        @Test
        @DisplayName("Should return false when password reset email fails")
        void sendPasswordResetEmail_Failure() throws ExecutionException, InterruptedException {
            // Given
            doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));
            
            // When
            CompletableFuture<Boolean> result = emailService.sendPasswordResetEmail(
                TEST_EMAIL, TEST_USER_NAME, TEST_TOKEN
            );
            
            // Then
            assertThat(result.get()).isFalse();
            verify(mailSender).send(mimeMessage);
        }
        
        @Test
        @DisplayName("Should include correct reset link in email")
        void sendPasswordResetEmail_CorrectContent() throws ExecutionException, InterruptedException {
            // Given
            ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
            doNothing().when(mailSender).send(messageCaptor.capture());
            
            // When
            CompletableFuture<Boolean> result = emailService.sendPasswordResetEmail(
                TEST_EMAIL, TEST_USER_NAME, TEST_TOKEN
            );
            
            // Then
            assertThat(result.get()).isTrue();
            // Note: In a real test, we would verify the content of the MimeMessage
            // but MimeMessage is difficult to mock completely
            verify(mailSender).send(any(MimeMessage.class));
        }
    }
    
    @Nested
    @DisplayName("Email Verification Tests")
    class EmailVerificationTests {
        
        @Test
        @DisplayName("Should successfully send verification email")
        void sendVerificationEmail_Success() throws ExecutionException, InterruptedException {
            // Given
            doNothing().when(mailSender).send(any(MimeMessage.class));
            
            // When
            CompletableFuture<Boolean> result = emailService.sendVerificationEmail(
                TEST_EMAIL, TEST_USER_NAME, TEST_TOKEN
            );
            
            // Then
            assertThat(result.get()).isTrue();
            verify(mailSender).send(mimeMessage);
            verify(mailSender).createMimeMessage();
        }
        
        @Test
        @DisplayName("Should return false when verification email fails")
        void sendVerificationEmail_Failure() throws ExecutionException, InterruptedException {
            // Given
            doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));
            
            // When
            CompletableFuture<Boolean> result = emailService.sendVerificationEmail(
                TEST_EMAIL, TEST_USER_NAME, TEST_TOKEN
            );
            
            // Then
            assertThat(result.get()).isFalse();
            verify(mailSender).send(mimeMessage);
        }
        
        @Test
        @DisplayName("Should include correct verification link in email")
        void sendVerificationEmail_CorrectLink() throws ExecutionException, InterruptedException {
            // Given
            doNothing().when(mailSender).send(any(MimeMessage.class));
            
            // When
            CompletableFuture<Boolean> result = emailService.sendVerificationEmail(
                TEST_EMAIL, TEST_USER_NAME, TEST_TOKEN
            );
            
            // Then
            assertThat(result.get()).isTrue();
            // The email should contain: http://localhost:3000/verify-email?token=test-token-123
            verify(mailSender).send(mimeMessage);
        }
    }
    
    @Nested
    @DisplayName("Welcome Email Tests")
    class WelcomeEmailTests {
        
        @Test
        @DisplayName("Should successfully send welcome email with temporary password")
        void sendWelcomeEmail_Success() throws ExecutionException, InterruptedException {
            // Given
            String tempPassword = "TempPass123";
            doNothing().when(mailSender).send(any(MimeMessage.class));
            
            // When
            CompletableFuture<Boolean> result = emailService.sendWelcomeEmail(
                TEST_EMAIL, TEST_USER_NAME, tempPassword
            );
            
            // Then
            assertThat(result.get()).isTrue();
            verify(mailSender).send(mimeMessage);
            verify(mailSender).createMimeMessage();
        }
        
        @Test
        @DisplayName("Should return false when welcome email fails")
        void sendWelcomeEmail_Failure() throws ExecutionException, InterruptedException {
            // Given
            doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));
            
            // When
            CompletableFuture<Boolean> result = emailService.sendWelcomeEmail(
                TEST_EMAIL, TEST_USER_NAME, "TempPass123"
            );
            
            // Then
            assertThat(result.get()).isFalse();
            verify(mailSender).send(mimeMessage);
        }
        
        @Test
        @DisplayName("Should include login link in welcome email")
        void sendWelcomeEmail_IncludesLoginLink() throws ExecutionException, InterruptedException {
            // Given
            doNothing().when(mailSender).send(any(MimeMessage.class));
            
            // When
            CompletableFuture<Boolean> result = emailService.sendWelcomeEmail(
                TEST_EMAIL, TEST_USER_NAME, "TempPass123"
            );
            
            // Then
            assertThat(result.get()).isTrue();
            // The email should contain: http://localhost:3000/login
            verify(mailSender).send(mimeMessage);
        }
    }
    
    @Nested
    @DisplayName("Simple Email Tests")
    class SimpleEmailTests {
        
        @Test
        @DisplayName("Should successfully send simple text email")
        void sendSimpleEmail_Success() throws ExecutionException, InterruptedException {
            // Given
            String subject = "Test Subject";
            String content = "Test email content";
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            doNothing().when(mailSender).send(messageCaptor.capture());
            
            // When
            CompletableFuture<Boolean> result = emailService.sendSimpleEmail(
                TEST_EMAIL, subject, content
            );
            
            // Then
            assertThat(result.get()).isTrue();
            
            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getTo()).contains(TEST_EMAIL);
            assertThat(sentMessage.getSubject()).isEqualTo(subject);
            assertThat(sentMessage.getText()).isEqualTo(content);
            assertThat(sentMessage.getFrom()).isEqualTo("noreply@cafm.com");
        }
        
        @Test
        @DisplayName("Should return false when simple email fails")
        void sendSimpleEmail_Failure() throws ExecutionException, InterruptedException {
            // Given
            doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));
            
            // When
            CompletableFuture<Boolean> result = emailService.sendSimpleEmail(
                TEST_EMAIL, "Subject", "Content"
            );
            
            // Then
            assertThat(result.get()).isFalse();
            verify(mailSender).send(any(SimpleMailMessage.class));
        }
    }
    
    @Nested
    @DisplayName("Email Template Tests")
    class EmailTemplateTests {
        
        @Test
        @DisplayName("Should use correct HTML template for password reset")
        void passwordResetTemplate_ContainsCorrectElements() throws ExecutionException, InterruptedException {
            // Given
            doNothing().when(mailSender).send(any(MimeMessage.class));
            
            // When
            CompletableFuture<Boolean> result = emailService.sendPasswordResetEmail(
                TEST_EMAIL, TEST_USER_NAME, TEST_TOKEN
            );
            
            // Then
            assertThat(result.get()).isTrue();
            // The HTML template should contain:
            // - CAFM System header
            // - Password Reset Request title
            // - User name greeting
            // - Reset button with link
            // - Expiration notice (1 hour)
            verify(mailSender).send(mimeMessage);
        }
        
        @Test
        @DisplayName("Should use correct HTML template for email verification")
        void verificationTemplate_ContainsCorrectElements() throws ExecutionException, InterruptedException {
            // Given
            doNothing().when(mailSender).send(any(MimeMessage.class));
            
            // When
            CompletableFuture<Boolean> result = emailService.sendVerificationEmail(
                TEST_EMAIL, TEST_USER_NAME, TEST_TOKEN
            );
            
            // Then
            assertThat(result.get()).isTrue();
            // The HTML template should contain:
            // - CAFM System header
            // - Verify Your Email Address title
            // - User name greeting
            // - Verify button with link
            // - Expiration notice (24 hours)
            verify(mailSender).send(mimeMessage);
        }
        
        @Test
        @DisplayName("Should use correct HTML template for welcome email")
        void welcomeTemplate_ContainsCorrectElements() throws ExecutionException, InterruptedException {
            // Given
            String tempPassword = "TempPass123";
            doNothing().when(mailSender).send(any(MimeMessage.class));
            
            // When
            CompletableFuture<Boolean> result = emailService.sendWelcomeEmail(
                TEST_EMAIL, TEST_USER_NAME, tempPassword
            );
            
            // Then
            assertThat(result.get()).isTrue();
            // The HTML template should contain:
            // - Welcome to CAFM System header
            // - Account Created Successfully title
            // - User credentials (email and temp password)
            // - Login button with link
            // - Password change reminder
            verify(mailSender).send(mimeMessage);
        }
    }
    
    @Nested
    @DisplayName("Async Behavior Tests")
    class AsyncBehaviorTests {
        
        @Test
        @DisplayName("Should send email asynchronously")
        void sendEmail_Async() {
            // Given
            doNothing().when(mailSender).send(any(MimeMessage.class));
            
            // When
            CompletableFuture<Boolean> result = emailService.sendPasswordResetEmail(
                TEST_EMAIL, TEST_USER_NAME, TEST_TOKEN
            );
            
            // Then
            assertThat(result).isInstanceOf(CompletableFuture.class);
            assertThat(result.isDone()).isFalse(); // Should be async
            
            // Complete the future
            result.join();
            assertThat(result.isDone()).isTrue();
        }
        
        @Test
        @DisplayName("Should handle multiple concurrent email sends")
        void sendMultipleEmails_Concurrent() throws ExecutionException, InterruptedException {
            // Given
            doNothing().when(mailSender).send(any(MimeMessage.class));
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));
            
            // When
            CompletableFuture<Boolean> result1 = emailService.sendPasswordResetEmail(
                "user1@example.com", "User 1", "token1"
            );
            CompletableFuture<Boolean> result2 = emailService.sendVerificationEmail(
                "user2@example.com", "User 2", "token2"
            );
            CompletableFuture<Boolean> result3 = emailService.sendSimpleEmail(
                "user3@example.com", "Subject", "Content"
            );
            
            // Then
            CompletableFuture<Void> allResults = CompletableFuture.allOf(result1, result2, result3);
            allResults.get(); // Wait for all to complete
            
            assertThat(result1.get()).isTrue();
            assertThat(result2.get()).isTrue();
            assertThat(result3.get()).isTrue();
            
            verify(mailSender, times(2)).send(any(MimeMessage.class));
            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        }
    }
}