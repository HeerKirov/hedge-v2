package com.heerkirov.hedge.server.components.service

import com.heerkirov.hedge.server.components.database.DataRepository
import com.heerkirov.hedge.server.components.database.transaction
import com.heerkirov.hedge.server.components.kit.AnnotationKit
import com.heerkirov.hedge.server.components.manager.query.QueryManager
import com.heerkirov.hedge.server.dao.album.AlbumAnnotationRelations
import com.heerkirov.hedge.server.dao.illust.IllustAnnotationRelations
import com.heerkirov.hedge.server.dao.meta.Annotations
import com.heerkirov.hedge.server.dao.meta.AuthorAnnotationRelations
import com.heerkirov.hedge.server.dao.meta.TagAnnotationRelations
import com.heerkirov.hedge.server.dao.meta.TopicAnnotationRelations
import com.heerkirov.hedge.server.exceptions.NotFound
import com.heerkirov.hedge.server.form.*
import com.heerkirov.hedge.server.utils.DateTime
import com.heerkirov.hedge.server.utils.types.anyOpt
import com.heerkirov.hedge.server.utils.ktorm.compositionContains
import com.heerkirov.hedge.server.utils.types.ListResult
import com.heerkirov.hedge.server.utils.types.toListResult
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.firstOrNull
import me.liuwj.ktorm.entity.sequenceOf

class AnnotationService(private val data: DataRepository, private val kit: AnnotationKit, private val queryManager: QueryManager) {
    fun list(filter: AnnotationFilter): ListResult<AnnotationRes> {
        val schema = if(filter.query.isNullOrBlank()) null else {
            queryManager.querySchema(filter.query, QueryManager.Dialect.ANNOTATION).executePlan ?: return ListResult(0, emptyList())
        }
        return data.db.from(Annotations).select()
            .whereWithConditions {
                if(filter.canBeExported != null) { it += Annotations.canBeExported eq filter.canBeExported }
                if(filter.target != null) { it += Annotations.target compositionContains filter.target }
                if(schema != null && schema.whereConditions.isNotEmpty()) {
                    it.addAll(schema.whereConditions)
                }
            }
            .limit(filter.offset, filter.limit)
            .orderBy(Annotations.createTime.desc())
            .toListResult { newAnnotationRes(Annotations.createEntity(it)) }
    }

    fun create(form: AnnotationCreateForm): Int {
        data.db.transaction {
            val createTime = DateTime.now()
            val name = kit.validateName(form.name)
            return data.db.insertAndGenerateKey(Annotations) {
                set(it.name, name)
                set(it.canBeExported, form.canBeExported)
                set(it.target, form.target)
                set(it.createTime, createTime)
            } as Int
        }
    }

    fun get(id: Int): AnnotationRes {
        return data.db.sequenceOf(Annotations).firstOrNull { it.id eq id }
            ?.let { newAnnotationRes(it) }
            ?: throw NotFound()
    }

    fun update(id: Int, form: AnnotationUpdateForm) {
        data.db.transaction {
            data.db.sequenceOf(Annotations).firstOrNull { it.id eq id } ?: throw NotFound()

            val newName = form.name.letOpt { kit.validateName(it, id) }
            if(anyOpt(newName, form.canBeExported, form.target)) {
                data.db.update(Annotations) {
                    where { it.id eq id }

                    newName.applyOpt { set(it.name, this) }
                    form.canBeExported.applyOpt { set(it.canBeExported, this) }
                    form.target.applyOpt { set(it.target, this) }
                }

                queryManager.flushCacheOf(QueryManager.CacheType.AUTHOR)
            }
        }
    }

    fun delete(id: Int) {
        data.db.transaction {
            data.db.delete(Annotations) { it.id eq id }.let {
                if(it <= 0) NotFound()
            }
            data.db.delete(IllustAnnotationRelations) { it.annotationId eq id }
            data.db.delete(AlbumAnnotationRelations) { it.annotationId eq id }
            data.db.delete(TagAnnotationRelations) { it.annotationId eq id }

            kit.updateAnnotationCacheForDelete(id)
            data.db.delete(AuthorAnnotationRelations) { it.annotationId eq id }
            data.db.delete(TopicAnnotationRelations) { it.annotationId eq id }

            queryManager.flushCacheOf(QueryManager.CacheType.AUTHOR)
        }
    }
}