package com.heerkirov.hedge.server.dao.meta

import com.heerkirov.hedge.server.dao.types.MetaAnnotationRelationTable
import com.heerkirov.hedge.server.model.meta.AuthorAnnotationRelation
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.Column
import me.liuwj.ktorm.schema.int

object AuthorAnnotationRelations : MetaAnnotationRelationTable<AuthorAnnotationRelation>("author_annotation_relation", schema = "meta_db") {
    val authorId = int("author_id")
    val annotationId = int("annotation_id")

    override fun metaId(): Column<Int> = authorId
    override fun annotationId(): Column<Int> = annotationId

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = AuthorAnnotationRelation(
        authorId = row[authorId]!!,
        annotationId = row[annotationId]!!
    )
}