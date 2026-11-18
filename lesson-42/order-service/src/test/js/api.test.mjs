import { test } from "node:test";
import assert from "node:assert/strict";
import { ApiError, RequestError, requestJson } from "../../main/resources/static/api.js";

async function withFetch(fake, run) {
  const original = globalThis.fetch;
  globalThis.fetch = fake;
  try { await run(); } finally { globalThis.fetch = original; }
}

test("성공 응답과 JSON 요청 본문을 전달한다", async () => {
  await withFetch(async (path, options) => {
    if (path === "/api/csrf") return new Response(JSON.stringify({headerName:"X-CSRF-TOKEN",token:"test-token"}));
    assert.equal(options.headers["X-CSRF-TOKEN"], "test-token");
    assert.equal(path, "/api/cart/items/K100");
    assert.equal(options.method, "PATCH");
    assert.deepEqual(JSON.parse(options.body), { quantity: 2 });
    assert.ok(options.signal instanceof AbortSignal);
    return new Response(JSON.stringify({ quantity: 2 }));
  }, async () => assert.deepEqual(await requestJson("/api/cart/items/K100", {
    method: "PATCH", body: { quantity: 2 }
  }), { quantity: 2 }));
});

test("400 응답의 상태와 필드 오류를 보존한다", async () => {
  await withFetch(async () => new Response(JSON.stringify({
    message: "수량 범위 오류", code: "VALIDATION_ERROR", fieldErrors: [{ field: "quantity" }]
  }), { status: 400 }), async () => {
    await assert.rejects(requestJson("/api/cart"), error => {
      assert.ok(error instanceof ApiError);
      assert.equal(error.status, 400);
      assert.equal(error.fieldErrors[0].field, "quantity");
      return true;
    });
  });
});

test("통신 실패를 성공으로 바꾸거나 자동 재요청하지 않는다", async () => {
  let calls = 0;
  await withFetch(async () => { calls++; throw new TypeError("Failed to fetch"); }, async () => {
    await assert.rejects(requestJson("/api/orders", { method: "POST" }),
      error => error instanceof RequestError && error.code === "UNCONFIRMED");
  });
  assert.equal(calls, 1);
});

test("제한 시간 뒤 대기를 중단하고 TIMEOUT을 전달한다", async () => {
  let signal;
  await withFetch((path, options) => new Promise((resolve, reject) => {
    signal = options.signal;
    signal.addEventListener("abort", () => reject(new DOMException("Aborted", "AbortError")));
  }), async () => await assert.rejects(requestJson("/api/cart", { timeoutMs: 20 }),
    error => error instanceof RequestError && error.code === "TIMEOUT"));
  assert.equal(signal.aborted, true);
});

test("204 응답은 본문을 읽지 않고 null을 반환한다", async () => {
  await withFetch(async () => new Response(null, { status: 204 }),
    async () => assert.equal(await requestJson("/api/cart"), null));
});

test("성공 상태라도 잘못된 JSON은 미확인 응답으로 처리한다", async () => {
  await withFetch(async () => new Response("broken"), async () => {
    await assert.rejects(requestJson("/api/cart"),
      error => error instanceof RequestError && error.code === "UNCONFIRMED");
  });
});


test("인증 요청 전 토큰을 새로 받고 폼 형식으로 비밀번호를 보낸다", async () => {
  let calls=0;
  await withFetch(async (path,options) => {
    calls++;
    if(path==="/api/csrf") return new Response(JSON.stringify({headerName:"X-CSRF-TOKEN",token:"fresh"}));
    assert.equal(path,"/login");
    assert.equal(options.headers["X-CSRF-TOKEN"],"fresh");
    assert.equal(options.headers["Content-Type"],"application/x-www-form-urlencoded");
    assert.equal(new URLSearchParams(options.body).get("password")," space & plus+ ");
    return new Response(null,{status:204});
  },async()=>assert.equal(await requestJson("/login",{method:"POST",form:true,body:{username:"tim_01",password:" space & plus+ "}}),null));
  assert.equal(calls,2);
});
test("토큰을 받지 못하면 상태 변경 요청을 보내지 않는다", async () => {
  let calls=0;
  await withFetch(async(path)=>{calls++;assert.equal(path,"/api/csrf");return new Response("{}",{status:503});},
    async()=>assert.rejects(requestJson("/logout",{method:"POST"}),error=>error instanceof ApiError && error.status===503));
  assert.equal(calls,1);
});
