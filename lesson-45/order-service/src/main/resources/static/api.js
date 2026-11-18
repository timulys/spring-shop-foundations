function validRequestId(value) {
  return typeof value === "string" && /^[A-Za-z0-9_-]{8,64}$/.test(value);
}

export class ApiError extends Error {
  constructor(status, body, requestId = "") {
    super(body?.message || "요청을 처리하지 못했습니다.");
    this.name = "ApiError";
    this.status = status;
    this.code = body?.code || "HTTP_" + status;
    this.requestId = validRequestId(requestId) ? requestId : validRequestId(body?.requestId) ? body.requestId : "";
    this.fieldErrors = body?.fieldErrors || [];
  }
}

// A client timeout ends waiting; the server may already have committed the request.
export class RequestError extends Error {
  constructor(code, message, requestId = "") {
    super(message); this.name = "RequestError"; this.code = code;
    this.requestId = validRequestId(requestId) ? requestId : "";
  }
}

async function responseData(response, requestId) {
  let data = null;
  if (response.status !== 204) {
    try { data = await response.json(); }
    catch {
      if (response.ok) throw new RequestError("UNCONFIRMED", "응답을 정상적으로 확인하지 못했습니다.", requestId);
    }
  }
  if (!response.ok) throw new ApiError(response.status, data, requestId);
  return data;
}

export async function requestJson(path, { method = "GET", body, form = false, timeoutMs = 12000, headers = {} } = {}) {
  const requestId = crypto.randomUUID();
  let activeRequestId = requestId;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  const options = { method, credentials: "same-origin", signal: controller.signal,
    headers: { ...headers, Accept: "application/json", "X-Request-Id": requestId } };
  if (body !== undefined) {
    options.headers["Content-Type"] = form ? "application/x-www-form-urlencoded" : "application/json";
    options.body = form ? new URLSearchParams(body).toString() : JSON.stringify(body);
  }
  try {
    if (!["GET", "HEAD", "OPTIONS", "TRACE"].includes(method.toUpperCase())) {
      activeRequestId = crypto.randomUUID();
      const tokenResponse = await fetch("/api/csrf", { credentials: "same-origin", cache: "no-store", signal: controller.signal,
        headers: { Accept: "application/json", "X-Request-Id": activeRequestId } });
      const tokenId = tokenResponse.headers.get("X-Request-Id");
      if (validRequestId(tokenId)) activeRequestId = tokenId;
      const csrf = await responseData(tokenResponse, activeRequestId);
      if (csrf?.headerName !== "X-CSRF-TOKEN" || typeof csrf.token !== "string" || !csrf.token) {
        throw new RequestError("UNCONFIRMED", "요청 토큰을 확인하지 못했습니다.", activeRequestId);
      }
      options.headers[csrf.headerName] = csrf.token;
    }
    activeRequestId = requestId;
    const response = await fetch(path, options);
    const returnedId = response.headers.get("X-Request-Id");
    if (validRequestId(returnedId)) activeRequestId = returnedId;
    return await responseData(response, activeRequestId);
  } catch (error) {
    if (error instanceof ApiError || error instanceof RequestError) throw error;
    if (controller.signal.aborted) throw new RequestError("TIMEOUT", "응답 대기 시간을 넘었습니다.", activeRequestId);
    throw new RequestError("UNCONFIRMED", "응답을 정상적으로 확인하지 못했습니다.", activeRequestId);
  } finally { clearTimeout(timer); }
}

export function requestErrorMessage(error, recovery) {
  const trace = validRequestId(error?.requestId) ? " (요청 ID: " + error.requestId + ")" : "";
  if (error instanceof ApiError) return error.status + " · " + error.message + trace;
  const message = error instanceof RequestError ? error.message : "요청 결과를 확인하지 못했습니다.";
  return message + " " + recovery + trace;
}
