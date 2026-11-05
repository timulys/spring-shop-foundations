const list = document.querySelector("#products");
const template = document.querySelector("#product-template");
const status = document.querySelector("#status");
const reloadButton = document.querySelector("#reload");
const catalog = document.querySelector(".catalog");
const priceFormat = new Intl.NumberFormat("ko-KR");

function renderProducts(products) {
  list.replaceChildren();
  for (const product of products) {
    const item = template.content.cloneNode(true);
    item.querySelector(".product").dataset.code = product.code;
    item.querySelector(".product-mark").textContent = product.name[0];
    item.querySelector(".product-code").textContent = product.code;
    item.querySelector(".product-name").textContent = product.name;
    item.querySelector(".product-price").textContent =
      priceFormat.format(product.unitPrice) + "원";
    list.append(item);
  }
}

async function loadProducts() {
  reloadButton.disabled = true;
  catalog.setAttribute("aria-busy", "true");
  list.replaceChildren();
  status.dataset.state = "loading";
  status.textContent = "상품을 불러오는 중입니다.";
  try {
    const response = await fetch("/api/products");
    if (!response.ok) {
      throw new Error("상품 요청 실패: " + response.status);
    }
    const products = await response.json();
    renderProducts(products);
    status.dataset.state = "success";
    status.textContent = products.length === 0
      ? "등록된 상품이 없습니다."
      : "상품 " + products.length + "개";
  } catch (error) {
    status.dataset.state = "error";
    status.textContent = "상품을 불러오지 못했습니다. 목록 새로고침을 눌러 주세요.";
    console.error(error);
  } finally {
    reloadButton.disabled = false;
    catalog.setAttribute("aria-busy", "false");
  }
}

reloadButton.addEventListener("click", loadProducts);
loadProducts();
