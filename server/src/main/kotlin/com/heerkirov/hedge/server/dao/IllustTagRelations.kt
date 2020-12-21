package com.heerkirov.hedge.server.dao

import com.heerkirov.hedge.server.dao.types.EntityMetaRelationTable
import com.heerkirov.hedge.server.model.IllustTagRelation
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.Column
import me.liuwj.ktorm.schema.boolean
import me.liuwj.ktorm.schema.int

object IllustTagRelations : EntityMetaRelationTable<IllustTagRelation>("illust_tag_relation") {
    val illustId = int("illust_id")
    val tagId = int("tag_id")
    val isExported = boolean("is_exported")

    override fun entityId(): Column<Int> = illustId
    override fun metaId(): Column<Int> = tagId
    override fun exported(): Column<Boolean> = isExported

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = IllustTagRelation(
        illustId = row[illustId]!!,
        tagId = row[tagId]!!,
        isExported = row[isExported]!!
    )
}
