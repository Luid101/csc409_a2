import java.security.MessageDigest
def md5(s: String) = {
    MessageDigest.getInstance("MD5").digest(s.getBytes)
}

def getHash(s: String) = {
        md5(s).map("%02X".format(_)).mkString
}

def getRangeForLength(l: Int) = {
    val upper = BigInt("9" * l)
    val lower = BigInt("0" * l)
  	val r = sc.parallelize(lower to upper)
    r.map(x => if (x.toString.length() == l) x.toString else "0" * (l - x.toString.length()) + x.toString)
}

def findPreimage(l: Int, s: String) = {
    val target = s.toUpperCase
    getRangeForLength(l).filter(x => getHash(x) == target)
}

