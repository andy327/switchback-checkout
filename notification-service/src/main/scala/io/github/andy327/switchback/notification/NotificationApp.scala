package io.github.andy327.switchback.notification

import cats.effect.{ExitCode, IO, IOApp}

/** notification-service - a pure Kafka consumer (no REST).
  *
  * Responsibilities:
  *   - consume the checkout.events topic with fs2-kafka;
  *   - act only on the terminal events (OrderPlaced -> confirmation, OrderFailed -> failure notice), ignoring the
  *     per-step audit events on the same stream;
  *   - "sending" is just a logged side effect in this toy - the point is that it runs fully decoupled from the order
  *     response and can lag or restart without affecting checkout.
  */
object NotificationApp extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    IO.println("notification-service: skeleton entry point (consumer not yet wired)")
      .as(ExitCode.Success)
}
