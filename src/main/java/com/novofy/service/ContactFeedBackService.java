package com.novofy.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.novofy.model.ContactFeedback;
import com.novofy.repository.ContactFeedBackRepository;


@Service
public class ContactFeedBackService {

    @Autowired
    private ContactFeedBackRepository contactFeedBackRepository;


    public void saveFeedback(ContactFeedback feedback) {
        System.out.println("Saving feedback: " + feedback);
        contactFeedBackRepository.save(feedback);
    }
}
