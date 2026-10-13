package com.gateprep.repo;

import com.gateprep.model.Question;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    boolean existsByTopicIdAndText(Long topicId, String text);
    long countByTopicIdAndActiveTrue(Long topicId);
    List<Question> findByActiveTrue();
    List<Question> findByTopicIdAndActiveTrue(Long topicId);
    List<Question> findByTopicSubjectIdAndActiveTrue(Long subjectId);
}
