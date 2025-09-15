package com.novofy.repository;

import com.novofy.model.PasswordResetToken;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, ObjectId> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByToken(String token);
}