package io.github.ryangwsimmons.wamobile

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.jsoup.Connection.Method
import org.jsoup.Connection.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.Serializable

import kotlin.collections.Map

@Parcelize
class WASession(private val username: String, private val password: String, private var homeCookies: Map<String, String>): Parcelable {

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
        //Check that a connection has been initialized
        if (this.homeCookies.size == 0) {
            throw Exception("Error: The connection has not yet been initialized.")
        }
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
    public suspend fun getTermsResponse(): Response {
        //Check that a connection has been initialized
        if (this.homeCookies.size == 0) {
            throw Exception("Error: The connection has not yet been initialized.")
        }

        //Connect to the WebAdvisor grades page for the signed-in user
        var res: Response = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TOKENIDX="
                + this.homeCookies.get("LASTTOKEN") + "&CONSTITUENCY=WBST&type=P&pid=ST-WESTS02A")
            .cookies(this.homeCookies)
            .followRedirects(true)
            .execute()

        return res
    }

    @Throws(Exception::class)
    public suspend fun getTerms(): ArrayList<Term> {
        //Check that a connection has been initialized
        if (this.homeCookies.size == 0) {
            throw Exception("Error: The connection has not yet been initialized.")
        }

        //Create a new ArrayList to hold all the terms obtained from the reequest
        var terms: ArrayList<Term> = ArrayList<Term>()

        //Connect to the WebAdvisor grades page for the signed-in user
        var res: Response = this.getTermsResponse()

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

    @Throws(Exception::class)
    public suspend fun getGrades(termPosition: Int, terms: List<Term>, grades: ArrayList<Grade>, advisor: StringBuilder, termGPA: StringBuilder) {
        //Check that a connection has been initialized
        if (this.homeCookies.size == 0) {
            throw Exception("Error: The connection has not yet been initialized.")
        }

        //Connect to the WebAdvisor terms page for the signed-in user
        var res: Response = this.getTermsResponse()

        //Create the request form for the next request using a map
        var gradesForm: HashMap<String, String> = HashMap<String, String>()

        //Construct the form
        //Add the parameters that stay the same for every request
        gradesForm.put("LIST.VAR1_CONTROLLER", "LIST.VAR1")
        gradesForm.put("LIST.VAR1_MEMBERS", "LIST.VAR1*LIST.VAR2*LIST.VAR3*DATE.LIST.VAR1*DATE.LIST.VAR2")
        gradesForm.put("LIST.VAR1_MAX", terms.size.toString())
        gradesForm.put("LIST.VAR2_MAX", terms.size.toString())
        gradesForm.put("LIST.VAR3_MAX", terms.size.toString())
        gradesForm.put("DATE.LIST.VAR1_MAX", terms.size.toString())
        gradesForm.put("DATE.LIST.VAR2_MAX", terms.size.toString())
        //Add the parameter that determines which term has been selected
        gradesForm.put("LIST.VAR1_RADIO", "LIST.VAR1_" + (termPosition + 1).toString())
        //Add the parameters that repeat for every term the signed-in user has attended at Guelph
        for (i in terms.indices) {
            gradesForm.put("LIST.VAR2_" + (i + 1).toString(), terms[i].shortName)
            gradesForm.put("LIST.VAR3_" + (i + 1).toString(), terms[i].longName.replace(' ', '+'))
            gradesForm.put("DATE.LIST.VAR1_" + (i + 1).toString(), terms[i].startDate)
            gradesForm.put("DATE.LIST.VAR2_" + (i + 1).toString(), terms[i].endDate)
        }
        //Add the other miscellaneous parameters
        gradesForm.put("VAR1", "")
        gradesForm.put("RETURN.URL", "https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TOKENIDX=" + this.homeCookies.get("LASTTOKEN") + "&type=M&constituency=WBST&pid=CORE-WBST")
        gradesForm.put("SUBMIT_OPTIONS", "")

        //Connect to the view grades page for the selected term
        res = Jsoup.connect(res.url().toString())
            .data(gradesForm)
            .cookies(res.cookies())
            .method(Method.POST)
            .followRedirects(true)
            .execute()

        //Parse the resulting document's HTML
        var doc: Document = res.parse()

        //Get the advisor and the term GPA
        advisor.append(doc.getElementById("GROUP_Grp_LIST_VAR1").getElementsByTag("p")[0].text())
        //Converting termGPA to float, and then to string again removes any unnecessary zeros
        termGPA.append(doc.getElementById("GROUP_Grp_VAR1").getElementById("LIST_VAR2").text().toFloat().toString() + "%")

        //Loop through all the courses for the selected term, adding course and grade info to the ArrayList of grades
        for(row: Element in doc.getElementById("GROUP_Grp_LIST_VAR3").getElementsByTag("tr")) {
            //If the current row isn't a header row, and actually contains data
            if (row.getElementsByTag("td").size != 0) {
                //Parse the HTML for the desired values
                val courseSection: String = row.getElementsByClass("LIST_VAR3")[0].getElementsByTag("p")[0].text()
                val courseTitle: String = row.getElementsByClass("VAR_STC_TITLE")[0].getElementsByTag("p")[0].text()
                var finalGrade: String = row.getElementsByClass("VAR_STC_VERIFIED_GRADE")[0].getElementsByTag("p")[0].text()
                //Check to see if the final grade is numeric, and set it's value accordingly
                finalGrade = when(finalGrade.toIntOrNull()) {
                    null -> finalGrade
                    else -> finalGrade.toInt().toString() + "%" //Converting finalGrade to integer and then back to string removes any leading zeros that may be present
                }
                val credits: String = row.getElementsByClass("VAR_STC_CRED")[0].getElementsByTag("p")[0].text()

                //Add the grade to the ArrayList
                grades.add(Grade(courseSection, courseTitle, finalGrade, credits))
            }
        }
    }

    @Throws(Exception::class)
    public suspend fun getNewsItems(): List<NewsItem> {
        //Check that a connection has been initialized
        if (this.homeCookies.size == 0) {
            throw Exception("Error: The connection has not yet been initialized.")
        }

        //Create an ArrayList to store the news items
        var newsItems = ArrayList<NewsItem>()

        //Make a connection to the WebAdvisor main page for students (the one with the news)
        val res: Response = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TOKENIDX=" + this.homeCookies.get("LASTTOKEN") + "&type=M&constituency=WBST&pid=CORE-WBST")
            .cookies(this.homeCookies)
            .followRedirects(true)
            .execute()

        //Parse the resulting document's HTML
        var doc: Document = res.parse()

        //Loop through all the news items on the page, adding each item to the ArrayList of news items
        for (i in 0..(doc.getElementById("content").getElementsByClass("customText").last().getElementsByTag("h1").size - 1)) {
            //Get the main heading of a particular news item
            val heading1: String = doc.getElementById("content").getElementsByClass("customText").last().getElementsByTag("h1")[i].text()

            //Get the subheading of a particular news item
            val heading2: String = doc.getElementById("content").getElementsByClass("customText").last().getElementsByTag("h2")[i].text()

            //Get the body of a particular news item
            val body: String = doc.getElementById("content").getElementsByClass("customText").last().getElementsByTag("p")[i].html()

            //Create a new NewsItem object, and add it to the ArrayList
            newsItems.add(NewsItem(heading1, heading2, body))
        }

        //Return the resulting list of news items
        return newsItems
    }
}