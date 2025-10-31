package com.study.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
// security/PasswordHasher.java
public interface PasswordHasher {
    String hash(String raw);
    boolean matches(String raw, String hash);
}



