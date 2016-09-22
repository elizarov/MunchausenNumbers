Münchausen Numbers
==================

A number _k_ is called a Münchausen number in base _b_, when it is equal
to the sum of its own digits _d[i]_ in base _b_ raised to their own power:
 _k = sum(d[i]^[i])_. For example, 3435 is a decimal Münchausen number, 
because _3435 = 3^3 + 4^4 + 3^3 + 5^5_.
 
Can we write an algorithm to find all hexadecimal Münchausen numbers? 
For a full problem statement and background see
[John D. Cook's blog post](http://www.johndcook.com/blog/2016/09/19/munchausen-numbers/)
on the topic.

The answer
==========

The are only three hexadecimal Münchausen numbers: 1, c4ef722b782c26f, and c76712ffc311e6e.
Answers for other bases up to 16 can be found in the [output.txt](output.txt) file.

An algorithm
============

In short, the algorithm in an exhaustive search with a variation of meet-in-the middle 
optimization to prune it.

For each base _b_, we solve separately for numbers with one, two, etc digits. 

Let's define a _balance_ of the number _k_ as _m - sum(d[i]^d[i])_, where _d[i]_ are 
digits in base _b_ of the number _k_. 
By this definition, Münchausen numbers have balance of zero.

On one side of the meet-in-the-middle algorithm, we start with the least significant digit.
We progressively compute all possible balances for one-digit numbers, two-digit numbers, etc.
The number of possible balances grows exponentially, so to optimize the memory we
don't keep the list of the numbers itself. Instead, for _n_ least significant digits
we keep just the minimal and maximal balance, plus a bits set of all possible balances
modulo some fixed number _mod_. This _mod_ number is selected by hand to optimize
the running time and in this implementation _mod = 16 * 9 * 5 * 7 * 11 * 13 * 17_
when running for bases below 15, and 19 times this number for base 15 and above.
Actually, experiments show that any big prime numbers can be used with similar results. 
As we move though the digits from the least significant one to more significant ones, 
the number of possible balances grows exponentially and at some point fills 
the set modulo _mod_ completely. At this point, we don't track the set anymore.

On the other side of the meet-in-the-middle algorithm we perform recursive exhaustive search
to find all _n_-digit numbers starting at the most significant digit and working toward 
less significant ones. This search is pruned with the maximal
and minimal balances and the bit sets that are collected in the first part.

The second part (exhaustive search) is run in parallel for _n_ starting at 12 and above,
which gives a  reasonable runtime on a decent workstation for base 16.

Building and running
====================

This project is built and run from 
[IntelliJ IDEA](https://www.jetbrains.com/idea/)
The corresponding project file is included.
The main source file to run is
[src/MunchausenNumbers.kt](src/MunchausenNumbers.kt).

Why Kotlin
==========

The code is written in Kotlin, because it is concise and pragmatic. In order to truly
check all hexadecimals numbers up to a proven limit of 2*16^16 we need to working with
numbers larger than 64 bits. In order to do this, Int96 class is implemented together
with all the arithmetic we need, and Kotlin gives us ability to define our own 
operators and helper methods in such a way, as to make the main code quite readable
despite some duplication we needed to do for performance reasons. The checks are
actually performed in 64-bit longs if the balances fit there, so there are two
versions of some methods -- one Long and one Int96 one.

 






