"use client";

import Image, { type ImageProps } from "next/image";
import React from "react";

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
