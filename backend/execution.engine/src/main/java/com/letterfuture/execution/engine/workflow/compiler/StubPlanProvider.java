package com.letterfuture.execution.engine.workflow.compiler;

import org.springframework.stereotype.Component;

@Component
public class StubPlanProvider {

    public String getStubPlan() {

        return """
        {
          "goalTitle": "Build Muscle in 6 Months",
          "targetDurationDays": 180,
          "phases": [
            {
              "title": "Foundation",
              "durationDays": 30,
              "tasks": [
                {
                  "title": "Join a gym",
                  "description": "Select a nearby gym and enroll.",
                  "day": 1
                },
                {
                  "title": "Start full body workout",
                  "description": "Train 3x per week focusing on compound lifts.",
                  "day": 3
                }
              ]
            },
            {
              "title": "Progressive Overload",
              "durationDays": 45,
              "tasks": [
                {
                  "title": "Move to 4-day split",
                  "description": "Upper/lower routine.",
                  "day": 31
                }
              ]
            }
          ]
        }
        """;
    }
}