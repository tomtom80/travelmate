# Business

This directory holds Travelmate's go-to-market and business strategy documentation, kept side-by-side with the code so it can evolve with the product instead of drifting in a separate Notion workspace.

## Documents

- **[business-model-and-strategy.md](business-model-and-strategy.md)** — the main strategy document: market analysis, competitor landscape, business model canvas, pricing, go-to-market roadmap, and a concrete 30-day action list.

## Conventions

- **Markdown only** — same toolchain as the rest of the repo.
- **DACH-first language for content** — strategy docs are written in a mix of German (domain language, market context) and English (technical/architectural content), matching the bilingual product.
- **Backlog-bridge required** — every business-strategy decision that requires code work must trace to a story in `docs/backlog/iteration-N-plan.md`. No silo'd business plans without engineering follow-through.
- **Update on insight, not on schedule** — the strategy doc is not a recurring deliverable. It changes when assumptions shift (customer interviews, traction data, new competitor moves), not on a quarterly cadence.

## Adjacent docs

- `docs/arc42/` — architecture rationale (technical decisions)
- `docs/backlog/` — iteration plans (delivery roadmap)
- `docs/operations/` — running-the-business artifacts (legal, deployment, incident response)
