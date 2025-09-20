package com.shiningpr1sm.feedbackbot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long chatId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeRole employeeRole;

    @Column(nullable = false)
    private String branch;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    private FeedbackSentiment sentiment;

    private Integer criticalityLevel;

    @Column(columnDefinition = "TEXT")
    private String resolutionSuggestion;

    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    private boolean trelloCardCreated;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
        trelloCardCreated = false;
    }
}