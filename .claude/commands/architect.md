---
description: "Run the Architect Agent for DDD modeling, EventStorming, architecture documentation, or architecture review"
argument-hint: "[task: eventstorming | domain-story | quality-storm | arc42 | adr | archunit | review] [context]"
model: opus
---

# Architect Command

You are orchestrating the Architect Agent. Based on the user's request, invoke the appropriate workflow.

## Available Tasks

| Task | Description |
|------|-------------|
| `eventstorming` | Facilitate an EventStorming session for a bounded context or process |
| `domain-story` | Create a Domain Storytelling model for a workflow |
| `quality-storm` | Run Quality Storming to discover and prioritize quality attributes |
| `arc42` | Update arc42 documentation sections |
| `adr` | Write a new Architecture Decision Record |
| `archunit` | Create ArchUnit architecture fitness tests |
| `review` | Perform a full architecture review (ATAM + conformance check) |

## Instructions

1. Parse the arguments: `$ARGUMENTS`
2. If no task specified, ask the user which task they want using the AskUserQuestion tool
3. Spawn the `architect-agent` using the Agent tool with `subagent_type: architect-agent`
4. Provide the agent with:
   - The specific task to perform
   - Any additional context from the arguments
   - Reference to relevant existing files (ADRs, arc42 docs, source code)
5. Present the agent's results to the user with a summary
