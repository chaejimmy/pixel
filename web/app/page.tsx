import Link from "next/link";

export default function Home() {
  return (
    <div className="mx-auto max-w-5xl px-6 py-20 text-center">
      <h1 className="text-4xl font-bold tracking-tight text-gray-900 sm:text-5xl">
        Welcome to <span className="text-brand">PaceDream</span>
      </h1>
      <p className="mt-4 text-lg text-gray-600">
        Find Your Perfect Space — hourly rentals, gear borrowing, and
        cost-splitting all in one place.
      </p>
      <div className="mt-10 flex flex-wrap justify-center gap-4">
        <Link
          href="/terms-of-service"
          className="rounded-lg bg-brand px-5 py-2.5 text-sm font-medium text-white transition hover:bg-brand-600"
        >
          Terms of Service
        </Link>
        <Link
          href="/privacy-policy"
          className="rounded-lg bg-brand px-5 py-2.5 text-sm font-medium text-white transition hover:bg-brand-600"
        >
          Privacy Policy
        </Link>
        <Link
          href="/cookie-policy"
          className="rounded-lg bg-brand px-5 py-2.5 text-sm font-medium text-white transition hover:bg-brand-600"
        >
          Cookie Policy
        </Link>
        <Link
          href="/cancellation-refund-policy"
          className="rounded-lg bg-brand px-5 py-2.5 text-sm font-medium text-white transition hover:bg-brand-600"
        >
          Cancellation &amp; Refund Policy
        </Link>
      </div>
    </div>
  );
}
