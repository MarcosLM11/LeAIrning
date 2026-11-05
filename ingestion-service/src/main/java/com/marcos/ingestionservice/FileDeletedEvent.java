package com.marcos.ingestionservice;

import java.util.UUID;

public record FileDeletedEvent(UUID fileId) {}
