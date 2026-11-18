import { ApiError, requestErrorMessage, requestJson } from "./api.js";
import { isBusy, setBusy } from "./ui-state.js";
import { hasDraftQuantities, loadCart } from "./cart.js";
import { OrderIntentStore } from "./order-intent.js";

const previewButton = document.querySelector("#preview-order");
const confirmButton = document.querySelector("#confirm-order");
const retryButton = document.querySelector("#retry-order");
const summary = document.querySelector("#checkout-summary");
const status = document.querySelector("#checkout-status");
const completion = document.querySelector("#order-completion");
const won = new Intl.NumberFormat("ko-KR");
let preview = null;
let intents = null;

export function initializeCheckout(memberId) {
  try {
    intents = new OrderIntentStore(sessionStorage, memberId);
    if (intents.pending) {
      status.textContent = "결과를 확인하지 못한 주문이 있습니다. 이전 주문 결과를 다시 확인해 주세요.";
    }
  } catch {
    intents = null;
    status.textContent = "이 탭의 주문 재전송 정보를 읽을 수 없습니다. 주문 내역을 확인한 뒤 새 탭에서 진행해 주세요.";
  }
  updateConfirmButton();
}

function updateConfirmButton() {
  const draft = hasDraftQuantities();
  const pending = intents?.pending != null;
  previewButton.disabled = isBusy() || draft || !intents || pending;
  confirmButton.disabled = isBusy() || draft || preview === null || !intents || pending;
  retryButton.hidden = !pending;
  retryButton.disabled = isBusy();
  document.querySelector("#cart-draft-note").hidden = !draft;
}

function invalidatePreview() {
  preview = null;
  summary.hidden = true;
  if (intents && !intents.pending) {
    status.textContent = "";
    status.dataset.state = "idle";
  }
  updateConfirmButton();
}

function renderPreview(value) {
  preview = value;
  const list = document.querySelector("#checkout-items");
  list.replaceChildren();
  for (const item of value.items) {
    const row = document.createElement("li");
    row.textContent = item.productName + " · " + item.quantity + "개 · "
      + won.format(item.lineTotal) + "원";
    list.append(row);
  }
  for (const field of ["subtotal", "discount", "shippingFee", "total"]) {
    document.querySelector('[data-checkout="' + field + '"]').textContent = won.format(value[field]) + "원";
  }
  summary.hidden = false;
}

async function loadPreview() {
  if (isBusy() || hasDraftQuantities() || !intents || intents.pending) return;
  invalidatePreview(); completion.hidden = true; setBusy(true);
  status.dataset.state = "loading";
  status.textContent = "서버에서 주문 금액을 계산하고 있습니다.";
  try {
    renderPreview(await requestJson("/api/orders/preview"));
    status.dataset.state = "success";
    status.textContent = "상품과 최종 금액을 확인한 뒤 모의 결제를 진행해 주세요.";
  } catch (error) {
    status.dataset.state = "error";
    status.textContent = requestErrorMessage(error, "장바구니를 새로 조회한 뒤 다시 진행해 주세요.");
  } finally { setBusy(false); }
}

async function submitIntent(intent) {
  setBusy(true); status.dataset.state = "loading";
  status.textContent = "주문 결과를 확인하고 있습니다.";
  let completed = false;
  let completionMessage = "";
  try {
    const order = await requestJson("/api/orders", {
      method: "POST", body: intent.body, headers: { "Idempotency-Key": intent.key }
    });
    intents.clear(); invalidatePreview();
    document.querySelector("#completed-order-id").textContent = order.id;
    document.querySelector("#completed-total").textContent = won.format(order.total) + "원";
    completion.hidden = false; status.dataset.state = "success";
    document.querySelector("#completed-order-state").textContent = order.status === "CANCELLED" ? "이미 취소된 주문입니다" : "주문이 확인되었습니다";
    completionMessage = order.status === "CANCELLED"
      ? "이 요청으로 만든 주문은 이미 취소되었습니다. 주문 내역에서 상태를 확인해 주세요."
      : "주문이 확인되었습니다. 모의 결제이므로 실제 금액은 청구되지 않습니다.";
    status.textContent = completionMessage;
    completed = true;
  } catch (error) {
    // Network/5xx and authentication failures keep the exact same key and body.
    if (error instanceof ApiError && error.status >= 400 && error.status < 500
        && ![401, 403].includes(error.status)) {
      try { intents.clear(); invalidatePreview(); } catch { /* Preserve the pending key if storage cannot be cleared. */ }
    }
    status.dataset.state = "error";
    status.textContent = requestErrorMessage(error, "이전 주문 결과 다시 확인 버튼을 눌러 주세요.");
    status.textContent += intents.pending
      ? " 같은 주문 내용으로 다시 확인할 수 있습니다."
      : " 장바구니를 새로 조회하고 주문 금액을 다시 확인해 주세요.";
  } finally { setBusy(false); }
  if (completed) {
    await loadCart();
    status.dataset.state = "success";
    status.textContent = completionMessage;
    document.dispatchEvent(new Event("shop:order-created"));
  }
}

async function confirmOrder() {
  if (isBusy() || hasDraftQuantities() || preview === null || !intents || intents.pending) return;
  let intent;
  try { intent = intents.begin({ cartRevision: preview.cartRevision, expectedTotalWon: preview.total }); }
  catch {
    status.textContent = "주문 재전송 정보를 저장할 수 없어 요청을 보내지 않았습니다. 브라우저 저장 공간을 확인해 주세요.";
    return;
  }
  await submitIntent(intent);
}

document.addEventListener("shop:busy-changed", updateConfirmButton);
document.addEventListener("shop:cart-changed", invalidatePreview);
document.addEventListener("shop:cart-edited", invalidatePreview);
previewButton.addEventListener("click", loadPreview);
confirmButton.addEventListener("click", confirmOrder);
retryButton.addEventListener("click", async () => {
  if (!isBusy() && intents?.pending) await submitIntent(intents.pending);
});
updateConfirmButton();
