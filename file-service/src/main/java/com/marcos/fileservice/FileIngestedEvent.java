package com.marcos.fileservice;

import java.util.UUID;

public record FileIngestedEvent(UUID fileId, boolean success) {}
