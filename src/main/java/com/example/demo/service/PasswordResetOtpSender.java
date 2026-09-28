package com.example.demo.service;

public interface PasswordResetOtpSender {

    default void checkAvailable() {
    }

    void send(String email, String code);
}
