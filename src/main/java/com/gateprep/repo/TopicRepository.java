package com.gateprep.repo;

import com.gateprep.model.Topic;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findBySubjectIdOrderBySortOrderAsc(Long subjectId);
    Optional<Topic> findBySubjectIdAndName(Long subjectId, String name);
    Optional<Topic> findBySubjectNameAndName(String subjectName, String name);
}
