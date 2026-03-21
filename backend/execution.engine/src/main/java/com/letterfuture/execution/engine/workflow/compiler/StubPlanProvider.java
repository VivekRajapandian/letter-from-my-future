package com.letterfuture.execution.engine.workflow.compiler;

import org.springframework.stereotype.Component;

@Component
public class StubPlanProvider {

    public String getStubPlan() {

        return """
        {{
                     "id": "resp_03fac451366ea34b0069b9d83f81f08198ac2d7f2d861f6160",
                     "object": "response",
                     "created_at": 1773787199,
                     "status": "completed",
                     "background": false,
                     "billing": {
                         "payer": "developer"
                     },
                     "completed_at": 1773787214,
                     "error": null,
                     "frequency_penalty": 0.0,
                     "incomplete_details": null,
                     "instructions": null,
                     "max_output_tokens": null,
                     "max_tool_calls": null,
                     "model": "gpt-4o-mini-2024-07-18",
                     "output": [
                         {
                             "id": "msg_03fac451366ea34b0069b9d8406b188198b37412d780285fc2",
                             "type": "message",
                             "status": "completed",
                             "content": [
                                 {
                                     "type": "output_text",
                                     "annotations": [],
                                     "logprobs": [],
                                     "text": "{\\"goal_title\\":\\"Gain 10 kg Muscle in 6 Months\\",\\"target_duration_days\\":180,\\"phases\\":[{\\"title\\":\\"New Foundation and Nutritional Assessment\\",\\"duration_days\\":30,\\"tasks\\":[{\\"title\\":\\"Consult a Nutritionist\\",\\"description\\":\\"Meet with a nutritionist to create a personalized eating plan focused on muscle gain.\\",\\"day\\":1},{\\"title\\":\\"Track Daily Caloric Intake\\",\\"description\\":\\"Begin tracking daily food intake to ensure adequate calorie consumption.\\",\\"day\\":2},{\\"title\\":\\"Establish a Baseline Workout Routine\\",\\"description\\":\\"Create a beginner workout routine focusing on compound lifts.\\",\\"day\\":3},{\\"title\\":\\"Meal Prep for the Week\\",\\"description\\":\\"Prepare meals for the week to ensure adherence to the eating plan.\\",\\"day\\":7},{\\"title\\":\\"Start Supplementing Protein\\",\\"description\\":\\"Incorporate a protein supplement post-workout to support muscle growth.\\",\\"day\\":15},{\\"title\\":\\"Evaluate Initial Progress\\",\\"description\\":\\"Assess weight change and adjust caloric intake as necessary.\\",\\"day\\":30}]},{\\"title\\":\\"Strength Building Phase\\",\\"duration_days\\":60,\\"tasks\\":[{\\"title\\":\\"Increase Workout Intensity\\",\\"description\\":\\"Gradually increase weights in current routine to stimulate muscle hypertrophy.\\",\\"day\\":31},{\\"title\\":\\"Add Weight Training Split\\",\\"description\\":\\"Implement a training split (upper/lower or push/pull) for focused muscle growth.\\",\\"day\\":40},{\\"title\\":\\"Focus on Protein-Rich Meals\\",\\"description\\":\\"Ensure every meal contains a source of protein, targeting 1.5-2g of protein per kg of weight.\\",\\"day\\":46},{\\"title\\":\\"Incorporate Supplementation\\",\\"description\\":\\"Introduce creatine to help increase strength and muscle mass.\\",\\"day\\":50},{\\"title\\":\\"Mid-Phase Assessment\\",\\"description\\":\\"Review progress and adapt meal and workout plans as needed.\\",\\"day\\":60}]},{\\"title\\":\\"Hypertrophy and Endurance\\",\\"duration_days\\":45,\\"tasks\\":[{\\"title\\":\\"Incorporate Higher Reps Training\\",\\"description\\":\\"Focus on higher reps (8-12) for hypertrophy in select muscle groups.\\",\\"day\\":61},{\\"title\\":\\"Increase Caloric Surplus\\",\\"description\\":\\"Adjust meal plan to increase daily caloric intake by 200-300 calories.\\",\\"day\\":65},{\\"title\\":\\"Include Cardio Sessions\\",\\"description\\":\\"Add moderate cardio sessions to maintain cardiovascular health without hindering muscle gain.\\",\\"day\\":70},{\\"title\\":\\"Focus on Recovery Techniques\\",\\"description\\":\\"Implement stretching and foam rolling sessions to promote recovery.\\",\\"day\\":75},{\\"title\\":\\"Progress Evaluation\\",\\"description\\":\\"Evaluate muscle gain and make adjustments to the routine.\\",\\"day\\":105}]},{\\"title\\":\\"Power and Performance Phase\\",\\"duration_days\\":30,\\"tasks\\":[{\\"title\\":\\"Start a Powerlifting-Style Routine\\",\\"description\\":\\"Shift to a powerlifting routine focusing on squats, deadlifts, and bench press.\\",\\"day\\":106},{\\"title\\":\\"Add Olympic Lifts Training\\",\\"description\\":\\"Incorporate clean and jerk, and snatch for explosive strength.\\",\\"day\\":112},{\\"title\\":\\"Monitor Nutritional Timing\\",\\"description\\":\\"Focus on pre- and post-workout nutrition to maximize training benefits.\\",\\"day\\":120},{\\"title\\":\\"Engage in Active Recovery\\",\\"description\\":\\"Include active recovery weekends with light activities like walking or yoga.\\",\\"day\\":125},{\\"title\\":\\"Assess Max Lifts\\",\\"description\\":\\"Test for maximum lifts in your main compound movements.\\",\\"day\\":135}]},{\\"title\\":\\"Final Push for Muscle Gain\\",\\"duration_days\\":15,\\"tasks\\":[{\\"title\\":\\"Refine Meal Plan for Last Phase\\",\\"description\\":\\"Refine caloric intake focusing on whole foods and nutrient-dense meals.\\",\\"day\\":136},{\\"title\\":\\"Ensure Consistent Training\\",\\"description\\":\\"Stick to a consistent training schedule without missing workouts.\\",\\"day\\":140},{\\"title\\":\\"Prioritize Sleep and Recovery\\",\\"description\\":\\"Aim for 7-9 hours of sleep per night to optimize recovery.\\",\\"day\\":145},{\\"title\\":\\"Mental Fortitude Practices\\",\\"description\\":\\"Incorporate mental practices such as visualization or positive self-talk to enhance commitment.\\",\\"day\\":150},{\\"title\\":\\"Final Weight Assessment\\",\\"description\\":\\"Assess final weight and physique transformation. Document progress and establish long-term goals.\\",\\"day\\":180}]}]}"
                                 }
                             ],
                             "role": "assistant"
                         }
                     ],
                     "parallel_tool_calls": true,
                     "presence_penalty": 0.0,
                     "previous_response_id": null,
                     "prompt_cache_key": null,
                     "prompt_cache_retention": null,
                     "reasoning": {
                         "effort": null,
                         "summary": null
                     },
                     "safety_identifier": null,
                     "service_tier": "default",
                     "store": true,
                     "temperature": 1.0,
                     "text": {
                         "format": {
                             "type": "json_schema",
                             "description": null,
                             "name": "goal_plan",
                             "schema": {
                                 "type": "object",
                                 "additionalProperties": false,
                                 "properties": {
                                     "goal_title": {
                                         "type": "string"
                                     },
                                     "target_duration_days": {
                                         "type": "integer"
                                     },
                                     "phases": {
                                         "type": "array",
                                         "items": {
                                             "type": "object",
                                             "additionalProperties": false,
                                             "properties": {
                                                 "title": {
                                                     "type": "string"
                                                 },
                                                 "duration_days": {
                                                     "type": "integer"
                                                 },
                                                 "tasks": {
                                                     "type": "array",
                                                     "items": {
                                                         "type": "object",
                                                         "additionalProperties": false,
                                                         "properties": {
                                                             "title": {
                                                                 "type": "string"
                                                             },
                                                             "description": {
                                                                 "type": "string"
                                                             },
                                                             "day": {
                                                                 "type": "integer"
                                                             }
                                                         },
                                                         "required": [
                                                             "title",
                                                             "description",
                                                             "day"
                                                         ]
                                                     }
                                                 }
                                             },
                                             "required": [
                                                 "title",
                                                 "duration_days",
                                                 "tasks"
                                             ]
                                         }
                                     }
                                 },
                                 "required": [
                                     "goal_title",
                                     "target_duration_days",
                                     "phases"
                                 ]
                             },
                             "strict": true
                         },
                         "verbosity": "medium"
                     },
                     "tool_choice": "auto",
                     "tools": [],
                     "top_logprobs": 0,
                     "top_p": 1.0,
                     "truncation": "disabled",
                     "usage": {
                         "input_tokens": 192,
                         "input_tokens_details": {
                             "cached_tokens": 0
                         },
                         "output_tokens": 823,
                         "output_tokens_details": {
                             "reasoning_tokens": 0
                         },
                         "total_tokens": 1015
                     },
                     "user": null,
                     "metadata": {}
                 }
        """;
    }
}
