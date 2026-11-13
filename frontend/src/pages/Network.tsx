import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { PageHeader } from '../components/Layout'
import { Badge, Button, Card } from '../components/ui'
import { api, compactInr, type Graph } from '../api'

type Node = Graph['nodes'][number] & { x?: number; y?: number; z?: number }
type LinkData = { source: string | Node; target: string | Node; amount: number; count: number; riskScore: number }
type ForceGraph = React.ComponentType<any>
const idOf = (v: string | Node) => typeof v === 'object' ? v.id : v
const colorOf = (risk: number) => risk >= .7 ? '#ff6363' : risk >= .3 ? '#f5b03d' : '#3ecf8e'

export default function Network() {
  const [graph, setGraph] = useState<Graph | null>(null)
  const [forceGraph, setForceGraph] = useState<ForceGraph | null>(null)
  const [selected, setSelected] = useState<Node | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [query, setQuery] = useState('')
  const [threshold, setThreshold] = useState(0)
  const [fraudOnly, setFraudOnly] = useState(false)
  const [ringsOnly, setRingsOnly] = useState(false)
  const load = useCallback(() => { setLoading(true); setError(null); api<Graph>('/api/risk/graph?limit=120').then(setGraph).catch(e => setError(e.message ?? 'Unable to load graph')).finally(() => setLoading(false)) }, [])
  useEffect(() => { load() }, [load])
  useEffect(() => { import('react-force-graph-3d').then(m => setForceGraph(() => m.default)).catch(() => setError('The WebGL graph engine could not be loaded. Use the list fallback.')) }, [])
  const visible = useMemo(() => {
    if (!graph) return { nodes: [] as Node[], links: [] as LinkData[] }
    let nodes = graph.nodes.filter(n => (!query || n.id.toLowerCase().includes(query.toLowerCase())) && (!fraudOnly || n.muleScore >= threshold || n.denied) && (!ringsOnly || graph.rings.length > 0 && (n.muleScore >= .3 || n.degree >= 3)))
    if (threshold > 0) nodes = nodes.filter(n => n.muleScore >= threshold || n.denied)
    nodes = [...nodes].sort((a, b) => b.degree - a.degree).slice(0, 250)
    const ids = new Set(nodes.map(n => n.id))
    const links = graph.edges.filter(e => ids.has(e.from) && ids.has(e.to)).map(e => ({ source: e.from, target: e.to, amount: e.amount, count: e.count, riskScore: e.riskScore }))
    return { nodes: nodes as Node[], links }
  }, [graph, query, threshold, fraudOnly, ringsOnly])
  const reset = () => { setSearch(''); setQuery(''); setThreshold(0); setFraudOnly(false); setRingsOnly(false); setSelected(null) }
  const find = () => { const q = search.trim().toLowerCase(); const n = visible.nodes.find(x => x.id.toLowerCase() === q) ?? visible.nodes.find(x => x.id.toLowerCase().includes(q)); if (n) setSelected(n) }
  return <div className="min-h-screen">
    <PageHeader title="Payment network" subtitle="Directed FraudShield payment events. Simulator traffic is synthetic; roles and rings are labelled heuristics." action={<Badge tone="amber">Synthetic stream</Badge>} />
    <div className="grid grid-cols-1 gap-4 xl:grid-cols-[250px_minmax(0,1fr)_300px]">
      <Controls graph={graph} search={search} setSearch={setSearch} threshold={threshold} setThreshold={setThreshold} fraudOnly={fraudOnly} setFraudOnly={setFraudOnly} ringsOnly={ringsOnly} setRingsOnly={setRingsOnly} onFind={find} onReset={reset} />
      <Card title="3D force-directed network" subtitle="Orbit · pan · zoom · drag nodes · click to inspect" padded={false} className="min-w-0 overflow-hidden">
        <div className="relative h-[650px] bg-[#050814]">
          {loading && <div className="absolute inset-0 z-10 grid place-items-center bg-[#050814]/90 text-sm text-muted">Loading payment graph…</div>}
          {error && <div className="absolute inset-0 z-10 grid place-items-center bg-[#050814]/95 p-6 text-center"><div><div className="mb-3 text-sm text-block">{error}</div><Button variant="secondary" onClick={load}>Retry graph</Button></div></div>}
          {!loading && !error && forceGraph && <ForceGraphView ForceGraph={forceGraph} data={visible} selected={selected} onSelect={setSelected} search={search} />}
          {!loading && !error && !forceGraph && <div className="grid h-full place-items-center text-sm text-muted">Initialising 3D engine…</div>}
        </div>
        <div className="flex flex-wrap gap-3 border-t border-line px-4 py-3 text-[11px] text-muted"><span className="text-approve">● NORMAL</span><span className="text-review">● HUB / BRIDGE</span><span className="text-block">● MULE / denied</span><span>size = degree · width = amount</span></div>
      </Card>
      <Inspector node={selected} graph={graph} visible={visible.nodes} onSelect={setSelected} />
    </div>
  </div>
}

function Controls({ graph, search, setSearch, threshold, setThreshold, fraudOnly, setFraudOnly, ringsOnly, setRingsOnly, onFind, onReset }: any) {
  return <Card title="Graph intelligence" subtitle="Backend-derived filters"><div className="space-y-5 text-xs">
    <div><label className="mb-2 block text-[10px] uppercase tracking-widest text-faint">Search account</label><div className="flex gap-2"><input value={search} onChange={e => setSearch(e.target.value)} onKeyDown={e => e.key === 'Enter' && onFind()} placeholder="VPA / account ID" className="min-w-0 flex-1 rounded-md border border-line-2 bg-surface-2 px-2.5 py-2 text-xs" /><Button size="sm" variant="secondary" onClick={onFind}>Find</Button></div></div>
    <div><div className="mb-2 flex justify-between"><span className="text-muted">Risk threshold</span><b>{Math.round(threshold * 100)}%+</b></div><input className="w-full accent-block" type="range" min="0" max="1" step=".05" value={threshold} onChange={e => setThreshold(+e.target.value)} /></div>
    <label className="flex items-center gap-2"><input type="checkbox" checked={fraudOnly} onChange={e => setFraudOnly(e.target.checked)} /> Mule-risk nodes only</label><label className="flex items-center gap-2"><input type="checkbox" checked={ringsOnly} onChange={e => setRingsOnly(e.target.checked)} /> Ring members only</label>
    <div className="grid grid-cols-3 gap-1 border-y border-line py-3 text-center"><div><b className="block text-signal">{graph?.nodes.length ?? 0}</b><span className="text-faint">nodes</span></div><div><b className="block text-block">{graph?.nodes.filter((n: Node) => n.muleScore >= .3 || n.denied).length ?? 0}</b><span className="text-faint">risk</span></div><div><b className="block text-review">{graph?.rings.length ?? 0}</b><span className="text-faint">rings</span></div></div>
    <div className="space-y-1 text-muted"><div className="mb-2 text-[10px] uppercase tracking-widest text-faint">Roles</div><div><span className="text-block">●</span> MULE — elevated score</div><div><span className="text-review">●</span> HUB / BRIDGE — degree</div><div><span className="text-approve">●</span> NORMAL — no elevated signal</div></div><Button className="w-full" variant="secondary" onClick={onReset}>Reset all filters</Button>
  </div></Card>
}

function ForceGraphView({ ForceGraph, data, selected, onSelect, search }: { ForceGraph: ForceGraph; data: { nodes: Node[]; links: LinkData[] }; selected: Node | null; onSelect: (node: Node | null) => void; search: string }) {
  const ref = useRef<any>(null); const host = useRef<HTMLDivElement>(null); const fitted = useRef(false); const [size, setSize] = useState({ width: 0, height: 0 })
  useEffect(() => { if (!host.current) return; const update = () => { if (host.current) setSize({ width: host.current.clientWidth, height: host.current.clientHeight }) }; update(); const observer = new ResizeObserver(update); observer.observe(host.current); return () => observer.disconnect() }, [])
  useEffect(() => { fitted.current = false }, [data])
  useEffect(() => { if (!search || !ref.current) return; const n = data.nodes.find(x => x.id.toLowerCase().includes(search.toLowerCase())); if (n?.x !== undefined) ref.current.cameraPosition({ x: (n.x ?? 0) * 1.8, y: (n.y ?? 0) * 1.8, z: (n.z ?? 0) * 1.8 }, n, 700) }, [search, data.nodes])
  const neighbors = useMemo(() => { const set = new Set<string>(); if (!selected) return set; set.add(selected.id); data.links.forEach(l => { const s = idOf(l.source), t = idOf(l.target); if (s === selected.id) set.add(t); if (t === selected.id) set.add(s) }); return set }, [selected, data.links])
  return <div ref={host} className="h-full w-full">{size.width > 0 && <ForceGraph ref={ref} width={size.width} height={size.height} graphData={data} backgroundColor="#050814" enableNodeDrag nodeLabel={(n: Node) => `${n.id}<br/>${n.role} · risk ${n.muleScore.toFixed(2)}<br/>degree ${n.degree} · ${compactInr(n.volume)}`} nodeRelSize={4} nodeVal={(n: Node) => 1 + Math.min(8, n.degree)} nodeColor={(n: Node) => colorOf(n.muleScore)} nodeOpacity={(n: Node) => selected && !neighbors.has(n.id) ? .12 : 1} linkDirectionalArrowLength={4} linkDirectionalArrowRelPos={1} linkWidth={(l: LinkData) => Math.max(.5, Math.min(4, l.amount / 5000))} linkColor={(l: LinkData) => l.riskScore >= .7 ? '#ff6363' : '#355d8c'} linkOpacity={(l: LinkData) => selected && idOf(l.source) !== selected.id && idOf(l.target) !== selected.id ? .12 : .65} onNodeClick={(n: Node) => onSelect(n)} onBackgroundClick={() => onSelect(null)} onEngineStop={() => { if (!fitted.current) { fitted.current = true; setTimeout(() => { ref.current?.zoomToFit(700, 50); ref.current?.centerAt(0, 0, 700) }, 80) } }} d3AlphaDecay={.04} d3VelocityDecay={.55} warmupTicks={80} cooldownTicks={120} />}</div>
}

function Inspector({ node, graph, visible, onSelect }: { node: Node | null; graph: Graph | null; visible: Node[]; onSelect: (node: Node | null) => void }) {
  if (!node) return <Card title="Graph summary"><div className="space-y-3 text-xs text-muted"><div>Select a node to inspect its neighborhood, role, risk and volume.</div><div>{graph?.rings.length ? graph.rings.map(r => <Badge key={r} tone="amber">{r}</Badge>) : 'No supported ring annotations in this graph window.'}</div><div>Source: {graph?.provenance.source ?? 'FraudShield payment events'}. Ground truth is not used.</div><div className="border-t border-line pt-3"><div className="mb-2 text-[10px] uppercase tracking-widest text-faint">List fallback</div><div className="max-h-72 overflow-auto">{visible.map(n => <button key={n.id} onClick={() => onSelect(n)} className="flex w-full items-center justify-between border-b border-line py-2 text-left hover:bg-white/[.03]"><span className="max-w-44 truncate font-mono">{n.id}</span><span style={{ color: colorOf(n.muleScore) }}>{n.muleScore.toFixed(2)}</span></button>)}</div></div></div></Card>
  const related = graph?.edges.filter(e => e.from === node.id || e.to === node.id) ?? []
  return <Card title="Node inspector" action={<button className="text-xs text-muted" onClick={() => onSelect(null)}>Close</button>}><div className="space-y-4 text-xs"><div className="break-all font-mono text-fg">{node.id}</div><div className="grid grid-cols-2 gap-2"><span>Type <b>{node.type}</b></span><span>Role <b>{node.role}</b></span><span>Risk <b>{node.muleScore.toFixed(2)}</b></span><span>Degree <b>{node.degree}</b></span><span>Volume <b>{compactInr(node.volume)}</b></span><span>Links <b>{related.length}</b></span></div><div><div className="mb-1 text-muted">Reasons</div>{node.reasons.length ? <ul className="list-disc pl-4 text-fg-2">{node.reasons.map(r => <li key={r}>{r}</li>)}</ul> : <span className="text-muted">No elevated mule signal.</span>}</div><Link className="text-signal" to={`/app/cases?subject=${encodeURIComponent(node.id)}`}>Find related cases →</Link></div></Card>
}
