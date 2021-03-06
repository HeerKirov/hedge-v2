package com.heerkirov.hedge.server.components.database

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

data class Metadata(
    val meta: MetaOption,
    val query: QueryOption,
    val source: SourceOption,
    val import: ImportOption,
    val spider: SpiderOption
)

data class MetaOption(
    /**
     * illust和album中允许给出的最大score值(最小总是为1)。
     */
    var scoreMaximum: Int,
    /**
     * score值的描述。[数组下标+1]表示要描述的分数。可以不写表示空出。
     */
    var scoreDescriptions: List<ScoreDescription?>,
    /**
     * 当编辑了对应的成份时，自动对illust的tagme做清理。
     */
    var autoCleanTagme: Boolean
) {
    data class ScoreDescription(val word: String, val content: String)
}

data class QueryOption(
    /**
     * 识别并转换HQL中的中文字符。
     */
    var chineseSymbolReflect: Boolean,
    /**
     * 将有限字符串中的下划线转义为空格。
     */
    var translateUnderscoreToSpace: Boolean,
    /**
     * 在元素向实体转换的查询过程中，每一个元素查询的结果的数量上限。此参数防止一个值在预查询中匹配了过多数量的结果。
     */
    var queryLimitOfQueryItems: Int,
    /**
     * 每一个元素合取项中，结果数量的警告阈值。此参数对过多的连接查询子项提出警告。
     */
    var warningLimitOfUnionItems: Int,
    /**
     * 合取项的总数的警告阈值。此参数对过多的连接查询层数提出警告。
     */
    var warningLimitOfIntersectItems: Int,
)

/**
 * 与原始数据相关的选项。
 */
data class SourceOption(
    /**
     * 注册在系统中的原始数据的site列表。此列表与SourceImage的source列值关联。
     */
    val sites: MutableList<Site>
) {
    data class Site(val name: String, var title: String, val hasSecondaryId: Boolean)
}

/**
 * 与导入相关的选项。
 */
data class ImportOption(
    /**
     * 在文件导入时，自动执行对source元数据的分析操作。
     */
    var autoAnalyseMeta: Boolean,
    /**
     * 在文件导入时，自动设置tag、topic、author的tagme。
     */
    var setTagmeOfTag: Boolean,
    /**
     * 在文件导入时如果没有解析source或无source，自动设置source的tagme；analyseMeta时如果分析出了值，自动取消source的tagme。
     */
    var setTagmeOfSource: Boolean,
    /**
     * 导入的新文件的createTime属性从什么属性派生。给出的可选项是几类文件的物理属性。
     * 其中有的属性是有可能不存在的。如果选用了这些不存在的属性，那么会去选用必定存在的属性，即IMPORT_TIME。
     */
    var setTimeBy: TimeType,
    /**
     * 默认的分区时间从createTime截取。但是此属性将影响日期的范围，使延后一定时间的时间范围仍然算作昨天。单位ms。
     */
    var setPartitionTimeDelay: Long?,
    /**
     * 解析来源时，使用的规则列表。
     */
    var sourceAnalyseRules: List<SourceAnalyseRule>,
    /**
     * 指定系统的下载历史数据库的位置路径。
     */
    var systemDownloadHistoryPath: String?,
) {
    enum class TimeType {
        IMPORT_TIME,
        CREATE_TIME,
        UPDATE_TIME
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(value = [
        JsonSubTypes.Type(value = SourceAnalyseRuleByName::class, name = "name"),
        JsonSubTypes.Type(value = SourceAnalyseRuleByFromMeta::class, name = "from-meta"),
        JsonSubTypes.Type(value = SourceAnalyseRuleBySystemHistory::class, name = "system-history")
    ])
    interface SourceAnalyseRule { val site: String }

    interface SourceAnalyseRuleOfRegex : SourceAnalyseRule {
        val regex: String
        val idIndex: Int
        val secondaryIdIndex: Int?
    }

    /**
     * 规则类型name：通过正则解析文件名来分析。
     * @param regex 使用此正则表达式匹配文件名来分析id。
     */
    class SourceAnalyseRuleByName(override val site: String, override val regex: String, override val idIndex: Int, override val secondaryIdIndex: Int?) : SourceAnalyseRuleOfRegex

    /**
     * 规则类型from-meta：通过正则解析来源信息来分析。仅对macOS有效。
     * macOS通常会在下载的文件中附加元信息，标记文件的下载来源URL。可以解析这个URL来获得需要的来源信息。
     * @param regex 使用此正则表达式匹配并分析下载来源URL，分析id。
     */
    class SourceAnalyseRuleByFromMeta(override val site: String, override val regex: String, override val idIndex: Int, override val secondaryIdIndex: Int?) : SourceAnalyseRuleOfRegex

    /**
     * 规则类型system-history：通过查阅系统下载历史数据库来分析。仅对macOS有效。
     * 这是一个用法比较狭隘的分析法。macOS有一个记载下载历史的数据库，如果已经丢失了文件的所有可供分析的元信息但仍保留文件名，那么可以尝试通过查询下载历史得到下载来源。
     * 也不是所有的下载历史查询都能得到正确的下载来源，但至少是最后的保留手段。
     * @param pattern 在{LSQuarantineDataURLString}列中，使用此正则表达式匹配文件名。
     * @param regex 在{LSQuarantineOriginURLString}列中，使用此正则表达式匹配并分析id。
     */
    class SourceAnalyseRuleBySystemHistory(override val site: String, override val regex: String, override val idIndex: Int, override val secondaryIdIndex: Int?) : SourceAnalyseRuleOfRegex
}

/**
 * 与爬虫相关的选项。
 */
class SpiderOption(
    /**
     * 爬虫算法的配对规则。key:value=siteName:爬虫算法名称。爬虫算法名称在系统中写死。
     */
    var rules: Map<String, String>,
    /**
     * 全局的爬虫规则。
     */
    var publicRule: SpiderRule,
    /**
     * 针对每种不同的site单独设置的爬虫规则。这些规则可空，空时从全局取默认值。
     */
    var siteRules: Map<String, SpiderRule>
) {
    class SpiderRule(
        /**
         * 开启使用代理。
         */
        val useProxy: Boolean,
        /**
         * 在失败指定的次数后，移除代理并尝试直连。设为null表示总是使用代理。
         */
        val disableProxyAfterTimes: Int?,
        /**
         * 单次请求多久未响应视作超时，单位毫秒。
         */
        val timeout: Long,
        /**
         * 失败重试的次数。
         */
        val retryCount: Int,
        /**
         * 在完成一个项目后等待多长时间，防止因频率过高引起的封禁。
         */
        val tryInterval: Long
    )
}