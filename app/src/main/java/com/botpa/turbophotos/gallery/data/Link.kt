package com.botpa.turbophotos.gallery.data

import androidx.compose.runtime.mutableStateListOf
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.StoragePairs
import com.botpa.turbophotos.util.Storage
import java.io.File

class Link(albumPath: String, metadataPath: String, vectorsPath: String) {

    //Link info
    var album: Album? = null

    val albumFolder: File = File(albumPath)
    val metadataFile: File = File(metadataPath)
    val vectorsFile: File = File(vectorsPath)

    val albumPath: String get() = albumFolder.absolutePath
    val metadataPath: String get() = metadataFile.absolutePath
    val vectorsPath: String get() = vectorsFile.absolutePath


    //Override toString to be able to save albums in a string
    override fun toString(): String {
        return "$albumPath\n$metadataPath"
    }


    //Static
    companion object {

        //Links
        private var areLinksLoaded: Boolean = false
        private val linksMap: MutableMap<String, Link?> = HashMap()
        val links = mutableStateListOf<Link>()


        //Loading & saving list
        fun loadLinks(reset: Boolean) {
            //Already loaded
            if (!reset && areLinksLoaded) return
            areLinksLoaded = true

            //Clear links
            links.clear()
            linksMap.clear()

            //Get links from storage (as strings)
            val unparsedLinks = Storage.getStringList(StoragePairs.LIBRARY_LINKS_KEY)

            //Parse links
            for (unparsedLink in unparsedLinks) addLink(parse(unparsedLink))
        }

        fun saveLinks() {
            //Save links
            val list = ArrayList<String>()
            for (link in links) list.add(link.toString())
            Storage.putStringList(StoragePairs.LIBRARY_LINKS_KEY, list)
        }

        //List management
        private fun addLinkAtIndex(index: Int, link: Link): Boolean {
            //Check if link exists
            val key = link.albumPath
            if (linksMap.containsKey(key)) return false

            //Add link
            links.add(index, link)
            linksMap[key] = link

            //Relink with album
            relinkWithAlbum(link)
            return true
        }

        fun addLink(link: Link): Boolean {
            return addLinkAtIndex(links.size, link)
        }

        fun removeLink(index: Int): Boolean {
            //Check if link exists
            if (index < 0 || index >= links.size) return false

            //Remove link
            val link = links.removeAt(index)
            linksMap.remove(link.albumPath)

            //Notify album
            link.album?.setLink(null)
            return true
        }

        fun getLink(albumPath: String): Link? {
            return linksMap.getOrDefault(albumPath, null)
        }

        //Link management
        fun relinkWithAlbum(link: Link) {
            //Update link album
            link.album = Library.albumsMap.getOrDefault(link.albumPath, null)

            //Notify album of link change
            link.album?.setLink(link)
        }

        fun updateLinkAlbumFolder(index: Int, newAlbumFolder: File): Boolean {
            //Get old link
            val oldLink = links[index]
            val oldKey = oldLink.albumPath
            val keyNew = newAlbumFolder.absolutePath

            //Check if new folder is the same
            if (oldKey == keyNew) return true

            //Check if album is already in a link
            if (linksMap.containsKey(keyNew)) return false

            //Remove old link
            removeLink(index)

            //Add new link with updated album folder
            val newLink = Link(newAlbumFolder.absolutePath, oldLink.metadataPath, oldLink.vectorsPath)
            addLinkAtIndex(index, newLink)

            //Relink with album
            relinkWithAlbum(newLink)
            return true
        }

        fun updateLinkMetadataFile(index: Int, newFile: File) {
            //Get link
            val link = links[index]

            //Update link metadata file
            links[index] = Link(link.albumPath, link.metadataPath, newFile.absolutePath)

            //Relink with album
            relinkWithAlbum(link)
        }

        fun updateLinkVectorsFile(index: Int, newFile: File) {
            //Get link
            val link = links[index]

            //Update link vectors file
            links[index] = Link(link.albumPath, newFile.absolutePath, link.vectorsPath)

            //Relink with album
            relinkWithAlbum(link)
        }

        //Parsing
        fun parse(string: String): Link {
            //Split string into parts
            val parts = string.split("\n")

            //Create link with parts
            return Link(
                parts[0],
                if (parts.size >= 2) parts[1] else "",
                if (parts.size >= 3) parts[2] else ""
            )
        }

    }

}