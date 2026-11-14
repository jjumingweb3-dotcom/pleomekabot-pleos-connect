# Project Antigravity 개발 명세서 - PLEOMEKABOT (플레오메카봇)

> **프로젝트 코드명:** `PROJECT-PLEOMEKABOT`  
> **타깃 플랫폼:** Hyundai Pleos Connect OS (Android Automotive OS API 34 기반)  
> **개발 환경:** Google Project Antigravity Agentic IDE & Workspace  
> **핵심 스택:** Kotlin Native (Pleos SDK Bridge), Three.js / WebGL, TypeScript, Redis Geospatial Mock, Antigravity Agentic Pipeline

---

## 1. Antigravity 워크스페이스 구조 (`Directory Layout`)

Antigravity 에이전트가 코드를 탐색하고 역할을 분담해 자동 빌드할 수 있도록 모듈을 분리합니다.

```
pleomekabot-antigravity/
├── .antigravity/
│   ├── project-rules.md               # 에이전트 개발 규칙 및 코딩 컨벤션
│   └── workflows/
│       ├── build-pipeline.yaml        # APK 빌드 및 WebGL 번들링 파이프라인
│       └── mock-sensor-stream.yaml    # Pleos Vehicle SDK 시뮬레이션 설정
├── android/                           # Native Pleos Connect Container
│   ├── app/src/main/
│   │   ├── java/io/pleos/mekabot/
│   │   │   ├── bridge/
│   │   │   │   ├── PleosVehicleBridge.kt   # Vehicle SDK 인터페이스 바인딩
│   │   │   │   ├── FusedLocationBridge.kt  # 1~5km 위치 동기화 브릿지
│   │   │   │   └── GleoAudioBridge.kt      # STT / TTS 브릿지
│   │   │   └── HmiActivity.kt              # AAOS 멀티 윈도우 & D/P 라이프사이클 관리
│   │   └── AndroidManifest.xml
├── web-engine/                        # In-Car 3D & 20s Battle Web Engine
│   ├── src/
│   │   ├── core/
│   │   │   ├── StateManager.ts        # 주행/정차(D/P), EXP, 스탯 상태 머신
│   │   │   └── ElementEngine.ts       # 5대 원소 상성 계산 및 센서 파싱
│   │   ├── battle/
│   │   │   ├── BattleTimer.ts         # 20초 타임라인 컨트롤러
│   │   │   └── TimingBarPhysics.ts    # 8초 OVERDRIVE 바늘 물리 및 PERFECT 판정
│   │   ├── render/
│   │   │   ├── MechaRenderer.ts       # Three.js 차종별 실루엣 3D 렌더링
│   │   │   └── AmbientGlowFx.ts       # 5원소 앰비언트 라이트 셰이더
│   │   └── index.ts
│   └── package.json
└── mock-server/                       # 로컬 개발용 목 서버
    └── src/
        ├── redis-geo.mock.ts          # 반경 1~5km 매칭 시뮬레이터
        └── spot-sync.mock.ts          # 현재 주차 위치·POI 스페셜 스팟 매칭 스트림
```

---

## 2. Antigravity 에이전트 프롬프트 가이드 (`Agent Directive Rules`)

Antigravity의 Agent Runner에게 지시할 코어 룰입니다. `.antigravity/project-rules.md`에 배치합니다.

```markdown
# Antigravity Agent Directive: PLEOMEKABOT Engineering Rules

1. Zero-Distraction Compliance:
   - 기어 상태가 'D', 'R', 'N'일 때는 어떠한 인터랙티브 UI도 렌더링하지 않는다.
   - WebView는 즉시 pauseTimers() 및 pause()를 호출하고 백그라운드 데이터 수집 모드로만 동작한다.
   - 조작 이벤트 리스너는 기어가 'P'로 체결되었을 때만 바인딩한다.

2. SDK Fallback Mandate:
   - Pleos Connect SDK 호출 전 반드시 checkXXXCapabilities()를 수행한다.
   - 앰비언트 라이트나 시트 진동 모듈 미지원 시 에러를 던지지 않고, WebGL 상의 화면 쉐이크(Screen Shake) 및 테두리 글로우(Border Glow)로 자동 Fallback 처리한다.

3. 20-Second Battle Strict Timeline:
   - 0~4초: 매칭 및 상성 오프닝
   - 4~16초: 자동 데미지 연산 (초당 1회 틱)
   - 8.0초: OVERDRIVE 타이밍 바 활성화 (정확히 2.0초간 좌우 왕복)
   - 16~20초: 결과 정산 및 경험치 반영
   - 총 실행 시간은 20.0초를 절대 초과해서는 안 되며, 미조작 시 'GOOD' 판정으로 강제 완료된다.
```

---

## 3. 핵심 모듈별 상세 구현 명세

### 3.1. Pleos Vehicle Bridge (`PleosVehicleBridge.kt`)
Kotlin Native 환경에서 Pleos Connect SDK의 이벤트를 감지하여 WebGL 엔진에 JavaScript 인터페이스로 주입합니다.

```kotlin
package io.pleos.mekabot.bridge

import android.webkit.JavascriptInterface
import android.webkit.WebView
import io.pleos.connect.sdk.Vehicle
import io.pleos.connect.sdk.vehicle.*

class PleosVehicleBridge(private val webView: WebView, private val vehicle: Vehicle) {

    fun startListening() {
        // 1. 기어 변속 감지 (P/D 분기)
        vehicle.carInfo.registerGearListener { gearPosition ->
            val gearStr = when (gearPosition) {
                GearPosition.PARK -> "P"
                GearPosition.DRIVE -> "D"
                GearPosition.REVERSE -> "R"
                GearPosition.NEUTRAL -> "N"
                else -> "UNKNOWN"
            }
            emitToWeb("onGearChanged", "{ gear: '$gearStr' }")
        }

        // 2. 환경 센서 스트림 (5대 원소 파싱)
        vehicle.hvac.registerExteriorTempListener { temp ->
            emitToWeb("onExteriorTempChanged", "{ temp: $temp }")
        }
        vehicle.wiper.registerWiperStatusListener { isWiping ->
            emitToWeb("onWiperChanged", "{ isRaining: $isWiping }")
        }

        // 3. 타이어 및 배터리 (컨디션 & EV 리차지)
        vehicle.tire.checkTireCapabilities { supported ->
            if (supported) {
                vehicle.tire.getTirePressure { pressures ->
                    val isNormal = pressures.all { it >= 32.0 }
                    emitToWeb("onTireStatusChanged", "{ fullArmor: $isNormal }")
                }
            }
        }
    }

    private fun emitToWeb(event: String, jsonPayload: String) {
        webView.post {
            webView.evaluateJavascript("window.PleosEventBus.dispatch('$event', $jsonPayload);", null)
        }
    }
}
```

### 3.2. 5대 원소 상성 판정 엔진 (`ElementEngine.ts`)
주행 센서 입력값을 5대 원소로 변환하고 상성 데미지 배율을 산출합니다.

```typescript
export type ElementType = 'FIRE' | 'ICE' | 'WIND' | 'EARTH' | 'WATER';

export interface SensorData {
  temp: number;
  isRaining: boolean;
  isCruise: boolean;
  accumulatedKm: number;
}

export class ElementEngine {
  // 상성 테이블: Attacker -> Defender = Multiplier
  private static readonly ADVANTAGE_MAP: Record<ElementType, ElementType> = {
    FIRE: 'ICE',
    ICE: 'WIND',
    WIND: 'EARTH',
    EARTH: 'WATER',
    WATER: 'FIRE',
  };

  public static determineCoreElement(sensor: SensorData): ElementType {
    if (sensor.isRaining) return 'WATER';
    if (sensor.temp <= 5) return 'ICE';
    if (sensor.temp >= 28) return 'FIRE';
    if (sensor.isCruise) return 'WIND';
    return 'EARTH';
  }

  public static calculateMultiplier(attacker: ElementType, defender: ElementType): number {
    if (this.ADVANTAGE_MAP[attacker] === defender) return 1.3;  // 상성 우위 (+30%)
    if (this.ADVANTAGE_MAP[defender] === attacker) return 0.8;  // 상성 열세 (-20%)
    return 1.0;                                                // 동등
  }
}
```

### 3.3. 8초 OVERDRIVE 타이밍 바 물리 엔진 (`TimingBarPhysics.ts`)
심사위원이 손맛을 느끼는 핵심 인터랙션인 페널티킥 방식 타이밍 바의 판정 로직입니다.

```typescript
export interface TimingResult {
  grade: 'PERFECT' | 'GREAT' | 'GOOD' | 'MISS';
  damageMultiplier: number;
  bonusExp: number;
}

export class TimingBarPhysics {
  private needlePos: number = 0; // 0.0 ~ 100.0%
  private direction: number = 1;
  private speed: number = 1.8;   // 스피드 스탯에 따라 감속

  constructor(userSpeedStat: number) {
    // 스피드 1당 왕복 속도 1.5% 감속 (조작 편의성 증대)
    this.speed = Math.max(0.8, 1.8 - (userSpeedStat * 0.02));
  }

  public update(): number {
    this.needlePos += this.direction * this.speed;
    if (this.needlePos >= 100.0 || this.needlePos <= 0.0) {
      this.direction *= -1;
    }
    return this.needlePos;
  }

  public evaluateTap(userTechStat: number): TimingResult {
    // 테크 스탯에 따라 PERFECT 존 확대 (기본 48~52% -> 테크 20일 때 45~55%)
    const techMargin = userTechStat * 0.25;
    const perfectMin = 49.0 - techMargin;
    const perfectMax = 51.0 + techMargin;

    if (this.needlePos >= perfectMin && this.needlePos <= perfectMax) {
      return { grade: 'PERFECT', damageMultiplier: 2.0, bonusExp: 40 };
    }
    if (this.needlePos >= 35.0 && this.needlePos <= 65.0) {
      return { grade: 'GREAT', damageMultiplier: 1.5, bonusExp: 20 };
    }
    if (this.needlePos >= 15.0 && this.needlePos <= 85.0) {
      return { grade: 'GOOD', damageMultiplier: 1.0, bonusExp: 0 };
    }
    return { grade: 'MISS', damageMultiplier: 0.7, bonusExp: 0 };
  }
}
```

---

## 4. Antigravity 태스크 워크플로우 (`Antigravity Task Script`)

Antigravity 터미널에서 순차적으로 호출하여 프로토타입을 빌드할 수 있는 스크립트 가이드입니다.

```bash
# Task 1: 웹 뷰엔진 번들링 (Three.js 및 배틀 로직)
antigravity run build:web-engine

# Task 2: Pleos SDK 시뮬레이터 목 서버 실행 (Redis Geo + 센서 스트림)
antigravity run start:mock-sensor

# Task 3: Android Automotive Emulator (API 34) 배포 및 테스트
antigravity run deploy:emulator --target="Pleos-Connect-Virtual-Device"
```

---

## 5. 심사 영상 녹화 및 GitHub 산출물 검증 체크리스트

Antigravity 빌드 후 산출물을 레포지토리에 커밋할 때 점검할 목록입니다.

* [ ] 기어 D 전환 시 0.1초 내 화면 블라인드 처리 및 터치 차단 동작 확인
* [ ] 와이퍼 체크 시 앰비언트 라이트가 `#0088FF`(블루)로 동기화되는지 확인
* [ ] 8초 타이밍 바 탭 시 정중앙 2% 범위에서 크리티컬 텍스트 및 시트 진동 트리거 확인
* [ ] 10km 주행 및 배틀 승리 시 상단 헤더와 EXP 바에 경험치가 즉시 리플렉션되는지 확인
* [ ] 레벨업 시 스탯 3포인트가 정상 차감 및 스탯 수치에 가산되는지 확인