package com.study.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class BCryptPasswordHasher implements PasswordHasher {
    public String hash(String raw){
        return BCrypt.withDefaults().hashToString(10, raw.toCharArray()); // cost 10~12
    }
    public boolean matches(String raw, String hash){
        return BCrypt.verifyer().verify(raw.toCharArray(), hash).verified;
    }
}
