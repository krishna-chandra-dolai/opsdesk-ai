import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {title: "OpsDesk", description: "Internal IT service desk"};

export default function RootLayout({children}: Readonly<{children: React.ReactNode}>) {
  return <html lang="en"><body>{children}</body></html>;
}
