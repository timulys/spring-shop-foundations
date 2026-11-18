export function passwordInputError(password) {
  if (password.trim().length === 0 || password.length < 8 || password.length > 64) {
    return "비밀번호는 공백만으로 만들 수 없으며 길이는 8~64자입니다.";
  }
  if (!password.isWellFormed()) return "비밀번호에 올바른 유니코드 문자를 입력해 주세요.";
  if (new TextEncoder().encode(password).length > 72) {
    return "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.";
  }
  return "";
}
