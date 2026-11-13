# FraudShield 3D graph integration - new coding-chat prompt

Use this document as the complete prompt for a fresh goal-oriented coding chat.

## Prompt to paste into the new chat

Work in:

`C:\Users\chity\Desktop\REFERENCES\fraudshield-main`

The objective is to finish FraudShield as a clear, credible real-time fraud-risk
demonstration and to add a Mule-Hunter-inspired 3D fraud-network experience.

Before changing code, read:

- `docs/STREAMSHIELD_COMPLETION_GOAL.md`
- `docs/HOW-IT-WORKS.md`
- `docs/DATA_PLAN.md`
- `ProblemStatement.txt` at `C:\Users\chity\Desktop\Vasavi\ProblemStatement.txt`
- the current frontend and backend graph implementations

Study this reference experience in a browser:

`https://mule-hunter.vercel.app/network`

Also inspect the local reference project, without copying code or data:

`C:\Users\chity\Desktop\REFERENCES\MULE_HUNTER-main`

Important local reference files include:

- `control-tower/app/components/graph/FraudGraph3D.tsx`
- `control-tower/app/network/page.tsx`
- `ai-engine/data_generator.py`
- `shared-data/nodes.csv`
- `shared-data/transactions.csv`

## Main product behavior

FraudShield receives or replays payment events, scores each payment in real time,
and returns an auditable decision such as APPROVE, REVIEW, or BLOCK. The graph is
an investigation and explanation surface for the payment network. It must not imply
that a synthetic simulator is a live connection to a bank or UPI production system.

Keep the existing Java/Spring/Kafka/Redis/PostgreSQL architecture. Do not replace
the backend with a new stack and do not introduce an unrelated data source.

## 3D graph requirements

Implement an original FraudShield 3D graph UI with the same useful interaction model
as Mule Hunter, adapted to the existing FraudShield dark/blue visual language:

1. Use a 3D force-directed network with pan, orbit, zoom, hover and click inspection.
2. Show payer, payee, device, account and other supported entity nodes only when the
   backend actually provides those entities. Do not invent relationships in the UI.
3. Draw directed payment/relationship links with amount, count, direction and risk
   context where available.
4. Use risk-aware node colors and sizes. A node's visual importance may use graph
   degree, PageRank-like centrality, amount volume or mule risk, but label the meaning
   clearly in the UI.
5. Show useful roles such as NORMAL, HUB, BRIDGE and MULE only when derived from
   implemented backend fields or explicitly documented heuristics.
6. Support search, risk threshold filtering, fraud/ring-only filtering and reset view.
7. Clicking a node opens an inspector with node id, entity type, risk score, reasons,
   degree/volume, related cases and recent transactions.
8. Highlight the selected node's neighborhood and the path/ring that explains the
   current alert.
9. Provide ring/cluster annotations for supported patterns such as STAR, CHAIN,
   CYCLE and DENSE. These labels must come from backend graph analysis or be clearly
   marked as demo heuristics.
10. Make the graph usable for both target users:
    - fraud analysts need investigation, explanations, cases and filtering;
    - operations/model users need live status, risk policy, drift and evaluation links.
11. Preserve the existing navigation flow and frontend theme. The 3D graph should be
    reachable from the investigation/cases workflow and from the live operations view,
    not be an isolated showcase page.
12. Provide a 2D/list fallback for browsers or machines where WebGL/3D is unavailable.

The goal is visual and functional parity with the reference experience, not a literal
copy of its source, assets, branding, or implementation. Recreate the interaction
concepts with original FraudShield code and compatible dependencies. Respect the
licenses of all referenced repositories.

## Backend contract and data rules

First inspect the existing graph endpoints and Redis graph state. Prefer extending the
current graph service and controller rather than creating a second graph pipeline.

The backend should expose the minimum stable contract needed by the UI, for example:

- graph nodes with id, type, risk score, role, centrality/volume and explanation data;
- directed links with source, target, amount/count and relationship type;
- optional selected-node neighborhood and path/ring information;
- timestamps and data-source metadata;
- a clear `synthetic` or `public_evaluation` provenance flag.

The live graph must use actual FraudShield payment events and the existing Redis/Kafka
flow. A simulator edge must not be presented as a confirmed bank transaction. Keep
`simLabel`/ground truth out of online model features. Ground truth may be used only by
evaluation and must be visibly labelled as privileged simulator truth.

Do not import Mule Hunter's IEEE-CIS card-fraud graph as if it were UPI data. If its
data-generation or graph ideas are discussed, document that IEEE-CIS is card-fraud
data and that shared-attribute edges are not necessarily money movement. FraudShield's
own payment edges should remain the primary live-demo graph semantics.

## Required work sequence

1. Audit the current graph backend, frontend graph, routes, state management and tests.
2. Record the existing API shape before modifying it.
3. Stabilize or extend the graph API and add backend tests.
4. Implement the 3D graph component and a safe 2D/list fallback.
5. Integrate node inspection, case navigation, live updates and filters.
6. Make labels explicit: synthetic traffic, simulator ground truth, public data and
   real-world production data must never be conflated.
7. Run backend tests, frontend type-check/build and Docker validation.
8. Verify the flow manually with baseline, adaptive-wave and cold-start simulator data.

## Acceptance criteria

- The live dashboard and case workflow can open a 3D network view.
- The graph contains real FraudShield stream edges, not hard-coded Mule Hunter data.
- Nodes and links are connected, directional and inspectable.
- Search/filter/reset, zoom/orbit, node selection and neighborhood highlighting work.
- Ring/role labels are explainable and traceable to backend data or documented rules.
- A browser without usable WebGL receives a functional fallback.
- The UI matches FraudShield's existing theme and does not expose unnecessary demo-only
  controls to the wrong target user.
- Every new API and evaluator has tests; frontend build/type-check passes.
- Docker validation passes.
- No synthetic or public-data metric is described as real-world performance.
- No source code, model weights, data files, branding or assets are copied from
  Mule Hunter or any other reference project without a compatible license.

## Final response required from the coding chat

Report:

- changed files;
- graph API changes;
- frontend routes and user flows;
- how node/link risk and roles are computed;
- test, build and Docker commands with results;
- screenshots or manual verification notes;
- data provenance and limitations;
- remaining blockers, if any.

Keep latency and throughput benchmarking separate from this implementation report.
