package com.gateprep.repo;

import com.gateprep.model.TopicProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicProgressRepository extends JpaRepository<TopicProgress, Long> {
    List<TopicProgress> findByUserId(Long userId);
    Optional<TopicProgress> findByUserIdAndTopicId(Long userId, Long topicId);
}
