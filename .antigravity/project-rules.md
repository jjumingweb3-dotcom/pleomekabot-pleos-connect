# Antigravity Agent Directive: PLEOMEKABOT Engineering Rules

1. Zero-Distraction Compliance:
   - 기어 상태가 'D', 'R', 'N'일 때는 어떠한 인터랙티브 UI도 렌더링하지 않는다.
   - WebView는 즉시 백그라운드 데이터 수집 모드로만 동작한다.
   - 조작 이벤트 리스너는 기어가 'P'로 체결되었을 때만 바인딩한다.

2. SDK Fallback Mandate:
   - Pleos Connect SDK (`ai.pleos.playground:Vehicle:2.3.2`) 호출 전 반드시 `checkXXXCapabilities()`를 수행한다.
   - 앰비언트 라이트나 시트 진동 모듈 미지원 시 에러를 던지지 않고, WebGL 상의 화면 쉐이크(Screen Shake) 및 테두리 글로우(Border Glow)로 자동 Fallback 처리한다.

3. 20-Second Battle Strict Timeline:
   - 0~4초: 매칭 및 상성 오프닝
   - 4~16초: 자동 데미지 연산 (초당 1회 틱)
   - 8.0초: OVERDRIVE 타이밍 바 활성화 (2.0초간 좌우 왕복)
   - 16~20초: 결과 정산 및 경험치 반영
   - 총 실행 시간은 20.0초를 절대 초과해서는 안 되며, 미조작 시 'GOOD' 판정으로 강제 완료된다.
