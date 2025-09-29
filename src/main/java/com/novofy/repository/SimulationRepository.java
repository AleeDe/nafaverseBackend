package com.novofy.repository;

import com.novofy.model.Simulation;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SimulationRepository extends MongoRepository<Simulation, ObjectId> {
}