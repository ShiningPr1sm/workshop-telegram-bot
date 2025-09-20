package com.shiningpr1sm.feedbackbot.controller;

import com.shiningpr1sm.feedbackbot.model.EmployeeRole;
import com.shiningpr1sm.feedbackbot.model.Feedback;
import com.shiningpr1sm.feedbackbot.model.FeedbackSentiment;
import com.shiningpr1sm.feedbackbot.repository.FeedbackRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/*
 * rest controller
 */
@RestController
@RequestMapping("/admin/api/feedbacks") // base path
public class AdminController {

    private final FeedbackRepository feedbackRepository;

    public AdminController(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    /*
     * Parameters:
     * - branch: Filter by branch name.
     * - role: Filter by employee role ("МЕХАНІК", "ЕЛЕКТРИК", "МЕНЕДЖЕР").
     * - criticality: Filter by criticality level (1-5).
     * - sentiment: Filter by sentiment (Нейтральний, Позитивний, Негативний).
     */
    @GetMapping
    public ResponseEntity<List<Feedback>> getFilteredFeedback(
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer criticality,
            @RequestParam(required = false) String sentiment
    ) {
        List<Feedback> feedbacks;

        if (Objects.nonNull(branch) && Objects.nonNull(role) && Objects.nonNull(criticality) && Objects.nonNull(sentiment)) {
            try {
                EmployeeRole employeeRole = EmployeeRole.valueOf(role.toUpperCase());
                FeedbackSentiment feedbackSentiment = FeedbackSentiment.valueOf(sentiment.toUpperCase());
                feedbacks = feedbackRepository.findByBranchAndEmployeeRoleAndCriticalityLevelAndSentiment(
                        branch, employeeRole, criticality, feedbackSentiment);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else if (Objects.nonNull(branch) && Objects.nonNull(role) && Objects.nonNull(criticality)) {
            try {
                EmployeeRole employeeRole = EmployeeRole.valueOf(role.toUpperCase());
                feedbacks = feedbackRepository.findByBranchAndEmployeeRoleAndCriticalityLevel(
                        branch, employeeRole, criticality);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else if (Objects.nonNull(branch) && Objects.nonNull(role)) {
            try {
                EmployeeRole employeeRole = EmployeeRole.valueOf(role.toUpperCase());
                feedbacks = feedbackRepository.findByBranchAndEmployeeRole(branch, employeeRole);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else if (Objects.nonNull(branch) && Objects.nonNull(criticality)) {
            feedbacks = feedbackRepository.findByBranchAndCriticalityLevel(branch, criticality);
        } else if (Objects.nonNull(role) && Objects.nonNull(criticality)) {
            try {
                EmployeeRole employeeRole = EmployeeRole.valueOf(role.toUpperCase());
                feedbacks = feedbackRepository.findByEmployeeRoleAndCriticalityLevel(employeeRole, criticality);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else if (Objects.nonNull(branch)) {
            feedbacks = feedbackRepository.findByBranch(branch);
        } else if (Objects.nonNull(role)) {
            try {
                EmployeeRole employeeRole = EmployeeRole.valueOf(role.toUpperCase());
                feedbacks = feedbackRepository.findByEmployeeRole(employeeRole);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else if (Objects.nonNull(criticality)) {
            feedbacks = feedbackRepository.findByCriticalityLevel(criticality);
        } else if (Objects.nonNull(sentiment)) {
            try {
                FeedbackSentiment feedbackSentiment = FeedbackSentiment.valueOf(sentiment.toUpperCase());
                feedbacks = feedbackRepository.findBySentiment(feedbackSentiment);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            feedbacks = feedbackRepository.findAll();
        }

        if (feedbacks.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(feedbacks);
    }
}