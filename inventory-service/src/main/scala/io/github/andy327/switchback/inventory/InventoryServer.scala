package io.github.andy327.switchback.inventory

import cats.effect.{ExitCode, IO, IOApp}

/** inventory-service - reserve / release stock against an in-memory store.
  *
  * Responsibilities:
  *   - serve Endpoints.reserve and Endpoints.release via tapir-http4s-server on Ember;
  *   - hold stock levels + reservations in a Ref-backed store (no database);
  *   - reserve fails with CheckoutError.OutOfStock when a SKU can't cover the requested quantity;
  *   - release is idempotent so order-service can safely retry a compensating call.
  */
object InventoryServer extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    IO.println("inventory-service: skeleton entry point (REST not yet wired)")
      .as(ExitCode.Success)
}
