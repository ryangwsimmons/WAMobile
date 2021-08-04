@file:Suppress("BlockingMethodInNonBlockingContext")

package ca.ryangwsimmons.wamobile

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.jsoup.Connection.Method
import org.jsoup.Connection.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

import kotlin.collections.Map

@Parcelize
class WASession(private val username: String, private val password: String, private var homeCookies: Map<String, String>): Parcelable {

    @Throws(Exception::class)
    fun initConnection() {
        //Make the initial connection to the WebAdvisor main page
        var res: Response = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TYPE=M&PID=CORE-WBMAIN&TOKENIDX=").followRedirects(true).execute()

        //Collect the cookies from the previous connection
        val cookies: MutableMap<String, String> = res.cookies()

        //Make the login request to the server
        res = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TOKENIDX="
                + cookies["LASTTOKEN"]
                + "&SS=LGRQ&URL=https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TYPE=M%26PID=CORE-WBMAIN%26TOKENIDX="
                + cookies["LASTTOKEN"]
                + "%26WARN=Y")
            .data("USER.NAME", this.username, "CURR.PWD", this.password, "RETURN.URL", "https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TYPE=M&PID=CORE-WBMAIN", "SUBMIT_OPTIONS", "")
            .cookies(cookies)
            .method(Method.POST)
            .followRedirects(true)
            .execute()

        //Update the cookies from the initial connection with the cookies from the login request
        cookies.putAll(res.cookies())

        //Parse the resulting document
        val doc: Document = res.parse()

        //Check that the series of connections resulted in the user getting the main page, and not the log in page or some other page
        val docTitle: String = doc.getElementsByTag("title").text()
        if (!(docTitle.contains("Main Menu"))) {
            throw Exception("Unable to log into WebAdvisor. Check that your login credentials are correct.")
        }

        //Copy the cookies from the home page into the homeCookies property
        this.homeCookies = cookies.toMap()
    }

    @Throws(Exception::class)
    fun getName(): String {
        //Check that a connection has been initialized
        if (this.homeCookies.isEmpty()) {
            throw Exception("Error: The connection has not yet been initialized.")
        }
        //Connect to the WebAdvisor main page
        val res: Response = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TYPE=M&PID=CORE-WBMAIN&TOKENIDX=" + this.homeCookies["LASTTOKEN"])
                                 .cookies(this.homeCookies)
                                 .followRedirects(true)
                                 .execute()

        //Parse the response document's HTML
        val doc: Document = res.parse()

        //Get the name
        var name: String = doc.getElementById("global")
            .getElementsByClass("container")[0]
            .getElementsByTag("h1")[0]
            .getElementsByClass("department")[0]
                              .getElementsByTag("a").text()
        name = "(?<=WebAdvisor for ).*$".toRegex(RegexOption.IGNORE_CASE).find(name)!!.value

        //Return the name
        return name
    }

    @Throws(Exception::class)
    fun getTermsResponse(): Response {
        //Check that a connection has been initialized
        if (this.homeCookies.isEmpty()) {
            throw Exception("Error: The connection has not yet been initialized.")
        }

        //Connect to the WebAdvisor grades page for the signed-in user

        return Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TOKENIDX="
                + homeCookies["LASTTOKEN"] + "&CONSTITUENCY=WBST&type=P&pid=ST-WESTS02A")
            .cookies(homeCookies)
            .followRedirects(true)
            .execute()
    }

    @Throws(Exception::class)
    fun getTerms(): ArrayList<Term> {
        //Check that a connection has been initialized
        if (this.homeCookies.isEmpty()) {
            throw Exception("Error: The connection has not yet been initialized.")
        }

        //Create a new ArrayList to hold all the terms obtained from the request
        val terms: ArrayList<Term> = ArrayList()

        //Connect to the WebAdvisor grades page for the signed-in user
        val res: Response = this.getTermsResponse()

        //Parse the response document's HTML
        val doc: Document = res.parse()

        //Add all the terms to the ArrayList
        for(row: Element in doc.getElementById("GROUP_Grp_LIST_VAR1").getElementsByTag("tr")) {
            //If the current row in the table contains body cells (as opposed to header cells), add a new Term to the ArrayList
            if (row.getElementsByTag("td").size != 0) {
                val shortName: String = row.getElementsByClass("LIST_VAR2")[0].getElementsByTag("p")[0].text()
                val longName: String = row.getElementsByClass("LIST_VAR3")[0].getElementsByTag("p")[0].text()
                val startDate: String = row.getElementsByClass("DATE_LIST_VAR1")[0].getElementsByTag("p")[0].text()
                val endDate: String = row.getElementsByClass("DATE_LIST_VAR2")[0].getElementsByTag("p")[0].text()

                terms.add(Term(shortName, longName, startDate, endDate))
            }
        }

        //Return the ArrayList of Terms
        return terms
    }

    @Throws(Exception::class)
    fun getGrades(termPosition: Int, terms: List<Term>, grades: ArrayList<Grade>, advisor: StringBuilder, termGPA: StringBuilder) {
        //Check that a connection has been initialized
        if (this.homeCookies.isEmpty()) {
            throw Exception("Error: The connection has not yet been initialized.")
        }

        //Connect to the WebAdvisor terms page for the signed-in user
        var res: Response = this.getTermsResponse()

        //Create the request form for the next request using a map
        val gradesForm: HashMap<String, String> = HashMap()

        //Construct the form
        //Add the parameters that stay the same for every request
        gradesForm["LIST.VAR1_CONTROLLER"] = "LIST.VAR1"
        gradesForm["LIST.VAR1_MEMBERS"] = "LIST.VAR1*LIST.VAR2*LIST.VAR3*DATE.LIST.VAR1*DATE.LIST.VAR2"
        gradesForm["LIST.VAR1_MAX"] = terms.size.toString()
        gradesForm["LIST.VAR2_MAX"] = terms.size.toString()
        gradesForm["LIST.VAR3_MAX"] = terms.size.toString()
        gradesForm["DATE.LIST.VAR1_MAX"] = terms.size.toString()
        gradesForm["DATE.LIST.VAR2_MAX"] = terms.size.toString()
        //Add the parameter that determines which term has been selected
        gradesForm["LIST.VAR1_RADIO"] = "LIST.VAR1_" + (termPosition + 1).toString()
        //Add the parameters that repeat for every term the signed-in user has attended at Guelph
        for (i in terms.indices) {
            gradesForm["LIST.VAR2_" + (i + 1).toString()] = terms[i].shortName
            gradesForm["LIST.VAR3_" + (i + 1).toString()] = terms[i].longName.replace(' ', '+')
            gradesForm["DATE.LIST.VAR1_" + (i + 1).toString()] = terms[i].startDate
            gradesForm["DATE.LIST.VAR2_" + (i + 1).toString()] = terms[i].endDate
        }
        //Add the other miscellaneous parameters
        gradesForm["VAR1"] = ""
        gradesForm["RETURN.URL"] = "https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TOKENIDX=" + this.homeCookies["LASTTOKEN"] + "&type=M&constituency=WBST&pid=CORE-WBST"
        gradesForm["SUBMIT_OPTIONS"] = ""

        //Connect to the view grades page for the selected term
        res = Jsoup.connect(res.url().toString())
            .data(gradesForm)
            .cookies(res.cookies())
            .method(Method.POST)
            .followRedirects(true)
            .execute()

        //Parse the resulting document's HTML
        val doc: Document = res.parse()

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
    fun getNews(): NewsData {
        //Check that a connection has been initialized
        if (this.homeCookies.isEmpty()) {
            throw Exception("Error: The connection has not yet been initialized.")
        }

        // Create a string to hold the news HTML
        var html = ""

        // Make a connection to the WebAdvisor main page for students (the one with the news)
        var res: Response =
            Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TOKENIDX=" + this.homeCookies["LASTTOKEN"] + "&type=M&constituency=WBST&pid=CORE-WBST")
                .cookies(this.homeCookies)
                .followRedirects(true)
                .execute()

        // Parse the resulting document's HTML
        val doc: Document = res.parse()

        // Find the news HTML in the page body
        for (customText: Element in doc.getElementById("content")
            .getElementsByClass("customText")) {
            if (customText.getElementsByTag("h1").size > 0 && customText.getElementsByTag("h2").size > 0 && customText.getElementsByTag(
                    "p"
                ).size > 0
            ) {
                html = customText.html()
                break
            }
        }

        // Add the outer divs back to the HTML, so that CSS styling works correctly
        html = "<div id=\"main\"><div id=\"content\">$html</div></div>"

        // Get the CSS for the news HTML
        res = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/stylesheets/themes/GUELPH/template.css")
                .cookies(this.homeCookies)
                .followRedirects(true)
                .ignoreContentType(true)
                .execute()

        var css: String = res.body()

        res = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/stylesheets/themes/GUELPH/ug-override.css")
            .cookies(this.homeCookies)
            .followRedirects(true)
            .ignoreContentType(true)
            .execute()

        css += res.body()

        // Return the news data
        return NewsData(html, css)
    }

    @Throws(Exception::class)
    fun getSearchSectionsFilterValues(): String {
        // Make a request to the Catalog Advanced Search options endpoint
        val res = Jsoup.connect("https://colleague-ss.uoguelph.ca/Student/Courses/GetCatalogAdvancedSearchAsync")
            .ignoreContentType(true)
            .execute()

        // Return the JSON string
        return res.body()
    }


    @Throws(Exception::class)
    fun getSearchResults(
        term: String,
        meetingStartDate: String,
        meetingEndDate: String,
        subjects: ArrayList<String>,
        courseNums: ArrayList<String>,
        sections: ArrayList<String>,
        timeOfDay: String,
        timeStartsBy: String,
        timeEndsBy: String,
        days: ArrayList<Boolean>,
        location: String,
        academicLevel: String,
        page: Int = 1,
        existingCookies: MutableMap<String, String>? = null,
        existingReqVerToken: String? = null
    ): SearchResultsData {
        var res: Response
        val cookies: MutableMap<String, String>
        val reqVerToken: String

        if (existingCookies != null && existingReqVerToken != null) {
            cookies = existingCookies
            reqVerToken = existingReqVerToken
        } else {
            // Check that a connection has been initialized
            if (this.homeCookies.isEmpty()) {
                throw Exception("Error: The connection has not yet been initialized.")
            }

            // Connect to the WebAdvisor students page
            res = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TYPE=M&CONSTITUENCY=WBST&PID=CORE-WBST&TOKENIDX=" + this.homeCookies["LASTTOKEN"])
                .cookies(this.homeCookies)
                .followRedirects(true)
                .execute()

            // Parse the HTML
            var doc = res.parse()

            // Get the <a> tag for the course catalog page, extract the token
            val token = "Token=(.*)\$".toRegex().find(
                doc.getElementsByClass("subnav")[0]
                    .select("a:contains(Search the Course Catalogue)")[0]
                    .attr("href")
            )!!.groupValues[1]

            // Make a request to the Search for Sections page to get a request verification code and cookies
            res = Jsoup.connect("https://colleague-ss.uoguelph.ca/Student/Courses?Token=$token")
                .followRedirects(true)
                .execute()

            // Store the cookies in a variable
            cookies = res.cookies()

            // Parse the response text to get the HTML
            doc = res.parse()

            // Store the request verification token in a variable
            reqVerToken = doc.select("input[name='__RequestVerificationToken']").attr("value")
        }

        // Build the POST body JSON for the next request
        val postBodyJson = """
            {
                "searchParameters": "{
                    \"terms\": [${if (term == "") "" else "\\\"$term\\\""}],
                    \"startTime\": ${if (timeOfDay.split(',')[0] == "") "null" else timeOfDay.split(',')[0]},
                    \"endTime\": ${if (timeOfDay.split(',')[1] == "") "null" else timeOfDay.split(',')[1]},
                    \"academicLevels\": [${if (academicLevel == "") "" else "\\\"$academicLevel\\\""}],
                    \"days\": [${days.mapIndexedNotNull { index: Int, day: Boolean -> if (day) "\\\"$index\\\"" else null}.joinToString(", ")}],
                    \"locations\": [${if (location == "") "" else "\\\"$location\\\""}],
                    \"keywordComponents\": [${subjects.mapIndexedNotNull { index: Int, subject: String ->
                        if (subject != "" || courseNums[index] != "" || sections[index] != "") {
                            """
                                {
                                    \"subject\": \"$subject\",
                                    \"courseNumber\": \"${courseNums[index]}\",
                                    \"section\": \"${sections[index]}\",
                                    \"synonym\": \"\"
                                }
                            """.trimIndent()
                        } else null
                    }.joinToString(", ")}],
                    \"startDate\": ${if (meetingStartDate == "") "null" else "\\\"$meetingStartDate\\\""},
                    \"endDate\": ${if (meetingEndDate == "") "null" else "\\\"$meetingEndDate\\\""},
                    \"startsAtTime\": ${if (timeStartsBy == "") "null" else "\\\"$timeStartsBy\\\""},
                    \"endsByTime\": ${if (timeEndsBy == "") "null" else "\\\"$timeEndsBy\\\""},
                    \"pageNumber\": $page,
                    \"sortOn\": \"None\",
                    \"sortDirection\": \"Ascending\",
                    \"quantityPerPage\": 30,
                    \"searchResultsView\": \"SectionListing\"
                }"
            }
        """.trimIndent().replace("\n", "")

        res = Jsoup.connect("https://colleague-ss.uoguelph.ca/Student/Courses/SearchAsync")
            .method(Method.POST)
            .ignoreContentType(true)
            .cookies(cookies)
            .header("Content-Type", "application/json, charset=utf-8")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("__RequestVerificationToken", reqVerToken)
            .requestBody(postBodyJson)
            .execute()

        return SearchResultsData(cookies, reqVerToken, res.body())
    }

    fun getSectionDetails(sectionId: String, cookies: MutableMap<String, String>, reqVerToken: String): String {
        // Build the body for the POST request
        val postBody = "{\"sectionId\": \"$sectionId\"}"

        // Make a request to the API to get section details for the given ID
        val res = Jsoup.connect("https://colleague-ss.uoguelph.ca/Student/Courses/SectionDetails")
            .method(Method.POST)
            .ignoreContentType(true)
            .cookies(cookies)
            .header("Content-Type", "application/json")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("__RequestVerificationToken", reqVerToken)
            .requestBody(postBody)
            .execute()

        return res.body()
    }

    @Throws(Exception::class)
    fun getEllucianCookies(): MutableMap<String, String>{
        //Check that a connection has been initialized
        if (this.homeCookies.isEmpty()) {
            throw Exception("Error: The connection has not yet been initialized.")
        }

        //Connect to the WebAdvisor students page
        var res: Response = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TYPE=M&CONSTITUENCY=WBST&PID=CORE-WBST&TOKENIDX=" + this.homeCookies["LASTTOKEN"])
            .cookies(this.homeCookies)
            .followRedirects(true)
            .execute()

        // Parse the HTML
        val doc: Document = res.parse()

        // Get the <a> tag for the account view, extract the token
        val token = "Token=(.*)\$".toRegex().find(
            doc.getElementsByClass("subnav")[0]
                .select("h3:contains(Financial Profile) + ul li a")[0]
                .attr("href")
        )!!.groupValues[1]

        // Connect to the Ellucian portal by submitting the token retrieved from WebAdvisor
        res = Jsoup.connect("https://colleague-ss.uoguelph.ca/Student/Finance?Token=$token")
            .followRedirects(true)
            .execute()

        // Get the cookies from the response
        return res.cookies()
    }

    @Throws(Exception::class)
    fun getAccountViewInfo(ellucianCookies: MutableMap<String, String>, timeFrameID: String = ""): String{
        // Navigate to the account activity page
        var res = Jsoup.connect("https://colleague-ss.uoguelph.ca/Student/Finance/AccountActivity")
            .cookies(ellucianCookies)
            .followRedirects(true)
            .execute()

        // Parse the response text to get the HTML
        val doc = res.parse()

        // Get the request verification token
        val reqVerToken = doc.select("input[name='__RequestVerificationToken']").attr("value")

        // If a time frame ID has been passed in, form the request body
        val bodyJSON = if (timeFrameID != "") {
            "{\"timeframeId\":\"$timeFrameID\"}"
        } else {
            "{}"
        }

        // Make a request to the Account Activity Info endpoint
        res = Jsoup.connect("https://colleague-ss.uoguelph.ca/Student/Finance/AccountActivity/GetAccountActivityViewModel")
            .method(Method.POST)
            .ignoreContentType(true)
            .cookies(ellucianCookies)
            .header("Content-Type", "application/json")
            .header("__RequestVerificationToken", reqVerToken)
            .header("X-Requested-With", "XMLHttpRequest")
            .requestBody(bodyJSON)
            .execute()

        return res.body()
    }
}