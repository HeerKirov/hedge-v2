package com.heerkirov.hedge.server.library.xattr

import com.dd.plist.NSArray
import com.dd.plist.PropertyListParser
import com.heerkirov.hedge.server.utils.defer
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object XAttrProcessor {
    /**
     * 从目标文件读取指定的xattr属性。仅适用于macOS。
     * 依赖于命令行工具"xattr"，这是系统自带的命令行工具。
     * @return 返回byte array。读取不到目标属性/目标文件不存在时返回null。
     * @throws IOException 其他解析失败的情况，属于预料之外的异常情况，抛出此异常。
     */
    fun readXattrProp(path: String, prop: String): ByteArray? {
        val process = ProcessBuilder().command("xattr", "-p", prop, path).start()
        if(process.waitFor() == 0) {
            InputStreamReader(process.inputStream).use { isr ->
                BufferedReader(isr).use { br ->
                    return br.readLines().joinToString(" ")
                        .split(" ")
                        .asSequence()
                        .map {
                            val v = it.toInt(16)
                            if(v > Byte.MAX_VALUE) (v - 256).toByte() else v.toByte()
                        }
                        .toList()
                        .toByteArray()
                }
            }
        }else{
            InputStreamReader(process.errorStream).use { isr ->
                BufferedReader(isr).use { br ->
                    val msg = br.readLines().joinToString("\n")
                    if("No such xattr" in msg || "No such file" in msg) {
                        return null
                    }else{
                        throw IOException(msg)
                    }
                }
            }
        }
    }

    /**
     * 将binary property list解析为字符串列表。只支持这一种格式。
     * @return 返回字符串列表。解析错误时返回null。
     */
    fun decodePList(byteArray: ByteArray): List<String>? = defer {
        return try {
            val p = PropertyListParser.parse(byteArray)
            if(p is NSArray) p.array.map { it.toJavaObject().toString() } else null
        }catch (e: Throwable) {
            null
        }
    }
}