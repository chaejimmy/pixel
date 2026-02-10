import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Terms of Service | PaceDream – Find Your Perfect Space",
  description:
    "Read PaceDream's Terms of Service covering account eligibility, user responsibilities, bookings, payments, and more.",
};

export default function TermsOfService() {
  return (
    <div className="legal-content mx-auto max-w-3xl px-6 py-16">
      <h1>Terms of Service</h1>
      <p className="mt-2 text-sm text-gray-400">
        Effective Date: February 10, 2025 &middot; Last Updated: February 10,
        2025
      </p>

      <p className="mt-6">
        Welcome to PaceDream! These Terms of Service (&quot;Terms&quot;) govern
        your access to and use of the PaceDream platform, including our website,
        mobile applications, and all related services (collectively, the
        &quot;Service&quot;). By accessing or using the Service, you agree to be
        bound by these Terms. If you do not agree, please do not use the
        Service.
      </p>
      <p>
        PaceDream is a marketplace for hourly space rentals (restrooms, meeting
        rooms, parking spots, nap pods, and more), gear borrowing, and
        cost-splitting. PaceDream LLC (&quot;PaceDream,&quot; &quot;we,&quot;
        &quot;us,&quot; or &quot;our&quot;) operates the Service.
      </p>

      {/* 1. Account Eligibility */}
      <h2>1. Account Eligibility</h2>
      <p>
        You must be at least 18 years of age to create an account and use the
        Service. By registering, you represent and warrant that:
      </p>
      <ul>
        <li>You are at least 18 years old.</li>
        <li>
          All information you provide during registration is accurate, current,
          and complete.
        </li>
        <li>
          You will maintain the accuracy of your account information and update
          it as necessary.
        </li>
        <li>
          You are responsible for safeguarding your account credentials and for
          all activity that occurs under your account.
        </li>
      </ul>
      <p>
        We reserve the right to suspend or terminate accounts that violate these
        Terms or are used for fraudulent purposes.
      </p>

      {/* 2. User Responsibilities */}
      <h2>2. User Responsibilities</h2>

      <h3>2.1 Hosts</h3>
      <p>
        If you list a space or gear on PaceDream (&quot;Host&quot;), you agree
        to:
      </p>
      <ul>
        <li>
          Provide accurate descriptions, photos, and availability for your
          listings.
        </li>
        <li>
          Ensure your space or gear is clean, safe, and matches the listing
          description.
        </li>
        <li>
          Comply with all applicable local, state, and federal laws and
          regulations, including zoning, health, and safety requirements.
        </li>
        <li>
          Respond promptly to booking requests, messages, and cancellation
          notices.
        </li>
        <li>
          Maintain adequate insurance for your property and any activities
          occurring on it.
        </li>
      </ul>

      <h3>2.2 Guests</h3>
      <p>
        If you book a space or borrow gear through PaceDream
        (&quot;Guest&quot;), you agree to:
      </p>
      <ul>
        <li>
          Use the rented space or borrowed gear respectfully and only for its
          intended purpose.
        </li>
        <li>
          Follow all rules and guidelines set by the Host for their space or
          gear.
        </li>
        <li>
          Leave the space in the same condition as you found it and return gear
          undamaged.
        </li>
        <li>
          Report any damage or issues to the Host and PaceDream promptly.
        </li>
        <li>Not exceed the agreed-upon booking duration without prior consent.</li>
      </ul>

      {/* 3. Booking and Cancellation */}
      <h2>3. Booking and Cancellation</h2>
      <p>
        All bookings are confirmed through the PaceDream platform. By making a
        booking, you enter into a direct agreement with the Host for the use of
        their space or gear.
      </p>
      <ul>
        <li>
          <strong>24-Hour Free Cancellation:</strong> Guests may cancel any
          booking within 24 hours of making the reservation for a full refund,
          provided the cancellation is made at least 24 hours before the
          scheduled start time.
        </li>
        <li>
          <strong>Late Cancellations:</strong> Cancellations made less than 24
          hours before the booking start time may be subject to partial or no
          refund, as outlined in our{" "}
          <a href="/cancellation-refund-policy">Cancellation &amp; Refund Policy</a>.
        </li>
        <li>
          <strong>No-Shows:</strong> Guests who fail to show up for a booking
          without canceling will not receive a refund.
        </li>
      </ul>
      <p>
        For full details, please review our{" "}
        <a href="/cancellation-refund-policy">
          Cancellation &amp; Refund Policy
        </a>
        .
      </p>

      {/* 4. Payment Terms */}
      <h2>4. Payment Terms</h2>
      <p>All payments are processed securely through Stripe.</p>
      <ul>
        <li>
          <strong>Service Fee:</strong> PaceDream charges a 15% service fee on
          each booking, calculated on the base rental price.
        </li>
        <li>
          <strong>Taxes:</strong> An 8.9% tax is applied in accordance with
          applicable tax regulations.
        </li>
        <li>
          <strong>Total Charges:</strong> The total amount displayed at checkout
          includes the base price, service fee, and applicable taxes.
        </li>
        <li>
          <strong>Host Payouts:</strong> Hosts receive payment after the booking
          is completed, minus the PaceDream service fee. Payout timelines depend
          on your connected Stripe account settings.
        </li>
        <li>
          <strong>Currency:</strong> All transactions are processed in US
          Dollars (USD).
        </li>
      </ul>

      {/* 5. Prohibited Conduct */}
      <h2>5. Prohibited Conduct</h2>
      <p>You agree not to:</p>
      <ul>
        <li>
          Use the Service for any unlawful purpose or in violation of any
          applicable law.
        </li>
        <li>
          Post false, misleading, or fraudulent listings, reviews, or account
          information.
        </li>
        <li>
          Harass, threaten, or discriminate against any other user of the
          Service.
        </li>
        <li>
          Circumvent the PaceDream payment system by arranging transactions
          outside the platform.
        </li>
        <li>
          Copy, modify, distribute, or reverse-engineer any part of the Service.
        </li>
        <li>
          Use automated tools (bots, scrapers) to access the Service without
          prior written consent.
        </li>
        <li>
          Sublease or re-list a space you have booked as a Guest without
          authorization.
        </li>
      </ul>

      {/* 6. Intellectual Property */}
      <h2>6. Intellectual Property</h2>
      <p>
        All content on the PaceDream platform — including but not limited to
        text, graphics, logos, icons, images, software, and the overall design —
        is the property of PaceDream LLC or its licensors and is protected by
        copyright, trademark, and other intellectual property laws.
      </p>
      <p>
        Users retain ownership of content they create (such as listing
        descriptions and photos) but grant PaceDream a non-exclusive,
        worldwide, royalty-free license to use, display, and distribute such
        content in connection with the Service.
      </p>

      {/* 7. Limitation of Liability */}
      <h2>7. Limitation of Liability</h2>
      <p>
        To the fullest extent permitted by law, PaceDream LLC and its officers,
        directors, employees, and agents shall not be liable for:
      </p>
      <ul>
        <li>
          Any indirect, incidental, special, consequential, or punitive damages,
          including loss of profits, data, or goodwill.
        </li>
        <li>
          Any damages arising from your use of or inability to use the Service.
        </li>
        <li>
          Any actions or omissions of Hosts, Guests, or other third parties on
          the platform.
        </li>
        <li>
          The condition, safety, or legality of any listed space or gear.
        </li>
      </ul>
      <p>
        PaceDream acts solely as a marketplace connecting Hosts and Guests. We
        do not own, operate, or control any listed spaces or gear. Our total
        liability to you for any claim arising from or related to the Service
        shall not exceed the amount you paid to PaceDream in the 12 months
        preceding the claim.
      </p>

      {/* 8. Dispute Resolution */}
      <h2>8. Dispute Resolution</h2>
      <p>
        If a dispute arises between you and PaceDream, we encourage you to
        contact us first at{" "}
        <a href="mailto:info@pacedream.com">info@pacedream.com</a> so we can
        try to resolve it informally.
      </p>
      <p>
        If we cannot resolve the dispute informally, both parties agree to
        submit to binding arbitration administered by the American Arbitration
        Association (AAA) under its Commercial Arbitration Rules. Arbitration
        shall take place in Fairfax County, Virginia. Each party shall bear its
        own costs and fees.
      </p>
      <p>
        <strong>Class Action Waiver:</strong> You agree that any dispute
        resolution proceedings will be conducted on an individual basis and not
        as a class action, collective action, or representative action.
      </p>

      {/* 9. Governing Law */}
      <h2>9. Governing Law</h2>
      <p>
        These Terms shall be governed by and construed in accordance with the
        laws of the Commonwealth of Virginia, United States of America, without
        regard to its conflict-of-law provisions. Any legal proceedings not
        subject to arbitration shall be brought exclusively in the state or
        federal courts located in Fairfax County, Virginia.
      </p>

      {/* 10. Changes to These Terms */}
      <h2>10. Changes to These Terms</h2>
      <p>
        We may update these Terms from time to time. If we make material
        changes, we will notify you by email or through the Service at least 30
        days before the changes take effect. Your continued use of the Service
        after the effective date constitutes your acceptance of the revised
        Terms.
      </p>

      {/* 11. Contact Us */}
      <h2>11. Contact Us</h2>
      <p>
        If you have any questions or concerns about these Terms of Service,
        please contact us:
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
