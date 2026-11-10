import { requestErrorMessage, requestJson } from "./api.js";
import { isBusy, setBusy } from "./ui-state.js";
import { hasDraftQuantities, loadCart } from "./cart.js";

const previewButton = document.querySelector("#preview-order");
const confirmButton = document.querySelector("#confirm-order");
const summary = document.querySelector("#checkout-summary");
const status = document.querySelector("#checkout-status");
const completion = document.querySelector("#order-completion");
const won = new Intl.NumberFormat("ko-KR");
let preview = null;

function updateConfirmButton() {
  const draft = hasDraftQuantities();
  previewButton.disabled = isBusy() || draft;
  confirmButton.disabled = isBusy() || draft || preview === null;
  document.querySelector("#cart-draft-note").hidden = !draft;
}

function invalidatePreview() {
  preview = null;
  summary.hidden = true;
  status.textContent = "";
  status.dataset.state = "idle";
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
    document.querySelector('[data-checkout="' + field + '"]').textContent =
      won.format(value[field]) + "원";
  }
  summary.hidden = false;
}

function showError(error) {
  status.dataset.state = "error";
  status.textContent = requestErrorMessage(error,
    "주문 내역과 장바구니를 확인한 뒤 다시 진행해 주세요.");
}

async function loadPreview() {
  if (isBusy() || hasDraftQuantities()) return;
  invalidatePreview();
  completion.hidden = true;
  setBusy(true);
  status.dataset.state = "loading";
  status.textContent = "서버에서 주문 금액을 계산하고 있습니다.";
  try {
    renderPreview(await requestJson("/api/orders/preview"));
    status.dataset.state = "success";
    status.textContent = "상품과 최종 금액을 확인한 뒤 모의 결제를 진행해 주세요.";
  } catch (error) {
    showError(error);
  } finally {
    setBusy(false);
  }
}

async function confirmOrder() {
  if (isBusy() || hasDraftQuantities() || preview === null) return;
  const body = { cartRevision: preview.cartRevision, expectedTotalWon: preview.total };
  setBusy(true);
  status.dataset.state = "loading";
  status.textContent = "주문을 처리하고 있습니다.";
  let completed = false;
  try {
    const order = await requestJson("/api/orders", { method: "POST", body });
    invalidatePreview();
    document.querySelector("#completed-order-id").textContent = order.id;
    document.querySelector("#completed-total").textContent = won.format(order.total) + "원";
    completion.hidden = false;
    status.dataset.state = "success";
    status.textContent = "모의 결제가 완료되었습니다. 실제 금액은 청구되지 않습니다.";
    completed = true;
  } catch (error) {
    invalidatePreview();
    showError(error);
  } finally {
    setBusy(false);
  }
  if (completed) {
    await loadCart();
    document.dispatchEvent(new Event("shop:order-created"));
  }
}

document.addEventListener("shop:busy-changed", updateConfirmButton);
document.addEventListener("shop:cart-changed", invalidatePreview);
document.addEventListener("shop:cart-edited", invalidatePreview);
previewButton.addEventListener("click", loadPreview);
confirmButton.addEventListener("click", confirmOrder);
updateConfirmButton();
