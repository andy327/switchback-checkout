package io.github.andy327.switchback.common.domain

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

/** The shared domain model for the checkout flow.
  *
  * Kept deliberately small: enough to carry a full order through reserve -> charge -> confirm/compensate, and no more.
  * All types are plain data with Circe codecs so they can cross both the REST edges and the Kafka topic.
  */

/** Money as integer minor units to avoid floating-point rounding. */
final case class Money(amountCents: Long, currency: String = "USD")
object Money {
  implicit val codec: Codec[Money] = deriveCodec
}

/** A single line in an order: what, and how many. */
final case class LineItem(sku: String, quantity: Int)
object LineItem {
  implicit val codec: Codec[LineItem] = deriveCodec
}

/** An opaque reference to a payment method.
  *
  * A token is all order-service ever forwards to payment-service.
  */
final case class CardRef(token: String)
object CardRef {
  implicit val codec: Codec[CardRef] = deriveCodec
}

/** The client's checkout request: what to buy, how much, and how to pay. */
final case class OrderRequest(customerId: String, items: List[LineItem], amount: Money, card: CardRef)
object OrderRequest {
  implicit val codec: Codec[OrderRequest] = deriveCodec
}

/** What order-service returns to the client on a successful checkout.
  *
  * Only the success path produces this; a failed checkout is reported through the [[CheckoutError]] error channel, so
  * both ids are always present here.
  */
final case class OrderResult(orderId: String, reservationId: String, paymentId: String)
object OrderResult {
  implicit val codec: Codec[OrderResult] = deriveCodec
}

// --- inventory-service ---

final case class ReserveRequest(orderId: String, items: List[LineItem])
object ReserveRequest {
  implicit val codec: Codec[ReserveRequest] = deriveCodec
}

final case class ReserveResponse(reservationId: String)
object ReserveResponse {
  implicit val codec: Codec[ReserveResponse] = deriveCodec
}

final case class ReleaseRequest(reservationId: String)
object ReleaseRequest {
  implicit val codec: Codec[ReleaseRequest] = deriveCodec
}

// --- payment-service ---

final case class ChargeRequest(orderId: String, amount: Money, card: CardRef)
object ChargeRequest {
  implicit val codec: Codec[ChargeRequest] = deriveCodec
}

final case class ChargeResponse(paymentId: String)
object ChargeResponse {
  implicit val codec: Codec[ChargeResponse] = deriveCodec
}

final case class RefundRequest(paymentId: String)
object RefundRequest {
  implicit val codec: Codec[RefundRequest] = deriveCodec
}

/** The error vocabulary shared across services.
  *
  * Each maps to a specific HTTP status at the REST edge so callers can branch on a typed failure
  * rather than a bare status code.
  */
sealed trait CheckoutError {
  def message: String
}
object CheckoutError {
  final case class OutOfStock(message: String) extends CheckoutError
  final case class PaymentDeclined(message: String) extends CheckoutError
  final case class NotFound(message: String) extends CheckoutError
  final case class Unavailable(message: String) extends CheckoutError

  implicit val codec: Codec[CheckoutError] = deriveCodec
}
