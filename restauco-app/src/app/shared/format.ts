/** es-CO formatting in the restaurant's timezone. */

const TZ = 'America/Bogota';
const LOCALE = 'es-CO';

const cop = new Intl.NumberFormat(LOCALE, { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
const timeFmt = new Intl.DateTimeFormat(LOCALE, { hour: 'numeric', minute: '2-digit', hour12: true, timeZone: TZ });

export function price(value: number): string {
  return cop.format(value);
}

/** "+ $2.000" for a priced option, "" when included. */
export function extra(value: number): string {
  return value > 0 ? `+ ${cop.format(value)}` : '';
}

/** "9:41 p. m." from an ISO instant. */
export function time(instant: string): string {
  return timeFmt.format(new Date(instant));
}

/** Whole minutes elapsed since an ISO instant (never negative). */
export function minutesSince(instant: string | null, now: number = Date.now()): number {
  if (!instant) return 0;
  return Math.max(0, Math.floor((now - new Date(instant).getTime()) / 60_000));
}

/** "ahora", "5 min", "1 h 10 min" */
export function elapsed(minutes: number): string {
  if (minutes < 1) return 'ahora';
  if (minutes < 60) return `${minutes} min`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m ? `${h} h ${m} min` : `${h} h`;
}

/** "+57 314 809 4166" */
export function phone(e164: string | null | undefined): string {
  if (!e164) return '';
  const d = e164.replace(/\D/g, '');
  if (d.startsWith('57') && d.length === 12) {
    return `+57 ${d.slice(2, 5)} ${d.slice(5, 8)} ${d.slice(8)}`;
  }
  return `+${d}`;
}

/** Lowercase without accents, for search. */
export function fold(value: string): string {
  return value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
}
