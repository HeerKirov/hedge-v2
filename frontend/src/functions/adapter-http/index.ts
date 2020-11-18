import { createHttpInstance, HttpInstance } from "./server"
import { createWebService, WebService } from "./impl/web"

export { HttpInstance, createHttpInstance }

export interface APIService {
    web: WebService
}

export function createAPIService(httpInstance: HttpInstance): APIService {
    return {
        web: createWebService(httpInstance)
    }
}
