package com.heerkirov.hedge.server.dao.illust

import com.heerkirov.hedge.server.dao.types.EntityMetaRelationTable
import com.heerkirov.hedge.server.model.illust.IllustAuthorRelation
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.BaseTable
import me.liuwj.ktorm.schema.Column
import me.liuwj.ktorm.schema.boolean
import me.liuwj.ktorm.schema.int

open class IllustAuthorRelations(alias: String?) : EntityMetaRelationTable<IllustAuthorRelation>("illust_author_relation", alias = alias) {
    companion object : IllustAuthorRelations(null)
    override fun aliased(alias: String) = IllustAuthorRelations(alias)

    val illustId = int("illust_id")
    val authorId = int("author_id")
    val isExported = boolean("is_exported")

    override fun entityId(): Column<Int> = illustId
    override fun metaId(): Column<Int> = authorId
    override fun exported(): Column<Boolean> = isExported

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = IllustAuthorRelation(
        illustId = row[illustId]!!,
        authorId = row[authorId]!!,
        isExported = row[isExported]!!
    )
}
