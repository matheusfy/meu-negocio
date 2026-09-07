'use strict';

/* ============ Ateliê de Decantes — front-end estático ============ */
/* Fala com a API REST em /api/v1. Servido pelo próprio Spring Boot. */

/* ---------- ícones ---------- */
const IC = {
  inicio: '<path d="M3 10.5 12 3l9 7.5"/><path d="M5 9.5V21h14V9.5"/>',
  perfumes: '<path d="M10 2h4v3h-4z"/><path d="M9 5h6l1 4v11a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V9z"/><path d="M8 13h8"/>',
  insumos: '<path d="M12 3s6 7 6 11a6 6 0 0 1-12 0c0-4 6-11 6-11z"/>',
  viabilidade: '<path d="M12 3v18"/><path d="M6 7h12"/><path d="m6 7-3 6h6z"/><path d="m18 7-3 6h6z"/><path d="M8 21h8"/>',
  chev: '<path d="m9 6 6 6-6 6"/>',
  arrow: '<path d="M19 12H5"/><path d="m12 19-7-7 7-7"/>',
};
const svg = (n) => `<svg viewBox="0 0 24 24" fill="none" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${IC[n]}</svg>`;

/* ---------- utilidades ---------- */
const $ = (sel, root = document) => root.querySelector(sel);
const screenEl = () => document.getElementById('screen');
const mount = (html) => { screenEl().innerHTML = html; };

const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => (
  { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
));
const brl = (n) => (Number(n) || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
const numf = (n, d = 0) => (Number(n) || 0).toLocaleString('pt-BR', { minimumFractionDigits: d, maximumFractionDigits: d });
const pctf = (n) => `${Math.round(Number(n) || 0)}%`;
const mlf = (n) => `${numf(n)} ml`;
const parseNum = (v) => {
  const n = parseFloat(String(v ?? '').replace(',', '.'));
  return Number.isNaN(n) ? null : n;
};
const custoMl = (p) => (p.precoCusto > 0 && p.volumeMl > 0 ? p.precoCusto / p.volumeMl : null);

let toastTimer;
function toast(msg, isError = false) {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.classList.toggle('err', isError);
  t.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.classList.remove('show'), isError ? 4200 : 2600);
}

/* ---------- API ---------- */
async function api(path, opts = {}) {
  const res = await fetch('/api/v1' + path, {
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    ...opts,
  });
  if (res.status === 204) return null;
  let body = null;
  try { body = await res.json(); } catch (_) { /* corpo vazio ou não-JSON */ }
  if (!res.ok) {
    const base = (body && (body.mensagem || body.erro)) || `Erro ${res.status}`;
    const det = body && Array.isArray(body.detalhes) && body.detalhes.length
      ? ' — ' + body.detalhes.join('; ')
      : '';
    throw new Error(base + det);
  }
  return body;
}
const listar = (path) => api(path).then((page) => (page && page.content) || []);

/* ---------- roteamento ---------- */
const state = { route: 'inicio', perfumeId: null };
const NAV_FOR = {
  inicio: 'inicio', perfumes: 'perfumes', perfume: 'perfumes',
  insumos: 'insumos', viabilidade: 'viabilidade',
};

async function go(route, params = {}) {
  if (params.perfumeId != null) state.perfumeId = params.perfumeId;
  state.route = route;
  document.querySelectorAll('#nav button').forEach((b) => {
    b.setAttribute('aria-current', String(b.dataset.go === NAV_FOR[route]));
  });
  window.scrollTo({ top: 0 });
  mount('<div class="loading">Carregando…</div>');
  try {
    await RENDERERS[route]();
  } catch (e) {
    mount(`<div class="page-head"><h1>Ops</h1></div>
      <div class="empty">Não consegui carregar esta tela.<br><br>${esc(e.message || e)}</div>`);
  }
}
const reload = () => go(state.route);

/* ================= TELAS ================= */

async function renderInicio() {
  const [perfumes, decantes] = await Promise.all([
    listar('/produtos?size=500&sort=nome,asc'),
    listar('/decantes?size=999'),
  ]);
  const decAtivos = decantes.filter((d) => d.ativo);
  const investido = perfumes.reduce((s, p) => s + (Number(p.precoCusto) || 0), 0);

  const comDecantes = perfumes.filter((p) => decAtivos.some((d) => d.produtoId === p.id));
  const analises = (await Promise.all(
    comDecantes.map((p) => api(`/produtos/${p.id}/analise`).then((a) => ({ p, a })).catch(() => null)),
  )).filter(Boolean);

  let melhor = null;
  for (const { p, a } of analises) {
    for (const an of a.comparacao.analises) {
      if (!melhor || Number(an.margemLote) > Number(melhor.an.margemLote)) melhor = { p, an };
    }
  }

  mount(`
    <div class="page-head">
      <p class="eyebrow">Ateliê de Decantes</p>
      <h1>Bom dia 🌿</h1>
      <p>Cadastre seus perfumes, informe os custos e descubra se compensa transformá-los em decantes — e em qual tamanho.</p>
    </div>

    <div class="tiles">
      <div class="tile"><div class="k">Perfumes</div><div class="v">${perfumes.length}</div><div class="s">cadastrados</div></div>
      <div class="tile"><div class="k">Decantes</div><div class="v">${decAtivos.length}</div><div class="s">configurados</div></div>
      <div class="tile"><div class="k">Investido em perfumes</div><div class="v">${brl(investido)}</div><div class="s">soma do preço de custo</div></div>
    </div>

    ${melhor ? `
    <div class="opp">
      <div>
        <p class="eyebrow">Melhor oportunidade agora</p>
        <h2>${esc(melhor.p.nome)} · decante de ${mlf(melhor.an.volumeMl)}</h2>
        <p>Investindo ${brl(melhor.an.investimentoLote)} no frasco você fatura ${brl(melhor.an.receitaTotal)}
        e lucra <b>${brl(melhor.an.lucroTotal)}</b> — margem de ${pctf(melhor.an.margemLote)}.</p>
      </div>
      <button class="btn" data-open-viab="${melhor.p.id}">Ver comparação</button>
    </div>` : ''}

    <div class="card">
      <div class="card-h">Seus perfumes <span class="h-note">toque para abrir</span></div>
      ${perfumes.length ? `<div class="rows">${perfumes.map((p) => {
        const ds = decAtivos.filter((d) => d.produtoId === p.id).length;
        const cm = custoMl(p);
        return `<button class="row" data-open-perfume="${p.id}">
          <div class="name">${esc(p.nome)}<small>${esc(p.marca || '—')}</small></div>
          <div class="cell"><small>Custo / ml</small><b>${cm ? brl(cm) : '—'}</b></div>
          <div class="cell"><small>Volume</small><b>${p.volumeMl ? mlf(p.volumeMl) : '—'}</b></div>
          <div class="cell"><small>Decantes</small><b>${ds || '—'}</b></div>
          <div class="chev">${svg('chev')}</div>
        </button>`;
      }).join('')}</div>`
        : '<div class="card-b"><div class="empty">Nenhum perfume ainda. Vá em <b>Perfumes</b> para cadastrar o primeiro.</div></div>'}
    </div>
  `);

  screenEl().querySelectorAll('[data-open-perfume]').forEach((b) =>
    b.addEventListener('click', () => go('perfume', { perfumeId: Number(b.dataset.openPerfume) })));
  screenEl().querySelectorAll('[data-open-viab]').forEach((b) =>
    b.addEventListener('click', () => go('viabilidade', { perfumeId: Number(b.dataset.openViab) })));
}

async function renderPerfumes() {
  const [perfumes, decantes] = await Promise.all([
    listar('/produtos?size=500&sort=nome,asc'),
    listar('/decantes?size=999'),
  ]);

  mount(`
    <div class="page-head">
      <h1>Perfumes</h1>
      <p>O <b>preço de custo</b> e o <b>volume</b> de cada frasco são o que o cálculo de viabilidade precisa.</p>
    </div>
    <div class="grid2">
      <div class="pcards">
        ${perfumes.length ? perfumes.map((p) => {
          const cm = custoMl(p);
          const ds = decantes.filter((d) => d.produtoId === p.id && d.ativo).length;
          return `<button class="pcard" data-open-perfume="${p.id}">
            <div>
              <h3>${esc(p.nome)}</h3>
              <div class="brand-line">${esc(p.marca || '—')}</div>
            </div>
            <div class="chips">
              <span class="chip">${brl(p.precoCusto)}</span>
              <span class="chip">${p.volumeMl ? mlf(p.volumeMl) : '— ml'}</span>
              ${cm ? `<span class="chip g">${brl(cm)}/ml</span>` : ''}
            </div>
            <div class="muted" style="font-size:.82rem">${ds} decante(s) configurado(s)</div>
          </button>`;
        }).join('') : '<div class="empty">Nenhum perfume cadastrado.</div>'}
      </div>

      <div class="card">
        <div class="card-h">Adicionar perfume</div>
        <div class="card-b">
          <form id="f-perfume">
            <div class="field">
              <label for="p-nome">Nome</label>
              <input id="p-nome" required placeholder="Ex.: Good Girl">
            </div>
            <div class="field">
              <label for="p-marca">Marca</label>
              <input id="p-marca" placeholder="Carolina Herrera">
            </div>
            <div class="field row2">
              <div class="field">
                <label for="p-custo">Preço de custo (R$)</label>
                <input id="p-custo" type="number" min="0" step="0.01" required placeholder="480,00">
              </div>
              <div class="field">
                <label for="p-vol">Volume (ml)</label>
                <input id="p-vol" type="number" min="1" step="1" required placeholder="80">
              </div>
            </div>
            <div class="field">
              <label for="p-venda">Preço do frasco cheio (R$)</label>
              <input id="p-venda" type="number" min="0" step="0.01" placeholder="opcional">
              <span class="hint">Só usado se um dia você quiser vender o perfume inteiro.</span>
            </div>
            <button class="btn" type="submit">Salvar perfume</button>
          </form>
        </div>
      </div>
    </div>
  `);

  screenEl().querySelectorAll('[data-open-perfume]').forEach((b) =>
    b.addEventListener('click', () => go('perfume', { perfumeId: Number(b.dataset.openPerfume) })));

  $('#f-perfume').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const nome = $('#p-nome').value.trim();
    const precoCusto = parseNum($('#p-custo').value);
    const volumeMl = parseNum($('#p-vol').value);
    if (!nome) return toast('Dê um nome ao perfume.', true);
    if (!precoCusto || precoCusto <= 0) return toast('Informe o preço de custo.', true);
    if (!volumeMl || volumeMl <= 0) return toast('Informe o volume em ml.', true);
    const btn = ev.submitter;
    btn.disabled = true;
    try {
      await api('/produtos', {
        method: 'POST',
        body: JSON.stringify({
          nome, marca: $('#p-marca').value.trim() || null, categoria: 'Perfume',
          sku: null, fornecedorPrincipalId: null,
          precoVenda: parseNum($('#p-venda').value) || 0,
          precoCusto, volumeMl, estoqueAtual: 0, estoqueMinimo: 0, ativo: true,
        }),
      });
      toast('Perfume cadastrado.');
      reload();
    } catch (e) {
      toast(e.message, true);
      btn.disabled = false;
    }
  });
}

async function renderPerfume() {
  const id = state.perfumeId;
  const [p, decantes, insumos] = await Promise.all([
    api(`/produtos/${id}`),
    listar(`/decantes?produtoId=${id}&size=999`),
    listar('/insumos?size=500&sort=nome,asc'),
  ]);
  const insumosAtivos = insumos.filter((i) => i.ativo);
  const cm = custoMl(p);

  const optsFor = (palavra) => {
    const pref = insumosAtivos.filter((i) => (i.nome || '').toLowerCase().includes(palavra));
    return (pref.length ? pref : insumosAtivos);
  };
  const optionEls = (arr, incluirNenhum) =>
    (incluirNenhum ? '<option value="">nenhuma</option>' : '') +
    arr.map((i) => `<option value="${i.id}">${esc(i.nome)} — ${brl(i.custoUnitario)}</option>`).join('');

  mount(`
    <button class="back" data-back>${svg('arrow')} Perfumes</button>

    <div class="ficha">
      <div class="title">${esc(p.nome)}<small>${esc(p.marca || '—')}</small></div>
      <div class="m"><span>Preço de custo</span><b>${brl(p.precoCusto)}</b></div>
      <div class="m"><span>Volume</span><b>${p.volumeMl ? mlf(p.volumeMl) : '—'}</b></div>
      <div class="m"><span>Custo por ml</span><b>${cm ? brl(cm) : '—'}</b></div>
    </div>

    <div class="form-actions" style="margin-bottom:1.6rem">
      <button class="btn ghost sm" id="b-editar-perfume">Editar perfume</button>
      <button class="btn danger sm" id="b-excluir-perfume">Excluir</button>
      ${decantes.some((d) => d.ativo) ? '<button class="btn sm" data-open-viab>Comparar tamanhos</button>' : ''}
    </div>

    <div class="card" id="box-editar-perfume" hidden>
      <div class="card-h">Editar perfume</div>
      <div class="card-b">
        <form id="f-editar-perfume">
          <div class="field"><label for="e-nome">Nome</label><input id="e-nome" value="${esc(p.nome)}" required></div>
          <div class="field"><label for="e-marca">Marca</label><input id="e-marca" value="${esc(p.marca || '')}"></div>
          <div class="field row2">
            <div class="field"><label for="e-custo">Preço de custo (R$)</label>
              <input id="e-custo" type="number" min="0" step="0.01" value="${p.precoCusto ?? ''}" required></div>
            <div class="field"><label for="e-vol">Volume (ml)</label>
              <input id="e-vol" type="number" min="1" step="1" value="${p.volumeMl ?? ''}" required></div>
          </div>
          <div class="field"><label for="e-venda">Preço do frasco cheio (R$)</label>
            <input id="e-venda" type="number" min="0" step="0.01" value="${p.precoVenda ?? ''}"></div>
          <div class="form-actions">
            <button class="btn" type="submit">Salvar</button>
            <button class="btn ghost" type="button" id="b-cancelar-editar">Cancelar</button>
          </div>
        </form>
      </div>
    </div>

    <div class="card" style="margin:1.4rem 0">
      <div class="card-h">Decantes deste perfume</div>
      <div class="card-b">
        ${decantes.length ? `<div class="tbl-wrap"><table class="tbl">
          <thead><tr><th>Tamanho</th><th class="num">Preço</th><th class="num">Embalagem</th><th></th></tr></thead>
          <tbody>${decantes.map((d) => `<tr>
            <td><b>${mlf(d.volumeMl)}</b></td>
            <td class="num">${brl(d.precoVenda)}</td>
            <td class="num">${brl((Number(d.custoEmbalagem) || 0) + (Number(d.custoSeringa) || 0) + (Number(d.custoEtiqueta) || 0))}</td>
            <td class="row-actions"><button class="btn danger sm" data-del-decante="${d.id}">Excluir</button></td>
          </tr>`).join('')}</tbody>
        </table></div>` : '<div class="empty">Nenhum decante configurado ainda.</div>'}
      </div>
    </div>

    <div class="card">
      <div class="card-h">Novo decante</div>
      <div class="card-b">
        ${insumosAtivos.length ? `<form id="f-decante">
          <div class="field row2">
            <div class="field"><label for="d-vol">Tamanho (ml)</label>
              <select id="d-vol"><option>3</option><option selected>5</option><option>10</option><option>15</option></select>
            </div>
            <div class="field"><label for="d-venda">Preço de venda (R$)</label>
              <input id="d-venda" type="number" min="0" step="0.01" placeholder="52,00"></div>
          </div>
          <div class="field"><label for="d-frasco">Frasco</label><select id="d-frasco">${optionEls(optsFor('frasco'), false)}</select></div>
          <div class="field"><label for="d-seringa">Seringa</label><select id="d-seringa">${optionEls(optsFor('seringa'), true)}</select></div>
          <div class="field"><label for="d-etiqueta">Etiqueta</label><select id="d-etiqueta">${optionEls(optsFor('etiqueta'), true)}</select></div>
          <p class="hint" id="d-embalagem">Custo de embalagem: —</p>
          <button class="btn" type="submit">Salvar decante</button>
        </form>` : `<div class="empty">Cadastre frasco, seringa e etiqueta em <b>Insumos</b> antes de criar um decante.</div>`}
      </div>
    </div>
  `);

  const root = screenEl();
  root.querySelector('[data-back]').addEventListener('click', () => go('perfumes'));
  const viabBtn = root.querySelector('[data-open-viab]');
  if (viabBtn) viabBtn.addEventListener('click', () => go('viabilidade', { perfumeId: id }));

  /* editar perfume */
  const box = $('#box-editar-perfume');
  $('#b-editar-perfume').addEventListener('click', () => { box.hidden = !box.hidden; });
  $('#b-cancelar-editar').addEventListener('click', () => { box.hidden = true; });
  $('#f-editar-perfume').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const nome = $('#e-nome').value.trim();
    const precoCusto = parseNum($('#e-custo').value);
    const volumeMl = parseNum($('#e-vol').value);
    if (!nome || !precoCusto || precoCusto <= 0 || !volumeMl || volumeMl <= 0) {
      return toast('Nome, preço de custo e volume são obrigatórios.', true);
    }
    ev.submitter.disabled = true;
    try {
      await api(`/produtos/${id}`, {
        method: 'PUT',
        body: JSON.stringify({
          nome, marca: $('#e-marca').value.trim() || null, categoria: p.categoria || 'Perfume',
          sku: p.sku || null, fornecedorPrincipalId: p.fornecedorPrincipalId || null,
          precoVenda: parseNum($('#e-venda').value) || 0, precoCusto, volumeMl,
          estoqueAtual: p.estoqueAtual || 0, estoqueMinimo: p.estoqueMinimo || 0, ativo: true,
        }),
      });
      toast('Perfume atualizado.');
      reload();
    } catch (e) { toast(e.message, true); ev.submitter.disabled = false; }
  });

  $('#b-excluir-perfume').addEventListener('click', async () => {
    if (!confirm(`Excluir "${p.nome}" e seus dados? Isso não pode ser desfeito.`)) return;
    try { await api(`/produtos/${id}`, { method: 'DELETE' }); toast('Perfume excluído.'); go('perfumes'); }
    catch (e) { toast(e.message, true); }
  });

  root.querySelectorAll('[data-del-decante]').forEach((b) => b.addEventListener('click', async () => {
    if (!confirm('Excluir este decante?')) return;
    try { await api(`/decantes/${b.dataset.delDecante}`, { method: 'DELETE' }); toast('Decante excluído.'); reload(); }
    catch (e) { toast(e.message, true); }
  }));

  /* novo decante */
  const form = $('#f-decante');
  if (form) {
    const insumoById = (v) => insumosAtivos.find((i) => String(i.id) === String(v)) || null;
    const custoDe = (v) => (insumoById(v) ? Number(insumoById(v).custoUnitario) || 0 : 0);
    const atualizarEmbalagem = () => {
      const total = custoDe($('#d-frasco').value) + custoDe($('#d-seringa').value) + custoDe($('#d-etiqueta').value);
      $('#d-embalagem').textContent = `Custo de embalagem: ${brl(total)}`;
    };
    ['d-frasco', 'd-seringa', 'd-etiqueta'].forEach((fid) => $('#' + fid).addEventListener('change', atualizarEmbalagem));
    atualizarEmbalagem();

    form.addEventListener('submit', async (ev) => {
      ev.preventDefault();
      const volumeMl = parseNum($('#d-vol').value);
      if (!volumeMl || volumeMl <= 0) return toast('Escolha o tamanho do decante.', true);
      ev.submitter.disabled = true;
      try {
        await api('/decantes', {
          method: 'POST',
          body: JSON.stringify({
            produtoId: id, volumeMl,
            precoVenda: parseNum($('#d-venda').value) || 0,
            custoEmbalagem: custoDe($('#d-frasco').value),
            custoSeringa: custoDe($('#d-seringa').value),
            custoEtiqueta: custoDe($('#d-etiqueta').value),
            ativo: true,
          }),
        });
        toast('Decante cadastrado.');
        reload();
      } catch (e) { toast(e.message, true); ev.submitter.disabled = false; }
    });
  }
}

async function renderInsumos() {
  const insumos = await listar('/insumos?size=500&sort=nome,asc');
  let editando = null; // id em edição

  const draw = () => {
    mount(`
      <div class="page-head">
        <h1>Insumos</h1>
        <p>O quanto você gasta com frasco, seringa e etiqueta em cada decante. É o seu <b>levantamento de custos</b>.</p>
      </div>
      <div class="grid2">
        <div class="card">
          <div class="card-h">Catálogo</div>
          <div class="card-b" style="padding:0">
            ${insumos.length ? `<div class="tbl-wrap"><table class="tbl">
              <thead><tr><th>Insumo</th><th class="num">Custo</th><th>Unidade</th><th>Status</th><th></th></tr></thead>
              <tbody>${insumos.map((i) => `<tr>
                <td><b>${esc(i.nome)}</b></td>
                <td class="num">${brl(i.custoUnitario)}</td>
                <td>${esc(i.unidade || '—')}</td>
                <td>${i.ativo ? '<span class="pill good">ativo</span>' : '<span class="pill bad">inativo</span>'}</td>
                <td class="row-actions">
                  <button class="btn ghost sm" data-edit="${i.id}">Editar</button>
                  <button class="btn danger sm" data-del="${i.id}">Excluir</button>
                </td>
              </tr>`).join('')}</tbody>
            </table></div>` : '<div class="card-b"><div class="empty">Nenhum insumo cadastrado.</div></div>'}
          </div>
        </div>

        <div class="card">
          <div class="card-h">${editando ? 'Editar insumo' : 'Adicionar insumo'}</div>
          <div class="card-b">
            <form id="f-insumo">
              <div class="field"><label for="i-nome">Nome</label><input id="i-nome" required placeholder="Frasco de vidro 15 ml"></div>
              <div class="field row2">
                <div class="field"><label for="i-custo">Custo (R$)</label>
                  <input id="i-custo" type="number" min="0" step="0.01" required placeholder="4,50"></div>
                <div class="field"><label for="i-un">Unidade</label><input id="i-un" value="unidade"></div>
              </div>
              <label class="hint" style="display:flex;gap:.4rem;align-items:center">
                <input type="checkbox" id="i-ativo" checked style="width:auto"> Ativo
              </label>
              <div class="form-actions">
                <button class="btn" type="submit">${editando ? 'Salvar alterações' : 'Salvar insumo'}</button>
                ${editando ? '<button class="btn ghost" type="button" id="i-cancelar">Cancelar</button>' : ''}
              </div>
            </form>
          </div>
        </div>
      </div>
    `);

    const item = editando ? insumos.find((i) => i.id === editando) : null;
    if (item) {
      $('#i-nome').value = item.nome || '';
      $('#i-custo').value = item.custoUnitario ?? '';
      $('#i-un').value = item.unidade || 'unidade';
      $('#i-ativo').checked = !!item.ativo;
      $('#i-cancelar').addEventListener('click', () => { editando = null; draw(); });
    }

    screenEl().querySelectorAll('[data-edit]').forEach((b) =>
      b.addEventListener('click', () => { editando = Number(b.dataset.edit); draw(); window.scrollTo({ top: 0 }); }));
    screenEl().querySelectorAll('[data-del]').forEach((b) => b.addEventListener('click', async () => {
      if (!confirm('Excluir este insumo?')) return;
      try {
        await api(`/insumos/${b.dataset.del}`, { method: 'DELETE' });
        toast('Insumo excluído.');
        go('insumos');
      } catch (e) { toast(e.message, true); }
    }));

    $('#f-insumo').addEventListener('submit', async (ev) => {
      ev.preventDefault();
      const nome = $('#i-nome').value.trim();
      const custoUnitario = parseNum($('#i-custo').value);
      if (!nome) return toast('Dê um nome ao insumo.', true);
      if (custoUnitario == null || custoUnitario < 0) return toast('Informe um custo válido.', true);
      const payload = JSON.stringify({
        nome, custoUnitario, unidade: $('#i-un').value.trim() || 'unidade', ativo: $('#i-ativo').checked,
      });
      ev.submitter.disabled = true;
      try {
        if (editando) await api(`/insumos/${editando}`, { method: 'PUT', body: payload });
        else await api('/insumos', { method: 'POST', body: payload });
        toast(editando ? 'Insumo atualizado.' : 'Insumo cadastrado.');
        go('insumos');
      } catch (e) { toast(e.message, true); ev.submitter.disabled = false; }
    });
  };

  draw();
}

/* --------- comparação / viabilidade --------- */
function bottle(ml, max) {
  const h = 34 + (ml / max) * 46;
  const y = 96 - h;
  return `<svg class="bottle" width="40" height="104" viewBox="0 0 40 104" fill="none" aria-hidden="true">
    <rect x="15" y="2" width="10" height="8" rx="1.5" style="fill:var(--line-strong)"/>
    <rect x="13" y="9" width="14" height="6" rx="1.5" style="fill:var(--accent-deep)"/>
    <rect x="8" y="${y}" width="24" height="${h}" rx="5" stroke-width="1.4" style="fill:var(--accent-wash);stroke:var(--accent)"/>
    <rect x="11" y="${y + h * 0.42}" width="18" height="${h * 0.55}" rx="3" style="fill:var(--accent);opacity:.72"/>
    <rect x="12" y="${y + 6}" width="4" height="${Math.max(6, h * 0.3)}" rx="2" style="fill:#fff;opacity:.35"/>
  </svg>`;
}
const margemClass = (m) => (Number(m) >= 40 ? 'good' : Number(m) >= 25 ? 'mid' : 'bad');

async function renderViabilidade() {
  const perfumes = await listar('/produtos?size=500&sort=nome,asc');
  if (!perfumes.length) {
    mount('<div class="page-head"><h1>Viabilidade</h1></div><div class="empty">Cadastre um perfume primeiro.</div>');
    return;
  }
  let id = state.perfumeId;
  if (!perfumes.some((p) => p.id === id)) id = perfumes[0].id;
  state.perfumeId = id;

  const seletor = `<div class="field" style="max-width:300px;margin-bottom:1.2rem">
    <label for="v-sel">Perfume</label>
    <select id="v-sel">${perfumes.map((p) => `<option value="${p.id}" ${p.id === id ? 'selected' : ''}>${esc(p.nome)}</option>`).join('')}</select>
  </div>`;

  const [p, dados] = await Promise.all([api(`/produtos/${id}`), api(`/produtos/${id}/analise`)]);
  const analises = [...dados.comparacao.analises].sort((a, b) => Number(a.volumeMl) - Number(b.volumeMl));
  const recs = dados.recomendacoes || [];
  const cm = custoMl(p);

  const wrapNoDecante = !analises.length;
  const maxMl = wrapNoDecante ? 1 : Math.max(...analises.map((a) => Number(a.volumeMl)));
  const cmp = dados.comparacao;
  const melhor = wrapNoDecante ? null
    : [...analises].sort((a, b) => Number(b.margemLote) - Number(a.margemLote))[0];

  const acaoLabel = { novo_decante: 'Cadastrar decante', editar_decante: 'Ajustar decante', editar_perfume: 'Ajustar perfume' };

  mount(`
    <div class="page-head">
      <h1>Viabilidade</h1>
      <p>“Tenho ${brl(p.precoCusto)} nesse perfume. Se eu transformar em decantes, quanto gasto, quanto faturo e quanto ganho?”</p>
    </div>
    ${seletor}

    <div class="ficha">
      <div class="title">${esc(p.nome)}<small>${esc(p.marca || '—')}</small></div>
      <div class="m"><span>Custo do frasco</span><b>${brl(p.precoCusto)}</b></div>
      <div class="m"><span>Volume</span><b>${p.volumeMl ? mlf(p.volumeMl) : '—'}</b></div>
      <div class="m"><span>Custo por ml</span><b>${cm ? brl(cm) : '—'}</b></div>
    </div>

    ${recs.length ? `<div class="recs">${recs.map((r) => `
      <div class="rec ${esc((r.nivel || 'info').toLowerCase())}">
        <span class="dot"></span>
        <div class="rec-b">
          <h4>${esc(r.titulo)}</h4>
          <p>${esc(r.texto)}</p>
          ${r.acao && acaoLabel[r.acao] ? `<button class="rec-act" data-acao="${esc(r.acao)}">${acaoLabel[r.acao]}</button>` : ''}
        </div>
      </div>`).join('')}</div>` : ''}

    ${wrapNoDecante ? '<div class="empty">Este perfume ainda não tem decantes para comparar.</div>' : `
    <div class="vials">
      ${analises.map((a) => {
        const badges = [
          a.decanteId === cmp.decanteMelhorMargem ? 'Melhor margem' : '',
          a.decanteId === cmp.decanteMaiorLucroUnitario ? 'Maior lucro/un.' : '',
          a.decanteId === cmp.decanteMelhorRetornoInvestimento ? 'Melhor retorno' : '',
        ].filter(Boolean);
        return `<div class="vial ${badges.length ? 'win' : ''}">
          <div class="badges">${badges.map((b) => `<span class="badge">${b}</span>`).join('')}</div>
          <div class="top">
            ${bottle(Number(a.volumeMl), maxMl)}
            <div>
              <div class="size">${mlf(a.volumeMl)}</div>
              <div class="price"><small>Preço de venda</small>${brl(a.precoVenda)}</div>
            </div>
          </div>
          <div class="vline"><span>Custo total</span><b>${brl(a.custoTotal)}</b></div>
          <div class="vline"><span>Lucro por unidade</span><b>${brl(a.lucroUnitario)}</b></div>
          <div class="vline"><span>Margem</span><span class="pill ${margemClass(a.margem)}">${pctf(a.margem)}</span></div>
          <div class="seg">No frasco inteiro</div>
          <div class="vline"><span>Rende</span><b>${a.quantidadeDecantes} decantes</b></div>
          <div class="vline"><span>Você investe</span><b>${brl(a.investimentoLote)}</b></div>
          <div class="vline"><span>Você fatura</span><b>${brl(a.receitaTotal)}</b></div>
          <div class="vline"><span>Você lucra</span><b>${brl(a.lucroTotal)}</b></div>
          <div class="vline"><span>Retorno (ROI)</span><b>${pctf(a.roi)}</b></div>
        </div>`;
      }).join('')}
    </div>

    <div class="verdict">
      Transformar <b>${esc(p.nome)}</b> em decantes de <b>${mlf(melhor.volumeMl)}</b> rende cerca de
      <b>${brl(melhor.lucroTotal)}</b> de lucro sobre ${brl(melhor.investimentoLote)} investidos
      (margem de ${pctf(melhor.margemLote)} no lote).
    </div>`}
  `);

  $('#v-sel').addEventListener('change', (e) => go('viabilidade', { perfumeId: Number(e.target.value) }));
  screenEl().querySelectorAll('[data-acao]').forEach((b) =>
    b.addEventListener('click', () => go('perfume', { perfumeId: id })));
}

/* ================= bootstrap ================= */
const RENDERERS = {
  inicio: renderInicio,
  perfumes: renderPerfumes,
  perfume: renderPerfume,
  insumos: renderInsumos,
  viabilidade: renderViabilidade,
};

document.querySelectorAll('#nav button').forEach((b) => {
  b.innerHTML = svg(b.dataset.go) + `<span>${b.textContent.trim()}</span>`;
  b.addEventListener('click', () => go(b.dataset.go));
});

go('inicio');
