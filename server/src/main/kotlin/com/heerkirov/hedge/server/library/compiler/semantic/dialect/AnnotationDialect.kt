package com.heerkirov.hedge.server.library.compiler.semantic.dialect

import com.heerkirov.hedge.server.library.compiler.semantic.framework.*

object AnnotationDialect : QueryDialect<AnnotationDialect.AnnotationOrderItem> {
    override val order = orderListOf<AnnotationOrderItem> {
        item(AnnotationOrderItem.CREATE_TIME, "create-time", "create", "ct")
        item(AnnotationOrderItem.UPDATE_TIME, "update-time", "update", "ut")
    }
    //TODO element转name

    val canBeExported = flagField("can-be-exported", "can-be-exported", "exported")
    val createTime = dateField("create-time", "create", "create-time", "ct")
    val updateTime = dateField("update-time", "update", "update-time", "ut")
    val target = enumField<Target>("target", "target") {
        for (value in Target.values()) {
            item(value, value.name)
        }
    }

    enum class AnnotationOrderItem {
        CREATE_TIME, UPDATE_TIME
    }
    enum class Target {
        TAG, AUTHOR, TOPIC, ARTIST, STUDIO, PUBLISH, COPYRIGHT, WORK, CHARACTER
    }
}