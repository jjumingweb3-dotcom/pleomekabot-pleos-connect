export type ElementType = 'FIRE' | 'ICE' | 'WIND' | 'EARTH' | 'WATER';

export interface SensorData {
  temp: number;
  isRaining: boolean;
  isCruise: boolean;
  accumulatedKm: number;
}

export class ElementEngine {
  // 5대 원소 순환 상성 테이블: 🔥불 -> ❄️얼음 -> 🌪️바람 -> 🪨땅 -> 💧물 -> 🔥불
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
