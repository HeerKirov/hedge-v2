package com.heerkirov.hedge.server.exceptions

//== 基础错误类型 ==

/**
 * 输入存在错误。
 */
abstract class BadRequestException(code: String, message: String, info: Any? = null) : BaseException(400, code, message, info)

/**
 * 要求附带登录认证。
 */
abstract class UnauthorizedException(code: String, message: String, info: Any? = null) : BaseException(401, code, message, info)

/**
 * 当前登录认证的权限不足以访问。
 */
abstract class ForbiddenException(code: String, message: String, info: Any? = null) : BaseException(403, code, message, info)

/**
 * 找不到当前的主体资源。
 */
abstract class NotFoundException(code: String, message: String, info: Any? = null) : BaseException(404, code, message, info)

//== 在此基础上扩展的通用错误类型 ==

/**
 * 表单参数的类型错误。
 */
open class ParamTypeError(paramName: String, reason: String) : BadRequestException("PARAM_TYPE_ERROR", "Param '$paramName' $reason", paramName), Unchecked

/**
 * 表单参数的值错误。
 */
open class ParamError(paramName: String) : BadRequestException("PARAM_ERROR", "Param '$paramName' has incorrect value.", paramName), Unchecked

/**
 * 表单参数的值空缺，但是业务需要这个值。
 */
open class ParamRequired(paramName: String) : BadRequestException("PARAM_REQUIRED", "Param '$paramName' is required.", paramName), Unchecked

/**
 * 表单参数的值已填写，但业务不需要这个值。
 */
open class ParamNotRequired(paramName: String) : BadRequestException("PARAM_NOT_REQUIRED", "Param '$paramName' is not required.", paramName), Unchecked

/**
 * 表单参数选取的某种目标资源并不存在，因此业务无法进行。
 */
open class ResourceNotExist : BadRequestException, Unchecked {
    /**
     * 指明是一项属性的目标资源。
     */
    constructor(prop: String) : super("NOT_EXIST", "Resource of $prop is not exist.", listOf(prop))
    /**
     * 指明是一项属性的目标资源。同时指明具体的值。
     */
    constructor(prop: String, value: Any) : super("NOT_EXIST", "Resource of $prop '$value' is not exist.", listOf(prop, value))
    /**
     * 指明是一项属性的目标资源。同时指明具体的值。
     */
    constructor(prop: String, value: Collection<Any>) : super("NOT_EXIST", "Resource of $prop '${value.joinToString(", ")}' is not exist.", listOf(prop, value))
}

/**
 * 表单参数选取的某种目标资源在当前业务中不适用，因此业务无法进行。
 */
open class ResourceNotSuitable : BadRequestException, Unchecked {
    /**
     * 指明是一项属性的目标资源。
     */
    constructor(prop: String) : super("NOT_SUITABLE", "Resource of $prop is not suitable.", listOf(prop))
    /**
     * 指明是一项属性的目标资源。同时指明具体的值。
     */
    constructor(prop: String, value: Any) : super("NOT_SUITABLE", "Resource of $prop '$value' is not suitable.", listOf(prop, value))
    /**
     * 指明是一项属性的目标资源。同时指明具体的值。
     */
    constructor(prop: String, value: Collection<Any>) : super("NOT_SUITABLE", "Resource of $prop '${value.joinToString(", ")}' is not suitable.", listOf(prop, value))
}

/**
 * 表单的某种目标资源已经存在，因此业务无法进行。
 */
open class AlreadyExists : BadRequestException {
    /**
     * 指定资源名称、资源属性、属性值。
     */
    constructor(resource: String, prop: String, value: Any) : super("ALREADY_EXISTS", "$resource with $prop '$value' is already exists.", listOf(resource, prop, value))
    /**
     * 只指定资源名称。
     */
    constructor(resource: String) : super("ALREADY_EXISTS", "$resource is already exists.", listOf(resource))
}

/**
 * 表单的某种目标资源重复给出，业务受此影响无法进行。
 */
open class ResourceDuplicated(prop: String, value: Collection<Any>) : BadRequestException("DUPLICATED", "Param $prop [$value] is duplicated.", listOf(prop, value)), Unchecked

/**
 * 当前资源存在某种级联的资源依赖，因此业务无法进行。
 * 多见于删除业务，且目标资源不允许被静默操作的情况，需要此错误提示，并搭配一个强制删除参数。
 */
open class CascadeResourceExists(resource: String) : BadRequestException("CASCADE_RESOURCE_EXISTS", "$resource depending on this exists.", resource)

/**
 * API的操作或一部分操作，因为某种原因拒绝执行。
 */
open class Reject(message: String): BadRequestException("REJECT", message)

/**
 * 由于服务尚未初始化，API不能被调用。
 */
open class NotInit: BadRequestException("NOT_INIT", "Server is not initialized.")

/**
 * 在headers中没有发现任何token，然而此API需要验证。或者token无法被正确解析。
 */
class NoToken : UnauthorizedException("NO_TOKEN", "No available token.")

/**
 * 使用的token是错误的，无法将其与任何token认证匹配。
 */
class TokenWrong : UnauthorizedException("TOKEN_WRONG", "Token is incorrect.")

/**
 * 使用的password是错误的。
 */
class PasswordWrong : UnauthorizedException("PASSWORD_WRONG", "Password is incorrect.")

/**
 * 此API只能在web端调用。
 */
class OnlyForWeb : ForbiddenException("ONLY_FOR_WEB", "This API can only be called from web.")

/**
 * 此API只能在客户端调用。
 */
class OnlyForClient : ForbiddenException("ONLY_FOR_CLIENT", "This API can only be called from client.")

/**
 * 此token只能由localhost使用。
 */
class RemoteDisabled : ForbiddenException("REMOTE_DISABLED", "This Token can only be used in localhost.")

/**
 * 当前主体资源未找到。
 */
class NotFound(resourceName: String? = null) : NotFoundException("NOT_FOUND", "${resourceName ?: "Resource"} not found.")
