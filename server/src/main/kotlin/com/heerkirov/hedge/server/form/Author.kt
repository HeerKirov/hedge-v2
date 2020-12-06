package com.heerkirov.hedge.server.form

import com.heerkirov.hedge.server.model.Author
import com.heerkirov.hedge.server.utils.Opt

data class AuthorRes(val id: Int, val name: String,
                     val type: Author.Type, val favorite: Boolean,
                     val annotations: List<Author.CachedAnnotation>,
                     val score: Int?, val count: Int)

data class AuthorDetailRes(val id: Int, val name: String, val otherNames: List<String>, val description: String,
                           val type: Author.Type, val favorite: Boolean,
                           val annotations: List<Author.CachedAnnotation>,
                           val links: List<Author.Link>,
                           val score: Int?, val count: Int)

data class AuthorCreateForm(val name: String,
                            val otherNames: List<String>? = null,
                            val type: Author.Type = Author.Type.UNKNOWN,
                            val description: String = "",
                            val links: List<Author.Link>? = null,
                            val annotations: List<Any>? = null,
                            val favorite: Boolean = false,
                            val score: Int? = null)

data class AuthorUpdateForm(val name: Opt<String>,
                            val otherNames: Opt<List<String>?>,
                            val type: Opt<Author.Type>,
                            val description: Opt<String>,
                            val links: Opt<List<Author.Link>?>,
                            val annotations: Opt<List<Any>?>,
                            val favorite: Opt<Boolean>,
                            val score: Opt<Int?>)

fun newAuthorRes(author: Author) = AuthorRes(author.id, author.name, author.type, author.favorite, author.cachedAnnotations ?: emptyList(), author.exportedScore, author.cachedCount)

fun newAuthorDetailRes(author: Author) = AuthorDetailRes(author.id, author.name,
    author.otherNames, author.description, author.type, author.favorite,
    author.cachedAnnotations ?: emptyList(),
    author.links ?: emptyList(),
    author.exportedScore, author.cachedCount)