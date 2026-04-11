export type SelectOption = {
  label: string;
  value: string;
};

export type TaskInputDefinition = {
  inputDefinitionId: string;
  key: string;
  label: string;
  type:
    | "TEXT"
    | "MULTILINE_TEXT"
    | "NUMBER"
    | "BOOLEAN"
    | "DATE"
    | "SELECT"
    | "PHOTO"
    | "DURATION_MINUTES"
    | "RATING_1_TO_5";
  required: boolean;
  placeholder?: string | null;
  helpText?: string | null;
  unit?: string | null;
  options?: SelectOption[] | null;
};

export type TaskInstruction = {
  what?: string | null;
  how?: string | null;
  why?: string | null;
  successCriteria?: string | null;
};

export type TaskLatestSubmission = {
  submissionId: string;
  action: string;
  submittedAt: string;
  note?: string | null;
  values: Record<string, unknown>;
};

export type ExecutionTask = {
  taskId: string;
  title: string;
  status: string;
  orderIndex: number;
  scheduledDay?: number | null;
  instruction: TaskInstruction;
  inputSchema: TaskInputDefinition[];
  latestSubmission?: TaskLatestSubmission | null;
};

export type ExecutionPlanning = {
  state: string;
  canGenerateNextPhase: boolean;
  reason?: string | null;
  sourcePhaseId?: string | null;
  nextPhaseId?: string | null;
  futureYouMessage?: string | null;
  transitionReason?: string | null;
  generatedFromSignals?: string | null;
  generatedAt?: string | null;
};

export type ExecutionSnapshot = {
  goal: {
    goalId: string;
    title: string;
    summary?: string | null;
    status: string;
    planningMode: string;
    targetDurationDays?: number | null;
    phaseCountPlanned?: number | null;
    phaseCountCreated?: number | null;
  };
  planning: ExecutionPlanning;
  activePhase: {
    phaseId: string;
    title: string;
    status: string;
    orderIndex: number;
    durationDays?: number | null;
    outlineTitle?: string | null;
  } | null;
  tasks: ExecutionTask[];
  progress: {
    completedTasks: number;
    totalVisibleTasks: number;
    phaseProgressPercent: number;
    goalProgressPercent: number;
  };
};