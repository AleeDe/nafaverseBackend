package com.novofy.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.novofy.model.ContactFeedback;

public interface ContactFeedBackRepository extends MongoRepository<ContactFeedback, ObjectId> {
}
