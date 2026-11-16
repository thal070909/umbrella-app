import "./App.css";

// 날씨 판단과 주변 장소 검색은 우산챙겨 서버가 맡는다 (server/)
const API_BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

function renderHome() {
  const root = document.getElementById("root");
  root.innerHTML = `
<div class="app">
  <div class="screen" id="screen">

    <!-- ============ HOME VIEW ============ -->
    <div class="view active" id="view-home">
      <div class="header">
        <div class="brand"><span class="brand-mark">🌂</span>우산챙겨!</div>
        <div style="display:flex; align-items:center; gap:8px;">
          <button class="location-btn" id="locationBtn">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M12 21s-7-6.1-7-11.5A7 7 0 0 1 19 9.5C19 14.9 12 21 12 21z"/><circle cx="12" cy="9.5" r="2.4"/></svg>
            <span id="locationLabel">위치 확인 중</span>
          </button>
          <button class="icon-btn" id="refreshBtn" title="새로고침">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-2.6-6.36"/><path d="M21 3v6h-6"/></svg>
          </button>
        </div>
      </div>

      <div class="hero state-clear" id="hero">
        <div class="sun-rays" id="sunRays"></div>
        <div class="rain-layer" id="rainLayer"></div>
        <div class="mascot-wrap">
          <svg class="mascot" id="mascot" viewBox="0 0 108 108" fill="none">
            <ellipse cx="54" cy="60" rx="34" ry="26" fill="#ffffff" fill-opacity="0.95"/>
            <circle cx="30" cy="52" r="16" fill="#ffffff" fill-opacity="0.95"/>
            <circle cx="78" cy="50" r="18" fill="#ffffff" fill-opacity="0.95"/>
            <circle class="face-eye" cx="44" cy="60" r="3.6" fill="#2A2F3A"/>
            <circle class="face-eye" cx="66" cy="60" r="3.6" fill="#2A2F3A"/>
            <path class="face-mouth" d="M46 70 Q55 77 64 70" stroke="#2A2F3A" stroke-width="3" stroke-linecap="round" fill="none"/>
            <ellipse cx="38" cy="55" rx="4" ry="2.6" fill="#FFB4B4" fill-opacity=".7"/>
            <ellipse cx="72" cy="55" rx="4" ry="2.6" fill="#FFB4B4" fill-opacity=".7"/>
          </svg>
        </div>
        <div class="hero-top">
          <div>
            <div class="hero-cond" id="condText">날씨 확인 중이에요</div>
            <div class="hero-msg" id="condMsg">잠시만 기다려주세요</div>
          </div>
        </div>
        <div class="hero-temp-row">
          <div class="hero-temp" id="tempMain">--</div>
          <div class="hero-temp-unit">°C</div>
        </div>
        <div class="hero-feels" id="feelsText">체감온도 계산 중이에요</div>
      </div>

      <div class="umbrella-banner" id="umbrellaBanner" style="display:none;">
        <div class="emoji">🌂</div>
        <div>
          <div class="umbrella-banner-text" id="umbrellaText">오늘은 우산을 챙기세요</div>
          <div class="umbrella-banner-sub" id="umbrellaSub">외출 전 꼭 확인하세요</div>
        </div>
      </div>

      <div class="heat-banner" id="heatCarBanner" style="display:none;">
        <div class="emoji">🚗</div>
        <div>
          <div class="heat-banner-text" id="heatCarText">실외 기온이 30도가 넘어요</div>
          <div class="heat-banner-sub">차 안에 보조배터리나 캔음료를 두고 내리지 않았는지 확인하세요</div>
        </div>
      </div>

      <div class="cards">
        <div class="card">
          <div class="card-head">
            <div class="card-icon" style="background:var(--blue-light);">💧</div>
            <div class="card-title">강수 정보</div>
            <div class="card-title-sub" id="precipUpdated">--</div>
          </div>
          <div class="precip-row">
            <div class="precip-stat"><div class="num" id="precipAmount">0</div><div class="lbl">mm/h</div></div>
            <div class="precip-stat"><div class="num" id="precipProb">0%</div><div class="lbl">강수확률</div></div>
            <div class="precip-stat"><div class="num" id="humidity">0%</div><div class="lbl">습도</div></div>
          </div>
          <div class="precip-desc" id="precipDesc">현재 날씨 정보를 불러오고 있어요.</div>
        </div>

        <div class="card">
          <div class="card-head">
            <div class="card-icon" style="background:var(--amber-light);">👕</div>
            <div class="card-title">오늘의 옷차림</div>
          </div>
          <div class="outfit-body">
            <div class="outfit-emoji" id="outfitEmoji">🧥</div>
            <div>
              <div class="outfit-title" id="outfitTitle">옷차림 계산 중</div>
              <div class="outfit-desc" id="outfitDesc">체감온도를 기준으로 추천해드려요.</div>
            </div>
          </div>
        </div>
      </div>

      <div class="notice-banner" id="fallbackNotice" style="display:none;">
        📍 위치를 가져올 수 없어 서울 기준으로 보여드리고 있어요.
      </div>
    </div>

    <!-- ============ SPOTS VIEW ============ -->
    <div class="view" id="view-spots">
      <div class="spots-header">
        <div class="spots-title" id="spotsTitle">☔ 비 피하기 스팟</div>
        <div class="spots-subtitle" id="spotsSubtitle">장마철 갑자기 비가 쏟아져도 당황하지 마세요. 근처 카페와 음식점을 찾아드려요.</div>
      </div>
      <div class="radius-chip-row" id="categoryChipRow">
        <div class="chip active" data-cat="rain">☔ 비 피하기</div>
        <div class="chip" data-cat="heat">🧊 무더위쉼터</div>
      </div>
      <div class="radius-chip-row" style="padding-top:0;">
        <div class="chip" style="background:transparent; box-shadow:none; color:var(--text-tertiary); padding-left:0;">도보 15분 이내</div>
        <div class="chip" id="spotsRefreshChip">🔄 다시 찾기</div>
      </div>
      <div id="spotsContent">
        <div class="state-box">
          <div class="spinner"></div>
          <div class="state-title">근처 스팟을 찾고 있어요</div>
          <div class="state-desc">위치 정보를 기반으로 검색 중이에요</div>
        </div>
      </div>
      <div class="disclosure">🏷️ '제휴' 배지가 붙은 매장은 제휴를 통해 우선 노출돼요. 실제 이용 후기와 정보를 꼭 확인해주세요.</div>
      <div class="ad-banner">
        <div class="ad-label">AD</div>
        <div class="ad-copy">이 자리에 광고가 표시될 수 있어요</div>
      </div>
    </div>

  </div>

  <div class="tabbar">
    <button class="tab active" data-view="home">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 11.5 12 4l9 7.5"/><path d="M5 10v10h14V10"/></svg>
      <span>홈</span>
    </button>
    <button class="tab" data-view="spots">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 21s-7-6.1-7-11.5A7 7 0 0 1 19 9.5C19 14.9 12 21 12 21z"/><circle cx="12" cy="9.5" r="2.4"/></svg>
      <span>여름 대피소</span>
    </button>
  </div>
</div>
  `;

  initWeatherApp();
}

function initWeatherApp(){
const els = {
    locationLabel: document.getElementById('locationLabel'),
    locationBtn: document.getElementById('locationBtn'),
    refreshBtn: document.getElementById('refreshBtn'),
    hero: document.getElementById('hero'),
    condText: document.getElementById('condText'),
    condMsg: document.getElementById('condMsg'),
    tempMain: document.getElementById('tempMain'),
    feelsText: document.getElementById('feelsText'),
    umbrellaBanner: document.getElementById('umbrellaBanner'),
    umbrellaText: document.getElementById('umbrellaText'),
    umbrellaSub: document.getElementById('umbrellaSub'),
    heatCarBanner: document.getElementById('heatCarBanner'),
    heatCarText: document.getElementById('heatCarText'),
    precipAmount: document.getElementById('precipAmount'),
    precipProb: document.getElementById('precipProb'),
    humidity: document.getElementById('humidity'),
    precipDesc: document.getElementById('precipDesc'),
    precipUpdated: document.getElementById('precipUpdated'),
    outfitEmoji: document.getElementById('outfitEmoji'),
    outfitTitle: document.getElementById('outfitTitle'),
    outfitDesc: document.getElementById('outfitDesc'),
    fallbackNotice: document.getElementById('fallbackNotice'),
    rainLayer: document.getElementById('rainLayer'),
    spotsContent: document.getElementById('spotsContent'),
    spotsTitle: document.getElementById('spotsTitle'),
    spotsSubtitle: document.getElementById('spotsSubtitle'),
    spotsRefreshChip: document.getElementById('spotsRefreshChip'),
    categoryChipRow: document.getElementById('categoryChipRow'),
  };

  let userCoords = null;
  let usedFallback = false;
  let lastWeather = null;
  let spotsLoadedFor = null;
  let staleNotice = false; // 서버가 예전 검색 결과를 대신 준 경우
  let spotCategory = 'rain'; // 'rain' | 'heat'

  const SEOUL = { lat:37.5665, lon:126.9780 };

  /* ---------------- Category chip switching ---------------- */
  els.categoryChipRow.querySelectorAll('.chip').forEach(chip=>{
    chip.addEventListener('click', ()=>{
      if(chip.dataset.cat === spotCategory) return;
      els.categoryChipRow.querySelectorAll('.chip').forEach(c=>c.classList.remove('active'));
      chip.classList.add('active');
      spotCategory = chip.dataset.cat;
      spotsLoadedFor = null;
      if(userCoords) loadSpots(true);
    });
  });

  /* ---------------- Tab navigation ---------------- */
  document.querySelectorAll('.tab').forEach(tab=>{
    tab.addEventListener('click', ()=>{
      document.querySelectorAll('.tab').forEach(t=>t.classList.remove('active'));
      tab.classList.add('active');
      document.querySelectorAll('.view').forEach(v=>v.classList.remove('active'));
      const target = tab.dataset.view === 'home' ? 'view-home' : 'view-spots';
      document.getElementById(target).classList.add('active');
      document.getElementById('screen').scrollTop = 0;
      if(tab.dataset.view === 'spots' && userCoords && spotsLoadedFor !== `${userCoords.lat},${userCoords.lon}`){
        loadSpots();
      }
    });
  });

  els.spotsRefreshChip.addEventListener('click', ()=>{
    if(userCoords) loadSpots(true);
  });

  els.refreshBtn.addEventListener('click', ()=>{
    els.refreshBtn.classList.add('spin');
    setTimeout(()=>els.refreshBtn.classList.remove('spin'), 500);
    init();
  });
  els.locationBtn.addEventListener('click', init);

  /* ---------------- Rain drops ambient ---------------- */
  function buildRainDrops(){
    els.rainLayer.innerHTML = '';
    for(let i=0;i<26;i++){
      const d = document.createElement('div');
      d.className='drop';
      d.style.left = (Math.random()*100)+'%';
      d.style.animationDuration = (0.5+Math.random()*0.5)+'s';
      d.style.animationDelay = (Math.random()*1)+'s';
      d.style.opacity = 0.3+Math.random()*0.5;
      els.rainLayer.appendChild(d);
    }
  }
  buildRainDrops();

  /* ---------------- Display helpers ---------------- */
  function precipLevel(mm){
    if(mm<=0) return {text:'비가 오지 않아요', level:0};
    if(mm<1) return {text:'약한 비가 내리고 있어요', level:1};
    if(mm<4) return {text:'보통 세기의 비가 내리고 있어요', level:2};
    if(mm<10) return {text:'강한 비가 내리고 있어요, 외출을 조심하세요', level:3};
    return {text:'매우 강한 비가 내리고 있어요, 가능하면 외출을 피해주세요', level:4};
  }

  function outfitFor(feels){
    if(feels>=28) return {emoji:'👕', title:'민소매 · 반바지', desc:'가볍고 통풍 잘 되는 옷차림이 좋아요. 자외선 차단제도 챙기세요.'};
    if(feels>=23) return {emoji:'👚', title:'반팔 · 얇은 셔츠', desc:'반팔에 얇은 셔츠나 원피스면 충분히 시원해요.'};
    if(feels>=20) return {emoji:'🧥', title:'얇은 가디건 · 긴바지', desc:'낮과 밤의 온도차가 있으니 얇은 겉옷을 챙기세요.'};
    if(feels>=17) return {emoji:'🧢', title:'가디건 · 자켓', desc:'아침저녁으로 쌀쌀할 수 있어요, 가벼운 자켓을 추천해요.'};
    if(feels>=12) return {emoji:'🧣', title:'자켓 · 니트', desc:'니트에 자켓을 더하면 딱 좋은 날씨예요.'};
    if(feels>=9) return {emoji:'🧤', title:'코트 · 니트', desc:'코트와 니트로 보온에 신경 써주세요.'};
    if(feels>=5) return {emoji:'🧥', title:'두꺼운 코트 · 목도리', desc:'히트텍과 목도리로 든든하게 챙겨 입으세요.'};
    return {emoji:'🧣', title:'패딩 · 목도리 · 장갑', desc:'매우 추운 날씨예요, 패딩과 장갑까지 완전 무장하세요.'};
  }

  /* ---------------- Mascot expression ---------------- */
  function setMascot(stateKey){
    const eyes = document.querySelectorAll('.face-eye');
    const mouth = document.querySelector('.face-mouth');
    if(stateKey==='clear'){
      eyes.forEach(e=>e.setAttribute('ry','3.6'));
      mouth.setAttribute('d','M46 68 Q55 78 64 68');
    } else if(stateKey==='rain' || stateKey==='thunder'){
      eyes.forEach(e=>e.setAttribute('ry','2'));
      mouth.setAttribute('d','M46 72 Q55 66 64 72');
    } else if(stateKey==='snow' || stateKey==='fog'){
      eyes.forEach(e=>e.setAttribute('ry','1.4'));
      mouth.setAttribute('d','M47 70 Q55 73 63 70');
    } else {
      eyes.forEach(e=>e.setAttribute('ry','3.6'));
      mouth.setAttribute('d','M47 70 Q55 74 63 70');
    }
  }

  /* ---------------- Render weather ---------------- */
  // 판단(비 시작 시각·옷차림·차 안 물건)은 서버가 하고, 화면은 그 결과를 그린다
  const EMOJI = {clear:'☀️', cloudy:'⛅', fog:'🌫️', rain:'🌧️', snow:'❄️', thunder:'⛈️'};

  function renderWeather(data){
    lastWeather = data;
    const key = data.condition.key;

    els.hero.className = 'hero state-' + key;
    setMascot(key);

    els.condText.textContent = `${data.place} · ${data.condition.text} ${EMOJI[key] ?? '☁️'}`;
    const feels = data.feelsLike;
    const actual = data.temperature;
    const diff = feels - actual;
    els.condMsg.textContent = data.message;

    els.tempMain.textContent = actual;
    if(diff===0){
      els.feelsText.innerHTML = `체감온도도 <b>${feels}°</b>로 비슷해요`;
    } else if(diff>0){
      els.feelsText.innerHTML = `체감온도 <b>${feels}°</b> · 실제보다 ${diff}도 더 따뜻하게 느껴져요`;
    } else {
      els.feelsText.innerHTML = `체감온도 <b>${feels}°</b> · 실제보다 ${Math.abs(diff)}도 더 춥게 느껴져요`;
    }

    // precipitation
    const precipMm = data.precipitationMm ?? 0;
    els.precipAmount.textContent = precipMm.toFixed(1);
    els.precipProb.textContent = data.precipitationProbability + '%';
    els.humidity.textContent = data.humidity + '%';
    els.precipDesc.textContent = precipLevel(precipMm).text;
    els.precipUpdated.textContent = data.observedAt.slice(11, 16) + ' 기준';

    // outfit: 이모지만 화면이 고르고, 문구는 서버 판단을 쓴다
    els.outfitEmoji.textContent = outfitFor(feels).emoji;
    els.outfitTitle.textContent = data.outfit.title;
    els.outfitDesc.textContent = data.outfit.description;

    // umbrella banner: "오후 3시부터 비 소식"처럼 언제 챙겨야 하는지까지
    if(data.umbrella.needed){
      els.umbrellaBanner.style.display = 'flex';
      els.umbrellaText.textContent = data.umbrella.message;
      els.umbrellaSub.textContent = '외출 전 우산을 미리 준비하면 좋아요';
      tryNotify(data.umbrella.message);
    } else {
      els.umbrellaBanner.style.display = 'none';
    }

    // heat car reminder banner (actual temp 30도 이상)
    if(data.heatCarAlert){
      els.heatCarBanner.style.display = 'flex';
      els.heatCarText.textContent = `실외 기온이 ${actual}도예요`;
      tryNotify('실외 기온이 높아요, 차 안에 보조배터리나 캔음료를 두고 내리지 않았는지 확인하세요');
    } else {
      els.heatCarBanner.style.display = 'none';
    }
  }

  function tryNotify(text){
    if(!('Notification' in window)) return;
    if(Notification.permission === 'granted'){
      new Notification('🌂 우산챙겨!', {body:text});
    } else if(Notification.permission !== 'denied'){
      Notification.requestPermission().then(p=>{
        if(p==='granted') new Notification('🌂 우산챙겨!', {body:text});
      });
    }
  }

  /* ---------------- Server API ---------------- */
  // 서버가 이미 8초 상한으로 외부 API를 기다리므로, 화면은 그보다 조금 더(10초)만 기다린다
  async function getJson(path, params){
    const query = new URLSearchParams(params).toString();
    const res = await fetch(`${API_BASE}${path}?${query}`, {signal: AbortSignal.timeout(10000)});
    if(!res.ok) throw new Error(`${path} ${res.status}`);
    return res.json();
  }

  /* ---------------- Init / geolocation ---------------- */
  async function init(){
    els.locationLabel.textContent = '위치 확인 중';
    els.condText.textContent = '날씨 확인 중이에요';
    els.condMsg.textContent = '잠시만 기다려주세요';

    if(!navigator.geolocation){
      useFallback();
      return;
    }
    navigator.geolocation.getCurrentPosition(
      pos=>{
        usedFallback = false;
        els.fallbackNotice.style.display = 'none';
        userCoords = {lat:pos.coords.latitude, lon:pos.coords.longitude};
        loadAll(userCoords);
      },
      err=>{
        useFallback();
      },
      {timeout:8000, maximumAge:300000}
    );
  }

  function useFallback(){
    usedFallback = true;
    userCoords = SEOUL;
    els.fallbackNotice.style.display = 'flex';
    loadAll(userCoords);
  }

  async function loadAll(coords){
    try{
      const weather = await getJson('/api/weather', {lat: coords.lat, lon: coords.lon});
      els.locationLabel.textContent = weather.place;
      renderWeather(weather);
    }catch(e){
      els.locationLabel.textContent = '위치 정보 없음';
      els.condText.textContent = '날씨를 불러오지 못했어요';
      els.condMsg.textContent = '새로고침 버튼을 눌러 다시 시도해주세요';
    }
  }

  /* ---------------- Spots (rain-shelter cafes/restaurants) ---------------- */
  function haversine(lat1,lon1,lat2,lon2){
    const R=6371000;
    const toRad = d=>d*Math.PI/180;
    const dLat = toRad(lat2-lat1), dLon = toRad(lon2-lon1);
    const a = Math.sin(dLat/2)**2 + Math.cos(toRad(lat1))*Math.cos(toRad(lat2))*Math.sin(dLon/2)**2;
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  }
  function walkMinutes(meters){ return Math.max(1, Math.round(meters/67)); }

  function sponsoredDemoCard(coords){
    // Demo-only sponsored placement (paid placement in production would come from a partner database)
    const offsetLat = coords.lat + (Math.random()-0.5)*0.004;
    const offsetLon = coords.lon + (Math.random()-0.5)*0.004;
    const dist = Math.round(haversine(coords.lat, coords.lon, offsetLat, offsetLon));
    return {
      name:'카페 무드인',
      category:'카페',
      dist, sponsored:true, emoji:'☕'
    };
  }

  async function loadSpots(force){
    if(!userCoords) return;
    const key = `${userCoords.lat},${userCoords.lon},${spotCategory}`;
    if(spotsLoadedFor === key && !force) return;

    els.spotsContent.innerHTML = `
      <div class="state-box">
        <div class="spinner"></div>
        <div class="state-title">근처 스팟을 찾고 있어요</div>
        <div class="state-desc">위치 정보를 기반으로 검색 중이에요</div>
      </div>`;

    const st = lastWeather?.condition?.key;
    const actualTemp = lastWeather ? lastWeather.temperature : null;

    if(spotCategory === 'heat'){
      els.spotsTitle.textContent = '🧊 무더위쉼터';
      if(actualTemp!==null && actualTemp>=30){
        els.spotsSubtitle.textContent = `지금 ${actualTemp}도예요, 근처 무더위쉼터에서 더위를 식히세요.`;
      } else {
        els.spotsSubtitle.textContent = '한여름 무더위를 피할 수 있는 도서관, 주민센터를 찾아드려요.';
      }
    } else {
      els.spotsTitle.textContent = '☔ 비 피하기 스팟';
      if(st==='rain' || st==='thunder'){
        els.spotsSubtitle.textContent = '지금 비가 오고 있어요, 가까운 곳으로 피하세요.';
      } else {
        els.spotsSubtitle.textContent = '장마철 갑자기 비가 쏟아져도 당황하지 마세요. 근처 카페와 음식점을 찾아드려요.';
      }
    }

    try{
      // 미러 동시 요청·캐시·장애 시 예전 결과는 서버가 맡는다. '다시 찾기'만 refresh로 새로 검색
      const data = await getJson('/api/spots', {
        lat: userCoords.lat, lon: userCoords.lon, category: spotCategory, refresh: !!force
      });

      const EMOJI_BY_CATEGORY = {'카페':'☕', '음식점':'🍽️', '도서관':'📚', '주민센터':'🏛️'};
      const spots = data.spots.map(s=>({
        name: s.name,
        category: s.category,
        emoji: EMOJI_BY_CATEGORY[s.category] ?? '🧊',
        dist: s.distanceMeters,
        sponsored: false
      }));
      staleNotice = data.stale;

      spotsLoadedFor = key;

      if(spotCategory === 'rain'){
        const sponsored = sponsoredDemoCard(userCoords);
        const all = [sponsored, ...spots];
        if(spots.length===0){
          renderSpots([sponsored], true);
        } else {
          renderSpots(all, false);
        }
      } else {
        if(spots.length===0){
          renderSpots([], true);
        } else {
          renderSpots(spots, false);
        }
      }
    }catch(e){
      els.spotsContent.innerHTML = `
        <div class="state-box">
          <div class="state-emoji">📡</div>
          <div class="state-title">스팟을 불러오지 못했어요</div>
          <div class="state-desc">장소 검색이 잠시 붐비고 있어요. 조금 뒤에 다시 시도해주세요</div>
          <div class="state-btn" id="spotsRetry">다시 시도</div>
        </div>`;
      document.getElementById('spotsRetry')?.addEventListener('click', ()=>loadSpots(true));
    }
  }

  function renderSpots(list, emptyReal){
    const cards = list.map(s=>`
      <div class="spot-card ${s.sponsored?'sponsored':''}">
        <div class="spot-thumb">${s.emoji}</div>
        <div class="spot-body">
          <div class="spot-name-row">
            <div class="spot-name">${escapeHtml(s.name)}</div>
            ${s.sponsored?'<span class="badge">제휴</span>':''}
          </div>
          <div class="spot-meta">
            <span>${s.category}</span>
            <span class="dot"></span>
            <span>도보 ${walkMinutes(s.dist)}분 · ${s.dist}m</span>
          </div>
        </div>
      </div>
    `).join('');

    const emptyMsg = emptyReal ? `
      <div class="state-box" style="padding:30px 20px;">
        <div class="state-emoji">🔍</div>
        <div class="state-title">근처에 검색 결과가 없어요</div>
        <div class="state-desc">검색 반경을 넓히거나 다른 위치에서 다시 찾아보세요</div>
      </div>` : '';

    // 검색 서버가 모두 실패해서 전에 찾아 둔 결과를 보여 줄 때는 그 사실을 알린다
    const staleMsg = staleNotice
      ? `<div class="notice-banner" style="display:flex; margin:0 20px 12px;">🕒 지금은 검색이 원활하지 않아 조금 전에 찾은 결과를 보여드려요</div>`
      : '';
    els.spotsContent.innerHTML = `${staleMsg}<div class="spot-list">${cards}</div>${emptyMsg}`;
  }

  function escapeHtml(str){
    const d = document.createElement('div');
    d.textContent = str;
    return d.innerHTML;
  }

  init();
}

function render() {
  renderHome();
}

render();
