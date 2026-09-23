package io.github.andy327.switchback.common.domain

import java.time.Instant

import io.circe.Codec
import io.circe.syntax._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Round-trip checks for the JSON that is a wire contract between separately-deployed services.
  *
  * [[CheckoutEvent]] earns a dedicated check because its encoding is a discriminated union keyed on the case name, so a
  * rename or codec tweak can change the on-the-wire shape and silently break a consumer. The [[CheckoutError]] variants
  * each carry their own codec (the HTTP status is the discriminator at the REST edge), so they are round-tripped
  * individually. The plain product DTOs are left to be exercised by the service and route tests.
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
    "round-trip each variant on its own codec" in {
      roundTrip(CheckoutError.OutOfStock("widget out of stock"))
      roundTrip(CheckoutError.PaymentDeclined("card declined"))
      roundTrip(CheckoutError.NotFound("no such reservation"))
      roundTrip(CheckoutError.Unavailable("payment-service timed out"))
    }
  }
}
