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

// 제한 시간은 이 실습의 화면 복구 정책이며 서버 처리 취소를 보장하지 않습니다.
export class RequestError extends Error {
  constructor(code, message) {
    super(message);
    this.name = "RequestError";
    this.code = code;
  }
}

export async function requestJson(path, { method = "GET", body, timeoutMs = 12000 } = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  const options = {
    method, signal: controller.signal, headers: { Accept: "application/json" }
  };
  if (body !== undefined) {
    options.headers["Content-Type"] = "application/json";
    options.body = JSON.stringify(body);
  }
  try {
    const response = await fetch(path, options);
    const data = response.status === 204 ? null : await response.json();
    if (!response.ok) throw new ApiError(response.status, data);
    return data;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    if (controller.signal.aborted) {
      throw new RequestError("TIMEOUT", "응답 대기 시간을 넘었습니다.");
    }
    throw new RequestError("UNCONFIRMED", "응답을 정상적으로 확인하지 못했습니다.");
  } finally {
    clearTimeout(timer);
  }
}

export function requestErrorMessage(error, recovery) {
  if (error instanceof ApiError) return error.status + " · " + error.message;
  const message = error instanceof RequestError ? error.message : "요청 결과를 확인하지 못했습니다.";
  return message + " " + recovery;
}
