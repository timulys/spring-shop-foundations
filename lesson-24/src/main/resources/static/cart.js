import { requestErrorMessage, requestJson } from "./api.js";
import { isBusy, setBusy } from "./ui-state.js";
import { cartQuantity } from "./cart-input.js";

const list = document.querySelector("#cart-items");
const template = document.querySelector("#cart-item-template");
const status = document.querySelector("#cart-status");
const count = document.querySelector("#cart-quantity");
const subtotal = document.querySelector("#cart-subtotal");
const priceFormat = new Intl.NumberFormat("ko-KR");
let lastCart = null;

function renderCart(cart) {
  lastCart = cart;
  list.replaceChildren();
  for (const entry of cart.items) {
    const item = template.content.cloneNode(true);
    item.querySelector(".cart-item").dataset.code = entry.productCode;
    item.querySelector(".cart-name").textContent = entry.name;
    item.querySelector(".cart-code").textContent = entry.productCode;
    item.querySelector(".cart-unit-price").textContent =
      priceFormat.format(entry.unitPrice) + "원";
    item.querySelector(".cart-line-total").textContent =
      priceFormat.format(entry.lineTotal) + "원";
    const input = item.querySelector(".quantity-input");
    input.value = entry.quantity;
    input.setAttribute("aria-label", entry.name + " 수량");
    input.addEventListener("input", () => {
      document.dispatchEvent(new Event("shop:cart-edited"));
    });
    item.querySelector(".quantity-form").addEventListener("submit", (event) => {
      event.preventDefault();
      changeQuantity(entry.productCode, input.value);
    });
    const remove = item.querySelector(".remove-button");
    remove.setAttribute("aria-label", entry.name + " 삭제");
    remove.addEventListener("click", () => removeItem(entry.productCode));
    list.append(item);
  }
  count.textContent = cart.totalQuantity;
  subtotal.textContent = priceFormat.format(cart.subtotal) + "원";
  document.querySelector("#cart-empty").hidden = cart.items.length > 0;
}

export function hasDraftQuantities() {
  return [...list.querySelectorAll(".quantity-input")].some((input) => {
    const code = input.closest(".cart-item").dataset.code;
    const saved = lastCart?.items.find((item) => item.productCode === code);
    return !saved || Number(input.value) !== saved.quantity;
  });
}

function showError(error) {
  status.dataset.state = "error";
  status.textContent = requestErrorMessage(error,
    "장바구니 새로고침으로 서버 상태를 확인해 주세요.");
  status.textContent += " 입력값은 남겨 두었습니다. 합계는 마지막 조회 값입니다.";
}

async function updateCart(path, options, message) {
  if (isBusy()) return;
  setBusy(true);
  status.dataset.state = "loading";
  status.textContent = "장바구니를 처리하고 있습니다.";
  try {
    const cart = await requestJson(path, options);
    renderCart(cart);
    document.dispatchEvent(new Event("shop:cart-changed"));
    status.dataset.state = "success";
    status.textContent = message;
  } catch (error) {
    showError(error);
  } finally {
    setBusy(false);
  }
}

export async function loadCart() {
  await updateCart("/api/cart", {}, "장바구니를 조회했습니다.");
}

export async function addItem(code) {
  await updateCart("/api/cart/items", {
    method: "POST", body: { productCode: code, quantity: 1 }
  }, "상품 1개를 담았습니다.");
}

async function changeQuantity(code, value) {
  if (isBusy()) return;
  let quantity;
  try {
    quantity = cartQuantity(value);
  } catch (error) {
    status.dataset.state = "error";
    status.textContent = error.message;
    return;
  }
  await updateCart("/api/cart/items/" + encodeURIComponent(code), {
    method: "PATCH", body: { quantity }
  }, "수량을 변경했습니다.");
}

async function removeItem(code) {
  await updateCart("/api/cart/items/" + encodeURIComponent(code), {
    method: "DELETE"
  }, "상품을 삭제했습니다.");
}

document.querySelector("#reload-cart").addEventListener("click", loadCart);
