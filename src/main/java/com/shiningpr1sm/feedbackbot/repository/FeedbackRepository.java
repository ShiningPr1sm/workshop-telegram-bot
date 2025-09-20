package com.shiningpr1sm.feedbackbot.repository;

import com.shiningpr1sm.feedbackbot.model.EmployeeRole;
import com.shiningpr1sm.feedbackbot.model.Feedback;
import com.shiningpr1sm.feedbackbot.model.FeedbackSentiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByBranch(String branch);

    List<Feedback> findByEmployeeRole(EmployeeRole employeeRole);

    List<Feedback> findByCriticalityLevel(Integer criticalityLevel);

    List<Feedback> findBySentiment(FeedbackSentiment sentiment);

    List<Feedback> findByBranchAndEmployeeRole(String branch, EmployeeRole employeeRole);

    List<Feedback> findByBranchAndCriticalityLevel(String branch, Integer criticalityLevel);

    List<Feedback> findByEmployeeRoleAndCriticalityLevel(EmployeeRole employeeRole, Integer criticalityLevel);

    List<Feedback> findByBranchAndEmployeeRoleAndCriticalityLevel(String branch, EmployeeRole employeeRole, Integer criticalityLevel);

    List<Feedback> findByBranchAndEmployeeRoleAndCriticalityLevelAndSentiment(String branch, EmployeeRole employeeRole, Integer criticalityLevel, FeedbackSentiment sentiment);
}