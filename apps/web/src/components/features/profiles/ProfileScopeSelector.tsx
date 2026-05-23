"use client";

export type ProfileScopeOption = { id: string; displayName: string };

const SELECT_CLASS =
  "min-h-12 w-full rounded-xl border border-[#b7e8e0] bg-[#f7fffd] px-4 text-sm font-semibold text-[#3d4947] outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#b7e8e0] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f] disabled:cursor-not-allowed disabled:opacity-60";

export function ProfileScopeSelector({
  profiles,
  value,
  onChange,
  label = "Hồ sơ",
  id = "profile-scope",
  disabled = false,
  className,
  ariaLabel = "Hồ sơ",
}: {
  profiles: ProfileScopeOption[];
  value: string;
  onChange: (profileId: string) => void;
  label?: string;
  id?: string;
  disabled?: boolean;
  className?: string;
  ariaLabel?: string;
}) {
  const visibleLabel = label?.trim() ?? "";
  const hasProfiles = profiles.length > 0;
  const hasValidValue = hasProfiles && profiles.some((profile) => profile.id === value);
  const selectValue = hasValidValue ? value : "";
  const isDisabled = disabled || !hasProfiles;

  return (
    <div className={className}>
      {visibleLabel ? (
        <label htmlFor={id} className="mb-2 block text-sm font-bold text-[#121e1c]">
          {visibleLabel}
        </label>
      ) : null}
      <select
        id={id}
        value={selectValue}
        disabled={isDisabled}
        onChange={(event) => onChange(event.target.value)}
        aria-label={visibleLabel ? undefined : ariaLabel}
        className={SELECT_CLASS}
      >
        {hasProfiles && !hasValidValue ? (
          <option value="" disabled>
            Chọn hồ sơ…
          </option>
        ) : null}
        {profiles.map((profile) => (
          <option key={profile.id} value={profile.id}>
            {profile.displayName}
          </option>
        ))}
      </select>
    </div>
  );
}
