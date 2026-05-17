package nl.rvantwisk.gatas.lib.extensions


class CobsByteArray {
  val COBS_MINIMAL_EXTRA_BYTES = 3

  private val buffer: ByteArray
  var offset: Int = 0

  constructor(rawSize: Int) {
    //if (rawSize > 253) throw IllegalArgumentException()
    buffer = ByteArray(rawSize + COBS_MINIMAL_EXTRA_BYTES)
  }

  constructor(buffer: ByteArray) {
    //if (buffer.size > 253) throw IllegalArgumentException()
    this.buffer = buffer.cobsDecode()
  }

  /**
   * Peek into the buffer, but don't modify the internal pointers
   */
  fun peekAhead(givenOffset: Int): Int {
    return buffer[givenOffset].toInt()
  }

  fun peekAhead(): Int {
    return buffer[offset].toInt()
  }

  fun getCobs(): ByteArray = buffer.copyOf(offset).cobsEncode()
  fun getBuffer(): ByteArray = buffer

  fun putInt4(value: Int) {
    buffer[offset++] = (value shr 24).toByte()
    buffer[offset++] = (value shr 16).toByte()
    buffer[offset++] = (value shr 8).toByte()
    buffer[offset++] = value.toByte()
  }

  fun putInt3(value: Int) {
    buffer[offset++] = (value shr 16).toByte()
    buffer[offset++] = (value shr 8).toByte()
    buffer[offset++] = value.toByte()
  }

    fun putUInt3(value: UInt) {
        buffer[offset++] = (value shr 16).toByte()
        buffer[offset++] = (value shr 8).toByte()
        buffer[offset++] = value.toByte()
    }

    fun putUInt3(value: Long) {
        buffer[offset++] = (value shr 16).toByte()
        buffer[offset++] = (value shr 8).toByte()
        buffer[offset++] = value.toByte()
    }

  fun put2(value: Short) {
    buffer[offset++] = (value.toInt() shr 8).toByte()
    buffer[offset++] = value.toByte()
  }

  fun put1(value: Byte) {
    buffer[offset++] = value
  }

  fun put1(value: UByte) {
    buffer[offset++] = value.toByte()
  }

  fun put1(value: Short) {
    buffer[offset++] = value.toByte()
  }

  fun put1(value: Int) {
    buffer[offset++] = value.toByte()
  }

  fun putArray(array: ByteArray) {
    buffer[offset++] = array.size.toByte()
    array.forEach { buffer[offset++] = it }
  }

  fun putBytes(array: ByteArray) {
    array.forEach { buffer[offset++] = it }
  }

  fun get1(): Byte {
    return buffer[offset++]
  }

  fun get2(): Short {
    val value = ((buffer[offset].toInt() and 0xFF) shl 8) or
      (buffer[offset + 1].toInt() and 0xFF)
    offset += 2
    return value.toShort()
  }

  fun getInt1(): Int {
    return buffer[offset++].toInt()
  }

  fun getUInt1(): UInt {
    return buffer[offset++].toUInt()
  }

  fun getInt2(): Int {
    val value = ((buffer[offset].toInt() and 0xFF) shl 8) or
      (buffer[offset + 1].toInt() and 0xFF)
    offset += 2
    return value
  }

  fun getUInt2(): UInt {
    val value = ((buffer[offset].toUInt() and 255u) shl 8) or
      (buffer[offset + 1].toUInt() and 255u)
    offset += 2
    return value
  }

  fun getInt3(): Int {
    val value = ((buffer[offset].toInt() and 0xFF) shl 16) or
      ((buffer[offset + 1].toInt() and 0xFF) shl 8) or
      (buffer[offset + 2].toInt() and 0xFF)
    offset += 3
    return value
  }


  fun getUInt3(): UInt {
    val value = ((buffer[offset].toUInt() and 255u) shl 16) or
      ((buffer[offset + 1].toUInt() and 255u) shl 8) or
      (buffer[offset + 2].toUInt() and 255u)
    offset += 3
    return value
  }

  fun getUInt4(): UInt {
    val value = ((buffer[offset].toUInt() and 255u) shl 24) or
      ((buffer[offset + 1].toUInt() and 255u) shl 16) or
      ((buffer[offset + 2].toUInt() and 255u) shl 8) or
      (buffer[offset + 3].toUInt() and 255u)
    offset += 4
    return value
  }

  fun getLong8(): Long {
    val value = ((buffer[offset].toLong() and 0xFF) shl 56) or
      ((buffer[offset + 1].toLong() and 0xFF) shl 48) or
      ((buffer[offset + 2].toLong() and 0xFF) shl 40) or
      ((buffer[offset + 3].toLong() and 0xFF) shl 32) or
      ((buffer[offset + 4].toLong() and 0xFF) shl 24) or
      ((buffer[offset + 5].toLong() and 0xFF) shl 16) or
      ((buffer[offset + 6].toLong() and 0xFF) shl 8)  or
      (buffer[offset + 7].toLong() and 0xFF)
    offset += 8
    return value
  }

  fun getInt4(): Int {
    val value = ((buffer[offset].toInt() and 0xFF) shl 24) or
      ((buffer[offset + 1].toInt() and 0xFF) shl 16) or
      ((buffer[offset + 2].toInt() and 0xFF) shl 8) or
      (buffer[offset + 3].toInt() and 0xFF)
    offset += 4
    return value
  }
}
