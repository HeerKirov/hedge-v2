package com.heerkirov.hedge.server.service

import com.heerkirov.hedge.server.components.database.DataRepository
import com.heerkirov.hedge.server.components.database.transaction
import com.heerkirov.hedge.server.dao.*
import com.heerkirov.hedge.server.exceptions.NotFound
import com.heerkirov.hedge.server.form.*
import com.heerkirov.hedge.server.manager.TopicManager
import com.heerkirov.hedge.server.model.Illust
import com.heerkirov.hedge.server.utils.ktorm.OrderTranslator
import com.heerkirov.hedge.server.utils.ktorm.first
import com.heerkirov.hedge.server.utils.ktorm.orderBy
import com.heerkirov.hedge.server.utils.types.ListResult
import com.heerkirov.hedge.server.utils.types.anyOpt
import com.heerkirov.hedge.server.utils.types.toListResult
import com.heerkirov.hedge.server.utils.types.undefined
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.firstOrNull
import me.liuwj.ktorm.entity.sequenceOf

class TopicService(private val data: DataRepository, private val topicMgr: TopicManager) {
    private val orderTranslator = OrderTranslator {
        "id" to Topics.id
        "name" to Topics.name
        "score" to Topics.exportedScore nulls last
        "count" to Topics.cachedCount nulls last
    }

    fun list(filter: TopicFilter): ListResult<TopicRes> {
        return data.db.from(Topics).select()
            .whereWithConditions {
                if(filter.favorite != null) { it += Topics.favorite eq filter.favorite }
                if(filter.type != null) { it += Topics.type eq filter.type }
                if(filter.parentId != null) { it += Topics.parentId eq filter.parentId }
            }
            .orderBy(filter.order, orderTranslator)
            .limit(filter.offset, filter.limit)
            .toListResult { newTopicRes(Topics.createEntity(it)) }
    }

    fun create(form: TopicCreateForm): Int {
        data.db.transaction {
            val name = topicMgr.validateName(form.name)
            val otherNames = topicMgr.validateOtherNames(form.otherNames)

            val parentId = form.parentId?.apply { topicMgr.validateParentType(this, form.type) }

            val annotations = topicMgr.validateAnnotations(form.annotations, form.type)

            val id = data.db.insertAndGenerateKey(Topics) {
                set(it.name, name)
                set(it.otherNames, otherNames)
                set(it.parentId, parentId)
                set(it.description, form.description)
                set(it.type, form.type)
                set(it.links, form.links)
                set(it.favorite, form.favorite)
                set(it.score, form.score)
                set(it.exportedScore, form.score ?: 0)
                set(it.cachedCount, 0)
                set(it.cachedAnnotations, annotations)
            } as Int

            topicMgr.processAnnotations(id, annotations.asSequence().map { it.id }.toSet(), creating = true)

            return id
        }
    }

    fun get(id: Int): TopicDetailRes {
        val topic = data.db.sequenceOf(Topics).firstOrNull { it.id eq id } ?: throw NotFound()
        val parent = topic.parentId?.let { parentId -> data.db.sequenceOf(Topics).firstOrNull { it.id eq parentId } }
        return newTopicDetailRes(topic, parent)
    }

    fun update(id: Int, form: TopicUpdateForm) {
        data.db.transaction {
            val record = data.db.sequenceOf(Topics).firstOrNull { it.id eq id } ?: throw NotFound()

            val newName = form.name.letOpt { topicMgr.validateName(it, id) }
            val newOtherNames = form.otherNames.letOpt { topicMgr.validateOtherNames(it) }

            val newParentId = if(form.parentId.isPresent || form.type.isPresent) {
                form.parentId.also {
                    it.unwrapOr { record.parentId }?.let { parentId ->
                        topicMgr.validateParentType(parentId, form.type.unwrapOr { record.type }, id)
                    }
                }
            }else undefined()

            form.type.letOpt { type -> topicMgr.checkChildrenType(id, type) }

            val newExportedScore = form.score.letOpt {
                it ?: data.db.from(Illusts)
                    .innerJoin(IllustTopicRelations, Illusts.id eq IllustTopicRelations.illustId)
                    .select(count(Illusts.exportedScore).aliased("count"))
                    .where { (IllustTopicRelations.topicId eq id) and (Illusts.type eq Illust.Type.IMAGE) or (Illusts.type eq Illust.Type.IMAGE_WITH_PARENT) }
                    .first().getInt("count")
            }

            val newAnnotations = form.annotations.letOpt { topicMgr.validateAnnotations(it, form.type.unwrapOr { record.type }) }

            if(anyOpt(newName, newOtherNames, newParentId, form.type, form.description, form.links, form.favorite, form.score, newExportedScore, newAnnotations)) {
                data.db.update(Topics) {
                    where { it.id eq id }
                    newName.applyOpt { set(it.name, this) }
                    newOtherNames.applyOpt { set(it.otherNames, this) }
                    newParentId.applyOpt { set(it.parentId, this) }
                    form.type.applyOpt { set(it.type, this) }
                    form.description.applyOpt { set(it.description, this) }
                    form.links.applyOpt { set(it.links, this) }
                    form.favorite.applyOpt { set(it.favorite, this) }
                    form.score.applyOpt { set(it.score, this) }
                    newExportedScore.applyOpt { set(it.exportedScore, this) }
                    newAnnotations.applyOpt { set(it.cachedAnnotations, this) }
                }
            }


            newAnnotations.letOpt { annotations -> topicMgr.processAnnotations(id, annotations.asSequence().map { it.id }.toSet()) }
        }
    }

    fun delete(id: Int) {
        data.db.transaction {
            data.db.delete(Topics) { it.id eq id }.let {
                if(it <= 0) throw NotFound()
            }
            data.db.delete(IllustTopicRelations) { it.topicId eq id }
            data.db.delete(AlbumTopicRelations) { it.topicId eq id }
            data.db.delete(TopicAnnotationRelations) { it.topicId eq id }
            data.db.update(Topics) {
                //删除topic时，不会像tag那样递归删除子标签，而是将子标签的parent设为null。
                where { it.parentId eq id }
                set(it.parentId, null)
            }
        }
    }
}