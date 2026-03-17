"use client";

type Props = {
  goalTitle?: string;
  phaseName?: string;
  taskIndex?: number;
  taskCount?: number;
  completedCount?: number;
  paused: boolean;
  onPause: () => void;
  onResume: () => void;
};

export default function GoalHeader({
  goalTitle,
  phaseName,
  taskIndex,
  taskCount,
  completedCount,
  paused,
  onPause,
  onResume,
}: Props) {
  const progress =
    taskCount && completedCount
      ? (completedCount / taskCount) * 100
      : 0;

  return (
    <div className="w-full max-w-xl mb-12">
      
      {/* Goal Title */}
      <h1 className="text-2xl font-semibold tracking-tight text-[#111]">
        {goalTitle || "Your Goal"}
      </h1>

      {/* Phase */}
      {phaseName && (
        <p className="text-sm text-gray-500 mt-2">
          {phaseName}
        </p>
      )}

      {/* Progress Row */}
      {taskCount && (
        <div className="mt-6">
          <div className="flex justify-between text-xs text-gray-400 mb-2">
            <span>
              Task {taskIndex} of {taskCount}
            </span>
            <span>{Math.round(progress)}%</span>
          </div>

          <div className="h-1.5 bg-gray-200 rounded-full overflow-hidden">
            <div
              className="h-full bg-black transition-all duration-500"
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>
      )}

      {/* Pause / Resume */}
      <div className="flex justify-end mt-4">
        {!paused ? (
          <button
            onClick={onPause}
            className="text-sm text-gray-500 hover:text-black transition"
          >
            Pause
          </button>
        ) : (
          <button
            onClick={onResume}
            className="text-sm text-black"
          >
            Resume
          </button>
        )}
      </div>
    </div>
  );
}