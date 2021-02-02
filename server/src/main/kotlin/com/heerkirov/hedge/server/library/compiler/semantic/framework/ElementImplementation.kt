package com.heerkirov.hedge.server.library.compiler.semantic.framework

import com.heerkirov.hedge.server.library.compiler.grammar.semantic.*
import com.heerkirov.hedge.server.library.compiler.grammar.semantic.Annotation
import com.heerkirov.hedge.server.library.compiler.semantic.*
import com.heerkirov.hedge.server.library.compiler.semantic.plan.*
import com.heerkirov.hedge.server.library.compiler.semantic.utils.semanticError
import java.util.*
import com.heerkirov.hedge.server.library.compiler.grammar.semantic.Element as SemanticElement

/**
 * 从element生成meta tag的生成器。被使用在illust/album中。
 */
class MetaTagElementField : ElementFieldByElement() {
    override val itemName = "meta-tag"
    override val forSourceFlag = false

    override fun generate(element: SemanticElement, minus: Boolean): TagElement<*> {
        //首先将element的各个子项按照最小类原则转换为MetaValue或其子类
        val metaValues = element.items.map(::mapSfpToMetaValue)
        //然后根据公共最小类决定实例化的类型
        val tagElement = when {
            metaValues.all { it is SingleMetaValue } -> {
                @Suppress("UNCHECKED_CAST")
                AuthorElementImpl(metaValues as List<SingleMetaValue>, element.prefix == null, minus)
            }
            metaValues.all { it is SimpleMetaValue } -> {
                @Suppress("UNCHECKED_CAST")
                TopicElementImpl(metaValues as List<SimpleMetaValue>, element.prefix == null, minus)
            }
            else -> TagElementImpl(metaValues, element.prefix == null, minus)
        }
        //如果指定了prefix，检验实例化类型是否满足prefix的要求
        if(element.prefix != null) {
            when (element.prefix.value) {
                "@" -> if(tagElement !is AuthorElement) semanticError(InvalidMetaTagForThisPrefix("@", element.beginIndex, element.endIndex))
                "#" -> if(tagElement !is TopicElement) semanticError(InvalidMetaTagForThisPrefix("#", element.beginIndex, element.endIndex))
                "$" -> {/*tag类型总是会被满足要求*/}
                else -> throw RuntimeException("Unsupported element prefix ${element.prefix.value}.")
            }
        }

        return tagElement
    }

    /**
     * 将主系表结构转换为对应的MetaValue。
     */
    private fun mapSfpToMetaValue(sfp: SFP): MetaValue {
        val (subject, family, predicative) = sfp
        val metaAddress = mapSubjectToMetaAddress(subject)

        return if(family != null && predicative != null) {
            //同时具有主系表结构
            when (family.value) {
                ":" -> mapIsFamily(metaAddress, predicative)
                ">" -> mapCompareFamily(metaAddress, family.value, predicative)
                ">=" -> mapCompareFamily(metaAddress, family.value, predicative)
                "<" -> mapCompareFamily(metaAddress, family.value, predicative)
                "<=" -> mapCompareFamily(metaAddress, family.value, predicative)
                "~" -> mapToFamily(metaAddress, predicative)
                else -> throw RuntimeException("Unsupported family ${family.value}.")
            }
        }else if(family != null) {
            //只有主语和单目系语
            when (family.value) {
                "~+" -> SequentialItemMetaValueToDirection(metaAddress, desc = false)
                "~-" -> SequentialItemMetaValueToDirection(metaAddress, desc = true)
                else -> throw RuntimeException("Unsupported unary family ${family.value}.")
            }
        }else{
            //只有主语
            if(metaAddress.size == 1) {
                //只有1项，将其优化为single value
                SingleMetaValue(metaAddress)
            }else{
                SimpleMetaValue(metaAddress)
            }
        }
    }

    /**
     * 在系语是(:)时，根据表语翻译表达式。
     */
    private fun mapIsFamily(metaAddress: MetaAddress, predicative: Predicative): MetaValue {
        return when (predicative) {
            is StrList -> SequentialMetaValueOfCollection(metaAddress, Collections.singleton(mapStrListInPredicative(predicative)))
            is Range -> {
                val begin = mapStrToMetaString(predicative.from)
                val end = mapStrToMetaString(predicative.to)
                SequentialMetaValueOfRange(metaAddress, begin, end, includeBegin = predicative.includeFrom, includeEnd = predicative.includeTo)
            }
            is Col -> SequentialMetaValueOfCollection(metaAddress, predicative.items.map(::mapStrToMetaString))
            is SortList -> semanticError(UnsupportedElementValueType(itemName, ValueType.SORT_LIST, predicative.beginIndex, predicative.endIndex))
            else -> throw RuntimeException("Unsupported predicative ${predicative::class.simpleName}.")
        }
    }

    /**
     * 在系语是(> >= < <=)时，根据表语翻译表达式。
     */
    private fun mapCompareFamily(metaAddress: MetaAddress, symbol: String, predicative: Predicative): MetaValue {
        return when (predicative) {
            is StrList -> when (symbol) {
                ">" -> SequentialMetaValueOfRange(metaAddress, mapStrListInPredicative(predicative), null, includeBegin = false, includeEnd = false)
                ">=" -> SequentialMetaValueOfRange(metaAddress, mapStrListInPredicative(predicative), null, includeBegin = true, includeEnd = false)
                "<" -> SequentialMetaValueOfRange(metaAddress, null, mapStrListInPredicative(predicative), includeBegin = false, includeEnd = false)
                "<=" -> SequentialMetaValueOfRange(metaAddress, null, mapStrListInPredicative(predicative), includeBegin = false, includeEnd = true)
                else -> throw RuntimeException("Unsupported family $symbol.")
            }
            is Range -> semanticError(UnsupportedElementValueTypeOfRelation(itemName, ValueType.RANGE, symbol, predicative.beginIndex, predicative.endIndex))
            is Col -> semanticError(UnsupportedElementValueTypeOfRelation(itemName, ValueType.COLLECTION, symbol, predicative.beginIndex, predicative.endIndex))
            is SortList -> semanticError(UnsupportedElementValueType(itemName, ValueType.SORT_LIST, predicative.beginIndex, predicative.endIndex))
            else -> throw RuntimeException("Unsupported predicative ${predicative::class.simpleName}.")
        }
    }

    /**
     * 在系语是(~)时，根据表语翻译表达式。
     */
    private fun mapToFamily(metaAddress: MetaAddress, predicative: Predicative): MetaValue {
        return when (predicative) {
            is StrList -> SequentialItemMetaValueToOther(metaAddress, mapStrListInPredicative(predicative))
            is Range -> semanticError(UnsupportedElementValueTypeOfRelation(itemName, ValueType.RANGE, "~", predicative.beginIndex, predicative.endIndex))
            is Col -> semanticError(UnsupportedElementValueTypeOfRelation(itemName, ValueType.COLLECTION, "~", predicative.beginIndex, predicative.endIndex))
            is SortList -> semanticError(UnsupportedElementValueType(itemName, ValueType.SORT_LIST, predicative.beginIndex, predicative.endIndex))
            else -> throw RuntimeException("Unsupported predicative ${predicative::class.simpleName}.")
        }
    }

    /**
     * 处理在表语中的地址段，将其转换为单一的一节MetaString。
     * Tips: 在表语位置出现的地址段，好像在任何位置都没有多段的应用，全都是将其转换为1节……
     */
    private fun mapStrListInPredicative(strList: StrList): MetaString {
        if(strList.items.size > 1) semanticError(ValueCannotBeAddress(strList.beginIndex, strList.endIndex))
        return mapStrToMetaString(strList.items.first())
    }

    /**
     * 将主语翻译为地址段。
     */
    private fun mapSubjectToMetaAddress(subject: Subject): MetaAddress {
        if(subject !is StrList) throw RuntimeException("Unsupported subject type ${subject::class.simpleName}.")
        return subject.items.map(::mapStrToMetaString)
    }

    /**
     * 将单个字符串转换为一个MetaString。
     */
    private fun mapStrToMetaString(str: Str): MetaString {
        return MetaString(str.value, str.type == Str.Type.BACKTICKS)
    }
}

/**
 * 从annotation生成annotation的生成器。被使用在illust/album/topic/author中。
 */
class AnnotationElementField : ElementFieldByAnnotation() {
    override val itemName = "annotation"

    override fun generate(annotation: Annotation, minus: Boolean): AnnotationElement {
        val metaType = annotation.prefixes.asSequence().map(::mapPrefixToMetaType).toSet()
        val items = annotation.items.map(::mapStrToMetaString)
        return AnnotationElement(items, metaType, minus)
    }

    private fun mapPrefixToMetaType(symbol: Symbol): AnnotationElement.MetaType {
        return when (symbol.value) {
            "@" -> AnnotationElement.MetaType.Author
            "#" -> AnnotationElement.MetaType.Topic
            "$" -> AnnotationElement.MetaType.Tag
            else -> throw RuntimeException("Unsupported annotation prefix ${symbol.value}.")
        }
    }

    /**
     * 将单个字符串转换为一个MetaString。
     */
    private fun mapStrToMetaString(str: Str): MetaString {
        return MetaString(str.value, str.type == Str.Type.BACKTICKS)
    }
}

/**
 * 从^element生成source tag的生成器。被使用在illust中。
 */
class SourceTagElementField : ElementFieldByElement() {
    override val itemName = "source-tag"
    override val forSourceFlag = true

    override fun generate(element: SemanticElement, minus: Boolean): SourceTagElement {
        if(element.prefix != null) semanticError(ElementPrefixNotRequired(itemName, element.beginIndex, element.endIndex))
        val items = element.items.map(::mapSfpToMetaValue)
        return SourceTagElement(items, minus)
    }

    /**
     * 将主系表结构转换为MetaString。
     */
    private fun mapSfpToMetaValue(sfp: SFP): MetaString {
        if(sfp.family != null || sfp.predicative != null) semanticError(ElementValueNotRequired(itemName, sfp.beginIndex, sfp.endIndex))
        if(sfp.subject !is StrList) throw RuntimeException("Unsupported subject type ${sfp.subject::class.simpleName}.")
        if(sfp.subject.items.size > 1) semanticError(ValueCannotBeAddress(sfp.subject.beginIndex, sfp.subject.endIndex))
        return MetaString(sfp.subject.items.first().value, sfp.subject.items.first().type == Str.Type.BACKTICKS)
    }
}

/**
 * 从element生成name filter的生成器。被使用在topic/author/annotation中。
 */
class NameFilterElementField : ElementFieldByElement() {
    override val itemName = "name"
    override val forSourceFlag = false

    override fun generate(element: SemanticElement, minus: Boolean): NameElement {
        if(element.prefix != null) semanticError(ElementPrefixNotRequired(itemName, element.beginIndex, element.endIndex))
        val items = element.items.map(::mapSfpToMetaValue)
        return NameElement(items, minus)
    }

    /**
     * 将主系表结构转换为MetaString。
     */
    private fun mapSfpToMetaValue(sfp: SFP): MetaString {
        if(sfp.family != null || sfp.predicative != null) semanticError(ElementValueNotRequired(itemName, sfp.beginIndex, sfp.endIndex))
        if(sfp.subject !is StrList) throw RuntimeException("Unsupported subject type ${sfp.subject::class.simpleName}.")
        if(sfp.subject.items.size > 1) semanticError(ValueCannotBeAddress(sfp.subject.beginIndex, sfp.subject.endIndex))
        return MetaString(sfp.subject.items.first().value, sfp.subject.items.first().type == Str.Type.BACKTICKS)
    }
}