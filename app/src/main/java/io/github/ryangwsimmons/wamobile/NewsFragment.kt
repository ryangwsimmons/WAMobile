package io.github.ryangwsimmons.wamobile

import android.annotation.TargetApi
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import io.github.ryangwsimmons.wamobile.databinding.FragmentNewsBinding
import kotlinx.coroutines.*
import kotlin.reflect.KFunction3

class NewsFragment(private var session: WASession, private var actionBar: ActionBar, private var progressBar: View, private var crossFade: KFunction3<View, View, Boolean, Unit>) : Fragment() {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNewsBinding.inflate(inflater, container, false)
        val viewModel = binding.root

        //Change the title of the action bar to "News"
        actionBar.title = getString(R.string.news_title)


        //Create an error handler for the coroutine that will be executed to get the news items
        val errorHandler = CoroutineExceptionHandler { _, error ->
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(requireActivity().applicationContext, error.message ?: getString(R.string.network_error), Toast.LENGTH_LONG).show()
                if (error.message != null) {
                    Toast.makeText(requireActivity().applicationContext, getString(R.string.network_error), Toast.LENGTH_LONG).show()
                }
            }
        }

        //Launch a coroutine to get the news items
        CoroutineScope(errorHandler).launch {
            //Get a list with the news items
            val newsData = session.getNews()

            withContext(Dispatchers.Main) {
                // Get the final HTML string
                val finalHtml = buildNewsContent(newsData)

                // Set the content of the web view to the final HTML string
                val newsWebView = this@NewsFragment.binding.webViewNews
                // Set the web view client to a custom client that opens any links in the default browser, instead of the web view itself
                newsWebView.webViewClient = object: WebViewClient() {
                    @SuppressWarnings("deprecation")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                            view!!.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            true
                        } else {
                            false
                        }
                    }

                    @TargetApi(Build.VERSION_CODES.N)
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: null
                        return if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                            view!!.context.startActivity(Intent(Intent.ACTION_VIEW, request!!.url))
                            true
                        } else {
                            false
                        }
                    }
                }
                newsWebView.loadDataWithBaseURL(null, finalHtml, "text/html", "UTF-8", null)
                crossFade(requireActivity().findViewById(R.id.fragment_container), progressBar, false)
            }
        }

        // Inflate the layout for this fragment
        return viewModel
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun buildNewsContent(newsData: NewsData): String {
        // Remove the background colour from the css string
        var finalCss = newsData.css.replace(Regex("background.*;"), "")

        // Remove the serif fonts from the CSS, so that styling is consistent
        finalCss = finalCss.replace("font-family:Georgia, \"Times New Roman\", Times, serif;", "")

        // Put the CSS in a style tag, then add the html body to it
        return "<html><style>" + finalCss + "</style>" + newsData.html + "</html>"
    }
}