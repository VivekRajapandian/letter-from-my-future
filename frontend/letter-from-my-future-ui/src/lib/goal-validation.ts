export type GoalValidationCode =
  | "EMPTY"
  | "TOO_SHORT"
  | "PLACEHOLDER"
  | "TOO_VAGUE";

export type GoalValidationResult =
  | {
      accepted: true;
      normalizedGoal: string;
    }
  | {
      accepted: false;
      code: GoalValidationCode;
      message: string;
      suggestion?: string;
    };

const MIN_CHARACTERS = 8;
const MIN_WORDS = 2;
const SHORT_VAGUE_WORD_THRESHOLD = 4;

const PLACEHOLDER_WORDS = new Set([
  "abc",
  "asdf",
  "goal",
  "idk",
  "nothing",
  "qwerty",
  "something",
  "stuff",
  "test",
  "whatever",
]);

const ACTION_WORDS = new Set([
  "apply",
  "build",
  "create",
  "exercise",
  "finish",
  "gain",
  "get",
  "improve",
  "increase",
  "launch",
  "learn",
  "lose",
  "pay",
  "practice",
  "prepare",
  "read",
  "reduce",
  "save",
  "start",
  "study",
  "switch",
  "train",
  "write",
]);

const TIME_WORDS = [
  "daily",
  "weekly",
  "monthly",
  "yearly",
  "today",
  "tomorrow",
  "week",
  "weeks",
  "month",
  "months",
  "year",
  "years",
  "day",
  "days",
];

const QUANTITY_WORDS = [
  "application",
  "applications",
  "hour",
  "hours",
  "interview",
  "interviews",
  "kg",
  "kilo",
  "kilos",
  "kilogram",
  "kilograms",
  "lb",
  "lbs",
  "project",
  "projects",
];

const VAGUE_WORDS = new Set([
  "be",
  "better",
  "do",
  "fix",
  "good",
  "improve",
  "life",
  "more",
  "myself",
  "productive",
  "successful",
  "thing",
  "things",
  "well",
]);

function normalizeGoal(rawInput: string) {
  return rawInput.replace(/\s+/g, " ").trim();
}

function getWords(input: string) {
  return input.toLowerCase().match(/[a-z0-9]+/g) ?? [];
}

function isSymbolSpam(input: string) {
  const cleaned = input.replace(/\s+/g, "");

  if (!cleaned) {
    return false;
  }

  const alphanumericCount = (cleaned.match(/[a-z0-9]/gi) ?? []).length;
  const symbolCount = cleaned.length - alphanumericCount;

  if (alphanumericCount === 0) {
    return true;
  }

  return symbolCount > alphanumericCount * 1.5;
}

function isRepeatedCharacters(input: string) {
  const cleaned = input.replace(/\s+/g, "").toLowerCase();
  return cleaned.length >= 4 && /^([a-z0-9!?.])\1+$/.test(cleaned);
}

function isPlaceholder(words: string[]) {
  if (words.length === 0) {
    return false;
  }

  return words.every((word) => PLACEHOLDER_WORDS.has(word));
}

function hasActionSignal(words: string[]) {
  return words.some((word) => ACTION_WORDS.has(word));
}

function hasTimeSignal(normalizedGoal: string) {
  const lowerInput = normalizedGoal.toLowerCase();

  if (/\bin\s+\d+\s+(day|days|week|weeks|month|months|year|years)\b/.test(lowerInput)) {
    return true;
  }

  if (/\bby\s+[a-z]+\b/.test(lowerInput) || /\bevery\s+(day|week|month)\b/.test(lowerInput)) {
    return true;
  }

  return TIME_WORDS.some((word) => lowerInput.includes(word));
}

function hasQuantitySignal(normalizedGoal: string, words: string[]) {
  return /\d/.test(normalizedGoal) || words.some((word) => QUANTITY_WORDS.includes(word));
}

function isLikelyVague(words: string[]) {
  if (words.length === 0) {
    return false;
  }

  const vagueWordCount = words.filter((word) => VAGUE_WORDS.has(word)).length;
  return vagueWordCount / words.length >= 0.6;
}

export function validateGoal(rawInput: string): GoalValidationResult {
  const normalizedGoal = normalizeGoal(rawInput);

  if (!normalizedGoal) {
    return {
      accepted: false,
      code: "EMPTY",
      message: "Enter a goal to continue.",
    };
  }

  const words = getWords(normalizedGoal);

  if (
    normalizedGoal.length < MIN_CHARACTERS ||
    words.length < MIN_WORDS ||
    isSymbolSpam(normalizedGoal) ||
    isRepeatedCharacters(normalizedGoal)
  ) {
    return {
      accepted: false,
      code: "TOO_SHORT",
      message: "Please add a little more detail so we can build a useful plan.",
    };
  }

  if (isPlaceholder(words)) {
    return {
      accepted: false,
      code: "PLACEHOLDER",
      message: "Please enter a real goal, not a placeholder.",
    };
  }

  const hasAction = hasActionSignal(words);
  const hasTime = hasTimeSignal(normalizedGoal);
  const hasQuantity = hasQuantitySignal(normalizedGoal, words);
  const vagueOnly = isLikelyVague(words);

  if (hasAction || hasTime || hasQuantity) {
    return {
      accepted: true,
      normalizedGoal,
    };
  }

  if (words.length >= 3 && !vagueOnly) {
    return {
      accepted: true,
      normalizedGoal,
    };
  }

  if (words.length <= SHORT_VAGUE_WORD_THRESHOLD && vagueOnly) {
    return {
      accepted: false,
      code: "TOO_VAGUE",
      message: "Try describing what you want to improve or achieve more specifically.",
      suggestion: "Example: Prepare for backend interviews in 3 months.",
    };
  }

  return {
    accepted: true,
    normalizedGoal,
  };
}
