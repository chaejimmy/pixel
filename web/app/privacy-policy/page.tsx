import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Privacy Policy | PaceDream – Find Your Perfect Space",
  description:
    "Learn how PaceDream collects, uses, and protects your personal data. Covers data collection, sharing, cookies, CCPA, GDPR, and your rights.",
};

export default function PrivacyPolicy() {
  return (
    <div className="legal-content mx-auto max-w-3xl px-6 py-16">
      <h1>Privacy Policy</h1>
      <p className="mt-2 text-sm text-gray-400">
        Effective Date: February 10, 2025 &middot; Last Updated: February 10,
        2025
      </p>

      <p className="mt-6">
        PaceDream LLC (&quot;PaceDream,&quot; &quot;we,&quot; &quot;us,&quot; or
        &quot;our&quot;) is committed to protecting your privacy. This Privacy
        Policy explains how we collect, use, disclose, and safeguard your
        information when you use our website, mobile applications, and related
        services (collectively, the &quot;Service&quot;).
      </p>
      <p>
        By using the Service, you consent to the data practices described in
        this policy. If you do not agree, please discontinue use of the Service.
      </p>

      {/* 1. Data Collection */}
      <h2>1. Information We Collect</h2>

      <h3>1.1 Personal Information</h3>
      <p>
        When you create an account or use the Service, we may collect the
        following personal information:
      </p>
      <ul>
        <li>Full name</li>
        <li>Email address</li>
        <li>Phone number</li>
        <li>Profile photo</li>
        <li>Mailing address (for Hosts)</li>
        <li>Date of birth (for age verification)</li>
        <li>
          Government-issued identification (when required for identity
          verification)
        </li>
      </ul>

      <h3>1.2 Payment Information</h3>
      <p>
        We use <strong>Stripe</strong> as our payment processor. When you make
        or receive payments, Stripe collects and processes your payment card
        details, bank account information, and billing address. PaceDream does
        not store your full credit card or bank account numbers on our servers.
        For more information, see{" "}
        <a
          href="https://stripe.com/privacy"
          target="_blank"
          rel="noopener noreferrer"
        >
          Stripe&apos;s Privacy Policy
        </a>
        .
      </p>

      <h3>1.3 Usage Data</h3>
      <p>
        We automatically collect information about how you interact with the
        Service, including:
      </p>
      <ul>
        <li>Device type, operating system, and browser type</li>
        <li>IP address</li>
        <li>Pages viewed, features used, and time spent on the Service</li>
        <li>Referring and exit pages</li>
        <li>Crash reports and performance data</li>
      </ul>

      <h3>1.4 Location Data</h3>
      <p>
        With your permission, we collect precise or approximate location data
        from your device to:
      </p>
      <ul>
        <li>Show nearby available spaces and listings</li>
        <li>Display maps and directions via Google Maps</li>
        <li>Improve search relevance</li>
      </ul>
      <p>
        You can disable location services through your device settings at any
        time, though this may limit certain features.
      </p>

      {/* 2. How We Use Your Data */}
      <h2>2. How We Use Your Information</h2>
      <p>We use the information we collect to:</p>
      <ul>
        <li>Create and manage your account</li>
        <li>Process bookings, payments, and refunds</li>
        <li>Facilitate communication between Hosts and Guests</li>
        <li>Provide customer support</li>
        <li>Send transactional notifications (booking confirmations, receipts)</li>
        <li>
          Send marketing communications (with your consent; you can opt out at
          any time)
        </li>
        <li>Improve the Service, including personalization and analytics</li>
        <li>Detect and prevent fraud, abuse, and security incidents</li>
        <li>Comply with legal obligations</li>
      </ul>

      {/* 3. Data Sharing */}
      <h2>3. How We Share Your Information</h2>
      <p>
        We do not sell your personal information. We may share your data with
        the following parties:
      </p>

      <h3>3.1 Hosts and Guests</h3>
      <p>
        When you make a booking, we share necessary information between Hosts
        and Guests to facilitate the transaction (e.g., name, profile photo,
        contact information, booking details).
      </p>

      <h3>3.2 Stripe</h3>
      <p>
        We share payment-related information with Stripe to process
        transactions, manage payouts, and comply with financial regulations.
      </p>

      <h3>3.3 Service Providers</h3>
      <p>
        We may share information with trusted third-party service providers who
        assist us in operating the Service, including:
      </p>
      <ul>
        <li>Cloud hosting and infrastructure providers</li>
        <li>Analytics services</li>
        <li>Customer support tools</li>
        <li>Email and push notification services (e.g., OneSignal)</li>
        <li>Map services (Google Maps)</li>
      </ul>

      <h3>3.4 Legal and Safety</h3>
      <p>
        We may disclose your information if required by law, regulation, or
        legal process, or if we believe in good faith that disclosure is
        necessary to protect the rights, property, or safety of PaceDream, our
        users, or the public.
      </p>

      {/* 4. Cookies */}
      <h2>4. Cookies and Tracking Technologies</h2>
      <p>
        We use cookies and similar technologies to enhance your experience. For
        detailed information about the cookies we use and how to manage them,
        please see our <a href="/cookie-policy">Cookie Policy</a>.
      </p>
      <p>Key cookies and tracking technologies include:</p>
      <ul>
        <li>
          <strong>OneSignal:</strong> For push notification delivery and
          engagement tracking.
        </li>
        <li>
          <strong>Google Maps:</strong> For location-based services and map
          rendering.
        </li>
        <li>
          <strong>Session Cookies:</strong> For authentication and session
          management.
        </li>
      </ul>

      {/* 5. User Rights */}
      <h2>5. Your Rights</h2>
      <p>
        Depending on your location, you may have the following rights regarding
        your personal data:
      </p>
      <ul>
        <li>
          <strong>Access:</strong> Request a copy of the personal data we hold
          about you.
        </li>
        <li>
          <strong>Correction:</strong> Request correction of inaccurate or
          incomplete data.
        </li>
        <li>
          <strong>Deletion:</strong> Request deletion of your personal data,
          subject to legal and contractual obligations.
        </li>
        <li>
          <strong>Portability:</strong> Request your data in a structured,
          commonly used, machine-readable format.
        </li>
        <li>
          <strong>Opt-Out:</strong> Opt out of marketing communications at any
          time by using the unsubscribe link in our emails or contacting us
          directly.
        </li>
      </ul>
      <p>
        To exercise any of these rights, please contact us at{" "}
        <a href="mailto:info@pacedream.com">info@pacedream.com</a>. We will
        respond to your request within 30 days.
      </p>

      {/* 6. CCPA */}
      <h2>6. California Consumer Privacy Act (CCPA)</h2>
      <p>
        If you are a California resident, you have additional rights under the
        CCPA:
      </p>
      <ul>
        <li>
          <strong>Right to Know:</strong> You may request details about the
          categories and specific pieces of personal information we have
          collected about you.
        </li>
        <li>
          <strong>Right to Delete:</strong> You may request that we delete your
          personal information, subject to certain exceptions.
        </li>
        <li>
          <strong>Right to Opt-Out of Sale:</strong> PaceDream does not sell
          personal information. If this changes, we will provide a &quot;Do Not
          Sell My Personal Information&quot; link.
        </li>
        <li>
          <strong>Non-Discrimination:</strong> We will not discriminate against
          you for exercising your CCPA rights.
        </li>
      </ul>
      <p>
        To submit a CCPA request, email us at{" "}
        <a href="mailto:info@pacedream.com">info@pacedream.com</a> or call{" "}
        <a href="tel:+15713587833">+1 571 358 7833</a>.
      </p>

      {/* 7. GDPR */}
      <h2>7. General Data Protection Regulation (GDPR)</h2>
      <p>
        If you are located in the European Economic Area (EEA) or the United
        Kingdom, you have additional rights under the GDPR:
      </p>
      <ul>
        <li>
          <strong>Lawful Basis:</strong> We process your data based on
          contractual necessity (to provide the Service), legitimate interest
          (to improve the Service), and consent (for marketing communications).
        </li>
        <li>
          <strong>Right to Restrict Processing:</strong> You may request that we
          restrict the processing of your data under certain circumstances.
        </li>
        <li>
          <strong>Right to Object:</strong> You may object to the processing of
          your data based on legitimate interest.
        </li>
        <li>
          <strong>Right to Lodge a Complaint:</strong> You have the right to
          file a complaint with your local data protection authority.
        </li>
        <li>
          <strong>Data Transfers:</strong> If your data is transferred outside
          the EEA, we ensure appropriate safeguards are in place (e.g., Standard
          Contractual Clauses).
        </li>
      </ul>

      {/* 8. Children's Privacy */}
      <h2>8. Children&apos;s Privacy</h2>
      <p>
        The Service is not directed to individuals under the age of 18.
        PaceDream does not knowingly collect personal information from children
        under 18. If we learn that we have collected personal data from a child
        under 18, we will delete that information promptly. If you believe a
        child under 18 has provided us with personal data, please contact us at{" "}
        <a href="mailto:info@pacedream.com">info@pacedream.com</a>.
      </p>

      {/* 9. Security */}
      <h2>9. Security Measures</h2>
      <p>
        We implement industry-standard security measures to protect your
        personal information, including:
      </p>
      <ul>
        <li>Encryption of data in transit (TLS/SSL) and at rest</li>
        <li>Secure authentication via Auth0</li>
        <li>Encrypted storage of sensitive credentials</li>
        <li>Regular security audits and vulnerability assessments</li>
        <li>Access controls limiting employee access to personal data</li>
        <li>
          PCI-DSS compliant payment processing through Stripe
        </li>
      </ul>
      <p>
        While we strive to protect your data, no method of transmission or
        storage is 100% secure. We cannot guarantee absolute security.
      </p>

      {/* 10. Data Retention */}
      <h2>10. Data Retention</h2>
      <p>
        We retain your personal information for as long as your account is
        active or as needed to provide the Service. We may also retain data as
        necessary to comply with legal obligations, resolve disputes, and
        enforce our agreements. When your data is no longer needed, we will
        securely delete or anonymize it.
      </p>

      {/* 11. Changes */}
      <h2>11. Changes to This Privacy Policy</h2>
      <p>
        We may update this Privacy Policy from time to time. If we make material
        changes, we will notify you by email or through the Service. Your
        continued use of the Service after the effective date constitutes your
        acceptance of the revised policy.
      </p>

      {/* 12. Contact */}
      <h2>12. Contact Us</h2>
      <p>
        If you have questions or concerns about this Privacy Policy or our data
        practices, please contact us:
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
