/** Display name for in-app about surfaces. */
export const APP_DISPLAY_NAME = "HealthLens";

/** Semantic version from `NEXT_PUBLIC_APP_VERSION` (injected at build from package.json by default). */
export function getAppVersionLabel(): string {
  const version = process.env.NEXT_PUBLIC_APP_VERSION?.trim();
  return version || "0.0.0";
}

/** Optional CI/build identifier when `NEXT_PUBLIC_BUILD_ID` is set in deploy env. */
export function getAppBuildLabel(): string | null {
  const build = process.env.NEXT_PUBLIC_BUILD_ID?.trim();
  return build || null;
}
