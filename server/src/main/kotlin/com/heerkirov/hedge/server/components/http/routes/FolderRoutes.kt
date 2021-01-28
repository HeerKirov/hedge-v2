package com.heerkirov.hedge.server.components.http.routes

import com.heerkirov.hedge.server.components.http.Endpoints
import com.heerkirov.hedge.server.components.service.FolderService
import com.heerkirov.hedge.server.exceptions.ParamTypeError
import com.heerkirov.hedge.server.form.*
import com.heerkirov.hedge.server.library.form.bodyAsForm
import com.heerkirov.hedge.server.library.form.queryAsFilter
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.Context

class FolderRoutes(private val folderService: FolderService) : Endpoints {
    override fun handle(javalin: Javalin) {
        javalin.routes {
            path("api/folders") {
                get(this::list)
                post(this::create)
                path("pin") {
                    get(this::pinList)
                    path(":id") {
                        put(this::pinUpdate)
                        delete(this::pinDelete)
                    }
                }
                path(":id") {
                    get(this::get)
                    patch(this::update)
                    delete(this::delete)
                    path("images") {
                        get(this::listImages)
                        put(this::updateImages)
                        patch(this::partialUpdateImages)
                    }
                }
            }
        }
    }

    private fun list(ctx: Context) {
        val filter = ctx.queryAsFilter<FolderFilter>()
        ctx.json(folderService.list(filter))
    }

    private fun create(ctx: Context) {
        val form = ctx.bodyAsForm<FolderCreateForm>()
        val id = folderService.create(form)
        ctx.status(201).json(IdRes(id))
    }

    private fun get(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        ctx.json(folderService.get(id))
    }

    private fun update(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        val form = ctx.bodyAsForm<FolderUpdateForm>()
        folderService.update(id, form)
    }

    private fun delete(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        folderService.delete(id)
        ctx.status(204)
    }

    private fun listImages(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        val filter = ctx.queryAsFilter<FolderImagesFilter>()
        ctx.json(folderService.getImages(id, filter))
    }

    private fun updateImages(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        val images = try { ctx.body<List<Int>>() }catch (e: Exception) {
            throw ParamTypeError("images", e.message ?: "cannot convert to List<Int>")
        }
        folderService.updateImages(id, images)
    }

    private fun partialUpdateImages(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        val form = ctx.bodyAsForm<FolderImagesPartialUpdateForm>()
        folderService.partialUpdateImages(id, form)
    }

    private fun pinList(ctx: Context) {
        ctx.json(folderService.getPinFolders())
    }

    private fun pinUpdate(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        val form = ctx.bodyAsForm<FolderPinForm>()
        folderService.updatePinFolder(id, form)
    }

    private fun pinDelete(ctx: Context) {
        val id = ctx.pathParam<Int>("id").get()
        folderService.deletePinFolder(id)
    }
}