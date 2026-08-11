package com.adil.profileservice.exception;

public class DuplicateProfileEmailException extends RuntimeException {
  public DuplicateProfileEmailException(String message) {
    super(message);
  }
}
