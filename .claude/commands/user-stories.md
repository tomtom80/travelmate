---
description: "Generate User Stories and Epics from short feature descriptions using the Requirements Engineer Agent"
argument-hint: "[feature description or backlog item]"
model: sonnet
---

# User Stories Command

1. Parse the feature description from: `$ARGUMENTS`
2. If no description provided, ask the user what feature they want to create stories for
3. Spawn the `requirements-engineer-agent` using the Agent tool with `subagent_type: requirements-engineer-agent`
4. Provide the agent with:
   - The feature description
   - Current iteration context (read `docs/backlog/product-backlog.md` for context)
   - Ubiquitous Language requirements (ADR-0011)
5. Present the generated User Stories with acceptance criteria
