package io.github.ryangwsimmons.wamobile

import org.jsoup.Connection.Method
import org.jsoup.Connection.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.Serializable

import kotlin.collections.Map

class WASession(private val username: String, private val password: String): Serializable {
    //Map that always holds the cookies for the home page of WebAdvisor, so that other various pages can be visited
    private lateinit var homeCookies: Map<String, String>

    @Throws(Exception::class)
    public suspend fun initConnection() {
        //Make the initial connection to the WebAdvisor main page
        var res: Response = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TYPE=M&PID=CORE-WBMAIN&TOKENIDX=").followRedirects(true).execute()

        //Collect the cookies from the previous connection
        var cookies: MutableMap<String, String> = res.cookies()

        //Make the login request to the server
        res = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TOKENIDX="
                + cookies.get("LASTTOKEN")
                + "&SS=LGRQ&URL=https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TYPE=M%26PID=CORE-WBMAIN%26TOKENIDX="
                + cookies.get("LASTTOKEN")
                + "%26WARN=Y")
            .data("USER.NAME", this.username, "CURR.PWD", this.password, "RETURN.URL", "https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TYPE=M&PID=CORE-WBMAIN", "SUBMIT_OPTIONS", "")
            .cookies(cookies)
            .method(Method.POST)
            .followRedirects(true)
            .execute()

        //Update the cookies from the initial connection with the cookies from the login request
        cookies.putAll(res.cookies())

        //Parse the resulting document
        var doc: Document = res.parse()

        //Check that the series of connections resulted in the user getting the main page, and not the log in page or some other page
        var docTitle: String = doc.getElementsByTag("title").text()
        if (!(docTitle.contains("Main Menu"))) {
            throw Exception("Unable to log into WebAdvisor. Check that your login credentials are correct.")
        }

        //Copy the cookies from the home page into the homeCookies property
        this.homeCookies = cookies.toMap()
    }

    @Throws(Exception::class)
    public suspend fun getName(): String {
        //Connect to the WebAdvisor main page
        var res: Response = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TYPE=M&PID=CORE-WBMAIN&TOKENIDX=" + this.homeCookies.get("LASTTOKEN"))
                                 .cookies(this.homeCookies)
                                 .followRedirects(true)
                                 .execute()

        //Parse the response document's HTML
        var doc: Document = res.parse()

        //Get the name
        var name: String = doc.getElementById("global")
                              .getElementsByClass("container").get(0)
                              .getElementsByTag("h1").get(0)
                              .getElementsByClass("department").get(0)
                              .getElementsByTag("a").text()
                              .substring(15)

        //Return the name
        return name
    }

    @Throws(Exception::class)
    public suspend fun getTerms(): ArrayList<Term> {
        //Create a new ArrayList to hold all the terms obtained from the reequest
        var terms: ArrayList<Term> = ArrayList<Term>()

        //Connect to the WebAdvisor grades page for the signed-in user
        var res: Response = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TOKENIDX="
                + this.homeCookies.get("LASTTOKEN") + "&CONSTITUENCY=WBST&type=P&pid=ST-WESTS02A")
            .cookies(this.homeCookies)
            .followRedirects(true)
            .execute()

        //Parse the response document's HTML
        var doc: Document = res.parse()

        //Add all the terms to the ArrayList
        for(row: Element in doc.getElementById("GROUP_Grp_LIST_VAR1").getElementsByTag("tr")) {
            //If the current row in the table contains body cells (as opposed to header cells), add a new Term to the ArrayList
            if (row.getElementsByTag("td").size != 0) {
                var shortName: String = row.getElementsByClass("LIST_VAR2").get(0).getElementsByTag("p").get(0).text()
                var longName: String = row.getElementsByClass("LIST_VAR3").get(0).getElementsByTag("p").get(0).text()
                var startDate: String = row.getElementsByClass("DATE_LIST_VAR1").get(0).getElementsByTag("p").get(0).text()
                var endDate: String = row.getElementsByClass("DATE_LIST_VAR2").get(0).getElementsByTag("p").get(0).text()

                terms.add(Term(shortName, longName, startDate, endDate))
            }
        }

        //Return the ArrayList of Terms
        return terms
    }
}