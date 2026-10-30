const BASE_URL = '';

/**
 * 获取本地存储的token
 * @returns {string}
 */
function getToken() {
  try {
    return localStorage.getItem('satoken') || '';
  } catch {
    return '';
  }
}

/**
 * 统一API请求方法
 * @param {string} path - API路径
 * @param {RequestInit} options - 请求选项
 * @returns {Promise<any>}
 */
export async function apiFetch(path, options = {}) {
  const headers = new Headers(options.headers || {});
  
  // 设置默认Content-Type
  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }
  
  // 添加认证token - Sa-Token支持多种header名称
  const token = getToken();
  if (token) {
    // Sa-Token默认支持Authorization和satoken两种header
    headers.set('Authorization', `Bearer ${token}`);
    headers.set('satoken', token);
  }
  
  try {
    const res = await fetch(`${BASE_URL}${path}`, {
      ...options,
      headers,
    });
    
    // 处理HTTP错误状态
    if (!res.ok) {
      console.error(`[API] HTTP错误: ${res.status} ${res.statusText}`);
      // 如果是401未授权，清除token
      if (res.status === 401) {
        try {
          localStorage.removeItem('satoken');
        } catch (e) {
          console.warn('[API] 清除token失败:', e);
        }
      }
    }
    
    const ct = res.headers.get('content-type') || '';
    if (ct.includes('application/json')) {
      const json = await res.json();
      return json;
    }
    const text = await res.text();
    return text;
  } catch (error) {
    console.error(`[API] 请求失败: ${path}`, error);
    throw error;
  }
}

/**
 * 上传文件
 * @param {string} path - API路径
 * @param {File} file - 文件对象
 * @param {string} fieldName - 表单字段名
 * @returns {Promise<any>}
 */
export async function uploadFile(path, file, fieldName = 'file') {
  const formData = new FormData();
  formData.append(fieldName, file);
  
  return apiFetch(path, {
    method: 'POST',
    body: formData,
    // 不设置Content-Type，让浏览器自动设置multipart/form-data和boundary
  });
}

/**
 * 上传文件（支持进度回调）
 * @param {string} path - API路径
 * @param {File} file - 文件对象
 * @param {string} fieldName - 表单字段名
 * @param {(percent: number) => void} onProgress - 进度回调 0~100
 * @param {() => void} onUploadComplete - 文件字节上传完成后的回调
 * @returns {Promise<any>}
 */
export function uploadFileWithProgress(path, file, fieldName = 'file', onProgress, onUploadComplete) {
  return new Promise((resolve, reject) => {
    const formData = new FormData();
    formData.append(fieldName, file);

    const xhr = new XMLHttpRequest();
    xhr.open('POST', `${BASE_URL}${path}`);

    const token = getToken();
    if (token) {
      xhr.setRequestHeader('Authorization', `Bearer ${token}`);
      xhr.setRequestHeader('satoken', token);
    }

    if (xhr.upload && onProgress) {
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) {
          onProgress(Math.round((e.loaded / e.total) * 100));
        }
      };
    }

    if (xhr.upload) {
      xhr.upload.onload = () => {
        if (onProgress) {
          onProgress(100);
        }
        if (onUploadComplete) {
          onUploadComplete();
        }
      };
    }

    xhr.onload = () => {
      try {
        if (xhr.status === 401) {
          try {
            localStorage.removeItem('satoken');
          } catch (e) {
            console.warn('[API] 清除token失败:', e);
          }
        }

        const ct = xhr.getResponseHeader('content-type') || '';
        if (ct.includes('application/json')) {
          const json = JSON.parse(xhr.responseText);
          if (xhr.status < 200 || xhr.status >= 300) {
            reject(new Error(json?.message || `请求失败(${xhr.status})`));
            return;
          }
          if (json && typeof json === 'object' && 'code' in json && json.code !== 0) {
            reject(new Error(json.message || '上传失败'));
            return;
          }
          resolve(json);
        } else {
          if (xhr.status < 200 || xhr.status >= 300) {
            reject(new Error(xhr.responseText || `请求失败(${xhr.status})`));
            return;
          }
          resolve(xhr.responseText);
        }
      } catch (e) {
        reject(e);
      }
    };

    xhr.onerror = () => reject(new Error('网络请求失败'));
    xhr.ontimeout = () => reject(new Error('请求超时'));

    xhr.send(formData);
  });
}

// 兼容 axios 风格的 API 客户端
const apiClient = {
  async get(path, config = {}) {
    let url = path;
    if (config.params) {
      // 过滤掉 undefined 和 null 值，避免被转成字符串
      const filteredParams = Object.fromEntries(
        Object.entries(config.params).filter(([_, v]) => v !== undefined && v !== null && v !== '')
      );
      if (Object.keys(filteredParams).length > 0) {
        url = `${path}?${new URLSearchParams(filteredParams).toString()}`;
      }
    }
    return apiFetch(url, { method: 'GET' });
  },
  async post(path, data, config = {}) {
    return apiFetch(path, {
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
    });
  },
  async put(path, data, config = {}) {
    return apiFetch(path, {
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined,
    });
  },
  async delete(path, config = {}) {
    return apiFetch(path, { method: 'DELETE' });
  },
};

export default apiClient;
