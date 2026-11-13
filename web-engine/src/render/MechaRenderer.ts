export class MechaRenderer {
  public static resolveModelProfile(carModel: string): { callsign: string; silhouette: string } {
    switch (carModel) {
      case 'GRANDEUR':
        return { callsign: '그랜저-발키리', silhouette: '🦾' };
      case 'TUCSON':
        return { callsign: '투싼-가디언', silhouette: '🛡️' };
      case 'IONIQ6':
      default:
        return { callsign: '아이오닉-타이탄', silhouette: '🤖' };
    }
  }
}
