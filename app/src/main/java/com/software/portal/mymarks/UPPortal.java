package com.software.portal.mymarks;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Noko on 2016/06/14.
 */
public class UPPortal {

    private final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:49.0) Gecko/20100101 Firefox/49.0";
    private final String BASE_URL = "https://upnet.up.ac.za/psp/pscsmpra/EMPLOYEE/HRMS/c/UP_SS_MENU.UP_SS_STUDENT.GBL";
    private java.net.CookieManager cm;
    public UPPortal() {
        System.setProperty("jsse.enableSNIExtension", "false");

        cm = new java.net.CookieManager();
        java.net.CookieHandler.setDefault(cm);
    }

    public ArrayList<String[]> processXML(String XML) {
        Pattern MY_PATTERN = Pattern.compile("<tr.*?STDNT_WEEK_SCHD.*?/tr>");
        Matcher m = MY_PATTERN.matcher(XML);
        m.find();
        m.find();

        ArrayList<String[]> marks = new ArrayList<String[]>();
        while (m.find()) {

            marks.add(splitRow(m.group(0)));
        }

        return marks;
    }

    private String[] splitRow(String row) {
        Pattern MY_PATTERN = Pattern.compile("<td.*?/td>");
        Matcher m = MY_PATTERN.matcher(row);
        Pattern EXTRACT = Pattern.compile(">([^></]+?)<");
        Matcher n;

        String result[] = new String[6];
        int i = 0;

        while(m.find()) {
            n = EXTRACT.matcher(m.group(0));
            n.find();
            result[i] = n.group(1);
            i += 1;
        }

        return result;
    }

    private void getCookie() throws Exception {
        URL obj = new URL(BASE_URL);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.connect();
    }

    private boolean login(String studentNumber, String password) throws Exception {
        getCookie();

        URL obj = new URL(BASE_URL);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
        con.setRequestMethod("POST");

        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Referer", BASE_URL + "?null&cmd=login&languageCd=ENG");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setDoOutput(true);

        DataOutputStream out = new DataOutputStream(con.getOutputStream());
        String content = "timezoneOffset=-120&ptmode=f&ptlangcd=ENG&ptinstalledlang=AFR%2CENG&ptlangsel=ENG";
        content += "&userid=u" + URLEncoder.encode(studentNumber, "UTF-8") + "&pwd=" + URLEncoder.encode(password, "UTF-8");

        out.writeBytes(content);
        out.flush();
        out.close();


        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        String html = response.toString();
        return html.contains(studentNumber);
    }

    public String getMarks(String studentNumber, String password) throws Exception {
        if (login(studentNumber, password)) {
            //GET ICSID
            URL obj = new URL("https://upnet.up.ac.za/psc/pscsmpra/EMPLOYEE/HRMS/c/UP_SS_MENU.UP_SS_STUDENT.GBL?PortalActualURL=https%3a%2f%2fupnet.up.ac.za%2fpsc%2fpscsmpra%2fEMPLOYEE%2fHRMS%2fc%2fUP_SS_MENU.UP_SS_STUDENT.GBL&PortalContentURL=https%3a%2f%2fupnet.up.ac.za%2fpsc%2fpscsmpra%2fEMPLOYEE%2fHRMS%2fc%2fUP_SS_MENU.UP_SS_STUDENT.GBL&PortalContentProvider=HRMS&PortalCRefLabel=UP%20Student%20Centre&PortalRegistryName=EMPLOYEE&PortalServletURI=https%3a%2f%2fupnet.up.ac.za%2fpsp%2fpscsmpra%2f&PortalURI=https%3a%2f%2fupnet.up.ac.za%2fpsc%2fpscsmpra%2f&PortalHostNode=HRMS&NoCrumbs=yes&PortalKeyStruct=yes");
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Referer", BASE_URL);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            String s = response.toString();
            Pattern MY_PATTERN = Pattern.compile("'ICSID' value=(\\S+)");
            Matcher m = MY_PATTERN.matcher(s);
            m.find();
            s = m.group(0);
            MY_PATTERN = Pattern.compile("value='(\\S+)'");
            m = MY_PATTERN.matcher(s);
            m.find();
            s = m.group(1);

            //Request page with timetable
            obj = new URL(BASE_URL);
            con = (HttpsURLConnection) obj.openConnection();
            con.setRequestMethod("POST");

            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Referer", BASE_URL + "?PortalActualURL=https%3a%2f%2fupnet.up.ac.za%2fpsc%2fpscsmpra%2fEMPLOYEE%2fHRMS%2fc%2fUP_SS_MENU.UP_SS_STUDENT.GBL&PortalContentURL=https%3a%2f%2fupnet.up.ac.za%2fpsc%2fpscsmpra%2fEMPLOYEE%2fHRMS%2fc%2fUP_SS_MENU.UP_SS_STUDENT.GBL&PortalContentProvider=HRMS&PortalCRefLabel=UP%20Student%20Centre&PortalRegistryName=EMPLOYEE&PortalServletURI=https%3a%2f%2fupnet.up.ac.za%2fpsp%2fpscsmpra%2f&PortalURI=https%3a%2f%2fupnet.up.ac.za%2fpsc%2fpscsmpra%2f&PortalHostNode=HRMS&NoCrumbs=yes&PortalKeyStruct=yes");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setDoOutput(true);

            DataOutputStream out = new DataOutputStream(con.getOutputStream());
            String content = "ICAJAX=1&ICNAVTYPEDROPDOWN=1&ICType=Panel&ICElementNum=0&ICStateNum=1&ICAction=UP_DERIVED_SSR_SS_ENRL_APP_LINK&ICXPos=0&ICYPos=0&ResponsetoDiffFrame=-1&TargetFrameName=None&FacetPath=None&ICFocus=&ICSaveWarningFilter=0&ICChanged=0&ICAutoSave=0&ICResubmit=0&ICActionPrompt=false&ICBcDomData=C~UP_STUDENT_SELF_SERVICE~EMPLOYEE~HRMS~UP_SS_MENU.UP_SS_STUDENT.GBL~UnknownValue~UP%20Student%20Centre~UnknownValue~UnknownValue~https%3A%2F%2Fupnet.up.ac.za%2Fpsp%2Fpscsmpra%2FEMPLOYEE%2FHRMS%2Fc%2FUP_SS_MENU.UP_SS_STUDENT.GBL~UnknownValue*F~CO_EMPLOYEE_SELF_SERVICE~EMPLOYEE~HRMS~UnknownValue~UnknownValue~Self%20Service~UnknownValue~UnknownValue~https%3A%2F%2Fupnet.up.ac.za%2Fpsp%2Fpscsmpra%2FEMPLOYEE%2FHRMS%2Fs%2FWEBLIB_PT_NAV.ISCRIPT1.FieldFormula.IScript_PT_NAV_INFRAME%3Fpt_fname%3DCO_EMPLOYEE_SELF_SERVICE%26c%3DeKZVJsGrrfoD903DKryoi2SZejftdvfj%26FolderPath%3DPORTAL_ROOT_OBJECT.CO_EMPLOYEE_SELF_SERVICE%26IsFolder%3Dtrue~UnknownValue&ICPanelName=&ICFind=&ICAddCount=&ICAPPCLSDATA=&ptus_defaultlocalnode=PSFT_LS&ptus_dbname=PSCSMPRA&ptus_portal=EMPLOYEE&ptus_node=HRMS&ptus_workcenterid=&ptus_componenturl=https%3A%2F%2Fupnet.up.ac.za%2Fpsp%2Fpscsmpra%2FEMPLOYEE%2FHRMS%2Fc%2FUP_SS_MENU.UP_SS_STUDENT.GBL";
            content += "&ICSID=" + s;

            out.writeBytes(content);
            out.flush();
            out.close();

            in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //logout
            obj = new URL("https://upnet.up.ac.za/psp/pscsmpra/EMPLOYEE/HRMS/?cmd=logout");
            HttpsURLConnection logout = (HttpsURLConnection) obj.openConnection();
            logout.setRequestMethod("GET");
            logout.setRequestProperty("User-Agent", USER_AGENT);
            logout.connect();

            return response.toString();
        } else {
            return "Failed to log in";
        }
    }
}
