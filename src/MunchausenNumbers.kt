import java.util.*
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask
import java.util.concurrent.RecursiveTask

/**
 * Münchausen numbers are numbers x1..xN in base b such that x1..xN = x1^x1 + ... + xN^xN.
 * @author Roman Elizarov
 */

val EMPTY = arrayOf<Int96>()
val ZERO_LIST = arrayOf(Int96.ZERO)

// Modules for balance bit-set pruning. Use large module for base 16 and up
// Note, that bit sets are build sequentially (only)
val OPT_MOD_0 = 16 * 9 * 5 * 7 * 11 * 13 * 17
val OPT_MOD_16 = OPT_MOD_0 * 19
fun modOf(base: Int) = if (base < 15) OPT_MOD_0 else OPT_MOD_16

// Threshold for parallel computation. Do parallel from this number of digits and more
// Only the recursive search is run in parallel, bit-sets are constructed sequentially
val PAR_THRESHOLD = 12

val SAFE_LONG = 1L shl 61

fun main(args : Array<String>) {
    if (args.isEmpty()) {
        for (i in 2..16)
            compute(i)
    } else
        compute(args[0].toInt())
}

fun compute(base: Int) {
    println("MünchausenNumbers for base $base")

    val maxDigs = base + 1
    val posPow = Array(maxDigs + 1) { p -> base pow p }
    val digPow = Array(base) { d -> d pow d }
    val ph = ArrayList<PosHolder>()
    val ctx = FindTaskContext(base, ph, posPow)

    ph.add(PosHolder(ctx.mod))

    fun initPosHolder(n : Int) {
       ph.add(PosHolder(ph[n - 1], Array(base) { d -> posPow[n - 1] * d - digPow[d] }))
    }

    var all = EMPTY
    for (n in 1 .. maxDigs) {
        print("  = Answers with $n digits: ")
        val time0 = System.currentTimeMillis()
        initPosHolder(n)
        val time1 = System.currentTimeMillis()
        val par = n >= PAR_THRESHOLD
        val find = FindTask(ctx, n, -1, Int96.ZERO)
        val ans = if (par)
            ForkJoinPool.commonPool().submit(find).join()!!
            else find.seq()
        val time2 = System.currentTimeMillis()
        println("${ans.map { it.toString(base) }} in ${time1 - time0}+${time2 - time1} ms${if (par) " (par)" else ""}, ${ph[n].ms}")
        all = concat(all, ans)
    }
    println("  All numbers in base $base: ${all.map { it .toString(base) }}")
    println()
}

// EXPLICIT CONTEXT -- WORKAROUND FOR KOTLIN CAPTURE BUGS
class FindTaskContext(
        val base : Int,
        val ph : ArrayList<PosHolder>,
        val posPow : Array<Int96>)
{
    val mod = modOf(base)

    fun findSeqLong(n : Int, first: Boolean, bal : Long): Array<Int96> {
        val readyAns = checkAnswerLong(n, bal)
        if (readyAns != null)
            return readyAns
        var ans = EMPTY
        for (d in (if (first) 1 else 0) .. base - 1) {
            val delta = ph[n].bal[d]
            val next = if (bal.fits(SAFE_LONG) && delta.fits(SAFE_LONG))
                    findSeqLong(n - 1, false, bal - delta.toLong()) else
                    findSeq96(n - 1, false, Int96(bal) - delta)
            ans = concat(ans, plusDigit(n - 1, d, next))
        }
        return ans
    }

    fun findSeq96(n : Int, first: Boolean, bal : Int96): Array<Int96> {
        val readyAns = checkAnswer96(n, bal)
        if (readyAns != null)
            return readyAns
        var ans = EMPTY
        for (d in (if (first) 1 else 0) .. base - 1) {
            val nextBal = bal - ph[n].bal[d]
            val next = if (nextBal.fitsLong())
                findSeqLong(n - 1, false, nextBal.toLong()) else
                findSeq96(n - 1, false, nextBal)
            ans = concat(ans, plusDigit(n - 1, d, next))
        }
        return ans
    }

    fun checkAnswerLong(n: Int, bal: Long): Array<Int96>? {
        val h = ph[n]
        if (h.minBal > bal || h.maxBal < bal)
            return EMPTY
        if (n == 0)
            return ZERO_LIST
        val i = bal rem mod
        if (!h.ms[i])
            return EMPTY
        return null
    }

    fun checkAnswer96(n: Int, bal: Int96): Array<Int96>? {
        val h = ph[n]
        if (h.minBal > bal || h.maxBal < bal)
            return EMPTY
        if (n == 0)
            return ZERO_LIST
        val i = bal rem mod
        if (!h.ms[i])
            return EMPTY
        return null
    }

    fun plusDigit(p: Int, d: Int, a: Array<Int96>): Array<Int96> {
        if (a == EMPTY)
            return EMPTY
        val b = a.copyOf()
        for (i in b.indices) {
            b[i] += posPow[p] * d
        }
        return b
    }
}

class FindTask(
        val ctx : FindTaskContext,
        val n : Int,
        val prevD: Int,
        val bal : Int96) : RecursiveTask<Array<Int96>>()
{
    override fun compute(): Array<Int96> {
        val ans = if (n < PAR_THRESHOLD) seq() else par()
        return if (prevD < 0) ans else ctx.plusDigit(n, prevD, ans)
    }

    fun seq() : Array<Int96> {
        return if (bal.fitsLong())
            ctx.findSeqLong(n, prevD < 0, bal.toLong()) else
            ctx.findSeq96(n, prevD < 0, bal)
    }

    private fun par(): Array<Int96> {
        val readyAns = ctx.checkAnswer96(n, bal)
        if (readyAns != null)
            return readyAns
        val subTasks = ((if (prevD < 0) 1 else 0) .. ctx.base - 1).map { d ->
            FindTask(ctx, n - 1, d, bal - ctx.ph[n].bal[d])
        }
        ForkJoinTask.invokeAll(subTasks)
        var ans = EMPTY
        for (s in subTasks)
            ans = concat(ans, s.join())
        return ans
    }
}

class PosHolder {
    val mod : Int
    val bal : Array<Int96>
    val maxBal : Int96
    val minBal : Int96
    val ms : ModSet

    constructor(mod : Int) {
        this.mod = mod
        bal = EMPTY
        maxBal = Int96.ZERO
        minBal = Int96.ZERO
        ms = ModSet(mod, false, false)
        ms.set(0)
    }

    constructor(prev : PosHolder, bal : Array<Int96>) {
        this.mod = prev.mod
        this.bal = bal
        maxBal = bal.max()!! + prev.maxBal
        minBal = bal.min()!! + prev.minBal
        ms = ModSet(mod, prev.ms.all, prev.ms.unlisted)
        if (ms.all)
            return
        for (i in prev.ms)
            for (b in bal)
                ms.set((b + i) rem mod)
    }
}

class ModSet(val mod: Int, var all : Boolean, var unlisted: Boolean) : Iterable<Int> {
    var bits : BitSet? = if (all) null else BitSet(mod)
    var list : ArrayList<Int>? = if (all || unlisted) null else ArrayList<Int>()
    var size = if (all) mod else 0

    override fun iterator(): Iterator<Int> {
        return if (unlisted) bits!!.stream().iterator() else list!!.iterator()
    }

    fun set(i : Int) {
        if (all)
            return
        if (!bits!![i]) {
            bits!![i] = true
            size++
            if (!unlisted) {
                if (size < mod / 32) {
                    list!!.add(i)
                } else {
                    list = null
                    unlisted = true
                }
            }
            if (size == mod) {
                all = true
                bits = null
            }
        }
    }

    operator fun get(i : Int) : Boolean {
        return all || bits!![i]
    }

    override fun toString(): String {
        return "set ${if (all) "all " else ""}${(size * 10000L / mod) / 100.0}%"
    }
}

fun concat(a: Array<Int96>, b: Array<Int96>): Array<Int96> {
    if (a == EMPTY)
        return b
    if (b == EMPTY)
        return a
    return a.union(b.toSet()).sorted().toTypedArray()
}