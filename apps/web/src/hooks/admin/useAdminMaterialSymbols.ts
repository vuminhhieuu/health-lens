"use client";

import { useEffect } from "react";

const MATERIAL_SYMBOLS_LINK_ID = "hl-material-symbols-outlined";
const MATERIAL_SYMBOLS_HREF =
  "https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@24,400,0,0";

/** Loads Material Symbols once for admin pages that use `material-symbols-outlined` icons. */
export function useAdminMaterialSymbols() {
  useEffect(() => {
    if (document.getElementById(MATERIAL_SYMBOLS_LINK_ID)) {
      return;
    }
    const link = document.createElement("link");
    link.id = MATERIAL_SYMBOLS_LINK_ID;
    link.rel = "stylesheet";
    link.href = MATERIAL_SYMBOLS_HREF;
    document.head.appendChild(link);
  }, []);
}
