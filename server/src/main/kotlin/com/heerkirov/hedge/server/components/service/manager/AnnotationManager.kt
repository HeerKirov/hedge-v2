package com.heerkirov.hedge.server.components.service.manager

import com.heerkirov.hedge.server.components.database.DataRepository
import com.heerkirov.hedge.server.dao.*
import com.heerkirov.hedge.server.exceptions.*
import com.heerkirov.hedge.server.model.Annotation
import com.heerkirov.hedge.server.utils.ktorm.asSequence
import com.heerkirov.hedge.server.utils.runIf
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.any
import me.liuwj.ktorm.entity.filter
import me.liuwj.ktorm.entity.sequenceOf
import me.liuwj.ktorm.entity.toList

class AnnotationManager(private val data: DataRepository) {
    /**
     * 校验并纠正name，同时对name进行查重。
     * @param thisId 指定此参数时，表示是在对一个项进行更新，此时绕过此id的记录的重名。
     */
    fun validateName(newName: String, thisId: Int? = null): String {
        return newName.trim().apply {
            if(!GeneralManager.checkTagName(this)) throw ParamError("name")
            if(data.db.sequenceOf(Annotations).any { (it.name eq newName).runIf(thisId != null) { and (it.id notEq thisId!!) } })
                throw AlreadyExists("Annotation", "name", newName)
        }
    }

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

    /**
     * 更新与此annotation关联的author/topic的annotation cache，删除当前注解。
     */
    fun updateAnnotationCacheForDelete(annotationId: Int) {
        val authors = data.db.from(Authors)
            .innerJoin(AuthorAnnotationRelations, AuthorAnnotationRelations.authorId eq Authors.id)
            .select(Authors.id, Authors.cachedAnnotations)
            .where { AuthorAnnotationRelations.annotationId eq annotationId }
            .asSequence()
            .map { Pair(it[Authors.id]!!, it[Authors.cachedAnnotations]!!) }
            .map { (id, cache) -> Pair(id, cache.filter { it.id != annotationId }) }
            .toMap()
        data.db.batchUpdate(Authors) {
            for ((id, cache) in authors) {
                item {
                    where { it.id eq id }
                    set(it.cachedAnnotations, cache)
                }
            }
        }

        val topics = data.db.from(Topics)
            .innerJoin(TopicAnnotationRelations, TopicAnnotationRelations.topicId eq Topics.id)
            .select(Topics.id, Topics.cachedAnnotations)
            .where { TopicAnnotationRelations.annotationId eq annotationId }
            .asSequence()
            .map { Pair(it[Topics.id]!!, it[Topics.cachedAnnotations]!!) }
            .map { (id, cache) -> Pair(id, cache.filter { it.id != annotationId }) }
            .toMap()
        data.db.batchUpdate(Topics) {
            for ((id, cache) in topics) {
                item {
                    where { it.id eq id }
                    set(it.cachedAnnotations, cache)
                }
            }
        }
    }
}