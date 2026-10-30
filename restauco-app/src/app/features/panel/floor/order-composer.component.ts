import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, ChevronUp, Ellipsis, Mic, Minus, Plus, Search, Send, Sparkles, Trash2, X } from 'lucide-angular';
import { ApiService, errorMessage } from '../../../core/api/api.service';
import { Dish, VoiceIssue } from '../../../core/api/models';
import { addToCart, cartCount, cartTotal, CartLine, describe, lineTotal, needsChoices, newLine } from '../../../shared/cart';
import { fold, price } from '../../../shared/format';
import { DishSheetComponent } from '../../menu/dish-sheet.component';
import { FloorStore } from './floor-store.service';

/**
 * Taking orders fast, for one table or several at once: one tap adds a dish, − takes one
 * back, and dishes with required choices (or the ⋯ button) open the full sheet. The strip
 * on top keeps a draft per table, so the waiter can switch tables and send each when ready.
 * Closing keeps the drafts; only "Vaciar" throws one away.
 *
 * The mic dictates an order with the phone's own speech-to-text ("mesa 3, dos pelúas y una
 * sifrina sin queso"); the API turns the text into lines of that table's draft, to review and
 * send like any other. What it could not place is listed, never guessed.
 */
@Component({
  selector: 'app-order-composer',
  imports: [FormsModule, LucideAngularModule, DishSheetComponent],
  template: `
    <div class="composer" role="dialog" aria-modal="true" [attr.aria-label]="'Pedido para la mesa ' + tableLabel()">
      <header class="head">
        <button type="button" class="icon-btn" (click)="store.composing.set(null)" aria-label="Cerrar (el pedido queda guardado)"><lucide-icon [img]="X" [size]="20" /></button>
        <div class="tables" role="tablist" aria-label="Mesas con pedido en curso">
          @for (t of strip(); track t.tableId) {
            <button type="button" role="tab" class="tchip" [attr.aria-selected]="t.tableId === tableId()" (click)="switchTo(t.tableId)">
              <span class="tnum display">{{ number(t.tableId) }}</span>
              @if (t.count) { <span class="tcount tabular">{{ t.count }}</span> }
            </button>
          }
          <button type="button" class="tchip add" (click)="picking.set(true)" aria-label="Pedido para otra mesa">
            <lucide-icon [img]="Plus" [size]="18" /> Mesa
          </button>
        </div>
      </header>
      <div class="filters">
        <div class="find">
          <label class="search">
            <lucide-icon [img]="Search" [size]="17" aria-hidden="true" />
            <input type="search" placeholder="Buscar plato" [(ngModel)]="query" aria-label="Buscar plato" enterkeyhint="search" />
            @if (query) { <button type="button" class="clear" (click)="query = ''" aria-label="Borrar búsqueda"><lucide-icon [img]="X" [size]="16" /></button> }
          </label>
          <button type="button" class="mic" (click)="openVoice()" aria-label="Dictar el pedido"><lucide-icon [img]="Mic" [size]="21" /></button>
        </div>
        <nav class="cats" aria-label="Categorías">
          <button type="button" class="chip" [class.on]="category() === null" (click)="category.set(null)">Todo</button>
          @for (c of categories(); track c.id) {
            <button type="button" class="chip" [class.on]="category() === c.id" (click)="category.set(c.id)">{{ c.name }}</button>
          }
        </nav>
      </div>

      <div class="grid-wrap">
        <ul class="grid">
          @for (d of visible(); track d.id) {
            <li class="tile-wrap">
              <button type="button" class="tile" [class.in]="inCart(d.id)" [disabled]="!d.orderable" (click)="tap(d)">
                <span class="name">{{ d.name }}</span>
                <span class="price tabular">{{ d.orderable ? price(d.price) : 'Agotado' }}</span>
                @if (needsChoices(d)) { <span class="hint">escoger opciones</span> }
              </button>
              @if (inCart(d.id); as n) {
                <span class="count badge" aria-hidden="true">{{ n }}</span>
                <button type="button" class="less" (click)="removeOne(d)" [attr.aria-label]="'Quitar un ' + d.name + ' (hay ' + n + ')'">
                  <lucide-icon [img]="Minus" [size]="18" />
                </button>
              }
              @if (d.orderable && hasExtras(d)) {
                <button type="button" class="more" (click)="selected.set(d)" [attr.aria-label]="'Opciones de ' + d.name">
                  <lucide-icon [img]="Ellipsis" [size]="18" />
                </button>
              }
            </li>
          } @empty {
            <li class="empty">Nada coincide con “{{ query }}”.</li>
          }
        </ul>
      </div>

      <footer class="cart" [class.open]="expanded()">
        @if (voiceResult(); as r) {
          <div class="heard" role="status">
            <div class="heard-head">
              <lucide-icon [img]="Sparkles" [size]="16" aria-hidden="true" />
              <span>{{ r.summary }}</span>
              <button type="button" class="icon-btn sm" (click)="voiceResult.set(null)" aria-label="Cerrar resumen"><lucide-icon [img]="X" [size]="16" /></button>
            </div>
            @if (r.issues.length) {
              <ul class="issues">
                @for (i of r.issues; track $index) {
                  <li>
                    @if (i.code === 'NEEDS_CHOICES' && dishById(i.dishId); as d) {
                      <button type="button" class="issue fix" (click)="selected.set(d)">{{ d.name }}: escoger opciones <small>«{{ i.heard }}»</small></button>
                    } @else if (i.code === 'UNAVAILABLE') {
                      <span class="issue">{{ i.dishName }} está agotado <small>«{{ i.heard }}»</small></span>
                    } @else {
                      <span class="issue">No entendí <small>«{{ i.heard }}»</small></span>
                    }
                  </li>
                }
              </ul>
            }
          </div>
        }
        @if (expanded() && lines().length) {
          <div class="lines-head">
            <span class="label">Mesa {{ tableLabel() }}</span>
            <button type="button" class="btn btn-sm btn-ghost" (click)="clear()"><lucide-icon [img]="Trash2" [size]="15" /> Vaciar</button>
          </div>
          <ul class="lines">
            @for (l of lines(); track l.key) {
              <li>
                <div class="line-main">
                  <span class="line-name">{{ l.dish.name }}</span>
                  @if (describe(l); as text) { <span class="line-desc">{{ text }}</span> }
                  @if (l.note) { <span class="line-note">“{{ l.note }}”</span> }
                </div>
                <span class="tabular line-total">{{ price(lineTotal(l)) }}</span>
                <div class="stepper">
                  <button type="button" class="icon-btn sm" (click)="changeQty(l, -1)" [attr.aria-label]="l.qty === 1 ? 'Quitar' : 'Uno menos'">
                    <lucide-icon [img]="l.qty === 1 ? Trash2 : Minus" [size]="16" />
                  </button>
                  <span class="tabular qty">{{ l.qty }}</span>
                  <button type="button" class="icon-btn sm" (click)="changeQty(l, 1)" [disabled]="l.qty >= 20" aria-label="Uno más"><lucide-icon [img]="Plus" [size]="16" /></button>
                </div>
              </li>
            }
          </ul>
        }
        @if (error()) { <p class="error-text" role="alert">{{ error() }}</p> }
        <div class="bar">
          <button type="button" class="summary" (click)="expanded.set(!expanded())" [disabled]="!lines().length" [attr.aria-expanded]="expanded()">
            <lucide-icon [img]="ChevronUp" [size]="18" [class.flip]="expanded()" />
            <span>{{ count() }} {{ count() === 1 ? 'ítem' : 'ítems' }}</span>
            <strong class="tabular">{{ price(total()) }}</strong>
          </button>
          <button type="button" class="btn btn-brand send" (click)="send()" [disabled]="!lines().length || sending()">
            <lucide-icon [img]="Send" [size]="18" /> {{ sending() ? 'Enviando…' : 'Mesa ' + tableLabel() + ' a cocina' }}
          </button>
        </div>
      </footer>
    </div>

    @if (picking()) {
      <div class="scrim picker-scrim" (click)="picking.set(false)">
        <section class="sheet" role="dialog" aria-modal="true" aria-labelledby="pick-title" (click)="$event.stopPropagation()">
          <header class="sheet-head">
            <h2 id="pick-title" class="display">¿Para qué mesa?</h2>
            <button type="button" class="icon-btn" (click)="picking.set(false)" aria-label="Cerrar"><lucide-icon [img]="X" [size]="20" /></button>
          </header>
          <ul class="pick">
            @for (t of store.tables() ?? []; track t.id) {
              <li>
                <button type="button" class="pick-btn" [class.busy]="t.sessionId" [class.current]="t.id === tableId()" (click)="switchTo(t.id)">
                  <span class="display">{{ t.number }}</span>
                  <small>{{ store.draft(t.id).length ? 'en curso' : t.sessionId ? 'abierta' : 'libre' }}</small>
                </button>
              </li>
            }
          </ul>
        </section>
      </div>
    }

    @if (voiceOpen()) {
      <div class="scrim picker-scrim" (click)="closeVoice()">
        <section class="sheet voice" role="dialog" aria-modal="true" aria-labelledby="voice-title" (click)="$event.stopPropagation()">
          <header class="sheet-head">
            <h2 id="voice-title" class="display">Dictar pedido</h2>
            <button type="button" class="icon-btn" (click)="closeVoice()" aria-label="Cerrar"><lucide-icon [img]="X" [size]="20" /></button>
          </header>
          <div class="voice-body">
            <p class="voice-hint">Di la mesa y los platos: <em>“mesa {{ tableLabel() }}, dos pelúas y una sifrina sin queso”</em>.
              @if (!canListen) { Toca el micrófono del teclado para dictar. }</p>
            <textarea rows="4" [(ngModel)]="voiceText" placeholder="Lo que dictes aparece aquí; puedes corregirlo"
                      aria-label="Pedido dictado" maxlength="1000"></textarea>
            @if (voiceError()) { <p class="error-text" role="alert">{{ voiceError() }}</p> }
            <div class="voice-actions">
              @if (canListen) {
                <button type="button" class="btn listen" [class.on]="listening()" (click)="listening() ? stopListening() : listen()"
                        [attr.aria-pressed]="listening()">
                  <lucide-icon [img]="Mic" [size]="20" /> {{ listening() ? 'Escuchando… toca para parar' : 'Dictar' }}
                </button>
              }
              <button type="button" class="btn btn-brand" (click)="understand()" [disabled]="!voiceText.trim() || understanding()">
                {{ understanding() ? 'Armando…' : 'Armar pedido' }}
              </button>
            </div>
            <p class="voice-foot">Nada se envía a cocina: revisas el pedido antes.</p>
          </div>
        </section>
      </div>
    }

    @if (selected(); as d) {
      <app-dish-sheet [dish]="d" [tags]="menu().tags" [orderable]="true" actionLabel="Agregar"
                      (close)="selected.set(null)" (add)="add($event)" />
    }
  `,
  styles: `
    .composer { position: fixed; inset: 0; z-index: 45; display: flex; flex-direction: column; background: var(--ground); }
    .head { display: flex; align-items: center; gap: 0.5rem; padding: calc(0.55rem + env(safe-area-inset-top)) 0.9rem 0.35rem; }
    .tables { flex: 1; min-width: 0; display: flex; gap: 0.4rem; overflow-x: auto; scrollbar-width: none; padding: 0.15rem 0.1rem; }
    .tchip { flex: none; display: inline-flex; align-items: center; gap: 0.4rem; min-height: 46px; min-width: 46px; justify-content: center; padding: 0 0.75rem; border-radius: 14px; border: 2px solid var(--line); background: var(--ground-raised); color: var(--ink-muted); font-family: var(--font-label); text-transform: uppercase; }
    .tchip[aria-selected="true"] { border-color: var(--brand); background: color-mix(in oklab, var(--brand) 16%, var(--ground-raised)); color: var(--brand); }
    .tnum { font-size: 1.5rem; line-height: 1; }
    .tcount { min-width: 1.4rem; height: 1.4rem; padding: 0 0.3rem; border-radius: 999px; display: grid; place-items: center; font-size: 0.8rem; font-weight: 800; background: var(--brand); color: var(--brand-ink); }
    .tchip.add { color: var(--ink); border-style: dashed; }

    .filters { flex: none; display: grid; gap: 0.45rem; padding: 0.25rem 0.9rem 0.6rem; }
    .search { display: flex; align-items: center; gap: 0.45rem; min-height: 44px; padding: 0 0.4rem 0 0.8rem; border-radius: 999px; background: var(--ground-raised); border: 1.5px solid var(--line); color: var(--ink-faint); min-width: 0; }
    .search:focus-within { border-color: var(--brand); }
    .search input { flex: 1; min-width: 0; background: none; border: 0; outline: none; color: var(--ink); font: inherit; font-size: 16px; }
    .search input::-webkit-search-cancel-button { display: none; }
    .clear { width: 36px; height: 36px; border: 0; border-radius: 999px; background: none; color: var(--ink-muted); display: grid; place-items: center; }
    .find { display: flex; gap: 0.5rem; }
    .find .search { flex: 1; }
    .mic { flex: none; width: 48px; height: 48px; margin: -2px 0; border-radius: 999px; border: 0; display: grid; place-items: center; background: var(--brand); color: var(--brand-ink); touch-action: manipulation; }
    .cats { display: flex; gap: 0.4rem; overflow-x: auto; scrollbar-width: none; }
    .cats .chip { flex: none; }

    .grid-wrap { flex: 1; overflow: auto; overscroll-behavior: contain; padding: 0 0.9rem 1rem; }
    .grid { list-style: none; margin: 0; padding: 0; display: grid; gap: 0.55rem; grid-template-columns: repeat(auto-fill, minmax(min(9.5rem, calc(50% - 0.3rem)), 1fr)); }
    .tile-wrap { position: relative; }
    .tile { position: relative; width: 100%; min-height: 6.6rem; display: flex; flex-direction: column; align-items: flex-start; gap: 0.25rem; padding: 0.75rem 0.8rem 0.7rem; border-radius: 16px; border: 2px solid var(--line); background: var(--ground-raised); color: var(--ink); text-align: left; font: inherit; touch-action: manipulation; -webkit-tap-highlight-color: transparent; user-select: none; transition: transform 90ms, border-color 120ms; }
    .tile.in { border-color: color-mix(in oklab, var(--brand) 70%, transparent); background: color-mix(in oklab, var(--brand) 8%, var(--ground-raised)); }
    .tile:active:not(:disabled) { transform: scale(0.96); border-color: var(--brand); }
    .tile:disabled { opacity: 0.4; }
    .name { font-family: var(--font-label); text-transform: uppercase; font-weight: 500; line-height: 1.15; padding-right: 1.9rem; }
    .price { margin-top: auto; font-family: var(--font-label); color: var(--brand); font-weight: 600; }
    .hint { font-size: 0.75rem; color: var(--ink-faint); }
    .count { position: absolute; right: 0.55rem; bottom: 0.55rem; min-width: 2rem; height: 2rem; font-size: 1rem; box-shadow: 0 0 0 3px var(--ground); pointer-events: none; }
    .less { position: absolute; right: 2.9rem; bottom: 0.45rem; width: 40px; height: 40px; border-radius: 999px; border: 2px solid var(--line); display: grid; place-items: center; background: var(--ground-sunken); color: var(--ink); touch-action: manipulation; }
    .more { position: absolute; top: 0.3rem; right: 0.3rem; width: 40px; height: 40px; border-radius: 999px; border: 0; display: grid; place-items: center; background: rgb(255 255 255 / 0.07); color: var(--ink-muted); touch-action: manipulation; }

    .cart { flex: none; background: var(--ground-sunken); border-top: 1px solid var(--line); padding: 0.6rem 0.9rem calc(0.7rem + env(safe-area-inset-bottom)); }
    .lines-head { display: flex; justify-content: space-between; align-items: center; }
    .lines-head .label { color: var(--ink-muted); }
    .lines { list-style: none; margin: 0 0 0.5rem; padding: 0; max-height: 42dvh; overflow: auto; }
    .lines li { display: grid; grid-template-columns: 1fr auto auto; gap: 0.6rem; align-items: center; padding: 0.55rem 0; border-bottom: 1px solid var(--line); }
    .line-main { display: flex; flex-direction: column; min-width: 0; }
    .line-name { font-family: var(--font-label); text-transform: uppercase; }
    .line-desc { color: var(--ink-muted); font-size: 0.85rem; }
    .line-note { color: var(--brand-bright); font-size: 0.85rem; }
    .line-total { font-size: 0.92rem; color: var(--ink-muted); }
    .stepper { display: flex; align-items: center; gap: 0.2rem; }
    .icon-btn.sm { width: 40px; height: 40px; }
    .qty { min-width: 1.3rem; text-align: center; font-weight: 700; }
    .bar { display: flex; gap: 0.6rem; align-items: center; }
    .summary { flex: 1; min-width: 0; display: flex; align-items: center; gap: 0.5rem; min-height: 54px; padding: 0 0.4rem; border: 0; border-radius: 14px; background: none; color: var(--ink); font: inherit; white-space: nowrap; }
    .summary:disabled { color: var(--ink-faint); }
    .summary strong { margin-left: auto; font-family: var(--font-label); font-size: 1.2rem; color: var(--brand); }
    .flip { transform: rotate(180deg); }
    .send { flex: none; min-height: 54px; padding: 0 1.1rem; white-space: nowrap; }

    .heard { margin-bottom: 0.5rem; padding: 0.45rem 0.4rem 0.5rem 0.7rem; border-radius: 14px; background: color-mix(in oklab, var(--brand) 12%, var(--ground-raised)); }
    .heard-head { display: flex; align-items: center; gap: 0.45rem; color: var(--ink); font-size: 0.92rem; }
    .heard-head span { flex: 1; min-width: 0; }
    .heard-head .icon-btn.sm { width: 36px; height: 36px; }
    .issues { list-style: none; margin: 0.3rem 0 0; padding: 0; display: grid; gap: 0.3rem; }
    .issue { display: block; width: 100%; text-align: left; font: inherit; font-size: 0.88rem; color: var(--ink-muted); padding: 0.25rem 0; background: none; border: 0; }
    .issue small { color: var(--ink-faint); }
    .issue.fix { min-height: 40px; padding: 0.4rem 0.7rem; border-radius: 10px; border: 1.5px dashed var(--brand); color: var(--brand); font-weight: 600; }

    .voice-body { padding: 0.2rem 1.1rem calc(1.2rem + env(safe-area-inset-bottom)); display: grid; gap: 0.7rem; }
    .voice-hint { margin: 0; color: var(--ink-muted); font-size: 0.92rem; }
    .voice-hint em { color: var(--ink); font-style: normal; }
    .voice textarea { width: 100%; min-height: 6.5rem; resize: vertical; padding: 0.7rem 0.8rem; border-radius: 14px; border: 1.5px solid var(--line); background: var(--ground-raised); color: var(--ink); font: inherit; font-size: 16px; }
    .voice textarea:focus { outline: none; border-color: var(--brand); }
    .voice-actions { display: flex; gap: 0.6rem; flex-wrap: wrap; }
    .voice-actions .btn { flex: 1 1 9rem; min-height: 52px; }
    .listen { border: 2px solid var(--brand); color: var(--brand); background: none; }
    .listen.on { background: var(--brand); color: var(--brand-ink); animation: pulse 1.2s ease-in-out infinite; }
    @keyframes pulse { 50% { box-shadow: 0 0 0 8px color-mix(in oklab, var(--brand) 25%, transparent); } }
    @media (prefers-reduced-motion: reduce) { .listen.on { animation: none; } }
    .voice-foot { margin: 0; font-size: 0.8rem; color: var(--ink-faint); }

    .picker-scrim { z-index: 55; }
    .sheet-head { display: flex; align-items: center; justify-content: space-between; padding: 1.1rem 1.1rem 0.4rem; }
    .sheet-head h2 { margin: 0; font-size: 1.6rem; }
    .pick { list-style: none; margin: 0; padding: 0.4rem 1.1rem calc(1.2rem + env(safe-area-inset-bottom)); display: grid; gap: 0.55rem; grid-template-columns: repeat(auto-fill, minmax(4.6rem, 1fr)); overflow: auto; }
    .pick-btn { width: 100%; min-height: 4.6rem; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 0.1rem; border-radius: 16px; border: 2px solid var(--line); background: var(--ground-raised); color: var(--ink); }
    .pick-btn .display { font-size: 2rem; line-height: 1; }
    .pick-btn small { font-size: 0.72rem; color: var(--ink-faint); }
    .pick-btn.busy { border-color: color-mix(in oklab, var(--brand) 55%, transparent); }
    .pick-btn.busy .display { color: var(--brand); }
    .pick-btn.current { background: color-mix(in oklab, var(--brand) 16%, var(--ground-raised)); border-color: var(--brand); }
  `
})
export class OrderComposerComponent implements OnInit, OnDestroy {
  readonly store = inject(FloorStore);
  private api = inject(ApiService);

  readonly category = signal<number | null>(null);
  readonly selected = signal<Dish | null>(null);
  readonly expanded = signal(false);
  readonly picking = signal(false);
  readonly sending = signal(false);
  readonly error = signal<string | null>(null);
  private readonly querySignal = signal('');

  readonly voiceOpen = signal(false);
  readonly listening = signal(false);
  readonly understanding = signal(false);
  readonly voiceError = signal<string | null>(null);
  readonly voiceResult = signal<{ summary: string; issues: VoiceIssue[] } | null>(null);
  voiceText = '';
  readonly canListen = !!speechRecognition();
  private recognition: SpeechRecognitionLike | null = null;

  get query() { return this.querySignal(); }
  set query(value: string) { this.querySignal.set(value); }

  readonly price = price;
  readonly describe = describe;
  readonly lineTotal = lineTotal;
  readonly needsChoices = needsChoices;
  readonly X = X;
  readonly Search = Search;
  readonly Ellipsis = Ellipsis;
  readonly Plus = Plus;
  readonly Minus = Minus;
  readonly Trash2 = Trash2;
  readonly Send = Send;
  readonly ChevronUp = ChevronUp;
  readonly Mic = Mic;
  readonly Sparkles = Sparkles;

  readonly menu = computed(() => this.store.menu()!);
  readonly tableId = computed(() => this.store.composing()!);
  readonly tableLabel = computed(() => this.number(this.tableId()));
  readonly lines = computed(() => this.store.draft(this.tableId()));
  /** The tables with a draft, plus the open one even while it is still empty. */
  readonly strip = computed(() => {
    const drafts = this.store.draftTables();
    const current = this.tableId();
    return drafts.some((d) => d.tableId === current) ? drafts : [...drafts, { tableId: current, count: 0 }];
  });

  readonly categories = computed(() => this.menu().categories);
  readonly visible = computed(() => {
    const q = fold(this.querySignal().trim());
    const cat = this.category();
    return this.menu().dishes.filter((d) =>
      (q ? fold(d.name).includes(q) : cat === null || d.categoryId === cat));
  });
  readonly count = computed(() => cartCount(this.lines()));
  readonly total = computed(() => cartTotal(this.lines()));
  private readonly perDish = computed(() => {
    const map = new Map<number, number>();
    for (const l of this.lines()) map.set(l.dish.id, (map.get(l.dish.id) ?? 0) + l.qty);
    return map;
  });

  ngOnInit() {
    document.body.style.overflow = 'hidden';
  }

  ngOnDestroy() {
    document.body.style.overflow = '';
    this.recognition?.abort();
  }

  dishById(id: number | null): Dish | null {
    return this.menu().dishes.find((d) => d.id === id) ?? null;
  }

  openVoice() {
    this.voiceText = '';
    this.voiceError.set(null);
    this.voiceOpen.set(true);
    if (this.canListen) this.listen();
  }

  closeVoice() {
    this.recognition?.abort();
    this.voiceOpen.set(false);
  }

  /** One phrase at a time: it stops on a pause, and tapping again adds to the text. */
  listen() {
    const Recognition = speechRecognition();
    if (!Recognition || this.recognition) return;
    const before = this.voiceText.trim();
    const rec = new Recognition();
    rec.lang = 'es-CO';
    rec.interimResults = true;
    rec.continuous = false;
    rec.onresult = (e) => {
      let said = '';
      for (let i = 0; i < e.results.length; i++) said += e.results[i][0].transcript;
      this.voiceText = [before, said.trim()].filter(Boolean).join(', ');
    };
    rec.onerror = (e) => {
      if (e.error === 'not-allowed' || e.error === 'service-not-allowed') {
        this.voiceError.set('El navegador no tiene permiso para el micrófono. Actívalo o dicta con el teclado.');
      } else if (e.error !== 'no-speech' && e.error !== 'aborted') {
        this.voiceError.set('No se pudo escuchar. Inténtalo de nuevo o escribe el pedido.');
      }
    };
    rec.onend = () => {
      this.recognition = null;
      this.listening.set(false);
    };
    this.voiceError.set(null);
    this.recognition = rec;
    this.listening.set(true);
    navigator.vibrate?.(15);
    rec.start();
  }

  stopListening() {
    this.recognition?.stop();
  }

  understand() {
    const text = this.voiceText.trim();
    if (!text) return;
    this.recognition?.abort();
    this.understanding.set(true);
    this.voiceError.set(null);
    this.api.voiceOrder(text).subscribe({
      next: (order) => {
        this.understanding.set(false);
        const { tableId, unknownTable, added } = this.store.applyVoice(order, this.tableId());
        if (tableId !== this.tableId()) this.switchTo(tableId);
        const parts = [added ? `Mesa ${this.number(tableId)}: ${added} ${added === 1 ? 'ítem agregado' : 'ítems agregados'}` : 'No agregué nada'];
        if (unknownTable) parts.push(`no hay mesa ${order.tableNumber}`);
        if (order.issues.length) parts.push(`${order.issues.length} por revisar`);
        this.voiceResult.set({ summary: parts.join(' · '), issues: order.issues });
        this.voiceOpen.set(false);
        if (added) this.expanded.set(true);
      },
      error: (e) => {
        this.understanding.set(false);
        this.voiceError.set(errorMessage(e));
      }
    });
  }

  number(tableId: number): string {
    return String(this.store.tableNumber(tableId) ?? '?');
  }

  inCart(dishId: number): number {
    return this.perDish().get(dishId) ?? 0;
  }

  hasExtras(d: Dish): boolean {
    return d.modifiers.length > 0 || d.ingredients.some((i) => i.removable);
  }

  switchTo(tableId: number) {
    this.store.compose(tableId);
    this.picking.set(false);
    this.expanded.set(false);
    this.error.set(null);
  }

  tap(d: Dish) {
    if (needsChoices(d)) {
      this.selected.set(d);
      return;
    }
    this.update((ls) => addToCart(ls, newLine(d)));
    navigator.vibrate?.(15);
  }

  /** Takes back the dish added last, so − on a tile undoes the last + on it. */
  removeOne(d: Dish) {
    const lines = this.lines();
    const last = [...lines].reverse().find((l) => l.dish.id === d.id);
    if (last) this.changeQty(last, -1);
    navigator.vibrate?.(10);
  }

  add(line: CartLine) {
    this.update((ls) => addToCart(ls, line));
    this.selected.set(null);
    // A dictated dish whose options were missing is done once they are picked
    const r = this.voiceResult();
    const fixed = r?.issues.findIndex((i) => i.code === 'NEEDS_CHOICES' && i.dishId === line.dish.id) ?? -1;
    if (r && fixed >= 0) this.voiceResult.set({ ...r, issues: r.issues.filter((_, i) => i !== fixed) });
  }

  changeQty(line: CartLine, delta: number) {
    this.update((ls) => ls
      .map((l) => (l.key === line.key ? { ...l, qty: l.qty + delta } : l))
      .filter((l) => l.qty > 0));
    if (!this.lines().length) this.expanded.set(false);
  }

  clear() {
    this.store.setDraft(this.tableId(), []);
    this.expanded.set(false);
  }

  send() {
    const tableId = this.tableId();
    this.sending.set(true);
    this.error.set(null);
    this.store.sendDraft(tableId).subscribe({
      next: () => {
        this.sending.set(false);
        this.expanded.set(false);
        this.voiceResult.set(null);
        // Next table still waiting, or done
        const next = this.store.draftTables()[0];
        if (next) this.switchTo(next.tableId);
        else this.store.composing.set(null);
      },
      error: (e) => {
        this.sending.set(false);
        this.error.set(errorMessage(e));
      }
    });
  }

  private update(change: (lines: CartLine[]) => CartLine[]) {
    this.store.setDraft(this.tableId(), change(this.lines()));
  }
}

// The Web Speech API is not in the TypeScript DOM types yet; this is the part used here.
interface SpeechRecognitionLike {
  lang: string;
  interimResults: boolean;
  continuous: boolean;
  onresult: ((e: { results: ArrayLike<ArrayLike<{ transcript: string }>> }) => void) | null;
  onerror: ((e: { error: string }) => void) | null;
  onend: (() => void) | null;
  start(): void;
  stop(): void;
  abort(): void;
}

function speechRecognition(): (new () => SpeechRecognitionLike) | null {
  const w = globalThis as unknown as Record<string, unknown>;
  return (w['SpeechRecognition'] ?? w['webkitSpeechRecognition'] ?? null) as (new () => SpeechRecognitionLike) | null;
}
