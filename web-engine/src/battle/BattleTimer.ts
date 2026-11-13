export type BattlePhase = 'OPENING' | 'COMBAT' | 'OVERDRIVE' | 'SETTLEMENT' | 'FINISHED';

export class BattleTimer {
  private elapsedSeconds: number = 0;

  public tick(deltaSec: number = 0.5): BattlePhase {
    this.elapsedSeconds = Math.min(20.0, +(this.elapsedSeconds + deltaSec).toFixed(1));
    if (this.elapsedSeconds < 4.0) return 'OPENING';
    if (this.elapsedSeconds === 8.0) return 'OVERDRIVE';
    if (this.elapsedSeconds < 16.0) return 'COMBAT';
    if (this.elapsedSeconds < 20.0) return 'SETTLEMENT';
    return 'FINISHED';
  }

  public getElapsed(): number {
    return this.elapsedSeconds;
  }
}
