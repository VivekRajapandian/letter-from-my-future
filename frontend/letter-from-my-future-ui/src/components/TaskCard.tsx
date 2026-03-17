"use client";

import { motion } from "framer-motion";

type Props = {
  taskId: string;
  title: string;
  description: string;
  paused: boolean;
  loading: boolean;
  onComplete: () => void;
  onSkip: () => void;
};

export default function TaskCard({
  taskId,
  title,
  description,
  paused,
  loading,
  onComplete,
  onSkip,
}: Props) {
  return (
    <motion.div
      key={taskId}
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: paused ? 0.6 : 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="w-full max-w-xl bg-white p-10 rounded-2xl shadow-sm border border-gray-100"
    >
      <p className="text-sm italic text-gray-400 mb-6">
        Momentum builds identity.
      </p>

      <h2 className="text-2xl font-medium leading-snug text-[#111]">
        {title}
      </h2>

      <p className="text-gray-600 mt-6 leading-relaxed">
        {description}
      </p>

      <div className="mt-10 flex gap-4">
        <button
          onClick={onComplete}
          disabled={loading || paused}
          className="px-6 py-3 bg-black text-white rounded-xl disabled:opacity-40 transition"
        >
          {loading ? "Processing..." : "Complete"}
        </button>

        <button
          onClick={onSkip}
          disabled={loading || paused}
          className="px-6 py-3 border border-gray-300 rounded-xl disabled:opacity-40 transition"
        >
          Skip
        </button>
      </div>
    </motion.div>
  );
}