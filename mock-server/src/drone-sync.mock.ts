export interface DroneDefenseEvent {
  sector: string;
  expReward: number;
  timestamp: string;
}

export class DroneSyncMock {
  public static emitDefenseVictory(sector: string = 'Seongsu-B2'): DroneDefenseEvent {
    return {
      sector,
      expReward: 30,
      timestamp: new Date().toISOString(),
    };
  }
}
