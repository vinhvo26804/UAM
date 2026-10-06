export type HttpOptions = Omit<RequestInit, "body"> & {
  /** Value to serialize as JSON. Omit for requests without a body. */
  body?: unknown;
};

export type HttpResponse<T = unknown> = {
  /** JSON, plain text (including malformed JSON), or null for an empty body. */
  data: T | string | null;
  status: number;
  headers: Headers;
};

export class HttpError extends Error {
  readonly status: number;
  readonly data: unknown;
  readonly headers: Headers;

  constructor(response: Response, data: unknown) {
    super(`HTTP ${response.status}${response.statusText ? ` ${response.statusText}` : ""}`);
    this.name = "HttpError";
    this.status = response.status;
    this.data = data;
    this.headers = response.headers;
  }
}

function backendPath(path: string): string {
  if (!path.startsWith("/") || path.startsWith("//") || path.includes("#")) {
    throw new TypeError("Use a backend path starting with /, without a host or fragment.");
  }

  // Validate only the pathname: query values can legitimately contain URLs.
  const pathname = path.split("?", 1)[0];
  for (const segment of pathname.split("/")) {
    let decoded: string;
    try {
      decoded = decodeURIComponent(segment);
    } catch {
      throw new TypeError("The backend path contains invalid percent encoding.");
    }

    if (
      decoded === "." ||
      decoded === ".." ||
      /[/\\%]/.test(decoded) ||
      /[\u0000-\u0020\u007f]/.test(decoded)
    ) {
      throw new TypeError("The backend path contains an unsafe path segment.");
    }
  }

  return `/backend${path}`;
}

async function responseData(response: Response): Promise<unknown> {
  const text = await response.text();
  if (text === "") return null;

  const contentType = response.headers.get("content-type")?.split(";", 1)[0].trim().toLowerCase();
  if (contentType === "application/json" || contentType?.endsWith("+json")) {
    try {
      return JSON.parse(text) as unknown;
    } catch {
      // Preserve the body and HTTP metadata even if the server sends invalid JSON.
      return text;
    }
  }

  return text;
}

/**
 * Browser-only Fetch helper: /me is requested through the Next.js /backend/me rewrite.
 * Do not call from Server Components or server code, where a relative URL has no origin.
 * T describes expected JSON; it does not perform runtime DTO validation.
 * No token persistence/refresh is performed. Fetch options (e.g. signal) pass through.
 * HTTP failures throw HttpError; network and abort errors propagate unchanged.
 */
export async function http<T = unknown>(
  path: string,
  options: HttpOptions = {},
): Promise<HttpResponse<T>> {
  if (typeof window === "undefined") {
    throw new Error("http() is browser-only and uses the relative /backend URL.");
  }

  const url = backendPath(path);
  const { body, headers: suppliedHeaders, ...requestOptions } = options;
  const headers = new Headers(suppliedHeaders);
  const jsonBody = body === undefined ? undefined : JSON.stringify(body);

  if (jsonBody !== undefined && !headers.has("content-type")) {
    headers.set("content-type", "application/json");
  }

  const response = await fetch(url, {
    ...requestOptions,
    headers,
    body: jsonBody,
  });
  const data = await responseData(response);

  if (!response.ok) throw new HttpError(response, data);

  return {
    data: data as T | string | null,
    status: response.status,
    headers: response.headers,
  };
}
