package com.shiningpr1sm.feedbackbot.repository;

import com.shiningpr1sm.feedbackbot.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByChatId(Long chatId);
}