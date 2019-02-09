package io.github.testra.java.client;

class TestraApiClientException extends RuntimeException {

  TestraApiClientException(Throwable cause) {
    super(cause);
  }

  TestraApiClientException(String message) {
    super(message);
  }
}
