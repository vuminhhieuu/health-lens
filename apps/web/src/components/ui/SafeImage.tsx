"use client";

import Image, { type ImageProps } from "next/image";
import React from "react";

/*
  NOTE (safety & typing):
  - This component accepts either plain <img> props (for `raw` use or blob/data URLs)
    or `next/image` props when rendering the Next.js Image component.
  - The current single `SafeImageProps` union below is permissive (ImgHTMLAttributes)
    which makes it easy to call `SafeImage` without providing required Next.js Image
    sizing props (width/height or `fill`). That can cause runtime build/type errors
    or layout regressions when Next/Image expects explicit sizing.
  - Recommendation: use a discriminated union for props so callers are forced to
    provide proper props for the non-raw branch. Example patterns:
      type RawProps = React.ImgHTMLAttributes<HTMLImageElement> & { raw: true }
      type NextProps = Omit<React.ComponentProps<typeof Image>, 'src'> & { raw?: false }
      type SafeImageProps = RawProps | NextProps
    This makes TypeScript require width/height (or other Image props) when raw is
    omitted/false, preventing accidental misuse.
  - Also avoid wide `any`/`as any` casts and @ts-expect-error; prefer explicit
    narrowing and typed spreads to keep lint/type checks useful.
*/

type SafeImageProps = React.ImgHTMLAttributes<HTMLImageElement> & {
  raw?: boolean; // when true, render a plain <img> instead of next/image
  unoptimized?: boolean; // pass-through to next/image when used
};

export function SafeImage({
  src,
  alt,
  className,
  raw = false,
  unoptimized = true,
  ...rest
}: SafeImageProps) {
  // If caller explicitly requests raw, or src is blob/data URL, render plain img to avoid next/image loader issues
  const srcStr = typeof src === "string" ? src : undefined;
  const isBlobLike = srcStr?.startsWith("blob:") || srcStr?.startsWith("data:");

  if (raw || isBlobLike || !srcStr) {
    // eslint-disable-next-line @next/next/no-img-element
    return <img src={srcStr} alt={alt} className={className} {...rest} />;
  }

  // Use next/image for public/static images to enable optimizations; keep unoptimized by default to avoid platform restrictions
  const imageRest = rest as unknown as Partial<ImageProps>;

  return (
    <Image
      src={srcStr!}
      alt={alt ?? ""}
      className={className}
      unoptimized={unoptimized}
      {...imageRest}
    />
  );
}

export default SafeImage;
