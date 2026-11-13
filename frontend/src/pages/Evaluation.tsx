import { useEffect, useState } from 'react'
import { PageHeader } from '../components/Layout'
import { Badge, Card, ErrorNote, Stat, StatGrid } from '../components/ui'
import { api, inr } from '../api'

type Metrics = { prAuc: number; precision: number; recall: number; fpr: number; reviewRate: number; blockRate: number; fraudValueStopped: number; totalCost: number; costPer1000: number; netPreventedLoss: number }
type Report = { dataType: string; seed: number; futureLabelsUsedForScoring: boolean; timeSplit: string[]; fraudRateShift: number; decisionRateShift: number; psi: Record<string, number>; baseline: Metrics; adaptiveWave: Metrics; comparisonReports: Record<string, Metrics>; coldStartCohorts: Record<string, { samples: number; blockedRate: number }> }

const pct = (v: number) => `${(v * 100).toFixed(1)}%`
const title = (s: string) => s.replaceAll('-', ' ').replace(/\b\w/g, c => c.toUpperCase())

export default function Evaluation() {
  const [report, setReport] = useState<Report | null>(null)
  const [error, setError] = useState<string | null>(null)
  useEffect(() => { api<Report>('/api/risk/evaluation?seed=20261113').then(setReport).catch(e => setError(e.message)) }, [])
  return <>
    <PageHeader title="Model evaluation" subtitle="For fraud-team leads: time-split drift, cold-start safety, and cost trade-offs. These results are synthetic evaluation evidence, not real-world performance." action={<Badge tone="amber">Synthetic evaluation</Badge>} />
    <ErrorNote error={error} />
    <StatGrid className="grid-cols-2 lg:grid-cols-5">
      <Stat label="Risk-score PSI" value={report?.psi.riskScore.toFixed(3) ?? '-'} hint="baseline → adaptive wave" tone="indigo" />
      <Stat label="Fraud-rate shift" value={report ? pct(report.fraudRateShift) : '-'} hint="time-split change" tone="red" />
      <Stat label="Adaptive recall" value={report ? pct(report.adaptiveWave.recall) : '-'} hint="evaluation-only labels" tone="green" />
      <Stat label="Adaptive FPR" value={report ? pct(report.adaptiveWave.fpr) : '-'} hint="legitimate payments flagged" />
      <Stat label="Cost / 1,000" value={report ? inr(report.adaptiveWave.costPer1000) : '-'} hint="INR synthetic estimate" tone="amber" />
    </StatGrid>
    <div className="mt-4 grid gap-4 xl:grid-cols-2">
      <Card title="Time-based evaluation">
        <div className="mb-4 flex flex-wrap gap-2">{report?.timeSplit.map((s, i) => <span key={s} className="rounded-md border border-line px-2.5 py-1 text-xs text-fg-2">{i + 1}. {title(s)}</span>)}</div>
        <div className="grid grid-cols-2 gap-3 text-sm"><Metric label="Baseline PR-AUC" value={report?.baseline.prAuc.toFixed(3)} /><Metric label="Adaptive PR-AUC" value={report?.adaptiveWave.prAuc.toFixed(3)} /><Metric label="Review-rate shift" value={report ? pct(report.decisionRateShift) : '-'} /><Metric label="Fraud value stopped" value={report ? inr(report.adaptiveWave.fraudValueStopped) : '-'} /></div>
        <p className="mt-4 text-xs text-muted">Future labels used for scoring: {report?.futureLabelsUsedForScoring ? 'yes' : 'no'}. Seed: {report?.seed ?? '-'}</p>
      </Card>
      <Card title="Cold-start cohorts">
        <table className="w-full text-sm"><thead className="text-left text-xs text-muted"><tr><th className="pb-2">Cohort</th><th className="pb-2">Samples</th><th className="pb-2 text-right">Blocked rate</th></tr></thead><tbody className="divide-y divide-line">{Object.entries(report?.coldStartCohorts ?? {}).map(([name, c]) => <tr key={name}><td className="py-2">{title(name)}</td><td className="py-2 tabular-nums">{c.samples}</td><td className="py-2 text-right tabular-nums">{pct(c.blockedRate)}</td></tr>)}</tbody></table>
        <p className="mt-3 text-xs text-muted">A legitimate first payment is a negative control; new customers are not automatically blocked.</p>
      </Card>
    </div>
    <Card className="mt-4" title="Baseline comparison">
      <div className="overflow-x-auto"><table className="w-full min-w-[760px] text-sm"><thead className="text-left text-xs text-muted"><tr><th className="pb-2">Policy</th><th>PR-AUC</th><th>Precision</th><th>Recall</th><th>FPR</th><th>Review</th><th>Block</th><th>INR cost</th></tr></thead><tbody className="divide-y divide-line">{Object.entries(report?.comparisonReports ?? {}).map(([name, m]) => <tr key={name}><td className="py-2 font-medium">{title(name)}</td><td>{m.prAuc.toFixed(3)}</td><td>{pct(m.precision)}</td><td>{pct(m.recall)}</td><td>{pct(m.fpr)}</td><td>{pct(m.reviewRate)}</td><td>{pct(m.blockRate)}</td><td>{inr(m.totalCost)}</td></tr>)}</tbody></table></div>
    </Card>
  </>
}

function Metric({ label, value }: { label: string; value?: string }) { return <div className="rounded-lg bg-surface-2 p-3"><div className="text-xs text-muted">{label}</div><div className="mt-1 font-mono text-lg text-fg">{value ?? '-'}</div></div> }
