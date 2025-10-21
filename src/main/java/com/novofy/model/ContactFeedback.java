package com.novofy.model;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "contactFeedback")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContactFeedback {
    private ObjectId id;
    private String name;
    private String email;
    private String message;

    
}
