/**
 * Short alert tones for the panel screens (no audio files). Browsers only allow sound after
 * a user gesture, so the first tap on the page unlocks it.
 */
let ctx: AudioContext | null = null;

export function unlockChime() {
  const unlock = () => {
    ctx ??= new AudioContext();
    void ctx.resume();
  };
  document.addEventListener('pointerdown', unlock, { once: true, capture: true });
}

export function soundEnabled(): boolean {
  return ctx?.state === 'running';
}

/** 'new' = two rising notes (kitchen: new ticket); 'ready' = three bright notes (waiter: pick up). */
export function chime(kind: 'new' | 'ready') {
  if (!ctx || ctx.state !== 'running') return;
  const notes = kind === 'new' ? [660, 880] : [880, 1175, 1568];
  notes.forEach((freq, i) => {
    const start = ctx!.currentTime + i * 0.16;
    const osc = ctx!.createOscillator();
    const gain = ctx!.createGain();
    osc.type = 'sine';
    osc.frequency.value = freq;
    gain.gain.setValueAtTime(0.0001, start);
    gain.gain.exponentialRampToValueAtTime(0.35, start + 0.02);
    gain.gain.exponentialRampToValueAtTime(0.0001, start + 0.28);
    osc.connect(gain).connect(ctx!.destination);
    osc.start(start);
    osc.stop(start + 0.3);
  });
  navigator.vibrate?.(kind === 'ready' ? [120, 60, 120] : 80);
}
