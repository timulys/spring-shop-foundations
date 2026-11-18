import { requestJson, requestErrorMessage } from "./api.js";
import { passwordInputError } from "./signup-input.js";

const form = document.querySelector("#signup-form");
const status = document.querySelector("#signup-status");
const result = document.querySelector("#signup-result");
const button = form.querySelector("button[type=submit]");
form.addEventListener("submit", async event => {
  event.preventDefault();
  if (button.disabled) return;
  result.hidden = true;
  const fields = new FormData(form);
  const password = String(fields.get("password"));
  const inputError = passwordInputError(password);
  if (inputError) { status.textContent = inputError; return; }
  button.disabled = true;
  form.setAttribute("aria-busy", "true");
  status.textContent = "가입 정보를 저장하고 있습니다.";
  try {
    const member = await requestJson("/api/members", { method: "POST", body: {
      loginId: String(fields.get("loginId")).trim(),
      displayName: String(fields.get("displayName")).trim(), password
    }});
    form.reset();
    status.textContent = "가입이 완료되었습니다.";
    result.textContent = `${member.displayName}님, 회원 번호는 ${member.id}, 로그인 아이디는 ${member.loginId}입니다.`;
    result.hidden = false;
  } catch (error) {
    status.textContent = requestErrorMessage(error,
      "가입이 완료되었을 수도 있습니다. 같은 아이디로 다시 시도했을 때 중복 안내가 나오면 가입 여부를 확인해 주세요.");
  } finally {
    form.elements.password.value = "";
    button.disabled = false;
    form.setAttribute("aria-busy", "false");
  }
});
