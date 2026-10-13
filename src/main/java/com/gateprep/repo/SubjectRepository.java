package com.gateprep.repo;

import com.gateprep.model.Subject;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findByName(String name);
    List<Subject> findAllByOrderBySortOrderAsc();
}
