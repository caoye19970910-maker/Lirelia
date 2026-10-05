package com.cy.languagereader.mobile.readium

import android.content.Context
import java.io.File
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

/**
 * The small boundary between Lirelia and Readium. The rest of the app never needs
 * to know how an EPUB is unpacked, served to WebView or parsed into a Publication.
 */
class ReadiumServices(context: Context) {
    private val appContext = context.applicationContext
    private val httpClient = DefaultHttpClient()
    val assetRetriever = AssetRetriever(appContext.contentResolver, httpClient)
    val publicationOpener = PublicationOpener(
        publicationParser = DefaultPublicationParser(
            appContext,
            httpClient = httpClient,
            assetRetriever = assetRetriever,
            pdfFactory = null,
        ),
    )

    suspend fun openEpub(file: File): Publication {
        require(file.exists()) { "找不到 EPUB 文件：${file.name}" }
        val asset = assetRetriever.retrieve(file.absoluteFile)
            .getOrElse { error("Readium 无法读取 EPUB：$it") }
        return publicationOpener.open(asset, allowUserInteraction = false)
            .getOrElse {
                asset.close()
                error("Readium 无法打开 EPUB：$it")
            }
    }
}
