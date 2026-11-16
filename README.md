# 🌂 우산챙겨!

> 비 알림부터 무더위쉼터까지, 오늘 날씨에 딱 맞는 준비를 도와주는 토스 미니앱

토스 앱인토스 **7월 바이브코딩 챌린지 "미니앱과 함께하는 여름"** 출품작에서 시작해, 백엔드 서버를 붙여 고도화하고 있습니다. 버전별 변경 내용은 [CHANGELOG](CHANGELOG.md)와 [Releases](https://github.com/thal070909/umbrella-app/releases)에 있습니다.

## 주요 기능

**🏠 홈**
- 현재 위치 기반 실시간 날씨 · 온도 · 체감온도
- **"오후 3시부터 비 소식이 있어요"**: 앞으로 12시간 안에 처음 비가 오는 시각을 알려 줘서 우산을 챙길지 바로 정할 수 있습니다
- 체감온도 8단계 옷차림 추천, 기온 30도 이상이면 차 안 물건(보조배터리 · 캔음료) 주의 배너
- 날씨에 따라 표정이 바뀌는 구름 마스코트

**☔ 비 피하기 스팟**
- 반경 1km 카페 · 음식점을 도보 시간과 도로명 주소로 안내 (카카오 로컬)

**🧊 무더위쉼터**
- 행정안전부 지정 무더위쉼터를 찾고, **지금 운영 중인 곳을 먼저** 보여 줍니다 (평일 운영시간 · 주말 개방 · 냉방기 표시)

## 구조

```
토스 앱(웹뷰) ──▶ 우산챙겨 서버 (Spring Boot) ──┬─▶ Open-Meteo (날씨)
                                                  ├─▶ BigDataCloud (동네 이름)
                                                  ├─▶ 카카오 로컬 (카페·음식점)
                                                  ├─▶ 재난안전데이터공유플랫폼 (무더위쉼터, 하루 1번 전량)
                                                  └─▶ OpenStreetMap Overpass (키가 없거나 위가 실패할 때)
```

앱은 화면만 그리고, 판단(비 시작 시각 · 옷차림 · 운영 중 여부)과 외부 API 장애 대응은 서버가 맡습니다.

## 사용자 입장에서 정한 것

| 불편했던 순간 | 정한 것 |
|---|---|
| 강수확률 숫자로는 우산을 챙길지 판단하기 어렵다 | 12시간 안의 첫 비 소식 시각을 "오후 3시부터"로 알려 준다 |
| 공개 검색 서버가 느리면 스피너가 최악 36초 돈다 | 미러 3곳 동시 요청 후 가장 먼저 성공한 응답, 전체 8초 상한 |
| 검색 서버가 모두 실패하면 빈 화면이다 | 24시간 안에 받아 둔 결과를 "조금 전에 찾은 결과"라고 알리고 보여 준다 |
| 가까운 쉼터가 문을 닫았을 수 있다 | 운영 중 → 시간 모름 → 문 닫음 순으로 정렬하고 배지로 표시 |
| 서버 문제인데 "네트워크를 확인하세요"라고 하면 사용자를 의심하게 한다 | "장소 검색이 잠시 붐비고 있어요"로 안내 |
| 위치 권한을 거부하면 앱이 멈춘다 | 서울 기준으로 바로 보여 주고 기준 위치를 배너로 알린다 |

## API

| 요청 | 설명 |
|---|---|
| `GET /api/weather?lat=&lon=` | 날씨, 동네 이름, 우산(`umbrella.message`, `rainStartsAt`), 옷차림, 차 안 물건 주의 |
| `GET /api/spots?lat=&lon=&category=rain\|heat&refresh=` | 근처 장소. `source`(kakao / public-data / openstreetmap), `stale`, 장소마다 `detail`, `openNow` |
| `GET /actuator/health` | 서버 상태 |

오류는 `{"code", "message"}` 형식입니다. `INVALID_INPUT`(400), `TOO_MANY_REQUESTS`(429), `WEATHER_UNAVAILABLE` · `SPOTS_UNAVAILABLE`(503)

## 실행

```bash
# 서버 (localhost:8080)
cd server
./gradlew bootRun

# 앱 (localhost:5173). 서버 주소는 .env.example 참고
npm install
npm run dev
```

Docker로 서버 실행:

```bash
docker build -t umbrella-server server
docker run -p 8080:8080 -v umbrella-data:/app/data \
  -e KAKAO_REST_API_KEY=... -e SAFETYDATA_SERVICE_KEY=... umbrella-server
```

## 서버 환경 변수

| 이름 | 설명 | 없으면 |
|---|---|---|
| `KAKAO_REST_API_KEY` | 카카오 디벨로퍼스 REST API 키 | 비 피하기도 OpenStreetMap으로 찾음 |
| `SAFETYDATA_SERVICE_KEY` | 재난안전데이터공유플랫폼 무더위쉼터 인증키 | 무더위쉼터도 OpenStreetMap으로 찾음 |
| `UMBRELLA_ALLOWED_ORIGINS` | 앱을 띄우는 주소(쉼표로 여러 개) | `http://localhost:5173` |
| `UMBRELLA_RATE_LIMIT_PER_MINUTE` | 한 사람이 1분에 보낼 수 있는 요청 수 | 60 |
| `SHELTER_SNAPSHOT_PATH` | 무더위쉼터 저장 파일 위치 | `./data/shelters.json` |
| `PORT` | 서버 포트 | 8080 |

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| 앱 | Vite + Vanilla JavaScript, 앱인토스 WebView SDK |
| 서버 | Java 21, Spring Boot 4, Caffeine 캐시, 가상 스레드 |
| 데이터 | Open-Meteo, BigDataCloud, 카카오 로컬, 재난안전데이터공유플랫폼, OpenStreetMap Overpass |
| 배포 | Docker, GitHub Actions (PR 테스트·이미지 빌드, 태그 시 Release 자동 생성) |

## 프로젝트 구조

```
umbrella-app/
├── src/                 # 앱 화면 (날씨·스팟 렌더링)
├── server/              # 우산챙겨 서버
│   └── src/main/java/com/umbrella/
│       ├── weather/     # 날씨 판단 (비 시작 시각, 옷차림)
│       ├── spot/        # 비 피하기·무더위쉼터 검색, 미러 동시 요청
│       ├── shelter/     # 무더위쉼터 동기화·위치 색인·운영시간
│       └── common/      # 요청 제한, 오류 응답
├── CHANGELOG.md
└── .github/workflows/   # ci.yml, release.yml
```
