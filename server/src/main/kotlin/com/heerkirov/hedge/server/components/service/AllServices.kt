package com.heerkirov.hedge.server.components.service

import com.heerkirov.hedge.server.library.framework.Component

class AllServices(val illust: IllustService,
                  val album: AlbumService,
                  val folder: FolderService,
                  val partition: PartitionService,
                  val import: ImportService,
                  val tag: TagService,
                  val annotation: AnnotationService,
                  val author: AuthorService,
                  val topic: TopicService,
                  val settingAppdata: SettingAppdataService,
                  val settingMeta: SettingMetaService,
                  val settingQuery: SettingQueryService,
                  val settingImport: SettingImportService,
                  val settingSource: SettingSourceService,
                  val settingSpider: SettingSpiderService,
                  val queryService: QueryService) : Component