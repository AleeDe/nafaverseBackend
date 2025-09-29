package com.novofy.dto;

import lombok.Data;

@Data
public class CreateGoalRequest {
    private String goalName;
    private String city;
    private int targetYear;
    private String prompt; // optional user instructions
}