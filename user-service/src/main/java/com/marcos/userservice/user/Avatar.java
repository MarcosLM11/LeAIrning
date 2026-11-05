package com.marcos.userservice.user;

import java.io.InputStream;

record Avatar(
        String contentType,
        InputStream body
) {}