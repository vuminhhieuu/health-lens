declare module "qrcode.react" {
  import type { ComponentType } from "react";

  export const QRCodeCanvas: ComponentType<{
    value: string;
    size?: number;
    level?: "L" | "M" | "Q" | "H";
    bgColor?: string;
    fgColor?: string;
    includeMargin?: boolean;
    className?: string;
  }>;
}
