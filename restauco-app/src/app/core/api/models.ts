/** restauco API contracts (see the backend DTOs). Prices are whole pesos. */

export interface Tenant {
  slug: string;
  name: string;
  timezone: string;
  contactPhone: string | null;
  address: string | null;
  branding: Branding;
}

/** Free-form profile the owner keeps in the tenant row; every field is optional. */
export interface Branding {
  tagline?: string;
  hours?: string;
  phones?: string[];
  delivery?: boolean;
  paymentNote?: string;
  mapsUrl?: string;
  instagram?: string;
  /** CSS token overrides, e.g. { "brand": "#ffc20e" } */
  theme?: Record<string, string>;
}

export interface Category {
  id: number;
  name: string;
  description: string | null;
  position: number;
  active: boolean;
}

export interface Ingredient {
  id: number;
  name: string;
  allergen: boolean;
  /** false = ran out today */
  available: boolean;
}

export interface Tag {
  id: number;
  name: string;
}

export interface DishIngredient {
  id: number;
  name: string;
  allergen: boolean;
  /** The guest may ask for the dish without it. */
  removable: boolean;
  /** false = ran out; if removable the dish comes without it */
  available: boolean;
}

export interface ModifierOption {
  id: string;
  name: string;
  price: number;
  /** What it is made with: it cannot be chosen once one of them runs out */
  ingredientIds: number[];
  available: boolean;
}

export interface ModifierGroup {
  id: string;
  name: string;
  min: number;
  max: number;
  options: ModifierOption[];
}

export interface Dish {
  id: number;
  categoryId: number;
  name: string;
  description: string | null;
  price: number;
  /** Relative to the API base (media/...) or absolute; null = no photo. */
  imageUrl: string | null;
  /** The owner's switch: false = marked sold out by hand */
  available: boolean;
  /** Can be ordered now: not marked sold out and nothing it needs ran out */
  orderable: boolean;
  /** What ran out that stops it (an ingredient or a choice) */
  soldOutReason: string | null;
  active: boolean;
  position: number;
  ingredients: DishIngredient[];
  tagIds: number[];
  modifiers: ModifierGroup[];
}

export interface Menu {
  categories: Category[];
  dishes: Dish[];
  ingredients: Ingredient[];
  tags: Tag[];
}

export type TicketStatus = 'PENDING_APPROVAL' | 'SENT' | 'PREPARING' | 'READY' | 'DELIVERED' | 'CANCELLED';
export type TicketSource = 'WAITER' | 'QR' | 'DELIVERY';

export interface TicketItem {
  dishId: number;
  name: string;
  unitPrice: number;
  qty: number;
  lineTotal: number;
  note: string | null;
  removed: string[];
  modifiers: { group: string; name: string; price: number }[];
}

export interface Ticket {
  id: number;
  sessionId: number;
  /** null for a delivery */
  tableId: number | null;
  tableNumber: number | null;
  deliveryId: number | null;
  status: TicketStatus;
  source: TicketSource;
  items: TicketItem[];
  total: number;
  createdAt: string;
  sentAt: string | null;
  startedAt: string | null;
  readyAt: string | null;
  deliveredAt: string | null;
  cancelledAt: string | null;
  cancelReason: string | null;
}

/** What a guest or waiter sends: references and choices only, never prices. */
export interface OrderLine {
  dishId: number;
  qty: number;
  note: string | null;
  removedIngredientIds: number[];
  selections: Record<string, string[]>;
}

export interface GuestTable {
  number: number;
  name: string;
  active: boolean;
  tickets: Ticket[];
  total: number;
}

export interface TableSummary {
  id: number;
  number: number;
  name: string;
  active: boolean;
  sessionId: number | null;
  openedAt: string | null;
  pendingApproval: number;
  inKitchen: number;
  ready: number;
  total: number;
}

export interface DiningTable {
  id: number;
  number: number;
  label: string | null;
  name: string;
  qrToken: string;
  active: boolean;
}

export interface Session {
  id: number;
  tableId: number | null;
  status: 'OPEN' | 'CLOSED' | 'CANCELLED';
  openedAt: string;
  closedAt: string | null;
  paymentMethodName: string | null;
  total: number | null;
  payments: { methodId: number; name: string; amount: number }[];
  deliveryFee: number;
}

/** A dictated order as the API understood it: lines for the draft, and what it could not place. */
export interface VoiceOrder {
  sampleId: number;
  transcript: string;
  tableNumber: number | null;
  lines: { dishId: number; dishName: string; qty: number; removedIngredientIds: number[]; selections: Record<string, string[]>; note: string | null }[];
  issues: VoiceIssue[];
}

export interface VoiceIssue {
  code: 'NEEDS_CHOICES' | 'UNAVAILABLE' | 'NOT_UNDERSTOOD';
  dishId: number | null;
  dishName: string | null;
  heard: string;
}

export interface PaymentPart {
  paymentMethodId: number;
  amount: number;
}

export interface TableDetail {
  table: DiningTable;
  session: Session | null;
  tickets: Ticket[];
  total: number;
}

export interface PaymentMethod {
  id: number;
  name: string;
  position: number;
  active: boolean;
}

export type Role = 'OWNER' | 'WAITER' | 'COOK';

export interface Staff {
  email: string;
  name: string | null;
  role: Role;
}

export interface Member {
  id: number;
  email: string;
  name: string | null;
  role: Role;
  /** Signed in at least once with that email */
  linked: boolean;
}

export interface DishInput {
  categoryId: number;
  name: string;
  description: string | null;
  price: number;
  available: boolean;
  active: boolean;
  position: number;
  ingredients: { ingredientId: number; removable: boolean }[];
  tagIds: number[];
  modifiers: { id: string | null; name: string; min: number; max: number; options: { id: string | null; name: string; price: number; ingredientIds: number[] }[] }[];
}

export interface ApiError {
  code: string;
  message: string;
}

export type ReportPeriod = 'TODAY' | 'WEEK' | 'MONTH';

export interface SalesTotals {
  sales: number;
  bills: number;
  averageTicket: number;
}

export interface Breakdown {
  label: string;
  bills: number;
  sales: number;
}

export interface SalesReport {
  period: ReportPeriod;
  granularity: 'HOUR' | 'DAY';
  /** Local dates, "2026-10-20" */
  from: string;
  to: string;
  current: SalesTotals;
  /** The period before, cut at the same time of day */
  previous: SalesTotals;
  /** start is the restaurant's local time, "2026-10-20T18:00:00" */
  timeline: { start: string; sales: number; bills: number }[];
  byPaymentMethod: Breakdown[];
  byWaiter: Breakdown[];
  topDishes: { dishId: number; name: string; units: number; sales: number }[];
  kitchen: { rounds: number; fromQr: number; cancelled: number; prepMinutes: number | null; pickupMinutes: number | null };
}

// --- Delivery ---

export type DeliveryStatus = 'PENDING' | 'CONFIRMED' | 'PREPARING' | 'READY' | 'ON_THE_WAY' | 'DELIVERED' | 'CANCELLED';

export interface DeliveryInfo {
  open: boolean;
  lat: number | null;
  lng: number | null;
  radiusKm: number;
  fee: number;
  minOrder: number;
  paymentMethods: { id: number; name: string }[];
}

/** A place on the map the customer could mean, and whether the restaurant goes there. */
export interface DeliveryQuote {
  lat: number;
  lng: number;
  label: string;
  road: string | null;
  neighbourhood: string | null;
  distanceMeters: number;
  inZone: boolean;
  fee: number;
}

export interface DeliveryOrderInput {
  customerName: string;
  phone: string;
  address: string;
  addressDetail: string | null;
  lat: number;
  lng: number;
  paymentMethodId: number;
  cashFor: number | null;
  lines: OrderLine[];
}

/** What the customer sees while following the order. */
export interface DeliveryTrack {
  token: string;
  status: DeliveryStatus;
  customerName: string;
  address: string;
  addressDetail: string | null;
  placeLabel: string;
  paymentMethodName: string | null;
  cashFor: number | null;
  items: TicketItem[];
  subtotal: number;
  fee: number;
  total: number;
  createdAt: string;
  confirmedAt: string | null;
  readyAt: string | null;
  dispatchedAt: string | null;
  deliveredAt: string | null;
  cancelReason: string | null;
}

/** A delivery as the waiter sees it. */
export interface Delivery {
  id: number;
  sessionId: number;
  status: DeliveryStatus;
  customerName: string;
  phone: string;
  address: string;
  addressDetail: string | null;
  lat: number;
  lng: number;
  placeLabel: string;
  /** The street the map found at the pin is the one the customer wrote */
  matchesAddress: boolean;
  distanceMeters: number;
  paymentMethodId: number | null;
  paymentMethodName: string | null;
  cashFor: number | null;
  subtotal: number;
  fee: number;
  total: number;
  createdAt: string;
  tickets: Ticket[];
}

export interface DeliverySettings {
  enabled: boolean;
  lat: number | null;
  lng: number | null;
  radiusKm: number;
  fee: number;
  minOrder: number;
  /** Accepted for delivery; empty = every active method */
  paymentMethodIds: number[];
}
