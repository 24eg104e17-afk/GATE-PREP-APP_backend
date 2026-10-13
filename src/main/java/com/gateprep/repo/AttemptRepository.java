package com.gateprep.repo;

import com.gateprep.model.Attempt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    List<Attempt> findByUserIdAndSubmittedAtIsNotNullOrderByIdDesc(Long userId);
    Optional<Attempt> findByIdAndUserId(Long id, Long userId);
    Optional<Attempt> findByIdAndUserIdAndSubmittedAtIsNotNull(Long id, Long userId);
}
