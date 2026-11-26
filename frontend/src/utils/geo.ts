export interface Point {
  latitude: number
  longitude: number
}

export const toPoint = (position: GeolocationPosition): Point => ({
  latitude: position.coords.latitude,
  longitude: position.coords.longitude,
})

export function currentPosition(): Promise<Point> {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) return reject(new Error('Sin geolocalización'))
    navigator.geolocation.getCurrentPosition((p) => resolve(toPoint(p)), reject, {
      enableHighAccuracy: true,
      timeout: 10_000,
    })
  })
}
