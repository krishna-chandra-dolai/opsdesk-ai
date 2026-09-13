"use client";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { loadSession } from "@/lib/auth";

export default function Home() {
  const router = useRouter();
  useEffect(() => { router.replace(loadSession() ? "/incidents" : "/login"); }, [router]);
  return <main className="center"><p>Opening OpsDesk…</p></main>;
}
