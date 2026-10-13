package com.gateprep.repo;

import com.gateprep.model.AttemptQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptQuestionRepository extends JpaRepository<AttemptQuestion, Long> {
    List<AttemptQuestion> findByAttemptIdOrderBySortOrder(Long attemptId);
    boolean existsByQuestionId(Long questionId);
}
