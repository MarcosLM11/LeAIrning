package com.marcos.fileservice;

import java.util.UUID;

public record FileDeletedEvent(UUID fileId) {}
