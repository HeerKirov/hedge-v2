package com.heerkirov.hedge.server.components.manager

import com.heerkirov.hedge.server.components.database.DataRepository
import com.heerkirov.hedge.server.dao.meta.Annotations
import com.heerkirov.hedge.server.exceptions.*
import com.heerkirov.hedge.server.model.meta.Annotation
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.filter
import me.liuwj.ktorm.entity.sequenceOf
import me.liuwj.ktorm.entity.toList

class AnnotationManager(private val data: DataRepository) {
    /**
     * 解析一个由string和int组成的annotations列表，对其校验、纠错，并返回一个等价的、包含id和name的集合。
     * @throws ParamTypeError 存在元素不是int或string时，抛出此异常。
     * @throws ResourceNotExist 有元素不存在时，抛出此异常。
     * @throws ResourceNotSuitable 指定target类型且有元素不满足此类型时，抛出此异常。
     */
    fun analyseAnnotationParam(annotations: List<Any>, target: Annotation.AnnotationTarget? = null, paramName: String = "annotations"): Map<Int, String> {
        if(annotations.any { it !is Int && it !is String }) {
            throw ParamTypeError(paramName, " must be id(Int) or name(String).")
        }
        val ids = annotations.filterIsInstance<Int>()
        val names = annotations.filterIsInstance<String>()

        val resultFromIds = data.db.sequenceOf(Annotations)
            .filter { Annotations.id inList ids }
            .toList()
            .also { rows ->
                if(rows.size < ids.size) {
                    val minus = ids.toSet() - rows.asSequence().map { it.id }.toSet()
                    throw ResourceNotExist(paramName, minus)
                }
            }.also { rows ->
                rows.filter { target != null && !it.target.isEmpty() && !it.target.any(target) }.takeIf { it.isNotEmpty() }?.let {
                    throw ResourceNotSuitable(paramName, it.map { a -> a.id })
                }
            }.map { Pair(it.id, it.name) }

        val resultFromNames = data.db.sequenceOf(Annotations)
            .filter { Annotations.name inList names }
            .toList()
            .also { rows ->
                if(rows.size < names.size) {
                    val minus = names.toSet() - rows.asSequence().map { it.name }.toSet()
                    throw ResourceNotExist(paramName, minus)
                }
            }.also { rows ->
                rows.filter { target != null && !it.target.isEmpty() && target !in it.target }.takeIf { it.isNotEmpty() }?.let {
                    throw ResourceNotSuitable(paramName, it.map { a -> a.id })
                }
            }
            .map { Pair(it.id, it.name) }

        return (resultFromIds + resultFromNames).toMap()
    }
}