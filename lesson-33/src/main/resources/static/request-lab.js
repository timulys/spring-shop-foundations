import { ApiError, requestJson } from "./api.js";

const buttons = document.querySelectorAll("[data-path]");
const panel = document.querySelector(".request-panel");
const status = document.querySelector("#request-status");
const output = document.querySelector("#response-body");

function setBusy(busy) {
  panel.setAttribute("aria-busy", String(busy));
  for (const button of buttons) button.disabled = busy;
}

async function handleRequest(event) {
  const path = event.currentTarget.dataset.path;
  setBusy(true);
  status.dataset.state = "loading";
  status.textContent = "서버에 요청하고 있습니다.";
  output.textContent = "응답을 기다리는 중입니다.";
  try {
    const data = await requestJson(path);
    output.textContent = JSON.stringify(data, null, 2);
    status.dataset.state = "success";
    status.textContent = "조회 성공 · 상품 " + data.length + "개";
  } catch (error) {
    status.dataset.state = "error";
    if (error instanceof ApiError) {
      status.textContent = error.status + " · " + error.code + " · " + error.message;
      output.textContent = JSON.stringify({
        code: error.code,
        message: error.message,
        requestId: error.requestId,
        fieldErrors: error.fieldErrors
      }, null, 2);
    } else {
      status.textContent = "서버 연결이나 응답 형식을 확인해 주세요.";
      output.textContent = "요청을 완료하지 못했습니다.";
    }
  } finally {
    setBusy(false);
  }
}

for (const button of buttons) {
  button.addEventListener("click", handleRequest);
}
