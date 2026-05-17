import { redirect } from "next/navigation";

/** 8.3 hiển thị trên /admin — giữ route cũ để tránh 404 bookmark. */
export default function AdminAnalyticsRedirectPage() {
  redirect("/admin");
}
