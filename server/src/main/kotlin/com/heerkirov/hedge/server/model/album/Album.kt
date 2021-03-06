package com.heerkirov.hedge.server.model.album

import java.time.LocalDateTime

/**
 * 画集。
 */
data class Album(val id: Int,
                 /**
                  * 标题。
                  */
                 val title: String,
                 /**
                  * 描述。
                  */
                 val description: String = "",
                 /**
                  * 评分。
                  */
                 val score: Int? = null,
                 /**
                  * 喜爱标记。
                  */
                 val favorite: Boolean = false,
                 /**
                  * [cache field]画集封面的文件id。
                  */
                 val fileId: Int?,
                 /**
                  * [cache field]画集中的图片数量。
                  */
                 val cachedCount: Int = 0,
                 /**
                  * 记录创建的时间。
                  */
                 val createTime: LocalDateTime,
                 /**
                  * 画集的项发生更新的时间。
                  */
                 val updateTime: LocalDateTime)