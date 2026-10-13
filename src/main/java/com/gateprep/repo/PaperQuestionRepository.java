package com.gateprep.repo;

import com.gateprep.model.PaperQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperQuestionRepository extends JpaRepository<PaperQuestion, Long> {
    List<PaperQuestion> findByPaperIdOrderBySortOrder(Long paperId);
    long countByPaperId(Long paperId);
    boolean existsByQuestionId(Long questionId);
    void deleteByPaperId(Long paperId);
}
