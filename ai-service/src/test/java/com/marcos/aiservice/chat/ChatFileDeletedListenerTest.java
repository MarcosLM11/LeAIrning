package com.marcos.aiservice.chat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import com.marcos.aiservice.FileDeletedEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatFileDeletedListenerTest {

    @Test
    void deletesTheSourcesOfTheDeletedFile() {
        var repository = mock(AnswerSourceRepository.class);
        var fileId = UUID.randomUUID();

        new ChatFileDeletedListener(repository).onFileDeleted(new FileDeletedEvent(fileId));

        verify(repository).deleteByFileId(fileId);
    }
}
