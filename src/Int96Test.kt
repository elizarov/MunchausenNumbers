import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.ThreadLocalRandom

class Int96Test {
    @Test
    fun testOps() {
        assertEquals(Int96.ZERO, Int96(0))
        assertEquals(Int96.ONE, Int96(1))

        assertEquals(Int96.ZERO, Int96.ZERO + 0)
        assertEquals(Int96.ONE, Int96.ZERO + 1)
        assertEquals(Int96.ZERO, Int96.ONE + (-1))
        assertEquals(Int96.ZERO, Int96.ONE - Int96.ONE)
        assertEquals(Int96(200000000000000), Int96(100000000000000) + Int96(100000000000000))
        assertEquals(Int96(0), Int96(100000000000000) - Int96(100000000000000))
        assertEquals(Int96(-1), Int96(100000000000000) - Int96(100000000000001))

        assertEquals(Int96(111111111), Int96(12345679) * 9)
        assertEquals(Int96(-111111111), Int96(12345679) * -9)

        assertEquals(Int96(123456789), Int96(123456789).abs())
        assertEquals(Int96(123456789), Int96(-123456789).abs())

        assertEquals(9, Int96(123456789) rem 10)
        assertEquals(8, Int96(12345678912345678) rem 10)
        assertEquals(7, Int96(-3) rem 10)
    }

    @Test
    fun testToLong() {
        assertEquals(100000000000000, Int96(100000000000000).toLong())
        assertEquals(-123456789101234, Int96(-123456789101234).toLong())
    }

    val N = 100000


    @Test
    fun testRandomSubAddInt() {
        for (i in 1..N) {
            val a = randomLong(-1000000000000000000, 1000000000000000000)
            val b = randomInt(-1000000000, 1000000000)
            assertEquals("$a + $b", Int96(a + b), Int96(a) + b)
            assertEquals("$a - $b", Int96(a - b), Int96(a) - b)
        }
    }

    @Test
    fun testRandomSubAddInt96() {
        for (i in 1..N) {
            val a = randomLong(-1000000000000000000, 1000000000000000000)
            val b = randomLong(-1000000000000000000, 1000000000000000000)
            assertEquals("$a + $b", Int96(a + b), Int96(a) + Int96(b))
            assertEquals("$a - $b", Int96(a - b), Int96(a) - Int96(b))
        }
    }

    @Test
    fun testRandomMulDivInt() {
        for (i in 1..N) {
            val a = randomInt(-1000000000, 1000000000).toLong()
            val b = randomInt(1, 1000000000)
            assertEquals("$a * $b", Int96(a * b), Int96(a) * b)
            assertEquals("$a / $b", Int96(a / b), Int96(a) / b)
        }
    }

    @Test
    fun testRandomDivInt() {
        for (i in 1..N) {
            val a = randomLong(-1000000000000000000, 1000000000000000000)
            val b = randomInt(1, 1000000000)
            assertEquals("$a / $b", Int96(a / b), Int96(a) / b)
        }
    }

    private fun randomInt(origin: Int, bound: Int): Int {
        return ThreadLocalRandom.current().nextInt(origin, bound)
    }

    private fun randomLong(origin: Long, bound: Long): Long {
        return ThreadLocalRandom.current().nextLong(origin, bound)
    }

    @Test
    fun testStr() {
        assertEquals("0", Int96.ZERO.toString())
        assertEquals("1", Int96.ONE.toString())
        assertEquals("123456789", Int96(123456789).toString())
        assertEquals("-123456789", Int96(-123456789).toString())
        assertEquals("123456789123456789", Int96(123456789123456789).toString())
        assertEquals("-123456789123456789", Int96(-123456789123456789).toString())
    }
}
