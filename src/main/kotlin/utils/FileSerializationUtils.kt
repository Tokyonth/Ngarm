package utils

import java.io.*
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

object FileSerializationUtils {

    /**
     * 序列化对象并写入文件
     * @param obj 要序列化的对象
     * @param filePath 文件路径
     * @return 写入的字节数
     */
    fun <T : Serializable> writeObject(obj: T, filePath: String): Int {
        // 使用缓冲字节数组输出流提高性能
        val byteArrayOutputStream = ByteArrayOutputStream(8192)
        ObjectOutputStream(byteArrayOutputStream).use { it.writeObject(obj) }

        val buffer = ByteBuffer.wrap(byteArrayOutputStream.toByteArray())

        // 使用NIO的FileChannel进行文件写入，性能优于传统IO
        FileChannel.open(
            Paths.get(filePath),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        ).use { channel ->
            return channel.write(buffer)
        }
    }

    /**
     * 从文件读取并反序列化对象
     * @param filePath 文件路径
     * @return 反序列化后的对象
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Serializable> readObject(filePath: String): T? {
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            return null
        }

        // 使用直接缓冲区提高大文件处理性能
        val buffer = ByteBuffer.allocateDirect(file.length().toInt())

        // 使用NIO的FileChannel进行文件读取
        FileChannel.open(Paths.get(filePath), StandardOpenOption.READ).use { channel ->
            channel.read(buffer)
            buffer.flip()

            // 将缓冲区数据转换为字节数组
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            // 反序列化对象
            ObjectInputStream(ByteArrayInputStream(bytes)).use {
                return it.readObject() as T
            }
        }
    }

    /**
     * 批量写入序列化对象列表
     * 适用于多个对象的情况，减少IO操作次数
     */
    fun <T : Serializable> writeObjects(objs: List<T>, filePath: String): Int {
        val byteArrayOutputStream = ByteArrayOutputStream(32768) // 更大的初始缓冲区
        ObjectOutputStream(byteArrayOutputStream).use { oos ->
            oos.writeInt(objs.size) // 先写入列表大小
            objs.forEach { oos.writeObject(it) }
        }

        val buffer = ByteBuffer.wrap(byteArrayOutputStream.toByteArray())

        FileChannel.open(
            Paths.get(filePath),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        ).use { channel ->
            return channel.write(buffer)
        }
    }

    /**
     * 批量读取序列化对象列表
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Serializable> readObjects(filePath: String): List<T>? {
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            return null
        }

        // 对于大文件，使用内存映射文件可以进一步提高性能
        FileChannel.open(Paths.get(filePath), StandardOpenOption.READ).use { channel ->
            val mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
            val bytes = ByteArray(mappedBuffer.remaining())
            mappedBuffer.get(bytes)

            ObjectInputStream(ByteArrayInputStream(bytes)).use { ois ->
                val size = ois.readInt()
                return mutableListOf<T>().apply {
                    repeat(size) {
                        add(ois.readObject() as T)
                    }
                }
            }
        }
    }

}
