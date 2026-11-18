import { ApiError, requestJson, requestErrorMessage } from "./api.js";
const form=document.querySelector("#login-form");
const status=document.querySelector("#login-status");
const panel=document.querySelector("#session-panel");
const info=document.querySelector("#member-info");
const loginButton=form.querySelector("button");
const logoutButton=document.querySelector("#logout");

async function refreshSession() {
  try {
    const member=await requestJson("/api/session/me");
    info.textContent=`${member.displayName}님 · ${member.loginId} · 회원 번호 ${member.id}`;
    panel.hidden=false; form.hidden=true;
    return true;
  } catch(error) {
    if (!(error instanceof ApiError && error.status===401)) throw error;
    panel.hidden=true; form.hidden=false; info.textContent="";
    return false;
  }
}
form.addEventListener("submit", async event => {
  event.preventDefault();
  if(loginButton.disabled) return;
  loginButton.disabled=true; status.textContent="로그인 중입니다.";
  const fields=new FormData(form);
  try {
    await requestJson("/login",{method:"POST",form:true,body:{
      username:String(fields.get("username")),password:String(fields.get("password"))
    }});
    status.textContent=await refreshSession()?"로그인되었습니다.":"로그인 상태를 다시 확인해 주세요.";
  } catch(error) {
    status.textContent=requestErrorMessage(error,"화면을 새로고침해 로그인 상태를 확인해 주세요.");
  } finally {
    form.elements.password.value=""; loginButton.disabled=false;
  }
});
logoutButton.addEventListener("click",async () => {
  if(logoutButton.disabled) return;
  logoutButton.disabled=true;
  try {
    await requestJson("/logout",{method:"POST"});
    await refreshSession(); status.textContent="로그아웃되었습니다.";
  } catch(error) { status.textContent=requestErrorMessage(error,"새로고침해 로그인 상태를 확인해 주세요."); }
  finally { logoutButton.disabled=false; }
});
refreshSession().then(signedIn => {status.textContent=signedIn?"로그인 상태가 유지되고 있습니다.":"아이디와 비밀번호를 입력해 주세요.";})
  .catch(error => {status.textContent=requestErrorMessage(error,"잠시 후 새로고침해 주세요.");});
