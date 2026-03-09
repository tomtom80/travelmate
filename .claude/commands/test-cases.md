---
description: "Generate test cases, BDD scenarios, and E2E tests from User Stories using the QA Engineer Agent"
argument-hint: "[task: cases | bdd | e2e] [user-story-id or feature]"
model: sonnet
---

# Test Cases Command

1. Parse the arguments: `$ARGUMENTS`
2. If no task specified, ask what kind of test artifacts to generate
3. Spawn the `qs-engineer-agent` using the Agent tool with `subagent_type: qs-engineer-agent`
4. Provide the agent with:
   - The task type (test cases, BDD scenarios, or E2E tests)
   - The User Story reference or feature description
   - Existing test patterns from the codebase
5. Present generated test artifacts with coverage analysis
