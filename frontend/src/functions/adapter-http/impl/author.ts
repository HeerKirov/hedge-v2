import { HttpInstance, Response } from "../server"
import { IdResponse, LimitAndOffsetFilter, Link, OrderList } from "./generic"
import { SimpleAnnotation } from "./annotations"

export function createAuthorEndpoint(http: HttpInstance): AuthorEndpoint {
    return {
        list: http.createQueryRequest("/api/authors"),
        create: http.createDataRequest("/api/authors", "POST"),
        get: http.createPathRequest(id => `/api/authors/${id}`),
        update: http.createPathDataRequest(id => `/api/authors/${id}`, "PATCH"),
        delete: http.createPathRequest(id => `/api/authors/${id}`, "DELETE")
    }
}

/**
 * 作者。
 */
export interface AuthorEndpoint {
    /**
     * 查询作者列表。
     */
    list(filter: AuthorFilter): Promise<Response<Author[]>>
    /**
     * 新建作者。
     * @exception ALREADY_EXISTS ("Author", "name", name) 作者重名
     * @exception NOT_EXISTS ("annotations", id) 指定的资源不存在
     * @exception NOT_SUITABLE ("annotations", id) 指定的资源不适用。对于annotations，此注解的target要求不能应用于此种类的tag
     */
    create(form: AuthorCreateForm): Promise<Response<IdResponse>>
    /**
     * 查看作者。
     * @exception NOT_FOUND
     */
    get(id: number): Promise<Response<Author>>
    /**
     * 更改作者。
     * @exception NOT_FOUND
     * @exception ALREADY_EXISTS ("Author", "name", name) 作者重名
     * @exception NOT_EXISTS ("annotations", id) 指定的资源不存在
     * @exception NOT_SUITABLE ("annotations", id) 指定的资源不适用。对于annotations，此注解的target要求不能应用于此种类的tag
     */
    update(id: number, form: AuthorUpdateForm): Promise<Response<unknown>>
    /**
     * 删除作者。
     * @exception NOT_FOUND
     */
    delete(id: number): Promise<Response<unknown>>
}

export type AuthorType = "UNKNOWN" | "ARTIST" | "STUDIO" | "PUBLISH"

export interface Author {
    /**
     * author id。
     */
    id: number
    /**
     * 作者名称。需要遵守tag name规范。
     */
    name: string
    /**
     * 作者类型。
     */
    type: AuthorType
    /**
     * 标记为喜爱。
     */
    favorite: boolean
    /**
     * 注解。
     */
    annotations: SimpleAnnotation[]
    /**
     * 手写的评分或关联的项目的平均分。
     */
    score: number | null
    /**
     * 关联的项目数量。
     */
    count: number
}

export interface DetailAuthor extends Author {
    /**
     * 其他名称。需要遵守tag name规范。
     */
    otherNames: string[]
    /**
     * 简介。
     */
    description: string
    /**
     * 相关链接。
     */
    links: Link[]
}

export interface SimpleAuthor {
    id: number
    name: string
    isExported: boolean
}

export interface AuthorCreateForm {
    name: string
    otherNames?: string[] | null
    type?: AuthorType
    description?: string
    links?: Link[] | null
    annotations?: (string | number)[] | null
    favorite?: boolean
    score?: number | null
}

export interface AuthorUpdateForm {
    name?: string
    otherNames?: string[] | null
    type?: AuthorType
    description?: string
    links?: Link[] | null
    annotations?: (string | number)[] | null
    favorite?: boolean
    score?: number | null
}

export interface AuthorFilter extends LimitAndOffsetFilter {
    query?: string
    order?: OrderList<"id" | "name" | "score" | "count" | "createTime" | "updateTime">
    type?: AuthorType
    favorite?: boolean
}
