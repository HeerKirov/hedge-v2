package com.heerkirov.hedge.server.components.service

import com.heerkirov.hedge.server.components.backend.AlbumExporterTask
import com.heerkirov.hedge.server.components.backend.IllustExporterTask
import com.heerkirov.hedge.server.components.backend.IllustMetaExporter
import com.heerkirov.hedge.server.components.database.DataRepository
import com.heerkirov.hedge.server.components.database.transaction
import com.heerkirov.hedge.server.exceptions.*
import com.heerkirov.hedge.server.form.*
import com.heerkirov.hedge.server.components.manager.FileManager
import com.heerkirov.hedge.server.components.kit.TagKit
import com.heerkirov.hedge.server.components.manager.query.QueryManager
import com.heerkirov.hedge.server.dao.album.AlbumTagRelations
import com.heerkirov.hedge.server.dao.illust.IllustTagRelations
import com.heerkirov.hedge.server.dao.illust.Illusts
import com.heerkirov.hedge.server.dao.meta.Annotations
import com.heerkirov.hedge.server.dao.meta.TagAnnotationRelations
import com.heerkirov.hedge.server.dao.meta.Tags
import com.heerkirov.hedge.server.dao.source.FileRecords
import com.heerkirov.hedge.server.model.meta.Tag
import com.heerkirov.hedge.server.tools.takeThumbnailFilepath
import com.heerkirov.hedge.server.utils.*
import com.heerkirov.hedge.server.utils.ktorm.OrderTranslator
import com.heerkirov.hedge.server.utils.ktorm.orderBy
import com.heerkirov.hedge.server.utils.types.*
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.*

class TagService(private val data: DataRepository,
                 private val kit: TagKit,
                 private val fileManager: FileManager,
                 private val queryManager: QueryManager,
                 private val illustMetaExporter: IllustMetaExporter) {
    private val orderTranslator = OrderTranslator {
        "id" to Tags.id
        "name" to Tags.name
        "ordinal" to Tags.ordinal
        "createTime" to Tags.createTime
        "updateTime" to Tags.updateTime
    }

    fun list(filter: TagFilter): ListResult<TagRes> {
        return data.db.from(Tags).select()
            .whereWithConditions {
                if(filter.parent != null) { it += Tags.parentId eq filter.parent }
                if(filter.type != null) { it += Tags.type eq filter.type }
                if(filter.group != null) { it += if(filter.group) Tags.isGroup notEq Tag.IsGroup.NO else Tags.isGroup eq Tag.IsGroup.NO }
            }
            .orderBy(orderTranslator, filter.order, default = ascendingOrderItem("ordinal"))
            .limit(filter.offset, filter.limit)
            .toListResult {
                newTagRes(Tags.createEntity(it))
            }
    }

    fun tree(filter: TagTreeFilter): List<TagTreeNode> {
        val records = data.db.sequenceOf(Tags).asKotlinSequence().groupBy { it.parentId }

        fun generateNodeList(key: Int?): List<TagTreeNode>? = records[key]
            ?.sortedBy { it.ordinal }
            ?.map { newTagTreeNode(it, generateNodeList(it.id)) }

        return generateNodeList(filter.parent) ?: emptyList()
    }

    fun create(form: TagCreateForm): Int {
        val name = kit.validateName(form.name)
        val otherNames = kit.validateOtherNames(form.otherNames)

        data.db.transaction {
            //检查parent是否存在
            val parent = form.parentId?.let { parentId -> data.db.sequenceOf(Tags).firstOrNull { it.id eq parentId } ?: throw ResourceNotExist("parentId", form.parentId) }

            //检查颜色，只有顶层tag允许指定颜色
            if(form.color != null && parent != null) throw CannotGiveColorError()

            //检查标签重名
            //addr类型的标签在相同的parent下重名
            //tag类型的标签除上一条外，还禁止与全局的其他tag类型标签重名
            if(form.type == Tag.Type.TAG) {
                if(data.db.sequenceOf(Tags).any { (if(form.parentId != null) { Tags.parentId eq form.parentId }else{ Tags.parentId.isNull() } or (it.type eq Tag.Type.TAG)) and (it.name eq name) }) throw AlreadyExists("Tag", "name", name)
            }else{
                if(data.db.sequenceOf(Tags).any { if(form.parentId != null) { Tags.parentId eq form.parentId }else{ Tags.parentId.isNull() } and (it.name eq name) }) throw AlreadyExists("Tag", "name", name)
            }

            //存在link时，检查link的目标是否存在
            val links = kit.validateLinks(form.links)

            //存在example时，检查example的目标是否存在，以及限制illust不能是collection
            val examples = kit.validateExamples(form.examples)

            val tagCountInParent by lazy {
                data.db.sequenceOf(Tags)
                    .filter { if(form.parentId != null) { Tags.parentId eq form.parentId }else{ Tags.parentId.isNull() } }
                    .count()
            }

            //未指定ordinal时，将其排在序列的末尾，相当于当前的序列长度
            //已指定ordinal时，按照指定的ordinal排序，并且不能超出[0, count]的范围
            val ordinal = if(form.ordinal == null) {
                tagCountInParent
            }else when {
                form.ordinal < 0 -> 0
                form.ordinal >= tagCountInParent -> tagCountInParent
                else -> form.ordinal
            }.also { ordinal ->
                data.db.update(Tags) {
                    //同parent下，ordinal>=newOrdinal的那些tag，向后顺延一位
                    where { if(form.parentId != null) { Tags.parentId eq form.parentId }else{ Tags.parentId.isNull() } and (it.ordinal greaterEq ordinal)  }
                    set(it.ordinal, it.ordinal + 1)
                }
            }

            val createTime = DateTime.now()

            val id = data.db.insertAndGenerateKey(Tags) {
                set(it.name, name)
                set(it.otherNames, otherNames)
                set(it.ordinal, ordinal)
                set(it.parentId, form.parentId)
                set(it.type, form.type)
                set(it.isGroup, form.group)
                set(it.description, form.description)
                set(it.color, parent?.color ?: form.color)
                set(it.links, links)
                set(it.examples, examples)
                set(it.exportedScore, null)
                set(it.cachedCount, 0)
                set(it.createTime, createTime)
                set(it.updateTime, createTime)
            } as Int

            kit.processAnnotations(id, form.annotations, creating = true)

            return id
        }
    }

    fun get(id: Int): TagDetailRes {
        val tag = data.db.sequenceOf(Tags).firstOrNull { it.id eq id } ?: throw NotFound()

        val annotations = data.db.from(TagAnnotationRelations)
            .innerJoin(Annotations, TagAnnotationRelations.annotationId eq Annotations.id)
            .select(Annotations.id, Annotations.name, Annotations.canBeExported)
            .where { TagAnnotationRelations.tagId eq id }
            .map { TagDetailRes.Annotation(it[Annotations.id]!!, it[Annotations.name]!!, it[Annotations.canBeExported]!!) }

        val examples = if(tag.examples.isNullOrEmpty()) emptyList() else data.db.from(Illusts)
            .innerJoin(FileRecords, FileRecords.id eq Illusts.fileId)
            .select(Illusts.id, FileRecords.id, FileRecords.folder, FileRecords.extension, FileRecords.thumbnail)
            .where { Illusts.id inList tag.examples }
            .map { IllustSimpleRes(it[Illusts.id]!!, takeThumbnailFilepath(it)) }

        return newTagDetailRes(tag, annotations, examples)
    }

    fun update(id: Int, form: TagUpdateForm) {
        data.db.transaction {
            val record = data.db.sequenceOf(Tags).firstOrNull { it.id eq id } ?: throw NotFound()

            val newName = form.name.letOpt { kit.validateName(it) }
            val newOtherNames = form.otherNames.letOpt { kit.validateOtherNames(it) }
            val newLinks = form.links.runOpt { kit.validateLinks(this) }
            val newExamples = form.examples.runOpt { kit.validateExamples(this) }

            val (newParentId, newOrdinal) = if(form.parentId.isPresent && form.parentId.value != record.parentId) {
                //parentId发生了变化
                val newParentId = form.parentId.value

                if(newParentId != null) {
                    tailrec fun recursiveCheckParent(id: Int, chains: Set<Int>) {
                        if(id in chains) {
                            //在过去经历过的parent中发现了重复的id，判定存在闭环
                            throw RecursiveParentError()
                        }
                        val parent = data.db.from(Tags)
                            .select(Tags.parentId)
                            .where { Tags.id eq id }
                            .limit(0, 1)
                            .map { optOf(it[Tags.parentId]) }
                            .firstOrNull()
                            //检查parent是否存在
                            ?: throw ResourceNotExist("parentId", newParentId)
                        val parentId = parent.value
                        if(parentId != null) recursiveCheckParent(parentId, chains + id)
                    }

                    recursiveCheckParent(newParentId, setOf(id))
                }

                //调整旧的parent下的元素顺序
                data.db.update(Tags) {
                    where { if(record.parentId != null) { Tags.parentId eq record.parentId }else{ Tags.parentId.isNull() } and (it.ordinal greater record.ordinal) }
                    set(it.ordinal, it.ordinal - 1)
                }

                val tagsInNewParent = data.db.sequenceOf(Tags)
                    .filter { if(newParentId != null) { Tags.parentId eq newParentId }else{ Tags.parentId.isNull() } }
                    .toList()

                Pair(optOf(newParentId), if(form.ordinal.isPresent) {
                    //指定了新的ordinal
                    val max = tagsInNewParent.size
                    val newOrdinal = if(form.ordinal.value > max) max else form.ordinal.value

                    data.db.update(Tags) {
                        where { if(newParentId != null) { Tags.parentId eq newParentId }else{ Tags.parentId.isNull() } and (it.ordinal greaterEq newOrdinal) }
                        set(it.ordinal, it.ordinal + 1)
                    }
                    optOf(newOrdinal)
                }else{
                    //没有指定新ordinal，追加到末尾
                    optOf(tagsInNewParent.size)
                })
            }else{
                //parentId没有变化，只在当前范围内变动
                val tagsInParent = data.db.sequenceOf(Tags)
                    .filter { if(record.parentId != null) { Tags.parentId eq record.parentId }else{ Tags.parentId.isNull() } }
                    .toList()
                Pair(undefined(), if(form.ordinal.isUndefined || form.ordinal.value == record.ordinal) undefined() else {
                    //ordinal发生了变化
                    val max = tagsInParent.size
                    val newOrdinal = if(form.ordinal.value > max) max else form.ordinal.value
                    if(newOrdinal > record.ordinal) {
                        data.db.update(Tags) {
                            where { if(record.parentId != null) { Tags.parentId eq record.parentId }else{ Tags.parentId.isNull() } and (it.ordinal greater record.ordinal) and (it.ordinal lessEq newOrdinal) }
                            set(it.ordinal, it.ordinal - 1)
                        }
                    }else{
                        data.db.update(Tags) {
                            where { if(record.parentId != null) { Tags.parentId eq record.parentId }else{ Tags.parentId.isNull() } and (it.ordinal greaterEq newOrdinal) and (it.ordinal less record.ordinal) }
                            set(it.ordinal, it.ordinal + 1)
                        }
                    }
                    optOf(newOrdinal)
                })
            }

            val newColor = if(form.color.isPresent) {
                //指定新color。此时如果parent为null，新color为指定的color，否则抛异常
                newParentId.unwrapOr { record.parentId }?.let { throw CannotGiveColorError() } ?: optOf(form.color.value)
            }else{
                //没有指定新color
                if(newParentId.isPresent && newParentId.value != null) {
                    //指定的parent且不是null，此时new color为新parent的color
                    data.db.from(Tags).select(Tags.color).where { Tags.id eq newParentId.value!! }.map { optOf(it[Tags.color]!!) }.first()
                }else{
                    //color和parent都没有变化，不修改color的值
                    //指定新parent为null，策略是继承之前的颜色，因此也不修改color的值
                    undefined()
                }
            }

            applyIf(form.type.isPresent || form.name.isPresent || form.parentId.isPresent) {
                //type/name/parentId的变化会触发重名检查
                val name = newName.unwrapOr { record.name }
                val type = form.type.unwrapOr { record.type }
                val parentId = newParentId.unwrapOr { record.parentId }
                //检查标签重名
                //addr类型的标签在相同的parent下重名
                //tag类型的标签除上一条外，还禁止与全局的其他tag类型标签重名
                //更新动作还要排除自己，防止与自己重名的检查
                if(type == Tag.Type.TAG) {
                    if(data.db.sequenceOf(Tags).any {
                            (if(parentId != null) { Tags.parentId eq parentId }else{ Tags.parentId.isNull() } or (it.type eq Tag.Type.TAG)) and (it.name eq name) and (it.id notEq record.id)
                    }) throw AlreadyExists("Tag", "name", name)
                }else{
                    if(data.db.sequenceOf(Tags).any {
                            if(parentId != null) { Tags.parentId eq parentId }else{ Tags.parentId.isNull() } and (it.name eq name) and (it.id notEq record.id)
                    }) throw AlreadyExists("Tag", "name", name)
                }
            }

            form.annotations.letOpt { newAnnotations -> kit.processAnnotations(id, newAnnotations) }

            newColor.letOpt { color ->
                fun recursionUpdateColor(parentId: Int) {
                    data.db.update(Tags) {
                        where { it.parentId eq parentId }
                        set(it.color, color)
                    }
                    data.db.from(Tags).select(Tags.id).where { Tags.parentId eq parentId }.map { it[Tags.id]!! }.forEach(::recursionUpdateColor)
                }
                recursionUpdateColor(id)
            }

            if(anyOpt(newName, newOtherNames, form.type, form.description, newLinks, newExamples, newParentId, newOrdinal, newColor)) {
                data.db.update(Tags) {
                    where { it.id eq id }

                    newName.applyOpt { set(it.name, this) }
                    newOtherNames.applyOpt { set(it.otherNames, this) }
                    form.type.applyOpt { set(it.type, this) }
                    form.description.applyOpt { set(it.description, this) }
                    newLinks.applyOpt { set(it.links, this) }
                    newExamples.applyOpt { set(it.examples, this) }
                    newParentId.applyOpt { set(it.parentId, this) }
                    newOrdinal.applyOpt { set(it.ordinal, this) }
                    newColor.applyOpt { set(it.color, this) }
                }
            }

            if ((newLinks.isPresent && newLinks.value != record.links) ||
                (form.type.isPresent && form.type.value != record.type) ||
                (newParentId.isPresent && newParentId.value != record.parentId) ||
                form.annotations.isPresent) {
                    //发生关系类变化时，将关联的illust/album重导出
                    data.db.from(IllustTagRelations)
                        .select(IllustTagRelations.illustId)
                        .where { IllustTagRelations.tagId eq id }
                        .map { IllustExporterTask(it[IllustTagRelations.illustId]!!, exportMeta = true, exportDescription = false, exportFileAndTime = false, exportScore = false) }
                        .let { illustMetaExporter.appendNewTask(it) }
                    data.db.from(AlbumTagRelations)
                        .select(AlbumTagRelations.albumId)
                        .where { AlbumTagRelations.tagId eq id }
                        .map { AlbumExporterTask(it[AlbumTagRelations.albumId]!!, exportMeta = true) }
                        .let { illustMetaExporter.appendNewTask(it) }

                    queryManager.flushCacheOf(QueryManager.CacheType.TAG)
            }
        }
    }

    fun delete(id: Int) {
        fun recursionDelete(id: Int) {
            data.db.delete(Tags) { it.id eq id }
            data.db.delete(IllustTagRelations) { it.tagId eq id }
            data.db.delete(AlbumTagRelations) { it.tagId eq id }
            data.db.delete(TagAnnotationRelations) { it.tagId eq id }
            val children = data.db.from(Tags).select(Tags.id).where { Tags.parentId eq id }.map { it[Tags.id]!! }
            for (child in children) {
                recursionDelete(child)
            }
        }
        data.db.transaction {
            if(data.db.sequenceOf(Tags).none { it.id eq id }) {
                throw NotFound()
            }
            //删除标签时，将关联的illust/album重导出。只需要导出当前标签的关联，而不需要导出子标签的。
            data.db.from(IllustTagRelations)
                .select(IllustTagRelations.illustId)
                .where { IllustTagRelations.tagId eq id }
                .map { IllustExporterTask(it[IllustTagRelations.illustId]!!, exportMeta = true, exportDescription = false, exportFileAndTime = false, exportScore = false) }
                .let { illustMetaExporter.appendNewTask(it) }
            data.db.from(AlbumTagRelations)
                .select(AlbumTagRelations.albumId)
                .where { AlbumTagRelations.tagId eq id }
                .map { AlbumExporterTask(it[AlbumTagRelations.albumId]!!, exportMeta = true) }
                .let { illustMetaExporter.appendNewTask(it) }
            recursionDelete(id)

            queryManager.flushCacheOf(QueryManager.CacheType.TAG)
        }
    }
}