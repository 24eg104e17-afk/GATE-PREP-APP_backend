package com.gateprep.repo;

import com.gateprep.model.Paper;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperRepository extends JpaRepository<Paper, Long> {
    List<Paper> findByUserIdOrderByIdDesc(Long userId);
    Optional<Paper> findByIdAndUserId(Long id, Long userId);
}
