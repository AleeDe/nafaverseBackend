package com.novofy.repository;


import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.novofy.model.User;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, ObjectId> {


    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);
}
