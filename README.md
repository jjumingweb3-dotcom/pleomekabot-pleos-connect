# PLEOMEKABOT (플레오메카봇)

> **주행(`Gear D`)으로 차량 센서를 통해 속성을 얻고, 정차(`Gear P`)하면 근처 AI 봇이나 다른 운전자가 배치해둔 봇과 20초 결투해 경험치를 얻는 Pleos Connect 기반 차량 연동 메카닉 배틀 RPG**

---

## 1. 프로젝트 개요

**PLEOMEKABOT**은 내연기관·하이브리드·전기차(EV) 등 **현대자동차그룹 전 차종**을 대상으로, **Pleos Vehicle SDK (`ai.pleos.playground:Vehicle:2.3.2`)**를 통해 주행 중 차량 센서 데이터를 수신하고 정차 순간에 20초 메카닉 배틀을 즐기는 **차량 특화 게이미피케이션 앱**입니다.

### 메인 루프 (Core Loop)
1. **주행 (`Gear D`)**: 주행 중에는 안전을 위해 조작 화면이 잠금(Safety Lock) 처리되며, 온도·와이퍼·크루즈 센서를 통해 **5대 원소 속성**이 충전되고 이전 배틀의 **쿨타임이 해제**됩니다.
2. **정차 (`Gear P`)**: 목적지에 정차하면 내 차종 실루엣의 메카봇이 깨어나고, 근처 반경 내 **AI 봇 또는 다른 운전자가 배치해둔 봇**과 20초 타이밍 배틀을 펼쳐 **경험치(EXP)와 스탯 포인트**를 획득합니다.

### 부가 기능 (Sub Features)
- **내 봇 주차 위치 배치**: 정차한 위치에 내 봇을 배치해 두고, 다른 운전자가 도전할 때 추가 경험치를 획득합니다.
- **EV 전용 충전소 보너스 & 협동 레이드**: 전기차(EV)의 경우 **현대자동차그룹이 운영하는 초고속 충전소(E-pit 등)**에서 충전할 때에만 보너스 개념으로 추가 배틀권과 협동 레이드 참여 기회를 제공합니다.
- **차량 컨디션 연동 버프**: 타이어 공기압 등 차량 상태가 정상일 때 보호막(`+10% HP`) 버프가 적용됩니다.

---

## 2. 시스템 아키텍처

```text
[Pleos Vehicle / Emulator]
   ├─ Gear / Speed / Odometer (P/D 안전 잠금 & 쿨타임 해제)
   ├─ HVAC / Wiper / Tire (5대 원소 연산 & 공기압 버프)
   └─ CarInfo / EvBattery (전 차종 식별 & EV E-pit 충전 감지)
          │
          ▼  (ai.pleos.playground:Vehicle:2.3.2 / Kotlin StateFlow)
[Android Native Host - PleosVehicleBridge.kt]
   ├─ Safety & Cooldown Controller
   └─ @JavascriptInterface (window.PleosBridge)
          │
          ▼  (WebView Pixel Art UI & Custom SVG Mecha Renderer)
[PLEOMEKABOT Pixel Web Engine (demo.html / index.html)]
   ├─ 5대 원소 상성 엔진 (불 → 얼음 → 바람 → 땅 → 물 → 불)
   ├─ 4대 핵심 스탯 (파워 · 방어 · 스피드 · 테크)
   └─ 20초 타이밍 배틀 & 8초 OVERDRIVE 물리 엔진
```

자세한 구조와 시나리오는 루트 경로의 **`PLEOMEKABOT_아키텍처_프로세스_요약.html`**에서 확인할 수 있습니다.

---

## 3. 프로젝트 구조

```text
.
├── android/                                # Android Studio / Pleos Vehicle SDK v2.3.2 네이티브 프로젝트
│   └── app/src/main/
│       ├── java/.../PleosVehicleBridge.kt  # Pleos SDK 연동 & Safety/Cooldown 브릿지
│       └── assets/index.html               # WebView 탑재 Pixel Art UI 게임 엔진
├── web-engine/                             # 웹 엔진 모듈 소스
├── mock-server/                            # Mock 텔레메트리 시뮬레이터 서버
├── demo.html                               # 단독 실행 가능한 웹 시뮬레이터
├── PLEOMEKABOT_아키텍처_프로세스_요약.html    # 아키텍처 · 프로세스 · 서비스 요약 HTML
├── PLEOMEKABOT-debug.apk                   # Pleos Connect Emulator 설치용 빌드 APK
└── README.md
```

---

## 4. 실행 및 빌드 방법

### A. 웹 시뮬레이터로 즉시 체험하기
별도 빌드 없이 `demo.html` 또는 `PLEOMEKABOT_아키텍처_프로세스_요약.html`을 웹 브라우저(Chrome/Edge)로 열면 즉시 동작을 확인할 수 있습니다.

### B. Android Studio & Pleos Connect Emulator 빌드
1. `android/` 디렉터리를 Android Studio에서 엽니다.
2. Pleos Playground Repository 및 `ai.pleos.playground:Vehicle:2.3.2` 의존성이 포함된 상태에서 Gradle Sync를 진행합니다.
3. Pleos Connect Emulator를 실행한 뒤 `./gradlew app:installDebug` (또는 루트의 `PLEOMEKABOT-debug.apk`를 `adb install`로 설치)하여 실행합니다.
