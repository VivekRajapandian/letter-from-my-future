"use client";

import { TaskInputDefinition } from "@/types/execution";

type TaskInputFormProps = {
  inputs: TaskInputDefinition[];
  values: Record<string, unknown>;
  onChange: (nextValues: Record<string, unknown>) => void;
};

export default function TaskInputForm({
  inputs,
  values,
  onChange,
}: TaskInputFormProps) {
  if (!inputs || inputs.length === 0) {
    return null;
  }

  const setValue = (key: string, value: unknown) => {
    onChange({
      ...values,
      [key]: value,
    });
  };

  return (
    <div className="space-y-4">
      <div className="text-sm font-semibold uppercase tracking-[0.14em] text-white/55">
        Task inputs
      </div>

      {inputs.map((input) => {
        const currentValue = values[input.inputDefinitionId] ?? "";

        return (
          <div
            key={input.inputDefinitionId}
            className="rounded-2xl border border-white/10 bg-black/20 p-4"
          >
            <label className="block">
              <div className="flex items-center gap-2">
                <span className="text-sm font-medium text-white">{input.label}</span>
                {input.required ? (
                  <span className="text-xs text-red-300">*</span>
                ) : null}
              </div>

              {input.helpText ? (
                <p className="mt-1 text-xs leading-5 text-white/50">{input.helpText}</p>
              ) : null}

              <div className="mt-3">
                {renderField(input, currentValue, (value) =>
                  setValue(input.inputDefinitionId, value)
                )}
              </div>

              {input.unit ? (
                <div className="mt-2 text-xs text-white/45">Unit: {input.unit}</div>
              ) : null}
            </label>
          </div>
        );
      })}
    </div>
  );
}

function renderField(
  input: TaskInputDefinition,
  value: unknown,
  onValueChange: (value: unknown) => void
) {
  const commonClassName =
    "w-full rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-white outline-none transition placeholder:text-white/30 focus:border-white/25 focus:bg-white/10";

  switch (input.type) {
    case "MULTILINE_TEXT":
      return (
        <textarea
          className={`${commonClassName} min-h-[110px] resize-y`}
          placeholder={input.placeholder ?? ""}
          value={String(value ?? "")}
          onChange={(e) => onValueChange(e.target.value)}
        />
      );

    case "NUMBER":
    case "DURATION_MINUTES":
    case "RATING_1_TO_5":
      return (
        <input
          type="number"
          className={commonClassName}
          placeholder={input.placeholder ?? ""}
          value={value === null || value === undefined ? "" : String(value)}
          onChange={(e) =>
            onValueChange(e.target.value === "" ? "" : Number(e.target.value))
          }
        />
      );

    case "BOOLEAN":
      return (
        <label className="inline-flex items-center gap-3 rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-white">
          <input
            type="checkbox"
            checked={Boolean(value)}
            onChange={(e) => onValueChange(e.target.checked)}
          />
          <span>Yes</span>
        </label>
      );

    case "DATE":
      return (
        <input
          type="date"
          className={commonClassName}
          value={String(value ?? "")}
          onChange={(e) => onValueChange(e.target.value)}
        />
      );

    case "SELECT":
      return (
        <select
          className={commonClassName}
          value={String(value ?? "")}
          onChange={(e) => onValueChange(e.target.value)}
        >
          <option value="">Select an option</option>
          {(input.options ?? []).map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      );

    case "PHOTO":
      return (
        <input
          type="text"
          className={commonClassName}
          placeholder={input.placeholder ?? "Paste image URL or reference"}
          value={String(value ?? "")}
          onChange={(e) => onValueChange(e.target.value)}
        />
      );

    case "TEXT":
    default:
      return (
        <input
          type="text"
          className={commonClassName}
          placeholder={input.placeholder ?? ""}
          value={String(value ?? "")}
          onChange={(e) => onValueChange(e.target.value)}
        />
      );
  }
}