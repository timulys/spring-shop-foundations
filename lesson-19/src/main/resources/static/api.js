export class ApiError extends Error {
  constructor(status, body) {
    super(body?.message || "요청을 처리하지 못했습니다.");
    this.name = "ApiError";
    this.status = status;
    this.code = body?.code || "HTTP_" + status;
    this.requestId = body?.requestId || "";
    this.fieldErrors = body?.fieldErrors || [];
  }
}

// 이 앱의 JSON API를 호출하는 공통 함수입니다.
export async function requestJson(path, { method = "GET", body } = {}) {
  const options = { method, headers: { Accept: "application/json" } };
  if (body !== undefined) {
    options.headers["Content-Type"] = "application/json";
    options.body = JSON.stringify(body);
  }
  const response = await fetch(path, options);
  const data = response.status === 204 ? null : await response.json();
  if (!response.ok) {
    throw new ApiError(response.status, data);
  }
  return data;
}
