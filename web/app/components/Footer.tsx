import Link from "next/link";

const legalLinks = [
  { href: "/terms-of-service", label: "Terms of Service" },
  { href: "/privacy-policy", label: "Privacy Policy" },
  { href: "/cookie-policy", label: "Cookie Policy" },
  { href: "/cancellation-refund-policy", label: "Cancellation & Refund Policy" },
];

export default function Footer() {
  return (
    <footer className="border-t border-gray-100 bg-gray-50">
      <div className="mx-auto max-w-5xl px-6 py-12">
        <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-3">
          {/* Brand */}
          <div>
            <span className="text-lg font-bold text-brand">PaceDream</span>
            <p className="mt-2 text-sm text-gray-500">
              Find Your Perfect Space. Hourly rentals for restrooms, meeting
              rooms, parking, nap pods, and more.
            </p>
          </div>

          {/* Legal */}
          <div>
            <h4 className="text-sm font-semibold text-gray-900">Legal</h4>
            <ul className="mt-3 space-y-2">
              {legalLinks.map((link) => (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    className="text-sm text-gray-500 transition hover:text-brand"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          {/* Contact */}
          <div>
            <h4 className="text-sm font-semibold text-gray-900">Contact</h4>
            <ul className="mt-3 space-y-2 text-sm text-gray-500">
              <li>
                <a
                  href="mailto:info@pacedream.com"
                  className="transition hover:text-brand"
                >
                  info@pacedream.com
                </a>
              </li>
              <li>
                <a
                  href="tel:+15713587833"
                  className="transition hover:text-brand"
                >
                  +1 571 358 7833
                </a>
              </li>
              <li>
                8521 Leesburg Pike
                <br />
                Vienna, VA 22182
              </li>
            </ul>
          </div>
        </div>

        <div className="mt-10 border-t border-gray-200 pt-6 text-center text-xs text-gray-400">
          &copy; {new Date().getFullYear()} PaceDream LLC. All rights reserved.
        </div>
      </div>
    </footer>
  );
}
