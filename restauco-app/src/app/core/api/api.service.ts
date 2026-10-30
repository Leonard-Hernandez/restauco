import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { appConfig } from '../config/app-config';
import {
  Category, Dish, DishInput, GuestTable, Ingredient, Member, Menu, OrderLine, PaymentMethod, PaymentPart, ReportPeriod, Role, SalesReport,
  Session, Staff, Tag, TableDetail, TableSummary, Tenant, Ticket, DiningTable, VoiceOrder,
  Delivery, DeliveryInfo, DeliveryOrderInput, DeliveryQuote, DeliverySettings, DeliveryTrack
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {

  private http = inject(HttpClient);
  private get base() {
    return `${appConfig().apiUrl}/api`;
  }

  // Public
  tenant(): Observable<Tenant> {
    return this.http.get<Tenant>(`${this.base}/public/tenant`);
  }
  menu(): Observable<Menu> {
    return this.http.get<Menu>(`${this.base}/public/menu`);
  }
  guestTable(token: string): Observable<GuestTable> {
    return this.http.get<GuestTable>(`${this.base}/public/tables/${token}`);
  }
  guestOrder(token: string, lines: OrderLine[]): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.base}/public/tables/${token}/orders`, { lines });
  }

  // Delivery (public). Addresses go in the body, never in the URL.
  deliveryInfo(): Observable<DeliveryInfo> {
    return this.http.get<DeliveryInfo>(`${this.base}/public/delivery`);
  }
  searchAddress(address: string): Observable<DeliveryQuote[]> {
    return this.http.post<DeliveryQuote[]>(`${this.base}/public/delivery/locate`, { address });
  }
  placeAt(lat: number, lng: number): Observable<DeliveryQuote[]> {
    return this.http.post<DeliveryQuote[]>(`${this.base}/public/delivery/locate`, { lat, lng });
  }
  orderDelivery(order: DeliveryOrderInput): Observable<DeliveryTrack> {
    return this.http.post<DeliveryTrack>(`${this.base}/public/delivery/orders`, order);
  }
  trackDelivery(token: string): Observable<DeliveryTrack> {
    return this.http.get<DeliveryTrack>(`${this.base}/public/delivery/orders/${token}`);
  }

  // Panel
  staff(): Observable<Staff> {
    return this.http.get<Staff>(`${this.base}/panel/me`);
  }

  // Floor (waiter)
  floor(): Observable<TableSummary[]> {
    return this.http.get<TableSummary[]>(`${this.base}/panel/floor/tables`);
  }
  floorTickets(): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(`${this.base}/panel/floor/tickets`);
  }
  tableDetail(id: number): Observable<TableDetail> {
    return this.http.get<TableDetail>(`${this.base}/panel/floor/tables/${id}`);
  }
  waiterOrder(tableId: number, lines: OrderLine[]): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.base}/panel/floor/tables/${tableId}/orders`, { lines });
  }
  /** Text from the phone's dictation -> a draft to review. Nothing is sent to the kitchen. */
  voiceOrder(text: string): Observable<VoiceOrder> {
    return this.http.post<VoiceOrder>(`${this.base}/panel/floor/voice-orders`, { text });
  }
  voiceSent(sampleId: number, ticketId: number): Observable<void> {
    return this.http.post<void>(`${this.base}/panel/floor/voice-orders/${sampleId}/sent`, { ticketId });
  }
  approve(ticketId: number): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.base}/panel/floor/tickets/${ticketId}/approve`, {});
  }
  deliver(ticketId: number): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.base}/panel/floor/tickets/${ticketId}/deliver`, {});
  }
  cancel(ticketId: number, reason: string | null): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.base}/panel/floor/tickets/${ticketId}/cancel`, { reason });
  }
  /** One part pays the whole bill; several split it and must add up to the total. */
  closeSession(sessionId: number, payments: PaymentPart[]): Observable<Session> {
    const body = payments.length === 1 ? { paymentMethodId: payments[0].paymentMethodId } : { payments };
    return this.http.post<Session>(`${this.base}/panel/floor/sessions/${sessionId}/close`, body);
  }
  deliveries(): Observable<Delivery[]> {
    return this.http.get<Delivery[]>(`${this.base}/panel/floor/deliveries`);
  }
  activePaymentMethods(): Observable<PaymentMethod[]> {
    return this.http.get<PaymentMethod[]>(`${this.base}/panel/floor/payment-methods`);
  }

  // Kitchen
  kitchenTickets(): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(`${this.base}/panel/kitchen/tickets`);
  }
  startTicket(id: number): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.base}/panel/kitchen/tickets/${id}/start`, {});
  }
  readyTicket(id: number): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.base}/panel/kitchen/tickets/${id}/ready`, {});
  }
  setAvailability(dishId: number, available: boolean): Observable<Dish> {
    return this.http.patch<Dish>(`${this.base}/panel/kitchen/dishes/${dishId}/availability`, { available });
  }
  setIngredientAvailability(id: number, available: boolean): Observable<Ingredient> {
    return this.http.patch<Ingredient>(`${this.base}/panel/kitchen/ingredients/${id}/availability`, { available });
  }

  // Admin (owner)
  fullMenu(): Observable<Menu> {
    return this.http.get<Menu>(`${this.base}/panel/admin/menu`);
  }
  saveCategory(c: Omit<Category, 'id'> & { id?: number }): Observable<Category> {
    const { id, ...body } = c;
    return id
      ? this.http.put<Category>(`${this.base}/panel/admin/categories/${id}`, body)
      : this.http.post<Category>(`${this.base}/panel/admin/categories`, body);
  }
  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/panel/admin/categories/${id}`);
  }
  saveDish(dish: DishInput, id?: number): Observable<Dish> {
    return id
      ? this.http.put<Dish>(`${this.base}/panel/admin/dishes/${id}`, dish)
      : this.http.post<Dish>(`${this.base}/panel/admin/dishes`, dish);
  }
  deleteDish(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/panel/admin/dishes/${id}`);
  }
  uploadDishImage(id: number, file: File): Observable<Dish> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<Dish>(`${this.base}/panel/admin/dishes/${id}/image`, form);
  }
  removeDishImage(id: number): Observable<Dish> {
    return this.http.delete<Dish>(`${this.base}/panel/admin/dishes/${id}/image`);
  }
  saveIngredient(i: { name: string; allergen: boolean }, id?: number): Observable<Ingredient> {
    return id
      ? this.http.put<Ingredient>(`${this.base}/panel/admin/ingredients/${id}`, i)
      : this.http.post<Ingredient>(`${this.base}/panel/admin/ingredients`, i);
  }
  deleteIngredient(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/panel/admin/ingredients/${id}`);
  }
  saveTag(name: string, id?: number): Observable<Tag> {
    return id
      ? this.http.put<Tag>(`${this.base}/panel/admin/tags/${id}`, { name })
      : this.http.post<Tag>(`${this.base}/panel/admin/tags`, { name });
  }
  deleteTag(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/panel/admin/tags/${id}`);
  }
  tables(): Observable<DiningTable[]> {
    return this.http.get<DiningTable[]>(`${this.base}/panel/admin/tables`);
  }
  saveTable(t: { number: number; label: string | null; active: boolean }, id?: number): Observable<DiningTable> {
    return id
      ? this.http.put<DiningTable>(`${this.base}/panel/admin/tables/${id}`, t)
      : this.http.post<DiningTable>(`${this.base}/panel/admin/tables`, t);
  }
  regenerateQr(id: number): Observable<DiningTable> {
    return this.http.post<DiningTable>(`${this.base}/panel/admin/tables/${id}/qr`, {});
  }
  paymentMethods(): Observable<PaymentMethod[]> {
    return this.http.get<PaymentMethod[]>(`${this.base}/panel/admin/payment-methods`);
  }
  savePaymentMethod(m: { name: string; position: number; active: boolean }, id?: number): Observable<PaymentMethod> {
    return id
      ? this.http.put<PaymentMethod>(`${this.base}/panel/admin/payment-methods/${id}`, m)
      : this.http.post<PaymentMethod>(`${this.base}/panel/admin/payment-methods`, m);
  }
  members(): Observable<Member[]> {
    return this.http.get<Member[]>(`${this.base}/panel/admin/members`);
  }
  invite(email: string, role: Role): Observable<Member> {
    return this.http.post<Member>(`${this.base}/panel/admin/members`, { email, role });
  }
  changeRole(id: number, role: Role): Observable<Member> {
    return this.http.put<Member>(`${this.base}/panel/admin/members/${id}`, { role });
  }
  removeMember(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/panel/admin/members/${id}`);
  }

  deliverySettings(): Observable<DeliverySettings> {
    return this.http.get<DeliverySettings>(`${this.base}/panel/admin/delivery`);
  }
  saveDeliverySettings(settings: DeliverySettings): Observable<DeliverySettings> {
    return this.http.put<DeliverySettings>(`${this.base}/panel/admin/delivery`, settings);
  }

  // --- Owner: statistics ---
  salesReport(period: ReportPeriod): Observable<SalesReport> {
    return this.http.get<SalesReport>(`${this.base}/panel/admin/reports/sales`, { params: { period } });
  }
}

const MESSAGES: Record<string, string> = {
  DISH_UNAVAILABLE: 'Uno de los platos se agotó. Quítalo del pedido e inténtalo de nuevo.',
  OPTION_UNAVAILABLE: 'Uno de los rellenos u opciones se agotó. Escoge otro e inténtalo de nuevo.',
  DELIVERY_CLOSED: 'En este momento no estamos tomando domicilios. Llámanos o vuelve más tarde.',
  OUT_OF_ZONE: 'Esa dirección queda fuera de nuestra zona de domicilios.',
  PLACE_NOT_FOUND: 'El mapa no tiene una dirección en ese punto. Mueve el marcador hasta tu casa o edificio.',
  GEOCODER_UNAVAILABLE: 'No pudimos revisar la dirección en el mapa ahora mismo. Inténtalo en un minuto.',
  ADDRESS_REQUIRED: 'Escribe la calle y el número de tu dirección.',
  INVALID_LOCATION: 'Marca en el mapa dónde te llevamos el pedido.',
  NAME_REQUIRED: 'Escribe tu nombre.',
  INVALID_PHONE: 'Escribe un celular colombiano de 10 dígitos, por ejemplo 300 123 4567.',
  BELOW_MINIMUM: 'Tu pedido no alcanza el mínimo para domicilio.',
  CASH_TOO_LOW: 'El billete con el que pagas debe cubrir el total.',
  TOO_MANY_DELIVERIES: 'Ya hiciste varios pedidos con este número en la última hora. Llámanos si necesitas algo más.',
  TOO_MANY_REQUESTS: 'Demasiados intentos seguidos. Espera unos minutos e inténtalo de nuevo.',
  DELIVERY_NOT_FOUND: 'No encontramos ese pedido.',
  INVALID_RADIUS: 'La zona de reparto debe ser de más de 0 y hasta 30 km.',
  INVALID_FEE: 'Los valores no pueden ser negativos.',
  INGREDIENT_NOT_FOUND: 'Ese ingrediente ya no existe.',
  INVALID_MODIFIERS: 'Revisa las opciones del plato: cada grupo necesita nombre, opciones y un mínimo y máximo posibles.',
  INVALID_REMOVAL: 'Ese ingrediente no se puede quitar de este plato.',
  INVALID_QTY: 'La cantidad debe estar entre 1 y 20.',
  NOTE_TOO_LONG: 'La nota es muy larga (máximo 140 caracteres).',
  EMPTY_ORDER: 'El pedido está vacío.',
  TOO_MANY_PENDING: 'Esta mesa ya tiene pedidos esperando al mesero. Espera a que los confirme.',
  TABLE_NOT_FOUND: 'Este código QR ya no es válido. Pide uno nuevo al mesero.',
  TABLE_INACTIVE: 'Esta mesa no está recibiendo pedidos. Llama al mesero.',
  INVALID_TICKET_STATE: 'Ese pedido ya cambió de estado. La pantalla se actualizó.',
  TICKET_NOT_FOUND: 'No encontramos ese pedido.',
  SESSION_HAS_ACTIVE_TICKETS: 'Entrega o cancela todos los pedidos antes de cerrar la cuenta.',
  SESSION_CLOSED: 'Esa cuenta ya estaba cerrada.',
  PAYMENT_METHOD_NOT_FOUND: 'Ese método de pago ya no está disponible.',
  PAYMENTS_DO_NOT_MATCH_TOTAL: 'Los pagos no suman el total de la cuenta. Revisa los montos.',
  INVALID_PAYMENTS: 'Revisa el pago: cada método una sola vez y con un monto mayor a cero.',
  CATEGORY_NOT_EMPTY: 'Mueve o borra los platos de esta categoría antes de borrarla.',
  DUPLICATE_NAME: 'Ya existe uno con ese nombre.',
  TABLE_NUMBER_TAKEN: 'Ya hay una mesa con ese número.',
  MEMBER_EXISTS: 'Ese correo ya está en el equipo.',
  SELF_CHANGE: 'No puedes quitarte ni cambiarte el rol a ti mismo.',
  INVALID_IMAGE: 'Sube una foto JPG, PNG o WebP de máximo 5 MB.',
  IDENTITY_UNAVAILABLE: 'No pudimos leer tu perfil de inicio de sesión. Inténtalo de nuevo.',
  TENANT_NOT_FOUND: 'No encontramos este restaurante.',
  VALIDATION_ERROR: 'Revisa los datos e inténtalo de nuevo.'
};

/** Spanish message for an API error, with a recovery when one exists. */
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const code = error.error?.code as string | undefined;
    if (code && MESSAGES[code]) return MESSAGES[code];
    if (error.status === 0) return 'Sin conexión con el servidor. Revisa tu internet e inténtalo de nuevo.';
    if (error.status === 403) return 'Tu cuenta no tiene acceso a esta sección.';
  }
  return 'Algo salió mal. Inténtalo de nuevo.';
}

/** Absolute URL for a dish photo (relative ones live under the API). */
export function imageSrc(url: string | null): string | null {
  if (!url) return null;
  return /^(https?:)?\//.test(url) ? url : `${appConfig().apiUrl}/${url}`;
}
