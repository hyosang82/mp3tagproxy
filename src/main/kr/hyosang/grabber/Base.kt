package kr.hyosang.grabber

import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

open class GrabberBase: Thread() {

    fun nextUrl(): String {
        var page = 1
        val url = "http://www.k-pop.or.kr/history2014/directory_year.jsp?pagenum=$page&year=200&ordertitle=tmttl&order=desc&reclist=100&keyword2="

        return url
    }

    protected fun getContent(url: String): String {
        val urlobj = URL(url)
        val conn = urlobj.openConnection() as HttpURLConnection
        conn.doInput = true
        conn.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
        conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36")
        conn.instanceFollowRedirects = true

        conn.connect()

        val resp = conn.responseCode
        if(resp == 200) {
            val istrm = InputStreamReader(conn.inputStream, Charsets.UTF_8)
            val buf = CharArray(512)
            val sb = StringBuffer()
            var nRead = 0

            while (true) {
                nRead = istrm.read(buf)
                if (nRead > 0) {
                    sb.append(buf, 0, nRead)
                } else {
                    break
                }
            }

            return sb.toString().replace(Regex("<!--(.*?)-->"), "")
        }else if(resp == 302) {
            System.out.println("loc = ${conn.getHeaderField("Location")}")
            return ""
        }else {
            System.out.println("Error on response: $resp, $url")
            return ""
        }
    }

    override fun run() {
        val pattern = Pattern.compile("<li>(.*?)</li>", Pattern.DOTALL)
        val ptrnAlbumId = Pattern.compile("javascript:goTitleMain\\('([0-9]+)'")
        while(true) {
            val url = nextUrl()

            try {
                val content = getContent(url)
                val idx1 = content.indexOf("<ul class=\"albumList\">")
                if(idx1 > 0) {
                    val idx2 = content.indexOf("</ul>", idx1)
                    if(idx2 > 0) {
                        val listContent = content.substring(idx1, idx2)

                        //앨범별로 컷
                        val m = pattern.matcher(listContent)
                        var idx3 = 0
                        while(m.find(idx3)) {
                            val m2 = ptrnAlbumId.matcher(m.group(0))
                            if(m2.find()) {
                                val album = parseAlbumDetail(m2.group(1))

                                System.out.println(album)
                            }

                            idx3 = m.end()
                        }
                    }
                }
            }catch(e: IOException) {
                e.printStackTrace()
            }

            break
        }
    }

    fun parseAlbumDetail(tid: String): Album {
        val pattern = Pattern.compile("<h4 class=\"contsubTit\">수록곡<span>(.*?)</span></h4>")
        val tr = Pattern.compile("<tr(.*?)</tr>", Pattern.DOTALL)
        val td = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.DOTALL)
        var lastIdx = 0

        val content = getContent("http://www.k-pop.or.kr/metadetail2014/title/title_main.jsp?tid=$tid")

        val album = parseAlbumBaseInfo(tid, content)

        val m = pattern.matcher(content)

        while(m.find(lastIdx)) {
            val disc = Disc(m.group(1))
            val tableEnd = content.indexOf("</table>", lastIdx)

            val m2 = tr.matcher(content.substring(m.end(), tableEnd))
            var lastIdx2 = 0
            while(m2.find(lastIdx2)) {
                var lastIdx3 = 0
                val m3 = td.matcher(m2.group(0))

                if(m3.find()) {
                    //track
                    val no = m3.group(1)

                    //title
                    m3.find(m3.end())
                    val track = parseTitle(m3.group(1))

                    track.no = no


                    //artist
                    m3.find(m3.end())
                    track.sung = parseArtist(m3.group(1))

                    //lyrics
                    m3.find(m3.end())
                    track.word = parseArtist(m3.group(1))

                    //compose
                    m3.find(m3.end())
                    track.compose = parseArtist(m3.group(1))

                    //arrange
                    m3.find(m3.end())
                    track.arrange = parseArtist(m3.group(1))

                    //runtime
                    m3.find(m3.end())
                    track.runtime = parseTime(m3.group(1))

                    disc.tracks.add(track)
                }

                lastIdx2 = m2.end()
            }

            album.discs.add(disc)

            lastIdx = tableEnd + 5
        }

        return album
    }

    fun parseAlbumBaseInfo(tid: String, content: String): Album {
        val patternCover = Pattern.compile("<div class=\"albImg\">(.*?)</div>", Pattern.DOTALL)
        val patternImg = Pattern.compile("javascript:imgLarge\\('([0-9]+)', '(.*?)'\\)")

        val pTitle = Pattern.compile("<h4 class=\"contsubTit mt_non\">(.*?)</h4>", Pattern.DOTALL)

        val pInfo = Pattern.compile("<dl class=\"albInfoCont\">(.*?)</dl>", Pattern.DOTALL)

        var m = pTitle.matcher(content)

        val album = Album(tid)

        if(m.find()) {
            album.albumTitle = removeTags(m.group(1)).trim()
        }

        var lastIdx = 0

        m = patternCover.matcher(content)
        if(m.find()) {
            val m2 = patternImg.matcher(m.group(1))

            while(m2.find(lastIdx)) {
                val imgurl = "http://www.k-pop.or.kr/common2014/large_jacket_image_pys.jsp?tid=${m2.group(1)}&side=${m2.group(2)}"
                val realuri = getAlbumartUrl(imgurl)
                if(realuri.isNotEmpty()) {
                    if(!realuri.endsWith("/noalbum.gif")) {
                        album.addAlbumArt("http://www.k-pop.or.kr$realuri", m2.group(2))
                    }
                }

                lastIdx = m2.end()
            }
        }

        m = pInfo.matcher(content)
        if(m.find()) {
            var m2 = Pattern.compile("<dt>(.*?)</dt>", Pattern.DOTALL).matcher(m.group(0))
            if(m2.find()) {
                album.albumArtist.addAll(parseArtist(m2.group(1)))
            }

            m2 = Pattern.compile("<dd>(.*?)</dd>", Pattern.DOTALL).matcher(m.group(0))
            if(m2.find()) {
                val p = Pattern.compile("<li><span class=\"tit\">(.*?)</span>\\s*<span class=\"txt\">(.*?)</span>\\s*</li>")
                val m3 = p.matcher(m2.group(1))

                lastIdx = 0

                while(m3.find(lastIdx)) {
                    var v = m3.group(2).replace(Regex("^:\\s+"), "")
                    when(m3.group(1)) {
                        "에디션" -> album.edition = decodeHtmlChars(v).trim()
                        "매체" -> album.mediaType = decodeHtmlChars(v).trim()
                        "음반번호" -> album.catno = decodeHtmlChars(v).trim()
                        "관련회사" -> album.companies = decodeHtmlChars(v).trim()
                    }

                    lastIdx = m3.end()
                }
            }
        }

        return album
    }

    fun parseArtist(td: String): ArrayList<Artist> {
        val pattern  = Pattern.compile("goArtistMain\\(([0-9]+)")
        val m = pattern.matcher(td)
        var id = ""
        var nm = ""

        val artists = ArrayList<Artist>()

        if(m.find()) {
            id = m.group(1)

            val pattern2 = Pattern.compile(">(.*?)<", Pattern.DOTALL)
            val m2 = pattern2.matcher(td)
            if(m2.find()) {
                nm = m2.group(1).trim()
            }

            artists.add(Artist(id, nm))
        }else {
            nm = td.trim()

            if(nm.indexOf("\n") > 0) {
                //multiline => multiple artist...
                nm.split("\n").filter { it.trim().isNotEmpty() }.forEach {
                    artists.add(Artist("", it.trim()))
                }
            }else {
                if(nm != "-") {
                    artists.add(Artist(id, nm))
                }
            }
        }


        return artists
    }

    fun parseTitle(td: String): Track {
        val pattern  = Pattern.compile("LayDispWin3\\(([0-9]+)")
        val m = pattern.matcher(decodeHtmlChars(td))
        var id = ""
        var nm = ""

        if(m.find()) {
            id = m.group(1)

            val pattern2 = Pattern.compile("title=\"(.*?)\"", Pattern.DOTALL)
            val m2 = pattern2.matcher(td)
            if(m2.find()) {
                nm = m2.group(1).trim()
            }
        }else {
            nm = td.trim()
        }


        return Track(id, nm)
    }

    fun parseTime(time: String): Int {
        var seconds = 0
        var multiplier = 1

        time.split(":").reversed().forEach {
            if(it.isNotEmpty()) {
                seconds += Integer.parseInt(it, 10) * multiplier
                multiplier *= 60
            }
        }

        return seconds
    }

    fun getAlbumartUrl(jsonUrl: String): String {
        val json = getContent(jsonUrl)
        val p = Pattern.compile("\"url\":\"(.*?)\"")
        val m = p.matcher(json)
        if(m.find()) {
            return m.group(1)
        }else {
            return ""
        }

    }

    fun removeTags(str: String): String {
        return str.replace(Regex("<(.*?)>"), "")
    }

    fun decodeHtmlChars(str: String): String {
        return str.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&#39;", "'")
    }

    fun formatYear(srcYear: String): String {
        val yr = srcYear.replace(Regex("[^0-9]"), "")
        when(yr.length) {
            8 -> return yr
            6 -> return yr + "00"
            4 -> return yr
        }

        return srcYear
    }


}

interface JSONSerializable {
    fun toJsonStringify(): String
    fun toJsonString(key: String, value: String): String {
        return "\"$key\": \"$value\""
    }
}

data class Album(val tid: String): JSONSerializable {
    var albumTitle = ""
    val albumart = ArrayList<String>()
    val albumArtist = ArrayList<Artist>()
    var mediaType = ""
    var edition = ""
    var catno = ""
    var companies = ""
    var year = ""
    val discs = ArrayList<Disc>()

    fun addAlbumArt(url: String, type: String) {
        if(type == "F") {
            albumart.add(0, url)
        }else {
            albumart.add(url)
        }
    }

    override fun toString(): String {
        return "[$catno] $mediaType $albumTitle($tid) ${albumArtist.first()} $edition images=${albumart.count()}, ${discs.joinToString()}"
    }

    fun toFormattedString(): String {
        return StringBuffer().apply {
            append("Album ID: $tid\n")
            append("Title: $albumTitle\n")
            albumArtist.forEach {
                append("Artist: $it\n")
            }
            albumart.forEach {
                append("AlbumArt: $it\n")
            }
            discs.forEach { d ->
                append("Disc ${d.name}\n")
                d.tracks.forEach { t ->
                    append("Track ${t.no}: ${t.title}\n")
                }
            }
        }.toString()
    }

    override fun toJsonStringify(): String {
        return StringBuffer().apply {
            append("{")
            append(toJsonString("albumId", tid)).append(",")
            append(toJsonString("title", albumTitle)).append(",")
            append(toJsonString("albumArtist", albumArtist.first().toString())).append(",")
            append(toJsonString("year", year)).append(",")
            append(toJsonString("cover", albumart.first())).append(",")
            append("\"tracks\": [")
            discs.forEach { disc ->
                disc.tracks.forEach { track ->
                    append("{").append(toJsonString("no", track.no)).append(",")
                    append(toJsonString("title", track.title)).append(",")
                    if(track.sung.isEmpty()) {
                        append(toJsonString("artist", albumArtist.first().name))
                    }else {
                        append(toJsonString("artist", track.sung.first().name))
                    }
                    append(",")
                    append(toJsonString("disc", disc.name))
                    append("}")

                    if(disc.tracks.last() !== track) {
                        append(", ")
                    }
                }
                if(discs.last() !== disc) {
                    append(", ")
                }
            }
            append("]")
            append("}")
        }.toString()

    }
}

data class Disc(val name: String) {
    var tracks = ArrayList<Track>()

    override fun toString(): String {
        return "[Disc $name] " + tracks.joinToString()
    }
}

data class Artist(var id: String, var name: String) {
    override fun toString(): String {
        if(id.isEmpty()) {
            return "$name"
        }else {
            return "$name($id)"
        }
    }
}

data class Track(var id: String, var title: String) {
    var no = ""
    var sung = ArrayList<Artist>()
    var word = ArrayList<Artist>()
    var compose = ArrayList<Artist>()
    var arrange = ArrayList<Artist>()
    var runtime = 0

    override fun toString(): String {
        return "${if(id.isEmpty()) title else "$title($id)"} (${sung.joinToString()}, ${compose.joinToString()}, ${word.joinToString()}, ${arrange.joinToString()}, ${runtime}sec)"
    }
}

data class AlbumSearchItem(val id: String,
                           val title: String,
                           val albumArtist: String,
                           val albumImageUrl: String
): JSONSerializable {
    override fun toJsonStringify(): String {
        return StringBuffer().apply {
            append("{")
            append(toJsonString("id", id)).append(",")
            append(toJsonString("title", title)).append(",")
            append(toJsonString("artist", albumArtist)).append(",")
            append(toJsonString("cover", albumImageUrl))
            append("}")
        }.toString()
    }
}