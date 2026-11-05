package com.marcos.aiservice.chat;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface AnswerSourceRepository extends JpaRepository<AnswerSource, UUID> {

    List<AnswerSource> findByEventIdInOrderByFilenameAscPageAsc(Collection<String> eventIds);

    @Transactional
    void deleteBySessionId(String sessionId);

    @Transactional
    void deleteByFileId(UUID fileId);
}
