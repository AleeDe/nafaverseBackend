package com.novofy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.novofy.model.ContactFeedback;
import com.novofy.service.ContactFeedBackService;



@RestController
@RequestMapping("/api/contact-feedback")

public class ContactFeedBackController {

    @Autowired
    private ContactFeedBackService contactFeedBackService;

    @PostMapping("/submit")
    public ResponseEntity<?> submitFeedback(@RequestBody ContactFeedback feedback) {
        try {
            System.out.println("Received feedback: " + feedback);
            contactFeedBackService.saveFeedback(feedback);
            return ResponseEntity.ok("Feedback submitted successfully" + feedback);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Server error: " + e.getMessage());
        }
    }
}
