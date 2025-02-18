/*
 * Copyright (C) 2023 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package okio.internal

import kotlin.wasm.unsafe.MemoryAllocator
import kotlin.wasm.unsafe.Pointer
import okio.internal.preview1.fd
import okio.internal.preview1.fd_close
import okio.internal.preview1.size

internal fun fdClose(fd: fd) {
  val errno = fd_close(fd = fd)
  if (errno != 0) throw ErrnoException(errno.toShort())
}

internal fun Pointer.readString(byteCount: Int): String {
  if (byteCount == 0) return ""

  // Drop the last byte if it's \0.
  // TODO: confirm this is necessary in practice.
  val lastByte = (this + byteCount - 1).loadByte()
  val byteArray = when {
    lastByte.toInt() == 0 -> readByteArray(byteCount - 1)
    else -> readByteArray(byteCount)
  }

  return byteArray.decodeToString()
}

private fun Pointer.readByteArray(byteCount: Int): ByteArray {
  val result = ByteArray(byteCount)
  for (i in 0 until byteCount) {
    result[i] = (this + i).loadByte()
  }
  return result
}

internal fun MemoryAllocator.write(
  string: String,
): Pair<Pointer, size> {
  val bytes = string.encodeToByteArray()
  return write(bytes) to bytes.size
}

internal fun MemoryAllocator.write(
  byteArray: ByteArray,
  offset: Int = 0,
  count: Int = byteArray.size - offset,
): Pointer {
  val result = allocate(count)
  var pos = result
  for (b in offset until (offset + count)) {
    pos.storeByte(byteArray[b])
    pos += 1
  }
  return result
}
