package io.github.andy327.switchback.payment

import cats.effect.{ExitCode, IO, IOApp}

/** payment-service - charge / refund against an in-memory ledger.
  *
  * Responsibilities:
  *   - serve Endpoints.charge and Endpoints.refund via tapir-http4s-server on Ember;
  *   - a deterministic decline rule (e.g. a sentinel card token or amount) so demos can reproduce the payment-failure
  *     path on demand, driving order-service's compensating release;
  *   - an optional injected latency/failure knob to exercise order-service's retries and fail-fast guard.
  */
object PaymentServer extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    IO.println("payment-service: skeleton entry point (REST not yet wired)")
      .as(ExitCode.Success)
}
