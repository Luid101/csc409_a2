import java.security.MessageDigest
def md5(s: String) = {
    MessageDigest.getInstance("MD5").digest(s.getBytes)
}
md5("hello").map("%02X".format(_)).mkString

def getHash(s: String) = {
        md5(s).map("%02X".format(_)).mkString
}

def getRangeForLength(l: Int) = {
    val upper = BigInt("9" * l)
    val lower = BigInt("0" * l)
    val nodes = (scala.math.pow(10,l)/1000000).toInt
    nodes
  	val r = sc.parallelize(lower to upper, nodes)
    r.map(x => if (x.toString.length() == l) x.toString else "0" * (l - x.toString.length()) + x.toString)
}

// def getRangeFor(start: Int, finish: Int, l: Int) = {
//   	val r = start to finish
//     r.map(x => if (x.toString.length() == l) x.toString else "0" * (l - x.toString.length()) + x.toString)
// }

// def generateRanges(l: Int) = {
//   val block = 1000
//   val numberOfRanges = (BigInt(10)^BigInt(l)) / block
//   var ranges = (BigInt(0) to numberOfRanges).map(x => (x * block, (x+1) * block - 1))
//   ranges
// }



def findPreimage(l: Int, s: String) = {
    val target = s.toUpperCase
    //val ranges = generateRanges(l)
    //ranges.map((x,y) => getRangeFor(x, y, l))
    getRangeForLength(l).filter(x => getHash(x) == target)
}

