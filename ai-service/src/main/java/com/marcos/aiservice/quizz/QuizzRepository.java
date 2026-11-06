package com.marcos.aiservice.quizz;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface QuizzRepository extends JpaRepository<Quizz, UUID> {

    Page<Quizz> findByUserId(UUID userId, Pageable pageable);
    List<Quizz> findByFileId(UUID fileId);
}
