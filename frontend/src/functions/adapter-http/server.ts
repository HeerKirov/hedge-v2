import { computed } from "vue"
import axios, { Method } from "axios"

export interface HttpClient {
    /**
     * 发送一个请求到server。
     */
    request<R>(config: RequestConfig): Promise<Response<R>>
    /**
     * 创建带有path和query参数的柯里化请求。
     */
    createPathQueryRequest: <P, Q, R>(url: (path: P) => string, method?: Method) => (path: P, query: Q) => Promise<Response<R>>
    /**
     * 创建带有path和data参数的柯里化请求。
     */
    createPathDataRequest: <P, T, R>(url: (path: P) => string, method?: Method) => (path: P, data: T) => Promise<Response<R>>
    /**
     * 创建带有path参数的柯里化请求。
     */
    createPathRequest: <P, R>(url: (path: P) => string, method?: Method) => (path: P) => Promise<Response<R>>
    /**
     * 创建带有query参数的柯里化请求。
     */
    createQueryRequest: <Q, R>(url: string, method?: Method) => (query: Q) => Promise<Response<R>>
    /**
     * 创建带有data参数的柯里化请求。
     */
    createDataRequest: <T, R>(url: string, method?: Method) => (data: T) => Promise<Response<R>>
    /**
     * 创建不带任何参数的柯里化请求。
     */
    createRequest: <R>(url: string, method?: Method) => () => Promise<Response<R>>
}

export interface HttpClientConfig {
    baseUrl?: string
    token?: string
}

export function createHttpClient(config: Readonly<HttpClientConfig>): HttpClient {
    const instance = axios.create()

    const headers = computed(() => config.token && {'Authorization': `Bearer ${config.token}`})

    function request<R>(config: RequestConfig): Promise<Response<R>> {
        return new Promise(resolve => {
            instance.request({
                baseURL: config.baseUrl,
                url: config.url,
                method: config.method,
                params: config.query,
                data: config.data,
                headers: headers.value
            })
            .then(res => resolve({
                ok: true,
                status: res.status,
                data: res.data
            }))
            .catch(reason => {
                if(reason.response) {
                    const data = reason.response.data as {code: string, message: string | null, info: any}
                    resolve({
                        ok: false,
                        status: reason.response.status,
                        code: data.code,
                        message: data.message
                    })
                }else{
                    resolve({
                        ok: false,
                        status: undefined,
                        message: reason.message
                    })
                }
            })
        })
    }

    return {
        request,
        createPathQueryRequest: <P, Q, R>(url: (path: P) => string, method?: Method) => (path: P, query: Q) => request<R>({baseUrl: config.baseUrl, url: url(path), method, query}),
        createPathDataRequest: <P, T, R>(url: (path: P) => string, method?: Method) => (path: P, data: T) => request<R>({baseUrl: config.baseUrl, url: url(path), method, data}),
        createPathRequest: <P, R>(url: (path: P) => string, method?: Method) => (path: P) => request<R>({baseUrl: config.baseUrl, url: url(path), method}),
        createQueryRequest: <Q, R>(url: string, method?: Method) => (query: Q) => request<R>({baseUrl: config.baseUrl, url, method, query}),
        createDataRequest: <T, R>(url: string, method?: Method) => (data: T) => request<R>({baseUrl: config.baseUrl, url, method, data}),
        createRequest: <R>(url: string, method?: Method) => () => request<R>({baseUrl: config.baseUrl, url, method})
    }
}

interface RequestConfig {
    baseUrl?: string
    url: string
    method?: Method
    query?: {[name: string]: any}
    data?: any
}

export type Response<T> = ResponseOk<T> | ResponseError | ResponseConnectionError

interface ResponseOk<T> {
    ok: true
    status: number
    data: T
}

interface ResponseError {
    ok: false
    status: number
    code: string
    message: string | null
}

interface ResponseConnectionError {
    ok: false
    status: undefined
    message: string
}