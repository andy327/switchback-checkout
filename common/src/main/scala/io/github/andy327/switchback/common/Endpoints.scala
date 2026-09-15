package io.github.andy327.switchback.common

import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

import io.github.andy327.switchback.common.domain._

/** The REST contract, defined once as Tapir endpoint values.
  *
  * Each endpoint is interpreted twice:
  *
  *   - a SERVER route, by the service that owns it (tapir-http4s-server), and
  *   - a type-safe CLIENT, by order-service calling inventory/payment (tapir-sttp-client).
  *
  * Because both sides derive from the same value here, a contract change is a compile error in every caller - not a
  * runtime 400 discovered in production. That shared-definition property is the main reason this project uses Tapir
  * rather than hand-written http4s routes.
  *
  * NOTE (skeleton): the error output currently maps every CheckoutError to a single status. A later refinement will use
  * Tapir `oneOf` so each error variant carries its own status (OutOfStock -> 409, PaymentDeclined -> 402,
  * NotFound -> 404, Unavailable -> 503).
  */
object Endpoints {

  /** Base endpoint carrying the shared typed error channel. */
  private val base: Endpoint[Unit, Unit, CheckoutError, Unit, Any] =
    endpoint.errorOut(statusCode(StatusCode.UnprocessableEntity).and(jsonBody[CheckoutError]))

  /** Liveness probe, present on every REST service. */
  val health: PublicEndpoint[Unit, Unit, String, Any] =
    endpoint.get.in("health").out(stringBody)

  // --- inventory-service ---
  val reserve: Endpoint[Unit, ReserveRequest, CheckoutError, ReserveResponse, Any] =
    base.post.in("inventory" / "reserve").in(jsonBody[ReserveRequest]).out(jsonBody[ReserveResponse])

  val release: Endpoint[Unit, ReleaseRequest, CheckoutError, Unit, Any] =
    base.post.in("inventory" / "release").in(jsonBody[ReleaseRequest]).out(statusCode(StatusCode.NoContent))

  // --- payment-service ---
  val charge: Endpoint[Unit, ChargeRequest, CheckoutError, ChargeResponse, Any] =
    base.post.in("payments" / "charge").in(jsonBody[ChargeRequest]).out(jsonBody[ChargeResponse])

  val refund: Endpoint[Unit, RefundRequest, CheckoutError, Unit, Any] =
    base.post.in("payments" / "refund").in(jsonBody[RefundRequest]).out(statusCode(StatusCode.NoContent))

  // --- order-service (client-facing entry point) ---
  val placeOrder: Endpoint[Unit, OrderRequest, CheckoutError, OrderResult, Any] =
    base.post.in("orders").in(jsonBody[OrderRequest]).out(statusCode(StatusCode.Created).and(jsonBody[OrderResult]))
}
