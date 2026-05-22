"use client";

type EmailPreferenceToggleProps = {
  id: string;
  label: string;
  description: string;
  checked: boolean;
  disabled?: boolean;
  onChange: (checked: boolean) => void;
};

export function EmailPreferenceToggle({
  id,
  label,
  description,
  checked,
  disabled = false,
  onChange,
}: EmailPreferenceToggleProps) {
  return (
    <div className="flex items-start justify-between gap-4 rounded-xl bg-[#e9f6f3]/60 px-4 py-4">
      <div className="min-w-0">
        <label htmlFor={id} className="text-sm font-bold text-[#121e1c]">
          {label}
        </label>
        <p className="mt-1 text-sm leading-6 text-[#6d7a77]">{description}</p>
      </div>
      <button
        id={id}
        type="button"
        role="switch"
        aria-checked={checked}
        disabled={disabled}
        onClick={() => onChange(!checked)}
        className={`relative h-7 w-12 shrink-0 rounded-full transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#00685f]/30 disabled:cursor-not-allowed disabled:opacity-60 ${
          checked ? "bg-[#00685f]" : "bg-[#bcc9c6]"
        }`}
      >
        <span
          className={`absolute top-0.5 left-0.5 h-6 w-6 rounded-full bg-white shadow transition ${
            checked ? "translate-x-5" : "translate-x-0"
          }`}
          aria-hidden="true"
        />
      </button>
    </div>
  );
}
