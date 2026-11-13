# StreamShield reproducible data plan

This repository's demo evaluation is synthetic and is not real-world performance.

`txn-service` generates deterministic scenario families (`LEGIT`, mule ring, account takeover,
digital arrest, probing, adaptive fraud and cold-start scam). The evaluation endpoint accepts a
seed and creates labels beside each observation. `simLabel` is privileged ground truth for
scoreboards only; it is never a model feature and is not used for online training. Training uses
analyst-confirmed labels from the feedback topic.

Evaluation is ordered by time: baseline traffic, training/validation period, then adaptive-wave
test traffic. Features are derived only from signals available at the payment timestamp. Threshold
selection must use validation data and be frozen for the test period. Re-running with the same
seed produces the same cohorts and metrics.

An external public dataset requires a separate adapter recording license, URL, checksum, schema
mapping and anonymisation. IBM HI-Small is not present here and no result claims to use it.

Demo VPAs, devices and locations are generated identifiers. Production exports must tokenize or
key identifiers, remove direct personal data, coarsen locations, restrict access, and retain only
minimum fields. Synthetic patterns, prevalence, labels and outcomes do not estimate real-world fraud.
