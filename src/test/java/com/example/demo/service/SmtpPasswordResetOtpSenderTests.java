package com.example.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SmtpPasswordResetOtpSenderTests {

    @Test
    void readinessRejectsMissingMailConfigurationWithoutARecipientLookup() {
        SmtpPasswordResetOtpSender sender = senderWith(null);

        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                sender::checkAvailable);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, failure.getStatusCode());
        assertEquals("Password reset email is not configured", failure.getReason());
        assertNull(failure.getCause());
    }

    @Test
    void readinessWithConfiguredSenderDoesNotSendEmail() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpPasswordResetOtpSender sender = senderWith(mailSender);

        sender.checkAvailable();

        verifyNoInteractions(mailSender);
    }

    @Test
    void sendsCodeToOnlyTheRequestedEmailWithExpiry() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpPasswordResetOtpSender sender = senderWith(mailSender);

        sender.send("alice@example.test", "012345");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertEquals("no-reply@example.test", message.getFrom());
        assertArrayEquals(new String[]{"alice@example.test"}, message.getTo());
        assertNull(message.getCc());
        assertNull(message.getBcc());
        assertEquals("Your password reset code", message.getSubject());
        assertTrue(message.getText().contains("012345"));
        assertTrue(message.getText().contains("expires in 5 minutes"));
        assertTrue(message.getText().contains("only be used once"));
    }

    @Test
    void missingMailConfigurationDoesNotPreventConstructionButRejectsSending() {
        SmtpPasswordResetOtpSender sender = senderWith(null);

        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                () -> sender.send("alice@example.test", "012345"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, failure.getStatusCode());
        assertEquals("Password reset email is not configured", failure.getReason());
        assertNull(failure.getCause());
    }

    @Test
    void mailFailureReturnsGenericErrorWithoutProviderDetailsOrCode() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("Provider details: alice@example.test code 012345"))
                .when(mailSender).send(any(SimpleMailMessage.class));
        SmtpPasswordResetOtpSender sender = senderWith(mailSender);

        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                () -> sender.send("alice@example.test", "012345"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, failure.getStatusCode());
        assertEquals("Password reset email could not be sent", failure.getReason());
        assertNull(failure.getCause());
    }

    private SmtpPasswordResetOtpSender senderWith(JavaMailSender mailSender) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        if (mailSender != null) {
            beanFactory.addBean("mailSender", mailSender);
        }
        return new SmtpPasswordResetOtpSender(
                beanFactory.getBeanProvider(JavaMailSender.class), "no-reply@example.test");
    }
}
