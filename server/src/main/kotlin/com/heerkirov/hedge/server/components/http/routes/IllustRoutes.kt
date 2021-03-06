package com.heerkirov.hedge.server.components.http.routes

import com.heerkirov.hedge.server.components.http.Endpoints
import com.heerkirov.hedge.server.components.service.IllustService
import com.heerkirov.hedge.server.exceptions.ParamTypeError
import com.heerkirov.hedge.server.form.*
import com.heerkirov.hedge.server.library.form.bodyAsForm
import com.heerkirov.hedge.server.library.form.queryAsFilter
import com.heerkirov.hedge.server.model.illust.Illust
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.Context

class IllustRoutes(private val illustService: IllustService) : Endpoints {
    override fun handle(javalin: Javalin) {
        javalin.routes {
            path("api/illusts") {
                get(::list)
                path("collection") {
                    post(::createCollection)
                    path(":id") {
                        get(::getCollection)
                        patch(::updateCollection)
                        delete(::deleteCollection)
                        path("related-items") {
                            get(::getCollectionRelatedItems)
                            patch(::updateCollectionRelatedItems)
                        }
                        path("images") {
                            get(::listCollectionImages)
                            put(::updateCollectionImages)
                        }
                    }
                }
                path("image") {
                    path(":id") {
                        get(::getImage)
                        patch(::updateImage)
                        delete(::deleteImage)
                        path("origin-data") {
                            get(::getImageOriginData)
                            patch(::updateImageOriginData)
                        }
                        path("related-items") {
                            get(::getImageRelatedItems)
                            patch(::updateImageRelatedItems)
                        }
                    }
                }
            }
        }
    }

    private fun list(ctx: Context) {
        val filter = ctx.queryAsFilter<IllustQueryFilter>()
        ctx.json(illustService.list(filter))
    }

    private fun createCollection(ctx: Context) {
        val form = ctx.bodyAsForm<IllustCollectionCreateForm>()
        val id = illustService.createCollection(form)
        ctx.status(201).json(IdRes(id))
    }

    private fun getCollection(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        ctx.json(illustService.get(id, Illust.IllustType.COLLECTION))
    }

    private fun updateCollection(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        val form = ctx.bodyAsForm<IllustCollectionUpdateForm>()
        illustService.updateCollection(id, form)
    }

    private fun deleteCollection(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        illustService.delete(id, Illust.IllustType.COLLECTION)
        ctx.status(204)
    }

    private fun getCollectionRelatedItems(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        ctx.json(illustService.getCollectionRelatedItems(id))
    }

    private fun updateCollectionRelatedItems(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        val form = ctx.bodyAsForm<IllustCollectionRelatedUpdateForm>()
        illustService.updateCollectionRelatedItems(id, form)
    }

    private fun listCollectionImages(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        val filter = ctx.queryAsFilter<LimitAndOffsetFilter>()
        ctx.json(illustService.getCollectionImages(id, filter))
    }

    private fun updateCollectionImages(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        val images = try { ctx.body<List<Int>>() }catch (e: Exception) {
            throw ParamTypeError("images", e.message ?: "cannot convert to List<Int>")
        }
        illustService.updateCollectionImages(id, images)
    }

    private fun getImage(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        ctx.json(illustService.get(id, Illust.IllustType.IMAGE))
    }

    private fun updateImage(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        val form = ctx.bodyAsForm<IllustImageUpdateForm>()
        illustService.updateImage(id, form)
    }

    private fun deleteImage(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        illustService.delete(id, Illust.IllustType.IMAGE)
        ctx.status(204)
    }

    private fun getImageOriginData(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        ctx.json(illustService.getImageOriginData(id))
    }

    private fun updateImageOriginData(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        val form = ctx.bodyAsForm<IllustImageOriginUpdateForm>()
        illustService.updateImageOriginData(id, form)
    }

    private fun getImageRelatedItems(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        ctx.json(illustService.getImageRelatedItems(id))
    }

    private fun updateImageRelatedItems(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        val form = ctx.bodyAsForm<IllustImageRelatedUpdateForm>()
        illustService.updateImageRelatedItems(id, form)
    }
}