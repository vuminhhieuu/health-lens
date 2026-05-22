"use client";

import Image, { type ImageProps } from "next/image";
import React from "react";

type RawSafeImageProps = Omit<
  React.ImgHTMLAttributes<HTMLImageElement>,
  "alt" | "src"
> & {
  raw: true;
  src: string;
  alt: string;
};

type NextSafeImageProps = Omit<ImageProps, "alt" | "src"> & {
  raw?: false;
  src: ImageProps["src"];
  alt: string;
};

type SafeImageProps = RawSafeImageProps | NextSafeImageProps;

type NextImageOnlyProps = Pick<
  ImageProps,
  | "blurDataURL"
  | "fill"
  | "loader"
  | "onLoadingComplete"
  | "overrideSrc"
  | "placeholder"
  | "priority"
  | "quality"
  | "unoptimized"
>;

export function SafeImage({
  src,
  alt,
  className,
  raw,
  ...rest
}: SafeImageProps) {
  const srcStr = typeof src === "string" ? src : undefined;
  const isBlobLike = srcStr?.startsWith("blob:") || srcStr?.startsWith("data:");

  if (raw || isBlobLike || (typeof src === "string" && !srcStr)) {
    const imgRest = {
      ...rest,
    } as React.ImgHTMLAttributes<HTMLImageElement> & Partial<NextImageOnlyProps>;
    delete imgRest.blurDataURL;
    delete imgRest.fill;
    delete imgRest.loader;
    delete imgRest.onLoadingComplete;
    delete imgRest.overrideSrc;
    delete imgRest.placeholder;
    delete imgRest.priority;
    delete imgRest.quality;
    delete imgRest.unoptimized;

    /*
      Raw <img> is intentional here for blob/data URLs and authenticated/private
      image sources that should bypass Next.js image optimization.
    */
    // eslint-disable-next-line @next/next/no-img-element
    return <img src={srcStr} alt={alt} className={className} {...imgRest} />;
  }

  return (
    <Image
      src={src}
      alt={alt}
      className={className}
      {...(rest as Omit<ImageProps, "alt" | "src">)}
    />
  );
}

export default SafeImage;
