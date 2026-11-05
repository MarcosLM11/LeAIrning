package com.marcos.ingestionservice;

import java.util.UUID;

public record FileIngestedEvent(
        UUID fileId,
        boolean success
) {}