import { requestErrorMessage, requestJson } from "./api.js";
import { isBusy, setBusy } from "./ui-state.js";

const list = document.querySelector("#orders");
const status = document.querySelector("#orders-status");
const detail = document.querySelector("#order-detail");
const cancelButton = document.querySelector("#cancel-order");
const previousButton = document.querySelector("#orders-previous");
const nextButton = document.querySelector("#orders-next");
const pageLabel = document.querySelector("#orders-page");
const won = new Intl.NumberFormat("ko-KR");
const date = new Intl.DateTimeFormat("ko-KR", { dateStyle: "short", timeStyle: "short" });
let page = null;
let selectedOrder = null;

function statusLabel(value) {
  return { CONFIRMED: "주문 완료", CANCELLED: "취소 완료" }[value] || value;
}

function updateButtons() {
  const busy = isBusy();
  previousButton.disabled = busy || page === null || page.number === 0;
  nextButton.disabled = busy || page === null || !page.hasNext;
  cancelButton.disabled = busy || selectedOrder?.status !== "CONFIRMED";
}

function showError(error) {
  status.dataset.state = "error";
  status.textContent = requestErrorMessage(error,
    "주문 내역과 상세를 다시 조회해 주세요.");
}

function renderOrders(value) {
  page = value;
  list.replaceChildren();
  for (const order of value.content) {
    const row = document.createElement("li");
    const text = document.createElement("span");
    text.textContent = "주문 " + order.id + " · " + statusLabel(order.status)
      + " · " + won.format(order.total) + "원 · " + date.format(new Date(order.createdAt));
    const button = document.createElement("button");
    button.type = "button";
    button.textContent = "상세 보기";
    button.setAttribute("aria-label", "주문 " + order.id + " 상세 보기");
    button.disabled = isBusy();
    button.addEventListener("click", () => loadOrder(order.id));
    row.append(text, button);
    list.append(row);
  }
  document.querySelector("#orders-empty").hidden = value.content.length > 0;
  pageLabel.textContent = value.totalPages === 0 ? "0 / 0 페이지"
    : (value.number + 1) + " / " + value.totalPages + " 페이지";
  updateButtons();
}

export async function loadOrders(number = 0) {
  if (isBusy()) return;
  setBusy(true);
  status.dataset.state = "loading";
  status.textContent = "주문 내역을 불러오는 중입니다.";
  try {
    const value = await requestJson("/api/orders?page=" + number + "&size=5");
    renderOrders(value);
    status.dataset.state = "success";
    status.textContent = "총 " + value.totalElements + "건 · 최근 주문부터 표시합니다.";
  } catch (error) {
    showError(error);
    if (page !== null) status.textContent += " 목록에는 이전 조회 결과를 표시합니다.";
  } finally {
    setBusy(false);
  }
}

function renderOrder(order) {
  selectedOrder = order;
  document.querySelector("#selected-order-id").textContent = order.id;
  document.querySelector("#selected-order-status").textContent = statusLabel(order.status);
  document.querySelector("#selected-order-date").textContent =
      date.format(new Date(order.createdAt));
  const items = document.querySelector("#ordered-items");
  items.replaceChildren();
  for (const item of order.items) {
    const row = document.createElement("li");
    row.textContent = item.productName + " · 단가 " + won.format(item.unitPrice)
      + "원 · " + item.quantity + "개 · " + won.format(item.lineTotal) + "원";
    items.append(row);
  }
  for (const field of ["subtotal", "discount", "shippingFee", "total"]) {
    detail.querySelector('[data-order="' + field + '"]').textContent =
      won.format(order[field]) + "원";
  }
  detail.hidden = false;
  document.querySelector("#order-cancel-note").textContent = order.status === "CANCELLED"
    ? "취소 완료 · 주문 당시 상품과 금액은 기록으로 남습니다."
    : "모의 주문입니다. 취소하면 상태가 바뀌며 장바구니에 상품을 다시 담지는 않습니다.";
  updateButtons();
}

async function loadOrder(id) {
  if (isBusy()) return;
  selectedOrder = null;
  detail.hidden = true;
  setBusy(true);
  status.dataset.state = "loading";
  status.textContent = "주문 상세를 불러오는 중입니다.";
  try {
    renderOrder(await requestJson("/api/orders/" + id));
    status.dataset.state = "success";
    status.textContent = "주문 " + id + " 상세를 조회했습니다.";
  } catch (error) {
    showError(error);
  } finally {
    setBusy(false);
  }
}

async function cancelOrder() {
  if (isBusy() || selectedOrder?.status !== "CONFIRMED") return;
  const id = selectedOrder.id;
  let cancelled = false;
  setBusy(true);
  status.dataset.state = "loading";
  status.textContent = "주문 " + id + " 취소를 처리하고 있습니다.";
  try {
    const order = await requestJson("/api/orders/" + id + "/cancel", { method: "POST" });
    renderOrder(order);
    cancelled = true;
    status.dataset.state = "success";
    status.textContent = "주문 " + id + "을 취소했습니다.";
  } catch (error) {
    showError(error);
  } finally {
    setBusy(false);
  }
  if (cancelled) await loadOrders(page?.number ?? 0);
}

document.querySelector("#reload-orders").addEventListener("click",
    () => loadOrders(page?.number ?? 0));
previousButton.addEventListener("click", () => loadOrders(page.number - 1));
nextButton.addEventListener("click", () => loadOrders(page.number + 1));
cancelButton.addEventListener("click", cancelOrder);
document.addEventListener("shop:busy-changed", updateButtons);
document.addEventListener("shop:order-created", () => loadOrders(0));
updateButtons();
