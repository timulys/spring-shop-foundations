import { ApiError, requestJson } from "./api.js";
import { productsPath } from "./product-query.js";

const list = document.querySelector("#products");
const template = document.querySelector("#product-template");
const status = document.querySelector("#status");
const form = document.querySelector("#search-form");
const category = document.querySelector("#category");
const keyword = document.querySelector("#keyword");
const resetButton = document.querySelector("#reset-search");
const catalog = document.querySelector(".catalog");
const detail = document.querySelector("#product-detail");
const priceFormat = new Intl.NumberFormat("ko-KR");
let busy = false;

function setBusy(value) {
  busy = value;
  catalog.setAttribute("aria-busy", String(value));
  for (const control of catalog.querySelectorAll("button, input, select")) {
    control.disabled = value;
  }
}

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
    list.append(item);
  }
}

function showError(error) {
  status.dataset.state = "error";
  status.textContent = error instanceof ApiError
    ? error.status + " · " + error.message
    : "상품을 불러오지 못했습니다. 잠시 뒤 다시 시도해 주세요.";
}

async function loadProducts() {
  if (busy) return;
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
  if (busy) return;
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
loadProducts();
