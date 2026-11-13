export interface GeoTarget {
  id: string;
  callsign: string;
  element: 'FIRE' | 'ICE' | 'WIND' | 'EARTH' | 'WATER';
  distanceKm: number;
}

export class RedisGeoMock {
  public static queryNearbyBots(lat: number, lng: number, radiusKm: number = 3.0): GeoTarget[] {
    return [
      { id: 'seongsu-01', callsign: '그랜저-크림슨', element: 'FIRE', distanceKm: 1.4 },
      { id: 'seongsu-02', callsign: '코나-프로스트', element: 'ICE', distanceKm: 2.8 },
    ];
  }
}
