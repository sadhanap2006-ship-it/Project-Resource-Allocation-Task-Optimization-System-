/**
 * SIST Capstone - API Communication Module
 * Unified request client handling credentials, CSRF, error unwrapping, and 401 redirects.
 */

function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(';').shift();
  return null;
}

export async function request(method, path, body = null) {
  const url = path.startsWith('/api') ? path : `/api${path}`;
  
  const headers = {
    'Accept': 'application/json'
  };

  if (body !== null && !(body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  // Attach JWT Bearer Token if present in localStorage
  const jwtToken = localStorage.getItem('prs_jwt_token');
  if (jwtToken) {
    headers['Authorization'] = `Bearer ${jwtToken}`;
  }

  // Attach CSRF token if cookie is present (fallback for session requests)
  const xsrfToken = getCookie('XSRF-TOKEN');
  if (xsrfToken) {
    headers['X-XSRF-TOKEN'] = decodeURIComponent(xsrfToken);
  }

  const options = {
    method: method.toUpperCase(),
    headers,
    credentials: 'same-origin'
  };

  if (body !== null) {
    options.body = body instanceof FormData ? body : JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(url, options);
  } catch (netErr) {
    throw {
      errorCode: 'NETWORK_ERROR',
      message: 'Failed to communicate with server. Check connectivity.',
      details: netErr.message
    };
  }

  // Handle unauthorized session expiration (skip login attempts)
  if (response.status === 401 && !endpoint.includes('/auth/login')) {
    localStorage.removeItem('prs_jwt_token');
    if (!window.location.pathname.endsWith('login.html')) {
      window.location.href = '/login.html?expired=true';
    }
    throw {
      errorCode: 'UNAUTHORIZED',
      message: 'Session expired or unauthenticated.'
    };
  }

  // Read response
  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/pdf')) {
    if (!response.ok) {
      throw { errorCode: 'PDF_EXPORT_FAILED', message: 'Failed to generate PDF report.' };
    }
    return await response.blob();
  }

  let jsonResult = null;
  try {
    jsonResult = await response.json();
  } catch (err) {
    // Empty body or non-JSON
    if (!response.ok) {
      throw {
        errorCode: `HTTP_${response.status}`,
        message: response.statusText || 'Request failed'
      };
    }
    return null;
  }

  if (!response.ok) {
    // Unpack ApiErrorResponse
    const errObj = {
      status: response.status,
      errorCode: jsonResult.error?.code || jsonResult.errorCode || `HTTP_${response.status}`,
      message: jsonResult.error?.message || jsonResult.message || 'Request failed',
      details: jsonResult.error?.details || jsonResult.details || null,
      fieldErrors: jsonResult.error?.fieldErrors || jsonResult.fieldErrors || []
    };
    throw errObj;
  }

  // Unwrap ApiResponse envelope: { status: "SUCCESS", data: ... }
  if (jsonResult && jsonResult.data !== undefined) {
    return jsonResult.data;
  }
  return jsonResult;
}

export const api = {
  get: (path) => request('GET', path),
  post: (path, body) => request('POST', path, body),
  put: (path, body) => request('PUT', path, body),
  delete: (path) => request('DELETE', path)
};
