package com.heerkirov.hedge.server.model

import java.time.LocalDateTime

/**
 * 文件夹。
 */
data class Folder(val id: Int?,
                  /**
                   * 标题。
                   */
                  val title: String,
                  /**
                   * 虚拟查询表达式。此项不为NULL时，文件夹为虚拟文件夹。
                   */
                  val query: String?,
                  /**
                   * [cache field]文件夹包含的图片数量，仅对非虚拟文件夹有效。
                   */
                  val cachedCount: Int = 0,
                  /**
                   * 文件夹创建时间。
                   */
                  val createTime: LocalDateTime,
                  /**
                   * 文件夹中的项的更改时间/query查询表达式的更改时间。
                   */
                  val updateTime: LocalDateTime) {

    /**
     * 文件夹中的image的关联关系。
     */
    data class ImageRelation(val folderId: Int,
                             val imageId: Int,
                             /**
                              * 此image在此文件夹中的排序顺位，从0开始，由系统统一调配，0号视作封面
                              */
                             val ordinal: Int)
}