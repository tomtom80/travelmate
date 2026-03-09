---
description: "Create UX design artifacts — user journey maps, component specs, wireframes — using the UX Designer Agent"
argument-hint: "[task: journey | component | wireframe] [feature or flow]"
model: sonnet
---

# UX Design Command

1. Parse the arguments: `$ARGUMENTS`
2. If no task specified, ask what UX artifact to create
3. Spawn the `ux-designer-agent` using the Agent tool with `subagent_type: ux-designer-agent`
4. Provide the agent with:
   - The design task type
   - The feature or user flow to design
   - Travelmate's design constraints (PicoCSS + HTMX + Thymeleaf)
5. Present design artifacts with implementation notes
