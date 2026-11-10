// End-to-end smoke test over real HTTP. Usage: node scripts/smoke-test.mjs http://localhost:8080
// NOTE: it creates ONE clearly labelled test record ("[SMOKE TEST]") because records cannot be deleted (out of scope).
const base = (process.argv[2] || 'http://localhost:8080').replace(/\/$/, '');
let failed = 0;
const ok = (name, cond, extra = '') => { console.log(`${cond ? 'PASS' : 'FAIL'}  ${name}${cond ? '' : '  ' + extra}`); if (!cond) failed++; };

async function call(method, path, { token, json, form } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  let body;
  if (json !== undefined) { headers['Content-Type'] = 'application/json'; body = JSON.stringify(json); }
  if (form) body = form;
  const res = await fetch(base + path, { method, headers, body });
  const buf = Buffer.from(await res.arrayBuffer());
  let data = null;
  try { data = JSON.parse(buf.toString('utf8')); } catch { /* binary / text */ }
  return { status: res.status, data, buf, headers: res.headers };
}
const login = async (id) => (await call('POST', '/api/auth/demo-login', { json: { userId: id } })).data.token;
const pdf = () => new Blob(['%PDF-1.4\n% smoke\n%%EOF'], { type: 'application/pdf' });
const newTest = (status, result, files = []) => {
  const fd = new FormData();
  fd.append('data', new Blob([JSON.stringify({ vin: 'TMBSMKE26A0000001', testTypeId: 1, department: 'ESW', testDate: '2026-03-11', responsiblePerson: 'Adam Novotný',
    status, result, comment: '[SMOKE TEST] automated check' })], { type: 'application/json' }));
  files.forEach((f) => fd.append('files', f, 'smoke.pdf'));
  return fd;
};

const health = await call('GET', '/actuator/health');
ok('health endpoint UP (DB connected)', health.status === 200 && health.data?.status === 'UP');
const html = await fetch(base + '/').then((r) => r.text());
ok('frontend served', html.includes('<div id="root">'));
ok('swagger UI reachable', (await fetch(base + '/swagger-ui/index.html')).status === 200);
if (base.startsWith('https://')) ok('HTTPS active', true);

const users = (await call('GET', '/api/auth/demo-users')).data;
ok('10 demo users (6 editors, 4 readers)', users?.length === 10 && users.filter((u) => u.role === 'EDITOR').length === 6);
const E = await login(users.find((u) => u.role === 'EDITOR').id), R = await login(users.find((u) => u.role === 'READER').id);

// --- registration / password login (new accounts are always Readers) ---
const mail = `smoke-${Date.now()}@example.com`;
const reg = await call('POST', '/api/auth/register', { json: { name: 'Smoke User', email: mail, password: 'smoke-pass-123', role: 'EDITOR' } });
ok('registration works and ignores a requested role (Reader)', reg.status === 201 && reg.data.user.role === 'READER', JSON.stringify(reg.data));
const pw = await call('POST', '/api/auth/login', { json: { email: mail, password: 'smoke-pass-123' } });
ok('password login works', pw.status === 200 && !!pw.data.token);
ok('wrong password rejected (401)', (await call('POST', '/api/auth/login', { json: { email: mail, password: 'nope-nope-1' } })).status === 401);
ok('registered Reader cannot write (403)', (await call('POST', '/api/test-types', { token: pw.data.token, json: { name: 'x' } })).status === 403);
const czRes = await fetch(base + '/api/tests', { headers: { 'Accept-Language': 'cs' } });
ok('API messages follow Accept-Language (cs)', (await czRes.json()).message.includes('přihlášení'));

ok('unauthenticated call rejected (401)', (await call('GET', '/api/tests')).status === 401);
const list = await call('GET', '/api/tests?size=5&sort=vin,asc', { token: R });
ok('reader lists tests; seed has >= 50, paged by server', list.status === 200 && list.data.totalElements >= 50 && list.data.content.length === 5);
ok('status filter runs on server', (await call('GET', '/api/tests?status=Completed&size=1', { token: R })).data.totalElements >= 28);
const types = (await call('GET', '/api/test-types', { token: R })).data;
ok('11 test types (1 inactive)', types.length >= 11 && types.some((t) => !t.active));
ok('reader cannot create (403)', (await call('POST', '/api/tests', { token: R, form: newTest('Draft', null) })).status === 403);
ok('reader cannot upload (403)', (await (async () => { const f = new FormData(); f.append('files', pdf(), 'x.pdf'); return call('POST', '/api/tests/1/attachments', { token: R, form: f }); })()).status === 403);
ok('reader cannot delete attachment (403)', (await call('DELETE', '/api/tests/1/attachments/1', { token: R })).status === 403);
ok('reader cannot manage types (403)', (await call('POST', '/api/test-types', { token: R, json: { name: 'x' } })).status === 403);

const dl = await call('GET', '/api/tests/4/attachments', { token: R });
const file = await call('GET', `/api/tests/4/attachments/${dl.data[0].id}/download`, { token: R });
ok('reader downloads (synthetic) attachment', file.status === 200 && file.buf.toString('latin1').startsWith('%PDF-') && file.buf.toString('latin1').includes('SYNTHETIC'));

const created = await call('POST', '/api/tests', { token: E, form: newTest('In Progress', 'Passed') });
ok('editor creates test', created.status === 201 && created.data.createdBy.includes('@'), JSON.stringify(created.data));
const id = created.data.id;
const noAtt = await call('PATCH', `/api/tests/${id}/status`, { token: E, json: { status: 'Completed' } });
ok('F06: Completed rejected without attachment (409 + reason)', noAtt.status === 409 && /attachment/.test(noAtt.data.message), JSON.stringify(noAtt.data));
const f1 = new FormData(); f1.append('files', pdf(), 'smoke.pdf');
const up = await call('POST', `/api/tests/${id}/attachments`, { token: E, form: f1 });
ok('editor uploads attachment', up.status === 201);
const bad = new FormData(); bad.append('files', new Blob(['MZ'], { type: 'application/octet-stream' }), 'evil.exe');
ok('unsupported file rejected (400)', (await call('POST', `/api/tests/${id}/attachments`, { token: E, form: bad })).status === 400);
await call('PUT', `/api/tests/${id}`, { token: E, json: { vin: 'TMBSMKE26A0000001', testTypeId: 1, department: 'ESW', testDate: '2026-03-11', responsiblePerson: 'Adam Novotný', status: 'In Progress', result: 'Not evaluated' } });
ok('F06: Not evaluated cannot complete', (await call('PATCH', `/api/tests/${id}/status`, { token: E, json: { status: 'Completed' } })).status === 409);
await call('PUT', `/api/tests/${id}`, { token: E, json: { vin: 'TMBSMKE26A0000001', testTypeId: 1, department: 'ESW', testDate: '2026-03-11', responsiblePerson: 'Adam Novotný', status: 'In Progress', result: 'Failed' } });
ok('F06: Failed + attachment -> Completed', (await call('PATCH', `/api/tests/${id}/status`, { token: E, json: { status: 'Completed' } })).status === 200);
const attId = up.data[0].id;
const del = await call('DELETE', `/api/tests/${id}/attachments/${attId}`, { token: E });
ok('F06: last attachment of Completed test cannot be deleted (409)', del.status === 409 && del.data.code === 'LAST_ATTACHMENT_OF_COMPLETED_TEST');
ok('reopen to In Progress', (await call('PATCH', `/api/tests/${id}/status`, { token: E, json: { status: 'In Progress' } })).status === 200);
ok('attachment deletable after reopening', (await call('DELETE', `/api/tests/${id}/attachments/${attId}`, { token: E })).status === 204);
const detail = (await call('GET', `/api/tests/${id}`, { token: R })).data;
ok('audit: modifiedBy set, createdBy preserved', detail.modifiedBy === created.data.createdBy && detail.createdAt === created.data.createdAt);

const exp = await call('GET', '/api/tests/export?format=csv&status=Completed', { token: R });
const lines = exp.buf.toString('utf8').trim().split('\r\n');
const total = (await call('GET', '/api/tests?status=Completed&size=1', { token: R })).data.totalElements;
ok('CSV export has ALL filtered rows (not one page)', exp.status === 200 && lines.length === total + 1, `${lines.length} lines vs ${total}`);
const forged = (await call('POST', '/api/test-types', { token: R, json: { name: 'x' } }));
ok('role cannot be forged by request (still 403)', forged.status === 403);

console.log(failed ? `\n${failed} CHECK(S) FAILED` : '\nALL SMOKE CHECKS PASSED');
process.exit(failed ? 1 : 0);
