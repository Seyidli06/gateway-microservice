package com.adil.profileservice.exception;

public class DuplicateProfileEmailException extends RuntimeException {

    public DuplicateProfileEmailException(String email) {
        super("Profile with email '%s' already exists".formatted(email));
    }
}