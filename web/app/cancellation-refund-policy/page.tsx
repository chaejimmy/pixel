import type { Metadata } from "next";

export const metadata: Metadata = {
  title:
    "Cancellation & Refund Policy | PaceDream – Find Your Perfect Space",
  description:
    "Understand PaceDream's cancellation and refund policies, including 24-hour free cancellation, no-show rules, host penalties, and refund timelines.",
};

export default function CancellationRefundPolicy() {
  return (
    <div className="legal-content mx-auto max-w-3xl px-6 py-16">
      <h1>Cancellation &amp; Refund Policy</h1>
      <p className="mt-2 text-sm text-gray-400">
        Effective Date: February 10, 2025 &middot; Last Updated: February 10,
        2025
      </p>

      <p className="mt-6">
        At PaceDream, we want every booking experience to be seamless. This
        Cancellation &amp; Refund Policy outlines the rules and procedures for
        cancellations, refunds, and disputes for both Guests and Hosts on the
        PaceDream platform.
      </p>

      {/* 1. 24-Hour Free Cancellation */}
      <h2>1. 24-Hour Free Cancellation</h2>
      <p>
        All bookings on PaceDream are eligible for a full refund if canceled
        within 24 hours of making the reservation, provided:
      </p>
      <ul>
        <li>
          The cancellation is made <strong>at least 24 hours before</strong> the
          scheduled booking start time.
        </li>
        <li>The cancellation is submitted through the PaceDream app or website.</li>
      </ul>
      <p>
        If both conditions are met, the Guest will receive a{" "}
        <strong>full refund</strong> of the total amount paid, including the
        service fee and taxes.
      </p>

      {/* 2. Cancellation After 24 Hours */}
      <h2>2. Cancellation After 24 Hours</h2>
      <p>
        If a Guest cancels a booking after the 24-hour free cancellation window
        has passed, the following applies:
      </p>
      <ul>
        <li>
          <strong>More than 12 hours before start time:</strong> The Guest
          receives a 50% refund of the base booking price. The service fee is
          non-refundable.
        </li>
        <li>
          <strong>Less than 12 hours before start time:</strong> No refund is
          issued. The full amount is retained.
        </li>
        <li>
          <strong>After the booking start time:</strong> No refund is issued.
        </li>
      </ul>
      <p>
        Exceptions may be granted in cases of documented emergencies or
        extenuating circumstances at PaceDream&apos;s sole discretion. To
        request an exception, contact us at{" "}
        <a href="mailto:info@pacedream.com">info@pacedream.com</a>.
      </p>

      {/* 3. No-Show Policy */}
      <h2>3. No-Show Policy</h2>
      <p>
        If a Guest fails to arrive at the booked space within 30 minutes of the
        scheduled start time without canceling or notifying the Host:
      </p>
      <ul>
        <li>The booking is marked as a <strong>no-show</strong>.</li>
        <li>
          <strong>No refund</strong> is issued. The Host receives their full
          payout.
        </li>
        <li>
          Repeated no-shows may result in account warnings, restrictions, or
          suspension.
        </li>
      </ul>

      {/* 4. Host Cancellation Penalties */}
      <h2>4. Host Cancellation Penalties</h2>
      <p>
        Hosts are expected to honor all confirmed bookings. If a Host cancels a
        confirmed booking:
      </p>
      <ul>
        <li>
          The Guest receives a <strong>full refund</strong> of all charges,
          including the service fee and taxes.
        </li>
        <li>
          The Host&apos;s listing may receive a <strong>cancellation flag</strong>{" "}
          visible to future Guests.
        </li>
        <li>
          <strong>First cancellation:</strong> Warning notification to the Host.
        </li>
        <li>
          <strong>Second cancellation (within 90 days):</strong> The
          Host&apos;s listing may be temporarily delisted for up to 7 days.
        </li>
        <li>
          <strong>Third cancellation (within 90 days):</strong> The Host&apos;s
          account may be suspended pending review.
        </li>
      </ul>
      <p>
        Exceptions may apply for emergencies, natural disasters, or
        circumstances beyond the Host&apos;s control, subject to review by
        PaceDream support.
      </p>

      {/* 5. Refund Processing */}
      <h2>5. Refund Processing</h2>
      <p>
        All eligible refunds are processed to the <strong>original payment
        method</strong> used at the time of booking.
      </p>
      <ul>
        <li>
          <strong>Processing Time:</strong> Refunds are initiated within 1-2
          business days of the cancellation. Depending on your bank or card
          issuer, refunds typically appear on your statement within{" "}
          <strong>5 to 10 business days</strong>.
        </li>
        <li>
          <strong>Partial Refunds:</strong> When a partial refund is issued, the
          refund amount is calculated on the base booking price only. Service
          fees are non-refundable for late cancellations.
        </li>
        <li>
          <strong>Failed Refunds:</strong> If a refund cannot be processed to
          your original payment method (e.g., expired card), PaceDream will
          contact you to arrange an alternative refund method.
        </li>
      </ul>

      {/* 6. Dispute Resolution */}
      <h2>6. Dispute Resolution</h2>
      <p>
        If you believe a refund decision was made in error, or if you have a
        dispute regarding a booking, please follow these steps:
      </p>
      <ol>
        <li>
          <strong>Contact Support:</strong> Email us at{" "}
          <a href="mailto:info@pacedream.com">info@pacedream.com</a> or call{" "}
          <a href="tel:+15713587833">+1 571 358 7833</a> within 7 days of the
          booking date. Include your booking ID and a description of the issue.
        </li>
        <li>
          <strong>Review Process:</strong> Our support team will review your
          claim, which may include contacting the other party (Host or Guest)
          for their account of events.
        </li>
        <li>
          <strong>Resolution:</strong> PaceDream will issue a final decision
          within 10 business days of receiving your dispute. Resolutions may
          include a full refund, partial refund, credit to your PaceDream
          account, or denial of the claim.
        </li>
        <li>
          <strong>Escalation:</strong> If you are not satisfied with the
          resolution, you may escalate the dispute through the arbitration
          process described in our{" "}
          <a href="/terms-of-service">Terms of Service</a>.
        </li>
      </ol>

      {/* 7. Special Circumstances */}
      <h2>7. Special Circumstances</h2>
      <p>
        PaceDream may issue full refunds outside of the standard policy in the
        following situations:
      </p>
      <ul>
        <li>
          The listed space was significantly different from the description.
        </li>
        <li>
          The space was unsafe, unsanitary, or inaccessible upon arrival.
        </li>
        <li>
          The Host was unresponsive and the Guest could not access the space.
        </li>
        <li>
          A natural disaster, severe weather, or government-mandated restriction
          prevented use of the space.
        </li>
      </ul>
      <p>
        Claims for special circumstances must be submitted within 24 hours of
        the booking start time with supporting evidence (photos, messages, etc.).
      </p>

      {/* 8. Contact */}
      <h2>8. Contact Us</h2>
      <p>
        For cancellation requests, refund inquiries, or disputes, please contact
        us:
      </p>
      <p>
        <strong>PaceDream LLC</strong>
        <br />
        8521 Leesburg Pike
        <br />
        Vienna, VA 22182
        <br />
        Email:{" "}
        <a href="mailto:info@pacedream.com">info@pacedream.com</a>
        <br />
        Phone:{" "}
        <a href="tel:+15713587833">+1 571 358 7833</a>
      </p>
    </div>
  );
}
