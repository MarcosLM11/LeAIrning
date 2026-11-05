package com.marcos.aiservice.quizz;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface QuizzRepository extends JpaRepository<Quizz, UUID> {

    List<Quizz> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Quizz> findByFileId(UUID fileId);
}
