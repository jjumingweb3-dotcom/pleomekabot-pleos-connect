# PLEOMEKABOT (플레오메카봇)

> **앱 이름 및 한 줄 소개**: **PLEOMEKABOT (플레오메카봇)** — 운전이 곧 게임이 되는 차량판 포켓몬 GO, Pleos Connect Vehicle SDK 기반 차량 데이터 연동 위치·주행 RPG

---

## 1. 앱 개요 및 핵심 게임 루프

**PLEOMEKABOT**은 현대자동차그룹의 **Pleos Playground Vehicle SDK (`ai.pleos.playground:Vehicle:2.3.2`)**와 차량 내 위치·주행·상태 데이터를 결합하여, **주행 → 정차(Gear P) 전투 → 성장·파츠 커스터마이징 → 재주행**의 직관적인 루프로 즐기는 차량용 게이미피케이션 앱입니다.

1. **주행거리 기반 배틀권 획득 (`DistanceDriven`)**
   - 차량을 **10km 주행할 때마다 배틀권 1회**가 자동 지급됩니다.
   - 배틀권은 오직 실제 주행거리에 의해서만 획득되며, 별도 조작 없이 안전한 주행 자체가 게임의 에너지원이 됩니다.

2. **직전 주행 기반 5대 속성 버프 (고정 속성 없음)**
   - 메카봇은 태생적으로 고정된 속성을 갖지 않으며, **전투 직전 주행 데이터**에 따라 5가지 속성 버프 중 하나를 획득해 전투에 적용합니다.
     - **불 (`FIRE`)**: 고온 주행 (`28°C+`, `HvacTemperatureValue` / 외기온도)
     - **얼음 (`ICE`)**: 저온 주행 (`5°C-`, `HvacTemperatureValue` / 외기온도)
     - **바람 (`WIND`)**: 크루즈 컨트롤 정속 주행 (`CruiseControlState`)
     - **땅 (`EARTH`)**: 안정적 일반 주행 (기본 버프)
     - **물 (`WATER`)**: 우천 와이퍼 작동 주행 (`WiperState`)

3. **정차(Gear `P`) 수동 배틀 시작 & 스페셜 스팟 매칭**
   - **안전 정책 준수**: 주행 중(`D`/`R`/`N`)에는 전투 및 UI 조작을 차단하고, **기어 `P`단 정차 상태**에서 운전자가 앱을 실행해 직접 **배틀 시작**을 누를 때만 전투가 진행됩니다.
   - **현재 주차 위치 배치 & 상대 매칭**:
     - 내 메카봇은 **현재 차량이 주차된 위치**에 자동 배치됩니다.
     - **유저 차량 메카봇 매칭**: 주변에 주차·배치된 다른 차량 메카봇과 대전 시 **보너스 경험치**를 획득합니다.
     - **스페셜 스팟(POI) 메카봇**: `경복궁`, `에버랜드`, `스타필드` 등 유명 관광지·유적지·랜드마크 정차 시 해당 장소만의 **스페셜 메카봇**이 등장합니다.
     - **AI 야생 메카봇**: 주변 유저 차량이나 스페셜 스팟이 없을 경우 AI 생성 메카봇과 대결합니다.

4. **실제 차량 상태 연동 디버프 (`Pleos Vehicle SDK v2.3.2`)**
   - 차량 이상 상태에 따라 게임 내 메카봇 능력치가 실시간으로 변경되며, 정상 컨디션 유지 시 **보호막(+10% HP)** 버프가 적용됩니다.
     - 타이어 공기압 경고 (`TirePressureListener`) → **방어력(DEF) 저하 (-20%)**
     - 엔진오일 레벨 경고 (`OilLevelStateListener`) → **파워(POW) 저하 (-20%)**
     - 워셔액 부족 경고 (`LowWasherFluidWaringListener`) → **테크(TEC) 저하 (-20%)**
     - 출력 제한 / 연료·배터리 부족 (`PowerDownWarningIndicatorListener` / `FuelPercentListener` / `EvBatteryStateOfChargeListener`) → **스피드(SPD) 저하 (-20%)**

5. **전 차종 확장 지원 & 파츠 성장 시스템**
   - **지원 차종**: 실제 서비스는 **Pleos 앱을 사용할 수 있는 모든 차종**을 대상으로 하며, 현재 **시안(프로토타입)에서는 대표 4종(`투싼`, `그랜저`, `아이오닉`, `팰리세이드`)**을 우선 구현했습니다.
   - **현대자동차그룹 서비스 연계 파츠 획득 & 외형 업그레이드**:
     - 레벨업에 따라 메카봇 외형 티어가 진화하며, **블루핸즈 정비(블루핸즈 실드 +3 DEF)**, **E-pit 충전(E-pit 윙 +3 SPD)**, **블루멤버스 포인트(블루포인트 캐논 +3 POW)**, **공식 이벤트(이벤트 코어 +3 TEC)** 등을 통해 능력치와 외형이 강화되는 파츠를 장착할 수 있습니다.

---

## 2. 실행 방법

### A. APK 직접 설치 및 실행 (권장)
1. Pleos Connect 에뮬레이터 실행 후 `adb`로 저장소 내 Release APK를 설치합니다.
   ```bash
   adb install -r apk/PLEOMEKABOT_PLEOMEKABOT_release.apk
   adb shell am start -n ai.pleos.playground.sample.vehicle/.MainActivity
   ```
2. 앱 실행 후 좌측 차량 센서 시뮬레이터(또는 Pleos Vehicle SDK 연동 신호)를 통해 기어(`P`/`R`/`N`/`D`), 차종 선택, `+10km 주행 완료`, 스페셜 스팟(`경복궁`·`에버랜드`·`스타필드`), 차량 이상 디버프, 파츠 장착 및 20초 배틀을 즉시 테스트할 수 있습니다.

### B. Android Studio 소스 빌드 및 실행
1. Android Studio에서 저장소 내 `android/` 폴더를 프로젝트로 엽니다.
2. Gradle JDK를 **JDK 21 (JBR 21)**로 설정합니다.
3. Pleos Connect 에뮬레이터를 선택한 뒤 **Run (`Shift + F10`)** 또는 터미널에서 아래 명령으로 빌드합니다.
   ```bash
   cd android
   ./gradlew app:assembleRelease
   ```

### C. 웹 브라우저 단독 시뮬레이터 실행
- 저장소 루트의 [`demo.html`](./demo.html) 파일을 브라우저(Chrome/Edge)로 열면 별도 빌드 없이도 동일한 인카 UI와 센서 시뮬레이터를 즉시 체험할 수 있습니다.

---

## 3. 테스트 환경 및 필요한 설정

- **Android SDK**: `compileSdk = 35`, `targetSdk = 35`, `minSdk = 28`
- **JDK / Kotlin / Gradle**: `JDK 21 (JBR 21.0.11)` / `Kotlin 2.0.0` / `Gradle 8.10.2 (AGP 8.7.2)`
- **Pleos Playground SDK**: `ai.pleos.playground:Vehicle:2.3.2`
- **Application ID / Entry Activity**: `ai.pleos.playground.sample.vehicle` / `.MainActivity` (Pleos Connect 런처 화이트리스트 규격 준수)
- **권한 및 설정**: 별도 외부 API 키 설정 불필요 (Pleos Vehicle Service 미연결 환경에서도 내장 Hybrid SDK Mock 컨트롤러로 즉시 동작)

---

## 4. 개발에 사용한 Pleos Connect 에뮬레이터 버전과 화면 규격

- **에뮬레이터 버전**: `Pleos Connect v2.1.0 APIs` (`system-images;android-34;pleos_car_emulator;x86_64`, Android 14 / API 34)
- **화면 규격 (해상도 및 밀도)**: `2560 × 1440 px` (`hw.lcd.width=2560`, `hw.lcd.height=1440`, `hw.lcd.density=160 dpi`, 가로형 차량 IVI 디스플레이 규격)

---

## 5. APK 파일 위치

- [`apk/PLEOMEKABOT_PLEOMEKABOT_release.apk`](./apk/PLEOMEKABOT_PLEOMEKABOT_release.apk)

---

## 6. 프로젝트 디렉토리 구조

- [`apk/`](./apk): 실행 가능한 서명된 Release APK (`PLEOMEKABOT_PLEOMEKABOT_release.apk`)
- [`android/`](./android): Pleos Playground Vehicle SDK (`ai.pleos.playground:Vehicle:2.3.2`) 연동 Android 앱 소스 (`PleosVehicleBridge.kt` + WebView 브리지)
- [`demo.html`](./demo.html): 인터랙티브 웹 시뮬레이터 (시안 프로토타입: 대표 4종 메카봇, 주행 거리 10km 배틀권 획득, 기어 P 배틀, POI 스페셜 메카봇, 파츠/디버프 실시간 테스트)
- [`PLEOMEKABOT_아키텍처_프로세스_요약.html`](./PLEOMEKABOT_아키텍처_프로세스_요약.html): 아키텍처, 게임 메커니즘, Pleos SDK 연동 요약 문서
