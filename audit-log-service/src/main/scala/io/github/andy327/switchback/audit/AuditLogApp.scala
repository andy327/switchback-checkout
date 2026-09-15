package io.github.andy327.switchback.audit

import cats.effect.{ExitCode, IO, IOApp}

/** audit-log-service - a second, independent pure Kafka consumer (no REST).
  *
  * Responsibilities:
  *   - consume the SAME checkout.events topic as notification-service, under its own consumer group;
  *   - append EVERY event (per-step and terminal) to an immutable, ordered log - an in-memory / append-only-file trail
  *     in this toy;
  *   - demonstrate fan-out: two subscribers reading one stream, each at its own offset, neither aware of the other.
  */
object AuditLogApp extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    IO.println("audit-log-service: skeleton entry point (consumer not yet wired)")
      .as(ExitCode.Success)
}
