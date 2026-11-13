# PLEOMEKABOT (플레오메카봇)

> **모든 현대·기아 차량의 주행이 곧 전력(戰力)이 되고, 정차 시 20초 배틀과 현대·기아 전용 충전소(E-pit 등) 보너스 배틀권으로 확장되는 Pleos Connect 기반 차량 연동 메카닉 배틀 RPG**

---

## 1. 프로젝트 개요

**PLEOMEKABOT**은 내연기관·하이브리드·전기차 등 **현대자동차그룹 전 차종**의 실시간 주행·센서·컨디션 데이터를 **Pleos Vehicle SDK (`ai.pleos.playground:Vehicle:2.3.2`)**로 수신하여, 주행 중에는 안전하게 전력을 비축하고 **정차(Gear `P`)** 순간에 5대 원소 메카봇을 소환·배틀·진화시키는 **SDV(Software-Defined Vehicle) 네이티브 게이미피케이션 앱**입니다.

### 핵심 가치
- **안전 제일(Safety First)**: `Gear D/R/N` 주행 중에는 전투 화면이 자동 잠금(Safety Lock) 처리되고, 주행 에너지가 메카봇 스탯(SP/HP/공·방)으로 변환·비축됩니다.
- **실제 주행 루프 기반 쿨타임 (전 차종 공통)**: 1회 배틀 완료 후에는 다시 **`Gear D`로 실제 주행**을 해야 배틀 쿨타임이 해제되어, 주행과 게임이 자연스럽게 순환합니다.
- **현대·기아 전용 충전소(E-pit 등) 보너스 배틀권 (EV 특화 보너스)**: 전기차 전용 서비스가 아닌 전 차종 대상 서비스이며, 전기차(EV) 유저가 **현대·기아 전용 충전소(E-pit 등)**에서 충전할 때에만 **보너스 개념으로 추가 배틀권(쿨타임 즉시 초기화 & 보너스 레이저/특수 파츠)**을 제공합니다.
- **20초 콤팩트 타임라인 배틀**: 정차 중 신호 대기 또는 충전 대기 시간 안에 완결되도록 설계된 20초 집중 전투(타이밍 바 공격 + 방어 쉴드 + 원소 상성)를 제공합니다.

---

## 2. 시스템 아키텍처

```text
[Pleos Vehicle / Emulator]
   ├─ GearManager (Gear P/D/R/N)
   ├─ DrivingManager (Speed km/h, Distance)
   └─ EnergyManager (SOC %, Charging Power kW)
          │
          ▼  (Kotlin Coroutines StateFlow / ai.pleos.playground:Vehicle:2.3.2)
[Android Native Host - PleosVehicleBridge]
   ├─ Safety & Cooldown Controller (Gear P Unlock / Gear D Cooldown Reset)
   └─ @JavascriptInterface (window.PleosBridge)
          │
          ▼  (WebView 60fps Canvas & Custom SVG Mecha Renderer)
[PLEOMEKABOT Web Engine (demo.html / index.html)]
   ├─ 5대 원소 상성 엔진 (화 → 금 → 목 → 토 → 수 → 화)
   ├─ 4대 핵심 스탯 (POW · DEF · SPD · TEC)
   ├─ 20초 타임라인 & 타이밍 바(Critical / Guard) 물리 엔진
   └─ E-pit Overclock & 도감/성장 시스템
```

자세한 아키텍처 다이어그램, End-to-End 프로세스 및 사업성 요약은 루트 경로의 **`PLEOMEKABOT_아키텍처_프로세스_요약.html`**에서 확인할 수 있습니다.

---

## 3. 주요 기능 및 메커니즘

1. **차량 상태 기반 안전 잠금 & D-기어 쿨타임 루프**
   - `Gear P` 상태에서만 배틀 및 튜닝 진입 허용 (`Gear D/R/N` 전환 시 즉시 안전 잠금 화면 전환)
   - 1회 전투 종료 후 **`Gear D` 주행(누적 주행)**을 수행해야 다음 전투 쿨타임이 초기화됨 (전기차의 경우 현대·기아 전용 충전소 충전 시 보너스 배틀권 지급)
2. **5대 원소(오행) 순환 상성 & 4대 스탯**
   - **원소 상성**: 화(Pyro) → 금(Metal) → 목(Aero) → 토(Terra) → 수(Hydro) → 화(Pyro) (상성 우위 1.25x / 열위 0.80x)
   - **4대 스탯**: `POW(화력)` · `DEF(장갑)` · `SPD(기동)` · `TEC(정밀)`
3. **커스텀 SVG 메카봇 전투 & 타격 이펙트**
   - 기체별 고유 SVG 메카 디자인(아군기/보스기), 빔·레이저 발사, 피격 셰이크 및 HP 게이지 연동
4. **듀얼 실행 환경**
   - **Pleos Connect Emulator / Android APK**: `PleosVehicleBridge`를 통해 실제 Pleos Vehicle SDK 데이터 연동
   - **Standalone Web Simulator (`demo.html`)**: 브라우저에서 차량 데이터(Gear, Speed, SOC, E-pit 충전 kW)를 실시간 시뮬레이션 가능

---

## 4. 프로젝트 구조

```text
.
├── android/                                # Android Studio / Pleos Vehicle SDK v2.3.2 네이티브 프로젝트
│   └── app/src/main/
│       ├── java/.../PleosVehicleBridge.kt  # Pleos SDK 연동 & Safety/Cooldown 브릿지
│       └── assets/index.html               # WebView 탑재 게임 엔진
├── web-engine/                             # 웹 엔진 모듈 소스
├── mock-server/                            # Mock 텔레메트리 시뮬레이터 서버
├── demo.html                               # 단독 실행 가능한 웹 시뮬레이터
├── PLEOMEKABOT_아키텍처_프로세스_요약.html    # 아키텍처 · 프로세스 · 기획 요약 HTML 문서
├── PLEOMEKABOT-debug.apk                   # Pleos Connect Emulator 설치용 빌드 APK
└── README.md
```

---

## 5. 실행 및 빌드 방법

### A. 웹 시뮬레이터로 즉시 체험하기
별도 빌드 없이 `demo.html` 또는 `PLEOMEKABOT_아키텍처_프로세스_요약.html`을 웹 브라우저(Chrome/Edge)로 열면 즉시 동작을 확인할 수 있습니다.
- 상단/좌측 **차량 상태 제어 패널**에서 `Gear P / D`, `속도(km/h)`, `배터리(SOC %)`, `E-pit 급속충전(kW)`을 조절하며 안전 잠금, 배틀, 쿨타임 해제를 테스트할 수 있습니다.

### B. Android Studio & Pleos Connect Emulator 빌드
1. `android/` 디렉터리를 Android Studio에서 엽니다.
2. Pleos Playground Repository 및 `ai.pleos.playground:Vehicle:2.3.2` 의존성이 포함된 상태에서 Gradle Sync를 진행합니다.
3. Pleos Connect Emulator를 실행한 뒤 `./gradlew app:installDebug` (또는 루트의 `PLEOMEKABOT-debug.apk`를 `adb install`로 설치)하여 실행합니다.
