import { test } from "node:test";
import assert from "node:assert/strict";
import { OrderIntentStore } from "../../main/resources/static/order-intent.js";
function storage() {
  const values = new Map();
  return { getItem: key => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value), removeItem: key => values.delete(key) };
}
const body = { cartRevision: 2, expectedTotalWon: 70000 };
const key = "12345678-1234-1234-1234-123456789abc";
test("응답 유실 뒤에도 새 인스턴스가 같은 키와 본문을 복원한다", () => {
  const memory = storage();
  const first = new OrderIntentStore(memory, 10, () => key).begin(body);
  const reloaded = new OrderIntentStore(memory, 10, () => { throw new Error("new key not allowed"); });
  assert.deepEqual(reloaded.pending, first);
  assert.deepEqual(reloaded.begin(body), first);
});
test("한 회원의 미확인 주문을 다른 회원에게 넘기지 않는다", () => {
  const memory = storage(); new OrderIntentStore(memory, 10, () => key).begin(body);
  assert.equal(new OrderIntentStore(memory, 20).pending, null);
});
test("미확인 요청이 있으면 다른 내용으로 덮어쓰지 않는다", () => {
  const store = new OrderIntentStore(storage(), 10, () => key); store.begin(body);
  assert.throws(() => store.begin({ ...body, cartRevision: 3 }));
  assert.deepEqual(store.pending.body, body);
});
test("결과를 확인한 뒤 지우면 다음 주문은 새 키를 사용한다", () => {
  let sequence = 0; const memory = storage();
  const store = new OrderIntentStore(memory, 10, () => key + sequence++);
  const first = store.begin(body); store.clear();
  assert.equal(new OrderIntentStore(memory, 10).pending, null);
  assert.notEqual(store.begin(body).key, first.key);
});
test("저장 실패와 손상된 기록을 새 요청으로 덮지 않는다", () => {
  const memory = storage(); memory.setItem = () => { throw new Error("quota"); };
  const store = new OrderIntentStore(memory, 10, () => key);
  assert.throws(() => store.begin(body)); assert.equal(store.pending, null);
  assert.throws(() => new OrderIntentStore({ getItem: () => "{}" }, 10));
});
