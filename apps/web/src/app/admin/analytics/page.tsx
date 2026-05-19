import { redirect } from "next/navigation";

/** Story 8.1: panel trên /admin — giữ route cũ để tránh 404 bookmark. */
export default function AdminAnalyticsRedirectPage() {
  redirect("/admin");
}
