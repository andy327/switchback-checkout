package io.github.andy327.switchback.common.domain

import java.time.Instant

import io.circe.Codec
import io.circe.syntax._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Round-trip checks for the sealed ADTs whose derived JSON is a wire contract between separately-deployed services.
  *
  * The plain product DTOs are left to be exercised by the service and route tests; these two ADTs earn a dedicated
  * check because their encoding is a discriminated union keyed on the case name, so a rename or codec tweak can change
  * the on-the-wire shape and silently break a consumer.
  */
class CodecSpec extends AnyWordSpec with Matchers {

  /** Encodes `value` to JSON and decodes it back; the result should equal the original. */
  private def roundTrip[A: Codec](value: A): Unit =
    value.asJson.as[A] shouldBe Right(value)

  private val at = Instant.parse("2026-01-01T00:00:00Z")
  private val amount = Money(1998, "USD")

  "CheckoutEvent JSON" should {
    "round-trip every variant" in {
      val events: List[CheckoutEvent] = List(
        CheckoutEvent.StockReserved("o1", "r1", at),
        CheckoutEvent.StockReleased("o1", "r1", at),
        CheckoutEvent.PaymentCharged("o1", "p1", amount, at),
        CheckoutEvent.PaymentDeclined("o1", "insufficient funds", at),
        CheckoutEvent.OrderPlaced("o1", "c1", amount, at),
        CheckoutEvent.OrderFailed("o1", "payment declined", at)
      )
      events.foreach(roundTrip[CheckoutEvent])
    }
  }

  "CheckoutError JSON" should {
    "round-trip every variant" in {
      val errors: List[CheckoutError] = List(
        CheckoutError.OutOfStock("widget out of stock"),
        CheckoutError.PaymentDeclined("card declined"),
        CheckoutError.NotFound("no such reservation"),
        CheckoutError.Unavailable("payment-service timed out")
      )
      errors.foreach(roundTrip[CheckoutError])
    }
  }
}
