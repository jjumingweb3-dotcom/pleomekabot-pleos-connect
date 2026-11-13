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
    // 스피드 1당 왕복 속도 감속 (조작 편의성 증대)
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
    // 테크 스탯에 따라 PERFECT 존 확대 (기본 49~51% -> 테크 10일 때 46.5~53.5%)
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
