package kr.hyosang.grabber

import java.util.regex.Pattern

class Melon: GrabberBase() {
    fun searchAlbum(title: String): ArrayList<AlbumSearchItem> {
        var startIndex = 1
        val list = ArrayList<AlbumSearchItem>()

        do {
            var urlStr = "https://www.melon.com/search/album/index.htm?startIndex=$startIndex&pageSize=21&q=$title&sortorder=&section=all&sectionId=&genreDir=&sort=weight&subLinkOrText=L"
            var pageCount = 0

            val content = getContent(urlStr)

            val ptAlbum = Pattern.compile("<li class=\"album11_li\">(.*?)</li>", Pattern.DOTALL)
            val mAlbum = ptAlbum.matcher(content)
            var lastIdx = 0

            val ptImg = Pattern.compile("<img onerror=\"WEBPOCIMG\\.defaultAlbumImg\\(this\\);\" width=\"130\" height=\"130\" src=\"(.*?)\"")
            val ptAlbumName = Pattern.compile("<div class=\"atist_info\">(.*?)<a href=(.*?)melon.link.goAlbumDetail\\('([0-9]+)'\\)(.*?)>(.*?)</a>", Pattern.DOTALL)
            val ptArtist = Pattern.compile("<dd class=\"atistname\">(.*?)<div class=\"ellipsis\">(.*?)</div>", Pattern.DOTALL)

            while (mAlbum.find(lastIdx)) {
                var albumNo = ""
                var albumName = ""
                var image = ""
                var artist = ""

                val mImg = ptImg.matcher(mAlbum.group(0))
                if(mImg.find()) {
                    image = mImg.group(1)
                }

                val mAlb = ptAlbumName.matcher(mAlbum.group(0))
                if(mAlb.find()) {
                    albumNo = mAlb.group(3)
                    albumName = removeTags(mAlb.group(5))
                }

                val mArtist = ptArtist.matcher(mAlbum.group(0))
                if(mArtist.find()) {
                    val tmp = mArtist.group(2)
                    val idx = tmp.indexOf("<span")
                    artist = removeTags(tmp.substring(0, idx)).trim()
                }

                if(arrayOf(albumNo, artist, albumName, image).filter { it.isEmpty() }.count() == 0) {
                    list.add(AlbumSearchItem(albumNo, albumName, artist, image))
                }else {
                    System.out.println("Empty? Album: $albumNo ($artist): $albumName $image")
                }


                startIndex++
                pageCount++

                lastIdx = mAlbum.end()
            }
            break
        }while((pageCount > 0) && (startIndex < 100))

        return list
    }

    fun getAlbumDetail(id: String): Album {
        val url = "https://www.melon.com/album/detail.htm?albumId=$id"
        var content = getContent(url)

        val ptAlbumName = Pattern.compile("<strong class=\"none\">앨범명</strong>(.*?)</div>", Pattern.DOTALL)
        val ptAlbumArtist = Pattern.compile("<div class=\"artist\">(.*?)</div>", Pattern.DOTALL)
        val ptMeta = Pattern.compile("<div class=\"meta\">(.*?)</div>", Pattern.DOTALL)
        val ptMetaItem = Pattern.compile("<dt>(.*?)</dt>(.*?)<dd>(.*?)</dd>", Pattern.DOTALL)
        val ptTracks = Pattern.compile("<tr data-group-items=\"(.*?)\">(.*?)<span class=\"rank(.*?)>(.*?)</span>(.*?)<div class=\"wrap_song_info\">(.*?)<div class=\"ellipsis\"(.*?)>(.*?)</div>(.*?)<div(.*?)>(.*?)</div>", Pattern.DOTALL)
        val ptATag = Pattern.compile("<a (.*?)>(.*?)</a>", Pattern.DOTALL)
        val ptSpanOnly = Pattern.compile("<span (.*?)>(.*?)</span>", Pattern.DOTALL)
        val ptSpan = Pattern.compile("<span class=\"disabled\">(.*?)</span>", Pattern.DOTALL)
        val ptArtistA = Pattern.compile("<a (.*?)class=\"artist_name\"(.*?)<span>(.*?)</span>", Pattern.DOTALL)
        val ptImgUrl = Pattern.compile("\\('#d_album_org'\\).click(.*?)'&albumImgPath='(.*?)'(.*?)'(.*?)'&albumImgMd5Hash='(.*?)'(.*?)'", Pattern.DOTALL)

        var albName = ""
        var albArtist = ""
        var agency = ""
        var publisher = ""
        var genre = ""
        var released = ""
        var albumImagePageUrl = ""
        var albumImage = ""

        var m = ptAlbumName.matcher(content)
        if(m.find()) {
            albName = decodeHtmlChars(m.group(1).trim())
        }

        m = ptImgUrl.matcher(content)
        if(m.find()) {
            albumImagePageUrl = "https://www.melon.com/album/albumImgAjax.htm?albumImgPath=${m.group(3)}&albumImgMd5Hash=${m.group(6)}"
        }

        m = ptAlbumArtist.matcher(content)
        if(m.find()) {
            val m2 = ptArtistA.matcher(m.group(1))
            if(m2.find()) {
                albArtist = decodeHtmlChars(m2.group(3))
            }else {
                //V.A
                albArtist = m.group(1).trim()
            }
        }

        m = ptMeta.matcher(content)
        if(m.find()) {
            val m2 = ptMetaItem.matcher(m.group(0))
            var lastIdx = 0
            while(m2.find(lastIdx)) {
                when(m2.group(1)) {
                    "발매일" -> released = formatYear(m2.group(3))
                    "장르" -> genre = m2.group(3)
                    "발매사" -> publisher = m2.group(3)
                    "기획사" -> agency = m2.group(3)
                }

                lastIdx = m2.end()
            }
        }

        val trackMap = HashMap<String, Disc>()

        m = ptTracks.matcher(content)
        var lastIdx = 0
        while(m.find(lastIdx)) {
            val cd = m.group(1)
            var title = ""
            val trackNo = m.group(4).trim()
            val tmpStr = m.group(8)

            var m2 = ptATag.matcher(tmpStr)
            if(m2.find()) {
                title = m2.group(2)
            }else {
                m2 = ptSpan.matcher(tmpStr)
                if(m2.find()) {
                    title = m2.group(1)
                }else {
                    System.out.println("Cannot find $tmpStr")
                }
            }

            val artistDiv = m.group(11).trim()
            var m3 = ptSpanOnly.matcher(artistDiv)
            var artist = ""
            if(m3.find()) {
                artist = removeTags(m3.group(2)).trim()
            }

            if(trackMap.containsKey(cd)) {
                trackMap[cd]!!.tracks.add(Track("", title).apply {
                    this.sung = arrayListOf(Artist("", artist))
                    this.no = trackNo
                })
            }else {
                trackMap.put(cd, Disc(cd).apply {
                    tracks.add(Track("", title).apply {
                        sung = arrayListOf(Artist("", artist))
                        no = trackNo
                    })
                })
            }

            lastIdx = m.end()
        }

        //load album image
        content = getContent(albumImagePageUrl)
        val ptImg = Pattern.compile("<img onerror=(.*?)src=\"(.*?)\"")
        m = ptImg.matcher(content)
        if(m.find()) {
            albumImage = m.group(2)
        }

        val album = Album(id)
        album.albumTitle = albName
        album.albumArtist.add(Artist("", albArtist))
        album.albumart.add(albumImage)
        album.companies = "$agency, $publisher"
        album.year = released
        album.discs.addAll(trackMap.values.sortedBy {it.name})

        return album
    }
}