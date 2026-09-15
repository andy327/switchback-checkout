package io.github.andy327.switchback.order

import cats.effect.{ExitCode, IO, IOApp}

/** order-service - the orchestrator and client-facing entry point.
  *
  * Responsibilities:
  *   - expose POST /orders (Endpoints.placeOrder) via tapir-http4s-server on Ember;
  *   - call inventory-service (reserve) then payment-service (charge) synchronously, each wrapped in retries
  *     (cats-retry) and a fail-fast guard around the downstream call;
  *   - on payment failure, issue the compensating inventory/release call, then respond to the client;
  *   - publish per-step + terminal CheckoutEvents to Kafka (fs2-kafka) once the outcome is decided.
  */
object OrderServer extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    IO.println("order-service: skeleton entry point (orchestration not yet wired)")
      .as(ExitCode.Success)
}
