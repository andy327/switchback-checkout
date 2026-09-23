package io.github.andy327.switchback.common

import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

import io.github.andy327.switchback.common.domain.CheckoutError._
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
  * The summaries, descriptions, tags, and examples attached below feed the Swagger UI each REST service mounts. All of
  * that documentation metadata lives here so the domain model stays free of any Tapir dependency.
  */
object Endpoints {

  /** Error channel shared by every service call.
    *
    * A Tapir `oneOf` maps each [[CheckoutError]] variant to its own status, so the status code is the discriminator and
    * the body carries only the variant's `message`. A type-safe client reconstructs the exact typed error from the
    * status it receives.
    */
  private val errors: EndpointOutput[CheckoutError] =
    oneOf[CheckoutError](
      oneOfVariant(
        StatusCode.Conflict,
        jsonBody[OutOfStock].description("A SKU could not cover the requested quantity")
      ),
      oneOfVariant(
        StatusCode.PaymentRequired,
        jsonBody[PaymentDeclined].description("The card was declined")
      ),
      oneOfVariant(
        StatusCode.NotFound,
        jsonBody[NotFound].description("The referenced reservation or payment does not exist")
      ),
      oneOfVariant(
        StatusCode.ServiceUnavailable,
        jsonBody[Unavailable].description("A downstream service was unavailable")
      )
    )

  /** Base endpoint carrying the shared typed error channel. */
  private val base: Endpoint[Unit, Unit, CheckoutError, Unit, Any] =
    endpoint.errorOut(errors)

  /** Liveness probe, present on every REST service. */
  val health: PublicEndpoint[Unit, Unit, String, Any] =
    endpoint.get
      .in("health")
      .out(stringBody)
      .summary("Liveness probe")
      .description("Returns 200 with a short status string while the service is up.")
      .tag("health")

  // --- inventory-service ---
  val reserve: Endpoint[Unit, ReserveRequest, CheckoutError, ReserveResponse, Any] =
    base.post
      .in("inventory" / "reserve")
      .in(jsonBody[ReserveRequest].example(ReserveRequest("order-123", List(LineItem("widget", 2)))))
      .out(jsonBody[ReserveResponse])
      .summary("Reserve stock for an order")
      .description("Holds stock for each line item. Fails with 409 when a SKU cannot cover the requested quantity.")
      .tag("inventory")

  val release: Endpoint[Unit, ReleaseRequest, CheckoutError, Unit, Any] =
    base.post
      .in("inventory" / "release")
      .in(jsonBody[ReleaseRequest].example(ReleaseRequest("rsv-123")))
      .out(statusCode(StatusCode.NoContent))
      .summary("Release a stock reservation")
      .description("Undoes a reservation. Idempotent: releasing an unknown or already-released id still succeeds.")
      .tag("inventory")

  // --- payment-service ---
  val charge: Endpoint[Unit, ChargeRequest, CheckoutError, ChargeResponse, Any] =
    base.post
      .in("payments" / "charge")
      .in(jsonBody[ChargeRequest].example(ChargeRequest("order-123", Money(1998, "USD"), CardRef("tok_ok"))))
      .out(jsonBody[ChargeResponse])
      .summary("Charge a card for an order")
      .description("Charges the card for the given amount. Fails with 402 when the card is declined.")
      .tag("payments")

  val refund: Endpoint[Unit, RefundRequest, CheckoutError, Unit, Any] =
    base.post
      .in("payments" / "refund")
      .in(jsonBody[RefundRequest].example(RefundRequest("pay-123")))
      .out(statusCode(StatusCode.NoContent))
      .summary("Refund a payment")
      .description("Reverses a prior charge by payment id.")
      .tag("payments")

  // --- order-service (client-facing entry point) ---
  val placeOrder: Endpoint[Unit, OrderRequest, CheckoutError, OrderResult, Any] =
    base.post
      .in("orders")
      .in(
        jsonBody[OrderRequest]
          .example(OrderRequest("cust-1", List(LineItem("widget", 2)), Money(1998, "USD"), CardRef("tok_ok")))
      )
      .out(statusCode(StatusCode.Created).and(jsonBody[OrderResult]))
      .summary("Place an order")
      .description(
        "Client-facing entry point. Reserves stock then charges payment; on a declined charge the reservation is " +
          "released and the call fails with 402."
      )
      .tag("orders")
}
