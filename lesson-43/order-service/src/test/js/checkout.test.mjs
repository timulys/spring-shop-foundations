import { test } from "node:test";
import assert from "node:assert/strict";
import { OrderIntentStore } from "../../main/resources/static/order-intent.js";

// DOM stand-ins verify the event handlers and network effects. Visual/browser QA is separate.
class Element {
  listeners = new Map(); dataset = {}; hidden = false; disabled = false; textContent = "";
  addEventListener(type, handler) { this.listeners.set(type, handler); }
  async click() { await this.listeners.get("click")?.(); }
  querySelectorAll() { return []; }
  replaceChildren() {}
  setAttribute() {}
}
const elements = new Map();
const element = selector => { if (!elements.has(selector)) elements.set(selector, new Element()); return elements.get(selector); };
const events = new Map();
globalThis.document = {
  querySelector: element,
  addEventListener: (type, handler) => { const list = events.get(type) ?? []; list.push(handler); events.set(type, list); },
  dispatchEvent: event => { for (const handler of events.get(event.type) ?? []) handler(event); }
};
const values = new Map();
globalThis.sessionStorage = {
  getItem: key => values.get(key) ?? null,
  setItem: (key, value) => values.set(key, value),
  removeItem: key => values.delete(key)
};
const { initializeCheckout } = await import("../../main/resources/static/checkout.js");
const key = "checkout-replay-key-0001";
const body = { cartRevision: 2, expectedTotalWon: 70000 };
function pending() {
  values.clear(); new OrderIntentStore(sessionStorage, 10, () => key).begin(body);
  initializeCheckout(10);
}
const token = () => new Response(JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "fresh" }));
const emptyCart = () => new Response(JSON.stringify({ items: [], totalQuantity: 0, subtotal: 0, revision: 3 }));
const order = status => new Response(JSON.stringify({ id: 99, total: 70000, status }), { status: 201 });
async function withFetch(fake, action) {
  const saved = globalThis.fetch; globalThis.fetch = fake;
  try { await action(); } finally { globalThis.fetch = saved; }
}

test("응답 유실 뒤 같은 키와 본문으로 다시 보내고 성공 후 기록을 지운다", async () => {
  pending(); let sends = 0;
  assert.equal(element("#retry-order").hidden, false);
  assert.equal(element("#preview-order").disabled, true);
  await withFetch(async (path, options) => {
    if (path === "/api/csrf") return token();
    if (path === "/api/cart") return emptyCart();
    assert.equal(path, "/api/orders"); sends++;
    assert.equal(options.headers["Idempotency-Key"], key);
    assert.deepEqual(JSON.parse(options.body), body);
    if (sends === 1) throw new TypeError("response lost after server commit");
    return order("CONFIRMED");
  }, async () => {
    await element("#retry-order").click();
    assert.equal(sends, 1); assert.equal(new OrderIntentStore(sessionStorage, 10).pending.key, key);
    await element("#retry-order").click();
  });
  assert.equal(sends, 2); assert.equal(new OrderIntentStore(sessionStorage, 10).pending, null);
  assert.equal(element("#retry-order").hidden, true);
  assert.equal(element("#completed-order-id").textContent, 99);
  assert.match(element("#checkout-status").textContent, /주문이 확인/);
});

test("503과 인증 실패는 요청을 보관하고 금액 충돌 409는 새 확인을 요구한다", async () => {
  for (const status of [503, 401, 403, 409]) {
    pending();
    await withFetch(async path => path === "/api/csrf" ? token()
      : new Response(JSON.stringify({ code: "EXPECTED_ERROR", message: "처리 결과" }), { status }),
    async () => element("#retry-order").click());
    assert.equal(new OrderIntentStore(sessionStorage, 10).pending !== null, status !== 409);
    assert.equal(element("#retry-order").hidden, status === 409);
  }
});

test("취소된 주문의 재전송 결과를 새 결제 완료로 표시하지 않는다", async () => {
  pending();
  await withFetch(async path => path === "/api/csrf" ? token() : path === "/api/cart" ? emptyCart() : order("CANCELLED"),
    async () => element("#retry-order").click());
  assert.match(element("#completed-order-state").textContent, /이미 취소/);
  assert.match(element("#checkout-status").textContent, /이미 취소/);
});

test("다른 회원의 화면에는 이전 회원의 재전송 버튼을 표시하지 않는다", () => {
  pending(); initializeCheckout(20);
  assert.equal(element("#retry-order").hidden, true);
  assert.equal(new OrderIntentStore(sessionStorage, 10).pending.key, key);
});
