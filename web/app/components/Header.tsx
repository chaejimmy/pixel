import Link from "next/link";

export default function Header() {
  return (
    <header className="sticky top-0 z-50 border-b border-gray-100 bg-white/80 backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-5xl items-center justify-between px-6">
        <Link href="/" className="flex items-center gap-2">
          <span className="text-xl font-bold text-brand">PaceDream</span>
        </Link>
        <nav className="hidden gap-6 text-sm font-medium text-gray-600 sm:flex">
          <Link href="/" className="transition hover:text-brand">
            Home
          </Link>
          <Link
            href="https://www.pacedream.com"
            className="transition hover:text-brand"
          >
            App
          </Link>
        </nav>
      </div>
    </header>
  );
}
