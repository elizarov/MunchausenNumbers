/**
 * @author Roman Elizarov
 */

// 96-bit integer class
// Numbers are stored as 3 x 32-bit ints, twos-complimentary
data class Int96(
        val i0 : Int,
        val i1 : Int,
        val i2 : Int) : Comparable<Int96> {

    constructor(x: Int) : this(
            x,
            if (x < 0) -1 else 0,
            if (x < 0) -1 else 0) {}

    constructor(x: Long) : this(
            x.toInt(),
            (x ushr 32).toInt(),
            if (x < 0) -1 else 0) {}

    companion object {
        val ZERO = Int96(0)
        val ONE = Int96(1)
    }

    fun neg() : Int96 {
        val r0 = i0.inv().toULong() + 1
        val r1 = i1.inv().toULong() + (r0 shr 32)
        val r2 = i2.inv().toULong() + (r1 shr 32)
        return Int96(
                r0.toInt(),
                r1.toInt(),
                r2.toInt())
    }

    fun abs() : Int96 {
        return if (this < ZERO) neg() else this
    }

    operator fun times(x : Int) : Int96 {
        val r0 = i0.toULong() * x
        val r1 = i1.toULong() * x + (r0 shr 32)
        val r2 = i2.toULong() * x + (r1 shr 32)
        return Int96(
                r0.toInt(),
                r1.toInt(),
                r2.toInt())
    }

    operator fun plus(x : Int) : Int96 {
        val r0 = i0.toULong() + x
        val r1 = i1.toULong() + (r0 shr 32)
        val r2 = i2.toULong() + (r1 shr 32)
        return Int96(
                r0.toInt(),
                r1.toInt(),
                r2.toInt())
    }

    operator fun plus(x : Int96) : Int96 {
        val r0 = i0.toULong() + x.i0.toULong()
        val r1 = i1.toULong() + x.i1.toULong() + (r0 shr 32)
        val r2 = i2.toULong() + x.i2.toULong() + (r1 shr 32)
        return Int96(
                r0.toInt(),
                r1.toInt(),
                r2.toInt())
    }

    operator fun minus(x : Int) : Int96 {
        return this + (-x)
    }

    operator fun minus(x : Int96) : Int96 {
        val r0 = i0.toULong() - x.i0.toULong()
        val r1 = i1.toULong() - x.i1.toULong() + (r0 shr 32)
        val r2 = i2.toULong() - x.i2.toULong() + (r1 shr 32)
        return Int96(
                r0.toInt(),
                r1.toInt(),
                r2.toInt())
    }

    operator fun div(x : Int) : Int96 {
        if (x <= 0)
            throw IllegalArgumentException()
        val a = abs()
        val d2 = a.i2 / x
        val r2 = a.i2 % x
        val m1 = a.i1.toULong() + (r2 shl 32)
        val d1 = m1 / x
        val r1 = m1 % x
        val m0 = a.i0.toULong() + (r1 shl 32)
        val d0 = m0 / x
        val b = Int96(
                d0.toInt(),
                d1.toInt(),
                d2.toInt())
        return if (this < ZERO) b.neg() else b
    }

    // True mathematical modulo
    infix fun rem(x: Int): Int {
        if (x <= 0)
            throw IllegalArgumentException()
        val a = abs()
        val r2 = a.i2 % x
        val r1 = (a.i1.toULong() + (r2 shl 32)) % x
        val r0 = ((a.i0.toULong() + (r1 shl 32)) % x).toInt()
        return if (this < ZERO) if (r0 == 0) 0 else x - r0
            else r0
    }

    fun fitsLong() = i1 shr 31 == i2

    private fun toLongImpl() = (i1.toLong() shl 32) or i0.toULong()

    fun toLong() : Long {
        if (!fitsLong())
            throw IllegalArgumentException()
        return toLongImpl()
    }

    fun fits(maxAbs: Long): Boolean {
        if (!fitsLong())
            return false
        return Math.abs(toLongImpl()) < maxAbs
    }

    override fun compareTo(other: Int96): Int {
        var res = java.lang.Integer.compare(i2, other.i2)
        if (res != 0)
            return res
        res = java.lang.Integer.compareUnsigned(i1, other.i1)
        if (res != 0)
            return res
        return java.lang.Integer.compareUnsigned(i0, other.i0)
    }

    operator fun compareTo(other: Long): Int {
        if (fitsLong())
            return java.lang.Long.compare(toLong(), other)
        return if (this < 0) -1 else 1
    }

    fun toString(base: Int): String {
        val sb = StringBuilder()
        var rem = abs()
        while (rem > ZERO) {
            sb.insert(0, Character.forDigit(rem rem base, base))
            rem /= base
        }
        if (sb.isEmpty())
            sb.append('0')
        if (this < ZERO)
            sb.insert(0, '-')
        return sb.toString()
    }

    override fun toString(): String {
        return toString(10)
    }
}

fun Int.toULong() = this.toLong() and 0xffffffffL

infix fun Long.rem(x: Int) : Int {
    if (x <= 0)
        throw IllegalArgumentException()
    val a = Math.abs(this)
    val r = (a % x).toInt()
    return if (this < 0) if (r == 0) 0 else x - r else r
}

fun Long.fits(maxAbs: Long): Boolean {
    return Math.abs(this) < maxAbs
}

infix fun Int.pow(b: Int): Int96 {
    return (1 .. b).fold(Int96.ONE) { res, n -> res * this }
}
