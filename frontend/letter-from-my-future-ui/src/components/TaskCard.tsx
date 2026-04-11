"use client";

import { useState } from "react";
import { motion } from "framer-motion";
import { submitTaskResponses } from "@/services/api";

type Question = {
  questionId: string;
  questionIndex: number;
  question: string;
  questionType: string;
  hint?: string;
};

type Response = {
  questionId: string;
  response: string;
};

type Props = {
  taskId: string;
  title: string;
  description: string;
  paused: boolean;
  loading: boolean;
  onComplete: () => void;
  onSkip: () => void;
  questions?: Question[];
  existingResponses?: Response[];
};

export default function TaskCard({
  taskId,
  title,
  description,
  paused,
  loading,
  onComplete,
  onSkip,
  questions = [],
  existingResponses = [],
}: Props) {
  const [responses, setResponses] = useState<Map<string, string>>(() => {
    const map = new Map();
    existingResponses.forEach((r) => {
      map.set(r.questionId, r.response);
    });
    return map;
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showValidationWarning, setShowValidationWarning] = useState(false);

  const handleResponseChange = (questionId: string, value: string) => {
    const newResponses = new Map(responses);
    newResponses.set(questionId, value);
    setResponses(newResponses);
    setShowValidationWarning(false);
  };

  const handleSubmitResponses = async () => {
    try {
      setSubmitting(true);
      setError(null);

      const responsesArray = Array.from(responses.entries()).map(
        ([questionId, response]) => ({
          questionId,
          response,
        })
      );

      await submitTaskResponses(taskId, responsesArray);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to submit responses");
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const allAnswered =
    questions.length === 0 ||
    questions.every((q) => {
      const answer = responses.get(q.questionId);
      return answer && answer.trim().length > 0;
    });

  const handleCompleteClick = async () => {
    if (questions.length > 0 && !allAnswered) {
      setShowValidationWarning(true);
      return;
    }

    // Submit responses if we have questions
    if (questions.length > 0) {
      try {
        await handleSubmitResponses();
      } catch (err) {
        console.error("Failed to submit responses:", err);
        return;
      }
    }

    onComplete();
  };

  return (
    <motion.div
      key={taskId}
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: paused ? 0.6 : 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="w-full max-w-2xl bg-white p-10 rounded-2xl shadow-sm border border-gray-100"
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

      {questions.length > 0 && (
        <div className="mt-10 pt-8 border-t border-gray-200">
          <h3 className="text-lg font-semibold text-gray-900 mb-6">
            Progress Check-in
          </h3>

          <div className="space-y-6">
            {questions.map((question) => (
              <div key={question.questionId} className="space-y-2">
                <label className="block text-sm font-medium text-gray-700">
                  {question.question}
                  {question.hint && (
                    <span className="text-xs text-gray-500 ml-2">
                      ({question.hint})
                    </span>
                  )}
                </label>

                {question.questionType === "text" && (
                  <textarea
                    value={responses.get(question.questionId) || ""}
                    onChange={(e) =>
                      handleResponseChange(question.questionId, e.target.value)
                    }
                    placeholder="Share your progress..."
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    rows={3}
                  />
                )}

                {question.questionType === "number" && (
                  <input
                    type="number"
                    value={responses.get(question.questionId) || ""}
                    onChange={(e) =>
                      handleResponseChange(question.questionId, e.target.value)
                    }
                    placeholder="Enter a number..."
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                )}

                {question.questionType === "date" && (
                  <input
                    type="date"
                    value={responses.get(question.questionId) || ""}
                    onChange={(e) =>
                      handleResponseChange(question.questionId, e.target.value)
                    }
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                )}

                {question.questionType === "boolean" && (
                  <select
                    value={responses.get(question.questionId) || ""}
                    onChange={(e) =>
                      handleResponseChange(question.questionId, e.target.value)
                    }
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">Select an option...</option>
                    <option value="true">Yes</option>
                    <option value="false">No</option>
                  </select>
                )}
              </div>
            ))}
          </div>

          {error && (
            <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-md text-red-700 text-sm">
              {error}
            </div>
          )}

          {showValidationWarning && (
            <div className="mt-4 p-3 bg-amber-50 border border-amber-200 rounded-md text-amber-700 text-sm">
              Please share your progress before completing this task. We'd love to hear how you're doing!
            </div>
          )}
        </div>
      )}

      <div className="mt-10 flex gap-4">
        <button
          onClick={handleCompleteClick}
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