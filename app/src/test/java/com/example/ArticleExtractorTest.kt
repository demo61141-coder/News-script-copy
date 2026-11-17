package com.example

import com.example.model.ArticleBlock
import com.example.network.ArticleExtractor
import com.example.network.UrlCleaner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleExtractorTest {

    @Test
    fun urlCleaner_stripsFbclidFromIttefaqLink() {
        val raw = "https://www.ittefaq.com.bd/821351?fbclid=IwZXh0bgNhZW0CMTEAcGRvZgNzcnRjBmFwcF9pZA8yNzUyNTQ2OTI1OTgyNzkAAR-jz2sp1vfFavCdiUFbCcyjoIR5cPV4QaW5DMFfDHRAOnFi2o8eDSL7wiX0Aw_aem_Jb3fG43fJ0Uqdu6dHSrQCA"
        val cleaned = UrlCleaner.normalizeAndClean(raw)
        assertEquals("https://www.ittefaq.com.bd/821351", cleaned)
    }

    @Test
    fun urlCleaner_extractsUrlFromSharedMessage() {
        val shared = "এই খবরটি পড়ুন: https://www.ittefaq.com.bd/821351?utm_source=facebook&fbclid=12345 খুব গুরুত্বপূর্ণ।"
        val cleaned = UrlCleaner.normalizeAndClean(shared)
        assertEquals("https://www.ittefaq.com.bd/821351", cleaned)
    }

    @Test
    fun articleExtractor_stripsAdsAndExtractsBengaliNews() {
        val sampleHtml = """
            <html lang="bn">
            <head>
                <title>রাজধানীতে নতুন মেট্রোরেল উদ্যোগ | দৈনিক ইত্তেফাক</title>
                <meta property="og:site_name" content="দৈনিক ইত্তেফাক" />
                <meta property="og:title" content="রাজধানীতে নতুন মেট্রোরেল উদ্যোগ" />
                <meta property="article:published_time" content="2026-11-16T10:30:00+06:00" />
            </head>
            <body>
                <header><nav><a href="/">প্রচ্ছদ</a> <a href="/national">জাতীয়</a></nav></header>
                <main>
                    <article>
                        <h1>রাজধানীতে নতুন মেট্রোরেল উদ্যোগ</h1>
                        <div class="ad-banner">বিজ্ঞাপন এখানে ক্লিক করুন বিশেষ অফার</div>
                        <ins class="adsbygoogle">Google Ad</ins>
                        <div class="jw_detail_content_holder">
                            <p>রাজধানী ঢাকার যানজট নিরসনে নতুন মেট্রোরেল রুট নির্মাণের পরিকল্পনা চূড়ান্ত করা হয়েছে। সংশ্লিষ্ট কর্মকর্তারা জানিয়েছেন, আগামী অর্থবছর থেকেই প্রাথমিক জরিপ শুরু হবে।</p>
                            <div class="ad-slot">Sponsored Ad Banner 300x250</div>
                            <p>আরও পড়ুন: গতকালের গুরুত্বপূর্ণ সংবাদ এখানে দেখুন</p>
                            <p>নগর পরিকল্পনাবিদরা মনে করছেন, গণপরিবহন ব্যবস্থায় আধুনিকায়ন হলে প্রতিদিন লাখো মানুষের কর্মঘণ্টা সাশ্রয় হবে এবং শহরের অর্থনৈতিক গতিশীলতা বাড়বে।</p>
                            <blockquote>গণপরিবহনের এই সম্প্রসারণ নগরবাসীর দীর্ঘদিনের প্রত্যাশা পূরণ করবে।</blockquote>
                        </div>
                        <div class="related-news">
                            <p>সম্পর্কিত খবর</p>
                            <ul><li><a href="/1">অন্য খবর ১</a></li><li><a href="/2">অন্য খবর ২</a></li></ul>
                        </div>
                    </article>
                </main>
                <footer><p>কপিরাইট © দৈনিক ইত্তেফাক</p></footer>
            </body>
            </html>
        """.trimIndent()

        val extractor = ArticleExtractor()
        val article = extractor.parseHtml(
            html = sampleHtml,
            originalUrl = "https://www.ittefaq.com.bd/821351?fbclid=abc",
            resolvedUrl = "https://www.ittefaq.com.bd/821351"
        )

        assertEquals("রাজধানীতে নতুন মেট্রোরেল উদ্যোগ", article.title)
        assertEquals("দৈনিক ইত্তেফাক", article.siteName)
        assertNotNull(article.publishedAt)
        val bodyText = article.plainTextBody
        assertTrue(bodyText.contains("রাজধানী ঢাকার যানজট নিরসনে"))
        assertTrue(bodyText.contains("নগর পরিকল্পনাবিদরা মনে করছেন"))
        assertTrue(article.blocks.any { it is ArticleBlock.Quote })
        assertFalse(bodyText.contains("বিজ্ঞাপন এখানে"))
        assertFalse(bodyText.contains("Sponsored Ad"))
        assertFalse(bodyText.contains("আরও পড়ুন"))
        assertFalse(bodyText.contains("অন্য খবর"))
    }
}
