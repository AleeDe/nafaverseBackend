package com.novofy.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "SuggestedPlan")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SuggestedPlan {

    @Id
    private ObjectId id;

    private String planName;

    private String desc;

    private int roi;

}
