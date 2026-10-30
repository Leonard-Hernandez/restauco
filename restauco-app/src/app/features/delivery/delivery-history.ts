const KEY = 'restauco.deliveries';
const KEEP = 5;

/** The customer's recent orders on this device, so "Mis pedidos" can find them again. */
export function rememberDelivery(token: string) {
  try {
    const list = recentDeliveries().filter((t) => t !== token);
    localStorage.setItem(KEY, JSON.stringify([token, ...list].slice(0, KEEP)));
  } catch {
    // private mode: the link in the address bar still works
  }
}

export function recentDeliveries(): string[] {
  try {
    const list = JSON.parse(localStorage.getItem(KEY) ?? '[]');
    return Array.isArray(list) ? list.filter((t) => typeof t === 'string') : [];
  } catch {
    return [];
  }
}
