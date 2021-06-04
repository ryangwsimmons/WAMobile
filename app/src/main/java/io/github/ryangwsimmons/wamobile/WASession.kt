@file:Suppress("BlockingMethodInNonBlockingContext")

package io.github.ryangwsimmons.wamobile

import android.annotation.SuppressLint
import android.os.Parcelable
import android.text.Html
import android.text.Spanned
import kotlinx.android.parcel.Parcelize
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
    suspend fun getTerms(): ArrayList<Term> {
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
    suspend fun getGrades(termPosition: Int, terms: List<Term>, grades: ArrayList<Grade>, advisor: StringBuilder, termGPA: StringBuilder) {
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
    fun getNewsItems(): List<NewsItem> {
        //Check that a connection has been initialized
        if (this.homeCookies.isEmpty()) {
            throw Exception("Error: The connection has not yet been initialized.")
        }

        //Create an ArrayList to store the news items
        val newsItems = ArrayList<NewsItem>()

        //Make a connection to the WebAdvisor main page for students (the one with the news)
        val res: Response = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TOKENIDX=" + this.homeCookies["LASTTOKEN"] + "&type=M&constituency=WBST&pid=CORE-WBST")
            .cookies(this.homeCookies)
            .followRedirects(true)
            .execute()

        //Parse the resulting document's HTML
        val doc: Document = res.parse()

        //Loop through all the news items on the page, adding each item to the ArrayList of news items
        var studentGroup: String = ""
        var itemHeading: String = ""
        var itemBody: Spanned
        var i: Int = 0
        for (customText: Element in doc.getElementById("content").getElementsByClass("customText")) {
            if (customText.getElementsByTag("h1").size > 0 && customText.getElementsByTag("h2").size > 0 && customText.getElementsByTag("p").size > 0) {
                for (element: Element in customText.children()) {
                    //Get the main heading of a particular news item
                    if (element.tagName() == "h1") {
                        studentGroup = element.text()
                    }

                    //Get the subheading of a particular news item
                    if (element.tagName() == "h2") {
                        itemHeading = element.text()
                    }

                    //Get the body of a particular news item, create new NewsItem object, and add the item to it
                    if (element.tagName() == "p") {
                        itemBody = if (android.os.Build.VERSION.SDK_INT > 23) {
                            Html.fromHtml(element.html(), Html.FROM_HTML_MODE_COMPACT)
                        } else {
                            Html.fromHtml(element.html())
                        }
                        newsItems.add(NewsItem(itemHeading, studentGroup, itemBody))
                    }
                }
            }
        }

        //Return the resulting list of news items
        return newsItems
    }

    @Throws(Exception::class)
    private fun getSearchSectionsResponse(): Response {
        //Check that a connection has been initialized
        if (this.homeCookies.isEmpty()) {
            throw Exception("Error: The connection has not yet been initialized.")
        }

        //Connect to the WebAdvisor search for sections filter page

        //Return the response
        return Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TOKENIDX=" + homeCookies["LASTTOKEN"] + "&CONSTITUENCY=WBST&type=P&pid=ST-WESTS12A")
            .cookies(homeCookies)
            .followRedirects(true)
            .execute()
    }

    @Throws(Exception::class)
    suspend fun getSearchSectionsFilterValues(terms: ArrayList<DropdownOption>,
                                                    subjects: ArrayList<DropdownOption>,
                                                    courseLevels: ArrayList<DropdownOption>,
                                                    times: ArrayList<DropdownOption>,
                                                    locations: ArrayList<DropdownOption>,
                                                    academicLevels: ArrayList<DropdownOption>) {
        //Check that a connection has been initialized
        if (this.homeCookies.isEmpty()) {
            throw Exception("Error: The connection has not yet been initialized.")
        }

        //Connect to the WebAdvisor search for sections filter page, and get the response
        val res: Response = this.getSearchSectionsResponse()

        //Parse the resulting document's HTML
        val doc: Document = res.parse()

        //Get the list of terms
        for (term: Element in doc.getElementById("VAR1").getElementsByTag("option")) {
            //Get the name of the term option
            val name: String = term.text()

            //Get the value of the term option
            val value: String = term.attr("value")

            //Add the term option to the ArrayList of terms
            terms.add(DropdownOption(name, value))
        }

        //Get the list of subjects
        for (subject: Element in doc.getElementById("LIST_VAR1_1").getElementsByTag("option")) {
            //Get the name of the subject option
            val name: String = subject.text()

            //Get the value of the subject option
            val value: String = subject.attr("value")

            //Add the subject option to the ArrayList of subjects
            subjects.add(DropdownOption(name, value))
        }

        //Get the list of course levels
        for (courseLevel: Element in doc.getElementById("LIST_VAR2_1").getElementsByTag("option")) {
            //Get the name of the course level option
            val name: String = courseLevel.text()

            //Get the value of the course level option
            val value: String = courseLevel.attr("value")

            //Add the course level option to the ArrayList of course levels
            courseLevels.add(DropdownOption(name, value))
        }

        //Get the list of times
        for (time: Element in doc.getElementById("VAR7").getElementsByTag("option")) {
            //Get the name of the time option
            val name: String = time.text()

            //Get the value of the time option
            val value: String = time.attr("value")

            //Add the time option to the ArrayList of times
            times.add(DropdownOption(name, value))
        }

        //Get the list of locations
        for (location: Element in doc.getElementById("VAR6").getElementsByTag("option")) {
            //Get the name of the location option
            val name: String = location.text()

            //Get the value of the location option
            val value: String = location.attr("value")

            //Add the location option to the ArrayList of locations
            locations.add(DropdownOption(name, value))
        }

        //Get the list of academic levels
        for (academicLevel: Element in doc.getElementById("VAR21").getElementsByTag("option")) {
            //Get the name of the academic level option
            val name: String = academicLevel.text()

            //Get the value of the academic level option
             val value: String = academicLevel.attr("value")

            //Add the academic level option to the ArrayList of academic levels
            academicLevels.add(DropdownOption(name, value))
        }
    }

    private fun formatMeeting(meetingString: String): String {
        //Function to format meeting strings into how they should look, given a WebAdvisor unformatted meeting string
        //Full disclosure - while all of the code written below is my own, some of the logic is derived from the same logic used to format the meetings on the actual WebAdvisor page
        //By making my formatting logic similar to the actual WebAdvisor formatting logic, I can handle any edge cases I might have otherwise missed

        var meeting: String = meetingString
        //If a meeting only contains dates, returns true, otherwise false
        fun isDatesOnly(meetingInfo: List<String>): Boolean {
            return meetingInfo.size <= 2
        }

        //Gets the location of a meeting
        fun getLocation(meetingInfo: List<String>): String? {
            if (!isDatesOnly(meetingInfo)) {
                return meetingInfo[5].trim()
            }
            return null
        }

        //Regular expression that checks if the meeting string is just dates
        var regex: Regex = Regex("^(\\d{4}/\\d{2}/\\d{2})-(\\d{4}/\\d{2}/\\d{2})$")

        if (regex.matches(meeting)) {
            //If just dates, put a "|" between the start and end dates
            meeting = regex.replace(meeting, "$1|$2")
        } else {
            //If not just dates, replace all duplicate values in the meeting string
            meeting = meeting.replace("Days TBA Days TBA", "Days TBA").replace("Times TBA Times TBA", "Times TBA").replace("Room TBA Room TBA", "Room TBA")
            //Use a regular expression to put "|" between all the different properties of the meeting
            regex = Regex("^(\\d{4}/\\d{2}/\\d{2})-(\\d{4}/\\d{2}/\\d{2}) (LEC|LAB|SEM|EXAM|Distance Education|Electronic) ((Mon,? ?|Tues,? ?|Wed,? ?|Thur,? ?|Fri,? ?|Sat,? ?|Sun,? ?|Days TBA|Days to be Announced){1,7}),? ?(\\d{2}:\\d{2}[AP]M - \\d{2}:\\d{2}[AP]M|Times TBA|Times to be Announced),? ?(.*Room[^,]*)$")
            meeting = regex.replace(meeting, "$1|$2|$3|$4|$6|$7")
        }

        //Create a list of meeting properties by splitting the meeting string using "|" as a delimiter
        val meetingProps: List<String> = meeting.split("|")

        //Get the meeting method
        val method: String = if (!isDatesOnly(meetingProps)) {
            meetingProps[2].trim()
        } else {
            ""
        }

        //Get the meeting days, if available
        val days: String = if (!isDatesOnly(meetingProps)) {
            meetingProps[3].trim()
        } else {
            ""
        }

        //Get the meeting time, if available
        val time: String = if (!isDatesOnly(meetingProps)) {
            meetingProps[4].trim()
        } else {
            ""
        }

        //Get the meeting end date
        val end: String = meetingProps[1].trim()

        //Get the meeting building, if available
        val building: String = if (!isDatesOnly(meetingProps)) {
            val location = getLocation(meetingProps)
            if (location != null) {
                val locationArray = location.split(", ")
                if (locationArray.size > 1) {
                    locationArray[0]
                } else {
                    ""
                }
            } else {
                ""
            }
        } else {
            ""
        }

        //Get the meeting room, if available
        val room: String = if (!isDatesOnly(meetingProps)) {
            val location = getLocation(meetingProps)
            if (location != null) {
                val locationArray = location.split(", ")
                if (locationArray.size > 1) {
                    locationArray[1]
                } else {
                    locationArray[0]
                }
            } else {
                ""
            }
        } else {
            ""
        }

        if (!isDatesOnly(meetingProps) && (method == "" || days == "" || time == "" || room == "")) {
            //If the meeting is not just dates, and one of the key meeting properties is missing, return the original string
            return meetingString
        } else if (!isDatesOnly(meetingProps)) {
            //Otherwise, construct the formatted meeting string
            val meetingFormatted: StringBuilder = StringBuilder()

            //Create the method and days line
            meetingFormatted.append("$method $days\n")

            //If the meeting is an exam, add the exam time and date, otherwise just add the time
            if (method == "EXAM") {
                meetingFormatted.append("$time ($end)\n")
            } else {
                meetingFormatted.append(time + "\n")
            }

            //If the building is available, add it and the room, otherwise add just the room
            if (building != "") {
                meetingFormatted.append("$building, $room")
            } else {
                meetingFormatted.append(room)
            }

            //Return the formatted meeting string
            return meetingFormatted.toString()
        }

        //If the meeting is just two dates, return an empty string
        return ""
    }


    @Throws(Exception::class)
    suspend fun getSearchResults(term: String,
                                        subjects: ArrayList<String>,
                                        courseLevels: ArrayList<String>,
                                        courseNums: ArrayList<String>,
                                        sections: ArrayList<String>,
                                        times: ArrayList<String>,
                                        days: ArrayList<Boolean>,
                                        courseKeywords: String,
                                        location: String,
                                        academicLevel: String,
                                        instructorsLastName: String,
                                        cookies: MutableMap<String, String>): ArrayList<SearchResult> {
        //Make initial request to filters page
        var res: Response = this.getSearchSectionsResponse()

        //Create the ArrayList to hold search results
        val results: ArrayList<SearchResult> = ArrayList()

        //Create and populate a map that holds the request body
        val searchForm: HashMap<String, String> = HashMap()

        searchForm["VAR1"] = term
        searchForm["DATE.VAR1"] = ""
        searchForm["DATE.VAR2"] = ""
        searchForm["LIST.VAR1_CONTROLLER"] = "LIST.VAR1"
        searchForm["LIST.VAR1_MEMBERS"] = "LIST.VAR1*LIST.VAR2*LIST.VAR3*LIST.VAR4"
        searchForm["LIST.VAR1_MAX"] = "5"
        searchForm["LIST.VAR2_MAX"] = "5"
        searchForm["LIST.VAR3_MAX"] = "5"
        searchForm["LIST.VAR4_MAX"] = "5"
        subjects.forEachIndexed {index, subject ->
            searchForm["LIST.VAR1_" + (index + 1).toString()] = subject
            searchForm["LIST.VAR2_" + (index + 1).toString()] = courseLevels[index]
            searchForm["LIST.VAR3_" + (index + 1).toString()] = courseNums[index]
            searchForm["LIST.VAR4_" + (index + 1).toString()] = sections[index]
        }
        searchForm["VAR7"] = times[0]
        searchForm["VAR8"] = times[1]
        days.forEachIndexed {index, day ->
            searchForm["VAR" + (10 + index).toString()] = if (day) "Y" else ""
        }
        searchForm["VAR3"] = courseKeywords
        searchForm["VAR6"] = location
        searchForm["VAR21"] = academicLevel
        searchForm["VAR9"] = instructorsLastName
        searchForm["RETURN.URL"] = "https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TOKENIDX=" + this.homeCookies["LASTTOKEN"] + "&type=M&constituency=WBST&pid=CORE-WBST"
        searchForm["SUBMIT_OPTIONS"] = ""

        //Make the connection to the server to get the search results page
        res = Jsoup.connect(res.url().toString())
            .data(searchForm)
            .cookies(res.cookies())
            .method(Method.POST)
            .followRedirects(true)
            .execute()

        //Store the cookies from the response in the parameter
        cookies.putAll(res.cookies())

        //Parse the response's HTML
        val doc: Document = res.parse()

        //Go through the results, parse the HTML for the properties of each section, and add each result into the ArrayList
        for (result: Element in doc.getElementById("GROUP_Grp_WSS_COURSE_SECTIONS").getElementsByTag("tbody")[0].getElementsByTag("tr")) {
            if (result.getElementsByTag("td").size > 0) {
                val term: String = result.getElementsByClass("WSS_COURSE_SECTIONS")[0].getElementsByTag("p")[0].text()
                val status: String = result.getElementsByClass("LIST_VAR1")[0].getElementsByTag("p")[0].text()
                val title: String = result.getElementsByClass("SEC_SHORT_TITLE")[0].getElementsByTag("a")[0].text()
                val location: String = result.getElementsByClass("SEC_LOCATION")[0].getElementsByTag("p")[0].text()

                val meetings: ArrayList<String> = ArrayList()
                var unformattedMeetingsString: String = result.getElementsByClass("SEC_MEETING_INFO")[0].getElementsByTag("p")[0].text()
                val regex: Regex = Regex("([ \\n])(\\d{4}/\\d{2}/\\d{2}|/  / {2}  )")
                unformattedMeetingsString = regex.replace(unformattedMeetingsString) { match -> "^" + match.groupValues[2]}
                val unformattedMeetings = unformattedMeetingsString.split("^")
                unformattedMeetings.forEachIndexed { index, unformattedMeeting ->
                    if (index < unformattedMeetings.size - 1) {
                        meetings.add(formatMeeting(unformattedMeeting) + "\n\n")
                    } else {
                        meetings.add(formatMeeting(unformattedMeeting))
                    }
                }

                val faculty: String = result.getElementsByClass("SEC_FACULTY_INFO")[0].getElementsByTag("p")[0].text()
                val availableCapacity: String = result.getElementsByClass("LIST_VAR5")[0].getElementsByTag("p")[0].text()
                val credits: String = result.getElementsByClass("SEC_MIN_CRED")[0].getElementsByTag("p")[0].text()
                val academicLevel: String = result.getElementsByClass("SEC_ACAD_LEVEL")[0].getElementsByTag("p")[0].text()

                //Extract the details URL parameters from the onclick attribute of the title link, and replace the initial "?? with an "&" so that it can be added onto the end of a URL
                val detailsURL: String = "(?<=')(.*?)(?=')".toRegex().find(result.getElementsByClass("SEC_SHORT_TITLE")[0].getElementsByTag("a")[0].attr("onclick"))!!.value.replace("&CLONE=Y", "")

                results.add(SearchResult(term, status, title, location, meetings, faculty, availableCapacity, credits, academicLevel, detailsURL))
            }
        }

        //Return the ArrayList of results
        return results
    }

    @SuppressLint("DefaultLocale")
    fun getSectionDetails(cookies: Map<String, String>, result: SearchResult): SectionDetails {
        //Make the connection to the server
        var res: Response = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor" + result.detailsURL)
            .cookies(cookies)
            .followRedirects(true)
            .execute()

        val doc: Document = res.parse()

        //Get the course name and split it into sections
        val courseNameSections: List<String> = doc.getElementById("VAR2").text().split("*")

        //Get the course name without the section, in all lowercase
        val courseName: String = (courseNameSections[0] + courseNameSections[1]).toLowerCase()

        //Create a variable that holds the url for the course calendar, and populate it based on the academic level of the course
        var calendarURL: String = ""
        when (result.academicLevel) {
            "Diploma" -> calendarURL = "https://www.uoguelph.ca/registrar/calendars/diploma/current/courses/"
            "Graduate" -> calendarURL = "https://www.uoguelph.ca/registrar/calendars/graduate/current/courses/"
            "Undergraduate" -> calendarURL = "https://www.uoguelph.ca/registrar/calendars/undergraduate/current/courses/"
            "Undergraduate Guelph-Humber" -> calendarURL = "https://www.uoguelph.ca/registrar/calendars/guelphhumber/current/courses/"
        }

        //Connect to the calendar page
        res = Jsoup.connect("$calendarURL$courseName.shtml")
            .followRedirects(true)
            .execute()

        //Parse the calendar document
        val calDoc: Document = res.parse()

        //Get the section details, and store them in variables
        val title: String = calDoc.getElementById("content").getElementsByClass("title")[0].text()
        val description: String = calDoc.getElementById("content").getElementsByClass("description")[0].text()
        var offerings: String = ""
        if (calDoc.getElementById("content").getElementsByClass("offerings").size > 0) {
            offerings = calDoc.getElementById("content").getElementsByClass("offerings")[0].getElementsByClass("text")[0].text()
        }
        var restrctions: String = ""
        if (calDoc.getElementById("content").getElementsByClass("restrictions").size > 0) {
            restrctions = calDoc.getElementById("content").getElementsByClass("restrictions")[0].getElementsByClass("text")[0].text()
        }
        var prereqs: String = ""
        if (calDoc.getElementById("content").getElementsByClass("prereqs").size > 0){
            prereqs = calDoc.getElementById("content").getElementsByClass("prereqs")[0].getElementsByClass("text")[0].text()
        }
        val departments: String = calDoc.getElementById("content").getElementsByClass("departments")[0].getElementsByClass("text")[0].text()
        val startDate: String = doc.getElementById("VAR6").text()
        val endDate: String = doc.getElementById("VAR7").text()
        val facultyName: String = doc.getElementById("LIST_VAR7_1").text()
        val facultyEmail: String = doc.getElementById("LIST_VAR10_1").text()
        val facultyPhone: String = doc.getElementById("LIST_VAR8_1").text()
        val facultyExtension: String = doc.getElementById("LIST_VAR9_1").text()

        //Return a new SectionDetails object containing the data
        return SectionDetails(title, description, offerings, restrctions, prereqs, departments, startDate, endDate, facultyName, facultyEmail, facultyPhone, facultyExtension)
    }

    @Throws(Exception::class)
    suspend fun getAccountViewInfo(){
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
        var doc: Document = res.parse()

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
        var cookies: MutableMap<String, String> = res.cookies()

        // Navigate to the account activity page
        res = Jsoup.connect("https://colleague-ss.uoguelph.ca/Student/Finance/AccountActivity")
            .cookies(cookies)
            .followRedirects(true)
            .execute()

        // Parse the response text to get the HTML
        doc = res.parse()

        // Get the request verification token
        val reqVerToken = doc.select("input[name='__RequestVerificationToken']").attr("value")

        // Make a request to the Account Activity Info endpoint
        res = Jsoup.connect("https://colleague-ss.uoguelph.ca/Student/Finance/AccountActivity/GetAccountActivityViewModel")
            .method(Method.POST)
            .ignoreContentType(true)
            .cookies(cookies)
            .header("__RequestVerificationToken", reqVerToken)
            .header("X-Requested-With", "XMLHttpRequest")
            .execute()
    }
}