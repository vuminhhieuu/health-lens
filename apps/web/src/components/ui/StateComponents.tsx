import type { ReactNode } from "react";
import { AlertTriangle, Inbox, Loader2 } from "lucide-react";

type StateActionProps =
  | {
      action?: ReactNode;
      actionLabel?: never;
      onAction?: never;
    }
  | {
      action?: never;
      actionLabel: string;
      onAction: () => void;
    };

type CommonStateProps = StateActionProps & {
  title: string;
  description?: string;
  className?: string;
};

const borderNonColorUtilities = new Set([
  "border",
  "border-0",
  "border-2",
  "border-4",
  "border-8",
  "border-x",
  "border-x-0",
  "border-x-2",
  "border-x-4",
  "border-x-8",
  "border-y",
  "border-y-0",
  "border-y-2",
  "border-y-4",
  "border-y-8",
  "border-s",
  "border-s-0",
  "border-s-2",
  "border-s-4",
  "border-s-8",
  "border-e",
  "border-e-0",
  "border-e-2",
  "border-e-4",
  "border-e-8",
  "border-t",
  "border-t-0",
  "border-t-2",
  "border-t-4",
  "border-t-8",
  "border-r",
  "border-r-0",
  "border-r-2",
  "border-r-4",
  "border-r-8",
  "border-b",
  "border-b-0",
  "border-b-2",
  "border-b-4",
  "border-b-8",
  "border-l",
  "border-l-0",
  "border-l-2",
  "border-l-4",
  "border-l-8",
  "border-solid",
  "border-dashed",
  "border-dotted",
  "border-double",
  "border-hidden",
  "border-none",
]);

function isBorderColorUtility(token: string) {
  if (!token.startsWith("border-")) return false;
  if (borderNonColorUtilities.has(token)) return false;
  return !/^border(?:-[xysetrbl])?-\d+$/.test(token);
}

function stateClassName(className?: string) {
  const baseClasses = [
    "flex",
    "min-h-64",
    "flex-col",
    "items-center",
    "justify-center",
    "rounded-2xl",
    "border",
    "border-dashed",
    "border-[#b7d8d1]",
    "bg-white/70",
    "px-6",
    "py-12",
    "text-center",
    "shadow-sm",
  ];
  const overrideClasses = className?.trim().split(/\s+/).filter(Boolean) ?? [];
  const hasMinHeightOverride = overrideClasses.some((token) => token.startsWith("min-h-"));
  const hasBackgroundOverride = overrideClasses.some((token) => token.startsWith("bg-"));
  const hasBorderColorOverride = overrideClasses.some(isBorderColorUtility);

  return [
    ...baseClasses.filter((token) => {
      if (hasMinHeightOverride && token.startsWith("min-h-")) return false;
      if (hasBackgroundOverride && token.startsWith("bg-")) return false;
      if (hasBorderColorOverride && token === "border-[#b7d8d1]") return false;
      return true;
    }),
    ...overrideClasses,
  ].join(" ");
}

function renderAction(props: StateActionProps) {
  if (props.action) return props.action;
  if (!props.actionLabel || !props.onAction) return null;

  return (
    <button
      type="button"
      onClick={props.onAction}
      className="inline-flex items-center justify-center rounded-xl bg-[#00685f] px-5 py-2.5 text-sm font-bold text-white shadow-sm transition hover:brightness-110 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
    >
      {props.actionLabel}
    </button>
  );
}

export function LoadingState({ title, description, className }: Omit<CommonStateProps, "action" | "actionLabel" | "onAction">) {
  return (
    <div role="status" aria-live="polite" className={stateClassName(className)}>
      <Loader2 className="h-10 w-10 animate-spin text-[#00685f]" aria-hidden="true" />
      <h2 className="mt-5 text-lg font-bold text-[#121e1c]">{title}</h2>
      {description ? <p className="mt-2 max-w-md text-sm font-medium text-[#4e6360]">{description}</p> : null}
    </div>
  );
}

export function EmptyState(props: CommonStateProps) {
  const action = renderAction(props);

  return (
    <div className={stateClassName(props.className)}>
      <div className="flex h-14 w-14 items-center justify-center rounded-full bg-[#e9f6f3] text-[#00685f]">
        <Inbox className="h-7 w-7" aria-hidden="true" />
      </div>
      <h2 className="mt-5 text-lg font-bold text-[#121e1c]">{props.title}</h2>
      {props.description ? <p className="mt-2 max-w-md text-sm font-medium text-[#4e6360]">{props.description}</p> : null}
      {action ? <div className="mt-6">{action}</div> : null}
    </div>
  );
}

export function ErrorState(props: CommonStateProps) {
  const action = renderAction(props);

  return (
    <div role="alert" className={stateClassName(props.className)}>
      <div className="flex h-14 w-14 items-center justify-center rounded-full bg-[#ffdad6] text-[#ba1a1a]">
        <AlertTriangle className="h-7 w-7" aria-hidden="true" />
      </div>
      <h2 className="mt-5 text-lg font-bold text-[#ba1a1a]">{props.title}</h2>
      {props.description ? <p className="mt-2 max-w-md text-sm font-medium text-[#4e6360]">{props.description}</p> : null}
      {action ? <div className="mt-6">{action}</div> : null}
    </div>
  );
}

export function InlineFieldError({ id, message }: { id: string; message?: string | null }) {
  if (!message) return null;

  return (
    <p id={id} role="alert" aria-live="assertive" className="mt-2 text-sm font-semibold text-[#ba1a1a]">
      {message}
    </p>
  );
}
