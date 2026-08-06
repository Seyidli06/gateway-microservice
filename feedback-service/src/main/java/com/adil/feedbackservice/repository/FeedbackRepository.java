package com.adil.feedbackservice.repository;

import com.adil.feedbackservice.model.Feedback;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class FeedbackRepository {

    private final Map<Long, Feedback> feedbackStorage =
            new ConcurrentHashMap<>();

    private final AtomicLong idGenerator = new AtomicLong(0);

    public Feedback save(Feedback feedback) {
        if (feedback.getId() == null) {
            feedback.setId(idGenerator.incrementAndGet());
        }

        feedbackStorage.put(feedback.getId(), feedback);

        return feedback;
    }

    public List<Feedback> findAll() {
        return feedbackStorage.values()
                .stream()
                .sorted(Comparator.comparing(Feedback::getId))
                .toList();
    }
}