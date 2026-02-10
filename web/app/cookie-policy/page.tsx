import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Cookie Policy | PaceDream – Find Your Perfect Space",
  description:
    "Learn about the cookies and tracking technologies PaceDream uses, including essential, functional, analytics, and third-party cookies.",
};

export default function CookiePolicy() {
  return (
    <div className="legal-content mx-auto max-w-3xl px-6 py-16">
      <h1>Cookie Policy</h1>
      <p className="mt-2 text-sm text-gray-400">
        Effective Date: February 10, 2025 &middot; Last Updated: February 10,
        2025
      </p>

      <p className="mt-6">
        PaceDream LLC (&quot;PaceDream,&quot; &quot;we,&quot; &quot;us,&quot; or
        &quot;our&quot;) uses cookies and similar tracking technologies on our
        website and mobile applications (collectively, the &quot;Service&quot;).
        This Cookie Policy explains what cookies are, how we use them, and how
        you can manage your preferences.
      </p>

      {/* 1. What Are Cookies */}
      <h2>1. What Are Cookies?</h2>
      <p>
        Cookies are small text files placed on your device (computer,
        smartphone, or tablet) when you visit a website. They help websites
        remember your preferences, understand how you use the site, and improve
        your overall experience. Cookies may be &quot;session&quot; cookies
        (deleted when you close your browser) or &quot;persistent&quot; cookies
        (stored until they expire or you delete them).
      </p>

      {/* 2. Essential Cookies */}
      <h2>2. Essential Cookies</h2>
      <p>
        Essential cookies are strictly necessary for the Service to function.
        Without these cookies, core features of the platform would not work.
      </p>
      <ul>
        <li>
          <strong>Authentication Session Cookie:</strong> Maintains your login
          state so you don&apos;t need to sign in on every page. This cookie is
          a session cookie that expires when you close your browser or after a
          set period of inactivity.
        </li>
        <li>
          <strong>Security Cookies:</strong> Help detect and prevent fraudulent
          activity, such as cross-site request forgery (CSRF) tokens.
        </li>
      </ul>
      <p>
        These cookies cannot be disabled as they are required for the Service to
        operate.
      </p>

      {/* 3. Functional Cookies */}
      <h2>3. Functional Cookies</h2>
      <p>
        Functional cookies allow the Service to remember choices you make and
        provide enhanced, personalized features.
      </p>
      <ul>
        <li>
          <strong>User Preferences:</strong> Remember your language, region,
          display preferences, and notification settings.
        </li>
        <li>
          <strong>localStorage:</strong> We use your browser&apos;s localStorage
          to store non-sensitive preferences locally on your device, such as
          recent search queries, preferred map view, dark/light theme selection,
          and onboarding completion status.
        </li>
      </ul>
      <p>
        Disabling functional cookies may result in a less personalized
        experience but will not prevent you from using the Service.
      </p>

      {/* 4. Analytics Cookies */}
      <h2>4. Analytics Cookies</h2>
      <p>
        Analytics cookies help us understand how visitors interact with the
        Service so we can improve it. These cookies collect aggregated,
        anonymized data about:
      </p>
      <ul>
        <li>Pages visited and time spent on each page</li>
        <li>Features used and user flows</li>
        <li>Error and crash reports</li>
        <li>Device and browser information</li>
      </ul>
      <p>
        We use this data to identify trends, fix bugs, and enhance the user
        experience. Analytics data is not used to identify individual users.
      </p>

      {/* 5. Third-Party Cookies */}
      <h2>5. Third-Party Cookies</h2>
      <p>
        Some cookies are placed by third-party services that we integrate into
        the PaceDream platform:
      </p>

      <h3>5.1 Google Maps</h3>
      <p>
        We use Google Maps to display interactive maps, provide directions, and
        show nearby listings. Google may set cookies on your device to remember
        map preferences and deliver map tiles efficiently. Google&apos;s use of
        cookies is governed by the{" "}
        <a
          href="https://policies.google.com/privacy"
          target="_blank"
          rel="noopener noreferrer"
        >
          Google Privacy Policy
        </a>
        .
      </p>

      <h3>5.2 OneSignal</h3>
      <p>
        We use OneSignal for push notification delivery and engagement tracking.
        OneSignal may set cookies or use device identifiers to:
      </p>
      <ul>
        <li>Deliver push notifications to your device</li>
        <li>Track notification open rates and engagement</li>
        <li>Segment users for targeted notification campaigns</li>
      </ul>
      <p>
        You can manage push notification preferences in your device settings or
        within the PaceDream app. For more information, see the{" "}
        <a
          href="https://onesignal.com/privacy_policy"
          target="_blank"
          rel="noopener noreferrer"
        >
          OneSignal Privacy Policy
        </a>
        .
      </p>

      {/* 6. How to Manage Cookies */}
      <h2>6. How to Manage and Disable Cookies</h2>
      <p>
        You can control and manage cookies in several ways:
      </p>

      <h3>6.1 Browser Settings</h3>
      <p>
        Most web browsers allow you to manage cookies through their settings.
        You can typically:
      </p>
      <ul>
        <li>View the cookies stored on your device</li>
        <li>Delete individual or all cookies</li>
        <li>Block cookies from specific or all websites</li>
        <li>Set your browser to notify you when a cookie is being set</li>
      </ul>
      <p>
        Refer to your browser&apos;s help documentation for specific
        instructions:
      </p>
      <ul>
        <li>
          <a
            href="https://support.google.com/chrome/answer/95647"
            target="_blank"
            rel="noopener noreferrer"
          >
            Google Chrome
          </a>
        </li>
        <li>
          <a
            href="https://support.mozilla.org/en-US/kb/enhanced-tracking-protection-firefox-desktop"
            target="_blank"
            rel="noopener noreferrer"
          >
            Mozilla Firefox
          </a>
        </li>
        <li>
          <a
            href="https://support.apple.com/guide/safari/manage-cookies-sfri11471/mac"
            target="_blank"
            rel="noopener noreferrer"
          >
            Safari
          </a>
        </li>
        <li>
          <a
            href="https://support.microsoft.com/en-us/microsoft-edge/delete-cookies-in-microsoft-edge-63947406-40ac-c3b8-57b9-2a946a29ae09"
            target="_blank"
            rel="noopener noreferrer"
          >
            Microsoft Edge
          </a>
        </li>
      </ul>

      <h3>6.2 Mobile Device Settings</h3>
      <p>
        On mobile devices, you can manage cookie-like technologies and
        advertising identifiers through your device&apos;s privacy settings.
      </p>

      <h3>6.3 Opt-Out Links</h3>
      <p>
        For third-party analytics and advertising cookies, you can opt out
        through:
      </p>
      <ul>
        <li>
          <a
            href="https://tools.google.com/dlpage/gaoptout"
            target="_blank"
            rel="noopener noreferrer"
          >
            Google Analytics Opt-Out
          </a>
        </li>
        <li>
          <a
            href="https://optout.aboutads.info/"
            target="_blank"
            rel="noopener noreferrer"
          >
            Digital Advertising Alliance Opt-Out
          </a>
        </li>
      </ul>

      <p>
        Please note that disabling cookies may impact the functionality of the
        Service. Essential cookies cannot be disabled.
      </p>

      {/* 7. Changes */}
      <h2>7. Changes to This Cookie Policy</h2>
      <p>
        We may update this Cookie Policy from time to time to reflect changes in
        technology or legal requirements. We will notify you of material changes
        by updating the &quot;Last Updated&quot; date at the top of this page.
      </p>

      {/* 8. Contact */}
      <h2>8. Contact Us</h2>
      <p>
        If you have questions about this Cookie Policy, please contact us:
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
