package com.example.demo.respository;

import com.example.demo.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    @Query("SELECT r FROM IdempotencyRecord r WHERE r.actor.Id = :actorId AND r.operation = :operation AND r.requestKey = :requestKey")
    Optional<IdempotencyRecord> findByActorIdAndOperationAndRequestKey(
            @Param("actorId") Long actorId,
            @Param("operation") String operation,
            @Param("requestKey") UUID requestKey
    );
}
