export type GearType = 'P' | 'R' | 'N' | 'D';

export interface CoreStats {
  pow: number;
  def: number;
  spd: number;
  tec: number;
}

export class StateManager {
  public gear: GearType = 'D';
  public level: number = 13;
  public exp: number = 80;
  public statPoints: number = 3;
  public stats: CoreStats = { pow: 14, def: 12, spd: 10, tec: 10 };

  public isInteractiveAllowed(): boolean {
    return this.gear === 'P';
  }

  public addExp(amount: number): boolean {
    this.exp += amount;
    let leveledUp = false;
    while (this.exp >= 100) {
      this.exp -= 100;
      this.level += 1;
      this.statPoints += 3;
      leveledUp = true;
    }
    return leveledUp;
  }

  public allocateStat(statKey: keyof CoreStats): boolean {
    if (this.statPoints <= 0) return false;
    this.statPoints -= 1;
    this.stats[statKey] += 1;
    return true;
  }
}
