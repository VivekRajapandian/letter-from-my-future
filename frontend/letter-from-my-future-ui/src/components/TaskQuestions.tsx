"use client";

import { useState } from "react";
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

type TaskQuestionsProps = {
  taskId: string;
  questions?: Question[];
  existingResponses?: Response[];
  onResponsesSubmitted?: () => void;
};

export default function TaskQuestions({
  taskId,
  questions = [],
  existingResponses = [],
  onResponsesSubmitted,
}: TaskQuestionsProps) {
  const [responses, setResponses] = useState<Map<string, string>>(() => {
    const map = new Map();
    existingResponses.forEach((r) => {
      map.set(r.questionId, r.response);
    });
    return map;
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!questions || questions.length === 0) {
    return null;
  }

  const handleResponseChange = (questionId: string, value: string) => {
    const newResponses = new Map(responses);
    newResponses.set(questionId, value);
    setResponses(newResponses);
  };

  const handleSubmit = async () => {
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
      onResponsesSubmitted?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to submit responses");
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const allAnswered = questions.every((q) => {
    const answer = responses.get(q.questionId);
    return answer && answer.trim().length > 0;
  });

  return (
    <div className="mt-8 p-6 bg-white rounded-lg border border-gray-200">
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
                placeholder="Enter your response..."
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

      <button
        onClick={handleSubmit}
        disabled={submitting || !allAnswered}
        className={`mt-6 px-6 py-2 rounded-md font-medium transition-colors ${
          allAnswered
            ? "bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-50"
            : "bg-gray-300 text-gray-500 cursor-not-allowed"
        }`}
      >
        {submitting ? "Submitting..." : "Submit Progress"}
      </button>
    </div>
  );
}
