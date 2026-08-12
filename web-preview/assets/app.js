// CIM Visual Preview - Sistema Intuitivo Multiconectado
const STATIONS = {
  MANUFACTURA: { name:'MANUFACTURA', label:'Manufactura', icon:'▤', mac:'AA:BB:CC:01', ip:'192.168.1.101', uuid:'CIM-ST-MAN-X2', color:'#4CAF50', type:'ROBOT_ARM', caps:'ROBOT+LASER' },
  CALIDAD:     { name:'CALIDAD',     label:'Calidad',     icon:'◎', mac:'AA:BB:CC:02', ip:'192.168.1.102', uuid:'CIM-ST-CAL-X3', color:'#2F86E8', type:'VISION',     caps:'CAMERA+ARUCO' },
  ALMACEN:     { name:'ALMACEN',     label:'Almacén',     icon:'▣', mac:'AA:BB:CC:03', ip:'192.168.1.103', uuid:'CIM-ST-ALM-X1', color:'#4CAF50', type:'STORAGE',    caps:'RACK+ROBOT' },
  PLC:         { name:'CINTA',       label:'Cinta PLC',   icon:'⇄', mac:'AA:BB:CC:04', ip:'192.168.1.104', uuid:'CIM-ST-PLC-X4', color:'#E0A526', type:'CONVEYOR',   caps:'RELAY+SENSOR' },
  WEAR:        { name:'WEAR',        label:'Wear OS',     icon:'⌚', mac:'AA:BB:CC:05', ip:'192.168.1.105', uuid:'CIM-ST-WEAR-X5',color:'#2F86E8', type:'WEAR',       caps:'MONITOR' },
};

let state = {
  serverRunning:false,
  autoMode:false,
  token:'CIM_LAB_PAIRING_TOKEN_CHANGE_ME',
  connected:{},
  authorized:{},
  pending:[],
  logs:[],
  pallet:{pos:0, moving:false, product:'tuerca_macho'},
  exec:{ currentFlow:'LISTO', eStop:false, stations:{} },
  terminal:[],
};

function init(){
  Object.keys(STATIONS).forEach(k=>{
    state.exec.stations[k]= { name:k, label:STATIONS[k].label, status:'ONLINE', detail:'Listo', lastEvent:'Sin eventos' };
  });
  load();
  renderAll();
  setInterval(tick, 1000);
}

function save(){ localStorage.setItem('cim_visual_v2', JSON.stringify({autoMode:state.autoMode, token:state.token, connected:state.connected, authorized:state.authorized})); }
function load(){
  try{
    const s=JSON.parse(localStorage.getItem('cim_visual_v2')||'{}');
    if(s.autoMode!==undefined) state.autoMode=s.autoMode;
    if(s.token) state.token=s.token;
    if(s.connected) state.connected=s.connected||{};
    if(s.authorized) state.authorized=s.authorized||{};
  }catch(e){}
}

function log(msg, level='info'){
  const time=new Date().toLocaleTimeString();
  state.logs.unshift({time, msg, level});
  if(state.logs.length>200) state.logs.pop();
  state.terminal.unshift(`[${time}] ${msg}`);
  if(state.terminal.length>100) state.terminal.pop();
  renderLogs();
}

function startServer(){
  if(state.serverRunning) return;
  state.serverRunning=true;
  log('✓ TCP Server escuchando en 0.0.0.0:8888', 'ok');
  log('✓ NSD publicado: _cim-hub._tcp.', 'ok');
  log('✓ BLE SPP Server iniciado', 'ok');
  save();
  renderAll();
}
function stopServer(){
  state.serverRunning=false;
  Object.keys(state.connected).forEach(k=> disconnect(k));
  log('✗ Servidor detenido', 'err');
  save();
  renderAll();
}

function toggleAuto(){
  state.autoMode=!state.autoMode;
  log(state.autoMode ? '✓ Modo AUTO activado: autorización automática' : '✗ Modo AUTO desactivado', state.autoMode?'ok':'warn');
  save();
  renderAll();
  if(state.autoMode){
    // auto aprueba pendientes
    [...state.pending].forEach(p=> approve(p.mac));
  }
}

function connectStation(key){
  const st=STATIONS[key];
  if(!st) return;
  if(!state.serverRunning){ log('✗ Servidor no iniciado - Inicia el Hub primero', 'err'); return; }
  if(state.connected[st.mac]){ log(`⟳ ${st.label} ya conectado`, 'warn'); return; }

  log(`→ ${st.label}: Handshake CIM_MASTER_HUB_V1;${st.name};${maskToken(state.token)};${st.mac};${st.uuid}`, 'cmd');
  state.connected[st.mac]= {...st, connectedAt:Date.now(), lastSeen:Date.now(), ip:st.ip };
  // simula validación token
  const valid = state.token.length>=8 && !state.token.includes('CHANGE_ME') || state.token==='CIM_LAB_PAIRING_TOKEN_CHANGE_ME';
  if(!valid){
    log(`✗ ${st.label}: Token inválido`, 'err');
    delete state.connected[st.mac];
    renderAll();
    return;
  }
  if(state.autoMode || state.authorized[st.mac]){
    authorize(st.mac);
  }else{
    const req={ id:Date.now().toString(), mac:st.mac, name:st.label, type:st.name, deviceName:st.label, at:Date.now() };
    state.pending.push(req);
    state.exec.stations[key].status='WARNING';
    state.exec.stations[key].detail='Esperando autorización';
    log(`⏳ ${st.label}: Solicita autorización - pendiente en Coordinador`, 'warn');
    showPermissionDialog(req);
  }
  save();
  renderAll();
}

function disconnect(keyOrMac){
  let mac = keyOrMac.includes(':') ? keyOrMac : (STATIONS[keyOrMac]?.mac || keyOrMac);
  const st = Object.values(STATIONS).find(s=>s.mac===mac) || {label:mac};
  delete state.connected[mac];
  // keep authorized memory but mark offline
  state.exec.stations[Object.keys(STATIONS).find(k=>STATIONS[k].mac===mac)] && (state.exec.stations[Object.keys(STATIONS).find(k=>STATIONS[k].mac===mac)].status='ONLINE');
  log(`✗ ${st.label||mac} desconectado`, 'err');
  save();
  renderAll();
}

function authorize(mac){
  const st = Object.values(STATIONS).find(s=>s.mac===mac);
  if(!st) return;
  state.authorized[mac]=true;
  state.pending=state.pending.filter(p=>p.mac!==mac);
  const key=Object.keys(STATIONS).find(k=>STATIONS[k].mac===mac);
  if(key){
    state.exec.stations[key].status='READY';
    state.exec.stations[key].detail='Autorizado y listo';
    state.exec.stations[key].lastEvent='VALIDADO '+new Date().toLocaleTimeString();
  }
  log(`✓ ${st.label}: Autorizado → VALIDADO`, 'ok');
  hidePermissionDialog();
  save();
  renderAll();
}

function reject(mac){
  const st = Object.values(STATIONS).find(s=>s.mac===mac);
  state.pending=state.pending.filter(p=>p.mac!==mac);
  delete state.connected[mac];
  const key=Object.keys(STATIONS).find(k=>STATIONS[k].mac===mac);
  if(key){
    state.exec.stations[key].status='STOPPED';
    state.exec.stations[key].detail='Rechazado';
  }
  log(`✗ ${st?.label||mac}: Rechazado → DENIED`, 'err');
  hidePermissionDialog();
  save();
  renderAll();
}

function approve(mac){ authorize(mac); }

function maskToken(t){ return t.length>8 ? t.slice(0,4)+'***'+t.slice(-3) : '***'; }

function connectAll(){
  if(!state.serverRunning) startServer();
  setTimeout(()=> Object.keys(STATIONS).forEach((k,i)=> setTimeout(()=> connectStation(k), i*400)), 300);
  log('→ Iniciando conexión masiva de 5 estaciones...', 'cmd');
}

function disconnectAll(){
  Object.keys(STATIONS).forEach(k=> disconnect(k));
  state.pending=[];
  hidePermissionDialog();
  log('✗ Desconexión masiva ejecutada', 'warn');
  renderAll();
}

function sendCommand(from, to, cmd){
  const f=STATIONS[from] || {label:from};
  const t=STATIONS[to] || {label:to};
  log(`→ ${f.label} → ${t.label}: ${cmd}`, 'cmd');
  // simula respuesta
  setTimeout(()=>{
    log(`← ${t.label}: ACK ${cmd}`, 'ok');
    if(cmd.includes('DELIVER')){
      const match=cmd.match(/DELIVER:(\d+):(\d+)/);
      if(match){
        const fromPos=parseInt(match[1]), toPos=parseInt(match[2]);
        movePallet(fromPos, toPos);
      }
    }
  }, 400+Math.random()*600);
}

function movePallet(from,to){
  state.pallet.moving=true;
  state.pallet.pos=from;
  renderExec();
  let steps=0;
  const interval=setInterval(()=>{
    state.pallet.pos += from<to ? 1 : -1;
    steps++;
    if(state.pallet.pos===to || steps>20){
      clearInterval(interval);
      state.pallet.moving=false;
      state.pallet.pos=to;
      log(`✓ Pallet ${state.pallet.product} llegó a estación ${to}`, 'ok');
    }
    renderExec();
  }, 300);
}

function triggerEStop(){
  state.exec.eStop=!state.exec.eStop;
  Object.keys(state.exec.stations).forEach(k=>{
    state.exec.stations[k].status= state.exec.eStop ? 'STOPPED' : 'READY';
    state.exec.stations[k].detail= state.exec.eStop ? 'E-STOP ACTIVO' : 'Operativo';
  });
  log(state.exec.eStop ? '✖ E-STOP ACTIVADO - Frenando Scorbot y Cinta' : '✓ E-STOP Liberado - Sistema listo', state.exec.eStop?'err':'ok');
  renderExec();
}

function calibrateGlobal(){
  log('→ FLUJO: Calibración global ejecutada', 'cmd');
  Object.keys(state.exec.stations).forEach(k=>{
    state.exec.stations[k].status='BUSY';
    state.exec.stations[k].detail='Calibrando...';
  });
  renderExec();
  setTimeout(()=>{
    Object.keys(state.exec.stations).forEach(k=>{
      state.exec.stations[k].status='READY';
      state.exec.stations[k].detail='Calibrado';
    });
    log('✓ Calibración global OK', 'ok');
    renderExec();
  }, 1800);
}

function simulateFullCycle(){
  log('→ SIMULACIÓN CICLO COMPLETO INICIADA', 'cmd');
  state.exec.currentFlow='CICLO EN CURSO';
  renderExec();
  const seq=[
    ()=> sendCommand('PLC','MANUFACTURA','PLC:START'),
    ()=> sendCommand('MANUFACTURA','MANUFACTURA','R:HOME'),
    ()=> sendCommand('MANUFACTURA','MANUFACTURA','R:RUN ARU1'),
    ()=> sendCommand('PLC','CALIDAD','C:DELIVER:1:3'),
    ()=> sendCommand('CALIDAD','CALIDAD','ARUCO:DETECT'),
    ()=> sendCommand('CALIDAD','MANUFACTURA','VAL:PASS'),
    ()=> sendCommand('PLC','ALMACEN','C:DELIVER:3:1'),
    ()=> sendCommand('ALMACEN','ALMACEN','STO:STORE:POS1'),
  ];
  seq.forEach((fn,i)=> setTimeout(fn, i*900));
  setTimeout(()=>{
    state.exec.currentFlow='CICLO COMPLETADO';
    log('✓ Ciclo completo simulado - Pallet almacenado', 'ok');
    renderExec();
  }, seq.length*900+500);
}

function tick(){
  // actualiza lastSeen
  Object.values(state.connected).forEach(c=> c.lastSeen=Date.now());
  // stale check
  const now=Date.now();
  Object.keys(state.connected).forEach(mac=>{
    if(now - state.connected[mac].lastSeen > 15000 && !state.pending.find(p=>p.mac===mac)){
      // still ok in sim
    }
  });
}

/* Render */

function renderAll(){
  renderHeader();
  renderCoordinator();
  renderStations();
  renderVisualWires();
  renderPermissionQueue();
}

function renderHeader(){
  const el=document.getElementById('headerStatus');
  if(!el) return;
  const total=Object.keys(state.connected).length;
  const auth=Object.keys(state.authorized).filter(m=> state.connected[m]).length;
  el.innerHTML=`
    <span class="badge ${state.serverRunning?'badge-online':'badge-off'}">${state.serverRunning?'● SERVER ON':'○ SERVER OFF'}</span>
    <span class="badge badge-auth">${total} Conectados</span>
    <span class="badge badge-online">${auth} Autorizados</span>
    <span class="badge ${state.autoMode?'badge-online':'badge-off'}">${state.autoMode?'AUTO':'MANUAL'}</span>
  `;
}

function renderCoordinator(){
  const srv=document.getElementById('coordinatorServer');
  if(srv){
    srv.innerHTML=`
      <div class="status-row"><span class="status-label">Estado Servidor</span><span class="status-value">${state.serverRunning?'<span class="dot dot-green"></span> ESCUCHANDO :8888':'<span class="dot dot-red"></span> DETENIDO'}</span></div>
      <div class="status-row"><span class="status-label">NSD</span><span class="status-value">${state.serverRunning?'_cim-hub._tcp. publicado':'detenido'}</span></div>
      <div class="status-row"><span class="status-label">Token Emparejamiento</span><span class="status-value mono" style="font-size:10px">${maskToken(state.token)} ${state.token.includes('CHANGE_ME')?'<span style="color:#E0A526">⚠ default</span>':''}</span></div>
      <div class="status-row"><span class="status-label">Dispositivos</span><span class="status-value">${Object.keys(state.connected).length} / 5</span></div>
      <div class="status-row"><span class="status-label">Pendientes</span><span class="status-value">${state.pending.length}</span></div>
    `;
  }
  const autoBtn=document.getElementById('autoModeToggle');
  if(autoBtn){
    autoBtn.textContent=state.autoMode?'AUTO: ON':'AUTO: OFF';
    autoBtn.className='btn '+(state.autoMode?'btn-success':'btn-ghost')+' btn-block';
  }
  renderExec();
}

function renderExec(){
  const execGrid=document.getElementById('execGrid');
  const flowEl=document.getElementById('flowStatus');
  const eStopEl=document.getElementById('eStopStatus');
  if(flowEl) flowEl.textContent=state.exec.currentFlow;
  if(eStopEl){
    eStopEl.textContent= state.exec.eStop ? 'EMERGENCIA ACTIVA' : 'Sistema OK';
    eStopEl.style.color= state.exec.eStop ? '#D23936' : '#4CAF50';
  }
  if(!execGrid) return;
  execGrid.innerHTML=Object.values(state.exec.stations).map(s=>{
    const colors={READY:'#3B8E35',ONLINE:'#5B646E',BUSY:'#2F86E8',WARNING:'#E0A526',STOPPED:'#D23936'};
    const bg=colors[s.status]||'#1B2027';
    return `<div class="exec-card" style="border-left:3px solid ${bg}">
      <div style="display:flex;justify-content:space-between;align-items:center">
        <h4>${s.label}</h4><span class="dot" style="background:${bg};box-shadow:0 0 6px ${bg}"></span>
      </div>
      <p>${s.detail}</p>
      <p style="font-size:9px;opacity:0.6">${s.lastEvent}</p>
    </div>`;
  }).join('');
  // pallet viz
  const palletEl=document.getElementById('palletViz');
  if(palletEl){
    const pos=state.pallet.pos;
    palletEl.innerHTML=`<div style="display:flex;gap:6px;margin-top:8px">${[1,2,3,4,5,6,7,8,9,10].map(i=>`<div style="flex:1;height:26px;border-radius:6px;display:flex;align-items:center;justify-content:center;font-size:9px;font-weight:800;border:1px solid ${pos===i?'#4CAF50':'rgba(255,255,255,0.08)'};background:${pos===i?'rgba(76,175,80,0.18)':'#13171C'};color:${pos===i?'#4CAF50':'#5B646E'}">${pos===i?'●':''}${i}</div>`).join('')}</div>
    <div style="font-size:10px;color:#8A939E;margin-top:6px">Pallet: ${state.pallet.product} @ pos ${pos} ${state.pallet.moving?'● MOVING':''}</div>`;
  }
}

function renderStations(){
  const container=document.getElementById('stationsGrid');
  if(!container) return;
  container.innerHTML=Object.keys(STATIONS).map(key=>{
    const def=STATIONS[key];
    const connected=!!state.connected[def.mac];
    const authorized=!!state.authorized[def.mac];
    const pending=!!state.pending.find(p=>p.mac===def.mac);
    let status='OFFLINE', dot='dot-red', badge='badge-off', text='Desconectado';
    if(pending){ status='PENDING'; dot='dot-yellow'; badge='badge-wait'; text='Esperando autorización'; }
    else if(connected && authorized){ status='VALIDADO'; dot='dot-green'; badge='badge-online'; text='VALIDADO'; }
    else if(connected){ status='CONECTADO'; dot='dot-blue'; badge='badge-auth'; text='Conectado (sin auth)'; }

    return `<div class="card" style="border-color:${connected?def.color+'55':''}">
      <div class="card-header">
        <h3><span style="font-size:16px">${def.icon}</span> ${def.label} <span class="badge ${badge}" style="margin-left:6px;font-size:9px">${text}</span></h3>
        <span class="dot ${dot}"></span>
      </div>
      <div class="card-body">
        <div class="status-row"><span class="status-label">MAC</span><span class="status-value mono" style="font-size:10px">${def.mac}</span></div>
        <div class="status-row"><span class="status-label">IP</span><span class="status-value mono" style="font-size:10px">${def.ip}</span></div>
        <div class="status-row"><span class="status-label">UUID</span><span class="status-value mono" style="font-size:9px">${def.uuid}</span></div>
        <div class="status-row"><span class="status-label">Caps</span><span class="status-value" style="font-size:10px">${def.caps}</span></div>
        <div style="display:flex;gap:6px;margin-top:6px">
          ${!connected?`<button class="btn btn-primary btn-sm" style="flex:1" onclick="connectStation('${key}')">VINCULAR AL HUB</button>`:`<button class="btn btn-ghost btn-sm" style="flex:1" onclick="disconnect('${key}')">✖ Desconectar</button>`}
          ${connected && !authorized && !pending?`<button class="btn btn-ghost btn-sm" onclick="requestAuth('${def.mac}')">AUTH</button>`:''}
        </div>
        <div style="margin-top:10px">${renderStationSpecific(key, connected && authorized)}</div>
      </div>
    </div>`;
  }).join('');
}

function renderStationSpecific(key, ready){
  const def=STATIONS[key];
  if(key==='PLC'){
    return `<div style="display:flex;flex-direction:column;gap:8px">
      <div class="grid-2">
        <button class="btn ${ready?'btn-success':'btn-ghost'} btn-sm" ${!ready?'disabled':''} onclick="sendCommand('PLC','PLC','PLC:START')">▶ START Cinta</button>
        <button class="btn ${ready?'btn-error':'btn-ghost'} btn-sm" ${!ready?'disabled':''} onclick="sendCommand('PLC','PLC','PLC:STOP')">■ STOP</button>
      </div>
      <div style="font-size:10px;color:var(--text-sec);font-weight:700;letter-spacing:0.5px">MATRIZ 3x10 DELIVER</div>
      <div class="station-matrix">${Array.from({length:30},(_,i)=>{ const from=Math.floor(i/10)+1; const to=(i%10)+1; return `<div class="matrix-btn" onclick="sendCommand('PLC','PLC','C:DELIVER:${from}:${to}')" title="De ${from} a ${to}">${from}>${to}</div>`}).join('')}</div>
    </div>`;
  }
  if(key==='MANUFACTURA'){
    return `<div style="display:flex;flex-direction:column;gap:8px">
      <div class="grid-2">
        <button class="btn ${ready?'btn-primary':'btn-ghost'} btn-sm" ${!ready?'disabled':''} onclick="sendCommand('MANUFACTURA','MANUFACTURA','R:HOME')">HOME</button>
        <button class="btn ${ready?'btn-ghost':'btn-ghost'} btn-sm" ${!ready?'disabled':''} onclick="sendCommand('MANUFACTURA','MANUFACTURA','R:READY')">✓ READY</button>
      </div>
      <div class="grid-3">
        ${['ARU','ARU1','ARU2'].map(p=>`<button class="btn btn-ghost btn-sm" ${!ready?'disabled':''} onclick="sendCommand('MANUFACTURA','MANUFACTURA','R:RUN ${p}')">RUN ${p}</button>`).join('')}
      </div>
      <div class="laser-viz"><div class="laser-beam"></div><span style="font-size:10px;color:#D23936">LASER Preview 80% 1200mm/min</span></div>
      <div class="grid-2">
        <button class="btn btn-success btn-sm" ${!ready?'disabled':''} onclick="sendCommand('MANUFACTURA','MANUFACTURA','L:START')">LASER START</button>
        <button class="btn btn-error btn-sm" ${!ready?'disabled':''} onclick="sendCommand('MANUFACTURA','MANUFACTURA','L:STOP')">⛔ LASER STOP</button>
      </div>
    </div>`;
  }
  if(key==='CALIDAD'){
    return `<div style="display:flex;flex-direction:column;gap:8px">
      <div class="camera-viz"><div class="camera-grid"></div><div style="z-index:1;display:flex;flex-direction:column;align-items:center;gap:6px"><div class="aruco-marker"><div class="aruco-cell"></div><div class="aruco-cell w"></div><div class="aruco-cell"></div><div class="aruco-cell w"></div><div class="aruco-cell w"></div><div class="aruco-cell"></div><div class="aruco-cell w"></div><div class="aruco-cell"></div><div class="aruco-cell"></div><div class="aruco-cell w"></div><div class="aruco-cell"></div><div class="aruco-cell w"></div><div class="aruco-cell w"></div><div class="aruco-cell"></div><div class="aruco-cell w"></div><div class="aruco-cell"></div></div><span style="font-size:9px;color:#4CAF50">ArUco #12 detectado 45°</span></div></div>
      <div class="grid-2">
        <button class="btn btn-primary btn-sm" ${!ready?'disabled':''} onclick="sendCommand('CALIDAD','CALIDAD','ARUCO:DETECT')">SCAN ArUco</button>
        <button class="btn btn-ghost btn-sm" ${!ready?'disabled':''} onclick="sendCommand('CALIDAD','MANUFACTURA','VAL:PASS')">✓ VAL PASS</button>
      </div>
      <div class="status-row"><span class="status-label">Modelo</span><span class="status-value" style="font-size:10px">YOLOv8n-int8 TFLite</span></div>
    </div>`;
  }
  if(key==='ALMACEN'){
    return `<div class="rack-viz">${Array.from({length:18},(_,i)=>{
        const level = 6 - Math.floor(i/3); const col = (i%3)+1; const pos = (level-1)*3+col;
        const occ = pos%3===1;
        return `<div class="rack-slot ${occ?'occupied':''}" title="Nivel ${level} · Columna ${col}">${occ?'▣':'▢'} A-01-${String(pos).padStart(2,'0')}</div>`;
      }).join('')}</div>
      <div class="rack-legend"><span style="color:var(--primary)"><i style="background:rgba(76,175,80,0.35)"></i>OCUPADO</span><span style="color:var(--text-dim)"><i></i>VACÍO</span></div>
      <div class="grid-2" style="margin-top:8px">
        <button class="btn btn-ghost btn-sm" ${!ready?'disabled':''} onclick="sendCommand('ALMACEN','ALMACEN','STO:STORE:POS${Math.floor(Math.random()*3)+1}')">STORE</button>
        <button class="btn btn-ghost btn-sm" ${!ready?'disabled':''} onclick="sendCommand('ALMACEN','ALMACEN','STO:RETRIEVE:POS${Math.floor(Math.random()*3)+1}')">RETRIEVE</button>
      </div>`;
  }
  if(key==='WEAR'){
    return `<div class="wear-frame"><div class="wear-screen">
      <div style="font-size:10px;font-weight:800;color:var(--primary)">CIM WEAR</div>
      <div style="font-size:9px;color:var(--text-sec)">${ready?'● Conectado al Hub':'○ Offline'}</div>
      <div style="display:flex;flex-direction:column;gap:4px;margin-top:6px">
        ${Object.values(STATIONS).slice(0,4).map(s=>{
          const ok=!!state.connected[s.mac];
          return `<div style="display:flex;justify-content:space-between;font-size:9px"><span>${s.label}</span><span class="dot ${ok?'dot-green':'dot-red'}" style="width:6px;height:6px"></span></div>`;
        }).join('')}
      </div>
    </div></div>`;
  }
  return '';
}

function renderVisualWires(){
  const container=document.getElementById('visualWires');
  if(!container) return;
  const total=Object.keys(state.connected).length;
  container.innerHTML=`
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
      <h3 style="font-size:12px;font-weight:800;letter-spacing:0.5px">MAPA DE RED - VISUAL</h3>
      <span style="font-size:10px;color:var(--text-sec)">${total} nodos vinculados</span>
    </div>
    <svg class="wire-svg" viewBox="0 0 400 200" preserveAspectRatio="none">
      ${Object.values(state.connected).map((c,i)=>{
        const x=80 + (i*70) % 320;
        const y=50 + Math.floor(i/4)*80;
        const connected=!!state.authorized[c.mac];
        return `<path d="M200 20 Q ${x} 60 ${x} ${y}" stroke="${connected?'#4CAF50':'#5B646E'}" stroke-width="1.5" fill="none" stroke-dasharray="${connected?'0':'4 4'}" opacity="0.6"><animate attributeName="stroke-dashoffset" from="0" to="8" dur="0.5s" repeatCount="indefinite"/></path>`;
      }).join('')}
    </svg>
    <div style="display:flex;justify-content:center;gap:24px;flex-wrap:wrap;position:relative;z-index:2">
      <div style="display:flex;flex-direction:column;align-items:center;gap:6px"><div class="node ${state.serverRunning?'active':''}" style="background:${state.serverRunning?'rgba(76,175,80,0.15)':''};border-color:${state.serverRunning?'#4CAF50':''}">▤</div><span style="font-size:10px;font-weight:800">HUB</span><span style="font-size:9px;color:var(--text-sec)">${state.serverRunning?':8888 ON':'OFF'}</span></div>
      ${Object.values(STATIONS).map(s=>{
        const con=!!state.connected[s.mac];
        const auth=!!state.authorized[s.mac];
        return `<div style="display:flex;flex-direction:column;align-items:center;gap:6px"><div class="node ${con?(auth?'success':'active'):''}">${s.icon}</div><span style="font-size:9px;font-weight:700">${s.label}</span><span style="font-size:8px;color:${auth?'#4CAF50':con?'#E0A526':'#5B646E'}">${auth?'VALIDADO':con?'CONECTADO':'OFF'}</span></div>`;
      }).join('')}
    </div>
  `;
}

function renderLogs(){
  const term=document.getElementById('terminal');
  if(!term) return;
  term.innerHTML=state.logs.slice(0,50).map(l=>{
    const cls=l.level==='ok'?'ok': l.level==='err'?'err': l.level==='warn'?'warn': l.level==='cmd'?'cmd':'info';
    return `<div class="line ${cls}">[${l.time}] ${l.msg}</div>`;
  }).join('');
  term.scrollTop=0;
}

function renderPermissionQueue(){
  const cont=document.getElementById('permissionQueue');
  if(!cont) return;
  if(state.pending.length===0){
    cont.innerHTML=`<div style="font-size:11px;color:var(--text-sec);text-align:center;padding:12px">Sin solicitudes pendientes</div>`;
    return;
  }
  cont.innerHTML=state.pending.map(p=>`
    <div style="background:#1B2027;border:1px solid rgba(255,255,255,0.08);border-radius:8px;padding:10px;display:flex;justify-content:space-between;align-items:center">
      <div><div style="font-size:12px;font-weight:800">${p.deviceName} <span style="font-size:10px;color:var(--text-sec)">${p.mac}</span></div><div style="font-size:10px;color:var(--text-sec)">${p.type} • ${new Date(p.at).toLocaleTimeString()}</div></div>
      <div style="display:flex;gap:6px"><button class="btn btn-success btn-sm" onclick="approve('${p.mac}')">✓ Autorizar</button><button class="btn btn-error btn-sm" onclick="reject('${p.mac}')">✗ Rechazar</button></div>
    </div>
  `).join('');
}

function showPermissionDialog(req){
  if(state.autoMode) return;
  const existing=document.getElementById('permDialog');
  if(existing) existing.remove();
  const div=document.createElement('div');
  div.id='permDialog';
  div.className='permission-dialog';
  div.innerHTML=`<div class="dialog">
    <h3>AUTORIZAR DISPOSITIVO</h3>
    <p>¿Autorizar <b>${req.deviceName}</b> (${req.type}) desde <span class="mono">${req.mac}</span>? Este equipo solicita unirse a la red CIM industrial.</p>
    <div style="display:flex;gap:8px">
      <button class="btn btn-success" style="flex:1" onclick="approve('${req.mac}')">✓ Autorizar Siempre</button>
      <button class="btn btn-ghost" style="flex:1" onclick="approve('${req.mac}')">⏱ Autorizar Una Vez</button>
      <button class="btn btn-error" onclick="reject('${req.mac}')">✗ Rechazar</button>
    </div>
    <div style="margin-top:12px;font-size:10px;color:var(--text-sec)">Tip: Activa AUTO MODE para aprobar automáticamente en laboratorio</div>
  </div>`;
  div.addEventListener('click', (e)=>{ if(e.target===div) hidePermissionDialog(); });
  document.body.appendChild(div);
}
function hidePermissionDialog(){
  const el=document.getElementById('permDialog');
  if(el) el.remove();
}
function requestAuth(mac){
  log(`→ Solicitando re-auth para ${mac}`, 'cmd');
  const req={ id:Date.now().toString(), mac, deviceName:Object.values(STATIONS).find(s=>s.mac===mac)?.label||mac, type:'RE-AUTH', at:Date.now() };
  state.pending.push(req);
  showPermissionDialog(req);
  renderAll();
}

// Expose globally for inline handlers
window.connectStation=connectStation;
window.disconnect=disconnect;
window.approve=approve;
window.reject=reject;
window.startServer=startServer;
window.stopServer=stopServer;
window.toggleAuto=toggleAuto;
window.connectAll=connectAll;
window.disconnectAll=disconnectAll;
window.sendCommand=sendCommand;
window.triggerEStop=triggerEStop;
window.calibrateGlobal=calibrateGlobal;
window.simulateFullCycle=simulateFullCycle;
window.requestAuth=requestAuth;

document.addEventListener('DOMContentLoaded', init);
