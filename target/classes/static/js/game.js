let stompClient = null;
let meta = null;

let arenaGrid = null; // received from /api/meta? We'll infer from state trails only; arena is static.
let lastState = null;

const canvas = document.getElementById('grid');
const ctx = canvas.getContext('2d');

const elCharacter = document.getElementById('characterSelect');
const elArena = document.getElementById('arenaSelect');
const elStart = document.getElementById('startBtn');
const elLoad = document.getElementById('loadBtn');
const elSave = document.getElementById('saveBtn');
const elLbBtn = document.getElementById('lbBtn');
const elExit = document.getElementById('exitBtn');
const elPlayerHud = document.getElementById('playerHud');
const elEnemyHud = document.getElementById('enemyHud');
const elStory = document.getElementById('storyText');
const elEvents = document.getElementById('events');
const elAchievements = document.getElementById('achievements');
const elLeaderboard = document.getElementById('leaderboard');
const elManualStep = document.getElementById('manualStep');

function connect() {
  const socket = new SockJS('/ws');
  stompClient = Stomp.over(socket);
  stompClient.debug = null;

  stompClient.connect({}, () => {
    elPlayerHud.textContent = 'Connected. Choose character + arena, then Start.';
    elEnemyHud.textContent = '—';

    stompClient.subscribe('/topic/state', (msg) => {
      const state = JSON.parse(msg.body);
      lastState = state;
      render(state);
      updateHud(state);
    });

  }, (err) => {
    elPlayerHud.textContent = 'WebSocket error. Check server is running.';
    console.error(err);
  });
}

async function loadMeta() {
  const res = await fetch('/api/meta');
  meta = await res.json();

  elCharacter.innerHTML = '';
  for (const c of meta.characters) {
    const opt = document.createElement('option');
    opt.value = c.id;
    opt.textContent = `${c.displayName} (speed ${c.speed}, handling ${c.handling}, lives ${c.lives}, discs ${c.discsOwned})`;
    elCharacter.appendChild(opt);
  }

  elArena.innerHTML = '';
  for (const a of meta.arenas) {
    const opt = document.createElement('option');
    opt.value = a.id;
    opt.textContent = `${a.name}${a.open ? ' (open)' : ''}`;
    elArena.appendChild(opt);
  }
}

function startGame() {
  if (!stompClient) return;
  const playerName = document.getElementById('playerName').value || 'Player';

  const manualStep = !!(elManualStep && elManualStep.checked);

  stompClient.send('/app/start', {}, JSON.stringify({
    playerName,
    characterId: elCharacter.value,
    arenaId: elArena.value,
    manualStep,
  }));

  try {
    canvas.focus();
  } catch (e) {
    // ignore
  }
}

async function loadGame() {
  const playerName = document.getElementById('playerName').value || 'Player';
  const manualStep = !!(elManualStep && elManualStep.checked);
  const res = await fetch(`/api/load/start?playerName=${encodeURIComponent(playerName)}&manualStep=${manualStep ? 'true' : 'false'}`, { method: 'POST' });
  const body = await res.json();
  if (!body.started) {
    elPlayerHud.textContent = (elPlayerHud.textContent || '') + `\nLoad failed: ${body.message || 'unknown'}`;
  }

  try {
    canvas.focus();
  } catch (e) {
    // ignore
  }
}

async function exitGame() {
  const saveFirst = window.confirm('Save before exiting?');
  if (saveFirst) {
    await saveGame();
  }

  try {
    if (stompClient) {
      stompClient.disconnect(() => {});
    }
  } catch (e) {
    // ignore
  }
  window.location.reload();
}

async function saveGame() {
  const res = await fetch('/api/save', { method: 'POST' });
  const body = await res.json();
  if (body.saved) {
    elPlayerHud.textContent = (elPlayerHud.textContent || '') + '\nSaved.';
  } else {
    elPlayerHud.textContent = (elPlayerHud.textContent || '') + `\nSave failed: ${body.message || 'unknown'}`;
  }
}

async function loadLeaderboard() {
  const res = await fetch('/api/leaderboard');
  const rows = await res.json();
  elLeaderboard.innerHTML = '';
  for (const r of rows) {
    const div = document.createElement('div');
    div.className = 'item';
    div.textContent = `${r.playerName} — lvl ${r.highestLevel} — score ${r.totalScore} — ${r.date}`;
    elLeaderboard.appendChild(div);
  }
  if (!rows.length) {
    const div = document.createElement('div');
    div.className = 'item';
    div.textContent = 'No entries yet.';
    elLeaderboard.appendChild(div);
  }
}

function sendInput(direction, throwDisc) {
  if (!stompClient || !stompClient.connected) return;
  const manualStep = !!(elManualStep && elManualStep.checked);
  stompClient.send('/app/input', {}, JSON.stringify({ direction, throwDisc, manualStep }));
}

function sendChoice(option) {
  if (!stompClient || !stompClient.connected) return;
  stompClient.send('/app/choice', {}, JSON.stringify({ option }));
}

function updateHud(state) {
  const p = state.player;
  const status = state.running ? 'RUNNING' : (state.victory ? 'VICTORY' : 'DEFEAT');

  elPlayerHud.textContent =
    `Arena: ${state.arenaName}${state.openArena ? ' (open)' : ''}\n` +
    `Status: ${status}\n` +
    `Player: ${p.playerName} as ${p.characterName}\n` +
    `Lives: ${p.lives.toFixed(1)}\n` +
    `Level: ${p.level}  XP: ${p.xp}/${p.xpForNextLevel}\n` +
    `Discs: ${p.activeDiscs}/${p.discSlots}`;

  const enemies = state.enemies || [];
  if (!enemies.length) {
    elEnemyHud.textContent = '—';
  } else {
    elEnemyHud.textContent = enemies
      .map(e => `${e.name} (${e.type})  Lives: ${e.lives.toFixed(1)}  Discs: ${e.activeDiscs}/${e.discSlots}`)
      .join('\n');
  }

  if (state.story && state.story.text) {
    elStory.textContent = `${state.story.title}\n\n${state.story.text}`;
  } else {
    elStory.textContent = '—';
  }

  elEvents.innerHTML = '';
  for (const e of state.events || []) {
    const div = document.createElement('div');
    const { klass, text } = classifyEvent(e);
    div.className = `item ${klass}`;
    div.textContent = text;
    elEvents.appendChild(div);
  }

  elAchievements.innerHTML = '';
  for (const a of state.achievements || []) {
    const div = document.createElement('div');
    div.className = 'item';
    div.textContent = a;
    elAchievements.appendChild(div);
  }
}

function render(state) {
  const size = state.size || 40;
  const cell = Math.floor(canvas.width / size);

  ctx.clearRect(0, 0, canvas.width, canvas.height);

  // Background grid.
  ctx.fillStyle = '#070a12';
  ctx.fillRect(0, 0, canvas.width, canvas.height);

  // Arena tiles.
  if (state.arena && state.arena.length) {
    for (let y = 0; y < state.arena.length; y++) {
      const row = state.arena[y];
      for (let x = 0; x < row.length; x++) {
        const c = row[x];
        if (c === '#') {
          ctx.fillStyle = 'rgba(30, 41, 59, 0.92)';
          ctx.fillRect(x * cell, y * cell, cell, cell);
        } else if (c === 'X') {
          ctx.fillStyle = 'rgba(51, 65, 85, 0.92)';
          ctx.fillRect(x * cell, y * cell, cell, cell);
        } else if (c === 'R') {
          ctx.fillStyle = 'rgba(14, 165, 233, 0.18)';
          ctx.fillRect(x * cell, y * cell, cell, cell);
        }
      }
    }
  }

  // Trails.
  for (const t of state.trails || []) {
    ctx.fillStyle = normalizeColor(t.color, 0.62);
    ctx.fillRect(t.x * cell, t.y * cell, cell, cell);
    ctx.strokeStyle = 'rgba(255,255,255,0.18)';
    ctx.lineWidth = 1;
    ctx.strokeRect(t.x * cell + 0.5, t.y * cell + 0.5, cell - 1, cell - 1);
  }

  // Discs.
  for (const d of state.discs || []) {
    ctx.fillStyle = normalizeColor(d.ownerColor, d.flying ? 1.0 : 0.85);
    ctx.beginPath();
    ctx.arc(d.x * cell + cell / 2, d.y * cell + cell / 2, cell * 0.35, 0, Math.PI * 2);
    ctx.fill();

    ctx.strokeStyle = 'rgba(255,255,255,0.38)';
    ctx.lineWidth = 1.25;
    ctx.beginPath();
    ctx.arc(d.x * cell + cell / 2, d.y * cell + cell / 2, cell * 0.35, 0, Math.PI * 2);
    ctx.stroke();
  }

  // Enemies.
  for (const e of state.enemies || []) {
    ctx.fillStyle = normalizeColor(e.color, 1.0);
    ctx.fillRect(e.x * cell, e.y * cell, cell, cell);
    ctx.strokeStyle = 'rgba(255,255,255,0.45)';
    ctx.lineWidth = 1.25;
    ctx.strokeRect(e.x * cell + 0.5, e.y * cell + 0.5, cell - 1, cell - 1);
  }

  // Player.
  const p = state.player;
  ctx.fillStyle = normalizeColor(p.color, 1.0);
  ctx.fillRect(p.x * cell, p.y * cell, cell, cell);
  ctx.strokeStyle = 'rgba(255,255,255,0.55)';
  ctx.lineWidth = 1.5;
  ctx.strokeRect(p.x * cell + 0.5, p.y * cell + 0.5, cell - 1, cell - 1);

  // Grid lines.
  ctx.strokeStyle = 'rgba(148, 163, 184, 0.14)';
  ctx.lineWidth = 1;
  for (let i = 0; i <= size; i++) {
    ctx.beginPath();
    ctx.moveTo(i * cell, 0);
    ctx.lineTo(i * cell, size * cell);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(0, i * cell);
    ctx.lineTo(size * cell, i * cell);
    ctx.stroke();
  }
}

function normalizeColor(name, alpha) {
  // Map assignment colors to usable CSS.
  const key = (name || '').toLowerCase();
  const rgba = (r, g, b, a) => `rgba(${r},${g},${b},${a})`;

  if (key.includes('blue')) return rgba(70, 140, 255, alpha);
  // On a dark board, keep "white" truly bright.
  if (key.includes('white')) return rgba(235, 245, 255, alpha);
  if (key.includes('red')) return rgba(255, 90, 90, alpha);
  if (key.includes('gold')) return rgba(240, 200, 80, alpha);
  if (key.includes('yellow')) return rgba(255, 225, 70, alpha);
  if (key.includes('green')) return rgba(70, 220, 140, alpha);

  return rgba(200, 210, 220, alpha);
}

function bindControls() {
  window.addEventListener('keydown', (e) => {
    if (!stompClient || !stompClient.connected) return;

    // If user is typing in a form field, don't hijack keys.
    const active = document.activeElement;
    if (active && (active.tagName === 'INPUT' || active.tagName === 'TEXTAREA' || active.tagName === 'SELECT')) {
      return;
    }

    const k = e.key.toLowerCase();

    // Move: WASD + ASDF + Arrow keys
    if (k === 'w' || k === 'f' || k === 'arrowup') {
      e.preventDefault();
      sendInput('UP', false);
    }
    if (k === 's' || k === 'arrowdown') {
      e.preventDefault();
      sendInput('DOWN', false);
    }
    if (k === 'a' || k === 'arrowleft') {
      e.preventDefault();
      sendInput('LEFT', false);
    }
    if (k === 'd' || k === 'arrowright') {
      e.preventDefault();
      sendInput('RIGHT', false);
    }

    if (k === ' ') {
      e.preventDefault();
      sendInput('', true);
    }
    if (k === 'e') {
      sendInput('', true);
    }
    if (k === 'r') {
      startGame();
    }

    if (k === '1') sendChoice(1);
    if (k === '2') sendChoice(2);
  });
}

function classifyEvent(raw) {
  const s = String(raw || '');
  if (s.startsWith('[P]')) return { klass: 'player', text: s.replace(/^\[P\]\s*/, '') };
  if (s.startsWith('[E]')) return { klass: 'enemy', text: s.replace(/^\[E\]\s*/, '') };
  if (s.startsWith('[ACH]')) return { klass: 'ach', text: s.replace(/^\[ACH\]\s*/, '') };
  if (s.startsWith('[SYS]')) return { klass: 'sys', text: s.replace(/^\[SYS\]\s*/, '') };
  return { klass: 'sys', text: s };
}

elStart.addEventListener('click', startGame);
elLoad.addEventListener('click', loadGame);
elSave.addEventListener('click', saveGame);
elLbBtn.addEventListener('click', loadLeaderboard);
elExit.addEventListener('click', exitGame);

(async function init() {
  await loadMeta();
  connect();
  bindControls();
  loadLeaderboard();
})();
