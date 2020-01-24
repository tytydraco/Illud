package com.draco.illud.utils

import java.io.ByteArrayOutputStream
import java.util.zip.*

class Compression {
    companion object {
        fun compress(bytes: ByteArray): ByteArray {
            val byteStream = ByteArrayOutputStream()
            val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
            val deflaterStream = DeflaterOutputStream(byteStream, deflater)
            deflaterStream.write(bytes)
            deflaterStream.close()
            return byteStream.toByteArray()
        }

        fun decompress(bytes: ByteArray): ByteArray {
            val byteStream = ByteArrayOutputStream()
            val inflater = Inflater(true)
            val inflaterStream = InflaterOutputStream(byteStream, inflater)
            inflaterStream.write(bytes)
            inflaterStream.close()
            return byteStream.toByteArray()
        }
    }
}