package kr.hyosang.mp3tagproxy

import kr.hyosang.grabber.Melon
import java.lang.System.out
import java.util.regex.Pattern
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class Mp3tagProxy: HttpServlet() {
    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
        val uri = (req?.requestURI ?: "").split("/")
        out.println("URI: ${uri}")
        when(uri[1]) {
            "mp3tagproxy" -> {
                when(uri[2]) {
                    "search" -> {
                        if(uri.count() > 4) {
                            searchQuery(resp, uri[3], uri[4])
                        }else {
                            out.println("No search part: ${req?.requestURI}")
                            resp?.setStatus(412)
                        }
                    }

                    "album" -> {
                        if(uri.count() > 4) {
                            albumQuery(resp, uri[3], uri[4])
                        }else {
                            out.println("More arguments")
                            resp?.setStatus(412)
                        }
                    }

                    else -> {
                        out.println("Not defined search host: ${uri[1]}")
                        resp?.setStatus(412)
                    }
                }
            }

            else -> {
                out.println("Not defined URI: ${req?.requestURI}")
                resp?.setStatus(404)
            }
        }

    }

    private fun searchQuery(resp: HttpServletResponse?, searchHost: String, searchQuery: String) {
        var worker: Melon? = null
        when(searchHost) {
            "melon" -> worker = Melon()

            else -> resp?.setStatus(404)
        }

        if(worker != null) {
            val sb = StringBuffer()
            val album = worker!!.searchAlbum(searchQuery)

            sb.append("<![ALBUMSEARCHRESULT[")
            album.forEach {
                sb.append("<![ALBUMINFO[")
                sb.append("<![ALBUMID[${it.id}]ALBUMID]>")
                sb.append("<![TITLE[${it.title}]TITLE]>")
                sb.append("<![ARTIST[${it.albumArtist}]ARTIST]>")
                sb.append("<![COVERARTURL[${it.albumImageUrl}]COVERARTURL]>")
                sb.append("]ALBUMINFO]>")
            }
            sb.append("]ALBUMSEARCHRESULT]>")

            resp?.addHeader("Content-Type", "text/plain; charset=utf8")
            resp?.outputStream?.write(sb.toString().toByteArray(Charsets.UTF_8))
        }
    }

    private fun albumQuery(resp: HttpServletResponse?, searchHost: String, albumId: String) {
        var worker: Melon? = null
        when(searchHost) {
            "melon" -> worker = Melon()

            else -> resp?.setStatus(404)
        }

        if(worker != null) {
            val data = worker.getAlbumDetail(albumId)
            val sb = StringBuffer()

            sb.append("<![ALBUMINFO[")
            sb.append("<![TITLE[${data.albumTitle}]TITLE]>")
            sb.append("<![ALBUMARTIST[${data.albumArtist}]ALBUMARTIST]>")
            sb.append("<![COVERART[${data.albumart}]COVERART]>")
            sb.append("<![ALBUMID[${data.tid}]ALBUMID]>")



            sb.append("<![TRACKLIST[")

            data.discs.forEach {
                it.tracks.forEach { tr ->
                    sb.append("<![TRACKS[")
                    sb.append("<![TRACKNO[${tr.no}]TRACKNO]>")
                      .append("<![TRACKTITLE[${tr.title}]TRACKTITLE]>")
                      .append("<![TRACKARTIST[${tr.sung}]TRACKARTIST]>")
                      .append("]TRACKS]>")

                }
            }

            sb.append("]TRACKLIST]>")
            sb.append("]ALBUMINFO]>")

            resp?.addHeader("Content-Type", "text/plain; charset=utf8")
            resp?.outputStream?.write(sb.toString().toByteArray(Charsets.UTF_8))
        }
    }

}