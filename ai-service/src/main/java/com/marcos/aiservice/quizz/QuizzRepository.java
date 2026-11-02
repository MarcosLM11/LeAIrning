package com.marcos.aiservice.quizz;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface QuizzRepository extends JpaRepository<Quizz, UUID> {
    List<Quizz> findByUserId(UUID userId);
}