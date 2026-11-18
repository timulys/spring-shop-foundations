import { loadOrders } from "./orders.js";
import "./checkout.js";
import { ApiError, requestErrorMessage, requestJson } from "./api.js";
import { productsPath } from "./product-query.js";
import { isBusy, setBusy } from "./ui-state.js";
import { addItem, loadCart } from "./cart.js";

let signedIn = false;
const list = document.querySelector("#products");
const template = document.querySelector("#product-template");
const status = document.querySelector("#status");
const form = document.querySelector("#search-form");
const category = document.querySelector("#category");
const keyword = document.querySelector("#keyword");
const resetButton = document.querySelector("#reset-search");
const detail = document.querySelector("#product-detail");
const priceFormat = new Intl.NumberFormat("ko-KR");
function renderProducts(products) {
  list.replaceChildren();
  for (const product of products) {
    const item = template.content.cloneNode(true);
    item.querySelector(".product").dataset.code = product.code;
    item.querySelector(".product-mark").textContent = product.name[0];
    item.querySelector(".product-code").textContent = product.code;
    item.querySelector(".product-name").textContent = product.name;
    item.querySelector(".product-category").textContent = product.category;
    item.querySelector(".product-price").textContent =
      priceFormat.format(product.unitPrice) + "원";
    const button = item.querySelector(".detail-button");
    button.setAttribute("aria-label", product.name + " 상세 보기");
    button.addEventListener("click", () => loadDetail(product.code));
    const add = item.querySelector(".add-button");
    add.hidden = !signedIn;
    add.setAttribute("aria-label", product.name + " 담기");
    add.addEventListener("click", () => addItem(product.code));
    list.append(item);
  }
}

function showError(error) {
  status.dataset.state = "error";
  status.textContent = requestErrorMessage(error,
    "검색 조건을 확인하고 다시 검색해 주세요.");
}

async function loadProducts() {
  if (isBusy()) return;
  const path = productsPath(category.value, keyword.value);
  setBusy(true);
  detail.hidden = true;
  list.replaceChildren();
  status.dataset.state = "loading";
  status.textContent = "상품을 검색하고 있습니다.";
  try {
    const products = await requestJson(path);
    renderProducts(products);
    status.dataset.state = "success";
    status.textContent = products.length === 0
      ? "검색 결과가 없습니다. 다른 조건으로 검색해 주세요."
      : "상품 " + products.length + "개";
  } catch (error) {
    showError(error);
  } finally {
    setBusy(false);
  }
}

async function loadDetail(code) {
  if (isBusy()) return;
  setBusy(true);
  detail.hidden = true;
  status.dataset.state = "loading";
  status.textContent = "상품 상세를 불러오는 중입니다.";
  try {
    const product = await requestJson("/api/products/" + encodeURIComponent(code));
    detail.querySelector(".detail-code").textContent = product.code;
    detail.querySelector(".detail-name").textContent = product.name;
    detail.querySelector(".detail-category").textContent = product.category;
    detail.querySelector(".detail-price").textContent =
      priceFormat.format(product.unitPrice) + "원";
    detail.hidden = false;
    status.dataset.state = "success";
    status.textContent = product.name + " 상세를 조회했습니다.";
  } catch (error) {
    showError(error);
  } finally {
    setBusy(false);
  }
}

form.addEventListener("submit", (event) => {
  event.preventDefault();
  loadProducts();
});
category.addEventListener("change", loadProducts);
resetButton.addEventListener("click", () => {
  form.reset();
  loadProducts();
});
try {
  const member = await requestJson("/api/session/me");
  signedIn = true;
  document.querySelector("#session-status").textContent = `${member.displayName}님의 장바구니와 주문입니다.`;
  for (const panel of document.querySelectorAll("[data-member-panel]")) panel.hidden = false;
} catch (error) {
  document.querySelector("#login-prompt").hidden = false;
  document.querySelector("#session-status").textContent = error instanceof ApiError && error.status === 401
    ? "상품은 로그인 없이 살펴볼 수 있습니다."
    : requestErrorMessage(error, "로그인 상태를 확인하려면 새로고침해 주세요.");
}
await loadProducts();
if (signedIn) {
  await loadCart();
  await loadOrders();
}
