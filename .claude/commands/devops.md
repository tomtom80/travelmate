---
description: "Design CI/CD pipelines, infrastructure automation, and SRE practices using the DevOps & SRE Engineer Agent"
argument-hint: "[task: pipeline | terraform | ansible | docker | slo | monitoring | incident | capacity] [scope]"
model: sonnet
---

# DevOps & SRE Command

## Available Tasks

| Task | Description |
|------|-------------|
| `pipeline` | Design CI/CD pipeline (GitHub Actions / GitLab CI) |
| `terraform` | Infrastructure as Code modules |
| `ansible` | Configuration management playbooks |
| `docker` | Docker/Compose optimization |
| `slo` | Define SLIs, SLOs, and error budget policies |
| `monitoring` | Set up metrics, alerting, and dashboards (Four Golden Signals) |
| `incident` | Incident response procedures and postmortem templates |
| `capacity` | Capacity planning and load testing strategy |
| `toil` | Identify and plan toil elimination |
| `runbook` | Create operational runbooks |

## Instructions

1. Parse the arguments: `$ARGUMENTS`
2. If no task specified, ask what DevOps/SRE task to perform
3. Spawn the `devops-engineer-agent` using the Agent tool with `subagent_type: devops-engineer-agent`
4. Provide the agent with:
   - The task type
   - Target scope and environment
   - Current infrastructure state (docker-compose.yml, Dockerfile)
   - For SRE tasks: reference quality scenarios from `docs/arc42/10-quality-requirements.md`
5. Present artifacts with implementation instructions
