import "./App.css";

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
  const spotsCache = {}; // 위치+카테고리별 검색 결과 캐시
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

  /* ---------------- Weather code mapping ---------------- */
  function weatherState(code){
    if(code===0) return {key:'clear', text:'맑음', emoji:'☀️'};
    if([1,2,3].includes(code)) return {key:'cloudy', text:'구름 많음', emoji:'⛅'};
    if([45,48].includes(code)) return {key:'fog', text:'안개', emoji:'🌫️'};
    if([51,53,55,56,57,61,63,65,66,67,80,81,82].includes(code)) return {key:'rain', text:'비', emoji:'🌧️'};
    if([71,73,75,77,85,86].includes(code)) return {key:'snow', text:'눈', emoji:'❄️'};
    if([95,96,99].includes(code)) return {key:'thunder', text:'뇌우', emoji:'⛈️'};
    return {key:'cloudy', text:'흐림', emoji:'☁️'};
  }

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
  function renderWeather(data, place){
    const cur = data.current;
    const st = weatherState(cur.weather_code);
    lastWeather = {st, cur};

    els.hero.className = 'hero state-' + st.key;
    setMascot(st.key);

    els.condText.textContent = `${place} · ${st.text} ${st.emoji}`;
    const feels = Math.round(cur.apparent_temperature);
    const actual = Math.round(cur.temperature_2m);
    const diff = feels - actual;
    let msg = '오늘 하루도 좋은 하루 보내세요';
    if(st.key==='rain') msg = '우산을 챙기고 물웅덩이 조심하세요';
    else if(st.key==='thunder') msg = '천둥번개가 있어요, 외출을 주의하세요';
    else if(st.key==='snow') msg = '눈길 미끄럼에 주의하세요';
    else if(st.key==='clear' && actual>=28) msg = '더위에 수분 보충 잊지 마세요';
    else if(st.key==='clear' && actual<=5) msg = '건조하고 쌀쌀하니 보습 챙기세요';
    els.condMsg.textContent = msg;

    els.tempMain.textContent = actual;
    if(diff===0){
      els.feelsText.innerHTML = `체감온도도 <b>${feels}°</b>로 비슷해요`;
    } else if(diff>0){
      els.feelsText.innerHTML = `체감온도 <b>${feels}°</b> · 실제보다 ${diff}도 더 따뜻하게 느껴져요`;
    } else {
      els.feelsText.innerHTML = `체감온도 <b>${feels}°</b> · 실제보다 ${Math.abs(diff)}도 더 춥게 느껴져요`;
    }

    // precipitation
    const precipMm = cur.precipitation ?? 0;
    const prob = data.currentProb ?? 0;
    els.precipAmount.textContent = precipMm.toFixed(1);
    els.precipProb.textContent = prob + '%';
    els.humidity.textContent = Math.round(cur.relative_humidity_2m) + '%';
    const lvl = precipLevel(precipMm);
    els.precipDesc.textContent = lvl.text;
    els.precipUpdated.textContent = new Date(cur.time).toLocaleTimeString('ko-KR',{hour:'2-digit', minute:'2-digit'}) + ' 기준';

    // outfit
    const outfit = outfitFor(feels);
    els.outfitEmoji.textContent = outfit.emoji;
    els.outfitTitle.textContent = outfit.title;
    els.outfitDesc.textContent = outfit.desc;

    // umbrella banner
    const willRain = st.key==='rain' || st.key==='thunder' || prob>=50;
    if(willRain){
      els.umbrellaBanner.style.display = 'flex';
      if(precipMm>0){
        els.umbrellaText.textContent = '지금 비가 오고 있어요, 우산을 챙기세요';
      } else {
        els.umbrellaText.textContent = `오늘 강수확률 ${prob}%, 우산을 챙기세요`;
      }
      els.umbrellaSub.textContent = '외출 전 우산을 미리 준비하면 좋아요';
      tryNotify(els.umbrellaText.textContent);
    } else {
      els.umbrellaBanner.style.display = 'none';
    }

    // heat car reminder banner (actual temp 30도 이상)
    if(actual >= 30){
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

  /* ---------------- Fetch weather ---------------- */
  async function fetchWeather(lat, lon){
    const url = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,apparent_temperature,precipitation,rain,weather_code,relative_humidity_2m,wind_speed_10m&hourly=precipitation_probability&timezone=auto`;
    const res = await fetch(url);
    if(!res.ok) throw new Error('weather fetch failed');
    const data = await res.json();
    // find matching hourly index for current time to get probability
    let prob = 0;
    try{
      const idx = data.hourly.time.indexOf(data.current.time.slice(0,14)+'00'.slice(0, data.current.time.length>13?0:0));
      // fallback: find closest hour
      const curDate = new Date(data.current.time);
      let bestIdx = 0, bestDiff = Infinity;
      data.hourly.time.forEach((t,i)=>{
        const diff = Math.abs(new Date(t) - curDate);
        if(diff < bestDiff){ bestDiff = diff; bestIdx = i; }
      });
      prob = data.hourly.precipitation_probability[bestIdx] ?? 0;
    }catch(e){ prob = 0; }
    data.currentProb = prob;
    return data;
  }

  async function reverseGeocode(lat, lon){
    try{
      const res = await fetch(`https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${lat}&longitude=${lon}&localityLanguage=ko`);
      const data = await res.json();
      const city = data.city || data.locality || '';
      const district = data.localityInfo?.administrative?.find(a=>a.adminLevel===8)?.name || data.principalSubdivision || '';
      const label = [district, city].filter(Boolean).filter((v,i,a)=>a.indexOf(v)===i).join(' ');
      return label || '현재 위치';
    }catch(e){
      return '현재 위치';
    }
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
      const [place, weather] = await Promise.all([
        reverseGeocode(coords.lat, coords.lon),
        fetchWeather(coords.lat, coords.lon)
      ]);
      els.locationLabel.textContent = place;
      renderWeather(weather, place);
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

    const st = lastWeather?.st?.key;
    const actualTemp = lastWeather?.cur ? Math.round(lastWeather.cur.temperature_2m) : null;

    let query;
    if(spotCategory === 'heat'){
      els.spotsTitle.textContent = '🧊 무더위쉼터';
      if(actualTemp!==null && actualTemp>=30){
        els.spotsSubtitle.textContent = `지금 ${actualTemp}도예요, 근처 무더위쉼터에서 더위를 식히세요.`;
      } else {
        els.spotsSubtitle.textContent = '한여름 무더위를 피할 수 있는 도서관, 주민센터를 찾아드려요.';
      }
      query = `[out:json][timeout:8];(node["amenity"="community_centre"](around:1500,${userCoords.lat},${userCoords.lon});node["amenity"="library"](around:1500,${userCoords.lat},${userCoords.lon});node["amenity"="townhall"](around:1500,${userCoords.lat},${userCoords.lon}););out body 24;`;
    } else {
      els.spotsTitle.textContent = '☔ 비 피하기 스팟';
      if(st==='rain' || st==='thunder'){
        els.spotsSubtitle.textContent = '지금 비가 오고 있어요, 가까운 곳으로 피하세요.';
      } else {
        els.spotsSubtitle.textContent = '장마철 갑자기 비가 쏟아져도 당황하지 마세요. 근처 카페와 음식점을 찾아드려요.';
      }
      query = `[out:json][timeout:8];(node["amenity"="cafe"](around:1000,${userCoords.lat},${userCoords.lon});node["amenity"="restaurant"](around:1000,${userCoords.lat},${userCoords.lon}););out body 24;`;
    }

    try{
      // 캐시된 결과가 있으면 즉시 표시 (같은 위치+카테고리 재방문 시 대기 없음, '다시 찾기'는 새로 검색)
      let data = force ? null : spotsCache[key];

      if(!data){
        // 서버 3곳에 "동시에" 요청을 보내고, 가장 먼저 성공한 응답을 사용
        // (순차 시도는 앞 서버가 느리면 그만큼 기다려야 해서 동시 요청으로 변경)
        const endpoints = [
          'https://overpass-api.de/api/interpreter',
          'https://overpass.kumi.systems/api/interpreter',
          'https://overpass.private.coffee/api/interpreter'
        ];
        const controllers = endpoints.map(()=>new AbortController());
        const attempts = endpoints.map((ep, i)=>
          fetch(ep, {
            method:'POST',
            body:'data=' + encodeURIComponent(query),
            signal: controllers[i].signal
          }).then(res=>{
            if(!res.ok) throw new Error('overpass ' + res.status);
            return res.json();
          })
        );
        // 전체 8초 제한: 어떤 서버도 8초 안에 응답 못 하면 실패 처리
        const timeout = new Promise((_, rej)=>setTimeout(()=>rej(new Error('timeout')), 8000));
        try{
          data = await Promise.race([Promise.any(attempts), timeout]);
        } finally {
          controllers.forEach(c=>c.abort()); // 남은 요청은 모두 취소
        }
        spotsCache[key] = data;
      }

      const emojiFor = (amenity)=>{
        if(amenity==='cafe') return '☕';
        if(amenity==='restaurant') return '🍽️';
        if(amenity==='library') return '📚';
        if(amenity==='townhall') return '🏛️';
        return '🧊';
      };
      const categoryFor = (amenity)=>{
        if(amenity==='cafe') return '카페';
        if(amenity==='restaurant') return '음식점';
        if(amenity==='library') return '도서관';
        if(amenity==='townhall') return '주민센터';
        return '경로당';
      };

      let spots = (data.elements || [])
        .filter(el=>el.tags && el.tags.name)
        .map(el=>{
          const dist = Math.round(haversine(userCoords.lat, userCoords.lon, el.lat, el.lon));
          return {
            name: el.tags.name,
            category: categoryFor(el.tags.amenity),
            emoji: emojiFor(el.tags.amenity),
            dist, sponsored:false
          };
        })
        .sort((a,b)=>a.dist-b.dist)
        .slice(0,15);

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
          <div class="state-desc">네트워크 상태를 확인하고 다시 시도해주세요</div>
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

    els.spotsContent.innerHTML = `<div class="spot-list">${cards}</div>${emptyMsg}`;
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
