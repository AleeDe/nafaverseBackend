package com.novofy.model;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private ObjectId id;

    private String username;

    @JsonIgnore
    private String password;

    private String email;

    private String phoneNumber;

    private String role;

    private String city;

    private String province;

    private String country;

    @DBRef(lazy = true)
    private List<Goal> goals = new ArrayList<>();
    
    @DBRef(lazy = true)
    private EducationProgress educationProgress;

    private int totalCoins;

    private LocalDate dob;

    private LocalDate createdAt;

    private String address;

    private String profilePictureUrl;

    private String provider; // e.g., "google" or "local"

}
