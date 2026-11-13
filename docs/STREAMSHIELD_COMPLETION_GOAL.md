# StreamShield completion goal (non-performance scope)

## Scope

Complete the StreamShield problem statement requirements other than latency and throughput.
Keep latency/speed benchmarking as a separate workstream.

The current base project is `fraudshield-main`. It already has Kafka, Redis, PostgreSQL,
an explainable risk scorer, analyst cases, feedback, a mule graph and a React dashboard.
Use the other repositories as design references; do not copy code whose license is unclear.

## Already completed in this pass

- Risk decisions now return at most three reason codes.
- The default background simulator fraud prevalence is 0.1% or lower.
- Added an adaptive second-wave scenario with changed amounts, paths and timing.
- Added a cold-start scam scenario with a new payer, device and payee.
- Added both scenarios to the live dashboard.

## Remaining P0 requirements

### 1. Explicit cost-aware policy

Implement an auditable INR cost model for every decision:

- fraud loss prevented by BLOCK and REVIEW;
- genuine-payment friction cost for REVIEW and BLOCK;
- missed-fraud loss for APPROVE;
- total cost, cost per 1,000 payments and net prevented loss.

Make the costs configurable, expose them through `/api/risk/cost-policy`, and show the
trade-off in the Model page. Add a policy evaluator that sweeps review/block thresholds
and selects the lowest expected INR cost subject to a configurable maximum friction rate.

Use `TRACER-Real-Time-Mule-Ring-Defense-main/COST_MODEL.md` and Prahari's alert-budget
policy as references, but keep the implementation native to the Java risk-engine.

### 2. Real drift and adaptive-wave evaluation

Add a reproducible evaluation command or endpoint that runs:

1. baseline traffic;
2. training/validation period;
3. changed adaptive fraud wave;
4. evaluation without using future labels;
5. optional champion/challenger retraining comparison.

Report PSI for important features, fraud-rate shift, decision-rate shift, recall,
precision, false-positive rate and fraud value stopped. Do not use `simLabel` as an input
feature. Keep it only as an evaluation label.

Use Prahari's time-based evaluation and PSI monitoring, StreamShield's `unknown-adaptive`
scenario, and `upi-fraud-ring-detection-main`'s streaming replay design as references.

### 3. Cold-start evaluation

Create explicit cohorts for:

- first payment by a new payer;
- new device;
- new payee;
- new payer and payee together;
- established legitimate customer.

Show performance by cohort and ensure the engine does not automatically block every
new customer. Add a negative-control test for a legitimate first payment.

### 4. Reproducible data plan

Add `docs/DATA_PLAN.md` documenting:

- synthetic generator and scenario labels;
- train/validation/test time split;
- how ground truth is kept separate from features;
- public-data evaluation path;
- privacy and anonymisation rules;
- known limitations of synthetic metrics.

The IBM HI-Small data mentioned in Mule-Net is not present in that repository. Do not
claim to evaluate on it unless it is obtained separately with its license and checksum.

### 5. Evaluation and baseline report

Add a generated report comparing:

- current logistic model;
- rules-only baseline;
- graph-only baseline;
- combined model + graph policy.

Metrics must include PR-AUC, precision, recall, FPR, fraud-value stopped, review rate,
block rate, false alerts per 10,000 legitimate payments and the INR cost model.

### 6. Minimum demo correctness

- Live monitor must use backend data, not the landing-page hard-coded scoring feed.
- Graph view must show the ring formed by the current stream.
- Each decision must show no more than three reason codes.
- Dashboard must clearly label synthetic traffic and privileged simulator ground truth.
- Case pages must distinguish simulator truth from analyst-confirmed labels.

## P1 stretch requirements

- Add privacy-preserving cross-bank feature sharing design and a local demo using keyed,
  non-reversible identifiers. Mule-Net's `docs/data_sharing.md` is the design reference.
- Add step-up outcome simulation rather than treating REVIEW as a final outcome.
- Add analyst feedback delay and delayed-label training.
- Add red-team adaptive variants that attempt to stay below known thresholds.

## Acceptance criteria

- `mvn test` passes for the affected services.
- Frontend type-check/build passes.
- A clean local run can demonstrate baseline, adaptive-wave and cold-start cohorts.
- `/api/risk/cost-policy` returns the active cost assumptions.
- Evaluation output includes time-split metrics, PSI and INR cost metrics.
- No metric is described as real-world performance; synthetic and public-data results are
  labelled separately.
- No code or data is copied from a repository without a compatible license or explicit
  permission.

## Prompt for a new coding chat

> Work in `C:\Users\chity\Desktop\REFERENCES\fraudshield-main`.
>
> Complete the non-performance requirements in `docs/STREAMSHIELD_COMPLETION_GOAL.md`.
> Keep latency and throughput benchmarking separate. Start by reading the current
> implementation, then implement P0 items in this order: cost-aware policy, time-based
> adaptive-wave evaluation with PSI drift, cold-start cohort evaluation, data plan,
> baseline comparison report, and dashboard correctness. Reuse ideas only from the
> listed references; do not copy code from repositories with unclear licensing.
>
> Preserve the existing Java/Spring/Kafka/Redis/PostgreSQL architecture. Keep simulator
> ground truth out of model features. Use immutable, reproducible seeds for evaluation.
> Add unit/integration tests for every new endpoint and evaluator. Update the React UI
> only after the backend contract is stable. Run tests and build the affected services,
> then report changed files, evidence, limitations and any remaining blockers. Do not
> claim production or real-world fraud performance from synthetic data.

