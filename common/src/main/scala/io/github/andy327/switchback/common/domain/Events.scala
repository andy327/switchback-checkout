package io.github.andy327.switchback.common.domain

import java.time.Instant

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

/** The Kafka event schema.
  *
  * In the orchestrated model these events are records of decided facts - order-service publishes them AFTER the
  * synchronous flow resolves, so consumers can only react, never steer the outcome.
  *
  * Two kinds live on the same `checkout.events` topic:
  *   - terminal events (OrderPlaced / OrderFailed) - what notification-service acts on;
  *   - per-step events (StockReserved / PaymentCharged / ...) - the richer trail audit-log-service folds into its
  *     immutable log.
  *
  * Both consumers subscribe to the same stream under their own consumer groups and each keeps only what it cares
  * about, so they run independently and neither is aware of the other.
  */
sealed trait CheckoutEvent {
  def orderId: String
  def at: Instant
}

object CheckoutEvent {
  // --- per-step (audit trail) ---
  final case class StockReserved(orderId: String, reservationId: String, at: Instant) extends CheckoutEvent
  final case class StockReleased(orderId: String, reservationId: String, at: Instant) extends CheckoutEvent
  final case class PaymentCharged(orderId: String, paymentId: String, amount: Money, at: Instant) extends CheckoutEvent
  final case class PaymentDeclined(orderId: String, reason: String, at: Instant) extends CheckoutEvent

  // --- terminal (order outcome) ---
  final case class OrderPlaced(orderId: String, customerId: String, amount: Money, at: Instant) extends CheckoutEvent
  final case class OrderFailed(orderId: String, reason: String, at: Instant) extends CheckoutEvent

  implicit val codec: Codec[CheckoutEvent] = deriveCodec
}
