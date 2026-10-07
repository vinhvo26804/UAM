package com.example.demo.service;

import com.example.demo.entity.IdempotencyRecord;
import com.example.demo.entity.Space;
import com.example.demo.entity.User;
import com.example.demo.respository.IdempotencyRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    public String computeHash(String canonicalPayload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public Optional<IdempotencyRecord> findRecord(User actor, String operation, UUID requestKey) {
        return idempotencyRecordRepository.findByActorIdAndOperationAndRequestKey(actor.getId(), operation, requestKey);
    }

    public void validateNotReusedWithDifferentPayload(IdempotencyRecord record, String currentHash) {
        if (!record.getRequestHash().equals(currentHash)) {
            throw new IssueOperationException(
                    HttpStatus.CONFLICT,
                    "IDEMPOTENCY_KEY_REUSED",
                    "Idempotency-Key đã được sử dụng cho một yêu cầu có nội dung khác"
            );
        }
    }

    public void saveRecord(User actor, Space space, String operation, UUID requestKey, String requestHash, int status, Object responseBody, Long resourceId) {
        try {
            String json = responseBody != null ? objectMapper.writeValueAsString(responseBody) : null;
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .actor(actor)
                    .space(space)
                    .operation(operation)
                    .requestKey(requestKey)
                    .requestHash(requestHash)
                    .responseStatus(status)
                    .responseBody(json)
                    .resourceId(resourceId)
                    .completedAt(LocalDateTime.now())
                    .build();
            idempotencyRecordRepository.save(record);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize idempotency response body", e);
        }
    }
}
