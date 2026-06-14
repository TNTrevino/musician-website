package com.project.backend.exceptions;

/**
 * Thrown when a Stripe webhook event is permanently unprocessable — e.g. missing or malformed
 * piece_ids metadata, unknown piece ids, or an unpaid session. Callers that catch this exception
 * should acknowledge the event (return HTTP 200) so Stripe does NOT retry; retrying would never
 * succeed for these cases and risks endpoint disablement after ~3 days of failures.
 *
 * <p>Contrast with transient failures (DB down, Stripe API error, IO) which should remain
 * retryable — throw those as plain {@link RuntimeException} or {@link IllegalStateException}.
 */
public class UnprocessableEventException extends RuntimeException {

  public UnprocessableEventException(String message) {
    super(message);
  }

  public UnprocessableEventException(String message, Throwable cause) {
    super(message, cause);
  }
}
