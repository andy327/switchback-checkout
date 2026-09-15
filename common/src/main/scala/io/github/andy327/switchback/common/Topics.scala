package io.github.andy327.switchback.common

/** Kafka topic names, kept in one place so producer and consumers can't drift.
  *
  * A single topic carries the whole [[io.github.andy327.switchback.common.domain.CheckoutEvent]] ADT;
  * notification-service and audit-log-service each subscribe independently and filter for the event types they care
  * about. (An alternative topic-per-concern layout is possible if the event vocabulary grows.)
  */
object Topics {
  val CheckoutEvents = "checkout.events"

  /** All topics the demo creates on startup. */
  val All: List[String] = List(CheckoutEvents)
}
