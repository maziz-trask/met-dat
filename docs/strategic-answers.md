# MET-DAT Benchmark 2026 – Strategic Areas 3.1 – 3.4

*Scope: answers to sections 3.1–3.4 of the specification. Every "example" is **illustrative** and is drawn from the demo module delivered with this benchmark or from generally known practice; no claim of previous customer or company delivery is made. Assumptions are listed per section.*

---

## 3.1 AI acceleration of development

### Proposed approach
AI is a pair-programmer, never an authority. Work is split by risk:

| Activity | AI role | Human control |
|---|---|---|
| Analysis of specs and data | summarise documents, extract requirement checklists, profile datasets, find contradictions | developer verifies against the source; contradictions go to the product owner |
| Scaffolding & boilerplate (DTOs, mappers, CRUD, forms, config, Dockerfiles) | generate first draft | review as for any pull request |
| Business logic (e.g. completion rule, authorisation) | propose design and edge cases; never merged unreviewed | author writes or fully understands each line; rule is encoded in one class and tested |
| Test creation | generate test matrices from the requirements, negative cases, test data | tests are run and mutated mentally ("would this fail if the rule broke?"); coverage of acceptance criteria tracked |
| Documentation, ADRs, estimates | draft | owner edits, numbers are re-derived |
| Review & refactoring | second-opinion review, security checklist, dependency hints | humans decide; findings are triaged, not auto-applied |
| Debugging | explain stack traces, suggest hypotheses | reproduce before fixing |

### Quality verification of AI output
1. **Executable verification first**: compiler, linters, unit/integration tests against a real PostgreSQL, a black-box smoke test over HTTP. Output that is not proven by a test is treated as unverified.
2. **Specification traceability**: a requirements checklist (F01–F12, 22 acceptance scenarios) is mapped to tests; a feature is "done" when its scenario passes.
3. **Review gates**: mandatory human code review, static analysis (SAST), dependency and licence scan, secret scan in CI. AI-authored code gets *at least* the same rigour, with extra attention to plausible-but-wrong code: invented APIs, outdated library usage, silent error swallowing, over-broad catch blocks, missing authorisation checks.
4. **Adversarial prompts**: ask the tool to attack its own result (missing validations, race conditions, injection).
5. **Data honesty**: AI must never "fix" inconsistent source data silently. *Illustration from this benchmark:* the workbook README claims all 50 tests satisfy the completion rule, but 13 Completed records do not. The correct behaviour was to preserve the data, report the exception, and still enforce the rule – a discrepancy found by profiling the data, not by trusting the README.
6. **Real defects found by verification** (illustrative, from this demo's own build): a black-box test revealed that a Reader sending an invalid body got HTTP 400 instead of 403 because bean validation ran before method security; fixed by a URL-level role rule plus a regression test. Timestamp precision differed between create response and database reads; fixed and re-tested. Neither would be caught by reading the code alone.

### Protection of source code, personal data and internal information
* **Tool tiering**: only approved tools/tenants with contractual no-training, retention and data-residency terms; personal or free accounts forbidden for company code.
* **Data classification** decides what may enter a prompt: public/synthetic – allowed; internal – only approved enterprise tools; confidential, secrets, personal data, unreleased vehicle test results – never, or only through an approved on-premises/private deployment.
* **Technical controls**: secret scanners and pre-commit hooks, `.gitignore`/ignore-files for AI tools, no credentials in the workspace, least-privilege tokens, network egress policy and audit logs for AI traffic, synthetic or masked datasets for development (as used in this benchmark).
* **Local-agent permissions**: AI agents run with sandboxing and explicit approval for shell, network and file-system writes outside the project.
* **Supply chain**: AI-suggested dependencies are verified (existence, maintainer, licence, vulnerability data) to avoid hallucinated or typosquatted packages.

### Developer accountability
The committer owns the change: "the AI wrote it" is not a defence. Rules: the author can explain every line in review; AI use is noted in the pull request when substantial; licence/IP of generated code follows company policy; security-relevant code (authentication, authorisation, cryptography, data access) requires a second reviewer; incidents are blameless but traceable to the human review that approved the change.

### Measuring real benefit
Compare against a baseline rather than impressions: lead time per change and cycle time, PR size and review time, **change-failure rate and escaped defects**, rework (code churn within 2–4 weeks), test coverage of acceptance criteria, build stability, developer survey (flow, trust), and time spent on documentation/tests. Run a time-boxed pilot (e.g. two comparable teams or alternate sprints), use *task-level* measures for repeatable tasks (e.g. "new CRUD endpoint with tests"), and track the cost side (licences, review overhead). Speed without a flat or falling defect rate is not a benefit.

### Main risks and mitigation
| Risk | Mitigation |
|---|---|
| Plausible but wrong code / hallucinated APIs | tests-first verification, review, small diffs |
| Leakage of code or personal data | tooling tiers, classification, DLP, synthetic data |
| Over-reliance, skill erosion, rubber-stamp reviews | review quality metrics, rotation, AI-free exercises for juniors |
| Licence/IP contamination | approved tools with indemnity, licence scanning |
| Prompt injection via repository content, web pages or issues read by agents | treat all tool-read content as data, restricted agent permissions, human approval for side effects |
| Misleading productivity claims | baseline metrics, defect-rate guardrails |

### Assumptions and limitations
Approved enterprise AI tooling exists; policies on classification are in place; the benefits observed on a small demo do not extrapolate automatically to a large legacy estate; measurement requires management patience.

---

## 3.2 Reporting and data trustworthiness

### Approach and architecture choice
For an application like MET-DAT the operational database must not be the reporting engine for heavy analytics. Proposal, in increasing maturity:

1. **Operational reports (now)**: read-only reporting views / materialised views in PostgreSQL on a **read replica**, exposed via the API (e.g. the filtered CSV export already implemented) and a BI tool connected with a read-only role. Suitable while volumes are small and freshness must be near real-time.
2. **Analytical layer (when cross-application reporting is needed)**: replicate or stream changes (CDC or scheduled ELT) into a **governed analytical store** (warehouse/lakehouse) with a semantic layer: conformed dimensions (vehicle, test type, department, calendar week), certified metrics, and a BI tool on top.

Selection criteria: data volume and growth; freshness requirement (minutes vs next day); number of source systems; query complexity; who builds reports (IT vs business self-service); security model (row/column-level); operating skills and existing corporate standards (preferring the existing BI platform over a new one); total cost; vendor lock-in; auditability and lineage support.

*Illustration:* test overview metrics such as "tests per status per calendar week" or "pass/fail rate per test type" are simple aggregations over `technical_test`; they can start as SQL views and later move to the warehouse unchanged because the definitions live in the semantic layer, not in dashboards.

### Ensuring correct, consistent, trusted data
* **Quality at source**: constraints and validation in the application and database (mandatory fields, CHECK constraints for status/result, FK for test types, F06 completion rule). Reports are only as good as the write path.
* **Single definition of metrics** (semantic layer / data dictionary) with owners: for example "completed test" = status Completed *and* result Passed/Failed; legacy exceptions are flagged, not hidden.
* **Automated data-quality tests** in the pipeline (row counts, uniqueness, referential integrity, accepted values, freshness, reconciliation of report totals against the source system) with alerts and quarantine of failing loads.
* **Lineage and documentation**: catalogue, versioned transformations, reproducible reports.
* **Trust signals for users**: show "data as of" timestamps, certification badges for approved datasets, visible data-quality exceptions (such as the 13 inconsistent Completed records in the demo data), and a defined channel for reporting discrepancies.
* **Change control**: schema changes go through migration scripts (Flyway) and impact analysis for downstream reports; contracts/versioned views for consumers.
* **Reconciliation**: periodic comparison between operational counts and report counts; differences are investigated, not smoothed.

### Self-service versus security and access control
Principle: *democratise curated data, protect raw data.*
* Tiered access: certified marts/semantic models for self-service; raw and sensitive tables restricted to data engineers.
* Central identity (SSO groups) mapped to roles; **row-level security** (e.g. a department sees its own tests) and **column masking** for personal data (responsible person, e-mail) in the BI layer or database.
* Sandbox workspaces for exploratory work on approved extracts; promotion path to "certified" requires review.
* Audit of who queried what; export controls and watermarking for confidential vehicle data; data-owner approval for new data sets; periodic access recertification.
* Pseudonymised or synthetic data for development and training.

### Risks and mitigation
| Risk | Mitigation |
|---|---|
| Different numbers in different reports | semantic layer, certified metrics, owner per KPI |
| Reporting load slowing the transactional system | replica or warehouse, resource limits |
| Stale data presented as current | freshness checks, "as of" labels |
| Leakage through self-service exports | RLS/masking, export rights, audit |
| Shadow IT spreadsheets | good certified datasets and training; easy access |
| Over-engineering | start with views/replica, move to warehouse when needs prove it |

### Assumptions and limitations
Volumes and the number of consuming applications are unknown; the corporate BI standard and SSO exist; no legal analysis of personal data was done; MET-DAT in this benchmark has no reporting layer implemented – only the CSV export and a status dashboard.

---

## 3.3 Modernisation of the application portfolio

### Initial analysis and categorisation
1. **Inventory** (discovery): automated scanning of repositories, build files, container registries and runtime/network data; interviews with owners; a CMDB/portfolio sheet with: owner, business criticality, users, technology (language, framework, versions, DB), age, hosting/operations model, integrations, data classification, release frequency, test coverage, known vulnerabilities, support/skills availability, cost.
2. **Categorise** along two axes – *business value/criticality* and *technical health* (supportability, security posture, change cost, operational burden) – plus technology family and hosting model.
3. **Strategy per application** (the "6R" family): **Retire** (low value, duplicate), **Retain** (healthy, leave), **Rehost** (lift-and-shift to the standard platform), **Replatform** (containerise, managed DB, CI/CD, no code change), **Refactor/Rearchitect** (code changes, strangler pattern), **Replace** (commercial/standard product).
4. **Prioritisation criteria**: security and compliance exposure (unsupported runtime, critical CVEs), business criticality, change demand, operational cost and incident rate, skills risk (end-of-life tech, bus factor), dependencies (platform prerequisites first), benefit/effort ratio, and fit with the target architecture. Score transparently (weighted matrix) and review quarterly. Quick wins (rehost/replatform of high-risk apps, retirements) go first to fund the harder refactoring.

### Target reference architecture and incremental introduction
Target state (a "paved road", not a mandate for one framework): containerised workloads on a standard platform (Kubernetes or managed container service); **modular monolith as default** with extraction of services only where scaling/ownership demands it (the MET-DAT demo follows this style); REST/OpenAPI contracts, event messaging for integration where needed; PostgreSQL as default relational store with migrations (Flyway/Liquibase); central identity (OIDC/SSO), secrets management; CI/CD templates; observability stack (logs, metrics, traces via OpenTelemetry); infrastructure as code; golden-path templates and reference implementations.

Incremental adoption **without stopping operations**: strangler-fig pattern behind an API gateway/reverse proxy – new functionality goes to the new stack while legacy continues; anti-corruption layers for data/protocol translation; database migration by synchronisation (replicate, dual-run, cut-over) rather than big bang; feature flags and canary releases; rollback plans; characterisation tests around legacy behaviour before touching it; each increment is deliverable and measurable. Architecture decision records and an architecture board keep convergence honest, with a documented exception process.

### Technology unification
Unify **interfaces and operations first, internals second**:
* **Runtime/deployment**: everything as OCI containers with a standard base image, health checks, config via environment, same deployment pipeline.
* **CI/CD**: shared pipeline templates (build, test, SAST/SCA, container scan, sign, deploy, promote) – applications of different stacks plug in their build step.
* **Data layer**: converge on few database technologies; versioned migrations; backup/restore standards; access via per-application accounts.
* **Languages/frameworks**: limit to a short, supported list (e.g. two backend stacks, one frontend stack); legacy stacks stay only while retired or isolated, with an end-date.
* **Observability**: common log format, metrics, tracing, dashboards and alert rules regardless of language (OpenTelemetry agents/sidecars for legacy).
* **Identity and security**: common SSO/OIDC, shared libraries or gateway policies.
* **Governance**: tech radar (adopt/trial/hold/retire), reference apps, inner-source libraries, communities of practice.

### Target operating model and sustainability
* Clear ownership: product teams "you build it, you run it" on a platform team's paved road; SLAs/SLOs, on-call, incident and problem management, change management proportionate to risk.
* Run standards: runbooks, monitoring/alerts, backup/DR tests, patch cadence, dependency update automation, cost transparency (FinOps).
* **Handover to internal or third-party teams**: every application ships with README/architecture docs (as in this demo: architecture, database design, ADR-style decisions, demo scenarios, deployment guide), automated build/test, infrastructure as code, seed/test data, a one-command local start, and a documented backlog of known limitations. Handover includes paired sessions, a trial period with the receiving team operating while the delivery team supports, acceptance tests, access/credential transfer through the secret store, and a checklist signed by both sides. Bus-factor reviews and living documentation ("docs as code") keep it maintainable.

### Risks and mitigation
| Risk | Mitigation |
|---|---|
| Big-bang rewrite fails | incremental strangler, measurable milestones |
| Hidden dependencies and undocumented behaviour | discovery, traffic analysis, characterisation tests |
| Business disruption | canary/dual-run, rollback, change windows |
| Platform becomes a bottleneck or forced standard that doesn't fit | paved road with exception process, platform team treated as product |
| Skills gaps | training, pairing, limited technology list |
| Scope/budget creep | quarterly re-prioritisation, benefit tracking, retire early |
| Modernised app not owned afterwards | handover criteria and named owners from day one |

### Assumptions and limitations
Size and composition of the portfolio, regulatory constraints, existing platform and budget are unknown; the target architecture is a proposal to be tuned with the corporate architecture board; the migration effort of any specific application can only be estimated after analysis.

---

## 3.4 Security of the application portfolio

### Approach
Risk-based, consistent baselines applied through the platform rather than app by app; "secure by default" templates; measured by coverage and time-to-fix.

**Identity and access management**: central SSO with OIDC/SAML and MFA; apps authenticate users through the identity provider and consume short-lived signed tokens; role/attribute-based authorisation enforced **on the server** (the demo enforces Reader/Editor in Spring Security and never trusts a role from the client – a simplified demo identity replaces SSO only for the benchmark); service-to-service identity with workload identities/mTLS; least privilege, separation of duties, joiner-mover-leaver automation, periodic access recertification, privileged access management, secrets in a vault with rotation, no shared accounts.

**Vulnerability management**: asset inventory as the base; SAST, SCA (dependencies), container and IaC scanning in CI; DAST and periodic penetration tests for exposed apps; risk-based prioritisation (CVSS + exploitability, e.g. known-exploited lists + asset criticality); remediation SLAs by severity (e.g. critical in days, high in weeks) with tracked exceptions; automated dependency update PRs; patch cadence for base images and platforms; metrics: mean time to remediate, open vulnerabilities by age, coverage.

**Software supply-chain security**: pinned dependency versions and lock files; trusted internal artifact proxy/registry; SBOM generation (CycloneDX/SPDX) for each build; signed commits and signed artifacts/images with provenance (e.g. SLSA-style attestations); protected branches, mandatory review, hardened CI runners with minimal privileges and ephemeral credentials; verified base images; checks against typosquatting and abandoned packages; licence compliance. For AI suggestions, packages are verified before use.

**API security**: inventory and OpenAPI contracts; authentication at gateway and service; authorisation checks per operation and object (prevent broken object-level authorisation – the "can Reader modify?" question is tested at API level, as in the demo's acceptance tests); input validation on the server (never UI only), size limits, safe file upload handling (type, size, content signature, safe names, path traversal protection), rate limiting and throttling, consistent error format without internals, CORS policy minimal, TLS everywhere, security headers, schema validation; OWASP API Security Top 10 as test checklist; WAF/gateway for exposed endpoints; API versioning and deprecation.

**Auditability**: who did what when – server-side audit fields (the demo's Created/Modified By/At are server-generated and cannot be edited), immutable security-relevant event logs (login, permission changes, data export, deletion) in a central SIEM with retention and tamper protection, correlation IDs, time synchronisation; regular access and log reviews; evidence for compliance (change records from CI/CD).

**Secure use of AI in development**: approved tools only, data-classification rules for prompts, no secrets or personal data in prompts, agents sandboxed with least-privilege tokens and human approval of side-effecting actions, protection against prompt injection from external content, human review and automated security scanning of AI-generated code, logging of AI tool usage, vendor assessment (retention, training, residency), training of developers.

### Illustrative example (from this demo)
Defence in depth around one rule: permissions are checked at the URL level and on the method; the signed token cannot be altered (a tampered role claim is rejected – covered by an automated test); direct API calls without a valid token are rejected; uploads are validated by extension, MIME type, content signature and size; CSV export neutralises spreadsheet-formula injection; errors never expose stack traces. This shows the controls the portfolio baseline would require everywhere.

### Rollout
1. Baseline assessment and risk ranking (internet-facing, personal data, high business criticality first).
2. Platform controls first (SSO, gateway, secrets, CI scanning templates) – benefits every app.
3. Per-app remediation by priority; legacy apps that cannot be fixed are isolated (network segmentation, WAF, reverse proxy with SSO) or retired.
4. Continuous: dashboards, quarterly reviews, tabletop incident exercises, red-team/pentest cycles.

### Main risks and mitigation
| Risk | Mitigation |
|---|---|
| Legacy apps without patches or SSO capability | front with SSO-enabled proxy, segmentation, retire |
| Alert fatigue and unfixable backlog | risk-based priority, SLAs, automation, ownership |
| Security as a blocker for teams | paved-road templates, fast feedback in CI, security champions |
| Supply-chain compromise | pinning, signing, provenance, restricted CI, SBOM monitoring |
| Overly permissive roles over time | recertification, least privilege, automated reviews |
| Incomplete inventory | continuous discovery, deny-by-default network policies |

### Assumptions and limitations
A corporate identity provider and central logging exist; the threat model and regulatory context (e.g. customer-data, export-control, automotive-industry standards) have not been analysed in detail; controls and SLAs need to be calibrated with the CISO organisation; nothing in this section claims that these measures have already been implemented for any real portfolio.
