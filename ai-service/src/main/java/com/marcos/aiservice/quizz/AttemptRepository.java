package com.marcos.aiservice.quizz;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface AttemptRepository extends JpaRepository<Attempt, UUID> {

    interface LatestScore {
        UUID getQuizzId();
        int getScore();
    }

    @Query("""
            select a.quizzId as quizzId, a.score as score from Attempt a
            where a.userId = :userId
              and a.createdAt = (select max(b.createdAt) from Attempt b where b.quizzId = a.quizzId and b.userId = a.userId)
            """)
    List<LatestScore> findLatestScores(UUID userId);
    void deleteByQuizzIdIn(Collection<UUID> quizzIds);
}
